package com.jj.dicomviewer.model;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DicomDatabase - DICOMデータベース管理クラス
 * 
 * HOROS-20240407のDicomDatabaseをJavaに移植
 * Core Dataの代わりにSQLiteを使用
 */
public class DicomDatabase {
    
    private static final Logger logger = LoggerFactory.getLogger(DicomDatabase.class);
    
    // ========== 静的フィールド ==========
    public static final String CURRENT_DATABASE_VERSION = "1.0";
    public static final String OSIRIX_DATA_DIR_NAME = "Horos Data";
    public static final String O2_SCREEN_CAPTURES_SERIES_NAME = "Screen Captures";
    
    // エンティティ名
    public static final String IMAGE_ENTITY_NAME = "Image";
    public static final String SERIES_ENTITY_NAME = "Series";
    public static final String STUDY_ENTITY_NAME = "Study";
    public static final String ALBUM_ENTITY_NAME = "Album";
    public static final String LOG_ENTRY_ENTITY_NAME = "LogEntry";
    
    private static DicomDatabase defaultDatabase = null;
    private static DicomDatabase activeLocalDatabase = null;
    
    // ========== インスタンスフィールド ==========
    private String baseDirPath; // "Horos Data"
    private String dataBaseDirPath; // DBFOLDER_LOCATIONに基づく
    private String sourcePath;
    private String name;
    
    private ReentrantLock processFilesLock;
    private ReentrantLock importFilesFromIncomingDirLock;
    
    private boolean isFileSystemFreeSizeLimitReached;
    private long timeOfLastIsFileSystemFreeSizeLimitReachedVerification;
    private long timeOfLastModification;
    
    private boolean isReadOnly;
    private boolean hasPotentiallySlowDataAccess;
    private boolean isLocal = true; // デフォルトはローカル
    
    // 圧縮/解凍
    private List<String> decompressQueue;
    private List<String> compressQueue;
    private Thread compressDecompressThread;
    
    // ルーティング
    private List<Object> routingSendQueues;
    private ReentrantLock routingLock;
    
    // クリーン
    private ReentrantLock cleanLock;
    
    private volatile boolean deallocating;
    
    // データファイルインデックス
    private int dataFileIndex;
    
    // スタディリスト（簡易実装）
    private List<DicomStudy> studies;
    
    // アルバムリスト
    private List<DicomAlbum> albums;
    
    // ファイルパスからImageへのマッピング（簡易実装）
    private java.util.Map<String, DicomImage> filePathToImageMap;
    
    // SQLite接続
    private Connection sqliteConnection;
    private String sqliteDatabasePath;
    
    // ========== コンストラクタ ==========
    
    /**
     * プライベートコンストラクタ
     * HOROS-20240407準拠: - (id)initWithPath:
     */
    private DicomDatabase(String baseDirPath, String name) {
        this.baseDirPath = baseDirPath;
        this.name = name;
        // HOROS-20240407準拠: DicomDatabase.mm 416-417行目
        // _dataBaseDirPath = [NSString stringWithContentsOfFile:[p stringByAppendingPathComponent:@"DBFOLDER_LOCATION"] encoding:NSUTF8StringEncoding error:NULL];
        // if (!_dataBaseDirPath) _dataBaseDirPath = p;
        File dbFolderLocationFile = new File(baseDirPath, "DBFOLDER_LOCATION");
        if (dbFolderLocationFile.exists() && dbFolderLocationFile.isFile()) {
            try {
                java.nio.file.Files.readAllLines(dbFolderLocationFile.toPath(), java.nio.charset.StandardCharsets.UTF_8)
                    .stream()
                    .findFirst()
                    .ifPresent(path -> this.dataBaseDirPath = path.trim());
            } catch (Exception e) {
                // 読み取りエラー時はbaseDirPathを使用
                this.dataBaseDirPath = baseDirPath;
            }
        }
        if (this.dataBaseDirPath == null) {
            this.dataBaseDirPath = baseDirPath;
        }
        this.processFilesLock = new ReentrantLock();
        this.importFilesFromIncomingDirLock = new ReentrantLock();
        this.routingLock = new ReentrantLock();
        this.cleanLock = new ReentrantLock();
        this.decompressQueue = new java.util.ArrayList<>();
        this.compressQueue = new java.util.ArrayList<>();
        this.routingSendQueues = new java.util.ArrayList<>();
        this.studies = new java.util.ArrayList<>();
        this.albums = new java.util.ArrayList<>();
        this.filePathToImageMap = new java.util.HashMap<>();
        
        // HOROS-20240407準拠: SQLiteデータベースを初期化
        initializeSQLiteDatabase();
        
        // HOROS-20240407準拠: データベースからデータを読み込む
        loadFromDatabase();
        
        // HOROS-20240407準拠: アルバムが空の場合はデフォルトアルバムを作成
        if (albums.isEmpty()) {
            createDefaultAlbums();
        }
    }
    
    // ========== 静的メソッド ==========
    
    /**
     * DicomDatabaseクラスを初期化
     */
    public static void initializeDicomDatabaseClass() {
        // TODO: 実装
    }
    
    /**
     * コンテキスト内の患者UIDを再計算
     */
    public static void recomputePatientUIDsInContext(Object context) {
        // TODO: 実装
    }
    
    /**
     * デフォルトのベースディレクトリパスを取得
     */
    public static String defaultBaseDirPath() {
        // TODO: 実装
        return System.getProperty("user.home") + File.separator + OSIRIX_DATA_DIR_NAME;
    }
    
    /**
     * パスのベースディレクトリパスを取得
     */
    public static String baseDirPathForPath(String path) {
        // TODO: 実装
        return path;
    }
    
    /**
     * モードとパスからベースディレクトリパスを取得
     */
    public static String baseDirPathForMode(int mode, String path) {
        // TODO: 実装
        return path;
    }
    
    /**
     * すべてのデータベースを取得
     */
    public static List<DicomDatabase> allDatabases() {
        // TODO: 実装
        return new java.util.ArrayList<>();
    }
    
    /**
     * デフォルトデータベースを取得
     */
    public static DicomDatabase defaultDatabase() {
        if (defaultDatabase == null) {
            defaultDatabase = databaseAtPath(defaultBaseDirPath());
        }
        return defaultDatabase;
    }
    
    /**
     * パスのデータベースを取得
     */
    public static DicomDatabase databaseAtPath(String path) {
        return databaseAtPath(path, null);
    }
    
    /**
     * パスと名前でデータベースを取得
     */
    public static DicomDatabase databaseAtPath(String path, String name) {
        // TODO: 実装
        // 既存のデータベースを返すか、新規作成
        return new DicomDatabase(path, name != null ? name : "Database");
    }
    
    /**
     * 既存のデータベースをパスで取得
     */
    public static DicomDatabase existingDatabaseAtPath(String path) {
        // TODO: 実装
        return null;
    }
    
    /**
     * コンテキストのデータベースを取得（非推奨）
     */
    @Deprecated
    public static DicomDatabase databaseForContext(Object context) {
        // TODO: 実装
        return null;
    }
    
    /**
     * アクティブなローカルデータベースを取得
     * HOROS-20240407準拠: activeLocalDatabaseがnullの場合はdefaultDatabaseを返す
     */
    public static DicomDatabase activeLocalDatabase() {
        if (activeLocalDatabase == null) {
            // HOROS-20240407準拠: activeLocalDatabaseが設定されていない場合はdefaultDatabaseを使用
            activeLocalDatabase = defaultDatabase();
        }
        return activeLocalDatabase;
    }
    
    /**
     * アクティブなローカルデータベースを設定
     */
    public static void setActiveLocalDatabase(DicomDatabase database) {
        activeLocalDatabase = database;
    }
    
    // ========== プロパティ ==========
    
    /**
     * ベースディレクトリパスを取得
     */
    public String getBaseDirPath() {
        return baseDirPath;
    }
    
    /**
     * データベースディレクトリパスを取得
     */
    public String getDataBaseDirPath() {
        return dataBaseDirPath;
    }
    
    /**
     * 名前を取得
     */
    public String getName() {
        return name;
    }
    
    /**
     * 名前を設定
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * スタディリストを取得
     * HOROS-20240407準拠
     */
    public List<DicomStudy> getStudies() {
        return studies;
    }
    
    /**
     * ソースパスを取得
     */
    public String getSourcePath() {
        return sourcePath;
    }
    
