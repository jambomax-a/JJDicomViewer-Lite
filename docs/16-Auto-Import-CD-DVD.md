# CD/DVD自動インポート・作成機能 - Java移植ドキュメント

## 1. 概要

このドキュメントは、HOROSのCD/DVD自動読み込み機能とお渡し用CD/DVD作成機能を解析し、Javaでの実装方法をまとめたものです。

この機能は、以下の用途で使用されます：
- **紹介症例のCD/DVD**: 他院からの紹介症例のDICOM画像を自動インポート
- **患者への提供CD/DVD**: 患者に渡すCD/DVDから画像を自動インポート
- **お渡し用CD/DVD作成**: 患者への提供用CD/DVDの作成
- **Vet-Systemでの使用**: Vet-Systemでも同じ機能を使用可能

## 1.1 HOROS実装の解析

HOROSでは以下の機能が実装されています：

1. **CD/DVD自動読み込み機能**
   - `BrowserController+Sources.m`: ドライブマウント通知の監視
   - `DicomDatabase+Scan.mm`: CD/DVD内のDICOMファイルのスキャン
   - `DiscMountedAskTheUserDialogController`: ユーザー確認ダイアログ
   - DICOMDIRファイルのサポート

2. **お渡し用CD/DVD作成機能**
   - `BurnerWindowController`: CD/DVD書き込みの管理
   - `DiscBurningOptions`: 書き込みオプション（匿名化、圧縮、ビューワー同梱など）
   - `DICOMExport`: DICOMファイルのエクスポート

## 2. 機能要件

### 2.1 基本機能

1. **CD/DVDドライブの自動検出**
   - ドライブの挿入を監視
   - プラットフォーム対応（Windows、macOS、Linux）

2. **DICOMファイルの自動スキャン**
   - CD/DVD内のDICOMファイルを再帰的に検索
   - 非DICOMファイルの除外

3. **DICOMファイルの解析とインポート**
   - DICOMファイルの検証
   - メタデータの抽出
   - データベースへの登録

4. **進捗表示とエラーハンドリング**
   - インポート進捗の表示
   - エラーファイルの記録

### 2.2 Vet-Systemとの連携

- **共通ライブラリ**: Vet-Systemでも使用可能な共通モジュール
- **API提供**: Vet-Systemから呼び出し可能なAPI
- **設定共有**: インポート先フォルダなどの設定を共有

## 3. プラットフォーム別の実装

### 3.1 Windows

**Java実装**:
```java
import java.nio.file.*;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class WindowsDriveMonitor {
    private WatchService watchService;
    private Path watchPath;
    private DriveInsertCallback callback;
    
    public interface DriveInsertCallback {
        void onDriveInserted(String driveLetter);
        void onDriveRemoved(String driveLetter);
    }
    
    public WindowsDriveMonitor(DriveInsertCallback callback) throws IOException {
        this.callback = callback;
        this.watchService = FileSystems.getDefault().newWatchService();
        
        // Windowsのドライブレターを監視
        // 注意: Windowsでは全ドライブを直接監視できないため、
        // 定期的にポーリングする方法を使用
    }
    
    public void startMonitoring() {
        Thread monitorThread = new Thread(() -> {
            Set<String> previousDrives = getAvailableDrives();
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(2000); // 2秒ごとにチェック
                    Set<String> currentDrives = getAvailableDrives();
                    
                    // 新しく挿入されたドライブ
                    for (String drive : currentDrives) {
                        if (!previousDrives.contains(drive)) {
                            callback.onDriveInserted(drive);
                        }
                    }
                    
                    // 取り外されたドライブ
                    for (String drive : previousDrives) {
                        if (!currentDrives.contains(drive)) {
                            callback.onDriveRemoved(drive);
                        }
                    }
                    
                    previousDrives = currentDrives;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
    
    private Set<String> getAvailableDrives() {
        Set<String> drives = new HashSet<>();
        File[] roots = File.listRoots();
        for (File root : roots) {
            String path = root.getAbsolutePath();
            if (path.matches("[A-Z]:\\\\")) {
                // CD/DVDドライブかどうかを判定
                if (isCDDVDDrive(path)) {
                    drives.add(path);
                }
            }
        }
        return drives;
    }
    
    private boolean isCDDVDDrive(String drivePath) {
        try {
            // Windows APIを使用してドライブタイプを判定
            // JNA (Java Native Access) を使用するか、
            // ファイルシステムの特性を確認
            Path path = Paths.get(drivePath);
            FileStore store = Files.getFileStore(path);
            String type = store.type();
            
            // CD/DVDドライブの判定
            // Windowsでは "CDFS" または "UDF" ファイルシステム
            return "CDFS".equals(type) || "UDF".equals(type) || 
                   store.getAttribute("volume:isRemovable").equals(true);
        } catch (IOException e) {
            return false;
        }
    }
}
```

