package com.jj.dicomviewer.model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReadParam;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DicomPix - DICOM画像データ
 * 
 * HOROS-20240407のDCMPixをJava Swingに移植
 * Weasisのアプローチを参考にdcm4che3を使用して画像を読み込む
 */
public class DicomPix {
    
    private static final Logger logger = LoggerFactory.getLogger(DicomPix.class);
    
    private Path path;
    private int imageIndex;
    private int numberOfImages;
    private int frameNumber;
    private int seriesId;
    private boolean isBonjour;
    private DicomImage imageObj;
    
    // 画像データ
    private BufferedImage bufferedImage;
    private BufferedImage thumbnailImage;
    
    // Window Level/Width
    private float windowLevel = 0.0f;
    private float windowWidth = 0.0f;
    
    // 画像サイズ
    private int width;
    private int height;
    
    // HOROS-20240407準拠: DCMPix.h 106行目 - pixelSpacingX, pixelSpacingY, pixelRatio
    private double pixelSpacingX = 0.0;
    private double pixelSpacingY = 0.0;
    private double pixelRatio = 1.0;
    
    // HOROS-20240407準拠: DCMPix.h 108行目 - pixelSpacingFromUltrasoundRegions
    private boolean pixelSpacingFromUltrasoundRegions = false;
    
    // HOROS-20240407準拠: DCMPix.h 175行目 - sliceThickness, sliceLocation
    private double sliceThickness = 0.0;
    private double sliceLocation = 0.0;
    
    // HOROS-20240407準拠: DCMPix.h 93行目 - orientation[9]
    // Image Orientation (Patient) (0020,0037) - 6つの値（行方向ベクトル3つ、列方向ベクトル3つ）
    private double[] orientation = new double[9]; // デフォルトは単位行列
    
    // HOROS-20240407準拠: Transfer Syntax UID (0002,0010)
    // 圧縮形式の判定に使用
    private String transferSyntaxUID = null;
    
    // HOROS-20240407準拠: DCMPix.m 3655行目 - modalityString
    // Modality (0008,0060) - モダリティ（CT, MR, CR, USなど）
    private String modality = null;
    
    /**
     * コンストラクタ
     * HOROS-20240407: - (id)initWithPath:(NSString*)path :(short)imageNb :(short)numberOfImages :(float*)fVolumePtr :(short)frameNo :(int)serieNo isBonjour:(BOOL)isBonjour imageObj:(DicomImage*)imageObj
     */
    public DicomPix(String path, int imageIndex, int numberOfImages, 
                    int frameNumber, int seriesId, boolean isBonjour, DicomImage imageObj) {
        if (path != null) {
            this.path = Paths.get(path);
        } else {
            this.path = null;
            logger.warn("DicomPix constructor: path is null");
        }
        this.imageIndex = imageIndex;
        this.numberOfImages = numberOfImages;
        this.frameNumber = frameNumber;
        this.seriesId = seriesId;
        this.isBonjour = isBonjour;
        this.imageObj = imageObj;
        
        // TODO: 実際の画像読み込み処理を実装
        // Weasisのアプローチを参考に、dcm4che3で画像を読み込む
    }
    
    /**
     * コンストラクタ（DicomImageから）
     * HOROS-20240407準拠
     */
    public DicomPix(DicomImage imageObj) {
        this.imageObj = imageObj;
        this.imageIndex = 0;
        this.numberOfImages = 1;
        this.frameNumber = 0;
        this.seriesId = 0;
        this.isBonjour = false;
        
        if (imageObj != null) {
            String pathStr = imageObj.completePath();
            if (pathStr != null) {
                this.path = Paths.get(pathStr);
            }
            this.frameNumber = (imageObj.getFrameID() != null) ? imageObj.getFrameID() : 0;
            this.seriesId = imageObj.getSeriesId();
        }
    }
    
    /**
     * コンストラクタ（DicomImage、フレーム番号、画像数から）
     * HOROS-20240407準拠
     */
    public DicomPix(DicomImage imageObj, int frameNumber, int numberOfImages) {
        this.imageObj = imageObj;
        this.frameNumber = frameNumber;
        this.numberOfImages = numberOfImages;
        this.imageIndex = frameNumber;
        this.seriesId = 0;
        this.isBonjour = false;
        
        if (imageObj != null) {
            String pathStr = imageObj.completePath();
            if (pathStr != null) {
                this.path = Paths.get(pathStr);
            }
            this.frameNumber = frameNumber;
            this.seriesId = imageObj.getSeriesId();
        }
    }
    
