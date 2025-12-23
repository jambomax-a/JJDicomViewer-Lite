# MPR/3D画像作成機能 - Java移植ドキュメント

## 1. 概要

このドキュメントは、HOROSのMPR（Multi-Planar Reconstruction）および3D画像生成機能を解析し、Javaでの実装方法をまとめたものです。

HOROSでは以下の3D/MPR機能を提供しています：
- **Orthogonal MPR**: 直交平面再構成（軸方向、冠状方向、矢状方向）
- **Curved MPR (CPR)**: 曲線平面再構成
- **Volume Rendering (VR)**: ボリュームレンダリング
- **Maximum Intensity Projection (MIP)**: 最大強度投影
- **Minimum Intensity Projection (MinIP)**: 最小強度投影

## 2. HOROS実装の解析

### 2.1 MPRController

**HOROS実装** (`MPRController.h`):
```objective-c
@interface MPRController : Window3DController <NSToolbarDelegate, NSSplitViewDelegate>
{
    IBOutlet MPRDCMView *mprView1, *mprView2, *mprView3;
    IBOutlet NSSplitView *horizontalSplit, *verticalSplit;
    
    NSMutableArray *filesList[ MAX4D], *pixList[ MAX4D];
    DCMPix *originalPix;
    NSData *volumeData[ MAX4D];
    
    float clippingRangeThickness;
    int clippingRangeMode;
    
    float LOD;  // Level of Detail
    BOOL lowLOD;
}
```

**主要機能**:
- 3つのMPRビュー（軸方向、冠状方向、矢状方向）の管理
- ボリュームデータの管理
- クリッピング範囲の設定
- LOD（詳細度）の調整
- ブレンディング機能

### 2.2 OrthogonalMPRController

**HOROS実装** (`OrthogonalMPRController.h`):
```objective-c
@interface OrthogonalMPRController : NSObject
{
    NSMutableArray *originalDCMPixList, *xReslicedDCMPixList, *yReslicedDCMPixList;
    OrthogonalReslice *reslicer;
    
    IBOutlet OrthogonalMPRView *originalView, *xReslicedView, *yReslicedView;
    
    short thickSlabMode, thickSlab;
    NSData *transferFunction;
}
```

**主要機能**:
- 元画像からの再スライス生成
- X方向、Y方向の再スライス
- 厚みスラブ（Thick Slab）モード
- 転送関数（Transfer Function）の適用

### 2.3 VRController (Volume Rendering)

**HOROS実装** (`VRController.h`):
```objective-c
@interface VRController : Window3DController <NSWindowDelegate, NSToolbarDelegate>
{
    IBOutlet VRView *view;
    
    NSMutableArray *pixList[ 100];
    NSData *volumeData[ 100];
    
    NSString *renderingMode;  // "VR", "MIP", "MinIP"
    
    float *undodata[ 100];
    float minimumValue, maximumValue;
    
    // CLUT & Opacity
    IBOutlet NSDrawer *clutOpacityDrawer;
    IBOutlet CLUTOpacityView *clutOpacityView;
}
```

**主要機能**:
- ボリュームレンダリング（VR）
- 最大強度投影（MIP）
- 最小強度投影（MinIP）
- CLUT（Color Look-Up Table）と不透明度の調整
- シェーディング（Shading）機能
- クリッピング範囲の設定

### 2.4 CPRController (Curved Planar Reconstruction)

**HOROS実装** (`CPRController.h`):
```objective-c
@interface CPRController : Window3DController <CPRViewDelegate, NSToolbarDelegate>
{
    IBOutlet CPRMPRDCMView *cprView;
    IBOutlet CPRView *cprView3D;
    
    CPRCurvedPath *curvedPath;
    CPRVolumeData *volumeData;
    
    enum _CPRType {
        CPRStraightenedType = 0,
        CPRStretchedType = 1
    };
}
```

