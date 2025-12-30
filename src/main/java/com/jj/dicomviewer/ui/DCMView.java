package com.jj.dicomviewer.ui;

import java.util.List;
import java.util.ArrayList;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Dimension;
import javax.swing.JPanel;

import com.jj.dicomviewer.model.DicomPix;

/**
 * DCMView - DICOM画像表示ビュー
 * 
 * HOROS-20240407のDCMViewをJava Swingに移植
 * HOROS-20240407準拠: DCMView.h, DCMView.m
 */
public class DCMView extends JPanel {
    
    // HOROS-20240407準拠: DCMView.h 172行目 - NSMutableArray *dcmPixList;
    protected List<DicomPix> dcmPixList;
    
    // HOROS-20240407準拠: DCMView.h 173行目 - NSArray *dcmFilesList;
    protected List<Object> dcmFilesList;
    
    // HOROS-20240407準拠: DCMView.h 174行目 - NSMutableArray *dcmRoiList, *curRoiList;
    protected List<Object> dcmRoiList;
    
    // HOROS-20240407準拠: DCMView.h 175行目 - DCMPix *curDCM;
    protected DicomPix curDCM;
    
    // HOROS-20240407準拠: DCMView.h 178行目 - char listType;
    protected char listType;
    
    // HOROS-20240407準拠: DCMView.h 180行目 - short curImage, startImage;
    protected short curImage = 0;
    protected short startImage = 0;
    
    // HOROS-20240407準拠: DCMView.h 147行目 - BOOL volumicSeries;
    protected boolean volumicSeries = false;
    
    // HOROS-20240407準拠: DCMView.h 146行目 - int volumicData;
    protected int volumicData = -1;
    
    // HOROS-20240407準拠: DCMView.m 2178行目 - drawLock（Javaではsynchronizedを使用）
    private final Object drawLock = new Object();
    
    // HOROS-20240407準拠: DCMView.h 609行目 - NSString *stringID;
    protected String stringID = null;
    
    // HOROS-20240407準拠: DCMView.h 201行目 - float scaleValue, startScaleValue;
    protected float scaleValue = 1.0f;
    protected float startScaleValue = 1.0f;
    
    // HOROS-20240407準拠: DCMView.h 203行目 - NSPoint origin;
    protected java.awt.Point origin = new java.awt.Point(0, 0);
    
    // HOROS-20240407準拠: DCMView.m 531行目 - @synthesize mouseXPos, mouseYPos;
    protected float mouseXPos = 0;
    protected float mouseYPos = 0;
    
    // HOROS-20240407準拠: BrowserControllerへの参照は不要
    // HOROS-20240407準拠: DCMView.m - BrowserController.currentBrowserを使用
    // browserControllerフィールドは削除し、BrowserController.currentBrowser()を使用
    
    // HOROS-20240407準拠: DCMView.h 141行目 - BOOL whiteBackground;
    protected boolean whiteBackground = false;
    
    // HOROS-20240407準拠: scaleToFitNoReentry（無限ループ防止）
    private boolean scaleToFitNoReentry = false;
    
    // HOROS-20240407準拠: DCMView.h 112行目 - annotationType
    // enum { annotNone = 0, annotGraphics, annotBase, annotFull };
    public static final int annotNone = 0;
    public static final int annotGraphics = 1;
    public static final int annotBase = 2;
    public static final int annotFull = 3;
    
    // HOROS-20240407準拠: DCMView.h 542行目 - @synthesize annotationType;
    // HOROS-20240407準拠: BrowserController.m 6490行目、12635行目
    // annotationType = [[NSUserDefaults standardUserDefaults] integerForKey:@"ANNOTATIONS"];
    private int annotationType = annotBase; // デフォルトはannotBase（定規とアノテーションを表示）
    
    /**
     * ToolMode - ツールモード
     * HOROS-20240407準拠: DCMViewのToolMode enum
     */
    public enum ToolMode {
        tPlain,      // 通常モード
        tMesure,     // 測定
        tOval,       // 楕円
        tOPolygon,   // 開いた多角形
        tCPolygon,   // 閉じた多角形
        tAngle,      // 角度
        tText,       // テキスト
        tArrow,      // 矢印
        tPencil,     // 鉛筆
        tROI,        // ROI
        t2DPoint,    // 2Dポイント
        t2DPolygon,   // 2D多角形
        t3DCut,      // 3Dカット
        t3Dpoint,     // 3Dポイント
        t3DROI,      // 3D ROI
        tRepulsor,   // リパルサー
        tBonesRemoval, // 骨除去
        t3DRotate,   // 3D回転
        tCross,      // クロス
        tUnselect,   // 選択解除
        tWLWW,       // ウィンドウレベル/ウィンドウ幅
        tNext,       // 次
        tPrevious,   // 前
        tFlip,       // フリップ
        tRefresh,    // リフレッシュ
        tRotate,     // 回転
        tZoom,       // ズーム
        tTranslate,  // 移動
        t3DpointTool, // 3Dポイントツール
        t3DCutTool,   // 3Dカットツール
        t3DROITool,   // 3D ROIツール
        t3DRotateTool, // 3D回転ツール
        tBonesRemovalTool, // 骨除去ツール
        tRepulsorTool,     // リパルサーツール
        t3DPointTool,      // 3Dポイントツール
        t3DPolygonTool,    // 3D多角形ツール
        t3DROITool2,      // 3D ROIツール2
        t3DCutTool2,      // 3Dカットツール2
        t3DRotateTool2,   // 3D回転ツール2
        tBonesRemovalTool2, // 骨除去ツール2
        tRepulsorTool2,     // リパルサーツール2
        t3DPointTool2,     // 3Dポイントツール2
        t3DPolygonTool2,   // 3D多角形ツール2
        t3DROITool3,       // 3D ROIツール3
        t3DCutTool3,       // 3Dカットツール3
        t3DRotateTool3,    // 3D回転ツール3
        tBonesRemovalTool3, // 骨除去ツール3
        tRepulsorTool3,     // リパルサーツール3
        t3DPointTool3,     // 3Dポイントツール3
        t3DPolygonTool3,   // 3D多角形ツール3
        t3DROITool4,       // 3D ROIツール4
        t3DCutTool4,       // 3Dカットツール4
        t3DRotateTool4,    // 3D回転ツール4
        tBonesRemovalTool4, // 骨除去ツール4
        tRepulsorTool4,     // リパルサーツール4
        t3DPointTool4,     // 3Dポイントツール4
        t3DPolygonTool4    // 3D多角形ツール4
    }
    
    /**
     * コンストラクタ
     * HOROS-20240407準拠: DCMView.m
     */
    public DCMView() {
        super();
        this.dcmPixList = new ArrayList<>();
        this.dcmFilesList = new ArrayList<>();
        this.dcmRoiList = new ArrayList<>();
        
        // HOROS-20240407準拠: DCMView.m 6490行目、12635行目
        // annotationType = [[NSUserDefaults standardUserDefaults] integerForKey:@"ANNOTATIONS"];
        // HOROS-20240407準拠: PreferencesからANNOTATIONSを読み込む
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DCMView.class);
        this.annotationType = prefs.getInt("ANNOTATIONS", annotBase);
        System.out.println("[DEBUG] DCMView() - annotationType loaded from Preferences: " + annotationType);
        
