# 追加機能 - Java移植ドキュメント

## 1. 概要

このドキュメントは、HOROSに実装されている追加機能（匿名化、レポート、印刷、WADO等）を解析し、Javaでの実装方法をまとめたものです。

## 2. 匿名化機能

### 2.1 HOROS実装の解析

**HOROS実装** (`Anonymization.h`):
```objective-c
@interface Anonymization : NSObject

+(NSDictionary*)anonymizeFiles:(NSArray*)files 
    dicomImages:(NSArray*)dicomImages 
    toPath:(NSString*)dirPath 
    withTags:(NSArray*)intags;

+(AnonymizationPanelController*)showPanelForDefaultsKey:(NSString*)defaultsKey 
    modalForWindow:(NSWindow*)window 
    modalDelegate:(id)delegate 
    didEndSelector:(SEL)sel 
    representedObject:(id)representedObject;
@end
```

**主要機能**:
- DICOMファイルの匿名化
- 匿名化タグの選択
- 匿名化テンプレートの保存・読み込み
- CD/DVD作成時の匿名化オプション

### 2.2 Java実装

```java
import org.dcm4che3.data.*;
import org.dcm4che3.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

public class DicomAnonymizer {
    /**
     * DICOMファイルを匿名化
     */
    public Path anonymizeFile(Path sourceFile, Path destFile, 
                              List<AnonymizationRule> rules) throws Exception {
        try (DicomInputStream dis = new DicomInputStream(Files.newInputStream(sourceFile))) {
            Attributes dataset = dis.readDataset(-1, -1);
            
            // 匿名化ルールを適用
            for (AnonymizationRule rule : rules) {
                rule.apply(dataset);
            }
            
            // 匿名化されたファイルを保存
            try (DicomOutputStream dos = new DicomOutputStream(
                    Files.newOutputStream(destFile))) {
                dos.writeDataset(dataset.createFileMetaInformation(
                    dataset.getString(Tag.TransferSyntaxUID)), dataset);
            }
        }
        
        return destFile;
    }
    
    /**
     * 複数のDICOMファイルを匿名化
     */
    public Map<Path, Path> anonymizeFiles(List<Path> sourceFiles, 
                                          Path destDir, 
                                          List<AnonymizationRule> rules) throws Exception {
        Map<Path, Path> result = new HashMap<>();
        
        Files.createDirectories(destDir);
        
        for (Path sourceFile : sourceFiles) {
            String fileName = sourceFile.getFileName().toString();
            Path destFile = destDir.resolve(fileName);
            
            anonymizeFile(sourceFile, destFile, rules);
            result.put(sourceFile, destFile);
        }
        
        return result;
    }
}

public class AnonymizationRule {
    private int tag;
    private AnonymizationAction action;
    private String replacementValue;
    
    public enum AnonymizationAction {
        REMOVE,         // タグを削除
        REPLACE,        // 値を置換
        REPLACE_DATE,   // 日付を置換（オフセット適用）
        REPLACE_UID     // UIDを置換（新しいUIDを生成）
    }
    
    public void apply(Attributes dataset) {
        switch (action) {
            case REMOVE:
                dataset.remove(tag);
                break;
            case REPLACE:
                dataset.setString(tag, VR.LO, replacementValue);
                break;
            case REPLACE_DATE:
                // 日付の置換（オフセット適用）
                String dateStr = dataset.getString(tag);
                if (dateStr != null) {
                    String anonymizedDate = replaceDate(dateStr);
                    dataset.setString(tag, VR.DA, anonymizedDate);
                }
                break;
            case REPLACE_UID:
                // UIDの置換（新しいUIDを生成）
                String uid = dataset.getString(tag);
                if (uid != null) {
                    String newUID = generateNewUID(uid);
                    dataset.setString(tag, VR.UI, newUID);
                }
                break;
        }
    }
    
    private String replaceDate(String dateStr) {
        // 日付のオフセット適用（例: 365日減算）
        // 実装は省略
        return dateStr;
    }
    
    private String generateNewUID(String originalUID) {
        // 新しいUIDを生成（元のUIDをハッシュ化して新しいUIDを生成）
        // 実装は省略
        return "1.2.840.10008.5.1.4.1.1.2." + UUID.randomUUID().toString().replace("-", "");
    }
}

// 標準的な匿名化ルール
public class StandardAnonymizationRules {
    public static List<AnonymizationRule> getDefaultRules() {
        List<AnonymizationRule> rules = new ArrayList<>();
        
        // Patient Name
        rules.add(new AnonymizationRule(Tag.PatientName, 
            AnonymizationRule.AnonymizationAction.REPLACE, "ANONYMOUS"));
        
        // Patient ID
        rules.add(new AnonymizationRule(Tag.PatientID, 
            AnonymizationRule.AnonymizationAction.REPLACE, "ANONYMOUS"));
        
        // Patient Birth Date
        rules.add(new AnonymizationRule(Tag.PatientBirthDate, 
            AnonymizationRule.AnonymizationAction.REMOVE));
        
        // Patient Sex
        rules.add(new AnonymizationRule(Tag.PatientSex, 
            AnonymizationRule.AnonymizationAction.REMOVE));
        
        // Institution Name
        rules.add(new AnonymizationRule(Tag.InstitutionName, 
            AnonymizationRule.AnonymizationAction.REMOVE));
        
        // Referring Physician Name
        rules.add(new AnonymizationRule(Tag.ReferringPhysicianName, 
            AnonymizationRule.AnonymizationAction.REMOVE));
        
        // Study Instance UID（新しいUIDを生成）
        rules.add(new AnonymizationRule(Tag.StudyInstanceUID, 
            AnonymizationRule.AnonymizationAction.REPLACE_UID, null));
        
        // Series Instance UID（新しいUIDを生成）
        rules.add(new AnonymizationRule(Tag.SeriesInstanceUID, 
            AnonymizationRule.AnonymizationAction.REPLACE_UID, null));
        
        // SOP Instance UID（新しいUIDを生成）
        rules.add(new AnonymizationRule(Tag.SOPInstanceUID, 
            AnonymizationRule.AnonymizationAction.REPLACE_UID, null));
        
        return rules;
    }
}
```

