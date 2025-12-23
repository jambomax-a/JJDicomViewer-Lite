# dcm4che-5.34.1 使用ガイド

## 1. 概要

このドキュメントは、dcm4che-5.34.1ライブラリを使用したDICOM処理の実装ガイドです。

## 2. 依存関係の設定

### 2.1 Gradle設定

```kotlin
dependencies {
    // DICOM processing
    implementation("org.dcm4che:dcm4che-core:5.34.1")
    implementation("org.dcm4che:dcm4che-image:5.34.1")
    implementation("org.dcm4che:dcm4che-imageio:5.34.1")
    implementation("org.dcm4che:dcm4che-net:5.34.1")
    
    // 必要に応じて
    implementation("org.dcm4che:dcm4che-imageio-opencv:5.34.1")
    implementation("org.dcm4che:dcm4che-imageio-rle:5.34.1")
}
```

### 2.2 Maven設定

```xml
<dependencies>
    <dependency>
        <groupId>org.dcm4che</groupId>
        <artifactId>dcm4che-core</artifactId>
        <version>5.34.1</version>
    </dependency>
    <dependency>
        <groupId>org.dcm4che</groupId>
        <artifactId>dcm4che-image</artifactId>
        <version>5.34.1</version>
    </dependency>
    <dependency>
        <groupId>org.dcm4che</groupId>
        <artifactId>dcm4che-imageio</artifactId>
        <version>5.34.1</version>
    </dependency>
    <dependency>
        <groupId>org.dcm4che</groupId>
        <artifactId>dcm4che-net</artifactId>
        <version>5.34.1</version>
    </dependency>
</dependencies>
```

## 3. DICOMファイルの読み込み

### 3.1 基本的な読み込み

```java
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;

public class DicomReader {
    public Attributes readDicomFile(Path file) throws IOException {
        try (DicomInputStream dis = new DicomInputStream(file.toFile())) {
            return dis.readDataset(-1, -1);
        }
    }
    
    public String getPatientName(Attributes attrs) {
        return attrs.getString(Tag.PatientName);
    }
    
    public int getWidth(Attributes attrs) {
        return attrs.getInt(Tag.Columns, 0);
    }
    
    public int getHeight(Attributes attrs) {
        return attrs.getInt(Tag.Rows, 0);
    }
}
```

### 3.2 ピクセルデータの読み込み

```java
import org.dcm4che3.imageio.codec.ImageReaderFactory;
import org.dcm4che3.imageio.codec.ImageWriterFactory;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class DicomImageReader {
    static {
        // ImageIOプラグインの登録
        ImageReaderFactory.registerImageReaders();
    }
    
    public BufferedImage readImage(Path dicomFile) throws IOException {
        BufferedImage image = ImageIO.read(dicomFile.toFile());
        return image;
    }
    
    public BufferedImage readImageWithWindowLevel(Path dicomFile, 
                                                   double windowCenter,
                                                   double windowWidth) 
            throws IOException {
        // Window Level/Widthを適用した画像読み込み
        // 注意: dcm4che-imageioは自動的にWindow Level/Widthを適用します
        return ImageIO.read(dicomFile.toFile());
    }
}
```

## 4. DICOMタグの操作

### 4.1 タグの読み取り

```java
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;

public class TagReader {
    public void readTags(Attributes attrs) {
        // 文字列タグ
        String patientName = attrs.getString(Tag.PatientName);
        String patientID = attrs.getString(Tag.PatientID);
        
        // 数値タグ
        int width = attrs.getInt(Tag.Columns, 0);
        int height = attrs.getInt(Tag.Rows, 0);
        
        // 日付タグ
        String studyDate = attrs.getString(Tag.StudyDate);
        
        // 配列タグ
        double[] pixelSpacing = attrs.getDoubles(Tag.PixelSpacing);
        if (pixelSpacing != null && pixelSpacing.length >= 2) {
            double spacingX = pixelSpacing[0];
            double spacingY = pixelSpacing[1];
        }
        
        // シーケンスタグ
        Sequence imageOrientation = attrs.getSequence(Tag.ImageOrientationPatient);
        if (imageOrientation != null && !imageOrientation.isEmpty()) {
            Attributes item = imageOrientation.get(0);
            double[] orientation = item.getDoubles(Tag.ImageOrientationPatient);
        }
    }
}
```