        // HOROS-20240407準拠: コンポーネントサイズ変更時にscaleToFitを呼び出す
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (curDCM != null && !scaleToFitNoReentry) {
                    scaleToFit();
                }
            }
        });
        
        // HOROS-20240407準拠: マウス位置を追跡するリスナーを追加
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                // HOROS-20240407準拠: DCMView.m 3769-3772行目
                // NSPoint imageLocation = [self ConvertFromNSView2GL: eventLocationInView];
                // mouseXPos = imageLocation.x;
                // mouseYPos = imageLocation.y;
                // HOROS-20240407準拠: DCMView.m 7590-7637行目 - ConvertFromNSView2GLの実装
                java.awt.Point viewPoint = new java.awt.Point(e.getX(), e.getY());
                java.awt.Point imageLocation = convertFromNSView2GL(viewPoint);
                mouseXPos = imageLocation.x;
                mouseYPos = imageLocation.y;
                repaint(); // マウス位置が変わったら再描画
            }
        });
    }
    
    /**
     * setPixels - ピクセルデータを設定
     * HOROS-20240407準拠: DCMView.m 2176-2257行目
     * - (void) setPixels: (NSMutableArray*) pixels files: (NSArray*) files rois: (NSMutableArray*) rois firstImage: (short) firstImage level: (char) level reset: (BOOL) reset
     */
    public void setPixels(List<DicomPix> pixels, List<Object> files, List<Object> rois, short firstImage, char level, boolean reset) {
        System.out.println("[DEBUG] DCMView.setPixels() called - pixels: " + (pixels != null ? pixels.size() : "null") + ", firstImage: " + firstImage + ", reset: " + reset);
        synchronized (drawLock) {
            try {
                // HOROS-20240407準拠: DCMView.m 2190行目 - self.curDCM = nil;
                this.curDCM = null;
                
                // HOROS-20240407準拠: DCMView.m 2192行目 - volumicData = -1;
                this.volumicData = -1;
                
                // HOROS-20240407準拠: DCMView.m 2197-2222行目 - dcmPixListの設定
                // HOROS-20240407準拠: dcmPixList != pixels の比較は参照比較
                // Javaでは、同じオブジェクト参照でない場合、常に新しいArrayListを作成する
                // HOROS-20240407準拠: [dcmPixList release]; dcmPixList = [pixels retain];
                // Javaでは、pixelsがnullでない場合、常に新しいArrayListを作成する
                if (this.dcmPixList != pixels) {
                    this.dcmPixList = pixels != null ? new ArrayList<>(pixels) : new ArrayList<>();
                    
                    // HOROS-20240407準拠: DCMView.m 2202行目 - volumicSeries = YES;
                    this.volumicSeries = true;
                    
                    // HOROS-20240407準拠: DCMView.m 2214-2221行目 - stringIDが"previewDatabase"でない場合のvolumicSeries判定
                    if (!"previewDatabase".equals(this.stringID)) {
                        if (this.dcmPixList.size() > 1) {
                            // HOROS-20240407準拠: sliceLocationが同じ場合はvolumicSeries = NO
                            // TODO: DicomPixにgetSliceLocation()メソッドを追加する必要がある
                            // 現時点では、volumicSeriesの判定をスキップ
                            // DicomPix first = this.dcmPixList.get(0);
                            // DicomPix last = this.dcmPixList.get(this.dcmPixList.size() - 1);
                            // if (first != null && last != null && 
                            //     first.getSliceLocation() == last.getSliceLocation()) {
                            //     this.volumicSeries = false;
                            // }
                        } else {
                            this.volumicSeries = false;
                        }
                    }
                }
                
                // HOROS-20240407準拠: DCMView.m 2224-2228行目 - dcmFilesListの設定
                if (this.dcmFilesList != files) {
                    this.dcmFilesList = files != null ? new ArrayList<>(files) : new ArrayList<>();
                }
                
                // HOROS-20240407準拠: DCMView.m 2232-2236行目 - dcmRoiListの設定
                if (this.dcmRoiList != rois) {
                    this.dcmRoiList = rois != null ? new ArrayList<>(rois) : new ArrayList<>();
                }
                
                // HOROS-20240407準拠: DCMView.m 2238行目 - listType = level;
                this.listType = level;
                
                // HOROS-20240407準拠: DCMView.m 2240-2247行目 - resetがtrueの場合はsetIndexWithResetを呼び出す
                if (this.dcmPixList != null && !this.dcmPixList.isEmpty()) {
                    if (reset) {
                        setIndexWithReset(firstImage, true);
                        // HOROS-20240407準拠: DCMView.m 2245行目 - [self updatePresentationStateFromSeries];
                        // TODO: updatePresentationStateFromSeriesの実装
                    }
                }
                
                // HOROS-20240407準拠: DCMView.m 2249行目 - [self setNeedsDisplay:true];
                System.out.println("[DEBUG] DCMView.setPixels() - dcmPixList.size(): " + (this.dcmPixList != null ? this.dcmPixList.size() : "null") + ", curDCM: " + (this.curDCM != null ? "not null" : "null"));
                repaint();
            } catch (Exception e) {
                System.out.println("[DEBUG] DCMView.setPixels() - exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * setIndexWithReset - インデックスをリセット付きで設定
     * HOROS-20240407準拠: DCMView.m 2095行目
     */
    protected void setIndexWithReset(short index, boolean sizeToFit) {
        // HOROS-20240407準拠: 基本的な実装
        this.curImage = index;
        this.startImage = index;
        if (this.dcmPixList != null && index >= 0 && index < this.dcmPixList.size()) {
            this.curDCM = this.dcmPixList.get(index);
            
            // HOROS-20240407準拠: DCMView.m 2095行目 - sizeToFitがtrueの場合はscaleToFitを呼び出す
            if (sizeToFit) {
                scaleToFit();
            }
        }
        repaint();
    }
    
    /**
     * setIndex - インデックスを設定
     * HOROS-20240407準拠: DCMView.m 2536-2580行目
     */
    public void setIndex(int index) {
        System.out.println("[DEBUG] DCMView.setIndex() called - index: " + index + ", dcmPixList: " + (this.dcmPixList != null ? this.dcmPixList.size() : "null"));
        synchronized (drawLock) {
            try {
                // HOROS-20240407準拠: DCMView.m 2550行目
                // if( dcmPixList && index > -1 && [dcmPixList count] > 0)
                if (this.dcmPixList != null && index > -1 && this.dcmPixList.size() > 0) {
                    // HOROS-20240407準拠: DCMView.m 2557-2559行目
                    // curImage = index;
                    // if( curImage >= [dcmPixList count]) curImage = (long)[dcmPixList count] -1;
                    // if( curImage < 0) curImage = 0;
                    this.curImage = (short) index;
                    if (this.curImage >= this.dcmPixList.size()) {
                        this.curImage = (short) (this.dcmPixList.size() - 1);
                    }
                    if (this.curImage < 0) {
                        this.curImage = 0;
                    }
                    
                    // HOROS-20240407準拠: DCMView.m 2562行目
                    // self.curDCM = [dcmPixList objectAtIndex:curImage];
                    if (this.curImage >= 0 && this.curImage < this.dcmPixList.size()) {
                        DicomPix oldCurDCM = this.curDCM;
                        this.curDCM = this.dcmPixList.get(this.curImage);
                        System.out.println("[DEBUG] DCMView.setIndex() - curDCM: " + (this.curDCM != null ? "not null" : "null") + ", curImage: " + this.curImage + ", oldCurDCM: " + (oldCurDCM != null ? "not null" : "null"));
                        
                        // HOROS-20240407準拠: DCMView.m 2564行目
                        // [self.curDCM CheckLoad];
                        // DicomPixの画像が読み込まれていることを確認
                        if (this.curDCM != null) {
                            // CheckLoadは画像の読み込みを確認するメソッド
                            // Javaでは、getBufferedImage()を呼び出すことで画像が読み込まれる
                            // ただし、既に読み込まれている場合は何もしない
                            java.awt.image.BufferedImage img = this.curDCM.getBufferedImage();
                            System.out.println("[DEBUG] DCMView.setIndex() - getBufferedImage() returned: " + (img != null ? "not null (" + img.getWidth() + "x" + img.getHeight() + ")" : "null"));
                            
                            // HOROS-20240407準拠: curDCMが変更された場合、画像サイズが変わった場合はscaleToFitを呼び出す
                            // ただし、setIndexWithResetで既に呼び出されている場合は不要
                            // 画像サイズが変わった場合のみscaleToFitを呼び出す
                            if (oldCurDCM != null && oldCurDCM != this.curDCM) {
                                int oldWidth = oldCurDCM.getPwidth();
                                int oldHeight = oldCurDCM.getPheight();
                                int newWidth = this.curDCM.getPwidth();
                                int newHeight = this.curDCM.getPheight();
                                if (oldWidth != newWidth || oldHeight != newHeight) {
                                    System.out.println("[DEBUG] DCMView.setIndex() - image size changed from " + oldWidth + "x" + oldHeight + " to " + newWidth + "x" + newHeight + ", calling scaleToFit()");
                                    if (!scaleToFitNoReentry) {
                                        scaleToFit();
                                    }
                                }
                            }
                        } else {
                            System.out.println("[DEBUG] DCMView.setIndex() - curDCM is null at index " + this.curImage);
                        }
                        
                        // HOROS-20240407準拠: DCMView.m 2580行目
                        // [self setNeedsDisplay:true];
                        // HOROS-20240407準拠: setNeedsDisplayは再描画を要求する
                        // Java Swingでは、repaint()を呼び出す
                        System.out.println("[DEBUG] DCMView.setIndex() - calling repaint()");
                        repaint();
                    } else {
                        System.out.println("[DEBUG] DCMView.setIndex() - curImage out of range: " + this.curImage + ", dcmPixList.size(): " + this.dcmPixList.size());
                    }
                } else {
                    System.out.println("[DEBUG] DCMView.setIndex() - dcmPixList is null or index < 0 or dcmPixList is empty: dcmPixList=" + (this.dcmPixList != null ? this.dcmPixList.size() : "null") + ", index=" + index);
                }
            } catch (Exception e) {
                System.out.println("[DEBUG] DCMView.setIndex() - exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * setStringID - 文字列IDを設定
     * HOROS-20240407準拠: DCMView.h
     */
    public void setStringID(String stringID) {
        this.stringID = stringID;
    }
    
    /**
     * getStringID - 文字列IDを取得
     */
    public String getStringID() {
        return this.stringID;
    }
    
    /**
     * is2DViewer - 2Dビューワーかどうか
     * HOROS-20240407準拠: DCMView.h
     */
    public boolean is2DViewer() {
        return false; // デフォルトはfalse（PreviewViewでオーバーライド）
    }
    
    /**
     * getCurDCM - 現在のDCMPixを取得
     * HOROS-20240407準拠: DCMView.h
     */
    public DicomPix getCurDCM() {
        return this.curDCM;
    }
    
    /**
     * scaleToFitForDCMPix - 画像をフィットさせるスケール値を計算
     * HOROS-20240407準拠: DCMView.m 2056-2073行目
     */
    protected float scaleToFitForDCMPix(DicomPix d) {
        if (d == null) {
            return 1.0f;
        }
        
        java.awt.Rectangle bounds = getBounds();
        int viewWidth = bounds.width > 0 ? bounds.width : getWidth();
        int viewHeight = bounds.height > 0 ? bounds.height : getHeight();
        
        if (viewWidth <= 0 || viewHeight <= 0) {
            return 1.0f;
        }
        
        java.awt.image.BufferedImage image = d.getBufferedImage();
        if (image == null) {
            return 1.0f;
        }
        
        int w = image.getWidth();
        int h = image.getHeight();
        
        // TODO: shutterEnabledとshutterRectの処理（現時点ではスキップ）
        
        // HOROS-20240407準拠: DCMView.m 2069-2072行目
        // pixelRatioは現時点では1.0として扱う
        float pixelRatio = 1.0f;
        
        float scaleX = (float) viewWidth / w;
        float scaleY = (float) viewHeight / h / pixelRatio;
        
        return Math.min(scaleX, scaleY);
    }
    
    /**
     * ConvertFromNSView2GL - ビュー座標を画像座標系に変換
     * HOROS-20240407準拠: DCMView.m 7590-7637行目
     */
    private java.awt.Point convertFromNSView2GL(java.awt.Point viewPoint) {
        // HOROS-20240407準拠: DCMView.m 7592行目 - convertPointToBacking（retina対応）
        // Java Swingではバッキングスケールは1.0として扱う
        float aX = viewPoint.x;
        float aY = viewPoint.y;
        
        // HOROS-20240407準拠: DCMView.m 7595行目 - inverse Y scaling system
        int panelHeight = getHeight();
        aY = panelHeight - aY;
        
        // HOROS-20240407準拠: DCMView.m 7608-7636行目 - ConvertFromUpLeftView2GL
        int panelWidth = getWidth();
        int panelHeight2 = getHeight();
        
        // HOROS-20240407準拠: DCMView.m 7612-7613行目 - xFlipped, yFlipped（現時点では未実装のためスキップ）
        // if( xFlipped) a.x = size.size.width - a.x;
        // if( yFlipped) a.y = size.size.height - a.y;
        
        // HOROS-20240407準拠: DCMView.m 7615-7619行目
        aX -= panelWidth / 2.0f;
        aX /= scaleValue;
        
        aY -= panelHeight2 / 2.0f;
        aY /= scaleValue;
        
        // HOROS-20240407準拠: DCMView.m 7621-7625行目 - rotation（現時点では未実装のためスキップ）
        // float xx = a.x*cos(rotation*deg2rad) + a.y*sin(rotation*deg2rad);
        // float yy = -a.x*sin(rotation*deg2rad) + a.y*cos(rotation*deg2rad);
        // a.y = yy;
        // a.x = xx;
        
        // HOROS-20240407準拠: DCMView.m 7627-7628行目
        aX -= origin.x / scaleValue;
        aY += origin.y / scaleValue;
        
        // HOROS-20240407準拠: DCMView.m 7630-7635行目
        if (curDCM != null) {
            aX += curDCM.getPwidth() * 0.5f;
            aY += curDCM.getPheight() * curDCM.getPixelRatio() * 0.5f;
            aY /= curDCM.getPixelRatio();
        }
        
        return new java.awt.Point((int)aX, (int)aY);
    }
    
    /**
     * scaleToFit - 画像をフィットさせる
     * HOROS-20240407準拠: DCMView.m 2075-2093行目
     */
    public void scaleToFit() {
        if (scaleToFitNoReentry || curDCM == null) {
            return;
        }
        
        scaleToFitNoReentry = true;
        
        try {
            this.scaleValue = scaleToFitForDCMPix(this.curDCM);
            
            // HOROS-20240407準拠: DCMView.m 2088行目 - origin.x = origin.y = 0;
            // TODO: shutterEnabledの処理（現時点ではスキップ）
            this.origin.x = 0;
            this.origin.y = 0;
            
            repaint();
        } finally {
            scaleToFitNoReentry = false;
        }
    }
    
    /**
     * paintComponent - 画像を描画
     * HOROS-20240407準拠: DCMView.m（OpenGL描画の代わりにJava Swingで実装）
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // HOROS-20240407準拠: 初期起動時はプレビュー画面を真っ黒にする
        // clearPreviewメソッド（9424行目）でsetPixels:nilが呼ばれる
        // BrowserController.m 9424行目: [imageView setPixels:nil files:nil rois:nil firstImage:0 level:0 reset:YES];
        if (curDCM == null || dcmPixList == null || dcmPixList.isEmpty()) {
            // HOROS-20240407準拠: DCMView.m 8997-9000行目
            // 画像がない場合は背景を描画（whiteBackgroundがtrueの場合は白、falseの場合は黒）
            if (whiteBackground) {
                g.setColor(java.awt.Color.WHITE);
            } else {
                g.setColor(java.awt.Color.BLACK);
            }
            g.fillRect(0, 0, getWidth(), getHeight());
            return;
        }
        
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            // アンチエイリアスを有効化
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // HOROS-20240407準拠: curDCMから画像を取得して描画
            java.awt.image.BufferedImage image = curDCM.getBufferedImage();
            if (image != null) {
                int panelWidth = getWidth();
                int panelHeight = getHeight();
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();
                
                if (imageWidth > 0 && imageHeight > 0) {
                    // HOROS-20240407準拠: scaleValueとoriginを使用して描画
                    int scaledWidth = (int) (imageWidth * scaleValue);
                    int scaledHeight = (int) (imageHeight * scaleValue);
                    
                    // originを考慮した描画位置
                    int x = (panelWidth - scaledWidth) / 2 + origin.x;
                    int y = (panelHeight - scaledHeight) / 2 + origin.y;
                    
                    // HOROS-20240407準拠: DCMView.m 8997-9000行目
                    // 背景色を設定（whiteBackgroundがtrueの場合は白、falseの場合は黒）
                    if (whiteBackground) {
                        g2d.setColor(java.awt.Color.WHITE);
                    } else {
                        g2d.setColor(java.awt.Color.BLACK);
                    }
                    g2d.fillRect(0, 0, panelWidth, panelHeight);
                    
                    // 画像を描画
                    g2d.drawImage(image, x, y, scaledWidth, scaledHeight, null);
                    
                    // HOROS-20240407準拠: DCMView.m 9273-9691行目 - アノテーションと定規の描画
                    // HOROS-20240407準拠: DCMView.m 9273行目 - if (annotations != annotNone)
                    if (annotationType != annotNone) {
                        // HOROS-20240407準拠: DCMView.m 9622-9689行目 - 定規（Ruler）の描画
                        // HOROS-20240407準拠: DCMView.m 9622行目 - if( annotations >= annotBase)
                        if (annotationType >= annotBase) {
                            drawRuler(g2d, panelWidth, panelHeight, x, y, scaledWidth, scaledHeight);
                        }
                        
                        // HOROS-20240407準拠: DCMView.m 9695行目 - アノテーションテキストの描画
                        // HOROS-20240407準拠: DCMView.m 9695行目 - [self drawTextualData: drawingFrameRect :annotations];
                        // annotations != annotNoneの場合にdrawTextualDataを呼ぶ（drawTextualData内でannotations > annotGraphicsをチェック）
                        drawTextualData(g2d, panelWidth, panelHeight);
                    }
                }
            } else {
                // HOROS-20240407準拠: 画像が読み込まれていない場合は背景を描画
                // DCMView.m 8997-9000行目: whiteBackgroundに応じて背景色を設定
                if (whiteBackground) {
                    g2d.setColor(java.awt.Color.WHITE);
                } else {
                    g2d.setColor(java.awt.Color.BLACK);
                }
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        } finally {
            g2d.dispose();
        }
    }
    
    /**
     * 定規（Ruler）を描画
     * HOROS-20240407準拠: DCMView.m 9624-9689行目
     */
    private void drawRuler(Graphics2D g2d, int panelWidth, int panelHeight, int imageX, int imageY, int scaledWidth, int scaledHeight) {
        if (curDCM == null) {
            System.out.println("[DEBUG] drawRuler() - curDCM is null");
            return;
        }
        
        double pixelSpacingX = curDCM.getPixelSpacingX();
        double pixelSpacingY = curDCM.getPixelSpacingY();
        double pixelRatio = curDCM.getPixelRatio();
        
        System.out.println("[DEBUG] drawRuler() - pixelSpacingX: " + pixelSpacingX + ", pixelSpacingY: " + pixelSpacingY + ", pixelRatio: " + pixelRatio);
        
        // HOROS-20240407準拠: DCMView.m 9646行目 - if( self.curDCM.pixelSpacingX != 0 && ...)
        // HOROS-20240407準拠: DCMView.m 9667行目 - else if( self.curDCM.pixelSpacingX != 0 && self.curDCM.pixelSpacingY != 0)
        // pixelSpacingX != 0 の場合のみ定規を表示
        // US画像でもpixelSpacingXが設定されている場合は表示される
        if (pixelSpacingX == 0.0) {
            System.out.println("[DEBUG] drawRuler() - pixelSpacingX is 0, skipping ruler drawing");
            return;
        }
        
        // HOROS-20240407準拠: DCMView.m 9625-9626行目
        float yOffset = 24.0f;
        float xOffset = 32.0f;
        
        g2d.setColor(java.awt.Color.GREEN);
        g2d.setStroke(new java.awt.BasicStroke(1.0f));
        
        // HOROS-20240407準拠: DCMView.m 9630-9645行目 - rr.originの設定
        // rr.originはdrawingFrameRectの原点（0, 0）を使用
        // HOROS-20240407準拠: DCMView.m 9644行目 - rr.origin = NSMakePoint( 0, 0);
        // HOROS-20240407準拠: DCMView.m 9648行目 - rr.origin.y + rr.size.height/2 - yOffset
        // パネルの中央下部を基準に描画
        // HOROS-20240407準拠: DCMView.m 9651行目 - rr.origin.x + -rr.size.width/2 + xOffset
        // パネルの左端からxOffsetだけ右にずらした位置
        
        // パネルの中央下部を基準に定規を描画（画面内に収めるため）
        float rulerY = panelHeight / 2.0f - yOffset; // HOROS-20240407準拠: rr.origin.y + rr.size.height/2 - yOffset
        float rulerX = -panelWidth / 2.0f + xOffset; // HOROS-20240407準拠: rr.origin.x + -rr.size.width/2 + xOffset
        // ただし、実際の描画ではパネルの座標系を使用するため、変換が必要
        // パネルの中央を原点とした座標系から、実際の描画座標系に変換
        float centerX = panelWidth / 2.0f;
        float centerY = panelHeight / 2.0f;
        float actualRulerX = centerX + rulerX; // パネルの中央 + オフセット
        float actualRulerY = centerY + rulerY; // パネルの中央 + オフセット
        
        // HOROS-20240407準拠: DCMView.m 9646-9666行目 - ピクセルスペーシングが小さい場合（< 1mm）
        if (pixelSpacingX != 0 && pixelSpacingX * 1000.0 < 1) {
            // 水平方向の定規（パネルの中央下部）
            // HOROS-20240407準拠: DCMView.m 9648-9649行目
            // メインライン（0.04m = 40mm）
            float lineLength = (float) (scaleValue * 0.02 / pixelSpacingX);
            // 画面内に収まるように制限
            float maxLineLength = Math.min(lineLength, panelWidth / 2.0f - xOffset);
            g2d.drawLine((int)(centerX - maxLineLength), (int)actualRulerY, (int)(centerX + maxLineLength), (int)actualRulerY);
            
            // 目盛り
            // HOROS-20240407準拠: DCMView.m 9654-9661行目
            for (short i = -20; i <= 20; i++) {
                int length = (i % 10 == 0) ? 10 : 5;
                float tickX = (float)(centerX + i * scaleValue * 0.001 / pixelSpacingX);
                // 画面内に収まる場合のみ描画
                if (tickX >= 0 && tickX <= panelWidth) {
                    g2d.drawLine((int)tickX, (int)actualRulerY, (int)tickX, (int)(actualRulerY - length));
                }
            }
            
            // 垂直方向の定規（パネルの左側中央）
            // HOROS-20240407準拠: DCMView.m 9651-9652行目
            lineLength = (float) (scaleValue * 0.02 / pixelSpacingY * pixelRatio);
            // 画面内に収まるように制限
            float maxLineLengthY = Math.min(lineLength, panelHeight / 2.0f - yOffset);
            g2d.drawLine((int)actualRulerX, (int)(centerY - maxLineLengthY), (int)actualRulerX, (int)(centerY + maxLineLengthY));
            
            // 目盛り
            // HOROS-20240407準拠: DCMView.m 9663-9664行目
            for (short i = -20; i <= 20; i++) {
                int length = (i % 10 == 0) ? 10 : 5;
                float tickY = (float)(centerY + i * scaleValue * 0.001 / pixelSpacingY * pixelRatio);
                // 画面内に収まる場合のみ描画
                if (tickY >= 0 && tickY <= panelHeight) {
                    g2d.drawLine((int)actualRulerX, (int)tickY, (int)(actualRulerX + length), (int)tickY);
                }
            }
        }
        // HOROS-20240407準拠: DCMView.m 9667-9687行目 - ピクセルスペーシングが大きい場合（>= 1mm）
        else if (pixelSpacingX != 0 && pixelSpacingY != 0) {
            // 水平方向の定規（パネルの中央下部）
            // HOROS-20240407準拠: DCMView.m 9669-9670行目
            // メインライン（50mm）
            float lineLength = (float) (scaleValue * 50.0 / pixelSpacingX);
            // 画面内に収まるように制限
            float maxLineLength = Math.min(lineLength, panelWidth / 2.0f - xOffset);
            g2d.drawLine((int)(centerX - maxLineLength), (int)actualRulerY, (int)(centerX + maxLineLength), (int)actualRulerY);
            
            // 目盛り（10mm間隔）
            // HOROS-20240407準拠: DCMView.m 9675-9682行目
            for (short i = -5; i <= 5; i++) {
                int length = (i % 5 == 0) ? 10 : 5;
                float tickX = (float)(centerX + i * scaleValue * 10.0 / pixelSpacingX);
                // 画面内に収まる場合のみ描画
                if (tickX >= 0 && tickX <= panelWidth) {
                    g2d.drawLine((int)tickX, (int)actualRulerY, (int)tickX, (int)(actualRulerY - length));
                }
            }
            
            // 垂直方向の定規（パネルの左側中央）
            // HOROS-20240407準拠: DCMView.m 9672-9673行目
            lineLength = (float) (scaleValue * 50.0 / pixelSpacingY * pixelRatio);
            // 画面内に収まるように制限
            float maxLineLengthY = Math.min(lineLength, panelHeight / 2.0f - yOffset);
            g2d.drawLine((int)actualRulerX, (int)(centerY - maxLineLengthY), (int)actualRulerX, (int)(centerY + maxLineLengthY));
            
            // 目盛り（10mm間隔）
            // HOROS-20240407準拠: DCMView.m 9684-9685行目
            for (short i = -5; i <= 5; i++) {
                int length = (i % 5 == 0) ? 10 : 5;
                float tickY = (float)(centerY + i * scaleValue * 10.0 / pixelSpacingY * pixelRatio);
                // 画面内に収まる場合のみ描画
                if (tickY >= 0 && tickY <= panelHeight) {
                    g2d.drawLine((int)actualRulerX, (int)tickY, (int)(actualRulerX + length), (int)tickY);
                }
            }
        }
    }
    
    /**
     * アノテーションテキストを描画
     * HOROS-20240407準拠: DCMView.m 8101-8648行目
     * HOROS-20240407準拠: AnnotationsDefault.plist - デフォルトのアノテーション項目を配置
     */
    private void drawTextualData(Graphics2D g2d, int panelWidth, int panelHeight) {
        if (curDCM == null) {
            System.out.println("[DEBUG] drawTextualData() - curDCM is null");
            return;
        }
        
        System.out.println("[DEBUG] drawTextualData() - annotationType: " + annotationType + ", annotGraphics: " + annotGraphics + ", annotBase: " + annotBase);
        
        // HOROS-20240407準拠: DCMView.m 8157行目 - else if( annotations > annotGraphics)
        // annotGraphicsより大きい場合のみテキスト情報を表示
        // annotationType == annotBase (2) は annotGraphics (1) より大きいので、表示される
        if (annotationType <= annotGraphics) {
            System.out.println("[DEBUG] drawTextualData() - annotationType (" + annotationType + ") <= annotGraphics (" + annotGraphics + "), skipping text data");
            return; // annotGraphics以下の場合はテキスト情報を表示しない
        }
        
        // HOROS-20240407準拠: DCMView.m 8332行目 - stringIDが設定されている場合は一部のアノテーションを表示しない
        // プレビュー画面（previewDatabase）では最小限の情報のみ表示
        boolean isPreviewDatabase = "previewDatabase".equals(stringID);
        System.out.println("[DEBUG] drawTextualData() - isPreviewDatabase: " + isPreviewDatabase + ", stringID: " + stringID);
        
        // HOROS-20240407準拠: BrowserController.m 521行目 - + (BrowserController*) currentBrowser { return browserWindow; }
        // HOROS-20240407準拠: DCMView.m - BrowserController.currentBrowserを使用
        BrowserController browserController = BrowserController.currentBrowser();
        
        // HOROS-20240407準拠: フォント設定
        java.awt.Font font = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12);
        g2d.setFont(font);
        g2d.setColor(java.awt.Color.WHITE);
        
        java.awt.FontMetrics fm = g2d.getFontMetrics();
        int lineHeight = fm.getHeight();
        int margin = 6; // HOROS-20240407準拠: DCMView.m 8203行目 - size.origin.x + 6*sf
        
        // HOROS-20240407準拠: DCMView.m 8200行目 - annotationsDictionaryを使用して項目をチェック
        // HOROS-20240407準拠: DCMView.m 8652行目 - fullText: YES で呼ばれる
        // HOROS-20240407準拠: AnnotationsDefault.plist - デフォルトのアノテーション項目
        // TopLeft: Image Size, View Size, Window Level / Window Width, Mouse Position (px), Mouse Position (mm)
        // TopRight: Patient Name, Patient ID, Age, Study Description, Series Description, Series ID
        // LowerLeft: Image Position, Thickness / Location / Position, Zoom & Angle, Compression
        // TopMiddle, MiddleLeft, MiddleRight, LowerMiddle: Orientation
        if (annotationType >= annotBase) {
            // ========== TopLeft ==========
            // HOROS-20240407準拠: DCMView.m 8203行目 - TopLeft: size.origin.x + 6*sf
            // HOROS-20240407準拠: DCMView.m 8223行目 - TopLeft: size.origin.y + _stringSize.height+2*sf
            int topLeftY = lineHeight + 2;
            // HOROS-20240407準拠: DCMView.m 8332行目 - stringID.length == 0 || [stringID isEqualToString: @"previewDatabase"]の条件で表示
            // previewDatabaseでも一部のアノテーションを表示
            if (true) { // 常に表示（HOROS-20240407準拠: DCMView.m 8332行目）
                // Image Size
                // HOROS-20240407準拠: DCMView.m 8322-8324行目
                g2d.drawString(String.format("Image size: %d x %d", curDCM.getPwidth(), curDCM.getPheight()), margin, topLeftY);
                topLeftY += lineHeight;
                
                // View Size
                // HOROS-20240407準拠: DCMView.m 8326-8328行目
                g2d.drawString(String.format("View size: %d x %d", panelWidth, panelHeight), margin, topLeftY);
                topLeftY += lineHeight;
            }
            
            // Window Level / Window Width
            // HOROS-20240407準拠: DCMView.m 8487-8511行目
            // 注: HOROS-20240407では "WW: %d WL: %d" の順序で表示される
            float wl = curDCM.getWindowLevel();
            float ww = curDCM.getWindowWidth();
            int iwl = (int)wl;
            int iww = (int)ww;
            String wlwwStr;
            if (ww < 50 && (wl != iwl || ww != iww)) {
                wlwwStr = String.format("WW: %.4f WL: %.4f", ww, wl);
            } else {
                wlwwStr = String.format("WW: %d WL: %d", iww, iwl);
            }
            g2d.drawString(wlwwStr, margin, topLeftY);
            topLeftY += lineHeight;
            
            // Mouse Position (px)
            // HOROS-20240407準拠: DCMView.m 8330-8371行目
            // HOROS-20240407準拠: AnnotationsDefault.plist - TopLeft: Mouse Position (px)
            // HOROS-20240407準拠: DCMView.m 8332行目 - stringID.length == 0 || [stringID isEqualToString: @"previewDatabase"]
            // HOROS-20240407準拠: DCMView.m 8341-8342行目 - "X: %d px Y: %d px Value: %2.2f %@"
            if (stringID == null || "previewDatabase".equals(stringID)) {
                // HOROS-20240407準拠: DCMView.m 8334行目 - 条件チェックをコメントアウト（常に表示）
                // ピクセル値の取得
                float pixelValue = 0.0f;
                String pixelUnit = "";
                if (curDCM != null && mouseXPos >= 0 && mouseYPos >= 0) {
                    try {
                        // HOROS-20240407準拠: DCMView.m 3805行目 - pixelMouseValue = [self.curDCM getPixelValueX: xPos Y:yPos];
                        int xPos = (int)mouseXPos;
                        int yPos = (int)mouseYPos;
                        if (xPos >= 0 && xPos < curDCM.getPwidth() && yPos >= 0 && yPos < curDCM.getPheight()) {
                            pixelValue = curDCM.getPixelValueX(xPos, yPos);
                            // HOROS-20240407準拠: DCMView.m 8338-8339行目 - SUVConvertedの場合は"SUV"を表示
                            // TODO: SUVConvertedの判定を実装
                        }
                    } catch (Exception e) {
                        pixelValue = 0.0f;
                    }
                }
                // HOROS-20240407準拠: DCMView.m 8342行目 - "X: %d px Y: %d px Value: %2.2f %@"
                String mousePosStr = String.format("X: %d px Y: %d px Value: %.2f %s", (int)mouseXPos, (int)mouseYPos, pixelValue, pixelUnit);
                g2d.drawString(mousePosStr, margin, topLeftY);
                topLeftY += lineHeight;
            }
            
            // Mouse Position (mm) - HOROS-20240407準拠: DCMView.m 8447-8486行目
            // HOROS-20240407準拠: DCMView.m 8449行目 - stringID == nil の時のみ表示（previewDatabaseでは表示されない）
            // previewDatabaseでは表示しないため、このセクションを削除
            
            // ========== TopRight ==========
            // HOROS-20240407準拠: DCMView.m 8206行目 - TopRight: size.origin.x + size.size.width-2*sf
            // HOROS-20240407準拠: DCMView.m 8225行目 - TopRight: size.origin.y + _stringSize.height+2*sf
            // HOROS-20240407準拠: DCMView.m 8216行目 - TopRight: DCMViewTextAlignRight
            // HOROS-20240407準拠: AnnotationsDefault.plist - TopRight: Patient Name, Patient ID, Age, Study Description, Series Description, Series ID
            int topRightY = lineHeight + 2;
            int topRightX = panelWidth - margin; // 右端からmarginだけ左に
            
            // HOROS-20240407準拠: DCMView.m 8578行目 - [[dcmFilesList objectAtIndex: 0] valueForKeyPath:@"series.study.name"]
            // HOROS-20240407準拠: dcmFilesListから直接Study/Series情報を取得
            // BrowserController.getSelectedItem()ではなく、dcmFilesList[curImage]から取得
            if (dcmFilesList != null && !dcmFilesList.isEmpty() && curImage >= 0 && curImage < dcmFilesList.size()) {
                Object currentFile = dcmFilesList.get(curImage);
                com.jj.dicomviewer.model.DicomStudy study = null;
                com.jj.dicomviewer.model.DicomSeries series = null;
                
                // HOROS-20240407準拠: valueForKeyPath:@"series.study.name"のように取得
                if (currentFile instanceof com.jj.dicomviewer.model.DicomSeries) {
                    series = (com.jj.dicomviewer.model.DicomSeries) currentFile;
                    study = series.getStudy(); // SeriesからStudy情報を取得
                } else if (currentFile instanceof com.jj.dicomviewer.model.DicomImage) {
                    // DicomImageの場合は、Seriesを経由してStudyを取得
                    // TODO: DicomImageからSeriesを取得する方法を実装
                    // 現時点では、BrowserControllerから取得
                    if (browserController != null) {
                        Object selectedItem = browserController.getSelectedItem();
                        if (selectedItem instanceof com.jj.dicomviewer.model.DicomSeries) {
                            series = (com.jj.dicomviewer.model.DicomSeries) selectedItem;
                            study = series.getStudy();
                        } else if (selectedItem instanceof com.jj.dicomviewer.model.DicomStudy) {
                            study = (com.jj.dicomviewer.model.DicomStudy) selectedItem;
                        }
                    }
                }
                
                if (study != null) {
                    
                    // Patient Name, Patient ID
                    String patientName = study.getName();
                    String patientID = study.getPatientID();
                    if (patientName != null && !patientName.isEmpty()) {
                        String patientInfo = patientName;
                        if (patientID != null && !patientID.isEmpty()) {
                            patientInfo += " " + patientID;
                        }
                        // 年齢の計算
                        java.time.LocalDate dateOfBirth = study.getDateOfBirth();
                        if (dateOfBirth != null) {
                            String age = com.jj.dicomviewer.model.DicomStudy.yearOldFromDateOfBirth(dateOfBirth);
                            if (!age.isEmpty()) {
                                patientInfo += " (" + age + ")";
                            }
                        }
                        // 右揃えで描画
                        int stringWidth = fm.stringWidth(patientInfo);
                        g2d.drawString(patientInfo, topRightX - stringWidth, topRightY);
                        topRightY += lineHeight;
                    }
                    
                    // Study Description
                    String studyName = study.getStudyName();
                    if (studyName != null && !studyName.isEmpty()) {
                        int stringWidth = fm.stringWidth(studyName);
                        g2d.drawString(studyName, topRightX - stringWidth, topRightY);
                        topRightY += lineHeight;
                    }
                    
                    // Series Description, Series ID
                    // HOROS-20240407準拠: 既にseriesが設定されている場合はそのまま使用
                    
                    if (series != null) {
                        String seriesDescription = series.getSeriesDescription();
                        if (seriesDescription != null && !seriesDescription.isEmpty()) {
                            int stringWidth = fm.stringWidth(seriesDescription);
                            g2d.drawString(seriesDescription, topRightX - stringWidth, topRightY);
                            topRightY += lineHeight;
                        }
                        Integer seriesID = series.getId();
                        if (seriesID != null) {
                            String seriesIDStr = "Series ID: " + seriesID;
                            int stringWidth = fm.stringWidth(seriesIDStr);
                            g2d.drawString(seriesIDStr, topRightX - stringWidth, topRightY);
                            topRightY += lineHeight;
                        }
                    }
                }
            }
            
            // ========== LowerLeft ==========
            // HOROS-20240407準拠: DCMView.m 8205行目 - LowerLeft: size.origin.x + 6*sf
            // HOROS-20240407準拠: DCMView.m 8228行目 - LowerLeft: size.origin.y + size.size.height-2*sf
            // HOROS-20240407準拠: DCMView.m 8238行目 - LowerLeft: increment = -_stringSize.height
            // HOROS-20240407準拠: AnnotationsDefault.plist - LowerLeft: Zoom & Angle, Image Position, Compression, Thickness / Location / Position
            // HOROS-20240407準拠: DCMView.m 8332行目 - stringID.length == 0 || [stringID isEqualToString: @"previewDatabase"]の条件で表示
            int lowerLeftY = panelHeight - 2;
            // HOROS-20240407準拠: previewDatabaseでも一部のアノテーションを表示
            // HOROS-20240407準拠: DCMView.m 8332行目 - stringID.length == 0 || [stringID isEqualToString: @"previewDatabase"]の条件で表示
            if (true) { // 常に表示（HOROS-20240407準拠: DCMView.m 8332行目）
                // HOROS-20240407準拠: AnnotationsDefault.plist - LowerLeftの順序
                // 1. Zoom & Angle (fullText時のみ)
                // 2. Image Position (fullText時のみ)
                // 3. Compression
                // 4. Thickness / Location / Position (fullText時のみ)
                
                // Zoom & Angle
                // HOROS-20240407準拠: DCMView.m 8372-8379行目
                // fullText時のみ表示（HOROS-20240407ではfullText: YESで呼ばれる）
                String zoomAngleStr = "";
                // HOROS-20240407準拠: DCMView.m 8374行目 - displayedScaleValue
                float displayedScale = scaleValue * 100.0f;
                zoomAngleStr = String.format("Zoom: %.0f%%", displayedScale);
                // HOROS-20240407準拠: DCMView.m 8378行目 - displayedRotation
                // TODO: rotation変数を実装
                float rotation = 0.0f; // 簡易版: rotation変数が未実装のため、0度を表示
                if (rotation != 0.0f) {
                    zoomAngleStr += String.format(" Angle: %.0f", rotation);
                }
                if (!zoomAngleStr.isEmpty()) {
                    g2d.drawString(zoomAngleStr, margin, lowerLeftY);
                    lowerLeftY -= lineHeight;
                }
                
                // Image Position
                // HOROS-20240407準拠: DCMView.m 8441-8445行目
                // fullText時のみ表示（HOROS-20240407ではfullText: YESで呼ばれる）
                if (dcmPixList != null && !dcmPixList.isEmpty()) {
                    int totalImages = dcmPixList.size();
                    int currentImage = curImage + 1; // HOROS-20240407準拠: 1-based index
                    String imagePosStr = String.format("Im: %d/%d", currentImage, totalImages);
                    g2d.drawString(imagePosStr, margin, lowerLeftY);
                    lowerLeftY -= lineHeight;
                }
                
                // Compression
                // HOROS-20240407準拠: AnnotationsDefault.plist - LowerLeft: Compression (TransferSyntaxUID)
                // HOROS-20240407準拠: DCMView.m 10857行目 - [BrowserController compressionString: value]
                String compressionStr = getCompressionString(curDCM);
                if (compressionStr != null && !compressionStr.isEmpty() && !compressionStr.equals("-")) {
                    g2d.drawString(compressionStr, margin, lowerLeftY);
                    lowerLeftY -= lineHeight;
                }
                
                // Thickness / Location / Position
                // HOROS-20240407準拠: DCMView.m 8533-8560行目
                // fullText時のみ表示（HOROS-20240407ではfullText: YESで呼ばれる）
                double sliceThickness = curDCM.getSliceThickness();
                double sliceLocation = curDCM.getSliceLocation();
                if (sliceThickness != 0 && sliceLocation != 0) {
                    String thicknessStr = String.format("Thickness: %.2f mm Location: %.2f mm", sliceThickness, sliceLocation);
                    g2d.drawString(thicknessStr, margin, lowerLeftY);
                    lowerLeftY -= lineHeight;
                }
            }
            
            // ========== LowerRight ==========
            // HOROS-20240407準拠: DCMView.m 8207行目 - LowerRight: size.origin.x + size.size.width-2*sf
            // HOROS-20240407準拠: DCMView.m 8229行目 - LowerRight: size.origin.y + size.size.height-2*sf-_stringSize.height
            // HOROS-20240407準拠: AnnotationsDefault.plist - LowerRight: Image Acquisition Date (DB_image.date)
            int lowerRightY = panelHeight - 2 - lineHeight; // HOROS-20240407準拠: _stringSize.heightを引く
            int lowerRightX = panelWidth - margin; // 右端からmarginだけ左に
            
            // browserControllerはメソッドの最初で宣言済み
            System.out.println("[DEBUG] drawTextualData() - LowerRight: browserController=" + (browserController != null ? "not null" : "null"));
            if (browserController != null) {
                // HOROS-20240407準拠: BrowserControllerからStudy/Series情報を取得
                Object selectedItem = browserController.getSelectedItem();
                System.out.println("[DEBUG] drawTextualData() - LowerRight: selectedItem=" + (selectedItem != null ? selectedItem.getClass().getName() : "null"));
                
                // HOROS-20240407準拠: DB_image.date - 画像レベルのdateを取得
                // StudyまたはSeriesが選択されている場合に表示
                if (selectedItem instanceof com.jj.dicomviewer.model.DicomStudy || selectedItem instanceof com.jj.dicomviewer.model.DicomSeries) {
                    // Date Acquired (Image Acquisition Date)
                    // HOROS-20240407準拠: AnnotationsDefault.plist - LowerRight: DB_image.date
                    // 現在表示中の画像の取得日時を表示
                    if (dcmFilesList != null && !dcmFilesList.isEmpty() && curImage >= 0 && curImage < dcmFilesList.size()) {
                        Object currentFile = dcmFilesList.get(curImage);
                        java.time.LocalDateTime imageDate = null;
                        
                        // HOROS-20240407準拠: DB_image.date - 画像レベルのdateを取得
                        if (currentFile instanceof com.jj.dicomviewer.model.DicomImage) {
                            com.jj.dicomviewer.model.DicomImage image = (com.jj.dicomviewer.model.DicomImage) currentFile;
                            imageDate = image.getDate();
                        } else if (currentFile instanceof com.jj.dicomviewer.model.DicomSeries) {
                            // フォールバック: Seriesのdateを使用
                            com.jj.dicomviewer.model.DicomSeries series = (com.jj.dicomviewer.model.DicomSeries) currentFile;
                            imageDate = series.getDate();
                        }
                        
                        if (imageDate != null) {
                            // HOROS-20240407準拠: 日時のフォーマット
                            String dateStr = imageDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                            int stringWidth = fm.stringWidth(dateStr);
                            g2d.drawString(dateStr, lowerRightX - stringWidth, lowerRightY);
                            // lowerRightYは既にlineHeightを引いているため、そのまま使用
                        }
                    }
                }
            }
        }
        
        // HOROS-20240407準拠: DCMView.m 8266-8273行目、8528-8531行目 - 方向マーカーの描画
        // AnnotationsDefault.plistではTopMiddle, MiddleLeft, MiddleRight, LowerMiddleにOrientationが含まれている
        // HOROS-20240407準拠: annotationsDictionaryに"Orientation"が含まれている場合のみ表示
        // 方向マーカーはCT/MR画像など、特定のモダリティでのみ表示される
        // CRやUS画像では方向マーカーを表示しない
        if (annotationType >= annotBase) {
            // HOROS-20240407準拠: 方向マーカーはCT/MR画像など、Image Orientation (Patient)が設定されている場合のみ表示
            // CRやUS画像では方向マーカーを表示しない
            String modality = curDCM.getModality();
            if (modality != null && (modality.equals("CT") || modality.equals("MR") || modality.equals("PT") || modality.equals("NM"))) {
                double[] orientation = curDCM.getOrientation();
                if (orientation != null && orientation.length >= 9) {
                    // デフォルト値（単位行列）でない場合のみ表示
                    boolean hasValidOrientation = false;
                    for (int i = 0; i < 6; i++) {
                        if (Math.abs(orientation[i]) > 0.001) {
                            hasValidOrientation = true;
                            break;
                        }
                    }
                    if (hasValidOrientation) {
                        drawOrientation(g2d, panelWidth, panelHeight);
                    }
                }
            }
        }
    }
    
    /**
     * 方向マーカーを描画
     * HOROS-20240407準拠: DCMView.m 7983-8049行目
     */
    private void drawOrientation(Graphics2D g2d, int panelWidth, int panelHeight) {
        if (curDCM == null) {
            return;
        }
        
        // HOROS-20240407準拠: DCMView.m 7994行目 - orientationCorrectedToView
        // 回転やフリップの補正を適用
        double[] correctedOrientation = orientationCorrectedToView();
        if (correctedOrientation == null || correctedOrientation.length < 9) {
            return;
        }
        
        // HOROS-20240407準拠: DCMView.m 7730-7776行目 - getOrientationText
        // 方向テキストを取得（左、右、上、下）
        String leftText = getOrientationText(correctedOrientation, 0, true);   // 左側（行方向ベクトルの逆）
        String rightText = getOrientationText(correctedOrientation, 0, false); // 右側（行方向ベクトル）
        String topText = getOrientationText(correctedOrientation, 3, true);    // 上側（列方向ベクトルの逆）
        String bottomText = getOrientationText(correctedOrientation, 3, false); // 下側（列方向ベクトル）
        
        // HOROS-20240407準拠: フォント設定
        java.awt.Font font = new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14);
        g2d.setFont(font);
        g2d.setColor(java.awt.Color.WHITE);
        
        java.awt.FontMetrics fm = g2d.getFontMetrics();
        int stringHeight = fm.getHeight();
        int margin = 6;
        
        // HOROS-20240407準拠: DCMView.m 7996-8002行目 - 左、右、上、下に方向テキストを描画
        // TopMiddle（上側中央）
        if (topText != null && !topText.isEmpty()) {
            int topTextWidth = fm.stringWidth(topText);
            g2d.drawString(topText, panelWidth / 2 - topTextWidth / 2, stringHeight + 2);
        }
        
        // MiddleLeft（左側中央）
        if (leftText != null && !leftText.isEmpty()) {
            g2d.drawString(leftText, margin, panelHeight / 2 + stringHeight / 2);
        }
        
        // MiddleRight（右側中央）
        if (rightText != null && !rightText.isEmpty()) {
            int rightTextWidth = fm.stringWidth(rightText);
            g2d.drawString(rightText, panelWidth - rightTextWidth - margin, panelHeight / 2 + stringHeight / 2);
        }
        
        // LowerMiddle（下側中央）
        if (bottomText != null && !bottomText.isEmpty()) {
            int bottomTextWidth = fm.stringWidth(bottomText);
            g2d.drawString(bottomText, panelWidth / 2 - bottomTextWidth / 2, panelHeight - 2);
        }
    }
    
    /**
     * 方向ベクトルをビューに補正
     * HOROS-20240407準拠: DCMView.m 11694-11761行目 - orientationCorrectedToView
     * 回転やフリップの補正を適用
     */
    private double[] orientationCorrectedToView() {
        if (curDCM == null) {
            return null;
        }
        
        double[] o = curDCM.getOrientation().clone();
        if (o == null || o.length < 9) {
            return null;
        }
        
        // HOROS-20240407準拠: DCMView.m 11697-11698行目
        // float yRot = -1, xRot = -1;
        // float rot = rotation;
        // 簡易版: rotation、xFlipped、yFlippedが未実装のため、デフォルト値を使用
        // double yRot = -1.0;
        // double xRot = -1.0;
        double rot = 0.0; // TODO: rotation変数を実装
        
        // HOROS-20240407準拠: DCMView.m 11702-11727行目
        // yFlippedとxFlippedの処理
        // 簡易版: xFlipped、yFlippedが未実装のため、デフォルト値を使用
        boolean xFlipped = false; // TODO: xFlipped変数を実装
        boolean yFlipped = false; // TODO: yFlipped変数を実装
        
        if (yFlipped && xFlipped) {
            rot = rot + 180.0;
        } else {
            if (yFlipped) {
                // xRot *= -1.0;
                // yRot *= -1.0;
                o[3] *= -1.0;
                o[4] *= -1.0;
                o[5] *= -1.0;
            }
            if (xFlipped) {
                // xRot *= -1.0;
                // yRot *= -1.0;
                o[0] *= -1.0;
                o[1] *= -1.0;
                o[2] *= -1.0;
            }
        }
        
        // HOROS-20240407準拠: DCMView.m 11729-11733行目 - 法線ベクトルの計算
        o[6] = o[1] * o[5] - o[2] * o[4];
        o[7] = o[2] * o[3] - o[0] * o[5];
        o[8] = o[0] * o[4] - o[1] * o[3];
        
        // HOROS-20240407準拠: DCMView.m 11734-11744行目 - 回転の適用
        // 簡易版: ArbitraryRotate関数が未実装のため、回転処理をスキップ
        // TODO: ArbitraryRotate関数を実装
        
        // HOROS-20240407準拠: DCMView.m 11746-11749行目 - 法線ベクトルの再計算
        o[6] = o[1] * o[5] - o[2] * o[4];
        o[7] = o[2] * o[3] - o[0] * o[5];
        o[8] = o[0] * o[4] - o[1] * o[3];
        
        // HOROS-20240407準拠: DCMView.m 11751-11758行目 - 正規化
        double length = Math.sqrt(o[0] * o[0] + o[1] * o[1] + o[2] * o[2]);
        if (length > 0.0) {
            o[0] = o[0] / length;
            o[1] = o[1] / length;
            o[2] = o[2] / length;
        }
        
        length = Math.sqrt(o[3] * o[3] + o[4] * o[4] + o[5] * o[5]);
        if (length > 0.0) {
            o[3] = o[3] / length;
            o[4] = o[4] / length;
            o[5] = o[5] / length;
        }
        
        length = Math.sqrt(o[6] * o[6] + o[7] * o[7] + o[8] * o[8]);
        if (length > 0.0) {
            o[6] = o[6] / length;
            o[7] = o[7] / length;
            o[8] = o[8] / length;
        }
        
        return o;
    }
    
    /**
     * 方向テキストを取得
     * HOROS-20240407準拠: DCMView.m 7730-7776行目 - getOrientationText
     * @param orientation 方向ベクトル配列（9要素）
     * @param offset ベクトルの開始インデックス（0=行方向、3=列方向）
     * @param inv 逆方向かどうか
     * @return 方向テキスト（例："L", "R", "A", "P", "S", "I", "LPI"など）
     */
    private String getOrientationText(double[] orientation, int offset, boolean inv) {
        if (orientation == null || orientation.length < offset + 3) {
            return "";
        }
        
        // HOROS-20240407準拠: DCMView.m 7740-7751行目
        // ベクトルの各成分の符号に基づいて方向を決定
        double x = inv ? -orientation[offset] : orientation[offset];
        double y = inv ? -orientation[offset + 1] : orientation[offset + 1];
        double z = inv ? -orientation[offset + 2] : orientation[offset + 2];
        
        String orientationX = x < 0 ? "R" : "L"; // Right or Left
        String orientationY = y < 0 ? "A" : "P"; // Anterior or Posterior
        String orientationZ = z < 0 ? "I" : "S"; // Inferior or Superior
        
        // HOROS-20240407準拠: DCMView.m 7753-7773行目
        // 絶対値が大きい順に最大3つの方向を結合
        double absX = Math.abs(x);
        double absY = Math.abs(y);
        double absZ = Math.abs(z);
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            if (absX > 0.2 && absX >= absY && absX >= absZ) {
                result.append(orientationX);
                absX = 0;
            } else if (absY > 0.2 && absY >= absX && absY >= absZ) {
                result.append(orientationY);
                absY = 0;
            } else if (absZ > 0.2 && absZ >= absX && absZ >= absY) {
                result.append(orientationZ);
                absZ = 0;
            } else {
                break;
            }
        }
        
        return result.toString();
    }
    
    /**
     * 圧縮形式文字列を取得
     * HOROS-20240407準拠: BrowserController.m - compressionStringメソッド（実装を確認する必要がある）
     * TransferSyntaxUIDから圧縮形式を判定
     */
    private String getCompressionString(DicomPix dcmPix) {
        if (dcmPix == null) {
            return null;
        }
        
        String transferSyntaxUID = dcmPix.getTransferSyntaxUID();
        if (transferSyntaxUID == null || transferSyntaxUID.isEmpty()) {
            return null;
        }
        
        // HOROS-20240407準拠: TransferSyntaxUIDから圧縮形式を判定
        // 一般的なTransfer Syntax UID:
        // 1.2.840.10008.1.2 = Implicit VR Little Endian (非圧縮)
        // 1.2.840.10008.1.2.1 = Explicit VR Little Endian (非圧縮)
        // 1.2.840.10008.1.2.2 = Explicit VR Big Endian (非圧縮)
        // 1.2.840.10008.1.2.4.50 = JPEG Baseline
        // 1.2.840.10008.1.2.4.51 = JPEG Extended
        // 1.2.840.10008.1.2.4.57 = JPEG Lossless, Non-Hierarchical, First-Order Prediction
        // 1.2.840.10008.1.2.4.70 = JPEG Lossless, Non-Hierarchical
        // 1.2.840.10008.1.2.4.80 = JPEG-LS Lossless
        // 1.2.840.10008.1.2.4.81 = JPEG-LS Lossy
        // 1.2.840.10008.1.2.4.90 = JPEG 2000 Lossless
        // 1.2.840.10008.1.2.4.91 = JPEG 2000 Lossy
        // 1.2.840.10008.1.2.5 = RLE Lossless
        
        if (transferSyntaxUID.equals("1.2.840.10008.1.2") ||
            transferSyntaxUID.equals("1.2.840.10008.1.2.1") ||
            transferSyntaxUID.equals("1.2.840.10008.1.2.2")) {
            return null; // 非圧縮の場合は表示しない
        } else if (transferSyntaxUID.equals("1.2.840.10008.1.2.4.50")) {
            return "JPEG Baseline";
        } else if (transferSyntaxUID.equals("1.2.840.10008.1.2.4.51")) {
            return "JPEG Extended";
        } else if (transferSyntaxUID.equals("1.2.840.10008.1.2.4.57")) {
            return "JPEG Lossless";
        } else if (transferSyntaxUID.equals("1.2.840.10008.1.2.4.70")) {
            return "JPEG Lossless";
        } else if (transferSyntaxUID.equals("1.2.840.10008.1.2.4.80")) {
            return "JPEG-LS Lossless";
        } else if (transferSyntaxUID.equals("1.2.840.10008.1.2.4.81")) {
            return "JPEG-LS Lossy";
        } else if (transferSyntaxUID.equals("1.2.840.10008.1.2.4.90")) {
            return "JPEG 2000 Lossless";
        } else if (transferSyntaxUID.equals("1.2.840.10008.1.2.4.91")) {
            return "JPEG 2000 Lossy";
        } else if (transferSyntaxUID.equals("1.2.840.10008.1.2.5")) {
            return "RLE Lossless";
        } else {
            // その他のTransfer Syntax UIDは、UIDの最後の部分を表示
            String[] parts = transferSyntaxUID.split("\\.");
            if (parts.length > 0) {
                return "TS: " + parts[parts.length - 1];
            }
            return transferSyntaxUID;
        }
    }
    
    /**
     * アノテーションタイプを設定
     * HOROS-20240407準拠: DCMView.h 542行目
     */
    public void setAnnotationType(int annotationType) {
        this.annotationType = annotationType;
        // HOROS-20240407準拠: DCMView.m 7357行目 - [[NSUserDefaults standardUserDefaults] setInteger: chosenLine forKey: @"ANNOTATIONS"];
        // Preferencesに保存
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DCMView.class);
        prefs.putInt("ANNOTATIONS", annotationType);
        System.out.println("[DEBUG] DCMView.setAnnotationType() - annotationType set to: " + annotationType);
        repaint(); // アノテーション表示を更新
    }
    
    /**
     * アノテーションタイプを取得
     */
    public int getAnnotationType() {
        return annotationType;
    }
    
    /**
     * setDefaults - デフォルト設定を読み込む
     * HOROS-20240407準拠: DCMView.m 677行目 - +(void) setDefaults
     * NSUserDefaultsから各種設定を読み込む
     */
    public static void setDefaults() {
        // HOROS-20240407準拠: DCMView.m 677-696行目
        // 各種設定をPreferencesから読み込む
        // java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DCMView.class);
        
        // HOROS-20240407準拠: DCMView.m 696行目 - CLUTBARS = [[NSUserDefaults standardUserDefaults] integerForKey: @"CLUTBARS"];
        // TODO: CLUTBARSなどの他の設定も読み込む必要がある場合はここに追加
        
        // HOROS-20240407準拠: DCMView.m 6490行目 - annotationType = [[NSUserDefaults standardUserDefaults] integerForKey:@"ANNOTATIONS"];
        // 注: ANNOTATIONSの読み込みは各DCMViewインスタンスのコンストラクタで行う
        // ここでは、必要に応じて他の設定を読み込む
        
        System.out.println("[DEBUG] DCMView.setDefaults() called");
    }
    
    /**
     * annotMenu - アノテーションメニューからの設定変更
     * HOROS-20240407準拠: DCMView.m 7353行目 - -(void) annotMenu:(id) sender
     * @param annotationType 選択されたアノテーションタイプ（annotNone, annotGraphics, annotBase, annotFull）
     */
    public void annotMenu(int annotationType) {
        // HOROS-20240407準拠: DCMView.m 7355-7358行目
        // short chosenLine = [sender tag];
        // [[NSUserDefaults standardUserDefaults] setInteger: chosenLine forKey: @"ANNOTATIONS"];
        // [DCMView setDefaults];
        setAnnotationType(annotationType);
        setDefaults(); // HOROS-20240407準拠: DCMView.m 7358行目
        
        // HOROS-20240407準拠: DCMView.m 7360-7362行目
        // NSNotificationCenter *nc;
        // nc = [NSNotificationCenter defaultCenter];
        // [nc postNotificationName: OsirixUpdateViewNotification object: self userInfo: nil];
        // TODO: 通知を送信して他のビューを更新する必要がある場合はここに追加
        
        System.out.println("[DEBUG] DCMView.annotMenu() - annotationType set to: " + annotationType);
    }
    
    /**
     * getPreferredSize - 推奨サイズを返す
     */
    @Override
    public Dimension getPreferredSize() {
        if (curDCM != null) {
            java.awt.image.BufferedImage image = curDCM.getBufferedImage();
            if (image != null) {
                return new Dimension(image.getWidth(), image.getHeight());
            }
        }
        return new Dimension(512, 512); // デフォルトサイズ
    }
}
