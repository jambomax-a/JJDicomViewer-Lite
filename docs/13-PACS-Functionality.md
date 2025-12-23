# PACS機能 - Java移植ドキュメント

## 1. 概要

このドキュメントは、HOROSのPACS（Picture Archiving and Communication System）機能を解析し、Javaでの実装方法をまとめたものです。

PACS機能には以下が含まれます：
- **DICOMサーバー機能（SCP）**: 画像の受信、検索、取得要求への応答
- **自動ルーティング**: 受信した画像の自動保存・処理
- **スケジュール機能**: 定期的な検索・取得
- **Worklist機能**: Modality Worklistのサポート（将来の拡張）

## 2. HOROS実装の解析

### 2.1 DCMTKQueryRetrieveSCP

**HOROS実装** (`DCMTKQueryRetrieveSCP.h`):
```objective-c
@interface DCMTKQueryRetrieveSCP : NSObject {
    int _port;
    NSString *_aeTitle;
    NSDictionary *_params;
    BOOL _abort;
    BOOL running;
}

+ (BOOL) storeSCP;
- (id)initWithPort:(int)port aeTitle:(NSString *)aeTitle extraParamaters:(NSDictionary *)params;
- (void)run;
- (void)abort;
- (int) port;
- (NSString*) aeTitle;
- (BOOL) running;
@end
```

**主要機能**:
- DICOMサーバー（SCP）の起動・停止
- C-FIND SCP（検索サーバー）
- C-MOVE SCP（取得サーバー）
- C-STORE SCP（画像受信サーバー）

### 2.2 OsiriXSCPDataHandler

**HOROS実装** (`OsiriXSCPDataHandler.h`):
```objective-c
@interface OsiriXSCPDataHandler : NSObject {
    NSArray *findArray;
    NSEnumerator *findEnumerator;
    NSString *callingAET;
    NSManagedObjectContext *context;
    int numberMoving;
    NSMutableDictionary *logDictionary;
    NSMutableDictionary *findTemplate;
}

- (NSPredicate *)predicateForDataset:(DcmDataset *)dataset 
    compressedSOPInstancePredicate:(NSPredicate**) csopPredicate 
    seriesLevelPredicate:(NSPredicate**) SLPredicate;

- (OFCondition)prepareFindForDataSet:(DcmDataset *)dataset;
- (OFCondition)prepareMoveForDataSet:(DcmDataset *)dataset;
- (OFCondition)nextFindObject:(DcmDataset *)dataset isComplete:(BOOL *)isComplete;
- (OFCondition)nextMoveObject:(char *)imageFileName;
@end
```

**主要機能**:
- データベースからの検索結果の準備
- C-FIND要求への応答データの生成
- C-MOVE要求への応答（ファイルパスの提供）

## 3. DICOMサーバー（SCP）の実装

### 3.1 サーバーの基本構造