### 3.2 macOS

**Java実装**:
```java
import java.nio.file.*;
import java.io.IOException;

public class MacOSDriveMonitor {
    private WatchService watchService;
    private Path watchPath;
    private DriveInsertCallback callback;
    
    public MacOSDriveMonitor(DriveInsertCallback callback) throws IOException {
        this.callback = callback;
        this.watchService = FileSystems.getDefault().newWatchService();
        
        // macOSの /Volumes ディレクトリを監視
        this.watchPath = Paths.get("/Volumes");
    }
    
    public void startMonitoring() throws IOException {
        watchPath.register(watchService, 
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE);
        
        Thread monitorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    WatchKey key = watchService.take();
                    
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path path = (Path) event.context();
                        Path fullPath = watchPath.resolve(path);
                        
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            // ドライブがマウントされた
                            if (isCDDVDDrive(fullPath)) {
                                callback.onDriveInserted(fullPath.toString());
                            }
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            // ドライブがアンマウントされた
                            callback.onDriveRemoved(fullPath.toString());
                        }
                    }
                    
                    key.reset();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
    
    private boolean isCDDVDDrive(Path path) {
        try {
            FileStore store = Files.getFileStore(path);
            String type = store.type();
            // macOSでは "cddafs" または "udf" ファイルシステム
            return "cddafs".equals(type) || "udf".equals(type);
        } catch (IOException e) {
            return false;
        }
    }
}
```

### 3.3 Linux

**Java実装**:
```java
import java.nio.file.*;
import java.io.IOException;

public class LinuxDriveMonitor {
    private WatchService watchService;
    private Path watchPath;
    private DriveInsertCallback callback;
    
    public LinuxDriveMonitor(DriveInsertCallback callback) throws IOException {
        this.callback = callback;
        this.watchService = FileSystems.getDefault().newWatchService();
        
        // Linuxの /media または /mnt ディレクトリを監視
        Path mediaPath = Paths.get("/media");
        Path mntPath = Paths.get("/mnt");
        
        if (Files.exists(mediaPath)) {
            this.watchPath = mediaPath;
        } else if (Files.exists(mntPath)) {
            this.watchPath = mntPath;
        } else {
            throw new IOException("Cannot find media mount point");
        }
    }
    
    public void startMonitoring() throws IOException {
        // Linuxでは /media または /mnt 配下のディレクトリを監視
        // 実装はmacOSと同様
        // ...
    }
    
    private boolean isCDDVDDrive(Path path) {
        try {
            FileStore store = Files.getFileStore(path);
            String type = store.type();
            // Linuxでは "iso9660" または "udf" ファイルシステム
            return "iso9660".equals(type) || "udf".equals(type);
        } catch (IOException e) {
            return false;
        }
    }
}
```

## 4. DICOMファイルの自動スキャン

### 4.1 ファイルスキャンエンジン

**Java実装**:
```java
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DicomFileScanner {
    private ExecutorService executorService;
    private ProgressCallback progressCallback;
    
    public interface ProgressCallback {
        void onFileFound(Path file);
        void onScanComplete(int totalFiles);
        void onError(Path file, Exception e);
    }
    
    public DicomFileScanner() {
        this.executorService = Executors.newFixedThreadPool(4);
    }
    
    /**
     * CD/DVD内のDICOMファイルを再帰的にスキャン
     */
    public Future<List<Path>> scanDicomFiles(Path rootPath, ProgressCallback callback) {
        this.progressCallback = callback;
        
        return executorService.submit(() -> {
            List<Path> dicomFiles = new ArrayList<>();
            
            try {
                Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (isDicomFile(file)) {
                            dicomFiles.add(file);
                            if (progressCallback != null) {
                                progressCallback.onFileFound(file);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                    
                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        if (progressCallback != null) {
                            progressCallback.onError(file, exc);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
                
                if (progressCallback != null) {
                    progressCallback.onScanComplete(dicomFiles.size());
                }
                
            } catch (IOException e) {
                throw new RuntimeException("Failed to scan directory", e);
            }
            
            return dicomFiles;
        });
    }
    
    private boolean isDicomFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        
        // 拡張子チェック
        if (fileName.endsWith(".dcm") || fileName.endsWith(".dicom")) {
            return true;
        }
        
        // 拡張子がない場合、ファイルヘッダーをチェック
        try {
            byte[] header = new byte[132];
            try (java.io.InputStream is = Files.newInputStream(file)) {
                int bytesRead = is.read(header);
                if (bytesRead >= 132) {
                    // DICOMファイルは128バイトのプリアンブル + "DICM" (4バイト)
                    String dicmTag = new String(header, 128, 4);
                    return "DICM".equals(dicmTag);
                }
            }
        } catch (IOException e) {
            // エラー時は拡張子のみで判定
        }
        
        return false;
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}
```