## 3. レポート機能

### 3.1 HOROS実装の解析

**HOROS実装** (`Reports.h`, `StructuredReport.h`):
```objective-c
@interface Reports : NSObject
- (BOOL)createNewReport:(NSManagedObject*)study 
    destination:(NSString*)path 
    type:(int)type;
- (BOOL) createNewPagesReportForStudy:(NSManagedObject*)aStudy 
    toDestinationPath:(NSString*)aPath;
- (BOOL) createNewOpenDocumentReportForStudy:(NSManagedObject*)aStudy 
    toDestinationPath:(NSString*)aPath;
@end

@interface StructuredReport : NSObject
- (NSArray *)findings;
- (void)setFindings:(NSMutableArray *)findings;
- (NSArray *)conclusions;
- (void)setConclusions:(NSMutableArray *)conclusions;
- (void)save;
- (void)export:(NSString *)path;
@end
```

**主要機能**:
- レポートの作成（Pages、Word、OpenDocument形式）
- DICOM Structured Report (SR) の作成・編集
- レポートテンプレートの使用
- キー画像の参照

### 3.2 Java実装

```java
import org.dcm4che3.data.*;
import org.dcm4che3.io.*;
import java.nio.file.*;
import java.util.List;

public class DicomReportGenerator {
    /**
     * レポートを生成
     */
    public void generateReport(DicomStudy study, Path outputPath, 
                              ReportTemplate template) throws Exception {
        // テンプレートからレポートを生成
        String reportContent = template.generate(study);
        
        // ファイルに保存
        Files.write(outputPath, reportContent.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * DICOM Structured Reportを作成
     */
    public void createStructuredReport(DicomStudy study, 
                                      List<String> findings,
                                      List<String> conclusions,
                                      Path outputPath) throws Exception {
        Attributes dataset = new Attributes();
        
        // SRの基本構造を作成
        dataset.setString(Tag.SOPClassUID, VR.UI, UID.BasicTextSRStorage);
        dataset.setString(Tag.SOPInstanceUID, VR.UI, UIDUtils.createUID());
        dataset.setString(Tag.StudyInstanceUID, VR.UI, study.getStudyInstanceUID());
        dataset.setString(Tag.SeriesInstanceUID, VR.UI, UIDUtils.createUID());
        
        // Content Sequenceを作成
        Sequence contentSequence = dataset.newSequence(Tag.ContentSequence, 10);
        
        // Findings
        for (String finding : findings) {
            Attributes findingItem = new Attributes();
            findingItem.setString(Tag.ValueType, VR.CS, "TEXT");
            findingItem.setString(Tag.ConceptNameCodeSequence, VR.SQ, 
                createCodeSequence("121071", "DCM", "Finding"));
            findingItem.setString(Tag.TextValue, VR.UT, finding);
            contentSequence.add(findingItem);
        }
        
        // Conclusions
        for (String conclusion : conclusions) {
            Attributes conclusionItem = new Attributes();
            conclusionItem.setString(Tag.ValueType, VR.CS, "TEXT");
            conclusionItem.setString(Tag.ConceptNameCodeSequence, VR.SQ, 
                createCodeSequence("121070", "DCM", "Conclusion"));
            conclusionItem.setString(Tag.TextValue, VR.UT, conclusion);
            contentSequence.add(conclusionItem);
        }
        
        // ファイルに保存
        try (DicomOutputStream dos = new DicomOutputStream(
                Files.newOutputStream(outputPath))) {
            dos.writeDataset(dataset.createFileMetaInformation(
                UID.ExplicitVRLittleEndian), dataset);
        }
    }
    
    private Sequence createCodeSequence(String codeValue, String codingScheme, 
                                       String codeMeaning) {
        Attributes codeItem = new Attributes();
        codeItem.setString(Tag.CodeValue, VR.SH, codeValue);
        codeItem.setString(Tag.CodingSchemeDesignator, VR.SH, codingScheme);
        codeItem.setString(Tag.CodeMeaning, VR.LO, codeMeaning);
        
        Sequence sequence = new Sequence(Tag.ConceptNameCodeSequence, 1);
        sequence.add(codeItem);
        return sequence;
    }
}

public class ReportTemplate {
    private String templateContent;
    private Map<String, String> placeholders;
    
    public String generate(DicomStudy study) {
        String result = templateContent;
        
        // プレースホルダーを置換
        result = result.replace("${PatientName}", study.getPatientName());
        result = result.replace("${PatientID}", study.getPatientID());
        result = result.replace("${StudyDate}", study.getStudyDate());
        result = result.replace("${StudyDescription}", study.getStudyDescription());
        // ...
        
        return result;
    }
}
```

