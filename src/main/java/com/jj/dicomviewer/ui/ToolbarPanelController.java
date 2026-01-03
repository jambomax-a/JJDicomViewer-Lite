package com.jj.dicomviewer.ui;

import javax.swing.*;
import java.awt.*;

/**
 * ToolbarPanelController - ツールバーパネルコントローラー
 * 
 * HOROS-20240407のToolbarPanelControllerをJava Swingに移植
 * HOROS-20240407準拠: ToolbarPanel.h, ToolbarPanel.m
 * 
 * ツールバーを別ウィンドウとして表示する
 */
public class ToolbarPanelController extends JFrame {
    
    // HOROS-20240407準拠: ToolbarPanel.h 44行目
    // NSToolbar *toolbar;
    private JToolBar toolbar;
    
    // HOROS-20240407準拠: ToolbarPanel.h 45行目
    // ViewerController *viewer;
    private ViewerController viewer;
    
    // HOROS-20240407準拠: ToolbarPanel.m 50行目
    // static int fixedHeight = 92;
    public static final int FIXED_HEIGHT = 92;
    
    // HOROS-20240407準拠: ToolbarPanel.m 61行目
    // + (long) hiddenHeight { return 15; }
    private static final int HIDDEN_HEIGHT = 15;
    
    /**
     * コンストラクタ
     * HOROS-20240407準拠: ToolbarPanel.m 101行目 - initForViewer:withToolbar:
     */
    public ToolbarPanelController(ViewerController v, JToolBar t) {
        super("Toolbar");
        
        // HOROS-20240407準拠: ToolbarPanel.m 105-106行目
        // toolbar = [t retain];
        // viewer = [v retain];
        this.toolbar = t;
        this.viewer = v;
        
        // HOROS-20240407準拠: ツールバーウィンドウはタイトルバーを表示しない
        // XIBファイルではtitled="YES"だが、実際の動作ではタイトルバーが表示されない仕様
        setUndecorated(true);
        
        // HOROS-20240407準拠: ToolbarPanel.m 108行目
        // [[self window] setAnimationBehavior: NSWindowAnimationBehaviorNone];
        // TODO: アニメーション動作の設定
        
        // HOROS-20240407準拠: ToolbarPanel.m 109行目
        // [[self window] setToolbar: toolbar];
        if (toolbar != null) {
            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(toolbar, BorderLayout.CENTER);
        }
        
        // HOROS-20240407準拠: ToolbarPanel.m 110行目
        // [[self window] setLevel: NSNormalWindowLevel];
        setAlwaysOnTop(false);
        
        // HOROS-20240407準拠: ToolbarPanel.m 111行目
        // [[self window] makeMainWindow];
        // TODO: メインウィンドウの設定
        
        // HOROS-20240407準拠: ToolbarPanel.m 113-114行目
        // [toolbar setShowsBaselineSeparator: NO];
        // [toolbar setVisible: YES];
        if (toolbar != null) {
            toolbar.setFloatable(false);
        }
        
        // HOROS-20240407準拠: ToolbarPanel.m 116行目
        // [self applicationDidChangeScreenParameters: nil];
        applicationDidChangeScreenParameters();
        
        // HOROS-20240407準拠: ToolbarPanel.m 128行目
        // [self.window safelySetMovable:NO];
        setResizable(false);
        
        // HOROS-20240407準拠: ToolbarPanel.m 129行目
        // [self.window setShowsToolbarButton:NO];
        // TODO: ツールバーボタンの非表示
        
        // HOROS-20240407準拠: ToolbarPanel.m 120行目
        // [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(viewerWillClose:) name: OsirixCloseViewerNotification object: nil];
        // ビューワーが閉じる時に通知を受け取る（Javaでは直接メソッド呼び出しで実装）
        
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // HOROS-20240407準拠: ToolbarPanel.m 153-167行目
        // windowDidBecomeKeyで、ツールバーウィンドウがキーになった時にビューワーウィンドウを前面に出す
        // ただし、フォーカスが回るのを防ぐため、windowActivatedイベントでは何もしない
        // HOROS-20240407準拠: ToolbarPanel.m 157-162行目
        // ツールバーウィンドウは常にビューワーの後ろに配置される（orderBack）
        // フォーカスが回るのを防ぐため、windowActivatedイベントハンドラは削除
    }
    
    /**
     * ビューワーが閉じる時の処理
     * HOROS-20240407準拠: ToolbarPanel.m 192行目 - viewerWillClose:
     */
    public void viewerWillClose(ViewerController closingViewer) {
        // HOROS-20240407準拠: ToolbarPanel.m 194-195行目
        // if( [n object] == viewer)
        //     [self.window orderOut: self];
        if (closingViewer == viewer) {
            setVisible(false);
        }
    }
    
    /**
     * 固定高さを取得
     * HOROS-20240407準拠: ToolbarPanel.m 56行目 - fixedHeight
     */
    public int getFixedHeight() {
        return FIXED_HEIGHT;
    }
    
    /**
     * 隠れた高さを取得
     * HOROS-20240407準拠: ToolbarPanel.m 61行目 - hiddenHeight
     */
    public static int getHiddenHeight() {
        return HIDDEN_HEIGHT;
    }
    
    /**
     * 表示された高さを取得（静的メソッド）
     * HOROS-20240407準拠: ToolbarPanel.m 65行目 - exposedHeight
     */
    public static int getExposedHeight() {
        return FIXED_HEIGHT - HIDDEN_HEIGHT;
    }
    
