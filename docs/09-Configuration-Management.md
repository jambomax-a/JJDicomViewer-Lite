# 設定管理 - Java移植ドキュメント

## 1. 概要

このドキュメントは、DICOMデータ格納フォルダとDB保存フォルダの設定管理、およびアプリケーション設定の実装方法をまとめたものです。

## 2. 設定ファイル構造

### 2.1 設定ファイルの場所

```
JJDicomViewer/
├── config/
│   ├── application.properties    # アプリケーション設定
│   └── database.properties       # データベース設定
└── data/
    ├── dicom/                    # DICOMデータ格納フォルダ（デフォルト）
    └── database/                 # データベース保存フォルダ（デフォルト）
```

### 2.2 設定ファイルの内容

**application.properties**:
```properties
# アプリケーション設定
app.name=JJDicomViewer
app.version=1.0.0

# データフォルダ設定
data.dicom.folder=${user.home}/JJDicomViewer/data/dicom
data.database.folder=${user.home}/JJDicomViewer/data/database

# UI設定
ui.language=ja
ui.theme=system

# ウィンドウ設定
window.width=1200
window.height=800
window.maximized=false

# 画像表示設定
image.default.window.level=0
image.default.window.width=0
image.interpolation=bilinear
```

**database.properties**:
```properties
# データベース設定
database.path=${data.database.folder}/database.db
database.backup.enabled=true
database.backup.interval=24
database.backup.count=7
```

## 3. 設定管理クラスの実装

### 3.1 設定マネージャー