## 4. 印刷機能

### 4.1 HOROS実装の解析

**HOROS実装** (`DCMTKPrintSCU.h`, `printView.h`):
```objective-c
@interface DCMTKPrintSCU : DCMTKServiceClassUser {
    const char *_printerID;
    unsigned int _columns;
    unsigned int _rows;
    unsigned int _copies;
    const char *_filmsize;
    const char *_resolution;
    // ...
}
@end

@interface printView : NSView
// 印刷用のビュー
@end
```

**主要機能**:
- DICOM Print SCU（DICOMプリンターへの送信）
- 印刷レイアウトの設定（カラム、行、フィルムサイズ）
- 印刷プレビュー

### 4.2 Java実装

```java
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.*;
import org.dcm4che3.data.*;
import java.awt.print.*;

public class DicomPrintSCU {
    private String callingAET;
    private String calledAET;
    private String hostname;
    private int port;
    private PrintOptions options;
    
    public DicomPrintSCU(String callingAET, String calledAET, 
                         String hostname, int port) {
        this.callingAET = callingAET;
        this.calledAET = calledAET;
        this.hostname = hostname;
        this.port = port;
    }
    
    /**
     * DICOM画像を印刷
     */
    public void printImages(List<Path> dicomFiles, PrintOptions options) throws Exception {
        this.options = options;
        
        Device device = new Device("printSCU");
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
        
        // Print Management SOP Class
        ae.addSOPClass(UID.BasicFilmSessionSOPClass);
        ae.addSOPClass(UID.BasicFilmBoxSOPClass);
        ae.addSOPClass(UID.BasicGrayscaleImageBoxSOPClass);
        
        try (Association as = ae.connect(remoteAE, null)) {
            // Print Jobの作成
            createPrintJob(as, dicomFiles);
        }
    }
    
    private void createPrintJob(Association as, List<Path> dicomFiles) throws Exception {
        // Basic Film Sessionの作成
        Attributes filmSession = new Attributes();
        filmSession.setString(Tag.SOPClassUID, VR.UI, UID.BasicFilmSessionSOPClass);
        filmSession.setString(Tag.SOPInstanceUID, VR.UI, UIDUtils.createUID());
        filmSession.setInt(Tag.NumberOfCopies, VR.US, options.getCopies());
        filmSession.setString(Tag.PrintPriority, VR.CS, "MEDIUM");
        filmSession.setString(Tag.MediumType, VR.CS, options.getMediumType());
        filmSession.setString(Tag.FilmDestination, VR.CS, options.getDestination());
        
        // Basic Film Boxの作成
        Attributes filmBox = new Attributes();
        filmBox.setString(Tag.SOPClassUID, VR.UI, UID.BasicFilmBoxSOPClass);
        filmBox.setString(Tag.SOPInstanceUID, VR.UI, UIDUtils.createUID());
        filmBox.setInt(Tag.ImageDisplayFormat, VR.ST, 
            String.format("%d,%d", options.getColumns(), options.getRows()));
        filmBox.setString(Tag.FilmSizeID, VR.CS, options.getFilmSize());
        filmBox.setString(Tag.FilmOrientation, VR.CS, options.getOrientation());
        filmBox.setString(Tag.MagnificationType, VR.CS, options.getMagnificationType());
        filmBox.setString(Tag.SmoothingType, VR.CS, options.getSmoothingType());
        filmBox.setString(Tag.BorderDensity, VR.CS, options.getBorderDensity());
        filmBox.setString(Tag.EmptyImageDensity, VR.CS, options.getEmptyImageDensity());
        
        // Image Boxの作成
        Sequence imageBoxSequence = filmBox.newSequence(Tag.ImageBoxSequence, dicomFiles.size());
        for (Path dicomFile : dicomFiles) {
            Attributes imageBox = new Attributes();
            imageBox.setString(Tag.SOPClassUID, VR.UI, UID.BasicGrayscaleImageBoxSOPClass);
            imageBox.setString(Tag.SOPInstanceUID, VR.UI, UIDUtils.createUID());
            // 画像データの参照
            imageBoxSequence.add(imageBox);
        }
        
        // N-ACTIONで印刷ジョブを送信
        // 実装は省略
    }
    
    public static class PrintOptions {
        private int columns = 2;
        private int rows = 2;
        private int copies = 1;
        private String filmSize = "A4";
        private String orientation = "PORTRAIT";
        private String magnificationType = "NONE";
        private String smoothingType = "MEDIUM";
        private String mediumType = "PAPER";
        private String destination = "PRINTER";
        
        // Getters and setters
    }
}

// 印刷プレビュー
public class PrintPreviewPanel extends JPanel implements Printable {
    private BufferedImage previewImage;
    
    public PrintPreviewPanel(List<BufferedImage> images, PrintOptions options) {
        // 印刷レイアウトに従って画像を配置
        createPreview(images, options);
    }
    
    private void createPreview(List<BufferedImage> images, PrintOptions options) {
        int cols = options.getColumns();
        int rows = options.getRows();
        int width = 800; // プレビュー幅
        int height = (int)(width * 1.414); // A4縦横比
        
        previewImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = previewImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        int cellWidth = width / cols;
        int cellHeight = height / rows;
        
        for (int i = 0; i < images.size() && i < cols * rows; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = col * cellWidth;
            int y = row * cellHeight;
            
            BufferedImage img = images.get(i);
            // 画像をリサイズして配置
            g2d.drawImage(img, x, y, cellWidth, cellHeight, null);
        }
        
        g2d.dispose();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (previewImage != null) {
            g.drawImage(previewImage, 0, 0, this);
        }
    }
    
    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }
        
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g2d.drawImage(previewImage, 0, 0, 
            (int)pageFormat.getImageableWidth(), 
            (int)pageFormat.getImageableHeight(), null);
        
        return PAGE_EXISTS;
    }
}
```

