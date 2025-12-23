# 国際化（i18n） - Java移植ドキュメント

## 1. 概要

このドキュメントは、日本語と英語の表記ファイルを使用した国際化（i18n）機能の実装方法をまとめたものです。

## 2. リソースファイル構造

### 2.1 ファイル配置

```
JJDicomViewer/
├── resources/
│   └── i18n/
│       ├── messages_ja.properties    # 日本語
│       └── messages_en.properties    # 英語
```

### 2.2 リソースファイルの内容

**messages_ja.properties**:
```properties
# アプリケーション
app.name=JJDicomViewer
app.title=DICOMビューワー

# メニュー
menu.file=ファイル(&F)
menu.file.open=開く(&O)
menu.file.import=インポート(&I)
menu.file.export=エクスポート(&E)
menu.file.exit=終了(&X)

menu.edit=編集(&E)
menu.edit.settings=設定(&S)

menu.view=表示(&V)
menu.view.zoom.in=拡大(&I)
menu.view.zoom.out=縮小(&O)
menu.view.zoom.fit=フィット(&F)
menu.view.window.level=ウィンドウレベル(&L)

menu.tools=ツール(&T)
menu.tools.roi=ROI(&R)
menu.tools.measure=測定(&M)

menu.help=ヘルプ(&H)
menu.help.about=バージョン情報(&A)

# ボタン
button.ok=OK
button.cancel=キャンセル
button.apply=適用
button.save=保存
button.open=開く
button.close=閉じる
button.browse=参照...

# ダイアログ
dialog.settings.title=設定
dialog.settings.dicom.folder=DICOMデータフォルダ:
dialog.settings.database.folder=データベースフォルダ:
dialog.settings.language=言語:

dialog.confirm.title=確認
dialog.confirm.delete=削除してもよろしいですか？
dialog.confirm.exit=アプリケーションを終了しますか？

dialog.error.title=エラー
dialog.error.load.failed=読み込みに失敗しました
dialog.error.save.failed=保存に失敗しました

# ステータス
status.loading=読み込み中...
status.saving=保存中...
status.ready=準備完了

# ツールバー
toolbar.zoom.in=拡大
toolbar.zoom.out=縮小
toolbar.zoom.fit=フィット
toolbar.pan=パン
toolbar.window.level=ウィンドウレベル

# 情報表示
info.patient=患者
info.study=スタディ
info.series=シリーズ
info.image=画像
info.instance.number=インスタンス番号
info.modality=モダリティ
info.study.date=スタディ日付
info.study.description=スタディ説明

# ROI
roi.rectangle=矩形
roi.oval=楕円
roi.polygon=多角形
roi.angle=角度
roi.distance=距離

# 統計
statistics.mean=平均
statistics.stddev=標準偏差
statistics.min=最小
statistics.max=最大
statistics.count=カウント

# 操作説明
operation.zoom=ズーム: CTRL + マウスホイール
operation.pan=パン: 左クリック + ドラッグ
operation.window.level=ウィンドウレベル: SHIFT + 左クリック + ドラッグ
```

**messages_en.properties**:
```properties
# Application
app.name=JJDicomViewer
app.title=DICOM Viewer

# Menu
menu.file=File(&F)
menu.file.open=Open(&O)
menu.file.import=Import(&I)
menu.file.export=Export(&E)
menu.file.exit=Exit(&X)

menu.edit=Edit(&E)
menu.edit.settings=Settings(&S)

menu.view=View(&V)
menu.view.zoom.in=Zoom In(&I)
menu.view.zoom.out=Zoom Out(&O)
menu.view.zoom.fit=Fit(&F)
menu.view.window.level=Window Level(&L)

menu.tools=Tools(&T)
menu.tools.roi=ROI(&R)
menu.tools.measure=Measure(&M)

menu.help=Help(&H)
menu.help.about=About(&A)

# Buttons
button.ok=OK
button.cancel=Cancel
button.apply=Apply
button.save=Save
button.open=Open
button.close=Close
button.browse=Browse...

# Dialogs
dialog.settings.title=Settings
dialog.settings.dicom.folder=DICOM Data Folder:
dialog.settings.database.folder=Database Folder:
dialog.settings.language=Language:

dialog.confirm.title=Confirm
dialog.confirm.delete=Are you sure you want to delete?
dialog.confirm.exit=Do you want to exit the application?

dialog.error.title=Error
dialog.error.load.failed=Failed to load
dialog.error.save.failed=Failed to save

# Status
status.loading=Loading...
status.saving=Saving...
status.ready=Ready

# Toolbar
toolbar.zoom.in=Zoom In
toolbar.zoom.out=Zoom Out
toolbar.zoom.fit=Fit
toolbar.pan=Pan
toolbar.window.level=Window Level

# Information
info.patient=Patient
info.study=Study
info.series=Series
info.image=Image
info.instance.number=Instance Number
info.modality=Modality
info.study.date=Study Date
info.study.description=Study Description

# ROI
roi.rectangle=Rectangle
roi.oval=Oval
roi.polygon=Polygon
roi.angle=Angle
roi.distance=Distance

# Statistics
statistics.mean=Mean
statistics.stddev=Std Dev
statistics.min=Min
statistics.max=Max
statistics.count=Count

# Operations
operation.zoom=Zoom: CTRL + Mouse Wheel
operation.pan=Pan: Left Click + Drag
operation.window.level=Window Level: SHIFT + Left Click + Drag
```

