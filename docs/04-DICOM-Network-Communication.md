# DICOM通信 - Java移植ドキュメント

## 1. 概要

このドキュメントは、HOROSのDICOM通信機能（C-STORE、C-FIND、C-MOVE）を解析し、Javaでの実装方法をまとめたものです。

**注意**: Bonjour機能による自動検出は実装しません。明示的な設定による接続のみをサポートします（Vet-Systemとの連携が主目的のため）。

## 2. DICOM通信の基本

### 2.1 DICOMネットワークプロトコル

DICOM通信は以下のサービスを使用：

- **C-ECHO**: 接続確認
- **C-STORE**: 画像送信（Store SCU）
- **C-FIND**: 検索（Query/Retrieve）
- **C-MOVE**: 画像取得（Move SCU）
- **C-GET**: 画像取得（Get SCU）

### 2.2 基本パラメータ

- **Calling AET**: 送信側のApplication Entity Title
- **Called AET**: 受信側のApplication Entity Title
- **Hostname**: サーバーのIPアドレスまたはホスト名
- **Port**: ポート番号（通常11112）
- **Transfer Syntax**: 転送構文（圧縮形式）

## 3. C-STORE (送信)

### 3.1 DCMTKStoreSCUクラスの解析

**HOROS実装** (`DCMTKStoreSCU.h`):
```objective-c
@interface DCMTKStoreSCU : NSObject {
    NSString *_callingAET;
    NSString *_calledAET;
    int _port;
    NSString *_hostname;
    NSMutableArray *_filesToSend;
    int _transferSyntax;
    float _compression;
    
    // TLS settings
    BOOL _secureConnection;
    BOOL _doAuthenticate;
}
```

### 3.2 Java実装

```java
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.*;
import org.dcm4che3.net.service.*;
import org.dcm4che3.data.*;
import org.dcm4che3.io.*;

public class DicomStoreSCU {
    private String callingAET;
    private String calledAET;
    private String hostname;
    private int port;
    private int transferSyntax;
    private float compression;
    
    private Device device;
    private Connection remote;
    private Connection conn;
    private ApplicationEntity ae;
    
    public DicomStoreSCU(String callingAET, String calledAET, 
                        String hostname, int port) {
        this.callingAET = callingAET;
        this.calledAET = calledAET;
        this.hostname = hostname;
        this.port = port;
        this.transferSyntax = UID.ImplicitVRLittleEndian;
        this.compression = 0.0f;
        
        initializeDevice();
    }
    
    private void initializeDevice() {
        device = new Device("storeSCU");
        
        // ローカル接続
        conn = new Connection();
        device.addConnection(conn);
        
        // リモート接続
        remote = new Connection();
        remote.setHostname(hostname);
        remote.setPort(port);
        device.addConnection(remote);
        
        // Application Entity
        ae = new ApplicationEntity(callingAET);
        ae.addConnection(conn);
        device.addApplicationEntity(ae);
    }
    
    public void sendFiles(List<Path> files) throws Exception {
        try (Association as = connect()) {
            for (Path file : files) {
                sendFile(as, file);
            }
        }
    }
    
    private Association connect() throws Exception {
        // リモートAEの設定
        ApplicationEntity remoteAE = new ApplicationEntity(calledAET);
        remoteAE.addConnection(remote);
        
        // プレゼンテーションコンテキストの追加
        ae.addTransferCapability(new TransferCapability(
            null, "*", TransferCapability.Role.SCU));
        
        // アソシエーション確立
        Association as = ae.connect(remoteAE, null);
        
        return as;
    }
    
    private void sendFile(Association as, Path file) throws Exception {
        try (DicomInputStream dis = new DicomInputStream(file.toFile())) {
            Attributes attrs = dis.readDataset(-1, -1);
            String sopClassUID = attrs.getString(Tag.SOPClassUID);
            String sopInstanceUID = attrs.getString(Tag.SOPInstanceUID);
            
            // 転送構文の選択
            String ts = selectTransferSyntax(sopClassUID);
            
            // C-STORE要求
            DimseRSP rsp = as.cstore(sopClassUID, sopInstanceUID, 
                attrs, ts, null);
            
            // レスポンス確認
            if (rsp.getCommand().getInt(Tag.Status) != 0) {
                throw new Exception("C-STORE failed: " + 
                    rsp.getCommand().getString(Tag.ErrorComment));
            }
        }
    }
    
    private String selectTransferSyntax(String sopClassUID) {
        // 転送構文の選択ロジック
        if (compression > 0.0f) {
            // JPEG圧縮
            if (compression < 0.5f) {
                return UID.JPEGLosslessSV1;
            } else {
                return UID.JPEGBaseline1;
            }
        }
        
        // 非圧縮
        return transferSyntax;
    }
    
    public void setTransferSyntax(int syntax) {
        this.transferSyntax = syntax;
    }
    
    public void setCompression(float compression) {
        this.compression = compression;
    }
}
```