**Java実装**:
```java
import org.dcm4che3.net.*;
import org.dcm4che3.net.service.*;
import org.dcm4che3.data.*;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DicomServer {
    private Device device;
    private ApplicationEntity ae;
    private Connection conn;
    private ExecutorService executorService;
    private boolean running = false;
    
    private String aeTitle;
    private int port;
    private Path storagePath;
    private DicomDatabase database;
    
    public DicomServer(String aeTitle, int port, Path storagePath, DicomDatabase database) {
        this.aeTitle = aeTitle;
        this.port = port;
        this.storagePath = storagePath;
        this.database = database;
        
        initializeDevice();
    }
    
    private void initializeDevice() {
        device = new Device("dicomServer");
        
        // ローカル接続
        conn = new Connection();
        conn.setPort(port);
        device.addConnection(conn);
        
        // Application Entity
        ae = new ApplicationEntity(aeTitle);
        ae.addConnection(conn);
        ae.setAssociationAcceptor(true);
        ae.setAssociationInitiator(false);
        
        // Transfer Capabilities
        ae.addTransferCapability(new TransferCapability(
            null, UID.ImplicitVRLittleEndian, TransferCapability.Role.SCP));
        ae.addTransferCapability(new TransferCapability(
            null, UID.ExplicitVRLittleEndian, TransferCapability.Role.SCP));
        ae.addTransferCapability(new TransferCapability(
            null, UID.ExplicitVRBigEndian, TransferCapability.Role.SCP));
        
        // SOP Classes
        ae.addSOPClass(UID.VerificationSOPClass);
        ae.addSOPClass(UID.StudyRootQueryRetrieveInformationModelFIND);
        ae.addSOPClass(UID.StudyRootQueryRetrieveInformationModelMOVE);
        ae.addSOPClass(UID.PatientRootQueryRetrieveInformationModelFIND);
        ae.addSOPClass(UID.PatientRootQueryRetrieveInformationModelMOVE);
        
        // C-STORE SCP
        ae.addSOPClass(UID.CTImageStorage);
        ae.addSOPClass(UID.MRImageStorage);
        ae.addSOPClass(UID.UltrasoundImageStorage);
        // ... その他のSOP Class
        
        device.addApplicationEntity(ae);
        
        // サービスハンドラーの設定
        setupServiceHandlers();
    }
    
    private void setupServiceHandlers() {
        // C-ECHO SCP
        ae.register(new BasicCEchoSCP());
        
        // C-STORE SCP
        ae.register(new DicomStoreSCP(storagePath, database));
        
        // C-FIND SCP
        ae.register(new DicomFindSCP(database));
        
        // C-MOVE SCP
        ae.register(new DicomMoveSCP(database));
    }
    
    public void start() throws IOException {
        if (running) {
            throw new IllegalStateException("Server is already running");
        }
        
        executorService = Executors.newCachedThreadPool();
        device.setExecutor(executorService);
        device.setScheduledExecutor(Executors.newSingleThreadScheduledExecutor());
        device.setBindSocket(new ServerSocket(port));
        
        device.bindConnections();
        running = true;
        
        System.out.println("DICOM Server started on port " + port + " with AE Title: " + aeTitle);
    }
    
    public void stop() throws IOException {
        if (!running) {
            return;
        }
        
        device.unbindConnections();
        if (executorService != null) {
            executorService.shutdown();
        }
        running = false;
        
        System.out.println("DICOM Server stopped");
    }
    
    public boolean isRunning() {
        return running;
    }
}
```

### 3.2 C-STORE SCP（画像受信）

**Java実装**:
```java
import org.dcm4che3.net.service.*;
import org.dcm4che3.data.*;
import org.dcm4che3.io.*;
import java.nio.file.*;

public class DicomStoreSCP extends BasicCStoreSCP {
    private Path storagePath;
    private DicomDatabase database;
    private StoreProgressCallback progressCallback;
    
    public DicomStoreSCP(Path storagePath, DicomDatabase database) {
        this.storagePath = storagePath;
        this.database = database;
    }
    
    @Override
    protected void store(Association as, Attributes rq, Attributes rsp, 
                        PDVInputStream dataStream) throws IOException {
        
        String callingAET = as.getCallingAET();
        String calledAET = as.getCalledAET();
        
        // データセットの読み込み
        Attributes dataset = new Attributes();
        try (DicomInputStream dis = new DicomInputStream(dataStream)) {
            dis.readAttributes(dataset, -1, -1);
        }
        
        // SOP Instance UIDの取得
        String sopInstanceUID = dataset.getString(Tag.SOPInstanceUID);
        if (sopInstanceUID == null) {
            throw new IOException("Missing SOP Instance UID");
        }
        
        // ファイルパスの決定
        Path filePath = determineFilePath(dataset, sopInstanceUID);
        
        // ディレクトリの作成
        Files.createDirectories(filePath.getParent());
        
        // ファイルの保存
        try (DicomOutputStream dos = new DicomOutputStream(
                Files.newOutputStream(filePath))) {
            dos.writeDataset(dataset.createFileMetaInformation(
                dataset.getString(Tag.TransferSyntaxUID)), dataset);
        }
        
        // データベースへの登録
        try {
            database.importDicomFile(filePath, dataset);
            
            if (progressCallback != null) {
                progressCallback.onFileStored(filePath, dataset);
            }
            
            // 成功レスポンス
            rsp.setInt(Tag.Status, VR.US, Status.Success);
            
        } catch (Exception e) {
            // エラーログ
            System.err.println("Failed to store DICOM file: " + e.getMessage());
            e.printStackTrace();
            
            // エラーレスポンス
            rsp.setInt(Tag.Status, VR.US, Status.ProcessingFailure);
            rsp.setString(Tag.ErrorComment, VR.LO, e.getMessage());
        }
    }
    
    private Path determineFilePath(Attributes dataset, String sopInstanceUID) {
        // 階層構造: StudyInstanceUID/SeriesInstanceUID/SOPInstanceUID.dcm
        String studyUID = dataset.getString(Tag.StudyInstanceUID);
        String seriesUID = dataset.getString(Tag.SeriesInstanceUID);
        
        if (studyUID == null || seriesUID == null) {
            // フォールバック: 日付ベースの構造
            String date = dataset.getString(Tag.StudyDate, "UNKNOWN");
            return storagePath.resolve(date).resolve(sopInstanceUID + ".dcm");
        }
        
        return storagePath
            .resolve(studyUID)
            .resolve(seriesUID)
            .resolve(sopInstanceUID + ".dcm");
    }
    
    public void setProgressCallback(StoreProgressCallback callback) {
        this.progressCallback = callback;
    }
    
    public interface StoreProgressCallback {
        void onFileStored(Path filePath, Attributes dataset);
    }
}
```

