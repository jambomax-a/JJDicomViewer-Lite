# 画像表示・レンダリング - Java移植ドキュメント

## 1. 概要

このドキュメントは、HOROSの`DCMPix`と`DCMView`クラスを解析し、Java Swingでの画像表示・レンダリング実装方法をまとめたものです。

## 2. DCMPixクラスの解析

### 2.1 クラス定義

**HOROS実装** (`DCMPix.h`):
```objective-c
@interface DCMPix: NSObject <NSCopying>
{
    NSString            *_srcFile;
    float               *fImage;  // float buffer of image data
    BOOL                isRGB;
    
    // DICOM TAGS
    double              originX, originY, originZ;
    double              orientation[9];
    double              pixelSpacingX, pixelSpacingY;
    float               slope, offset;
    
    // Window Level & Width
    float               ww, wl;
    float               savedWL, savedWW;
    
    // Pixel representation
    BOOL                fIsSigned;
    short               bitsAllocated, bitsStored;
    long                height, width;
    
    // Slice information
    double              sliceLocation;
    double              sliceThickness;
    double              sliceInterval;
}
```

### 2.2 主要機能

#### 2.2.1 画像データの読み込み

**HOROS実装**:
```objective-c
- (void) CheckLoadIn;
- (float*) computefImage;
```

**処理フロー**:
1. DICOMファイルからピクセルデータを読み込み
2. 生データをfloat配列に変換
3. Rescale Slope/Interceptを適用
4. Window Level/Widthを適用

**Java実装**:
```java
public class DicomPix {
    private float[] pixelData;
    private int width, height;
    private double slope = 1.0;
    private double offset = 0.0;
    private boolean isSigned = false;
    private int bitsAllocated = 16;
    private int bitsStored = 16;
    
    public void loadFromDicomFile(Path file) throws IOException {
        try (DicomInputStream dis = new DicomInputStream(file.toFile())) {
            Attributes attrs = dis.readDataset(-1, -1);
            
            // メタデータ読み込み
            loadMetadata(attrs);
            
            // ピクセルデータ読み込み
            loadPixelData(dis, attrs);
        }
    }
    
    private void loadMetadata(Attributes attrs) {
        this.width = attrs.getInt(Tag.Columns, 0);
        this.height = attrs.getInt(Tag.Rows, 0);
        this.bitsAllocated = attrs.getInt(Tag.BitsAllocated, 16);
        this.bitsStored = attrs.getInt(Tag.BitsStored, 16);
        this.isSigned = attrs.getInt(Tag.PixelRepresentation, 0) != 0;
        
        this.slope = attrs.getDouble(Tag.RescaleSlope, 1.0);
        this.offset = attrs.getDouble(Tag.RescaleIntercept, 0.0);
        
        // 空間情報
        double[] pixelSpacing = attrs.getDoubles(Tag.PixelSpacing);
        if (pixelSpacing != null && pixelSpacing.length >= 2) {
            this.pixelSpacingX = pixelSpacing[0];
            this.pixelSpacingY = pixelSpacing[1];
        }
        
        double[] imagePosition = attrs.getDoubles(Tag.ImagePositionPatient);
        if (imagePosition != null && imagePosition.length >= 3) {
            this.originX = imagePosition[0];
            this.originY = imagePosition[1];
            this.originZ = imagePosition[2];
        }
        
        double[] imageOrientation = attrs.getDoubles(Tag.ImageOrientationPatient);
        if (imageOrientation != null && imageOrientation.length >= 6) {
            System.arraycopy(imageOrientation, 0, this.orientation, 0, 6);
        }
    }
    
    private void loadPixelData(DicomInputStream dis, Attributes attrs) 
            throws IOException {
        int pixelDataLength = attrs.getInt(Tag.PixelDataLength, -1);
        
        // ピクセルデータを読み込み
        byte[] pixelBytes = dis.readPixelData();
        
        // データ型に応じて変換
        if (bitsAllocated == 16) {
            if (isSigned) {
                convertSignedShortToFloat(pixelBytes);
            } else {
                convertUnsignedShortToFloat(pixelBytes);
            }
        } else if (bitsAllocated == 8) {
            convertByteToFloat(pixelBytes);
        } else if (bitsAllocated == 12) {
            convert12BitToFloat(pixelBytes);
        }
        
        // Rescale Slope/Interceptを適用
        applyRescale();
    }
    
    private void convertUnsignedShortToFloat(byte[] pixelBytes) {
        pixelData = new float[width * height];
        ByteBuffer buffer = ByteBuffer.wrap(pixelBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        for (int i = 0; i < pixelData.length; i++) {
            int pixelValue = buffer.getShort() & 0xFFFF; // unsigned
            pixelData[i] = pixelValue;
        }
    }
    
    private void convertSignedShortToFloat(byte[] pixelBytes) {
        pixelData = new float[width * height];
        ByteBuffer buffer = ByteBuffer.wrap(pixelBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        for (int i = 0; i < pixelData.length; i++) {
            short pixelValue = buffer.getShort();
            pixelData[i] = pixelValue;
        }
    }
    
    private void convertByteToFloat(byte[] pixelBytes) {
        pixelData = new float[width * height];
        for (int i = 0; i < pixelData.length; i++) {
            pixelData[i] = pixelBytes[i] & 0xFF;
        }
    }
    
    private void convert12BitToFloat(byte[] pixelBytes) {
        pixelData = new float[width * height];
        int index = 0;
        
        for (int i = 0; i < pixelBytes.length; i += 3) {
            // 12bitパッキング形式の変換
            int byte1 = pixelBytes[i] & 0xFF;
            int byte2 = pixelBytes[i + 1] & 0xFF;
            int byte3 = pixelBytes[i + 2] & 0xFF;
            
            // 2ピクセル分のデータ
            int pixel1 = byte1 | ((byte2 & 0x0F) << 8);
            int pixel2 = ((byte2 & 0xF0) >> 4) | (byte3 << 4);
            
            if (index < pixelData.length) pixelData[index++] = pixel1;
            if (index < pixelData.length) pixelData[index++] = pixel2;
        }
    }
    
    private void applyRescale() {
        for (int i = 0; i < pixelData.length; i++) {
            pixelData[i] = pixelData[i] * slope + offset;
        }
    }
}
```