**主要機能**:
- 曲線パスに沿った再構成
- ストレッチ（Stretched）モード
- ストレート（Straightened）モード
- 3Dビューでのパス編集

## 3. Java実装方針

### 3.1 アーキテクチャ

Java実装では、以下のアーキテクチャを採用します：

```
MPRController (Swing JFrame)
├── MPRViewPanel (JPanel) - 3つのMPRビュー
│   ├── AxialViewPanel
│   ├── CoronalViewPanel
│   └── SagittalViewPanel
├── VolumeDataManager - ボリュームデータ管理
├── ResliceEngine - 再スライス処理エンジン
└── MPRToolbar - ツールバー
```

### 3.2 ボリュームデータの管理

**Java実装**:
```java
import java.nio.FloatBuffer;
import java.util.List;

public class VolumeData {
    private FloatBuffer volumeBuffer;
    private int width;
    private int height;
    private int depth;
    private double[] pixelSpacing;  // [x, y, z]
    private double[] imagePosition;  // [x, y, z]
    private double[] imageOrientation;  // [6 values]
    
    public VolumeData(List<DCMPix> pixList) {
        // DICOMスライスからボリュームデータを構築
        this.width = pixList.get(0).getWidth();
        this.height = pixList.get(0).getHeight();
        this.depth = pixList.size();
        
        volumeBuffer = FloatBuffer.allocate(width * height * depth);
        
        for (int z = 0; z < depth; z++) {
            DCMPix pix = pixList.get(z);
            float[] pixels = pix.getPixels();
            volumeBuffer.put(pixels, z * width * height, width * height);
        }
        
        // メタデータの設定
        extractMetadata(pixList);
    }
    
    public float getPixel(int x, int y, int z) {
        if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth) {
            return 0.0f;
        }
        return volumeBuffer.get(z * width * height + y * width + x);
    }
    
    public void setPixel(int x, int y, int z, float value) {
        if (x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth) {
            volumeBuffer.put(z * width * height + y * width + x, value);
        }
    }
    
    private void extractMetadata(List<DCMPix> pixList) {
        // ピクセルスペーシング、画像位置、画像方向の抽出
        // ...
    }
}
```

### 3.3 再スライス処理