### 3.3 C-FIND SCP（検索サーバー）

**Java実装**:
```java
import org.dcm4che3.net.service.*;
import org.dcm4che3.data.*;
import java.util.List;

public class DicomFindSCP extends BasicCFindSCP {
    private DicomDatabase database;
    
    public DicomFindSCP(DicomDatabase database) {
        this.database = database;
    }
    
    @Override
    protected void find(Association as, Attributes rq, Attributes rsp, 
                       CFINDSCP.Callback callback) throws IOException {
        
        String callingAET = as.getCallingAET();
        String queryLevel = rq.getString(Tag.QueryRetrieveLevel);
        
        if (queryLevel == null) {
            throw new IOException("Missing Query Retrieve Level");
        }
        
        // 検索条件の構築
        SearchCriteria criteria = buildSearchCriteria(rq);
        
        // データベースから検索
        List<Attributes> results;
        switch (queryLevel) {
            case "PATIENT":
                results = database.findPatients(criteria);
                break;
            case "STUDY":
                results = database.findStudies(criteria);
                break;
            case "SERIES":
                results = database.findSeries(criteria);
                break;
            case "IMAGE":
                results = database.findImages(criteria);
                break;
            default:
                throw new IOException("Unsupported Query Retrieve Level: " + queryLevel);
        }
        
        // 結果の送信
        for (Attributes result : results) {
            // 必須フィールドの設定
            result.setString(Tag.QueryRetrieveLevel, VR.CS, queryLevel);
            
            // コールバックで結果を送信
            callback.onCFindResponse(result);
        }
        
        // 完了
        callback.onCFindResponse(null);
    }
    
    private SearchCriteria buildSearchCriteria(Attributes rq) {
        SearchCriteria criteria = new SearchCriteria();
        
        // Patient Level
        if (rq.contains(Tag.PatientID)) {
            criteria.setPatientID(rq.getString(Tag.PatientID));
        }
        if (rq.contains(Tag.PatientName)) {
            criteria.setPatientName(rq.getString(Tag.PatientName));
        }
        if (rq.contains(Tag.PatientBirthDate)) {
            criteria.setPatientBirthDate(rq.getString(Tag.PatientBirthDate));
        }
        
        // Study Level
        if (rq.contains(Tag.StudyInstanceUID)) {
            criteria.setStudyInstanceUID(rq.getString(Tag.StudyInstanceUID));
        }
        if (rq.contains(Tag.StudyDate)) {
            criteria.setStudyDate(rq.getString(Tag.StudyDate));
        }
        if (rq.contains(Tag.StudyDescription)) {
            criteria.setStudyDescription(rq.getString(Tag.StudyDescription));
        }
        if (rq.contains(Tag.AccessionNumber)) {
            criteria.setAccessionNumber(rq.getString(Tag.AccessionNumber));
        }
        
        // Series Level
        if (rq.contains(Tag.SeriesInstanceUID)) {
            criteria.setSeriesInstanceUID(rq.getString(Tag.SeriesInstanceUID));
        }
        if (rq.contains(Tag.SeriesNumber)) {
            criteria.setSeriesNumber(rq.getInt(Tag.SeriesNumber, -1));
        }
        if (rq.contains(Tag.Modality)) {
            criteria.setModality(rq.getString(Tag.Modality));
        }
        
        // Image Level
        if (rq.contains(Tag.SOPInstanceUID)) {
            criteria.setSOPInstanceUID(rq.getString(Tag.SOPInstanceUID));
        }
        
        return criteria;
    }
}
```

