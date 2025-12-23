# データベース管理 - Java移植ドキュメント

## 1. 概要

このドキュメントは、HOROSの`DicomDatabase`クラスを解析し、Javaでのデータベース管理実装方法をまとめたものです。

**注意**: 
- HOROSはCore Dataを使用していますが、Java実装ではSQLiteを使用します
- HOROS/OsirixのDB共通性によるインポート・エクスポート連携機能は実装しません（Vet-Systemとの連携が主目的のため）

## 2. DicomDatabaseクラスの解析

### 2.1 クラス定義

**HOROS実装** (`DicomDatabase.h`):
```objective-c
@interface DicomDatabase : N2ManagedDatabase {
    NSString* _baseDirPath;
    NSString* _dataBaseDirPath;
    NSString* _name;
    
    NSRecursiveLock* _processFilesLock;
    NSRecursiveLock* _importFilesFromIncomingDirLock;
}
```

### 2.2 データモデル

HOROSはCore Dataを使用していますが、Java実装ではSQLiteを直接使用します。

**エンティティ階層**:
```
DicomStudy (スタディ)
  └── DicomSeries (シリーズ)
      └── DicomImage (画像)
```

## 3. データベーススキーマ

### 3.1 テーブル定義

```sql
-- スタディテーブル
CREATE TABLE dicom_study (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    study_instance_uid TEXT UNIQUE NOT NULL,
    patient_id TEXT,
    patient_name TEXT,
    patient_uid TEXT,
    study_date TEXT,
    study_description TEXT,
    modality TEXT,
    accession_number TEXT,
    institution_name TEXT,
    referring_physician_name TEXT,
    performing_physician_name TEXT,
    patient_birth_date TEXT,
    patient_sex TEXT,
    patient_age TEXT,
    number_of_images INTEGER DEFAULT 0,
    date_added TEXT,
    date_opened TEXT,
    comment TEXT,
    locked INTEGER DEFAULT 0,
    hidden INTEGER DEFAULT 0,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- シリーズテーブル
CREATE TABLE dicom_series (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    series_instance_uid TEXT UNIQUE NOT NULL,
    study_id INTEGER NOT NULL,
    series_number TEXT,
    series_description TEXT,
    modality TEXT,
    number_of_images INTEGER DEFAULT 0,
    window_level REAL,
    window_width REAL,
    date_added TEXT,
    date_opened TEXT,
    comment TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (study_id) REFERENCES dicom_study(id) ON DELETE CASCADE
);

-- 画像テーブル
CREATE TABLE dicom_image (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sop_instance_uid TEXT UNIQUE NOT NULL,
    series_id INTEGER NOT NULL,
    instance_number INTEGER,
    path_string TEXT NOT NULL,
    stored_extension TEXT,
    width INTEGER,
    height INTEGER,
    number_of_frames INTEGER DEFAULT 1,
    slice_location REAL,
    window_level REAL,
    window_width REAL,
    is_key_image INTEGER DEFAULT 0,
    date_added TEXT,
    comment TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (series_id) REFERENCES dicom_series(id) ON DELETE CASCADE
);

-- インデックス
CREATE INDEX idx_study_patient_id ON dicom_study(patient_id);
CREATE INDEX idx_study_study_instance_uid ON dicom_study(study_instance_uid);
CREATE INDEX idx_series_study_id ON dicom_series(study_id);
CREATE INDEX idx_series_series_instance_uid ON dicom_series(series_instance_uid);
CREATE INDEX idx_image_series_id ON dicom_image(series_id);
CREATE INDEX idx_image_sop_instance_uid ON dicom_image(sop_instance_uid);
```

## 4. Java実装

### 4.1 データベース接続管理