## 5. DICOMファイルの自動インポート

### 5.1 インポートマネージャー

**Java実装**:
```java
import org.dcm4che3.data.*;
import org.dcm4che3.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;

public class DicomAutoImporter {
    private DicomDatabase database;
    private Path importDestination;
    private ExecutorService importExecutor;
    private ImportProgressCallback progressCallback;
    
    public interface ImportProgressCallback {
        void onImportStart(int totalFiles);
        void onFileImported(Path file, Attributes dataset, int current, int total);
        void onImportComplete(int successCount, int failureCount);
        void onError(Path file, Exception e);
    }
    
    public DicomAutoImporter(DicomDatabase database, Path importDestination) {
        this.database = database;
        this.importDestination = importDestination;
        this.importExecutor = Executors.newFixedThreadPool(2);
    }
    
    /**
     * DICOMファイルを自動インポート
     */
    public CompletableFuture<ImportResult> importFiles(List<Path> dicomFiles, 
                                                       ImportProgressCallback callback) {
        this.progressCallback = callback;
        
        return CompletableFuture.supplyAsync(() -> {
            int successCount = 0;
            int failureCount = 0;
            int total = dicomFiles.size();
            
            if (progressCallback != null) {
                progressCallback.onImportStart(total);
            }
            
            for (int i = 0; i < dicomFiles.size(); i++) {
                Path file = dicomFiles.get(i);
                
                try {
                    // DICOMファイルの読み込み
                    Attributes dataset = readDicomFile(file);
                    
                    // ファイルのコピー先を決定
                    Path destinationPath = determineDestinationPath(dataset, file);
                    
                    // ファイルをコピー
                    Files.createDirectories(destinationPath.getParent());
                    Files.copy(file, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    
                    // データベースに登録
                    database.importDicomFile(destinationPath, dataset);
                    
                    successCount++;
                    
                    if (progressCallback != null) {
                        progressCallback.onFileImported(file, dataset, i + 1, total);
                    }
                    
                } catch (Exception e) {
                    failureCount++;
                    
                    if (progressCallback != null) {
                        progressCallback.onError(file, e);
                    }
                    
                    System.err.println("Failed to import file: " + file + " - " + e.getMessage());
                }
            }
            
            if (progressCallback != null) {
                progressCallback.onImportComplete(successCount, failureCount);
            }
            
            return new ImportResult(successCount, failureCount);
        }, importExecutor);
    }
    
    private Attributes readDicomFile(Path file) throws Exception {
        try (DicomInputStream dis = new DicomInputStream(Files.newInputStream(file))) {
            Attributes dataset = dis.readDataset(-1, -1);
            return dataset;
        }
    }
    
    private Path determineDestinationPath(Attributes dataset, Path sourceFile) {
        // 階層構造: StudyInstanceUID/SeriesInstanceUID/SOPInstanceUID.dcm
        String studyUID = dataset.getString(Tag.StudyInstanceUID);
        String seriesUID = dataset.getString(Tag.SeriesInstanceUID);
        String sopInstanceUID = dataset.getString(Tag.SOPInstanceUID);
        
        if (studyUID == null || seriesUID == null || sopInstanceUID == null) {
            // フォールバック: 日付ベースの構造
            String date = dataset.getString(Tag.StudyDate, "UNKNOWN");
            String fileName = sourceFile.getFileName().toString();
            return importDestination.resolve(date).resolve(fileName);
        }
        
        return importDestination
            .resolve(studyUID)
            .resolve(seriesUID)
            .resolve(sopInstanceUID + ".dcm");
    }
    
    public static class ImportResult {
        private int successCount;
        private int failureCount;
        
        public ImportResult(int successCount, int failureCount) {
            this.successCount = successCount;
            this.failureCount = failureCount;
        }
        
        // Getters
    }
}
```

## 6. 統合実装

### 6.1 CD/DVD自動インポートサービス

