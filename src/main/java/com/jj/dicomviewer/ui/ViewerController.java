package com.jj.dicomviewer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.jj.dicomviewer.model.DicomImage;
import com.jj.dicomviewer.model.DicomPix;
import com.jj.dicomviewer.model.DicomSeries;
import com.jj.dicomviewer.model.DicomStudy;

/**
 * ViewerController - ビューアコントローラー
 * 
 * HOROS-20240407のViewerControllerをJava Swingに移植
 * HOROS-20240407準拠: ViewerController.h, ViewerController.m
 */
public class ViewerController extends JFrame {
    
    // HOROS-20240407準拠: ViewerController.h 38行目
    // #define MAX4D 500
    private static final int MAX4D = 500;
    
    // HOROS-20240407準拠: ViewerController.h 279-283行目
    // NSMutableArray *fileList[ MAX4D];
    // NSMutableArray<DCMPix *> *pixList[MAX4D];
    // NSData *volumeData[ MAX4D];
    @SuppressWarnings("unchecked")
    private List<Object>[] fileList = new List[MAX4D];
    @SuppressWarnings("unchecked")
    private List<Object>[] pixList = new List[MAX4D];
    private byte[][] volumeData = new byte[MAX4D][];
    
    // HOROS-20240407準拠: ViewerController.h 284行目
    // short curMovieIndex, maxMovieIndex, orientationVector;
    private short curMovieIndex = 0;
    private short maxMovieIndex = 1;
    
    // HOROS-20240407準拠: ViewerController.h 306行目
    // BOOL windowWillClose;
    private boolean windowWillClose = false;
    
    // HOROS-20240407準拠: ViewerController.h 732行目
    // - (NSString*) studyInstanceUID;
    private String studyInstanceUID;
    
    // HOROS-20240407準拠: ViewerController.h 101行目
    // IBOutlet NSMatrix *previewMatrix;
    // IBOutlet NSScrollView *previewMatrixScrollView;
    private JPanel previewMatrix;
    private JScrollPane previewMatrixScrollView;
    
    // HOROS-20240407準拠: ViewerController.h 96行目
    // ToolbarPanelController *toolbarPanel;
    public ToolbarPanelController toolbarPanel;
    
    // HOROS-20240407準拠: ViewerController.m 3399-3420行目
    // ThumbnailsListPanelへの参照（ThumbnailsListPanelが管理）
    public ThumbnailsListPanel thumbnailsPanel;
    
    // HOROS-20240407準拠: ViewerController.h 119行目
    // DCMView *imageView;
    private DCMView imageView;
    
    // HOROS-20240407準拠: ViewerController.h 99行目
    // SeriesView *seriesView;
    private DCMView seriesView;
    
    // HOROS-20240407準拠: ViewerController.m 8760行目 - showWindowTransition
    // スライダーはビューアーの上部に配置
    private JSlider slider;
    
    /**
     * コンストラクタ
     * HOROS-20240407準拠: ViewerController.m 7620行目 - initWithPix:withFiles:withVolume:
     */
    public ViewerController() {
        super("DICOM Viewer");
        
        // HOROS-20240407準拠: ViewerController.m 7637行目
        // self = [super initWithWindowNibName:@"Viewer"];
        initializeUI();
    }
    
