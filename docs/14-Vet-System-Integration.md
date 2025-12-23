# Vet-System連携と操作性の継承 - Java移植ドキュメント

## 1. 概要

このドキュメントは、Vet-system（獣医向け受付・電子カルテシステム）との連携方法と、Vet-systemの操作性・デザインパターンをJJDICOMViewerに継承する方法をまとめたものです。

**重要な注意事項**:
- Vet-SystemのDICOMビューワー部分も仕様からの独自ロジックで実装されています
- JJDICOMViewerをHOROSベースで実装する場合、Vet-System側のビューワーも修正が必要になる可能性があります
- 操作性の統一とデータ連携の方法を事前に設計する必要があります

## 2. Vet-systemの概要

### 2.1 システム構成

Vet-systemは、FastAPI製の院内受付・マスタ管理システムです：

- **バックエンド**: FastAPI + SQL Server
- **フロントエンド**: Jinja2テンプレート + スタティックアセット（CSS/JS）
- **主要機能**:
  - 飼い主／ペット管理
  - 受付登録・編集
  - カルテ閲覧
  - DICOMビューワー
  - 音声入力（Whisper）連携

### 2.2 DICOMビューワーの構成

Vet-systemのDICOMビューワー（`dicom_viewer.html`）は以下の構造：

```
┌─────────────────────────────────────────┐
│ ヘッダー（📷 DICOM ビューア）          │
├──────────┬──────────────────────────────┤
│          │ ツールバー                   │
│ スタディ │ ├─ メタ情報表示             │
│ 一覧     │ ├─ ROIセレクター            │
│          │ └─ 前へ/次へボタン          │
│          ├──────────────────────────────┤
│          │ 画像表示エリア               │
│          │                              │
└──────────┴──────────────────────────────┘
```

## 3. Vet-systemの操作性パターン

### 3.1 UIレイアウト

**Vet-system実装** (`dicom_viewer.css`):
```css
.viewer-container {
    display: flex;
    height: calc(100vh - 64px);
    min-height: 520px;
}

.viewer-sidebar {
    width: 22%;
    min-width: 240px;
    max-width: 340px;
    background: #f8fafc;
    border-right: 1px solid #d6e0ef;
}

.viewer-main {
    flex: 1;
    display: flex;
    flex-direction: column;
}
```

**Java実装での対応**:
```java
import javax.swing.*;
import java.awt.*;

public class DicomViewerFrame extends JFrame {
    private JSplitPane mainSplitPane;
    private JPanel sidebarPanel;
    private JPanel viewerPanel;
    
    public DicomViewerFrame() {
        setLayout(new BorderLayout());
        
        // サイドバー（スタディ一覧）
        sidebarPanel = createSidebarPanel();
        sidebarPanel.setPreferredSize(new Dimension(280, 0));
        sidebarPanel.setMinimumSize(new Dimension(240, 0));
        sidebarPanel.setMaximumSize(new Dimension(340, Integer.MAX_VALUE));
        sidebarPanel.setBackground(new Color(0xf8fafc));
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xd6e0ef)));
        
        // ビューワー本体
        viewerPanel = createViewerPanel();
        
        // スプリッターパネル
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPanel, viewerPanel);
        mainSplitPane.setDividerLocation(280);
        mainSplitPane.setResizeWeight(0.0); // サイドバーは固定幅
        
        add(mainSplitPane, BorderLayout.CENTER);
    }
}
```

### 3.2 カラースキーム

**Vet-system実装** (`dicom_viewer.css`, `style.css`):
```css
/* ヘッダー */
header {
    background: #2c5aa0;  /* または #2b6cb0 */
    color: #fff;
}

/* サイドバー */
.viewer-sidebar {
    background: #f8fafc;
    border-right: 1px solid #d6e0ef;
}

/* ボタン */
.btn {
    background: #2c5aa0;
    color: #fff;
}

.btn:hover {
    background: #1d4ed8;
}
```

**Java実装での対応**:
```java
public class VetSystemTheme {
    // ヘッダー色
    public static final Color HEADER_BACKGROUND = new Color(0x2c5aa0);
    public static final Color HEADER_FOREGROUND = Color.WHITE;
    
    // サイドバー色
    public static final Color SIDEBAR_BACKGROUND = new Color(0xf8fafc);
    public static final Color SIDEBAR_BORDER = new Color(0xd6e0ef);
    
    // ボタン色
    public static final Color BUTTON_PRIMARY = new Color(0x2c5aa0);
    public static final Color BUTTON_PRIMARY_HOVER = new Color(0x1d4ed8);
    public static final Color BUTTON_SECONDARY = new Color(0x64748b);
    
    // テキスト色
    public static final Color TEXT_PRIMARY = new Color(0x1f2933);
    public static final Color TEXT_SECONDARY = new Color(0x64748b);
    
    public static void applyTheme(JComponent component) {
        component.setBackground(SIDEBAR_BACKGROUND);
        component.setForeground(TEXT_PRIMARY);
    }
}
```