```java
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class ConfigurationManager {
    private static final String CONFIG_DIR = "config";
    private static final String APP_PROPERTIES = "application.properties";
    private static final String DB_PROPERTIES = "database.properties";
    
    private Properties appProperties;
    private Properties dbProperties;
    private Path configDir;
    private Path appPropertiesFile;
    private Path dbPropertiesFile;
    
    private static ConfigurationManager instance;
    
    private ConfigurationManager() {
        initialize();
    }
    
    public static ConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (ConfigurationManager.class) {
                if (instance == null) {
                    instance = new ConfigurationManager();
                }
            }
        }
        return instance;
    }
    
    private void initialize() {
        // 設定ディレクトリの作成
        configDir = Paths.get(CONFIG_DIR);
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config directory", e);
        }
        
        appPropertiesFile = configDir.resolve(APP_PROPERTIES);
        dbPropertiesFile = configDir.resolve(DB_PROPERTIES);
        
        // デフォルト設定の読み込み
        loadDefaultProperties();
        
        // 設定ファイルの読み込み
        loadProperties();
    }
    
    private void loadDefaultProperties() {
        appProperties = new Properties();
        
        // デフォルト値の設定
        String userHome = System.getProperty("user.home");
        appProperties.setProperty("data.dicom.folder", 
            Paths.get(userHome, "JJDicomViewer", "data", "dicom").toString());
        appProperties.setProperty("data.database.folder", 
            Paths.get(userHome, "JJDicomViewer", "data", "database").toString());
        appProperties.setProperty("ui.language", "ja");
        appProperties.setProperty("ui.theme", "system");
        appProperties.setProperty("window.width", "1200");
        appProperties.setProperty("window.height", "800");
        appProperties.setProperty("window.maximized", "false");
        appProperties.setProperty("image.default.window.level", "0");
        appProperties.setProperty("image.default.window.width", "0");
        appProperties.setProperty("image.interpolation", "bilinear");
        
        dbProperties = new Properties();
        dbProperties.setProperty("database.backup.enabled", "true");
        dbProperties.setProperty("database.backup.interval", "24");
        dbProperties.setProperty("database.backup.count", "7");
    }
    
    private void loadProperties() {
        // アプリケーション設定の読み込み
        if (Files.exists(appPropertiesFile)) {
            try (InputStream is = Files.newInputStream(appPropertiesFile)) {
                appProperties.load(is);
            } catch (IOException e) {
                System.err.println("Failed to load application properties: " + e.getMessage());
            }
        } else {
            // デフォルト設定を保存
            saveProperties();
        }
        
        // データベース設定の読み込み
        if (Files.exists(dbPropertiesFile)) {
            try (InputStream is = Files.newInputStream(dbPropertiesFile)) {
                dbProperties.load(is);
            } catch (IOException e) {
                System.err.println("Failed to load database properties: " + e.getMessage());
            }
        } else {
            saveDatabaseProperties();
        }
        
        // 変数展開（${user.home}等）
        expandVariables();
    }
    
    private void expandVariables() {
        String userHome = System.getProperty("user.home");
        
        // アプリケーション設定の変数展開
        for (String key : appProperties.stringPropertyNames()) {
            String value = appProperties.getProperty(key);
            if (value != null) {
                value = value.replace("${user.home}", userHome);
                value = value.replace("${data.dicom.folder}", 
                    getDicomDataFolder().toString());
                value = value.replace("${data.database.folder}", 
                    getDatabaseFolder().toString());
                appProperties.setProperty(key, value);
            }
        }
        
        // データベース設定の変数展開
        for (String key : dbProperties.stringPropertyNames()) {
            String value = dbProperties.getProperty(key);
            if (value != null) {
                value = value.replace("${user.home}", userHome);
                value = value.replace("${data.database.folder}", 
                    getDatabaseFolder().toString());
                dbProperties.setProperty(key, value);
            }
        }
    }
    
    public void saveProperties() {
        try (OutputStream os = Files.newOutputStream(appPropertiesFile)) {
            appProperties.store(os, "JJDicomViewer Application Properties");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save application properties", e);
        }
    }
    
    public void saveDatabaseProperties() {
        try (OutputStream os = Files.newOutputStream(dbPropertiesFile)) {
            dbProperties.store(os, "JJDicomViewer Database Properties");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save database properties", e);
        }
    }
    
    // ゲッター/セッター
    public Path getDicomDataFolder() {
        String path = appProperties.getProperty("data.dicom.folder");
        return Paths.get(path);
    }
    
    public void setDicomDataFolder(Path folder) {
        appProperties.setProperty("data.dicom.folder", folder.toString());
        saveProperties();
    }
    
    public Path getDatabaseFolder() {
        String path = appProperties.getProperty("data.database.folder");
        return Paths.get(path);
    }
    
    public void setDatabaseFolder(Path folder) {
        appProperties.setProperty("data.database.folder", folder.toString());
        // データベース設定も更新
        String dbPath = Paths.get(folder, "database.db").toString();
        dbProperties.setProperty("database.path", dbPath);
        saveProperties();
        saveDatabaseProperties();
    }
    
    public String getLanguage() {
        return appProperties.getProperty("ui.language", "ja");
    }
    
    public void setLanguage(String language) {
        appProperties.setProperty("ui.language", language);
        saveProperties();
    }
    
    public String getProperty(String key) {
        return appProperties.getProperty(key);
    }
    
    public String getProperty(String key, String defaultValue) {
        return appProperties.getProperty(key, defaultValue);
    }
    
    public void setProperty(String key, String value) {
        appProperties.setProperty(key, value);
        saveProperties();
    }
    
    public String getDatabaseProperty(String key) {
        return dbProperties.getProperty(key);
    }
    
    public String getDatabaseProperty(String key, String defaultValue) {
        return dbProperties.getProperty(key, defaultValue);
    }
    
    public void setDatabaseProperty(String key, String value) {
        dbProperties.setProperty(key, value);
        saveDatabaseProperties();
    }
}
```

## 4. 設定ダイアログ

### 4.1 設定ダイアログの実装