#### 2.2.2 Window Level/Widthの適用

**HOROS実装**:
```objective-c
- (void) changeWLWW:(float)newWL :(float)newWW;
- (void) compute8bitRepresentation;
```

**処理フロー**:
1. Window Level/Widthに基づいてピクセル値をマッピング
2. 8bit表現（0-255）に変換
3. カラーマッピング（CLUT）を適用（オプション）

**Java実装**:
```java
public class DicomPix {
    private float windowLevel = 0.0f;
    private float windowWidth = 0.0f;
    private BufferedImage renderedImage;
    
    public void setWindowLevel(float wl, float ww) {
        this.windowLevel = wl;
        this.windowWidth = ww;
        compute8bitRepresentation();
    }
    
    private void compute8bitRepresentation() {
        if (pixelData == null) return;
        
        // ウィンドウ範囲の計算
        float windowMin = windowLevel - windowWidth / 2.0f;
        float windowMax = windowLevel + windowWidth / 2.0f;
        
        // ピクセル値の最小・最大を取得
        float minValue = Float.MAX_VALUE;
        float maxValue = Float.MIN_VALUE;
        for (float value : pixelData) {
            minValue = Math.min(minValue, value);
            maxValue = Math.max(maxValue, value);
        }
        
        // デフォルトWL/WWが設定されていない場合
        if (windowWidth == 0.0f) {
            windowMin = minValue;
            windowMax = maxValue;
            windowLevel = (minValue + maxValue) / 2.0f;
            windowWidth = maxValue - minValue;
        }
        
        // 8bit画像の生成
        renderedImage = new BufferedImage(width, height, 
            BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = renderedImage.getRaster();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float pixelValue = pixelData[y * width + x];
                
                // ウィンドウ範囲内にクリップ
                pixelValue = Math.max(windowMin, Math.min(windowMax, pixelValue));
                
                // 0-255に正規化
                int grayValue = (int) ((pixelValue - windowMin) / 
                    (windowMax - windowMin) * 255.0f);
                grayValue = Math.max(0, Math.min(255, grayValue));
                
                raster.setSample(x, y, 0, grayValue);
            }
        }
    }
    
    public BufferedImage getRenderedImage() {
        if (renderedImage == null) {
            compute8bitRepresentation();
        }
        return renderedImage;
    }
}
```