## 5. WADO機能

### 5.1 HOROS実装の解析

**HOROS実装** (`WADODownload.h`):
```objective-c
@interface WADODownload : NSObject
- (void) WADODownload: (NSArray*) urlToDownload;
@property int countOfSuccesses, WADOGrandTotal, WADOBaseTotal;
@property unsigned long totalData, receivedData;
@end
```

**主要機能**:
- WADO (Web Access to DICOM Objects) による画像取得
- HTTP/HTTPS経由でのDICOM画像のダウンロード
- 進捗管理

### 5.2 Java実装

```java
import java.net.http.*;
import java.net.URI;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WADODownloader {
    private HttpClient httpClient;
    private ProgressCallback progressCallback;
    
    public interface ProgressCallback {
        void onProgress(int current, int total, long receivedBytes, long totalBytes);
        void onComplete(int successCount, int failureCount);
        void onError(String url, Exception e);
    }
    
    public WADODownloader() {
        this.httpClient = HttpClient.newHttpClient();
    }
    
    /**
     * WADO URLからDICOM画像をダウンロード
     */
    public CompletableFuture<List<Path>> download(List<String> wadoUrls, 
                                                   Path destDir,
                                                   ProgressCallback callback) {
        this.progressCallback = callback;
        
        return CompletableFuture.supplyAsync(() -> {
            List<Path> downloadedFiles = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;
            long totalReceived = 0;
            long totalSize = 0;
            
            for (int i = 0; i < wadoUrls.size(); i++) {
                String url = wadoUrls.get(i);
                
                try {
                    Path downloadedFile = downloadSingleFile(url, destDir);
                    downloadedFiles.add(downloadedFile);
                    successCount++;
                    
                    if (progressCallback != null) {
                        progressCallback.onProgress(i + 1, wadoUrls.size(), 
                            totalReceived, totalSize);
                    }
                } catch (Exception e) {
                    failureCount++;
                    
                    if (progressCallback != null) {
                        progressCallback.onError(url, e);
                    }
                }
            }
            
            if (progressCallback != null) {
                progressCallback.onComplete(successCount, failureCount);
            }
            
            return downloadedFiles;
        });
    }
    
    private Path downloadSingleFile(String wadoUrl, Path destDir) throws Exception {
        URI uri = URI.create(wadoUrl);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .GET()
            .build();
        
        HttpResponse<Path> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofFile(createTempFile(destDir)));
        
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("HTTP error: " + response.statusCode());
        }
    }
    
    private Path createTempFile(Path destDir) throws Exception {
        Files.createDirectories(destDir);
        return Files.createTempFile(destDir, "wado_", ".dcm");
    }
    
    /**
     * WADO URLを構築
     */
    public static String buildWADOUrl(String baseUrl, 
                                     String studyInstanceUID,
                                     String seriesInstanceUID,
                                     String sopInstanceUID,
                                     String contentType) {
        return String.format("%s?requestType=WADO&studyUID=%s&seriesUID=%s&objectUID=%s&contentType=%s",
            baseUrl, studyInstanceUID, seriesInstanceUID, sopInstanceUID, contentType);
    }
}
```