### 3.3 使用例

```java
public class StoreExample {
    public static void main(String[] args) {
        DicomStoreSCU storeSCU = new DicomStoreSCU(
            "MY_AET",      // Calling AET
            "SERVER_AET",   // Called AET
            "192.168.1.100", // Hostname
            11112          // Port
        );
        
        List<Path> files = Arrays.asList(
            Paths.get("image1.dcm"),
            Paths.get("image2.dcm")
        );
        
        try {
            storeSCU.sendFiles(files);
            System.out.println("Files sent successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 4. C-FIND (検索)

### 4.1 DCMTKQueryNodeクラスの解析

**HOROS実装** (`DCMTKQueryNode.h`):
```objective-c
@interface DCMTKQueryNode : DCMTKServiceClassUser {
    NSMutableArray *_children;
    NSString *_uid;
    NSString *_theDescription;
    NSString *_name;
    NSString *_patientID;
    DCMCalendarDate *_date;
    NSString *_modality;
}
```

### 4.2 Java実装

```java
public class DicomQueryNode {
    private String callingAET;
    private String calledAET;
    private String hostname;
    private int port;
    
    private Device device;
    private ApplicationEntity ae;
    private Connection conn;
    private Connection remote;
    
    public DicomQueryNode(String callingAET, String calledAET,
                         String hostname, int port) {
        this.callingAET = callingAET;
        this.calledAET = calledAET;
        this.hostname = hostname;
        this.port = port;
        
        initializeDevice();
    }
    
    private void initializeDevice() {
        device = new Device("querySCU");
        
        conn = new Connection();
        device.addConnection(conn);
        
        remote = new Connection();
        remote.setHostname(hostname);
        remote.setPort(port);
        device.addConnection(remote);
        
        ae = new ApplicationEntity(callingAET);
        ae.addConnection(conn);
        device.addApplicationEntity(ae);
    }
    
    // Patient Level Query
    public List<DicomStudy> queryPatients(String patientName) 
            throws Exception {
        Attributes keys = new Attributes();
        keys.setString(Tag.PatientName, VR.PN, patientName);
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, "PATIENT");
        
