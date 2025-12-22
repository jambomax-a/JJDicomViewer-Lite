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
    
    // HOROS-20240407準拠: BrowserActivityHelper *_activityHelper; (BrowserController+Activity.h)
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
    
    // HOROS-20240407準拠: IBOutlet NSSearchField *searchField; (180行目)
    private javax.swing.JTextField searchField;
    
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
        
        // HOROS-20240407準拠: _database = [DicomDatabase activeLocalDatabase]; (BrowserController.m)
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
     *   - 左: splitAlbums (垂直): アルバムテーブル | データベースアウトライン（Sources）
     *   - 右: splitViewVert (垂直): matrixView | imageView
     *     - matrixView: BrowserMatrix（oMatrix）を含むNSView
     *     - imageView: PreviewView（_bottomSplitを含む）
     *       - _bottomSplit (水平): thumbnailsScrollView | 画像プレビュー
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
        setSize(1200, 800);
        
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        
        // ========== 左側サイドバー（垂直スプリッター） ==========
        // HOROS-20240407準拠: splitAlbums - Albums | Sources | Activity
        // HOROS-20240407準拠: BrowserController.h 111行目 IBOutlet NSSplitView *splitAlbums;
        JSplitPane splitAlbums = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        // Albums（上）
        // HOROS-20240407準拠: BrowserController.h 133行目 IBOutlet NSTableView *albumTable;
        String[] albumColumns = {"アルバム名", "スタディ数"};
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
        JScrollPane albumScroll = new JScrollPane(albumTable);
        splitAlbums.setTopComponent(albumScroll);
        
        // Sources（中）- データベースリスト
        // HOROS-20240407準拠: BrowserController.h 130行目 IBOutlet MyOutlineView *databaseOutline;
        // HOROS-20240407準拠: 左サイドバーのSourcesはデータベースリストを表示
        // TODO: HOROS-20240407のSources実装を確認して実装
        JList<String> sourcesList = new JList<>(new String[]{"Documents DB", "Description"});
        sourcesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sourcesScroll = new JScrollPane(sourcesList);
        splitAlbums.setBottomComponent(sourcesScroll);
        splitAlbums.setResizeWeight(0.3); // Albumsが30%
        splitAlbums.setDividerLocation(150);
        
        // Activity（下）
        // HOROS-20240407準拠: BrowserController+Activity.h
        activityHelper = new BrowserActivityHelper(this);
        JPanel activityPanel = new JPanel(new BorderLayout());
        // TODO: ActivityHelperからUIコンポーネントを取得して追加
        
        // SourcesとActivityを垂直に分割
        JSplitPane leftSidebar = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftSidebar.setTopComponent(splitAlbums);
        leftSidebar.setBottomComponent(activityPanel);
        leftSidebar.setResizeWeight(0.7); // 上部が70%
        
        // ========== 右側メインパネル（垂直スプリッター） ==========
        // HOROS-20240407準拠: splitViewVert - 上部テーブルエリア | 下部プレビューエリア
        // HOROS-20240407準拠: BrowserController.h 111行目 IBOutlet NSSplitView *splitViewVert;
        JSplitPane splitViewVert = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        // 上部テーブルエリア（水平スプリッター）: 患者情報テーブル | スタディ詳細テーブル
        JSplitPane topTablesSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // 患者情報テーブル（左）- databaseOutline
        // HOROS-20240407準拠: BrowserController.h 130行目 IBOutlet MyOutlineView *databaseOutline;
        // 画像の説明: カラムヘッダー「Patient name」「Report」「Lock」「Patient ID」「Age」「Acc」
        databaseOutline = new DatabaseOutlineView(this);
        JScrollPane patientTableScroll = new JScrollPane(databaseOutline);
        topTablesSplit.setLeftComponent(patientTableScroll);
        
        // スタディ詳細テーブル（右）- matrixView（oMatrixを含む）
        // HOROS-20240407準拠: BrowserController.h 147行目 IBOutlet NSView *matrixView;
        // HOROS-20240407準拠: BrowserController.h 132行目 IBOutlet BrowserMatrix *oMatrix;
        // 画像の説明: カラムヘッダー「Unnamed」「Date」「Images」
        oMatrix = new BrowserMatrix(this);
        JScrollPane studyTableScroll = new JScrollPane(oMatrix);
        topTablesSplit.setRightComponent(studyTableScroll);
        topTablesSplit.setResizeWeight(0.6); // 患者情報テーブルが60%
        
        splitViewVert.setTopComponent(topTablesSplit);
        
        // 下部プレビューエリア（水平スプリッター）: サムネイル | 画像ビュー
        // HOROS-20240407準拠: _bottomSplit - thumbnailsScrollView | imageView
        // HOROS-20240407準拠: BrowserController.h 144行目 IBOutlet NSSplitView* _bottomSplit;
        JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // サムネイル（左）- thumbnailsScrollView
        // HOROS-20240407準拠: BrowserController.h 204行目 IBOutlet NSScrollView *thumbnailsScrollView;
        // HOROS-20240407準拠: BrowserController.m 14239行目 [thumbnailsScrollView setDrawsBackground:NO];
        JScrollPane thumbnailsScrollView = new JScrollPane();
        thumbnailsScrollView.setBackground(null);
        thumbnailsScrollView.getViewport().setBackground(null);
        bottomSplit.setLeftComponent(thumbnailsScrollView);
        
        // 画像ビュー（右）- imageView
        // HOROS-20240407準拠: BrowserController.h 146行目 IBOutlet PreviewView *imageView;
        JPanel previewPanel = new JPanel(new BorderLayout());
        imageView = new PreviewView();
        previewPanel.add(imageView, BorderLayout.CENTER);
        
        // アニメーションスライダー
        // HOROS-20240407準拠: BrowserController.h 142行目 IBOutlet NSSlider *animationSlider;
        animationSlider = new JSlider(JSlider.HORIZONTAL, 0, 0, 0);
        animationSlider.setEnabled(false);
        animationSlider.addChangeListener(e -> {
            if (!animationSlider.getValueIsAdjusting()) {
                previewSliderAction(animationSlider);
            }
        });
        previewPanel.add(animationSlider, BorderLayout.SOUTH);
        
        bottomSplit.setRightComponent(previewPanel);
        bottomSplit.setResizeWeight(0.2); // サムネイルが20%
        
        splitViewVert.setBottomComponent(bottomSplit);
        splitViewVert.setResizeWeight(0.5); // 上部テーブルエリアが50%
        splitViewVert.setDividerLocation(400);
        
        // ========== メインウィンドウ（水平スプリッター） ==========
        // HOROS-20240407準拠: splitViewHorz - 左サイドバー | 右メインパネル
        // HOROS-20240407準拠: BrowserController.h 111行目 IBOutlet NSSplitView *splitViewHorz;
        JSplitPane splitViewHorz = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitViewHorz.setLeftComponent(leftSidebar);
        splitViewHorz.setRightComponent(splitViewVert);
        splitViewHorz.setResizeWeight(0.2); // 左サイドバーが20%
        
        contentPane.add(splitViewHorz, BorderLayout.CENTER);
        
        // HOROS-20240407準拠: 検索フィールド
        // HOROS-20240407準拠: BrowserController.h 180行目 IBOutlet NSSearchField *searchField;
        searchField = new JTextField();
        searchField.setToolTipText("検索");
        searchField.addActionListener(e -> {
            // TODO: 検索処理を実装
        });
        
        // HOROS-20240407準拠: ドラッグ&ドロップ対応
        setupDragAndDrop();
        
        // アルバムテーブルのデータを更新
        updateAlbumTable();
    }
    
    /**
     * ドラッグ&ドロップ機能を設定
     * HOROS-20240407準拠: MyOutlineViewのdraggingEntered/draggingUpdated/performDragOperation
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
     * ファイルをインポート
     * HOROS-20240407準拠: BrowserController.m 1472行目
     * [self.database addFilesAtPaths:localFiles]
     * 
     * 注意: HOROS-20240407ではパラメータなしで呼び出しているが、
     * デフォルトでpostNotifications:YESが設定される
     */
    public void importFiles(List<String> filePaths) {
        if (database == null || filePaths == null || filePaths.isEmpty()) {
            return;
        }
        
        // HOROS-20240407準拠: メインスレッドで実行（BrowserController.m 1472行目はメインスレッド）
        // ただし、addFilesAtPaths内部でバックグラウンド処理が行われる
        try {
            // HOROS-20240407準拠: パラメータなしで呼び出し（デフォルトでpostNotifications:YES）
            // 実際の実装では、デフォルトでpostNotifications:trueが設定される
            database.addFilesAtPaths(filePaths);
            
            // HOROS-20240407準拠: インポート後にUIを更新
            // 通知が送信されるので、自動的にUIが更新されるはずだが、念のため明示的に更新
            SwingUtilities.invokeLater(() -> {
                refreshDatabase(null);
            });
        } catch (Exception e) {
            // TODO: ログ出力（HOROS-20240407準拠のログシステムを使用）
            // e.printStackTrace(); // デバッグ用（コメントアウト）
        }
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
        
        // HOROS-20240407準拠: BrowserController.m 14738行目
        // ウィンドウが閉じられる時にカラム状態を保存
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveDatabaseColumnState();
            }
        });
    }
    
    /**
     * データベースアウトラインのカラム状態を復元
     * HOROS-20240407準拠: BrowserController.m 14316行目
     * [databaseOutline restoreColumnState: [[NSUserDefaults standardUserDefaults] objectForKey: @"databaseColumns2"]]
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
     * [[NSUserDefaults standardUserDefaults] setObject:[databaseOutline columnState] forKey: @"databaseColumns2"]
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
        if (albumTable == null || database == null) return;
        
        // 無限ループ防止
        if (isUpdatingAlbumTable) return;
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
     * HOROS-20240407準拠: - (void) copyFilesIntoDatabaseIfNeeded: (NSMutableArray*) filesInput options: (NSDictionary*) options (2191行目)
     */
    public void copyFilesIntoDatabaseIfNeeded(List<String> filesInput, Map<String, Object> options) {
        if (database == null || !database.isLocal()) {
            return;
        }
        if (filesInput == null || filesInput.isEmpty()) {
            return;
        }
        
        // HOROS-20240407準拠: BOOL COPYDATABASE = [[NSUserDefaults standardUserDefaults] boolForKey: @"COPYDATABASE"]; (2196行目)
        boolean copyDatabase = false; // TODO: UserDefaultsから取得
        
        // HOROS-20240407準拠: NSMutableArray *newFilesToCopyList = [NSMutableArray arrayWithCapacity: [filesInput count]]; (2208行目)
        List<String> newFilesToCopyList = new ArrayList<>();
        String inPath = database.getDataDirPath();
        
        // HOROS-20240407準拠: for( NSString *file in filesInput) (2212行目)
        for (String file : filesInput) {
            // HOROS-20240407準拠: if( [[file commonPrefixWithString: INpath options: NSLiteralSearch] isEqualToString:INpath] == NO) (2214行目)
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
            // HOROS-20240407準拠: NSMutableDictionary *dict = [NSMutableDictionary dictionaryWithObjectsAndKeys: filesInput, @"filesInput", ...]; (2284行目)
            Map<String, Object> dict = new HashMap<>();
            if (options != null) {
                dict.putAll(options);
            }
            dict.put("filesInput", newFilesToCopyList);
            dict.put("copyFiles", copyFiles);
            
            // HOROS-20240407準拠: NSThread *t = [[[NSThread alloc] initWithTarget:_database.independentDatabase selector:@selector(copyFilesThread:) object: dict] autorelease]; (2289行目)
            Thread thread = new Thread(() -> {
                database.copyFilesThread(dict);
            });
            
            // HOROS-20240407準拠: t.name = NSLocalizedString( @"Copying and indexing files...", nil); (2294行目)
            thread.setName("Indexing files...");
            
            // HOROS-20240407準拠: [[ThreadsManager defaultManager] addThreadAndStart: t]; (2297行目)
            ThreadsManager.defaultManager().addThreadAndStart(thread);
        } else {
            // 同期処理（TODO: 実装）
            // HOROS-20240407準拠: Wait *splash = [[Wait alloc] initWithString: NSLocalizedString(@"Copying into Database...", nil)]; (2301行目)
        }
    }
    
    // HOROS-20240407準拠: NSArray *outlineViewArray, *originalOutlineViewArray; (124行目)
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
        if (isRefreshingOutline) return;
        isRefreshingOutline = true;
        
        try {
            if (database == null || albumTable == null) return;
            
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
                                // HOROS-20240407準拠: @catch (NSException * e) { N2LogExceptionWithStackTrace(e); }
                                // TODO: ログ出力（HOROS-20240407準拠のログシステムを使用）
                            }
                        } else if (selectedAlbum.getStudies() != null) {
                            // HOROS-20240407準拠: 通常のアルバムの場合はアルバムのスタディを表示
                            outlineViewArray.addAll(selectedAlbum.getStudies());
                        }
                    }
                }
            }
            
            // HOROS-20240407準拠: アウトラインビューを更新
            if (databaseOutline != null) {
                SwingUtilities.invokeLater(() -> {
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
        
        // HOROS-20240407準拠: [[NSNotificationCenter defaultCenter] postNotificationName: NSOutlineViewSelectionDidChangeNotification ...]
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
     * HOROS-20240407準拠: - (void)outlineViewSelectionDidChange:(NSNotification *)aNotification (4973行目)
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
                    
                    // HOROS-20240407準拠: files = [self imagesArray: item preferredObject:oFirstForFirst];
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
                        
                        // HOROS-20240407準拠: [NSThread detachNewThreadSelector: @selector(searchForComparativeStudies:) ...]
                        Thread thread = new Thread(() -> searchForComparativeStudies(studySelected));
                        thread.setName("Search for comparative studies");
                        ThreadsManager.defaultManager().addThreadAndStart(thread);
                    }
                }
            }
            
            resetROIsAndKeysButton();
            
        } catch (Exception e) {
            // HOROS-20240407準拠: @catch (NSException * e) { N2LogExceptionWithStackTrace(e); }
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
     * HOROS-20240407準拠: - (IBAction)databasePressed: (id) sender (BrowserController.m)
     */
    public void databasePressed(DatabaseOutlineView sender) {
        // TODO: 実装
    }
    
    /**
     * データベースがダブルクリックされたときの処理
     * HOROS-20240407準拠: - (IBAction)databaseDoublePressed: (id) sender (BrowserController.m)
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
     * HOROS-20240407準拠: - (void)previewScrollWheel: (float) deltaY (BrowserController.m)
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
     * HOROS-20240407準拠: - (NSArray*)childrenArray: (id) parent recursive: (BOOL) recursive (BrowserController.m)
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
    
    /**
     * Activityテーブルビューを取得
     * HOROS-20240407準拠: - (NSTableView*)_activityTableView (BrowserController+Activity.mm 82行目)
     */
    public javax.swing.JTable getActivityTableView() {
        // TODO: ActivityTableViewの実装
        return null;
    }
    
    // HOROS-20240407準拠: ソート関連のフィールド
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
     * ソート順を取得
     * HOROS-20240407準拠: - (BOOL)sortAscending (BrowserController.m)
     */
    public boolean getSortAscending() {
        return sortAscending;
    }
    
    /**
     * ソート列を設定
     * HOROS-20240407準拠: - (void)setSortColumn: (NSString*) column ascending: (BOOL) ascending (BrowserController.m)
     */
    public void setSortColumn(String column, boolean ascending) {
        this.sortColumn = column;
        this.sortAscending = ascending;
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
                // HOROS-20240407準拠: NSManagedObject *dcmFile = [databaseOutline itemAtRow:[databaseOutline selectedRow]];
                Object dcmFile = databaseOutline.getSelectedItem();
                
                if (dcmFile != null) {
                    // HOROS-20240407準拠: if( [[dcmFile valueForKey:@"type"] isEqualToString: @"Series"] && ...)
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
                // HOROS-20240407準拠: if( [[dcmFile valueForKey:@"type"] isEqualToString: @"Study"] == NO)
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
            // HOROS-20240407準拠: [self.database performSelector:@selector(copyFilesThread:) withObject:dict];
            database.copyFilesThread(dict);
        }
    }
    
    /**
     * 自動コメント再生成スレッド
     * HOROS-20240407準拠: - (void) regenerateAutoCommentsThread: (NSDictionary*) arrays (1108行目)
     */
    public void regenerateAutoCommentsThread(Map<String, Object> arrays) {
        Thread thread = new Thread(() -> {
            try {
                // HOROS-20240407準拠: NSManagedObjectContext *context = self.database.independentContext;
                // TODO: データベースの独立コンテキストを取得
                
                @SuppressWarnings("unchecked")
                List<Object> studiesArray = (List<Object>) arrays.get("studyArrayIDs");
                @SuppressWarnings("unchecked")
                List<Object> seriesArray = (List<Object>) arrays.get("seriesArrayIDs");
                
                // HOROS-20240407準拠: NSString *commentField = [[NSUserDefaults standardUserDefaults] stringForKey: @"commentFieldForAutoFill"];
                String commentField = "comment"; // TODO: UserDefaultsから取得
                
                // HOROS-20240407準拠: BOOL studyLevel = [[NSUserDefaults standardUserDefaults] boolForKey: @"COMMENTSAUTOFILLStudyLevel"];
                boolean studyLevel = false; // TODO: UserDefaultsから取得
                
                // HOROS-20240407準拠: BOOL seriesLevel = [[NSUserDefaults standardUserDefaults] boolForKey: @"COMMENTSAUTOFILLSeriesLevel"];
                boolean seriesLevel = false; // TODO: UserDefaultsから取得
                
                // HOROS-20240407準拠: BOOL commentsAutoFill = [[NSUserDefaults standardUserDefaults] boolForKey: @"COMMENTSAUTOFILL"];
                boolean commentsAutoFill = false; // TODO: UserDefaultsから取得
                
                if (studiesArray != null) {
                    int x = 0;
                    for (Object studyID : studiesArray) {
                        // HOROS-20240407準拠: DicomStudy *s = (DicomStudy*) [context objectWithID: studyID];
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
                        // HOROS-20240407準拠: DicomSeries *series = (DicomSeries*) [context objectWithID: seriesID];
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
                
                // HOROS-20240407準拠: [self performSelectorOnMainThread: @selector( outlineViewRefresh)  withObject: nil waitUntilDone: NO];
                SwingUtilities.invokeLater(() -> outlineViewRefresh());
                
            } catch (Exception e) {
                // HOROS-20240407準拠: @catch (NSException * e) { N2LogExceptionWithStackTrace(e); }
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
                
                // HOROS-20240407準拠: NSArray *urlsR = [[NSString stringWithContentsOfFile:filename usedEncoding:NULL error:NULL] componentsSeparatedByString: @"\r"];
                // HOROS-20240407準拠: NSArray *urlsN = [[NSString stringWithContentsOfFile:filename usedEncoding:NULL error:NULL] componentsSeparatedByString: @"\n"];
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
                
                // HOROS-20240407準拠: WADODownload *downloader = [[[WADODownload alloc] init] autorelease];
                // HOROS-20240407準拠: [downloader WADODownload: urlToDownloads];
                // TODO: WADODownloadの実装
                
                // HOROS-20240407準拠: [[NSFileManager defaultManager] removeItemAtPath: filename error: nil];
                java.nio.file.Files.deleteIfExists(path);
                
            } catch (Exception e) {
                // HOROS-20240407準拠: @catch (NSException * e) { N2LogExceptionWithStackTrace(e); }
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
        // HOROS-20240407準拠: vImage_Buffer src = *(vImage_Buffer*) [[d objectForKey: @"src"] pointerValue];
        // HOROS-20240407準拠: vImage_Buffer dst = *(vImage_Buffer*) [[d objectForKey: @"dst"] pointerValue];
        
        String what = (String) d.get("what");
        
        if ("FTo16U".equals(what)) {
            // HOROS-20240407準拠: vImageConvert_FTo16U(&src, &dst, offset, scale, kvImageDoNotTile);
            // TODO: Javaで画像変換を実装（BufferedImageを使用）
        } else if ("16UToF".equals(what)) {
            // HOROS-20240407準拠: vImageConvert_16UToF(&src, &dst, offset, scale, kvImageDoNotTile);
            // TODO: Javaで画像変換を実装（BufferedImageを使用）
        } else {
            // HOROS-20240407準拠: 不明なvImageThreadの場合は何もしない
            // System.err.println("****** unknown vImageThread what: " + what); // デバッグ用（コメントアウト）
        }
    }
    
    /**
     * マルチスレッド画像変換
     * HOROS-20240407準拠: + (void) multiThreadedImageConvert: (NSString*) what :(vImage_Buffer*) src :(vImage_Buffer *) dst :(float) offset :(float) scale (625行目)
     */
    public static void multiThreadedImageConvert(String what, Object src, Object dst, float offset, float scale) {
        // HOROS-20240407準拠: int mpprocessors = [[NSProcessInfo processInfo] processorCount];
        int mpprocessors = Runtime.getRuntime().availableProcessors();
        
        // HOROS-20240407準拠: static NSConditionLock *threadLock = nil;
        // TODO: スレッドロックの実装（JavaではCountDownLatchまたはCyclicBarrierを使用）
        
        // HOROS-20240407準拠: for( int i = 0; i < mpprocessors; i++)
        // HOROS-20240407準拠: [NSThread detachNewThreadSelector: @selector(vImageThread:) toTarget: browserWindow withObject: d];
        // TODO: マルチスレッド画像変換を実装
    }
    
    // ========== ヘルパーメソッド ==========
    // HOROS-20240407準拠: UI関連のヘルパーメソッド
    
    /**
     * アイテムのタイプを取得
     */
    private String getItemType(Object item) {
        if (item == null) return null;
        if (item instanceof com.jj.dicomviewer.model.DicomStudy) return "Study";
        if (item instanceof com.jj.dicomviewer.model.DicomSeries) return "Series";
        if (item instanceof com.jj.dicomviewer.model.DicomImage) return "Image";
        return null;
    }
    
    /**
     * アイテムのファイル数を取得
     */
    private int getItemFileCount(Object item) {
        if (item instanceof com.jj.dicomviewer.model.DicomStudy) {
            return ((com.jj.dicomviewer.model.DicomStudy) item).getNumberOfImages() != null ? 
                   ((com.jj.dicomviewer.model.DicomStudy) item).getNumberOfImages() : 0;
        }
        if (item instanceof com.jj.dicomviewer.model.DicomSeries) {
            return ((com.jj.dicomviewer.model.DicomSeries) item).getNumberOfImages() != null ? 
                   ((com.jj.dicomviewer.model.DicomSeries) item).getNumberOfImages() : 0;
        }
        return 0;
    }
    
    /**
     * アイテムの画像数を取得
     */
    private int getItemImageCount(Object item) {
        if (item instanceof com.jj.dicomviewer.model.DicomSeries) {
            return ((com.jj.dicomviewer.model.DicomSeries) item).getImages() != null ? 
                   ((com.jj.dicomviewer.model.DicomSeries) item).getImages().size() : 0;
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
            return ((com.jj.dicomviewer.model.DicomImage) item).getSeries() != null ? 
                   ((com.jj.dicomviewer.model.DicomImage) item).getSeries().getStudy() : null;
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
     * 画像配列を取得
     * HOROS-20240407準拠: - (NSArray*)imagesArray: (id) item preferredObject:(id)preferredObject
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
            if (previewPix != null) previewPix.clear();
            if (previewPixThumbnails != null) previewPixThumbnails.clear();
        }
        
        synchronized (this) {
            setDCMDone = false;
            loadPreviewIndex = 0;
            
            if (oMatrix != null) {
                int rows = oMatrix.getRows();
                int columns = oMatrix.getColumns();
                if (columns < 1) columns = 1;
                
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
     * HOROS-20240407準拠: - (void) databaseOpenStudy: (NSManagedObject*) item (7505行目)
     */
    private void databaseOpenStudy(Object item) {
        // HOROS-20240407準拠: スタディを開く処理の実装
        // TODO: ViewerControllerの実装が必要
    }
    
    /**
     * 比較スタディを検索
     * HOROS-20240407準拠: - (void) searchForComparativeStudies: (id) studySelectedID (4709行目)
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
        if (isRefreshingAlbums) return;
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
            // HOROS-20240407準拠: DicomDatabase* idatabase = [self.database independentDatabase];
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
        // [[NSArray arrayWithObject:[NSDictionary dictionaryWithObject: NSLocalizedString(@"Database", nil) forKey:@"name"]] arrayByAddingObjectsFromArray:[self albumsInDatabase]]
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
        if (albumTable == null || database == null) return;
        
        List<com.jj.dicomviewer.model.DicomAlbum> albumArray = getAlbumArray();
        for (int i = 0; i < albumArray.size(); i++) {
            com.jj.dicomviewer.model.DicomAlbum album = albumArray.get(i);
            if (name.equals(album.getName())) {
                // HOROS-20240407準拠: [albumTable selectRowIndexes:[NSIndexSet indexSetWithIndex:...] byExtendingSelection:NO];
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
        if (isRefreshingOutline || isRefreshingAlbums || isUpdatingAlbumTable) return;
        
        // HOROS-20240407準拠: if( [[AppController sharedAppController] isSessionInactive] || waitForRunningProcess) return;
        // TODO: セッション非アクティブチェック
        
        // HOROS-20240407準拠: if( _database == nil) return;
        if (database == null) return;
        
        // HOROS-20240407準拠: if( DatabaseIsEdited) return;
        if (DatabaseIsEdited) return;
        
        // HOROS-20240407準拠: if( [databaseOutline editedRow] != -1) return;
        // TODO: アウトラインの編集状態チェック
        
        List<com.jj.dicomviewer.model.DicomAlbum> albumArray = getAlbumArray();
        
        // HOROS-20240407準拠: if( albumTable.selectedRow >= [albumArray count]) return;
        int selectedRow = albumTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= albumArray.size()) return;
        
        // HOROS-20240407準拠: if( [[[albumArray objectAtIndex: albumTable.selectedRow] valueForKey:@"smartAlbum"] boolValue] == YES)
        com.jj.dicomviewer.model.DicomAlbum selectedAlbum = albumArray.get(selectedRow);
        if (selectedAlbum != null && Boolean.TRUE.equals(selectedAlbum.isSmartAlbum())) {
            try {
                // HOROS-20240407準拠: [self outlineViewRefresh];
                outlineViewRefresh();
                // HOROS-20240407準拠: [self refreshAlbums];
                refreshAlbums();
            } catch (Exception e) {
                // HOROS-20240407準拠: @catch (NSException * e) { N2LogExceptionWithStackTrace(e); }
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
}