```java
import java.sql.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DicomDatabase {
    private Connection connection;
    private Path databasePath;
    private Path baseDirPath;
    private Path dataDirPath;
    
    public DicomDatabase(Path baseDir) throws SQLException {
        this.baseDirPath = baseDir;
        this.dataDirPath = baseDir.resolve("DATA");
        this.databasePath = baseDir.resolve("database.db");
        
        // ディレクトリ作成
        try {
            Files.createDirectories(dataDirPath);
        } catch (IOException e) {
            throw new SQLException("Failed to create data directory", e);
        }
        
        // データベース接続
        String url = "jdbc:sqlite:" + databasePath.toString();
        connection = DriverManager.getConnection(url);
        
        // テーブル作成
        createTables();
    }
    
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // スタディテーブル
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS dicom_study (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    study_instance_uid TEXT UNIQUE NOT NULL,
                    patient_id TEXT,
                    patient_name TEXT,
                    patient_uid TEXT,
                    study_date TEXT,
                    study_description TEXT,
                    modality TEXT,
                    accession_number TEXT,
                    institution_name TEXT,
                    referring_physician_name TEXT,
                    performing_physician_name TEXT,
                    patient_birth_date TEXT,
                    patient_sex TEXT,
                    patient_age TEXT,
                    number_of_images INTEGER DEFAULT 0,
                    date_added TEXT,
                    date_opened TEXT,
                    comment TEXT,
                    locked INTEGER DEFAULT 0,
                    hidden INTEGER DEFAULT 0,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // シリーズテーブル
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS dicom_series (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    series_instance_uid TEXT UNIQUE NOT NULL,
                    study_id INTEGER NOT NULL,
                    series_number TEXT,
                    series_description TEXT,
                    modality TEXT,
                    number_of_images INTEGER DEFAULT 0,
                    window_level REAL,
                    window_width REAL,
                    date_added TEXT,
                    date_opened TEXT,
                    comment TEXT,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (study_id) REFERENCES dicom_study(id) ON DELETE CASCADE
                )
            """);
            
            // 画像テーブル
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS dicom_image (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sop_instance_uid TEXT UNIQUE NOT NULL,
                    series_id INTEGER NOT NULL,
                    instance_number INTEGER,
                    path_string TEXT NOT NULL,
                    stored_extension TEXT,
                    width INTEGER,
                    height INTEGER,
                    number_of_frames INTEGER DEFAULT 1,
                    slice_location REAL,
                    window_level REAL,
                    window_width REAL,
                    is_key_image INTEGER DEFAULT 0,
                    date_added TEXT,
                    comment TEXT,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (series_id) REFERENCES dicom_series(id) ON DELETE CASCADE
                )
            """);
            
            // インデックス作成
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_study_patient_id ON dicom_study(patient_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_study_study_instance_uid ON dicom_study(study_instance_uid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_series_study_id ON dicom_series(study_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_series_series_instance_uid ON dicom_series(series_instance_uid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_image_series_id ON dicom_image(series_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_image_sop_instance_uid ON dicom_image(sop_instance_uid)");
        }
    }
    
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
```

### 4.2 ファイルインポート

