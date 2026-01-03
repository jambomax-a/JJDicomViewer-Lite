package com.jj.dicomviewer.ui;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * ThumbnailsListPanel - サムネイルリストパネル
 * 
 * HOROS-20240407のThumbnailsListPanelをJava Swingに移植
 * HOROS-20240407準拠: ThumbnailsListPanel.h, ThumbnailsListPanel.m
 * 
 * サムネイルを別ウィンドウとして表示する
 */
public class ThumbnailsListPanel extends JFrame {
    
    // HOROS-20240407準拠: AppController.m 123行目
    // ThumbnailsListPanel *thumbnailsListPanel[ MAXSCREENS] = {nil, nil, nil, nil, nil, nil, nil, nil, nil, nil};
    private static final int MAXSCREENS = 10;
    private static ThumbnailsListPanel[] thumbnailsListPanel = new ThumbnailsListPanel[MAXSCREENS];
    
    // HOROS-20240407準拠: ThumbnailsListPanel.h 43行目
    // NSView *thumbnailsView;
    private java.awt.Component thumbnailsView;
    
    // HOROS-20240407準拠: ThumbnailsListPanel.h 45行目
    // long screen;
    private int screen;
    
    // HOROS-20240407準拠: ThumbnailsListPanel.h 46行目
    // ViewerController *viewer;
    private ViewerController viewer;
    
    // HOROS-20240407準拠: ThumbnailsListPanel.m 53行目 - fixedWidth
    public static int getFixedWidth() {
        Preferences prefs = Preferences.userNodeForPackage(ThumbnailsListPanel.class);
        int dbFontSize = prefs.getInt("dbFontSize", 0);
        
        float w = 0;
        switch (dbFontSize) {
            case -1:
                w = 100 * 0.8f;
                break;
            case 0:
                w = 100;
                break;
            case 1:
                w = 100 * 1.3f;
                break;
            default:
                w = 100;
                break;
        }
        
        w += 10;
        return (int) w;
    }
    
    /**
     * コンストラクタ
     * HOROS-20240407準拠: ThumbnailsListPanel.m 99行目 - initForScreen:
     */
    private ThumbnailsListPanel(int screenIndex) {
        super("Thumbnails");
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 101行目
        // screen = s;
        this.screen = screenIndex;
        
        // HOROS-20240407準拠: サムネイルウィンドウはタイトルバーを表示しない
        setUndecorated(true);
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 105行目
        // thumbnailsView = nil;
        this.thumbnailsView = null;
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 107行目
        // [[self window] setAnimationBehavior: NSWindowAnimationBehaviorNone];
        // TODO: アニメーション動作の設定
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 108行目
        // [[self window] setLevel: NSNormalWindowLevel];
        setAlwaysOnTop(false);
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 110行目
        // [self applicationDidChangeScreenParameters: nil];
        applicationDidChangeScreenParameters();
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 124行目
        // [self.window safelySetMovable:NO];
        setResizable(false);
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 114行目
        // [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(viewerWillClose:) name: OsirixCloseViewerNotification object: nil];
        // ビューワーが閉じる時に通知を受け取る（Javaでは直接メソッド呼び出しで実装）
        
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 142-168行目
        // windowDidResignKeyとwindowDidBecomeKeyで、サムネイルウィンドウの順序を管理
        // ただし、フォーカスが回るのを防ぐため、windowActivated/windowDeactivatedイベントハンドラは削除
        // サムネイルウィンドウは常にビューワーの上に配置されるが、フォーカスはビューワーに維持される
    }
    
    /**
     * ビューワーが閉じる時の処理
     * HOROS-20240407準拠: ThumbnailsListPanel.m viewerWillClose相当
     */
    public void viewerWillClose(ViewerController closingViewer) {
        // HOROS-20240407準拠: ビューワーが閉じる時にサムネイルウィンドウを非表示にする
        if (closingViewer == viewer) {
            setVisible(false);
            thumbnailsView = null;
            viewer = null;
        }
    }
    
    /**
     * 画面ごとのThumbnailsListPanelを取得
     * HOROS-20240407準拠: AppController.m 830行目 - thumbnailsListPanelForScreen:
     */
    public static ThumbnailsListPanel getInstanceForScreen(GraphicsDevice screen) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();
        
        int screenIndex = -1;
        for (int i = 0; i < screens.length; i++) {
            if (screens[i] == screen) {
                screenIndex = i;
                break;
            }
        }
        
        if (screenIndex == -1 || screenIndex >= MAXSCREENS) {
            return null;
        }
        
        // HOROS-20240407準拠: AppController.m 773行目
        // thumbnailsListPanel[ i] = [[ThumbnailsListPanel alloc] initForScreen: i];
        if (thumbnailsListPanel[screenIndex] == null) {
            thumbnailsListPanel[screenIndex] = new ThumbnailsListPanel(screenIndex);
        }
        
