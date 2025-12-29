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
    
    // HOROS-20240407準拠: DCMView.h 141行目 - BOOL whiteBackground;
    protected boolean whiteBackground = false;
    
    // HOROS-20240407準拠: scaleToFitNoReentry（無限ループ防止）
    private boolean scaleToFitNoReentry = false;
    
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
        
        // HOROS-20240407準拠: コンポーネントサイズ変更時にscaleToFitを呼び出す
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (curDCM != null && !scaleToFitNoReentry) {
                    scaleToFit();
                }
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
                            DicomPix first = this.dcmPixList.get(0);
                            DicomPix last = this.dcmPixList.get(this.dcmPixList.size() - 1);
                            // HOROS-20240407準拠: sliceLocationが同じ場合はvolumicSeries = NO
                            // TODO: DicomPixにgetSliceLocation()メソッドを追加する必要がある
                            // 現時点では、volumicSeriesの判定をスキップ
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
                            
                            // HOROS-20240407準拠: curDCMが変更された場合、scaleToFitを呼び出す必要がある
                            // ただし、setIndexWithResetで既に呼び出されている場合は不要
                            // ここでは、scaleValueとoriginを維持する
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