**Java実装**:
```java
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;

public class ResliceEngine {
    private VolumeData volumeData;
    
    public ResliceEngine(VolumeData volumeData) {
        this.volumeData = volumeData;
    }
    
    /**
     * 任意の平面で再スライスを生成
     * @param planeNormal 平面の法線ベクトル [nx, ny, nz]
     * @param planePoint 平面上の点 [px, py, pz]
     * @param outputWidth 出力画像の幅
     * @param outputHeight 出力画像の高さ
     * @param pixelSpacing 出力画像のピクセルスペーシング
     * @return 再スライスされた画像
     */
    public BufferedImage reslice(
            double[] planeNormal, 
            double[] planePoint,
            int outputWidth, 
            int outputHeight,
            double pixelSpacing) {
        
        BufferedImage result = new BufferedImage(
            outputWidth, outputHeight, BufferedImage.TYPE_USHORT_GRAY);
        
        // 平面の基底ベクトルを計算
        double[] u = calculateBasisVector1(planeNormal);
        double[] v = calculateBasisVector2(planeNormal, u);
        
        // 各ピクセル位置でのボリューム値を取得
        for (int y = 0; y < outputHeight; y++) {
            for (int x = 0; x < outputWidth; x++) {
                // 画像座標から3D座標への変換
                double[] worldPos = imageToWorld(
                    x, y, 
                    outputWidth, outputHeight,
                    pixelSpacing,
                    planePoint, u, v);
                
                // 3D座標からボリューム座標への変換
                int[] volumePos = worldToVolume(worldPos);
                
                // トリリニア補間でピクセル値を取得
                float pixelValue = trilinearInterpolation(volumePos);
                
                // 画像に設定
                int grayValue = (int) Math.round(pixelValue);
                result.setRGB(x, y, (grayValue << 16) | (grayValue << 8) | grayValue);
            }
        }
        
        return result;
    }
    
    /**
     * トリリニア補間
     */
    private float trilinearInterpolation(int[] pos) {
        int x0 = pos[0], y0 = pos[1], z0 = pos[2];
        int x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;
        
        // 境界チェック
        if (x1 >= volumeData.getWidth()) x1 = x0;
        if (y1 >= volumeData.getHeight()) y1 = y0;
        if (z1 >= volumeData.getDepth()) z1 = z0;
        
        // 8頂点の値を取得
        float c000 = volumeData.getPixel(x0, y0, z0);
        float c100 = volumeData.getPixel(x1, y0, z0);
        float c010 = volumeData.getPixel(x0, y1, z0);
        float c110 = volumeData.getPixel(x1, y1, z0);
        float c001 = volumeData.getPixel(x0, y0, z1);
        float c101 = volumeData.getPixel(x1, y0, z1);
        float c011 = volumeData.getPixel(x0, y1, z1);
        float c111 = volumeData.getPixel(x1, y1, z1);
        
        // 補間計算
        double fx = pos[0] - x0;
        double fy = pos[1] - y0;
        double fz = pos[2] - z0;
        
        float c00 = (float)(c000 * (1 - fx) + c100 * fx);
        float c01 = (float)(c001 * (1 - fx) + c101 * fx);
        float c10 = (float)(c010 * (1 - fx) + c110 * fx);
        float c11 = (float)(c011 * (1 - fx) + c111 * fx);
        
        float c0 = (float)(c00 * (1 - fy) + c10 * fy);
        float c1 = (float)(c01 * (1 - fy) + c11 * fy);
        
        return (float)(c0 * (1 - fz) + c1 * fz);
    }
    
    private double[] calculateBasisVector1(double[] normal) {
        // 法線ベクトルに垂直な第1基底ベクトルを計算
        // ...
    }
    
    private double[] calculateBasisVector2(double[] normal, double[] u) {
        // 法線ベクトルとuに垂直な第2基底ベクトルを計算
        // ...
    }
    
    private double[] imageToWorld(int x, int y, int width, int height, 
                                   double spacing, double[] origin, 
                                   double[] u, double[] v) {
        // 画像座標から3D世界座標への変換
        // ...
    }
    
    private int[] worldToVolume(double[] worldPos) {
        // 3D世界座標からボリューム座標への変換
        // ...
    }
}
```

### 3.4 MPRビューの実装

**Java実装**:
```java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class MPRViewPanel extends JPanel {
    private VolumeData volumeData;
    private ResliceEngine resliceEngine;
    private BufferedImage currentSlice;
    private double[] planeNormal;
    private double[] planePoint;
    
    private double zoom = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;
    private double windowLevel = 0.0;
    private double windowWidth = 0.0;
    
    public MPRViewPanel(VolumeData volumeData, double[] planeNormal) {
        this.volumeData = volumeData;
        this.planeNormal = planeNormal;
        this.resliceEngine = new ResliceEngine(volumeData);
        
        setPreferredSize(new Dimension(512, 512));
        setBackground(Color.BLACK);
        
        // マウス操作の設定（JJDICOMViewer-Liteと共通）
        setupMouseControls();
        
        // 初期スライスの生成
        updateSlice();
    }
    
    private void setupMouseControls() {
        // パンニング: 左クリック + ドラッグ
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && !e.isShiftDown()) {
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    isPanning = true;
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isPanning = false;
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isPanning) {
                    int deltaX = e.getX() - lastMouseX;
                    int deltaY = e.getY() - lastMouseY;
                    panX += deltaX / zoom;
                    panY += deltaY / zoom;
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    repaint();
                }
            }
        });
        
        // ズーム: CTRL + マウスホイール
        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                double zoomDelta = e.getWheelRotation() < 0 ? 1.1 : 0.9;
                setZoom(zoom * zoomDelta);
            }
        });
        
        // Window Level/Width: SHIFT + 左クリック + ドラッグ
        // （JJDICOMViewer-Liteと共通の実装）
    }
    
    public void updateSlice() {
        int width = getWidth();
        int height = getHeight();
        double pixelSpacing = 1.0 / zoom;
        
        currentSlice = resliceEngine.reslice(
            planeNormal, planePoint, width, height, pixelSpacing);
        
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        if (currentSlice != null) {
            // Window Level/Widthの適用
            BufferedImage displayed = applyWindowLevel(currentSlice);
            
            // ズームとパンの適用
            AffineTransform transform = new AffineTransform();
            transform.translate(panX * zoom, panY * zoom);
            transform.scale(zoom, zoom);
            
            g2d.setTransform(transform);
            g2d.drawImage(displayed, 0, 0, null);
        }
    }
    
    private BufferedImage applyWindowLevel(BufferedImage image) {
        // Window Level/Widthの適用
        // ...
    }
}
```

