package com.jjdicomviewer.ui;

import com.jjdicomviewer.config.AppConfig;
import com.jjdicomviewer.core.Instance;
import com.jjdicomviewer.core.Series;
import com.jjdicomviewer.dicom.DicomLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * DICOM画像表示パネル（Swing版）
 * JavaFX版と同様の機能：ウィンドウ/レベル、ズーム、パンに対応
 */
public class ImageViewerPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(ImageViewerPanel.class);
    
    private BufferedImage originalImage;
    private BufferedImage processedImage;
    private DicomLoader dicomLoader;
    
    // ウィンドウ/レベル
    private double windowCenter = 128;
    private double windowWidth = 256;
    
    // ズーム
    private double zoomFactor = 1.0;
    private double baseZoomFactor = 1.0; // 初期表示時のfitScaleを保存
    private boolean isInitialDisplay = true; // 初期表示フラグ
    
    // パン
    private double panX = 0;
    private double panY = 0;
    
    // マウス操作
    private int lastMouseX;
    private int lastMouseY;
    private boolean isPanning = false;
    private boolean isWindowLevelAdjusting = false;
    
    // ビット深度（デフォルト16bit）
    private int bitsStored = 16;
    
    // スライス移動コールバック
    private SliceChangeCallback sliceChangeCallback;
    
    // 現在のシリーズ（WW/WL維持の判定用）
    private Series currentSeries;
    
    // 現在のインスタンス（リセット時に使用）
    private Instance currentInstance;
    
    // WW/WL変更コールバック
    private WindowLevelChangeCallback windowLevelChangeCallback;

    public ImageViewerPanel() {
        dicomLoader = new DicomLoader();
        setBackground(Color.BLACK);
        setOpaque(true); // 背景を不透明に設定（重複描画を防ぐ）
        setDoubleBuffered(true); // ダブルバッファリングを有効化（ちらつきを防ぐ）
        setPreferredSize(new Dimension(1024, 768));
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
                    isPanning = true;
                } else if (SwingUtilities.isLeftMouseButton(e) && e.isShiftDown()) {
                    isWindowLevelAdjusting = true;
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
                    int deltaX = e.getX() - lastMouseX;
                    int deltaY = e.getY() - lastMouseY;
                    panX += deltaX;
                    panY += deltaY;
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    repaint();
                } else if (isWindowLevelAdjusting) {
                    int deltaX = e.getX() - lastMouseX;
                    int deltaY = e.getY() - lastMouseY;
                    // 横方向: ウィンドウ幅、縦方向: ウィンドウセンター
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
                // Ctrl + ホイール: ズーム
                double zoomDelta = e.getWheelRotation() < 0 ? 1.1 : 0.9;
                setZoom(zoomFactor * zoomDelta);
            } else {
                // 通常のホイール: スライス移動
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
    
    /**
     * スライス移動コールバックを設定
     */
    public void setSliceChangeCallback(SliceChangeCallback callback) {
        this.sliceChangeCallback = callback;
    }
    
    /**
     * スライス移動コールバックインターフェース
     */
    public interface SliceChangeCallback {
        void onNextSlice();
        void onPreviousSlice();
    }
    
    /**
     * WW/WL変更コールバックインターフェース
     */
    public interface WindowLevelChangeCallback {
        void onWindowLevelChanged(double center, double width);
    }
    
    /**
     * WW/WL変更コールバックを設定
     */
    public void setWindowLevelChangeCallback(WindowLevelChangeCallback callback) {
        this.windowLevelChangeCallback = callback;
    }

    /**
     * Seriesを読み込んで表示
     */
    public void loadSeries(Series series) {
        if (series == null) {
            originalImage = null;
            processedImage = null;
            currentSeries = null;
            currentInstance = null;
            resetView();
            repaint();
            return;
        }
        
        // 新しいシリーズかどうかを判定
        boolean isNewSeries = (currentSeries == null || 
                               !series.getSeriesInstanceUID().equals(currentSeries.getSeriesInstanceUID()));
        
        currentSeries = series;
        
        List<Instance> instances = series.getInstanceList();
        if (instances == null || instances.isEmpty()) {
            return;
        }
        
        // インスタンスをソート（InstanceNumber順）
        instances.sort((a, b) -> {
            int numA = a.getInstanceNumber() != null ? a.getInstanceNumber() : 0;
            int numB = b.getInstanceNumber() != null ? b.getInstanceNumber() : 0;
            return Integer.compare(numA, numB);
        });
        
        // 最初のインスタンスを読み込む（新しいシリーズの場合はWW/WLをリセット）
        loadInstance(instances.get(0), isNewSeries);
    }
    
    /**
     * Instanceを読み込んで表示
     * @param instance インスタンス
     * @param resetWindowLevel 新しいシリーズの場合はtrue（WW/WLをリセット）、同一シリーズ内のスクロール時はfalse（WW/WLを維持）
     */
    public void loadInstance(Instance instance, boolean resetWindowLevel) {
        if (instance == null || instance.getFilePath() == null) {
            logger.warn("loadInstance: instanceまたはfilePathがnullです");
            return;
        }
        
        logger.debug("loadInstance: SOPInstanceUID={}, filePath={}, resetWindowLevel={}", 
            instance.getSopInstanceUID(), instance.getFilePath(), resetWindowLevel);
        
        // 現在のインスタンスを保存（リセット時に使用）
        currentInstance = instance;
        
        try {
            // ビット深度を取得
            if (instance.getBitsStored() != null) {
                bitsStored = instance.getBitsStored();
            }
            
            // ファイルパスを検証し、存在しない場合はストレージベースパス配下を検索
            Path filePath = instance.getFilePath();
            if (!Files.exists(filePath)) {
                Path correctedPath = findFileInStorage(instance);
                if (correctedPath != null && Files.exists(correctedPath)) {
                    filePath = correctedPath;
                    instance.setFilePath(correctedPath);
                } else {
                    logger.error("ファイルが見つかりません: {} (SOPInstanceUID: {})", filePath, instance.getSopInstanceUID());
                    return;
                }
            }
            
            
            // 新しいシリーズの場合はデフォルトのウィンドウ/レベルを設定
            // 同一シリーズ内のスクロール時は現在のWW/WLを維持
            if (resetWindowLevel) {
                // currentSeriesが設定されていることを確認してからWW/WLを設定
                setDefaultWindowLevel(instance);
                // 新しいシリーズの場合はズームとパンもリセット
                zoomFactor = 1.0;
                baseZoomFactor = 1.0;
                isInitialDisplay = true;
                panX = 0;
                panY = 0;
            }
            
            // 画像を読み込む
            originalImage = dicomLoader.loadDicomImage(filePath.toFile(), 
                    windowCenter, windowWidth);
            
            if (originalImage != null) {
                logger.debug("画像読み込み成功: サイズ={}x{}, SOPInstanceUID={}", 
                    originalImage.getWidth(), originalImage.getHeight(), instance.getSopInstanceUID());
                
                // 新しいシリーズの場合、実際のピクセル値の範囲を計算してウィンドウ/レベルを調整
                // DICOMファイルのWW/WLが不適切な場合もあるため、常に画像から計算した値を優先
                if (resetWindowLevel) {
                    adjustWindowLevelFromImage();
                }
                
                processImage();
                
                if (processedImage != null) {
                    logger.info("画像処理完了: サイズ={}x{}", 
                        processedImage.getWidth(), processedImage.getHeight());
                } else {
                    logger.warn("画像処理後、processedImageがnullです");
                }
                
                repaint();
            } else {
                logger.error("画像の読み込みに失敗: filePath={}, SOPInstanceUID={}", 
                    filePath, instance.getSopInstanceUID());
            }
        } catch (Exception e) {
            logger.error("インスタンスの読み込み中にエラーが発生しました", e);
        }
    }
    
    /**
     * ストレージベースパス配下でファイルを検索
     * @param instance インスタンス
     * @return 見つかったファイルのパス、見つからない場合はnull
     */
    private Path findFileInStorage(Instance instance) {
        try {
            AppConfig appConfig = AppConfig.getInstance();
            Path storageBasePath = appConfig.getStorageBasePath();
            
            if (!Files.exists(storageBasePath)) {
                return null;
            }
            
            String fileName = instance.getSopInstanceUID() + ".dcm";
            final Path[] foundPath = new Path[1];
            java.nio.file.FileVisitor<Path> capturingVisitor = new java.nio.file.SimpleFileVisitor<Path>() {
                @Override
                public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) {
                    if (file.getFileName().toString().equals(fileName)) {
                        foundPath[0] = file;
                        return java.nio.file.FileVisitResult.TERMINATE;
                    }
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
            };
            
            Files.walkFileTree(storageBasePath, capturingVisitor);
            
            return foundPath[0];
        } catch (Exception e) {
            logger.error("ストレージベースパス配下のファイル検索中にエラーが発生しました", e);
            return null;
        }
    }
    
    /**
     * Instanceを読み込んで表示（後方互換性のため）
     */
    public void loadInstance(Instance instance) {
        loadInstance(instance, true);
    }
    
    /**
     * DICOMファイルを読み込んで表示（後方互換性のため残す）
     */
    public void loadDicomFile(File file) {
        try {
            originalImage = dicomLoader.loadDicomImage(file);
            
            if (originalImage != null) {
                resetView();
                setDefaultWindowLevel();
                processImage();
                repaint();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "DICOMファイルから画像を読み込めませんでした。", 
                    "エラー", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            logger.error("DICOMファイルの読み込み中にエラーが発生しました", e);
            JOptionPane.showMessageDialog(this, 
                "エラー: " + e.getMessage(), 
                "エラー", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * デフォルトのウィンドウ/レベルを設定
     */
    private void setDefaultWindowLevel() {
        setDefaultWindowLevel(null);
    }
    
    /**
     * デフォルトのウィンドウ/レベルを設定（Instanceから）
     */
    private void setDefaultWindowLevel(Instance instance) {
        // DICOMファイルからウィンドウ/レベルを取得
        if (instance != null && instance.getWindowCenter() != null && instance.getWindowWidth() != null) {
            try {
                String centerStr = instance.getWindowCenter().trim();
                String widthStr = instance.getWindowWidth().trim();
                
                if (!centerStr.isEmpty() && !widthStr.isEmpty()) {
                    String[] centers = centerStr.split("[\\\\/]");
                    String[] widths = widthStr.split("[\\\\/]");
                    
                    if (centers.length > 0 && widths.length > 0) {
                        double center = Double.parseDouble(centers[0].trim());
                        double width = Double.parseDouble(widths[0].trim());
                        
                        int bits = instance.getBitsStored() != null ? instance.getBitsStored() : 16;
                        double maxValue = (1 << bits) - 1;
                        
                        if (width > 0 && width <= maxValue * 2 && Math.abs(center) <= maxValue * 2) {
                            windowCenter = center;
                            windowWidth = width;
                            bitsStored = bits;
                            notifyWindowLevelChanged();
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                // パース失敗時はデフォルト値を使用
            }
        }
        
        // デフォルトのウィンドウ/レベルを設定
        // Rescale Slope/Interceptを考慮するため、実際の画像を読み込んでからピクセル値の範囲を計算
        int bits = instance != null && instance.getBitsStored() != null ? instance.getBitsStored() : 16;
        bitsStored = bits;
        
        // Rescale Slope/Interceptを取得
        double rescaleSlope = 1.0;
        double rescaleIntercept = 0.0;
        if (instance != null) {
            if (instance.getRescaleSlope() != null) {
                rescaleSlope = instance.getRescaleSlope();
            }
            if (instance.getRescaleIntercept() != null) {
                rescaleIntercept = instance.getRescaleIntercept();
            }
        }
        
        // 初期値として、生のピクセル値の範囲に基づいたウィンドウ/レベルを設定
        int rawMaxValue = (1 << bits) - 1;
        
        // Rescale Slope/Interceptを考慮した実際の値の範囲を計算
        double minRescaledValue = 0.0 * rescaleSlope + rescaleIntercept;
        double maxRescaledValue = rawMaxValue * rescaleSlope + rescaleIntercept;
        
        // ウィンドウ/レベルを設定（中央値と範囲の80%を使用）
        windowCenter = (minRescaledValue + maxRescaledValue) / 2.0;
        windowWidth = (maxRescaledValue - minRescaledValue) * 0.8;
        
        // 最小値と最大値を確保
        if (windowWidth < 1.0) {
            windowWidth = Math.max(maxRescaledValue - minRescaledValue, 1.0);
        }
        
        // ビット深度に応じた妥当な範囲に制限
        if (bits == 8) {
            windowCenter = Math.max(0, Math.min(255, windowCenter));
            windowWidth = Math.max(1, Math.min(255, windowWidth));
        } else if (bits == 12) {
            windowCenter = Math.max(0, Math.min(4095, windowCenter));
            windowWidth = Math.max(1, Math.min(4095, windowWidth));
        } else if (bits == 16) {
            // 16bitの場合、範囲を広くする（MRIなどの場合、値が大きくなる可能性があるため）
            windowCenter = Math.max(-32768, Math.min(32767, windowCenter));
            windowWidth = Math.max(1, Math.min(65535, windowWidth));
        }
        
        logger.info("デフォルトウィンドウ/レベルを設定: bitsStored={}, rescaleSlope={}, rescaleIntercept={}, windowCenter={}, windowWidth={}", 
            bits, rescaleSlope, rescaleIntercept, windowCenter, windowWidth);
        
        // WW/WL変更を通知
        notifyWindowLevelChanged();
    }
    
    /**
     * ウィンドウ/レベルを適用して画像を処理
     * 生のピクセルデータ（RAWデータ）に対してウィンドウ/レベルを適用
     */
    private void processImage() {
        if (originalImage == null) {
            return;
        }
        
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        
        if (width <= 0 || height <= 0) {
            return;
        }
        
        // Instanceから追加情報を取得
        boolean isSigned = false;
        double rescaleSlope = 1.0;
        double rescaleIntercept = 0.0;
        String photometricInterpretation = "MONOCHROME2";
        int bitsAllocated = 16;
        int samplesPerPixel = 1;
        
        if (currentInstance != null) {
            if (currentInstance.getBitsAllocated() != null) {
                bitsAllocated = currentInstance.getBitsAllocated();
            }
            if (currentInstance.getRescaleSlope() != null) {
                rescaleSlope = currentInstance.getRescaleSlope();
            }
            if (currentInstance.getRescaleIntercept() != null) {
                rescaleIntercept = currentInstance.getRescaleIntercept();
            }
            if (currentInstance.getPhotometricInterpretation() != null) {
                photometricInterpretation = currentInstance.getPhotometricInterpretation();
            }
            if (currentInstance.getPixelRepresentation() != null) {
                isSigned = (currentInstance.getPixelRepresentation() != 0);
            }
            if (currentInstance.getSamplesPerPixel() != null) {
                samplesPerPixel = currentInstance.getSamplesPerPixel();
            }
        }
        
        // カラー画像かどうかを判定（すべての画像でWW/WLを適用）
        boolean isColorImage = (samplesPerPixel == 3 || samplesPerPixel == 4 || "RGB".equals(photometricInterpretation));
        
        processedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // ウィンドウ/レベルの計算
        double wc = windowCenter;
        double ww = Math.max(0.1, windowWidth);
        double windowMin = wc - ww / 2.0;
        double windowMax = wc + ww / 2.0;
        
        // BufferedImageから直接ピクセル値を取得
        java.awt.image.Raster raster = originalImage.getRaster();
        int imageType = originalImage.getType();
        int dataType = originalImage.getSampleModel().getDataType();
        boolean isShortType = (imageType == BufferedImage.TYPE_USHORT_GRAY || 
                              dataType == java.awt.image.DataBuffer.TYPE_USHORT);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isColorImage) {
                    // カラー画像：各RGBチャンネルにWW/WLを適用
                    int rgb = originalImage.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = (rgb) & 0xFF;
                    
                    double rescaledR = r * rescaleSlope + rescaleIntercept;
                    double rescaledG = g * rescaleSlope + rescaleIntercept;
                    double rescaledB = b * rescaleSlope + rescaleIntercept;
                    
                    double normalizedR = (rescaledR < windowMin) ? 0.0 : (rescaledR > windowMax) ? 1.0 : (rescaledR - windowMin) / ww;
                    double normalizedG = (rescaledG < windowMin) ? 0.0 : (rescaledG > windowMax) ? 1.0 : (rescaledG - windowMin) / ww;
                    double normalizedB = (rescaledB < windowMin) ? 0.0 : (rescaledB > windowMax) ? 1.0 : (rescaledB - windowMin) / ww;
                    
                    int grayR = (int) Math.round(Math.max(0, Math.min(255, normalizedR * 255)));
                    int grayG = (int) Math.round(Math.max(0, Math.min(255, normalizedG * 255)));
                    int grayB = (int) Math.round(Math.max(0, Math.min(255, normalizedB * 255)));
                    
                    if ("MONOCHROME1".equals(photometricInterpretation)) {
                        grayR = 255 - grayR;
                        grayG = 255 - grayG;
                        grayB = 255 - grayB;
                    }
                    
                    processedImage.setRGB(x, y, (grayR << 16) | (grayG << 8) | grayB);
                } else {
                    // グレースケール画像
                    double rawPixelValue;
                    if (isShortType) {
                        int sample = raster.getSample(x, y, 0);
                        rawPixelValue = isSigned ? sample - 32768 : sample;
                    } else {
                        int rgb = originalImage.getRGB(x, y);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = (rgb) & 0xFF;
                        rawPixelValue = (r + g + b) / 3.0;
                    }
                    
                    double rescaledValue = rawPixelValue * rescaleSlope + rescaleIntercept;
                    double normalizedValue = (rescaledValue < windowMin) ? 0.0 : (rescaledValue > windowMax) ? 1.0 : (rescaledValue - windowMin) / ww;
                    int grayValue = (int) Math.round(Math.max(0, Math.min(255, normalizedValue * 255)));
                    
                    if ("MONOCHROME1".equals(photometricInterpretation)) {
                        grayValue = 255 - grayValue;
                    }
                    
                    processedImage.setRGB(x, y, (grayValue << 16) | (grayValue << 8) | grayValue);
                }
            }
        }
    }
    
    /**
     * 実際の画像のピクセル値の範囲からウィンドウ/レベルを計算して調整
     * MRIなど、Rescale Slope/Interceptを考慮した実際の値の範囲を計算
     */
    private void adjustWindowLevelFromImage() {
        if (originalImage == null || currentInstance == null) {
            return;
        }
        
        try {
            // Rescale Slope/Interceptを取得
            double rescaleSlope = currentInstance.getRescaleSlope() != null ? currentInstance.getRescaleSlope() : 1.0;
            double rescaleIntercept = currentInstance.getRescaleIntercept() != null ? currentInstance.getRescaleIntercept() : 0.0;
            
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();
            java.awt.image.Raster raster = originalImage.getRaster();
            int imageType = originalImage.getType();
            int dataType = originalImage.getSampleModel().getDataType();
            boolean isShortType = (imageType == BufferedImage.TYPE_USHORT_GRAY || 
                                  dataType == java.awt.image.DataBuffer.TYPE_USHORT);
            boolean isSigned = currentInstance.getPixelRepresentation() != null && currentInstance.getPixelRepresentation() != 0;
            
            // 実際のピクセル値の範囲を計算
            double minValue = Double.MAX_VALUE;
            double maxValue = Double.MIN_VALUE;
            
            // サンプリングして計算（全ピクセルを計算すると重いため、100ピクセルごとにサンプリング）
            int sampleStep = Math.max(1, Math.max(width, height) / 100);
            
            for (int y = 0; y < height; y += sampleStep) {
                for (int x = 0; x < width; x += sampleStep) {
                    double rawPixelValue;
                    if (isShortType) {
                        int sample = raster.getSample(x, y, 0);
                        rawPixelValue = isSigned ? sample - 32768 : sample;
                    } else {
                        int rgb = originalImage.getRGB(x, y);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = (rgb) & 0xFF;
                        rawPixelValue = (r + g + b) / 3.0;
                    }
                    
                    // Rescale Slope/Interceptを適用
                    double rescaledValue = rawPixelValue * rescaleSlope + rescaleIntercept;
                    
                    if (rescaledValue < minValue) {
                        minValue = rescaledValue;
                    }
                    if (rescaledValue > maxValue) {
                        maxValue = rescaledValue;
                    }
                }
            }
            
            // ピクセル値の範囲が有効な場合、ウィンドウ/レベルを設定
            if (minValue != Double.MAX_VALUE && maxValue != Double.MIN_VALUE && maxValue > minValue) {
                // ウィンドウ/レベルを設定（中央値と範囲全体を使用）
                double newWindowCenter = (minValue + maxValue) / 2.0;
                double newWindowWidth = maxValue - minValue;
                
                // 最小幅を確保（値の範囲が小さい場合でも適切に表示できるように）
                if (newWindowWidth < 1.0) {
                    newWindowWidth = Math.max(1.0, Math.abs(maxValue) + Math.abs(minValue));
                }
                
                windowCenter = newWindowCenter;
                windowWidth = newWindowWidth;
                
                logger.info("実際のピクセル値からウィンドウ/レベルを調整: min={}, max={}, windowCenter={}, windowWidth={}, rescaleSlope={}, rescaleIntercept={}", 
                    minValue, maxValue, windowCenter, windowWidth, rescaleSlope, rescaleIntercept);
                
                notifyWindowLevelChanged();
            } else {
                logger.warn("ピクセル値の範囲を計算できませんでした: min={}, max={}", minValue, maxValue);
                // ピクセル値の範囲を計算できない場合、setDefaultWindowLevelの値を維持
            }
        } catch (Exception e) {
            logger.error("ピクセル値の範囲を計算中にエラーが発生しました", e);
        }
    }
    
    /**
     * ウィンドウ/レベルを調整
     */
    public void adjustWindowLevel(double deltaCenter, double deltaWidth) {
        if (currentInstance != null && currentInstance.getBitsAllocated() != null && currentInstance.getBitsAllocated() == 8) {
            double newCenter = windowCenter + deltaCenter * 1.0;
            double newWidth = windowWidth + deltaWidth * 1.0;
            
            if (Math.abs(deltaCenter) < 0.001 && Math.abs(deltaWidth) < 0.001) {
                windowCenter = newCenter;
                windowWidth = Math.max(0.1, newWidth);
            } else {
                if (newCenter >= 0 && newCenter <= 255) {
                    windowCenter = newCenter;
                }
                if (newWidth >= 0.1 && newWidth <= 255) {
                    windowWidth = newWidth;
                }
            }
        } else {
            windowCenter += deltaCenter * 2;
            windowWidth = Math.max(1, windowWidth + deltaWidth * 2);
        }
        reloadImageWithWindowLevel();
        notifyWindowLevelChanged();
    }
    
    /**
     * ウィンドウ/レベルを設定
     */
    public void setWindowLevel(double center, double width) {
        windowCenter = center;
        windowWidth = Math.max(1, width);
        // ウィンドウ/レベルを変更した場合は、画像を再読み込みする
        reloadImageWithWindowLevel();
        notifyWindowLevelChanged();
    }
    
    /**
     * 現在のウィンドウ/レベルで画像を再読み込み
     */
    private void reloadImageWithWindowLevel() {
        if (currentInstance == null || currentInstance.getFilePath() == null) {
            // インスタンスがない場合は、processImage()のみ実行
            processImage();
            repaint();
            return;
        }
        
        try {
            originalImage = dicomLoader.loadDicomImage(currentInstance.getFilePath().toFile(), 
                    windowCenter, windowWidth);
            
            if (originalImage != null) {
                processImage();
                repaint();
            }
        } catch (Exception e) {
            logger.error("画像の再読み込み中にエラーが発生しました", e);
        }
    }
    
    /**
     * WW/WL変更を通知
     */
    private void notifyWindowLevelChanged() {
        if (windowLevelChangeCallback != null) {
            windowLevelChangeCallback.onWindowLevelChanged(windowCenter, windowWidth);
        }
    }
    
    /**
     * ズームを設定
     */
    public void setZoom(double factor) {
        // 初期表示が完了していない場合は、baseZoomFactorを設定してからズーム
        if (isInitialDisplay && baseZoomFactor == 1.0) {
            // まだbaseZoomFactorが設定されていない場合は、次のpaintComponentで設定されるまで待つ
            // この場合は単純にfactorを設定
            zoomFactor = Math.max(0.1, Math.min(10.0, factor));
        } else {
            // 初期表示が完了している場合は、相対的なズームを設定
            zoomFactor = Math.max(0.1, Math.min(10.0, factor));
            isInitialDisplay = false; // ズームしたので初期表示ではない
        }
        repaint();
    }
    
    /**
     * ズームイン
     */
    public void zoomIn() {
        setZoom(zoomFactor * 1.2);
    }
    
    /**
     * ズームアウト
     */
    public void zoomOut() {
        setZoom(zoomFactor / 1.2);
    }
    
    /**
     * リセット（ズーム、パン、WW/WLをシリーズ選択時の初期値に戻す）
     */
    public void resetView() {
        zoomFactor = 1.0;
        baseZoomFactor = 1.0; // リセット時もbaseZoomFactorをリセット
        isInitialDisplay = true; // リセット後は初期表示状態に戻る
        panX = 0;
        panY = 0;
        // 現在のインスタンスを使って、シリーズ選択時と同じ初期値にリセット
        if (currentInstance != null) {
            setDefaultWindowLevel(currentInstance);
            // ウィンドウ/レベルを変更したので、画像を再読み込み
            reloadImageWithWindowLevel();
        } else {
            // インスタンスがない場合はデフォルト値を使用
            setDefaultWindowLevel(null);
            processImage();
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 背景をクリア（重複描画を防ぐ）
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                              RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
                              RenderingHints.VALUE_RENDER_QUALITY);
        
        if (processedImage != null) {
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            int imageWidth = processedImage.getWidth();
            int imageHeight = processedImage.getHeight();
            
            // パネルのサイズが0の場合は描画しない
            if (panelWidth <= 0 || panelHeight <= 0) {
                return;
            }
            
            // 初期表示時の適切なズームファクターを計算（パネルに収まるように）
            double fitScaleX = (double) panelWidth / imageWidth;
            double fitScaleY = (double) panelHeight / imageHeight;
            double fitScale = Math.min(fitScaleX, fitScaleY) * 0.95; // 少し余白を持たせる
            
            // baseZoomFactorが初期値（1.0）のままの場合、fitScaleを設定
            // または、画像サイズが変更された場合も再計算
            if (baseZoomFactor == 1.0 || isInitialDisplay) {
                if (fitScale > 0) {
                    baseZoomFactor = fitScale;
                    isInitialDisplay = false;
                    logger.info("初期表示: 画像サイズ={}x{}, パネルサイズ={}x{}, fitScale={}, baseZoomFactor={}", 
                        imageWidth, imageHeight, panelWidth, panelHeight, fitScale, baseZoomFactor);
                } else {
                    // fitScaleが0以下の場合（画像がパネルより大きい場合）、1.0を使用
                    baseZoomFactor = 1.0;
                    logger.warn("fitScaleが0以下: 画像サイズ={}x{}, パネルサイズ={}x{}, baseZoomFactor=1.0に設定", 
                        imageWidth, imageHeight, panelWidth, panelHeight);
                }
            }
            
            double scaledWidth;
            double scaledHeight;
            
            // baseZoomFactorを使用してスケール（zoomFactorも考慮）
            double effectiveZoom = baseZoomFactor * zoomFactor;
            scaledWidth = imageWidth * effectiveZoom;
            scaledHeight = imageHeight * effectiveZoom;
            
            // スケール後のサイズが異常に小さい場合の警告
            if (scaledWidth < 10 || scaledHeight < 10) {
                logger.error("スケール後の画像サイズが異常に小さい: 元のサイズ={}x{}, スケール後={}x{}, baseZoomFactor={}, zoomFactor={}, effectiveZoom={}", 
                    imageWidth, imageHeight, scaledWidth, scaledHeight, baseZoomFactor, zoomFactor, effectiveZoom);
            }
            
            // パンを適用
            double centerX = panelWidth / 2.0;
            double centerY = panelHeight / 2.0;
            double drawX = centerX - scaledWidth / 2.0 + panX;
            double drawY = centerY - scaledHeight / 2.0 + panY;
            
            // パンを適用
            //double centerX = panelWidth / 2.0;
            //double centerY = panelHeight / 2.0;
            //double drawX = centerX - scaledWidth / 2.0 + panX;
            //double drawY = centerY - scaledHeight / 2.0 + panY;
            
            // デバッグログ（画像サイズが異常に小さい場合を検出）
            if (scaledWidth < panelWidth / 3.0 || scaledHeight < panelHeight / 3.0) {
                logger.warn("画像サイズが異常に小さい可能性: 画像={}x{}, スケール後={}x{}, パネル={}x{}, baseZoomFactor={}, zoomFactor={}, effectiveZoom={}", 
                    imageWidth, imageHeight, scaledWidth, scaledHeight, panelWidth, panelHeight, 
                    baseZoomFactor, zoomFactor, effectiveZoom);
            }
            
            // クリッピング領域を設定（パネル外に描画されないようにする）
            Shape oldClip = g2d.getClip();
            g2d.setClip(0, 0, panelWidth, panelHeight);
            
            // 画像を描画（1回のみ、絶対に1回だけ）
            logger.info("画像描画: 位置=({}, {}), サイズ={}x{}, 元のサイズ={}x{}, パネル={}x{}", 
                (int)drawX, (int)drawY, (int)scaledWidth, (int)scaledHeight, imageWidth, imageHeight, panelWidth, panelHeight);
            
            // 画像が異常に小さい場合、強制的にパネルサイズに合わせる（デバッグ用）
            if (scaledWidth < panelWidth / 2.0 || scaledHeight < panelHeight / 2.0) {
                logger.warn("画像サイズが異常に小さいため、強制的にパネルサイズに合わせます");
                // アスペクト比を維持してパネルに収める
                double aspectRatio = (double) imageWidth / imageHeight;
                double panelAspectRatio = (double) panelWidth / panelHeight;
                
                if (aspectRatio > panelAspectRatio) {
                    scaledWidth = panelWidth * 0.9;
                    scaledHeight = scaledWidth / aspectRatio;
                } else {
                    scaledHeight = panelHeight * 0.9;
                    scaledWidth = scaledHeight * aspectRatio;
                }
                drawX = (panelWidth - scaledWidth) / 2.0;
                drawY = (panelHeight - scaledHeight) / 2.0;
                logger.info("修正後: 位置=({}, {}), サイズ={}x{}", (int)drawX, (int)drawY, (int)scaledWidth, (int)scaledHeight);
            }
            
            g2d.drawImage(processedImage, (int) drawX, (int) drawY, 
                         (int) scaledWidth, (int) scaledHeight, null);
            
            // クリッピング領域を復元
            g2d.setClip(oldClip);
            
            // オーバーレイ情報を表示
            drawOverlay(g2d);
        } else {
            // 画像が読み込まれていない場合のメッセージ
            g.setColor(Color.WHITE);
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
            g.setFont(font);
            String message = "DICOMファイルを開いてください";
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(message);
            int x = (getWidth() - textWidth) / 2;
            int y = getHeight() / 2;
            g.drawString(message, x, y);
        }
    }
    
    /**
     * オーバーレイ情報を描画
     */
    private void drawOverlay(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        String info = String.format("WL: %.1f / WW: %.1f | Zoom: %.1fx", 
                                    windowCenter, windowWidth, zoomFactor);
        g2d.drawString(info, 10, 20);
    }
    
    // Getters
    public double getWindowCenter() {
        return windowCenter;
    }
    
    public double getWindowWidth() {
        return windowWidth;
    }
    
    public double getZoomFactor() {
        return zoomFactor;
    }
}