    /**
     * UIを初期化
     * HOROS-20240407準拠: ViewerController.m 7620-7669行目
     */
    private void initializeUI() {
        // HOROS-20240407準拠: ViewerController.m 3399-3420行目
        // サムネイルは常に別ウィンドウとして扱う
        // previewMatrixはThumbnailsListPanelに移動される
        
        // HOROS-20240407準拠: ViewerController.m 5057行目
        // [previewMatrix setCellClass: [ThumbnailCell class]];
        // Java Swingでは、JPanel + BoxLayoutでNSMatrixを再現
        previewMatrix = new JPanel();
        previewMatrix.setLayout(new BoxLayout(previewMatrix, BoxLayout.Y_AXIS));
        previewMatrix.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewMatrix.setBorder(new EmptyBorder(0, 0, 0, 0));
        
        // HOROS-20240407準拠: ViewerController.m 4944行目
        // [previewMatrixScrollView setPostsBoundsChangedNotifications:YES];
        previewMatrixScrollView = new JScrollPane(previewMatrix);
        previewMatrixScrollView.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        previewMatrixScrollView.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        previewMatrixScrollView.setBorder(null);
        
        // HOROS-20240407準拠: ViewerController.m 8760行目 - showWindowTransition
        // スライダーはビューアーの上部に配置
        slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        slider.setPreferredSize(new Dimension(200, 30));
        
        // HOROS-20240407準拠: ViewerController.m 7663行目
        // [seriesView setPixels:pixList[0] files:fileList[0] rois:roiList[0] firstImage:0 level:'i' reset:YES];
        seriesView = new DCMView();
        imageView = seriesView; // 簡易実装
        
        // HOROS-20240407準拠: ViewerController.m 3399-3420行目
        // サムネイルは常に別ウィンドウとして扱う
        // previewMatrixScrollViewはThumbnailsListPanelに移動される
        
        // メインコンテンツパネル
        JPanel contentPane = new JPanel(new BorderLayout());
        
        // HOROS-20240407準拠: ViewerController.m 8760行目 - showWindowTransition
        // スライダーはビューアーの上部に配置
        contentPane.add(slider, BorderLayout.NORTH);
        contentPane.add(seriesView, BorderLayout.CENTER);
        
        setContentPane(contentPane);
        
        // HOROS-20240407準拠: ViewerController.m 3399-3420行目
        // ウィンドウクローズ時の処理
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                processWindowEvent(e);
            }
        });
    }
    
    /**
     * 画像データで初期化
     * HOROS-20240407準拠: ViewerController.m 7620行目 - initWithPix:withFiles:withVolume:
     * 
     * @param f List<List<Object>> - pixList[0]を含むリスト（f.get(0)がList<Object>）
     * @param d List<List<Object>> - fileList[0]を含むリスト（d.get(0)がList<Object>）
     * @param v byte[] - volumeData[0]
     */
    public void initWithPix(List<List<Object>> f, List<List<Object>> d, byte[] v) {
        // HOROS-20240407準拠: ViewerController.m 7654行目
        // [self viewerControllerInit];
        viewerControllerInit();
        
        // HOROS-20240407準拠: ViewerController.m 7655行目
        // [self changeImageData:f :d :v :YES];
        changeImageData(f, d, v, true);
        
        // HOROS-20240407準拠: ViewerController.m 7715行目
        // toolbarPanel = [[ToolbarPanelController alloc] initForViewer: self withToolbar: toolbar];
        Preferences prefs = Preferences.userNodeForPackage(ViewerController.class);
        boolean useToolbarPanel = prefs.getBoolean("USETOOLBARPANEL", true);
        if (useToolbarPanel) {
            // JToolBarを作成（簡易実装）
            JToolBar jToolBar = new JToolBar();
            jToolBar.setFloatable(false);
            toolbarPanel = new ToolbarPanelController(this, jToolBar);
        }
        
        // HOROS-20240407準拠: ViewerController.m 4876-4884行目 - checkBuiltMatrixPreview
        // サムネイルマトリックスを構築（ウィンドウが表示された後に呼ばれる）
        // ここでは呼ばない（windowDidBecomeMainで呼ぶ）
    }
    
    /**
     * ビューアーコントローラーを初期化
     * HOROS-20240407準拠: ViewerController.m 20764行目 - viewerControllerInit
     */
    private void viewerControllerInit() {
        // HOROS-20240407準拠: ViewerController.m 20766行目
        // BOOL matrixVisible = [[NSUserDefaults standardUserDefaults] boolForKey: @"SeriesListVisible"];
        Preferences prefs = Preferences.userNodeForPackage(ViewerController.class);
        boolean matrixVisible = prefs.getBoolean("SeriesListVisible", true);
        
        // HOROS-20240407準拠: ViewerController.m 20770行目
        // numberOf2DViewer++;
        // TODO: グローバルカウンターの実装
        
        // HOROS-20240407準拠: ViewerController.m 20797行目
        // roiLock = [[NSRecursiveLock alloc] init];
        // TODO: ROIロックの実装
        
        // HOROS-20240407準拠: ViewerController.m 20802行目
        // maxMovieIndex = 1;
        maxMovieIndex = 1;
        
        // HOROS-20240407準拠: ViewerController.m 20812行目
        // direction = 1;
        // TODO: directionの実装
    }
    
    /**
     * 画像データを変更
     * HOROS-20240407準拠: ViewerController.m changeImageData:withFiles:withVolume:withReset:
     * 
     * @param f List<List<Object>> - pixList[0]を含むリスト（f.get(0)がList<Object>）
     * @param d List<List<Object>> - fileList[0]を含むリスト（d.get(0)がList<Object>）
     * @param v byte[] - volumeData[0]
     * @param reset boolean - リセットフラグ
     */
    private void changeImageData(List<List<Object>> f, List<List<Object>> d, byte[] v, boolean reset) {
        // HOROS-20240407準拠: ViewerController.m
        // fileList[0] = d;
        // pixList[0] = f;
        // volumeData[0] = v;
        if (d != null && d.size() > 0 && f != null && f.size() > 0) {
            // HOROS-20240407準拠: fileList[0]はList<Object>（correspondingObjects）そのもの
            // dはList<List<Object>>なので、d.get(0)がList<Object>（correspondingObjects）
            @SuppressWarnings("unchecked")
            List<Object> correspondingObjects = (List<Object>) d.get(0);
            fileList[0] = correspondingObjects;
            
            // HOROS-20240407準拠: pixList[0]はList<Object>（viewerPix）そのもの
            // fはList<List<Object>>なので、f.get(0)がList<Object>（viewerPix）
            @SuppressWarnings("unchecked")
            List<Object> viewerPix = (List<Object>) f.get(0);
            pixList[0] = viewerPix;
            
            volumeData[0] = v;
            
            // HOROS-20240407準拠: ViewerController.m 7663行目
            // [seriesView setPixels:pixList[0] files:fileList[0] rois:roiList[0] firstImage:0 level:'i' reset:YES];
            if (imageView != null && pixList[0] != null && fileList[0] != null) {
                // HOROS-20240407準拠: pixList[0]とfileList[0]をDCMViewに設定
                List<DicomPix> pixListForView = new ArrayList<>();
                for (Object pix : pixList[0]) {
                    if (pix instanceof DicomPix) {
                        pixListForView.add((DicomPix) pix);
                    }
                }
                
                List<Object> fileListForView = new ArrayList<>(fileList[0]);
                
                // HOROS-20240407準拠: DCMView.m 2176行目 - setPixels:files:rois:firstImage:level:reset:
                imageView.setPixels(pixListForView, fileListForView, null, (short) 0, (char) 'i', reset);
                
                // HOROS-20240407準拠: 最初の画像を表示
                if (!pixListForView.isEmpty()) {
                    imageView.setIndex(0);
                    // HOROS-20240407準拠: 画像が読み込まれたことを確認
                    System.out.println("ViewerController.changeImageData: setIndex(0) called, curDCM: " + (imageView.curDCM != null ? "not null" : "null"));
                } else {
                    System.out.println("ViewerController.changeImageData: pixListForView is empty, cannot set index");
                }
                
                System.out.println("ViewerController.changeImageData: setPixels called, pixList size: " + pixListForView.size() + ", fileList size: " + fileListForView.size());
                
                // HOROS-20240407準拠: 再描画を強制
                if (imageView != null) {
                    imageView.repaint();
                }
            }
            
            // HOROS-20240407準拠: ViewerController.m 4946行目
            // DicomStudy *study = [curImage valueForKeyPath:@"series.study"];
            if (correspondingObjects != null && !correspondingObjects.isEmpty()) {
                Object curImage = correspondingObjects.get(0);
                if (curImage instanceof DicomImage) {
                    DicomImage image = (DicomImage) curImage;
                    DicomSeries series = image.getSeries();
                    if (series != null) {
                        DicomStudy study = series.getStudy();
                        if (study != null) {
                            studyInstanceUID = study.getStudyInstanceUID();
                        }
                    }
                }
            }
        }
    }
    
    /**
     * ウィンドウのサイズと位置を設定
     * HOROS-20240407準拠: ViewerController.m 8760行目 - showWindowTransition
     */
    public void showWindowTransition() {
        // HOROS-20240407準拠: ViewerController.m 8783行目
        // NSRect screenRect = [AppController usefullRectForScreen: screen];
        Rectangle screenRect = getUsefulRectForScreen();
        
        // HOROS-20240407準拠: ViewerController.m 8785行目
        // [[self window] setFrame:screenRect display:YES];
        setBounds(screenRect);
        
        // HOROS-20240407準拠: ViewerController.m 8787-8793行目
        // switch( [[NSUserDefaults standardUserDefaults] integerForKey: @"WINDOWSIZEVIEWER"])
        Preferences prefs = Preferences.userNodeForPackage(ViewerController.class);
        int windowSizeViewer = prefs.getInt("WINDOWSIZEVIEWER", 0);
        switch (windowSizeViewer) {
            case 0:
                // [self setWindowFrame:screenRect showWindow: NO];
                setBounds(screenRect);
                break;
            case 1:
                // [imageView resizeWindowToScale: 1.0];
                // TODO: 実装
                break;
            case 2:
                // [imageView resizeWindowToScale: 1.5];
                // TODO: 実装
                break;
            case 3:
                // [imageView resizeWindowToScale: 2.0];
                // TODO: 実装
                break;
        }
        
        // HOROS-20240407準拠: ViewerController.m 8785行目
        // ウィンドウの最大サイズを設定（サムネイルウィンドウと重ならないように）
        setMaximumSize(new Dimension(screenRect.width, screenRect.height));
    }
    
    /**
     * 画面の有効領域を取得
     * HOROS-20240407準拠: AppController.m usefullRectForScreen:
     */
    private Rectangle getUsefulRectForScreen() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        Rectangle screenBounds = gd.getDefaultConfiguration().getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gd.getDefaultConfiguration());
        
        // HOROS-20240407準拠: ViewerController.m 3399-3420行目
        // ツールバーとサムネイルウィンドウのサイズを考慮
        int toolbarHeight = ToolbarPanelController.FIXED_HEIGHT;
        int thumbnailWidth = ThumbnailsListPanel.getFixedWidth();
        
        // HOROS-20240407準拠: ViewerController.m 8783行目
        // NSRect screenRect = [AppController usefullRectForScreen: screen];
        int x = screenBounds.x + screenInsets.left + thumbnailWidth;
        int y = screenBounds.y + screenInsets.top + toolbarHeight;
        int width = screenBounds.width - screenInsets.left - screenInsets.right - thumbnailWidth;
        int height = screenBounds.height - screenInsets.top - screenInsets.bottom - toolbarHeight;
        
        return new Rectangle(x, y, width, height);
    }
    
    /**
     * ウィンドウがメインになったときの処理
     * HOROS-20240407準拠: ViewerController.m 3460行目 - windowDidBecomeMain:
     */
    public void windowDidBecomeMain() {
        System.out.println("ViewerController.windowDidBecomeMain: called");
        // HOROS-20240407準拠: ViewerController.m 3399-3420行目
        // サムネイルウィンドウを表示
        Preferences prefs = Preferences.userNodeForPackage(ViewerController.class);
        boolean useFloatingThumbnailsList = prefs.getBoolean("UseFloatingThumbnailsList", true);
        boolean seriesListVisible = prefs.getBoolean("SeriesListVisible", true);
        
        System.out.println("ViewerController.windowDidBecomeMain: useFloatingThumbnailsList = " + useFloatingThumbnailsList + ", seriesListVisible = " + seriesListVisible);
        
        if (useFloatingThumbnailsList && seriesListVisible) {
            // HOROS-20240407準拠: ViewerController.m 3412行目
            // [thumbnailsListPanel[ i] setThumbnailsView: previewMatrixScrollView viewer: self];
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();
            
            if (screens.length > 0) {
                GraphicsDevice defaultScreen = screens[0];
                int screenIndex = 0;
                for (int i = 0; i < screens.length; i++) {
                    if (screens[i] == defaultScreen) {
                        screenIndex = i;
                        break;
                    }
                }
                
                System.out.println("ViewerController.windowDidBecomeMain: screenIndex = " + screenIndex);
                thumbnailsPanel = ThumbnailsListPanel.getInstanceForScreenIndex(screenIndex);
                System.out.println("ViewerController.windowDidBecomeMain: thumbnailsPanel = " + thumbnailsPanel);
                if (thumbnailsPanel != null) {
                    System.out.println("ViewerController.windowDidBecomeMain: setting thumbnailsView, previewMatrixScrollView = " + previewMatrixScrollView);
                    thumbnailsPanel.setThumbnailsView(previewMatrixScrollView, this);
                    // HOROS-20240407準拠: applicationDidChangeScreenParametersは内部で呼ばれる
                    thumbnailsPanel.setVisible(true);
                    thumbnailsPanel.toFront();
                } else {
                    System.out.println("ViewerController.windowDidBecomeMain: thumbnailsPanel is null");
                }
            }
        }
        
        // HOROS-20240407準拠: ViewerController.m 3468行目
        // [self refreshToolbar];
        // TODO: refreshToolbarの実装
        
        // HOROS-20240407準拠: ViewerController.m 3469行目
        // [self updateNavigator];
        // TODO: updateNavigatorの実装
        
        // HOROS-20240407準拠: ViewerController.m 3470行目
        // [imageView setNeedsDisplay: YES];
        if (imageView != null) {
            imageView.repaint();
        }
        
        // HOROS-20240407準拠: ViewerController.m 4876-4884行目 - checkBuiltMatrixPreview
        // サムネイルマトリックスを構築
        System.out.println("ViewerController.windowDidBecomeMain: calling updatePreviewMatrix");
        if (isVisible()) {
            updatePreviewMatrix(false);
        } else {
            System.out.println("ViewerController.windowDidBecomeMain: window is not visible, skipping updatePreviewMatrix");
        }
    }
    
    /**
     * 画像読み込みスレッドを開始
     * HOROS-20240407準拠: ViewerController.m 8807行目 - startLoadImageThread
     */
    public void startLoadImageThread() {
        // HOROS-20240407準拠: ViewerController.m 8809行目
        // if( windowWillClose) return;
        if (windowWillClose) {
            return;
        }
        
        // HOROS-20240407準拠: ViewerController.m 8811行目
        // originalOrientation = -1;
        // TODO: originalOrientationの実装
        
        // HOROS-20240407準拠: ViewerController.m 8820-8825行目
        // 画像読み込み処理
        // TODO: 実装
    }
    
    /**
     * ウィンドウイベントを処理
     * HOROS-20240407準拠: ViewerController.m windowWillClose:
     */
    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            // HOROS-20240407準拠: ViewerController.m windowWillClose:
            windowWillClose = true;
            
            // HOROS-20240407準拠: ViewerController.m 3399-3420行目
            // 関連ウィンドウを閉じる
            if (toolbarPanel != null) {
                toolbarPanel.viewerWillClose(this);
            }
            if (thumbnailsPanel != null) {
                thumbnailsPanel.viewerWillClose(this);
            }
            
            // HOROS-20240407準拠: ViewerController.m windowWillClose:
            // ウィンドウを閉じる
            dispose();
        } else {
            super.processWindowEvent(e);
        }
    }
    
    /**
     * プレビューマトリックスを更新
     * HOROS-20240407準拠: ViewerController.m 4903行目 - buildMatrixPreview:
     */
    public void updatePreviewMatrix(boolean showSelected) {
        // HOROS-20240407準拠: ViewerController.m 4905行目
        // if( [[self window] isVisible] == NO) return;
        if (!isVisible()) {
            System.out.println("ViewerController.updatePreviewMatrix: window is not visible");
            return;
        }
        
        // HOROS-20240407準拠: ViewerController.m 4906行目
        // if( windowWillClose) return;
        if (windowWillClose) {
            System.out.println("ViewerController.updatePreviewMatrix: windowWillClose is true");
            return;
        }
        
        // HOROS-20240407準拠: ViewerController.m 4924行目
        // if( [self matrixIsVisible] == NO) return;
        Preferences prefs = Preferences.userNodeForPackage(ViewerController.class);
        boolean seriesListVisible = prefs.getBoolean("SeriesListVisible", true);
        if (!seriesListVisible) {
            System.out.println("ViewerController.updatePreviewMatrix: SeriesListVisible is false");
            return;
        }
        
        System.out.println("ViewerController.updatePreviewMatrix: starting update");
        try {
            // HOROS-20240407準拠: ViewerController.m 4936行目
            // NSManagedObject *curImage = [fileList[0] objectAtIndex:0];
            if (fileList[0] == null || fileList[0].isEmpty()) {
                System.out.println("ViewerController.updatePreviewMatrix: fileList[0] is null or empty");
                previewMatrix.removeAll();
                previewMatrix.revalidate();
                previewMatrix.repaint();
                return;
            }
            
            System.out.println("ViewerController.updatePreviewMatrix: fileList[0] size = " + fileList[0].size());
            Object curImage = fileList[0].get(0);
            if (!(curImage instanceof DicomImage)) {
                System.out.println("ViewerController.updatePreviewMatrix: curImage is not DicomImage: " + (curImage != null ? curImage.getClass().getName() : "null"));
                previewMatrix.removeAll();
                previewMatrix.revalidate();
                previewMatrix.repaint();
                return;
            }
            
            DicomImage image = (DicomImage) curImage;
            DicomSeries series = image.getSeries();
            if (series == null) {
                System.out.println("ViewerController.updatePreviewMatrix: series is null");
                previewMatrix.removeAll();
                previewMatrix.revalidate();
                previewMatrix.repaint();
                return;
            }
            
            DicomStudy study = series.getStudy();
            if (study == null) {
                System.out.println("ViewerController.updatePreviewMatrix: study is null");
                previewMatrix.removeAll();
                previewMatrix.revalidate();
                previewMatrix.repaint();
                return;
            }
            
            System.out.println("ViewerController.updatePreviewMatrix: study = " + study.getStudyInstanceUID());
            System.out.println("ViewerController.updatePreviewMatrix: series = " + series.getName());
            
            // HOROS-20240407準拠: ViewerController.m 4955-4958行目
            // NSMutableArray *viewerSeries = [NSMutableArray array];
            // for( int i = 0 ; i < maxMovieIndex; i++)
            //     [viewerSeries addObject: [[fileList[ i] objectAtIndex:0] valueForKey:@"series"]];
            List<DicomSeries> viewerSeries = new ArrayList<>();
            for (int i = 0; i < maxMovieIndex; i++) {
                if (fileList[i] != null && !fileList[i].isEmpty()) {
                    Object img = fileList[i].get(0);
                    if (img instanceof DicomImage) {
                        DicomSeries s = ((DicomImage) img).getSeries();
                        if (s != null) {
                            viewerSeries.add(s);
                        }
                    }
                }
            }
            
            // HOROS-20240407準拠: ViewerController.m 4960-4982行目
            // FIND ALL STUDIES of this patient
            String searchString = study.getPatientUID();
            List<DicomStudy> studiesArray;
            
            // HOROS-20240407準拠: ViewerController.m 4973-4982行目
            // Use the 'history' array of the browser controller, if available (with the distant studies)
            // TODO: comparativeStudiesの実装
            // 現在はデータベースから直接検索
            com.jj.dicomviewer.model.DicomDatabase db = com.jj.dicomviewer.model.DicomDatabase.defaultDatabase();
            if (db == null) {
                studiesArray = new ArrayList<>();
                studiesArray.add(study);
            } else {
                if (searchString == null || searchString.isEmpty() || searchString.equals("0")) {
                    // HOROS-20240407準拠: ViewerController.m 4965-4966行目
                    // searchString = [study valueForKey:@"name"];
                    // predicate = [NSPredicate predicateWithFormat: @"(name == %@)", searchString];
                    searchString = study.getName();
                    // TODO: nameで検索する実装（現在は簡易実装）
                    studiesArray = new ArrayList<>();
                    studiesArray.add(study);
                } else {
                    // HOROS-20240407準拠: ViewerController.m 4968行目
                    // predicate = [NSPredicate predicateWithFormat: @"(patientUID BEGINSWITH[cd] %@)", searchString];
                    // HOROS-20240407準拠: patientUID BEGINSWITH[cd]（大文字小文字を区別しない前方一致）
                    studiesArray = db.getStudiesByPatientUID(searchString);
                }
            }
            
            // HOROS-20240407準拠: ViewerController.m 4981行目
            // studiesArray = [studiesArray sortedArrayUsingDescriptors: [NSArray arrayWithObject: [NSSortDescriptor sortDescriptorWithKey: @"date" ascending: NO]]];
            studiesArray.sort((a, b) -> {
                if (a.getDate() == null && b.getDate() == null) return 0;
                if (a.getDate() == null) return 1;
                if (b.getDate() == null) return -1;
                return b.getDate().compareTo(a.getDate());
            });
            
            if (studiesArray.isEmpty()) {
                previewMatrix.removeAll();
                previewMatrix.revalidate();
                previewMatrix.repaint();
                return;
            }
            
            // HOROS-20240407準拠: ViewerController.m 4996-5027行目
            // NSArray *displayedSeries = [ViewerController getDisplayedSeries];
            // NSMutableArray *seriesArray = [NSMutableArray array];
            List<DicomSeries> displayedSeries = getDisplayedSeries();
            List<List<DicomSeries>> seriesArray = new ArrayList<>();
            
            int totalSeriesCount = 0;
            for (DicomStudy s : studiesArray) {
                // HOROS-20240407準拠: ViewerController.m 5014行目
                // [seriesArray addObject: [[BrowserController currentBrowser] childrenArray: s]];
                System.out.println("ViewerController.updatePreviewMatrix: getting children for study " + s.getStudyInstanceUID());
                
                // HOROS-20240407準拠: Seriesを遅延読み込み
                com.jj.dicomviewer.model.DicomDatabase studyDb = com.jj.dicomviewer.model.DicomDatabase.defaultDatabase();
                if (studyDb != null) {
                    System.out.println("ViewerController.updatePreviewMatrix: loading series for study");
                    studyDb.loadSeriesForStudyIfNeeded(s);
                }
                
                BrowserController browser = BrowserController.currentBrowser();
                if (browser == null) {
                    System.out.println("ViewerController.updatePreviewMatrix: BrowserController.currentBrowser() is null");
                    // BrowserControllerがnullの場合、直接allSeries()を使用
                    List<DicomSeries> seriesList = s.allSeries();
                    System.out.println("ViewerController.updatePreviewMatrix: seriesList.size() (direct) = " + seriesList.size());
                    seriesArray.add(seriesList);
                    continue;
                }
                List<Object> children = browser.childrenArray(s, false);
                System.out.println("ViewerController.updatePreviewMatrix: children.size() = " + children.size());
                List<DicomSeries> seriesList = new ArrayList<>();
                for (Object child : children) {
                    if (child instanceof DicomSeries) {
                        seriesList.add((DicomSeries) child);
                    }
                }
                System.out.println("ViewerController.updatePreviewMatrix: seriesList.size() = " + seriesList.size());
                seriesArray.add(seriesList);
                
                // HOROS-20240407準拠: ViewerController.m 5016行目
                // if( [s isHidden] == NO)
                //     i += [[seriesArray lastObject] count];
                if (!s.isHidden()) {
                    totalSeriesCount += seriesList.size();
                }
            }
            
            // HOROS-20240407準拠: ViewerController.m 5059行目
            // if( [previewMatrix numberOfRows] != i+[studiesArray count])
            //     [previewMatrix renewRows: i+[studiesArray count] columns: 1];
            // Java Swingでは、コンポーネントを直接追加/削除
            previewMatrix.removeAll();
            
            // HOROS-20240407準拠: ViewerController.m 3992行目
            // NSArray *allStudiesArray = studiesArray;
            // HOROS-20240407準拠: allStudiesArrayは全てのStudyを含む配列（現在はstudiesArrayと同じ）
            List<DicomStudy> allStudiesArray = studiesArray;
            
            // HOROS-20240407準拠: ViewerController.m 5091-5442行目
            // for( id curStudy in studiesArray)
            int index = 0;
            for (int studyIdx = 0; studyIdx < studiesArray.size(); studyIdx++) {
                DicomStudy curStudy = studiesArray.get(studyIdx);
                List<DicomSeries> curStudySeries = seriesArray.get(studyIdx);
                
                // HOROS-20240407準拠: データベースから取得したStudyオブジェクトのisHidden状態を確認
                // データベースから最新のisHidden状態を読み込む
                if (db != null) {
                    try {
                        // データベースからisHidden状態を読み込む
                        db.loadStudyHiddenState(curStudy);
                        System.out.println("ViewerController.updatePreviewMatrix: loaded isHidden from database: " + curStudy.isHidden() + " for " + curStudy.getStudyInstanceUID());
                    } catch (Exception e) {
                        // hiddenカラムが存在しない場合は、デフォルト値（false）を使用
                        System.err.println("ViewerController.updatePreviewMatrix: failed to load isHidden from database: " + e.getMessage());
                        curStudy.setHidden(false);
                    }
                } else {
                    // データベースが利用できない場合は、デフォルト値（false）を使用
                    curStudy.setHidden(false);
                }
                
                // HOROS-20240407準拠: ViewerController.m 5095行目
                // NSUInteger curStudyIndexAll = [allStudiesArray indexOfObject: curStudy];
                int curStudyIndexAll = allStudiesArray.indexOf(curStudy);
                
                // HOROS-20240407準拠: ViewerController.m 5093行目
                // NSButtonCell* cell = [previewMatrix cellAtRow: index column:0];
                // Java Swingでは、ThumbnailButtonを直接作成
                // HOROS-20240407準拠: curStudyIndexAllを使用して番号を表示
                ThumbnailButton studyButton = createStudyCell(curStudy, study, curStudyIndexAll, allStudiesArray.size(), curStudySeries.size());
                
                // HOROS-20240407準拠: ViewerController.m 5099行目
                // [cell setAction: @selector(matrixPreviewSwitchHidden:)];
                studyButton.addActionListener(e -> matrixPreviewSwitchHidden(curStudy));
                
                previewMatrix.add(studyButton);
                index++;
                
                // HOROS-20240407準拠: ViewerController.m 5307行目
                // if(![curStudy respondsToSelector:@selector(isHidden)] || [curStudy isHidden] == NO)
                System.out.println("ViewerController.updatePreviewMatrix: curStudy.isHidden() = " + curStudy.isHidden());
                System.out.println("ViewerController.updatePreviewMatrix: curStudySeries.size() = " + curStudySeries.size());
                if (!curStudy.isHidden()) {
                    // HOROS-20240407準拠: ViewerController.m 5309-5441行目
                    // for( i = 0; i < [series count]; i++)
                    for (int i = 0; i < curStudySeries.size(); i++) {
                        DicomSeries curSeries = curStudySeries.get(i);
                        System.out.println("ViewerController.updatePreviewMatrix: creating series cell for " + curSeries.getName());
                        
                        // HOROS-20240407準拠: ViewerController.m 5313行目
                        // NSButtonCell *cell = [previewMatrix cellAtRow: index column:0];
                        ThumbnailButton seriesButton = createSeriesCell(curSeries, curStudy, study, viewerSeries, displayedSeries);
                        
                        // HOROS-20240407準拠: ViewerController.m 5322行目
                        // [cell setAction: @selector(matrixPreviewPressed:)];
                        seriesButton.addActionListener(e -> matrixPreviewPressed(curSeries, false));
                        
                        previewMatrix.add(seriesButton);
                        System.out.println("ViewerController.updatePreviewMatrix: added series cell to previewMatrix");
                        index++;
                    }
                } else {
                    System.out.println("ViewerController.updatePreviewMatrix: curStudy is hidden, skipping series");
                }
            }
            
            previewMatrix.revalidate();
            previewMatrix.repaint();
            
            System.out.println("ViewerController.updatePreviewMatrix: added " + previewMatrix.getComponentCount() + " components");
            System.out.println("ViewerController.updatePreviewMatrix: studiesArray.size() = " + studiesArray.size());
            System.out.println("ViewerController.updatePreviewMatrix: previewMatrixScrollView = " + previewMatrixScrollView);
            System.out.println("ViewerController.updatePreviewMatrix: thumbnailsPanel = " + thumbnailsPanel);
            
            // HOROS-20240407準拠: ViewerController.m 5477-5488行目
            // if( showSelected)
            if (showSelected) {
                // HOROS-20240407準拠: ViewerController.m 5479行目
                // NSInteger index = [[[previewMatrix cells] valueForKeyPath:@"representedObject.object"] indexOfObject: [[fileList[ curMovieIndex] objectAtIndex:0] valueForKey:@"series"]];
                // TODO: 選択されたSeriesまでスクロール
            } else {
                // HOROS-20240407準拠: ViewerController.m 5486行目
                // [[previewMatrixScrollView contentView] scrollToPoint: origin];
                // TODO: スクロール位置をリセット
            }
            
        } catch (Exception ex) {
            System.err.println("ViewerController.updatePreviewMatrix: error occurred");
            ex.printStackTrace();
        }
    }
    
    /**
     * Studyセルを作成
     * HOROS-20240407準拠: ViewerController.m 5091-5242行目
     */
    private ThumbnailButton createStudyCell(DicomStudy study, DicomStudy currentStudy, int studyIndex, int totalStudies, int seriesCount) {
        ThumbnailButton button = new ThumbnailButton();
        
        // HOROS-20240407準拠: ViewerController.m 5123行目
        // NSString *name = [[curStudy valueForKey:@"studyName"] stringByTruncatingToLength: 34];
        String name = study.getStudyName();
        if (name == null || name.isEmpty()) {
            name = "Unnamed";
        }
        if (name.length() > 34) {
            name = name.substring(0, 34);
        }
        
        // HOROS-20240407準拠: ViewerController.m 5125-5128行目
        // NSString *stateText;
        String stateText = "";
        if (study.getStateText() != null) {
            // TODO: BrowserController.statesArrayから取得
        }
        
        // HOROS-20240407準拠: ViewerController.m 5129-5133行目
        // NSString *comment = [curStudy valueForKey:@"comment"];
        String comment = study.getComment();
        if (comment == null) {
            comment = "";
        }
        if (comment.length() > 32) {
            comment = comment.substring(0, 32);
        }
        
        // HOROS-20240407準拠: ViewerController.m 5135-5137行目
        // NSString *modality = [curStudy valueForKey:@"modality"];
        String modality = study.getModality();
        if (modality == null || modality.isEmpty()) {
            modality = "OT:";
        }
        
        // HOROS-20240407準拠: ViewerController.m 5164-5167行目
        // if( [curStudy isHidden])
        //     action = NSLocalizedString(@"Show Series", nil);
        // else
        //     action = NSLocalizedString(@"Hide Series", nil);
        String action = study.isHidden() ? "Show Series" : "Hide Series";
        
        // HOROS-20240407準拠: ViewerController.m 5183-5192行目
        // NSMutableArray* components = [NSMutableArray array];
        // [components addObject: [NSString stringWithFormat: @" %d ", (int) curStudyIndexAll+1]];
        List<String> components = new ArrayList<>();
        components.add(String.format(" %d ", studyIndex + 1));
        components.add("");
        if (name.length() > 0) {
            components.add(name);
        }
        if (study.getDate() != null) {
            // TODO: 日付フォーマット
            components.add(study.getDate().toString());
        }
        components.add(String.format("%s: %d %s", modality, seriesCount, seriesCount == 1 ? "series" : "series"));
        if (stateText.length() > 0) {
            components.add(stateText);
        }
        if (comment.length() > 0) {
            components.add(comment);
        }
        if (action.length() > 0) {
            components.add("\r" + action);
        }
        
        // HOROS-20240407準拠: ViewerController.m 5195行目
        // NSMutableAttributedString *finalString = [[[NSMutableAttributedString alloc] initWithString: [components componentsJoinedByString:@"\r"]] autorelease];
        String text = String.join("\r", components);
        
        // HOROS-20240407準拠: ViewerController.m 5198-5215行目
        // 番号部分を太字・背景色付きで表示
        // HOROS-20240407準拠: ViewerController.m 5198行目 - viewerNumberFont
        // HOROS-20240407準拠: ViewerController.m 5213行目 - dbSmallMatrixFont
        Dimension cellSize = button.getCellSize();
        // HOROS-20240407準拠: テキストの幅を制限して右はみ出しを防止
        // フォントサイズをさらに小さくして、日付を1段で表示できるようにする
        Preferences prefs = Preferences.userNodeForPackage(ViewerController.class);
        int dbFontSize = prefs.getInt("dbFontSize", 0);
        int fontSize = 7; // デフォルトフォントサイズ（さらに小さく調整）
        int numberFontSize = 8; // 番号部分のフォントサイズ
        switch (dbFontSize) {
            case -1: fontSize = 6; numberFontSize = 7; break;
            case 0: fontSize = 7; numberFontSize = 8; break;
            case 1: fontSize = 9; numberFontSize = 10; break;
        }
        
        // HOROS-20240407準拠: ViewerController.m 5217行目 - [finalString setAlignment:NSCenterTextAlignment range: NSMakeRange( 0, finalString.length)];
        // テキストを中央揃えで表示
        // HOROS-20240407準拠: テキストの幅をセルサイズに合わせる
        // HTMLのbodyに幅を設定せず、divで中央揃えを確実にする
        String html = "<html><head><style>body { margin: 0; padding: 0; }</style></head><body style='text-align: center; word-wrap: break-word; overflow: hidden; font-size: " + fontSize + "px;'>";
        html += "<div style='width: 100%; max-width: " + cellSize.width + "px; margin: 0 auto; text-align: center;'>";
        html += "<span style='background-color: " + getStudyColor(studyIndex) + "; font-weight: bold; font-size: " + numberFontSize + "px;'>" + String.format(" %d ", studyIndex + 1) + "</span>";
        html += "<br>";
        if (name.length() > 0) {
            html += name + "<br>";
        }
        if (study.getDate() != null) {
            // HOROS-20240407準拠: ViewerController.m 5188行目 - [[NSUserDefaults dateTimeFormatter] stringFromDate:[curStudy date]]
            // LocalDateTime.toString()はISO 8601形式（2024-01-01T12:00:00）を返すので、「T」をスペースに置換
            String dateStr = study.getDate().toString().replace("T", " ");
            html += dateStr + "<br>";
        }
        html += String.format("%s: %d %s<br>", modality, seriesCount, seriesCount == 1 ? "series" : "series");
        if (stateText.length() > 0) {
            html += stateText + "<br>";
        }
        if (comment.length() > 0) {
            html += comment + "<br>";
        }
        if (action.length() > 0) {
            html += "<br>" + action;
        }
        html += "</div>";
        html += "</body></html>";
        
        // HOROS-20240407準拠: HTMLテキストのみを使用し、通常のテキストは設定しない
        button.setText(html);
        // HOROS-20240407準拠: ViewerController.m 5072行目 - [cell setImagePosition: NSImageBelow];
        // Studyセルには画像がないので、テキストを中央に配置
        button.setVerticalTextPosition(SwingConstants.CENTER);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        // HOROS-20240407準拠: Studyセルの枠を非表示にする（Show/Hideを押すと消える問題を解決）
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        
        // HOROS-20240407準拠: ViewerController.m 5106-5109行目
        // if( [[curStudy valueForKey: @"studyInstanceUID"] isEqualToString: study.studyInstanceUID])
        //     [cell setBackgroundColor: nil];
        // else
        //     [cell setBackgroundColor: [[self class] _differentStudyColor]];
        // 現在のStudyと一致する場合は背景色なし、異なる場合は異なるStudyの色を設定
        if (study.getStudyInstanceUID() != null && currentStudy.getStudyInstanceUID() != null 
            && study.getStudyInstanceUID().equals(currentStudy.getStudyInstanceUID())) {
            // 一致する場合：背景色なし
            button.setBackground(null);
        } else {
            // 異なるStudyの場合：異なるStudyの色を設定（gray: 0.55, 0.55, 0.55）
            button.setBackground(new Color(140, 140, 140)); // _differentStudyColor相当
        }
        
        return button;
    }
    
    /**
     * Seriesセルを作成
     * HOROS-20240407準拠: ViewerController.m 5309-5441行目
     */
    private ThumbnailButton createSeriesCell(DicomSeries series, DicomStudy curStudy, DicomStudy currentStudy, 
                                             List<DicomSeries> viewerSeries, List<DicomSeries> displayedSeries) {
        ThumbnailButton button = new ThumbnailButton();
        
        // HOROS-20240407準拠: ViewerController.m 5325行目
        // NSString *name = [curSeries valueForKey:@"name"];
        String name = series.getName();
        if (name == null) {
            name = "";
        }
        
        // HOROS-20240407準拠: ViewerController.m 5327-5331行目
        // if( [name length] > 18)
        if (name.length() > 18) {
            // [cell setFont:[NSFont boldSystemFontOfSize: [[BrowserController currentBrowser] fontSize: @"viewerSmallCellFont"]]];
            // name = [name stringByTruncatingToLength: 34];
            if (name.length() > 34) {
                name = name.substring(0, 34);
            }
        }
        
        // HOROS-20240407準拠: ViewerController.m 5333-5370行目
        // 画像数の計算
        int count = series.getNumberOfImages() != null ? series.getNumberOfImages() : 0;
        String singleType = "Image";
        String pluralType = "Images";
        
        if (count == 1) {
            // TODO: フレーム数の確認
        } else if (count == 0) {
            // TODO: rawNoFilesの確認
        }
        
        // HOROS-20240407準拠: ViewerController.m 5370行目
        // [cell setTitle:[NSString stringWithFormat:@"%@\r%@\r%@", name, [[NSUserDefaults dateTimeFormatter] stringFromDate: [curSeries valueForKey:@"date"]], N2LocalizedSingularPluralCount(count, singleType, pluralType)]];
        // HOROS-20240407準拠: 順序は name, date, count
        // ユーザー要望: Study Descriptionが最上段、真ん中が撮影日付、最下段がイメージ数
        // Study Descriptionを取得（Studyから）
        String studyDescription = "";
        if (curStudy != null) {
            studyDescription = curStudy.getStudyName();
            if (studyDescription == null || studyDescription.isEmpty()) {
                studyDescription = "Unnamed";
            }
        }
        
        // HOROS-20240407準拠: ViewerController.m 5072行目 - [cell setImagePosition: NSImageBelow];
        // テキストは上、画像は下に表示
        // HOROS-20240407準拠: ViewerController.m 5321行目 - dbSmallMatrixFont
        // HTMLを使用して複数行テキストを表示
        Dimension cellSize = button.getCellSize();
        Preferences prefs = Preferences.userNodeForPackage(ViewerController.class);
        int dbFontSize = prefs.getInt("dbFontSize", 0);
        int fontSize = 7; // デフォルトフォントサイズ（さらに小さく調整）
        switch (dbFontSize) {
            case -1: fontSize = 6; break;
            case 0: fontSize = 7; break;
            case 1: fontSize = 9; break;
        }
        
        // ユーザー要望に合わせて順序を変更: Study Description, date, count
        // HOROS-20240407準拠: ViewerController.m 5370行目 - [cell setTitle:[NSString stringWithFormat:@"%@\r%@\r%@", name, date, count]];
        // HOROS-20240407準拠: テキストを中央揃えで表示
        // HTMLのbodyに幅を設定せず、divで中央揃えを確実にする
        String html = "<html><head><style>body { margin: 0; padding: 0; }</style></head><body style='text-align: center; word-wrap: break-word; overflow: hidden; font-size: " + fontSize + "px;'>";
        html += "<div style='width: 100%; max-width: " + cellSize.width + "px; margin: 0 auto; text-align: center;'>";
        html += studyDescription + "<br>";
        if (series.getDate() != null) {
            // HOROS-20240407準拠: ViewerController.m 5370行目 - [[NSUserDefaults dateTimeFormatter] stringFromDate: [curSeries valueForKey:@"date"]]
            // LocalDateTime.toString()はISO 8601形式（2024-01-01T12:00:00）を返すので、「T」をスペースに置換
            String dateStr = series.getDate().toString().replace("T", " ");
            html += dateStr + "<br>";
        }
        html += count + " " + (count == 1 ? singleType : pluralType);
        html += "</div>";
        html += "</body></html>";
        
        // HOROS-20240407準拠: HTMLテキストのみを使用し、通常のテキストは設定しない
        button.setText(html);
        // HOROS-20240407準拠: ViewerController.m 5072行目 - [cell setImagePosition: NSImageBelow];
        // テキストは上、画像は下に表示
        button.setVerticalTextPosition(SwingConstants.TOP);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        // HOROS-20240407準拠: Seriesセルの枠を非表示にする
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        
        // HOROS-20240407準拠: ViewerController.m 5372-5383行目
        // 背景色の設定
        if (viewerSeries.contains(series)) {
            // Red - 選択されている
            button.setBackground(new Color(255, 200, 200));
        } else if (displayedSeries.contains(series)) {
            // Yellow - 開かれている
            button.setBackground(new Color(255, 255, 200));
        } else if (curStudy.getStudyInstanceUID() != null && currentStudy.getStudyInstanceUID() != null
            && !curStudy.getStudyInstanceUID().equals(currentStudy.getStudyInstanceUID())) {
            // HOROS-20240407準拠: ViewerController.m 5315-5318行目
            // if( [[curStudy valueForKey: @"studyInstanceUID"] isEqualToString: study.studyInstanceUID])
            //     [cell setBackgroundColor: nil];
            // else
            //     [cell setBackgroundColor: [[self class] _differentStudyColor]];
            // 異なるStudyの場合：異なるStudyの色を設定
            button.setBackground(new Color(240, 240, 240)); // _differentStudyColor相当
        } else {
            // 同じStudyの場合：背景色なし
            button.setBackground(null);
        }
        
        // HOROS-20240407準拠: ViewerController.m 5385-5438行目
        // サムネイル画像の設定
        System.out.println("ViewerController.createSeriesCell: creating thumbnail for series " + series.getName());
        try {
            // HOROS-20240407準拠: ViewerController.m 5387行目
            // NSImage *img = [[[NSImage alloc] initWithData: [curSeries primitiveValueForKey:@"thumbnail"]] autorelease];
            java.awt.image.BufferedImage thumbnailImage = null;
            byte[] thumbnailData = series.getThumbnail();
            System.out.println("ViewerController.createSeriesCell: thumbnailData = " + (thumbnailData != null ? thumbnailData.length + " bytes" : "null"));
            
            if (thumbnailData != null && thumbnailData.length > 0) {
                // サムネイルデータから画像を読み込む
                try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(thumbnailData)) {
                    thumbnailImage = javax.imageio.ImageIO.read(bais);
                    System.out.println("ViewerController.createSeriesCell: loaded thumbnail from data: " + (thumbnailImage != null ? thumbnailImage.getWidth() + "x" + thumbnailImage.getHeight() : "null"));
                } catch (Exception e) {
                    // サムネイルデータの読み込みに失敗した場合、後で生成する
                    System.out.println("ViewerController.createSeriesCell: failed to load thumbnail from data: " + e.getMessage());
                    thumbnailImage = null;
                }
            }
            
            // HOROS-20240407準拠: ViewerController.m 5389-5418行目
            // サムネイルが存在しない場合、DCMPixから生成
            if (thumbnailImage == null) {
                System.out.println("ViewerController.createSeriesCell: generating thumbnail from DicomPix");
                List<DicomImage> images = series.sortedImages();
                System.out.println("ViewerController.createSeriesCell: images.size() = " + (images != null ? images.size() : "null"));
                if (images != null && !images.isEmpty()) {
                    DicomImage firstImage = images.get(0);
                    System.out.println("ViewerController.createSeriesCell: firstImage = " + (firstImage != null ? firstImage.toString() : "null"));
                    try {
                        // HOROS-20240407準拠: ViewerController.m 5393行目
                        // DCMPix* dcmPix = [[DCMPix alloc] initWithPath: [[images objectAtIndex: i] valueForKey:@"completePath"] :0 :0 :nil :0 :[[[images objectAtIndex: i] valueForKeyPath:@"series.id"] intValue] isBonjour:[[BrowserController currentBrowser] isCurrentDatabaseBonjour] imageObj:[images objectAtIndex: i]];
                        com.jj.dicomviewer.model.DicomPix dcmPix = new com.jj.dicomviewer.model.DicomPix(
                            firstImage,
                            firstImage.getFrameID() != null ? firstImage.getFrameID() : 0,
                            1
                        );
                        
                        // HOROS-20240407準拠: ViewerController.m 5395行目
                        // [dcmPix CheckLoad];
                        dcmPix.loadImage();
                        System.out.println("ViewerController.createSeriesCell: dcmPix.loadImage() completed, bufferedImage = " + (dcmPix.getBufferedImage() != null ? "not null" : "null"));
                        
                        // HOROS-20240407準拠: ViewerController.m 5399行目
                        // img = [dcmPix generateThumbnailImageWithWW:0 WL:0];
                        if (dcmPix.getBufferedImage() != null) {
                            // HOROS-20240407準拠: BrowserMatrixのセルサイズと同じサイズでサムネイルを生成
                            // BrowserMatrixのセルサイズは105x113（固定）
                            // テキスト領域を確保するため、サムネイル画像は少し小さくする
                            // テキスト領域（約30-40px）を確保してサムネイルサイズを計算
                            int thumbnailHeight = cellSize.height - 40; // テキスト領域を確保
                            int thumbnailWidth = (int) (thumbnailHeight * 1.0); // アスペクト比を維持
                            if (thumbnailWidth > cellSize.width - 4) {
                                thumbnailWidth = cellSize.width - 4;
                                thumbnailHeight = thumbnailWidth;
                            }
                            System.out.println("ViewerController.createSeriesCell: generating thumbnail: " + thumbnailWidth + "x" + thumbnailHeight);
                            thumbnailImage = dcmPix.generateThumbnail(thumbnailWidth, thumbnailHeight);
                            System.out.println("ViewerController.createSeriesCell: generated thumbnail: " + (thumbnailImage != null ? thumbnailImage.getWidth() + "x" + thumbnailImage.getHeight() : "null"));
                        } else {
                            System.out.println("ViewerController.createSeriesCell: dcmPix.getBufferedImage() is null");
                        }
                    } catch (Exception e) {
                        // 画像の読み込みに失敗した場合、デフォルト画像を使用
                        System.err.println("Failed to load thumbnail for series: " + series.getName());
                        e.printStackTrace();
                    }
                }
            }
            
            // HOROS-20240407準拠: ViewerController.m 5420-5421行目
            // if( DisplayUseInvertedPolarity)
            //     img = [img imageInverted];
            boolean displayUseInvertedPolarity = prefs.getBoolean("DisplayUseInvertedPolarity", false);
            if (displayUseInvertedPolarity && thumbnailImage != null) {
                // 画像を反転（簡易実装）
                java.awt.image.BufferedImage inverted = new java.awt.image.BufferedImage(
                    thumbnailImage.getWidth(), thumbnailImage.getHeight(), 
                    java.awt.image.BufferedImage.TYPE_INT_RGB
                );
                java.awt.Graphics2D g2d = inverted.createGraphics();
                g2d.drawImage(thumbnailImage, 0, 0, null);
                g2d.dispose();
                // 簡易反転処理（実際の実装ではより高度な処理が必要）
                for (int y = 0; y < inverted.getHeight(); y++) {
                    for (int x = 0; x < inverted.getWidth(); x++) {
                        int rgb = inverted.getRGB(x, y);
                        int r = 255 - ((rgb >> 16) & 0xFF);
                        int g = 255 - ((rgb >> 8) & 0xFF);
                        int b = 255 - (rgb & 0xFF);
                        inverted.setRGB(x, y, (r << 16) | (g << 8) | b);
                    }
                }
                thumbnailImage = inverted;
            }
            
            // HOROS-20240407準拠: ViewerController.m 5423-5437行目
            // dbFontSizeに応じて画像をスケーリング
            if (thumbnailImage != null) {
                double scale = 1.0;
                switch (dbFontSize) {
                    case -1:
                        scale = 0.6;
                        break;
                    case 0:
                        scale = 1.0;
                        break;
                    case 1:
                        scale = 1.3;
                        break;
                }
                
                if (scale != 1.0) {
                    int newWidth = (int) (thumbnailImage.getWidth() * scale);
                    int newHeight = (int) (thumbnailImage.getHeight() * scale);
                    java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(
                        newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_RGB
                    );
                    java.awt.Graphics2D g2d = scaled.createGraphics();
                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                                        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(thumbnailImage, 0, 0, newWidth, newHeight, null);
                    g2d.dispose();
                    thumbnailImage = scaled;
                }
                
                // HOROS-20240407準拠: ViewerController.m 5430行目
                // [cell setImage: img];
                button.setIcon(new javax.swing.ImageIcon(thumbnailImage));
            } else {
                // HOROS-20240407準拠: ViewerController.m 5406, 5409, 5416行目
                // img = [NSImage imageNamed:@"FileNotFound.tif"];
                // TODO: デフォルト画像を設定
            }
        } catch (Exception e) {
            System.err.println("Error loading thumbnail for series: " + series.getName());
            e.printStackTrace();
        }
        
        return button;
    }
    
    /**
     * Studyの色を取得
     * HOROS-20240407準拠: ViewerController.m 5200-5205行目
     */
    private String getStudyColor(int studyIndex) {
        // HOROS-20240407準拠: ViewerController.studyColors
        // 簡易実装
        Color[] colors = {
            new Color(255, 200, 200),
            new Color(200, 255, 200),
            new Color(200, 200, 255),
            new Color(255, 255, 200),
            new Color(255, 200, 255),
            new Color(200, 255, 255)
        };
        Color color = colors[studyIndex % colors.length];
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    /**
     * Studyの表示/非表示を切り替え
     * HOROS-20240407準拠: ViewerController.m 4845行目 - matrixPreviewSwitchHidden:
     */
    private void matrixPreviewSwitchHidden(DicomStudy study) {
        // HOROS-20240407準拠: ViewerController.m 4857行目
        // [curStudy setHidden: ![curStudy isHidden]];
        System.out.println("ViewerController.matrixPreviewSwitchHidden: study = " + study.getStudyInstanceUID() + ", isHidden = " + study.isHidden());
        study.setHidden(!study.isHidden());
        System.out.println("ViewerController.matrixPreviewSwitchHidden: after setHidden, isHidden = " + study.isHidden());
        
        // HOROS-20240407準拠: データベースに保存
        com.jj.dicomviewer.model.DicomDatabase db = com.jj.dicomviewer.model.DicomDatabase.defaultDatabase();
        if (db != null) {
            try {
                // HOROS-20240407準拠: Core Dataでは自動的に保存されるが、Javaでは明示的に保存する必要がある
                // updateStudyHiddenState()を呼び出してhidden状態を更新
                db.updateStudyHiddenState(study.getStudyInstanceUID(), study.isHidden());
                System.out.println("ViewerController.matrixPreviewSwitchHidden: saved to database");
            } catch (Exception e) {
                System.err.println("ViewerController.matrixPreviewSwitchHidden: failed to save to database: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // HOROS-20240407準拠: ViewerController.m 4859-4865行目
        // for( ViewerController *v in [ViewerController getDisplayed2DViewers])
        // {
        //     if( [v.studyInstanceUID isEqualToString: self.studyInstanceUID)
        //         [v buildMatrixPreview: NO];
        //     else
        //         [v buildMatrixPreview: YES];
        // }
        // HOROS-20240407準拠: ViewerController.m 4859-4865行目
        // 全てのViewerControllerのbuildMatrixPreview:を呼び出す
        // 簡易実装: 現在のViewerControllerのみ更新（SwingUtilities.invokeLaterでUIスレッドで実行）
        System.out.println("ViewerController.matrixPreviewSwitchHidden: calling updatePreviewMatrix(false)");
        javax.swing.SwingUtilities.invokeLater(() -> {
            updatePreviewMatrix(false);
        });
    }
    
    /**
     * Seriesを選択
     * HOROS-20240407準拠: ViewerController.m 4413行目 - matrixPreviewPressed:
     */
    private void matrixPreviewPressed(DicomSeries series, boolean rightClick) {
        // HOROS-20240407準拠: ViewerController.m 4433行目
        // [self loadSelectedSeries: series rightClick: cell.rightClick];
        loadSelectedSeries(series, rightClick);
    }
    
    /**
     * 選択されたSeriesを読み込む
     * HOROS-20240407準拠: ViewerController.m 4272行目 - loadSelectedSeries:rightClick:
     */
    private void loadSelectedSeries(Object series, boolean rightClick) {
        // HOROS-20240407準拠: ViewerController.m 4274-4283行目
        // SeriesがDicomStudyの場合は、最初のSeriesを取得
        if (series instanceof DicomStudy) {
            DicomStudy s = (DicomStudy) series;
            List<DicomSeries> seriesArray = s.imageSeriesContainingPixels(true);
            if (seriesArray != null && !seriesArray.isEmpty()) {
                series = seriesArray.get(0);
            } else {
                return;
            }
        }
        
        if (!(series instanceof DicomSeries)) {
            return;
        }
        
        DicomSeries targetSeries = (DicomSeries) series;
        
        // HOROS-20240407準拠: ViewerController.m 4285-4291行目
        // 現在のviewerSeriesを取得（fileListから）
        List<DicomSeries> viewerSeries = new ArrayList<>();
        for (int i = 0; i < maxMovieIndex; i++) {
            if (fileList[i] != null && !fileList[i].isEmpty()) {
                Object img = fileList[i].get(0);
                if (img instanceof DicomImage) {
                    DicomSeries s = ((DicomImage) img).getSeries();
                    if (s != null) {
                        viewerSeries.add(s);
                    }
                }
            }
        }
        
        // HOROS-20240407準拠: ViewerController.m 4293-4323行目
        // rightClickまたはCommandキーの場合：新しいViewerControllerを作成
        // TODO: Commandキーの検出（現在はrightClickのみ対応）
        if (rightClick) {
            // HOROS-20240407準拠: ViewerController.m 4301行目
            // ViewerController *newViewer = [[BrowserController currentBrowser] loadSeries :series :nil :YES keyImagesOnly: displayOnlyKeyImages];
            // HOROS-20240407準拠: ViewerController.m 4301行目
            // ViewerController *newViewer = [[BrowserController currentBrowser] loadSeries :series :nil :YES keyImagesOnly: displayOnlyKeyImages];
            // TODO: 新しいViewerControllerを作成する実装（現在は簡易実装）
            BrowserController browser = BrowserController.currentBrowser();
            if (browser != null) {
                List<DicomImage> images = targetSeries.sortedImages();
                if (images != null && !images.isEmpty()) {
                    // TODO: viewerDICOMIntを使用して新しいViewerControllerを作成
                    System.out.println("ViewerController.loadSelectedSeries: creating new viewer for series: " + targetSeries.getName());
                    // TODO: 実装
                }
            }
            return;
        }
        
        // HOROS-20240407準拠: ViewerController.m 4324-4379行目
        // それ以外の場合
        if (!viewerSeries.contains(targetSeries)) {
            // HOROS-20240407準拠: ViewerController.m 4326-4365行目
            // viewerSeriesに含まれていない場合：新しいSeriesを読み込む
            // HOROS-20240407準拠: ViewerController.m 4358行目
            // [[BrowserController currentBrowser] loadSeries :series :self :YES keyImagesOnly: displayOnlyKeyImages];
            // 現在のViewerControllerにSeriesを追加
            List<DicomImage> images = targetSeries.sortedImages();
            if (images != null && !images.isEmpty()) {
                System.out.println("ViewerController.loadSelectedSeries: loading new series: " + targetSeries.getName());
                
                // HOROS-20240407準拠: fileListとpixListにSeriesを追加
                if (maxMovieIndex < MAX4D) {
                    List<Object> newFileList = new ArrayList<>();
                    List<Object> newPixList = new ArrayList<>();
                    
                    for (DicomImage image : images) {
                        newFileList.add(image);
                        // TODO: DicomPixを作成してpixListに追加
                        // DicomPix pix = new DicomPix(image, ...);
                        // newPixList.add(pix);
                    }
                    
                    fileList[maxMovieIndex] = newFileList;
                    pixList[maxMovieIndex] = newPixList;
                    maxMovieIndex++;
                    
                    // HOROS-20240407準拠: 新しいSeriesを表示
                    setMovieIndex((short)(maxMovieIndex - 1));
                    
                    // HOROS-20240407準拠: ViewerController.m 4360-4361行目
                    // [self showCurrentThumbnail:self];
                    // [self updateNavigator];
                    updatePreviewMatrix(false);
                }
            }
        } else if (!targetSeries.equals(getCurrentSeries())) {
            // HOROS-20240407準拠: ViewerController.m 4367-4376行目
            // viewerSeriesに含まれているが、現在のcurMovieIndexと異なる場合：setMovieIndexで切り替え
            int idx = viewerSeries.indexOf(targetSeries);
            if (idx >= 0 && idx < maxMovieIndex) {
                setMovieIndex((short) idx);
                propagateSettings();
            }
        } else {
            // HOROS-20240407準拠: ViewerController.m 4378行目
            // 同じ場合：mouseMovedを呼ぶ
            // TODO: mouseMovedの実装
        }
    }
    
    /**
     * 現在表示されているSeriesを取得
     * HOROS-20240407準拠: ViewerController.m 4367行目
     */
    private DicomSeries getCurrentSeries() {
        if (fileList[curMovieIndex] != null && !fileList[curMovieIndex].isEmpty()) {
            Object img = fileList[curMovieIndex].get(0);
            if (img instanceof DicomImage) {
                return ((DicomImage) img).getSeries();
            }
        }
        return null;
    }
    
    /**
     * ムービーインデックスを設定
     * HOROS-20240407準拠: ViewerController.m setMovieIndex:
     */
    private void setMovieIndex(short index) {
        if (index < 0 || index >= maxMovieIndex) {
            return;
        }
        
        curMovieIndex = index;
        
        // HOROS-20240407準拠: 画像を更新
        if (imageView != null && pixList[curMovieIndex] != null && !pixList[curMovieIndex].isEmpty()
            && fileList[curMovieIndex] != null && !fileList[curMovieIndex].isEmpty()) {
            
            // HOROS-20240407準拠: DCMView.setPixels()を呼び出す
            List<DicomPix> pixListForView = new ArrayList<>();
            for (Object pix : pixList[curMovieIndex]) {
                if (pix instanceof DicomPix) {
                    pixListForView.add((DicomPix) pix);
                }
            }
            
            List<Object> fileListForView = new ArrayList<>(fileList[curMovieIndex]);
            
            // HOROS-20240407準拠: DCMView.m 2176行目 - setPixels:files:rois:firstImage:level:reset:
            imageView.setPixels(pixListForView, fileListForView, null, (short) 0, (char) 0, false);
            
            // HOROS-20240407準拠: 最初の画像を表示
            if (!pixListForView.isEmpty()) {
                imageView.setIndex(0);
            }
            
            System.out.println("ViewerController.setMovieIndex: setting movie index to " + index + ", pixList size: " + pixListForView.size());
        }
    }
    
    /**
     * 設定を伝播
     * HOROS-20240407準拠: ViewerController.m propagateSettings
     */
    private void propagateSettings() {
        // TODO: 実装
        System.out.println("ViewerController.propagateSettings: propagating settings");
    }
    
    /**
     * 表示されているSeriesを取得
     * HOROS-20240407準拠: ViewerController.m getDisplayedSeries
     */
    private List<DicomSeries> getDisplayedSeries() {
        // HOROS-20240407準拠: ViewerController.m getDisplayedSeries
        // TODO: 実装
        return new ArrayList<>();
    }
}