**Java実装**:
```java
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CDDVDAutoImportService {
    private DriveMonitor driveMonitor;
    private DicomFileScanner fileScanner;
    private DicomAutoImporter autoImporter;
    private DicomDatabase database;
    private Path importDestination;
    private boolean enabled = false;
    
    public CDDVDAutoImportService(DicomDatabase database, Path importDestination) {
        this.database = database;
        this.importDestination = importDestination;
        this.fileScanner = new DicomFileScanner();
        this.autoImporter = new DicomAutoImporter(database, importDestination);
        
        // プラットフォーム別のドライブモニターを初期化
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            this.driveMonitor = new WindowsDriveMonitor(this::onDriveInserted);
        } else if (os.contains("mac")) {
            this.driveMonitor = new MacOSDriveMonitor(this::onDriveInserted);
        } else if (os.contains("linux")) {
            this.driveMonitor = new LinuxDriveMonitor(this::onDriveInserted);
        }
    }
    
    public void start() throws IOException {
        if (enabled) {
            return;
        }
        
        driveMonitor.startMonitoring();
        enabled = true;
        
        System.out.println("CD/DVD自動インポートサービスを開始しました");
    }
    
    public void stop() {
        if (!enabled) {
            return;
        }
        
        if (driveMonitor instanceof AutoCloseable) {
            try {
                ((AutoCloseable) driveMonitor).close();
            } catch (Exception e) {
                // エラー処理
            }
        }
        
        fileScanner.shutdown();
        enabled = false;
        
        System.out.println("CD/DVD自動インポートサービスを停止しました");
    }
    
    private void onDriveInserted(String drivePath) {
        System.out.println("CD/DVDが検出されました: " + drivePath);
        
        Path driveRoot = Paths.get(drivePath);
        
        // DICOMファイルをスキャン
        CompletableFuture<List<Path>> scanFuture = fileScanner.scanDicomFiles(
            driveRoot,
            new DicomFileScanner.ProgressCallback() {
                @Override
                public void onFileFound(Path file) {
                    System.out.println("DICOMファイルを発見: " + file);
                }
                
                @Override
                public void onScanComplete(int totalFiles) {
                    System.out.println("スキャン完了: " + totalFiles + " ファイル");
                }
                
                @Override
                public void onError(Path file, Exception e) {
                    System.err.println("スキャンエラー: " + file + " - " + e.getMessage());
                }
            }
        );
        
        // スキャン完了後にインポート
        scanFuture.thenAccept(files -> {
            if (files.isEmpty()) {
                System.out.println("DICOMファイルが見つかりませんでした");
                return;
            }
            
            System.out.println("インポートを開始します: " + files.size() + " ファイル");
            
            autoImporter.importFiles(files, new DicomAutoImporter.ImportProgressCallback() {
                @Override
                public void onImportStart(int totalFiles) {
                    System.out.println("インポート開始: " + totalFiles + " ファイル");
                }
                
                @Override
                public void onFileImported(Path file, Attributes dataset, int current, int total) {
                    System.out.println(String.format("インポート中: %d/%d - %s", 
                        current, total, file.getFileName()));
                }
                
                @Override
                public void onImportComplete(int successCount, int failureCount) {
                    System.out.println(String.format("インポート完了: 成功 %d, 失敗 %d", 
                        successCount, failureCount));
                }
                
                @Override
                public void onError(Path file, Exception e) {
                    System.err.println("インポートエラー: " + file + " - " + e.getMessage());
                }
            });
        });
    }
}
```

## 7. Vet-Systemとの連携

### 7.1 共通ライブラリの提供

**Java実装**:
```java
/**
 * Vet-Systemからも使用可能な共通DICOMインポートライブラリ
 */
public class DicomImportLibrary {
    /**
     * 指定されたパスからDICOMファイルをスキャンしてインポート
     * Vet-Systemから呼び出し可能
     */
    public static ImportResult importFromPath(Path sourcePath, 
                                              DicomDatabase database,
                                              Path importDestination,
                                              ImportProgressCallback callback) {
        DicomFileScanner scanner = new DicomFileScanner();
        DicomAutoImporter importer = new DicomAutoImporter(database, importDestination);
        
        // スキャン
        List<Path> files = scanner.scanDicomFiles(sourcePath, null).join();
        
        // インポート
        return importer.importFiles(files, callback).join();
    }
    
    /**
     * CD/DVDドライブを監視して自動インポート
     */
    public static CDDVDAutoImportService createAutoImportService(
            DicomDatabase database, Path importDestination) {
        return new CDDVDAutoImportService(database, importDestination);
    }
}
```

### 7.2 Vet-System側の実装

**Python実装例** (Vet-System側):
```python
# Vet-System側での使用例
import subprocess
import json
from pathlib import Path

def import_dicom_from_cd_dvd(cd_dvd_path: str, import_destination: str):
    """
    CD/DVDからDICOMファイルをインポート
    JJDICOMViewerの共通ライブラリを使用
    """
    # JJDICOMViewerのJARファイルを実行
    jar_path = "/path/to/JJDICOMViewer/dicom-import-library.jar"
    
    result = subprocess.run(
        [
            "java", "-jar", jar_path,
            "--source", cd_dvd_path,
            "--destination", import_destination,
            "--format", "json"
        ],
        capture_output=True,
        text=True
    )
    
    if result.returncode == 0:
        import_result = json.loads(result.stdout)
        return {
            "success": True,
            "success_count": import_result["successCount"],
            "failure_count": import_result["failureCount"]
        }
    else:
        return {
            "success": False,
            "error": result.stderr
        }
```