### 3.4 C-MOVE SCP（取得サーバー）

**Java実装**:
```java
import org.dcm4che3.net.service.*;
import org.dcm4che3.data.*;
import java.nio.file.Path;
import java.util.List;

public class DicomMoveSCP extends BasicCMoveSCP {
    private DicomDatabase database;
    
    public DicomMoveSCP(DicomDatabase database) {
        this.database = database;
    }
    
    @Override
    protected void move(Association as, Attributes rq, Attributes rsp, 
                       CMOVESCP.Callback callback) throws IOException {
        
        String callingAET = as.getCallingAET();
        String destinationAET = rq.getString(Tag.MoveDestination);
        String queryLevel = rq.getString(Tag.QueryRetrieveLevel);
        
        if (destinationAET == null) {
            throw new IOException("Missing Move Destination");
        }
        
        // 検索条件の構築
        SearchCriteria criteria = buildSearchCriteria(rq);
        
        // データベースからファイルパスを取得
        List<Path> files;
        switch (queryLevel) {
            case "STUDY":
                files = database.getStudyFiles(criteria);
                break;
            case "SERIES":
                files = database.getSeriesFiles(criteria);
                break;
            case "IMAGE":
                files = database.getImageFiles(criteria);
                break;
            default:
                throw new IOException("Unsupported Query Retrieve Level: " + queryLevel);
        }
        
        // ファイルを送信（C-STORE SCUを使用）
        DicomStoreSCU storeSCU = new DicomStoreSCU(
            callingAET, destinationAET, getDestinationHost(destinationAET), 
            getDestinationPort(destinationAET));
        
        int total = files.size();
        int success = 0;
        int failed = 0;
        
        for (Path file : files) {
            try {
                storeSCU.sendFile(file);
                success++;
                callback.onCMoveResponse(file.getFileName().toString(), Status.Pending);
            } catch (Exception e) {
                failed++;
                System.err.println("Failed to send file: " + file + " - " + e.getMessage());
            }
        }
        
        // 完了レスポンス
        rsp.setInt(Tag.Status, VR.US, Status.Success);
        rsp.setInt(Tag.NumberOfCompletedSubOperations, VR.US, success);
        rsp.setInt(Tag.NumberOfFailedSubOperations, VR.US, failed);
        rsp.setInt(Tag.NumberOfRemainingSubOperations, VR.US, 0);
    }
    
    private String getDestinationHost(String destinationAET) {
        // 設定から取得（PACS設定管理を参照）
        // ...
        return "localhost"; // デフォルト
    }
    
    private int getDestinationPort(String destinationAET) {
        // 設定から取得
        // ...
        return 11112; // デフォルト
    }
}
```

## 4. 自動ルーティング

### 4.1 ルーティングルールの定義