    /**
     * 表示された高さを取得（インスタンスメソッド）
     * HOROS-20240407準拠: ToolbarPanel.m 65行目 - exposedHeight
     */
    public int getExposedHeightInstance() {
        return getExposedHeight();
    }
    
    /**
     * 画面パラメータ変更時の処理
     * HOROS-20240407準拠: ToolbarPanel.m 87行目 - applicationDidChangeScreenParameters:
     */
    public void applicationDidChangeScreenParameters() {
        if (viewer == null || !viewer.isVisible()) {
            return;
        }
        
        // HOROS-20240407準拠: ToolbarPanel.m 89行目
        // NSRect screenRect = [viewer.window.screen visibleFrame];
        // visibleFrameはメニューバーやDockを除いた領域
        GraphicsDevice screen = viewer.getGraphicsConfiguration().getDevice();
        Rectangle screenBounds = screen.getDefaultConfiguration().getBounds();
        
        // Java Swingでは、Toolkit.getScreenInsets()を使用してタスクバーやメニューバーの領域を取得
        java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
        java.awt.Insets screenInsets = toolkit.getScreenInsets(screen.getDefaultConfiguration());
        
        // visibleFrame相当の領域を計算（タスクバーやメニューバーを除く）
        int visibleX = screenBounds.x + screenInsets.left;
        int visibleY = screenBounds.y + screenInsets.top;
        int visibleWidth = screenBounds.width - screenInsets.left - screenInsets.right;
        int visibleHeight = screenBounds.height - screenInsets.top - screenInsets.bottom;
        
        // HOROS-20240407準拠: ToolbarPanel.m 91-95行目
        // NSRect dstframe;
        // dstframe.size.height = [self fixedHeight];
        // dstframe.size.width = screenRect.size.width;
        // dstframe.origin.x = screenRect.origin.x;
        // dstframe.origin.y = screenRect.origin.y + screenRect.size.height - dstframe.size.height + [ToolbarPanelController hiddenHeight];
        // ツールバーウィンドウは画面の上部に配置される
        // screenRect.origin.y + screenRect.size.height は画面の下端
        // そこから dstframe.size.height を引くと、ツールバーの上端のY座標
        // さらに hiddenHeight を足すと、ツールバーウィンドウのY座標（画面の上部）
        int width = visibleWidth;
        int height = FIXED_HEIGHT;
        int x = visibleX;
        // HOROS-20240407準拠: 画面の上部に配置
        // HOROS-20240407準拠: ToolbarPanel.m 95行目
        // dstframe.origin.y = screenRect.origin.y + screenRect.size.height - dstframe.size.height + [ToolbarPanelController hiddenHeight];
        // macOSではY座標は下方向が正で、画面の下部がY=0、上部がY=最大値
        // screenRect.origin.y + screenRect.size.height は画面の下端
        // そこから dstframe.size.height を引くと、ツールバーの上端
        // さらに hiddenHeight を足すと、ツールバーウィンドウのY座標
        // Java Swingでは、Y座標は上方向が正で、画面の上部がY=0、下部がY=最大値
        // したがって、Java Swingでは画面の上部に配置するには visibleY が正しい
        // hiddenHeightはツールバーウィンドウの一部が画面外に隠れるためのオフセット
        // Java Swingでは、setUndecorated(true)を使用しているため、タイトルバーはないが、境界線は存在する可能性がある
        // ただし、HOROS-20240407では、ツールバーウィンドウは画面の上部に配置され、hiddenHeight分だけ上に隠れる
        // Java Swingでは、これを実現するために、Y座標を負の値にすることはできないため、visibleYから開始する
        int y = visibleY;
        
        // デバッグ: 計算結果を確認
        // System.out.println("ToolbarPanel: visibleY=" + visibleY + ", visibleHeight=" + visibleHeight + ", height=" + height + ", HIDDEN_HEIGHT=" + HIDDEN_HEIGHT + ", y=" + y);
        
        // HOROS-20240407準拠: ToolbarPanel.m 97-98行目
        // if( NSEqualRects( dstframe, self.window.frame) == NO)
        //     [[self window] setFrame:dstframe display:YES];
        Rectangle currentBounds = getBounds();
        if (currentBounds.x != x || currentBounds.y != y || currentBounds.width != width || currentBounds.height != height) {
            setBounds(x, y, width, height);
        }
    }
    
    /**
     * ViewerControllerを取得
     * HOROS-20240407準拠: ToolbarPanel.h 49行目 - @property (readonly) ViewerController *viewer;
     */
    public ViewerController getViewer() {
        return viewer;
    }
    
    /**
     * ツールバーを取得
     * HOROS-20240407準拠: ToolbarPanel.m 187行目 - toolbar
     */
    public JToolBar getToolbar() {
        return toolbar;
    }
    
    /**
     * 閉じる
     * HOROS-20240407準拠: ToolbarPanel.m 135行目 - close
     */
    @Override
    public void dispose() {
        // HOROS-20240407準拠: ToolbarPanel.m 137行目
        // [self.window orderOut: self];
        setVisible(false);
        
        // HOROS-20240407準拠: ToolbarPanel.m 141行目
        // self.window.toolbar = nil;
        // TODO: ツールバーのクリア
        
        super.dispose();
    }
}