        return thumbnailsListPanel[screenIndex];
    }
    
    /**
     * 画面インデックスでThumbnailsListPanelを取得
     * HOROS-20240407準拠: AppController.m 830行目 - thumbnailsListPanelForScreen:
     */
    public static ThumbnailsListPanel getInstanceForScreenIndex(int screenIndex) {
        if (screenIndex < 0 || screenIndex >= MAXSCREENS) {
            return null;
        }
        
        if (thumbnailsListPanel[screenIndex] == null) {
            thumbnailsListPanel[screenIndex] = new ThumbnailsListPanel(screenIndex);
        }
        
        return thumbnailsListPanel[screenIndex];
    }
    
    /**
     * 画面パラメータ変更時の処理
     * HOROS-20240407準拠: ThumbnailsListPanel.m 75行目 - applicationDidChangeScreenParameters:
     */
    public void applicationDidChangeScreenParameters() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 77行目
        // if ([[NSScreen screens] count] <= screen) return;
        if (screen >= screens.length) {
            return;
        }
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 80行目
        // NSRect screenRect = [[[NSScreen screens] objectAtIndex:screen] visibleFrame];
        // visibleFrameはメニューバーやDockを除いた領域
        GraphicsDevice screenDevice = screens[screen];
        Rectangle screenBounds = screenDevice.getDefaultConfiguration().getBounds();
        
        // Java Swingでは、Toolkit.getScreenInsets()を使用してタスクバーやメニューバーの領域を取得
        java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
        java.awt.Insets screenInsets = toolkit.getScreenInsets(screenDevice.getDefaultConfiguration());
        
        // visibleFrame相当の領域を計算（タスクバーやメニューバーを除く）
        int visibleX = screenBounds.x + screenInsets.left;
        int visibleY = screenBounds.y + screenInsets.top;
        int visibleWidth = screenBounds.width - screenInsets.left - screenInsets.right;
        int visibleHeight = screenBounds.height - screenInsets.top - screenInsets.bottom;
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 82-87行目
        // NSRect dstframe;
        // dstframe.size.height = screenRect.size.height;
        // dstframe.size.width = [ThumbnailsListPanel fixedWidth];
        // dstframe.origin.x = screenRect.origin.x;
        // dstframe.origin.y = screenRect.origin.y;
        // dstframe.size.height -= [ToolbarPanelController exposedHeight];
        // サムネイルウィンドウは画面の左側、ツールバーの下に配置される
        // 注意: macOSではY座標は下方向が正で、画面の下部がY=0、上部がY=最大値
        // Java Swingでは、Y座標は上方向が正で、画面の上部がY=0、下部がY=最大値
        // したがって、サムネイルウィンドウはツールバーの下に配置するには、Y座標をツールバーの高さ分下にずらす必要がある
        int width = getFixedWidth();
        int height = visibleHeight;
        int x = visibleX;
        // HOROS-20240407準拠: ThumbnailsListPanel.m 86-87行目
        // dstframe.origin.y = screenRect.origin.y;
        // dstframe.size.height -= [ToolbarPanelController exposedHeight];
        // サムネイルウィンドウは画面の上端から開始し、高さをツールバーの高さ（exposedHeight）分減らす
        // ただし、実際にはツールバーの下に配置する必要があるため、Y座標も調整する
        // HOROS-20240407では、サムネイルウィンドウは画面の上端から開始し、高さをexposedHeight分減らすだけ
        // しかし、Java Swingでは、ツールバーの下に配置するために、Y座標も調整する必要がある
        int y = visibleY;
        
        // HOROS-20240407準拠: ToolbarPanelController.exposedHeightを引く
        // 高さを減らすことで、ツールバーの下に配置される
        // ビューワーウィンドウと同様に、ツールバーウィンドウの実際の高さ（FIXED_HEIGHT = 92）を使用
        int toolbarHeight = ToolbarPanelController.FIXED_HEIGHT; // 92（ツールバーウィンドウの実際の高さ）
        height -= toolbarHeight;
        
        // サムネイルウィンドウのY座標をツールバーの下に配置するため、toolbarHeight分下にずらす
        // これにより、ツールバーと重ならないようにする
        y += toolbarHeight;
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 89-93行目
        // NavigatorWindowControllerの処理（TODO）
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 95-96行目
        // if( NSEqualRects(self.window.frame, dstframe) == NO)
        //     [[self window] setFrame:dstframe display:YES];
        Rectangle currentBounds = getBounds();
        if (currentBounds.x != x || currentBounds.y != y || currentBounds.width != width || currentBounds.height != height) {
            setBounds(x, y, width, height);
        }
    }
    
    /**
     * サムネイルビューを設定
     * HOROS-20240407準拠: ThumbnailsListPanel.m 254行目 - setThumbnailsView:viewer:
     */
    public void setThumbnailsView(java.awt.Component tb, ViewerController v) {
        // HOROS-20240407準拠: ThumbnailsListPanel.m 254行目 - setThumbnailsView:viewer:
        // 注意: ユーザー要求により、常に別ウィンドウとして実装するため、
        // UseFloatingThumbnailsListのチェックは行わない（常に別ウィンドウとして表示）
        
        Preferences prefs = Preferences.userNodeForPackage(ThumbnailsListPanel.class);
        // HOROS-20240407準拠: ThumbnailsListPanel.m 256行目
        // if( [[NSUserDefaults standardUserDefaults] boolForKey: @"UseFloatingThumbnailsList"] == NO) return;
        // 常に別ウィンドウとして実装するため、このチェックはスキップ
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 263行目
        // boolean seriesListVisible = prefs.getBoolean("SeriesListVisible", true);
        // if( [[NSUserDefaults standardUserDefaults] boolForKey: @"SeriesListVisible"] == NO) tb = nil;
        boolean seriesListVisible = prefs.getBoolean("SeriesListVisible", true);
        if (!seriesListVisible) {
            tb = null;
        }
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 292行目
        // [viewer release];
        // viewer = [v retain];
        this.viewer = v;
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 268-290行目
        // 同じthumbnailsViewの場合は、ウィンドウの順序のみ調整
        if (thumbnailsView == tb) {
            if (tb != null && v != null && v.isVisible()) {
                // HOROS-20240407準拠: ThumbnailsListPanel.m 270-271行目
                // [[self window] orderWindow: NSWindowAbove relativeTo: [[v window] windowNumber]];
                toFront();
            }
            if (tb == null && isVisible()) {
                // HOROS-20240407準拠: ThumbnailsListPanel.m 285-286行目
                // [self.window orderOut: self];
                setVisible(false);
            }
            return;
        }
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 292-293行目
        // [viewer release];
        // viewer = [v retain];
        this.viewer = v;
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 295-307行目
        // thumbnailsViewの設定
        if (thumbnailsView != null && thumbnailsView.getParent() != null) {
            Container parent = thumbnailsView.getParent();
            parent.remove(thumbnailsView);
        }
        
        this.thumbnailsView = tb;
        
        if (tb != null) {
            getContentPane().setLayout(new BorderLayout());
            getContentPane().removeAll();
            getContentPane().add(tb, BorderLayout.CENTER);
            revalidate();
            repaint();
        }
        
        // HOROS-20240407準拠: ThumbnailsListPanel.m 309-352行目
        // thumbnailsViewがある場合、ウィンドウを表示
        if (thumbnailsView != null) {
            // HOROS-20240407準拠: ThumbnailsListPanel.m 313-352行目
            // ウィンドウの表示と位置調整
            applicationDidChangeScreenParameters();
            setVisible(true);
            toFront();
            
            // HOROS-20240407準拠: ThumbnailsListPanel.m 270-271行目
            // viewerウィンドウの上に表示
            if (v != null && v.isVisible()) {
                toFront();
            }
        } else {
            // HOROS-20240407準拠: ThumbnailsListPanel.m 285-286行目
            // thumbnailsViewがない場合、ウィンドウを非表示
            setVisible(false);
        }
    }
    
    /**
     * サムネイルビューを取得
     * HOROS-20240407準拠: ThumbnailsListPanel.m 249行目 - thumbnailsView
     */
    public java.awt.Component getThumbnailsView() {
        return thumbnailsView;
    }
    
    /**
     * ViewerControllerを取得
     * HOROS-20240407準拠: ThumbnailsListPanel.h 50行目 - @property (readonly) ViewerController *viewer;
     */
    public ViewerController getViewer() {
        return viewer;
    }
    
    /**
     * サムネイルリストが閉じられる時の処理
     * HOROS-20240407準拠: ThumbnailsListPanel.m 220行目 - thumbnailsListWillClose:
     */
    public void thumbnailsListWillClose(java.awt.Component tb) {
        // HOROS-20240407準拠: ThumbnailsListPanel.m 222行目
        // if( thumbnailsView == tb)
        if (thumbnailsView == tb) {
            // HOROS-20240407準拠: ThumbnailsListPanel.m 224行目
            // [[self window] orderOut: self];
            setVisible(false);
            
            // HOROS-20240407準拠: ThumbnailsListPanel.m 233-234行目
            // [thumbnailsView release];
            // thumbnailsView = 0L;
            thumbnailsView = null;
            
            // HOROS-20240407準拠: ThumbnailsListPanel.m 236行目
            // [viewer release];
            // viewer = 0L;
            viewer = null;
        }
    }
}