#### 2.2.3 カラーマッピング（CLUT）

**HOROS実装**:
```objective-c
- (void) setCLUT:(unsigned char*)r :(unsigned char*)g :(unsigned char*)b;
```

**Java実装**:
```java
public class DicomPix {
    private ColorLookupTable clut;
    
    public void setColorLookupTable(ColorLookupTable clut) {
        this.clut = clut;
        compute8bitRepresentation();
    }
    
    private void applyColorLookupTable(BufferedImage image) {
        if (clut == null) return;
        
        // グレースケール画像をカラー画像に変換
        BufferedImage colorImage = new BufferedImage(
            width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int grayValue = image.getRaster().getSample(x, y, 0);
                int[] rgb = clut.getRGB(grayValue);
                int rgbValue = (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
                colorImage.setRGB(x, y, rgbValue);
            }
        }
        
        renderedImage = colorImage;
    }
}

public class ColorLookupTable {
    private byte[] redTable;
    private byte[] greenTable;
    private byte[] blueTable;
    
    public ColorLookupTable(byte[] r, byte[] g, byte[] b) {
        this.redTable = r;
        this.greenTable = g;
        this.blueTable = b;
    }
    
    public int[] getRGB(int index) {
        if (index < 0 || index >= 256) {
            return new int[]{0, 0, 0};
        }
        return new int[]{
            redTable[index] & 0xFF,
            greenTable[index] & 0xFF,
            blueTable[index] & 0xFF
        };
    }
    
    // プリセットCLUT
    public static ColorLookupTable createHotIron() {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        
        for (int i = 0; i < 256; i++) {
            if (i < 85) {
                r[i] = (byte) (i * 3);
                g[i] = 0;
                b[i] = 0;
            } else if (i < 170) {
                r[i] = (byte) 255;
                g[i] = (byte) ((i - 85) * 3);
                b[i] = 0;
            } else {
                r[i] = (byte) 255;
                g[i] = (byte) 255;
                b[i] = (byte) ((i - 170) * 3);
            }
        }
        
        return new ColorLookupTable(r, g, b);
    }
}
```

## 3. DCMViewクラスの解析

### 3.1 クラス定義

**HOROS実装** (`DCMView.h`):
```objective-c
@interface DCMView: NSOpenGLView <NSDraggingSource>
{
    NSMutableArray      *dcmPixList;
    DCMPix             *curDCM;
    ToolMode           currentTool;
    
    float               scaleValue;
    NSPoint             origin;
    float               rotation;
    BOOL                xFlipped, yFlipped;
    
    float               curWW, curWL;
    
    GLuint              *pTextureName;
    long                textureWidth, textureHeight;
}
```

### 3.2 主要機能

#### 3.2.1 画像表示