```java
public class DicomDatabase {
    private final Object importLock = new Object();
    
    public void importFiles(List<Path> files) throws SQLException, IOException {
        synchronized (importLock) {
            connection.setAutoCommit(false);
            
            try {
                for (Path file : files) {
                    importFile(file);
                }
                
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
    
    private void importFile(Path file) throws SQLException, IOException {
        // DICOMファイル解析
        DicomFile dicomFile = new DicomFile();
        dicomFile.parseDicomFile(file);
        Map<String, Object> elements = dicomFile.getDicomElements();
        
        // スタディ取得または作成
        String studyInstanceUID = (String) elements.get("studyInstanceUID");
        DicomStudy study = getOrCreateStudy(studyInstanceUID, elements);
        
        // シリーズ取得または作成
        String seriesInstanceUID = (String) elements.get("seriesInstanceUID");
        DicomSeries series = getOrCreateSeries(study.getId(), 
            seriesInstanceUID, elements);
        
        // 画像作成
        String sopInstanceUID = (String) elements.get("sopInstanceUID");
        createImage(series.getId(), sopInstanceUID, file, elements);
        
        // カウント更新
        updateCounts(study.getId(), series.getId());
    }
    
    private DicomStudy getOrCreateStudy(String studyInstanceUID, 
                                       Map<String, Object> elements) 
            throws SQLException {
        // 既存スタディを検索
        DicomStudy study = findStudyByUID(studyInstanceUID);
        
        if (study == null) {
            // 新規作成
            study = createStudy(studyInstanceUID, elements);
        }
        
        return study;
    }
    
    private DicomStudy findStudyByUID(String studyInstanceUID) 
            throws SQLException {
        String sql = "SELECT * FROM dicom_study WHERE study_instance_uid = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, studyInstanceUID);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapStudyFromResultSet(rs);
                }
            }
        }
        
        return null;
    }
    
    private DicomStudy createStudy(String studyInstanceUID, 
                                   Map<String, Object> elements) 
            throws SQLException {
        String sql = """
            INSERT INTO dicom_study (
                study_instance_uid, patient_id, patient_name, patient_uid,
                study_date, study_description, modality, accession_number,
                institution_name, referring_physician_name,
                performing_physician_name, patient_birth_date, patient_sex,
                patient_age, date_added
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, 
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, studyInstanceUID);
            stmt.setString(2, (String) elements.get("patientID"));
            stmt.setString(3, (String) elements.get("patientName"));
            stmt.setString(4, (String) elements.get("patientUID"));
            stmt.setString(5, (String) elements.get("studyDate"));
            stmt.setString(6, (String) elements.get("studyDescription"));
            stmt.setString(7, (String) elements.get("modality"));
            stmt.setString(8, (String) elements.get("accessionNumber"));
            stmt.setString(9, (String) elements.get("institutionName"));
            stmt.setString(10, (String) elements.get("referringPhysiciansName"));
            stmt.setString(11, (String) elements.get("performingPhysiciansName"));
            stmt.setString(12, (String) elements.get("patientBirthDate"));
            stmt.setString(13, (String) elements.get("patientSex"));
            stmt.setString(14, (String) elements.get("patientAge"));
            stmt.setString(15, LocalDateTime.now().toString());
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return findStudyById(id);
                }
            }
        }
        
        throw new SQLException("Failed to create study");
    }
    
    private DicomSeries getOrCreateSeries(int studyId, 
                                         String seriesInstanceUID,
                                         Map<String, Object> elements) 
            throws SQLException {
        DicomSeries series = findSeriesByUID(seriesInstanceUID);
        
        if (series == null) {
            series = createSeries(studyId, seriesInstanceUID, elements);
        }
        
        return series;
    }
    
    private DicomSeries findSeriesByUID(String seriesInstanceUID) 
            throws SQLException {
        String sql = "SELECT * FROM dicom_series WHERE series_instance_uid = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, seriesInstanceUID);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapSeriesFromResultSet(rs);
                }
            }
        }
        
        return null;
    }
    
    private DicomSeries createSeries(int studyId, String seriesInstanceUID,
                                    Map<String, Object> elements) 
            throws SQLException {
        String sql = """
            INSERT INTO dicom_series (
                series_instance_uid, study_id, series_number,
                series_description, modality, date_added
            ) VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, seriesInstanceUID);
            stmt.setInt(2, studyId);
            stmt.setString(3, (String) elements.get("seriesNumber"));
            stmt.setString(4, (String) elements.get("seriesDescription"));
            stmt.setString(5, (String) elements.get("modality"));
            stmt.setString(6, LocalDateTime.now().toString());
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return findSeriesById(id);
                }
            }
        }
        
        throw new SQLException("Failed to create series");
    }
    
    private void createImage(int seriesId, String sopInstanceUID,
                            Path file, Map<String, Object> elements) 
            throws SQLException, IOException {
        // ファイルをデータディレクトリにコピー
        Path destFile = copyFileToDataDir(file, sopInstanceUID);
        
        String sql = """
            INSERT OR IGNORE INTO dicom_image (
                sop_instance_uid, series_id, instance_number,
                path_string, stored_extension, width, height,
                number_of_frames, slice_location, date_added
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, sopInstanceUID);
            stmt.setInt(2, seriesId);
            stmt.setObject(3, elements.get("instanceNumber"));
            stmt.setString(4, destFile.toString());
            stmt.setString(5, getExtension(file));
            stmt.setObject(6, elements.get("width"));
            stmt.setObject(7, elements.get("height"));
            stmt.setInt(8, (Integer) elements.getOrDefault("numberOfFrames", 1));
            stmt.setObject(9, elements.get("sliceLocation"));
            stmt.setString(10, LocalDateTime.now().toString());
            
            stmt.executeUpdate();
        }
    }
    
    private Path copyFileToDataDir(Path sourceFile, String sopInstanceUID) 
            throws IOException {
        String extension = getExtension(sourceFile);
        Path destFile = dataDirPath.resolve(sopInstanceUID + extension);
        Files.copy(sourceFile, destFile, StandardCopyOption.REPLACE_EXISTING);
        return destFile;
    }
    
    private String getExtension(Path file) {
        String name = file.getFileName().toString();
        int lastDot = name.lastIndexOf('.');
        return lastDot >= 0 ? name.substring(lastDot) : "";
    }
    
    private void updateCounts(int studyId, int seriesId) throws SQLException {
        // シリーズの画像数を更新
        String sql1 = """
            UPDATE dicom_series 
            SET number_of_images = (
                SELECT COUNT(*) FROM dicom_image WHERE series_id = ?
            )
            WHERE id = ?
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql1)) {
            stmt.setInt(1, seriesId);
            stmt.setInt(2, seriesId);
            stmt.executeUpdate();
        }
        
        // スタディの画像数を更新
        String sql2 = """
            UPDATE dicom_study 
            SET number_of_images = (
                SELECT SUM(number_of_images) FROM dicom_series WHERE study_id = ?
            )
            WHERE id = ?
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql2)) {
            stmt.setInt(1, studyId);
            stmt.setInt(2, studyId);
            stmt.executeUpdate();
        }
    }
}
```