    /**
     * パスを取得
     */
    public Path getPath() {
        return path;
    }
    
    /**
     * 画像インデックスを取得
     */
    public int getImageIndex() {
        return imageIndex;
    }
    
    /**
     * フレーム番号を取得
     */
    public int getFrameNumber() {
        return frameNumber;
    }
    
    /**
     * シリーズIDを取得
     */
    public int getSeriesId() {
        return seriesId;
    }
    
    /**
     * HOROS-20240407準拠: srcFileプロパティを取得
     * [curDCM srcFile]
     */
    public String getSrcFile() {
        if (path != null) {
            return path.toString();
        }
        return null;
    }
    
    /**
     * HOROS-20240407準拠: frameNoプロパティを取得
     * [curDCM frameNo]
     */
    public Integer getFrameNo() {
        return frameNumber;
    }
    
    /**
     * HOROS-20240407準拠: serieNoプロパティを取得
     * [curDCM serieNo]
     */
    public Integer getSerieNo() {
        return seriesId;
    }
    
    /**
     * 画像を読み込む
     * Weasisのアプローチを参考に実装
     * dcm4che3のImageIO APIを使用してBufferedImageを取得
     */
    public void loadImage() {
        if (bufferedImage != null) {
            return; // 既に読み込み済み
        }
        
        if (path == null) {
            logger.warn("loadImage: path is null");
            return;
        }
        
        if (!path.toFile().exists()) {
            logger.warn("loadImage: DICOM file not found: {}", path);
            return;
        }
        
        logger.info("loadImage: Loading DICOM image from {}", path);
        
        try {
            // Weasisのアプローチを参考に、dcm4che3のImageIO APIを使用
            ImageReader reader = null;
            ImageInputStream iis = null;
            
            try {
                // ImageIOプラグインをスキャン（JPEG圧縮DICOM画像を読み込むために必要）
                ImageIO.scanForPlugins();
                
                // HOROS-20240407準拠: DicomImageReaderは自動的にJPEG圧縮画像を処理する
                // dcm4che3のDicomImageReaderは、Transfer Syntaxに基づいて適切なデコンプレッサーを選択する
                // そのため、jpeg-cvフォーマットを直接使用する必要はない
                File dicomFile = path.toFile();
                
                // DICOM属性を読み込む（Transfer Syntax、Pixel Spacingなど）
                org.dcm4che3.data.Attributes attrs = null;
                try {
                    org.dcm4che3.io.DicomInputStream dis = new org.dcm4che3.io.DicomInputStream(dicomFile);
                    attrs = dis.readDataset(-1, -1);
                    dis.close();
                    
                    // Transfer Syntaxを確認（デバッグ用）
                    String transferSyntaxUID = attrs.getString(org.dcm4che3.data.Tag.TransferSyntaxUID);
                    if (transferSyntaxUID != null) {
                        this.transferSyntaxUID = transferSyntaxUID;
                        logger.debug("Transfer Syntax UID: {}", transferSyntaxUID);
                        // JPEG圧縮のTransfer Syntax UIDを確認
                        if (transferSyntaxUID.equals("1.2.840.10008.1.2.4.50") ||  // JPEG Baseline
                            transferSyntaxUID.equals("1.2.840.10008.1.2.4.51") ||  // JPEG Extended
                            transferSyntaxUID.equals("1.2.840.10008.1.2.4.70") ||  // JPEG Lossless
                            transferSyntaxUID.equals("1.2.840.10008.1.2.4.57") ||  // JPEG Lossless 14
                            transferSyntaxUID.equals("1.2.840.10008.1.2.4.90") ||  // JPEG2000 Lossless
                            transferSyntaxUID.equals("1.2.840.10008.1.2.4.91")) {  // JPEG2000 Lossy
                            logger.info("JPEG compressed DICOM image detected (Transfer Syntax: {})", transferSyntaxUID);
                        }
                    }
                    
                    // HOROS-20240407準拠: DCMPix.m 3655行目 - modalityString
                    // Modality (0008,0060)を読み込む
                    String modality = attrs.getString(org.dcm4che3.data.Tag.Modality);
                    if (modality != null) {
                        this.modality = modality;
                        logger.debug("Modality: {}", modality);
                    }
                    
                    // HOROS-20240407準拠: Pixel Spacingを読み込む
                    // DCMPix.m 5655-5682行目 - pixelSpacingX, pixelSpacingYの読み込み処理
                    // HOROS-20240407準拠: DCMPix.m 5655行目 - if( pixelSpacingFromUltrasoundRegions == NO)
                    if (!pixelSpacingFromUltrasoundRegions) {
                        double[] pixelSpacing = attrs.getDoubles(org.dcm4che3.data.Tag.PixelSpacing);
                        if (pixelSpacing != null && pixelSpacing.length >= 2) {
                            this.pixelSpacingY = pixelSpacing[0]; // DICOMでは行間隔（Row Spacing）が最初
                            this.pixelSpacingX = pixelSpacing[1]; // 列間隔（Column Spacing）が2番目
                            logger.debug("Pixel Spacing: X={}, Y={}", pixelSpacingX, pixelSpacingY);
                        } else if (pixelSpacing != null && pixelSpacing.length >= 1) {
                            // HOROS-20240407準拠: DCMPix.m 5663-5667行目
                            this.pixelSpacingY = pixelSpacing[0];
                            this.pixelSpacingX = pixelSpacing[0];
                            logger.debug("Pixel Spacing (single value): X={}, Y={}", pixelSpacingX, pixelSpacingY);
                        } else {
                            // Pixel Spacingが存在しない場合は、Imager Pixel Spacingを試す
                            double[] imagerPixelSpacing = attrs.getDoubles(org.dcm4che3.data.Tag.ImagerPixelSpacing);
                            if (imagerPixelSpacing != null && imagerPixelSpacing.length >= 2) {
                                this.pixelSpacingY = imagerPixelSpacing[0];
                                this.pixelSpacingX = imagerPixelSpacing[1];
                                logger.debug("Imager Pixel Spacing: X={}, Y={}", pixelSpacingX, pixelSpacingY);
                            } else if (imagerPixelSpacing != null && imagerPixelSpacing.length >= 1) {
                                // HOROS-20240407準拠: DCMPix.m 5676-5680行目
                                this.pixelSpacingY = imagerPixelSpacing[0];
                                this.pixelSpacingX = imagerPixelSpacing[0];
                                logger.debug("Imager Pixel Spacing (single value): X={}, Y={}", pixelSpacingX, pixelSpacingY);
                            }
                            
                            // HOROS-20240407準拠: CR画像の場合、DetectorElementSpacingからpixelSpacingを計算
                            // PixelSpacingとImagerPixelSpacingが存在しない場合、DetectorElementSpacingを試す
                            // DICOM規格: DetectorElementSpacing (0018,7020) = 0x00187020
                            if (pixelSpacingX == 0.0 || pixelSpacingY == 0.0) {
                                double[] detectorElementSpacing = attrs.getDoubles(0x00187020); // DetectorElementSpacing (0018,7020)
                                if (detectorElementSpacing != null && detectorElementSpacing.length >= 2) {
                                    this.pixelSpacingY = detectorElementSpacing[0];
                                    this.pixelSpacingX = detectorElementSpacing[1];
                                    logger.debug("Detector Element Spacing: X={}, Y={}", pixelSpacingX, pixelSpacingY);
                                } else if (detectorElementSpacing != null && detectorElementSpacing.length >= 1) {
                                    this.pixelSpacingY = detectorElementSpacing[0];
                                    this.pixelSpacingX = detectorElementSpacing[0];
                                    logger.debug("Detector Element Spacing (single value): X={}, Y={}", pixelSpacingX, pixelSpacingY);
                                }
                            }
                        }
                    }
                    
                    // HOROS-20240407準拠: DCMPix.m 5684-5765行目 - SequenceOfUltrasoundRegionsからpixelSpacingを計算
                    // US画像の場合、PhysicalDeltaXとPhysicalDeltaYからpixelSpacingXとpixelSpacingYを計算
                    org.dcm4che3.data.Sequence seq = attrs.getSequence(org.dcm4che3.data.Tag.SequenceOfUltrasoundRegions);
                    if (seq != null && !seq.isEmpty()) {
                        // HOROS-20240407準拠: DCMPix.m 5748-5756行目
                        // 最初の有効なUS RegionからPhysicalDeltaXとPhysicalDeltaYを取得
                        for (org.dcm4che3.data.Attributes seqItem : seq) {
                            int physicalUnitsX = seqItem.getInt(org.dcm4che3.data.Tag.PhysicalUnitsXDirection, 0);
                            int physicalUnitsY = seqItem.getInt(org.dcm4che3.data.Tag.PhysicalUnitsYDirection, 0);
                            int regionSpatialFormat = seqItem.getInt(org.dcm4che3.data.Tag.RegionSpatialFormat, 0);
                            
                            // HOROS-20240407準拠: DCMPix.m 5748行目
                            // physicalUnitsX == 3 (cm), physicalUnitsY == 3 (cm), regionSpatialFormat == 1 (2D)
                            if (physicalUnitsX == 3 && physicalUnitsY == 3 && regionSpatialFormat == 1) {
                                double physicalDeltaX = seqItem.getDouble(org.dcm4che3.data.Tag.PhysicalDeltaX, 0.0);
                                double physicalDeltaY = seqItem.getDouble(org.dcm4che3.data.Tag.PhysicalDeltaY, 0.0);
                                
                                if (physicalDeltaX != 0.0 && physicalDeltaY != 0.0) {
                                    // HOROS-20240407準拠: DCMPix.m 5752-5753行目
                                    // These are in cm, so multiply by 10 to convert to mm
                                    this.pixelSpacingX = Math.abs(physicalDeltaX) * 10.0;
                                    this.pixelSpacingY = Math.abs(physicalDeltaY) * 10.0;
                                    this.pixelSpacingFromUltrasoundRegions = true; // HOROS-20240407準拠: DCMPix.m 5754行目
                                    logger.debug("Pixel Spacing from Ultrasound Regions: X={}, Y={}", pixelSpacingX, pixelSpacingY);
                                    break; // 最初の有効なUS Regionを使用
                                }
                            }
                        }
                    }
                    
                    // HOROS-20240407準拠: Pixel Aspect Ratioを読み込む
                    // DCMPix.m 5767-5786行目 - pixelRatioの計算
                    // HOROS-20240407準拠: DCMPix.m 5768行目 - if( pixelSpacingFromUltrasoundRegions == NO)
                    if (!pixelSpacingFromUltrasoundRegions) {
                        // HOROS-20240407準拠: DCMPix.m 5770-5781行目 - PixelAspectRatioタグから読み込む
                        double[] pixelAspectRatio = attrs.getDoubles(org.dcm4che3.data.Tag.PixelAspectRatio);
                        if (pixelAspectRatio != null && pixelAspectRatio.length >= 2) {
                            double ratiox = pixelAspectRatio[0];
                            double ratioy = pixelAspectRatio[1];
                            if (ratioy != 0.0) {
                                this.pixelRatio = ratiox / ratioy;
                                logger.debug("Pixel Ratio from PixelAspectRatio: {}", pixelRatio);
                            }
                        } else if (pixelSpacingX != pixelSpacingY) {
                            // HOROS-20240407準拠: DCMPix.m 5782-5785行目
                            if (pixelSpacingY != 0.0 && pixelSpacingX != 0.0) {
                                this.pixelRatio = pixelSpacingY / pixelSpacingX;
                                logger.debug("Pixel Ratio from PixelSpacing: {}", pixelRatio);
                            }
                        }
                    }
                    // pixelSpacingFromUltrasoundRegions == YESの場合は、pixelRatioは既に計算されている（またはデフォルト値）
                    if (pixelRatio == 1.0 && pixelSpacingX != 0.0 && pixelSpacingY != 0.0) {
                        this.pixelRatio = pixelSpacingY / pixelSpacingX;
                        logger.debug("Pixel Ratio (fallback): {}", pixelRatio);
                    }
                    
                    // HOROS-20240407準拠: DCMPix.m 6866-6869行目 - EstimatedRadiographicMagnificationFactorでpixelSpacingを補正
                    // CR画像の場合、EstimatedRadiographicMagnificationFactorが設定されている場合、pixelSpacingを補正
                    double estimatedRadiographicMagnificationFactor = attrs.getDouble(org.dcm4che3.data.Tag.EstimatedRadiographicMagnificationFactor, 0.0);
                    if (estimatedRadiographicMagnificationFactor > 0.0 && pixelSpacingX != 0.0 && pixelSpacingY != 0.0) {
                        // HOROS-20240407準拠: DCMPix.m 6868-6869行目
                        pixelSpacingX /= estimatedRadiographicMagnificationFactor;
                        pixelSpacingY /= estimatedRadiographicMagnificationFactor;
                        logger.debug("Pixel Spacing corrected by EstimatedRadiographicMagnificationFactor ({}): X={}, Y={}", 
                                estimatedRadiographicMagnificationFactor, pixelSpacingX, pixelSpacingY);
                    }
                    
                    // HOROS-20240407準拠: Slice Thicknessを読み込む
                    // DCMPix.m - sliceThicknessの読み込み処理
                    double sliceThicknessValue = attrs.getDouble(org.dcm4che3.data.Tag.SliceThickness, 0.0);
                    if (sliceThicknessValue > 0.0) {
                        this.sliceThickness = sliceThicknessValue;
                        logger.debug("Slice Thickness: {}", sliceThickness);
                    }
                    
                    // HOROS-20240407準拠: Slice Locationを読み込む
                    // DCMPix.m - sliceLocationの読み込み処理
                    double sliceLocationValue = attrs.getDouble(org.dcm4che3.data.Tag.SliceLocation, 0.0);
                    if (sliceLocationValue != 0.0) {
                        this.sliceLocation = sliceLocationValue;
                        logger.debug("Slice Location: {}", sliceLocation);
                    }
                    
                    // HOROS-20240407準拠: Image Orientation (Patient)を読み込む
                    // DCMPix.m - orientation[9]の読み込み処理
                    // Image Orientation (Patient) (0020,0037) - 6つの値（行方向ベクトル3つ、列方向ベクトル3つ）
                    double[] imageOrientation = attrs.getDoubles(org.dcm4che3.data.Tag.ImageOrientationPatient);
                    if (imageOrientation != null && imageOrientation.length >= 6) {
                        // 行方向ベクトル（最初の3つ）
                        this.orientation[0] = imageOrientation[0];
                        this.orientation[1] = imageOrientation[1];
                        this.orientation[2] = imageOrientation[2];
                        // 列方向ベクトル（次の3つ）
                        this.orientation[3] = imageOrientation[3];
                        this.orientation[4] = imageOrientation[4];
                        this.orientation[5] = imageOrientation[5];
                        // 法線ベクトル（外積で計算）
                        this.orientation[6] = this.orientation[1] * this.orientation[5] - this.orientation[2] * this.orientation[4];
                        this.orientation[7] = this.orientation[2] * this.orientation[3] - this.orientation[0] * this.orientation[5];
                        this.orientation[8] = this.orientation[0] * this.orientation[4] - this.orientation[1] * this.orientation[3];
                        logger.debug("Image Orientation: [{}, {}, {}, {}, {}, {}]", 
                            orientation[0], orientation[1], orientation[2], 
                            orientation[3], orientation[4], orientation[5]);
                    } else {
                        // デフォルト値（単位行列）
                        this.orientation[0] = 1.0; this.orientation[1] = 0.0; this.orientation[2] = 0.0;
                        this.orientation[3] = 0.0; this.orientation[4] = 1.0; this.orientation[5] = 0.0;
                        this.orientation[6] = 0.0; this.orientation[7] = 0.0; this.orientation[8] = 1.0;
                    }
                } catch (Exception e) {
                    logger.debug("Could not read DICOM attributes: {}", e.getMessage());
                }
                
                // DICOMフォーマット名でImageReaderを取得
                // DicomImageReaderは内部的にJPEG圧縮画像を処理する
                java.util.Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("DICOM");
                if (readers.hasNext()) {
                    reader = readers.next();
                    logger.debug("Using ImageReader: {}", reader.getClass().getName());
                } else {
                    // フォールバック: DicomImageReaderを直接作成
                    reader = new DicomImageReader(null);
                    logger.debug("Using DicomImageReader directly (no ImageReader found via ImageIO)");
                }
                
                // ファイルを開く
                iis = ImageIO.createImageInputStream(dicomFile);
                reader.setInput(iis);
                
                // DicomImageReadParamを作成
                DicomImageReadParam param = new DicomImageReadParam();
                
                // Window Level/Widthを設定（Weasis方式）
                if (windowLevel != 0.0f || windowWidth != 0.0f) {
                    param.setWindowCenter(windowLevel);
                    param.setWindowWidth(windowWidth);
                }
                
                // フレーム番号を設定
                int frameIndex = frameNumber >= 0 ? frameNumber : 0;
                
                // 画像を読み込む
                // DicomImageReaderは内部的にTransfer Syntaxを確認し、適切なデコンプレッサーを使用する
                // NativeImageReaderが失敗した場合、例外がスローされるため、フォールバック処理を追加
                try {
                    logger.debug("Attempting to read frame {} from DICOM file", frameIndex);
                    bufferedImage = reader.read(frameIndex, param);
                    if (bufferedImage != null) {
                        logger.debug("Successfully read DICOM image: {}x{}", bufferedImage.getWidth(), bufferedImage.getHeight());
                    } else {
                        logger.warn("DicomImageReader.read() returned null for frame {}", frameIndex);
                    }
                } catch (RuntimeException e) {
                    logger.error("RuntimeException while reading DICOM image from {}: {}", path, e.getMessage(), e);
                    // NativeImageReaderが失敗した場合（OpenCVネイティブライブラリが見つからない、またはdicomJpgFileReadメソッドが存在しないなど）
                    String errorMsg = e.getMessage();
                    Throwable cause = e.getCause();
                    boolean isOpenCVError = false;
                    
                    // OpenCV関連のエラーをチェック
                    if (errorMsg != null && (errorMsg.contains("opencv") || errorMsg.contains("opencv_java") || 
                        errorMsg.contains("No stream adaptor") || errorMsg.contains("StreamSegment") ||
                        errorMsg.contains("No Reader for format: jpeg-cv") ||
                        errorMsg.contains("dicomJpgFileRead") || errorMsg.contains("UnsatisfiedLinkError"))) {
                        isOpenCVError = true;
                    }
                    if (cause != null && (cause instanceof UnsatisfiedLinkError || 
                        (cause.getMessage() != null && cause.getMessage().contains("dicomJpgFileRead")))) {
                        isOpenCVError = true;
                    }
                    
                    if (isOpenCVError) {
                        logger.warn("NativeImageReader failed (OpenCV native method not found or inaccessible). " +
                                "This may be due to missing dicomJpgFileRead method in OpenCV native library. File: {}", path);
                        logger.warn("Note: The OpenCV native library may not have the custom dicomJpgFileRead method. " +
                                "JPEG compressed DICOM images may not be readable without a properly built OpenCV library.");
                        // DicomImageReaderは内部的にJava実装のJPEGデコンプレッサーにフォールバックする可能性があるが、
                        // 実際にはNativeImageReaderが失敗すると、JPEG圧縮画像を読み込むことは困難
                        throw new UnsupportedOperationException("JPEG compressed DICOM images require OpenCV native library " +
                                "with dicomJpgFileRead method. The current OpenCV library may not support this method.", e);
                    } else {
                        throw e;
                    }
                }
                
                if (bufferedImage != null) {
                    width = bufferedImage.getWidth();
                    height = bufferedImage.getHeight();
                    logger.debug("Loaded DICOM image: {}x{} from {}", width, height, path);
                } else {
                    logger.warn("Failed to read DICOM image from {}", path);
                }
                
            } finally {
                // リソースを解放
                if (reader != null) {
                    reader.dispose();
                }
                if (iis != null) {
                    try {
                        iis.close();
                    } catch (IOException e) {
                        logger.warn("Failed to close ImageInputStream", e);
                    }
                }
            }
            
        } catch (RuntimeException e) {
            // HOROS-20240407準拠: JPEG圧縮画像の読み込みエラーを処理
            // DicomImageReaderが内部的にNativeImageReaderを使用しようとして失敗する場合がある
            // これは、OpenCVネイティブライブラリが見つからない場合に発生する
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("opencv") || errorMsg.contains("opencv_java") || 
                    errorMsg.contains("No stream adaptor") || errorMsg.contains("StreamSegment")) {
                    logger.warn("JPEG compressed DICOM image cannot be loaded: OpenCV native library not found or inaccessible. File: {}", path);
                    logger.debug("Error details: {}", errorMsg);
                } else if (errorMsg.contains("jpeg-cv")) {
                    logger.debug("JPEG compressed DICOM image (jpeg-cv) cannot be loaded without dcm4che-imageio-opencv: {}", path);
                } else {
                    logger.error("Error loading DICOM image from {}", path, e);
                }
            } else {
                logger.error("Error loading DICOM image from {}: {}", path, e.getClass().getSimpleName(), e);
            }
            bufferedImage = null;
        } catch (Exception e) {
            // その他の例外
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("opencv") || errorMsg.contains("opencv_java") || 
                errorMsg.contains("No stream adaptor") || errorMsg.contains("StreamSegment"))) {
                logger.warn("JPEG compressed DICOM image cannot be loaded: OpenCV native library not found or inaccessible. File: {}", path);
                logger.debug("Error details: {}", errorMsg);
            } else {
                logger.error("Error loading DICOM image from {}", path, e);
            }
            bufferedImage = null;
        }
    }
    
    /**
     * サムネイル画像を生成
     * HOROS-20240407準拠で実装
     */
    public BufferedImage generateThumbnail(int maxWidth, int maxHeight) {
        if (bufferedImage == null) {
            loadImage();
        }
        
        if (bufferedImage == null) {
            return null;
        }
        
        int srcWidth = bufferedImage.getWidth();
        int srcHeight = bufferedImage.getHeight();
        
        // アスペクト比を維持してリサイズ
        double scaleX = (double) maxWidth / srcWidth;
        double scaleY = (double) maxHeight / srcHeight;
        double scale = Math.min(scaleX, scaleY);
        
        int thumbWidth = (int) (srcWidth * scale);
        int thumbHeight = (int) (srcHeight * scale);
        
        // サムネイル画像を作成
        BufferedImage thumbnail = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = thumbnail.createGraphics();
        try {
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, 
                                java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
                                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(bufferedImage, 0, 0, thumbWidth, thumbHeight, null);
        } finally {
            g2d.dispose();
        }
        
        return thumbnail;
    }
    
    /**
     * Window Level/Widthを設定
     * HOROS-20240407: - (void)setWLWW:(float)wl :(float)ww
     */
    public void setWindowLevelWidth(float wl, float ww) {
        if (this.windowLevel == wl && this.windowWidth == ww) {
            return; // 変更なし
        }
        
        this.windowLevel = wl;
        this.windowWidth = ww;
        
        // 画像を再読み込み（Window Level/Widthを適用）
        bufferedImage = null;
        thumbnailImage = null;
        loadImage();
    }
    
    /**
     * Window Level/Widthを取得
     */
    public void getWindowLevelWidth(float[] wlww) {
        if (wlww != null && wlww.length >= 2) {
            wlww[0] = windowLevel;
            wlww[1] = windowWidth;
        }
    }
    
    /**
     * Window Levelを取得
     */
    public float getWindowLevel() {
        return windowLevel;
    }
    
    /**
     * Window Widthを取得
     */
    public float getWindowWidth() {
        return windowWidth;
    }
    
    /**
     * 保存されたWindow Levelを取得
     * HOROS-20240407準拠: savedWL
     */
    public float getSavedWL() {
        return windowLevel;
    }
    
    /**
     * 保存されたWindow Widthを取得
     * HOROS-20240407準拠: savedWW
     */
    public float getSavedWW() {
        return windowWidth;
    }
    
    /**
     * Window Levelを設定
     */
    public void setWindowLevel(float wl) {
        if (this.windowLevel != wl) {
            this.windowLevel = wl;
            bufferedImage = null;
        }
    }
    
    /**
     * Window Widthを設定
     */
    public void setWindowWidth(float ww) {
        if (this.windowWidth != ww) {
            this.windowWidth = ww;
            bufferedImage = null;
        }
    }
    
    /**
     * WL/WWを変更して画像を更新
     * HOROS-20240407準拠: - (void) changeWLWW:(float)newWL :(float)newWW
     */
    public void changeWLWW(float newWL, float newWW) {
        setWindowLevelWidth(newWL, newWW);
    }
    
    /**
     * BufferedImageを取得
     */
    public BufferedImage getBufferedImage() {
        if (bufferedImage == null) {
            loadImage();
        }
        return bufferedImage;
    }
    
    /**
     * 指定座標のピクセル値を取得
     * HOROS-20240407準拠: DCMPix.m 9191-9258行目 - (float) getPixelValueX: (long) x Y:(long) y
     */
    public float getPixelValueX(int x, int y) {
        float val = 0.0f;
        
        // HOROS-20240407準拠: DCMPix.m 9195-9196行目
        if (x < 0 || x >= width || y < 0 || y >= height) return 0.0f;
        
        BufferedImage img = getBufferedImage();
        if (img == null) return 0.0f;
        
        try {
            // HOROS-20240407準拠: DCMPix.m 9248-9254行目
            // if( isRGB == NO) val = fImage[ x + (y * width)];
            // else { unsigned char *rgbPtr = (unsigned char*) (&fImage[ x + (y * width)]); val = (rgbPtr[ 1] + rgbPtr[ 2] + rgbPtr[ 3])/3.; }
            int rgb = img.getRGB(x, y);
            
            // RGBから輝度値を計算（R, G, Bの平均）
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            
            val = (r + g + b) / 3.0f;
        } catch (Exception e) {
            logger.error("getPixelValueX: Error getting pixel value at ({}, {}): {}", x, y, e.getMessage());
            val = 0.0f;
        }
        
        return val;
    }
    
    /**
     * サムネイル画像を取得
     */
    public BufferedImage getThumbnailImage() {
        if (thumbnailImage == null) {
            logger.debug("getThumbnailImage: Generating thumbnail for {}", path);
            thumbnailImage = generateThumbnail(105, 113); // BrowserMatrixのセルサイズに合わせる
            if (thumbnailImage == null) {
                logger.warn("getThumbnailImage: Failed to generate thumbnail for {}", path);
            } else {
                logger.debug("getThumbnailImage: Generated thumbnail {}x{} for {}", 
                           thumbnailImage.getWidth(), thumbnailImage.getHeight(), path);
            }
        }
        return thumbnailImage;
    }
    
    /**
     * 画像の幅を取得
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * 画像の高さを取得
     */
    public int getHeight() {
        return height;
    }
    
    // HOROS-20240407準拠: DCMPix.h 263行目 - pixelSpacingX, pixelSpacingY
    public double getPixelSpacingX() {
        return pixelSpacingX;
    }
    
    public void setPixelSpacingX(double pixelSpacingX) {
        this.pixelSpacingX = pixelSpacingX;
    }
    
    public double getPixelSpacingY() {
        return pixelSpacingY;
    }
    
    public void setPixelSpacingY(double pixelSpacingY) {
        this.pixelSpacingY = pixelSpacingY;
    }
    
    // HOROS-20240407準拠: DCMPix.h 260行目 - pixelRatio
    public double getPixelRatio() {
        return pixelRatio;
    }
    
    public void setPixelRatio(double pixelRatio) {
        this.pixelRatio = pixelRatio;
    }
    
    /**
     * Slice Thicknessを取得
     * HOROS-20240407準拠: DCMPix.h 283行目 - @property double sliceThickness;
     */
    public double getSliceThickness() {
        return sliceThickness;
    }
    
    /**
     * Slice Locationを取得
     * HOROS-20240407準拠: DCMPix.h 281行目 - @property double sliceLocation;
     */
    public double getSliceLocation() {
        return sliceLocation;
    }
    
    /**
     * Image Orientationを取得
     * HOROS-20240407準拠: DCMPix.h 93行目 - double orientation[9];
     */
    public double[] getOrientation() {
        return orientation.clone(); // コピーを返す
    }
    
    /**
     * Transfer Syntax UIDを取得
     * HOROS-20240407準拠: Transfer Syntax UID (0002,0010)
     */
    public String getTransferSyntaxUID() {
        return transferSyntaxUID;
    }
    
    /**
     * Modalityを取得
     * HOROS-20240407準拠: DCMPix.m 3655行目 - modalityString
     */
    public String getModality() {
        return modality;
    }
    
    // HOROS-20240407準拠: DCMPix.h - pwidth, pheight
    public int getPwidth() {
        return width;
    }
    
    public int getPheight() {
        return height;
    }
    
    /**
     * 8-bit画像をクリア（メモリ節約）
     * HOROS-20240407: - (void)kill8bitsImage
     */
    public void kill8bitsImage() {
        // TODO: 8-bit画像をクリアしてメモリを節約
        bufferedImage = null;
        thumbnailImage = null;
    }
    
    /**
     * 画像を元に戻す
     * HOROS-20240407: - (void)revert:(BOOL)NO
     */
    public void revert(boolean reset) {
        // TODO: 画像を元の状態に戻す
        if (reset) {
            kill8bitsImage();
        }
    }
}