**Java実装**:
```java
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class RoutingRule {
    private String name;
    private List<Condition> conditions;
    private RoutingAction action;
    private boolean enabled;
    
    public RoutingRule(String name) {
        this.name = name;
        this.conditions = new ArrayList<>();
        this.enabled = true;
    }
    
    public boolean matches(Attributes dataset) {
        if (!enabled) {
            return false;
        }
        
        for (Condition condition : conditions) {
            if (!condition.matches(dataset)) {
                return false;
            }
        }
        
        return true;
    }
    
    public void execute(Path filePath, Attributes dataset) throws Exception {
        if (matches(dataset)) {
            action.execute(filePath, dataset);
        }
    }
    
    // Getters and setters
    public void addCondition(Condition condition) {
        conditions.add(condition);
    }
    
    public void setAction(RoutingAction action) {
        this.action = action;
    }
}

public interface Condition {
    boolean matches(Attributes dataset);
}

public class TagValueCondition implements Condition {
    private int tag;
    private String expectedValue;
    private Pattern pattern;
    
    public TagValueCondition(int tag, String expectedValue) {
        this.tag = tag;
        this.expectedValue = expectedValue;
        this.pattern = Pattern.compile(expectedValue);
    }
    
    @Override
    public boolean matches(Attributes dataset) {
        String value = dataset.getString(tag);
        if (value == null) {
            return false;
        }
        return pattern.matcher(value).matches();
    }
}

public interface RoutingAction {
    void execute(Path filePath, Attributes dataset) throws Exception;
}

public class CopyToDirectoryAction implements RoutingAction {
    private Path targetDirectory;
    
    public CopyToDirectoryAction(Path targetDirectory) {
        this.targetDirectory = targetDirectory;
    }
    
    @Override
    public void execute(Path filePath, Attributes dataset) throws Exception {
        String studyUID = dataset.getString(Tag.StudyInstanceUID);
        Path targetPath = targetDirectory.resolve(studyUID).resolve(filePath.getFileName());
        Files.createDirectories(targetPath.getParent());
        Files.copy(filePath, targetPath);
    }
}

public class SendToPacsAction implements RoutingAction {
    private String destinationAET;
    private String hostname;
    private int port;
    
    public SendToPacsAction(String destinationAET, String hostname, int port) {
        this.destinationAET = destinationAET;
        this.hostname = hostname;
        this.port = port;
    }
    
    @Override
    public void execute(Path filePath, Attributes dataset) throws Exception {
        DicomStoreSCU storeSCU = new DicomStoreSCU("JJDICOMVIEWER", destinationAET, hostname, port);
        storeSCU.sendFile(filePath);
    }
}
```

### 4.2 ルーティングマネージャー

**Java実装**:
```java
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RoutingManager {
    private List<RoutingRule> rules;
    private DicomDatabase database;
    
    public RoutingManager(DicomDatabase database) {
        this.rules = new CopyOnWriteArrayList<>();
        this.database = database;
    }
    
    public void addRule(RoutingRule rule) {
        rules.add(rule);
    }
    
    public void removeRule(RoutingRule rule) {
        rules.remove(rule);
    }
    
    public void processReceivedFile(Path filePath, Attributes dataset) {
        for (RoutingRule rule : rules) {
            try {
                rule.execute(filePath, dataset);
            } catch (Exception e) {
                System.err.println("Routing rule execution failed: " + rule.getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public void loadRulesFromConfig(Path configPath) {
        // 設定ファイルからルールを読み込む
        // JSONまたはYAML形式
        // ...
    }
    
    public void saveRulesToConfig(Path configPath) {
        // ルールを設定ファイルに保存
        // ...
    }
}
```

## 5. スケジュール機能

### 5.1 スケジュールタスクの定義

