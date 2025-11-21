package com.jjdicomviewer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * アプリケーション設定管理クラス
 */
public class AppConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final String CONFIG_DIR_NAME = ".jjdicomviewerlite";
    private static final String CONFIG_FILE_NAME = "config.yaml";
    
    private static AppConfig instance;
    
    private Path configFilePath;
    private ConfigData configData;
    private final ObjectMapper yamlMapper;
    
    private AppConfig() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.configFilePath = Paths.get(System.getProperty("user.home"), CONFIG_DIR_NAME, CONFIG_FILE_NAME);
        loadConfig();
    }
    
    /**
     * シングルトンインスタンスを取得
     */
    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }
    
    /**
     * 設定を読み込む
     */
    private void loadConfig() {
        try {
            // 設定ディレクトリが存在しない場合は作成
            if (configFilePath.getParent() != null) {
                Files.createDirectories(configFilePath.getParent());
            }
            
            // 設定ファイルが存在する場合は読み込む
            if (Files.exists(configFilePath)) {
                configData = yamlMapper.readValue(configFilePath.toFile(), ConfigData.class);
                logger.info("設定ファイルを読み込みました: {}", configFilePath);
            } else {
                // デフォルト設定を作成
                configData = new ConfigData();
                saveConfig();
                logger.info("デフォルト設定を作成しました: {}", configFilePath);
            }
        } catch (IOException e) {
            logger.error("設定ファイルの読み込みに失敗しました", e);
            // デフォルト設定を使用
            configData = new ConfigData();
        }
    }
    
    /**
     * 設定を保存
     */
    public void saveConfig() {
        try {
            // 設定ディレクトリが存在しない場合は作成
            if (configFilePath.getParent() != null) {
                Files.createDirectories(configFilePath.getParent());
            }
            
            yamlMapper.writeValue(configFilePath.toFile(), configData);
            logger.info("設定ファイルを保存しました: {}", configFilePath);
        } catch (IOException e) {
            logger.error("設定ファイルの保存に失敗しました", e);
            throw new RuntimeException("設定ファイルの保存に失敗しました", e);
        }
    }
    
    /**
     * ストレージベースパスを取得
     */
    public Path getStorageBasePath() {
        if (configData.getStoragePath() != null && !configData.getStoragePath().isEmpty()) {
            return Paths.get(configData.getStoragePath());
        }
        // デフォルト: ~/.jjdicomviewerlite/data
        return Paths.get(System.getProperty("user.home"), CONFIG_DIR_NAME, "data");
    }
    
    /**
     * ストレージパスを設定
     */
    public void setStoragePath(String storagePath) {
        configData.setStoragePath(storagePath);
        saveConfig();
        logger.info("ストレージパスを変更しました: {}", storagePath);
    }
    
    /**
     * データベースパスを取得
     */
    public Path getDatabasePath() {
        if (configData.getDatabasePath() != null && !configData.getDatabasePath().isEmpty()) {
            return Paths.get(configData.getDatabasePath());
        }
        // デフォルト: ~/.jjdicomviewerlite/jjdicomviewerlite.db
        return Paths.get(System.getProperty("user.home"), CONFIG_DIR_NAME, "jjdicomviewerlite.db");
    }
    
    /**
     * データベースパスを設定
     */
    public void setDatabasePath(String databasePath) {
        configData.setDatabasePath(databasePath);
        saveConfig();
        logger.info("データベースパスを変更しました: {}", databasePath);
    }
    
    /**
     * 言語設定を取得
     */
    public String getLanguage() {
        if (configData.getLanguage() != null && !configData.getLanguage().isEmpty()) {
            return configData.getLanguage();
        }
        // デフォルト: 日本語
        return "ja";
    }
    
    /**
     * 言語設定を設定
     */
    public void setLanguage(String language) {
        configData.setLanguage(language);
        saveConfig();
        logger.info("言語設定を変更しました: {}", language);
    }
    
    /**
     * インストール先の言語ファイルディレクトリパスを取得
     * jpackageでインストールされた場合のパスを取得
     */
    public Path getLanguageDirectory() {
        // jpackageでインストールされた場合、--resource-dirで指定したディレクトリが
        // アプリケーションのルートにコピーされる
        // Windows: <インストール先>\JJDicomViewer-Lite\language
        // macOS: <App>.app/Contents/app/language
        // Linux: <インストール先>/JJDicomViewer-Lite/lib/app/language
        
        // jpackageでインストールされた場合のパスを取得
        // app.dirはjpackageで設定されるプロパティ（存在しない場合もある）
        String appDir = System.getProperty("app.dir");
        if (appDir != null && !appDir.isEmpty()) {
            Path langDir = Paths.get(appDir).resolve("language");
            // 存在チェックを削除：存在しなくてもインストール先を優先
            logger.info("app.dirプロパティから言語ディレクトリを取得: {}", langDir);
            return langDir;
        }
        
        // 実行中のJARファイルの親ディレクトリを取得
        try {
            java.net.URI uri = AppConfig.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            Path jarFile = Paths.get(uri);
            if (jarFile.toString().endsWith(".jar")) {
                Path parentDir = jarFile.getParent();
                if (parentDir != null) {
                    // jpackageの場合、JARは通常 app/ または lib/app/ に配置される
                    // --resource-dirで指定したディレクトリは app/ にコピーされる可能性がある
                    
                    // Program Filesへの書き込みは権限がないため、ユーザーのホームディレクトリにフォールバック
                    String parentPath = parentDir.toString();
                    if (parentPath.contains("Program Files") || parentPath.contains("Program Files (x86)")) {
                        logger.warn("Program Filesへの書き込みは権限がないため、ユーザーホームディレクトリを使用します: {}", parentPath);
                        // ユーザーのホームディレクトリにフォールバック
                        Path fallbackDir = Paths.get(System.getProperty("user.home"), CONFIG_DIR_NAME, "language");
                        logger.info("フォールバックとしてユーザーホームディレクトリを使用: {}", fallbackDir);
                        return fallbackDir;
                    }
                    
                    // Linuxの/optへの書き込みは権限がないため、ユーザーのホームディレクトリにフォールバック
                    if (parentPath.startsWith("/opt/") || parentPath.startsWith("/usr/")) {
                        logger.warn("/optまたは/usrへの書き込みは権限がないため、ユーザーホームディレクトリを使用します: {}", parentPath);
                        // ユーザーのホームディレクトリにフォールバック
                        Path fallbackDir = Paths.get(System.getProperty("user.home"), CONFIG_DIR_NAME, "language");
                        logger.info("フォールバックとしてユーザーホームディレクトリを使用: {}", fallbackDir);
                        return fallbackDir;
                    }
                    
                    // %LOCALAPPDATA%\Programs\ の場合も書き込み可能なので、そのまま使用
                    // （--win-per-user-installを使用した場合のインストール先）
                    
                    // 1. app/language を優先（存在チェックを削除：存在しなくてもインストール先を優先）
                    Path appDirectory = parentDir; // app/ ディレクトリ
                    Path langDir = appDirectory.resolve("language");
                    logger.info("JARの親ディレクトリから言語ディレクトリを取得: {} (JAR: {})", langDir, jarFile);
                    return langDir;
                }
            }
        } catch (Exception e) {
            logger.warn("JARファイルのパス取得に失敗: {}", e.getMessage());
        }
        
        // 開発環境の場合は、プロジェクトルートのlanguageフォルダを使用
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isEmpty()) {
            Path langDir = Paths.get(userDir, "language");
            logger.info("開発環境の言語ディレクトリを使用: {}", langDir);
            return langDir;
        }
        
        // 最後のフォールバック: user.home（通常は使用されない）
        Path fallbackDir = Paths.get(System.getProperty("user.home"), CONFIG_DIR_NAME, "language");
        logger.warn("フォールバックとしてユーザーホームディレクトリを使用: {}", fallbackDir);
        return fallbackDir;
    }
    
    /**
     * 設定データクラス
     */
    public static class ConfigData {
        private String storagePath;
        private String databasePath;
        private String language;
        
        public ConfigData() {
            // デフォルト値は空（getStorageBasePath/getDatabasePathで処理）
        }
        
        public String getStoragePath() {
            return storagePath;
        }
        
        public void setStoragePath(String storagePath) {
            this.storagePath = storagePath;
        }
        
        public String getDatabasePath() {
            return databasePath;
        }
        
        public void setDatabasePath(String databasePath) {
            this.databasePath = databasePath;
        }
        
        public String getLanguage() {
            return language;
        }
        
        public void setLanguage(String language) {
            this.language = language;
        }
    }
}