### 3.5 Volume Renderingの実装

**Java実装**:
```java
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

public class VolumeRenderer {
    private VolumeData volumeData;
    private ColorLookupTable clut;
    private OpacityTransferFunction opacityTF;
    
    public VolumeRenderer(VolumeData volumeData) {
        this.volumeData = volumeData;
        this.clut = new ColorLookupTable();
        this.opacityTF = new OpacityTransferFunction();
    }
    
    /**
     * レイキャスティングによるボリュームレンダリング
     * @param viewMatrix ビュー変換行列
     * @param projectionMatrix 射影変換行列
     * @param outputWidth 出力画像の幅
     * @param outputHeight 出力画像の高さ
     * @return レンダリングされた画像
     */
    public BufferedImage render(
            double[][] viewMatrix,
            double[][] projectionMatrix,
            int outputWidth,
            int outputHeight) {
        
        BufferedImage result = new BufferedImage(
            outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        
        // 各ピクセルに対してレイキャスティング
        for (int y = 0; y < outputHeight; y++) {
            for (int x = 0; x < outputWidth; x++) {
                // レイの生成
                double[] rayOrigin = calculateRayOrigin(x, y, viewMatrix, projectionMatrix);
                double[] rayDirection = calculateRayDirection(x, y, viewMatrix, projectionMatrix);
                
                // レイキャスティング
                int color = castRay(rayOrigin, rayDirection);
                
                result.setRGB(x, y, color);
            }
        }
        
        return result;
    }
    
    private int castRay(double[] origin, double[] direction) {
        float[] accumulatedColor = new float[]{0.0f, 0.0f, 0.0f};
        float accumulatedOpacity = 0.0f;
        
        // レイとボリュームの交差判定
        double[] entryPoint = findEntryPoint(origin, direction);
        double[] exitPoint = findExitPoint(origin, direction);
        
        if (entryPoint == null || exitPoint == null) {
            return 0x000000; // 黒
        }
        
        // レイに沿ってサンプリング
        double stepSize = 0.5; // サンプリング間隔
        double[] currentPos = entryPoint.clone();
        double[] step = new double[]{
            direction[0] * stepSize,
            direction[1] * stepSize,
            direction[2] * stepSize
        };
        
        double distance = Math.sqrt(
            Math.pow(exitPoint[0] - entryPoint[0], 2) +
            Math.pow(exitPoint[1] - entryPoint[1], 2) +
            Math.pow(exitPoint[2] - entryPoint[2], 2));
        
        int numSteps = (int) (distance / stepSize);
        
        for (int i = 0; i < numSteps && accumulatedOpacity < 0.99f; i++) {
            // ボリューム座標への変換
            int[] volumePos = worldToVolume(currentPos);
            
            // トリリニア補間でピクセル値を取得
            float value = trilinearInterpolation(volumePos);
            
            // 色と不透明度の取得
            int[] color = clut.getColor(value);
            float opacity = opacityTF.getOpacity(value) * stepSize;
            
            // アルファブレンディング
            float alpha = opacity * (1.0f - accumulatedOpacity);
            accumulatedColor[0] += color[0] * alpha / 255.0f;
            accumulatedColor[1] += color[1] * alpha / 255.0f;
            accumulatedColor[2] += color[2] * alpha / 255.0f;
            accumulatedOpacity += alpha;
            
            // 次のサンプルポイントへ
            currentPos[0] += step[0];
            currentPos[1] += step[1];
            currentPos[2] += step[2];
        }
        
        // 最終色の計算
        int r = (int) (Math.min(1.0f, accumulatedColor[0]) * 255);
        int g = (int) (Math.min(1.0f, accumulatedColor[1]) * 255);
        int b = (int) (Math.min(1.0f, accumulatedColor[2]) * 255);
        
        return (r << 16) | (g << 8) | b;
    }
    
    /**
     * MIP (Maximum Intensity Projection)
     */
    public BufferedImage renderMIP(
            double[][] viewMatrix,
            double[][] projectionMatrix,
            int outputWidth,
            int outputHeight) {
        
        BufferedImage result = new BufferedImage(
            outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < outputHeight; y++) {
            for (int x = 0; x < outputWidth; x++) {
                double[] rayOrigin = calculateRayOrigin(x, y, viewMatrix, projectionMatrix);
                double[] rayDirection = calculateRayDirection(x, y, viewMatrix, projectionMatrix);
                
                float maxValue = findMaxValueAlongRay(rayOrigin, rayDirection);
                
                int gray = (int) Math.round(maxValue);
                int color = (gray << 16) | (gray << 8) | gray;
                result.setRGB(x, y, color);
            }
        }
        
        return result;
    }
    
    private float findMaxValueAlongRay(double[] origin, double[] direction) {
        float maxValue = Float.NEGATIVE_INFINITY;
        
        double[] entryPoint = findEntryPoint(origin, direction);
        double[] exitPoint = findExitPoint(origin, direction);
        
        if (entryPoint == null || exitPoint == null) {
            return 0.0f;
        }
        
        // レイに沿ってサンプリングし、最大値を探す
        // ...
        
        return maxValue;
    }
}
```