## 8. UI実装

### 8.1 自動インポート設定ダイアログ

**Java実装**:
```java
import javax.swing.*;
import java.awt.*;

public class AutoImportSettingsDialog extends JDialog {
    private JCheckBox enableAutoImportCheckBox;
    private JTextField importDestinationField;
    private JButton browseButton;
    private JCheckBox showNotificationCheckBox;
    
    public AutoImportSettingsDialog(Frame parent) {
        super(parent, "CD/DVD自動インポート設定", true);
        
        createUI();
        loadSettings();
    }
    
    private void createUI() {
        setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 自動インポート有効化
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        enableAutoImportCheckBox = new JCheckBox("CD/DVD自動インポートを有効にする");
        mainPanel.add(enableAutoImportCheckBox, gbc);
        
        // インポート先フォルダ
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        mainPanel.add(new JLabel("インポート先フォルダ:"), gbc);
        
        gbc.gridx = 1;
        JPanel pathPanel = new JPanel(new BorderLayout());
        importDestinationField = new JTextField(30);
        browseButton = new JButton("参照...");
        browseButton.addActionListener(e -> browseImportDestination());
        pathPanel.add(importDestinationField, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);
        mainPanel.add(pathPanel, gbc);
        
        // 通知表示
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        showNotificationCheckBox = new JCheckBox("インポート完了時に通知を表示");
        mainPanel.add(showNotificationCheckBox, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // ボタンパネル
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(e -> saveSettings());
        JButton cancelButton = new JButton("キャンセル");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        pack();
    }
    
    private void browseImportDestination() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            importDestinationField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void loadSettings() {
        // 設定を読み込む
        // ...
    }
    
    private void saveSettings() {
        // 設定を保存
        // ...
        dispose();
    }
}
```

### 8.2 インポート進捗ダイアログ

**Java実装**:
```java
import javax.swing.*;
import java.awt.*;

public class ImportProgressDialog extends JDialog {
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JTextArea logArea;
    private int totalFiles;
    private int currentFile;
    
    public ImportProgressDialog(Frame parent, int totalFiles) {
        super(parent, "DICOMインポート中", false);
        this.totalFiles = totalFiles;
        this.currentFile = 0;
        
        createUI();
    }
    
    private void createUI() {
        setLayout(new BorderLayout());
        
        // ステータスラベル
        statusLabel = new JLabel("インポートを開始しています...");
        add(statusLabel, BorderLayout.NORTH);
        
        // プログレスバー
        progressBar = new JProgressBar(0, totalFiles);
        progressBar.setStringPainted(true);
        add(progressBar, BorderLayout.CENTER);
        
        // ログエリア
        logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(getParent());
    }
    
    public void updateProgress(int current, int total, String fileName) {
        SwingUtilities.invokeLater(() -> {
            currentFile = current;
            progressBar.setValue(current);
            progressBar.setString(String.format("%d / %d", current, total));
            statusLabel.setText("インポート中: " + fileName);
            logArea.append(String.format("[%d/%d] %s\n", current, total, fileName));
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    public void complete(int successCount, int failureCount) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(String.format("インポート完了: 成功 %d, 失敗 %d", 
                successCount, failureCount));
            progressBar.setValue(totalFiles);
            logArea.append(String.format("\n完了: 成功 %d, 失敗 %d\n", 
                successCount, failureCount));
        });
    }
}
```

## 9. 実装の考慮事項

### 9.1 パフォーマンス

- **非同期処理**: スキャンとインポートを非同期で実行
- **マルチスレッド**: 複数ファイルの並列処理
- **進捗表示**: UIをブロックしない進捗表示

### 9.2 エラーハンドリング

- **ファイルアクセスエラー**: 読み取り専用メディアの処理
- **破損ファイル**: DICOMファイルの検証とエラー記録
- **重複ファイル**: 既にインポート済みのファイルの検出

### 9.3 セキュリティ

- **ファイル検証**: DICOMファイルの検証
- **パス検証**: パストラバーサル攻撃の防止
- **権限チェック**: ファイルアクセス権限の確認

## 10. HOROSのCD/DVD自動読み込み機能の解析

### 10.1 ドライブマウント通知の監視

**HOROS実装** (`BrowserController+Sources.m`):
```objective-c
- (void)_observeVolumeNotification:(NSNotification*)notification
{
    if ([notification.name isEqualToString:NSWorkspaceDidMountNotification])
    {
        [self _analyzeVolumeAtPath:[[notification.userInfo objectForKey: NSWorkspaceVolumeURLKey] path]];
    }
}

- (void)_analyzeVolumeAtPath:(NSString*)path
{
    // ボリュームを分析
    // DICOMファイルの数をカウント
    // ユーザーに確認ダイアログを表示
}
```

