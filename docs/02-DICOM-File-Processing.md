# DICOMファイル処理 - Java移植ドキュメント

## 1. 概要

このドキュメントは、HOROSの`DicomFile`クラスを解析し、JavaでのDICOMファイル処理実装方法をまとめたものです。

## 2. DicomFileクラスの解析

### 2.1 クラス定義

**HOROS実装** (`DicomFile.h`):
```objective-c
@interface DicomFile: NSObject
{
    NSString            *name;
    NSString            *study;
    NSString            *serie;
    NSString            *filePath, *fileType;
    NSString            *Modality;
    NSString            *SOPUID;
    NSString            *imageType;
    
    NSString            *studyID;
    NSString            *serieID;
    NSString            *imageID;
    NSString            *patientID;
    NSString            *studyIDs;
    NSString            *seriesNo;
    NSDate              *date;
    
    long                width, height;
    long                NoOfFrames;
    long                NoOfSeries;
    
    NSMutableDictionary *dicomElements;
}
```

### 2.2 主要メソッド

#### 2.2.1 ファイル形式判定

```objective-c
+ (BOOL) isDICOMFile:(NSString *) file;
+ (BOOL) isDICOMFile:(NSString *) file compressed:(BOOL*) compressed;
+ (BOOL) isTiffFile:(NSString *) file;
+ (BOOL) isFVTiffFile:(NSString *) file;
+ (BOOL) isNIfTIFile:(NSString *) file;
```

**実装ロジック**:
1. ファイルヘッダーを読み込み
2. DICOMプリアンブル（128バイト）の確認
3. "DICM"マジックナンバーの確認
4. 転送構文の判定

**Java実装**:
```java
public class DicomFile {
    public static boolean isDicomFile(Path file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            // 128バイトのプリアンブルをスキップ
            raf.seek(128);
            byte[] magic = new byte[4];
            raf.readFully(magic);
            return new String(magic, StandardCharsets.US_ASCII).equals("DICM");
        }
    }
    
    public static boolean isDicomFile(Path file, AtomicBoolean compressed) 
            throws IOException {
        if (!isDicomFile(file)) {
            return false;
        }
        
        // 転送構文を確認して圧縮判定
        try (DicomInputStream dis = new DicomInputStream(file.toFile())) {
            Attributes attrs = dis.readDataset(-1, -1);
            String ts = attrs.getString(Tag.TransferSyntaxUID);
            compressed.set(isCompressed(ts));
        }
        return true;
    }
    
    private static boolean isCompressed(String transferSyntax) {
        return transferSyntax != null && 
               (transferSyntax.contains("JPEG") || 
                transferSyntax.contains("RLE") ||
                transferSyntax.contains("JPEG2000"));
    }
}
```

#### 2.2.2 DICOMファイル解析

```objective-c
- (short) getDicomFile;
```

**実装ロジック**:
1. DCMTKまたはPapyrusを使用してDICOMファイルを読み込み
2. 必要なDICOMタグを抽出
3. `dicomElements`辞書に格納

**主要な抽出タグ**:
- Patient ID (0010,0020)
- Patient Name (0010,0010)
- Study Instance UID (0020,000D)
- Series Instance UID (0020,000E)
- SOP Instance UID (0008,0018)
- Modality (0008,0060)
- Study Date (0008,0020)
- Series Number (0020,0011)
- Instance Number (0020,0013)
- Image Type (0008,0008)
- Width (0028,0011)
- Height (0028,0010)
- Number of Frames (0028,0008)