**Java実装**:
```java
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ScheduledTask {
    private String name;
    private TaskType type;
    private Schedule schedule;
    private SearchCriteria criteria;
    private String destinationAET;
    private boolean enabled;
    
    public enum TaskType {
        QUERY,      // 定期的な検索
        RETRIEVE,   // 定期的な取得
        EXPORT      // 定期的なエクスポート
    }
    
    public enum Schedule {
        DAILY,      // 毎日
        WEEKLY,     // 毎週
        MONTHLY,    // 毎月
        CUSTOM      // カスタム
    }
    
    // Getters and setters
}

public class ScheduleManager {
    private ScheduledExecutorService scheduler;
    private List<ScheduledTask> tasks;
    private DicomDatabase database;
    
    public ScheduleManager(DicomDatabase database) {
        this.scheduler = Executors.newScheduledThreadPool(5);
        this.tasks = new ArrayList<>();
        this.database = database;
    }
    
    public void addTask(ScheduledTask task) {
        tasks.add(task);
        scheduleTask(task);
    }
    
    private void scheduleTask(ScheduledTask task) {
        if (!task.isEnabled()) {
            return;
        }
        
        LocalTime executionTime = task.getExecutionTime();
        long initialDelay = calculateInitialDelay(executionTime);
        long period = calculatePeriod(task.getSchedule());
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                executeTask(task);
            } catch (Exception e) {
                System.err.println("Scheduled task execution failed: " + task.getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }, initialDelay, period, TimeUnit.SECONDS);
    }
    
    private void executeTask(ScheduledTask task) throws Exception {
        switch (task.getType()) {
            case QUERY:
                executeQueryTask(task);
                break;
            case RETRIEVE:
                executeRetrieveTask(task);
                break;
            case EXPORT:
                executeExportTask(task);
                break;
        }
    }
    
    private void executeQueryTask(ScheduledTask task) {
        List<Attributes> results = database.findStudies(task.getCriteria());
        // 結果の処理（通知、ログ等）
        System.out.println("Query task '" + task.getName() + "' found " + results.size() + " studies");
    }
    
    private void executeRetrieveTask(ScheduledTask task) throws Exception {
        List<Path> files = database.getStudyFiles(task.getCriteria());
        DicomStoreSCU storeSCU = new DicomStoreSCU(
            "JJDICOMVIEWER", task.getDestinationAET(), 
            getHostname(task.getDestinationAET()), 
            getPort(task.getDestinationAET()));
        
        for (Path file : files) {
            storeSCU.sendFile(file);
        }
    }
    
    private long calculateInitialDelay(LocalTime executionTime) {
        LocalTime now = LocalTime.now();
        if (executionTime.isAfter(now)) {
            return java.time.Duration.between(now, executionTime).getSeconds();
        } else {
            // 明日の実行時間まで
            return java.time.Duration.between(now, executionTime).plusHours(24).getSeconds();
        }
    }
    
    private long calculatePeriod(ScheduledTask.Schedule schedule) {
        switch (schedule) {
            case DAILY:
                return 24 * 60 * 60; // 24時間
            case WEEKLY:
                return 7 * 24 * 60 * 60; // 7日
            case MONTHLY:
                return 30 * 24 * 60 * 60; // 30日（簡易実装）
            default:
                return 24 * 60 * 60;
        }
    }
}
```

## 6. PACS設定管理

### 6.1 PACS設定クラス

**Java実装**:
```java
import java.util.List;
import java.util.ArrayList;

public class PacsConfiguration {
    private String aeTitle;
    private int port;
    private Path storagePath;
    private boolean autoStart;
    private int maxAssociations;
    private int timeout;
    
    private List<RemotePacsNode> remoteNodes;
    private List<RoutingRule> routingRules;
    private List<ScheduledTask> scheduledTasks;
    
    public PacsConfiguration() {
        this.remoteNodes = new ArrayList<>();
        this.routingRules = new ArrayList<>();
        this.scheduledTasks = new ArrayList<>();
    }
    
    // Getters and setters
}

public class RemotePacsNode {
    private String aeTitle;
    private String hostname;
    private int port;
    private String description;
    private boolean enabled;
    
    // Getters and setters
}
```

### 6.2 設定UI

**Java実装**:
```java
import javax.swing.*;
import java.awt.*;

public class PacsConfigurationDialog extends JDialog {
    private PacsConfiguration config;
    
    private JTextField aeTitleField;
    private JSpinner portSpinner;
    private JTextField storagePathField;
    private JCheckBox autoStartCheckBox;
    
    private JTable remoteNodesTable;
    private JTable routingRulesTable;
    private JTable scheduledTasksTable;
    
    public PacsConfigurationDialog(Frame parent, PacsConfiguration config) {
        super(parent, "PACS Configuration", true);
        this.config = config;
        
        createUI();
        loadConfiguration();
    }
    
    private void createUI() {
        setLayout(new BorderLayout());
        
        // タブパネル
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // 基本設定タブ
        tabbedPane.addTab("Basic", createBasicSettingsPanel());
        
        // リモートノードタブ
        tabbedPane.addTab("Remote Nodes", createRemoteNodesPanel());
        
        // ルーティングルールタブ
        tabbedPane.addTab("Routing Rules", createRoutingRulesPanel());
        
        // スケジュールタブ
        tabbedPane.addTab("Schedule", createSchedulePanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // ボタンパネル
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveConfiguration());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        pack();
    }
    
    private JPanel createBasicSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // AE Title
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("AE Title:"), gbc);
        gbc.gridx = 1;
        aeTitleField = new JTextField(20);
        panel.add(aeTitleField, gbc);
        
        // Port
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        portSpinner = new JSpinner(new SpinnerNumberModel(11112, 1, 65535, 1));
        panel.add(portSpinner, gbc);
        
        // Storage Path
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Storage Path:"), gbc);
        gbc.gridx = 1;
        storagePathField = new JTextField(30);
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> browseStoragePath());
        JPanel pathPanel = new JPanel(new BorderLayout());
        pathPanel.add(storagePathField, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);
        panel.add(pathPanel, gbc);
        
        // Auto Start
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        autoStartCheckBox = new JCheckBox("Auto Start Server");
        panel.add(autoStartCheckBox, gbc);
        
        return panel;
    }
    
    private void loadConfiguration() {
        aeTitleField.setText(config.getAeTitle());
        portSpinner.setValue(config.getPort());
        storagePathField.setText(config.getStoragePath().toString());
        autoStartCheckBox.setSelected(config.isAutoStart());
    }
    
    private void saveConfiguration() {
        config.setAeTitle(aeTitleField.getText());
        config.setPort((Integer) portSpinner.getValue());
        config.setStoragePath(Paths.get(storagePathField.getText()));
        config.setAutoStart(autoStartCheckBox.isSelected());
        
        // 設定をファイルに保存
        // ...
        
        dispose();
    }
}
```