**Java実装**:
```java
public class DicomView extends JPanel {
    private List<DicomPix> pixList;
    private DicomPix currentPix;
    private int currentIndex = 0;
    
    private float scale = 1.0f;
    private Point2D origin = new Point2D.Double(0, 0);
    private float rotation = 0.0f;
    private boolean xFlipped = false;
    private boolean yFlipped = false;
    
    private float windowLevel = 0.0f;
    private float windowWidth = 0.0f;
    
    private BufferedImage displayImage;
    
    public DicomView() {
        setOpaque(true);
        setBackground(Color.BLACK);
        
        // マウスリスナー
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }
        });
        
        addMouseWheelListener(e -> {
            handleMouseWheel(e);
        });
    }
    
    public void setPixels(List<DicomPix> pixels) {
        this.pixList = pixels;
        if (pixels != null && !pixels.isEmpty()) {
            this.currentPix = pixels.get(0);
            this.currentIndex = 0;
            updateDisplay();
        }
    }
    
    public void setCurrentImage(int index) {
        if (pixList == null || index < 0 || index >= pixList.size()) {
            return;
        }
        this.currentIndex = index;
        this.currentPix = pixList.get(index);
        updateDisplay();
    }
    
    private void updateDisplay() {
        if (currentPix == null) return;
        
        // Window Level/Widthを適用
        currentPix.setWindowLevel(windowLevel, windowWidth);
        
        // 画像を取得
        BufferedImage sourceImage = currentPix.getRenderedImage();
        
        // 変換を適用
        displayImage = applyTransforms(sourceImage);
        
        repaint();
    }
    
    private BufferedImage applyTransforms(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        
        // スケール適用
        int scaledWidth = (int) (width * scale);
        int scaledHeight = (int) (height * scale);
        
        BufferedImage transformed = new BufferedImage(
            scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = transformed.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
        // 回転
        if (rotation != 0.0f) {
            AffineTransform transform = AffineTransform.getRotateInstance(
                Math.toRadians(rotation), scaledWidth / 2.0, scaledHeight / 2.0);
            g2d.setTransform(transform);
        }
        
        // 反転
        if (xFlipped || yFlipped) {
            double sx = xFlipped ? -1 : 1;
            double sy = yFlipped ? -1 : 1;
            double tx = xFlipped ? scaledWidth : 0;
            double ty = yFlipped ? scaledHeight : 0;
            g2d.translate(tx, ty);
            g2d.scale(sx, sy);
        }
        
        g2d.drawImage(source, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();
        
        return transformed;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
        // 背景
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        if (displayImage == null) return;
        
        // 画像を中央に配置
        int x = (int) (getWidth() / 2.0 - displayImage.getWidth() / 2.0 + 
                      origin.getX());
        int y = (int) (getHeight() / 2.0 - displayImage.getHeight() / 2.0 + 
                      origin.getY());
        
        g2d.drawImage(displayImage, x, y, null);
        
        // アノテーション描画
        drawAnnotations(g2d);
    }
    
    private void drawAnnotations(Graphics2D g2d) {
        // 患者情報、スタディ情報などのアノテーション
        if (currentPix == null) return;
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // 左上に情報を表示
        String info = String.format("WL: %.1f  WW: %.1f", 
            windowLevel, windowWidth);
        g2d.drawString(info, 10, 20);
    }
}
```

#### 3.2.2 マウス操作（JJDICOMViewer-Liteと共通化）

**操作仕様**（JJDICOMViewer-Liteの実装に基づく）:
- **パンニング**: 左クリック + ドラッグ（SHIFTキーが押されていない場合）
- **ズーム**: CTRL + マウスホイール
- **Window Level/Width**: SHIFT + 左クリック + ドラッグ
  - 縦方向（deltaY）: Window Center（上にドラッグで増加、下にドラッグで減少）
  - 横方向（deltaX）: Window Width（右にドラッグで増加、左にドラッグで減少）
- **スライス移動**: 通常のマウスホイール（CTRLが押されていない場合）