## 6. 圧縮/展開機能

### 6.1 HOROS実装の解析

**HOROS実装** (`DicomCompressor.h`):
```objective-c
@interface DicomCompressor : NSObject

+(void)decompressFiles:(NSArray*)filePaths toDirectory:(NSString*)dirPath;
+(void)compressFiles:(NSArray*)filePaths toDirectory:(NSString*)dirPath;
@end
```

**主要機能**:
- JPEG/JPEG2000圧縮
- JPEG/JPEG2000展開
- CD/DVD作成時の圧縮オプション
- 転送構文の変換

### 6.2 Java実装

```java
import org.dcm4che3.data.*;
import org.dcm4che3.io.*;
import org.dcm4che3.imageio.codec.*;
import java.nio.file.*;
import java.util.List;

public class DicomCompressor {
    /**
     * DICOMファイルを圧縮（JPEG/JPEG2000）
     */
    public Path compressFile(Path sourceFile, Path destFile, 
                             CompressionType type, 
                             CompressionQuality quality) throws Exception {
        try (DicomInputStream dis = new DicomInputStream(Files.newInputStream(sourceFile))) {
            Attributes dataset = dis.readDataset(-1, -1);
            
            // 圧縮処理
            String transferSyntaxUID = getTransferSyntaxUID(type, quality);
            Attributes compressedDataset = compressDataset(dataset, transferSyntaxUID);
            
            // 圧縮されたファイルを保存
            try (DicomOutputStream dos = new DicomOutputStream(
                    Files.newOutputStream(destFile))) {
                dos.writeDataset(compressedDataset.createFileMetaInformation(
                    transferSyntaxUID), compressedDataset);
            }
        }
        
        return destFile;
    }
    
    /**
     * DICOMファイルを展開
     */
    public Path decompressFile(Path sourceFile, Path destFile) throws Exception {
        try (DicomInputStream dis = new DicomInputStream(Files.newInputStream(sourceFile))) {
            Attributes dataset = dis.readDataset(-1, -1);
            
            // 展開処理
            String transferSyntaxUID = UID.ExplicitVRLittleEndian;
            Attributes decompressedDataset = decompressDataset(dataset, transferSyntaxUID);
            
            // 展開されたファイルを保存
            try (DicomOutputStream dos = new DicomOutputStream(
                    Files.newOutputStream(destFile))) {
                dos.writeDataset(decompressedDataset.createFileMetaInformation(
                    transferSyntaxUID), decompressedDataset);
            }
        }
        
        return destFile;
    }
    
    private String getTransferSyntaxUID(CompressionType type, CompressionQuality quality) {
        switch (type) {
            case JPEG:
                switch (quality) {
                    case LOSSLESS:
                        return UID.JPEGLossless;
                    case HIGH:
                        return UID.JPEGBaseline1;
                    case MEDIUM:
                        return UID.JPEGExtended24;
                    case LOW:
                        return UID.JPEGBaseline8Bit;
                }
                break;
            case JPEG2000:
                switch (quality) {
                    case LOSSLESS:
                        return UID.JPEG2000LosslessOnly;
                    case HIGH:
                        return UID.JPEG2000;
                    case MEDIUM:
                        return UID.JPEG2000;
                    case LOW:
                        return UID.JPEG2000;
                }
                break;
        }
        return UID.ExplicitVRLittleEndian;
    }
    
    private Attributes compressDataset(Attributes dataset, String transferSyntaxUID) throws Exception {
        // dcm4cheのImageWriterを使用して圧縮
        // 実装は省略
        return dataset;
    }
    
    private Attributes decompressDataset(Attributes dataset, String transferSyntaxUID) throws Exception {
        // dcm4cheのImageReaderを使用して展開
        // 実装は省略
        return dataset;
    }
    
    public enum CompressionType {
        JPEG, JPEG2000
    }
    
    public enum CompressionQuality {
        LOSSLESS, HIGH, MEDIUM, LOW
    }
}
```

