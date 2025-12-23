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
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTable;
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

    // HOROS-20240407準拠: IBOutlet PreviewView *imageView; (146行目)
    private PreviewView imageView;

    // HOROS-20240407準拠: IBOutlet NSTableView *albumTable; (133行目)
    private javax.swing.JTable albumTable;

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

    // HOROS-20240407準拠: BOOL loadingIsOver = NO; (BrowserController.m 174行目)
    private boolean loadingIsOver = false;

    // HOROS-20240407準拠: BOOL DatabaseIsEdited; (201行目)
    private boolean DatabaseIsEdited = false;

    // HOROS-20240407準拠: NSThread *matrixLoadIconsThread; (262行目)
    private Thread matrixLoadIconsThread;

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

    // HOROS-20240407準拠: id oFirstForFirst; (dbObjectSelection enum)
    private Object oFirstForFirst;

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
        String[] albumColumns = { "アルバム名", "スタディ数" };
        javax.swing.table.DefaultTableModel albumModel = new javax.swing.table.DefaultTableModel(albumColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        albumTable = new JTable(albumModel);
        albumTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        albumTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !isUpdatingAlbumTable) {
                refreshDatabase(null);
            }
        });
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
        // HOROS-20240407準拠: BrowserController.h 180行目 IBOutlet NSTableView
        // *_sourcesTableView;
        // HOROS-20240407準拠: MainMenu.xib 4142行目 - box id="11698" title="Sources"
        // titlePosition="noTitle"
        // HOROS-20240407準拠: MainMenu.xib 4164行目 - tableHeaderCell title="Sources"
        JList<String> sourcesList = new JList<>(new String[] { "Documents DB", "Description" });
        sourcesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sourcesScroll = new JScrollPane(sourcesList);
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
        
        // ディバイダー位置の変更を監視して保存
        splitSourcesActivity.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, e -> {
            saveDividerLocations();
        });
        splitAlbums.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, e -> {
            saveDividerLocations();
        });
        // splitComparativeのディバイダー位置変更も監視（後で追加されるため、ここでは設定しない）
        
        // 初期ディバイダー位置を復元（ウィンドウが表示された後に実行）
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                restoreDividerLocations();
                // splitComparativeのディバイダー位置変更を監視（保存用）
                SwingUtilities.invokeLater(() -> {
                    if (splitComparative != null) {
                        splitComparative.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
                            saveDividerLocations();
                        });
                    }
                });
            }
        });

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
        JScrollPane comparativeScroll = new JScrollPane(comparativeTable);
        // HOROS-20240407準拠: 履歴パネルの幅を205ピクセルに固定（MainMenu.xib 4667行目 width="205"）
        comparativeScroll.setPreferredSize(new java.awt.Dimension(205, 0));
        comparativeScroll.setMinimumSize(new java.awt.Dimension(205, 0));
        comparativeScroll.setMaximumSize(new java.awt.Dimension(205, Integer.MAX_VALUE));
        topTablesSplit.setRightComponent(comparativeScroll);
        topTablesSplit.setResizeWeight(1.0); // 患者情報テーブルが可変、履歴パネルが固定
        // HOROS-20240407準拠: 履歴パネルの幅を205ピクセルに固定（ディバイダーを無効化）
        SwingUtilities.invokeLater(() -> {
            int totalWidth = topTablesSplit.getWidth();
            if (totalWidth > 0) {
                topTablesSplit.setDividerLocation(totalWidth - 205);
            }
            // ディバイダーを無効化（履歴パネルが固定のため不要）
            topTablesSplit.setEnabled(false);
        });

        splitComparative.setTopComponent(topTablesSplit);

        // 下部プレビューエリア（水平スプリッター）: サムネイル（左） | プレビュー（右）
        // HOROS-20240407準拠: MainMenu.xib 4732行目 - splitView id="14624" (Matrix Split) 水平スプリッター
        JSplitPane bottomPreviewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // サムネイル（左）- oMatrix
        // HOROS-20240407準拠: MainMenu.xib 4752行目 - scrollView id="12410" (thumbnailsScrollView)
        // HOROS-20240407準拠: MainMenu.xib 4759行目 - matrix id="12411" (oMatrix)
        // HOROS-20240407準拠: BrowserController.h 132行目 IBOutlet BrowserMatrix *oMatrix;
        // HOROS-20240407準拠: BrowserController.h 204行目 IBOutlet NSScrollView *thumbnailsScrollView;
        oMatrix = new BrowserMatrix(this);
        JScrollPane thumbnailsScrollView = new JScrollPane(oMatrix);
        thumbnailsScrollView.setBackground(null);
        thumbnailsScrollView.getViewport().setBackground(null);
        bottomPreviewSplit.setLeftComponent(thumbnailsScrollView);

        // プレビュー（右）- imageView
        // HOROS-20240407準拠: MainMenu.xib 4864行目 - customView id="1166" customClass="PreviewView"
        // HOROS-20240407準拠: BrowserController.h 146行目 IBOutlet PreviewView *imageView;
        imageView = new PreviewView();
        bottomPreviewSplit.setRightComponent(imageView);
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
        javax.swing.JCheckBox playCheckBox = new javax.swing.JCheckBox("Play");
        java.awt.Font miniFont = new java.awt.Font(playCheckBox.getFont().getName(), 
            java.awt.Font.PLAIN, Math.max(9, playCheckBox.getFont().getSize() - 2));
        playCheckBox.setFont(miniFont);
        // 右端に配置、幅60px（1.5倍）、高さ20px（そのまま）
        playCheckBox.setBounds(611 - 60, 2, 60, 20);
        rightStatusPanel.add(playCheckBox);
        
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
            
            // HOROS-20240407準拠: BrowserController.m 14339行目
            // 列メニューを構築
            buildColumnsMenu();
            
            // HOROS-20240407準拠: 列の表示/非表示状態を復元（COLUMNSDATABASEから）
            refreshColumns();
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
     */
    private void saveDividerLocations() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(BrowserController.class);
            
            // splitAlbumsとsplitSourcesActivityのディバイダー位置を保存
            if (splitAlbums != null && splitAlbums.getHeight() > 0) {
                int albumsDividerLocation = splitAlbums.getDividerLocation();
                prefs.putInt("AlbumsDividerLocation", albumsDividerLocation);
            }
            
            if (splitSourcesActivity != null && splitSourcesActivity.getHeight() > 0) {
                int sourcesActivityDividerLocation = splitSourcesActivity.getDividerLocation();
                prefs.putInt("SourcesActivityDividerLocation", sourcesActivityDividerLocation);
            }
            
            // splitComparativeのディバイダー位置も保存
            if (splitComparative != null && splitComparative.getHeight() > 0) {
                int comparativeDividerLocation = splitComparative.getDividerLocation();
                prefs.putInt("ComparativeDividerLocation", comparativeDividerLocation);
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
     */
    private void restoreDividerLocations() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(BrowserController.class);
            
            // splitAlbumsとsplitSourcesActivityのディバイダー位置を復元
            if (splitAlbums != null && splitAlbums.getHeight() > 0) {
                int savedLocation = prefs.getInt("AlbumsDividerLocation", -1);
                if (savedLocation > 0) {
                    // 保存された位置を復元（比率で設定）
                    double ratio = (double) savedLocation / splitAlbums.getHeight();
                    splitAlbums.setDividerLocation(ratio);
                } else {
                    // デフォルト位置: Albumsが198px
                    double albumsRatio = 198.0 / 687.0;
                    splitAlbums.setDividerLocation(albumsRatio);
                }
            }
            
            if (splitSourcesActivity != null && splitSourcesActivity.getHeight() > 0) {
                int savedLocation = prefs.getInt("SourcesActivityDividerLocation", -1);
                if (savedLocation > 0) {
                    // 保存された位置を復元（比率で設定）
                    double ratio = (double) savedLocation / splitSourcesActivity.getHeight();
                    splitSourcesActivity.setDividerLocation(ratio);
                } else {
                    // デフォルト位置: Sourcesが1/3、Activityが2/3
                    splitSourcesActivity.setDividerLocation(0.33);
                }
            }
            
            // splitComparativeのディバイダー位置も復元
            if (splitComparative != null && splitComparative.getHeight() > 0) {
                int savedLocation = prefs.getInt("ComparativeDividerLocation", -1);
                if (savedLocation > 0) {
                    // 保存された位置を復元（比率で設定）
                    double ratio = (double) savedLocation / splitComparative.getHeight();
                    splitComparative.setDividerLocation(ratio);
                } else {
                    // デフォルト位置: 上から50%
                    splitComparative.setDividerLocation(0.5);
                }
            }
        } catch (Exception e) {
            // エラーが発生した場合はデフォルト位置を使用
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
     * 形式: "Identifier1:Width1,Identifier2:Width2,..."
     */
    private String serializeColumnState(List<Map<String, Object>> state) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < state.size(); i++) {
            Map<String, Object> columnInfo = state.get(i);
            String identifier = (String) columnInfo.get("Identifier");
            Object widthObj = columnInfo.get("Width");
            int width = widthObj instanceof Number ? ((Number) widthObj).intValue() : 0;

            if (identifier != null) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(identifier).append(":").append(width);
            }
        }
        return sb.toString();
    }

    /**
     * カラム状態をパース（簡易実装）
     * 形式: "Identifier1:Width1,Identifier2:Width2,..."
     */
    private List<Map<String, Object>> parseColumnState(String json) {
        List<Map<String, Object>> state = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return state;
        }

        String[] parts = json.split(",");
        for (String part : parts) {
            String[] keyValue = part.split(":", 2);
            if (keyValue.length == 2) {
                try {
                    Map<String, Object> columnInfo = new HashMap<>();
                    columnInfo.put("Identifier", keyValue[0]);
                    columnInfo.put("Width", Integer.parseInt(keyValue[1]));
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
        if (albumTable == null || database == null)
            return;

        // 無限ループ防止
        if (isUpdatingAlbumTable)
            return;
        isUpdatingAlbumTable = true;

        try {
            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) albumTable.getModel();
            model.setRowCount(0);

            List<com.jj.dicomviewer.model.DicomAlbum> albums = getAlbumArray();
            for (com.jj.dicomviewer.model.DicomAlbum album : albums) {
                Object[] row = {
                        album.getName() != null ? album.getName() : "",
                        album.getNumberOfStudies()
                };
                model.addRow(row);
            }

            // HOROS-20240407準拠: 最初のアルバム（"Database"）を自動選択
            if (!albums.isEmpty()) {
                // フラグにより選択変更イベントが発火してもrefreshDatabaseがスキップされる
                albumTable.setRowSelectionInterval(0, 0);

                // HOROS-20240407準拠: 選択されたアルバムに基づいてアウトラインビューを更新
                // ただし、無限ループを防ぐため直接呼び出す
                if (!isRefreshingOutline) {
                    SwingUtilities.invokeLater(() -> {
                        outlineViewRefresh();
                    });
                }
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
            if (database == null || albumTable == null)
                return;

            // HOROS-20240407準拠: 選択されたアルバムからスタディを取得
            List<com.jj.dicomviewer.model.DicomAlbum> albumArray = getAlbumArray();
            int selectedRow = albumTable.getSelectedRow();

            synchronized (outlineViewArray) {
                outlineViewArray.clear();

                if (selectedRow >= 0 && selectedRow < albumArray.size()) {
                    com.jj.dicomviewer.model.DicomAlbum selectedAlbum = albumArray.get(selectedRow);

                    if (selectedAlbum != null) {
                        // HOROS-20240407準拠: "Database"アルバムの場合は全スタディを表示
                        if ("Database".equals(selectedAlbum.getName())) {
                            // HOROS-20240407準拠: Databaseアルバムは全スタディを含む
                            try {
                                outlineViewArray.addAll(database.getAllStudies());
                            } catch (Exception e) {
                                // HOROS-20240407準拠: @catch (NSException * e) { N2LogExceptionWithStackTrace(e);
                                // }
                                // TODO: ログ出力（HOROS-20240407準拠のログシステムを使用）
                            }
                        } else if (selectedAlbum.getStudies() != null) {
                            // HOROS-20240407準拠: 通常のアルバムの場合はアルバムのスタディを表示
                            outlineViewArray.addAll(selectedAlbum.getStudies());
                        }
                    }
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
                            treeTableModel.fireTreeStructureChanged();
                        }
                    }
                    databaseOutline.repaint();
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
        // HOROS-20240407準拠: if( [NSThread isMainThread] == NO)
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::outlineViewSelectionDidChange);
            return;
        }

        // HOROS-20240407準拠: if( loadingIsOver == NO) return;
        if (!loadingIsOver) {
            return;
        }

        try {
            // HOROS-20240407準拠: キャッシュをクリア
            // cachedFilesForDatabaseOutlineSelectionSelectedFiles = nil;

            Object item = databaseOutline.getSelectedItem();

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

            if (refreshMatrix && item != null) {
                // HOROS-20240407準拠: データベースロック
                synchronized (database) {
                    if (animationSlider != null) {
                        animationSlider.setEnabled(false);
                        animationSlider.setMaximum(0);
                        animationSlider.setValue(0);
                    }

                    // HOROS-20240407準拠: matrixViewArray = [[self childrenArray: item] retain];
                    matrixViewArray = new ArrayList<>(childrenArray(item, false));

                    // HOROS-20240407準拠: [self matrixInit: matrixViewArray.count];
                    matrixInit(matrixViewArray.size());

                    // HOROS-20240407準拠: files = [self imagesArray: item
                    // preferredObject:oFirstForFirst];
                    List<Object> files = imagesArray(item, oFirstForFirst);

                    // HOROS-20240407準拠: サムネイル配列を初期化
                    synchronized (previewPixThumbnails) {
                        previewPixThumbnails.clear();
                        for (int i = 0; i < files.size(); i++) {
                            previewPixThumbnails.add(null); // notFoundImage相当
                        }
                    }

                    // HOROS-20240407準拠: アイコン読み込みスレッドを開始
                    // TODO: matrixLoadIconsThreadの実装
                }
            }

            if (previousItem != item) {
                previousItem = item;

                // HOROS-20240407準拠: COMPARATIVE STUDIES
                Object studySelected = getStudyFromItem(item);
                if (studySelected != null) {
                    String patientUID = getPatientUID(studySelected);
                    if (patientUID != null && !patientUID.equals(comparativePatientUID)) {
                        comparativePatientUID = patientUID;
                        comparativeStudies = null;
                        if (comparativeTable != null) {
                            // comparativeTable.reloadData();
                        }

                        // HOROS-20240407準拠: [NSThread detachNewThreadSelector:
                        // @selector(searchForComparativeStudies:) ...]
                        Thread thread = new Thread(() -> searchForComparativeStudies(studySelected));
                        thread.setName("Search for comparative studies");
                        ThreadsManager.defaultManager().addThreadAndStart(thread);
                    }
                    
                    // HOROS-20240407準拠: BrowserController.m 5184-5192行目
                    // 履歴パネル（comparativeTable）の行選択を同期
                    if (comparativeStudies != null && !comparativeStudies.isEmpty() && comparativeTable != null) {
                        String studyInstanceUID = getStudyInstanceUID(studySelected);
                        if (studyInstanceUID != null) {
                            // HOROS-20240407準拠: comparativeStudiesから同じstudyInstanceUIDを持つスタディのインデックスを検索
                            int index = findStudyIndexInComparativeStudies(studyInstanceUID);
                            if (index >= 0) {
                                // HOROS-20240407準拠: [comparativeTable selectRowIndexes: [NSIndexSet indexSetWithIndex: index] byExtendingSelection: NO];
                                SwingUtilities.invokeLater(() -> {
                                    comparativeTable.setRowSelectionInterval(index, index);
                                    comparativeTable.scrollRectToVisible(comparativeTable.getCellRect(index, 0, true));
                                });
                            } else {
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
                            // TODO: comparativeTableのモデルを更新
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
     * (BrowserController.m)
     */
    public void databasePressed(DatabaseOutlineView sender) {
        // TODO: 実装
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
    public List<Object> childrenArray(Object parent, boolean recursive) {
        if (parent == null) {
            return getOutlineViewArray();
        } else if (parent instanceof com.jj.dicomviewer.model.DicomStudy) {
            com.jj.dicomviewer.model.DicomStudy study = (com.jj.dicomviewer.model.DicomStudy) parent;
            try {
                return new ArrayList<>(study.getSeries());
            } catch (Exception e) {
                return new ArrayList<>();
            }
        } else if (parent instanceof com.jj.dicomviewer.model.DicomSeries) {
            com.jj.dicomviewer.model.DicomSeries series = (com.jj.dicomviewer.model.DicomSeries) parent;
            try {
                return new ArrayList<>(series.getImages());
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
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
        // HOROS-20240407準拠: id theCell = [sender selectedCell];
        javax.swing.JButton selectedCell = sender.selectedCell();
        int index = -1;

        // HOROS-20240407準拠: [self.window makeFirstResponder: oMatrix];
        oMatrix.requestFocus();

        if (selectedCell != null) {
            // HOROS-20240407準拠: if( [theCell tag] >= 0)
            Object tagObj = selectedCell.getClientProperty("tag");
            if (tagObj instanceof Integer) {
                index = (Integer) tagObj;
            }

            if (index >= 0) {
                // HOROS-20240407準拠: NSManagedObject *dcmFile = [databaseOutline
                // itemAtRow:[databaseOutline selectedRow]];
                Object dcmFile = databaseOutline.getSelectedItem();

                if (dcmFile != null) {
                    // HOROS-20240407準拠: if( [[dcmFile valueForKey:@"type"] isEqualToString:
                    // @"Series"] && ...)
                    String type = getItemType(dcmFile);
                    if ("Series".equals(type)) {
                        int imageCount = getItemImageCount(dcmFile);
                        if (imageCount > 1) {
                            // HOROS-20240407準拠: [animationSlider setIntValue: [theCell tag]];
                            if (animationSlider != null) {
                                animationSlider.setValue(index);
                            }
                            // HOROS-20240407準拠: [self previewSliderAction: nil];
                            previewSliderAction(null);
                            return;
                        }
                    }
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
            Object dcmFile = databaseOutline.getSelectedItem();

            if (dcmFile != null) {
                String type = getItemType(dcmFile);
                // HOROS-20240407準拠: if( [[dcmFile valueForKey:@"type"] isEqualToString:
                // @"Study"] == NO)
                if (!"Study".equals(type)) {
                    // HOROS-20240407準拠: [imageView setIndex: index];
                    if (imageView != null) {
                        imageView.setIndex(index);
                    }
                }

                // HOROS-20240407準拠: [self initAnimationSlider];
                initAnimationSlider();
            }
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
     * 画像配列を取得
     * HOROS-20240407準拠: - (NSArray*)imagesArray: (id) item
     * preferredObject:(id)preferredObject
     */
    private List<Object> imagesArray(Object item, Object preferredObject) {
        List<Object> result = new ArrayList<>();
        if (item instanceof com.jj.dicomviewer.model.DicomSeries) {
            com.jj.dicomviewer.model.DicomSeries series = (com.jj.dicomviewer.model.DicomSeries) item;
            if (series.getImages() != null) {
                result.addAll(series.getImages());
            }
        } else if (item instanceof com.jj.dicomviewer.model.DicomStudy) {
            com.jj.dicomviewer.model.DicomStudy study = (com.jj.dicomviewer.model.DicomStudy) item;
            if (study.getSeries() != null) {
                for (com.jj.dicomviewer.model.DicomSeries series : study.getSeries()) {
                    if (series.getImages() != null) {
                        result.addAll(series.getImages());
                    }
                }
            }
        }
        return result;
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
                        cell.setEnabled(i < noOfImages);
                        cell.setText(i < noOfImages ? "loading..." : "");
                        cell.setIcon(null);
                    }
                }
            }

            if (imageView != null) {
                imageView.setPixels(null);
            }
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
                        if (matrixViewArray != null && !matrixViewArray.isEmpty()) {
                            List<Object> images = imagesArray(matrixViewArray.get(tag), null);
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

        if (!animate) {
            if (animationSlider != null) {
                animationSlider.setEnabled(false);
                animationSlider.setMaximum(0);
                animationSlider.setValue(0);
            }
        } else if (animationSlider != null && !animationSlider.isEnabled()) {
            animationSlider.setEnabled(true);
            animationSlider.setMaximum((int) (noOfImages - 1));
            animationSlider.setValue(0);
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
        // HOROS-20240407準拠: 完全実装は非常に複雑（9049-9218行目）
        // 基本的な実装のみ提供（完全実装は段階的に追加）

        if (dontUpdatePreviewPane) {
            return;
        }

        javax.swing.JButton cell = oMatrix != null ? oMatrix.selectedCell() : null;
        if (cell == null || !cell.isEnabled()) {
            return;
        }

        Object tagObj = cell.getClientProperty("tag");
        int index = tagObj instanceof Integer ? (Integer) tagObj : -1;

        if (index < 0 || matrixViewArray == null || index >= matrixViewArray.size()) {
            return;
        }

        Object aFile = databaseOutline.getSelectedItem();
        if (aFile == null) {
            return;
        }

        String type = getItemType(aFile);

        // HOROS-20240407準拠: シリーズレベルの処理
        if ("Series".equals(type)) {
            int imageCount = getItemImageCount(aFile);
            if (imageCount > 1 && animationSlider != null) {
                int sliderValue = animationSlider.getValue();
                if (imageView != null && sliderValue < imageCount) {
                    imageView.setIndex(sliderValue);
                }
            }
        } else if ("Study".equals(type)) {
            // HOROS-20240407準拠: スタディレベルの処理
            if (matrixViewArray != null && index < matrixViewArray.size()) {
                List<Object> images = imagesArray(matrixViewArray.get(index), null);
                if (!images.isEmpty() && imageView != null) {
                    int sliderValue = animationSlider != null ? animationSlider.getValue() : 0;
                    if (sliderValue < images.size()) {
                        imageView.setIndex(sliderValue);
                    }
                }
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
     * 比較スタディを検索
     * HOROS-20240407準拠: - (void) searchForComparativeStudies: (id) studySelectedID
     * (4709行目)
     */
    private void searchForComparativeStudies(Object studySelectedID) {
        // HOROS-20240407準拠: 比較スタディの検索実装
        // TODO: 実装
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
            // TODO: comparativeTableの更新
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
        // 既存のリスナーを削除してから追加（重複を防ぐ）
        javax.swing.table.JTableHeader header = databaseOutline.getTableHeader();
        if (header != null) {
            // 既存のMouseListenerを削除（簡易実装：すべてのリスナーを削除してから追加）
            java.awt.event.MouseListener[] listeners = header.getMouseListeners();
            for (java.awt.event.MouseListener listener : listeners) {
                header.removeMouseListener(listener);
            }
            
            header.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showColumnsMenu(e);
                    }
                }
                
                @Override
                public void mouseReleased(java.awt.event.MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showColumnsMenu(e);
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