### 3.3 スタディ一覧の表示

**Vet-system実装** (`dicom_viewer.js`):
- スタディを日付でグループ化
- クリックで展開/折りたたみ
- アクティブなスタディのハイライト
- シリーズの階層表示

**Java実装での対応**:
```java
import javax.swing.tree.*;

public class StudyTreePanel extends JPanel {
    private JTree studyTree;
    private DefaultTreeModel treeModel;
    private StudyTreeCellRenderer cellRenderer;
    
    public StudyTreePanel() {
        setLayout(new BorderLayout());
        setBackground(VetSystemTheme.SIDEBAR_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 24, 12));
        
        // ツリーモデルの作成
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("スタディ");
        treeModel = new DefaultTreeModel(root);
        studyTree = new JTree(treeModel);
        
        // カスタムレンダラー
        cellRenderer = new StudyTreeCellRenderer();
        studyTree.setCellRenderer(cellRenderer);
        
        // 選択リスナー
        studyTree.addTreeSelectionListener(e -> {
            TreePath path = e.getPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = node.getUserObject();
                if (userObject instanceof DicomSeries) {
                    DicomSeries series = (DicomSeries) userObject;
                    onSeriesSelected(series);
                } else if (userObject instanceof DicomStudy) {
                    DicomStudy study = (DicomStudy) userObject;
                    onStudySelected(study);
                }
            }
        });
        
        // スクロールパネル
        JScrollPane scrollPane = new JScrollPane(studyTree);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void onStudySelected(DicomStudy study) {
        // スタディが選択された時の処理
        // シリーズ一覧をロード
    }
    
    private void onSeriesSelected(DicomSeries series) {
        // シリーズが選択された時の処理
        // 画像を表示
    }
}

class StudyTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();
        
        if (userObject instanceof DicomStudy) {
            DicomStudy study = (DicomStudy) userObject;
            setText(formatStudyText(study));
            setIcon(UIManager.getIcon("Tree.closedIcon"));
        } else if (userObject instanceof DicomSeries) {
            DicomSeries series = (DicomSeries) userObject;
            setText(formatSeriesText(series));
            setIcon(UIManager.getIcon("Tree.leafIcon"));
        }
        
        // 選択時の色
        if (sel) {
            setBackground(VetSystemTheme.BUTTON_PRIMARY);
            setForeground(Color.WHITE);
        } else {
            setBackground(VetSystemTheme.SIDEBAR_BACKGROUND);
            setForeground(VetSystemTheme.TEXT_PRIMARY);
        }
        
        return this;
    }
    
    private String formatStudyText(DicomStudy study) {
        return String.format("%s - %s", 
            study.getStudyDate(), 
            study.getPatientName());
    }
    
    private String formatSeriesText(DicomSeries series) {
        return String.format("%s (%d images)", 
            series.getSeriesDescription(), 
            series.getNumberOfImages());
    }
}
```

### 3.4 ツールバーの実装

**Vet-system実装** (`dicom_viewer.html`):
```html
<div class="viewer-toolbar">
    <div id="viewerMeta" class="viewer-meta">スタディを選択してください</div>
    <div id="viewerROISelector" class="viewer-roi-selector" style="display: none;">
        <label>ROI:</label>
        <button class="btn btn-secondary btn-small" id="viewerROIAllBtn">全画像</button>
        <div id="viewerROIButtons" class="viewer-roi-buttons"></div>
    </div>
    <div class="viewer-nav">
        <button class="btn btn-secondary" id="viewerPrevBtn" disabled>前へ</button>
        <button class="btn btn-secondary" id="viewerNextBtn" disabled>次へ</button>
    </div>
</div>
```