    /**
     * ソースパスを設定
     */
    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }
    
    /**
     * 最終変更時刻を取得
     */
    public long getTimeOfLastModification() {
        return timeOfLastModification;
    }
    
    /**
     * 最終変更時刻を設定
     */
    public void setTimeOfLastModification(long time) {
        this.timeOfLastModification = time;
    }
    
    /**
     * 読み取り専用かどうか
     */
    public boolean isReadOnly() {
        return isReadOnly;
    }
    
    /**
     * 読み取り専用を設定
     */
    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
    }
    
    /**
     * 潜在的に遅いデータアクセスがあるかどうか
     */
    public boolean hasPotentiallySlowDataAccess() {
        return hasPotentiallySlowDataAccess;
    }
    
    /**
     * 潜在的に遅いデータアクセスを設定
     */
    public void setHasPotentiallySlowDataAccess(boolean hasSlowAccess) {
        this.hasPotentiallySlowDataAccess = hasSlowAccess;
    }
    
    /**
     * ローカルデータベースかどうか
     */
    public boolean isLocal() {
        return isLocal;
    }
    
    /**
     * データノード識別子を取得
     */
    public Object dataNodeIdentifier() {
        // TODO: 実装
        return null;
    }
    
    // ========== SQLite関連メソッド ==========
    
    /**
     * SQLiteデータベースを初期化
     * HOROS-20240407準拠: DicomDatabase.mm 755-757行目 sqlFilePathForBasePath
     * return [basePath stringByAppendingPathComponent:SqlFileName];
     * SqlFileName = @"Database.sql"
     */
    private void initializeSQLiteDatabase() {
        try {
            // HOROS-20240407準拠: DicomDatabase.mm 410行目
            // NSString* sqlFilePath = [DicomDatabase sqlFilePathForBasePath:p];
            // sqlFilePathForBasePathはbasePath/Database.sqlを返す（755-757行目）
            // しかし、実際のファイルはdataBaseDirPath/DATABASE.noindex/Database.sqlにある
            // HOROS-20240407準拠: dataBaseDirPath/DATABASE.noindex/Database.sqlを使用
            File dbDir = new File(dataBaseDirPath, "DATABASE.noindex");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            sqliteDatabasePath = new File(dbDir, "Database.sql").getAbsolutePath();
            logger.info("Initializing SQLite database at: {}", sqliteDatabasePath);
            
            // SQLite接続を開く
            sqliteConnection = DriverManager.getConnection("jdbc:sqlite:" + sqliteDatabasePath);
            
            // テーブルを作成
            createTables();
            
            logger.info("SQLite database initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize SQLite database", e);
        }
    }
    
    /**
     * SQLiteテーブルを作成
     * HOROS-20240407準拠: Study, Series, Image, Albumエンティティ
     */
    private void createTables() throws SQLException {
        try (Statement stmt = sqliteConnection.createStatement()) {
            // Studyテーブル
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Study (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    studyInstanceUID TEXT UNIQUE,
                    name TEXT,
                    patientID TEXT,
                    patientUID TEXT,
                    patientBirthDate TEXT,
                    patientSex TEXT,
                    studyName TEXT,
                    modality TEXT,
                    date TEXT,
                    dateAdded TEXT,
                    numberOfImages INTEGER DEFAULT 0,
                    comment TEXT,
                    accessionNumber TEXT,
                    referringPhysician TEXT,
                    performingPhysician TEXT,
                    institutionName TEXT,
                    studyID TEXT
                )
            """);
            
            // Seriesテーブル
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Series (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    seriesInstanceUID TEXT UNIQUE,
                    studyId INTEGER,
                    name TEXT,
                    seriesDescription TEXT,
                    modality TEXT,
                    date TEXT,
                    dateAdded TEXT,
                    numberOfImages INTEGER DEFAULT 0,
                    seriesNumber INTEGER,
                    seriesSOPClassUID TEXT,
                    windowLevel REAL,
                    windowWidth REAL,
                    FOREIGN KEY (studyId) REFERENCES Study(id)
                )
            """);
            
            // Imageテーブル
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Image (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sopInstanceUID TEXT UNIQUE,
                    seriesId INTEGER,
                    pathString TEXT,
                    completePath TEXT,
                    instanceNumber INTEGER,
                    numberOfFrames INTEGER DEFAULT 1,
                    frameID INTEGER,
                    date TEXT,
                    modality TEXT,
                    fileType TEXT,
                    height INTEGER,
                    width INTEGER,
                    sliceLocation REAL,
                    FOREIGN KEY (seriesId) REFERENCES Series(id)
                )
            """);
            
            // Albumテーブル
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Album (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT,
                    smartAlbum INTEGER DEFAULT 0,
                    predicate TEXT,
                    dateAdded TEXT
                )
            """);
            
            // Album-Study関連テーブル
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS AlbumStudy (
                    albumId INTEGER,
                    studyId INTEGER,
                    PRIMARY KEY (albumId, studyId),
                    FOREIGN KEY (albumId) REFERENCES Album(id),
                    FOREIGN KEY (studyId) REFERENCES Study(id)
                )
            """);
            
            logger.info("SQLite tables created successfully");
        }
    }
    
    /**
     * データベースからデータを読み込む
     * HOROS-20240407準拠: 起動時にDBからStudy-Series-Imageを読み込む
     */
    private void loadFromDatabase() {
        if (sqliteConnection == null) {
            logger.warn("SQLite connection is null, cannot load data");
            return;
        }
        
        try {
            logger.info("Loading data from SQLite database at: {}", sqliteDatabasePath);
            
            // データベースファイルの存在確認
            File dbFile = new File(sqliteDatabasePath);
            if (!dbFile.exists()) {
                logger.warn("Database file does not exist: {}", sqliteDatabasePath);
                return;
            }
            
            // Studyを読み込む
            loadStudies();
            
            // HOROS-20240407準拠: アルバムを読み込む
            loadAlbums();
            
            logger.info("Loaded {} studies, {} albums from database", studies.size(), albums.size());
        } catch (SQLException e) {
            logger.error("Failed to load data from database: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error loading data from database: {}", e.getMessage(), e);
        }
    }
    
    /**
     * アルバムをデータベースから読み込む
     * HOROS-20240407準拠: Albumテーブルからアルバムを読み込む
     */
    private void loadAlbums() throws SQLException {
        synchronized (albums) {
            albums.clear();
            
            // HOROS-20240407準拠: すべてのテーブルをリストアップして確認
            try {
                logger.info("Checking database tables...");
                String listTablesSql = "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name";
                try (Statement listStmt = sqliteConnection.createStatement();
                     ResultSet listRs = listStmt.executeQuery(listTablesSql)) {
                    java.util.List<String> tableNames = new java.util.ArrayList<>();
                    while (listRs.next()) {
                        String tableName = listRs.getString("name");
                        tableNames.add(tableName);
                    }
                    logger.info("Database tables: {}", tableNames);
                }
                
                // テーブルの存在確認
                String checkTableSql = "SELECT name FROM sqlite_master WHERE type='table' AND name='Album'";
                boolean albumTableExists = false;
                try (Statement checkStmt = sqliteConnection.createStatement();
                     ResultSet checkRs = checkStmt.executeQuery(checkTableSql)) {
                    albumTableExists = checkRs.next();
                }
                
                if (!albumTableExists) {
                    logger.warn("Album table does not exist, skipping album loading");
                    return;
                }
                
                // アルバムの数を確認
                String countSql = "SELECT COUNT(*) as count FROM Album";
                int albumCount = 0;
                try (Statement countStmt = sqliteConnection.createStatement();
                     ResultSet countRs = countStmt.executeQuery(countSql)) {
                    if (countRs.next()) {
                        albumCount = countRs.getInt("count");
                    }
                }
                logger.info("Found {} albums in Album table", albumCount);
                
                String sql = "SELECT * FROM Album ORDER BY name";
                try (Statement stmt = sqliteConnection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    int count = 0;
                    while (rs.next()) {
                        DicomAlbum album = new DicomAlbum();
                        int albumDbId = rs.getInt("id");
                        album.setIndex(albumDbId);  // データベースIDをindexとして設定
                        album.setName(rs.getString("name"));
                        
                        // HOROS-20240407準拠: smartAlbumはINTEGER（0または1）
                        int smartAlbumInt = rs.getInt("smartAlbum");
                        album.setSmartAlbum(smartAlbumInt != 0);
                        
                        // HOROS-20240407準拠: predicateString（スマートアルバムの場合）
                        if (album.isSmartAlbum()) {
                            album.setPredicateString(rs.getString("predicate"));
                        }
                        
                        // HOROS-20240407準拠: AlbumStudyテーブルからStudyを読み込む
                        loadAlbumStudies(album, albumDbId);
                        
                        albums.add(album);
                        count++;
                        
                    }
                    
                    logger.info("Loaded {} albums from database", count);
                }
            } catch (SQLException e) {
                logger.error("Failed to load albums from database: {}", e.getMessage(), e);
                throw e;
            }
        }
    }
    
    /**
     * アルバムに属するStudyを読み込む
     * HOROS-20240407準拠: AlbumStudyテーブルからStudyを読み込む
     */
    private void loadAlbumStudies(DicomAlbum album, int albumDbId) throws SQLException {
        String sql = "SELECT studyId FROM AlbumStudy WHERE albumId = ?";
        try (PreparedStatement pstmt = sqliteConnection.prepareStatement(sql)) {
            pstmt.setInt(1, albumDbId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int studyDbId = rs.getInt("studyId");
                    
                    // Studyを検索
                    DicomStudy study = findStudyByDbId(studyDbId);
                    if (study != null) {
                        album.addStudiesObject(study);
                    }
                }
            }
        }
    }
    
    /**
     * データベースIDでStudyを検索
     */
    private DicomStudy findStudyByDbId(int studyDbId) {
        synchronized (studies) {
            for (DicomStudy study : studies) {
                String id = study.getId();
                if (id != null && id.equals(String.valueOf(studyDbId))) {
                    return study;
                }
            }
        }
        return null;
    }
    
    /**
     * Studyを読み込む（遅延読み込み対応）
     * HOROS-20240407準拠: 起動時はStudyのみ読み込み、Series/Imageは必要時に読み込む
     */
    private void loadStudies() throws SQLException {
        synchronized (studies) {
            studies.clear();
        }
        
        // HOROS-20240407準拠: Studyテーブルからデータを読み込む
        String sql = "SELECT * FROM Study ORDER BY date DESC";
        try (Statement stmt = sqliteConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                DicomStudy study = new DicomStudy();
                int studyDbId = rs.getInt("id");
                study.setStudyInstanceUID(rs.getString("studyInstanceUID"));
                study.setName(rs.getString("name"));
                study.setPatientID(rs.getString("patientID"));
                study.setPatientUID(rs.getString("patientUID"));
                study.setModality(rs.getString("modality"));
                study.setNumberOfImages(rs.getInt("numberOfImages"));
                study.setComment(rs.getString("comment"));
                study.setAccessionNumber(rs.getString("accessionNumber"));
                study.setReferringPhysician(rs.getString("referringPhysician"));
                study.setPerformingPhysician(rs.getString("performingPhysician"));
                study.setInstitutionName(rs.getString("institutionName"));
                study.setStudyID(rs.getString("studyID"));
                
                // HOROS-20240407準拠: studyNameを読み込む
                study.setStudyName(rs.getString("studyName"));
                
                // HOROS-20240407準拠: dateOfBirthを読み込む（patientBirthDateから）
                String patientBirthDateStr = rs.getString("patientBirthDate");
                if (patientBirthDateStr != null && !patientBirthDateStr.isEmpty()) {
                    try {
                        study.setDateOfBirth(java.time.LocalDate.parse(patientBirthDateStr));
                    } catch (Exception e) {
                        // 日付パースエラーは無視
                    }
                }
                
                // DBのIDを保存（遅延読み込み用）
                study.setId(String.valueOf(studyDbId));
                
                String dateStr = rs.getString("date");
                if (dateStr != null && !dateStr.isEmpty()) {
                    try {
                        study.setDate(LocalDateTime.parse(dateStr));
                    } catch (Exception e) {
                        // 日付パースエラーは無視
                    }
                }
                
                // HOROS-20240407準拠: dateAddedフィールドを読み込む
                String dateAddedStr = rs.getString("dateAdded");
                if (dateAddedStr != null && !dateAddedStr.isEmpty()) {
                    try {
                        study.setDateAdded(LocalDateTime.parse(dateAddedStr));
                    } catch (Exception e) {
                        // 日付パースエラーは無視
                    }
                }
                
                // HOROS-20240407準拠: 遅延読み込み - Seriesは必要時に読み込む
                // loadSeriesForStudy(study, studyDbId);
                
                synchronized (studies) {
                    studies.add(study);
                }
            }
            logger.info("Loaded {} studies from database", count);
        } catch (SQLException e) {
            logger.error("Failed to load studies from database: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * StudyのSeriesを遅延読み込み
     * HOROS-20240407準拠: Studyが選択されたときに呼び出される
     */
    public void loadSeriesForStudyIfNeeded(DicomStudy study) {
        if (study == null || study.getSeries() != null && !study.getSeries().isEmpty()) {
            return; // 既に読み込み済み
        }
        
        try {
            String idStr = study.getId();
            if (idStr != null && !idStr.isEmpty()) {
                int studyDbId = Integer.parseInt(idStr);
                loadSeriesForStudy(study, studyDbId);
            }
        } catch (Exception e) {
            logger.error("Failed to load series for study: {}", study.getStudyInstanceUID(), e);
        }
    }
    
    /**
     * StudyのSeriesを読み込む
     */
    private void loadSeriesForStudy(DicomStudy study, int studyDbId) throws SQLException {
        String sql = "SELECT * FROM Series WHERE studyId = ? ORDER BY seriesNumber";
        try (PreparedStatement pstmt = sqliteConnection.prepareStatement(sql)) {
            pstmt.setInt(1, studyDbId);
            try (ResultSet rs = pstmt.executeQuery()) {
                java.util.Set<DicomSeries> seriesSet = new java.util.HashSet<>();
                
                while (rs.next()) {
                    DicomSeries series = new DicomSeries();
                    series.setSeriesInstanceUID(rs.getString("seriesInstanceUID"));
                    series.setName(rs.getString("name"));
                    series.setSeriesDescription(rs.getString("seriesDescription"));
                    series.setModality(rs.getString("modality"));
                    series.setNumberOfImages(rs.getInt("numberOfImages"));
                    series.setSeriesNumber(rs.getInt("seriesNumber"));
                    series.setSeriesSOPClassUID(rs.getString("seriesSOPClassUID"));
                    series.setWindowLevel((double) rs.getFloat("windowLevel"));
                    series.setWindowWidth((double) rs.getFloat("windowWidth"));
                    series.setStudy(study);
                    
                    String dateStr = rs.getString("date");
                    if (dateStr != null && !dateStr.isEmpty()) {
                        try {
                            series.setDate(LocalDateTime.parse(dateStr));
                        } catch (Exception e) {
                            // 日付パースエラーは無視
                        }
                    }
                    
                    // HOROS-20240407準拠: dateAddedフィールドを読み込む
                    String dateAddedStr = rs.getString("dateAdded");
                    if (dateAddedStr != null && !dateAddedStr.isEmpty()) {
                        try {
                            series.setDateAdded(LocalDateTime.parse(dateAddedStr));
                        } catch (Exception e) {
                            // 日付パースエラーは無視
                        }
                    }
                    
                    // Imageを読み込む
                    loadImagesForSeries(series, rs.getInt("id"));
                    
                    seriesSet.add(series);
                    study.addSeriesObject(series);
                }
                
                study.setSeries(seriesSet);
            }
        }
    }
    
    /**
     * SeriesのImageを遅延読み込み
     * HOROS-20240407準拠: Seriesが選択されたときに呼び出される
     */
    public void loadImagesForSeriesIfNeeded(DicomSeries series) {
        if (series == null || (series.getImages() != null && !series.getImages().isEmpty())) {
            return; // 既に読み込み済み
        }
        
        try {
            // SeriesのDBIDを取得（StudyのSeriesから検索）
            String sql = "SELECT id FROM Series WHERE seriesInstanceUID = ?";
            try (PreparedStatement pstmt = sqliteConnection.prepareStatement(sql)) {
                pstmt.setString(1, series.getSeriesInstanceUID());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int seriesDbId = rs.getInt("id");
                        loadImagesForSeries(series, seriesDbId);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load images for series: {}", series.getSeriesInstanceUID(), e);
        }
    }
    
    /**
     * SeriesのImageを読み込む
     */
    private void loadImagesForSeries(DicomSeries series, int seriesDbId) throws SQLException {
        String sql = "SELECT * FROM Image WHERE seriesId = ? ORDER BY instanceNumber";
        try (PreparedStatement pstmt = sqliteConnection.prepareStatement(sql)) {
            pstmt.setInt(1, seriesDbId);
            try (ResultSet rs = pstmt.executeQuery()) {
                java.util.Set<DicomImage> imageSet = new java.util.HashSet<>();
                
                while (rs.next()) {
                    DicomImage image = new DicomImage();
                    image.setSopInstanceUID(rs.getString("sopInstanceUID"));
                    image.setPathString(rs.getString("pathString"));
                    image.setCompletePathCache(rs.getString("completePath"));
                    image.setInstanceNumber(rs.getInt("instanceNumber"));
                    image.setNumberOfFrames(rs.getInt("numberOfFrames"));
                    image.setFrameID(rs.getInt("frameID"));
                    image.setModality(rs.getString("modality"));
                    image.setFileType(rs.getString("fileType"));
                    image.setHeight(rs.getInt("height"));
                    image.setWidth(rs.getInt("width"));
                    image.setSliceLocation((double) rs.getFloat("sliceLocation"));
                    image.setSeries(series);
                    
                    String dateStr = rs.getString("date");
                    if (dateStr != null && !dateStr.isEmpty()) {
                        try {
                            image.setDate(LocalDateTime.parse(dateStr));
                        } catch (Exception e) {
                            // 日付パースエラーは無視
                        }
                    }
                    
                    imageSet.add(image);
                    series.addImagesObject(image);
                    
                    // ファイルパスマッピングに追加
                    String completePath = image.getCompletePath();
                    if (completePath != null && !completePath.isEmpty()) {
                        synchronized (filePathToImageMap) {
                            filePathToImageMap.put(completePath, image);
                        }
                    }
                }
                
                series.setImages(imageSet);
            }
        }
    }
    
    /**
     * データベースに保存
     * HOROS-20240407準拠: - (BOOL)save:(NSError**)error
     * 最適化: バッチ処理でトランザクションを使用して高速化
     */
    public boolean save() {
        if (sqliteConnection == null) {
            logger.warn("SQLite connection is null, cannot save data");
            return false;
        }
        
        try {
            // HOROS-20240407準拠: トランザクションを使用して高速化
            sqliteConnection.setAutoCommit(false);
            
            synchronized (studies) {
                for (DicomStudy study : studies) {
                    saveStudy(study);
                }
            }
            
            sqliteConnection.commit();
            sqliteConnection.setAutoCommit(true);
            
            return true;
        } catch (SQLException e) {
            logger.error("Failed to save data to database", e);
            try {
                sqliteConnection.rollback();
                sqliteConnection.setAutoCommit(true);
            } catch (SQLException e2) {
                logger.error("Failed to rollback transaction", e2);
            }
            return false;
        }
    }
    
    /**
     * Studyを保存
     */
    private void saveStudy(DicomStudy study) throws SQLException {
        // HOROS-20240407準拠: dateAddedがnullの場合は現在の日時を設定
        if (study.getDateAdded() == null) {
            study.setDateAdded(java.time.LocalDateTime.now());
        }
        
        // StudyをINSERT OR REPLACE
        String sql = """
            INSERT OR REPLACE INTO Study 
            (studyInstanceUID, name, patientID, patientUID, patientBirthDate, patientSex, 
             studyName, modality, date, dateAdded, 
             numberOfImages, comment, accessionNumber, referringPhysician, 
             performingPhysician, institutionName, studyID)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement pstmt = sqliteConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, study.getStudyInstanceUID());
            pstmt.setString(2, study.getName());
            pstmt.setString(3, study.getPatientID());
            pstmt.setString(4, study.getPatientUID());
            pstmt.setString(5, study.getDateOfBirth() != null ? study.getDateOfBirth().toString() : null);
            pstmt.setString(6, study.getPatientSex());
            pstmt.setString(7, study.getStudyName());
            pstmt.setString(8, study.getModality());
            pstmt.setString(9, study.getDate() != null ? study.getDate().toString() : null);
            pstmt.setString(10, study.getDateAdded() != null ? study.getDateAdded().toString() : null);
            pstmt.setInt(11, study.getNumberOfImages() != null ? study.getNumberOfImages() : 0);
            pstmt.setString(12, study.getComment());
            pstmt.setString(13, study.getAccessionNumber());
            pstmt.setString(14, study.getReferringPhysician());
            pstmt.setString(15, study.getPerformingPhysician());
            pstmt.setString(16, study.getInstitutionName());
            pstmt.setString(17, study.getStudyID());
            
            pstmt.executeUpdate();
            
            // StudyのIDを取得
            int studyDbId = getStudyDbId(study.getStudyInstanceUID());
            
            // Seriesを保存
            java.util.Set<DicomSeries> seriesSet = study.getSeries();
            if (seriesSet != null) {
                for (DicomSeries series : seriesSet) {
                    saveSeries(series, studyDbId);
                }
            }
        }
    }
    
    /**
     * StudyのDBIDを取得
     */
    private int getStudyDbId(String studyInstanceUID) throws SQLException {
        String sql = "SELECT id FROM Study WHERE studyInstanceUID = ?";
        try (PreparedStatement pstmt = sqliteConnection.prepareStatement(sql)) {
            pstmt.setString(1, studyInstanceUID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }
    
    /**
     * Seriesを保存
     */
    private void saveSeries(DicomSeries series, int studyDbId) throws SQLException {
        // HOROS-20240407準拠: dateAddedがnullの場合は現在の日時を設定（2080行目、2192行目）
        if (series.getDateAdded() == null) {
            series.setDateAdded(java.time.LocalDateTime.now());
        }
        
        String sql = """
            INSERT OR REPLACE INTO Series 
            (seriesInstanceUID, studyId, name, seriesDescription, modality, date, dateAdded,
             numberOfImages, seriesNumber, seriesSOPClassUID, windowLevel, windowWidth)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement pstmt = sqliteConnection.prepareStatement(sql)) {
            pstmt.setString(1, series.getSeriesInstanceUID());
            pstmt.setInt(2, studyDbId);
            pstmt.setString(3, series.getName());
            pstmt.setString(4, series.getSeriesDescription());
            pstmt.setString(5, series.getModality());
            pstmt.setString(6, series.getDate() != null ? series.getDate().toString() : null);
            pstmt.setString(7, series.getDateAdded() != null ? series.getDateAdded().toString() : null);
            pstmt.setInt(8, series.getNumberOfImages() != null ? series.getNumberOfImages() : 0);
            pstmt.setInt(9, series.getSeriesNumber() != null ? series.getSeriesNumber() : 0);
            pstmt.setString(10, series.getSeriesSOPClassUID());
            pstmt.setFloat(11, series.getWindowLevel() != null ? series.getWindowLevel().floatValue() : 0);
            pstmt.setFloat(12, series.getWindowWidth() != null ? series.getWindowWidth().floatValue() : 0);
            
            pstmt.executeUpdate();
            
            // SeriesのIDを取得
            int seriesDbId = getSeriesDbId(series.getSeriesInstanceUID());
            
            // Imageを保存
            java.util.Set<DicomImage> imageSet = series.getImages();
            if (imageSet != null) {
                for (DicomImage image : imageSet) {
                    saveImage(image, seriesDbId);
                }
            }
        }
    }
    
    /**
     * SeriesのDBIDを取得
     */
    private int getSeriesDbId(String seriesInstanceUID) throws SQLException {
        String sql = "SELECT id FROM Series WHERE seriesInstanceUID = ?";
        try (PreparedStatement pstmt = sqliteConnection.prepareStatement(sql)) {
            pstmt.setString(1, seriesInstanceUID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }
    
    /**
     * Imageを保存
     */
    private void saveImage(DicomImage image, int seriesDbId) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO Image 
            (sopInstanceUID, seriesId, pathString, completePath, instanceNumber,
             numberOfFrames, frameID, date, modality, fileType, height, width, sliceLocation)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement pstmt = sqliteConnection.prepareStatement(sql)) {
            pstmt.setString(1, image.getSopInstanceUID());
            pstmt.setInt(2, seriesDbId);
            pstmt.setString(3, image.getPathString());
            pstmt.setString(4, image.getCompletePath());
            pstmt.setInt(5, image.getInstanceNumber() != null ? image.getInstanceNumber() : 0);
            pstmt.setInt(6, image.getNumberOfFrames() != null ? image.getNumberOfFrames() : 1);
            pstmt.setInt(7, image.getFrameID() != null ? image.getFrameID() : 0);
            pstmt.setString(8, image.getDate() != null ? image.getDate().toString() : null);
            pstmt.setString(9, image.getModality());
            pstmt.setString(10, image.getFileType());
            pstmt.setInt(11, image.getHeight() != null ? image.getHeight() : 0);
            pstmt.setInt(12, image.getWidth() != null ? image.getWidth() : 0);
            pstmt.setFloat(13, image.getSliceLocation() != null ? image.getSliceLocation().floatValue() : 0);
            
            pstmt.executeUpdate();
        }
    }
    
    /**
     * データベース接続を閉じる
     */
    public void close() {
        if (sqliteConnection != null) {
            try {
                // 保存してから閉じる
                save();
                sqliteConnection.close();
                logger.info("SQLite database connection closed");
            } catch (SQLException e) {
                logger.error("Failed to close SQLite database connection", e);
            }
        }
    }
    
    // ========== パス関連メソッド ==========
    
    /**
     * モデルバージョンファイルパスを取得
     */
    public String modelVersionFilePath() {
        // TODO: 実装
        return baseDirPath + File.separator + "ModelVersion";
    }
    
    /**
     * ローディングファイルパスを取得
     */
    public String loadingFilePath() {
        // TODO: 実装
        return baseDirPath + File.separator + "Loading";
    }
    
    /**
     * データディレクトリパスを取得
     */
    public String dataDirPath() {
        return getDataDirPath();
    }
    
    /**
     * データディレクトリパスを取得
     * HOROS-20240407準拠: - (NSString*)dataDirPath (DicomDatabase.mm 763-765行目)
     * return [[self.dataBaseDirPath stringByAppendingPathComponent:@"DATABASE.noindex"] stringByResolvingSymlinksAndAliases];
     */
    public String getDataDirPath() {
        // HOROS-20240407準拠: dataBaseDirPath/DATABASE.noindex
        return dataBaseDirPath + File.separator + "DATABASE.noindex";
    }
    
    /**
     * 受信ディレクトリパスを取得
     */
    public String incomingDirPath() {
        // TODO: 実装
        return dataBaseDirPath + File.separator + "INCOMING";
    }
    
    /**
     * エラーディレクトリパスを取得
     */
    public String errorsDirPath() {
        // TODO: 実装
        return dataBaseDirPath + File.separator + "ERRORS";
    }
    
    /**
     * 解凍ディレクトリパスを取得
     */
    public String decompressionDirPath() {
        // TODO: 実装
        return dataBaseDirPath + File.separator + "DECOMPRESSION";
    }
    
    /**
     * インデックス待ちディレクトリパスを取得
     */
    public String toBeIndexedDirPath() {
        // TODO: 実装
        return dataBaseDirPath + File.separator + "TO_BE_INDEXED";
    }
    
    /**
     * レポートディレクトリパスを取得
     */
    public String reportsDirPath() {
        // TODO: 実装
        return dataBaseDirPath + File.separator + "REPORTS";
    }
    
    /**
     * 一時ディレクトリパスを取得
     */
    public String tempDirPath() {
        // TODO: 実装
        return dataBaseDirPath + File.separator + "TEMP";
    }
    
    /**
     * ダンプディレクトリパスを取得
     */
    public String dumpDirPath() {
        // TODO: 実装
        return dataBaseDirPath + File.separator + "DUMP";
    }
    
    /**
     * ページディレクトリパスを取得
     */
    public String pagesDirPath() {
        // TODO: 実装
        return dataBaseDirPath + File.separator + "PAGES";
    }
    
    /**
     * HTMLテンプレートディレクトリパスを取得
     */
    public String htmlTemplatesDirPath() {
        // TODO: 実装
        return dataBaseDirPath + File.separator + "HTML_TEMPLATES";
    }
    
    /**
     * ステートディレクトリパスを取得
     */
    public String statesDirPath() {
        // TODO: 実装
        return dataBaseDirPath + File.separator + "STATES";
    }
    
    /**
     * CLUTディレクトリパスを取得
     */
    public String clutsDirPath() {
        // TODO: 実装
        return dataBaseDirPath + File.separator + "CLUTS";
    }
    
    /**
     * プリセットディレクトリパスを取得
     */
    public String presetsDirPath() {
        // TODO: 実装
        return dataBaseDirPath + File.separator + "PRESETS";
    }
    
    /**
     * ベースディレクトリパス（C文字列）を取得
     */
    public String baseDirPathC() {
        // TODO: 実装
        return baseDirPath;
    }
    
    /**
     * 受信ディレクトリパス（C文字列）を取得
     */
    public String incomingDirPathC() {
        // TODO: 実装
        return incomingDirPath();
    }
    
    /**
     * 一時ディレクトリパス（C文字列）を取得
     */
    public String tempDirPathC() {
        // TODO: 実装
        return tempDirPath();
    }
    
    /**
     * データファイルインデックスを計算
     */
    public int computeDataFileIndex() {
        // TODO: 実装
        return ++dataFileIndex;
    }
    
    /**
     * 新しいデータファイルの一意パスを取得
     */
    public String uniquePathForNewDataFileWithExtension(String ext) {
        // TODO: 実装
        return dataDirPath() + File.separator + "file_" + computeDataFileIndex() + "." + ext;
    }
    
    // ========== アルバム関連メソッド ==========
    
    /**
     * デフォルトアルバムを追加
     */
    public void addDefaultAlbums() {
        // TODO: 実装
    }
    
    /**
     * アルバム配列を取得
     */
    public List<Object> albums() {
        // TODO: 実装
        return new java.util.ArrayList<>();
    }
    
    /**
     * スマートアルバムフィルターの述語を作成
     */
    public static Object predicateForSmartAlbumFilter(String string) {
        // TODO: 実装
        return null;
    }
    
    /**
     * アルバムをパスに保存
     */
    public void saveAlbumsToPath(String path) {
        // TODO: 実装
    }
    
    /**
     * アルバムをパスから読み込み
     */
    public void loadAlbumsFromPath(String path) {
        // TODO: 実装
    }
    
    /**
     * スタディをアルバムに追加
     */
    public void addStudies(List<Object> dicomStudies, Object dicomAlbum) {
        // TODO: 実装
    }
    
    /**
     * Studyを削除
     * HOROS-20240407準拠
     */
    public void deleteStudy(DicomStudy study) {
        if (study == null || sqliteConnection == null) {
            return;
        }
        
        try {
            // まずStudyのIDを取得
            String selectSql = "SELECT id FROM Study WHERE studyInstanceUID = ?";
            int studyDbId = -1;
            try (PreparedStatement pstmt = sqliteConnection.prepareStatement(selectSql)) {
                pstmt.setString(1, study.getStudyInstanceUID());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    studyDbId = rs.getInt("id");
                }
            }
            
            if (studyDbId > 0) {
                // Seriesに属するImageを削除
                String deleteImagesSql = "DELETE FROM Image WHERE seriesId IN (SELECT id FROM Series WHERE studyId = ?)";
                try (PreparedStatement pstmt = sqliteConnection.prepareStatement(deleteImagesSql)) {
                    pstmt.setInt(1, studyDbId);
                    pstmt.executeUpdate();
                }
                
                // Seriesを削除
                String deleteSeriesSql = "DELETE FROM Series WHERE studyId = ?";
                try (PreparedStatement pstmt = sqliteConnection.prepareStatement(deleteSeriesSql)) {
                    pstmt.setInt(1, studyDbId);
                    pstmt.executeUpdate();
                }
                
                // Studyを削除
                String deleteStudySql = "DELETE FROM Study WHERE id = ?";
                try (PreparedStatement pstmt = sqliteConnection.prepareStatement(deleteStudySql)) {
                    pstmt.setInt(1, studyDbId);
                    pstmt.executeUpdate();
                }
            }
            
            // メモリからも削除
            synchronized (studies) {
                studies.remove(study);
            }
            
            logger.info("Deleted study: " + study.getStudyInstanceUID());
            
        } catch (SQLException e) {
            logger.error("Failed to delete study: " + study.getStudyInstanceUID(), e);
        }
    }
    
    /**
     * Seriesを削除
     * HOROS-20240407準拠
     */
    public void deleteSeries(DicomSeries series) {
        if (series == null || sqliteConnection == null) {
            return;
        }
        
        try {
            // まずSeriesのIDを取得
            String selectSql = "SELECT id FROM Series WHERE seriesInstanceUID = ?";
            int seriesDbId = -1;
            try (PreparedStatement pstmt = sqliteConnection.prepareStatement(selectSql)) {
                pstmt.setString(1, series.getSeriesInstanceUID());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    seriesDbId = rs.getInt("id");
                }
            }
            
            if (seriesDbId > 0) {
                // Imageを削除
                String deleteImagesSql = "DELETE FROM Image WHERE seriesId = ?";
                try (PreparedStatement pstmt = sqliteConnection.prepareStatement(deleteImagesSql)) {
                    pstmt.setInt(1, seriesDbId);
                    pstmt.executeUpdate();
                }
                
                // Seriesを削除
                String deleteSeriesSql = "DELETE FROM Series WHERE id = ?";
                try (PreparedStatement pstmt = sqliteConnection.prepareStatement(deleteSeriesSql)) {
                    pstmt.setInt(1, seriesDbId);
                    pstmt.executeUpdate();
                }
            }
            
            // メモリからも削除
            DicomStudy study = series.getStudy();
            if (study != null && study.getSeries() != null) {
                study.getSeries().remove(series);
            }
            
            logger.info("Deleted series: " + series.getSeriesInstanceUID());
            
        } catch (SQLException e) {
            logger.error("Failed to delete series: " + series.getSeriesInstanceUID(), e);
        }
    }
    
    // ========== ファイル追加メソッド ==========
    
    /**
     * パスのファイルを追加
     */
    public List<Object> addFilesAtPaths(List<String> paths) {
        return addFilesAtPaths(paths, true);
    }
    
    /**
     * パスのファイルを追加（通知オプション付き）
     */
    public List<Object> addFilesAtPaths(List<String> paths, boolean postNotifications) {
        return addFilesAtPaths(paths, postNotifications, false, false);
    }
    
    /**
     * パスのファイルを追加（全オプション）
     */
    public List<Object> addFilesAtPaths(List<String> paths, boolean postNotifications, 
                                         boolean dicomOnly, boolean rereadExistingItems) {
        return addFilesAtPaths(paths, postNotifications, dicomOnly, rereadExistingItems, false);
    }
    
    /**
     * パスのファイルを追加（OsiriX生成フラグ付き）
     */
    public List<Object> addFilesAtPaths(List<String> paths, boolean postNotifications, 
                                         boolean dicomOnly, boolean rereadExistingItems, 
                                         boolean generatedByOsiriX) {
        return addFilesAtPaths(paths, postNotifications, dicomOnly, rereadExistingItems, 
                               generatedByOsiriX, true);
    }
    
    /**
     * パスのファイルを追加（返却配列オプション付き）
     */
    public List<Object> addFilesAtPaths(List<String> paths, boolean postNotifications, 
                                         boolean dicomOnly, boolean rereadExistingItems, 
                                         boolean generatedByOsiriX, boolean returnArray) {
        return addFilesAtPaths(paths, postNotifications, dicomOnly, rereadExistingItems, 
                               generatedByOsiriX, false, returnArray);
    }
    
    /**
     * パスのファイルを追加（インポートファイルフラグ付き）
     */
    public List<Object> addFilesAtPaths(List<String> paths, boolean postNotifications, 
                                         boolean dicomOnly, boolean rereadExistingItems, 
                                         boolean generatedByOsiriX, boolean importedFiles, 
                                         boolean returnArray) {
        // HOROS-20240407準拠: NSThread* thread = [NSThread currentThread]; (1502行目)
        Thread thread = Thread.currentThread();
        com.jj.dicomviewer.threads.ThreadsManager tm = com.jj.dicomviewer.threads.ThreadsManager.defaultManager();
        
        // HOROS-20240407準拠: ファイルを追加してスタディを作成
        List<Object> addedObjects = new java.util.ArrayList<>();
        
        if (paths == null || paths.isEmpty()) {
            return addedObjects;
        }
        
        // HOROS-20240407準拠: [thread enterOperation]; thread.status = [NSString stringWithFormat:NSLocalizedString(@"Scanning %@", nil), N2LocalizedSingularPluralCount(paths.count, NSLocalizedString(@"file", nil), NSLocalizedString(@"files", nil))]; (1526-1527行目)
        String scanningStatus = paths.size() == 1 ? "Scanning 1 file" : "Scanning " + paths.size() + " files";
        tm.setThreadStatus(thread, scanningStatus);
        tm.setThreadProgress(thread, -1.0); // indeterminateモード
        
        // HOROS-20240407準拠: ファイルパスを展開（ディレクトリの場合は再帰的にファイルを取得）
        List<String> expandedPaths = new java.util.ArrayList<>();
        for (String path : paths) {
            java.io.File file = new java.io.File(path);
            if (file.isDirectory()) {
                collectFilesRecursively(file, expandedPaths);
            } else if (file.isFile()) {
                expandedPaths.add(path);
            }
        }
        
        // HOROS-20240407準拠: ディレクトリを作成（1539-1543行目）
        String dataDirPath = this.dataBaseDirPath;
        String reportsDirPath = this.dataBaseDirPath + File.separator + "REPORTS";
        String errorsDirPath = this.dataBaseDirPath + File.separator + "ERRORS";
        
        java.io.File dataDir = new java.io.File(dataDirPath);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        java.io.File reportsDir = new java.io.File(reportsDirPath);
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }
        java.io.File errorsDir = new java.io.File(errorsDirPath);
        if (!errorsDir.exists()) {
            errorsDir.mkdirs();
        }
        
        // HOROS-20240407準拠: DICOMファイルをスキャンしてdicomFilesArrayを構築（1537-1641行目）
        List<Map<String, Object>> dicomFilesArray = new java.util.ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < expandedPaths.size(); i++) {
            String path = expandedPaths.get(i);
            
            // HOROS-20240407準拠: 進捗更新（1555-1558行目）
            long currentTime = System.currentTimeMillis();
            if (currentTime - startTime > 500 || i == expandedPaths.size() - 1) {
                tm.setThreadProgress(thread, 1.0 * i / expandedPaths.size());
                startTime = currentTime;
            }
            
            try {
                java.io.File file = new java.io.File(path);
                if (!file.exists() || !file.isFile()) {
                    continue;
                }
                
                // HOROS-20240407準拠: DicomFileを作成してdicomElementsを取得（1567-1580行目）
                Map<String, Object> curDict = extractDicomElements(file);
                
                if (curDict != null) {
                    // HOROS-20240407準拠: dicomOnlyチェック（1581-1585行目）
                    if (dicomOnly) {
                        String fileType = (String) curDict.get("fileType");
                        if (fileType == null || !fileType.startsWith("DICOM")) {
                            curDict = null;
                        }
                    }
                    
                    if (curDict != null) {
                        dicomFilesArray.add(curDict);
                    } else {
                        // HOROS-20240407準拠: 読み取り不能なファイルの処理（1593-1609行目）
                        String absolutePath = file.getAbsolutePath();
                        if (dataDirPath != null && absolutePath.startsWith(dataDirPath)) {
                            logger.warn("Unreadable file in DATABASE folder: " + absolutePath);
                            // TODO: DELETEFILELISTENER設定に基づいて削除または移動
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Exception while scanning file: " + path, e);
            }
            
            // HOROS-20240407準拠: キャンセルチェック（1624-1640行目）
            if (Thread.currentThread().isInterrupted()) {
                dicomFilesArray.clear();
                break;
            }
        }
        
        // HOROS-20240407準拠: [thread enterOperationIgnoringLowerLevels]; thread.status = [NSString stringWithFormat:NSLocalizedString(@"Adding %@", nil), N2LocalizedSingularPluralCount(dicomFilesArray.count, NSLocalizedString(@"file", nil), NSLocalizedString(@"files", nil))]; (1643-1644行目)
        String addingStatus = dicomFilesArray.size() == 1 ? "Adding 1 file" : "Adding " + dicomFilesArray.size() + " files";
        tm.setThreadStatus(thread, addingStatus);
        
        // HOROS-20240407準拠: addFilesDescribedInDictionariesを呼び出す（1648-1653行目）
        // HOROS-20240407準拠: List<Map>をList<Object>に変換
        List<Object> dicomFilesArrayAsObject = new java.util.ArrayList<>();
        for (Map<String, Object> dict : dicomFilesArray) {
            dicomFilesArrayAsObject.add(dict);
        }
        List<Object> objectIDs = addFilesDescribedInDictionaries(
            dicomFilesArrayAsObject,
            postNotifications,
            rereadExistingItems,
            generatedByOsiriX,
            importedFiles,
            returnArray
        );
        
        // HOROS-20240407準拠: 返却配列に追加（1668-1676行目）
        if (returnArray) {
            if (objectIDs != null) {
                addedObjects.addAll(objectIDs);
            }
        } else {
            // HOROS-20240407準拠: データベースに保存（1672-1673行目）
            save();
        }
        
        return addedObjects;
    }
    
    /**
     * 辞書で記述されたファイルを追加（インポートファイルフラグ付き）
     * HOROS-20240407準拠: - (NSArray*)addFilesDescribedInDictionaries:... (1755-4271行目)
     */
    public List<Object> addFilesDescribedInDictionaries(List<Object> dicomFilesArray, 
                                                          boolean postNotifications, 
                                                          boolean rereadExistingItems, 
                                                          boolean generatedByOsiriX, 
                                                          boolean importedFiles, 
                                                          boolean returnArray) {
        // HOROS-20240407準拠: NSThread* thread = [NSThread currentThread]; (1761行目)
        Thread thread = Thread.currentThread();
        com.jj.dicomviewer.threads.ThreadsManager tm = com.jj.dicomviewer.threads.ThreadsManager.defaultManager();
        
        // HOROS-20240407準拠: thread.status = [NSString stringWithFormat:NSLocalizedString(@"Adding %@", nil), N2LocalizedSingularPluralCount(dicomFilesArray.count, NSLocalizedString(@"file", nil), NSLocalizedString(@"files", nil))]; (1762行目)
        String addingStatus = dicomFilesArray.size() == 1 ? "Adding 1 file" : "Adding " + dicomFilesArray.size() + " files";
        tm.setThreadStatus(thread, addingStatus);
        
        // HOROS-20240407準拠: NSMutableArray* newStudies = [NSMutableArray array]; (1764行目)
        List<DicomStudy> newStudies = new java.util.ArrayList<>();
        
        // HOROS-20240407準拠: NSMutableArray* addedImageObjects = nil; (1766行目)
        List<Object> addedImageObjects = null;
        if (returnArray) {
            addedImageObjects = new java.util.ArrayList<>();
        }
        
        // HOROS-20240407準拠: NSDate* today = [NSDate date]; (1801行目)
        LocalDateTime today = LocalDateTime.now();
        
        // HOROS-20240407準拠: NSDate *defaultDate = [NSCalendarDate dateWithYear:1901 month:1 day:1 hour:0 minute:0 second:0 timeZone:nil]; (1792行目)
        LocalDateTime defaultDate = LocalDateTime.of(1901, 1, 1, 0, 0, 0);
        
        // HOROS-20240407準拠: NSString* dataDirPath = self.dataDirPath; (1802行目)
        String dataDirPath = this.dataBaseDirPath;
        
        // HOROS-20240407準拠: NSMutableArray* studiesArray = [[self objectsForEntity:self.studyEntity] mutableCopy]; (1789行目)
        List<DicomStudy> studiesArray = new java.util.ArrayList<>();
        synchronized (studies) {
            studiesArray.addAll(studies);
        }
        
        // HOROS-20240407準拠: NSMutableArray *studiesArrayStudyInstanceUID = [[studiesArray valueForKey:@"studyInstanceUID"] mutableCopy]; (1797行目)
        List<String> studiesArrayStudyInstanceUID = new java.util.ArrayList<>();
        for (DicomStudy s : studiesArray) {
            studiesArrayStudyInstanceUID.add(s.getStudyInstanceUID());
        }
        
        DicomStudy study = null;
        DicomSeries series = null;
        DicomImage image = null;
        String curPatientUID = null;
        String curStudyID = null;
        String curSerieID = null;
        boolean newObject = false;
        boolean newStudy = false;
        
        // HOROS-20240407準拠: for (NSInteger i = 0; i < dicomFilesArray.count; ++i) (1815行目)
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < dicomFilesArray.size(); i++) {
            // HOROS-20240407準拠: 進捗更新（1817-1820行目）
            long currentTime = System.currentTimeMillis();
            if (currentTime - startTime > 500 || i == dicomFilesArray.size() - 1) {
                tm.setThreadProgress(thread, 1.0 * i / dicomFilesArray.size());
                startTime = currentTime;
            }
            
            try {
                // HOROS-20240407準拠: NSMutableDictionary *curDict = [dicomFilesArray objectAtIndex:i]; (1826行目)
                @SuppressWarnings("unchecked")
                Map<String, Object> curDict = (Map<String, Object>) dicomFilesArray.get(i);
                
                // HOROS-20240407準拠: newFile = [curDict objectForKey:@"filePath"]; (1829行目)
                String newFile = (String) curDict.get("filePath");
                
                // HOROS-20240407準拠: BOOL inParseExistingObject = rereadExistingItems; (1832行目)
                boolean inParseExistingObject = rereadExistingItems;
                
                // HOROS-20240407準拠: NSString *SOPClassUID = [curDict objectForKey:@"SOPClassUID"]; (1834行目)
                String sopClassUID = (String) curDict.get("SOPClassUID");
                
                // HOROS-20240407準拠: 辞書からStudy/Series/Imageを作成（1902-4271行目）
                // まず、Studyを検索または作成
                String studyID = (String) curDict.get("studyID");
                String patientUID = (String) curDict.get("patientUID");
                
                // HOROS-20240407準拠: Study検索（1904-1969行目）
                if (studyID != null && studyID.equals(curStudyID) && 
                    patientUID != null && patientUID.equals(curPatientUID)) {
                    // 同じStudyの続き
                } else {
                    // 新しいStudyを検索または作成
                    study = null;
                    curSerieID = null;
                    newObject = false;
                    
                    // HOROS-20240407準拠: NSInteger index = [studiesArrayStudyInstanceUID indexOfObject:[curDict objectForKey: @"studyID"]]; (1918行目)
                    int index = studiesArrayStudyInstanceUID.indexOf(studyID);
                    
                    if (index >= 0) {
                        // HOROS-20240407準拠: 既存のStudyを検索（1922-1968行目）
                        DicomStudy tstudy = studiesArray.get(index);
                        
                        if (tstudy.getPatientUID() == null) {
                            tstudy.setPatientUID(patientUID);
                        }
                        
                        // HOROS-20240407準拠: patientUIDを比較（1943行目）
                        if (patientUID != null && patientUID.equals(tstudy.getPatientUID())) {
                            study = tstudy;
                        } else {
                            // HOROS-20240407準拠: 複数のStudyを検索（1947-1958行目）
                            for (int j = 0; j < studiesArrayStudyInstanceUID.size(); j++) {
                                String uid = studiesArrayStudyInstanceUID.get(j);
                                if (uid != null && uid.equals(studyID)) {
                                    DicomStudy s = studiesArray.get(j);
                                    if (patientUID != null && patientUID.equals(s.getPatientUID())) {
                                        study = s;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    // HOROS-20240407準拠: 新しいStudyを作成（1971-1992行目）
                    if (study == null) {
                        study = new DicomStudy();
                        newObject = true;
                        newStudy = true;
                        
                        study.setDateAdded(today);
                        newStudies.add(study);
                        studiesArray.add(study);
                        if (studyID != null) {
                            studiesArrayStudyInstanceUID.add(studyID);
                        } else {
                            logger.warn("no studyID !");
                            studiesArrayStudyInstanceUID.add("noStudyID");
                        }
                        
                        curSerieID = null;
                    }
                    
                    // HOROS-20240407準拠: Studyのフィールドを設定（1994-2056行目）
                    if (newObject || inParseExistingObject) {
                        study.setStudyInstanceUID(studyID);
                        study.setAccessionNumber((String) curDict.get("accessionNumber"));
                        study.setPatientID((String) curDict.get("patientID"));
                        study.setName((String) curDict.get("patientName"));
                        study.setPatientUID(patientUID);
                        study.setStudyID((String) curDict.get("studyNumber"));
                        
                        study.setStudyName((String) curDict.get("studyDescription"));
                        study.setReferringPhysician((String) curDict.get("referringPhysiciansName"));
                        study.setPerformingPhysician((String) curDict.get("performingPhysiciansName"));
                        study.setInstitutionName((String) curDict.get("institutionName"));
                        
                        // HOROS-20240407準拠: studyNameが空または"unnamed"の場合はseriesDescriptionを使用（2025-2026行目）
                        String studyName = study.getStudyName();
                        if (studyName == null || studyName.isEmpty() || studyName.equals("unnamed")) {
                            study.setStudyName((String) curDict.get("seriesDescription"));
                        }
                        
                        // HOROS-20240407準拠: dateOfBirthを設定（1999行目）
                        Object patientBirthDateObj = curDict.get("patientBirthDate");
                        if (patientBirthDateObj != null) {
                            if (patientBirthDateObj instanceof Date) {
                                Date patientBirthDate = (Date) patientBirthDateObj;
                                study.setDateOfBirth(patientBirthDate.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate());
                            } else if (patientBirthDateObj instanceof String) {
                                try {
                                    study.setDateOfBirth(java.time.LocalDate.parse((String) patientBirthDateObj));
                                } catch (Exception e) {
                                    // パースエラーは無視
                                }
                            }
                        }
                        
                        // HOROS-20240407準拠: patientSexを設定
                        String patientSex = (String) curDict.get("patientSex");
                        if (patientSex != null) {
                            study.setPatientSex(patientSex);
                        }
                        
                        // HOROS-20240407準拠: studyDateを設定（2047-2051行目）
                        Date studyDate = (Date) curDict.get("studyDate");
                        if (studyDate != null) {
                            LocalDateTime studyDateLocal = LocalDateTime.ofInstant(studyDate.toInstant(), ZoneId.systemDefault());
                            if (studyDateLocal.isAfter(defaultDate)) {
                                if (study.getDate() == null || study.getDate().isBefore(studyDateLocal)) {
                                    study.setDate(studyDateLocal);
                                }
                            }
                        }
                    } else {
                        // HOROS-20240407準拠: 既存のStudyの場合もstudyNameを更新（2035-2045行目）
                        // modalityが"SR"または"OT"の場合は更新（2037-2038行目）
                        String modality = study.getModality();
                        if ("SR".equals(modality) || "OT".equals(modality)) {
                            String newModality = (String) curDict.get("modality");
                            if (newModality != null) {
                                study.setModality(newModality);
                            }
                        }
                        
                        // HOROS-20240407準拠: studyNameが空または"unnamed"の場合は更新（2040-2044行目）
                        String currentStudyName = study.getStudyName();
                        if (currentStudyName == null || currentStudyName.isEmpty() || currentStudyName.equals("unnamed")) {
                            String studyDescription = (String) curDict.get("studyDescription");
                            if (studyDescription != null) {
                                study.setStudyName(studyDescription);
                            }
                        }
                        
                        // HOROS-20240407準拠: studyNameが空または"unnamed"の場合はseriesDescriptionを使用（2043-2044行目）
                        String updatedStudyName = study.getStudyName();
                        if (updatedStudyName == null || updatedStudyName.isEmpty() || updatedStudyName.equals("unnamed")) {
                            String seriesDescription = (String) curDict.get("seriesDescription");
                            if (seriesDescription != null) {
                                study.setStudyName(seriesDescription);
                            }
                        }
                    }
                    
                    curStudyID = studyID;
                    curPatientUID = patientUID;
                    
                    // HOROS-20240407準拠: studiesに追加
                    synchronized (studies) {
                        if (!studies.contains(study)) {
                            studies.add(study);
                        }
                    }
                }
                
                // HOROS-20240407準拠: Seriesを検索または作成（2059-2110行目）
                String seriesID = (String) curDict.get("seriesID");
                if (seriesID == null) {
                    seriesID = (String) curDict.get("seriesDICOMUID");
                }
                
                if (seriesID == null || !seriesID.equals(curSerieID)) {
                    // 新しいSeriesを検索または作成
                    series = null;
                    java.util.Set<DicomSeries> seriesSet = study.getSeries();
                    if (seriesSet != null) {
                        for (DicomSeries s : seriesSet) {
                            if (seriesID != null && seriesID.equals(s.getSeriesInstanceUID())) {
                                series = s;
                                break;
                            }
                        }
                    }
                    
                    if (series == null) {
                        // HOROS-20240407準拠: 新しいSeriesを作成（2079-2107行目）
                        series = new DicomSeries();
                        series.setDateAdded(today);
                        newObject = true;
                    } else {
                        newObject = false;
                    }
                    
                    if (newObject || inParseExistingObject) {
                        series.setSeriesInstanceUID(seriesID);
                        series.setName((String) curDict.get("seriesDescription"));
                        series.setModality((String) curDict.get("modality"));
                        series.setSeriesNumber(((Number) curDict.get("seriesNumber")).intValue());
                        series.setSeriesDescription((String) curDict.get("protocolName"));
                        series.setStudy(study);
                        
                        Date seriesDate = (Date) curDict.get("studyDate");
                        if (seriesDate != null) {
                            series.setDate(LocalDateTime.ofInstant(seriesDate.toInstant(), ZoneId.systemDefault()));
                        }
                    }
                    
                    curSerieID = seriesID;
                    
                    // HOROS-20240407準拠: StudyにSeriesを追加
                    java.util.Set<DicomSeries> studySeriesSet = study.getSeries();
                    if (studySeriesSet == null) {
                        studySeriesSet = new java.util.HashSet<>();
                        study.setSeries(studySeriesSet);
                    }
                    if (!studySeriesSet.contains(series)) {
                        studySeriesSet.add(series);
                        study.addSeriesObject(series);
                    }
                }
                
                // HOROS-20240407準拠: Imageを検索または作成（2112-2271行目）
                String sopUID = (String) curDict.get("SOPUID");
                int numberOfFrames = ((Number) curDict.getOrDefault("numberOfFrames", 1)).intValue();
                if (numberOfFrames == 0) {
                    numberOfFrames = 1;
                }
                
                for (int f = 0; f < numberOfFrames; f++) {
                    image = null;
                    
                    // HOROS-20240407準拠: 既存のImageを検索（2132-2140行目）
                    java.util.Set<DicomImage> imageSet = series.getImages();
                    if (imageSet != null) {
                        for (DicomImage img : imageSet) {
                            if (sopUID != null && sopUID.equals(img.getSopInstanceUID()) && 
                                img.getFrameID() != null && img.getFrameID() == f) {
                                image = img;
                                break;
                            }
                        }
                    }
                    
                    if (image == null) {
                        // HOROS-20240407準拠: 新しいImageを作成（2169-2271行目）
                        image = new DicomImage();
                        newObject = true;
                    } else {
                        newObject = false;
                    }
                    
                    if (newObject || inParseExistingObject) {
                        image.setSopInstanceUID(sopUID);
                        image.setFrameID(f);
                        image.setInstanceNumber(((Number) curDict.getOrDefault("imageID", 0)).intValue());
                        image.setPathString(new java.io.File(newFile).getName());
                        image.setCompletePathCache(newFile);
                        image.setSliceLocation(((Number) curDict.getOrDefault("sliceLocation", 0.0)).doubleValue());
                        image.setHeight(((Number) curDict.getOrDefault("height", 0)).intValue());
                        image.setWidth(((Number) curDict.getOrDefault("width", 0)).intValue());
                        image.setFileType((String) curDict.get("fileType"));
                        image.setSeries(series);
                        
                        Date imageDate = (Date) curDict.get("studyDate");
                        if (imageDate != null) {
                            image.setDate(LocalDateTime.ofInstant(imageDate.toInstant(), ZoneId.systemDefault()));
                        }
                    }
                    
                    // HOROS-20240407準拠: SeriesにImageを追加
                    java.util.Set<DicomImage> currentImageSet = series.getImages();
                    if (currentImageSet == null) {
                        currentImageSet = new java.util.HashSet<>();
                        series.setImages(currentImageSet);
                    }
                    if (!currentImageSet.contains(image)) {
                        currentImageSet.add(image);
                        series.addImagesObject(image);
                    }
                    
                    // HOROS-20240407準拠: ファイルパスマッピングに追加
                    synchronized (filePathToImageMap) {
                        filePathToImageMap.put(newFile, image);
                    }
                    
                    // HOROS-20240407準拠: returnArrayがtrueの場合は追加されたImageを返す
                    if (returnArray && addedImageObjects != null) {
                        addedImageObjects.add(image);
                    }
                }
                
                // HOROS-20240407準拠: Seriesの画像数を更新
                java.util.Set<DicomImage> finalImageSetForCount = series.getImages();
                if (finalImageSetForCount != null) {
                    series.setNumberOfImages(finalImageSetForCount.size());
                } else {
                    series.setNumberOfImages(0);
                }
                
                // HOROS-20240407準拠: Studyの画像数を更新
                updateStudyNumberOfImages(study);
                
            } catch (Exception e) {
                logger.warn("Exception in addFilesDescribedInDictionaries loop", e);
            }
        }
        
        // HOROS-20240407準拠: データベースに保存
        save();
        
        // HOROS-20240407準拠: 返却配列を返す
        if (returnArray) {
            return addedImageObjects != null ? addedImageObjects : new java.util.ArrayList<>();
        } else {
            return new java.util.ArrayList<>();
        }
    }
    
    
    /**
     * 全てのスタディを取得
     * HOROS-20240407準拠
     */
    public List<DicomStudy> getAllStudies() {
        synchronized (studies) {
            return new java.util.ArrayList<>(studies);
        }
    }
    
    /**
     * アルバムリストを取得
     * HOROS-20240407準拠: - (NSArray*) albums
     */
    public List<DicomAlbum> getAlbums() {
        synchronized (albums) {
            return new java.util.ArrayList<>(albums);
        }
    }
    
    /**
     * アルバムを追加
     * HOROS-20240407準拠
     */
    public void addAlbum(DicomAlbum album) {
        if (album != null) {
            synchronized (albums) {
                albums.add(album);
            }
        }
    }
    
    /**
     * アルバムを削除
     * HOROS-20240407準拠
     */
    public void removeAlbum(DicomAlbum album) {
        if (album != null) {
            synchronized (albums) {
                albums.remove(album);
            }
        }
    }
    
    /**
     * デフォルトアルバムを作成
     * HOROS-20240407準拠
     */
    private void createDefaultAlbums() {
        // HOROS-20240407準拠: 最初のアルバムは「Database」（全Study）
        // これはalbumsリストには含めず、albumTable.selectedRow == 0で判定
        
        // HOROS-20240407準拠: デフォルトのスマートアルバムを作成
        albums.add(new DicomAlbum("Today", "date >= $TODAY"));
        albums.add(new DicomAlbum("Yesterday", "date >= $YESTERDAY AND date < $TODAY"));
        albums.add(new DicomAlbum("Last Week", "date >= $LASTWEEK"));
    }
    
    /**
     * DICOMファイルからStudyInstanceUIDを抽出（簡易実装）
     * TODO: dcm4cheなどを使用して実際のDICOMファイルを読み込む
     * HOROS-20240407準拠: DicomFile.dicomElementsから取得
     */
    /**
     * DICOMファイルからメタデータを抽出
     * HOROS-20240407準拠: DicomFile.dicomElementsから取得
     */
    private Attributes readDicomAttributes(java.io.File file) {
        try (DicomInputStream dis = new DicomInputStream(file)) {
            return dis.readDataset();
        } catch (Exception e) {
            logger.warn("Failed to read DICOM file: " + file.getAbsolutePath(), e);
            return null;
        }
    }
    
    private String extractStudyInstanceUID(java.io.File file) {
        Attributes attrs = readDicomAttributes(file);
        if (attrs != null) {
            return attrs.getString(Tag.StudyInstanceUID);
        }
        return null;
    }
    
    private String extractSeriesInstanceUID(java.io.File file) {
        Attributes attrs = readDicomAttributes(file);
        if (attrs != null) {
            return attrs.getString(Tag.SeriesInstanceUID);
        }
        return null;
    }
    
    private String extractSOPInstanceUID(java.io.File file) {
        Attributes attrs = readDicomAttributes(file);
        if (attrs != null) {
            return attrs.getString(Tag.SOPInstanceUID);
        }
        return null;
    }
    
    /**
     * DICOMファイルから辞書（Map）を作成
     * HOROS-20240407準拠: DicomFile.dicomElements (1686-1742行目のコメント参照)
     * 辞書のキーはHOROS-20240407のDicomDatabase.mm 1686-1742行目に記載されている
     */
    private Map<String, Object> extractDicomElements(java.io.File file) {
        Attributes attrs = readDicomAttributes(file);
        if (attrs == null) {
            return null;
        }
        
        Map<String, Object> dict = new HashMap<>();
        
        // HOROS-20240407準拠: filePath (1689行目)
        dict.put("filePath", file.getAbsolutePath());
        
        // HOROS-20240407準拠: fileType (1693行目)
        String fileType = "DICOM";
        dict.put("fileType", fileType);
        dict.put("hasDICOM", true);
        
        // HOROS-20240407準拠: SOPClassUID (1690行目)
        String sopClassUID = attrs.getString(Tag.SOPClassUID);
        if (sopClassUID != null) {
            dict.put("SOPClassUID", sopClassUID);
        }
        
        // HOROS-20240407準拠: studyID (1694行目) = StudyInstanceUID
        String studyInstanceUID = attrs.getString(Tag.StudyInstanceUID);
        if (studyInstanceUID != null) {
            dict.put("studyID", studyInstanceUID);
        }
        
        // HOROS-20240407準拠: patientUID (1695行目)
        // HOROS-20240407準拠: patientUIDはpatientName, patientID, patientBirthDateから計算される
        String patientName = attrs.getString(Tag.PatientName);
        String patientID = attrs.getString(Tag.PatientID);
        Date patientBirthDate = attrs.getDate(Tag.PatientBirthDate);
        String patientUID = calculatePatientUID(patientName, patientID, patientBirthDate);
        if (patientUID != null) {
            dict.put("patientUID", patientUID);
        }
        
        // HOROS-20240407準拠: patientName (1701行目)
        if (patientName != null) {
            dict.put("patientName", patientName);
        }
        
        // HOROS-20240407準拠: patientID (1702行目)
        if (patientID != null) {
            dict.put("patientID", patientID);
        }
        
        // HOROS-20240407準拠: patientBirthDate (1699行目)
        if (patientBirthDate != null) {
            dict.put("patientBirthDate", patientBirthDate);
        }
        
        // HOROS-20240407準拠: patientSex (1700行目)
        String patientSex = attrs.getString(Tag.PatientSex);
        if (patientSex != null) {
            dict.put("patientSex", patientSex);
        }
        
        // HOROS-20240407準拠: studyDescription (1704行目)
        String studyDescription = attrs.getString(Tag.StudyDescription);
        if (studyDescription == null || studyDescription.trim().isEmpty()) {
            org.dcm4che3.data.Sequence procedureCodeSeq = attrs.getSequence(Tag.ProcedureCodeSequence);
            if (procedureCodeSeq != null && !procedureCodeSeq.isEmpty()) {
                Attributes firstItem = procedureCodeSeq.get(0);
                if (firstItem != null) {
                    studyDescription = firstItem.getString(Tag.CodeMeaning);
                }
            }
        }
        if (studyDescription == null || studyDescription.trim().isEmpty()) {
            studyDescription = "unnamed";
        }
        dict.put("studyDescription", studyDescription);
        
        // HOROS-20240407準拠: studyDate (1710行目)
        Date studyDate = attrs.getDate(Tag.StudyDateAndTime);
        if (studyDate == null) {
            studyDate = attrs.getDate(Tag.StudyDate);
        }
        if (studyDate != null) {
            dict.put("studyDate", studyDate);
        }
        
        // HOROS-20240407準拠: accessionNumber (1698行目)
        String accessionNumber = attrs.getString(Tag.AccessionNumber);
        if (accessionNumber != null) {
            dict.put("accessionNumber", accessionNumber);
        }
        
        // HOROS-20240407準拠: referringPhysiciansName (1705行目)
        String referringPhysician = attrs.getString(Tag.ReferringPhysicianName);
        if (referringPhysician != null) {
            dict.put("referringPhysiciansName", referringPhysician);
        }
        
        // HOROS-20240407準拠: performingPhysiciansName (1706行目)
        String performingPhysician = attrs.getString(Tag.PerformingPhysicianName);
        if (performingPhysician != null) {
            dict.put("performingPhysiciansName", performingPhysician);
        }
        
        // HOROS-20240407準拠: institutionName (1707行目)
        String institutionName = attrs.getString(Tag.InstitutionName);
        if (institutionName != null) {
            dict.put("institutionName", institutionName);
        }
        
        // HOROS-20240407準拠: studyNumber (1703行目)
        String studyNumber = attrs.getString(Tag.StudyID);
        if (studyNumber != null) {
            dict.put("studyNumber", studyNumber);
        }
        
        // HOROS-20240407準拠: seriesID (1714行目) = SeriesInstanceUID
        String seriesInstanceUID = attrs.getString(Tag.SeriesInstanceUID);
        if (seriesInstanceUID != null) {
            dict.put("seriesID", seriesInstanceUID);
            dict.put("seriesDICOMUID", seriesInstanceUID);
        }
        
        // HOROS-20240407準拠: seriesDescription (1715行目)
        String seriesDescription = attrs.getString(Tag.SeriesDescription);
        if (seriesDescription == null || seriesDescription.trim().isEmpty()) {
            seriesDescription = attrs.getString(0x00400254); // PerformedProcedureStepDescription
        }
        if (seriesDescription == null || seriesDescription.trim().isEmpty()) {
            seriesDescription = attrs.getString(0x00181400); // AcquisitionDeviceProcessingDescription
        }
        if (seriesDescription == null || seriesDescription.trim().isEmpty()) {
            seriesDescription = "unnamed";
        }
        dict.put("seriesDescription", seriesDescription);
        
        // HOROS-20240407準拠: seriesNumber (1716行目)
        int seriesNumber = attrs.getInt(Tag.SeriesNumber, 0);
        dict.put("seriesNumber", seriesNumber);
        
        // HOROS-20240407準拠: modality (1696行目)
        String modality = attrs.getString(Tag.Modality);
        if (modality != null) {
            dict.put("modality", modality);
        }
        
        // HOROS-20240407準拠: protocolName (1718行目)
        String protocolName = attrs.getString(Tag.ProtocolName);
        if (protocolName != null) {
            dict.put("protocolName", protocolName);
        }
        
        // HOROS-20240407準拠: numberOfFrames (1720行目)
        int numberOfFrames = attrs.getInt(Tag.NumberOfFrames, 1);
        dict.put("numberOfFrames", numberOfFrames);
        
        // HOROS-20240407準拠: SOPUID (1721行目) = SOPInstanceUID
        String sopInstanceUID = attrs.getString(Tag.SOPInstanceUID);
        if (sopInstanceUID != null) {
            dict.put("SOPUID", sopInstanceUID);
        }
        
        // HOROS-20240407準拠: imageID (1722行目) = InstanceNumber
        int instanceNumber = attrs.getInt(Tag.InstanceNumber, 0);
        dict.put("imageID", instanceNumber);
        
        // HOROS-20240407準拠: sliceLocation (1724行目)
        double sliceLocation = attrs.getDouble(Tag.SliceLocation, 0.0);
        dict.put("sliceLocation", sliceLocation);
        
        // HOROS-20240407準拠: height (1726行目)
        int height = attrs.getInt(Tag.Rows, 0);
        dict.put("height", height);
        
        // HOROS-20240407準拠: width (1727行目)
        int width = attrs.getInt(Tag.Columns, 0);
        dict.put("width", width);
        
        // HOROS-20240407準拠: numberOfSeries (1728行目)
        // 通常は1だが、複数Seriesを含むファイルの場合は設定される
        dict.put("numberOfSeries", 1);
        
        return dict;
    }
    
    /**
     * PatientUIDを計算
     * HOROS-20240407準拠: DicomFile.patientUID (patientName, patientID, patientBirthDateから計算)
     */
    private String calculatePatientUID(String patientName, String patientID, Date patientBirthDate) {
        // HOROS-20240407準拠: patientUIDはpatientName, patientID, patientBirthDateから計算される
        // 簡易実装: patientIDが存在する場合はそれを使用、なければpatientNameを使用
        if (patientID != null && !patientID.trim().isEmpty()) {
            return "PATIENT_" + patientID;
        } else if (patientName != null && !patientName.trim().isEmpty()) {
            return "PATIENT_" + patientName;
        } else {
            return "PATIENT_UNKNOWN";
        }
    }
    
    /**
     * DICOMファイルから全メタデータを抽出してStudy/Series/Imageに設定
     * HOROS-20240407準拠
     * @deprecated extractDicomElementsを使用してください
     */
    @Deprecated
    private DicomMetadata extractAllMetadata(java.io.File file) {
        Attributes attrs = readDicomAttributes(file);
        if (attrs == null) {
            return null;
        }
        
        DicomMetadata meta = new DicomMetadata();
        
        // Study level
        meta.studyInstanceUID = attrs.getString(Tag.StudyInstanceUID);
        meta.patientName = attrs.getString(Tag.PatientName);
        meta.patientID = attrs.getString(Tag.PatientID);
        
        // HOROS-20240407準拠: DicomFileDCMTKCategory.mm 517-531行目を完全に写経
        // Study Description
        // if (dataset->findAndGetString(DCM_StudyDescription, string, OFFalse).good() && string != NULL)
        //     study = [[DicomFile stringWithBytes: (char*) string encodings:encoding] retain];
        // else
        // {
        //     DcmItem *item = NULL;
        //     if (dataset->findAndGetSequenceItem(DCM_ProcedureCodeSequence, item).good())
        //     {
        //         if( item->findAndGetString(DCM_CodeMeaning, string, OFFalse).good() && string != NULL)
        //             study = [[DicomFile stringWithBytes: (char*) string encodings:encoding] retain];
        //     }
        // }
        // if( !study)
        //     study = [[NSString alloc] initWithString: @"unnamed"];
        String studyDescription = attrs.getString(Tag.StudyDescription);
        // HOROS-20240407準拠: ProcedureCodeSequence内のCodeMeaningをフォールバックとして使用
        // DICOMタグ(0008,1032) ProcedureCodeSequence
        // DICOMタグ(0008,0104) CodeMeaning
        if (studyDescription == null || studyDescription.trim().isEmpty()) {
            org.dcm4che3.data.Sequence procedureCodeSeq = attrs.getSequence(Tag.ProcedureCodeSequence);
            if (procedureCodeSeq != null && !procedureCodeSeq.isEmpty()) {
                Attributes firstItem = procedureCodeSeq.get(0);
                if (firstItem != null) {
                    studyDescription = firstItem.getString(Tag.CodeMeaning);
                }
            }
        }
        // HOROS-20240407準拠: if( !study) study = [[NSString alloc] initWithString: @"unnamed"];
        if (studyDescription == null || studyDescription.trim().isEmpty()) {
            studyDescription = "unnamed";
        }
        meta.studyDescription = studyDescription;
        // HOROS-20240407準拠: StudyDateとStudyTimeを組み合わせる
        meta.studyDate = attrs.getDate(Tag.StudyDateAndTime);
        if (meta.studyDate == null) {
            meta.studyDate = attrs.getDate(Tag.StudyDate);
        }
        meta.accessionNumber = attrs.getString(Tag.AccessionNumber);
        meta.referringPhysician = attrs.getString(Tag.ReferringPhysicianName);
        meta.institutionName = attrs.getString(Tag.InstitutionName);
        
        // Series level
        meta.seriesInstanceUID = attrs.getString(Tag.SeriesInstanceUID);
        // HOROS-20240407準拠: DicomFileDCMTKCategory.mm 594-604行目
        // Series Description
        // if (dataset->findAndGetString(DCM_SeriesDescription, string, OFFalse).good() && string != NULL)
        //     serie = [[DicomFile stringWithBytes: (char*) string encodings:encoding] retain];
        // else if (dataset->findAndGetString(DCM_PerformedProcedureStepDescription, string, OFFalse).good() && string != NULL)
        //     serie = [[DicomFile stringWithBytes: (char*) string encodings:encoding] retain];
        // else if (dataset->findAndGetString(DCM_AcquisitionDeviceProcessingDescription, string, OFFalse).good() && string != NULL)
        //     serie = [[DicomFile stringWithBytes: (char*) string encodings:encoding] retain];
        // else if( serie == nil)
        //     serie = [[NSString alloc] initWithString: @"unnamed"];
        // HOROS-20240407準拠: DicomFileDCMTKCategory.mm 595-602行目を完全に写経
        // if (dataset->findAndGetString(DCM_SeriesDescription, string, OFFalse).good() && string != NULL)
        //     serie = [[DicomFile stringWithBytes: (char*) string encodings:encoding] retain];
        // else if (dataset->findAndGetString(DCM_PerformedProcedureStepDescription, string, OFFalse).good() && string != NULL)
        //     serie = [[DicomFile stringWithBytes: (char*) string encodings:encoding] retain];
        // else if (dataset->findAndGetString(DCM_AcquisitionDeviceProcessingDescription, string, OFFalse).good() && string != NULL)
        //     serie = [[DicomFile stringWithBytes: (char*) string encodings:encoding] retain];
        // else if( serie == nil)
        //     serie = [[NSString alloc] initWithString: @"unnamed"];
        String seriesDescription = attrs.getString(Tag.SeriesDescription);
        // HOROS-20240407準拠: else if (dataset->findAndGetString(DCM_PerformedProcedureStepDescription, string, OFFalse).good() && string != NULL)
        // DICOMタグ(0040,0254) PerformedProcedureStepDescription
        if (seriesDescription == null || seriesDescription.trim().isEmpty()) {
            seriesDescription = attrs.getString(0x00400254);
        }
        // HOROS-20240407準拠: else if (dataset->findAndGetString(DCM_AcquisitionDeviceProcessingDescription, string, OFFalse).good() && string != NULL)
        // DICOMタグ(0018,1400) AcquisitionDeviceProcessingDescription
        if (seriesDescription == null || seriesDescription.trim().isEmpty()) {
            seriesDescription = attrs.getString(0x00181400);
        }
        // HOROS-20240407準拠: else if( serie == nil) serie = [[NSString alloc] initWithString: @"unnamed"];
        if (seriesDescription == null || seriesDescription.trim().isEmpty()) {
            seriesDescription = "unnamed";
        }
        meta.seriesDescription = seriesDescription;
        meta.seriesNumber = attrs.getInt(Tag.SeriesNumber, 0);
        meta.modality = attrs.getString(Tag.Modality);
        // HOROS-20240407準拠: SeriesDateとSeriesTimeを組み合わせる
        meta.seriesDate = attrs.getDate(Tag.SeriesDateAndTime);
        if (meta.seriesDate == null) {
            meta.seriesDate = attrs.getDate(Tag.SeriesDate);
        }
        
        // Image level
        meta.sopInstanceUID = attrs.getString(Tag.SOPInstanceUID);
        meta.instanceNumber = attrs.getInt(Tag.InstanceNumber, 0);
        meta.sliceLocation = attrs.getDouble(Tag.SliceLocation, 0.0);
        meta.numberOfFrames = attrs.getInt(Tag.NumberOfFrames, 1);
        
        // Window level/width
        meta.windowCenter = attrs.getDouble(Tag.WindowCenter, 0.0);
        meta.windowWidth = attrs.getDouble(Tag.WindowWidth, 0.0);
        
        return meta;
    }
    
    /**
     * ディレクトリ内のファイルを再帰的に収集
     * HOROS-20240407準拠
     */
    private void collectFilesRecursively(java.io.File directory, List<String> files) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }
        
        java.io.File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        
        for (java.io.File child : children) {
            String name = child.getName();
            // 隠しファイル・フォルダをスキップ
            if (name.startsWith(".")) {
                continue;
            }
            
            if (child.isDirectory()) {
                collectFilesRecursively(child, files);
            } else if (child.isFile()) {
                files.add(child.getAbsolutePath());
            }
        }
    }
    
    /**
     * DICOMメタデータを保持する内部クラス
     */
    private static class DicomMetadata {
        // Study
        String studyInstanceUID;
        String patientName;
        String patientID;
        String studyDescription;
        Date studyDate;
        String accessionNumber;
        String referringPhysician;
        String institutionName;
        
        // Series
        String seriesInstanceUID;
        String seriesDescription;
        int seriesNumber;
        String modality;
        Date seriesDate;
        
        // Image
        String sopInstanceUID;
        int instanceNumber;
        double sliceLocation;
        int numberOfFrames;
        double windowCenter;
        double windowWidth;
    }
    
    /**
     * Studyの画像数を更新
     * HOROS-20240407準拠: Studyの全てのSeriesの画像数を合計
     */
    private void updateStudyNumberOfImages(DicomStudy study) {
        if (study == null) {
            return;
        }
        
        int totalImages = 0;
        java.util.Set<DicomSeries> seriesSet = study.getSeries();
        if (seriesSet != null) {
            try {
            for (DicomSeries series : seriesSet) {
                Integer numberOfImages = series.getNumberOfImages();
                if (numberOfImages != null) {
                    totalImages += numberOfImages;
                    }
                }
            } catch (java.util.ConcurrentModificationException e) {
                // マルチスレッド環境での競合は無視（ログを抑制）
                // コピーを作成して再試行
                try {
                    for (DicomSeries series : new java.util.ArrayList<>(seriesSet)) {
                        Integer numberOfImages = series.getNumberOfImages();
                        if (numberOfImages != null) {
                            totalImages += numberOfImages;
                        }
                    }
                } catch (Exception e2) {
                    // 再試行も失敗した場合は無視
                }
            }
        }
        
        study.setNumberOfImages(totalImages);
    }
    
    /**
     * 辞書で記述されたファイルを追加
     */
    public List<Object> addFilesDescribedInDictionaries(List<Object> dicomFilesArray, 
                                                          boolean postNotifications, 
                                                          boolean rereadExistingItems, 
                                                          boolean generatedByOsiriX) {
        return addFilesDescribedInDictionaries(dicomFilesArray, postNotifications, 
                                               rereadExistingItems, generatedByOsiriX, true);
    }
    
    /**
     * 辞書で記述されたファイルを追加（返却配列オプション付き）
     */
    public List<Object> addFilesDescribedInDictionaries(List<Object> dicomFilesArray, 
                                                          boolean postNotifications, 
                                                          boolean rereadExistingItems, 
                                                          boolean generatedByOsiriX, 
                                                          boolean returnArray) {
        return addFilesDescribedInDictionaries(dicomFilesArray, postNotifications, 
                                               rereadExistingItems, generatedByOsiriX, 
                                               false, returnArray);
    }
    
    
    // ========== 受信関連メソッド ==========
    
    /**
     * ファイルシステムの空き容量制限に達したかどうか
     */
    public boolean isFileSystemFreeSizeLimitReached() {
        // TODO: 実装
        return isFileSystemFreeSizeLimitReached;
    }
    
    /**
     * インポートするファイルがあるかどうか
     */
    public boolean hasFilesToImport() {
        // TODO: 実装
        return false;
    }
    
    /**
     * 受信ディレクトリからファイルをインポート
     */
    public int importFilesFromIncomingDir() {
        return importFilesFromIncomingDir(null);
    }
    
    /**
     * 受信ディレクトリからファイルをインポート（GUI表示オプション付き）
     */
    public int importFilesFromIncomingDir(Boolean showGUI) {
        return importFilesFromIncomingDir(showGUI, 0);
    }
    
    /**
     * 受信ディレクトリからファイルをインポート（リスナー圧縮設定付き）
     */
    public int importFilesFromIncomingDir(Boolean showGUI, int listenerCompressionSettings) {
        // TODO: 実装
        return 0;
    }
    
    /**
     * 圧縮スレッドを待機
     */
    public boolean waitForCompressThread() {
        // TODO: 実装
        return true;
    }
    
    /**
     * 既にインポート中でない限り、受信ディレクトリからのファイルインポートを開始
     */
    public void initiateImportFilesFromIncomingDirUnlessAlreadyImporting() {
        // TODO: 実装
    }
    
    /**
     * 受信ディレクトリからファイルをインポート（スレッド）
     */
    public void importFilesFromIncomingDirThread() {
        // TODO: 実装
    }
    
    /**
     * ユーザー設定と同期して受信ディレクトリからのファイルインポートタイマーを設定（非推奨）
     */
    @Deprecated
    public static void syncImportFilesFromIncomingDirTimerWithUserDefaults() {
        // TODO: 実装
    }
    
    // ========== 圧縮/解凍メソッド ==========
    
    /**
     * パスのファイルを圧縮
     */
    public boolean compressFilesAtPaths(List<String> paths) {
        return compressFilesAtPaths(paths, null);
    }
    
    /**
     * パスのファイルを圧縮（出力先ディレクトリ指定）
     */
    public boolean compressFilesAtPaths(List<String> paths, String destDir) {
        // TODO: 実装
        return false;
    }
    
    /**
     * パスのファイルを解凍
     */
    public boolean decompressFilesAtPaths(List<String> paths) {
        return decompressFilesAtPaths(paths, null);
    }
    
    /**
     * パスのファイルを解凍（出力先ディレクトリ指定）
     */
    public boolean decompressFilesAtPaths(List<String> paths, String destDir) {
        // TODO: 実装
        return false;
    }
    
    /**
     * パスのファイルの圧縮を開始
     */
    public void initiateCompressFilesAtPaths(List<String> paths) {
        initiateCompressFilesAtPaths(paths, null);
    }
    
    /**
     * パスのファイルの圧縮を開始（出力先ディレクトリ指定）
     */
    public void initiateCompressFilesAtPaths(List<String> paths, String destDir) {
        // TODO: 実装
    }
    
    /**
     * パスのファイルの解凍を開始
     */
    public void initiateDecompressFilesAtPaths(List<String> paths) {
        initiateDecompressFilesAtPaths(paths, null);
    }
    
    /**
     * パスのファイルの解凍を開始（出力先ディレクトリ指定）
     */
    public void initiateDecompressFilesAtPaths(List<String> paths, String destDir) {
        // TODO: 実装
    }
    
    /**
     * パスのファイルを処理（モード指定）
     */
    public void processFilesAtPaths(List<String> paths, String destDir, int mode) {
        // TODO: 実装（mode: Compress=0, Decompress=1）
    }
    
    // ========== その他のメソッド ==========
    
    /**
     * 再構築が許可されているかどうか
     */
    public boolean rebuildAllowed() {
        // TODO: 実装
        return true;
    }
    
    /**
     * 再構築
     */
    public void rebuild() {
        rebuild(false);
    }
    
    /**
     * 再構築（完全再構築オプション付き）
     */
    public void rebuild(boolean complete) {
        // TODO: 実装
    }
    
    /**
     * スタディの既存レポートをチェック
     */
    public void checkForExistingReportForStudy(Object study) {
        // TODO: 実装
    }
    
    /**
     * DICOMSRとのレポート一貫性をチェック
     */
    public void checkReportsConsistencyWithDICOMSR() {
        // TODO: 実装
    }
    
    /**
     * SQLファイルを再構築
     */
    public void rebuildSqlFile() {
        // TODO: 実装
    }
    
    /**
     * HTMLテンプレートをチェック
     */
    public void checkForHtmlTemplates() {
        // TODO: 実装
    }
    
    /**
     * オートルーティングが許可されているかどうか
     */
    public boolean allowAutoroutingWithPostNotifications(boolean postNotifications, 
                                                          boolean rereadExistingItems) {
        // TODO: 実装
        return true;
    }
    
    /**
     * ルーティングルールを画像に適用するアラート
     */
    public void alertToApplyRoutingRules(List<Object> routingRules, List<Object> images) {
        // TODO: 実装
    }
    
    /**
     * ファイルをコピー（スレッド）
     * HOROS-20240407準拠: - (void)copyFilesThread:(NSDictionary*)dict (2610-2848行目)
     */
    public void copyFilesThread(java.util.Map<String, Object> dict) {
        logger.info("copyFilesThread: starting");
        // HOROS-20240407準拠: @autoreleasepool (2612行目)
        try {
            // HOROS-20240407準拠: NSOperationQueue* queue = [[[NSOperationQueue alloc] init] autorelease]; [queue setMaxConcurrentOperationCount:1]; (2614-2615行目)
            java.util.concurrent.ExecutorService queue = java.util.concurrent.Executors.newSingleThreadExecutor();
            
            // HOROS-20240407準拠: BOOL onlyDICOM = [[dict objectForKey: @"onlyDICOM"] boolValue], copyFiles = [[dict objectForKey: @"copyFiles"] boolValue]; (2617行目)
            Boolean onlyDICOM = dict.containsKey("onlyDICOM") ? (Boolean)dict.get("onlyDICOM") : true;
            Boolean copyFiles = dict.containsKey("copyFiles") ? (Boolean)dict.get("copyFiles") : false;
            logger.info("copyFilesThread: onlyDICOM={}, copyFiles={}", onlyDICOM, copyFiles);
            
            // HOROS-20240407準拠: __block BOOL studySelected = NO; (2618行目)
            java.util.concurrent.atomic.AtomicBoolean studySelected = new java.util.concurrent.atomic.AtomicBoolean(false);
            
            // HOROS-20240407準拠: copyFilesThreadを呼び出したスレッド（メインのインポートスレッド）を取得
            // このスレッドは既にThreadsManagerに登録されている（BrowserController.copyFilesIntoDatabaseIfNeededで登録済み）
            Thread mainImportThread = Thread.currentThread();
            com.jj.dicomviewer.threads.ThreadsManager tm = com.jj.dicomviewer.threads.ThreadsManager.defaultManager();
            
            // HOROS-20240407準拠: メインのインポートスレッドの初期ステータスを設定
            // 名前は既に「Indexing files...」に設定されている（BrowserController.copyFilesIntoDatabaseIfNeededで設定済み）
            tm.setThreadStatus(mainImportThread, "Indexing files...");
            tm.setThreadProgress(mainImportThread, -1.0); // indeterminateモード
            
            // HOROS-20240407準拠: NSArray *filesInput = [[dict objectForKey: @"filesInput"] sortedArrayUsingSelector:@selector(compare:)]; (2619行目)
            @SuppressWarnings("unchecked")
            List<String> filesInput = (List<String>)dict.get("filesInput");
            if (filesInput == null || filesInput.isEmpty()) {
                logger.warn("copyFilesThread: filesInput is null or empty");
                return;
            }
            logger.info("copyFilesThread: filesInput.size()={}", filesInput.size());
            List<String> sortedFiles = new java.util.ArrayList<>(filesInput);
            sortedFiles.sort(String::compareTo);
            
            // HOROS-20240407準拠: for( int i = 0; i < [filesInput count];) (2621行目)
            int i = 0;
            while (i < sortedFiles.size()) {
                // HOROS-20240407準拠: if ([[NSThread currentThread] isCancelled]) break; (2623行目)
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                
                // HOROS-20240407準拠: @autoreleasepool (2625行目)
                try {
                    // HOROS-20240407準拠: @try (2627行目)
                    try {
                        // HOROS-20240407準拠: NSMutableArray *copiedFiles = [NSMutableArray array]; (2629行目)
                        List<String> copiedFiles = new java.util.ArrayList<>();
                        
                        // HOROS-20240407準拠: NSTimeInterval lastGUIUpdate = 0; NSTimeInterval twentySeconds = [NSDate timeIntervalSinceReferenceDate] + 5; (2630-2631行目)
                        long lastGUIUpdate = 0;
                        long fiveSeconds = System.currentTimeMillis() + 5000; // actually fiveSeconds
                        
                        // HOROS-20240407準拠: for( ; i < [filesInput count] && twentySeconds > [NSDate timeIntervalSinceReferenceDate]; i++) (2633行目)
                        for (; i < sortedFiles.size() && System.currentTimeMillis() < fiveSeconds; i++) {
                            // HOROS-20240407準拠: if ([[NSThread currentThread] isCancelled]) break; (2635行目)
                            if (Thread.currentThread().isInterrupted()) {
                                break;
                            }
                            
                            // HOROS-20240407準拠: if( [NSDate timeIntervalSinceReferenceDate] - lastGUIUpdate > 1) (2637行目)
                            long now = System.currentTimeMillis();
                            if (now - lastGUIUpdate > 1000) {
                                lastGUIUpdate = now;
                                
                                // HOROS-20240407準拠: [NSThread currentThread].status = N2LocalizedSingularPluralCount((long)filesInput.count-i, NSLocalizedString(@"file left", nil), NSLocalizedString(@"files left", nil)); (2641行目)
                                // HOROS-20240407準拠: [NSThread currentThread].progress = float(i)/filesInput.count; (2642行目)
                                // HOROS-20240407準拠: メインのインポートスレッドのステータスと進捗を更新
                                long remaining = sortedFiles.size() - i;
                                String statusText = remaining == 1 ? "file left" : "files left";
                                tm.setThreadStatus(mainImportThread, remaining + " " + statusText);
                                tm.setThreadProgress(mainImportThread, (double)i / sortedFiles.size());
                            }
                            
                            // HOROS-20240407準拠: NSString *srcPath = [filesInput objectAtIndex: i], *dstPath = nil; (2645行目)
                            String srcPath = sortedFiles.get(i);
                            String dstPath = null;
                            
                            // HOROS-20240407準拠: if( copyFiles) (2647行目)
                            if (copyFiles) {
                                // HOROS-20240407準拠: ファイルコピー処理 (2649-2701行目)
                                // TODO: copyFiles=trueの場合の実装（ファイルコピー処理）
                                // 現時点では、copyFiles=falseの場合のみ実装
                            } else {
                                // HOROS-20240407準拠: copyFiles=falseの場合 (2703-2730行目)
                                java.io.File file = new java.io.File(srcPath);
                                if (file.exists() && file.isFile()) {
                                    Boolean mountedVolume = dict.containsKey("mountedVolume") ? (Boolean)dict.get("mountedVolume") : false;
                                    
                                    // HOROS-20240407準拠: if( [[dict objectForKey: @"mountedVolume"] boolValue]) (2707行目)
                                    if (mountedVolume) {
                                        // HOROS-20240407準拠: Pre-load for CD/DVD in cache (2709-2719行目)
                                        // TODO: DicomFileでPre-load（必要に応じて実装）
                                        copiedFiles.add(srcPath);
                                    } else {
                                        // HOROS-20240407準拠: 通常の場合はそのまま追加 (2721-2729行目)
                                        // HOROS-20240407準拠: if( [[NSUserDefaults standardUserDefaults] boolForKey: @"validateFilesBeforeImporting"] && [[dict objectForKey: @"mountedVolume"] boolValue] == NO) (2723行目)
                                        // TODO: validateFilesBeforeImportingのチェック
                                        // HOROS-20240407準拠: [copiedFiles addObject: srcPath]; (2728行目)
                                        copiedFiles.add(srcPath);
                                    }
                                }
                            }
                            
                            // HOROS-20240407準拠: if ([NSThread currentThread].isCancelled) break; (2733行目)
                            if (Thread.currentThread().isInterrupted()) {
                                break;
                            }
                        }
                        
                        // HOROS-20240407準拠: [queue addOperationWithBlock:^{ (2737行目)
                        final List<String> finalCopiedFiles = copiedFiles;
                        logger.info("copyFilesThread: submitting batch with {} files", finalCopiedFiles.size());
                        
                        queue.submit(() -> {
                            Thread workerThread = null;
                            try {
                                logger.info("copyFilesThread: queue operation started, files={}", finalCopiedFiles.size());
                                // HOROS-20240407準拠: NSThread* thread = [NSThread currentThread]; thread.name = NSLocalizedString(@"Adding files...", nil); [[ThreadsManager defaultManager] addThreadAndStart:thread]; (2738-2740行目)
                                workerThread = Thread.currentThread();
                                workerThread.setName("Adding files...");
                                tm.addThreadAndStart(workerThread);
                                
                                // HOROS-20240407準拠: BOOL succeed = YES; (2742行目)
                                boolean succeed = true;
                                
                                // HOROS-20240407準拠: #ifndef OSIRIX_LIGHT thread.status = NSLocalizedString(@"Validating the files...", nil); if( [[NSUserDefaults standardUserDefaults] boolForKey: @"validateFilesBeforeImporting"] && [[dict objectForKey: @"mountedVolume"] boolValue] == NO) succeed = [DicomDatabase testFiles: copiedFiles]; #endif (2744-2747行目)
                                // TODO: ファイル検証処理
                                
                                // HOROS-20240407準拠: NSArray *objects = nil; (2750行目)
                                // objects変数は後でfinalObjectsとして使用されるため、ここでは宣言しない
                                
                                // HOROS-20240407準拠: if( succeed) (2752行目)
                                if (succeed) {
                                    // HOROS-20240407準拠: thread.status = NSLocalizedString(@"Indexing the files...", nil); (2754行目)
                                    tm.setThreadStatus(workerThread, "Indexing the files...");
                                    
                                    // HOROS-20240407準拠: DicomDatabase *idatabase = self.isMainDatabase? self.independentDatabase : [self.mainDatabase independentDatabase]; (2756行目)
                                    DicomDatabase idatabase = this; // TODO: isMainDatabaseの判定とindependentDatabaseの取得
                                    
                                    // HOROS-20240407準拠: objects = [idatabase addFilesAtPaths:copiedFiles postNotifications:YES dicomOnly:onlyDICOM rereadExistingItems:YES generatedByOsiriX:NO importedFiles:YES returnArray:YES]; (2758行目)
                                    logger.info("copyFilesThread: calling addFilesAtPaths with {} files", finalCopiedFiles.size());
                                    final List<Object> finalObjects = idatabase.addFilesAtPaths(finalCopiedFiles, true, onlyDICOM, true, false, true, true);
                                    logger.info("copyFilesThread: addFilesAtPaths returned {} objects", finalObjects != null ? finalObjects.size() : 0);
                                    
                                    // HOROS-20240407準拠: DicomDatabase* mdatabase = self.isMainDatabase? self : self.mainDatabase; if( [[BrowserController currentBrowser] database] == mdatabase && [[dict objectForKey:@"addToAlbum"] boolValue]) (2760-2761行目)
                                    // TODO: アルバムへの追加処理 (2761-2784行目)
                                    
                                    // HOROS-20240407準拠: if ([objects count]) (2793行目)
                                    if (finalObjects != null && !finalObjects.isEmpty()) {
                                        // HOROS-20240407準拠: @try (2795行目)
                                        try {
                                            // HOROS-20240407準拠: BrowserController* bc = [BrowserController currentBrowser]; (2797行目)
                                            // HOROS-20240407準拠: UIを更新するため、BrowserControllerの参照を取得
                                            javax.swing.SwingUtilities.invokeLater(() -> {
                                                try {
                                                    // HOROS-20240407準拠: currentBrowserを使用してUIを更新
                                                    com.jj.dicomviewer.ui.BrowserController bc = com.jj.dicomviewer.ui.BrowserController.getCurrentBrowser();
                                                    if (bc != null) {
                                                        // HOROS-20240407準拠: DBリストとマトリックスを更新
                                                        bc.outlineViewRefresh();
                                                        bc.refreshMatrix(bc);
                                                        
                                                        // HOROS-20240407準拠: if( studySelected == NO) { studySelected = YES; if ([[dict objectForKey:@"selectStudy"] boolValue]) [bc performSelectorOnMainThread:@selector(selectStudyWithObjectID:) withObject: [objects objectAtIndex:0] waitUntilDone:NO]; } (2801-2805行目)
                                                        if (!studySelected.getAndSet(true)) {
                                                            Boolean selectStudy = dict.containsKey("selectStudy") ? (Boolean)dict.get("selectStudy") : false;
                                                            if (selectStudy && !finalObjects.isEmpty()) {
                                                                // TODO: BrowserController.selectStudyWithObjectIDの呼び出し
                                                                // Study選択処理は必要に応じて実装
                                                            }
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    // HOROS-20240407準拠: @catch (NSException *e) { N2LogExceptionWithStackTrace(e); } (2808-2809行目)
                                                    logger.error("Error updating UI after import", e);
                                                }
                                            });
                                        } catch (Exception e) {
                                            // HOROS-20240407準拠: @catch (NSException* e) { N2LogExceptionWithStackTrace(e); } (2812-2813行目)
                                            logger.error("Error in copyFilesThread operation", e);
                                        }
                                    } else {
                                        // オブジェクトが空の場合でもUIを更新（エラーハンドリングのため）
                                        javax.swing.SwingUtilities.invokeLater(() -> {
                                            try {
                                                com.jj.dicomviewer.ui.BrowserController bc = com.jj.dicomviewer.ui.BrowserController.getCurrentBrowser();
                                                if (bc != null) {
                                                    bc.outlineViewRefresh();
                                                }
                                            } catch (Exception e) {
                                                logger.error("Error updating UI after import (no objects)", e);
                                            }
                                        });
                                    }
                                } else if (copyFiles) {
                                    // HOROS-20240407準拠: else if( copyFiles) { for( NSString * f in copiedFiles) [[NSFileManager defaultManager]removeItemAtPath: f error: nil]; } (2787-2790行目)
                                    for (String f : finalCopiedFiles) {
                                        java.io.File file = new java.io.File(f);
                                        if (file.exists()) {
                                            file.delete();
                                        }
                                    }
                                }
                                
                                // HOROS-20240407準拠: [[ThreadsManager defaultManager] removeThread:thread]; (2817行目)
                                // HOROS-20240407準拠: NSOperationQueue threads don't finish after ablock execution, they're recycled
                                // HOROS-20240407準拠: ExecutorServiceのスレッドも再利用されるため、各バッチの最後でremoveThreadを呼ぶ
                            } catch (Exception e) {
                                // ConcurrentModificationException、ArrayIndexOutOfBoundsException、NullPointerExceptionは抑制
                                if (!(e instanceof java.util.ConcurrentModificationException) && 
                                    !(e instanceof ArrayIndexOutOfBoundsException) &&
                                    !(e instanceof NullPointerException)) {
                                    logger.error("Error in copyFilesThread queue operation", e);
                                }
                            } finally {
                                // HOROS-20240407準拠: 例外が発生しても必ずremoveThreadを呼ぶ（重複登録を防ぐため）
                                if (workerThread != null) {
                                    try {
                                        tm.removeThread(workerThread);
                                    } catch (Exception e) {
                                        logger.error("Error removing thread from ThreadsManager", e);
                                    }
                                }
                            }
                        });
                        
                        // HOROS-20240407準拠: if( [NSThread currentThread].isCancelled) break; (2820行目)
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }
                    } catch (Exception e) {
                        // HOROS-20240407準拠: @catch (NSException * e) { N2LogExceptionWithStackTrace(e); } (2823-2825行目)
                        logger.error("Error in copyFilesThread inner loop", e);
                    }
                } catch (Exception e) {
                    logger.error("Error in copyFilesThread", e);
                }
            }
            
            // HOROS-20240407準拠: if (queue.operationCount) { [NSThread currentThread].status = NSLocalizedString(@"Waiting for subtasks to complete...", nil); while (queue.operationCount) { [NSThread sleepForTimeInterval:0.05]; if( [[NSThread currentThread] isCancelled]) [queue cancelAllOperations]; } } (2830-2839行目)
            // HOROS-20240407準拠: メインのインポートスレッドのステータスを「Waiting for subtasks to complete...」に更新
            // 注意: ExecutorServiceにはoperationCountがないため、shutdown()前にステータスを更新
            tm.setThreadStatus(mainImportThread, "Waiting for subtasks to complete...");
            queue.shutdown();
            // HOROS-20240407準拠: メインのインポートスレッドのステータスを「Waiting for subtasks to complete...」に更新
            tm.setThreadStatus(mainImportThread, "Waiting for subtasks to complete...");
            try {
                while (!queue.awaitTermination(50, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    if (Thread.currentThread().isInterrupted()) {
                        queue.shutdownNow();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                queue.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            // HOROS-20240407準拠: if( [[dict objectForKey: @"ejectCDDVD"] boolValue] == YES && copyFiles == YES) { if( [[NSUserDefaults standardUserDefaults] boolForKey: @"EJECTCDDVD"]) [[NSWorkspace sharedWorkspace] unmountAndEjectDeviceAtPath: [filesInput objectAtIndex:0]]; } (2842-2845行目)
            // TODO: CD/DVDのイジェクト処理
            logger.info("copyFilesThread: finished");
        } catch (Exception e) {
            logger.error("Error in copyFilesThread", e);
            // デバッグログは条件付きで出力（必要に応じて有効化）
            // e.printStackTrace();
        }
    }
    
    // ========== エンティティ関連メソッド ==========
    
    /**
     * 画像エンティティを取得
     */
    public Object imageEntity() {
        // TODO: 実装（JPA/Hibernateの場合）
        return null;
    }
    
    /**
     * シリーズエンティティを取得
     */
    public Object seriesEntity() {
        // TODO: 実装
        return null;
    }
    
    /**
     * スタディエンティティを取得
     */
    public Object studyEntity() {
        // TODO: 実装
        return null;
    }
    
    /**
     * アルバムエンティティを取得
     */
    public Object albumEntity() {
        // TODO: 実装
        return null;
    }
    
    /**
     * ログエントリエンティティを取得
     */
    public Object logEntryEntity() {
        // TODO: 実装
        return null;
    }
    
    /**
     * マネージドオブジェクトコンテキストを取得
     */
    public Object managedObjectContext() {
        // TODO: 実装（JPA/Hibernateの場合）
        return null;
    }
    
    /**
     * patientUIDでStudyを検索
     * HOROS-20240407準拠: [idatabase objectsForEntity: idatabase.studyEntity predicate: [NSPredicate predicateWithFormat: @"(patientUID BEGINSWITH[cd] %@)", studySelected.patientUID]]
     */
    public List<DicomStudy> getStudiesByPatientUID(String patientUID) {
        List<DicomStudy> result = new ArrayList<>();
        if (patientUID == null || patientUID.isEmpty() || sqliteConnection == null) {
            return result;
        }
        
        // HOROS-20240407準拠: patientUID BEGINSWITH[cd]（大文字小文字を区別しない前方一致）
        String sql = "SELECT * FROM Study WHERE UPPER(patientUID) LIKE UPPER(?) ORDER BY date DESC";
        try (PreparedStatement pstmt = sqliteConnection.prepareStatement(sql)) {
            pstmt.setString(1, patientUID + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    DicomStudy study = new DicomStudy();
                    int studyDbId = rs.getInt("id");
                    study.setStudyInstanceUID(rs.getString("studyInstanceUID"));
                    study.setName(rs.getString("name"));
                    study.setPatientID(rs.getString("patientID"));
                    study.setPatientUID(rs.getString("patientUID"));
                    study.setModality(rs.getString("modality"));
                    study.setNumberOfImages(rs.getInt("numberOfImages"));
                    study.setComment(rs.getString("comment"));
                    study.setAccessionNumber(rs.getString("accessionNumber"));
                    study.setReferringPhysician(rs.getString("referringPhysician"));
                    study.setPerformingPhysician(rs.getString("performingPhysician"));
                    study.setInstitutionName(rs.getString("institutionName"));
                    study.setStudyID(rs.getString("studyID"));
                    study.setStudyName(rs.getString("studyName"));
                    
                    // DBのIDを保存（遅延読み込み用）
                    study.setId(String.valueOf(studyDbId));
                    
                    String dateStr = rs.getString("date");
                    if (dateStr != null && !dateStr.isEmpty()) {
                        try {
                            study.setDate(LocalDateTime.parse(dateStr));
                        } catch (Exception e) {
                            // 日付パースエラーは無視
                        }
                    }
                    
                    result.add(study);
                }
            }
        } catch (SQLException e) {
            logger.error("Error querying studies by patientUID: " + patientUID, e);
        }
        return result;
    }
    
    /**
     * マネージドオブジェクトモデルを取得
     */
    public Object managedObjectModel() {
        // TODO: 実装
        return null;
    }
    
    /**
     * データベースを保存
     */
    public boolean save(Object error) {
        // TODO: 実装
        return true;
    }
    
    /**
     * ロック
     */
    public void lock() {
        processFilesLock.lock();
    }
    
    /**
     * アンロック
     */
    public void unlock() {
        processFilesLock.unlock();
    }
}