## 4. 実装の考慮事項

### 4.1 パフォーマンス

- **マルチスレッド処理**: 再スライス処理をバックグラウンドスレッドで実行
- **キャッシング**: よく使用されるスライスをキャッシュ
- **LOD (Level of Detail)**: ズームアウト時は低解像度で表示
- **GPUアクセラレーション**: 可能であればOpenCLやCUDAを検討

### 4.2 メモリ管理

- **ボリュームデータの圧縮**: 必要に応じて圧縮形式を使用
- **段階的読み込み**: 必要な部分のみをメモリに読み込む
- **ガベージコレクション**: 大きなオブジェクトの適切な解放

### 4.3 ライブラリの選択

**推奨ライブラリ**:
- **JOGL (Java OpenGL)**: OpenGLバインディング（3Dレンダリング用）
- **JOML (Java OpenGL Math Library)**: 数学計算ライブラリ
- **ImageJ**: 画像処理ライブラリ（オプション）

## 5. 実装順序

1. **VolumeDataクラス**: ボリュームデータの管理
2. **ResliceEngineクラス**: 基本的な再スライス処理
3. **MPRViewPanelクラス**: MPRビューの実装
4. **MPRControllerクラス**: MPRウィンドウのコントローラー
5. **VolumeRendererクラス**: ボリュームレンダリング
6. **CPRControllerクラス**: 曲線平面再構成（オプション）

## 6. 参考資料

- HOROSソースコード: `MPRController.h/m`, `VRController.h/m`, `CPRController.h/m`
- DICOM標準: PS3.3 (Image Plane Module)
- ボリュームレンダリングアルゴリズム: Ray Casting, Shear-Warp Factorization