## 3. 国際化マネージャーの実装

### 3.1 リソースバンドル管理

```java
import java.util.*;
import java.util.ResourceBundle;

public class I18nManager {
    private static final String BASE_NAME = "i18n/messages";
    private ResourceBundle bundle;
    private Locale currentLocale;
    
    private static I18nManager instance;
    
    private I18nManager() {
        loadLocale();
    }
    
    public static I18nManager getInstance() {
        if (instance == null) {
            synchronized (I18nManager.class) {
                if (instance == null) {
                    instance = new I18nManager();
                }
            }
        }
        return instance;
    }
    
    private void loadLocale() {
        ConfigurationManager config = ConfigurationManager.getInstance();
        String language = config.getLanguage();
        
        if ("ja".equals(language)) {
            currentLocale = Locale.JAPANESE;
        } else if ("en".equals(language)) {
            currentLocale = Locale.ENGLISH;
        } else {
            currentLocale = Locale.getDefault();
        }
        
        try {
            bundle = ResourceBundle.getBundle(BASE_NAME, currentLocale);
        } catch (MissingResourceException e) {
            // フォールバック: 英語
            currentLocale = Locale.ENGLISH;
            bundle = ResourceBundle.getBundle(BASE_NAME, currentLocale);
        }
    }
    
    public void setLocale(Locale locale) {
        this.currentLocale = locale;
        bundle = ResourceBundle.getBundle(BASE_NAME, locale);
        
        // 設定に保存
        ConfigurationManager config = ConfigurationManager.getInstance();
        String lang = locale.getLanguage();
        config.setLanguage(lang);
    }
    
    public String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }
    
    public String getString(String key, Object... args) {
        String pattern = getString(key);
        return MessageFormat.format(pattern, args);
    }
    
    public Locale getCurrentLocale() {
        return currentLocale;
    }
    
    public boolean isJapanese() {
        return currentLocale.getLanguage().equals("ja");
    }
    
    public boolean isEnglish() {
        return currentLocale.getLanguage().equals("en");
    }
}
```

### 3.2 便利メソッド

```java
public class I18n {
    private static I18nManager manager = I18nManager.getInstance();
    
    public static String t(String key) {
        return manager.getString(key);
    }
    
    public static String t(String key, Object... args) {
        return manager.getString(key, args);
    }
    
    public static Locale getLocale() {
        return manager.getCurrentLocale();
    }
}
```

## 4. UIコンポーネントでの使用

### 4.1 メニューの国際化

```java
public class BrowserController extends JFrame {
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File メニュー
        JMenu fileMenu = new JMenu(I18n.t("menu.file"));
        JMenuItem openItem = new JMenuItem(I18n.t("menu.file.open"));
        JMenuItem importItem = new JMenuItem(I18n.t("menu.file.import"));
        JMenuItem exportItem = new JMenuItem(I18n.t("menu.file.export"));
        JMenuItem exitItem = new JMenuItem(I18n.t("menu.file.exit"));
        
        fileMenu.add(openItem);
        fileMenu.add(importItem);
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Edit メニュー
        JMenu editMenu = new JMenu(I18n.t("menu.edit"));
        JMenuItem settingsItem = new JMenuItem(I18n.t("menu.edit.settings"));
        editMenu.add(settingsItem);
        
        // View メニュー
        JMenu viewMenu = new JMenu(I18n.t("menu.view"));
        JMenuItem zoomInItem = new JMenuItem(I18n.t("menu.view.zoom.in"));
        JMenuItem zoomOutItem = new JMenuItem(I18n.t("menu.view.zoom.out"));
        JMenuItem fitItem = new JMenuItem(I18n.t("menu.view.zoom.fit"));
        
        viewMenu.add(zoomInItem);
        viewMenu.add(zoomOutItem);
        viewMenu.add(fitItem);
        
        // Help メニュー
        JMenu helpMenu = new JMenu(I18n.t("menu.help"));
        JMenuItem aboutItem = new JMenuItem(I18n.t("menu.help.about"));
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }
}
```