### 4.2 タグの書き込み

```java
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;

public class TagWriter {
    public void writeTags(Attributes attrs) {
        // 文字列タグ
        attrs.setString(Tag.PatientName, VR.PN, "DOE^JOHN");
        attrs.setString(Tag.PatientID, VR.LO, "12345");
        
        // 数値タグ
        attrs.setInt(Tag.Columns, VR.US, 512);
        attrs.setInt(Tag.Rows, VR.US, 512);
        
        // 日付タグ
        attrs.setString(Tag.StudyDate, VR.DA, "20240101");
        
        // 配列タグ
        attrs.setDoubles(Tag.PixelSpacing, VR.DS, 
            new double[]{0.5, 0.5});
    }
}
```

## 5. DICOMファイルの書き込み

### 5.1 基本的な書き込み

```java
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.UID;

public class DicomWriter {
    public void writeDicomFile(Attributes attrs, Path outputFile) 
            throws IOException {
        try (DicomOutputStream dos = new DicomOutputStream(outputFile.toFile())) {
            dos.writeDataset(null, attrs);
        }
    }
    
    public void writeDicomFileWithTransferSyntax(Attributes attrs, 
                                                 Path outputFile,
                                                 String transferSyntax) 
            throws IOException {
        try (DicomOutputStream dos = new DicomOutputStream(outputFile.toFile())) {
            dos.setTransferSyntax(transferSyntax);
            dos.writeDataset(null, attrs);
        }
    }
}
```

## 6. ネットワーク通信

### 6.1 アソシエーションの確立

```java
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.*;

public class AssociationExample {
    public Association connect(String callingAET, String calledAET,
                              String hostname, int port) throws Exception {
        Device device = new Device("device");
        
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
        
        return ae.connect(remoteAE, null);
    }
}
```

### 6.2 C-STORE実装

```java
import org.dcm4che3.net.*;
import org.dcm4che3.net.service.*;
import org.dcm4che3.data.*;

public class StoreSCU {
    public void store(Association as, Attributes attrs, String ts) 
            throws Exception {
        String sopClassUID = attrs.getString(Tag.SOPClassUID);
        String sopInstanceUID = attrs.getString(Tag.SOPInstanceUID);
        
        DimseRSP rsp = as.cstore(sopClassUID, sopInstanceUID, attrs, ts, null);
        
        int status = rsp.getCommand().getInt(Tag.Status);
        if (status != 0) {
            throw new Exception("C-STORE failed with status: " + status);
        }
    }
}
```

### 6.3 C-FIND実装

```java
public class FindSCU {
    public List<Attributes> find(Association as, Attributes keys) 
            throws Exception {
        List<Attributes> results = new ArrayList<>();
        
        DimseRSP rsp = as.cfind(
            UID.StudyRootQueryRetrieveInformationModelFIND,
            UID.ImplicitVRLittleEndian, keys, null);
        
        while (rsp.next()) {
            Attributes attrs = rsp.getDataset();
            if (attrs != null) {
                results.add(attrs);
            }
        }
        
        return results;
    }
}
```

## 7. 画像処理

### 7.1 Window Level/Widthの適用

```java
import org.dcm4che3.imageio.codec.ImageReaderFactory;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class WindowLevelProcessor {
    static {
        ImageReaderFactory.registerImageReaders();
    }
    
    public BufferedImage applyWindowLevel(Path dicomFile,
                                         double windowCenter,
                                         double windowWidth) 
            throws IOException {
        // dcm4che-imageioは自動的にWindow Level/Widthを適用
        // カスタムWL/WWを適用する場合は、Attributesを変更
        Attributes attrs = readAttributes(dicomFile);
        attrs.setDoubles(Tag.WindowCenter, VR.DS, 
            new double[]{windowCenter});
        attrs.setDoubles(Tag.WindowWidth, VR.DS, 
            new double[]{windowWidth});
        
        // 一時ファイルに書き込んで読み込み
        Path tempFile = Files.createTempFile("dicom", ".dcm");
        writeAttributes(attrs, tempFile);
        
        try {
            return ImageIO.read(tempFile.toFile());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
```