```java
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SettingsDialog extends JDialog {
    private ConfigurationManager configManager;
    private JTextField dicomFolderField;
    private JTextField databaseFolderField;
    private JComboBox<String> languageCombo;
    
    public SettingsDialog(JFrame parent) {
        super(parent, "Settings", true);
        this.configManager = ConfigurationManager.getInstance();
        
        initializeUI();
        loadSettings();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // メインパネル
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // DICOMデータフォルダ
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("DICOM Data Folder:"), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        dicomFolderField = new JTextField(30);
        mainPanel.add(dicomFolderField, gbc);
        
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton dicomBrowseButton = new JButton("Browse...");
        dicomBrowseButton.addActionListener(e -> browseDicomFolder());
        mainPanel.add(dicomBrowseButton, gbc);
        
        // データベースフォルダ
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Database Folder:"), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        databaseFolderField = new JTextField(30);
        mainPanel.add(databaseFolderField, gbc);
        
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton dbBrowseButton = new JButton("Browse...");
        dbBrowseButton.addActionListener(e -> browseDatabaseFolder());
        mainPanel.add(dbBrowseButton, gbc);
        
        // 言語設定
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Language:"), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        languageCombo = new JComboBox<>(new String[]{"日本語", "English"});
        mainPanel.add(languageCombo, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // ボタンパネル
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> saveSettings());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(getParent());
    }
    
    private void loadSettings() {
        dicomFolderField.setText(configManager.getDicomDataFolder().toString());
        databaseFolderField.setText(configManager.getDatabaseFolder().toString());
        
        String lang = configManager.getLanguage();
        languageCombo.setSelectedIndex(lang.equals("ja") ? 0 : 1);
    }
    
    private void browseDicomFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(configManager.getDicomDataFolder().toFile());
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            dicomFolderField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void browseDatabaseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(configManager.getDatabaseFolder().toFile());
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            databaseFolderField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void saveSettings() {
        try {
            Path dicomFolder = Paths.get(dicomFolderField.getText());
            Path databaseFolder = Paths.get(databaseFolderField.getText());
            
            // フォルダの存在確認と作成
            if (!Files.exists(dicomFolder)) {
                int result = JOptionPane.showConfirmDialog(this,
                    "DICOM data folder does not exist. Create it?",
                    "Create Folder", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    Files.createDirectories(dicomFolder);
                } else {
                    return;
                }
            }
            
            if (!Files.exists(databaseFolder)) {
                int result = JOptionPane.showConfirmDialog(this,
                    "Database folder does not exist. Create it?",
                    "Create Folder", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    Files.createDirectories(databaseFolder);
                } else {
                    return;
                }
            }
            
            // 設定の保存
            configManager.setDicomDataFolder(dicomFolder);
            configManager.setDatabaseFolder(databaseFolder);
            
            String language = languageCombo.getSelectedIndex() == 0 ? "ja" : "en";
            configManager.setLanguage(language);
            
            JOptionPane.showMessageDialog(this,
                "Settings saved. Please restart the application for changes to take effect.",
                "Settings Saved", JOptionPane.INFORMATION_MESSAGE);
            
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Failed to save settings: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
```

## 5. データベース初期化の更新

### 5.1 設定に基づくデータベース初期化

```java
public class DicomDatabase {
    private ConfigurationManager configManager;
    
    public DicomDatabase() throws SQLException {
        this.configManager = ConfigurationManager.getInstance();
        Path databaseFolder = configManager.getDatabaseFolder();
        Path databasePath = databaseFolder.resolve("database.db");
        
        // データベース接続
        String url = "jdbc:sqlite:" + databasePath.toString();
        connection = DriverManager.getConnection(url);
        
        // テーブル作成
        createTables();
    }
    
    // ... 既存のコード ...
}
```

## 6. データフォルダの検証

### 6.1 フォルダ検証ユーティリティ

```java
public class FolderValidator {
    public static ValidationResult validateDicomFolder(Path folder) {
        ValidationResult result = new ValidationResult();
        
        if (folder == null) {
            result.addError("Folder path is null");
            return result;
        }
        
        if (!Files.exists(folder)) {
            result.addError("Folder does not exist: " + folder);
            return result;
        }
        
        if (!Files.isDirectory(folder)) {
            result.addError("Path is not a directory: " + folder);
            return result;
        }
        
        if (!Files.isReadable(folder)) {
            result.addError("Folder is not readable: " + folder);
            return result;
        }
        
        if (!Files.isWritable(folder)) {
            result.addWarning("Folder is not writable: " + folder);
        }
        
        result.setValid(true);
        return result;
    }
    
    public static ValidationResult validateDatabaseFolder(Path folder) {
        ValidationResult result = new ValidationResult();
        
        if (folder == null) {
            result.addError("Folder path is null");
            return result;
        }
        
        if (!Files.exists(folder)) {
            result.addError("Folder does not exist: " + folder);
            return result;
        }
        
        if (!Files.isDirectory(folder)) {
            result.addError("Path is not a directory: " + folder);
            return result;
        }
        
        if (!Files.isWritable(folder)) {
            result.addError("Folder is not writable: " + folder);
            return result;
        }
        
        result.setValid(true);
        return result;
    }
}

class ValidationResult {
    private boolean valid = false;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    
    public List<String> getErrors() { return errors; }
    public void addError(String error) { errors.add(error); }
    
    public List<String> getWarnings() { return warnings; }
    public void addWarning(String warning) { warnings.add(warning); }
    
    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
}
```

## 7. まとめ

設定管理の実装では：

1. **設定ファイル**による永続化
2. **設定ダイアログ**によるユーザー設定
3. **フォルダ検証**による安全性確保
4. **変数展開**による柔軟な設定

これにより、ユーザーはDICOMデータフォルダとデータベースフォルダを自由に変更できます。