**Java実装**（JJDICOMViewer-Lite準拠）:
```java
public class DicomView extends JPanel {
    private int lastMouseX;
    private int lastMouseY;
    private boolean isPanning = false;
    private boolean isWindowLevelAdjusting = false;
    
    public DicomView() {
        setOpaque(true);
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);
        
        setupMouseListeners();
        setupWheelListener();
    }
    
    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                
                if (SwingUtilities.isLeftMouseButton(e) && !e.isShiftDown()) {
                    // 左クリック（SHIFTなし）: パン開始
                    isPanning = true;
                    isWindowLevelAdjusting = false;
                } else if (SwingUtilities.isLeftMouseButton(e) && e.isShiftDown()) {
                    // SHIFT + 左クリック: Window Level/Width調整開始
                    isWindowLevelAdjusting = true;
                    isPanning = false;
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isPanning = false;
                isWindowLevelAdjusting = false;
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isPanning) {
                    // パン: 左クリック + ドラッグ
                    int deltaX = e.getX() - lastMouseX;
                    int deltaY = e.getY() - lastMouseY;
                    panX += deltaX;
                    panY += deltaY;
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    repaint();
                } else if (isWindowLevelAdjusting) {
                    // Window Level/Width調整: SHIFT + 左クリック + ドラッグ
                    int deltaX = e.getX() - lastMouseX;
                    int deltaY = e.getY() - lastMouseY;
                    // 縦方向: Window Center（上にドラッグで増加 = -deltaY）
                    // 横方向: Window Width（右にドラッグで増加 = +deltaX）
                    adjustWindowLevel(-deltaY * 2, deltaX * 2);
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                }
            }
        });
    }
    
    private void setupWheelListener() {
        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                // CTRL + マウスホイール: ズーム
                double zoomDelta = e.getWheelRotation() < 0 ? 1.1 : 0.9;
                setZoom(zoomFactor * zoomDelta);
            } else {
                // 通常のマウスホイール: スライス移動
                if (sliceChangeCallback != null) {
                    if (e.getWheelRotation() < 0) {
                        sliceChangeCallback.onPreviousSlice();
                    } else {
                        sliceChangeCallback.onNextSlice();
                    }
                }
            }
        });
    }
    
    private void adjustWindowLevel(double deltaCenter, double deltaWidth) {
        // ビット深度に応じた調整
        if (currentPix != null && currentPix.getBitsAllocated() == 8) {
            // 8bit画像の場合
            double newCenter = windowLevel + deltaCenter * 1.0;
            double newWidth = windowWidth + deltaWidth * 1.0;
            
            if (newCenter >= 0 && newCenter <= 255) {
                windowLevel = newCenter;
            }
            if (newWidth >= 0.1 && newWidth <= 255) {
                windowWidth = newWidth;
            }
        } else {
            // 16bit画像などの場合
            windowLevel += deltaCenter * 2;
            windowWidth = Math.max(1, windowWidth + deltaWidth * 2);
        }
        
        updateDisplay();
        
        // コールバックで通知
        if (windowLevelChangeCallback != null) {
            windowLevelChangeCallback.onWindowLevelChanged(windowLevel, windowWidth);
        }
    }
    
    public void setZoom(double factor) {
        zoomFactor = Math.max(0.1, Math.min(10.0, factor));
        repaint();
    }
    
    public void scaleToFit() {
        if (currentPix == null) return;
        
        int imgWidth = currentPix.getWidth();
        int imgHeight = currentPix.getHeight();
        
        double scaleX = (double) getWidth() / imgWidth;
        double scaleY = (double) getHeight() / imgHeight;
        
        zoomFactor = Math.min(scaleX, scaleY) * 0.95; // 少し余白を持たせる
        panX = 0;
        panY = 0;
        repaint();
    }
    
    // スライス移動コールバック
    public interface SliceChangeCallback {
        void onNextSlice();
        void onPreviousSlice();
    }
    
    private SliceChangeCallback sliceChangeCallback;
    
    public void setSliceChangeCallback(SliceChangeCallback callback) {
        this.sliceChangeCallback = callback;
    }
    
    // Window Level/Width変更コールバック
    public interface WindowLevelChangeCallback {
        void onWindowLevelChanged(double center, double width);
    }
    
    private WindowLevelChangeCallback windowLevelChangeCallback;
    
    public void setWindowLevelChangeCallback(WindowLevelChangeCallback callback) {
        this.windowLevelChangeCallback = callback;
    }
}
```