        return queryStudies(keys);
    }
    
    // Study Level Query
    public List<DicomStudy> queryStudies(String patientID, 
                                        String studyDate) 
            throws Exception {
        Attributes keys = new Attributes();
        if (patientID != null) {
            keys.setString(Tag.PatientID, VR.LO, patientID);
        }
        if (studyDate != null) {
            keys.setString(Tag.StudyDate, VR.DA, studyDate);
        }
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
        
        return queryStudies(keys);
    }
    
    // Series Level Query
    public List<DicomSeries> querySeries(String studyInstanceUID) 
            throws Exception {
        Attributes keys = new Attributes();
        keys.setString(Tag.StudyInstanceUID, VR.UI, studyInstanceUID);
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, "SERIES");
        
        return querySeries(keys);
    }
    
    // Image Level Query
    public List<DicomImage> queryImages(String seriesInstanceUID) 
            throws Exception {
        Attributes keys = new Attributes();
        keys.setString(Tag.SeriesInstanceUID, VR.UI, seriesInstanceUID);
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, "IMAGE");
        
        return queryImages(keys);
    }
    
    private List<DicomStudy> queryStudies(Attributes keys) 
            throws Exception {
        List<DicomStudy> studies = new ArrayList<>();
        
        try (Association as = connect()) {
            // C-FIND要求
            DimseRSP rsp = as.cfind(UID.StudyRootQueryRetrieveInformationModelFIND,
                UID.ImplicitVRLittleEndian, keys, null);
            
            // レスポンス処理
            while (rsp.next()) {
                Attributes attrs = rsp.getDataset();
                if (attrs != null) {
                    DicomStudy study = createStudyFromAttributes(attrs);
                    studies.add(study);
                }
            }
        }
        
        return studies;
    }
    
    private List<DicomSeries> querySeries(Attributes keys) 
            throws Exception {
        List<DicomSeries> series = new ArrayList<>();
        
        try (Association as = connect()) {
            DimseRSP rsp = as.cfind(
                UID.StudyRootQueryRetrieveInformationModelFIND,
                UID.ImplicitVRLittleEndian, keys, null);
            
            while (rsp.next()) {
                Attributes attrs = rsp.getDataset();
                if (attrs != null) {
                    DicomSeries s = createSeriesFromAttributes(attrs);
                    series.add(s);
                }
            }
        }
        
        return series;
    }
    
    private List<DicomImage> queryImages(Attributes keys) 
            throws Exception {
        List<DicomImage> images = new ArrayList<>();
        
        try (Association as = connect()) {
            DimseRSP rsp = as.cfind(
                UID.StudyRootQueryRetrieveInformationModelFIND,
                UID.ImplicitVRLittleEndian, keys, null);
            
            while (rsp.next()) {
                Attributes attrs = rsp.getDataset();
                if (attrs != null) {
                    DicomImage img = createImageFromAttributes(attrs);
                    images.add(img);
                }
            }
        }
        
        return images;
    }
    
    private Association connect() throws Exception {
        ApplicationEntity remoteAE = new ApplicationEntity(calledAET);
        remoteAE.addConnection(remote);
        
        ae.addTransferCapability(new TransferCapability(
            null, UID.StudyRootQueryRetrieveInformationModelFIND,
            TransferCapability.Role.SCU));
        
        return ae.connect(remoteAE, null);
    }
    
    private DicomStudy createStudyFromAttributes(Attributes attrs) {
        DicomStudy study = new DicomStudy();
        study.setStudyInstanceUID(attrs.getString(Tag.StudyInstanceUID));
        study.setPatientID(attrs.getString(Tag.PatientID));
        study.setPatientName(attrs.getString(Tag.PatientName));
        study.setStudyDate(attrs.getString(Tag.StudyDate));
        study.setStudyDescription(attrs.getString(Tag.StudyDescription));
        study.setModality(attrs.getString(Tag.ModalitiesInStudy));
        study.setAccessionNumber(attrs.getString(Tag.AccessionNumber));
        return study;
    }
    
    private DicomSeries createSeriesFromAttributes(Attributes attrs) {
        DicomSeries series = new DicomSeries();
        series.setSeriesInstanceUID(attrs.getString(Tag.SeriesInstanceUID));
        series.setSeriesNumber(attrs.getString(Tag.SeriesNumber));
        series.setSeriesDescription(attrs.getString(Tag.SeriesDescription));
        series.setModality(attrs.getString(Tag.Modality));
        series.setNumberOfImages(attrs.getInt(Tag.NumberOfSeriesRelatedInstances, 0));
        return series;
    }
    
    private DicomImage createImageFromAttributes(Attributes attrs) {
        DicomImage image = new DicomImage();
        image.setSopInstanceUID(attrs.getString(Tag.SOPInstanceUID));
        image.setInstanceNumber(attrs.getString(Tag.InstanceNumber));
        image.setWidth(attrs.getInt(Tag.Columns, 0));
        image.setHeight(attrs.getInt(Tag.Rows, 0));
        return image;
    }
}
```

### 4.3 使用例

```java
public class QueryExample {
    public static void main(String[] args) {
        DicomQueryNode query = new DicomQueryNode(
            "MY_AET", "SERVER_AET", "192.168.1.100", 11112);
        
        try {
            // 患者検索
            List<DicomStudy> studies = query.queryPatients("DOE^JOHN");
            
            for (DicomStudy study : studies) {
                System.out.println("Study: " + study.getStudyDescription());
                
                // シリーズ検索
                List<DicomSeries> series = query.querySeries(
                    study.getStudyInstanceUID());
                
                for (DicomSeries s : series) {
                    System.out.println("  Series: " + s.getSeriesDescription());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 5. C-MOVE (取得)

### 5.1 Java実装

```java
public class DicomMoveSCU {
    private String callingAET;
    private String calledAET;
    private String hostname;
    private int port;
    private String destinationAET;
    
    private Device device;
    private ApplicationEntity ae;
    private Connection conn;
    private Connection remote;
    
    public DicomMoveSCU(String callingAET, String calledAET,
                        String hostname, int port, 
                        String destinationAET) {
        this.callingAET = callingAET;
        this.calledAET = calledAET;
        this.hostname = hostname;
        this.port = port;
        this.destinationAET = destinationAET;
        
        initializeDevice();
    }
    
    private void initializeDevice() {
        device = new Device("moveSCU");
        
        conn = new Connection();
        device.addConnection(conn);
        
        remote = new Connection();
        remote.setHostname(hostname);
        remote.setPort(port);
        device.addConnection(remote);
        
        ae = new ApplicationEntity(callingAET);
        ae.addConnection(conn);
        device.addApplicationEntity(ae);
    }
    
    // Study取得
    public void retrieveStudy(String studyInstanceUID, 
                             Path outputDir) throws Exception {
        Attributes keys = new Attributes();
        keys.setString(Tag.StudyInstanceUID, VR.UI, studyInstanceUID);
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
        
        retrieve(keys, outputDir);
    }
    
    // Series取得
    public void retrieveSeries(String seriesInstanceUID, 
                              Path outputDir) throws Exception {
        Attributes keys = new Attributes();
        keys.setString(Tag.SeriesInstanceUID, VR.UI, seriesInstanceUID);
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, "SERIES");
        
        retrieve(keys, outputDir);
    }
    
    // Image取得
    public void retrieveImage(String sopInstanceUID, 
                             Path outputFile) throws Exception {
        Attributes keys = new Attributes();
        keys.setString(Tag.SOPInstanceUID, VR.UI, sopInstanceUID);
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, "IMAGE");
        
        retrieve(keys, outputFile.getParent());
    }
    
    private void retrieve(Attributes keys, Path outputDir) 
            throws Exception {
        try (Association as = connect()) {
            // C-MOVE要求
            DimseRSP rsp = as.cmove(
                UID.StudyRootQueryRetrieveInformationModelMOVE,
                UID.ImplicitVRLittleEndian, keys, destinationAET);
            
            // 進捗コールバック
            rsp.setDimseRSPHandler(new DimseRSPHandler() {
                @Override
                public void onDimseRSP(Association as, 
                                      Attributes cmd, 
                                      Attributes data) {
                    if (data != null) {
                        try {
                            String sopInstanceUID = 
                                data.getString(Tag.SOPInstanceUID);
                            Path outputFile = outputDir.resolve(
                                sopInstanceUID + ".dcm");
                            
                            // ファイル保存
                            try (DicomOutputStream dos = 
                                new DicomOutputStream(outputFile.toFile())) {
                                dos.writeDataset(null, data);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            
            // レスポンス処理
            while (rsp.next()) {
                // 進捗更新
            }
        }
    }
    
    private Association connect() throws Exception {
        ApplicationEntity remoteAE = new ApplicationEntity(calledAET);
        remoteAE.addConnection(remote);
        
        ae.addTransferCapability(new TransferCapability(
            null, UID.StudyRootQueryRetrieveInformationModelMOVE,
            TransferCapability.Role.SCU));
        
        return ae.connect(remoteAE, null);
    }
}
```

### 5.2 使用例

```java
public class MoveExample {
    public static void main(String[] args) {
        DicomMoveSCU moveSCU = new DicomMoveSCU(
            "MY_AET", "SERVER_AET", "192.168.1.100", 11112, "MY_AET");
        
        try {
            Path outputDir = Paths.get("retrieved");
            Files.createDirectories(outputDir);
            
            // シリーズ取得
            moveSCU.retrieveSeries("1.2.3.4.5.6", outputDir);
            
            System.out.println("Retrieval completed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 6. C-ECHO (接続確認)

```java
public class DicomEchoSCU {
    public boolean echo(String callingAET, String calledAET,
                       String hostname, int port) throws Exception {
        Device device = new Device("echoSCU");
        
        Connection conn = new Connection();
        device.addConnection(conn);
        
        Connection remote = new Connection();
        remote.setHostname(hostname);
        remote.setPort(port);
        device.addConnection(remote);
        
        ApplicationEntity ae = new ApplicationEntity(callingAET);
        ae.addConnection(conn);
        device.addApplicationEntity(ae);
        
        ApplicationEntity remoteAE = new ApplicationEntity(calledAET);
        remoteAE.addConnection(remote);
        
        try (Association as = ae.connect(remoteAE, null)) {
            // C-ECHO要求
            DimseRSP rsp = as.cecho();
            
            int status = rsp.getCommand().getInt(Tag.Status);
            return status == 0;
        }
    }
}
```

## 7. エラーハンドリング

```java
public class DicomCommunicationException extends Exception {
    private int status;
    private String errorComment;
    
    public DicomCommunicationException(int status, String errorComment) {
        super("DICOM communication failed: " + errorComment);
        this.status = status;
        this.errorComment = errorComment;
    }
    
    public int getStatus() {
        return status;
    }
    
    public String getErrorComment() {
        return errorComment;
    }
}

// 使用例
try {
    storeSCU.sendFiles(files);
} catch (DicomCommunicationException e) {
    System.err.println("Status: " + e.getStatus());
    System.err.println("Error: " + e.getErrorComment());
}
```

## 8. 進捗管理

```java
public interface ProgressCallback {
    void onProgress(int current, int total, String message);
    void onComplete();
    void onError(Exception e);
}

public class DicomStoreSCU {
    private ProgressCallback progressCallback;
    
    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }
    
    public void sendFiles(List<Path> files) throws Exception {
        int total = files.size();
        
        try (Association as = connect()) {
            for (int i = 0; i < files.size(); i++) {
                Path file = files.get(i);
                
                if (progressCallback != null) {
                    progressCallback.onProgress(i + 1, total, 
                        "Sending: " + file.getFileName());
                }
                
                sendFile(as, file);
            }
            
            if (progressCallback != null) {
                progressCallback.onComplete();
            }
        }
    }
}
```

## 9. まとめ

DICOM通信のJava実装では：

1. **dcm4che**のネットワーク機能を使用
2. **C-STORE**: 画像送信
3. **C-FIND**: 階層的検索（Patient→Study→Series→Image）
4. **C-MOVE**: 画像取得
5. **エラーハンドリング**と**進捗管理**を実装

次のステップ: データベース管理の実装