**Java実装での対応**:
```java
public class ViewerToolbar extends JPanel {
    private JLabel metaLabel;
    private JPanel roiSelectorPanel;
    private JButton roiAllButton;
    private JPanel roiButtonsPanel;
    private JButton prevButton;
    private JButton nextButton;
    
    public ViewerToolbar() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        setBackground(Color.WHITE);
        
        // メタ情報表示
        metaLabel = new JLabel("スタディを選択してください");
        metaLabel.setFont(metaLabel.getFont().deriveFont(Font.PLAIN, 13f));
        add(metaLabel);
        
        add(Box.createHorizontalGlue());
        
        // ROIセレクター
        roiSelectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        roiSelectorPanel.setOpaque(false);
        roiSelectorPanel.setVisible(false);
        
        JLabel roiLabel = new JLabel("ROI:");
        roiSelectorPanel.add(roiLabel);
        
        roiAllButton = createSmallButton("全画像");
        roiAllButton.addActionListener(e -> onROIAllSelected());
        roiSelectorPanel.add(roiAllButton);
        
        roiButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        roiButtonsPanel.setOpaque(false);
        roiSelectorPanel.add(roiButtonsPanel);
        
        add(roiSelectorPanel);
        
        add(Box.createHorizontalGlue());
        
        // ナビゲーションボタン
        prevButton = createButton("前へ");
        prevButton.setEnabled(false);
        prevButton.addActionListener(e -> onPrevious());
        add(prevButton);
        
        nextButton = createButton("次へ");
        nextButton.setEnabled(false);
        nextButton.addActionListener(e -> onNext());
        add(nextButton);
    }
    
    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(VetSystemTheme.BUTTON_SECONDARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(VetSystemTheme.BUTTON_PRIMARY_HOVER);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(VetSystemTheme.BUTTON_SECONDARY);
            }
        });
        return button;
    }
    
    private JButton createSmallButton(String text) {
        JButton button = createButton(text);
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 11f));
        button.setPreferredSize(new Dimension(60, 24));
        return button;
    }
    
    public void updateMeta(String text) {
        metaLabel.setText(text);
    }
    
    public void setROISelectorVisible(boolean visible) {
        roiSelectorPanel.setVisible(visible);
    }
    
    public void addROIButton(String roiName, int roiIndex) {
        JButton roiButton = createSmallButton(roiName);
        roiButton.addActionListener(e -> onROISelected(roiIndex));
        roiButtonsPanel.add(roiButton);
        roiButtonsPanel.revalidate();
        roiButtonsPanel.repaint();
    }
    
    public void clearROIButtons() {
        roiButtonsPanel.removeAll();
        roiButtonsPanel.revalidate();
        roiButtonsPanel.repaint();
    }
    
    public void setNavigationEnabled(boolean enabled) {
        prevButton.setEnabled(enabled);
        nextButton.setEnabled(enabled);
    }
    
    // イベントハンドラー（実装は呼び出し側で定義）
    private void onROIAllSelected() { /* ... */ }
    private void onROISelected(int index) { /* ... */ }
    private void onPrevious() { /* ... */ }
    private void onNext() { /* ... */ }
}
```

### 3.5 ボタンスタイルの統一

**Vet-system実装** (`form_common.css`):
```css
.btn {
    padding: 12px 24px;
    border: none;
    border-radius: 4px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: background-color 0.2s;
}

.btn-primary {
    background: #2c5aa0;
    color: #fff;
}

.btn-secondary {
    background: #64748b;
    color: #fff;
}

.btn:hover {
    opacity: 0.9;
}
```

**Java実装での対応**:
```java
public class VetSystemButton extends JButton {
    public enum ButtonStyle {
        PRIMARY, SECONDARY
    }
    
    public VetSystemButton(String text, ButtonStyle style) {
        super(text);
        setFocusPainted(false);
        setBorderPainted(false);
        setFont(getFont().deriveFont(Font.PLAIN, 14f));
        setPreferredSize(new Dimension(0, 36));
        
        switch (style) {
            case PRIMARY:
                setBackground(VetSystemTheme.BUTTON_PRIMARY);
                setForeground(Color.WHITE);
                break;
            case SECONDARY:
                setBackground(VetSystemTheme.BUTTON_SECONDARY);
                setForeground(Color.WHITE);
                break;
        }
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Color bg = getBackground();
                setBackground(new Color(
                    Math.max(0, bg.getRed() - 20),
                    Math.max(0, bg.getGreen() - 20),
                    Math.max(0, bg.getBlue() - 20)
                ));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                switch (style) {
                    case PRIMARY:
                        setBackground(VetSystemTheme.BUTTON_PRIMARY);
                        break;
                    case SECONDARY:
                        setBackground(VetSystemTheme.BUTTON_SECONDARY);
                        break;
                }
            }
        });
    }
}
```

## 4. Vet-Systemとの連携

### 4.1 データ連携