## 7. ログ管理

### 7.1 DICOM通信ログ

**Java実装**:
```java
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class DicomLogEntry {
    private LocalDateTime timestamp;
    private String callingAET;
    private String calledAET;
    private String operation; // C-STORE, C-FIND, C-MOVE
    private String status;
    private String message;
    private Path filePath;
    
    // Getters and setters
}

public class DicomLogManager {
    private List<DicomLogEntry> logs;
    private int maxLogEntries;
    
    public DicomLogManager(int maxLogEntries) {
        this.logs = new ArrayList<>();
        this.maxLogEntries = maxLogEntries;
    }
    
    public void logOperation(String callingAET, String calledAET, 
                            String operation, String status, String message) {
        DicomLogEntry entry = new DicomLogEntry();
        entry.setTimestamp(LocalDateTime.now());
        entry.setCallingAET(callingAET);
        entry.setCalledAET(calledAET);
        entry.setOperation(operation);
        entry.setStatus(status);
        entry.setMessage(message);
        
        logs.add(entry);
        
        // 最大件数を超えたら古いログを削除
        if (logs.size() > maxLogEntries) {
            logs.remove(0);
        }
    }
    
    public List<DicomLogEntry> getLogs(LocalDateTime from, LocalDateTime to) {
        return logs.stream()
            .filter(log -> !log.getTimestamp().isBefore(from) && !log.getTimestamp().isAfter(to))
            .collect(Collectors.toList());
    }
    
    public void exportLogs(Path filePath) throws IOException {
        // ログをファイルにエクスポート（CSV形式等）
        // ...
    }
}
```

## 8. 実装の考慮事項

### 8.1 セキュリティ

- **TLS/SSL**: 暗号化通信のサポート
- **認証**: 接続元の認証
- **アクセス制御**: 特定のAETからの接続のみ許可

### 8.2 パフォーマンス

- **マルチスレッド**: 複数のアソシエーションを同時処理
- **接続プール**: リモートPACSへの接続の再利用
- **非同期処理**: 大きなファイルの処理を非同期化

### 8.3 エラーハンドリング

- **リトライ機能**: 失敗した送信の自動リトライ
- **エラーログ**: 詳細なエラーログの記録
- **通知機能**: 重要なエラーの通知

## 9. 実装順序

1. **DicomServerクラス**: 基本的なサーバー機能
2. **DicomStoreSCPクラス**: 画像受信機能
3. **DicomFindSCPクラス**: 検索サーバー機能
4. **DicomMoveSCPクラス**: 取得サーバー機能
5. **RoutingManagerクラス**: 自動ルーティング機能
6. **ScheduleManagerクラス**: スケジュール機能
7. **PacsConfigurationDialogクラス**: 設定UI

## 10. 参考資料

- HOROSソースコード: `DCMTKQueryRetrieveSCP.h/m`, `OsiriXSCPDataHandler.h/m`
- DICOM標準: PS3.4 (Service Class Specifications)
- dcm4cheドキュメント: https://www.dcm4che.org/