### 7.2 カラーマッピング

```java
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;

public class ColorMapping {
    public void applyColorLUT(Attributes attrs, byte[] redLUT, 
                             byte[] greenLUT, byte[] blueLUT) {
        // Palette Color LUTの設定
        attrs.setBytes(Tag.RedPaletteColorLookupTableDescriptor, VR.US,
            new byte[]{(byte) 256, 0, 16}); // Length, First Value, Bits
        attrs.setBytes(Tag.RedPaletteColorLookupTableData, VR.OW, redLUT);
        
        attrs.setBytes(Tag.GreenPaletteColorLookupTableDescriptor, VR.US,
            new byte[]{(byte) 256, 0, 16});
        attrs.setBytes(Tag.GreenPaletteColorLookupTableData, VR.OW, greenLUT);
        
        attrs.setBytes(Tag.BluePaletteColorLookupTableDescriptor, VR.US,
            new byte[]{(byte) 256, 0, 16});
        attrs.setBytes(Tag.BluePaletteColorLookupTableData, VR.OW, blueLUT);
        
        attrs.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
    }
}
```

## 8. 転送構文

### 8.1 転送構文の確認

```java
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;

public class TransferSyntaxChecker {
    public boolean isCompressed(Attributes attrs) {
        String ts = attrs.getString(Tag.TransferSyntaxUID);
        return ts != null && (
            ts.contains("JPEG") ||
            ts.contains("RLE") ||
            ts.contains("JPEG2000") ||
            ts.contains("JPEG-LS")
        );
    }
    
    public String getTransferSyntax(Attributes attrs) {
        return attrs.getString(Tag.TransferSyntaxUID);
    }
}
```

### 8.2 転送構文の変換

```java
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;

public class TransferSyntaxConverter {
    public void convert(Path inputFile, Path outputFile, 
                      String targetTransferSyntax) 
            throws IOException {
        try (DicomInputStream dis = new DicomInputStream(inputFile.toFile());
             DicomOutputStream dos = new DicomOutputStream(outputFile.toFile())) {
            
            Attributes attrs = dis.readDataset(-1, -1);
            dos.setTransferSyntax(targetTransferSyntax);
            dos.writeDataset(null, attrs);
        }
    }
}
```

## 9. エラーハンドリング

```java
import org.dcm4che3.net.DimseRSP;
import org.dcm4che3.data.Tag;

public class ErrorHandler {
    public void checkResponse(DimseRSP rsp) throws Exception {
        int status = rsp.getCommand().getInt(Tag.Status);
        
        if (status != 0) {
            String errorComment = rsp.getCommand().getString(Tag.ErrorComment);
            throw new Exception("DICOM operation failed: " + 
                status + " - " + errorComment);
        }
    }
}
```

## 10. パフォーマンス最適化

### 10.1 ストリーミング読み込み

```java
import org.dcm4che3.io.DicomInputStream;

public class StreamingReader {
    public void readLargeFile(Path file) throws IOException {
        try (DicomInputStream dis = new DicomInputStream(file.toFile())) {
            // ピクセルデータをスキップしてメタデータのみ読み込み
            dis.setIncludePixelData(false);
            Attributes attrs = dis.readDataset(-1, -1);
            
            // メタデータ処理
            processMetadata(attrs);
        }
    }
}
```

### 10.2 バッチ処理

```java
public class BatchProcessor {
    public void processFiles(List<Path> files, int batchSize) {
        for (int i = 0; i < files.size(); i += batchSize) {
            List<Path> batch = files.subList(i, 
                Math.min(i + batchSize, files.size()));
            
            processBatch(batch);
        }
    }
    
    private void processBatch(List<Path> batch) {
        // バッチ処理
    }
}
```

## 11. まとめ

dcm4che-5.34.1を使用することで：

1. **DICOMファイルの読み書き**が簡単
2. **ネットワーク通信**（C-STORE、C-FIND、C-MOVE）が実装可能
3. **画像処理**（Window Level/Width、カラーマッピング）がサポート
4. **転送構文**の変換が可能
5. **パフォーマンス最適化**の機能が豊富

HOROSの機能をJavaで実装する際の強力な基盤となります。