**Vet-system実装** (`dicom_viewer.js`):
```javascript
const context = {
    ownerId: initialState.ownerId || "",
    patientId: initialState.patientId || "",
    recordId: initialState.recordId || "",
    consultationDate: initialState.consultationDate || "",
    linksMap: new Map(),
    linksLoaded: false,
};

async function ensureLinksLoaded() {
    const params = new URLSearchParams({
        ownerId: context.ownerId,
        patientId: context.patientId,
    });
    const url = `/api/dicom/links?${params.toString()}`;
    const data = await fetchJSON(url);
    // リンク情報をマップに保存
}
```

**Java実装での対応**:
```java
public class VetSystemLinkManager {
    private String ownerId;
    private String patientId;
    private String recordId;
    private String consultationDate;
    private Map<String, DicomLink> linksMap;
    
    public VetSystemLinkManager(String ownerId, String patientId) {
        this.ownerId = ownerId;
        this.patientId = patientId;
        this.linksMap = new HashMap<>();
    }
    
    public void loadLinks() throws Exception {
        // Vet-SystemのAPIからリンク情報を取得
        // テキストベースの連携（JSON形式）
        String url = String.format("http://localhost:58806/api/dicom/links?ownerId=%s&patientId=%s", 
            ownerId, patientId);
        
        // HTTPリクエストで取得（実装はHTTPクライアントライブラリを使用）
        // ...
    }
    
    public DicomLink getLinkForStudy(String studyInstanceUID) {
        return linksMap.get(studyInstanceUID);
    }
}
```

### 4.2 テキストベースの連携

**連携方式**:

1. **JSON形式のデータ交換**
   - Vet-SystemのAPIからJSON形式でデータを取得
   - 患者情報、記録情報、DICOMリンク情報

2. **ファイルベースの連携**
   - 共有フォルダを使用
   - ファイルの監視による自動読み込み

3. **データベース連携**（将来の拡張）
   - Vet-Systemのデータベースに直接接続
   - SQLクエリでデータを取得