**Java実装での対応**:
```java
import java.nio.file.*;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;

public class MacOSDriveMonitor {
    private WatchService watchService;
    private Path watchPath;
    
    public void startMonitoring() throws IOException {
        // macOSの /Volumes ディレクトリを監視
        watchPath = Paths.get("/Volumes");
        watchService = FileSystems.getDefault().newWatchService();
        
        watchPath.register(watchService, 
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE);
        
        // 監視スレッド
        Thread monitorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    WatchKey key = watchService.take();
                    
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path path = (Path) event.context();
                        Path fullPath = watchPath.resolve(path);
                        
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            analyzeVolume(fullPath);
                        }
                    }
                    
                    key.reset();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
    
    private void analyzeVolume(Path volumePath) {
        // DICOMファイルの数をカウント
        int dicomFileCount = countDicomFiles(volumePath);
        
        if (dicomFileCount > 0) {
            // ユーザーに確認ダイアログを表示
            SwingUtilities.invokeLater(() -> {
                showDiscMountedDialog(volumePath, dicomFileCount);
            });
        }
    }
}
```

### 10.2 DICOMDIRファイルのサポート

**HOROS実装** (`DicomDatabase+Scan.mm`):
```objective-c
// DICOMDIRファイルを探す
NSString* dicomdirPath = [[self class] _findDicomdirIn:allpaths];
if (dicomdirPath) {
    dicomImages = [self scanDicomdirAt:dicomdirPath withPaths:allpaths pathsToScanAnyway:pathsToScanAnyway];
}
```

**Java実装での対応**:
```java
import org.dcm4che3.data.*;
import org.dcm4che3.io.*;
import java.nio.file.*;

public class DicomDirParser {
    /**
     * DICOMDIRファイルを解析してDICOMファイルのリストを取得
     */
    public List<DicomDirEntry> parseDicomDir(Path dicomDirPath) throws Exception {
        List<DicomDirEntry> entries = new ArrayList<>();
        
        try (DicomInputStream dis = new DicomInputStream(Files.newInputStream(dicomDirPath))) {
            Attributes dataset = dis.readDataset(-1, -1);
            
            // DICOMDIRの構造を解析
            // Patient Record → Study Record → Series Record → Image Record
            // ...
        }
        
        return entries;
    }
    
    public static class DicomDirEntry {
        private String fileId;
        private String studyInstanceUID;
        private String seriesInstanceUID;
        private String sopInstanceUID;
        private String patientName;
        private String studyDate;
        
        // Getters and setters
    }
}
```

### 10.3 ユーザー確認ダイアログ

**HOROS実装** (`DiscMountedAskTheUserDialogController.h`):
```objective-c
@interface DiscMountedAskTheUserDialogController : NSWindowController {
    NSString* _mountedPath;
    NSInteger _filesCount;
    NSInteger _choice;  // 0: コピー, 1: ブラウズ, 2: キャンセル
}
```

**Java実装での対応**:
```java
import javax.swing.*;

public class DiscMountedDialog extends JDialog {
    public enum UserChoice {
        COPY,      // データベースにコピー
        BROWSE,    // ブラウズのみ（コピーしない）
        CANCEL     // キャンセル
    }
    
    private UserChoice choice = UserChoice.CANCEL;
    
    public DiscMountedDialog(Frame parent, Path mountedPath, int fileCount) {
        super(parent, "CD/DVDが検出されました", true);
        
        setLayout(new BorderLayout());
        
        JLabel messageLabel = new JLabel(
            String.format("<html>CD/DVDが検出されました。<br>%d個のDICOMファイルが見つかりました。<br>どうしますか？</html>", 
                fileCount));
        add(messageLabel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton copyButton = new JButton("データベースにコピー");
        copyButton.addActionListener(e -> {
            choice = UserChoice.COPY;
            dispose();
        });
        
        JButton browseButton = new JButton("ブラウズのみ");
        browseButton.addActionListener(e -> {
            choice = UserChoice.BROWSE;
            dispose();
        });
        
        JButton cancelButton = new JButton("キャンセル");
        cancelButton.addActionListener(e -> {
            choice = UserChoice.CANCEL;
            dispose();
        });
        
        buttonPanel.add(copyButton);
        buttonPanel.add(browseButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    public UserChoice getChoice() {
        return choice;
    }
}
```

## 11. HOROSのお渡し用CD/DVD作成機能の解析

### 11.1 CD/DVD作成のオプション