### 4.2 ダイアログの国際化

```java
public class SettingsDialog extends JDialog {
    private void initializeUI() {
        setTitle(I18n.t("dialog.settings.title"));
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // DICOMデータフォルダ
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel(I18n.t("dialog.settings.dicom.folder")), gbc);
        
        // データベースフォルダ
        gbc.gridy = 1;
        mainPanel.add(new JLabel(I18n.t("dialog.settings.database.folder")), gbc);
        
        // 言語
        gbc.gridy = 2;
        mainPanel.add(new JLabel(I18n.t("dialog.settings.language")), gbc);
        
        // ボタン
        JButton okButton = new JButton(I18n.t("button.ok"));
        JButton cancelButton = new JButton(I18n.t("button.cancel"));
    }
}
```

### 4.3 メッセージダイアログの国際化

```java
public class MessageDialog {
    public static void showError(Component parent, String key) {
        JOptionPane.showMessageDialog(parent,
            I18n.t(key),
            I18n.t("dialog.error.title"),
            JOptionPane.ERROR_MESSAGE);
    }
    
    public static void showError(Component parent, String key, Object... args) {
        JOptionPane.showMessageDialog(parent,
            I18n.t(key, args),
            I18n.t("dialog.error.title"),
            JOptionPane.ERROR_MESSAGE);
    }
    
    public static int showConfirm(Component parent, String key) {
        return JOptionPane.showConfirmDialog(parent,
            I18n.t(key),
            I18n.t("dialog.confirm.title"),
            JOptionPane.YES_NO_OPTION);
    }
    
    public static void showInfo(Component parent, String key) {
        JOptionPane.showMessageDialog(parent,
            I18n.t(key),
            I18n.t("dialog.info.title"),
            JOptionPane.INFORMATION_MESSAGE);
    }
}
```

## 5. 動的な言語切り替え

### 5.1 言語変更リスナー

```java
public interface LanguageChangeListener {
    void onLanguageChanged(Locale newLocale);
}

public class I18nManager {
    private List<LanguageChangeListener> listeners = new ArrayList<>();
    
    public void addLanguageChangeListener(LanguageChangeListener listener) {
        listeners.add(listener);
    }
    
    public void removeLanguageChangeListener(LanguageChangeListener listener) {
        listeners.remove(listener);
    }
    
    public void setLocale(Locale locale) {
        this.currentLocale = locale;
        bundle = ResourceBundle.getBundle(BASE_NAME, locale);
        
        // リスナーに通知
        for (LanguageChangeListener listener : listeners) {
            listener.onLanguageChanged(locale);
        }
    }
}
```

### 5.2 UIコンポーネントの更新

```java
public class BrowserController extends JFrame 
        implements LanguageChangeListener {
    
    public BrowserController() {
        I18nManager.getInstance().addLanguageChangeListener(this);
        initializeUI();
    }
    
    @Override
    public void onLanguageChanged(Locale newLocale) {
        // UIコンポーネントを再構築
        SwingUtilities.invokeLater(() -> {
            updateUI();
        });
    }
    
    private void updateUI() {
        setTitle(I18n.t("app.title"));
        createMenuBar();
        // その他のUIコンポーネントも更新
        revalidate();
        repaint();
    }
}
```

## 6. リソースファイルの読み込み

### 6.1 クラスパスからの読み込み

```java
public class I18nManager {
    private ResourceBundle loadBundle(Locale locale) {
        try {
            // クラスパスから読み込み
            return ResourceBundle.getBundle(BASE_NAME, locale);
        } catch (MissingResourceException e) {
            // フォールバック
            return ResourceBundle.getBundle(BASE_NAME, Locale.ENGLISH);
        }
    }
}
```

### 6.2 外部ファイルからの読み込み

```java
import java.io.*;
import java.nio.file.*;

public class I18nManager {
    private ResourceBundle loadBundleFromFile(Locale locale) {
        String fileName = "messages_" + locale.getLanguage() + ".properties";
        Path filePath = Paths.get("resources", "i18n", fileName);
        
        if (Files.exists(filePath)) {
            try (InputStream is = Files.newInputStream(filePath)) {
                return new PropertyResourceBundle(
                    new InputStreamReader(is, StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // フォールバック: クラスパスから
        return ResourceBundle.getBundle(BASE_NAME, locale);
    }
}
```

## 7. まとめ

国際化機能の実装では：

1. **リソースバンドル**による多言語対応
2. **設定に基づく言語選択**
3. **動的な言語切り替え**
4. **外部ファイルからの読み込み**

これにより、日本語と英語の両方の表記をサポートできます。