## 7. その他の機能

### 7.1 ヒストグラム機能

**HOROS実装** (`HistogramWindow.h`, `HistoView.h`):
- 画像のヒストグラム表示
- 統計情報の表示

**Java実装**:
```java
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class HistogramPanel extends JPanel {
    private int[] histogram;
    private int maxCount;
    
    public void calculateHistogram(BufferedImage image) {
        histogram = new int[256];
        maxCount = 0;
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int gray = new Color(image.getRGB(x, y)).getRed();
                histogram[gray]++;
                maxCount = Math.max(maxCount, histogram[gray]);
            }
        }
        
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        if (histogram == null) {
            return;
        }
        
        int width = getWidth();
        int height = getHeight();
        
        g2d.setColor(Color.BLACK);
        for (int i = 0; i < 256; i++) {
            int barHeight = (int)((double)histogram[i] / maxCount * height);
            int x = i * width / 256;
            g2d.drawLine(x, height, x, height - barHeight);
        }
    }
}
```

### 6.2 ログ機能

**HOROS実装** (`LogManager.h`):
- ネットワーク通信のログ管理
- ログの表示・エクスポート

**Java実装**:
```java
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DicomLogManager {
    private List<LogEntry> logs;
    private int maxLogEntries = 10000;
    
    public void addLog(LogLevel level, String category, String message) {
        LogEntry entry = new LogEntry(LocalDateTime.now(), level, category, message);
        logs.add(entry);
        
        if (logs.size() > maxLogEntries) {
            logs.remove(0);
        }
    }
    
    public List<LogEntry> getLogs(LocalDateTime from, LocalDateTime to) {
        return logs.stream()
            .filter(log -> !log.getTimestamp().isBefore(from) && !log.getTimestamp().isAfter(to))
            .collect(Collectors.toList());
    }
    
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
    
    public static class LogEntry {
        private LocalDateTime timestamp;
        private LogLevel level;
        private String category;
        private String message;
        
        // Getters and setters
    }
}
```