**HOROS実装** (`DiscBurningOptions.h`):
```objective-c
@interface DiscBurningOptions : NSObject {
    BOOL anonymize;                    // 匿名化
    NSArray* anonymizationTags;        // 匿名化タグ
    BOOL includeWeasis;                // Weasisビューワーの同梱
    BOOL includeOsirixLite;            // OsirixLiteビューワーの同梱
    BOOL includeHTMLQT;                // HTML/QuickTimeの同梱
    BOOL includeReports;              // レポートの同梱
    Compression compression;           // 圧縮方式
    BOOL zip;                          // ZIP圧縮
    BOOL zipEncrypt;                   // ZIP暗号化
}
```

**Java実装での対応**:
```java
public class DiscBurningOptions {
    private boolean anonymize;
    private List<String> anonymizationTags;
    private boolean includeJJDICOMViewerLite;
    private boolean includeHTML;
    private boolean includeReports;
    private CompressionMode compression;
    private boolean zip;
    private boolean zipEncrypt;
    private String zipPassword;
    
    public enum CompressionMode {
        NONE,           // 圧縮なし
        JPEG,           // JPEG圧縮
        JPEG2000        // JPEG2000圧縮
    }
    
    // Getters and setters
}
```

### 11.2 CD/DVD内容の準備

**HOROS実装** (`BurnerWindowController.m` - `prepareCDContent:`):
```objective-c
- (void) prepareCDContent: (NSMutableArray*) dbObjects :(NSMutableArray*) originalDbObjects
{
    NSString *burnFolder = [self folderToBurn];
    NSString *subFolder = [NSString stringWithFormat:@"%@/DICOM",burnFolder];
    
    // DICOMファイルをコピー
    // 圧縮処理
    // DICOMDIRファイルの作成
    // Weasis/OsirixLiteの同梱
    // HTML/QuickTimeのエクスポート
}
```

**Java実装での対応**:
```java
import java.nio.file.*;
import java.util.List;

public class CDDVDContentPreparer {
    private DiscBurningOptions options;
    private Path burnFolder;
    
    public CDDVDContentPreparer(DiscBurningOptions options) {
        this.options = options;
    }
    
    /**
     * CD/DVDの内容を準備
     */
    public void prepareContent(List<Path> dicomFiles, Path outputFolder) throws Exception {
        this.burnFolder = outputFolder;
        Path dicomFolder = outputFolder.resolve("DICOM");
        
        // DICOMフォルダの作成
        Files.createDirectories(dicomFolder);
        
        // DICOMファイルのコピー
        List<Path> copiedFiles = copyDicomFiles(dicomFiles, dicomFolder);
        
        // 圧縮処理
        if (options.getCompression() != CompressionMode.NONE) {
            compressFiles(copiedFiles);
        }
        
        // DICOMDIRファイルの作成
        createDicomDir(copiedFiles, outputFolder);
        
        // JJDICOMViewer-Liteの同梱
        if (options.isIncludeJJDICOMViewerLite()) {
            includeViewerLite(outputFolder);
        }
        
        // HTMLのエクスポート
        if (options.isIncludeHTML()) {
            exportHTML(dicomFiles, outputFolder);
        }
        
        // レポートの同梱
        if (options.isIncludeReports()) {
            includeReports(outputFolder);
        }
    }
    
    private List<Path> copyDicomFiles(List<Path> sourceFiles, Path destFolder) throws Exception {
        List<Path> copiedFiles = new ArrayList<>();
        
        for (int i = 0; i < sourceFiles.size(); i++) {
            Path sourceFile = sourceFiles.get(i);
            Path destFile = destFolder.resolve(String.format("%05d.dcm", i));
            
            // 匿名化処理
            if (options.isAnonymize()) {
                destFile = anonymizeDicomFile(sourceFile, destFile, options.getAnonymizationTags());
            } else {
                Files.copy(sourceFile, destFile, StandardCopyOption.REPLACE_EXISTING);
            }
            
            copiedFiles.add(destFile);
        }
        
        return copiedFiles;
    }
    
    private void createDicomDir(List<Path> dicomFiles, Path outputFolder) throws Exception {
        // DICOMDIRファイルの作成
        // dcm4cheのDicomDirBuilderを使用
        // ...
    }
    
    private void includeViewerLite(Path outputFolder) throws Exception {
        // JJDICOMViewer-LiteのJARファイルをコピー
        Path viewerLiteJar = getViewerLiteJarPath();
        Files.copy(viewerLiteJar, outputFolder.resolve("JJDICOMViewer-Lite.jar"), 
            StandardCopyOption.REPLACE_EXISTING);
        
        // README.txtの作成
        createReadmeFile(outputFolder);
    }
    
    private void createReadmeFile(Path outputFolder) throws Exception {
        String readme = """
            JJDICOMViewer-Lite 使用方法
            ============================
            
            1. JJDICOMViewer-Lite.jarをダブルクリックして起動してください
               （Javaがインストールされている必要があります）
            
            2. ファイルメニューから「DICOMフォルダを開く」を選択し、
               DICOMフォルダを選択してください
            
            操作説明:
            - パンニング: 左クリック + ドラッグ
            - ズーム: CTRL + マウスホイール
            - Window Level/Width: SHIFT + 左クリック + ドラッグ
            - スライス移動: マウスホイール
            - 左右反転: Hキー
            - 上下反転: Vキー
            - 右回転: Rキー
            - 左回転: Lキー
            
            システム要件:
            - Java 21以上が必要です
            - Windows 10/11、macOS、Linuxで動作します
            """;
        
        Files.write(outputFolder.resolve("README.txt"), 
            readme.getBytes(StandardCharsets.UTF_8));
    }
}
```

