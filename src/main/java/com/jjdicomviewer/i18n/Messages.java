package com.jjdicomviewer.i18n;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jjdicomviewer.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 国際化メッセージ管理クラス
 * YAMLファイルからメッセージを読み込み、多言語対応を可能にする
 */
public class Messages {
    private static final Logger logger = LoggerFactory.getLogger(Messages.class);
    private static final String DEFAULT_LANGUAGE = "ja";
    private static final String MESSAGES_FILE_PREFIX = "/i18n/messages_";
    private static final String MESSAGES_FILE_SUFFIX = ".yaml";
    
    private static Messages instance;
    private final Map<String, Object> messages;
    private final String language;
    
    private Messages(String language) {
        this.language = language != null ? language : DEFAULT_LANGUAGE;
        this.messages = loadMessages(this.language);
    }
    
    /**
     * デフォルト言語でMessagesインスタンスを取得（AppConfigから言語設定を読み込む）
     */
    public static Messages getInstance() {
        AppConfig config = AppConfig.getInstance();
        String lang = config.getLanguage();
        if (instance == null || !instance.language.equals(lang)) {
            instance = new Messages(lang);
        }
        return instance;
    }
    
    /**
     * 指定された言語でMessagesインスタンスを取得
     */
    public static Messages getInstance(String language) {
        if (instance == null || !instance.language.equals(language)) {
            instance = new Messages(language);
        }
        return instance;
    }
    
    /**
     * Messagesインスタンスを再読み込み（言語変更時に使用）
     */
    public static void reload() {
        AppConfig config = AppConfig.getInstance();
        String lang = config.getLanguage();
        instance = new Messages(lang);
    }
    
    /**
     * 現在の言語を取得
     */
    public String getLanguage() {
        return language;
    }
    
    /**
     * メッセージを取得（キーはドット区切りの階層パス）
     * 例: get("app.title") または get("menu.file.import")
     */
    public String get(String key) {
        Object value = getValue(key);
        if (value == null) {
            logger.warn("メッセージキーが見つかりません: {}", key);
            return "[" + key + "]";
        }
        return value.toString();
    }
    
    /**
     * メッセージを取得（パラメータ付き）
     * 例: get("slice.label", 1, 10) -> "スライス: 1/10"
     */
    public String get(String key, Object... args) {
        String message = get(key);
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }
        return message;
    }
    
    /**
     * 階層キーから値を取得
     */
    @SuppressWarnings("unchecked")
    private Object getValue(String key) {
        String[] parts = key.split("\\.");
        Object current = messages;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
                if (current == null) {
                    return null;
                }
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * YAMLファイルからメッセージを読み込み
     * 優先順位:
     * 1. 外部ファイル（インストール先のlanguageフォルダ）
     * 2. リソース（jar内）
     * 3. デフォルト（日本語）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadMessages(String lang) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        String fileName = "messages_" + lang + MESSAGES_FILE_SUFFIX;
        String resourcePath = MESSAGES_FILE_PREFIX + lang + MESSAGES_FILE_SUFFIX;
        
        // まず、リソースファイルを外部ファイルにコピーして更新（最新版を確保）
        try {
            AppConfig config = AppConfig.getInstance();
            Path languageDir = config.getLanguageDirectory();
            logger.info("言語ディレクトリのパス: {}", languageDir);
            
            // languageディレクトリが存在しない場合は作成
            if (!Files.exists(languageDir)) {
                try {
                    Files.createDirectories(languageDir);
                    logger.info("言語ディレクトリを作成しました: {}", languageDir);
                } catch (Exception e) {
                    logger.error("言語ディレクトリの作成に失敗しました: {} - エラー: {}", languageDir, e.getMessage(), e);
                }
            }
            
            // リソースファイルが存在する場合、外部ファイルにコピー（常に更新）
            Path externalFile = languageDir.resolve(fileName);
            try (InputStream resourceIs = Messages.class.getResourceAsStream(resourcePath)) {
                if (resourceIs != null) {
                    Files.copy(resourceIs, externalFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    logger.info("言語ファイルを外部ファイルに更新しました: {} (言語: {})", externalFile, lang);
                }
            }
        } catch (Exception e) {
            // 外部ファイルへのコピーに失敗しても、リソースから読み込めるので続行
            logger.warn("外部ファイルへのコピーに失敗しました（リソースから読み込みを続行）: {}", e.getMessage());
        }
        
        // 1. 外部ファイル（インストール先のlanguageフォルダ）から読み込みを試す
        try {
            AppConfig config = AppConfig.getInstance();
            Path languageDir = config.getLanguageDirectory();
            Path externalFile = languageDir.resolve(fileName);
            
            if (Files.exists(externalFile)) {
                Map<String, Object> loaded = mapper.readValue(externalFile.toFile(), Map.class);
                logger.info("外部メッセージファイルを読み込みました: {} (言語: {})", externalFile, lang);
                return loaded;
            }
        } catch (Exception e) {
            logger.debug("外部メッセージファイルの読み込みに失敗（リソースから読み込みを試みます）: {}", e.getMessage());
        }
        
        // 2. リソース（jar内）から読み込みを試す
        try (InputStream is = Messages.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                Map<String, Object> loaded = mapper.readValue(is, Map.class);
                logger.info("リソースメッセージファイルを読み込みました: {} (言語: {})", resourcePath, lang);
                return loaded;
            }
        } catch (IOException e) {
            logger.debug("リソースメッセージファイルの読み込みに失敗: {}", resourcePath);
        }
        
        // 3. デフォルト（日本語）を試す
        if (!lang.equals(DEFAULT_LANGUAGE)) {
            logger.warn("メッセージファイルが見つかりません: {}。デフォルト（日本語）を使用します。", fileName);
            return loadMessages(DEFAULT_LANGUAGE);
        }
        
        // デフォルト（日本語）も見つからない場合
        logger.error("デフォルトメッセージファイルも見つかりません: messages_{}.yaml", DEFAULT_LANGUAGE);
        return new HashMap<>();
    }
}

