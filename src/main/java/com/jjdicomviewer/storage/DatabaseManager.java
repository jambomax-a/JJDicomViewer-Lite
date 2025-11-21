package com.jjdicomviewer.storage;

import com.jjdicomviewer.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Path;

/**
 * SQLiteデータベース管理クラス
 */
public class DatabaseManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    
    private Connection connection;
    private final Path dbPath;
    
    public DatabaseManager() {
        // AppConfigからデータベースパスを取得
        AppConfig appConfig = AppConfig.getInstance();
        this.dbPath = appConfig.getDatabasePath();
        initializeDatabase();
    }
    
    public DatabaseManager(Path dbPath) {
        this.dbPath = dbPath;
        initializeDatabase();
    }
    
    /**
     * データベースを初期化し、テーブルを作成
     */
    private void initializeDatabase() {
        try {
            // データベースディレクトリが存在しない場合は作成
            if (dbPath.getParent() != null) {
                java.nio.file.Files.createDirectories(dbPath.getParent());
            }
            
            String url = "jdbc:sqlite:" + dbPath.toString();
            connection = DriverManager.getConnection(url);
            
            createTables();
            logger.info("データベースを初期化しました: {}", dbPath);
        } catch (SQLException e) {
            logger.error("データベースの初期化に失敗しました", e);
            throw new RuntimeException("データベースの初期化に失敗しました", e);
        } catch (java.io.IOException e) {
            logger.error("データベースディレクトリの作成に失敗しました", e);
            throw new RuntimeException("データベースディレクトリの作成に失敗しました", e);
        }
    }
    
    /**
     * テーブルを作成
     */
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            
            // Studies テーブル
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS studies (
                    study_instance_uid TEXT PRIMARY KEY,
                    patient_id TEXT,
                    patient_name TEXT,
                    patient_birth_date TEXT,
                    patient_sex TEXT,
                    study_date TEXT,
                    study_time TEXT,
                    study_description TEXT,
                    accession_number TEXT,
                    referring_physician_name TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Series テーブル
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS series (
                    series_instance_uid TEXT PRIMARY KEY,
                    study_instance_uid TEXT NOT NULL,
                    series_number INTEGER,
                    modality TEXT,
                    series_date TEXT,
                    series_time TEXT,
                    series_description TEXT,
                    body_part_examined TEXT,
                    patient_position TEXT,
                    instance_count INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (study_instance_uid) REFERENCES studies(study_instance_uid) ON DELETE CASCADE
                )
            """);
            
            // Instances テーブル
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS instances (
                    sop_instance_uid TEXT PRIMARY KEY,
                    series_instance_uid TEXT NOT NULL,
                    instance_number INTEGER,
                    sop_class_uid TEXT,
                    file_path TEXT NOT NULL,
                    file_size INTEGER,
                    transfer_syntax_uid TEXT,
                    rows INTEGER,
                    columns INTEGER,
                    bits_allocated INTEGER,
                    bits_stored INTEGER,
                    samples_per_pixel INTEGER,
                    photometric_interpretation TEXT,
                    window_center TEXT,
                    window_width TEXT,
                    rescale_slope REAL,
                    rescale_intercept REAL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (series_instance_uid) REFERENCES series(series_instance_uid) ON DELETE CASCADE
                )
            """);
            
            // インデックスの作成
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_studies_patient_id ON studies(patient_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_studies_study_date ON studies(study_date)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_series_study_uid ON series(study_instance_uid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_instances_series_uid ON instances(series_instance_uid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_instances_file_path ON instances(file_path)");
            
            logger.info("データベーステーブルを作成しました");
        }
    }
    
    /**
     * データベース接続を取得
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                String url = "jdbc:sqlite:" + dbPath.toString();
                connection = DriverManager.getConnection(url);
            }
            return connection;
        } catch (SQLException e) {
            logger.error("データベース接続の取得に失敗しました", e);
            throw new RuntimeException("データベース接続の取得に失敗しました", e);
        }
    }
    
    /**
     * データベース接続を閉じる
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("データベース接続を閉じました");
            }
        } catch (SQLException e) {
            logger.error("データベース接続のクローズに失敗しました", e);
        }
    }
    
    /**
     * データベースパスを取得
     */
    public Path getDbPath() {
        return dbPath;
    }
}