### 4.3 検索機能

```java
public class DicomDatabase {
    public List<DicomStudy> searchStudies(String patientName, 
                                         String patientID,
                                         String studyDate) 
            throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM dicom_study WHERE 1=1");
        List<Object> params = new ArrayList<>();
        
        if (patientName != null && !patientName.isEmpty()) {
            sql.append(" AND patient_name LIKE ?");
            params.add("%" + patientName + "%");
        }
        
        if (patientID != null && !patientID.isEmpty()) {
            sql.append(" AND patient_id = ?");
            params.add(patientID);
        }
        
        if (studyDate != null && !studyDate.isEmpty()) {
            sql.append(" AND study_date = ?");
            params.add(studyDate);
        }
        
        sql.append(" ORDER BY study_date DESC");
        
        List<DicomStudy> studies = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    studies.add(mapStudyFromResultSet(rs));
                }
            }
        }
        
        return studies;
    }
    
    public List<DicomSeries> getSeriesByStudy(int studyId) throws SQLException {
        String sql = "SELECT * FROM dicom_series WHERE study_id = ? ORDER BY series_number";
        List<DicomSeries> series = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, studyId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    series.add(mapSeriesFromResultSet(rs));
                }
            }
        }
        
        return series;
    }
    
    public List<DicomImage> getImagesBySeries(int seriesId) throws SQLException {
        String sql = "SELECT * FROM dicom_image WHERE series_id = ? ORDER BY instance_number";
        List<DicomImage> images = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, seriesId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    images.add(mapImageFromResultSet(rs));
                }
            }
        }
        
        return images;
    }
}
```

### 4.4 データモデルクラス

```java
public class DicomStudy {
    private int id;
    private String studyInstanceUID;
    private String patientID;
    private String patientName;
    private String patientUID;
    private String studyDate;
    private String studyDescription;
    private String modality;
    private String accessionNumber;
    private int numberOfImages;
    // ... getters and setters
}

public class DicomSeries {
    private int id;
    private String seriesInstanceUID;
    private int studyId;
    private String seriesNumber;
    private String seriesDescription;
    private String modality;
    private int numberOfImages;
    // ... getters and setters
}

public class DicomImage {
    private int id;
    private String sopInstanceUID;
    private int seriesId;
    private Integer instanceNumber;
    private String pathString;
    private Integer width;
    private Integer height;
    private int numberOfFrames;
    // ... getters and setters
}
```

## 5. まとめ

データベース管理のJava実装では：

1. **SQLite**を使用したリレーショナルデータベース
2. **階層構造**（Study→Series→Image）の管理
3. **ファイルインポート**と**検索機能**
4. **トランザクション管理**と**エラーハンドリング**

次のステップ: UIコンポーネントの実装