**Java実装**:
```java
public class DicomFile {
    private Attributes attributes;
    private Map<String, Object> dicomElements;
    
    public void parseDicomFile(Path file) throws IOException {
        try (DicomInputStream dis = new DicomInputStream(file.toFile())) {
            attributes = dis.readDataset(-1, -1);
            extractDicomElements();
        }
    }
    
    private void extractDicomElements() {
        dicomElements = new HashMap<>();
        
        // Patient情報
        dicomElements.put("patientID", getString(Tag.PatientID));
        dicomElements.put("patientName", getString(Tag.PatientName));
        dicomElements.put("patientUID", getString(Tag.PatientID)); // カスタム
        
        // Study情報
        dicomElements.put("studyInstanceUID", getString(Tag.StudyInstanceUID));
        dicomElements.put("studyID", getString(Tag.StudyID));
        dicomElements.put("studyDate", getString(Tag.StudyDate));
        dicomElements.put("studyDescription", getString(Tag.StudyDescription));
        dicomElements.put("accessionNumber", getString(Tag.AccessionNumber));
        
        // Series情報
        dicomElements.put("seriesInstanceUID", getString(Tag.SeriesInstanceUID));
        dicomElements.put("seriesNumber", getString(Tag.SeriesNumber));
        dicomElements.put("seriesDescription", getString(Tag.SeriesDescription));
        dicomElements.put("modality", getString(Tag.Modality));
        
        // Image情報
        dicomElements.put("sopInstanceUID", getString(Tag.SOPInstanceUID));
        dicomElements.put("sopClassUID", getString(Tag.SOPClassUID));
        dicomElements.put("instanceNumber", getString(Tag.InstanceNumber));
        dicomElements.put("imageType", getString(Tag.ImageType));
        
        // 画像サイズ
        dicomElements.put("width", attributes.getInt(Tag.Columns, 0));
        dicomElements.put("height", attributes.getInt(Tag.Rows, 0));
        dicomElements.put("numberOfFrames", 
            attributes.getInt(Tag.NumberOfFrames, 1));
        
        // ピクセル情報
        dicomElements.put("bitsAllocated", 
            attributes.getInt(Tag.BitsAllocated, 16));
        dicomElements.put("bitsStored", 
            attributes.getInt(Tag.BitsStored, 16));
        dicomElements.put("pixelRepresentation", 
            attributes.getInt(Tag.PixelRepresentation, 0));
        dicomElements.put("rescaleSlope", 
            attributes.getDouble(Tag.RescaleSlope, 1.0));
        dicomElements.put("rescaleIntercept", 
            attributes.getDouble(Tag.RescaleIntercept, 0.0));
        
        // 空間情報
        double[] pixelSpacing = attributes.getDoubles(Tag.PixelSpacing);
        if (pixelSpacing != null && pixelSpacing.length >= 2) {
            dicomElements.put("pixelSpacingX", pixelSpacing[0]);
            dicomElements.put("pixelSpacingY", pixelSpacing[1]);
        }
        
        double[] imagePosition = attributes.getDoubles(Tag.ImagePositionPatient);
        if (imagePosition != null && imagePosition.length >= 3) {
            dicomElements.put("originX", imagePosition[0]);
            dicomElements.put("originY", imagePosition[1]);
            dicomElements.put("originZ", imagePosition[2]);
        }
        
        double[] imageOrientation = attributes.getDoubles(Tag.ImageOrientationPatient);
        if (imageOrientation != null && imageOrientation.length >= 6) {
            dicomElements.put("orientation", imageOrientation);
        }
        
        // Window Level/Width
        double[] windowCenter = attributes.getDoubles(Tag.WindowCenter);
        double[] windowWidth = attributes.getDoubles(Tag.WindowWidth);
        if (windowCenter != null && windowCenter.length > 0) {
            dicomElements.put("windowLevel", windowCenter[0]);
        }
        if (windowWidth != null && windowWidth.length > 0) {
            dicomElements.put("windowWidth", windowWidth[0]);
        }
    }
    
    private String getString(int tag) {
        return attributes.getString(tag);
    }
    
    public Map<String, Object> getDicomElements() {
        return Collections.unmodifiableMap(dicomElements);
    }
}
```

#### 2.2.3 ファイル名からの情報抽出