**重要な実装詳細**:
1. **パンニング**: `panX`と`panY`を累積的に更新
2. **Window Level/Width**: 
   - 縦方向の移動（deltaY）がWindow Centerに影響（上にドラッグ = 負のdeltaY = Center増加）
   - 横方向の移動（deltaX）がWindow Widthに影響（右にドラッグ = 正のdeltaX = Width増加）
   - 調整量は`delta * 2`で、ビット深度に応じて調整
3. **ズーム**: CTRL + ホイールで1.1倍または0.9倍のズーム
4. **スライス移動**: CTRLなしのホイールで前後のスライスに移動

## 4. パフォーマンス最適化

### 4.1 画像キャッシング

```java
public class ImageCache {
    private static final int MAX_CACHE_SIZE = 100;
    private final Map<String, BufferedImage> cache = 
        new LinkedHashMap<String, BufferedImage>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(
                Map.Entry<String, BufferedImage> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
    
    public BufferedImage get(String key) {
        return cache.get(key);
    }
    
    public void put(String key, BufferedImage image) {
        cache.put(key, image);
    }
    
    public void clear() {
        cache.clear();
    }
}
```

### 4.2 非同期画像読み込み

```java
public class AsyncImageLoader {
    private ExecutorService executor = Executors.newFixedThreadPool(2);
    
    public void loadImageAsync(Path file, Consumer<BufferedImage> callback) {
        executor.submit(() -> {
            try {
                DicomPix pix = new DicomPix();
                pix.loadFromDicomFile(file);
                BufferedImage image = pix.getRenderedImage();
                
                SwingUtilities.invokeLater(() -> callback.accept(image));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
```

## 4. キーボードショートカット

### 4.1 反転と回転機能

HOROSでは以下のキーボードショートカットが実装されています：

- **Hキー**: 左右反転（FlipHorizontal）
- **Vキー**: 上下反転（FlipVertical）
- **Rキー**: 右回転（90度時計回り）- 実装推奨
- **Lキー**: 左回転（90度反時計回り）- 実装推奨

### 4.2 HOROSの実装

**HOROS実装** (`DCMView.m`):
```objective-c
- (void)flipVertical: (id)sender
{
    self.yFlipped = !yFlipped;
}

- (void)flipHorizontal: (id)sender
{
    self.xFlipped = !xFlipped;
}
```

**ホットキー設定** (`DefaultsOsiriX.m`):
```objective-c
@"v",	//FlipVerticalHotKeyAction
@"h",	//FlipHorizontalHotKeyAction
```

### 4.3 Java実装

