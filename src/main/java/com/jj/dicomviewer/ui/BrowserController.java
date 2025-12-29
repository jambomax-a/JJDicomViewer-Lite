package com.jj.dicomviewer.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import com.jj.dicomviewer.model.DicomDatabase;
import com.jj.dicomviewer.threads.ThreadsManager;

/**
 * BrowserController - ブラウザウィンドウコントローラー
 * 
 * HOROS-20240407のBrowserControllerをJava Swingに移植
 */
public class BrowserController extends JFrame {
    
    // HOROS-20240407準拠: static BrowserController *browserWindow = nil; (170行目)
    private static BrowserController browserWindow = null;
    
    // HOROS-20240407準拠: DicomDatabase* _database; (80行目)
    private DicomDatabase database;
    
    // HOROS-20240407準拠: BrowserMatrix *oMatrix; (BrowserController.h)
    private BrowserMatrix oMatrix;
    
    // HOROS-20240407準拠: MyOutlineView *databaseOutline; (BrowserController.h)
    private DatabaseOutlineView databaseOutline;
    
    // HOROS-20240407準拠: BrowserActivityHelper *_activityHelper;
    // (BrowserController+Activity.h)
    private BrowserActivityHelper activityHelper;
    
    // ========== UI関連フィールド（HOROS-20240407準拠） ==========
    // HOROS-20240407準拠: BrowserController.h から主要なフィールドを復元
    
    // HOROS-20240407準拠: NSMutableArray *previewPix, *previewPixThumbnails; (93行目)
    private List<com.jj.dicomviewer.model.DicomPix> previewPix;
    private List<javax.swing.ImageIcon> previewPixThumbnails;
    
    // HOROS-20240407準拠: long loadPreviewIndex, previousNoOfFiles; (106行目)
    private long loadPreviewIndex = 0;
    private long previousNoOfFiles = 0;
    
    // HOROS-20240407準拠: NSManagedObject *previousItem; (107行目)
    private Object previousItem;
    
    // HOROS-20240407準拠: BOOL setDCMDone, dontUpdatePreviewPane; (114行目)
    private boolean setDCMDone = false;
    private boolean dontUpdatePreviewPane = false;
    
    // HOROS-20240407準拠: NSArray *matrixViewArray; (125行目)
    private List<Object> matrixViewArray;
    
    // HOROS-20240407準拠: IBOutlet NSSlider *animationSlider; (142行目)
    private javax.swing.JSlider animationSlider;
    
    // HOROS-20240407準拠: IBOutlet NSButton *animationCheck; (143行目)
    private javax.swing.JCheckBox animationCheck;
    
    // HOROS-20240407準拠: アニメーションタイマー
    private javax.swing.Timer animationTimer;
    
    // HOROS-20240407準拠: IBOutlet PreviewView *imageView; (146行目)
    private PreviewView imageView;
    
    // HOROS-20240407準拠: IBOutlet NSTableView *albumTable; (133行目)
    private AlbumTableView albumTable;
    
    // HOROS-20240407準拠: IBOutlet NSTableView *_sourcesTableView; (138行目)
    private javax.swing.JTable sourcesTableView;
    
    // HOROS-20240407準拠: IBOutlet NSTableView *comparativeTable; (270行目)
    private javax.swing.JTable comparativeTable;

    // HOROS-20240407準拠: NSMenu *columnsMenu; (BrowserController.h 131行目)
    private javax.swing.JPopupMenu columnsMenu;
    
    // HOROS-20240407準拠: IBOutlet NSSearchField *searchField; (180行目)
    private javax.swing.JTextField searchField;

    // HOROS-20240407準拠: IBOutlet NSTextField *databaseDescription; (BrowserController.h 129行目)
    private javax.swing.JTextField databaseDescription;

    // HOROS-20240407準拠: IBOutlet NSSplitView *splitAlbums; (BrowserController.h 111行目)
    private JSplitPane splitAlbums;
    
    // HOROS-20240407準拠: splitSourcesActivity（SourcesとActivityを分割するスプリッター）
    private JSplitPane splitSourcesActivity;
    
    // HOROS-20240407準拠: IBOutlet NSSplitView *splitComparative; (BrowserController.h 111行目)
    private JSplitPane splitComparative;
    
        // HOROS-20240407準拠: bottomPreviewSplit（サムネイルとプレビューを分割するスプリッター）
    private JSplitPane bottomPreviewSplit;
    
    // サムネイルスクロールビューの垂直スクロールバー（ディバイダーの右側に固定配置するため）
    private javax.swing.JScrollBar thumbnailsVerticalScrollBar;
    
    // 手動スクロールバーの調整中フラグ（無限ループを防ぐため）
    private boolean isAdjustingThumbnailsScrollBar = false;
    
    // サムネイルスクロールビュー（previewMatrixScrollViewFrameDidChangeで直接参照するため）
    private javax.swing.JScrollPane thumbnailsScrollView;
    
    // HOROS-20240407準拠: BOOL loadingIsOver = NO; (BrowserController.m 174行目)
    private boolean loadingIsOver = false;
    
    // HOROS-20240407準拠: BOOL DatabaseIsEdited; (201行目)
    private boolean DatabaseIsEdited = false;
    
    // HOROS-20240407準拠: NSThread *matrixLoadIconsThread; (262行目)
    private Thread matrixLoadIconsThread;
    
    // HOROS-20240407準拠: NSTimeInterval _timeIntervalOfLastLoadIconsDisplayIcons; (BrowserController.h 261行目)
    private long timeIntervalOfLastLoadIconsDisplayIcons = 0;
    
    // HOROS-20240407準拠: BOOL withReset; (プレビューリセットフラグ)
    private boolean withReset = false;
    
    // HOROS-20240407準拠: NSString *comparativePatientUID; (267行目)
    private String comparativePatientUID;
    
    // HOROS-20240407準拠: NSArray *comparativeStudies; (269行目)
    private List<Object> comparativeStudies;
    
    // HOROS-20240407準拠: BOOL _computingNumberOfStudiesForAlbums; (253行目)
    private boolean _computingNumberOfStudiesForAlbums = false;
    
    // 無限ループ防止フラグ
    private boolean isRefreshingOutline = false;
    private boolean isRefreshingAlbums = false;
    private boolean isUpdatingAlbumTable = false;
    
    // ディバイダー位置の復元が完了したかどうか（一度だけ実行するため）
    private boolean dividerLocationsRestored = false;
    
    // UI初期化が完了したかどうか（previewMatrixScrollViewFrameDidChangeを呼び出す前に確認）
    private boolean uiInitialized = false;
    
    // ディバイダー位置の調整中かどうか（無限ループ防止）
    private boolean isAdjustingDivider = false;
    
    // HOROS-20240407準拠: dbObjectSelection enum (BrowserController.m)
    // oAny, oMiddle, oFirstForFirst は画像選択方法を指定する定数
    private static final int oAny = 0;
    private static final int oMiddle = 1;
    private static final int oFirstForFirst = 2;
    
    // HOROS-20240407準拠: NSImage *notFoundImage; (230行目)
    private javax.swing.ImageIcon notFoundImage;
    
    /**
     * コンストラクタ
     * HOROS-20240407準拠: - (id)init (BrowserController.m)
     */
    public BrowserController() {
        super("JJDicomViewer");
        
        // HOROS-20240407準拠: browserWindow = self; (BrowserController.m)
        browserWindow = this;
        
        // HOROS-20240407準拠: _database = [DicomDatabase activeLocalDatabase];
        // (BrowserController.m)
        this.database = DicomDatabase.activeLocalDatabase();
        
        // HOROS-20240407準拠: フィールドの初期化
        this.previewPix = new ArrayList<>();
        this.previewPixThumbnails = new ArrayList<>();
        this.matrixViewArray = new ArrayList<>();
        this.comparativeStudies = new ArrayList<>();
        this.notFoundImage = new javax.swing.ImageIcon(); // TODO: 実際のnotFoundImageを設定
        
        // UI初期化
        initializeUIComponents();
    }
    
    /**
     * UIコンポーネントの初期化
     * HOROS-20240407準拠: - (void)awakeFromNib (BrowserController.m 14164行目)
     * 
     * HOROS-20240407のコード構造（BrowserController.h/mから確認）:
     * - splitViewHorz (水平): 左サイドバー | 右メインパネル
     * - 左: splitAlbums (垂直): アルバムテーブル | データベースアウトライン（Sources）
     * - 右: splitViewVert (垂直): matrixView | imageView
     * - matrixView: BrowserMatrix（oMatrix）を含むNSView
     * - imageView: PreviewView（_bottomSplitを含む）
     * - _bottomSplit (水平): thumbnailsScrollView | 画像プレビュー
     * 
     * HOROS-20240407準拠: BrowserController.m 10536行目
     * splitViewVert subviews[0] = matrixView
     * splitViewVert subviews[1] = imageView
     * 
     * HOROS-20240407準拠: BrowserController.m 10540-10541行目
     * _bottomSplit subviews[0] = thumbnailsScrollView
     * _bottomSplit subviews[1] = 画像プレビュー
     */
    private void initializeUIComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // HOROS-20240407準拠: BrowserController.m 14183-14188行目
        // ウィンドウ位置・サイズを復元
        restoreWindowFrame();

        // HOROS-20240407準拠: スクロールバーの横幅を設定
        // HOROS-20240407準拠: MainMenu.xib 4851行目 - scroller key="verticalScroller" width="16"
        // 注意: HOROS-20240407ではコードでスクロールバーの横幅を設定していない（XIBファイルで設定）
        // Java SwingではUIManagerで設定する必要がある（プラットフォーム差を埋めるための最小限のカスタムロジック）
        // Windowsではデフォルトのスクロールバー幅が17pxのため、16pxに調整
        try {
            javax.swing.UIManager.put("ScrollBar.width", 16);
            // スクロールバーの最小幅も設定（プラットフォーム差の調整）
            javax.swing.UIManager.put("ScrollBar.minimumThumbSize", new java.awt.Dimension(16, 16));
        } catch (Exception e) {
            // UIManagerの設定に失敗した場合はデフォルトを使用
        }

        // HOROS-20240407準拠: MainMenu.xibからメニューバーを実装
        initializeMenuBar();
        
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        
        // ========== 左側サイドバー（垂直スプリッター） ==========
        // HOROS-20240407準拠: splitAlbums - Albums | Sources | Activity
        // HOROS-20240407準拠: BrowserController.h 111行目 IBOutlet NSSplitView
        // *splitAlbums;
        // HOROS-20240407準拠: MainMenu.xib 4078行目 -
        // splitAlbumsは垂直分割でAlbums、Sources、Activityを含む
        // Java SwingのJSplitPaneは2分割しかできないため、入れ子構造にする必要がある
        splitAlbums = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitAlbums.setName("splitAlbums"); // 検索用に名前を設定
        
        // Albums（上）
        // HOROS-20240407準拠: BrowserController.h 133行目 IBOutlet NSTableView *albumTable;
        // HOROS-20240407準拠: MainMenu.xib 4082行目 - box id="11696" (Albums)
        // HOROS-20240407準拠: MainMenu.xib 4104行目 - tableHeaderCell title="Albums"
        // HOROS-20240407準拠: AlbumTableViewを使用
        albumTable = new AlbumTableView(this);
        // HOROS-20240407準拠: 2列モデル（アルバム名、スタディ数）を設定
        String[] albumColumns = { "アルバム名", "スタディ数" };
        javax.swing.table.DefaultTableModel albumModel = new javax.swing.table.DefaultTableModel(albumColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        albumTable.setModel(albumModel);
        // HOROS-20240407準拠: 選択変更リスナーはAlbumTableView内で処理されるため、ここでは追加しない
        // ただし、isUpdatingAlbumTableフラグを考慮する必要があるため、AlbumTableViewを修正
        // HOROS-20240407準拠: MainMenu.xib 4104行目 - tableHeaderCell title="Albums"
        albumTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane albumScroll = new JScrollPane(albumTable);
        // HOROS-20240407準拠: MainMenu.xib 4082行目 - box title="Albums"
        // titlePosition="noTitle"
        // Java SwingではTitledBorderで見出しを表示
        albumScroll.setBorder(javax.swing.BorderFactory.createTitledBorder(
                javax.swing.BorderFactory.createEmptyBorder(),
                "Albums",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP));
        splitAlbums.setTopComponent(albumScroll);

        // SourcesとActivityを垂直に分割（下部）
        // HOROS-20240407準拠: MainMenu.xib 4196行目 - box id="11697" (Sources)
        // HOROS-20240407準拠: MainMenu.xib 4225行目 - tableView id="14153" (Activity)
        splitSourcesActivity = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitSourcesActivity.setName("splitSourcesActivity"); // 検索用に名前を設定
        
        // Sources（中）- データベースリスト
        // HOROS-20240407準拠: BrowserController.h 138行目 IBOutlet NSTableView *_sourcesTableView;
        // HOROS-20240407準拠: MainMenu.xib 4156行目 - tableView id="11693"
        // HOROS-20240407準拠: MainMenu.xib 4163行目 - tableColumn identifier="Source" title="Sources"
        // HOROS-20240407準拠: MainMenu.xib 4176行目 - binding keyPath="arrangedObjects.description"
        String[] sourcesColumns = { "Sources" };
        javax.swing.table.DefaultTableModel sourcesModel = new javax.swing.table.DefaultTableModel(sourcesColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        // HOROS-20240407準拠: 初期データとして"Documents DB"を追加
        sourcesModel.addRow(new Object[] { "Documents DB" });
        sourcesTableView = new javax.swing.JTable(sourcesModel);
        sourcesTableView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // HOROS-20240407準拠: MainMenu.xib 4156行目 - columnReordering="NO" columnResizing="NO"
        sourcesTableView.getTableHeader().setReorderingAllowed(false);
        sourcesTableView.setColumnSelectionAllowed(false);
        sourcesTableView.setRowSelectionAllowed(true);
        // HOROS-20240407準拠: MainMenu.xib 4163行目 - 列識別子を設定
        sourcesTableView.getColumnModel().getColumn(0).setIdentifier("Source");
        // HOROS-20240407準拠: 選択変更リスナー
        sourcesTableView.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                sourceSelectionChanged();
            }
        });
        JScrollPane sourcesScroll = new JScrollPane(sourcesTableView);
        // HOROS-20240407準拠: MainMenu.xib 4142行目 - box title="Sources"
        // titlePosition="noTitle"
        // Java SwingではTitledBorderで見出しを表示
        sourcesScroll.setBorder(javax.swing.BorderFactory.createTitledBorder(
                javax.swing.BorderFactory.createEmptyBorder(),
                "Sources",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP));
        splitSourcesActivity.setTopComponent(sourcesScroll);
        
        // Activity（下）
        // HOROS-20240407準拠: BrowserController+Activity.h
        // HOROS-20240407準拠: BrowserController+Activity.mm 64-72行目
        // HOROS-20240407準拠: MainMenu.xib 4225行目 - tableView id="14153"
        // customClass="ThreadsTableView"
        activityHelper = new BrowserActivityHelper(this);

        // HOROS-20240407準拠: BrowserController+Activity.mm 67-68行目
        // [_activityTableView setDelegate: _activityHelper];
        // [_activityTableView setDataSource: _activityHelper];
        this.activityTableView = new javax.swing.JTable(activityHelper);
        // HOROS-20240407準拠: BrowserController.m 338行目 - Regular mode: 38
        this.activityTableView.setRowHeight(38);
        // HOROS-20240407準拠: MainMenu.xib 4225行目 - headerView="14179" (Activityヘッダーあり)
        // HOROS-20240407準拠: MainMenu.xib 4233行目 - tableHeaderCell title="Activity"
        // Activityテーブルにはヘッダーがあり、"Activity"というタイトルが表示される
        this.activityTableView.getTableHeader().setReorderingAllowed(false);
        this.activityTableView.setShowGrid(false);
        this.activityTableView.setIntercellSpacing(new java.awt.Dimension(0, 0));

        // HOROS-20240407準拠: ThreadCellRendererを使用してThreadCellを表示
        this.activityTableView.setDefaultRenderer(Object.class,
                new com.jj.dicomviewer.ui.ThreadCellRenderer(activityHelper));

        // HOROS-20240407準拠: Activityパネルにテーブルを追加
        // HOROS-20240407準拠: MainMenu.xib 4211行目 - box id="14167" title="Threads"
        // titlePosition="noTitle"
        // HOROS-20240407準拠: MainMenu.xib 4233行目 - tableHeaderCell title="Activity"
        // Activityテーブルのヘッダーに"Activity"が表示されるため、TitledBorderは不要
        JScrollPane activityScroll = new JScrollPane(this.activityTableView);
        activityScroll.setBorder(null);
        splitSourcesActivity.setBottomComponent(activityScroll);
        // HOROS-20240407準拠: MainMenu.xib 4143行目 - Sources box height="381"
        // Sourcesパネルの高さを3/4に設定
        splitSourcesActivity.setResizeWeight(0.75); // Sourcesが75%（3/4）

        // splitAlbumsの下部にSourcesとActivityを含むスプリッターを設定
        splitAlbums.setBottomComponent(splitSourcesActivity);
        // HOROS-20240407準拠: MainMenu.xib 4083行目 - Albums box height="198"
        // HOROS-20240407準拠: Albums: 198px, Sources+Activity: 489px, 合計: 687px
        // Albumsの割合: 198/687 ≈ 0.29
        splitAlbums.setResizeWeight(0.29); // Albumsが29%（HOROS-20240407準拠）
        
        // Activityの最小高さを設定（見えるようにするため）
        activityScroll.setMinimumSize(new java.awt.Dimension(0, 108));

        // HOROS-20240407準拠: awakeActivityを呼び出す（BrowserController.m 14432行目）
        // ただし、activityHelperは既に初期化済みなので、ここではactivityTableViewの設定のみ行う
        // 実際のawakeActivity相当の処理はBrowserActivityHelperのコンストラクタで行われる

        // ========== 右側メインパネル ==========
        // HOROS-20240407準拠: MainMenu.xib 4281行目 - splitComparative (id="14729") 垂直スプリッター
        // HOROS-20240407準拠: BrowserController.h 111行目 IBOutlet NSSplitView *splitComparative;
        splitComparative = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // 上部テーブルエリア（水平スプリッター）: databaseOutline | comparativeScrollView
        // HOROS-20240407準拠: MainMenu.xib 4284-4714行目
        JSplitPane topTablesSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // 患者情報テーブル（左）- databaseOutline
        // HOROS-20240407準拠: BrowserController.h 130行目 IBOutlet MyOutlineView *databaseOutline;
        // HOROS-20240407準拠: MainMenu.xib 4292行目 - outlineView id="966"
        databaseOutline = new DatabaseOutlineView(this);
        JScrollPane patientTableScroll = new JScrollPane(databaseOutline);
        topTablesSplit.setLeftComponent(patientTableScroll);
        
        // 履歴パネル（右）- comparativeTable
        // HOROS-20240407準拠: BrowserController.h 270行目 IBOutlet NSTableView *comparativeTable;
        // HOROS-20240407準拠: MainMenu.xib 4666-4714行目 - comparativeScrollView id="14720" width="205"
        // HOROS-20240407準拠: BrowserController.m 11369-11393行目 - ComparativeCellを使用して2行表示
        String[] comparativeColumns = { "History" };
        javax.swing.table.DefaultTableModel comparativeModel = new javax.swing.table.DefaultTableModel(comparativeColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        comparativeTable = new javax.swing.JTable(comparativeModel);
        comparativeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        comparativeTable.getTableHeader().setReorderingAllowed(false);
        // HOROS-20240407準拠: 行の高さを設定（2行表示のため）
        // HOROS-20240407準拠: BrowserController.m 322-353行目 - setRowHeight: 13/16/21/24/29/43
        // 通常モード（mode == 0）: gHorizontalHistoryの場合は16、そうでない場合は29
        // 罫線に重ならないように、行の高さを適切に設定（ユーザー要求：下が余っているので小さく）
        comparativeTable.setRowHeight(30); // 2行表示に適した高さ（下の余白を減らす）
        
        // HOROS-20240407準拠: カスタムセルレンダラーを設定（2行表示：スタディ名/モダリティ、日付/画像数）
        // HOROS-20240407準拠: BrowserController.m 11369-11393行目 - ComparativeCellを使用して2行表示
        
        // HOROS-20240407準拠: ComparativeCell.mm 185-254行目 - カスタム描画を使用
        // HOROS-20240407準拠: 右寄せのテキストを先に描画し、その幅を取得して左側のテキストの幅を調整
        comparativeTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                // HOROS-20240407準拠: ComparativeCell.mm 98-105行目 - カスタム描画を使用
                final javax.swing.JTable finalTable = table;
                final boolean finalRowSelected = isSelected || table.isRowSelected(row);
                
                // カスタムJPanelクラスを定義
                class ComparativeCellPanel extends javax.swing.JPanel {
                    private com.jj.dicomviewer.model.DicomStudy study;
                    private boolean selected;
                    private java.awt.Font customFont;
                    private javax.swing.JTable tableRef;
                    
                    public void setStudy(com.jj.dicomviewer.model.DicomStudy s) { 
                        this.study = s; 
                        // HOROS-20240407準拠: BrowserController.m 11369-11401行目 - willDisplayCell
                        // HISTORYパネル（comparativeTable）のセルには画像を設定しない
                        // テキスト情報のみを設定（leftTextFirstLine, rightTextFirstLine, leftTextSecondLine, rightTextSecondLine）
                    }
                    public void setSelected(boolean sel) { this.selected = sel; }
                    public void setCustomFont(java.awt.Font f) { this.customFont = f; }
                    public void setTable(javax.swing.JTable t) { this.tableRef = t; }
                    
                    @Override
                    protected void paintComponent(java.awt.Graphics g) {
                        super.paintComponent(g);
                        
                        if (study == null || tableRef == null) return;
                        
                        java.awt.Graphics2D g2d = (java.awt.Graphics2D) g.create();
                        g2d.setFont(customFont != null ? customFont : getFont());
                        
                        // HOROS-20240407準拠: ComparativeCell.mm 100-102行目 - 罫線を描画
                        g2d.setColor(new java.awt.Color(0.666f, 0.666f, 0.666f, 0.333f));
                        g2d.setStroke(new java.awt.BasicStroke(1.0f));
                        int lineY = getHeight() - 1;
                        g2d.drawLine(0, lineY, getWidth(), lineY);
                        
                        // HOROS-20240407準拠: ComparativeCell.mm 56行目 - setImagePosition:NSImageLeft
                        // 注意: HOROS-20240407では、HISTORYパネル（comparativeTable）にサムネイル画像を表示しない
                        // BrowserController.m 11369-11401行目のwillDisplayCellでは、画像を設定していない
                        // そのため、サムネイル画像は描画しない（HOROS-20240407準拠）
                        int textStartX = 0;
                        
                        // HOROS-20240407準拠: スタディ名、モダリティ、日付、画像数を取得
                        String studyName = study.getStudyName();
                        boolean isUnnamed = false;
                        if (studyName == null || studyName.isEmpty() || "unnamed".equalsIgnoreCase(studyName)) {
                            studyName = "Unnamed";
                            isUnnamed = true;
                        }
                        String modality = study.getModality();
                        if (modality == null || modality.isEmpty()) {
                            modality = "";
                        }
                        java.time.LocalDateTime date = study.getDate();
                        String dateStr = "";
                        if (date != null) {
                            try {
                                dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
                            } catch (Exception e) {
                                dateStr = date.toString();
                            }
                        }
                        Integer numberOfImages = study.getNumberOfImages();
                        String imageCountStr = "";
                        if (numberOfImages != null) {
                            int count = Math.abs(numberOfImages.intValue());
                            imageCountStr = count + (count == 1 ? " image" : " images");
                        }
                        
                        // HOROS-20240407準拠: 色を設定
                        java.awt.Color fgColor = selected ? tableRef.getSelectionForeground() : tableRef.getForeground();
                        java.awt.Color studyNameColor = fgColor;
                        if (isUnnamed && !selected) {
                            java.awt.Color originalColor = tableRef.getForeground();
                            java.awt.Color grayColor = new java.awt.Color(128, 128, 128);
                            studyNameColor = new java.awt.Color(
                                (int)(originalColor.getRed() * 0.6 + grayColor.getRed() * 0.4),
                                (int)(originalColor.getGreen() * 0.6 + grayColor.getGreen() * 0.4),
                                (int)(originalColor.getBlue() * 0.6 + grayColor.getBlue() * 0.4)
                            );
                        }
                        java.awt.Color dateColor = selected ? fgColor : new java.awt.Color(102, 102, 102);
                        
                        // HOROS-20240407準拠: ComparativeCell.mm 185-254行目
                        // 1行目：rightTextFirstLine（右寄せ、モダリティ）を先に描画
                        // 右端が切れないように、列幅を考慮して描画位置を計算
                        // テーブルの列幅を取得（列幅が設定されていない場合はgetWidth()を使用）
                        int cellWidth = getWidth();
                        if (tableRef != null && tableRef.getColumnModel().getColumnCount() > 0) {
                            int columnWidth = tableRef.getColumnModel().getColumn(0).getWidth();
                            if (columnWidth > 0) {
                                cellWidth = columnWidth;
                            }
                        }
                        // 垂直スクロールバーが表示されている場合、その幅を考慮して描画領域を調整
                        // ただし、セル自体の幅は列幅に合わせられているため、getWidth()を使用
                        // 実際の描画可能な幅は、セルの幅（列幅）を使用
                        // 右端に3ピクセルのマージンを設けて、スクロールバーに被さることを防ぐ
                        java.awt.Rectangle frame = new java.awt.Rectangle(textStartX, 0, cellWidth - textStartX, getHeight());
                        final int spacer = 2; // HOROS-20240407準拠: ComparativeCell.mm 112行目
                        final int lineSpace = 12; // HOROS-20240407準拠: BrowserController.m 368行目 - comparativeLineSpace
                        final int rightPadding = 3; // 右端のパディング（スクロールバーに被さることを防ぐため）
                        
                        // 1行目：右寄せのテキスト（モダリティ）を先に描画
                        if (modality != null && !modality.isEmpty()) {
                            java.awt.FontMetrics modalityFm = g2d.getFontMetrics();
                            int modalityWidth = modalityFm.stringWidth(modality);
                            g2d.setColor(fgColor);
                            // 右端が切れないように、列幅から直接計算（列幅は既にスクロールバーを考慮済み）
                            g2d.drawString(modality, cellWidth - modalityWidth - rightPadding, frame.y + 1 + modalityFm.getAscent());
                            
                            // 左側のテキスト用のフレーム幅を調整
                            frame.width -= modalityWidth + spacer + rightPadding;
                        }
                        
                        // 1行目：左寄せのテキスト（スタディ名）を描画
                        if (studyName != null && !studyName.isEmpty()) {
                            java.awt.FontMetrics nameFm = g2d.getFontMetrics();
                            g2d.setColor(studyNameColor);
                            String displayName = studyName;
                            int nameWidth = nameFm.stringWidth(displayName);
                            if (nameWidth > frame.width) {
                                // 省略表示
                                displayName = nameFm.stringWidth("...") > frame.width ? "" : 
                                    nameFm.stringWidth(displayName.substring(0, Math.max(0, displayName.length() - 3)) + "...") <= frame.width ?
                                    displayName.substring(0, Math.max(0, displayName.length() - 3)) + "..." : displayName;
                            }
                            g2d.drawString(displayName, frame.x, frame.y + 1 + nameFm.getAscent());
                        }
                        
                        // 2行目：右寄せのテキスト（画像数）を先に描画
                        frame = new java.awt.Rectangle(textStartX, 0, cellWidth - textStartX, getHeight());
                        if (imageCountStr != null && !imageCountStr.isEmpty()) {
                            java.awt.Font smallFont = g2d.getFont().deriveFont(g2d.getFont().getSize() * 0.85f);
                            java.awt.FontMetrics imageFm = g2d.getFontMetrics(smallFont);
                            g2d.setFont(smallFont);
                            int imageWidth = imageFm.stringWidth(imageCountStr);
                            g2d.setColor(dateColor);
                            // 右端が切れないように、列幅から直接計算（列幅は既にスクロールバーを考慮済み）
                            g2d.drawString(imageCountStr, cellWidth - imageWidth - rightPadding, frame.y + lineSpace + 1 + imageFm.getAscent());
                            
                            // 左側のテキスト用のフレーム幅を調整
                            frame.width -= imageWidth + spacer + rightPadding;
                        }
                        
                        // 2行目：左寄せのテキスト（日付）を描画
                        if (dateStr != null && !dateStr.isEmpty()) {
                            java.awt.Font smallFont = g2d.getFont().deriveFont(g2d.getFont().getSize() * 0.85f);
                            java.awt.FontMetrics dateFm = g2d.getFontMetrics(smallFont);
                            g2d.setFont(smallFont);
                            g2d.setColor(dateColor);
                            g2d.drawString(dateStr, frame.x, frame.y + lineSpace + 1 + dateFm.getAscent());
                        }
                        
                        g2d.dispose();
                    }
                }
                
                ComparativeCellPanel panel = new ComparativeCellPanel();
                panel.setOpaque(true);
                
                if (finalRowSelected) {
                    panel.setBackground(finalTable.getSelectionBackground());
                } else {
                    panel.setBackground(finalTable.getBackground());
                }
                
                // HOROS-20240407準拠: BrowserController.m 11373-11393行目
                com.jj.dicomviewer.model.DicomStudy dicomStudy = null;
                if (value instanceof com.jj.dicomviewer.model.DicomStudy) {
                    dicomStudy = (com.jj.dicomviewer.model.DicomStudy) value;
                } else if (comparativeStudies != null && row >= 0 && row < comparativeStudies.size()) {
                    Object study = comparativeStudies.get(row);
                    if (study instanceof com.jj.dicomviewer.model.DicomStudy) {
                        dicomStudy = (com.jj.dicomviewer.model.DicomStudy) study;
                    }
                }
                
                if (dicomStudy != null) {
                    // HOROS-20240407準拠: ローカルスタディは太字（BrowserController.m 11383行目）
                    // HOROS-20240407準拠: フォントサイズを2ポイント小さくする（ユーザー要求）
                    java.awt.Font originalFont = finalTable.getFont();
                    java.awt.Font smallerFont = originalFont.deriveFont(java.awt.Font.BOLD, Math.max(9, originalFont.getSize() - 2));
                    panel.setFont(smallerFont);
                    
                    // パネルにデータを設定
                    panel.setStudy(dicomStudy);
                    panel.setSelected(finalRowSelected);
                    panel.setCustomFont(smallerFont);
                    panel.setTable(finalTable);
                }
                
                return panel;
            }
        });
        // HOROS-20240407準拠: HISTORYパネルの選択変更イベントを処理
        // HOROS-20240407準拠: BrowserController.m 11828-11901行目 - tableViewSelectionDidChange:
        comparativeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = comparativeTable.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < comparativeStudies.size()) {
                    Object study = comparativeStudies.get(selectedRow);
                    if (study instanceof com.jj.dicomviewer.model.DicomStudy) {
                        // HOROS-20240407準拠: DBリストで同じstudyInstanceUIDを持つスタディを選択
                        String studyInstanceUID = ((com.jj.dicomviewer.model.DicomStudy) study).getStudyInstanceUID();
                        if (studyInstanceUID != null) {
                            // HOROS-20240407準拠: databaseOutlineで同じstudyInstanceUIDを持つスタディを検索して選択
                            SwingUtilities.invokeLater(() -> {
                                try {
                                    // HOROS-20240407準拠: databaseOutlineで同じstudyInstanceUIDを持つスタディを検索して選択
                                    com.jj.dicomviewer.model.DicomStudy studyToSelect = null;
                                    List<Object> outlineArray = getOutlineViewArray();
                                    if (outlineArray != null) {
                                        for (Object item : outlineArray) {
                                            if (item instanceof com.jj.dicomviewer.model.DicomStudy) {
                                                com.jj.dicomviewer.model.DicomStudy s = (com.jj.dicomviewer.model.DicomStudy) item;
                                                if (studyInstanceUID.equals(s.getStudyInstanceUID())) {
                                                    studyToSelect = s;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (studyToSelect != null) {
                                        // HOROS-20240407準拠: スタディを選択
                                        if (databaseOutline.selectStudy(studyToSelect)) {
                                            // HOROS-20240407準拠: 選択変更後にoutlineViewSelectionDidChangeを呼び出す
                                            outlineViewSelectionDidChange();
                                        }
                                    }
                                } catch (Exception ex) {
                                    // エラーは無視
                                }
                            });
                        }
                    }
                }
            }
        });
        
        // HOROS-20240407準拠: HISTORYパネルのダブルクリックイベントを処理
        // HOROS-20240407準拠: BrowserController.m 14396行目 - [comparativeTable setDoubleAction: @selector(doubleClickComparativeStudy:)];
        comparativeTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = comparativeTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < comparativeStudies.size()) {
                        Object study = comparativeStudies.get(row);
                        if (study instanceof com.jj.dicomviewer.model.DicomStudy) {
                            // HOROS-20240407準拠: ダブルクリックでビューワーを開く
                            // HOROS-20240407準拠: BrowserController.m 11810-11827行目 - doubleClickComparativeStudy:
                            // HOROS-20240407準拠: displayStudy:object:command:を呼び出す
                            com.jj.dicomviewer.model.DicomStudy dicomStudy = (com.jj.dicomviewer.model.DicomStudy) study;
                            // HOROS-20240407準拠: まずスタディを選択してからビューワーを開く
                            if (databaseOutline.selectStudy(dicomStudy)) {
                                outlineViewSelectionDidChange();
                                // HOROS-20240407準拠: ビューワーを開く（matrixPressedと同様の処理）
                                matrixPressed(null);
                            }
                        }
                    }
                }
            }
        });
        
        JScrollPane comparativeScroll = new JScrollPane(comparativeTable);
        // HOROS-20240407準拠: 履歴パネルの幅を160ピクセルに固定（ユーザー要求：180ピクセルからさらに縮小、水平スクロールバーが出ないように）
        int historyPanelWidth = 160;
        comparativeScroll.setPreferredSize(new java.awt.Dimension(historyPanelWidth, 0));
        comparativeScroll.setMinimumSize(new java.awt.Dimension(historyPanelWidth, 0));
        comparativeScroll.setMaximumSize(new java.awt.Dimension(historyPanelWidth, Integer.MAX_VALUE));
        
        // 水平スクロールバーを無効にする（ユーザー要求：水平スクロールバーが出ないように）
        comparativeScroll.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // HOROS-20240407準拠: テーブルのセル間隔を0に設定して余白を減らす
        comparativeTable.setIntercellSpacing(new java.awt.Dimension(0, 0));
        
        // HOROS-20240407準拠: テーブルの列幅をパネル幅に完全に合わせる
        // スクロールバーが表示されない場合の余白を防ぐため、列幅をパネル幅に完全に合わせる
        comparativeTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        
        // 列幅を動的に調整する関数（水平スクロールバーが出ないように、列幅をパネル幅に完全に合わせる）
        // HOROS-20240407準拠: スクロールバーが表示されていてもいなくても、常にスクロールバー分のスペースを確保
        java.util.function.Consumer<Void> adjustColumnWidth = (Void v) -> {
            if (comparativeTable.getColumnModel().getColumnCount() > 0) {
                javax.swing.JScrollBar verticalScrollBar = comparativeScroll.getVerticalScrollBar();
                int availableWidth = historyPanelWidth;
                // スクロールバーが表示されているかどうかに関わらず、常にスクロールバーの幅を引く
                // HOROS-20240407準拠: スクロールバー分のスペースを常に確保することで、右端の文字が切れないようにする
                if (verticalScrollBar != null) {
                    // スクロールバーの幅を取得（表示されていない場合でも、preferredSizeから取得）
                    int scrollBarWidth = verticalScrollBar.isVisible() ? 
                        verticalScrollBar.getWidth() : 
                        verticalScrollBar.getPreferredSize().width;
                    availableWidth -= scrollBarWidth;
                }
                // 水平スクロールバーが出ないように、列幅を利用可能な幅に完全に合わせる
                // テーブルの幅と列幅の合計が一致するようにする
                int columnWidth = availableWidth;
                comparativeTable.getColumnModel().getColumn(0).setPreferredWidth(columnWidth);
                comparativeTable.getColumnModel().getColumn(0).setMinWidth(columnWidth);
                comparativeTable.getColumnModel().getColumn(0).setMaxWidth(columnWidth);
                // テーブル自体の幅も設定（列幅の合計と一致させる）
                comparativeTable.setPreferredScrollableViewportSize(new java.awt.Dimension(columnWidth, 0));
                // テーブルのサイズを強制的に更新
                comparativeTable.setSize(columnWidth, comparativeTable.getHeight());
                // テーブルを再描画して、列幅の変更を反映
                comparativeTable.revalidate();
                comparativeTable.repaint();
            }
        };
        
        // スクロールバーの表示状態に応じて列幅を動的に調整するリスナーを追加
        javax.swing.JScrollBar verticalScrollBar = comparativeScroll.getVerticalScrollBar();
        if (verticalScrollBar != null) {
            verticalScrollBar.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentShown(java.awt.event.ComponentEvent e) {
                    adjustColumnWidth.accept(null);
                }
                
                @Override
                public void componentHidden(java.awt.event.ComponentEvent e) {
                    adjustColumnWidth.accept(null);
                }
            });
        }
        
        // 初期状態：列幅をパネル幅に完全に合わせる（スクロールバーが表示されない場合を想定）
        // コンポーネントが表示された後に、スクロールバーの状態を確認して列幅を調整
        SwingUtilities.invokeLater(() -> {
            adjustColumnWidth.accept(null);
        });
        
        topTablesSplit.setRightComponent(comparativeScroll);
        topTablesSplit.setResizeWeight(1.0); // 患者情報テーブルが可変、履歴パネルが固定
        // HOROS-20240407準拠: 履歴パネルの幅を固定（ディバイダーを非表示にする）
        topTablesSplit.setDividerSize(0); // デバイダーを非表示（動かせない仕様のため）
        SwingUtilities.invokeLater(() -> {
            int totalWidth = topTablesSplit.getWidth();
            if (totalWidth > 0) {
                topTablesSplit.setDividerLocation(totalWidth - historyPanelWidth);
            }
        });

        splitComparative.setTopComponent(topTablesSplit);

        // 下部プレビューエリア（水平スプリッター）: サムネイル（左） | プレビュー（右）
        // HOROS-20240407準拠: MainMenu.xib 4732行目 - splitView id="14624" (Matrix Split) 水平スプリッター
        bottomPreviewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // HOROS-20240407準拠: _bottomSplitは標準のNSSplitViewとして実装されている
        // HOROS-20240407準拠: カスタムマウスイベント処理はない - NSSplitViewの標準機能を使用
        // HOROS-20240407準拠: BrowserController.m 10509-10513行目、10585-10586行目
        // Java Swingでは標準のJSplitPaneの機能を使用（カスタムマウスイベント処理を削除）
        // HOROS-20240407準拠: ディバイダーのサイズを2pxに設定（一番右まで来ても制御できるように）
        bottomPreviewSplit.setDividerSize(2);
        bottomPreviewSplit.setBorder(null);
        bottomPreviewSplit.setOpaque(false);
        
        // HOROS-20240407準拠: ディバイダーを非表示にするため、カスタムUIで描画を無効化
        // HOROS-20240407準拠: ディバイダーのサイズを2pxに設定（一番右まで来ても制御できるように）
        bottomPreviewSplit.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(java.awt.Graphics g) {
                        // 描画しない（完全に非表示）
                    }
                    
                    @Override
                    public java.awt.Dimension getPreferredSize() {
                        // サイズは2px（ドラッグ可能にするため、一番右まで来ても制御できるように）
                        return new java.awt.Dimension(2, 1);
                    }
                    
                    @Override
                    public void setBounds(int x, int y, int width, int height) {
                        // ディバイダーの位置を設定（幅は2px、高さは親の高さ）
                        super.setBounds(x, y, 2, height);
                    }
                };
            }
        });
        
        // サムネイル（左）- oMatrix
        // HOROS-20240407準拠: MainMenu.xib 4752行目 - scrollView id="12410" (thumbnailsScrollView)
        // HOROS-20240407準拠: MainMenu.xib 4759行目 - matrix id="12411" (oMatrix)
        // HOROS-20240407準拠: BrowserController.h 132行目 IBOutlet BrowserMatrix *oMatrix;
        // HOROS-20240407準拠: BrowserController.h 204行目 IBOutlet NSScrollView *thumbnailsScrollView;
        oMatrix = new BrowserMatrix(this);
        
        // HOROS-20240407準拠: MainMenu.xib準拠
        // マトリックスを直接JScrollPaneに配置
        // スクロールバーの幅（40px）分だけ表示領域を小さくすることで、スクロールバーがセルに被らないようにする
        // JScrollPaneの作成をtry-catchで囲む（初期化中にgetViewport()が失敗する可能性があるため）
        try {
            thumbnailsScrollView = new JScrollPane(oMatrix);
            // HOROS-20240407準拠: BrowserController.m 14239-14240行目
            // [thumbnailsScrollView setDrawsBackground:NO];
            // [[thumbnailsScrollView contentView] setDrawsBackground:NO];
            thumbnailsScrollView.setBackground(null);
            // JViewportの設定はcomponentShownイベントで実行する（初期化中にgetViewport()が失敗する可能性があるため）
            // HOROS-20240407準拠: 水平スクロールバーは不要（セルサイズに合わせてディバイダー位置を調整するため）
            thumbnailsScrollView.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            // 垂直スクロールバーを常に表示（左側のサムネイルパネル内に表示）
            // スクロールが不要なときは無効化、必要なときだけ有効化
            thumbnailsScrollView.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            // bottomPreviewSplitにthumbnailsScrollViewを設定
            // SwingUtilities.invokeLaterで遅延実行することで、JScrollPaneが完全に初期化されるまで待機
            SwingUtilities.invokeLater(() -> {
                try {
                    if (bottomPreviewSplit != null && thumbnailsScrollView != null) {
                        bottomPreviewSplit.setLeftComponent(thumbnailsScrollView);
                    }
                } catch (Exception ex) {
                    // 初期化中にsetLeftComponent()が失敗する可能性があるため、例外をキャッチ
                    // 後で再試行する
                }
            });
        } catch (Exception ex) {
            // JScrollPaneの作成中にエラーが発生した場合、後で再試行する
            thumbnailsScrollView = null;
        }
        
        // HOROS-20240407準拠: bottomPreviewSplitのディバイダー位置変更時にpreviewMatrixScrollViewFrameDidChangeを呼び出す
        // componentShownイベントが発生した後にリスナーを追加するため、ここでは追加しない
        // componentShownイベントで追加する
        
        // プレビュー（右）- imageView
        // HOROS-20240407準拠: MainMenu.xib 4864行目 - customView id="1166" customClass="PreviewView"
        // HOROS-20240407準拠: BrowserController.h 146行目 IBOutlet PreviewView *imageView;
        imageView = new PreviewView();
        // HOROS-20240407準拠: BrowserController.m 14250行目 - [imageView setTheMatrix:oMatrix];
        imageView.setTheMatrix(oMatrix);
        imageView.setBrowserController(this);
        
        // HOROS-20240407準拠: マウススクロール処理を追加
        // BrowserController.m 9264行目: - (void)scrollWheel: (NSEvent *)theEvent
        if (oMatrix != null) {
            oMatrix.addMouseWheelListener(e -> {
                // HOROS-20240407準拠: BrowserController.m 9268-9271行目
                // スクロールホイールの反転設定を確認
                boolean reverseScrollWheel = java.util.prefs.Preferences.userRoot().node("com.jj.dicomviewer").getBoolean("Scroll Wheel Reversed", false);
                float change = reverseScrollWheel ? -1.0f : 1.0f;
                change *= e.getWheelRotation();
                
                if (e.getWheelRotation() == 0) {
                    return;
                }
                
                // HOROS-20240407準拠: BrowserController.m 9278-9295行目
                // HOROS-20240407準拠: Studyタイプの場合でも、animationSliderの値を更新してサムネイルを選択する
                if (animationSlider != null) {
                    Object aFile = databaseOutline != null ? databaseOutline.getSelectedItem() : null;
                    String type = aFile != null ? getItemType(aFile) : null;
                    
                    // HOROS-20240407準拠: Studyタイプの場合、previewPixのサイズを最大値として使用
                    int maxValue = animationSlider.getMaximum();
                    if ("Study".equals(type) && previewPix != null && !previewPix.isEmpty()) {
                        maxValue = previewPix.size() - 1;
                    }
                    
                    // HOROS-20240407準拠: animationSliderが無効化されている場合でも、値を更新する
                    if (maxValue > 0 || "Study".equals(type)) {
                        int pos = animationSlider.getValue();
                        
                        if (change > 0) {
                            change = 1;
                            pos += (int) change;
                        } else {
                            change = -1;
                            pos += (int) change;
                        }
                        
                        if (pos > maxValue) pos = 0;
                        if (pos < 0) pos = maxValue;
                        
                        animationSlider.setValue(pos);
                        // HOROS-20240407準拠: BrowserController.m 9295行目
                        // [self previewSliderAction: animationSlider];
                        previewSliderAction(animationSlider);
                    }
                }
            });
        }
        
        // プレビュービューにもマウススクロール処理を追加
        if (imageView != null) {
            imageView.addMouseWheelListener(e -> {
                // 同じロジックを適用
                boolean reverseScrollWheel = java.util.prefs.Preferences.userRoot().node("com.jj.dicomviewer").getBoolean("Scroll Wheel Reversed", false);
                float change = reverseScrollWheel ? -1.0f : 1.0f;
                change *= e.getWheelRotation();
                
                if (e.getWheelRotation() == 0) {
                    return;
                }
                
                // HOROS-20240407準拠: BrowserController.m 9278-9295行目
                // HOROS-20240407準拠: Studyタイプの場合でも、animationSliderの値を更新してサムネイルを選択する
                if (animationSlider != null) {
                    Object aFile = databaseOutline != null ? databaseOutline.getSelectedItem() : null;
                    String type = aFile != null ? getItemType(aFile) : null;
                    
                    // HOROS-20240407準拠: Studyタイプの場合、previewPixのサイズを最大値として使用
                    int maxValue = animationSlider.getMaximum();
                    if ("Study".equals(type) && previewPix != null && !previewPix.isEmpty()) {
                        maxValue = previewPix.size() - 1;
                    }
                    
                    // HOROS-20240407準拠: animationSliderが無効化されている場合でも、値を更新する
                    if (maxValue > 0 || "Study".equals(type)) {
                        int pos = animationSlider.getValue();
                        
                        if (change > 0) {
                            change = 1;
                            pos += (int) change;
                        } else {
                            change = -1;
                            pos += (int) change;
                        }
                        
                        if (pos > maxValue) pos = 0;
                        if (pos < 0) pos = maxValue;
                        
                        animationSlider.setValue(pos);
                        // HOROS-20240407準拠: BrowserController.m 9295行目
                        // [self previewSliderAction: animationSlider];
                        previewSliderAction(animationSlider);
                    }
                }
            });
        }
        
        // プレビューエリア（右側のスクロールバーは非表示）
        // 手動スクロールバーは非表示にするが、後で使用する可能性があるため保持
        thumbnailsVerticalScrollBar = new javax.swing.JScrollBar(javax.swing.JScrollBar.VERTICAL);
        thumbnailsVerticalScrollBar.setEnabled(false); // 初期状態では無効化
        thumbnailsVerticalScrollBar.setVisible(false); // 非表示
        
        // 手動スクロールバーのイベントリスナーを追加（JScrollPaneのビューポートと同期）
        // 現在は非表示だが、将来使用する可能性があるため保持
        thumbnailsVerticalScrollBar.addAdjustmentListener(e -> {
            if (thumbnailsScrollView != null && !isAdjustingThumbnailsScrollBar) {
                isAdjustingThumbnailsScrollBar = true;
                try {
                    javax.swing.JViewport viewport = thumbnailsScrollView.getViewport();
                    if (viewport != null) {
                        java.awt.Point currentPosition = viewport.getViewPosition();
                        int newY = e.getValue();
                        if (currentPosition.y != newY) {
                            viewport.setViewPosition(new java.awt.Point(currentPosition.x, newY));
                        }
                    }
                } finally {
                    isAdjustingThumbnailsScrollBar = false;
                }
            }
        });
        
        // プレビューパネル（スクロールバーは非表示のため、imageViewのみ配置）
        javax.swing.JPanel previewPanel = new javax.swing.JPanel(new java.awt.BorderLayout());
        previewPanel.setOpaque(false);
        previewPanel.add(imageView, java.awt.BorderLayout.CENTER);
        
        bottomPreviewSplit.setRightComponent(previewPanel);
        bottomPreviewSplit.setResizeWeight(0.3); // サムネイルが30%

        // ステータスバー: 左側（databaseDescription） | 右側（animationSlider + Playボタン）
        // HOROS-20240407準拠: MainMenu.xib 4895行目 - splitView id="14352" (_bottomSplit)
        // ただし、デバイダーは不要なため、JSplitPaneではなくBorderLayoutで配置
        // HOROS-20240407準拠: BrowserController.h 144行目 IBOutlet NSSplitView* _bottomSplit;
        JPanel bottomSplit = new JPanel(new BorderLayout());
        
        // 左パネル: databaseDescriptionのみ（ボタンは存在しない）
        // HOROS-20240407準拠: MainMenu.xib 4899行目 - customView id="et0-HG-KxJ" width="431"
        // HOROS-20240407準拠: MainMenu.xib 4914行目 - textField id="14357" (databaseDescription)
        // HOROS-20240407準拠: BrowserController.h 129行目 IBOutlet NSTextField *databaseDescription;
        databaseDescription = new JTextField("Local Database: Documents DB / No album selected / Result = 0 studies (0 images)");
        databaseDescription.setEditable(false);
        databaseDescription.setBorder(null);
        databaseDescription.setHorizontalAlignment(JTextField.LEFT);
        bottomSplit.add(databaseDescription, BorderLayout.WEST);
        
        // 右パネル: animationSlider + Playボタン
        // HOROS-20240407準拠: MainMenu.xib 4925行目 - customView id="14354" width="611"
        // HOROS-20240407準拠: MainMenu.xib 4929行目 - slider id="1293" (animationSlider)
        // HOROS-20240407準拠: MainMenu.xib 4939行目 - button id="1294" (Play)
        JPanel rightStatusPanel = new JPanel(null);
        rightStatusPanel.setPreferredSize(new java.awt.Dimension(611, 21));
        rightStatusPanel.setMinimumSize(new java.awt.Dimension(611, 21));
        rightStatusPanel.setMaximumSize(new java.awt.Dimension(611, 21));
        rightStatusPanel.setLayout(null);
        
        // Playチェックボックス（右端に配置、幅を1.5倍に）
        // 元の幅40px → 60px（1.5倍）
        // HOROS-20240407準拠: BrowserController.h 143行目 IBOutlet NSButton *animationCheck;
        // HOROS-20240407準拠: MainMenu.xib 4942行目 - controlSize="mini" font="metaFont="miniSystem"
        // HOROS-20240407準拠: IBOutlet NSButton *animationCheck; (BrowserController.h 143行目)
        animationCheck = new javax.swing.JCheckBox("Play");
        java.awt.Font miniFont = new java.awt.Font(animationCheck.getFont().getName(), 
            java.awt.Font.PLAIN, Math.max(9, animationCheck.getFont().getSize() - 2));
        animationCheck.setFont(miniFont);
        // 右端に配置、幅60px（1.5倍）、高さ20px（そのまま）
        animationCheck.setBounds(611 - 60, 2, 60, 20);
        // HOROS-20240407準拠: BrowserController.m 14343行目 - 状態を復元
        boolean autoPlayAnimation = java.util.prefs.Preferences.userNodeForPackage(BrowserController.class).getBoolean("AutoPlayAnimation", false);
        animationCheck.setSelected(autoPlayAnimation);
        // HOROS-20240407準拠: チェックボックスのアクション（状態変更時に保存）
        animationCheck.addActionListener(e -> {
            // HOROS-20240407準拠: BrowserController.m 14742行目 - 状態を保存
            java.util.prefs.Preferences.userNodeForPackage(BrowserController.class).putBoolean("AutoPlayAnimation", animationCheck.isSelected());
        });
        rightStatusPanel.add(animationCheck);
        
        // animationSlider（Playチェックボックスのすぐ左隣に配置）
        // HOROS-20240407準拠: MainMenu.xib 4930行目 - rect width="294" height="16"
        // HOROS-20240407準拠: BrowserController.h 142行目 IBOutlet NSSlider *animationSlider;
        animationSlider = new JSlider(JSlider.HORIZONTAL, 0, 0, 0);
        animationSlider.setEnabled(false);
        // Playチェックボックスの左隣に配置（幅294pxのまま）
        animationSlider.setBounds(611 - 60 - 294, 2, 294, 16);
        animationSlider.addChangeListener(e -> {
            if (!animationSlider.getValueIsAdjusting()) {
                previewSliderAction(animationSlider);
            }
        });
        rightStatusPanel.add(animationSlider);
        
        // HOROS-20240407準拠: BrowserController.m 13889行目
        // [NSTimer scheduledTimerWithTimeInterval: 0.15 target:self selector:@selector(previewPerformAnimation:) userInfo:self repeats:YES];
        // Java Swingではjavax.swing.Timerを使用
        animationTimer = new javax.swing.Timer(150, e -> {
            previewPerformAnimation();
        });
        animationTimer.setRepeats(true);
        animationTimer.start();
        
        // HOROS-20240407準拠: BrowserController.m 14838-14839行目
        // else if(c == ' ')
        //     [animationCheck setState: ![animationCheck state]];
        // スペースキーでPlayチェックボックスの状態をトグル
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SPACE, 0),
            "togglePlayAnimation"
        );
        getRootPane().getActionMap().put("togglePlayAnimation", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (animationCheck != null) {
                    animationCheck.setSelected(!animationCheck.isSelected());
                    // 状態を保存
                    java.util.prefs.Preferences.userNodeForPackage(BrowserController.class).putBoolean("AutoPlayAnimation", animationCheck.isSelected());
                }
            }
        });
        
        bottomSplit.add(rightStatusPanel, BorderLayout.EAST);

        // bottomPreviewSplitのみをsplitComparativeの下部に配置（ステータスバーは後で追加）
        // 右側の垂直スプリットの位置を上から約50%の位置に設定（上部50%、下部50%）
        splitComparative.setBottomComponent(bottomPreviewSplit);
        splitComparative.setResizeWeight(0.5); // 上部テーブルエリアが50%、下部プレビューエリアが50%
        
        // ========== メインウィンドウ（水平スプリッター） ==========
        // HOROS-20240407準拠: splitViewHorz - 左サイドバー | 右メインパネル
        // HOROS-20240407準拠: BrowserController.h 111行目 IBOutlet NSSplitView
        // *splitViewHorz;
        // HOROS-20240407準拠: MainMenu.xib 12422行目 - splitView id="12422"
        JSplitPane splitViewHorz = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        // HOROS-20240407準拠: splitAlbumsは左サイドバー全体（Albums、Sources、Activityを含む）
        splitViewHorz.setLeftComponent(splitAlbums);
        splitViewHorz.setRightComponent(splitComparative);
        splitViewHorz.setResizeWeight(0.2); // 左サイドバーが20%
        
        // ステータスバーをウィンドウ全体の最下部に配置
        // HOROS-20240407準拠: MainMenu.xib 4895行目 - splitView id="14352" (_bottomSplit) 水平スプリッター
        // HOROS-20240407準拠: ステータスバーはsplitViewHorzの下に配置（ウィンドウ全体の最下部）
        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.add(splitViewHorz, BorderLayout.CENTER);
        mainContentPanel.add(bottomSplit, BorderLayout.SOUTH);

        contentPane.add(mainContentPanel, BorderLayout.CENTER);
        
        // HOROS-20240407準拠: 検索フィールド
        // HOROS-20240407準拠: BrowserController.h 180行目 IBOutlet NSSearchField
        // *searchField;
        searchField = new JTextField();
        searchField.setToolTipText("検索");
        searchField.addActionListener(e -> {
            // TODO: 検索処理を実装
        });
        
        // HOROS-20240407準拠: ドラッグ&ドロップ対応
        setupDragAndDrop();

        // HOROS-20240407準拠: awakeActivityを呼び出す（BrowserController.m 14432行目）
        // BrowserController+Activity.mm
        // 64-72行目で_activityTableViewのdelegateとdataSourceを設定
        // ただし、JavaではactivityHelperのコンストラクタで既に設定されているため、ここでは不要
        // 念のため、activityTableViewが正しく設定されていることを確認
        if (this.activityTableView != null && activityHelper != null) {
            // activityTableViewは既にactivityHelperをTableModelとして設定済み
            // BrowserActivityHelperがThreadsManagerを監視して自動的に更新される
        }
        
        // アルバムテーブルのデータを更新
        updateAlbumTable();

        // HOROS-20240407準拠: BrowserController.m 14312-14335行目
        // データベースアウトラインの初期状態を設定し、列状態とソート状態を復元
        // HOROS-20240407準拠: [databaseOutline setInitialState] (14312行目)
        // HOROS-20240407準拠: restoreColumnState (14315-14316行目)
        // HOROS-20240407準拠: databaseSortDescriptorの復元 (14318-14333行目)
        // HOROS-20240407準拠: loadSortDescriptors:nil (14335行目) - アルバムごとのソート状態（TODO: 実装）
        // databaseOutlineが完全に初期化された後に復元するため、SwingUtilities.invokeLaterで遅延させる
        SwingUtilities.invokeLater(() -> {
            // HOROS-20240407準拠: BrowserController.m 14315-14316行目
            // カラム状態を復元
            restoreDatabaseColumnState();

            // HOROS-20240407準拠: BrowserController.m 14318-14333行目
            // ソート状態を復元（列状態の復元後に実行）
            restoreSortState();
            
            // HOROS-20240407準拠: BrowserController.m 14337-14338行目
            // 起動時に最初の行（インデックス0）を選択
            // [databaseOutline selectRowIndexes: [NSIndexSet indexSetWithIndex: 0] byExtendingSelection:NO];
            // [databaseOutline scrollRowToVisible: 0];
            // HOROS-20240407準拠: selectRowIndexesを呼び出すと自動的にNSOutlineViewSelectionDidChangeNotificationが送信され、
            // outlineViewSelectionDidChangeが呼び出される
            // ただし、loadingIsOverがtrueになる前に選択処理が実行されると、outlineViewSelectionDidChangeが早期リターンするため、
            // loadingIsOverがtrueになった後に選択処理を実行する必要がある
            // そのため、ここでは選択処理を行わず、loadingIsOverがtrueになった後に実行する
            if (databaseOutline != null && databaseOutline.getRowCount() > 0) {
                javax.swing.tree.TreePath path = databaseOutline.getPathForRow(0);
                if (path != null) {
                    databaseOutline.getTreeSelectionModel().setSelectionPath(path);
                    databaseOutline.scrollPathToVisible(path);
                }
            }
            
            // HOROS-20240407準拠: BrowserController.m 14339行目
            // 列メニューを構築
            buildColumnsMenu();
            
            // HOROS-20240407準拠: 列の表示/非表示状態を復元（COLUMNSDATABASEから）
            refreshColumns();
            
        });
        
        // ディバイダー位置変更を監視（保存用）
        // HOROS-20240407準拠: NSSplitViewのautosaveNameにより自動保存されるが、Java Swingでは手動実装が必要
        // 注意: 復元処理が完了するまで保存を無効化するため、componentShownで設定
        // ディバイダー位置を復元（ウィンドウが表示された後に一度だけ実行）
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                // UI初期化が完了したことをマーク
                uiInitialized = true;
                
                // JViewportの設定を実行（初期化中に失敗した可能性があるため、componentShownイベントで再設定）
                if (thumbnailsScrollView != null) {
                    try {
                        javax.swing.JViewport viewport = thumbnailsScrollView.getViewport();
                        if (viewport != null) {
                            viewport.setBackground(null);
                            // HOROS-20240407準拠: JViewportのサイズ制約を無効化
                            // マトリックスのサイズがスクロールビューのサイズより広くなることを許可
                            // MainMenu.xib準拠: マトリックスの幅（424px）がスクロールビューの幅（384px）より40px広い
                            viewport.setScrollMode(javax.swing.JViewport.BLIT_SCROLL_MODE);
                        }
                    } catch (NullPointerException ex) {
                        // 初期化中にgetViewport()が失敗する可能性があるため、例外をキャッチ
                        // 後で再試行する
                    }
                }
                
                // フレーム変更通知を監視（componentShownイベントが発生した後にリスナーを追加）
                if (thumbnailsScrollView != null) {
                    thumbnailsScrollView.addComponentListener(new java.awt.event.ComponentAdapter() {
                        @Override
                        public void componentResized(java.awt.event.ComponentEvent e) {
                            // 初期化が完了していない場合は処理をスキップ
                            if (!uiInitialized) {
                                return;
                            }
                            if (thumbnailsScrollView == null || !thumbnailsScrollView.isDisplayable()) {
                                return;
                            }
                            if (bottomPreviewSplit == null || !bottomPreviewSplit.isDisplayable()) {
                                return;
                            }
                            previewMatrixScrollViewFrameDidChange();
                        }
                    });
                    
                    // ビューポートの変更を監視して、手動スクロールバーの値を更新
                    javax.swing.JViewport viewport = thumbnailsScrollView.getViewport();
                    if (viewport != null && thumbnailsVerticalScrollBar != null) {
                        viewport.addChangeListener(changeEvent -> {
                            if (thumbnailsVerticalScrollBar != null && !isAdjustingThumbnailsScrollBar) {
                                java.awt.Point viewPosition = viewport.getViewPosition();
                                int currentValue = thumbnailsVerticalScrollBar.getValue();
                                if (currentValue != viewPosition.y) {
                                    isAdjustingThumbnailsScrollBar = true;
                                    try {
                                        thumbnailsVerticalScrollBar.setValue(viewPosition.y);
                                    } finally {
                                        isAdjustingThumbnailsScrollBar = false;
                                    }
                                }
                            }
                        });
                    }
                }
                
                // HOROS-20240407準拠: bottomPreviewSplitのディバイダー位置変更時にpreviewMatrixScrollViewFrameDidChangeを呼び出す
                // HOROS-20240407準拠: ディバイダー位置変更の制約処理を防ぐためのフラグ
                // setDividerLocationを呼び出す際に無限ループを防ぐ
                if (bottomPreviewSplit != null) {
                    bottomPreviewSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
                        // 制約処理中はスキップ（無限ループ防止）
                        if (isAdjustingDivider) {
                            return;
                        }
                        
                        if (!uiInitialized) {
                            return;
                        }
                        
                        if (thumbnailsScrollView == null || !thumbnailsScrollView.isDisplayable()) {
                            return;
                        }
                        
                        // HOROS-20240407準拠: splitView:constrainSplitPosition:ofSubviewAt: (BrowserController.m 10194-10224行目)
                        // ディバイダーの位置をセルサイズに合わせて調整し、セルが隠れないようにする
                        // HOROS-20240407準拠: 常にセル幅に合わせて制約を適用（常にセル幅分しか動かない）
                        if (oMatrix != null) {
                            java.awt.Dimension cellSize = oMatrix.getCellSize();
                            if (cellSize != null && cellSize.width > 0) {
                                java.awt.Dimension intercellSpacingDim = oMatrix.getIntercellSpacing();
                                int intercellSpacing = intercellSpacingDim != null ? intercellSpacingDim.width : 0;
                                int rcs = cellSize.width + intercellSpacing;
                                
                                if (rcs > 0) {
                                    int currentLocation = bottomPreviewSplit.getDividerLocation();
                                    
                                    // HOROS-20240407準拠: hcells = MAX(roundf((proposedPosition+oMatrix.intercellSpacing.width)/rcs), 1) (10217行目)
                                    int hcells = Math.max(Math.round((float) (currentLocation + intercellSpacing) / rcs), 1);
                                    // HOROS-20240407準拠: proposedPosition = rcs*hcells-oMatrix.intercellSpacing.width (10218行目)
                                    int constrainedLocation = rcs * hcells - intercellSpacing;
                                    
                                    // スクロールバーの幅を考慮（別コンポーネントとして固定配置されるため）
                                    int scrollbarWidth = 0;
                                    if (thumbnailsVerticalScrollBar != null) {
                                        scrollbarWidth = thumbnailsVerticalScrollBar.getPreferredSize().width;
                                    }
                                    if (scrollbarWidth == 0) {
                                        Object scrollBarWidthObj = javax.swing.UIManager.get("ScrollBar.width");
                                        if (scrollBarWidthObj instanceof Integer) {
                                            scrollbarWidth = (Integer) scrollBarWidthObj;
                                        } else {
                                            scrollbarWidth = 16; // デフォルト値
                                        }
                                    }
                                    constrainedLocation += scrollbarWidth;
                                    
                                    // HOROS-20240407準拠: constrainMaxCoordinate (BrowserController.m 10657-10658行目)
                                    // 最大位置を制限（プレビュー画面があるので右側に制限）
                                    int maxLocation = bottomPreviewSplit.getWidth() - 200; // HOROS-20240407準拠: width - 200
                                    constrainedLocation = Math.min(constrainedLocation, maxLocation);
                                    
                                    // HOROS-20240407準拠: 常にセル幅に合わせて制約を適用（位置が変更された場合のみ調整）
                                    if (Math.abs(constrainedLocation - currentLocation) > 1) {
                                        isAdjustingDivider = true;
                                        try {
                                            bottomPreviewSplit.setDividerLocation(constrainedLocation);
                                        } finally {
                                            isAdjustingDivider = false;
                                        }
                                        // 位置を調整した後、previewMatrixScrollViewFrameDidChangeを呼び出してセルを並べ替え
                                        // HOROS-20240407準拠: ディバイダー位置変更後、セルの行・列を再計算
                                        SwingUtilities.invokeLater(() -> {
                                            if (uiInitialized) {
                                                previewMatrixScrollViewFrameDidChange();
                                            }
                                        });
                                        return;
                                    }
                                }
                            }
                        }
                        
                        // HOROS-20240407準拠: constrainMaxCoordinate (BrowserController.m 10657-10658行目)
                        // 最大位置を制限（プレビュー画面があるので右側に制限）
                        int currentLocation = bottomPreviewSplit.getDividerLocation();
                        int maxLocation = bottomPreviewSplit.getWidth() - 200; // HOROS-20240407準拠: width - 200
                        if (currentLocation > maxLocation) {
                            isAdjustingDivider = true;
                            try {
                                bottomPreviewSplit.setDividerLocation(maxLocation);
                            } finally {
                                isAdjustingDivider = false;
                            }
                        }
                        
                        // 位置を調整しなかった場合、通常通りpreviewMatrixScrollViewFrameDidChangeを呼び出してセルを並べ替え
                        // HOROS-20240407準拠: ディバイダー位置変更後、セルの行・列を再計算
                        if (uiInitialized) {
                            previewMatrixScrollViewFrameDidChange();
                        }
                    });
                }
                
                // HOROS-20240407準拠: BrowserController.m 14233行目
                // [self previewMatrixScrollViewFrameDidChange:nil];
                // コンポーネントが表示された後に一度だけ呼び出す
                SwingUtilities.invokeLater(() -> {
                    if (uiInitialized) {
                        previewMatrixScrollViewFrameDidChange();
                    }
                });
                
                if (!dividerLocationsRestored) {
                    // コンポーネントのサイズが確定してから復元するため、SwingUtilities.invokeLaterで遅延
                    SwingUtilities.invokeLater(() -> {
                        SwingUtilities.invokeLater(() -> {
                            restoreDividerLocations();
                            dividerLocationsRestored = true;
                            
                            // 復元が完了した後に、ディバイダー位置変更を監視して保存
                            if (splitAlbums != null) {
                                splitAlbums.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
                                    saveDividerLocations();
                                });
                            }
                            if (splitSourcesActivity != null) {
                                splitSourcesActivity.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
                                    saveDividerLocations();
                                });
                            }
                            if (splitComparative != null) {
                                splitComparative.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
                                    saveDividerLocations();
                                });
                            }
                            
                            // HOROS-20240407準拠: BrowserController.m 14428行目
                            // loadingIsOver = YES;
                            // すべての初期化と復元が完了した後に設定
                            loadingIsOver = true;
                            System.out.println("[DEBUG] loadingIsOver set to true");
                            
                            // HOROS-20240407準拠: BrowserController.m 14430行目
                            // [self outlineViewRefresh];
                            // outlineViewRefreshが呼び出されると、outlineViewSelectionDidChangeが呼び出される
                            // ただし、起動時に最初の行を選択する処理は、loadingIsOverがtrueになった後に実行する必要がある
                            // HOROS-20240407準拠: BrowserController.m 14337-14338行目
                            // [databaseOutline selectRowIndexes: [NSIndexSet indexSetWithIndex: 0] byExtendingSelection:NO];
                            // [databaseOutline scrollRowToVisible: 0];
                            // HOROS-20240407準拠: selectRowIndexesを呼び出すと自動的にNSOutlineViewSelectionDidChangeNotificationが送信され、
                            // outlineViewSelectionDidChangeが呼び出される
                            SwingUtilities.invokeLater(() -> {
                                if (databaseOutline != null && databaseOutline.getRowCount() > 0) {
                                    javax.swing.tree.TreePath path = databaseOutline.getPathForRow(0);
                                    if (path != null) {
                                        // HOROS-20240407準拠: setSelectionPathを呼び出すと、TreeSelectionListenerのvalueChangedが呼び出され、
                                        // databasePressedが呼び出され、それがoutlineViewSelectionDidChangeを呼び出す
                                        databaseOutline.getTreeSelectionModel().setSelectionPath(path);
                                        databaseOutline.scrollPathToVisible(path);
                                        // HOROS-20240407準拠: この時点でloadingIsOverはtrueなので、outlineViewSelectionDidChangeが正常に実行される
                                        // databasePressedが自動的にoutlineViewSelectionDidChangeを呼び出すため、ここでは明示的に呼び出す必要はない
                                    }
                                }
                            });
                        });
                    });
                }
            }
        });

        // HOROS-20240407準拠: BrowserController.m 14682行目
        // ウィンドウが閉じられる時に位置・サイズを保存
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveWindowFrame();
                saveDatabaseColumnState();
                saveSortState();
                saveDividerLocations();
            }
        });
    }

    /**
     * ウィンドウ位置・サイズを復元
     * HOROS-20240407準拠: BrowserController.m 14183-14188行目
     * r = NSRectFromString( [[NSUserDefaults standardUserDefaults]
     * stringForKey: @"DBWindowFrame"]);
     * if( NSIsEmptyRect( r)) // No position for the window -> fullscreen
     * [[self window] zoom: self];
     * else
     * [self.window setFrame: r display: YES];
     */
    private void restoreWindowFrame() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(BrowserController.class);
            String frameStr = prefs.get("DBWindowFrame", null);

            if (frameStr != null && !frameStr.isEmpty()) {
                // 形式: "x,y,width,height"
                String[] parts = frameStr.split(",");
                if (parts.length == 4) {
                    try {
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int width = Integer.parseInt(parts[2]);
                        int height = Integer.parseInt(parts[3]);

                        // 画面内に収まるかチェック
                        java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
                        java.awt.GraphicsDevice[] screens = ge.getScreenDevices();
                        boolean isValidPosition = false;

                        for (java.awt.GraphicsDevice screen : screens) {
                            java.awt.Rectangle screenBounds = screen.getDefaultConfiguration().getBounds();
                            if (x >= screenBounds.x && y >= screenBounds.y &&
                                    x + width <= screenBounds.x + screenBounds.width &&
                                    y + height <= screenBounds.y + screenBounds.height) {
                                isValidPosition = true;
                                break;
                            }
                        }

                        if (isValidPosition) {
                            setBounds(x, y, width, height);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        // 無効な形式の場合はデフォルトサイズを使用
                    }
                }
            }
        } catch (Exception e) {
            // エラーが発生した場合はデフォルトサイズを使用
        }

        // デフォルトサイズ（画面中央に配置）
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        // HOROS-20240407準拠: BrowserController.m 14233行目
        // [self previewMatrixScrollViewFrameDidChange:nil];
        // UI初期化が完了した後に一度だけ呼び出す
        // ただし、コンポーネントが完全に初期化されるまで待つため、componentShownイベントで呼び出す
        // uiInitializedフラグは、componentShownイベントで設定する
    }

    /**
     * ウィンドウ位置・サイズを保存
     * HOROS-20240407準拠: BrowserController.m 14682行目
     * [[NSUserDefaults standardUserDefaults] setObject: NSStringFromRect(
     * self.window.frame) forKey: @"DBWindowFrame"];
     */
    private void saveWindowFrame() {
        try {
            java.awt.Rectangle bounds = getBounds();
            // 形式: "x,y,width,height"
            String frameStr = bounds.x + "," + bounds.y + "," + bounds.width + "," + bounds.height;

            Preferences prefs = Preferences.userNodeForPackage(BrowserController.class);
            prefs.put("DBWindowFrame", frameStr);
            // HOROS-20240407準拠: BrowserController.m 14742行目
            // [[NSUserDefaults standardUserDefaults] setBool: [animationCheck state] forKey: @"AutoPlayAnimation"];
            if (animationCheck != null) {
                prefs.putBoolean("AutoPlayAnimation", animationCheck.isSelected());
            }
            prefs.flush();
        } catch (Exception e) {
            // エラーが発生した場合は保存をスキップ
        }
    }

    /**
     * ディバイダー位置を保存
     * HOROS-20240407準拠: MainMenu.xib 4078行目 - splitView autosaveName="albumsAndSources"
     * NSSplitViewのautosaveNameにより自動保存されるが、Java Swingでは手動実装が必要
     * （プラットフォーム差を埋めるための最小限のカスタムロジック）
     * 
     * 注意: 比率で保存することで、ウィンドウサイズが変わっても正しく復元される
     */
    private void saveDividerLocations() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(BrowserController.class);
            
            // splitAlbumsとsplitSourcesActivityのディバイダー位置を保存（比率で保存）
            if (splitAlbums != null && splitAlbums.getHeight() > 0) {
                int albumsDividerLocation = splitAlbums.getDividerLocation();
                double albumsRatio = (double) albumsDividerLocation / splitAlbums.getHeight();
                prefs.putDouble("AlbumsDividerRatio", albumsRatio);
            }
            
            if (splitSourcesActivity != null && splitSourcesActivity.getHeight() > 0) {
                int sourcesActivityDividerLocation = splitSourcesActivity.getDividerLocation();
                double sourcesActivityRatio = (double) sourcesActivityDividerLocation / splitSourcesActivity.getHeight();
                prefs.putDouble("SourcesActivityDividerRatio", sourcesActivityRatio);
            }
            
            // splitComparativeのディバイダー位置も保存（比率で保存）
            if (splitComparative != null && splitComparative.getHeight() > 0) {
                int comparativeDividerLocation = splitComparative.getDividerLocation();
                double comparativeRatio = (double) comparativeDividerLocation / splitComparative.getHeight();
                prefs.putDouble("ComparativeDividerRatio", comparativeRatio);
            }
            
            // bottomPreviewSplitのディバイダー位置も保存（比率で保存）
            // HOROS-20240407準拠: NSSplitViewのautosaveNameにより自動保存されるが、Java Swingでは手動実装が必要
            if (bottomPreviewSplit != null && bottomPreviewSplit.getWidth() > 0) {
                int bottomPreviewDividerLocation = bottomPreviewSplit.getDividerLocation();
                double bottomPreviewRatio = (double) bottomPreviewDividerLocation / bottomPreviewSplit.getWidth();
                prefs.putDouble("BottomPreviewDividerRatio", bottomPreviewRatio);
            }
            
            prefs.flush();
        } catch (Exception e) {
            // エラーが発生した場合は保存をスキップ
        }
    }

    /**
     * ディバイダー位置を復元
     * HOROS-20240407準拠: MainMenu.xib 4078行目 - splitView autosaveName="albumsAndSources"
     * NSSplitViewのautosaveNameにより自動復元されるが、Java Swingでは手動実装が必要
     * （プラットフォーム差を埋めるための最小限のカスタムロジック）
     * 
     * 注意: 保存された比率を使用して復元することで、ウィンドウサイズが変わっても正しく復元される
     */
    private void restoreDividerLocations() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(BrowserController.class);
            
            // 復元中はPropertyChangeListenerを無効化（無限ループ防止）
            isAdjustingDivider = true;
            try {
                // splitAlbumsとsplitSourcesActivityのディバイダー位置を復元（比率で復元）
                if (splitAlbums != null && splitAlbums.getHeight() > 0) {
                double savedRatio = prefs.getDouble("AlbumsDividerRatio", -1.0);
                if (savedRatio > 0.0 && savedRatio < 1.0) {
                    // 保存された比率で復元
                    splitAlbums.setDividerLocation(savedRatio);
                } else {
                    // デフォルト位置: Albumsが198px（比率で計算）
                    // ただし、高さが確定していない場合は後で再設定
                    double albumsRatio = 198.0 / 687.0;
                    splitAlbums.setDividerLocation(albumsRatio);
                }
            }
            
            if (splitSourcesActivity != null && splitSourcesActivity.getHeight() > 0) {
                double savedRatio = prefs.getDouble("SourcesActivityDividerRatio", -1.0);
                if (savedRatio > 0.0 && savedRatio < 1.0) {
                    // 保存された比率で復元
                    splitSourcesActivity.setDividerLocation(savedRatio);
                } else {
                    // デフォルト位置: Sourcesが1/3、Activityが2/3
                    splitSourcesActivity.setDividerLocation(0.33);
                }
            }
            
            // splitComparativeのディバイダー位置も復元（比率で復元）
            if (splitComparative != null && splitComparative.getHeight() > 0) {
                double savedRatio = prefs.getDouble("ComparativeDividerRatio", -1.0);
                if (savedRatio > 0.0 && savedRatio < 1.0) {
                    // 保存された比率で復元
                    splitComparative.setDividerLocation(savedRatio);
                } else {
                    // デフォルト位置: 上から50%
                    splitComparative.setDividerLocation(0.5);
                }
            }
            
                // bottomPreviewSplitのディバイダー位置を復元（比率で復元）
                // HOROS-20240407準拠: NSSplitViewのautosaveNameにより自動復元されるが、Java Swingでは手動実装が必要
                if (bottomPreviewSplit != null && bottomPreviewSplit.getWidth() > 0) {
                    double savedRatio = prefs.getDouble("BottomPreviewDividerRatio", -1.0);
                    if (savedRatio > 0.0 && savedRatio < 1.0) {
                        // 保存された比率で復元
                        bottomPreviewSplit.setDividerLocation(savedRatio);
                    } else {
                        // デフォルト位置: 左から30%（サムネイルが30%）
                        bottomPreviewSplit.setDividerLocation(0.3);
                    }
                }
            } finally {
                // 復元完了後、PropertyChangeListenerを再有効化
                isAdjustingDivider = false;
            }
        } catch (Exception e) {
            // エラーが発生した場合は復元をスキップ
            isAdjustingDivider = false;
        }
    }
    
    /**
     * ドラッグ&ドロップ機能を設定
     * HOROS-20240407準拠:
     * MyOutlineViewのdraggingEntered/draggingUpdated/performDragOperation
     */
    private void setupDragAndDrop() {
        // ウィンドウ全体にドロップターゲットを設定
        new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();
                    
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        
                        // ファイルパスのリストを作成
                        List<String> filePaths = new ArrayList<>();
                        for (File file : files) {
                            if (file.isFile()) {
                                filePaths.add(file.getAbsolutePath());
                            } else if (file.isDirectory()) {
                                // ディレクトリの場合は再帰的にファイルを収集
                                collectDicomFiles(file, filePaths);
                            }
                        }
                        
                        if (!filePaths.isEmpty()) {
                            // HOROS-20240407準拠: BrowserController.m 1472行目
                            // [self.database addFilesAtPaths:localFiles]
                            importFiles(filePaths);
                        }
                        
                        dtde.dropComplete(true);
                    } else {
                        dtde.rejectDrop();
                    }
                } catch (UnsupportedFlavorException | IOException e) {
                    dtde.rejectDrop();
                }
            }
        });
    }
    
    /**
     * ディレクトリからDICOMファイルを再帰的に収集
     */
    private void collectDicomFiles(File directory, List<String> filePaths) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    // DICOMファイルの拡張子をチェック（.dcm, .dicom, 拡張子なしなど）
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".dcm") || name.endsWith(".dicom") || 
                        name.endsWith(".ima") || name.endsWith(".img") ||
                        !name.contains(".")) {
                        filePaths.add(file.getAbsolutePath());
                    }
                } else if (file.isDirectory()) {
                    collectDicomFiles(file, filePaths);
                }
            }
        }
    }
    
    /**
     * ファイルとフォルダをデータベースに追加
     * HOROS-20240407準拠: - (void) addFilesAndFolderToDatabase:(NSArray*) filenames
     * (864行目)
     * 
     * このメソッドはファイルを収集してから、copyFilesIntoDatabaseIfNeededを呼び出す
     * HOROS-20240407準拠: BrowserController.m 864-998行目
     */
    public void addFilesAndFolderToDatabase(List<String> filenames) {
        if (database == null || filenames == null || filenames.isEmpty()) {
            return;
        }
        
        // HOROS-20240407準拠: BrowserController.m 870行目
        // NSMutableArray *filesArray = [[[NSMutableArray alloc] initWithCapacity:0]
        // autorelease];
        List<String> filesArray = new ArrayList<>();

        // HOROS-20240407準拠: BrowserController.m 872-995行目
        for (String filename : filenames) {
            try {
                java.io.File file = new java.io.File(filename);
                if (!file.exists()) {
                    continue;
                }

                // HOROS-20240407準拠: BrowserController.m 878行目
                // if( [[filename lastPathComponent] characterAtIndex: 0] != '.')
                String lastComponent = file.getName();
                if (lastComponent.isEmpty() || lastComponent.charAt(0) == '.') {
                    continue;
                }

                if (file.isDirectory()) {
                    // HOROS-20240407準拠: BrowserController.m 880-950行目
                    // ディレクトリの場合は再帰的にファイルを列挙
                    // ただし、pagesやapp拡張子のディレクトリはスキップ
                    String extension = getPathExtension(filename);
                    if ("pages".equals(extension) || "app".equals(extension)) {
                        continue;
                    }

                    // HOROS-20240407準拠: BrowserController.m 886行目
                    // NSDirectoryEnumerator *enumer = [[NSFileManager defaultManager]
                    // enumeratorAtPath: filename];
                    String folderSkip = null;
                    try {
                        java.nio.file.Files.walk(java.nio.file.Paths.get(filename))
                                .filter(p -> {
                                    try {
                                        return java.nio.file.Files.isRegularFile(p);
                                    } catch (Exception e) {
                                        return false;
                                    }
                                })
                                .forEach(p -> {
                                    try {
                                        String itemPath = p.toString();
                                        String pathname = java.nio.file.Paths.get(filename).relativize(p).toString();

                                        // HOROS-20240407準拠: BrowserController.m 901-903行目
                                        // folderSkipの処理（簡略化）

                                        // HOROS-20240407準拠: BrowserController.m 909行目
                                        String itemLastComponent = p.getFileName().toString();
                                        if (itemLastComponent.isEmpty() || itemLastComponent.charAt(0) == '.') {
                                            return;
                                        }

                                        // HOROS-20240407準拠: BrowserController.m 911-935行目
                                        // 特殊なファイルタイプの処理
                                        String itemExtension = getPathExtension(itemPath);
                                        if ("dcmURLs".equals(itemExtension)) {
                                            // TODO: asyncWADODownloadの実装
                                            // HOROS-20240407準拠: BrowserController.m 913-917行目
                                        } else if ("zip".equals(itemExtension) || "osirixzip".equals(itemExtension)) {
                                            // TODO: ZIP解凍の実装
                                            // HOROS-20240407準拠: BrowserController.m 919-930行目
                                        } else if ("DICOMDIR".equalsIgnoreCase(itemLastComponent)
                                                || "DICOMDIR.".equalsIgnoreCase(itemLastComponent)) {
                                            // TODO: addDICOMDIRの実装
                                            // HOROS-20240407準拠: BrowserController.m 932-933行目
                                        } else {
                                            // HOROS-20240407準拠: BrowserController.m 935行目
                                            filesArray.add(itemPath);
                                        }
                                    } catch (Exception e) {
                                        // HOROS-20240407準拠: BrowserController.m 944-947行目
                                        // エラーが発生したファイルはスキップ
                                    }
            });
        } catch (Exception e) {
                        // エラーが発生したディレクトリはスキップ
                    }
                } else {
                    // HOROS-20240407準拠: BrowserController.m 952-984行目
                    // ファイルの場合
                    String extension = getPathExtension(filename);
                    if ("xml".equals(extension)) {
                        // TODO: asyncWADOXMLDownloadURLの実装
                        // HOROS-20240407準拠: BrowserController.m 954-956行目
                    } else if ("dcmURLs".equals(extension)) {
                        // TODO: asyncWADODownloadの実装
                        // HOROS-20240407準拠: BrowserController.m 958-964行目
                    } else if ("zip".equals(extension) || "osirixzip".equals(extension)) {
                        // TODO: ZIP解凍の実装
                        // HOROS-20240407準拠: BrowserController.m 966-977行目
                    } else if ("DICOMDIR".equalsIgnoreCase(lastComponent)
                            || "DICOMDIR.".equalsIgnoreCase(lastComponent)) {
                        // TODO: addDICOMDIRの実装
                        // HOROS-20240407準拠: BrowserController.m 979-980行目
                    } else if ("app".equals(extension)) {
                        // HOROS-20240407準拠: BrowserController.m 981-983行目
                        // appファイルはスキップ
                    } else {
                        // HOROS-20240407準拠: BrowserController.m 984行目
                        filesArray.add(filename);
                    }
                }
            } catch (Exception e) {
                // HOROS-20240407準拠: BrowserController.m 989-992行目
                // エラーが発生したファイルはスキップ
            }
        }

        // HOROS-20240407準拠: BrowserController.m 997行目
        // [self copyFilesIntoDatabaseIfNeeded: filesArray options: [NSDictionary
        // dictionaryWithObjectsAndKeys: ...]]
        if (!filesArray.isEmpty()) {
            Map<String, Object> options = new HashMap<>();
            // HOROS-20240407準拠: BrowserController.m 997行目
            // [[NSUserDefaults standardUserDefaults] objectForKey: @"onlyDICOM"]
            // TODO: UserDefaultsから取得
            // options.put("onlyDICOM", ...);
            options.put("async", true); // HOROS-20240407準拠: [NSNumber numberWithBool: YES], @"async"
            options.put("addToAlbum", true); // HOROS-20240407準拠: [NSNumber numberWithBool: YES], @"addToAlbum"
            options.put("selectStudy", true); // HOROS-20240407準拠: [NSNumber numberWithBool: YES], @"selectStudy"
            copyFilesIntoDatabaseIfNeeded(filesArray, options);
        }
    }

    /**
     * パスの拡張子を取得
     * HOROS-20240407準拠: [NSString pathExtension]
     */
    private String getPathExtension(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        int lastDot = path.lastIndexOf('.');
        int lastSep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (lastDot > lastSep && lastDot < path.length() - 1) {
            return path.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * ファイルをインポート
     * HOROS-20240407準拠: addFilesAndFolderToDatabaseを呼び出す
     * 
     * 注意: このメソッドは後方互換性のために残されている
     * 新しいコードではaddFilesAndFolderToDatabaseを使用すること
     */
    public void importFiles(List<String> filePaths) {
        addFilesAndFolderToDatabase(filePaths);
    }
    
    /**
     * ファイル選択ダイアログからインポート
     * HOROS-20240407準拠: メニューからのインポート機能
     */
    public void importFilesFromDialog() {
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setFileSelectionMode(javax.swing.JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setDialogTitle("DICOMファイルをインポート");
        
        int result = fileChooser.showOpenDialog(this);
        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            List<String> filePaths = new ArrayList<>();
            
            for (File file : selectedFiles) {
                if (file.isFile()) {
                    filePaths.add(file.getAbsolutePath());
                } else if (file.isDirectory()) {
                    collectDicomFiles(file, filePaths);
                }
            }
            
            if (!filePaths.isEmpty()) {
                importFiles(filePaths);
            }
        }
        
        // HOROS-20240407準拠: BrowserController.m 14316行目
        // カラム状態を復元
        restoreDatabaseColumnState();

        // HOROS-20240407準拠: ソート状態を復元
        // HOROS-20240407準拠: restoreColumnStateの後にrestoreSortStateを呼び出す（14315-14333行目）
        // 列の状態が復元された後にソート状態を復元するため、SwingUtilities.invokeLaterで遅延させる
        SwingUtilities.invokeLater(() -> {
            restoreSortState();
        });

        // HOROS-20240407準拠: 列ヘッダーのクリック処理はNSOutlineViewが自動的に処理
        // Java Swingでは、JXTreeTableが自動的にヘッダークリックを処理するため、
        // カスタムのMouseListenerは不要（HOROS-20240407準拠）
        
        // HOROS-20240407準拠: BrowserController.m 14738行目
        // ウィンドウが閉じられる時にカラム状態とソート状態を保存
        // 注意: 既にaddWindowListenerが呼ばれている場合は、重複を避ける
        // HOROS-20240407準拠: windowWillClose:で保存（14733-14738行目）
    }
    
    /**
     * データベースアウトラインのカラム状態を復元
     * HOROS-20240407準拠: BrowserController.m 14316行目
     * [databaseOutline restoreColumnState: [[NSUserDefaults standardUserDefaults]
     * objectForKey: @"databaseColumns2"]]
     */
    private void restoreDatabaseColumnState() {
        if (databaseOutline == null) {
            return;
        }
        
        try {
            Preferences prefs = Preferences.userNodeForPackage(BrowserController.class);
            String columnStateJson = prefs.get("databaseColumns2", null);
            
            if (columnStateJson != null && !columnStateJson.isEmpty()) {
                // JSON文字列をパースしてList<Map<String, Object>>に変換
                // 簡易実装: JSONライブラリを使用せず、カンマ区切りで保存
                // より堅牢な実装にはJSONライブラリ（Jackson、Gsonなど）を使用することを推奨
                List<Map<String, Object>> state = parseColumnState(columnStateJson);
                if (state != null && !state.isEmpty()) {
                    databaseOutline.restoreColumnState(state);
                }
            }
        } catch (Exception e) {
            // エラーが発生した場合はデフォルトのカラム幅を使用
            // logger.error("Failed to restore column state", e);
        }
    }
    
    /**
     * データベースアウトラインのカラム状態を保存
     * HOROS-20240407準拠: BrowserController.m 14738行目
     * [[NSUserDefaults standardUserDefaults] setObject:[databaseOutline
     * columnState] forKey: @"databaseColumns2"]
     */
    private void saveDatabaseColumnState() {
        if (databaseOutline == null) {
            return;
        }
        
        try {
            List<Map<String, Object>> state = databaseOutline.getColumnState();
            if (state != null && !state.isEmpty()) {
                // List<Map<String, Object>>をJSON文字列に変換
                // 簡易実装: JSONライブラリを使用せず、カンマ区切りで保存
                // より堅牢な実装にはJSONライブラリ（Jackson、Gsonなど）を使用することを推奨
                String columnStateJson = serializeColumnState(state);
                Preferences prefs = Preferences.userNodeForPackage(BrowserController.class);
                prefs.put("databaseColumns2", columnStateJson);
                prefs.flush();
            }
        } catch (Exception e) {
            // エラーが発生した場合は保存をスキップ
            // logger.error("Failed to save column state", e);
        }
    }
    
    /**
     * カラム状態をシリアライズ（簡易実装）
     * 形式: "Identifier1:Width1:Index1,Identifier2:Width2:Index2,..."
     * HOROS-20240407準拠: 列の位置（Index）も保存する必要がある
     */
    private String serializeColumnState(List<Map<String, Object>> state) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < state.size(); i++) {
            Map<String, Object> columnInfo = state.get(i);
            String identifier = (String) columnInfo.get("Identifier");
            Object widthObj = columnInfo.get("Width");
            Object indexObj = columnInfo.get("Index");
            int width = widthObj instanceof Number ? ((Number) widthObj).intValue() : 0;
            int index = indexObj instanceof Number ? ((Number) indexObj).intValue() : -1;
            
            if (identifier != null) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(identifier).append(":").append(width).append(":").append(index);
            }
        }
        return sb.toString();
    }
    
    /**
     * カラム状態をパース（簡易実装）
     * 形式: "Identifier1:Width1:Index1,Identifier2:Width2:Index2,..."
     * HOROS-20240407準拠: 列の位置（Index）も復元する必要がある
     * 後方互換性: 古い形式（"Identifier1:Width1"）もサポート
     */
    private List<Map<String, Object>> parseColumnState(String json) {
        List<Map<String, Object>> state = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return state;
        }
        
        String[] parts = json.split(",");
        for (String part : parts) {
            String[] keyValue = part.split(":");
            if (keyValue.length >= 2) {
                try {
                    Map<String, Object> columnInfo = new HashMap<>();
                    columnInfo.put("Identifier", keyValue[0]);
                    columnInfo.put("Width", Integer.parseInt(keyValue[1]));
                    // Indexが存在する場合は復元、存在しない場合は-1（後方互換性）
                    if (keyValue.length >= 3) {
                        columnInfo.put("Index", Integer.parseInt(keyValue[2]));
                    } else {
                        columnInfo.put("Index", -1); // 古い形式の場合は-1
                    }
                    state.add(columnInfo);
                } catch (NumberFormatException e) {
                    // 無効な数値はスキップ
                }
            }
        }
        return state;
    }
    
    /**
     * アルバムテーブルのデータを更新
     */
    private void updateAlbumTable() {
        if (albumTable == null || database == null) {
            System.out.println("[DEBUG] updateAlbumTable() - albumTable or database is null");
            return;
        }
        
        // 無限ループ防止
        if (isUpdatingAlbumTable)
            return;
        isUpdatingAlbumTable = true;
        
        try {
            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) albumTable.getModel();
            model.setRowCount(0);
            
            List<com.jj.dicomviewer.model.DicomAlbum> albums = getAlbumArray();
            System.out.println("[DEBUG] updateAlbumTable() - albums.size(): " + albums.size());
            for (com.jj.dicomviewer.model.DicomAlbum album : albums) {
                Object[] row = {
                    album.getName() != null ? album.getName() : "",
                    album.getNumberOfStudies()
                };
                model.addRow(row);
                System.out.println("[DEBUG] updateAlbumTable() - added album: " + album.getName() + " (" + album.getNumberOfStudies() + " studies)");
            }
            
            // HOROS-20240407準拠: 最初のアルバム（"Database"）を自動選択
            if (!albums.isEmpty()) {
                // フラグにより選択変更イベントが発火してもrefreshDatabaseがスキップされる
                albumTable.setRowSelectionInterval(0, 0);
                System.out.println("[DEBUG] updateAlbumTable() - selected first album (row 0)");
                
                // HOROS-20240407準拠: 選択されたアルバムに基づいてアウトラインビューを更新
                // ただし、無限ループを防ぐため直接呼び出す
                if (!isRefreshingOutline) {
                    System.out.println("[DEBUG] updateAlbumTable() - calling outlineViewRefresh()");
                    SwingUtilities.invokeLater(() -> {
                        outlineViewRefresh();
                    });
                } else {
                    System.out.println("[DEBUG] updateAlbumTable() - skipping outlineViewRefresh() (already refreshing)");
                }
            } else {
                System.out.println("[DEBUG] updateAlbumTable() - no albums found");
            }
        } finally {
            isUpdatingAlbumTable = false;
        }
    }
    
    /**
     * HOROS-20240407準拠: + (BrowserController*) currentBrowser (521行目)
     */
    public static BrowserController getCurrentBrowser() {
        return browserWindow;
    }
    
    /**
     * データベースを取得
     * HOROS-20240407準拠: - (DicomDatabase*)database (BrowserController.h)
     */
    public DicomDatabase getDatabase() {
        return database;
    }
    
    /**
     * ファイルをデータベースにコピー
     * HOROS-20240407準拠: - (void) copyFilesIntoDatabaseIfNeeded: (NSMutableArray*)
     * filesInput options: (NSDictionary*) options (2191行目)
     */
    public void copyFilesIntoDatabaseIfNeeded(List<String> filesInput, Map<String, Object> options) {
        if (database == null || !database.isLocal()) {
            return;
        }
        if (filesInput == null || filesInput.isEmpty()) {
            return;
        }
        
        // HOROS-20240407準拠: BOOL COPYDATABASE = [[NSUserDefaults standardUserDefaults]
        // boolForKey: @"COPYDATABASE"]; (2196行目)
        boolean copyDatabase = false; // TODO: UserDefaultsから取得
        
        // HOROS-20240407準拠: NSMutableArray *newFilesToCopyList = [NSMutableArray
        // arrayWithCapacity: [filesInput count]]; (2208行目)
        List<String> newFilesToCopyList = new ArrayList<>();
        String inPath = database.getDataDirPath();
        
        // HOROS-20240407準拠: for( NSString *file in filesInput) (2212行目)
        for (String file : filesInput) {
            // HOROS-20240407準拠: if( [[file commonPrefixWithString: INpath options:
            // NSLiteralSearch] isEqualToString:INpath] == NO) (2214行目)
            if (!file.startsWith(inPath)) {
                newFilesToCopyList.add(file);
            }
        }
        
        boolean copyFiles = false;
        if (copyDatabase && !newFilesToCopyList.isEmpty()) {
            copyFiles = true;
        }
        
        // HOROS-20240407準拠: if( [[options objectForKey: @"async"] boolValue]) (2282行目)
        boolean async = options != null && Boolean.TRUE.equals(options.get("async"));
        
        if (async) {
            // HOROS-20240407準拠: NSMutableDictionary *dict = [NSMutableDictionary
            // dictionaryWithObjectsAndKeys: filesInput, @"filesInput", ...]; (2284行目)
            Map<String, Object> dict = new HashMap<>();
            if (options != null) {
                dict.putAll(options);
            }
            dict.put("filesInput", filesInput); // HOROS-20240407準拠: filesInputをそのまま使用（newFilesToCopyListではなく）
            dict.put("copyFiles", copyFiles);
            
            // HOROS-20240407準拠: NSThread *t = [[[NSThread alloc]
            // initWithTarget:_database.independentDatabase
            // selector:@selector(copyFilesThread:) object: dict] autorelease]; (2289行目)
            // HOROS-20240407準拠: BrowserController.m 2288-2291行目
            // メインスレッドでない場合はindependentDatabaseを使用、メインスレッドの場合はdatabaseを使用
            Thread thread = new Thread(() -> {
                database.copyFilesThread(dict);
            });
            
            // HOROS-20240407準拠: t.name = NSLocalizedString( @"Copying and indexing
            // files...", nil); (2294行目)
            if (copyFiles) {
                if (options != null && Boolean.TRUE.equals(options.get("mountedVolume"))) {
                    thread.setName("Copying and indexing files from CD/DVD...");
                } else {
                    thread.setName("Copying and indexing files...");
                }
            } else {
                if (options != null && Boolean.TRUE.equals(options.get("mountedVolume"))) {
                    thread.setName("Indexing files from CD/DVD...");
                } else {
            thread.setName("Indexing files...");
                }
            }

            // HOROS-20240407準拠: t.status = N2LocalizedSingularPluralCount( [filesInput
            // count], NSLocalizedString(@"file", nil), NSLocalizedString(@"files", nil));
            // (2295行目)
            // HOROS-20240407準拠: t.supportsCancel = YES; (2296行目)
            // HOROS-20240407準拠: [[ThreadsManager defaultManager] addThreadAndStart: t];
            // (2297行目)
            ThreadsManager.defaultManager().addThreadAndStart(thread);
        } else {
            // 同期処理（TODO: 実装）
            // HOROS-20240407準拠: Wait *splash = [[Wait alloc] initWithString:
            // NSLocalizedString(@"Copying into Database...", nil)]; (2301行目)
            // 同期処理の場合は、Waitスプラッシュを表示してから処理を実行
            // 現在は非同期処理のみをサポート
            // 同期処理が必要な場合は、async=falseを設定して呼び出す
        }
    }

    // HOROS-20240407準拠: NSArray *outlineViewArray, *originalOutlineViewArray;
    // (124行目)
    private List<Object> outlineViewArray = new ArrayList<>();
    
    /**
     * アウトラインビューを更新
     * HOROS-20240407準拠: - (void)outlineViewRefresh (BrowserController.m)
     */
    /**
     * アウトラインビューを更新
     * HOROS-20240407準拠: - (void)outlineViewRefresh (BrowserController.m)
     */
    public void outlineViewRefresh() {
        // 無限ループ防止
        if (isRefreshingOutline)
            return;
        isRefreshingOutline = true;
        
        try {
            if (database == null || albumTable == null) {
                System.out.println("[DEBUG] outlineViewRefresh() - database or albumTable is null");
                return;
            }
            
            // HOROS-20240407準拠: 選択されたアルバムからスタディを取得
            List<com.jj.dicomviewer.model.DicomAlbum> albumArray = getAlbumArray();
            int selectedRow = albumTable.getSelectedRow();
            System.out.println("[DEBUG] outlineViewRefresh() - selectedRow: " + selectedRow + ", albumArray.size(): " + albumArray.size());
            
            synchronized (outlineViewArray) {
                outlineViewArray.clear();
                
                if (selectedRow >= 0 && selectedRow < albumArray.size()) {
                    com.jj.dicomviewer.model.DicomAlbum selectedAlbum = albumArray.get(selectedRow);
                    
                    if (selectedAlbum != null) {
                        // HOROS-20240407準拠: "Database"アルバムの場合は全スタディを表示
                        if ("Database".equals(selectedAlbum.getName())) {
                            // HOROS-20240407準拠: Databaseアルバムは全スタディを含む
                            try {
                                List<com.jj.dicomviewer.model.DicomStudy> allStudies = database.getAllStudies();
                                System.out.println("[DEBUG] outlineViewRefresh() - database.getAllStudies() returned " + (allStudies != null ? allStudies.size() : 0) + " studies");
                                if (allStudies != null && !allStudies.isEmpty()) {
                                    outlineViewArray.addAll(allStudies);
                                    System.out.println("[DEBUG] outlineViewRefresh() - added " + outlineViewArray.size() + " studies to outlineViewArray");
                                } else {
                                    System.out.println("[DEBUG] outlineViewRefresh() - no studies in database");
                                }
                            } catch (Exception e) {
                                // HOROS-20240407準拠: @catch (NSException * e) { N2LogExceptionWithStackTrace(e);
                                // }
                                System.out.println("[DEBUG] outlineViewRefresh() - exception getting studies: " + e.getMessage());
                                e.printStackTrace();
                            }
                        } else if (selectedAlbum.getStudies() != null) {
                            // HOROS-20240407準拠: 通常のアルバムの場合はアルバムのスタディを表示
                            outlineViewArray.addAll(selectedAlbum.getStudies());
                            System.out.println("[DEBUG] outlineViewRefresh() - added " + selectedAlbum.getStudies().size() + " studies from album");
                        }
                    }
                } else {
                    System.out.println("[DEBUG] outlineViewRefresh() - no album selected or invalid selectedRow");
                }
            }

            // HOROS-20240407準拠: ソート処理 (BrowserController.m 3193-3265行目)
            // HOROS-20240407準拠: NSSortDescriptor * sortdate = [[[NSSortDescriptor alloc]
            // initWithKey: @"date" ascending:NO] autorelease];
            // HOROS-20240407準拠: デフォルトはname（昇順、caseInsensitiveCompare）とdate（降順）
            synchronized (outlineViewArray) {
                if (!outlineViewArray.isEmpty()) {
                    // HOROS-20240407準拠: ソート記述子を決定
                    String primarySortColumn = sortColumn;
                    boolean primaryAscending = sortAscending;

                    // HOROS-20240407準拠: ソート列が未設定または空の場合はデフォルト（name昇順、date降順）
                    if (primarySortColumn == null || primarySortColumn.isEmpty()) {
                        primarySortColumn = "name";
                        primaryAscending = true;
                    }

                    // HOROS-20240407準拠: ソート実行
                    java.util.Comparator<Object> comparator = createSortComparator(primarySortColumn, primaryAscending);
                    outlineViewArray.sort(comparator);

                    // HOROS-20240407準拠: name以外の列でソートした場合は、dateで二次ソート（降順）
                    if (!"name".equals(primarySortColumn)) {
                        java.util.Comparator<Object> dateComparator = createSortComparator("date", false);
                        outlineViewArray.sort(comparator.thenComparing(dateComparator));
                    } else {
                        // HOROS-20240407準拠: nameでソートした場合は、dateで二次ソート（降順）
                        java.util.Comparator<Object> dateComparator = createSortComparator("date", false);
                        outlineViewArray.sort(comparator.thenComparing(dateComparator));
                    }
                }
            }
            
            // HOROS-20240407準拠: アウトラインビューを更新
            if (databaseOutline != null) {
                SwingUtilities.invokeLater(() -> {
                    // HOROS-20240407準拠: ソートインジケーターを更新
                    // HOROS-20240407準拠: デフォルトはname昇順（BrowserController.m 3198行目）
                    // HOROS-20240407準拠: 列が非表示（hidden）でない限り、可視領域外でもソートインジケーターを表示
                    String currentSortCol = (sortColumn != null && !sortColumn.isEmpty()) ? sortColumn : "name";
                    boolean currentSortAsc = (sortColumn != null && !sortColumn.isEmpty()) ? sortAscending : true;
                    // HOROS-20240407準拠: 列が非表示（hidden）でない限り、常にソートインジケーターを表示
                    if (databaseOutline.isColumnWithIdentifierVisible(currentSortCol)) {
                        databaseOutline.updateSortIndicator(currentSortCol, currentSortAsc);
                    }

                    // HOROS-20240407準拠: [databaseOutline reloadData]
                    if (databaseOutline.getTreeTableModel() != null) {
                        org.jdesktop.swingx.treetable.TreeTableModel model = databaseOutline.getTreeTableModel();
                        if (model instanceof DatabaseOutlineView.DatabaseOutlineTreeTableModel) {
                            DatabaseOutlineView.DatabaseOutlineTreeTableModel treeTableModel = (DatabaseOutlineView.DatabaseOutlineTreeTableModel) model;
                            // HOROS-20240407準拠: TreeModelSupportを使って構造変更を通知
                            System.out.println("[DEBUG] outlineViewRefresh() - calling fireTreeStructureChanged()");
                            treeTableModel.fireTreeStructureChanged();
                            System.out.println("[DEBUG] outlineViewRefresh() - fireTreeStructureChanged() completed");
                        }
                    }
                    System.out.println("[DEBUG] outlineViewRefresh() - calling databaseOutline.revalidate() and repaint()");
                    databaseOutline.revalidate();
                    databaseOutline.repaint();
                    System.out.println("[DEBUG] outlineViewRefresh() - databaseOutline rowCount: " + databaseOutline.getRowCount());
                });
            }
        } finally {
            isRefreshingOutline = false;
        }
    }
    
    /**
     * マトリックスを更新
     * HOROS-20240407準拠: - (void)refreshMatrix: (id) sender (5232行目)
     * 完全実装：前回アイテムをクリアして強制的にマトリックスを更新
     */
    public void refreshMatrix(Object sender) {
        // HOROS-20240407準拠: [previousItem release]; previousItem = nil;
        previousItem = null; // これによりマトリックスの更新が強制される
        
        boolean firstResponderMatrix = false;
        
        // HOROS-20240407準拠: if( [[self window] firstResponder] == oMatrix && ...)
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner == oMatrix && focusOwner != searchField) {
            // HOROS-20240407準拠: [[self window] makeFirstResponder: databaseOutline];
            databaseOutline.requestFocus();
            firstResponderMatrix = true;
        }
        
        // HOROS-20240407準拠: [[NSNotificationCenter defaultCenter] postNotificationName:
        // NSOutlineViewSelectionDidChangeNotification ...]
        // アウトライン選択変更通知を送信（これによりoutlineViewSelectionDidChangeが呼ばれる）
        outlineViewSelectionDidChange();
        
        // HOROS-20240407準拠: [imageView display];
        if (imageView != null) {
            imageView.repaint();
        }
        
        // HOROS-20240407準拠: if( firstResponderMatrix && ...)
        if (firstResponderMatrix && focusOwner != searchField) {
            oMatrix.requestFocus();
        }
    }
    
    /**
     * アウトライン選択変更通知
     * HOROS-20240407準拠: - (void)outlineViewSelectionDidChange:(NSNotification
     * *)aNotification (4973行目)
     * 完全実装：選択変更時のマトリックス更新、プレビュー更新、比較スタディの検索
     */
    public void outlineViewSelectionDidChange() {
        System.out.println("[DEBUG] outlineViewSelectionDidChange() called");
        // HOROS-20240407準拠: if( [NSThread isMainThread] == NO)
        if (!SwingUtilities.isEventDispatchThread()) {
            System.out.println("[DEBUG] outlineViewSelectionDidChange() - not on EDT, invoking later");
            SwingUtilities.invokeLater(this::outlineViewSelectionDidChange);
            return;
        }
        
        // HOROS-20240407準拠: if( loadingIsOver == NO) return;
        System.out.println("[DEBUG] outlineViewSelectionDidChange() - loadingIsOver: " + loadingIsOver);
        if (!loadingIsOver) {
            System.out.println("[DEBUG] outlineViewSelectionDidChange() - early return due to loadingIsOver == false");
            return;
        }
        
        try {
            // HOROS-20240407準拠: キャッシュをクリア
            // cachedFilesForDatabaseOutlineSelectionSelectedFiles = nil;
            
            Object item = databaseOutline.getSelectedItem();
            System.out.println("[DEBUG] outlineViewSelectionDidChange() - selected item: " + item);
            
            boolean refreshMatrix = true;
            int nowFiles = 0;
            if (item != null) {
                nowFiles = getItemFileCount(item);
            }
            
            // HOROS-20240407準拠: if( item == previousItem || ...)
            if (item == previousItem || (previousItem != null && item != null && previousItem.equals(item))) {
                if (nowFiles == previousNoOfFiles) {
                    refreshMatrix = false;
                }
            } else {
                DatabaseIsEdited = false;
            }
            
            previousNoOfFiles = nowFiles;
            
            System.out.println("[DEBUG] outlineViewSelectionDidChange() - refreshMatrix: " + refreshMatrix + ", item: " + item);
            
            if (refreshMatrix && item != null) {
                // HOROS-20240407準拠: データベースロック
                synchronized (database) {
                    // HOROS-20240407準拠: DicomStudyが選択された場合、Seriesを遅延読み込み
                    if (item instanceof com.jj.dicomviewer.model.DicomStudy) {
                        com.jj.dicomviewer.model.DicomStudy study = (com.jj.dicomviewer.model.DicomStudy) item;
                        System.out.println("[DEBUG] outlineViewSelectionDidChange() - calling loadSeriesForStudyIfNeeded()");
                        database.loadSeriesForStudyIfNeeded(study);
                        System.out.println("[DEBUG] outlineViewSelectionDidChange() - after loadSeriesForStudyIfNeeded(), study.getSeries() size: " + (study.getSeries() != null ? study.getSeries().size() : "null"));
                    }
                    
                    if (animationSlider != null) {
                        animationSlider.setEnabled(false);
                        animationSlider.setMaximum(0);
                        animationSlider.setValue(0);
                    }
                    
                    // HOROS-20240407準拠: BrowserController.m 9403行目 - setDCMDone = NO;
                    // 新しいスタディが選択されたときにプレビューを更新するために、setDCMDoneをfalseにリセット
                    setDCMDone = false;
                    
                    // HOROS-20240407準拠: matrixViewArray = [[self childrenArray: item] retain];
                    // HOROS-20240407準拠: childrenArray:item は childrenArray:item onlyImages:YES を呼び出す
                    matrixViewArray = new ArrayList<>(childrenArray(item, true));
                    
                    // HOROS-20240407準拠: BrowserController.m 5058-5074行目
                    // item == previousItem でない場合、最初のセルを選択
                    if (item != previousItem && (previousItem == null || !item.equals(previousItem))) {
                        // HOROS-20240407準拠: [oMatrix selectCellWithTag: 0]; (5074行目)
                        if (oMatrix != null) {
                            oMatrix.selectCellWithTag(0);
                        }
                    }
                    
                    // HOROS-20240407準拠: [self matrixInit: matrixViewArray.count];
                    matrixInit(matrixViewArray.size());
                    
                    // HOROS-20240407準拠: files = [self imagesArray: item preferredObject:oFirstForFirst]; (5078行目)
                    List<Object> files = imagesArray(item, oFirstForFirst);
                    
                    // HOROS-20240407準拠: サムネイル配列を初期化 (5081-5083行目)
                    synchronized (previewPixThumbnails) {
                        // HOROS-20240407準拠: for (unsigned int i = 0; i < [files count]; i++) [previewPixThumbnails addObject:notFoundImage]; (5083行目)
                        // HOROS-20240407準拠: clear()は呼ばれず、直接addObject:notFoundImageを追加
                        // ただし、Javaでは毎回新しい選択が行われるため、clear()が必要（HOROS-20240407では既存の要素を上書きする）
                        previewPixThumbnails.clear();
                        for (int i = 0; i < files.size(); i++) {
                            previewPixThumbnails.add(notFoundImage);
                        }
                        // HOROS-20240407準拠: previewPixも同様に初期化（HOROS-20240407では明示的に初期化されていないが、matrixLoadIconsで使用される）
                        if (previewPix == null) {
                            previewPix = new ArrayList<>();
                        }
                        previewPix.clear();
                        for (int i = 0; i < files.size(); i++) {
                            previewPix.add(null);
                        }
                    }
                    
                    // HOROS-20240407準拠: アイコン読み込みスレッドを開始
                    // HOROS-20240407準拠: BrowserController.m 5105-5158行目
                    startMatrixLoadIconsThread(files, item instanceof com.jj.dicomviewer.model.DicomImage);
                }
            }
            
            if (previousItem != item) {
                previousItem = item;
                
                // HOROS-20240407準拠: BrowserController.m 6739-6756行目
                // previousItemが設定された後、DBリストの背景色を更新するため、databaseOutlineを再描画
                // HOROS-20240407準拠: willDisplayCellでpreviousItemと比較するため、
                // previousItemが設定されていれば自動的に背景色が変更される
                SwingUtilities.invokeLater(() -> {
                    if (databaseOutline != null) {
                        databaseOutline.repaint();
                    }
                });
                
                // HOROS-20240407準拠: COMPARATIVE STUDIES
                Object studySelected = getStudyFromItem(item);
                if (studySelected != null) {
                    String patientUID = getPatientUID(studySelected);
                    if (patientUID != null && !patientUID.equals(comparativePatientUID)) {
                        comparativePatientUID = patientUID;
                        comparativeStudies = null;
                        if (comparativeTable != null) {
                            // HOROS-20240407準拠: comparativeTableをクリア
                            SwingUtilities.invokeLater(() -> {
                                javax.swing.table.DefaultTableModel model = 
                                    (javax.swing.table.DefaultTableModel) comparativeTable.getModel();
                                model.setRowCount(0);
                            });
                        }

                        // HOROS-20240407準拠: [NSThread detachNewThreadSelector:
                        // @selector(searchForComparativeStudies:) ...]
                        System.out.println("[DEBUG] outlineViewSelectionDidChange() - calling searchForComparativeStudies() for patientUID: " + patientUID);
                        Thread thread = new Thread(() -> searchForComparativeStudies(studySelected));
                        thread.setName("Search for comparative studies");
                        ThreadsManager.defaultManager().addThreadAndStart(thread);
                    } else if (comparativeStudies != null && !comparativeStudies.isEmpty()) {
                        // HOROS-20240407準拠: BrowserController.m 5184-5192行目
                        // 履歴パネル（comparativeTable）の行選択を同期
                        String studyInstanceUID = getStudyInstanceUID(studySelected);
                        if (studyInstanceUID != null) {
                            // HOROS-20240407準拠: comparativeStudiesから同じstudyInstanceUIDを持つスタディのインデックスを検索
                            int index = findStudyIndexInComparativeStudies(studyInstanceUID);
                            if (index >= 0 && comparativeTable != null) {
                                // HOROS-20240407準拠: [comparativeTable selectRowIndexes: [NSIndexSet indexSetWithIndex: index] byExtendingSelection: NO];
                                SwingUtilities.invokeLater(() -> {
                                    if (index < comparativeTable.getRowCount()) {
                                        comparativeTable.setRowSelectionInterval(index, index);
                                        comparativeTable.scrollRectToVisible(comparativeTable.getCellRect(index, 0, true));
                                    }
                                });
                            } else if (comparativeTable != null) {
                                // HOROS-20240407準拠: 見つからない場合は最初の行を選択
                                SwingUtilities.invokeLater(() -> {
                                    if (comparativeTable.getRowCount() > 0) {
                                        comparativeTable.setRowSelectionInterval(0, 0);
                                    }
                                });
                            }
                        }
                    }
                } else {
                    // HOROS-20240407準拠: BrowserController.m 5202-5216行目
                    // アイテムが選択されていない場合は履歴パネルをクリア
                    comparativePatientUID = null;
                    comparativeStudies = null;
                    if (comparativeTable != null) {
                        SwingUtilities.invokeLater(() -> {
                            // HOROS-20240407準拠: [comparativeTable reloadData];
                            javax.swing.table.DefaultTableModel model = 
                                (javax.swing.table.DefaultTableModel) comparativeTable.getModel();
                            model.setRowCount(0);
                        });
                    }
                }
            }
            
            resetROIsAndKeysButton();
            
        } catch (Exception e) {
            // HOROS-20240407準拠: @catch (NSException * e) { N2LogExceptionWithStackTrace(e);
            // }
            // TODO: ログ出力（HOROS-20240407準拠のログシステムを使用）
            // e.printStackTrace(); // デバッグ用（コメントアウト）
        }
    }
    
    /**
     * データベースアウトラインを選択
     * HOROS-20240407準拠: - (void) selectDatabaseOutline (5227行目)
     */
    public void selectDatabaseOutline() {
        databaseOutline.requestFocus();
    }
    
    /**
     * データベースが押されたときの処理
     * HOROS-20240407準拠: - (IBAction)databasePressed: (id) sender
     * (BrowserController.m 7899-7902行目)
     * HOROS-20240407準拠: [self resetROIsAndKeysButton];
     */
    public void databasePressed(DatabaseOutlineView sender) {
        System.out.println("[DEBUG] databasePressed() called");
        // HOROS-20240407準拠: BrowserController.m 7901行目
        resetROIsAndKeysButton();
        
        // HOROS-20240407準拠: NSOutlineViewSelectionDidChangeNotificationが通知として送信される
        // Java Swingでは、選択変更時にoutlineViewSelectionDidChangeを呼び出す必要がある
        outlineViewSelectionDidChange();
    }
    
    /**
     * データベースがダブルクリックされたときの処理
     * HOROS-20240407準拠: - (IBAction)databaseDoublePressed: (id) sender
     * (BrowserController.m)
     */
    public void databaseDoublePressed(DatabaseOutlineView sender) {
        // TODO: 実装
    }
    
    /**
     * アウトラインビューの配列を取得
     * HOROS-20240407準拠: - (NSArray*)outlineViewArray (BrowserController.m)
     */
    public List<Object> getOutlineViewArray() {
        // HOROS-20240407準拠: outlineViewArrayを返す
        synchronized (outlineViewArray) {
            return new ArrayList<>(outlineViewArray);
        }
    }
    
    /**
     * プレビューのスクロールホイール処理
     * HOROS-20240407準拠: - (void)previewScrollWheel: (float) deltaY
     * (BrowserController.m)
     */
    public void previewScrollWheel(int wheelRotation) {
        // TODO: 実装
        // HOROS-20240407準拠: プレビュービューのスクロール処理
    }
    
    /**
     * マトリックスを取得
     * HOROS-20240407準拠: - (BrowserMatrix*)oMatrix (BrowserController.h)
     */
    public BrowserMatrix getMatrix() {
        return oMatrix;
    }
    
    /**
     * 子要素の配列を取得
     * HOROS-20240407準拠: - (NSArray*)childrenArray: (id) parent recursive: (BOOL)
     * recursive (BrowserController.m)
     */
    /**
     * 子要素の配列を取得
     * HOROS-20240407準拠: - (NSArray*) childrenArray: (id) item onlyImages:(BOOL) onlyImages (3620行目)
     * HOROS-20240407準拠: - (NSArray*) childrenArray: (id) item (3693行目) - onlyImages:YESで呼び出す
     * 
     * @param parent 親要素
     * @param onlyImages 画像のみかどうか（true: imageSeries, false: allSeries）
     * @return 子要素のリスト
     */
    public List<Object> childrenArray(Object parent, boolean onlyImages) {
        // HOROS-20240407準拠: if( [item isDeleted] || item == nil) return [NSArray array];
        if (parent == null) {
            return getOutlineViewArray();
        }
        
        // HOROS-20240407準拠: if ([[item valueForKey:@"type"] isEqualToString:@"Series"])
        if (parent instanceof com.jj.dicomviewer.model.DicomSeries) {
            com.jj.dicomviewer.model.DicomSeries series = (com.jj.dicomviewer.model.DicomSeries) parent;
            try {
                // HOROS-20240407準拠: NSArray *sortedArray = [item sortedImages];
                return new ArrayList<>(series.sortedImages());
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        
        // HOROS-20240407準拠: if ([[item valueForKey:@"type"] isEqualToString:@"Study"])
        if (parent instanceof com.jj.dicomviewer.model.DicomStudy) {
            com.jj.dicomviewer.model.DicomStudy study = (com.jj.dicomviewer.model.DicomStudy) parent;
            try {
                // HOROS-20240407準拠: if( onlyImages) sortedArray = [item valueForKey:@"imageSeries"];
                // HOROS-20240407準拠: else sortedArray = [item valueForKey:@"allSeries"];
                if (onlyImages) {
                    // HOROS-20240407準拠: imageSeriesの実装（画像を含むシリーズのみ）
                    List<com.jj.dicomviewer.model.DicomSeries> imageSeries = study.imageSeries();
                    return new ArrayList<>(imageSeries);
                } else {
                    // HOROS-20240407準拠: allSeriesの実装（すべてのシリーズ）
                    // HOROS-20240407準拠: sortedArray = [item valueForKey:@"allSeries"]; (3662行目)
                    List<com.jj.dicomviewer.model.DicomSeries> allSeries = study.allSeries();
                    
                    // HOROS-20240407準拠: Put the ROI, Comments, Reports, ... at the end of the array (3664-3677行目)
                    // StructuredReportを最後に移動
                    java.util.List<com.jj.dicomviewer.model.DicomSeries> resortedArray = new java.util.ArrayList<>(allSeries);
                    java.util.List<com.jj.dicomviewer.model.DicomSeries> SRArray = new java.util.ArrayList<>();
                    
                    for (int i = 0; i < resortedArray.size(); i++) {
                        com.jj.dicomviewer.model.DicomSeries s = resortedArray.get(i);
                        // HOROS-20240407準拠: [DCMAbstractSyntaxUID isStructuredReport: [[resortedArray objectAtIndex: i] valueForKey:@"seriesSOPClassUID"]] (3670行目)
                        if (com.jj.dicomviewer.model.DicomStudy.isStructuredReport(s.getSeriesSOPClassUID())) {
                            SRArray.add(s);
                        }
                    }
                    
                    resortedArray.removeAll(SRArray);
                    resortedArray.addAll(SRArray);
                    
                    return new ArrayList<>(resortedArray);
                }
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 画像配列を取得
     * HOROS-20240407準拠: - (NSArray*) imagesArray: (id) item preferredObject: (int) preferredObject onlyImages:(BOOL) onlyImages (3698行目)
     * HOROS-20240407準拠: Studyが選択された場合、各Seriesから1つの画像を選択して返す（全画像を返さない）
     */
    private List<Object> imagesArray(Object item, int preferredObject) {
        return imagesArray(item, preferredObject, true);
    }
    
    /**
     * 画像配列を取得（onlyImagesパラメータ付き）
     * HOROS-20240407準拠: - (NSArray*) imagesArray: (id) item preferredObject: (int) preferredObject onlyImages:(BOOL) onlyImages (3698行目)
     */
    private List<Object> imagesArray(Object item, int preferredObject, boolean onlyImages) {
        // HOROS-20240407準拠: NSArray *childrenArray = [self childrenArray: item onlyImages:onlyImages]; (3700行目)
        List<Object> childrenArray = childrenArray(item, onlyImages);
        
        if (childrenArray == null || childrenArray.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Object> imagesPathArray = new ArrayList<>();
        
        try {
            // HOROS-20240407準拠: if ([[item valueForKey:@"type"] isEqualToString:@"Series"]) (3710行目)
            if (item instanceof com.jj.dicomviewer.model.DicomSeries) {
                // HOROS-20240407準拠: imagesPathArray = [NSMutableArray arrayWithArray: childrenArray]; (3712行目)
                imagesPathArray.addAll(childrenArray);
            }
            // HOROS-20240407準拠: else if ([[item valueForKey:@"type"] isEqualToString:@"Study"]) (3714行目)
            else if (item instanceof com.jj.dicomviewer.model.DicomStudy) {
                // HOROS-20240407準拠: imagesPathArray = [NSMutableArray arrayWithCapacity: [childrenArray count]]; (3716行目)
                imagesPathArray = new ArrayList<>(childrenArray.size());
                
                // HOROS-20240407準拠: BOOL first = YES; (3718行目)
                boolean first = true;
                int currentPreferredObject = preferredObject;
                
                // HOROS-20240407準拠: for( id i in childrenArray) (3720行目)
                for (Object i : childrenArray) {
                    // HOROS-20240407準拠: int whichObject = preferredObject; (3722行目)
                    int whichObject = currentPreferredObject;
                    
                    // HOROS-20240407準拠: if( preferredObject == oFirstForFirst) (3724行目)
                    if (currentPreferredObject == oFirstForFirst) {
                        // HOROS-20240407準拠: if( first == NO) preferredObject = oAny; (3726行目)
                        if (!first) {
                            currentPreferredObject = oAny;
                            whichObject = oAny;
                        }
                    }
                    
                    first = false;
                    
                    // HOROS-20240407準拠: if( preferredObject != oMiddle) (3731行目)
                    if (whichObject != oMiddle) {
                        // HOROS-20240407準拠: if( [i primitiveValueForKey:@"thumbnail"] == nil) (3733行目)
                        // サムネイルがない場合はoMiddleを使用
                        if (i instanceof com.jj.dicomviewer.model.DicomSeries) {
                            com.jj.dicomviewer.model.DicomSeries series = (com.jj.dicomviewer.model.DicomSeries) i;
                            // HOROS-20240407準拠: primitiveValueForKey:@"thumbnail"の実装
                            // Seriesのサムネイルが存在しない場合はoMiddleを使用
                            byte[] thumbnail = series.getThumbnail();
                            if (thumbnail == null || thumbnail.length == 0) {
                                whichObject = oMiddle;
                            }
                        }
                    }
                    
                    // HOROS-20240407準拠: switch( whichObject) (3737行目)
                    switch (whichObject) {
                        case oAny:
                            // HOROS-20240407準拠: NSManagedObject *obj = [[i valueForKey:@"images"] anyObject]; (3741行目)
                            if (i instanceof com.jj.dicomviewer.model.DicomSeries) {
                                com.jj.dicomviewer.model.DicomSeries series = (com.jj.dicomviewer.model.DicomSeries) i;
                                // HOROS-20240407準拠: anyObjectは任意の1つの画像を返すが、
                                // 順序を保証するためにsortedImages()を使用
                                List<com.jj.dicomviewer.model.DicomImage> sortedImages = series.sortedImages();
                                if (sortedImages != null && !sortedImages.isEmpty()) {
                                    // HOROS-20240407準拠: anyObject（任意の1つの画像を選択）
                                    // sortedImagesの最初の画像を選択（順序が保証される）
                                    com.jj.dicomviewer.model.DicomImage selectedImage = sortedImages.get(0);
                                    // デバッグ: 選択された画像が正しいSeriesに属しているか確認
                                    if (selectedImage.getSeries() != series) {
                                        System.err.println("[ERROR] imagesArray: Selected image belongs to different series!");
                                        System.err.println("  Expected series: " + series.getSeriesInstanceUID() + " (" + series.getModality() + ")");
                                        System.err.println("  Actual series: " + (selectedImage.getSeries() != null ? selectedImage.getSeries().getSeriesInstanceUID() : "null"));
                                    }
                                    imagesPathArray.add(selectedImage);
                                }
                            }
                            break;
                            
                        case oMiddle:
                            // HOROS-20240407準拠: NSArray *seriesArray = [self childrenArray: i onlyImages:onlyImages]; (3748行目)
                            if (i instanceof com.jj.dicomviewer.model.DicomSeries) {
                                com.jj.dicomviewer.model.DicomSeries series = (com.jj.dicomviewer.model.DicomSeries) i;
                                List<Object> seriesArray = childrenArray(series, onlyImages);
                                
                                // HOROS-20240407準拠: Get the middle image of the series (3750行目)
                                if (seriesArray != null && !seriesArray.isEmpty()) {
                                    int index;
                                    if (seriesArray.size() > 1) {
                                        // HOROS-20240407準拠: [seriesArray objectAtIndex: -1 + [seriesArray count]/2] (3754行目)
                                        index = -1 + seriesArray.size() / 2;
                                    } else {
                                        // HOROS-20240407準拠: [seriesArray objectAtIndex: [seriesArray count]/2] (3756行目)
                                        index = seriesArray.size() / 2;
                                    }
                                    if (index >= 0 && index < seriesArray.size()) {
                                        Object selectedImage = seriesArray.get(index);
                                        // デバッグ: 選択された画像が正しいSeriesに属しているか確認
                                        if (selectedImage instanceof com.jj.dicomviewer.model.DicomImage) {
                                            com.jj.dicomviewer.model.DicomImage img = (com.jj.dicomviewer.model.DicomImage) selectedImage;
                                            if (img.getSeries() != series) {
                                                System.err.println("[ERROR] imagesArray: Selected image belongs to different series!");
                                                System.err.println("  Expected series: " + series.getSeriesInstanceUID() + " (" + series.getModality() + ")");
                                                System.err.println("  Actual series: " + (img.getSeries() != null ? img.getSeries().getSeriesInstanceUID() : "null"));
                                            }
                                        }
                                        imagesPathArray.add(selectedImage);
                                    }
                                }
                            }
                            break;
                            
                        case oFirstForFirst:
                            // HOROS-20240407準拠: NSArray *seriesArray = [self childrenArray: i onlyImages:onlyImages]; (3764行目)
                            if (i instanceof com.jj.dicomviewer.model.DicomSeries) {
                                com.jj.dicomviewer.model.DicomSeries series = (com.jj.dicomviewer.model.DicomSeries) i;
                                List<Object> seriesArray = childrenArray(series, onlyImages);
                                
                                // HOROS-20240407準拠: Get the first image of the series (3767行目)
                                if (seriesArray != null && !seriesArray.isEmpty()) {
                                    // HOROS-20240407準拠: [seriesArray objectAtIndex: 0] (3768行目)
                                    Object selectedImage = seriesArray.get(0);
                                    // デバッグ: 選択された画像が正しいSeriesに属しているか確認
                                    if (selectedImage instanceof com.jj.dicomviewer.model.DicomImage) {
                                        com.jj.dicomviewer.model.DicomImage img = (com.jj.dicomviewer.model.DicomImage) selectedImage;
                                        if (img.getSeries() != series) {
                                            System.err.println("[ERROR] imagesArray: Selected image belongs to different series!");
                                            System.err.println("  Expected series: " + series.getSeriesInstanceUID() + " (" + series.getModality() + ")");
                                            System.err.println("  Actual series: " + (img.getSeries() != null ? img.getSeries().getSeriesInstanceUID() : "null"));
                                        }
                                    }
                                    imagesPathArray.add(selectedImage);
                                }
                            }
                            break;
                    }
                }
            }
        } catch (Exception e) {
            // HOROS-20240407準拠: @catch (NSException *e) { N2LogExceptionWithStackTrace(e); } (3776行目)
            e.printStackTrace();
        }
        
        // HOROS-20240407準拠: return imagesPathArray; (3783行目)
        return imagesPathArray;
    }

    // HOROS-20240407準拠: BrowserController.h 255行目 IBOutlet NSTableView*
    // _activityTableView;
    private javax.swing.JTable activityTableView;
    
    /**
     * Activityテーブルビューを取得
     * HOROS-20240407準拠: - (NSTableView*)_activityTableView
     * (BrowserController+Activity.mm 82行目)
     */
    public javax.swing.JTable getActivityTableView() {
        return activityTableView;
    }
    
    // HOROS-20240407準拠: ソート関連のフィールド
    // HOROS-20240407準拠: ソート状態は保存/復元される（NSUserDefaults相当）
    private String sortColumn = null;
    private boolean sortAscending = true;
    
    /**
     * ソート列を取得
     * HOROS-20240407準拠: - (NSString*)sortColumn (BrowserController.m)
     */
    public String getSortColumn() {
        return sortColumn;
    }

    /**
     * ソートが昇順かどうかを取得
     * HOROS-20240407準拠: ソート状態の取得
     */
    public boolean isSortAscending() {
        return sortAscending;
    }
    
    /**
     * ソート順を取得
     * HOROS-20240407準拠: - (BOOL)sortAscending (BrowserController.m)
     */
    public boolean getSortAscending() {
        return sortAscending;
    }
    
    /**
     * ソート列を設定
     * HOROS-20240407準拠: - (void)setSortColumn: (NSString*) column ascending: (BOOL)
     * ascending (BrowserController.m)
     * HOROS-20240407準拠: outlineView:sortDescriptorsDidChange: (6665行目) -
     * ソート変更時にoutlineViewRefreshを呼び出す
     */
    public void setSortColumn(String column, boolean ascending) {
        this.sortColumn = column;
        this.sortAscending = ascending;

        // HOROS-20240407準拠: ソート状態の保存はwindowWillClose:で行う（14733-14736行目）
        // setSortColumn内では保存しない（HOROS-20240407準拠）

        // HOROS-20240407準拠: ソートインジケーターを更新
        if (databaseOutline != null) {
            SwingUtilities.invokeLater(() -> {
                databaseOutline.updateSortIndicator(column, ascending);
            });
        }

        // HOROS-20240407準拠: ソート変更時にoutlineViewRefreshを呼び出す
        outlineViewRefresh();

        // HOROS-20240407準拠: name以外の列でソートした場合は最初の行を選択 (6669-6670行目)
        if (column != null && !"name".equals(column) && databaseOutline != null) {
            SwingUtilities.invokeLater(() -> {
                if (databaseOutline.getRowCount() > 0) {
                    databaseOutline.setRowSelectionInterval(0, 0);
                    databaseOutline.scrollRowToVisible(0);
                }
            });
        }
    }

    /**
     * ソート状態を保存
     * HOROS-20240407準拠: BrowserController.m 14733-14736行目
     * HOROS-20240407準拠: if( [[databaseOutline sortDescriptors] count] >= 1)
     * HOROS-20240407準拠: NSDictionary	*sort = [NSDictionary dictionaryWithObjectsAndKeys: [NSNumber numberWithBool:[[[databaseOutline sortDescriptors] objectAtIndex: 0] ascending]], @"order", [[[databaseOutline sortDescriptors] objectAtIndex: 0] key], @"key", nil];
     * HOROS-20240407準拠: [[NSUserDefaults standardUserDefaults] setObject:sort forKey: @"databaseSortDescriptor"];
     * HOROS-20240407準拠: NSDictionary形式で保存（keyとorderを含む）
     */
    private void saveSortState() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(BrowserController.class);
            // HOROS-20240407準拠: if( [[databaseOutline sortDescriptors] count] >= 1)
            // HOROS-20240407準拠: 現在のソート状態（sortColumnとsortAscending）を保存
            if (sortColumn != null && !sortColumn.isEmpty()) {
                // HOROS-20240407準拠: NSDictionary形式で保存（keyとorderを含む）
                // 簡易実装: "key:order"形式で保存（より堅牢な実装にはJSONライブラリを使用）
                String sortDescriptor = sortColumn + ":" + (sortAscending ? "true" : "false");
                prefs.put("databaseSortDescriptor", sortDescriptor);
            } else {
                // ソート状態が未設定の場合は削除
                prefs.remove("databaseSortDescriptor");
            }
            try {
                prefs.flush();
            } catch (java.util.prefs.BackingStoreException e) {
                // エラーが発生した場合は保存をスキップ
            }
        } catch (Exception e) {
            // エラーが発生した場合は保存をスキップ
        }
    }

    /**
     * ソート状態を復元
     * HOROS-20240407準拠: BrowserController.m 14318-14333行目
     * HOROS-20240407準拠: if( [[NSUserDefaults standardUserDefaults]
     * objectForKey: @"databaseSortDescriptor"])
     * HOROS-20240407準拠: NSDictionary形式から復元（keyとorderを含む）
     * HOROS-20240407準拠: [databaseOutline setSortDescriptors:...]でソート状態を設定
     * HOROS-20240407準拠: 列が表示されている場合のみソート状態を復元（14322-14329行目）
     */
    private void restoreSortState() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(BrowserController.class);
            String sortDescriptor = prefs.get("databaseSortDescriptor", null);
            if (sortDescriptor != null && !sortDescriptor.isEmpty()) {
                // HOROS-20240407準拠: NSDictionary形式から復元（keyとorderを含む）
                // 簡易実装: "key:order"形式から復元（より堅牢な実装にはJSONライブラリを使用）
                String[] parts = sortDescriptor.split(":", 2);
                if (parts.length == 2) {
                    String savedColumn = parts[0];
                    boolean savedAscending = "true".equals(parts[1]);

                    // HOROS-20240407準拠: BrowserController.m 14322-14329行目
                    // if( [databaseOutline isColumnWithIdentifierVisible: [sort objectForKey:@"key"]])
                    // 列が表示されている場合のみソート状態を復元
                    SwingUtilities.invokeLater(() -> {
                        if (databaseOutline != null && databaseOutline.isColumnWithIdentifierVisible(savedColumn)) {
                            // HOROS-20240407準拠: 列が表示されている場合は、そのソート記述子を設定
                            // HOROS-20240407準拠: setSortColumnを呼び出してソート状態を設定（outlineViewRefreshも呼ばれる）
                            setSortColumn(savedColumn, savedAscending);
                        } else {
                            // HOROS-20240407準拠: 列が表示されていない場合は、デフォルト（name昇順）を設定
                            // HOROS-20240407準拠: [databaseOutline setSortDescriptors:[NSArray arrayWithObject: [[[NSSortDescriptor alloc] initWithKey:@"name" ascending:YES selector:@selector(caseInsensitiveCompare:)] autorelease]]];
                            setSortColumn("name", true);
                        }
                    });
                } else {
                    // 無効な形式の場合はデフォルトを設定
                    SwingUtilities.invokeLater(() -> {
                        setSortColumn("name", true);
                    });
                }
            } else {
                // HOROS-20240407準拠: else [databaseOutline setSortDescriptors:[NSArray
                // arrayWithObject: [[[NSSortDescriptor alloc] initWithKey:@"name" ascending:YES
                // selector:@selector(caseInsensitiveCompare:)] autorelease]]];
                // 保存されたソート状態がない場合はデフォルト（name昇順）を設定
                // HOROS-20240407準拠: setSortColumnを呼び出してソート状態を設定（outlineViewRefreshも呼ばれる）
                SwingUtilities.invokeLater(() -> {
                    setSortColumn("name", true);
                });
            }
        } catch (Exception e) {
            // エラーが発生した場合はデフォルトのソート状態を使用
            this.sortColumn = "name";
            this.sortAscending = true;
        }
    }
    
    /**
     * マトリックスが押されたときの処理
     * HOROS-20240407準拠: - (IBAction)matrixPressed: (id) sender (9298行目)
     * 完全実装：セル選択時の処理、アニメーションスライダーの制御、プレビュー更新
     */
    public void matrixPressed(BrowserMatrix sender) {
        System.out.println("[DEBUG] matrixPressed() called - sender: " + sender);
        // HOROS-20240407準拠: id theCell = [sender selectedCell];
        javax.swing.JButton selectedCell = sender.selectedCell();
        System.out.println("[DEBUG] matrixPressed() - selectedCell: " + selectedCell);
        int index = -1;
        
        // HOROS-20240407準拠: [self.window makeFirstResponder: oMatrix];
        oMatrix.requestFocus();
        
        if (selectedCell != null) {
            // HOROS-20240407準拠: if( [theCell tag] >= 0)
            Object tagObj = selectedCell.getClientProperty("tag");
            if (tagObj instanceof Integer) {
                index = (Integer) tagObj;
                System.out.println("[DEBUG] matrixPressed() - index: " + index);
            } else {
                System.out.println("[DEBUG] matrixPressed() - tagObj is not Integer: " + tagObj);
            }
        } else {
            System.out.println("[DEBUG] matrixPressed() - selectedCell is null");
        }
        
        if (index >= 0) {
            // HOROS-20240407準拠: NSManagedObject *dcmFile = [databaseOutline
            // itemAtRow:[databaseOutline selectedRow]];
            Object dcmFile = databaseOutline.getSelectedItem();
            
            if (dcmFile != null) {
                // HOROS-20240407準拠: BrowserController.m 9329行目
                // if( [[dcmFile valueForKey:@"type"] isEqualToString: @"Series"] && [[[dcmFile valueForKey:@"images"] allObjects] count] > 1)
                String type = getItemType(dcmFile);
                System.out.println("[DEBUG] matrixPressed() - type: " + type);
                if ("Series".equals(type)) {
                    int imageCount = getItemImageCount(dcmFile);
                    System.out.println("[DEBUG] matrixPressed() - imageCount: " + imageCount);
                    if (imageCount > 1) {
                        // HOROS-20240407準拠: BrowserController.m 9331行目 - [animationSlider setIntValue: [theCell tag]];
                        if (animationSlider != null) {
                            animationSlider.setValue(index);
                        }
                        // HOROS-20240407準拠: BrowserController.m 9332行目 - [self previewSliderAction: nil];
                        System.out.println("[DEBUG] matrixPressed() - calling previewSliderAction(null) for Series with imageCount > 1");
                        previewSliderAction(null);
                        return;
                    } else {
                        System.out.println("[DEBUG] matrixPressed() - imageCount <= 1, not calling previewSliderAction");
                    }
                } else {
                    System.out.println("[DEBUG] matrixPressed() - type is not Series: " + type);
                }
            }
        }
        
        // HOROS-20240407準拠: [animationSlider setEnabled:NO];
        if (animationSlider != null) {
            animationSlider.setEnabled(false);
            animationSlider.setMaximum(0);
            animationSlider.setValue(0);
        }
        
        if (index >= 0) {
            // HOROS-20240407準拠: BrowserController.m 9348行目
            // NSManagedObject *dcmFile = [databaseOutline itemAtRow:[databaseOutline selectedRow]];
            Object dcmFile = databaseOutline.getSelectedItem();
            
            if (dcmFile != null) {
                String type = getItemType(dcmFile);
                System.out.println("[DEBUG] matrixPressed() - second check, type: " + type + ", index: " + index);
                // HOROS-20240407準拠: BrowserController.m 9369-9373行目
                // if( [[dcmFile valueForKey:@"type"] isEqualToString: @"Study"] == NO)
                // {
                //     index = [theCell tag];
                //     [imageView setIndex: index];
                // }
                if (!"Study".equals(type)) {
                    // HOROS-20240407準拠: BrowserController.m 9369-9373行目
                    // if( [[dcmFile valueForKey:@"type"] isEqualToString: @"Study"] == NO)
                    // {
                    //     index = [theCell tag];
                    //     [imageView setIndex: index];
                    // }
                    // HOROS-20240407準拠: BrowserController.m 9372行目 - [imageView setIndex: index];
                    // HOROS-20240407準拠: setIndexはdcmPixListから画像を取得するため、
                    // setPixelsでpreviewPixがdcmPixListに設定されている必要がある
                    // HOROS-20240407準拠: matrixDisplayIconsでsetPixelsが呼び出されているため、
                    // ここではsetIndexを呼び出すだけでよい
                    if (imageView != null) {
                        System.out.println("[DEBUG] matrixPressed() - calling imageView.setIndex(" + index + ") for non-Study type");
                        synchronized (previewPixThumbnails) {
                            // HOROS-20240407準拠: previewPixが正しく設定されているか確認
                            if (previewPix == null || previewPix.isEmpty() || 
                                index >= previewPix.size() || previewPix.get(index) == null) {
                                System.out.println("[DEBUG] matrixPressed() - previewPix is not properly set, calling setPixels");
                                // previewPixが正しく設定されていない場合、setPixelsを呼び出す
                                List<Object> files = imagesArray(dcmFile, oAny);
                                if (!files.isEmpty()) {
                                    // previewPixを初期化
                                    if (previewPix == null || previewPix.isEmpty()) {
                                        previewPix = new ArrayList<>();
                                        for (int j = 0; j < files.size(); j++) {
                                            previewPix.add(null);
                                        }
                                    }
                                    // setPixelsを呼び出してdcmPixListを設定
                                    imageView.setPixels(previewPix, files, null, (short) index, 'i', true);
                                    imageView.setStringID("previewDatabase");
                                }
                            } else {
                                System.out.println("[DEBUG] matrixPressed() - previewPix is properly set, calling setIndex directly");
                            }
                            // HOROS-20240407準拠: BrowserController.m 9372行目 - [imageView setIndex: index];
                            imageView.setIndex(index);
                        }
                    } else {
                        System.out.println("[DEBUG] matrixPressed() - imageView is null");
                    }
                } else {
                    // HOROS-20240407準拠: BrowserController.m 9369-9373行目
                    // if( [[dcmFile valueForKey:@"type"] isEqualToString: @"Study"] == NO)
                    // {
                    //     index = [theCell tag];
                    //     [imageView setIndex: index];
                    // }
                    // スタディレベルの場合はsetIndexを呼び出さない（BrowserController.m 9369行目）
                    // しかし、HOROS-20240407の実装を見ると、previewSliderActionではStudyタイプの場合、
                    // matrixViewArrayからSeriesを取得してsetIndexを呼び出している
                    // そのため、ここではpreviewPixが既にスタディ全体の画像リストを保持しているため、
                    // setPixelsを呼び出す必要はなく、setIndex(index)を呼び出すだけでよい
                    System.out.println("[DEBUG] matrixPressed() - type is Study, using previewPix index: " + index);
                    if (imageView != null) {
                        synchronized (previewPixThumbnails) {
                            // HOROS-20240407準拠: previewPixが既にスタディ全体の画像リストを保持しているため、
                            // setPixelsを呼び出す必要はなく、setIndex(index)を呼び出すだけでよい
                            // previewPixのインデックスは、matrixViewArrayのインデックスと一致している
                            if (previewPix != null && index >= 0 && index < previewPix.size()) {
                                System.out.println("[DEBUG] matrixPressed() - calling imageView.setIndex(" + index + ") for Study type");
                                imageView.setIndex(index);
                                System.out.println("[DEBUG] matrixPressed() - imageView.setIndex(" + index + ") completed");
                            } else {
                                System.out.println("[DEBUG] matrixPressed() - previewPix is null or index out of range: previewPix=" + (previewPix != null ? previewPix.size() : "null") + ", index=" + index);
                            }
                        }
                    } else {
                        System.out.println("[DEBUG] matrixPressed() - imageView is null");
                    }
                }
                // HOROS-20240407準拠: スタディレベルの場合はsetIndexを呼び出さない（BrowserController.m 9369行目）
                
                // HOROS-20240407準拠: [self initAnimationSlider];
                initAnimationSlider();
            } else {
                System.out.println("[DEBUG] matrixPressed() - dcmFile is null");
            }
        } else {
            System.out.println("[DEBUG] matrixPressed() - index < 0");
        }
        
        // HOROS-20240407準拠: [self resetROIsAndKeysButton];
        resetROIsAndKeysButton();
    }
    
    /**
     * マトリックスがダブルクリックされたときの処理
     * HOROS-20240407準拠: - (IBAction) matrixDoublePressed:(id)sender (9381行目)
     */
    public void matrixDoublePressed(BrowserMatrix sender) {
        javax.swing.JButton selectedCell = sender.selectedCell();
        if (selectedCell != null) {
            Object tagObj = selectedCell.getClientProperty("tag");
            if (tagObj instanceof Integer && (Integer) tagObj >= 0) {
                // HOROS-20240407準拠: [self viewerDICOM: [[oMatrix menu] itemAtIndex:0]];
                // TODO: viewerDICOMメソッドの実装
                databaseOpenStudy(databaseOutline.getSelectedItem());
            }
        }
    }
    
    /**
     * アルバムを追加
     * HOROS-20240407準拠: - (void)addAlbum: (id) sender (BrowserController.m)
     */
    public void addAlbum(Object sender) {
        // TODO: 実装
        if (database != null) {
            // 新しいアルバムを作成してデータベースに追加
            // DicomAlbum album = new DicomAlbum("New Album");
            // database.addAlbum(album);
        }
    }
    
    /**
     * スマートアルバムを追加
     * HOROS-20240407準拠: - (void)addSmartAlbum: (id) sender (BrowserController.m)
     */
    public void addSmartAlbum(Object sender) {
        // TODO: 実装
        if (database != null) {
            // 新しいスマートアルバムを作成してデータベースに追加
            // DicomAlbum album = new DicomAlbum("New Smart Album", "predicate");
            // database.addAlbum(album);
        }
    }
    
    /**
     * アルバムを削除
     * HOROS-20240407準拠: - (void)removeAlbum: (id) album (BrowserController.m)
     */
    public void removeAlbum(Object album) {
        // TODO: 実装
        if (database != null) {
            // database.removeAlbum(album);
        }
    }
    
    // ========== スレッド関連メソッド ==========
    // HOROS-20240407準拠: BrowserController.m のスレッド関連メソッドを復元
    
    /**
     * ファイルコピースレッド
     * HOROS-20240407準拠: - (void) copyFilesThread: (NSDictionary*) dict (2105行目)
     * データベースに委譲
     */
    public void copyFilesThread(Map<String, Object> dict) {
        if (database != null) {
            // HOROS-20240407準拠: [self.database performSelector:@selector(copyFilesThread:)
            // withObject:dict];
            database.copyFilesThread(dict);
        }
    }
    
    /**
     * 自動コメント再生成スレッド
     * HOROS-20240407準拠: - (void) regenerateAutoCommentsThread: (NSDictionary*)
     * arrays (1108行目)
     */
    public void regenerateAutoCommentsThread(Map<String, Object> arrays) {
        Thread thread = new Thread(() -> {
            try {
                // HOROS-20240407準拠: NSManagedObjectContext *context =
                // self.database.independentContext;
                // TODO: データベースの独立コンテキストを取得
                
                @SuppressWarnings("unchecked")
                List<Object> studiesArray = (List<Object>) arrays.get("studyArrayIDs");
                @SuppressWarnings("unchecked")
                List<Object> seriesArray = (List<Object>) arrays.get("seriesArrayIDs");
                
                // HOROS-20240407準拠: NSString *commentField = [[NSUserDefaults
                // standardUserDefaults] stringForKey: @"commentFieldForAutoFill"];
                String commentField = "comment"; // TODO: UserDefaultsから取得
                
                // HOROS-20240407準拠: BOOL studyLevel = [[NSUserDefaults standardUserDefaults]
                // boolForKey: @"COMMENTSAUTOFILLStudyLevel"];
                boolean studyLevel = false; // TODO: UserDefaultsから取得
                
                // HOROS-20240407準拠: BOOL seriesLevel = [[NSUserDefaults standardUserDefaults]
                // boolForKey: @"COMMENTSAUTOFILLSeriesLevel"];
                boolean seriesLevel = false; // TODO: UserDefaultsから取得
                
                // HOROS-20240407準拠: BOOL commentsAutoFill = [[NSUserDefaults
                // standardUserDefaults] boolForKey: @"COMMENTSAUTOFILL"];
                boolean commentsAutoFill = false; // TODO: UserDefaultsから取得
                
                if (studiesArray != null) {
                    int x = 0;
                    for (Object studyID : studiesArray) {
                        // HOROS-20240407準拠: DicomStudy *s = (DicomStudy*) [context objectWithID:
                        // studyID];
                        // TODO: スタディを取得してコメントをリセット
                        
                        // HOROS-20240407準拠: float p = (float) (x++) / (float) studiesArray.count;
                        double progress = (double) (x++) / studiesArray.size();
                        ThreadsManager.defaultManager().setThreadProgress(Thread.currentThread(), progress);
                        
                        // HOROS-20240407準拠: if( [[NSThread currentThread] isCancelled])
                        if (ThreadsManager.defaultManager().isCancelled(Thread.currentThread())) {
                            break;
                        }
                    }
                }
                
                if (seriesArray != null) {
                    int i = 0;
                    for (Object seriesID : seriesArray) {
                        // HOROS-20240407準拠: DicomSeries *series = (DicomSeries*) [context objectWithID:
                        // seriesID];
                        // TODO: シリーズを取得してコメントを処理
                        
                        // HOROS-20240407準拠: float p = (float) (i++) / (float) seriesArray.count;
                        double progress = (double) (i++) / seriesArray.size();
                        ThreadsManager.defaultManager().setThreadProgress(Thread.currentThread(), progress);
                        
                        // HOROS-20240407準拠: if( [[NSThread currentThread] isCancelled])
                        if (ThreadsManager.defaultManager().isCancelled(Thread.currentThread())) {
                            break;
                        }
                    }
                }
                
                // HOROS-20240407準拠: [self performSelectorOnMainThread: @selector(
                // outlineViewRefresh) withObject: nil waitUntilDone: NO];
                SwingUtilities.invokeLater(() -> outlineViewRefresh());
                
            } catch (Exception e) {
                // HOROS-20240407準拠: @catch (NSException * e) { N2LogExceptionWithStackTrace(e);
                // }
                // TODO: ログ出力（HOROS-20240407準拠のログシステムを使用）
                // e.printStackTrace(); // デバッグ用（コメントアウト）
            }
        });
        
        thread.setName("Regenerating auto comments...");
        ThreadsManager.defaultManager().addThreadAndStart(thread);
    }
    
    /**
     * 自動コメント再生成
     * HOROS-20240407準拠: - (IBAction) regenerateAutoComments:(id) sender (1264行目)
     */
    public void regenerateAutoComments(Object sender) {
        // TODO: 確認ダイアログを表示
        // HOROS-20240407準拠: NSRunInformationalAlertPanel
        
        // 選択されたスタディとシリーズを取得
        List<Object> studiesArray = new ArrayList<>();
        List<Object> seriesArray = new ArrayList<>();
        
        // TODO: 選択されたアイテムからIDを取得
        
        Map<String, Object> arrays = new HashMap<>();
        arrays.put("studyArrayIDs", studiesArray);
        arrays.put("seriesArrayIDs", seriesArray);
        
        regenerateAutoCommentsThread(arrays);
    }
    
    /**
     * 非同期WADOダウンロード
     * HOROS-20240407準拠: - (void) asyncWADODownload:(NSString*) filename (824行目)
     */
    public void asyncWADODownload(String filename) {
        Thread thread = new Thread(() -> {
            try {
                // HOROS-20240407準拠: NSMutableArray *urlToDownloads = [NSMutableArray array];
                List<java.net.URL> urlToDownloads = new ArrayList<>();
                
                // HOROS-20240407準拠: NSArray *urlsR = [[NSString
                // stringWithContentsOfFile:filename usedEncoding:NULL error:NULL]
                // componentsSeparatedByString: @"\r"];
                // HOROS-20240407準拠: NSArray *urlsN = [[NSString
                // stringWithContentsOfFile:filename usedEncoding:NULL error:NULL]
                // componentsSeparatedByString: @"\n"];
                java.nio.file.Path path = java.nio.file.Paths.get(filename);
                String content = new String(java.nio.file.Files.readAllBytes(path));
                
                String[] urlsR = content.split("\r");
                String[] urlsN = content.split("\n");
                
                String[] urls = urlsR.length >= urlsN.length ? urlsR : urlsN;
                
                for (String url : urls) {
                    if (url != null && !url.trim().isEmpty()) {
                        try {
                            urlToDownloads.add(new java.net.URL(url.trim()));
                        } catch (java.net.MalformedURLException e) {
                            // 無効なURLはスキップ
                        }
                    }
                }
                
                // HOROS-20240407準拠: WADODownload *downloader = [[[WADODownload alloc] init]
                // autorelease];
                // HOROS-20240407準拠: [downloader WADODownload: urlToDownloads];
                // TODO: WADODownloadの実装
                
                // HOROS-20240407準拠: [[NSFileManager defaultManager] removeItemAtPath: filename
                // error: nil];
                java.nio.file.Files.deleteIfExists(path);
                
            } catch (Exception e) {
                // HOROS-20240407準拠: @catch (NSException * e) { N2LogExceptionWithStackTrace(e);
                // }
                // TODO: ログ出力（HOROS-20240407準拠のログシステムを使用）
                // e.printStackTrace(); // デバッグ用（コメントアウト）
            }
        });
        
        thread.setName("WADO Download");
        ThreadsManager.defaultManager().addThreadAndStart(thread);
    }
    
    /**
     * 画像処理スレッド（vImage相当）
     * HOROS-20240407準拠: - (void) vImageThread: (NSDictionary*) d (582行目)
     * 注意: vImageはmacOS固有のため、Javaでは別の実装が必要
     */
    public void vImageThread(Map<String, Object> d) {
        // HOROS-20240407準拠: vImage_Buffer src = *(vImage_Buffer*) [[d objectForKey:
        // @"src"] pointerValue];
        // HOROS-20240407準拠: vImage_Buffer dst = *(vImage_Buffer*) [[d objectForKey:
        // @"dst"] pointerValue];
        
        String what = (String) d.get("what");
        
        if ("FTo16U".equals(what)) {
            // HOROS-20240407準拠: vImageConvert_FTo16U(&src, &dst, offset, scale,
            // kvImageDoNotTile);
            // TODO: Javaで画像変換を実装（BufferedImageを使用）
        } else if ("16UToF".equals(what)) {
            // HOROS-20240407準拠: vImageConvert_16UToF(&src, &dst, offset, scale,
            // kvImageDoNotTile);
            // TODO: Javaで画像変換を実装（BufferedImageを使用）
        } else {
            // HOROS-20240407準拠: 不明なvImageThreadの場合は何もしない
            // System.err.println("****** unknown vImageThread what: " + what); //
            // デバッグ用（コメントアウト）
        }
    }
    
    /**
     * マルチスレッド画像変換
     * HOROS-20240407準拠: + (void) multiThreadedImageConvert: (NSString*) what
     * :(vImage_Buffer*) src :(vImage_Buffer *) dst :(float) offset :(float) scale
     * (625行目)
     */
    public static void multiThreadedImageConvert(String what, Object src, Object dst, float offset, float scale) {
        // HOROS-20240407準拠: int mpprocessors = [[NSProcessInfo processInfo]
        // processorCount];
        int mpprocessors = Runtime.getRuntime().availableProcessors();
        
        // HOROS-20240407準拠: static NSConditionLock *threadLock = nil;
        // TODO: スレッドロックの実装（JavaではCountDownLatchまたはCyclicBarrierを使用）
        
        // HOROS-20240407準拠: for( int i = 0; i < mpprocessors; i++)
        // HOROS-20240407準拠: [NSThread detachNewThreadSelector: @selector(vImageThread:)
        // toTarget: browserWindow withObject: d];
        // TODO: マルチスレッド画像変換を実装
    }
    
    // ========== ヘルパーメソッド ==========
    // HOROS-20240407準拠: UI関連のヘルパーメソッド
    
    /**
     * アイテムのタイプを取得
     */
    private String getItemType(Object item) {
        if (item == null)
            return null;
        if (item instanceof com.jj.dicomviewer.model.DicomStudy)
            return "Study";
        if (item instanceof com.jj.dicomviewer.model.DicomSeries)
            return "Series";
        if (item instanceof com.jj.dicomviewer.model.DicomImage)
            return "Image";
        return null;
    }
    
    /**
     * アイテムのファイル数を取得
     */
    private int getItemFileCount(Object item) {
        if (item instanceof com.jj.dicomviewer.model.DicomStudy) {
            return ((com.jj.dicomviewer.model.DicomStudy) item).getNumberOfImages() != null
                    ? ((com.jj.dicomviewer.model.DicomStudy) item).getNumberOfImages()
                    : 0;
        }
        if (item instanceof com.jj.dicomviewer.model.DicomSeries) {
            return ((com.jj.dicomviewer.model.DicomSeries) item).getNumberOfImages() != null
                    ? ((com.jj.dicomviewer.model.DicomSeries) item).getNumberOfImages()
                    : 0;
        }
        return 0;
    }
    
    /**
     * アイテムの画像数を取得
     */
    private int getItemImageCount(Object item) {
        if (item instanceof com.jj.dicomviewer.model.DicomSeries) {
            return ((com.jj.dicomviewer.model.DicomSeries) item).getImages() != null
                    ? ((com.jj.dicomviewer.model.DicomSeries) item).getImages().size()
                    : 0;
        }
        return 0;
    }
    
    /**
     * アイテムからスタディを取得
     */
    private Object getStudyFromItem(Object item) {
        if (item instanceof com.jj.dicomviewer.model.DicomStudy) {
            return item;
        }
        if (item instanceof com.jj.dicomviewer.model.DicomSeries) {
            return ((com.jj.dicomviewer.model.DicomSeries) item).getStudy();
        }
        if (item instanceof com.jj.dicomviewer.model.DicomImage) {
            return ((com.jj.dicomviewer.model.DicomImage) item).getSeries() != null
                    ? ((com.jj.dicomviewer.model.DicomImage) item).getSeries().getStudy()
                    : null;
        }
        return null;
    }
    
    /**
     * 患者UIDを取得
     */
    private String getPatientUID(Object study) {
        if (study instanceof com.jj.dicomviewer.model.DicomStudy) {
            return ((com.jj.dicomviewer.model.DicomStudy) study).getPatientUID();
        }
        return null;
    }

    /**
     * スタディのstudyInstanceUIDを取得
     * HOROS-20240407準拠: BrowserController.m 5184-5192行目
     */
    private String getStudyInstanceUID(Object study) {
        if (study instanceof com.jj.dicomviewer.model.DicomStudy) {
            return ((com.jj.dicomviewer.model.DicomStudy) study).getStudyInstanceUID();
        }
        return null;
    }
    
    /**
     * comparativePatientUIDを取得
     * HOROS-20240407準拠: BrowserController.m 497行目 - @synthesize comparativePatientUID
     * DatabaseOutlineViewのセルレンダラーから使用される
     */
    public String getComparativePatientUID() {
        return comparativePatientUID;
    }
    
    /**
     * 選択された行の患者UIDを取得
     * HOROS-20240407準拠: BrowserController.m 6739-6756行目
     * willDisplayCellでpreviousItemの患者UIDと比較するため
     */
    public String getSelectedPatientUID() {
        if (previousItem == null) {
            return null;
        }
        Object study = getStudyFromItem(previousItem);
        if (study != null) {
            return getPatientUID(study);
        }
        return null;
    }
    
    /**
     * previousItemを取得
     * HOROS-20240407準拠: BrowserController.m 6745行目 - [[previousItem valueForKey: @"type"] isEqualToString:@"Study"]
     * willDisplayCellでpreviousItemと比較するため
     */
    public Object getPreviousItem() {
        return previousItem;
    }

    /**
     * comparativeStudiesから同じstudyInstanceUIDを持つスタディのインデックスを検索
     * HOROS-20240407準拠: BrowserController.m 5186行目
     * HOROS-20240407準拠: NSUInteger index = [[self.comparativeStudies valueForKey: @"studyInstanceUID"] indexOfObject: [studySelected valueForKey: @"studyInstanceUID"]];
     */
    private int findStudyIndexInComparativeStudies(String studyInstanceUID) {
        if (comparativeStudies == null || studyInstanceUID == null) {
            return -1;
        }
        
        for (int i = 0; i < comparativeStudies.size(); i++) {
            Object study = comparativeStudies.get(i);
            String uid = getStudyInstanceUID(study);
            if (studyInstanceUID.equals(uid)) {
                return i;
            }
        }
        
        return -1;
    }

    /**
     * ソート用Comparatorを作成
     * HOROS-20240407準拠: NSSortDescriptorの動作を再現
     * HOROS-20240407準拠: BrowserController.m 3193-3199行目
     */
    private java.util.Comparator<Object> createSortComparator(String column, boolean ascending) {
        return (o1, o2) -> {
            if (!(o1 instanceof com.jj.dicomviewer.model.DicomStudy) ||
                    !(o2 instanceof com.jj.dicomviewer.model.DicomStudy)) {
                return 0;
            }

            com.jj.dicomviewer.model.DicomStudy study1 = (com.jj.dicomviewer.model.DicomStudy) o1;
            com.jj.dicomviewer.model.DicomStudy study2 = (com.jj.dicomviewer.model.DicomStudy) o2;

            int result = 0;

            try {
                switch (column) {
                    case "name":
                        // HOROS-20240407準拠: caseInsensitiveCompare相当
                        String name1 = study1.getName() != null ? study1.getName() : "";
                        String name2 = study2.getName() != null ? study2.getName() : "";
                        result = name1.compareToIgnoreCase(name2);
                        break;
                    case "date":
                        // HOROS-20240407準拠: date降順がデフォルト
                        java.time.LocalDateTime date1 = study1.getDate();
                        java.time.LocalDateTime date2 = study2.getDate();
                        if (date1 == null && date2 == null) {
                            result = 0;
                        } else if (date1 == null) {
                            result = 1; // nullは後ろに
                        } else if (date2 == null) {
                            result = -1; // nullは後ろに
                        } else {
                            result = date1.compareTo(date2);
                        }
                        break;
                    case "patientID":
                        String pid1 = study1.getPatientID() != null ? study1.getPatientID() : "";
                        String pid2 = study2.getPatientID() != null ? study2.getPatientID() : "";
                        result = pid1.compareToIgnoreCase(pid2);
                        break;
                    case "accessionNumber":
                        String acc1 = study1.getAccessionNumber() != null ? study1.getAccessionNumber() : "";
                        String acc2 = study2.getAccessionNumber() != null ? study2.getAccessionNumber() : "";
                        result = acc1.compareToIgnoreCase(acc2);
                        break;
                    case "studyName":
                        String sn1 = study1.getStudyName() != null ? study1.getStudyName() : "";
                        String sn2 = study2.getStudyName() != null ? study2.getStudyName() : "";
                        result = sn1.compareToIgnoreCase(sn2);
                        break;
                    case "modality":
                        String mod1 = study1.getModality() != null ? study1.getModality() : "";
                        String mod2 = study2.getModality() != null ? study2.getModality() : "";
                        result = mod1.compareToIgnoreCase(mod2);
                        break;
                    case "dateAdded":
                        java.time.LocalDateTime da1 = study1.getDateAdded();
                        java.time.LocalDateTime da2 = study2.getDateAdded();
                        if (da1 == null && da2 == null) {
                            result = 0;
                        } else if (da1 == null) {
                            result = 1;
                        } else if (da2 == null) {
                            result = -1;
                        } else {
                            result = da1.compareTo(da2);
                        }
                        break;
                    case "dateOpened":
                        java.time.LocalDateTime do1 = study1.getDateOpened();
                        java.time.LocalDateTime do2 = study2.getDateOpened();
                        if (do1 == null && do2 == null) {
                            result = 0;
                        } else if (do1 == null) {
                            result = 1;
                        } else if (do2 == null) {
                            result = -1;
                        } else {
                            result = do1.compareTo(do2);
                        }
                        break;
                    default:
                        // その他の列は文字列として比較
                        String val1 = getStudyValueAsString(study1, column);
                        String val2 = getStudyValueAsString(study2, column);
                        result = val1.compareToIgnoreCase(val2);
                        break;
                }
            } catch (Exception e) {
                // エラーが発生した場合は0を返す（順序を変更しない）
                result = 0;
            }

            return ascending ? result : -result;
        };
    }

    /**
     * スタディの値を文字列として取得（ソート用）
     */
    private String getStudyValueAsString(com.jj.dicomviewer.model.DicomStudy study, String column) {
        try {
            switch (column) {
                case "id":
                    return study.getId() != null ? study.getId() : "";
                case "comment":
                    return study.getComment() != null ? study.getComment() : "";
                case "stateText":
                    return study.getStateText() != null ? study.getStateText().toString() : "";
                case "noFiles":
                    return study.getNumberOfImages() != null ? study.getNumberOfImages().toString() : "0";
                case "noSeries":
                    return study.getSeries() != null ? String.valueOf(study.getSeries().size()) : "0";
                case "yearOld":
                    // TODO: 年齢計算の実装
                    return "";
                case "referringPhysician":
                    return study.getReferringPhysician() != null ? study.getReferringPhysician() : "";
                case "performingPhysician":
                    return study.getPerformingPhysician() != null ? study.getPerformingPhysician() : "";
                case "institutionName":
                    return study.getInstitutionName() != null ? study.getInstitutionName() : "";
                case "dateOfBirth":
                    return study.getDateOfBirth() != null ? study.getDateOfBirth().toString() : "";
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * マトリックス初期化
     * HOROS-20240407準拠: - (void) matrixInit:(long) noOfImages (9391行目)
     */
    private void matrixInit(long noOfImages) {
        synchronized (previewPixThumbnails) {
            if (previewPix != null)
                previewPix.clear();
            if (previewPixThumbnails != null)
                previewPixThumbnails.clear();
        }
        
        synchronized (this) {
            setDCMDone = false;
            loadPreviewIndex = 0;
            
            // HOROS-20240407準拠: [self previewMatrixScrollViewFrameDidChange: nil] (9406行目)
            // oMatrixがまだ親に追加されていない可能性があるため、SwingUtilities.invokeLaterで遅延実行
            SwingUtilities.invokeLater(() -> {
                // 初期化が完了していない場合は処理をスキップ
                if (!uiInitialized) {
                    return;
                }
                if (thumbnailsScrollView == null || !thumbnailsScrollView.isDisplayable()) {
                    return;
                }
                if (bottomPreviewSplit == null || !bottomPreviewSplit.isDisplayable()) {
                    return;
                }
                previewMatrixScrollViewFrameDidChange();
            });
            
            if (oMatrix != null) {
                int rows = oMatrix.getRows();
                int columns = oMatrix.getColumns();
                if (columns < 1)
                    columns = 1;
                
                for (long i = 0; i < rows * columns; i++) {
                    int row = (int) (i / columns);
                    int col = (int) (i % columns);
                    javax.swing.JButton cell = oMatrix.cellAtRowColumn(row, col);
                    if (cell != null) {
                        cell.putClientProperty("tag", (int) i);
                        // HOROS-20240407準拠: [cell setTransparent:(i>=noOfImages)] (9415行目)
                        cell.setOpaque(i < noOfImages);
                        // HOROS-20240407準拠: [cell setEnabled:NO] (9416行目)
                        cell.setEnabled(false);
                        // HOROS-20240407準拠: [cell setFont:[NSFont systemFontOfSize: [self fontSize: @"dbMatrixFont"]]] (9417行目)
                        // HOROS-20240407準拠: [cell setImagePosition: NSImageBelow] (9418行目)
                        cell.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
                        cell.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                        // HOROS-20240407準拠: cell.title = NSLocalizedString(@"loading...", nil) (9419行目)
                        cell.setText(i < noOfImages ? "loading..." : "");
                        cell.setIcon(null);
                        // HOROS-20240407準拠: cell.bezelStyle = NSShadowlessSquareBezelStyle (9421行目)
                        // Java SwingではJButtonのデフォルトスタイルを使用
                    }
                }
            }
            
            // HOROS-20240407準拠: [imageView setPixels:nil files:nil rois:nil firstImage:0 level:0 reset:YES] (9424行目)
            // 初期起動時はプレビュー画面をクリア（真っ黒にする）
            if (imageView != null) {
                imageView.setPixels(null, null, null, (short) 0, (char) 0, true);
            }
        }
    }
    
    /**
     * サムネイルスクロールビューのフレーム変更通知
     * HOROS-20240407準拠: - (void)previewMatrixScrollViewFrameDidChange:(NSNotification*)note (10331行目)
     */
    private void previewMatrixScrollViewFrameDidChange() {
        // HOROS-20240407準拠: BrowserController.m 10331-10373行目
        // HOROS-20240407準拠: if( matrixViewArray.count == 0) return; (10333行目)
        if (matrixViewArray == null || matrixViewArray.isEmpty()) {
            return;
        }
        
        // UI初期化が完了していない場合は処理をスキップ
        // componentShownイベントが発生するまで、uiInitializedはfalseのまま
        if (!uiInitialized) {
            return;
        }
        
        if (oMatrix == null) {
            return;
        }
        
        // thumbnailsScrollViewが初期化されていない場合は処理をスキップ
        if (thumbnailsScrollView == null) {
            return;
        }
        
        // bottomPreviewSplitが初期化されていない場合は処理をスキップ
        if (bottomPreviewSplit == null) {
            return;
        }
        
        // thumbnailsScrollViewから直接JViewportを取得
        // HOROS-20240407準拠: thumbnailsScrollViewから直接JViewportを取得
        // ただし、thumbnailsScrollViewが完全に初期化されていない場合は処理をスキップ
        if (!thumbnailsScrollView.isDisplayable() || !thumbnailsScrollView.isShowing()) {
            return;
        }
        
        // JViewportを取得（初期化中に失敗する可能性があるため、NullPointerExceptionをキャッチ）
        // JScrollPaneの内部実装で、getViewport()が内部でcomp.parentを参照するため、
        // 初期化中にNullPointerExceptionが発生する可能性がある
        javax.swing.JViewport viewport = null;
        try {
            viewport = thumbnailsScrollView.getViewport();
        } catch (NullPointerException e) {
            // JViewportの内部でcompがnullの場合にエラーが発生する可能性があるため、例外をキャッチ
            // 初期化が完了していない場合は処理をスキップ
            return;
        }
        if (viewport == null) {
            return;
        }
        
        // HOROS-20240407準拠: NSInteger selectedCellTag = [oMatrix.selectedCell tag]; (10336行目)
        javax.swing.JButton selectedCell = oMatrix.selectedCell();
        int selectedCellTag = -1;
        if (selectedCell != null) {
            Object tagObj = selectedCell.getClientProperty("tag");
            if (tagObj instanceof Integer) {
                selectedCellTag = (Integer) tagObj;
            }
        }
        
        // セルサイズと間隔を取得
        java.awt.Dimension cellSize = oMatrix.getCellSize();
        if (cellSize == null) {
            cellSize = new java.awt.Dimension(105, 113); // デフォルトサイズ
        }
        
        java.awt.Dimension intercellSpacingDim = oMatrix.getIntercellSpacing();
        int intercellSpacing = intercellSpacingDim != null ? intercellSpacingDim.width : 0;
        int rcs = cellSize.width + intercellSpacing;
        
        if (rcs > 0) {
                // スクロールビューの幅を取得
                // HOROS-20240407準拠: BrowserController.m 10340行目
                // NSSize size = thumbnailsScrollView.bounds.size;
                // thumbnailsScrollViewから直接JViewportを取得（oMatrix.getParent()を呼び出す必要がない）
                if (thumbnailsScrollView != null && viewport != null) {
                    // HOROS-20240407準拠: JViewportのサイズを使用
                    // スクロールバーは別コンポーネントとしてディバイダーの右側に固定配置されるため、
                    // ここではスクロールバーの幅を考慮する必要はない
                    java.awt.Dimension size = viewport.getSize();
                    if (size == null || size.width <= 0) {
                        return;
                    }
                    int width = size.width;
                    
                    // HOROS-20240407準拠: size.width += oMatrix.intercellSpacing.width; (10341行目)
                    // BrowserController.m 10340-10345行目
                    width += intercellSpacing;
                    
                    // HOROS-20240407準拠: NSInteger hcells = (NSInteger)roundf(size.width/rcs); (10345行目)
                    int hcells = Math.round((float) width / rcs);
                    
                    if (hcells > 0) {
                        int vcells = (int) Math.ceil((double) matrixViewArray.size() / hcells);
                        
                        if (vcells < 1) {
                            vcells = 1;
                        }
                        
                        if (vcells > 0 && hcells > 0) {
                            oMatrix.renewRows(vcells, hcells);
                            
                            // 追加セルを透明・無効化
                            synchronized (previewPixThumbnails) {
                                int previewPixCount = previewPix != null ? previewPix.size() : 0;
                                for (int i = previewPixCount; i < hcells * vcells; i++) {
                                    int row = i / hcells;
                                    int col = i % hcells;
                                    javax.swing.JButton cell = oMatrix.cellAtRowColumn(row, col);
                                    if (cell != null) {
                                        cell.setOpaque(false);
                                        cell.setEnabled(false);
                                    }
                                }
                            }
                            
                            // HOROS-20240407準拠: [oMatrix sizeToCells]; (10368行目)
                            // マトリックスのサイズをセルに合わせて調整（引数なし）
                            // NSMatrixのsizeToCellsはセルのサイズと間隔に基づいてマトリックスのサイズを計算する
                            // スクロールバーの幅は自動的に考慮される（MainMenu.xib: matrix幅424px vs scrollView幅384px = 40px差）
                            oMatrix.sizeToCells();
                            
                            // マトリックスのサイズを取得
                            java.awt.Dimension matrixSize = oMatrix.getPreferredSize();
                            java.awt.Dimension viewportSize = viewport.getSize();
                            if (viewportSize == null) {
                                return;
                            }
                            
                            // マトリックスの高さとビューポートの高さを比較して、スクロールが必要かどうかを判定
                            // HOROS-20240407準拠: 垂直スクロールバーは常に表示されるが、必要に応じて有効/無効化される
                            if (thumbnailsVerticalScrollBar != null && !isAdjustingThumbnailsScrollBar) {
                                boolean needsScroll = matrixSize.height > viewportSize.height;
                                thumbnailsVerticalScrollBar.setEnabled(needsScroll);
                                
                                // スクロールバーが有効な場合、スクロール範囲を設定
                                if (needsScroll) {
                                    thumbnailsVerticalScrollBar.setMinimum(0);
                                    thumbnailsVerticalScrollBar.setMaximum((int) matrixSize.height);
                                    thumbnailsVerticalScrollBar.setVisibleAmount((int) viewportSize.height);
                                    // 現在のビューポート位置を取得して設定
                                    if (viewport instanceof javax.swing.JViewport) {
                                        java.awt.Point viewPosition = ((javax.swing.JViewport) viewport).getViewPosition();
                                        isAdjustingThumbnailsScrollBar = true;
                                        try {
                                            thumbnailsVerticalScrollBar.setValue(viewPosition.y);
                                        } finally {
                                            isAdjustingThumbnailsScrollBar = false;
                                        }
                                    }
                                } else {
                                    isAdjustingThumbnailsScrollBar = true;
                                    try {
                                        thumbnailsVerticalScrollBar.setValue(0);
                                    } finally {
                                        isAdjustingThumbnailsScrollBar = false;
                                    }
                                }
                            }
                            
                            // マトリックスのサイズを強制的に適用
                            oMatrix.revalidate();
                            oMatrix.repaint();
                            
                            // HOROS-20240407準拠: [oMatrix selectCellWithTag:selectedCellTag]; (10369行目)
                            if (selectedCellTag >= 0) {
                                oMatrix.selectCellWithTag(selectedCellTag);
                            }
                            
                            // Java Swing実装上の制約: renewRowsでセルが再生成されると内容が失われるため、
                            // セルの内容を再設定するためにmatrixDisplayIconsを呼び出す必要がある
                            // HOROS-20240407ではNSMatrixのrenewRowsが既存セルを再利用するが、
                            // Java SwingのJButtonでは再生成されるため、この処理が必要
                            // renewRowsでセルが再生成されたため、loadPreviewIndexを0にリセットして
                            // すべてのセルを再表示する
                            synchronized (previewPixThumbnails) {
                                loadPreviewIndex = 0;
                            }
                            SwingUtilities.invokeLater(() -> {
                                matrixDisplayIcons();
                            });
                        }
                    }
                }
            }
    }
    
    /**
     * サムネイルアイコンの更新
     * HOROS-20240407準拠: - (void) matrixNewIcon:(long) index :(NSManagedObject*)curFile (9428行目)
     * 
     * @param index インデックス
     * @param curFile 現在のファイル（DicomSeriesまたはDicomImage）
     */
    public void matrixNewIcon(long index, Object curFile) {
        // HOROS-20240407準拠: BrowserController.m 9428-9577行目
        long i = index;
        
        if (curFile == null) {
            if (oMatrix != null) {
                oMatrix.repaint();
            }
            return;
        }
        
        javax.swing.ImageIcon img = null;
        synchronized (previewPixThumbnails) {
            // HOROS-20240407準拠: if( i >= [previewPix count]) return; (9443行目)
            if (previewPix != null && i >= previewPix.size()) {
                return;
            }
            // HOROS-20240407準拠: if( i >= [previewPixThumbnails count]) return; (9444行目)
            if (i >= previewPixThumbnails.size()) {
                return;
            }
            
            // HOROS-20240407準拠: img = [[previewPixThumbnails objectAtIndex: i] retain]; (9446行目)
            img = previewPixThumbnails.get((int) i);
            // HOROS-20240407準拠: if( img == nil) NSLog( @"Error: [previewPixThumbnails objectAtIndex: i] == nil"); (9447行目)
            // HOROS-20240407準拠: imgがnilでも処理を続行するが、HOROS-20240407ではnotFoundImageが設定されているはず
            // 念のため、imgがnullの場合はnotFoundImageを使用（HOROS-20240407準拠: 5083行目でnotFoundImageが追加されている）
            if (img == null) {
                img = notFoundImage;
            }
        }
        
        try {
            String modality = null;
            String seriesSOPClassUID = null;
            String fileType = null;
            String name = null;
            
            // HOROS-20240407準拠: ファイルタイプに応じてmodality等を取得
            if (curFile instanceof com.jj.dicomviewer.model.DicomImage) {
                com.jj.dicomviewer.model.DicomImage image = (com.jj.dicomviewer.model.DicomImage) curFile;
                modality = image.getModality();
                if (image.getSeries() != null) {
                    seriesSOPClassUID = image.getSeries().getSeriesSOPClassUID();
                }
                fileType = image.getFileType();
                // HOROS-20240407準拠: DicomImageにはnameがないため、series nameを使用
                if (image.getSeries() != null) {
                    name = image.getSeries().getName();
                }
            } else if (curFile instanceof com.jj.dicomviewer.model.DicomSeries) {
                com.jj.dicomviewer.model.DicomSeries series = (com.jj.dicomviewer.model.DicomSeries) curFile;
                seriesSOPClassUID = series.getSeriesSOPClassUID();
                if (series.getImages() != null && !series.getImages().isEmpty()) {
                    // HOROS-20240407準拠: Setから最初の要素を取得
                    java.util.Iterator<com.jj.dicomviewer.model.DicomImage> it = series.getImages().iterator();
                    if (it.hasNext()) {
                        com.jj.dicomviewer.model.DicomImage firstImage = it.next();
                        modality = firstImage.getModality();
                        fileType = firstImage.getFileType();
                    }
                }
                name = series.getName();
            }
            
            // HOROS-20240407準拠: if( img || [modality  hasPrefix: @"RT"]) (9482行目)
            // HOROS-20240407準拠: imgがnilでもRTモダリティの場合は処理を続行
            if (img != null || (modality != null && modality.startsWith("RT"))) {
                if (oMatrix != null) {
                    int rows = oMatrix.getRows();
                    int cols = oMatrix.getColumns();
                    if (cols < 1) {
                        cols = 1;
                    }
                    
                    int row = (int) (i / cols);
                    int col = (int) (i % cols);
                    javax.swing.JButton cell = oMatrix.cellAtRowColumn(row, col);
                    
                    if (cell != null) {
                        // HOROS-20240407準拠: セルの設定
                        // HOROS-20240407準拠: [cell setLineBreakMode: NSLineBreakByCharWrapping];
                        // HOROS-20240407準拠: [cell setFont:[NSFont systemFontOfSize: [self fontSize: @"dbMatrixFont"]]];
                        // HOROS-20240407準拠: [cell setRepresentedObject: [curFile objectID]];
                        // HOROS-20240407準拠: [cell setImagePosition: NSImageBelow];
                        // HOROS-20240407準拠: [cell setTransparent:NO];
                        // HOROS-20240407準拠: [cell setEnabled:YES];
                        // HOROS-20240407準拠: [cell setButtonType:NSPushOnPushOffButton];
                        // HOROS-20240407準拠: [cell setBezelStyle:NSShadowlessSquareBezelStyle];
                        // HOROS-20240407準拠: [cell setImageScaling:NSImageScaleProportionallyDown];
                        // HOROS-20240407準拠: [cell setBordered:YES]; (9501行目)
                        // HOROS-20240407準拠: [cell setTransparent:NO]; (9493行目)
                        cell.setOpaque(true);
                        cell.setContentAreaFilled(true);
                        cell.setBorderPainted(true);
                        cell.setEnabled(true);
                        
                        // HOROS-20240407準拠: 画像のスケーリング（HOROS-20240407準拠: BrowserController.m 9594-9608行目）
                        // HOROS-20240407準拠: switch( [[NSUserDefaults standardUserDefaults] integerForKey: @"dbFontSize"])
                        // case -1: [cell setImage: [img imageByScalingProportionallyUsingNSImage: 0.6]];
                        // case 0: [cell setImage: img];
                        // case 1: [cell setImage: [img imageByScalingProportionallyUsingNSImage: 1.3]];
                        javax.swing.ImageIcon scaledImg = img;
                        // TODO: dbFontSizeの設定を確認（デフォルトは0）
                        int dbFontSize = 0; // TODO: UserDefaultsから取得
                        if (img != null) {
                            if (dbFontSize == -1) {
                                // HOROS-20240407準拠: 0.6倍にスケーリング
                                scaledImg = scaleImageIcon(img, 0.6);
                            } else if (dbFontSize == 1) {
                                // HOROS-20240407準拠: 1.3倍にスケーリング
                                scaledImg = scaleImageIcon(img, 1.3);
                            }
                        }
                        // HOROS-20240407準拠: imgがnullの場合はnotFoundImageを使用（HOROS-20240407準拠: 5083行目でnotFoundImageが追加されている）
                        if (scaledImg == null) {
                            scaledImg = notFoundImage;
                        }
                        // HOROS-20240407準拠: セルタイトルの設定（HOROS-20240407準拠: BrowserController.m 9513-9577行目）
                        
                        // HOROS-20240407準拠: NSString *name = [curFile valueForKey:@"name"]; (9513行目)
                        // HOROS-20240407準拠: if( name == nil) name = @""; (9515-9516行目)
                        if (name == null) {
                            name = "";
                        }
                        
                        // HOROS-20240407準拠: if( name.length > 18) (9518行目)
                        if (name.length() > 18) {
                            // HOROS-20240407準拠: [cell setFont:[NSFont systemFontOfSize: [self fontSize: @"dbSmallMatrixFont"]]]; (9520行目)
                            // HOROS-20240407準拠: name = [name stringByTruncatingToLength: 36]; // 2 lines (9521行目)
                            name = name.substring(0, Math.min(36, name.length())); // 2行分
                        }
                        
                        // HOROS-20240407準拠: if( name.length == 0) name = modality; (9524-9525行目)
                        if (name.length() == 0 && modality != null) {
                            name = modality;
                        }
                        
                        String title = "";
                        // HOROS-20240407準拠: if ( [modality hasPrefix: @"RT"]) (9527行目)
                        if (modality != null && modality.startsWith("RT")) {
                            // HOROS-20240407準拠: [cell setTitle: [NSString stringWithFormat: @"%@\r%@", name, modality]]; (9529行目)
                            // Java Swingでは\nを使用（\rは改行として認識されない）
                            title = name + "\n" + modality;
                        } else if ("DICOMMPEG2".equals(fileType)) {
                            // HOROS-20240407準拠: MPEG-2シリーズ (9531-9535行目)
                            int count = 0;
                            if (curFile instanceof com.jj.dicomviewer.model.DicomSeries) {
                                com.jj.dicomviewer.model.DicomSeries series = (com.jj.dicomviewer.model.DicomSeries) curFile;
                                count = series.getImages() != null ? series.getImages().size() : 0;
                            }
                            // Java Swingでは\nを使用
                            title = String.format("MPEG-2 Series\n%s\n%d Images", name, count);
                            // TODO: MPEG-2アイコンの設定
                        } else if (curFile instanceof com.jj.dicomviewer.model.DicomSeries) {
                            // HOROS-20240407準拠: Seriesレベルのタイトル (9537-9571行目)
                            com.jj.dicomviewer.model.DicomSeries series = (com.jj.dicomviewer.model.DicomSeries) curFile;
                            // HOROS-20240407準拠: int count = [[curFile valueForKey:@"noFiles"] intValue]; (9539行目)
                            // HOROS-20240407準拠: noFilesはnumberOfImagesを返す (DicomSeries.m 578-580行目)
                            int count = series.getNumberOfImages() != null ? series.getNumberOfImages() : 0;
                            
                            // HOROS-20240407準拠: シリーズタイプの判定 (9540-9569行目)
                            String singleType = "Image";
                            String pluralType = "Images";
                            
                            if (seriesSOPClassUID != null && (seriesSOPClassUID.contains("StructuredReport") || seriesSOPClassUID.contains("PDF"))) {
                                // HOROS-20240407準拠: StructuredReportまたはPDFの場合 (9542-9548行目)
                                if (count <= 1 && series.getImages() != null && !series.getImages().isEmpty()) {
                                    java.util.Iterator<com.jj.dicomviewer.model.DicomImage> it = series.getImages().iterator();
                                    if (it.hasNext()) {
                                        com.jj.dicomviewer.model.DicomImage firstImage = it.next();
                                        if (firstImage.getNumberOfFrames() != null && firstImage.getNumberOfFrames() >= 1) {
                                            count = firstImage.getNumberOfFrames();
                                        }
                                    }
                                }
                                singleType = "Page";
                                pluralType = "Pages";
                            } else if (count == 1 && series.getImages() != null && !series.getImages().isEmpty()) {
                                // HOROS-20240407準拠: count == 1 かつ numberOfFrames > 1 の場合 (9550-9555行目)
                                java.util.Iterator<com.jj.dicomviewer.model.DicomImage> it = series.getImages().iterator();
                                if (it.hasNext()) {
                                    com.jj.dicomviewer.model.DicomImage firstImage = it.next();
                                    if (firstImage.getNumberOfFrames() != null && firstImage.getNumberOfFrames() > 1) {
                                        count = firstImage.getNumberOfFrames();
                                        singleType = "Frame";
                                        pluralType = "Frames";
                                    }
                                }
                            } else if (count == 0) {
                                // HOROS-20240407準拠: count == 0 の場合 (9556-9564行目)
                                // HOROS-20240407準拠: count = [[curFile valueForKey: @"rawNoFiles"] intValue]; (9558行目)
                                // TODO: rawNoFilesの実装が必要（現在はnumberOfImagesを使用）
                                if (series.getImages() != null && !series.getImages().isEmpty()) {
                                    java.util.Iterator<com.jj.dicomviewer.model.DicomImage> it = series.getImages().iterator();
                                    if (it.hasNext()) {
                                        com.jj.dicomviewer.model.DicomImage firstImage = it.next();
                                        if (firstImage.getNumberOfFrames() != null && firstImage.getNumberOfFrames() >= 1) {
                                            count = firstImage.getNumberOfFrames();
                                        }
                                    }
                                }
                                singleType = "Object";
                                pluralType = "Objects";
                            }
                            
                            // HOROS-20240407準拠: [cell setTitle:[NSString stringWithFormat: @"%@\r%@", name, N2LocalizedSingularPluralCount(count, singleType, pluralType)]]; (9571行目)
                            // HOROS-20240407準拠: \rで改行（2行表示）
                            // HOROS-20240407準拠: N2LocalizedSingularPluralCountは数値をローカライズして表示 (N2Stuff.h 45行目)
                            // HOROS-20240407準拠: N2LocalizedSingularPluralCount(c, s, p) = [NSString stringWithFormat:@"%@ %@", [NSNumberFormatter localizedStringFromNumber:[NSNumber numberWithInteger:(NSInteger)c] numberStyle:NSNumberFormatterDecimalStyle], (c == 1? s : p)]
                            // Java Swingでは\nを使用（\rは改行として認識されない）
                            String countStr = n2LocalizedSingularPluralCount(count, singleType, pluralType);
                            title = name + "\n" + countStr;
                        } else if (curFile instanceof com.jj.dicomviewer.model.DicomImage) {
                            // HOROS-20240407準拠: Imageレベルのタイトル (9573-9580行目)
                            if (seriesSOPClassUID != null && (seriesSOPClassUID.contains("StructuredReport") || seriesSOPClassUID.contains("PDF"))) {
                                // HOROS-20240407準拠: [cell setTitle:[NSString stringWithFormat:NSLocalizedString(@"Page %d", nil), i+1]]; (9576行目)
                                title = String.format("Page %d", (int) i + 1);
                            } else {
                                // HOROS-20240407準拠: sliceLocationの確認（TODO: 実装が必要）
                                // HOROS-20240407準拠: [cell setTitle:[NSString stringWithFormat:NSLocalizedString(@"Image %d", nil), i+1]]; (9580行目)
                                title = String.format("Image %d", (int) i + 1);
                            }
                        }
                        
                        // HOROS-20240407準拠: [cell setFont:[NSFont systemFontOfSize: [self fontSize: @"dbMatrixFont"]]]; (9488行目)
                        // HOROS-20240407準拠: dbMatrixFont = 8 (Small), 9 (Regular), 13 (Large) (BrowserController.m 389-475行目)
                        // HOROS-20240407準拠: フォントサイズを設定してテキストが2行表示できるようにする
                        // HOROS-20240407準拠: dbFontSizeは既に定義されている（3624行目）ので、それを使用
                        int fontSize = 10; // HOROS-20240407準拠: Regular mode = 9 → 1つ大きくして10
                        if (dbFontSize == -1) {
                            fontSize = 9; // HOROS-20240407準拠: Small mode = 8 → 1つ大きくして9
                        } else if (dbFontSize == 1) {
                            fontSize = 14; // HOROS-20240407準拠: Large mode = 13 → 1つ大きくして14
                        }
                        // HOROS-20240407準拠: name.length > 18 の場合は dbSmallMatrixFont を使用 (9520行目)
                        if (name != null && name.length() > 18) {
                            fontSize = (dbFontSize == -1) ? 8 : (dbFontSize == 1) ? 9 : 9; // HOROS-20240407準拠: dbSmallMatrixFont → 1つ大きく
                        }
                        cell.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, fontSize));
                        
                        // HOROS-20240407準拠: [cell setImagePosition: NSImageBelow]; (9492行目)
                        // HOROS-20240407準拠: 順序: setImagePosition → setTitle → setImage (9492, 9529/9571, 9601行目)
                        // NSImageBelow = 画像を下に、テキストを上に配置
                        // Java Swingでは、setVerticalTextPosition(TOP)でテキストを上に配置し、アイコンを下に配置
                        cell.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
                        cell.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                        cell.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                        cell.setIconTextGap(4);
                        
                        // HOROS-20240407準拠: [cell setTitle: ...]; (9529/9571行目)
                        // HOROS-20240407準拠: テキストを先に設定（setImagePositionの後、setImageの前）
                        // HOROS-20240407準拠: 2行表示（\rで改行、Java SwingではHTMLタグを使用して2行表示）
                        // HOROS-20240407準拠: Java SwingのJButtonで複数行テキストを表示するにはHTMLタグを使用
                        if (title.contains("\n")) {
                            // 2行表示の場合、HTMLタグを使用
                            String htmlTitle = "<html><center>" + title.replace("\n", "<br>") + "</center></html>";
                            cell.setText(htmlTitle);
                        } else {
                            cell.setText(title);
                        }
                        
                        // HOROS-20240407準拠: [cell setImage: img]; (9601行目)
                        // HOROS-20240407準拠: アイコンを最後に設定
                        System.out.println("[DEBUG] matrixNewIcon() - setting icon for index " + i + ", scaledImg: " + (scaledImg != null ? "not null" : "null") + ", img: " + (img != null ? "not null" : "null"));
                        cell.setIcon(scaledImg);
                        System.out.println("[DEBUG] matrixNewIcon() - icon set, cell.getIcon(): " + (cell.getIcon() != null ? "not null" : "null"));
                        
                        // HOROS-20240407準拠: [cell setEnabled:YES]; (9494行目)
                        cell.setEnabled(true);
                        
                        cell.putClientProperty("file", curFile);
                        cell.putClientProperty("tag", (int) i);
                        
                        // Weasis実装参考: セルの表示を更新
                        // Java Swingでは、アイコンとテキストを設定した後、明示的に再検証と再描画が必要
                        // renewRowsで作成されたセルの初期設定を上書きするため、明示的に設定
                        cell.invalidate();
                        cell.revalidate();
                        cell.repaint();
                        
                        // HOROS-20240407準拠: マトリックスの表示を更新
                        if (oMatrix != null) {
                            oMatrix.invalidate();
                            oMatrix.revalidate();
                            oMatrix.repaint();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // HOROS-20240407準拠: エラーが発生した場合はスキップ
            // デバッグログは削除（HOROS-20240407準拠）
        }
    }
    
    /**
     * 単数形/複数形をローカライズして表示
     * HOROS-20240407準拠: N2LocalizedSingularPluralCount (N2Stuff.h 45行目)
     * HOROS-20240407準拠: #define N2LocalizedSingularPluralCount(c, s, p) [NSString stringWithFormat:@"%@ %@", [NSNumberFormatter localizedStringFromNumber:[NSNumber numberWithInteger:(NSInteger)c] numberStyle:NSNumberFormatterDecimalStyle], (c == 1? s : p)]
     * 
     * @param count 数値
     * @param singleType 単数形（例: "Image"）
     * @param pluralType 複数形（例: "Images"）
     * @return ローカライズされた文字列（例: "1 Image" または "3 Images"）
     */
    private String n2LocalizedSingularPluralCount(int count, String singleType, String pluralType) {
        // HOROS-20240407準拠: 数値をローカライズして表示
        java.text.NumberFormat formatter = java.text.NumberFormat.getIntegerInstance();
        String countStr = formatter.format(count);
        
        // HOROS-20240407準拠: count == 1 の場合は単数形、それ以外は複数形
        String type = (count == 1) ? singleType : pluralType;
        
        // HOROS-20240407準拠: "%@ %@" の形式で結合
        return countStr + " " + type;
    }
    
    /**
     * 画像アイコンをスケーリング
     * HOROS-20240407準拠: [img imageByScalingProportionallyUsingNSImage: scale]
     * 
     * @param icon 元のアイコン
     * @param scale スケール（0.6, 1.0, 1.3など）
     * @return スケーリングされたアイコン
     */
    private javax.swing.ImageIcon scaleImageIcon(javax.swing.ImageIcon icon, double scale) {
        if (icon == null) {
            return null;
        }
        
        java.awt.Image originalImage = icon.getImage();
        int newWidth = (int) (originalImage.getWidth(null) * scale);
        int newHeight = (int) (originalImage.getHeight(null) * scale);
        
        if (newWidth <= 0 || newHeight <= 0) {
            return icon;
        }
        
        // HOROS-20240407準拠: 比例スケーリング
        java.awt.Image scaledImage = originalImage.getScaledInstance(
            newWidth, 
            newHeight, 
            java.awt.Image.SCALE_SMOOTH
        );
        
        return new javax.swing.ImageIcon(scaledImage);
    }
    
    /**
     * サムネイル読み込みスレッドを開始
     * HOROS-20240407準拠: BrowserController.m 5105-5158行目
     * 
     * @param files 画像ファイルのリスト
     * @param imageLevel イメージレベルかどうか
     */
    private void startMatrixLoadIconsThread(List<Object> files, boolean imageLevel) {
        System.out.println("[DEBUG] startMatrixLoadIconsThread() - files.size(): " + files.size() + ", imageLevel: " + imageLevel);
        // HOROS-20240407準拠: 既存のスレッドをキャンセル
        synchronized (previewPixThumbnails) {
            if (matrixLoadIconsThread != null && matrixLoadIconsThread.isAlive()) {
                // TODO: スレッドのキャンセル処理（Javaではinterruptを使用）
                matrixLoadIconsThread.interrupt();
            }
            
            // HOROS-20240407準拠: スレッドで実行するデータを準備
            // シリーズレベルの場合、サムネイルが5個未満ならメインスレッドで実行
            boolean separateThread = true;
            if (!imageLevel) {
                int thumbnailsToGenerate = 0;
                for (Object file : files) {
                    if (file instanceof com.jj.dicomviewer.model.DicomSeries) {
                        com.jj.dicomviewer.model.DicomSeries series = (com.jj.dicomviewer.model.DicomSeries) file;
                        if (series.getThumbnail() == null) {
                            thumbnailsToGenerate++;
                        }
                    }
                }
                
                if (thumbnailsToGenerate < 5) {
                    separateThread = false;
                }
            }
            
            if (separateThread) {
                // HOROS-20240407準拠: 別スレッドで実行
                final List<Object> filesCopy = new ArrayList<>(files);
                final boolean imageLevelCopy = imageLevel;
                final Object context = previewPix; // コンテキストとしてpreviewPixを使用
                
                matrixLoadIconsThread = new Thread(() -> {
                    try {
                        matrixLoadIcons(filesCopy, imageLevelCopy, context);
                    } catch (Exception e) {
                        // HOROS-20240407準拠: デバッグログは削除
                    }
                }, "matrixLoadIcons");
                matrixLoadIconsThread.start();
            } else {
                // HOROS-20240407準拠: メインスレッドで実行
                matrixLoadIcons(files, imageLevel, previewPix);
            }
        }
    }
    
    /**
     * サムネイルを読み込む
     * HOROS-20240407準拠: - (void)matrixLoadIcons: (NSDictionary*)dict (10021行目)
     * 
     * @param files 画像ファイルのリスト
     * @param imageLevel イメージレベルかどうか
     * @param context コンテキスト（previewPix）
     */
    private void matrixLoadIcons(List<Object> files, boolean imageLevel, Object context) {
        System.out.println("[DEBUG] matrixLoadIcons() - files.size(): " + files.size() + ", imageLevel: " + imageLevel);
        // HOROS-20240407準拠: BrowserController.m 10021-10170行目
        try {
            List<javax.swing.ImageIcon> tempPreviewPixThumbnails = null;
            List<com.jj.dicomviewer.model.DicomPix> tempPreviewPix = null;
            
            
            synchronized (previewPixThumbnails) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                
                // HOROS-20240407準拠: tempPreviewPixThumbnails = [[previewPixThumbnails mutableCopy] autorelease]; (10046行目)
                // HOROS-20240407準拠: tempPreviewPix = [[previewPix mutableCopy] autorelease]; (10047行目)
                // HOROS-20240407準拠: previewPixThumbnailsには既にnotFoundImageが追加されている（5083行目）
                tempPreviewPixThumbnails = new ArrayList<>(previewPixThumbnails);
                if (previewPix != null) {
                    tempPreviewPix = new ArrayList<>(previewPix);
                } else {
                    tempPreviewPix = new ArrayList<>();
                }
                
                // HOROS-20240407準拠: files.size()分のサイズを確保（notFoundImageで埋める）
                while (tempPreviewPixThumbnails.size() < files.size()) {
                    tempPreviewPixThumbnails.add(notFoundImage);
                }
                while (tempPreviewPix.size() < files.size()) {
                    tempPreviewPix.add(null);
                }
            }
            
            // HOROS-20240407準拠: NSTimeInterval now = [NSDate timeIntervalSinceReferenceDate];
            // HOROS-20240407準拠: if (now-_timeIntervalOfLastLoadIconsDisplayIcons > 0.5)
            // インスタンス変数を使用（HOROS-20240407準拠: BrowserController.h 261行目）
            
            for (int i = 0; i < files.size(); i++) {
                try {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    
                    // HOROS-20240407準拠: if( i != 0)
                    if (i != 0) {
                        // HOROS-20240407準拠: // only do it on a delayed basis
                        // HOROS-20240407準拠: NSTimeInterval now = [NSDate timeIntervalSinceReferenceDate];
                        long now = System.currentTimeMillis();
                        // HOROS-20240407準拠: if (now-_timeIntervalOfLastLoadIconsDisplayIcons > 0.5)
                        if (now - timeIntervalOfLastLoadIconsDisplayIcons > 500) {
                            // HOROS-20240407準拠: _timeIntervalOfLastLoadIconsDisplayIcons = now;
                            timeIntervalOfLastLoadIconsDisplayIcons = now;
                            
                            synchronized (previewPixThumbnails) {
                                if (!Thread.currentThread().isInterrupted()) {
                                    if (previewPix == context) {
                                        previewPixThumbnails.clear();
                                        previewPixThumbnails.addAll(tempPreviewPixThumbnails);
                                        
                                        if (previewPix != null) {
                                            previewPix.clear();
                                            previewPix.addAll(tempPreviewPix);
                                        }
                                    }
                                }
                            }
                            
                            if (!Thread.currentThread().isInterrupted()) {
                                SwingUtilities.invokeLater(() -> {
                                    matrixDisplayIcons();
                                });
                            }
                        }
                    }
                    
                    // HOROS-20240407準拠: DicomImage* image = [idatabase objectWithID:[objectIDs objectAtIndex:i]];
                    // HOROS-20240407準拠: if (!image) break; // the objects don't exist anymore, the selection has very likely changed after this call
                    Object fileObj = files.get(i);
                    if (fileObj == null) {
                        break;
                    }
                    
                    // HOROS-20240407準拠: サムネイルを生成
                    javax.swing.ImageIcon thumbnail = null;
                    com.jj.dicomviewer.model.DicomPix pix = null;
                    
                    if (fileObj instanceof com.jj.dicomviewer.model.DicomImage) {
                        com.jj.dicomviewer.model.DicomImage image = (com.jj.dicomviewer.model.DicomImage) fileObj;
                        
                        // HOROS-20240407準拠: int frame = 0;
                        // HOROS-20240407準拠: if (image.numberOfFrames.intValue > 1) frame = image.numberOfFrames.intValue/2;
                        // HOROS-20240407準拠: if (image.frameID) frame = image.frameID.intValue;
                        int frame = 0;
                        if (image.getNumberOfFrames() != null && image.getNumberOfFrames() > 1) {
                            frame = image.getNumberOfFrames() / 2;
                        }
                        if (image.getFrameID() != null) {
                            frame = image.getFrameID();
                        }
                        
                        // HOROS-20240407準拠: DCMPix* dcmPix = [self getDCMPixFromViewerIfAvailable:image.completePath frameNumber: frame];
                        // HOROS-20240407準拠: if (dcmPix == nil) dcmPix = [[[DCMPix alloc] initWithPath:image.completePath :0 :1 :nil :frame :0 isBonjour:![idatabase isLocal] imageObj: image] autorelease];
                        // TODO: getDCMPixFromViewerIfAvailableの実装
                        String completePath = image.getCompletePath();
                        String pathValue = image.path();
                        System.out.println("[DEBUG] matrixLoadIcons() - image " + i + ", completePath: " + completePath + ", path(): " + pathValue + ", pathNumber: " + image.getPathNumber() + ", pathString: " + image.getPathString() + ", inDatabaseFolder: " + image.getInDatabaseFolder());
                        if (completePath == null || completePath.isEmpty()) {
                            System.err.println("[ERROR] matrixLoadIcons() - completePath is null or empty for image " + i);
                            // HOROS-20240407準拠: エラー時はnotFoundImageを使用
                            while (tempPreviewPixThumbnails.size() <= i) {
                                tempPreviewPixThumbnails.add(null);
                            }
                            tempPreviewPixThumbnails.set(i, notFoundImage);
                            while (tempPreviewPix.size() <= i) {
                                tempPreviewPix.add(null);
                            }
                            tempPreviewPix.set(i, null);
                            continue;
                        }
                        // ファイルの存在確認
                        java.io.File file = new java.io.File(completePath);
                        if (!file.exists()) {
                            System.err.println("[ERROR] matrixLoadIcons() - file does not exist: " + completePath);
                        } else {
                            System.out.println("[DEBUG] matrixLoadIcons() - file exists: " + completePath);
                        }
                        if (completePath != null && !completePath.isEmpty()) {
                            try {
                                pix = new com.jj.dicomviewer.model.DicomPix(
                                    completePath,
                                    0, // imageIndex
                                    1, // numberOfImages
                                    frame, // frameNumber
                                    0, // seriesId
                                    false, // isBonjour (TODO: database.isLocal()の確認)
                                    image // imageObj
                                );
                            } catch (Exception e) {
                                // HOROS-20240407準拠: エラー時はnotFoundImageを使用
                                // JPEG圧縮画像（jpeg-cv）の場合はエラーログを抑制
                                if (e.getMessage() != null && e.getMessage().contains("jpeg-cv")) {
                                    // デバッグログは出力しない（JPEG圧縮画像はdcm4che-imageio-opencvが必要）
                                } else {
                                    System.err.println("[ERROR] matrixLoadIcons() - failed to create DicomPix for image " + i + ", completePath: " + completePath);
                                    e.printStackTrace();
                                }
                                pix = null;
                            }
                        }
                        
                        // HOROS-20240407準拠: if (!imageLevel)
                        if (!imageLevel) {
                            // HOROS-20240407準拠: NSData* dbThmb = image.series.thumbnail;
                            // HOROS-20240407準拠: if (dbThmb)
                            if (image.getSeries() != null) {
                                byte[] dbThmb = image.getSeries().getThumbnail();
                                if (dbThmb != null) {
                                    // HOROS-20240407準拠: NSImageRep* rep = [[[NSBitmapImageRep alloc] initWithData:dbThmb] autorelease];
                                    // HOROS-20240407準拠: NSImage* dbIma = [[[NSImage alloc] initWithSize:[rep size]] autorelease];
                                    // HOROS-20240407準拠: [dbIma addRepresentation:rep];
                                    try {
                                        java.awt.Image img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(dbThmb));
                                        thumbnail = new javax.swing.ImageIcon(img);
                                        
                                        // HOROS-20240407準拠: DCMPix *pix = (dcmPix? dcmPix : [[[DCMPix alloc] myinitEmpty] autorelease]);
                                        if (pix == null) {
                                            // TODO: DicomPixの空初期化
                                            pix = null; // 後で実装
                                        }
                                        
                                        // HOROS-20240407準拠: [tempPreviewPixThumbnails replaceObjectAtIndex: i withObject: dbIma];
                                        // HOROS-20240407準拠: [tempPreviewPix addObject: pix];
                                        // HOROS-20240407準拠: continue;
                                        // 配列に追加（後で処理）
                                        while (tempPreviewPixThumbnails.size() <= i) {
                                            tempPreviewPixThumbnails.add(null);
                                        }
                                        tempPreviewPixThumbnails.set(i, thumbnail);
                                        
                                        while (tempPreviewPix.size() <= i) {
                                            tempPreviewPix.add(null);
                                        }
                                        tempPreviewPix.set(i, pix);
                                        
                                        continue; // HOROS-20240407準拠: continue
                                    } catch (Exception e) {
                                        // HOROS-20240407準拠: デバッグログは削除
                                    }
                                }
                            }
                        }
                        
                        // HOROS-20240407準拠: if (dcmPix)
                        if (pix != null) {
                            // HOROS-20240407準拠: if ([DCMAbstractSyntaxUID isStructuredReport:image.series.seriesSOPClassUID] || [DCMAbstractSyntaxUID isPDF:image.series.seriesSOPClassUID])
                            String seriesSOPClassUID = null;
                            if (image.getSeries() != null) {
                                seriesSOPClassUID = image.getSeries().getSeriesSOPClassUID();
                            }
                            
                            if (seriesSOPClassUID != null && (seriesSOPClassUID.contains("StructuredReport") || seriesSOPClassUID.contains("PDF"))) {
                                // HOROS-20240407準拠: NSImage *icon = [[NSWorkspace sharedWorkspace] iconForFileType: @"txt"];
                                // HOROS-20240407準拠: NSImage *thumbnail = [[[NSImage alloc] initWithSize: NSMakeSize( THUMBNAILSIZE, THUMBNAILSIZE)] autorelease];
                                // HOROS-20240407準拠: [thumbnail lockFocus];
                                // HOROS-20240407準拠: [icon drawInRect: NSMakeRect( 0, 0, THUMBNAILSIZE, THUMBNAILSIZE) fromRect: [icon alignmentRect] operation: NSCompositeCopy fraction: 1.0];
                                // HOROS-20240407準拠: [thumbnail unlockFocus];
                                // TODO: テキストファイルアイコンの生成
                                thumbnail = notFoundImage;
                                
                                // HOROS-20240407準拠: [tempPreviewPixThumbnails replaceObjectAtIndex: i withObject: thumbnail];
                                // HOROS-20240407準拠: [tempPreviewPix addObject: dcmPix];
                                while (tempPreviewPixThumbnails.size() <= i) {
                                    tempPreviewPixThumbnails.add(null);
                                }
                                tempPreviewPixThumbnails.set(i, thumbnail);
                                
                                while (tempPreviewPix.size() <= i) {
                                    tempPreviewPix.add(null);
                                }
                                tempPreviewPix.set(i, pix);
                                
                                continue; // HOROS-20240407準拠: continue
                            } else {
                                // HOROS-20240407準拠: NSImage* thumbnail = [dcmPix generateThumbnailImageWithWW:image.series.windowWidth.floatValue WL:image.series.windowLevel.floatValue];
                                float ww = 0.0f;
                                float wl = 0.0f;
                                if (image.getSeries() != null) {
                                    if (image.getSeries().getWindowWidth() != null) {
                                        ww = image.getSeries().getWindowWidth().floatValue();
                                    }
                                    if (image.getSeries().getWindowLevel() != null) {
                                        wl = image.getSeries().getWindowLevel().floatValue();
                                    }
                                }
                                
                                // HOROS-20240407準拠: Window Level/Widthを設定
                                pix.setWindowLevelWidth(wl, ww);
                                
                                // HOROS-20240407準拠: サムネイルを生成
                                System.out.println("[DEBUG] matrixLoadIcons() - generating thumbnail for image " + i);
                                // HOROS-20240407準拠: THUMBNAILSIZE = 70 (DicomSeries.h 42行目)
                                // HOROS-20240407準拠: サムネイルは70x70で生成される
                                java.awt.image.BufferedImage thumbBuff = pix.generateThumbnail(70, 70); // HOROS-20240407準拠: THUMBNAILSIZE
                                if (thumbBuff != null) {
                                    thumbnail = new javax.swing.ImageIcon(thumbBuff);
                                    System.out.println("[DEBUG] matrixLoadIcons() - thumbnail generated successfully for image " + i + ", size: " + thumbBuff.getWidth() + "x" + thumbBuff.getHeight());
                                } else {
                                    System.out.println("[DEBUG] matrixLoadIcons() - thumbnail generation failed for image " + i + ", using notFoundImage");
                                    thumbnail = notFoundImage;
                                }
                                
                                // HOROS-20240407準拠: [dcmPix revert:NO];	// <- Kill the raw data
                                pix.revert(false);
                                
                                // HOROS-20240407準拠: if (thumbnail == nil || dcmPix.notAbleToLoadImage == YES) thumbnail = notFoundImage;
                                // TODO: notAbleToLoadImageの確認
                                
                                // HOROS-20240407準拠: [tempPreviewPixThumbnails replaceObjectAtIndex: i withObject: thumbnail];
                                // HOROS-20240407準拠: [tempPreviewPix addObject: dcmPix];
                                while (tempPreviewPixThumbnails.size() <= i) {
                                    tempPreviewPixThumbnails.add(null);
                                }
                                tempPreviewPixThumbnails.set(i, thumbnail);
                                
                                while (tempPreviewPix.size() <= i) {
                                    tempPreviewPix.add(null);
                                }
                                tempPreviewPix.set(i, pix);
                                
                                continue; // HOROS-20240407準拠: continue
                            }
                        }
                    } else if (fileObj instanceof com.jj.dicomviewer.model.DicomSeries) {
                        // HOROS-20240407準拠: シリーズレベルの処理
                        com.jj.dicomviewer.model.DicomSeries series = (com.jj.dicomviewer.model.DicomSeries) fileObj;
                        
                        // HOROS-20240407準拠: DBに保存されたサムネイルを使用
                        byte[] dbThmb = series.getThumbnail();
                        if (dbThmb != null) {
                            try {
                                java.awt.Image img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(dbThmb));
                                thumbnail = new javax.swing.ImageIcon(img);
                            } catch (Exception e) {
                                // HOROS-20240407準拠: デバッグログは削除
                            }
                        }
                        
                        // HOROS-20240407準拠: サムネイルがなければ生成
                        if (thumbnail == null) {
                            // TODO: DCMPix/DicomPixを使用してサムネイルを生成
                            // 現在はプレースホルダー
                            thumbnail = notFoundImage;
                        }
                        
                        // HOROS-20240407準拠: 配列に追加
                        while (tempPreviewPixThumbnails.size() <= i) {
                            tempPreviewPixThumbnails.add(null);
                        }
                        tempPreviewPixThumbnails.set(i, thumbnail);
                        
                        while (tempPreviewPix.size() <= i) {
                            tempPreviewPix.add(null);
                        }
                        tempPreviewPix.set(i, pix);
                    }
                }
                // HOROS-20240407準拠: @catch (NSException* e)
                catch (Exception e) {
                    // HOROS-20240407準拠: N2LogExceptionWithStackTrace(e);
                    // デバッグログは削除（HOROS-20240407準拠）
                }
                // HOROS-20240407準拠: // successful iterations don't execute this (they continue to the next iteration), this is in case no image has been provided by this iteration (exception, no file, ...)
                // HOROS-20240407準拠: [tempPreviewPixThumbnails replaceObjectAtIndex: i withObject: notFoundImage]; (10146行目)
                // HOROS-20240407準拠: [tempPreviewPix addObject: [[[DCMPix alloc] myinitEmpty] autorelease]]; (10147行目)
                // エラー時はnotFoundImageを使用（HOROS-20240407準拠）
                // HOROS-20240407準拠: replaceObjectAtIndexで置き換える（既にnotFoundImageが入っているはず）
                while (tempPreviewPixThumbnails.size() <= i) {
                    tempPreviewPixThumbnails.add(notFoundImage);
                }
                tempPreviewPixThumbnails.set(i, notFoundImage);
                
                // TODO: DicomPixの空初期化
                while (tempPreviewPix.size() <= i) {
                    tempPreviewPix.add(null);
                }
                if (tempPreviewPix.get(i) == null) {
                    tempPreviewPix.set(i, null); // 後で実装
                }
            }
            
            // HOROS-20240407準拠: @synchronized( previewPixThumbnails)
            synchronized (previewPixThumbnails) {
                // HOROS-20240407準拠: if( [[NSThread currentThread] isCancelled] == NO)
                if (!Thread.currentThread().isInterrupted()) {
                    // HOROS-20240407準拠: if( previewPix == context)
                    if (previewPix == context) {
                        System.out.println("[DEBUG] matrixLoadIcons() - updating previewPixThumbnails, tempPreviewPixThumbnails.size(): " + tempPreviewPixThumbnails.size() + ", tempPreviewPix.size(): " + tempPreviewPix.size());
                        // HOROS-20240407準拠: [previewPixThumbnails removeAllObjects];
                        // HOROS-20240407準拠: [previewPixThumbnails addObjectsFromArray: tempPreviewPixThumbnails];
                        previewPixThumbnails.clear();
                        previewPixThumbnails.addAll(tempPreviewPixThumbnails);
                        
                        // HOROS-20240407準拠: [previewPix removeAllObjects];
                        // HOROS-20240407準拠: [previewPix addObjectsFromArray: tempPreviewPix];
                        if (previewPix != null) {
                            previewPix.clear();
                            previewPix.addAll(tempPreviewPix);
                            System.out.println("[DEBUG] matrixLoadIcons() - previewPix updated, final size: " + previewPix.size());
                        } else {
                            System.out.println("[DEBUG] matrixLoadIcons() - previewPix is null!");
                        }
                        System.out.println("[DEBUG] matrixLoadIcons() - previewPixThumbnails updated, final size: " + previewPixThumbnails.size());
                    } else {
                        System.out.println("[DEBUG] matrixLoadIcons() - previewPix != context, skipping update");
                    }
                }
                
                // HOROS-20240407準拠: if( [NSThread isMainThread] == NO)
                //     [self performSelectorOnMainThread:@selector(matrixDisplayIcons:) withObject:nil waitUntilDone:NO modes:[NSArray arrayWithObject:NSRunLoopCommonModes]];
                // else
                //     [self matrixDisplayIcons: nil];
                if (!SwingUtilities.isEventDispatchThread()) {
                    SwingUtilities.invokeLater(() -> {
                        matrixDisplayIcons();
                    });
                } else {
                    matrixDisplayIcons();
                }
            }
        }
        // HOROS-20240407準拠: @catch (NSException* e)
        catch (Exception e) {
            // HOROS-20240407準拠: N2LogExceptionWithStackTrace(e);
            // デバッグログは削除（HOROS-20240407準拠）
        }
        // HOROS-20240407準拠: [pool release];
        // Javaでは不要（GCが自動的に処理）
    }
    
    /**
     * サムネイルを表示する
     * HOROS-20240407準拠: - (void)matrixDisplayIcons:(id) sender (9719行目)
     */
    private void matrixDisplayIcons() {
        System.out.println("[DEBUG] matrixDisplayIcons() called");
        // HOROS-20240407準拠: BrowserController.m 9719-9769行目
        if (database == null) {
            System.out.println("[DEBUG] matrixDisplayIcons() - database is null, returning");
            return;
        }
        
        try {
            synchronized (previewPixThumbnails) {
                // HOROS-20240407準拠: if ([previewPix count] && loadPreviewIndex < [previewPix count])
                System.out.println("[DEBUG] matrixDisplayIcons() - previewPix: " + (previewPix != null ? previewPix.size() : "null") + ", previewPixThumbnails: " + (previewPixThumbnails != null ? previewPixThumbnails.size() : "null") + ", matrixViewArray: " + (matrixViewArray != null ? matrixViewArray.size() : "null") + ", loadPreviewIndex: " + loadPreviewIndex);
                
                // HOROS-20240407準拠: if ([previewPix count] && loadPreviewIndex < [previewPix count]) (9729行目)
                // HOROS-20240407準拠: previewPixが空の場合は何もしない
                if (previewPix != null && !previewPix.isEmpty() && loadPreviewIndex < previewPix.size()) {
                    System.out.println("[DEBUG] matrixDisplayIcons() - entering loop, previewPix.size(): " + previewPix.size());
                    long i;
                    // HOROS-20240407準拠: for( i = 0; i < [previewPix count]; i++) (9732行目)
                    for (i = 0; i < previewPix.size(); i++) {
                        if (oMatrix != null) {
                            int rows = oMatrix.getRows();
                            int cols = oMatrix.getColumns();
                            if (cols < 1) {
                                cols = 1;
                            }
                            
                            int row = (int) (i / cols);
                            int col = (int) (i % cols);
                            javax.swing.JButton cell = oMatrix.cellAtRowColumn(row, col);
                            
                            // HOROS-20240407準拠: if( [cell isEnabled] == NO) (9737行目)
                            if (cell != null && !cell.isEnabled()) {
                                // HOROS-20240407準拠: if( i < [previewPix count]) (9739行目)
                                // HOROS-20240407準拠: if( [previewPix objectAtIndex: i] != nil) (9741行目)
                                Object pix = previewPix.get((int) i);
                                // HOROS-20240407準拠: if( i < [matrixViewArray count]) (9743行目)
                                if (pix != null && i < matrixViewArray.size()) {
                                    // HOROS-20240407準拠: [self matrixNewIcon:i :[matrixViewArray objectAtIndex: i]]; (9745行目)
                                    System.out.println("[DEBUG] matrixDisplayIcons() - calling matrixNewIcon(" + i + ", " + matrixViewArray.get((int) i) + ")");
                                    matrixNewIcon(i, matrixViewArray.get((int) i));
                                }
                            }
                        }
                    }
                    
                    // HOROS-20240407準拠: 選択されていない場合は最初のセルを選択
                    if (oMatrix != null) {
                        javax.swing.JButton selectedCell = oMatrix.selectedCell();
                        if (selectedCell == null) {
                            if (!matrixViewArray.isEmpty()) {
                                oMatrix.selectCellWithTag(0);
                            }
                        }
                    }
                    
                    // HOROS-20240407準拠: 最初のサムネイルが読み込まれたらアニメーションスライダーを初期化
                    if (loadPreviewIndex == 0) {
                        initAnimationSlider();
                    }
                    
                    loadPreviewIndex = i;
                }
                
                // HOROS-20240407準拠: BrowserController.m 9610-9625行目
                // setDCMDone == NO の場合、imageViewにsetPixelsを呼び出す
                // HOROS-20240407準拠: この処理はループの外で実行される（loadPreviewIndex == iの後）
                if (!setDCMDone) {
                    Object aFile = databaseOutline.getSelectedItem();
                    
                    if (aFile != null && imageView != null) {
                        synchronized (previewPixThumbnails) {
                            // HOROS-20240407準拠: BrowserController.m 9619行目
                            // [imageView setPixels:previewPix files:[self imagesArray: aFile preferredObject: oAny] rois:nil firstImage:[[oMatrix selectedCell] tag] level:'i' reset:YES];
                            javax.swing.JButton selectedCell = oMatrix != null ? oMatrix.selectedCell() : null;
                            int firstImage = 0;
                            if (selectedCell != null) {
                                Object tagObj = selectedCell.getClientProperty("tag");
                                firstImage = tagObj instanceof Integer ? (Integer) tagObj : 0;
                            }
                            
                            List<Object> files = imagesArray(aFile, oAny);
                            // HOROS-20240407準拠: BrowserController.m 9619行目
                            // previewPixが空でないことを確認していない
                            // そのため、previewPixが空の場合は、空のリストを渡す
                            if (!files.isEmpty()) {
                                // HOROS-20240407準拠: previewPixが空の場合は、空のリストを渡す
                                if (previewPix == null || previewPix.isEmpty()) {
                                    previewPix = new ArrayList<>();
                                    for (int j = 0; j < files.size(); j++) {
                                        previewPix.add(null);
                                    }
                                }
                                imageView.setPixels(previewPix, files, null, (short) firstImage, 'i', true);
                                
                                // HOROS-20240407準拠: BrowserController.m 9622行目
                                // [imageView setStringID:@"previewDatabase"];
                                imageView.setStringID("previewDatabase");
                                
                                setDCMDone = true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * アニメーションスライダー初期化
     * HOROS-20240407準拠: - (void) initAnimationSlider (8861行目)
     */
    private void initAnimationSlider() {
        if (animationSlider == null) {
            animationSlider = new javax.swing.JSlider();
        }
        
        boolean animate = false;
        long noOfImages = 0;
        
        javax.swing.JButton cell = oMatrix != null ? oMatrix.selectedCell() : null;
        
        if (cell != null) {
            Object tagObj = cell.getClientProperty("tag");
            int tag = tagObj instanceof Integer ? (Integer) tagObj : -1;
            
            if (tag >= 0 && matrixViewArray != null && tag < matrixViewArray.size()) {
                Object aFile = databaseOutline.getSelectedItem();
                
                if (aFile != null) {
                    String type = getItemType(aFile);
                    
                    if ("Series".equals(type)) {
                        int imageCount = getItemImageCount(aFile);
                        if (imageCount == 1) {
                            // TODO: マルチフレーム画像の処理
                            // noOfImages = numberOfFrames;
                            // animate = YES;
                        } else if (imageCount > 1) {
                            noOfImages = imageCount;
                            animate = true;
                        }
                    } else if ("Study".equals(type)) {
                        // HOROS-20240407準拠: Studyタイプの場合、previewPixのサイズを使用
                        if (previewPix != null && !previewPix.isEmpty()) {
                            noOfImages = previewPix.size();
                            animate = true;
                        } else if (matrixViewArray != null && !matrixViewArray.isEmpty()) {
                            List<Object> images = imagesArray(matrixViewArray.get(tag), oAny);
                            if (!images.isEmpty()) {
                                if (images.size() > 1) {
                                    noOfImages = images.size();
                                    animate = true;
                                } else {
                                    // TODO: マルチフレーム画像の処理
                                    // noOfImages = numberOfFrames;
                                    // animate = YES;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // HOROS-20240407準拠: Studyタイプの場合でも、previewPixのサイズを使用してanimationSliderを有効化
        if (!animate) {
            // HOROS-20240407準拠: Studyタイプの場合、previewPixのサイズを使用
            Object aFile = databaseOutline != null ? databaseOutline.getSelectedItem() : null;
            String type = aFile != null ? getItemType(aFile) : null;
            if ("Study".equals(type) && previewPix != null && !previewPix.isEmpty()) {
                // HOROS-20240407準拠: Studyタイプの場合、previewPixのサイズを使用してanimationSliderを有効化
                if (animationSlider != null) {
                    animationSlider.setEnabled(true);
                    animationSlider.setMaximum(previewPix.size() - 1);
                    // HOROS-20240407準拠: 現在選択されているサムネイルのインデックスを使用
                    if (cell != null) {
                        Object tagObj = cell.getClientProperty("tag");
                        int currentTag = tagObj instanceof Integer ? (Integer) tagObj : -1;
                        if (currentTag >= 0 && currentTag < previewPix.size()) {
                            animationSlider.setValue(currentTag);
                        } else {
                            animationSlider.setValue(0);
                        }
                    } else {
                        animationSlider.setValue(0);
                    }
                }
            } else {
                if (animationSlider != null) {
                    animationSlider.setEnabled(false);
                    animationSlider.setMaximum(0);
                    animationSlider.setValue(0);
                }
            }
        } else if (animationSlider != null) {
            // HOROS-20240407準拠: animateがtrueの場合、animationSliderを有効化
            if (!animationSlider.isEnabled()) {
                animationSlider.setEnabled(true);
            }
            animationSlider.setMaximum((int) (noOfImages - 1));
            // HOROS-20240407準拠: 現在選択されているサムネイルのインデックスを使用
            if (cell != null) {
                Object tagObj = cell.getClientProperty("tag");
                int currentTag = tagObj instanceof Integer ? (Integer) tagObj : -1;
                if (currentTag >= 0 && currentTag < noOfImages) {
                    animationSlider.setValue(currentTag);
                } else {
                    animationSlider.setValue(0);
                }
            } else {
                animationSlider.setValue(0);
            }
        }
        
        withReset = true;
        previewSliderAction(animationSlider);
        withReset = false;
    }
    
    /**
     * プレビュースライダーアクション
     * HOROS-20240407準拠: - (void) previewSliderAction:(id) sender (9049行目)
     * 注意: このメソッドは200行以上の複雑なロジックを含む
     */
    private void previewSliderAction(Object sender) {
        System.out.println("[DEBUG] previewSliderAction() called - sender: " + sender);
        // HOROS-20240407準拠: BrowserController.m 9049-9218行目
        
        if (dontUpdatePreviewPane) {
            System.out.println("[DEBUG] previewSliderAction() - dontUpdatePreviewPane is true, returning");
            return;
        }
        
        javax.swing.JButton cell = oMatrix != null ? oMatrix.selectedCell() : null;
        System.out.println("[DEBUG] previewSliderAction() - cell: " + cell);
        if (cell == null || !cell.isEnabled()) {
            System.out.println("[DEBUG] previewSliderAction() - cell is null or disabled, returning");
            return;
        }
        
        Object tagObj = cell.getClientProperty("tag");
        int index = tagObj instanceof Integer ? (Integer) tagObj : -1;
        System.out.println("[DEBUG] previewSliderAction() - index: " + index);
        
        if (index < 0 || matrixViewArray == null || index >= matrixViewArray.size()) {
            System.out.println("[DEBUG] previewSliderAction() - index out of range, returning");
            return;
        }
        
        Object aFile = databaseOutline.getSelectedItem();
        System.out.println("[DEBUG] previewSliderAction() - aFile: " + aFile);
        if (aFile == null) {
            System.out.println("[DEBUG] previewSliderAction() - aFile is null, returning");
            return;
        }
        
        String type = getItemType(aFile);
        System.out.println("[DEBUG] previewSliderAction() - type: " + type);
        
        // HOROS-20240407準拠: BrowserController.m 9102-9106行目 - シリーズレベルの処理
        if ("Series".equals(type)) {
            int imageCount = getItemImageCount(aFile);
            System.out.println("[DEBUG] previewSliderAction() - imageCount: " + imageCount);
            if (imageCount > 1 && animationSlider != null) {
                int sliderValue = animationSlider.getValue();
                System.out.println("[DEBUG] previewSliderAction() - sliderValue: " + sliderValue);
                // HOROS-20240407準拠: BrowserController.m 9104-9105行目
                // if( sender) [oMatrix selectCellWithTag: [animationSlider intValue]];
                // senderが存在する場合（スライドバー操作時）はサムネイルを選択
                if (sender != null && oMatrix != null) {
                    oMatrix.selectCellWithTag(sliderValue);
                }
                if (imageView != null && sliderValue < imageCount) {
                    System.out.println("[DEBUG] previewSliderAction() - calling imageView.setIndex(" + sliderValue + ")");
                    imageView.setIndex(sliderValue);
                } else {
                    System.out.println("[DEBUG] previewSliderAction() - imageView is null or sliderValue >= imageCount");
                }
            } else {
                System.out.println("[DEBUG] previewSliderAction() - imageCount <= 1 or animationSlider is null");
            }
        } else if ("Study".equals(type)) {
            // HOROS-20240407準拠: BrowserController.m 9099-9100行目 - スタディレベルの処理
            // HOROS-20240407準拠: Studyタイプの場合、senderが存在する場合（スライダー操作時）は
            // animationSliderの値を使用してサムネイルを選択し、そのインデックスで画像を表示する
            // HOROS-20240407準拠: BrowserController.m 9104-9105行目
            // if( sender) [oMatrix selectCellWithTag: [animationSlider intValue]];
            int targetIndex = index;
            if (sender != null && animationSlider != null) {
                // HOROS-20240407準拠: スライダー操作時は、animationSliderの値を使用
                targetIndex = animationSlider.getValue();
                // HOROS-20240407準拠: サムネイルを選択
                if (oMatrix != null && targetIndex >= 0 && targetIndex < (previewPix != null ? previewPix.size() : 0)) {
                    oMatrix.selectCellWithTag(targetIndex);
                }
            }
            
            // HOROS-20240407準拠: BrowserController.m 9138-9144行目
            // [previewPix replaceObjectAtIndex:[cell tag] withObject:(id) dcmPix];
            // if( withReset) [imageView setIndexWithReset:[cell tag] :YES];
            if (imageView != null && previewPix != null && targetIndex >= 0 && targetIndex < previewPix.size()) {
                // HOROS-20240407準拠: previewPixのインデックスを使用して画像を表示
                System.out.println("[DEBUG] previewSliderAction() - calling imageView.setIndex(" + targetIndex + ") for Study (sender=" + (sender != null ? "not null" : "null") + ", index=" + index + ")");
                imageView.setIndex(targetIndex);
            } else {
                System.out.println("[DEBUG] previewSliderAction() - Study type: imageView=" + (imageView != null ? "not null" : "null") + ", previewPix=" + (previewPix != null ? previewPix.size() : "null") + ", targetIndex=" + targetIndex);
            }
        }
    }
    
    /**
     * データベーススタディを開く
     * HOROS-20240407準拠: - (void) databaseOpenStudy: (NSManagedObject*) item
     * (7505行目)
     */
    private void databaseOpenStudy(Object item) {
        // HOROS-20240407準拠: スタディを開く処理の実装
        // TODO: ViewerControllerの実装が必要
    }
    
    /**
     * プレビューアニメーション実行
     * HOROS-20240407準拠: - (void)previewPerformAnimation: (id)sender (9239行目)
     */
    private void previewPerformAnimation() {
        // HOROS-20240407準拠: BrowserController.m 9245-9251行目
        // if( [[AppController sharedAppController] isSessionInactive] || waitForRunningProcess)
        //     return;
        // if( _database == nil) return;
        // if( animationCheck.state == NSOffState) return;
        if (database == null) {
            return;
        }
        if (animationCheck == null || !animationCheck.isSelected()) {
            return;
        }
        
        // HOROS-20240407準拠: BrowserController.m 9253-9254行目
        // if( self.window.isKeyWindow == NO) return;
        // if( animationSlider.isEnabled == NO) return;
        if (!isActive()) {
            return;
        }
        if (animationSlider == null || !animationSlider.isEnabled()) {
            return;
        }
        
        // HOROS-20240407準拠: BrowserController.m 9256-9261行目
        // int	pos = animationSlider.intValue;
        // pos++;
        // if( pos > animationSlider.maxValue) pos = 0;
        // [animationSlider setIntValue: pos];
        // [self previewSliderAction: nil];
        int pos = animationSlider.getValue();
        pos++;
        int maxValue = animationSlider.getMaximum();
        if (pos > maxValue) {
            pos = 0;
        }
        animationSlider.setValue(pos);
        previewSliderAction(null);
    }
    
    /**
     * 比較スタディを検索
     * HOROS-20240407準拠: - (void) searchForComparativeStudies: (id) studySelectedID
     * (4709行目)
     * HOROS-20240407準拠: BrowserController.m 4709-4716行目
     */
    private void searchForComparativeStudies(Object studySelectedID) {
        System.out.println("[DEBUG] searchForComparativeStudies() called with studySelectedID: " + studySelectedID);
        if (database == null || studySelectedID == null) {
            System.out.println("[DEBUG] searchForComparativeStudies() - database or studySelectedID is null");
            return;
        }
        
        try {
            // HOROS-20240407準拠: subSearchForComparativeStudiesを呼び出す
            List<Object> studies = subSearchForComparativeStudies(studySelectedID);
            System.out.println("[DEBUG] searchForComparativeStudies() - found " + studies.size() + " studies");
            
            // HOROS-20240407準拠: refreshComparativeStudiesをメインスレッドで呼び出す
            SwingUtilities.invokeLater(() -> {
                System.out.println("[DEBUG] searchForComparativeStudies() - calling refreshComparativeStudies()");
                refreshComparativeStudies(studies);
            });
        } catch (Exception e) {
            System.out.println("[DEBUG] searchForComparativeStudies() - exception: " + e.getMessage());
            e.printStackTrace();
            // エラーが発生した場合は空のリストを設定
            SwingUtilities.invokeLater(() -> {
                refreshComparativeStudies(new ArrayList<>());
            });
        }
    }
    
    /**
     * 比較スタディをサブ検索
     * HOROS-20240407準拠: - (NSArray*) subSearchForComparativeStudies: (id) studySelectedID
     * (4536行目)
     */
    private List<Object> subSearchForComparativeStudies(Object studySelectedID) {
        System.out.println("[DEBUG] subSearchForComparativeStudies() called");
        List<Object> result = new ArrayList<>();
        
        if (database == null || studySelectedID == null) {
            System.out.println("[DEBUG] subSearchForComparativeStudies() - database or studySelectedID is null");
            return result;
        }
        
        try {
            // HOROS-20240407準拠: studySelectedIDからスタディを取得
            if (!(studySelectedID instanceof com.jj.dicomviewer.model.DicomStudy)) {
                System.out.println("[DEBUG] subSearchForComparativeStudies() - studySelectedID is not DicomStudy");
                // TODO: IDからスタディを取得する処理
                return result;
            }
            
            com.jj.dicomviewer.model.DicomStudy studySelected = 
                (com.jj.dicomviewer.model.DicomStudy) studySelectedID;
            
            // HOROS-20240407準拠: 患者UIDを取得
            String patientUID = getPatientUID(studySelected);
            System.out.println("[DEBUG] subSearchForComparativeStudies() - patientUID: " + patientUID);
            if (patientUID == null || patientUID.isEmpty()) {
                System.out.println("[DEBUG] subSearchForComparativeStudies() - patientUID is null or empty");
                return result;
            }
            
            // HOROS-20240407準拠: 同じ患者UIDを持つスタディを検索
            // HOROS-20240407準拠: BrowserController.m 4569行目 - ローカルデータベースから検索
            List<com.jj.dicomviewer.model.DicomStudy> allStudies = database.getAllStudies();
            System.out.println("[DEBUG] subSearchForComparativeStudies() - allStudies count: " + (allStudies != null ? allStudies.size() : 0));
            if (allStudies == null || allStudies.isEmpty()) {
                System.out.println("[DEBUG] subSearchForComparativeStudies() - allStudies is null or empty");
                return result;
            }
            
            for (com.jj.dicomviewer.model.DicomStudy study : allStudies) {
                String studyPatientUID = study.getPatientUID();
                if (studyPatientUID != null && studyPatientUID.equalsIgnoreCase(patientUID)) {
                    result.add(study);
                }
            }
            System.out.println("[DEBUG] subSearchForComparativeStudies() - matching studies count: " + result.size());
            
            // HOROS-20240407準拠: 日付でソート（降順）
            result.sort((a, b) -> {
                if (a instanceof com.jj.dicomviewer.model.DicomStudy && 
                    b instanceof com.jj.dicomviewer.model.DicomStudy) {
                    com.jj.dicomviewer.model.DicomStudy studyA = (com.jj.dicomviewer.model.DicomStudy) a;
                    com.jj.dicomviewer.model.DicomStudy studyB = (com.jj.dicomviewer.model.DicomStudy) b;
                    java.time.LocalDateTime dateA = studyA.getDate();
                    java.time.LocalDateTime dateB = studyB.getDate();
                    if (dateA == null && dateB == null) {
                        return 0;
                    } else if (dateA == null) {
                        return 1;
                    } else if (dateB == null) {
                        return -1;
                    } else {
                        return dateB.compareTo(dateA); // 降順
                    }
                }
                return 0;
            });
        } catch (Exception e) {
            // エラーが発生した場合は空のリストを返す
            // デバッグ用: エラーをログ出力（本番環境では削除）
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * 比較スタディを更新
     * HOROS-20240407準拠: - (void) refreshComparativeStudies: (NSArray*) newStudies
     * (4747行目)
     */
    private void refreshComparativeStudies(List<Object> newStudies) {
        System.out.println("[DEBUG] refreshComparativeStudies() called with " + (newStudies != null ? newStudies.size() : 0) + " studies");
        if (comparativeTable == null) {
            System.out.println("[DEBUG] refreshComparativeStudies() - comparativeTable is null");
            return;
        }
        
        try {
            // HOROS-20240407準拠: comparativeStudiesを設定
            comparativeStudies = newStudies != null ? new ArrayList<>(newStudies) : new ArrayList<>();
            
            // HOROS-20240407準拠: comparativeTableのモデルを更新
            javax.swing.table.DefaultTableModel model = 
                (javax.swing.table.DefaultTableModel) comparativeTable.getModel();
            
            // モデルをクリア
            int rowCount = model.getRowCount();
            System.out.println("[DEBUG] refreshComparativeStudies() - current rowCount: " + rowCount);
            if (rowCount > 0) {
                model.setRowCount(0);
            }
            
            // HOROS-20240407準拠: 各スタディをテーブルに追加
            // HOROS-20240407準拠: BrowserController.m 11369-11393行目
            // セルレンダラーで表示内容を制御するため、ここではstudyオブジェクトを直接追加
            for (Object study : comparativeStudies) {
                if (study instanceof com.jj.dicomviewer.model.DicomStudy) {
                    // HOROS-20240407準拠: セルレンダラーがスタディ情報を表示するため、studyオブジェクトを直接追加
                    // セルレンダラーのvalueパラメータから直接DicomStudyを取得できるようにする
                    model.addRow(new Object[] { study });
                    System.out.println("[DEBUG] refreshComparativeStudies() - added study: " + 
                        ((com.jj.dicomviewer.model.DicomStudy) study).getStudyName());
                }
            }
            
            // HOROS-20240407準拠: [comparativeTable reloadData]相当の処理
            // モデル変更を通知
            model.fireTableDataChanged();
            
            // HOROS-20240407準拠: テーブルを再描画（サムネイルも更新される）
            comparativeTable.revalidate();
            comparativeTable.repaint();
            
            // HOROS-20240407準拠: HISTORYパネルのサムネイルを更新
            // DBリストで選択されているスタディと同じ患者UIDを持つスタディのサムネイルを表示
            SwingUtilities.invokeLater(() -> {
                comparativeTable.repaint();
            });
            
            // HOROS-20240407準拠: 現在選択されているスタディと同じstudyInstanceUIDを持つ行を選択
            Object item = databaseOutline.getSelectedItem();
            if (item != null) {
                Object studySelected = getStudyFromItem(item);
                if (studySelected != null) {
                    String studyInstanceUID = getStudyInstanceUID(studySelected);
                    if (studyInstanceUID != null) {
                        int index = findStudyIndexInComparativeStudies(studyInstanceUID);
                        if (index >= 0 && index < comparativeTable.getRowCount()) {
                            comparativeTable.setRowSelectionInterval(index, index);
                            comparativeTable.scrollRectToVisible(
                                comparativeTable.getCellRect(index, 0, true));
                            // HOROS-20240407準拠: 選択行の背景色を確実に表示するため、再描画
                            // SwingUtilities.invokeLaterで遅延して再描画することで、
                            // セルレンダラーが正しく呼ばれるようにする
                            SwingUtilities.invokeLater(() -> {
                                comparativeTable.repaint();
                            });
                        } else if (comparativeTable.getRowCount() > 0) {
                            // 見つからない場合は最初の行を選択
                            comparativeTable.setRowSelectionInterval(0, 0);
                            // HOROS-20240407準拠: 選択行の背景色を確実に表示するため、再描画
                            SwingUtilities.invokeLater(() -> {
                                comparativeTable.repaint();
                            });
                        }
                    }
                    
                    // HOROS-20240407準拠: BrowserController.m 6739-6756行目
                    // 選択された行（previousItem）と同じ患者UIDを持つDBリスト行の背景色を変更するため、
                    // databaseOutlineを再描画
                    // HOROS-20240407準拠: willDisplayCellでpreviousItemと比較するため、
                    // previousItemが設定されていれば自動的に背景色が変更される
                    // HOROS-20240407準拠: databaseOutlineを再描画してセルレンダラーを呼ぶ
                    SwingUtilities.invokeLater(() -> {
                        if (databaseOutline != null) {
                            databaseOutline.repaint();
                        }
                    });
                }
            }
        } catch (Exception e) {
            // エラーが発生した場合はスキップ
            // デバッグ用: エラーをログ出力（本番環境では削除）
            e.printStackTrace();
        }
    }
    
    /**
     * ROIとキー画像ボタンをリセット
     * HOROS-20240407準拠: - (void) resetROIsAndKeysButton (4170行目)
     */
    private void resetROIsAndKeysButton() {
        // HOROS-20240407準拠: ROIとキー画像ボタンのリセット実装
        // TODO: 実装
    }
    
    /**
     * アルバムを更新
     * HOROS-20240407準拠: - (void)refreshAlbums (3571行目)
     */
    public void refreshAlbums() {
        // 無限ループ防止
        if (isRefreshingAlbums)
            return;
        isRefreshingAlbums = true;
        
        try {
            if (database != null) {
                if (_computingNumberOfStudiesForAlbums) {
                    delayedRefreshAlbums();
                } else {
                    // アルバムテーブルを更新
                    SwingUtilities.invokeLater(this::updateAlbumTable);
                    
                    Thread thread = new Thread(this::_computeNumberOfStudiesForAlbumsThread);
                    thread.setName("Compute Albums...");
                    ThreadsManager.defaultManager().addThreadAndStart(thread);
                }
            }
        } finally {
            isRefreshingAlbums = false;
        }
    }
    
    /**
     * アルバム更新を遅延
     * HOROS-20240407準拠: - (void)delayedRefreshAlbums (3565行目)
     */
    private void delayedRefreshAlbums() {
        // 無限ループ防止: 既に更新中の場合はスキップ
        if (!isRefreshingAlbums) {
            SwingUtilities.invokeLater(this::refreshAlbums);
        }
    }
    
    /**
     * アルバムのスタディ数を計算するスレッド
     * HOROS-20240407準拠: - (void)_computeNumberOfStudiesForAlbumsThread (3376行目)
     */
    private void _computeNumberOfStudiesForAlbumsThread() {
        if (_computingNumberOfStudiesForAlbums) {
            delayedRefreshAlbums();
            return;
        }
        
        _computingNumberOfStudiesForAlbums = true;
        
        Thread currentThread = Thread.currentThread();
        currentThread.setName("Compute Albums...");
        ThreadsManager.defaultManager().addThreadAndStart(currentThread);
        
        try {
            // HOROS-20240407準拠: DicomDatabase* idatabase = [self.database
            // independentDatabase];
            // TODO: 独立データベースの取得とスタディ数の計算
            
        } finally {
            _computingNumberOfStudiesForAlbums = false;
            SwingUtilities.invokeLater(this::delayedRefreshAlbums);
        }
    }
    
    /**
     * アルバム配列を取得
     * HOROS-20240407準拠: - (NSArray*) albumArray (11276行目)
     * 最初の要素は常に"Database"アルバム（全スタディを表示）
     */
    private List<com.jj.dicomviewer.model.DicomAlbum> getAlbumArray() {
        List<com.jj.dicomviewer.model.DicomAlbum> result = new ArrayList<>();
        
        if (database == null) {
            return result;
        }
        
        // HOROS-20240407準拠: 最初の要素は"Database"アルバム
        // [[NSArray arrayWithObject:[NSDictionary dictionaryWithObject:
        // NSLocalizedString(@"Database", nil) forKey:@"name"]]
        // arrayByAddingObjectsFromArray:[self albumsInDatabase]]
        com.jj.dicomviewer.model.DicomAlbum databaseAlbum = new com.jj.dicomviewer.model.DicomAlbum("Database");
        databaseAlbum.setSmartAlbum(false);
        // Databaseアルバムは全スタディを含む（numberOfStudiesは後で計算）
        result.add(databaseAlbum);
        
        // HOROS-20240407準拠: その後にデータベースのアルバムを追加
        result.addAll(database.getAlbums());
        
        return result;
    }
    
    /**
     * アルバム名でアルバムを選択
     * HOROS-20240407準拠: - (void) selectAlbumWithName: (NSString*) name (2863行目)
     */
    public void selectAlbumWithName(String name) {
        if (albumTable == null || database == null)
            return;
        
        List<com.jj.dicomviewer.model.DicomAlbum> albumArray = getAlbumArray();
        for (int i = 0; i < albumArray.size(); i++) {
            com.jj.dicomviewer.model.DicomAlbum album = albumArray.get(i);
            if (name.equals(album.getName())) {
                // HOROS-20240407準拠: [albumTable selectRowIndexes:[NSIndexSet
                // indexSetWithIndex:...] byExtendingSelection:NO];
                albumTable.setRowSelectionInterval(i, i);
                albumTable.scrollRectToVisible(albumTable.getCellRect(i, 0, true));
                break;
            }
        }
    }
    
    /**
     * データベースを更新
     * HOROS-20240407準拠: - (void)refreshDatabase: (id)sender (3585行目)
     */
    public void refreshDatabase(Object sender) {
        // 無限ループ防止: 既に更新中の場合はスキップ
        if (isRefreshingOutline || isRefreshingAlbums || isUpdatingAlbumTable)
            return;
        
        // HOROS-20240407準拠: if( [[AppController sharedAppController] isSessionInactive]
        // || waitForRunningProcess) return;
        // TODO: セッション非アクティブチェック
        
        // HOROS-20240407準拠: if( _database == nil) return;
        if (database == null)
            return;
        
        // HOROS-20240407準拠: if( DatabaseIsEdited) return;
        if (DatabaseIsEdited)
            return;
        
        // HOROS-20240407準拠: if( [databaseOutline editedRow] != -1) return;
        // TODO: アウトラインの編集状態チェック
        
        List<com.jj.dicomviewer.model.DicomAlbum> albumArray = getAlbumArray();
        
        // HOROS-20240407準拠: if( albumTable.selectedRow >= [albumArray count]) return;
        int selectedRow = albumTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= albumArray.size())
            return;
        
        // HOROS-20240407準拠: if( [[[albumArray objectAtIndex: albumTable.selectedRow]
        // valueForKey:@"smartAlbum"] boolValue] == YES)
        com.jj.dicomviewer.model.DicomAlbum selectedAlbum = albumArray.get(selectedRow);
        if (selectedAlbum != null && Boolean.TRUE.equals(selectedAlbum.isSmartAlbum())) {
            try {
                // HOROS-20240407準拠: [self outlineViewRefresh];
                outlineViewRefresh();
                // HOROS-20240407準拠: [self refreshAlbums];
                refreshAlbums();
            } catch (Exception e) {
                // HOROS-20240407準拠: @catch (NSException * e) { N2LogExceptionWithStackTrace(e);
                // }
                // TODO: ログ出力（HOROS-20240407準拠のログシステムを使用）
                // e.printStackTrace(); // デバッグ用（コメントアウト）
            }
        } else {
            // HOROS-20240407準拠: 通常のアルバムの場合
            // [self refreshAlbums];
            refreshAlbums();
            // HOROS-20240407準拠: [databaseOutline reloadData];
            if (databaseOutline != null) {
                SwingUtilities.invokeLater(() -> {
                    outlineViewRefresh(); // outlineViewArrayを更新
                    // HOROS-20240407準拠: reloadData相当の処理
                    if (databaseOutline.getTreeTableModel() != null) {
                        org.jdesktop.swingx.treetable.TreeTableModel model = databaseOutline.getTreeTableModel();
                        if (model instanceof DatabaseOutlineView.DatabaseOutlineTreeTableModel) {
                            DatabaseOutlineView.DatabaseOutlineTreeTableModel treeTableModel = (DatabaseOutlineView.DatabaseOutlineTreeTableModel) model;
                            treeTableModel.fireTreeStructureChanged();
                        }
                    }
                    databaseOutline.repaint();
                });
            }
            // HOROS-20240407準拠: [comparativeTable reloadData];
            if (comparativeTable != null && comparativeStudies != null) {
                SwingUtilities.invokeLater(() -> {
                    refreshComparativeStudies(comparativeStudies);
                });
            }
        }
    }

    /**
     * メニューバーの初期化
     * HOROS-20240407準拠: MainMenu.xibからメニュー構造を実装
     * HOROS-20240407準拠: MainMenu.xib 69-397行目（Fileメニュー）
     */
    private void initializeMenuBar() {
        javax.swing.JMenuBar menuBar = new javax.swing.JMenuBar();

        // ========== Fileメニュー ==========
        // HOROS-20240407準拠: MainMenu.xib 69行目 <menuItem title="File" id="848">
        javax.swing.JMenu fileMenu = new javax.swing.JMenu("File");

        // Show Database Window
        // HOROS-20240407準拠: MainMenu.xib 72行目
        javax.swing.JMenuItem showDatabaseItem = new javax.swing.JMenuItem("Show Database Window");
        showDatabaseItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, 0));
        showDatabaseItem.addActionListener(e -> {
            // TODO: データベースウィンドウを表示
        });
        fileMenu.add(showDatabaseItem);

        // Toggle Albums & Sources drawer
        // HOROS-20240407準拠: MainMenu.xib 77行目
        javax.swing.JMenuItem drawerToggleItem = new javax.swing.JMenuItem("Toggle Albums & Sources drawer");
        drawerToggleItem.addActionListener(e -> drawerToggle(null));
        fileMenu.add(drawerToggleItem);

        // Toggle History drawer
        // HOROS-20240407準拠: MainMenu.xib 83行目
        javax.swing.JMenuItem comparativeToggleItem = new javax.swing.JMenuItem("Toggle History drawer");
        comparativeToggleItem.addActionListener(e -> comparativeToggle(null));
        fileMenu.add(comparativeToggleItem);

        fileMenu.addSeparator();

        // New Database Folder...
        // HOROS-20240407準拠: MainMenu.xib 92行目
        javax.swing.JMenuItem newDatabaseFolderItem = new javax.swing.JMenuItem("New Database Folder...");
        newDatabaseFolderItem.addActionListener(e -> createDatabaseFolder(null));
        fileMenu.add(newDatabaseFolderItem);

        // Open Database Folder...
        // HOROS-20240407準拠: MainMenu.xib 97行目
        javax.swing.JMenuItem openDatabaseFolderItem = new javax.swing.JMenuItem("Open Database Folder...");
        openDatabaseFolderItem.addActionListener(e -> openDatabase(null));
        fileMenu.add(openDatabaseFolderItem);

        fileMenu.addSeparator();

        // Albums サブメニュー
        // HOROS-20240407準拠: MainMenu.xib 105行目
        javax.swing.JMenu albumsMenu = new javax.swing.JMenu("Albums");

        // Add an album...
        // HOROS-20240407準拠: MainMenu.xib 109行目
        javax.swing.JMenuItem addAlbumItem = new javax.swing.JMenuItem("Add an album...");
        addAlbumItem.addActionListener(e -> addAlbum(null));
        albumsMenu.add(addAlbumItem);

        // Add a smart album...
        // HOROS-20240407準拠: MainMenu.xib 115行目
        javax.swing.JMenuItem addSmartAlbumItem = new javax.swing.JMenuItem("Add a smart album...");
        addSmartAlbumItem.addActionListener(e -> addSmartAlbum(null));
        albumsMenu.add(addSmartAlbumItem);

        // Delete selected album
        // HOROS-20240407準拠: MainMenu.xib 121行目
        javax.swing.JMenuItem deleteAlbumItem = new javax.swing.JMenuItem("Delete selected album");
        deleteAlbumItem.addActionListener(e -> {
            // TODO: 選択されたアルバムを削除
            int selectedRow = albumTable.getSelectedRow();
            if (selectedRow >= 0) {
                // deleteAlbum処理
            }
        });
        albumsMenu.add(deleteAlbumItem);

        albumsMenu.addSeparator();

        // Save albums...
        // HOROS-20240407準拠: MainMenu.xib 128行目
        javax.swing.JMenuItem saveAlbumsItem = new javax.swing.JMenuItem("Save albums...");
        saveAlbumsItem.addActionListener(e -> saveAlbums(null));
        albumsMenu.add(saveAlbumsItem);

        // Import albums...
        // HOROS-20240407準拠: MainMenu.xib 134行目
        javax.swing.JMenuItem importAlbumsItem = new javax.swing.JMenuItem("Import albums...");
        importAlbumsItem.addActionListener(e -> addAlbums(null));
        albumsMenu.add(importAlbumsItem);

        albumsMenu.addSeparator();

        // Create default albums...
        // HOROS-20240407準拠: MainMenu.xib 141行目
        javax.swing.JMenuItem defaultAlbumsItem = new javax.swing.JMenuItem("Create default albums...");
        defaultAlbumsItem.addActionListener(e -> {
            // TODO: defaultAlbums実装
        });
        albumsMenu.add(defaultAlbumsItem);

        fileMenu.add(albumsMenu);

        fileMenu.addSeparator();

        // Report サブメニュー
        // HOROS-20240407準拠: MainMenu.xib 153行目
        javax.swing.JMenu reportMenu = new javax.swing.JMenu("Report");

        // Open report
        // HOROS-20240407準拠: MainMenu.xib 156行目
        javax.swing.JMenuItem openReportItem = new javax.swing.JMenuItem("Open report");
        openReportItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, 0));
        openReportItem.addActionListener(e -> generateReport(null));
        reportMenu.add(openReportItem);

        // Delete report
        // HOROS-20240407準拠: MainMenu.xib 161行目
        javax.swing.JMenuItem deleteReportItem = new javax.swing.JMenuItem("Delete report");
        deleteReportItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, 0));
        deleteReportItem.addActionListener(e -> deleteReport(null));
        reportMenu.add(deleteReportItem);

        reportMenu.addSeparator();

        // Convert to PDF...
        // HOROS-20240407準拠: MainMenu.xib 167行目
        javax.swing.JMenuItem convertToPDFItem = new javax.swing.JMenuItem("Convert to PDF...");
        convertToPDFItem.addActionListener(e -> convertReportToPDF(null));
        reportMenu.add(convertToPDFItem);

        // Convert to DICOM PDF
        // HOROS-20240407準拠: MainMenu.xib 173行目
        javax.swing.JMenuItem convertToDICOMSRItem = new javax.swing.JMenuItem("Convert to DICOM PDF");
        convertToDICOMSRItem.addActionListener(e -> convertReportToDICOMSR(null));
        reportMenu.add(convertToDICOMSRItem);

        fileMenu.add(reportMenu);

        fileMenu.addSeparator();

        // Import サブメニュー
        // HOROS-20240407準拠: MainMenu.xib 185行目
        javax.swing.JMenu importMenu = new javax.swing.JMenu("Import");

        // Import Files...
        // HOROS-20240407準拠: MainMenu.xib 188行目
        javax.swing.JMenuItem importFilesItem = new javax.swing.JMenuItem("Import Files...");
        importFilesItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, 0));
        importFilesItem.addActionListener(e -> importFilesFromDialog());
        importMenu.add(importFilesItem);

        // Import Image from URL...
        // HOROS-20240407準拠: MainMenu.xib 198行目
        javax.swing.JMenuItem importURLItem = new javax.swing.JMenuItem("Import Image from URL...");
        importURLItem.addActionListener(e -> {
            // TODO: addURLToDatabase実装
        });
        importMenu.add(importURLItem);

        // Import Raw Data...
        // HOROS-20240407準拠: MainMenu.xib 209行目
        javax.swing.JMenuItem importRawDataItem = new javax.swing.JMenuItem("Import Raw Data...");
        importRawDataItem.addActionListener(e -> importRawData(null));
        importMenu.add(importRawDataItem);

        fileMenu.add(importMenu);

        // Export サブメニュー
        // HOROS-20240407準拠: MainMenu.xib 222行目
        javax.swing.JMenu exportMenu = new javax.swing.JMenu("Export");

        // Export to DICOM Network Node
        // HOROS-20240407準拠: MainMenu.xib 225行目
        javax.swing.JMenuItem export2PACSItem = new javax.swing.JMenuItem("Export to DICOM Network Node");
        export2PACSItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, 0));
        export2PACSItem.addActionListener(e -> {
            // TODO: export2PACS実装
        });
        exportMenu.add(export2PACSItem);

        // Export to Movie
        // HOROS-20240407準拠: MainMenu.xib 230行目
        javax.swing.JMenuItem exportMovieItem = new javax.swing.JMenuItem("Export to Movie");
        exportMovieItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, 0));
        exportMovieItem.addActionListener(e -> {
            // TODO: exportQuicktime実装
        });
        exportMenu.add(exportMovieItem);

        // Export to JPEG
        // HOROS-20240407準拠: MainMenu.xib 235行目
        javax.swing.JMenuItem exportJPEGItem = new javax.swing.JMenuItem("Export to JPEG");
        exportJPEGItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, 0));
        exportJPEGItem.addActionListener(e -> {
            // TODO: exportJPEG実装
        });
        exportMenu.add(exportJPEGItem);

        // Export to Raw
        // HOROS-20240407準拠: MainMenu.xib 240行目
        javax.swing.JMenuItem exportRAWItem = new javax.swing.JMenuItem("Export to Raw");
        exportRAWItem.addActionListener(e -> {
            // TODO: exportRAW実装
        });
        exportMenu.add(exportRAWItem);

        // Export to TIFF
        // HOROS-20240407準拠: MainMenu.xib 245行目
        javax.swing.JMenuItem exportTIFFItem = new javax.swing.JMenuItem("Export to TIFF");
        exportTIFFItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E,
                java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        exportTIFFItem.addActionListener(e -> {
            // TODO: exportTIFF実装
        });
        exportMenu.add(exportTIFFItem);

        // Export to DICOM file(s)
        // HOROS-20240407準拠: MainMenu.xib 251行目
        javax.swing.JMenuItem exportDICOMFileItem = new javax.swing.JMenuItem("Export to DICOM file(s)");
        exportDICOMFileItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, 0));
        exportDICOMFileItem.addActionListener(e -> {
            // TODO: exportDICOMFile実装
        });
        exportMenu.add(exportDICOMFileItem);

        // Export to Email
        // HOROS-20240407準拠: MainMenu.xib 256行目
        javax.swing.JMenuItem exportEmailItem = new javax.swing.JMenuItem("Export to Email");
        exportEmailItem.addActionListener(e -> sendMail(null));
        exportMenu.add(exportEmailItem);

        // Export to Photos
        // HOROS-20240407準拠: MainMenu.xib 261行目
        javax.swing.JMenuItem exportPhotosItem = new javax.swing.JMenuItem("Export to Photos");
        exportPhotosItem.addActionListener(e -> {
            // TODO: export2iPhoto実装
        });
        exportMenu.add(exportPhotosItem);

        // Export Displayed Database List as...
        // HOROS-20240407準拠: MainMenu.xib 266行目
        javax.swing.JMenuItem saveDBListItem = new javax.swing.JMenuItem("Export Displayed Database List as...");
        saveDBListItem.addActionListener(e -> saveDBListAs(null));
        exportMenu.add(saveDBListItem);

        fileMenu.add(exportMenu);

        fileMenu.addSeparator();

        // Burn...
        // HOROS-20240407準拠: MainMenu.xib 277行目
        javax.swing.JMenuItem burnItem = new javax.swing.JMenuItem("Burn...");
        burnItem.addActionListener(e -> {
            // TODO: burnDICOM実装
        });
        fileMenu.add(burnItem);

        // Anonymize...
        // HOROS-20240407準拠: MainMenu.xib 282行目
        javax.swing.JMenuItem anonymizeItem = new javax.swing.JMenuItem("Anonymize...");
        anonymizeItem.addActionListener(e -> anonymizeDICOM(null));
        fileMenu.add(anonymizeItem);

        // Copy Linked Files to Database Folder
        // HOROS-20240407準拠: MainMenu.xib 287行目
        javax.swing.JMenuItem copyToDBFolderItem = new javax.swing.JMenuItem("Copy Linked Files to Database Folder");
        copyToDBFolderItem.addActionListener(e -> copyToDBFolder(null));
        fileMenu.add(copyToDBFolderItem);

        // Search
        // HOROS-20240407準拠: MainMenu.xib 292行目
        javax.swing.JMenuItem searchItem = new javax.swing.JMenuItem("Search");
        searchItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F,
                java.awt.event.InputEvent.CTRL_DOWN_MASK));
        searchItem.addActionListener(e -> searchField(null));
        fileMenu.add(searchItem);

        // Merge Selected Studies
        // HOROS-20240407準拠: MainMenu.xib 298行目
        javax.swing.JMenuItem mergeStudiesItem = new javax.swing.JMenuItem("Merge Selected Studies");
        mergeStudiesItem.addActionListener(e -> mergeStudies(null));
        fileMenu.add(mergeStudiesItem);

        // Delete Selected Exam
        // HOROS-20240407準拠: MainMenu.xib 303行目
        javax.swing.JMenuItem deleteExamItem = new javax.swing.JMenuItem("Delete Selected Exam");
        deleteExamItem.addActionListener(e -> delItem(null));
        fileMenu.add(deleteExamItem);

        // Meta-Data...
        // HOROS-20240407準拠: MainMenu.xib 308行目
        javax.swing.JMenuItem metaDataItem = new javax.swing.JMenuItem("Meta-Data...");
        metaDataItem.addActionListener(e -> viewXML(null));
        fileMenu.add(metaDataItem);

        fileMenu.addSeparator();

        // Compress selected DICOM files
        // HOROS-20240407準拠: MainMenu.xib 316行目
        javax.swing.JMenuItem compressFilesItem = new javax.swing.JMenuItem("Compress selected DICOM files");
        compressFilesItem.addActionListener(e -> compressSelectedFiles(null));
        fileMenu.add(compressFilesItem);

        // Decompress selected DICOM files
        // HOROS-20240407準拠: MainMenu.xib 321行目
        javax.swing.JMenuItem decompressFilesItem = new javax.swing.JMenuItem("Decompress selected DICOM files");
        decompressFilesItem.addActionListener(e -> decompressSelectedFiles(null));
        fileMenu.add(decompressFilesItem);

        fileMenu.addSeparator();

        // Rebuild Entire Database...
        // HOROS-20240407準拠: MainMenu.xib 329行目
        javax.swing.JMenuItem rebuildDatabaseItem = new javax.swing.JMenuItem("Rebuild Entire Database...");
        rebuildDatabaseItem.addActionListener(e -> ReBuildDatabaseSheet(null));
        fileMenu.add(rebuildDatabaseItem);

        // Rebuild SQL Index File...
        // HOROS-20240407準拠: MainMenu.xib 347行目
        javax.swing.JMenuItem rebuildSQLItem = new javax.swing.JMenuItem("Rebuild SQL Index File...");
        rebuildSQLItem.addActionListener(e -> {
            // TODO: rebuildSQLFile実装
        });
        fileMenu.add(rebuildSQLItem);

        // Rebuild Selected Thumbnails
        // HOROS-20240407準拠: MainMenu.xib 365行目
        javax.swing.JMenuItem rebuildThumbnailsItem = new javax.swing.JMenuItem("Rebuild Selected Thumbnails");
        rebuildThumbnailsItem.addActionListener(e -> rebuildThumbnails(null));
        fileMenu.add(rebuildThumbnailsItem);

        fileMenu.addSeparator();

        // Close Window
        // HOROS-20240407準拠: MainMenu.xib 373行目
        javax.swing.JMenuItem closeWindowItem = new javax.swing.JMenuItem("Close Window");
        closeWindowItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, 0));
        closeWindowItem.addActionListener(e -> dispose());
        fileMenu.add(closeWindowItem);

        fileMenu.addSeparator();

        // Page Setup...
        // HOROS-20240407準拠: MainMenu.xib 381行目
        javax.swing.JMenuItem pageSetupItem = new javax.swing.JMenuItem("Page Setup...");
        pageSetupItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P,
                java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        pageSetupItem.addActionListener(e -> {
            // TODO: ページ設定ダイアログ
        });
        fileMenu.add(pageSetupItem);

        // Print...
        // HOROS-20240407準拠: MainMenu.xib 386行目
        javax.swing.JMenuItem printItem = new javax.swing.JMenuItem("Print...");
        printItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, 0));
        printItem.addActionListener(e -> {
            // TODO: 印刷ダイアログ
        });
        fileMenu.add(printItem);

        // DICOM Print...
        // HOROS-20240407準拠: MainMenu.xib 391行目
        javax.swing.JMenuItem dicomPrintItem = new javax.swing.JMenuItem("DICOM Print...");
        dicomPrintItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P,
                java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.ALT_DOWN_MASK));
        dicomPrintItem.addActionListener(e -> {
            // TODO: DICOM印刷実装
        });
        fileMenu.add(dicomPrintItem);

        menuBar.add(fileMenu);

        // ========== Networkメニュー ==========
        // HOROS-20240407準拠: MainMenu.xib 400行目
        javax.swing.JMenu networkMenu = new javax.swing.JMenu("Network");

        // Export to DICOM Network Node
        // HOROS-20240407準拠: MainMenu.xib 404行目
        javax.swing.JMenuItem networkExport2PACSItem = new javax.swing.JMenuItem("Export to DICOM Network Node");
        networkExport2PACSItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, 0));
        networkExport2PACSItem.addActionListener(e -> {
            // TODO: export2PACS実装
        });
        networkMenu.add(networkExport2PACSItem);

        networkMenu.addSeparator();

        // Query / Retrieve Window...
        // HOROS-20240407準拠: MainMenu.xib 412行目
        javax.swing.JMenuItem queryDICOMItem = new javax.swing.JMenuItem("Query / Retrieve Window...");
        queryDICOMItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R,
                java.awt.event.InputEvent.CTRL_DOWN_MASK));
        queryDICOMItem.addActionListener(e -> {
            // TODO: queryDICOM実装
        });
        networkMenu.add(queryDICOMItem);

        // Query Selected Patient from Q&R Window...
        // HOROS-20240407準拠: MainMenu.xib 418行目
        javax.swing.JMenuItem querySelectedStudyItem = new javax.swing.JMenuItem(
                "Query Selected Patient from Q&R Window...");
        querySelectedStudyItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R,
                java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        querySelectedStudyItem.addActionListener(e -> querySelectedStudy(null));
        networkMenu.add(querySelectedStudyItem);

        networkMenu.addSeparator();

        // Auto Query / Retrieve Window...
        // HOROS-20240407準拠: MainMenu.xib 427行目
        javax.swing.JMenuItem autoQueryItem = new javax.swing.JMenuItem("Auto Query / Retrieve Window...");
        autoQueryItem.addActionListener(e -> {
            // TODO: queryDICOM実装（tag=1）
        });
        networkMenu.add(autoQueryItem);

        // Auto Query / Retrieve Refresh
        // HOROS-20240407準拠: MainMenu.xib 433行目
        javax.swing.JMenuItem autoQueryRefreshItem = new javax.swing.JMenuItem("Auto Query / Retrieve Refresh");
        autoQueryRefreshItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, 0));
        autoQueryRefreshItem.addActionListener(e -> {
            // TODO: autoQueryRefresh実装
        });
        networkMenu.add(autoQueryRefreshItem);

        networkMenu.addSeparator();

        // Retrieve selected PACS On-Demand studies
        // HOROS-20240407準拠: MainMenu.xib 441行目
        javax.swing.JMenuItem retrievePODItem = new javax.swing.JMenuItem("Retrieve selected PACS On-Demand studies");
        retrievePODItem.addActionListener(e -> retrieveSelectedPODStudies(null));
        networkMenu.add(retrievePODItem);

        // Refresh PACS On-Demand results
        // HOROS-20240407準拠: MainMenu.xib 447行目
        javax.swing.JMenuItem refreshPODItem = new javax.swing.JMenuItem("Refresh PACS On-Demand results");
        refreshPODItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, 0));
        refreshPODItem.addActionListener(e -> refreshPACSOnDemandResults(null));
        networkMenu.add(refreshPODItem);

        networkMenu.addSeparator();

        // Abort Incoming DICOM Processes (Store-SCU)
        // HOROS-20240407準拠: MainMenu.xib 455行目
        javax.swing.JMenuItem abortStoreSCUItem = new javax.swing.JMenuItem(
                "Abort Incoming DICOM Processes (Store-SCU)");
        abortStoreSCUItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A,
                java.awt.event.InputEvent.ALT_DOWN_MASK));
        abortStoreSCUItem.addActionListener(e -> {
            // TODO: killAllStoreSCU実装
        });
        networkMenu.add(abortStoreSCUItem);

        networkMenu.addSeparator();

        // Network Logs
        // HOROS-20240407準拠: MainMenu.xib 464行目
        javax.swing.JMenuItem networkLogsItem = new javax.swing.JMenuItem("Network Logs");
        networkLogsItem.addActionListener(e -> showLogWindow(null));
        networkMenu.add(networkLogsItem);

        menuBar.add(networkMenu);

        // ========== Editメニュー ==========
        // HOROS-20240407準拠: MainMenu.xib 472行目
        javax.swing.JMenu editMenu = new javax.swing.JMenu("Edit");

        // Undo
        // HOROS-20240407準拠: MainMenu.xib 475行目
        javax.swing.JMenuItem undoItem = new javax.swing.JMenuItem("Undo");
        undoItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, 0));
        undoItem.addActionListener(e -> {
            // TODO: undo実装
        });
        editMenu.add(undoItem);

        // Redo
        // HOROS-20240407準拠: MainMenu.xib 480行目
        javax.swing.JMenuItem redoItem = new javax.swing.JMenuItem("Redo");
        redoItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z,
                java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        redoItem.addActionListener(e -> {
            // TODO: redo実装
        });
        editMenu.add(redoItem);

        editMenu.addSeparator();

        // Cut
        // HOROS-20240407準拠: MainMenu.xib 488行目
        javax.swing.JMenuItem cutItem = new javax.swing.JMenuItem("Cut");
        cutItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, 0));
        cutItem.addActionListener(e -> {
            // TODO: cut実装
        });
        editMenu.add(cutItem);

        // Copy
        // HOROS-20240407準拠: MainMenu.xib 493行目
        javax.swing.JMenuItem copyItem = new javax.swing.JMenuItem("Copy");
        copyItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, 0));
        copyItem.addActionListener(e -> copy(null));
        editMenu.add(copyItem);

        // Paste
        // HOROS-20240407準拠: MainMenu.xib 498行目
        javax.swing.JMenuItem pasteItem = new javax.swing.JMenuItem("Paste");
        pasteItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, 0));
        pasteItem.addActionListener(e -> paste(null));
        editMenu.add(pasteItem);

        // Select All
        // HOROS-20240407準拠: MainMenu.xib 503行目
        javax.swing.JMenuItem selectAllItem = new javax.swing.JMenuItem("Select All");
        selectAllItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, 0));
        selectAllItem.addActionListener(e -> {
            // TODO: selectAll実装
        });
        editMenu.add(selectAllItem);

        menuBar.add(editMenu);

        // ========== Formatメニュー ==========
        // HOROS-20240407準拠: MainMenu.xib 511行目
        javax.swing.JMenu formatMenu = new javax.swing.JMenu("Format");

        // Customize Toolbar...
        // HOROS-20240407準拠: MainMenu.xib 514行目
        javax.swing.JMenuItem customizeToolbarItem = new javax.swing.JMenuItem("Customize Toolbar...");
        customizeToolbarItem.addActionListener(e -> {
            // TODO: customizeViewerToolBar実装
        });
        formatMenu.add(customizeToolbarItem);

        // Fullscreen
        // HOROS-20240407準拠: MainMenu.xib 519行目
        javax.swing.JMenuItem fullscreenItem = new javax.swing.JMenuItem("Fullscreen");
        fullscreenItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, 0));
        fullscreenItem.addActionListener(e -> {
            // TODO: fullScreenMenu実装
        });
        formatMenu.add(fullscreenItem);

        formatMenu.addSeparator();

        // Font サブメニュー
        // HOROS-20240407準拠: MainMenu.xib 527行目
        javax.swing.JMenu fontMenu = new javax.swing.JMenu("Font");

        // Show Fonts
        // HOROS-20240407準拠: MainMenu.xib 530行目
        javax.swing.JMenuItem showFontsItem = new javax.swing.JMenuItem("Show Fonts");
        showFontsItem.addActionListener(e -> {
            // TODO: フォントパネル表示
        });
        fontMenu.add(showFontsItem);

        // Bold
        // HOROS-20240407準拠: MainMenu.xib 536行目
        javax.swing.JMenuItem boldItem = new javax.swing.JMenuItem("Bold");
        boldItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, 0));
        boldItem.addActionListener(e -> {
            // TODO: addFontTrait実装
        });
        fontMenu.add(boldItem);

        // Italic
        // HOROS-20240407準拠: MainMenu.xib 541行目
        javax.swing.JMenuItem italicItem = new javax.swing.JMenuItem("Italic");
        italicItem.addActionListener(e -> {
            // TODO: addFontTrait実装
        });
        fontMenu.add(italicItem);

        fontMenu.addSeparator();

        // Bigger
        // HOROS-20240407準拠: MainMenu.xib 550行目
        javax.swing.JMenuItem biggerItem = new javax.swing.JMenuItem("Bigger");
        biggerItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PLUS, 0));
        biggerItem.addActionListener(e -> {
            // TODO: modifyFont実装
        });
        fontMenu.add(biggerItem);

        // Smaller
        // HOROS-20240407準拠: MainMenu.xib 555行目
        javax.swing.JMenuItem smallerItem = new javax.swing.JMenuItem("Smaller");
        smallerItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_MINUS, 0));
        smallerItem.addActionListener(e -> {
            // TODO: modifyFont実装
        });
        fontMenu.add(smallerItem);

        fontMenu.addSeparator();

        // Show Colors
        // HOROS-20240407準拠: MainMenu.xib 563行目
        javax.swing.JMenuItem showColorsItem = new javax.swing.JMenuItem("Show Colors");
        showColorsItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C,
                java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        showColorsItem.addActionListener(e -> {
            // TODO: カラーパネル表示
        });
        fontMenu.add(showColorsItem);

        formatMenu.add(fontMenu);

        menuBar.add(formatMenu);

        // ========== 2D Viewerメニュー ==========
        // HOROS-20240407準拠: MainMenu.xib 574行目
        javax.swing.JMenu viewer2DMenu = new javax.swing.JMenu("2D Viewer");

        // Next Series
        // HOROS-20240407準拠: MainMenu.xib 577行目
        javax.swing.JMenuItem nextSeriesItem = new javax.swing.JMenuItem("Next Series");
        nextSeriesItem.addActionListener(e -> loadSerie(null));
        viewer2DMenu.add(nextSeriesItem);

        // Previous Series
        // HOROS-20240407準拠: MainMenu.xib 582行目
        javax.swing.JMenuItem previousSeriesItem = new javax.swing.JMenuItem("Previous Series");
        previousSeriesItem.addActionListener(e -> loadSerie(null));
        viewer2DMenu.add(previousSeriesItem);

        // Next Patient
        // HOROS-20240407準拠: MainMenu.xib 587行目
        javax.swing.JMenuItem nextPatientItem = new javax.swing.JMenuItem("Next Patient");
        nextPatientItem.addActionListener(e -> loadPatient(null));
        viewer2DMenu.add(nextPatientItem);

        // Previous Patient
        // HOROS-20240407準拠: MainMenu.xib 593行目
        javax.swing.JMenuItem previousPatientItem = new javax.swing.JMenuItem("Previous Patient");
        previousPatientItem.addActionListener(e -> loadPatient(null));
        viewer2DMenu.add(previousPatientItem);

        viewer2DMenu.addSeparator();

        // Convert between BW / RGB サブメニュー
        // HOROS-20240407準拠: MainMenu.xib 607行目
        javax.swing.JMenu convertBWRGBMenu = new javax.swing.JMenu("Convert between BW / RGB");

        javax.swing.JMenuItem redToBWItem = new javax.swing.JMenuItem("Red ->BW");
        redToBWItem.addActionListener(e -> ConvertToBWMenu(null));
        convertBWRGBMenu.add(redToBWItem);

        javax.swing.JMenuItem greenToBWItem = new javax.swing.JMenuItem("Green ->BW");
        greenToBWItem.addActionListener(e -> ConvertToBWMenu(null));
        convertBWRGBMenu.add(greenToBWItem);

        javax.swing.JMenuItem blueToBWItem = new javax.swing.JMenuItem("Blue ->BW");
        blueToBWItem.addActionListener(e -> ConvertToBWMenu(null));
        convertBWRGBMenu.add(blueToBWItem);

        javax.swing.JMenuItem rgbToBWItem = new javax.swing.JMenuItem("RGB ->BW");
        rgbToBWItem.addActionListener(e -> ConvertToBWMenu(null));
        convertBWRGBMenu.add(rgbToBWItem);

        convertBWRGBMenu.addSeparator();

        javax.swing.JMenuItem bwToRedItem = new javax.swing.JMenuItem("BW -> Red");
        bwToRedItem.addActionListener(e -> ConvertToRGBMenu(null));
        convertBWRGBMenu.add(bwToRedItem);

        javax.swing.JMenuItem bwToGreenItem = new javax.swing.JMenuItem("BW -> Green");
        bwToGreenItem.addActionListener(e -> ConvertToRGBMenu(null));
        convertBWRGBMenu.add(bwToGreenItem);

        javax.swing.JMenuItem bwToBlueItem = new javax.swing.JMenuItem("BW -> Blue");
        bwToBlueItem.addActionListener(e -> ConvertToRGBMenu(null));
        convertBWRGBMenu.add(bwToBlueItem);

        javax.swing.JMenuItem bwToRGBItem = new javax.swing.JMenuItem("BW -> RGB");
        bwToRGBItem.addActionListener(e -> ConvertToRGBMenu(null));
        convertBWRGBMenu.add(bwToRGBItem);

        viewer2DMenu.add(convertBWRGBMenu);

        viewer2DMenu.addSeparator();

        // Reset Image View
        // HOROS-20240407準拠: MainMenu.xib 659行目
        javax.swing.JMenuItem resetImageViewItem = new javax.swing.JMenuItem("Reset Image View");
        resetImageViewItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, 0));
        resetImageViewItem.addActionListener(e -> resetImage(null));
        viewer2DMenu.add(resetImageViewItem);

        // Revert series
        // HOROS-20240407準拠: MainMenu.xib 664行目
        javax.swing.JMenuItem revertSeriesItem = new javax.swing.JMenuItem("Revert series");
        revertSeriesItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R,
                java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        revertSeriesItem.addActionListener(e -> revertSeries(null));
        viewer2DMenu.add(revertSeriesItem);

        // Scale サブメニュー
        // HOROS-20240407準拠: MainMenu.xib 669行目
        javax.swing.JMenu scaleMenu = new javax.swing.JMenu("Scale");

        javax.swing.JMenuItem noRescaleItem = new javax.swing.JMenuItem("No Rescale Size (100%)");
        noRescaleItem.addActionListener(e -> actualSize(null));
        scaleMenu.add(noRescaleItem);

        javax.swing.JMenuItem actualSizeItem = new javax.swing.JMenuItem("Actual Size");
        actualSizeItem.addActionListener(e -> realSize(null));
        scaleMenu.add(actualSizeItem);

        javax.swing.JMenuItem scaleToFitItem = new javax.swing.JMenuItem("Scale To Fit");
        scaleToFitItem.addActionListener(e -> scaleToFit(null));
        scaleMenu.add(scaleToFitItem);

        viewer2DMenu.add(scaleMenu);

        // Sort By サブメニュー
        // HOROS-20240407準拠: MainMenu.xib 694行目
        javax.swing.JMenu sortByMenu = new javax.swing.JMenu("Sort By");

        javax.swing.JMenuItem instanceAscItem = new javax.swing.JMenuItem("Instance Number - Ascending");
        instanceAscItem.addActionListener(e -> sortSeriesByValue(null));
        sortByMenu.add(instanceAscItem);

        javax.swing.JMenuItem instanceDescItem = new javax.swing.JMenuItem("Instance Number - Descending");
        instanceDescItem.addActionListener(e -> sortSeriesByValue(null));
        sortByMenu.add(instanceDescItem);

        sortByMenu.addSeparator();

        javax.swing.JMenuItem sliceLocAscItem = new javax.swing.JMenuItem("Slice Location - Ascending");
        sliceLocAscItem.addActionListener(e -> sortSeriesByValue(null));
        sortByMenu.add(sliceLocAscItem);

        javax.swing.JMenuItem sliceLocDescItem = new javax.swing.JMenuItem("Slice Location - Descending");
        sliceLocDescItem.addActionListener(e -> sortSeriesByValue(null));
        sortByMenu.add(sliceLocDescItem);

        viewer2DMenu.add(sortByMenu);

        // Orientation サブメニュー
        // HOROS-20240407準拠: MainMenu.xib 726行目
        javax.swing.JMenu orientationMenu = new javax.swing.JMenu("Orientation");

        javax.swing.JMenuItem flipHorizontalItem = new javax.swing.JMenuItem("Flip Image Horizontal");
        flipHorizontalItem.addActionListener(e -> flipHorizontal(null));
        orientationMenu.add(flipHorizontalItem);

        javax.swing.JMenuItem flipVerticalItem = new javax.swing.JMenuItem("Flip Image Vertical");
        flipVerticalItem.addActionListener(e -> flipVertical(null));
        orientationMenu.add(flipVerticalItem);

        orientationMenu.addSeparator();

        javax.swing.JMenuItem rotate0Item = new javax.swing.JMenuItem("Rotation 0 Degrees");
        rotate0Item.addActionListener(e -> rotate0(null));
        orientationMenu.add(rotate0Item);

        javax.swing.JMenuItem rotate90Item = new javax.swing.JMenuItem("Rotation 90 Degrees");
        rotate90Item.addActionListener(e -> rotate90(null));
        orientationMenu.add(rotate90Item);

        javax.swing.JMenuItem rotate180Item = new javax.swing.JMenuItem("Rotation 180 Degrees");
        rotate180Item.addActionListener(e -> rotate180(null));
        orientationMenu.add(rotate180Item);

        viewer2DMenu.add(orientationMenu);

        // Calibrate Resolution
        // HOROS-20240407準拠: MainMenu.xib 760行目
        javax.swing.JMenuItem calibrateItem = new javax.swing.JMenuItem("Calibrate Resolution");
        calibrateItem.addActionListener(e -> calibrate(null));
        viewer2DMenu.add(calibrateItem);

        // 3D Position Panel
        // HOROS-20240407準拠: MainMenu.xib 765行目
        javax.swing.JMenuItem threeDPanelItem = new javax.swing.JMenuItem("3D Position Panel");
        threeDPanelItem.addActionListener(e -> threeDPanel(null));
        viewer2DMenu.add(threeDPanelItem);

        // Navigator Panel
        // HOROS-20240407準拠: MainMenu.xib 770行目
        javax.swing.JMenuItem navigatorItem = new javax.swing.JMenuItem("Navigator Panel");
        navigatorItem.addActionListener(e -> navigator(null));
        viewer2DMenu.add(navigatorItem);

        // Flip Series
        // HOROS-20240407準拠: MainMenu.xib 775行目
        javax.swing.JMenuItem flipSeriesItem = new javax.swing.JMenuItem("Flip Series");
        flipSeriesItem.addActionListener(e -> flipDataSeries(null));
        viewer2DMenu.add(flipSeriesItem);

        // Use VOI LUT
        // HOROS-20240407準拠: MainMenu.xib 780行目
        javax.swing.JCheckBoxMenuItem useVOILUTItem = new javax.swing.JCheckBoxMenuItem("Use VOI LUT");
        useVOILUTItem.setSelected(true);
        useVOILUTItem.addActionListener(e -> useVOILUT(null));
        viewer2DMenu.add(useVOILUTItem);

        // Display DICOM Overlays
        // HOROS-20240407準拠: MainMenu.xib 785行目
        javax.swing.JCheckBoxMenuItem displayOverlaysItem = new javax.swing.JCheckBoxMenuItem("Display DICOM Overlays");
        displayOverlaysItem.setSelected(true);
        displayOverlaysItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O,
                java.awt.event.InputEvent.ALT_DOWN_MASK));
        viewer2DMenu.add(displayOverlaysItem);

        // DICOM Meta-Data
        // HOROS-20240407準拠: MainMenu.xib 795行目
        javax.swing.JMenuItem dicomMetaDataItem = new javax.swing.JMenuItem("DICOM Meta-Data");
        dicomMetaDataItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I,
                java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        dicomMetaDataItem.addActionListener(e -> viewXML(null));
        viewer2DMenu.add(dicomMetaDataItem);

        // Convert from/to SUV
        // HOROS-20240407準拠: MainMenu.xib 801行目
        javax.swing.JMenuItem convertSUVItem = new javax.swing.JMenuItem("Convert from/to SUV");
        convertSUVItem.addActionListener(e -> displaySUV(null));
        viewer2DMenu.add(convertSUVItem);

        // Fuse/De-Fuse PET/SPECT - CT
        // HOROS-20240407準拠: MainMenu.xib 807行目
        javax.swing.JMenuItem fuseItem = new javax.swing.JMenuItem("Fuse/De-Fuse PET/SPECT - CT");
        fuseItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, 0));
        fuseItem.addActionListener(e -> blendWindows(null));
        viewer2DMenu.add(fuseItem);

        // Flatten Fused Image
        // HOROS-20240407準拠: MainMenu.xib 812行目
        javax.swing.JMenuItem flattenFusedItem = new javax.swing.JMenuItem("Flatten Fused Image");
        flattenFusedItem.addActionListener(e -> mergeFusedImages(null));
        viewer2DMenu.add(flattenFusedItem);

        viewer2DMenu.addSeparator();

        // Propagate Settings Between Series
        // HOROS-20240407準拠: MainMenu.xib 821行目
        javax.swing.JCheckBoxMenuItem propagateSettingsItem = new javax.swing.JCheckBoxMenuItem(
                "Propagate Settings Between Series");
        propagateSettingsItem.setSelected(true);
        propagateSettingsItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, 0));
        propagateSettingsItem.addActionListener(e -> copySettingsToOthers(null));
        viewer2DMenu.add(propagateSettingsItem);

        // Don't Propagate WL&WW Between Series
        // HOROS-20240407準拠: MainMenu.xib 831行目
        javax.swing.JCheckBoxMenuItem dontPropagateWLWWItem = new javax.swing.JCheckBoxMenuItem(
                "Don't Propagate WL&WW Between Series");
        dontPropagateWLWWItem.setSelected(false);
        dontPropagateWLWWItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G,
                java.awt.event.InputEvent.ALT_DOWN_MASK));
        viewer2DMenu.add(dontPropagateWLWWItem);

        // Propagate Settings in Current Series
        // HOROS-20240407準拠: MainMenu.xib 838行目
        javax.swing.JMenuItem propagateInSeriesItem = new javax.swing.JMenuItem("Propagate Settings in Current Series");
        propagateInSeriesItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G,
                java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        propagateInSeriesItem.addActionListener(e -> switchCopySettingsInSeries(null));
        viewer2DMenu.add(propagateInSeriesItem);

        // Sync Series (Same Study) サブメニュー
        // HOROS-20240407準拠: MainMenu.xib 853行目
        javax.swing.JMenu syncSeriesMenu = new javax.swing.JMenu("Sync Series (Same Study)");

        javax.swing.JMenuItem syncOffItem = new javax.swing.JMenuItem("Off");
        syncOffItem.addActionListener(e -> syncronize(null));
        syncSeriesMenu.add(syncOffItem);

        javax.swing.JMenuItem syncSlicePosItem = new javax.swing.JMenuItem("Slice Position - Absolute");
        syncSlicePosItem.addActionListener(e -> syncronize(null));
        syncSeriesMenu.add(syncSlicePosItem);

        syncSeriesMenu.addSeparator();

        javax.swing.JMenuItem syncSliceIDAbsItem = new javax.swing.JMenuItem("Slice ID - Absolute");
        syncSliceIDAbsItem.addActionListener(e -> syncronize(null));
        syncSeriesMenu.add(syncSliceIDAbsItem);

        javax.swing.JMenuItem syncSliceIDRatioItem = new javax.swing.JMenuItem("Slice ID - Absolute Ratio");
        syncSliceIDRatioItem.addActionListener(e -> syncronize(null));
        syncSeriesMenu.add(syncSliceIDRatioItem);

        javax.swing.JMenuItem syncSliceIDRelItem = new javax.swing.JMenuItem("Slice ID - Relative");
        syncSliceIDRelItem.addActionListener(e -> syncronize(null));
        syncSeriesMenu.add(syncSliceIDRelItem);

        syncSeriesMenu.addSeparator();

        javax.swing.JCheckBoxMenuItem limitSameStudyItem = new javax.swing.JCheckBoxMenuItem("Limit to Same Study");
        limitSameStudyItem.addActionListener(e -> alwaysSyncMenu(null));
        syncSeriesMenu.add(limitSameStudyItem);

        viewer2DMenu.add(syncSeriesMenu);

        // Sync Series (Different Studies) at Current Position
        // HOROS-20240407準拠: MainMenu.xib 899行目
        javax.swing.JMenuItem syncDifferentItem = new javax.swing.JMenuItem(
                "Sync Series (Different Studies) at Current Position");
        syncDifferentItem.addActionListener(e -> SyncSeries(null));
        viewer2DMenu.add(syncDifferentItem);

        viewer2DMenu.addSeparator();

        // Key Image
        // HOROS-20240407準拠: MainMenu.xib 907行目
        javax.swing.JMenuItem keyImageItem = new javax.swing.JMenuItem("Key Image");
        keyImageItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K, 0));
        keyImageItem.addActionListener(e -> setKeyImage(null));
        viewer2DMenu.add(keyImageItem);

        // Mark All Images as Key Images
        // HOROS-20240407準拠: MainMenu.xib 912行目
        javax.swing.JMenuItem markAllKeyItem = new javax.swing.JMenuItem("Mark All Images as Key Images");
        markAllKeyItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K,
                java.awt.event.InputEvent.ALT_DOWN_MASK));
        markAllKeyItem.addActionListener(e -> setAllKeyImages(null));
        viewer2DMenu.add(markAllKeyItem);

        // Unmark All Images as Key Images
        // HOROS-20240407準拠: MainMenu.xib 918行目
        javax.swing.JMenuItem unmarkAllKeyItem = new javax.swing.JMenuItem("Unmark All Images as Key Images");
        unmarkAllKeyItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K,
                java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        unmarkAllKeyItem.addActionListener(e -> setAllNonKeyImages(null));
        viewer2DMenu.add(unmarkAllKeyItem);

        // Mark All ROIs Images as Key Images
        // HOROS-20240407準拠: MainMenu.xib 924行目
        javax.swing.JMenuItem markROIsKeyItem = new javax.swing.JMenuItem("Mark All ROIs Images as Key Images");
        markROIsKeyItem.addActionListener(e -> setROIsImagesKeyImages(null));
        viewer2DMenu.add(markROIsKeyItem);

        // Save as DICOM SC and mark as Key Image
        // HOROS-20240407準拠: MainMenu.xib 930行目
        javax.swing.JMenuItem saveDICOMSCItem = new javax.swing.JMenuItem("Save as DICOM SC and mark as Key Image");
        saveDICOMSCItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K,
                java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        saveDICOMSCItem.addActionListener(e -> captureAndSetKeyImage(null));
        viewer2DMenu.add(saveDICOMSCItem);

        // Find Next Key Image
        // HOROS-20240407準拠: MainMenu.xib 936行目
        javax.swing.JMenuItem findNextKeyItem = new javax.swing.JMenuItem("Find Next Key Image");
        findNextKeyItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K,
                java.awt.event.InputEvent.CTRL_DOWN_MASK));
        findNextKeyItem.addActionListener(e -> findNextPreviousKeyImage(null));
        viewer2DMenu.add(findNextKeyItem);

        // Find Previous Key Image
        // HOROS-20240407準拠: MainMenu.xib 942行目
        javax.swing.JMenuItem findPrevKeyItem = new javax.swing.JMenuItem("Find Previous Key Image");
        findPrevKeyItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K,
                java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        findPrevKeyItem.addActionListener(e -> findNextPreviousKeyImage(null));
        viewer2DMenu.add(findPrevKeyItem);

        // 注: 2D Viewerメニューには他にも多くの項目がありますが、主要な項目のみ実装しています
        // 残りの項目（Subtraction、Annotations、Window Width & Level、Color Look Up Table、Image
        // Tiling、Windows Tiling、Save/Load Workspace Stateなど）は後で実装

        menuBar.add(viewer2DMenu);

        // ========== 3D Viewerメニュー ==========
        // HOROS-20240407準拠: MainMenu.xib 1371行目
        javax.swing.JMenu viewer3DMenu = new javax.swing.JMenu("3D Viewer");

        // 3D MPR
        // HOROS-20240407準拠: MainMenu.xib 1374行目
        javax.swing.JMenuItem mprViewerItem = new javax.swing.JMenuItem("3D MPR");
        mprViewerItem.addActionListener(e -> mprViewer(null));
        viewer3DMenu.add(mprViewerItem);

        // 3D Curved-MPR
        // HOROS-20240407準拠: MainMenu.xib 1379行目
        javax.swing.JMenuItem cprViewerItem = new javax.swing.JMenuItem("3D Curved-MPR");
        cprViewerItem.addActionListener(e -> cprViewer(null));
        viewer3DMenu.add(cprViewerItem);

        // 2D Orthogonal MPR
        // HOROS-20240407準拠: MainMenu.xib 1384行目
        javax.swing.JMenuItem orthogonalMPRItem = new javax.swing.JMenuItem("2D Orthogonal MPR");
        orthogonalMPRItem.addActionListener(e -> orthogonalMPRViewer(null));
        viewer3DMenu.add(orthogonalMPRItem);

        viewer3DMenu.addSeparator();

        // 3D MIP
        // HOROS-20240407準拠: MainMenu.xib 1392行目
        javax.swing.JMenuItem mipViewerItem = new javax.swing.JMenuItem("3D MIP");
        mipViewerItem.addActionListener(e -> VRViewer(null));
        viewer3DMenu.add(mipViewerItem);

        // 3D Volume Rendering
        // HOROS-20240407準拠: MainMenu.xib 1397行目
        javax.swing.JMenuItem volumeRenderingItem = new javax.swing.JMenuItem("3D Volume Rendering");
        volumeRenderingItem.addActionListener(e -> VRViewer(null));
        viewer3DMenu.add(volumeRenderingItem);

        // 3D Surface Rendering
        // HOROS-20240407準拠: MainMenu.xib 1402行目
        javax.swing.JMenuItem surfaceRenderingItem = new javax.swing.JMenuItem("3D Surface Rendering");
        surfaceRenderingItem.addActionListener(e -> SRViewer(null));
        viewer3DMenu.add(surfaceRenderingItem);

        // 3D Endoscopy
        // HOROS-20240407準拠: MainMenu.xib 1407行目
        javax.swing.JMenuItem endoscopyItem = new javax.swing.JMenuItem("3D Endoscopy");
        endoscopyItem.addActionListener(e -> endoscopyViewer(null));
        viewer3DMenu.add(endoscopyItem);

        viewer3DMenu.addSeparator();

        // Reset to Initial View
        // HOROS-20240407準拠: MainMenu.xib 1415行目
        javax.swing.JMenuItem reset3DItem = new javax.swing.JMenuItem("Reset to Initial View");
        reset3DItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, 0));
        reset3DItem.addActionListener(e -> resetImage(null));
        viewer3DMenu.add(reset3DItem);

        // Revert Series
        // HOROS-20240407準拠: MainMenu.xib 1420行目
        javax.swing.JMenuItem revert3DItem = new javax.swing.JMenuItem("Revert Series");
        revert3DItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R,
                java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        revert3DItem.addActionListener(e -> revertSeries(null));
        viewer3DMenu.add(revert3DItem);

        // 注: 3D Viewerメニューには他にも多くの項目がありますが、主要な項目のみ実装しています

        menuBar.add(viewer3DMenu);

        // ========== ROIメニュー ==========
        // HOROS-20240407準拠: MainMenu.xib 1532行目
        javax.swing.JMenu roiMenu = new javax.swing.JMenu("ROI");

        // Import ROI(s)...
        // HOROS-20240407準拠: MainMenu.xib 1535行目
        javax.swing.JMenuItem importROIItem = new javax.swing.JMenuItem("Import ROI(s)...");
        importROIItem.addActionListener(e -> roiLoadFromFiles(null));
        roiMenu.add(importROIItem);

        roiMenu.addSeparator();

        // Save Selected ROI(s)...
        // HOROS-20240407準拠: MainMenu.xib 1543行目
        javax.swing.JMenuItem saveSelectedROIItem = new javax.swing.JMenuItem("Save Selected ROI(s)...");
        saveSelectedROIItem.addActionListener(e -> roiSaveSelected(null));
        roiMenu.add(saveSelectedROIItem);

        // Save All ROIs of this Series...
        // HOROS-20240407準拠: MainMenu.xib 1548行目
        javax.swing.JMenuItem saveSeriesROIItem = new javax.swing.JMenuItem("Save All ROIs of this Series...");
        saveSeriesROIItem.addActionListener(e -> roiSaveSeries(null));
        roiMenu.add(saveSeriesROIItem);

        roiMenu.addSeparator();

        // Delete All ROIs in this Series
        // HOROS-20240407準拠: MainMenu.xib 1556行目
        javax.swing.JMenuItem deleteAllROIItem = new javax.swing.JMenuItem("Delete All ROIs in this Series");
        deleteAllROIItem.addActionListener(e -> roiDeleteAll(null));
        roiMenu.add(deleteAllROIItem);

        // Delete All ROIs with Same Name as Selected ROI
        // HOROS-20240407準拠: MainMenu.xib 1564行目
        javax.swing.JMenuItem deleteSameNameROIItem = new javax.swing.JMenuItem(
                "Delete All ROIs with Same Name as Selected ROI");
        deleteSameNameROIItem.addActionListener(e -> roiDeleteAllROIsWithSameName(null));
        roiMenu.add(deleteSameNameROIItem);

        roiMenu.addSeparator();

        // Select All ROIs in this Series
        // HOROS-20240407準拠: MainMenu.xib 1573行目
        javax.swing.JMenuItem selectAllROIItem = new javax.swing.JMenuItem("Select All ROIs in this Series");
        selectAllROIItem.addActionListener(e -> roiSelectDeselectAll(null));
        roiMenu.add(selectAllROIItem);

        // Deselect All ROIs in this Series
        // HOROS-20240407準拠: MainMenu.xib 1578行目
        javax.swing.JMenuItem deselectAllROIItem = new javax.swing.JMenuItem("Deselect All ROIs in this Series");
        deselectAllROIItem.addActionListener(e -> roiSelectDeselectAll(null));
        roiMenu.add(deselectAllROIItem);

        roiMenu.addSeparator();

        // ROI Manager...
        // HOROS-20240407準拠: MainMenu.xib 1586行目
        javax.swing.JMenuItem roiManagerItem = new javax.swing.JMenuItem("ROI Manager...");
        roiManagerItem.addActionListener(e -> roiGetManager(null));
        roiMenu.add(roiManagerItem);

        // ROI Info...
        // HOROS-20240407準拠: MainMenu.xib 1591行目
        javax.swing.JMenuItem roiInfoItem = new javax.swing.JMenuItem("ROI Info...");
        roiInfoItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, 0));
        roiInfoItem.addActionListener(e -> roiGetInfo(null));
        roiMenu.add(roiInfoItem);

        // ROI Rename...
        // HOROS-20240407準拠: MainMenu.xib 1596行目
        javax.swing.JMenuItem roiRenameItem = new javax.swing.JMenuItem("ROI Rename...");
        roiRenameItem.addActionListener(e -> roiRename(null));
        roiMenu.add(roiRenameItem);

        // Set Default ROI Name...
        // HOROS-20240407準拠: MainMenu.xib 1601行目
        javax.swing.JMenuItem roiDefaultsItem = new javax.swing.JMenuItem("Set Default ROI Name...");
        roiDefaultsItem.addActionListener(e -> roiDefaults(null));
        roiMenu.add(roiDefaultsItem);

        // 注: ROIメニューには他にも多くの項目がありますが、主要な項目のみ実装しています

        menuBar.add(roiMenu);

        // ========== Pluginsメニュー ==========
        // HOROS-20240407準拠: MainMenu.xib 1853行目
        javax.swing.JMenu pluginsMenu = new javax.swing.JMenu("Plugins");

        // Image Filters サブメニュー
        // HOROS-20240407準拠: MainMenu.xib 1856行目
        javax.swing.JMenu imageFiltersMenu = new javax.swing.JMenu("Image Filters");
        pluginsMenu.add(imageFiltersMenu);

        // ROI Tools サブメニュー
        // HOROS-20240407準拠: MainMenu.xib 1859行目
        javax.swing.JMenu roiToolsMenu = new javax.swing.JMenu("ROI Tools");
        pluginsMenu.add(roiToolsMenu);

        // Others サブメニュー
        // HOROS-20240407準拠: MainMenu.xib 1862行目
        javax.swing.JMenu othersMenu = new javax.swing.JMenu("Others");
        pluginsMenu.add(othersMenu);

        // Database サブメニュー
        // HOROS-20240407準拠: MainMenu.xib 1865行目
        javax.swing.JMenu databasePluginsMenu = new javax.swing.JMenu("Database");
        pluginsMenu.add(databasePluginsMenu);

        pluginsMenu.addSeparator();

        // Plugins Manager...
        // HOROS-20240407準拠: MainMenu.xib 1871行目
        javax.swing.JMenuItem pluginsManagerItem = new javax.swing.JMenuItem("Plugins Manager...");
        pluginsManagerItem.addActionListener(e -> showWindow(null));
        pluginsMenu.add(pluginsManagerItem);

        menuBar.add(pluginsMenu);

        // ========== Recent Studiesメニュー ==========
        // HOROS-20240407準拠: MainMenu.xib 1879行目
        javax.swing.JMenu recentStudiesMenu = new javax.swing.JMenu("Recent Studies");
        // 注: Recent Studiesメニューは動的に更新されるため、初期状態では空
        javax.swing.JMenuItem emptyItem = new javax.swing.JMenuItem("Empty");
        emptyItem.setEnabled(false);
        recentStudiesMenu.add(emptyItem);

        menuBar.add(recentStudiesMenu);

        // ========== Windowメニュー ==========
        // HOROS-20240407準拠: MainMenu.xib 1889行目
        javax.swing.JMenu windowMenu = new javax.swing.JMenu("Window");

        // Minimize
        // HOROS-20240407準拠: MainMenu.xib 1892行目
        javax.swing.JMenuItem minimizeItem = new javax.swing.JMenuItem("Minimize");
        minimizeItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, 0));
        minimizeItem.addActionListener(e -> setState(java.awt.Frame.ICONIFIED));
        windowMenu.add(minimizeItem);

        // Resize Window サブメニュー
        // HOROS-20240407準拠: MainMenu.xib 1897行目
        javax.swing.JMenu resizeWindowMenu = new javax.swing.JMenu("Resize Window");
        String[] resizePercentages = { "25%", "50%", "100%", "200%", "300%", "iPod Video" };
        for (int i = 0; i < resizePercentages.length; i++) {
            javax.swing.JMenuItem resizeItem = new javax.swing.JMenuItem(resizePercentages[i]);
            final int tag = i;
            resizeItem.addActionListener(e -> {
                // TODO: resizeWindow実装（tagを使用）
            });
            resizeWindowMenu.add(resizeItem);
        }
        windowMenu.add(resizeWindowMenu);

        // Tile 2D Viewer Windows
        // HOROS-20240407準拠: MainMenu.xib 1933行目
        javax.swing.JMenuItem tile2DItem = new javax.swing.JMenuItem("Tile 2D Viewer Windows");
        tile2DItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, 0));
        tile2DItem.addActionListener(e -> {
            // TODO: tileWindows実装
        });
        windowMenu.add(tile2DItem);

        // Tile 3D Viewer Windows
        // HOROS-20240407準拠: MainMenu.xib 1938行目
        javax.swing.JMenuItem tile3DItem = new javax.swing.JMenuItem("Tile 3D Viewer Windows");
        tile3DItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T,
                java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        tile3DItem.addActionListener(e -> {
            // TODO: tile3DWindows実装
        });
        windowMenu.add(tile3DItem);

        // Close All Viewers
        // HOROS-20240407準拠: MainMenu.xib 1943行目
        javax.swing.JMenuItem closeAllViewersItem = new javax.swing.JMenuItem("Close All Viewers");
        closeAllViewersItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W,
                java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        closeAllViewersItem.addActionListener(e -> {
            // TODO: closeAllViewers実装
        });
        windowMenu.add(closeAllViewersItem);

        windowMenu.addSeparator();

        // Bring All to Front
        // HOROS-20240407準拠: MainMenu.xib 1949行目
        javax.swing.JMenuItem bringAllToFrontItem = new javax.swing.JMenuItem("Bring All to Front");
        bringAllToFrontItem.addActionListener(e -> {
            // TODO: arrangeInFront実装
        });
        windowMenu.add(bringAllToFrontItem);

        menuBar.add(windowMenu);

        setJMenuBar(menuBar);
    }

    // ========== メニューアクションメソッド（HOROS-20240407準拠） ==========
    // 既に実装されているメソッドはそのまま使用、未実装のものは空実装（TODOコメント付き）

    /**
     * ドロワーをトグル
     * HOROS-20240407準拠: - (IBAction)drawerToggle: (id) sender (10611行目)
     */
    public void drawerToggle(Object sender) {
        // TODO: ドロワーの表示/非表示を切り替え
    }

    /**
     * 比較スタディドロワーをトグル
     * HOROS-20240407準拠: - (IBAction)comparativeToggle:(id)sender (10591行目)
     */
    public void comparativeToggle(Object sender) {
        // TODO: 比較スタディドロワーの表示/非表示を切り替え
    }

    /**
     * データベースフォルダを作成
     * HOROS-20240407準拠: - (IBAction) createDatabaseFolder:(id) sender (1860行目)
     */
    public void createDatabaseFolder(Object sender) {
        // TODO: データベースフォルダ作成ダイアログ
    }

    /**
     * データベースを開く
     * HOROS-20240407準拠: -(IBAction)openDatabase:(id)sender (1843行目)
     */
    public void openDatabase(Object sender) {
        // TODO: データベースフォルダ選択ダイアログ
    }

    /**
     * アルバムを保存
     * HOROS-20240407準拠: - (IBAction) saveAlbums:(id) sender (10811行目)
     */
    public void saveAlbums(Object sender) {
        // TODO: アルバム保存ダイアログ
    }

    /**
     * アルバムを追加（インポート）
     * HOROS-20240407準拠: - (IBAction) addAlbums:(id) sender (10834行目)
     */
    public void addAlbums(Object sender) {
        // TODO: アルバムインポートダイアログ
    }

    /**
     * レポートを生成
     * HOROS-20240407準拠: - (IBAction) generateReport: (id)sender (18655行目)
     */
    public void generateReport(Object sender) {
        // TODO: レポート生成実装
    }
    
    // ========== Sourcesパネル関連メソッド ==========
    // HOROS-20240407準拠: BrowserController.m のSourcesパネル関連メソッドを写経
    
    /**
     * ローカルデータベースにリセット
     * HOROS-20240407準拠: - (void)resetToLocalDatabase (1654行目)
     */
    public void resetToLocalDatabase() {
        // HOROS-20240407準拠: [self setDatabase:[DicomDatabase activeLocalDatabase]];
        if (database != null) {
            // TODO: アクティブなローカルデータベースを設定
            // DicomDatabase localDatabase = DicomDatabase.getActiveLocalDatabase();
            // setDatabase(localDatabase);
        }
    }
    
    /**
     * データベースパスを開く
     * HOROS-20240407準拠: - (void)openDatabasePath: (NSString*)path (20201行目)
     * 
     * @param path データベースのパス
     */
    public void openDatabasePath(String path) {
        // HOROS-20240407準拠: BrowserController.m 20201-20223行目
        // NSThread* thread = [NSThread currentThread];
        // [thread setName:NSLocalizedString(@"Opening database...", nil)];
        // ThreadModalForWindowController* tmc = [thread startModalForWindow:self.window];
        // 
        // @try
        // {
        //     DicomDatabase* db = [DicomDatabase databaseAtPath:path];
        //     if( db)
        //         [self setDatabase:db];
        //     else
        //         [NSException raise:NSGenericException format: @"DicomDatabase == nil"];
        // }
        // @catch (NSException* e)
        // {
        //     N2LogExceptionWithStackTrace(e);
        //     NSRunAlertPanel(NSLocalizedString(@"Horos Database", nil), NSLocalizedString( @"Horos cannot read/create this file/folder. Permissions error?", nil), nil, nil, nil);
        //     [self resetToLocalDatabase];
        // }
        // 
        // [tmc invalidate];
        
        Thread thread = new Thread(() -> {
            try {
                Thread.currentThread().setName("Opening database...");
                
                // TODO: DICOM通信が実装されたら、リモートデータベースを開く処理を実装
                // DicomDatabase db = DicomDatabase.databaseAtPath(path);
                // if (db != null) {
                //     SwingUtilities.invokeLater(() -> {
                //         setDatabase(db);
                //     });
                // } else {
                //     throw new Exception("DicomDatabase == null");
                // }
                
                // 現在はローカルデータベースのみサポート
                SwingUtilities.invokeLater(() -> {
                    resetToLocalDatabase();
                });
            } catch (Exception e) {
                // HOROS-20240407準拠: エラーが発生した場合はローカルデータベースにリセット
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    resetToLocalDatabase();
                });
            }
        });
        thread.setName("Opening database...");
        ThreadsManager.defaultManager().addThreadAndStart(thread);
    }
    
    /**
     * デフォルトデータベースに切り替え（必要に応じて）
     * HOROS-20240407準拠: - (void) switchToDefaultDBIfNeeded (20193行目) // __deprecated
     */
    public void switchToDefaultDBIfNeeded() {
        // HOROS-20240407準拠: BrowserController.m 20193-20199行目
        // NSString *defaultPath = [self documentsDirectoryFor: [[NSUserDefaults standardUserDefaults] integerForKey: @"DEFAULT_DATABASELOCATION"] url: [[NSUserDefaults standardUserDefaults] stringForKey: @"DEFAULT_DATABASELOCATIONURL"]];
        // 
        // if( [[self documentsDirectory] isEqualToString: defaultPath] == NO)
        //     [self resetToLocalDatabase];
        
        // TODO: デフォルトデータベースパスを取得して比較
        // String defaultPath = getDocumentsDirectoryFor(...);
        // String currentPath = getDocumentsDirectory();
        // if (!defaultPath.equals(currentPath)) {
        //     resetToLocalDatabase();
        // }
    }
    
    /**
     * ソース識別子を取得（行インデックスから）
     * HOROS-20240407準拠: - (DataNodeIdentifier*)sourceIdentifierAtRow: (int)row
     * 
     * @param row 行インデックス
     * @return ソース識別子（DataNodeIdentifier相当）、存在しない場合はnull
     */
    public Object sourceIdentifierAtRow(int row) {
        // HOROS-20240407準拠: BrowserController.m 2945行目
        // DataNodeIdentifier* bs = [self sourceIdentifierAtRow: [_sourcesTableView selectedRow]];
        // 
        // if( bs)
        //     description = [description stringByAppendingFormat:NSLocalizedString(@"%@: %@ / ", nil), [_database isLocal] ? NSLocalizedString( @"Local Database", nil) : NSLocalizedString( @"Remote Database", nil), [bs description]];
        
        // TODO: DICOM通信が実装されたら、BonjourBrowserからソース識別子を取得
        // if (bonjourBrowser != null && row > 0) {
        //     // BonjourBrowserからサービスを取得
        //     return bonjourBrowser.getServiceAtRow(row - 1);
        // }
        
        // 現在はローカルデータベースのみサポート
        return null;
    }
    
    /**
     * サーバーを選択（DICOM送信用）
     * HOROS-20240407準拠: - (void) selectServer: (NSArray*)objects (18098行目)
     * 
     * @param objects 送信するオブジェクトのリスト
     */
    public void selectServer(java.util.List<Object> objects) {
        // HOROS-20240407準拠: BrowserController.m 18098-18102行目
        // if( [objects count] > 0) [SendController sendFiles: objects];
        // else NSRunCriticalAlertPanel(NSLocalizedString(@"DICOM Send",nil),NSLocalizedString( @"No files are selected...",nil),NSLocalizedString( @"OK",nil), nil, nil);
        
        if (objects != null && !objects.isEmpty()) {
            // TODO: DICOM通信が実装されたら、SendControllerでファイルを送信
            // SendController.sendFiles(objects);
        } else {
            // HOROS-20240407準拠: ファイルが選択されていない場合はアラートを表示
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No files are selected...",
                "DICOM Send",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
        }
    }
    
    /**
     * パスをアンマウント
     * HOROS-20240407準拠: - (void)unmountPath: (NSString*)path (18054行目)
     * 
     * @param path アンマウントするパス
     */
    public void unmountPath(String path) {
        // HOROS-20240407準拠: BrowserController.m 18054-18080行目
        // [_sourcesTableView display];
        // 
        // int attempts = 0;
        // BOOL success = NO;
        // while( success == NO)
        // {
        //     success = [[NSWorkspace sharedWorkspace] unmountAndEjectDeviceAtPath:  path];
        //     if( success == NO)
        //     {
        //         attempts++;
        //         if( attempts < 5)
        //         {
        //             [NSThread sleepForTimeInterval: 1.0];
        //         }
        //         else success = YES;
        //     }
        // }
        // 
        // [_sourcesTableView display];
        // [_sourcesTableView setNeedsDisplay];
        // 
        // if( attempts == 5)
        // {
        //     NSRunCriticalAlertPanel(NSLocalizedString(@"Failed", nil), NSLocalizedString(@"Unable to unmount this disk. This disk is probably in used by another application.", nil), NSLocalizedString(@"OK",nil),nil, nil);
        // }
        
        // TODO: Windowsではデバイスのアンマウント処理が異なるため、実装が必要
        // Javaでは、java.nio.file.FileSystemsを使用してアンマウント処理を実装する必要がある
        // 現在は未実装
    }
    
    /**
     * 代替ボタンが押された
     * HOROS-20240407準拠: - (void)alternateButtonPressed: (NSNotification*)n (18082行目)
     * 
     * @param notification 通知オブジェクト
     */
    public void alternateButtonPressed(Object notification) {
        // HOROS-20240407準拠: BrowserController.m 18082-18092行目
        // int i = [_sourcesTableView selectedRow];
        // if( i > 0)
        // {
        //     NSString *path = [[[bonjourBrowser services] objectAtIndex: i-1] valueForKey:@"Path"];
        //     
        //     [self resetToLocalDatabase];
        //     [self unmountPath: path];
        // }
        
        // TODO: DICOM通信が実装されたら、選択されたソースのパスを取得してアンマウント
        if (sourcesTableView != null) {
            int selectedRow = sourcesTableView.getSelectedRow();
            if (selectedRow > 0) {
                // TODO: bonjourBrowserからサービスを取得
                // if (bonjourBrowser != null) {
                //     Object service = bonjourBrowser.getServiceAtRow(selectedRow - 1);
                //     if (service != null) {
                //         String path = getPathFromService(service);
                //         resetToLocalDatabase();
                //         unmountPath(path);
                //     }
                // }
            }
        }
    }
    
    /**
     * ソース選択が変更された
     * HOROS-20240407準拠: Sourcesテーブルの選択変更時に呼ばれる
     */
    private void sourceSelectionChanged() {
        // HOROS-20240407準拠: 選択されたソースに基づいてデータベースを切り替え
        if (sourcesTableView != null) {
            int selectedRow = sourcesTableView.getSelectedRow();
            if (selectedRow == 0) {
                // HOROS-20240407準拠: 最初の行（"Documents DB"）が選択された場合はローカルデータベースにリセット
                resetToLocalDatabase();
            } else if (selectedRow > 0) {
                // TODO: DICOM通信が実装されたら、選択されたリモートデータベースを開く
                // Object sourceIdentifier = sourceIdentifierAtRow(selectedRow);
                // if (sourceIdentifier != null) {
                //     String path = getPathFromSourceIdentifier(sourceIdentifier);
                //     openDatabasePath(path);
                // }
            }
        }
    }

    /**
     * レポートを削除
     * HOROS-20240407準拠: - (IBAction)deleteReport: (id)sender (18583行目)
     */
    public void deleteReport(Object sender) {
        // TODO: レポート削除実装
    }

    /**
     * レポートをPDFに変換
     * HOROS-20240407準拠: - (IBAction) convertReportToPDF: (id)sender (18542行目)
     */
    public void convertReportToPDF(Object sender) {
        // TODO: レポートPDF変換実装
    }

    /**
     * レポートをDICOM SRに変換
     * HOROS-20240407準拠: - (IBAction) convertReportToDICOMSR: (id)sender (18494行目)
     */
    public void convertReportToDICOMSR(Object sender) {
        // TODO: レポートDICOM SR変換実装
    }

    /**
     * Rawデータをインポート
     * HOROS-20240407準拠: - (IBAction)importRawData:(id)sender (18186行目)
     */
    public void importRawData(Object sender) {
        // TODO: Rawデータインポート実装
    }

    /**
     * メールを送信
     * HOROS-20240407準拠: -(IBAction)sendMail:(id)sender (16940行目)
     */
    public void sendMail(Object sender) {
        // TODO: メール送信実装
    }

    /**
     * データベースリストを保存
     * HOROS-20240407準拠: - (IBAction) saveDBListAs:(id) sender (8825行目)
     */
    public void saveDBListAs(Object sender) {
        // TODO: データベースリスト保存実装
    }

    /**
     * DICOMを匿名化
     * HOROS-20240407準拠: - (IBAction)anonymizeDICOM:(id)sender (17978行目)
     */
    public void anonymizeDICOM(Object sender) {
        // TODO: DICOM匿名化実装
    }

    /**
     * データベースフォルダにコピー
     * HOROS-20240407準拠: - (IBAction) copyToDBFolder: (id) sender (2110行目)
     */
    public void copyToDBFolder(Object sender) {
        // TODO: データベースフォルダにコピー実装
    }

    /**
     * 検索フィールド
     * HOROS-20240407準拠: - (IBAction) searchField: (id)sender (13752行目)
     */
    public void searchField(Object sender) {
        if (searchField != null) {
            searchField.requestFocus();
        }
    }

    /**
     * スタディをマージ
     * HOROS-20240407準拠: - (IBAction) mergeStudies:(id) sender (5501行目)
     */
    public void mergeStudies(Object sender) {
        // TODO: スタディマージ実装
    }

    /**
     * アイテムを削除
     * HOROS-20240407準拠: - (IBAction)delItem: (id)sender (5999行目)
     */
    public void delItem(Object sender) {
        // TODO: アイテム削除実装
    }

    /**
     * XMLを表示
     * HOROS-20240407準拠: - (IBAction) viewXML:(id) sender (18391行目)
     */
    public void viewXML(Object sender) {
        // TODO: XML表示実装
    }

    /**
     * ファイルを圧縮
     * HOROS-20240407準拠: - (IBAction) compressSelectedFiles: (id)sender (15952行目)
     */
    public void compressSelectedFiles(Object sender) {
        // TODO: ファイル圧縮実装
    }

    /**
     * ファイルを展開
     * HOROS-20240407準拠: - (IBAction)decompressSelectedFiles: (id)sender (15982行目)
     */
    public void decompressSelectedFiles(Object sender) {
        // TODO: ファイル展開実装
    }

    /**
     * データベースを再構築
     * HOROS-20240407準拠: - (IBAction) ReBuildDatabaseSheet: (id)sender (2459行目)
     */
    public void ReBuildDatabaseSheet(Object sender) {
        // TODO: データベース再構築シート表示
    }

    /**
     * サムネイルを再構築
     * HOROS-20240407準拠: - (IBAction)rebuildThumbnails:(id)sender (9985行目)
     */
    public void rebuildThumbnails(Object sender) {
        // TODO: サムネイル再構築実装
    }

    /**
     * 選択スタディをクエリ
     * HOROS-20240407準拠: - (IBAction)querySelectedStudy: (id)sender (18120行目)
     */
    public void querySelectedStudy(Object sender) {
        // TODO: 選択スタディクエリ実装
    }

    /**
     * PACS On-Demandスタディを取得
     * HOROS-20240407準拠: -(IBAction)retrieveSelectedPODStudies:(id) sender (9955行目)
     */
    public void retrieveSelectedPODStudies(Object sender) {
        // TODO: PACS On-Demandスタディ取得実装
    }

    /**
     * PACS On-Demand結果を更新
     * HOROS-20240407準拠: - (IBAction) refreshPACSOnDemandResults:(id) sender
     * (4721行目)
     */
    public void refreshPACSOnDemandResults(Object sender) {
        // TODO: PACS On-Demand結果更新実装
    }

    /**
     * ログウィンドウを表示
     * HOROS-20240407準拠: - (IBAction)showLogWindow: (id)sender (20372行目)
     */
    public void showLogWindow(Object sender) {
        // TODO: ログウィンドウ表示実装
    }

    /**
     * コピー
     * HOROS-20240407準拠: - (IBAction) copy: (id)sender (8809行目)
     */
    public void copy(Object sender) {
        // TODO: コピー実装
    }

    /**
     * ペースト
     * HOROS-20240407準拠: - (IBAction) paste: (id)sender (8802行目)
     */
    public void paste(Object sender) {
        // TODO: ペースト実装
    }

    // ========== 2D Viewerメニューアクションメソッド ==========

    /**
     * シリーズを読み込み
     * HOROS-20240407準拠: - (IBAction) loadSerie: (id) sender
     */
    public void loadSerie(Object sender) {
        // TODO: シリーズ読み込み実装
    }

    /**
     * 患者を読み込み
     * HOROS-20240407準拠: - (IBAction) loadPatient: (id) sender
     */
    public void loadPatient(Object sender) {
        // TODO: 患者読み込み実装
    }

    /**
     * BW変換メニュー
     * HOROS-20240407準拠: - (IBAction) ConvertToBWMenu: (id) sender
     */
    public void ConvertToBWMenu(Object sender) {
        // TODO: BW変換実装
    }

    /**
     * RGB変換メニュー
     * HOROS-20240407準拠: - (IBAction) ConvertToRGBMenu: (id) sender
     */
    public void ConvertToRGBMenu(Object sender) {
        // TODO: RGB変換実装
    }

    /**
     * 画像をリセット
     * HOROS-20240407準拠: - (IBAction) resetImage: (id) sender
     */
    public void resetImage(Object sender) {
        // TODO: 画像リセット実装
    }

    /**
     * シリーズを元に戻す
     * HOROS-20240407準拠: - (IBAction) revertSeries: (id) sender
     */
    public void revertSeries(Object sender) {
        // TODO: シリーズ元に戻す実装
    }

    /**
     * 実際のサイズ
     * HOROS-20240407準拠: - (IBAction) actualSize: (id) sender
     */
    public void actualSize(Object sender) {
        // TODO: 実際のサイズ実装
    }

    /**
     * 実サイズ
     * HOROS-20240407準拠: - (IBAction) realSize: (id) sender
     */
    public void realSize(Object sender) {
        // TODO: 実サイズ実装
    }

    /**
     * フィットにスケール
     * HOROS-20240407準拠: - (IBAction) scaleToFit: (id) sender
     */
    public void scaleToFit(Object sender) {
        // TODO: フィットにスケール実装
    }

    /**
     * シリーズをソート
     * HOROS-20240407準拠: - (IBAction) sortSeriesByValue: (id) sender
     */
    public void sortSeriesByValue(Object sender) {
        // TODO: シリーズソート実装
    }

    /**
     * 水平反転
     * HOROS-20240407準拠: - (IBAction) flipHorizontal: (id) sender
     */
    public void flipHorizontal(Object sender) {
        // TODO: 水平反転実装
    }

    /**
     * 垂直反転
     * HOROS-20240407準拠: - (IBAction) flipVertical: (id) sender
     */
    public void flipVertical(Object sender) {
        // TODO: 垂直反転実装
    }

    /**
     * 0度回転
     * HOROS-20240407準拠: - (IBAction) rotate0: (id) sender
     */
    public void rotate0(Object sender) {
        // TODO: 0度回転実装
    }

    /**
     * 90度回転
     * HOROS-20240407準拠: - (IBAction) rotate90: (id) sender
     */
    public void rotate90(Object sender) {
        // TODO: 90度回転実装
    }

    /**
     * 180度回転
     * HOROS-20240407準拠: - (IBAction) rotate180: (id) sender
     */
    public void rotate180(Object sender) {
        // TODO: 180度回転実装
    }

    /**
     * キャリブレート
     * HOROS-20240407準拠: - (IBAction) calibrate: (id) sender
     */
    public void calibrate(Object sender) {
        // TODO: キャリブレート実装
    }

    /**
     * 3Dパネル
     * HOROS-20240407準拠: - (IBAction) threeDPanel: (id) sender
     */
    public void threeDPanel(Object sender) {
        // TODO: 3Dパネル実装
    }

    /**
     * ナビゲータ
     * HOROS-20240407準拠: - (IBAction) navigator: (id) sender
     */
    public void navigator(Object sender) {
        // TODO: ナビゲータ実装
    }

    /**
     * データシリーズを反転
     * HOROS-20240407準拠: - (IBAction) flipDataSeries: (id) sender
     */
    public void flipDataSeries(Object sender) {
        // TODO: データシリーズ反転実装
    }

    /**
     * VOI LUTを使用
     * HOROS-20240407準拠: - (IBAction) useVOILUT: (id) sender
     */
    public void useVOILUT(Object sender) {
        // TODO: VOI LUT使用実装
    }

    /**
     * SUVを表示
     * HOROS-20240407準拠: - (IBAction) displaySUV: (id) sender
     */
    public void displaySUV(Object sender) {
        // TODO: SUV表示実装
    }

    /**
     * ウィンドウをブレンド
     * HOROS-20240407準拠: - (IBAction) blendWindows: (id) sender
     */
    public void blendWindows(Object sender) {
        // TODO: ウィンドウブレンド実装
    }

    /**
     * 融合画像をマージ
     * HOROS-20240407準拠: - (IBAction) mergeFusedImages: (id) sender
     */
    public void mergeFusedImages(Object sender) {
        // TODO: 融合画像マージ実装
    }

    /**
     * 設定を他にコピー
     * HOROS-20240407準拠: - (IBAction) copySettingsToOthers: (id) sender
     */
    public void copySettingsToOthers(Object sender) {
        // TODO: 設定コピー実装
    }

    /**
     * シリーズ内で設定を切り替え
     * HOROS-20240407準拠: - (IBAction) switchCopySettingsInSeries: (id) sender
     */
    public void switchCopySettingsInSeries(Object sender) {
        // TODO: シリーズ内設定切り替え実装
    }

    /**
     * 同期
     * HOROS-20240407準拠: - (IBAction) syncronize: (id) sender
     */
    public void syncronize(Object sender) {
        // TODO: 同期実装
    }

    /**
     * 常に同期メニュー
     * HOROS-20240407準拠: - (IBAction) alwaysSyncMenu: (id) sender
     */
    public void alwaysSyncMenu(Object sender) {
        // TODO: 常に同期メニュー実装
    }

    /**
     * シリーズを同期
     * HOROS-20240407準拠: - (IBAction) SyncSeries: (id) sender
     */
    public void SyncSeries(Object sender) {
        // TODO: シリーズ同期実装
    }

    /**
     * キー画像を設定
     * HOROS-20240407準拠: - (IBAction) setKeyImage: (id) sender
     */
    public void setKeyImage(Object sender) {
        // TODO: キー画像設定実装
    }

    /**
     * 全ての画像をキー画像に設定
     * HOROS-20240407準拠: - (IBAction) setAllKeyImages: (id) sender
     */
    public void setAllKeyImages(Object sender) {
        // TODO: 全ての画像をキー画像に設定実装
    }

    /**
     * 全ての画像を非キー画像に設定
     * HOROS-20240407準拠: - (IBAction) setAllNonKeyImages: (id) sender
     */
    public void setAllNonKeyImages(Object sender) {
        // TODO: 全ての画像を非キー画像に設定実装
    }

    /**
     * ROI画像をキー画像に設定
     * HOROS-20240407準拠: - (IBAction) setROIsImagesKeyImages: (id) sender
     */
    public void setROIsImagesKeyImages(Object sender) {
        // TODO: ROI画像をキー画像に設定実装
    }

    /**
     * キャプチャしてキー画像に設定
     * HOROS-20240407準拠: - (IBAction) captureAndSetKeyImage: (id) sender
     */
    public void captureAndSetKeyImage(Object sender) {
        // TODO: キャプチャしてキー画像に設定実装
    }

    /**
     * 次の/前のキー画像を検索
     * HOROS-20240407準拠: - (IBAction) findNextPreviousKeyImage: (id) sender
     */
    public void findNextPreviousKeyImage(Object sender) {
        // TODO: 次の/前のキー画像検索実装
    }

    // ========== 3D Viewerメニューアクションメソッド ==========

    /**
     * MPRビューア
     * HOROS-20240407準拠: - (IBAction) mprViewer: (id) sender
     */
    public void mprViewer(Object sender) {
        // TODO: MPRビューア実装
    }

    /**
     * CPRビューア
     * HOROS-20240407準拠: - (IBAction) cprViewer: (id) sender
     */
    public void cprViewer(Object sender) {
        // TODO: CPRビューア実装
    }

    /**
     * 直交MPRビューア
     * HOROS-20240407準拠: - (IBAction) orthogonalMPRViewer: (id) sender
     */
    public void orthogonalMPRViewer(Object sender) {
        // TODO: 直交MPRビューア実装
    }

    /**
     * VRビューア
     * HOROS-20240407準拠: - (IBAction) VRViewer: (id) sender
     */
    public void VRViewer(Object sender) {
        // TODO: VRビューア実装
    }

    /**
     * SRビューア
     * HOROS-20240407準拠: - (IBAction) SRViewer: (id) sender
     */
    public void SRViewer(Object sender) {
        // TODO: SRビューア実装
    }

    /**
     * 内視鏡ビューア
     * HOROS-20240407準拠: - (IBAction) endoscopyViewer: (id) sender
     */
    public void endoscopyViewer(Object sender) {
        // TODO: 内視鏡ビューア実装
    }

    // ========== ROIメニューアクションメソッド ==========

    /**
     * ROIをファイルから読み込み
     * HOROS-20240407準拠: - (IBAction) roiLoadFromFiles: (id) sender
     */
    public void roiLoadFromFiles(Object sender) {
        // TODO: ROIファイル読み込み実装
    }

    /**
     * 選択されたROIを保存
     * HOROS-20240407準拠: - (IBAction) roiSaveSelected: (id) sender
     */
    public void roiSaveSelected(Object sender) {
        // TODO: 選択ROI保存実装
    }

    /**
     * シリーズのROIを保存
     * HOROS-20240407準拠: - (IBAction) roiSaveSeries: (id) sender
     */
    public void roiSaveSeries(Object sender) {
        // TODO: シリーズROI保存実装
    }

    /**
     * 全てのROIを削除
     * HOROS-20240407準拠: - (IBAction) roiDeleteAll: (id) sender
     */
    public void roiDeleteAll(Object sender) {
        // TODO: 全てのROI削除実装
    }

    /**
     * 同じ名前のROIを削除
     * HOROS-20240407準拠: - (IBAction) roiDeleteAllROIsWithSameName: (id) sender
     */
    public void roiDeleteAllROIsWithSameName(Object sender) {
        // TODO: 同じ名前のROI削除実装
    }

    /**
     * ROIを選択/選択解除
     * HOROS-20240407準拠: - (IBAction) roiSelectDeselectAll: (id) sender
     */
    public void roiSelectDeselectAll(Object sender) {
        // TODO: ROI選択/選択解除実装
    }

    /**
     * ROIマネージャーを取得
     * HOROS-20240407準拠: - (IBAction) roiGetManager: (id) sender
     */
    public void roiGetManager(Object sender) {
        // TODO: ROIマネージャー取得実装
    }

    /**
     * ROI情報を取得
     * HOROS-20240407準拠: - (IBAction) roiGetInfo: (id) sender
     */
    public void roiGetInfo(Object sender) {
        // TODO: ROI情報取得実装
    }

    /**
     * ROIをリネーム
     * HOROS-20240407準拠: - (IBAction) roiRename: (id) sender
     */
    public void roiRename(Object sender) {
        // TODO: ROIリネーム実装
    }

    /**
     * ROIデフォルト
     * HOROS-20240407準拠: - (IBAction) roiDefaults: (id) sender
     */
    public void roiDefaults(Object sender) {
        // TODO: ROIデフォルト実装
    }

    /**
     * ウィンドウを表示
     * HOROS-20240407準拠: - (IBAction) showWindow: (id) sender
     */
    public void showWindow(Object sender) {
        // TODO: ウィンドウ表示実装
    }

    // HOROS-20240407準拠: 列ヘッダーのクリック処理はNSOutlineViewが自動的に処理
    // Java Swingでは、JXTreeTableが自動的にヘッダークリックを処理するため、
    // カスタムのMouseListenerは不要（HOROS-20240407準拠）
    // setupColumnHeaderListener()とhandleColumnHeaderClick()は削除

    /**
     * 列メニューを構築
     * HOROS-20240407準拠: BrowserController.m 6183-6204行目 - (void)buildColumnsMenu
     */
    private void buildColumnsMenu() {
        if (databaseOutline == null) {
            return;
        }
        
        // HOROS-20240407準拠: columnsMenuを初期化
        columnsMenu = new javax.swing.JPopupMenu();
        
        // HOROS-20240407準拠: すべての列を取得して、ヘッダー名でソート
        javax.swing.table.TableColumnModel columnModel = databaseOutline.getColumnModel();
        List<javax.swing.table.TableColumn> columns = new ArrayList<>();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            columns.add(columnModel.getColumn(i));
        }
        
        // HOROS-20240407準拠: ヘッダー名でソート
        columns.sort((a, b) -> {
            String nameA = a.getHeaderValue() != null ? a.getHeaderValue().toString() : "";
            String nameB = b.getHeaderValue() != null ? b.getHeaderValue().toString() : "";
            return nameA.compareTo(nameB);
        });
        
        // HOROS-20240407準拠: 各列に対してメニューアイテムを追加
        for (javax.swing.table.TableColumn column : columns) {
            Object identifier = column.getIdentifier();
            String headerValue = column.getHeaderValue() != null ? column.getHeaderValue().toString() : "";
            
            if (identifier != null && !headerValue.isEmpty()) {
                javax.swing.JCheckBoxMenuItem item = new javax.swing.JCheckBoxMenuItem(headerValue);
                item.putClientProperty("columnIdentifier", identifier.toString());
                // HOROS-20240407準拠: メニューアイテムを有効化
                item.setEnabled(true);
                // HOROS-20240407準拠: メニューアイテムがクリックされたときにcolumnsMenuActionを呼ぶ
                item.addActionListener(e -> {
                    // HOROS-20240407準拠: ActionListenerが呼ばれた時点で既にチェック状態が変更されている
                    columnsMenuAction(item);
                });
                columnsMenu.add(item);
            }
        }
        
        // HOROS-20240407準拠: ヘッダーに右クリックリスナーを追加
        // 【重要】列の移動とリサイズを妨害しないよう、右クリックのみを処理する
        // 左クリックの標準動作（列の移動、リサイズ、ソート）を妨害しない
        // 【修正】既存のリスナーを削除しない（JTableHeaderの標準リスナーを保持する）
        javax.swing.table.JTableHeader header = databaseOutline.getTableHeader();
        if (header != null) {
            // 【重要】既存のMouseListenerを削除しない
            // JTableHeaderの標準リスナー（列の移動、リサイズ、ソート）を保持する
            // 右クリックのみを処理するリスナーを追加（左クリックの標準動作を妨害しない）
            header.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    // 右クリックのみを処理（左クリックの標準動作を妨害しない）
                    if (e.isPopupTrigger() || e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                        showColumnsMenu(e);
                        // 右クリックのイベントを消費しない（標準動作を妨害しない）
                        // e.consume()は呼ばない
                    }
                }
                
                @Override
                public void mouseReleased(java.awt.event.MouseEvent e) {
                    // 右クリックのみを処理（左クリックの標準動作を妨害しない）
                    if (e.isPopupTrigger() || e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                        showColumnsMenu(e);
                        // 右クリックのイベントを消費しない（標準動作を妨害しない）
                        // e.consume()は呼ばない
                    }
                }
            });
        }
    }
    
    /**
     * 列メニューを表示
     * HOROS-20240407準拠: BrowserController.m 13985-13990行目 - menuWillOpen:
     */
    private void showColumnsMenu(java.awt.event.MouseEvent e) {
        if (columnsMenu == null || databaseOutline == null) {
            return;
        }
        
        // HOROS-20240407準拠: columnsMenuWillOpen相当の処理
        columnsMenuWillOpen();
        
        // HOROS-20240407準拠: メニューを表示
        columnsMenu.show(e.getComponent(), e.getX(), e.getY());
    }
    
    /**
     * 列メニューが開かれる前に呼ばれる
     * HOROS-20240407準拠: BrowserController.m 6206-6226行目 - (void)columnsMenuWillOpen
     */
    private void columnsMenuWillOpen() {
        if (columnsMenu == null || databaseOutline == null) {
            return;
        }
        
        // HOROS-20240407準拠: 各メニューアイテムのチェック状態を更新
        for (int i = 0; i < columnsMenu.getComponentCount(); i++) {
            java.awt.Component comp = columnsMenu.getComponent(i);
            if (comp instanceof javax.swing.JCheckBoxMenuItem) {
                javax.swing.JCheckBoxMenuItem item = (javax.swing.JCheckBoxMenuItem) comp;
                Object identifierObj = item.getClientProperty("columnIdentifier");
                if (identifierObj instanceof String) {
                    String identifier = (String) identifierObj;
                    
                    // HOROS-20240407準拠: name列の場合はHIDEPATIENTNAME設定を確認
                    if ("name".equals(identifier)) {
                        // TODO: HIDEPATIENTNAME設定を確認（現在は常に表示）
                        item.setSelected(true);
                    } else {
                        // HOROS-20240407準拠: その他の列はisColumnWithIdentifierVisibleで確認
                        boolean visible = databaseOutline.isColumnWithIdentifierVisible(identifier);
                        item.setSelected(visible);
                    }
                }
            }
        }
    }
    
    /**
     * 列メニューアイテムがクリックされたとき
     * HOROS-20240407準拠: BrowserController.m 6228-6250行目 - (void)columnsMenuAction:(id)sender
     * HOROS-20240407準拠: [sender setState: ![sender state]] - チェック状態を切り替え
     * Java Swingでは、ActionListenerが呼ばれた時点で既にチェック状態が変更されているため、
     * 現在の状態（既に変更された後）をそのまま使用する
     */
    private void columnsMenuAction(javax.swing.JCheckBoxMenuItem sender) {
        if (databaseOutline == null) {
            return;
        }
        
        // HOROS-20240407準拠: チェック状態を切り替え
        // Java Swingでは、ActionListenerが呼ばれた時点で既にチェック状態が変更されているため、
        // 現在の状態（既に変更された後）をそのまま使用する
        boolean newState = sender.isSelected();
        
        Object identifierObj = sender.getClientProperty("columnIdentifier");
        if (!(identifierObj instanceof String)) {
            return;
        }
        
        String identifier = (String) identifierObj;
        
        // HOROS-20240407準拠: name列の場合はHIDEPATIENTNAMEを設定
        if ("name".equals(identifier)) {
            // TODO: HIDEPATIENTNAME設定を保存（現在は常に表示）
            // Preferences prefs = Preferences.userNodeForPackage(BrowserController.class);
            // prefs.putBoolean("HIDEPATIENTNAME", !newState);
        } else {
            // HOROS-20240407準拠: その他の列はCOLUMNSDATABASEに保存してrefreshColumnsを呼ぶ
            saveColumnsDatabaseState();
            refreshColumns();
        }
    }
    
    /**
     * 列の表示/非表示状態を保存
     * HOROS-20240407準拠: BrowserController.m 6228-6246行目 - columnsMenuAction内の保存処理
     */
    private void saveColumnsDatabaseState() {
        if (columnsMenu == null || databaseOutline == null) {
            return;
        }
        
        try {
            // HOROS-20240407準拠: COLUMNSDATABASEに保存
            Preferences prefs = Preferences.userNodeForPackage(BrowserController.class);
            java.util.Map<String, Integer> columnsDatabase = new java.util.HashMap<>();
            
            for (int i = 0; i < columnsMenu.getComponentCount(); i++) {
                java.awt.Component comp = columnsMenu.getComponent(i);
                if (comp instanceof javax.swing.JCheckBoxMenuItem) {
                    javax.swing.JCheckBoxMenuItem item = (javax.swing.JCheckBoxMenuItem) comp;
                    String title = item.getText();
                    if (title != null && !title.isEmpty()) {
                        // HOROS-20240407準拠: チェック状態を保存（1=表示、0=非表示）
                        columnsDatabase.put(title, item.isSelected() ? 1 : 0);
                    }
                }
            }
            
            // HOROS-20240407準拠: Mapを文字列にシリアライズ（簡易実装）
            StringBuilder sb = new StringBuilder();
            for (java.util.Map.Entry<String, Integer> entry : columnsDatabase.entrySet()) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(entry.getKey()).append(":").append(entry.getValue());
            }
            prefs.put("COLUMNSDATABASE", sb.toString());
            prefs.flush();
        } catch (Exception e) {
            // エラーが発生した場合は保存をスキップ
        }
    }
    
    /**
     * 列の表示/非表示を更新
     * HOROS-20240407準拠: BrowserController.m 6252-6294行目 - (void)refreshColumns
     */
    private void refreshColumns() {
        if (databaseOutline == null) {
            return;
        }
        
        try {
            // HOROS-20240407準拠: COLUMNSDATABASEから設定を読み込む
            Preferences prefs = Preferences.userNodeForPackage(BrowserController.class);
            String columnsDatabaseStr = prefs.get("COLUMNSDATABASE", null);
            
            if (columnsDatabaseStr == null || columnsDatabaseStr.isEmpty()) {
                return;
            }
            
            // HOROS-20240407準拠: 文字列をMapにパース（簡易実装）
            java.util.Map<String, Integer> columnsDatabase = new java.util.HashMap<>();
            String[] parts = columnsDatabaseStr.split(",");
            for (String part : parts) {
                String[] keyValue = part.split(":", 2);
                if (keyValue.length == 2) {
                    try {
                        columnsDatabase.put(keyValue[0], Integer.parseInt(keyValue[1]));
                    } catch (NumberFormatException e) {
                        // 無効な数値はスキップ
                    }
                }
            }
            
            // HOROS-20240407準拠: 各列の表示/非表示を設定
            javax.swing.table.TableColumnModel columnModel = databaseOutline.getColumnModel();
            for (int i = 0; i < columnModel.getColumnCount(); i++) {
                javax.swing.table.TableColumn column = columnModel.getColumn(i);
                String headerValue = column.getHeaderValue() != null ? column.getHeaderValue().toString() : "";
                Object identifier = column.getIdentifier();
                
                if (headerValue != null && !headerValue.isEmpty() && identifier != null) {
                    Integer state = columnsDatabase.get(headerValue);
                    if (state != null) {
                        boolean visible = state.intValue() != 0;
                        
                        // HOROS-20240407準拠: name列は常に表示（非表示にできない）
                        if ("name".equals(identifier.toString())) {
                            // name列は常に表示
                            continue;
                        }
                        
                        // HOROS-20240407準拠: 列の表示/非表示を設定
                        boolean currentVisible = databaseOutline.isColumnWithIdentifierVisible(identifier.toString());
                        if (currentVisible != visible) {
                            databaseOutline.setColumnWithIdentifierVisible(identifier.toString(), visible);
                            
                            // HOROS-20240407準拠: 選択されている列が非表示になる場合は最初の列を選択
                            if (!visible && databaseOutline.getSelectedColumn() == i) {
                                if (databaseOutline.getRowCount() > 0) {
                                    databaseOutline.setRowSelectionInterval(0, 0);
                                }
                            }
                        }
                    }
                }
            }
            
            // HOROS-20240407準拠: 列メニューを再構築
            buildColumnsMenu();
        } catch (Exception e) {
            // エラーが発生した場合はスキップ
        }
    }
}