### 11.3 CD/DVD書き込み

**HOROS実装** (`BurnerWindowController.m` - `burnCD:`):
```objective-c
- (void)burnCD:(id)object
{
    DRTrack* track = [DRTrack trackForRootFolder: [DRFolder folderWithPath: [self folderToBurn]]];
    
    DRBurnSetupPanel *bsp = [DRBurnSetupPanel setupPanel];
    if( [bsp runSetupPanel] == NSOKButton)
    {
        DRBurnProgressPanel *bpp = [DRBurnProgressPanel progressPanel];
        [bpp beginProgressSheetForBurn:[bsp burnObject] layout:track modalForWindow: [self window]];
    }
}
```

**Java実装での対応**:
```java
import java.nio.file.*;

public class CDDVDBurner {
    /**
     * CD/DVDに書き込み
     * 注意: Javaでは直接CD/DVD書き込みは難しいため、
     * 外部ツール（cdrecord、hdiutil等）を使用するか、
     * JNIでネイティブライブラリを呼び出す必要がある
     */
    public void burnToDisc(Path folderToBurn, String discName) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            // Windows: ImgBurn、CDBurnerXP等の外部ツールを使用
            burnWindows(folderToBurn, discName);
        } else if (os.contains("mac")) {
            // macOS: hdiutilを使用
            burnMacOS(folderToBurn, discName);
        } else if (os.contains("linux")) {
            // Linux: cdrecord、growisofs等を使用
            burnLinux(folderToBurn, discName);
        }
    }
    
    private void burnMacOS(Path folderToBurn, String discName) throws Exception {
        // hdiutilを使用してDMGを作成し、その後ディスクユーティリティで書き込み
        // または、drutilコマンドを使用
        ProcessBuilder pb = new ProcessBuilder(
            "hdiutil", "makehybrid", "-iso", "-joliet",
            "-o", discName + ".dmg",
            folderToBurn.toString()
        );
        Process process = pb.start();
        process.waitFor();
    }
    
    private void burnLinux(Path folderToBurn, String discName) throws Exception {
        // mkisofs + cdrecordを使用
        // または、growisofsを使用
        ProcessBuilder pb = new ProcessBuilder(
            "mkisofs", "-r", "-J", "-o", discName + ".iso",
            folderToBurn.toString()
        );
        Process process = pb.start();
        process.waitFor();
    }
    
    private void burnWindows(Path folderToBurn, String discName) throws Exception {
        // Windowsでは外部ツールを使用
        // ImgBurnのコマンドライン版を使用するか、
        // Windows APIをJNIで呼び出す
        // ...
    }
}
```

## 12. 実装順序

1. **プラットフォーム別ドライブモニター**: Windows、macOS、Linux対応
2. **DICOMファイルスキャナー**: 再帰的なファイル検索
3. **DICOMDIRパーサー**: DICOMDIRファイルの解析
4. **ユーザー確認ダイアログ**: コピー/ブラウズ/キャンセルの選択
5. **DICOM自動インポーター**: ファイルのコピーとデータベース登録
6. **CD/DVD内容準備**: DICOMファイルのコピー、圧縮、DICOMDIR作成
7. **CD/DVD書き込み**: 外部ツールまたはJNIを使用
8. **統合サービス**: CD/DVD自動インポートサービス
9. **UI実装**: 設定ダイアログと進捗ダイアログ
10. **Vet-System連携**: 共通ライブラリの提供

## 13. 参考資料

- HOROSソースコード: 
  - `BrowserController+Sources.m`: ドライブマウント通知の監視
  - `DicomDatabase+Scan.mm`: CD/DVDスキャン機能
  - `BurnerWindowController.m`: CD/DVD書き込み機能
  - `DiscBurningOptions.h`: 書き込みオプション
- Java NIO FileSystem API: https://docs.oracle.com/javase/tutorial/essential/io/fileio.html
- dcm4cheドキュメント: https://www.dcm4che.org/
- DICOMDIR仕様: DICOM Part 10 (Media Storage and File Format)