```java
public class DicomView extends JPanel {
    private boolean xFlipped = false;
    private boolean yFlipped = false;
    private float rotation = 0.0f;
    
    public DicomView() {
        // ... 既存のコード ...
        
        // キーボードリスナー
        setupKeyboardListeners();
    }
    
    private void setupKeyboardListeners() {
        setFocusable(true);
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_H:
                        // Hキー: 左右反転
                        flipHorizontal();
                        break;
                        
                    case KeyEvent.VK_V:
                        // Vキー: 上下反転
                        flipVertical();
                        break;
                        
                    case KeyEvent.VK_R:
                        // Rキー: 右回転（90度時計回り）
                        rotateRight();
                        break;
                        
                    case KeyEvent.VK_L:
                        // Lキー: 左回転（90度反時計回り）
                        rotateLeft();
                        break;
                }
            }
        });
    }
    
    public void flipHorizontal() {
        xFlipped = !xFlipped;
        updateDisplay();
    }
    
    public void flipVertical() {
        yFlipped = !yFlipped;
        updateDisplay();
    }
    
    public void rotateRight() {
        rotation += 90.0f;
        if (rotation >= 360.0f) {
            rotation -= 360.0f;
        }
        updateDisplay();
    }
    
    public void rotateLeft() {
        rotation -= 90.0f;
        if (rotation < 0.0f) {
            rotation += 360.0f;
        }
        updateDisplay();
    }
    
    private BufferedImage applyTransforms(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        
        // 回転を適用
        BufferedImage rotated = source;
        if (rotation != 0.0f) {
            rotated = rotateImage(source, rotation);
            width = rotated.getWidth();
            height = rotated.getHeight();
        }
        
        // スケール適用
        int scaledWidth = (int) (width * scale);
        int scaledHeight = (int) (height * scale);
        
        BufferedImage transformed = new BufferedImage(
            scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = transformed.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
        // 反転
        if (xFlipped || yFlipped) {
            double sx = xFlipped ? -1 : 1;
            double sy = yFlipped ? -1 : 1;
            double tx = xFlipped ? scaledWidth : 0;
            double ty = yFlipped ? scaledHeight : 0;
            g2d.translate(tx, ty);
            g2d.scale(sx, sy);
        }
        
        g2d.drawImage(rotated, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();
        
        return transformed;
    }
    
    private BufferedImage rotateImage(BufferedImage source, float angle) {
        double radians = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        
        int newWidth = (int) Math.round(
            source.getWidth() * cos + source.getHeight() * sin);
        int newHeight = (int) Math.round(
            source.getWidth() * sin + source.getHeight() * cos);
        
        BufferedImage rotated = new BufferedImage(
            newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = rotated.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
        // 回転の中心を画像の中心に設定
        AffineTransform transform = AffineTransform.getRotateInstance(
            radians, newWidth / 2.0, newHeight / 2.0);
        
        // 回転後の画像を中央に配置
        double tx = (newWidth - source.getWidth()) / 2.0;
        double ty = (newHeight - source.getHeight()) / 2.0;
        transform.translate(tx, ty);
        
        g2d.setTransform(transform);
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        
        return rotated;
    }
    
    public boolean isXFlipped() {
        return xFlipped;
    }
    
    public boolean isYFlipped() {
        return yFlipped;
    }
    
    public float getRotation() {
        return rotation;
    }
    
    public void setRotation(float rotation) {
        this.rotation = rotation;
        if (this.rotation < 0) {
            this.rotation += 360.0f;
        }
        if (this.rotation >= 360.0f) {
            this.rotation -= 360.0f;
        }
        updateDisplay();
    }
}
```

### 4.4 90度回転の最適化

90度、180度、270度の回転は、より効率的な実装が可能です：

```java
private BufferedImage rotateImage90(BufferedImage source, boolean clockwise) {
    int width = source.getWidth();
    int height = source.getHeight();
    
    BufferedImage rotated = new BufferedImage(
        height, width, BufferedImage.TYPE_INT_RGB);
    
    if (clockwise) {
        // 時計回り90度
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                rotated.setRGB(height - 1 - y, x, rgb);
            }
        }
    } else {
        // 反時計回り90度
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                rotated.setRGB(y, width - 1 - x, rgb);
            }
        }
    }
    
    return rotated;
}

public void rotateRight() {
    rotation += 90.0f;
    if (rotation >= 360.0f) {
        rotation -= 360.0f;
    }
    
    // 90度の倍数の場合は最適化された回転を使用
    if (Math.abs(rotation % 90.0f) < 0.01f) {
        if (processedImage != null) {
            processedImage = rotateImage90(processedImage, true);
        }
    }
    
    updateDisplay();
}

public void rotateLeft() {
    rotation -= 90.0f;
    if (rotation < 0.0f) {
        rotation += 360.0f;
    }
    
    // 90度の倍数の場合は最適化された回転を使用
    if (Math.abs(rotation % 90.0f) < 0.01f) {
        if (processedImage != null) {
            processedImage = rotateImage90(processedImage, false);
        }
    }
    
    updateDisplay();
}
```

## 5. まとめ

画像表示・レンダリングのJava実装では：

1. **DicomPix**: DICOM画像データの読み込みと変換
2. **DicomView**: Swingコンポーネントでの画像表示
3. **Window Level/Width**: コントラスト調整
4. **マウス操作**: ズーム、パン、WL/WW調整（JJDICOMViewer-Liteと共通）
5. **キーボードショートカット**: H（左右反転）、V（上下反転）、R（右回転）、L（左回転）
6. **パフォーマンス**: キャッシングと非同期読み込み

次のステップ: DICOM通信の実装