```objective-c
- (void)extractSeriesStudyImageNumbersFromFileName:(NSString *)tempString;
```

**実装ロジック**:
- 非DICOMファイル（TIFF、JPEG等）の場合、ファイル名から番号を抽出
- ファイル名の末尾の数字を画像番号として使用
- 残りの部分をシリーズ/スタディIDとして使用

**Java実装**:
```java
public class DicomFile {
    public void extractNumbersFromFileName(String fileName) {
        // ファイル名から数字を抽出
        Pattern pattern = Pattern.compile("(.*?)(\\d+)(\\.[^.]+)?$");
        Matcher matcher = pattern.matcher(fileName);
        
        if (matcher.matches()) {
            String baseName = matcher.group(1);
            String numberStr = matcher.group(2);
            
            this.imageID = numberStr;
            this.serieID = baseName;
            this.studyID = baseName; // デフォルト
        }
    }
}
```

## 3. 非DICOM形式のサポート

### 3.1 TIFFファイル

```objective-c
+ (BOOL) isTiffFile:(NSString *) file;
```

**Java実装**:
```java
public static boolean isTiffFile(Path file) {
    try {
        ImageIO.read(file.toFile());
        return file.toString().toLowerCase().endsWith(".tif") ||
               file.toString().toLowerCase().endsWith(".tiff");
    } catch (IOException e) {
        return false;
    }
}
```

### 3.2 NIfTIファイル

```objective-c
+ (BOOL) isNIfTIFile:(NSString *) file;
- (short) getNIfTI;
```

**Java実装**:
```java
// NIfTIライブラリが必要（例: niftijio）
public static boolean isNiftiFile(Path file) {
    String name = file.toString().toLowerCase();
    return name.endsWith(".nii") || name.endsWith(".nii.gz");
}
```

## 4. 文字エンコーディング処理

### 4.1 不正文字の置換

```objective-c
+ (NSString*) NSreplaceBadCharacter: (NSString*) str;
+ (char *) replaceBadCharacter:(char *) str encoding: (NSStringEncoding) encoding;
```

**置換ルール**:
- `^` → スペース
- `/` → `-`
- `\r`, `\n` → 削除
- `:` → `-`
- 末尾のスペースを削除

**Java実装**:
```java
public static String replaceBadCharacters(String str) {
    if (str == null) return null;
    
    return str
        .replace('^', ' ')
        .replace('/', '-')
        .replace('\r', "")
        .replace('\n', "")
        .replace(':', '-')
        .replaceAll("\\s+$", ""); // 末尾のスペース削除
}
```

## 5. データベースインポート用要素

### 5.1 dicomElements辞書の構造

HOROSでは、データベースインポート用に以下のキーを使用：

```objective-c
- (NSMutableDictionary *)dicomElements;
```

**主要キー**:
- `studyComment`
- `studyID`
- `studyDescription`
- `studyDate`
- `modality`
- `patientID`
- `patientName`
- `patientUID`
- `fileType`
- `commentsAutoFill`
- `album`
- `SOPClassUID`
- `SOPUID`
- `institutionName`
- `referringPhysiciansName`
- `performingPhysiciansName`
- `accessionNumber`
- `patientAge`
- `patientBirthDate`
- `patientSex`
- `cardiacTime`
- `protocolName`
- `sliceLocation`
- `imageID`
- `seriesNumber`
- `seriesDICOMUID`
- `studyNumber`
- `seriesID`
- `hasDICOM`