## 8. 実装の優先順位

### 8.1 高優先度

1. **圧縮/展開機能**: CD/DVD作成時に必要
2. **匿名化機能**: CD/DVD作成時に必要
3. **ログ機能**: デバッグと運用に必要

### 8.2 中優先度

4. **レポート機能**: 基本的なレポート生成
5. **印刷機能**: DICOM Print SCU
6. **WADO機能**: Web経由での画像取得

### 8.3 低優先度（オプション）

7. **ヒストグラム機能**: 画像解析の補助
8. **Structured Report**: 高度なレポート機能
9. **FlyThru機能**: 3D画像の動画生成（MPR/3D機能に含まれる）
10. **Key Image機能**: キー画像のマーキング
11. **Hanging Protocol機能**: 表示レイアウトの自動設定

## 9. 実装順序

1. **ログ機能**: 基本的なログ管理
2. **圧縮/展開機能**: CD/DVD作成機能と連携
3. **匿名化機能**: CD/DVD作成機能と連携
4. **レポート機能**: 基本的なレポート生成
5. **印刷機能**: DICOM Print SCU
6. **WADO機能**: Web経由での画像取得
7. **ヒストグラム機能**: 画像解析の補助

## 10. 参考資料

- HOROSソースコード:
  - `DicomCompressor.h/m`: 圧縮/展開機能
  - `Anonymization.h/m`: 匿名化機能
  - `Reports.h/m`: レポート機能
  - `StructuredReport.h/m`: Structured Report機能
  - `DCMTKPrintSCU.h/m`: 印刷機能
  - `WADODownload.h/m`: WADO機能
  - `HistogramWindow.h/m`: ヒストグラム機能
  - `FlyThruController.h/m`: FlyThru機能
  - `KeyObjectController.h/m`: Key Image機能
  - `HangingProtocolController.h/m`: Hanging Protocol機能
- DICOM標準: 
  - Part 3 (Information Object Definitions)
  - Part 4 (Service Class Specifications)
  - Part 10 (Media Storage and File Format)
  - Part 11 (Media Storage Application Profiles)