**Java実装での対応**:
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class VetSystemAPIClient {
    private String baseUrl;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    
    public VetSystemAPIClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    public List<DicomLink> getDicomLinks(String ownerId, String patientId) throws Exception {
        String url = String.format("%s/api/dicom/links?ownerId=%s&patientId=%s", 
            baseUrl, ownerId, patientId);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            ApiResponse apiResponse = objectMapper.readValue(response.body(), ApiResponse.class);
            if (apiResponse.isOk()) {
                return apiResponse.getLinks();
            }
        }
        
        throw new Exception("Failed to fetch DICOM links: " + response.statusCode());
    }
    
    public static class ApiResponse {
        private boolean ok;
        private List<DicomLink> links;
        private String error;
        
        // Getters and setters
    }
    
    public static class DicomLink {
        private String studyInstanceUID;
        private String seriesInstanceUID;
        private String ownerId;
        private String patientId;
        private String recordId;
        private String consultationDate;
        private String studyDate;
        
        // Getters and setters
    }
}
```

## 5. 操作性の継承チェックリスト

### 5.1 UIレイアウト

- [ ] 左側にスタディ一覧（サイドバー）
- [ ] 右側にビューワー本体
- [ ] ツールバーにメタ情報、ROIセレクター、ナビゲーションボタン
- [ ] レスポンシブなレイアウト（サイドバーの幅調整可能）

### 5.2 カラースキーム

- [ ] ヘッダー: #2c5aa0（青）
- [ ] サイドバー: #f8fafc（薄いグレー）
- [ ] ボタン: #2c5aa0（プライマリ）、#64748b（セカンダリ）
- [ ] ホバー効果: 色の変化

### 5.3 操作性

- [ ] スタディのクリックで展開/折りたたみ
- [ ] シリーズの選択で画像表示
- [ ] ROIセレクターでROI別の画像表示
- [ ] 前へ/次へボタンで画像ナビゲーション
- [ ] メタ情報の表示更新

### 5.4 連携機能

- [ ] Vet-Systemからの患者情報の取得
- [ ] DICOMリンク情報の取得
- [ ] 記録との関連付け
- [ ] 自動スタディ選択（最新のリンクされたスタディ）

## 6. 実装順序

1. **VetSystemThemeクラス**: カラースキームの定義
2. **VetSystemButtonクラス**: ボタンスタイルの統一
3. **StudyTreePanelクラス**: スタディ一覧の実装
4. **ViewerToolbarクラス**: ツールバーの実装
5. **VetSystemAPIClientクラス**: Vet-Systemとの連携
6. **VetSystemLinkManagerクラス**: リンク情報の管理

## 7. Vet-Systemとの連携方針

### 7.1 現状の課題

Vet-SystemのDICOMビューワーは、仕様からの独自ロジックで実装されています：

- **Webベースのビューワー**: HTML/CSS/JavaScriptで実装
- **独自の操作性**: Vet-System独自のUIパターン
- **データ連携**: FastAPI経由でDICOMデータを取得

JJDICOMViewerをHOROSベースで実装する場合：

- **Java Swingベース**: デスクトップアプリケーション
- **HOROSの操作性**: HOROSの操作パターンを継承
- **データ連携**: Vet-SystemのAPIまたはデータベース経由

### 7.2 連携方針の選択肢

#### 方針A: JJDICOMViewerをメインとする

**メリット**:
- HOROSの成熟した操作性を活用
- デスクトップアプリとしてのパフォーマンス
- スタンドアローン使用が容易

**デメリット**:
- Vet-Systemの既存ビューワーを大幅に修正する必要がある
- Webベースの操作性との差異が生じる可能性

**実装**:
- Vet-SystemからJJDICOMViewerを起動（外部アプリとして）
- 患者情報やDICOMリンク情報を引数またはファイルで渡す
- JJDICOMViewerで画像を表示・操作

#### 方針B: 操作性を統一する

**メリット**:
- Vet-SystemとJJDICOMViewerの操作性を統一
- ユーザーの学習コストを低減

**デメリット**:
- Vet-Systemの既存ビューワーを修正する必要がある
- 両方のシステムの開発・保守コストが増加

**実装**:
- Vet-SystemのビューワーをJJDICOMViewerの操作性に合わせて修正
- または、JJDICOMViewerをVet-Systemの操作性に合わせて実装
- 共通の操作パターンを定義

#### 方針C: 並行運用

**メリット**:
- 既存のVet-Systemビューワーを維持
- 必要に応じてJJDICOMViewerを使用

**デメリット**:
- 操作性の差異が生じる
- ユーザーの混乱を招く可能性

**実装**:
- Vet-SystemのWebビューワーはそのまま維持
- JJDICOMViewerはスタンドアローンまたは外部起動で使用
- データ連携は最小限に

### 7.3 推奨方針

**推奨**: 方針A（JJDICOMViewerをメインとする）+ 操作性の部分的統一

**理由**:
1. HOROSの成熟した操作性を活用できる
2. デスクトップアプリとしてのパフォーマンスが優れている
3. Vet-Systemの修正を最小限に抑えられる

**実装方針**:
1. **JJDICOMViewerの実装**
   - HOROSベースの操作性を実装
   - Vet-Systemのカラースキームとレイアウトパターンを部分的に継承

2. **Vet-Systemの修正**
   - DICOMビューワー画面からJJDICOMViewerを起動する機能を追加
   - 患者情報やDICOMリンク情報を渡す仕組みを実装
   - Webビューワーは簡易表示用として残す（オプション）

3. **データ連携**
   - テキストベース（JSON形式）でデータを連携
   - ファイルベースの連携もサポート

### 7.4 Vet-System側の修正が必要な箇所

JJDICOMViewerを実装する場合、Vet-System側で以下の修正が必要になる可能性があります：

1. **DICOMビューワー画面の修正**
   - JJDICOMViewer起動ボタンの追加
   - 患者情報の引き渡し機能
   - DICOMリンク情報の引き渡し機能

2. **APIの追加・修正**
   - JJDICOMViewer用のAPIエンドポイント
   - データ形式の統一

3. **操作性の調整**
   - 操作性の差異を最小限に抑える
   - ユーザーへの説明・マニュアルの更新

### 7.5 実装の優先順位

1. **Phase 1: JJDICOMViewerの基本実装**
   - HOROSベースの基本機能
   - スタンドアローンでの動作確認

2. **Phase 2: Vet-System連携の基本機能**
   - テキストベースのデータ連携
   - 外部起動機能

3. **Phase 3: 操作性の統一**
   - Vet-Systemのカラースキームの適用
   - レイアウトパターンの部分的統一

4. **Phase 4: Vet-System側の修正**
   - DICOMビューワー画面の修正
   - APIの追加・修正

## 8. 参考資料

- Vet-systemソースコード: `C:\Users\jam11\Vet-system\`
- DICOMビューワー: `reception/templates/dicom_viewer.html`
- CSS: `reception/static/dicom_viewer.css`
- JavaScript: `reception/static/dicom_viewer.js`