**Java実装**:
```java
public Map<String, Object> getImportElements() {
    Map<String, Object> elements = new HashMap<>();
    
    // Study
    elements.put("studyID", getString(Tag.StudyID));
    elements.put("studyDescription", getString(Tag.StudyDescription));
    elements.put("studyDate", getString(Tag.StudyDate));
    elements.put("studyInstanceUID", getString(Tag.StudyInstanceUID));
    
    // Patient
    elements.put("patientID", getString(Tag.PatientID));
    elements.put("patientName", getString(Tag.PatientName));
    elements.put("patientUID", generatePatientUID());
    elements.put("patientAge", getString(Tag.PatientAge));
    elements.put("patientBirthDate", getString(Tag.PatientBirthDate));
    elements.put("patientSex", getString(Tag.PatientSex));
    
    // Series
    elements.put("seriesID", generateSeriesID());
    elements.put("seriesNumber", getString(Tag.SeriesNumber));
    elements.put("seriesDICOMUID", getString(Tag.SeriesInstanceUID));
    elements.put("seriesDescription", getString(Tag.SeriesDescription));
    
    // Image
    elements.put("imageID", getString(Tag.InstanceNumber));
    elements.put("sopInstanceUID", getString(Tag.SOPInstanceUID));
    elements.put("sopClassUID", getString(Tag.SOPClassUID));
    
    // Modality
    elements.put("modality", getString(Tag.Modality));
    
    // Other
    elements.put("accessionNumber", getString(Tag.AccessionNumber));
    elements.put("institutionName", getString(Tag.InstitutionName));
    elements.put("referringPhysiciansName", 
        getString(Tag.ReferringPhysicianName));
    elements.put("performingPhysiciansName", 
        getString(Tag.PerformingPhysicianName));
    elements.put("protocolName", getString(Tag.ProtocolName));
    
    elements.put("hasDICOM", true);
    elements.put("fileType", "DICOM");
    
    return elements;
}

private String generatePatientUID() {
    // HOROSのロジックに基づくUID生成
    String patientID = getString(Tag.PatientID);
    String patientName = getString(Tag.PatientName);
    String birthDate = getString(Tag.PatientBirthDate);
    
    // カスタムUID生成ロジック
    return UUID.randomUUID().toString();
}
```

## 6. エラーハンドリング

### 6.1 ファイル読み込みエラー

```java
public class DicomFileException extends Exception {
    public DicomFileException(String message) {
        super(message);
    }
    
    public DicomFileException(String message, Throwable cause) {
        super(message, cause);
    }
}

public void parseDicomFile(Path file) throws DicomFileException {
    try {
        // パース処理
    } catch (IOException e) {
        throw new DicomFileException("Failed to read DICOM file: " + file, e);
    } catch (Exception e) {
        throw new DicomFileException("Failed to parse DICOM file: " + file, e);
    }
}
```

## 7. パフォーマンス最適化

### 7.1 メタデータのみ読み込み

大きなDICOMファイルの場合、ピクセルデータを読み込まずにメタデータのみを読み込む：

```java
public void parseMetadataOnly(Path file) throws IOException {
    try (DicomInputStream dis = new DicomInputStream(file.toFile())) {
        // ピクセルデータをスキップ
        dis.setIncludePixelData(false);
        attributes = dis.readDataset(-1, -1);
        extractDicomElements();
    }
}
```

### 7.2 キャッシング

一度読み込んだDICOM要素をキャッシュ：

```java
public class DicomFileCache {
    private static final Map<Path, Map<String, Object>> cache = 
        new ConcurrentHashMap<>();
    
    public static Map<String, Object> getCachedElements(Path file) {
        return cache.get(file);
    }
    
    public static void cacheElements(Path file, Map<String, Object> elements) {
        cache.put(file, elements);
    }
}
```

## 8. dcm4che使用例

### 8.1 基本的な読み込み

```java
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;

public class DicomFileReader {
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

## 9. まとめ

DICOMファイル処理のJava実装では：

1. **dcm4che**を使用してDICOMファイルを読み込む
2. 必要なDICOMタグを抽出してMapに格納
3. データベースインポート用の形式に変換
4. エラーハンドリングとパフォーマンス最適化を考慮

次のステップ: 画像データの読み込みと表示（`DCMPix`の実装）

