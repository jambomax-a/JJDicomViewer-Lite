package com.jj.dicomviewer.model;

import java.awt.Image;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * DicomImage - 画像（フレーム）情報を表すエンティティ
 * 
 * HOROS-20240407のDicomImageをJavaに移植
 * Core Dataの代わりにJPA/HibernateまたはSQLiteを使用
 */
public class DicomImage {
    
    public static final int OSIRIX_DICOM_IMAGE_SIZE_UNKNOWN = Integer.MAX_VALUE;
    
    // ========== インスタンスフィールド ==========
    private String completePathCache;
    private String sopInstanceUID;
    private Boolean inDatabaseFolder;
    private Integer height;
    private Integer width;
    private Integer numberOfFrames;
    private Integer numberOfSeries;
    private Boolean isKeyImage;
    private LocalDateTime dicomTime;
    private String extension;
    private String modality;
    private String fileType;
    private Image thumbnail;
    
    // プロパティ
    private String comment;
    private String comment2;
    private String comment3;
    private String comment4;
    private byte[] compressedSopInstanceUID;
    private LocalDateTime date;
    private Integer frameID;
    private Integer instanceNumber;
    private Boolean importedFile;
    private Integer pathNumber;
    private String pathString;
    private Double rotationAngle;
    private Double scale;
    private Double sliceLocation;
    private String stateText;
    private String storedExtension;
    private String storedFileType;
    private Integer storedHeight;
    private Boolean storedInDatabaseFolder;
    private Boolean storedIsKeyImage;
    private String storedModality;
    @Deprecated
    private Boolean storedMountedVolume;
    private Integer storedNumberOfFrames;
    private Integer storedNumberOfSeries;
    private Integer storedWidth;
    private Double windowLevel;
    private Double windowWidth;
    private Boolean xFlipped;
    private Double xOffset;
    private Boolean yFlipped;
    private Double yOffset;
    private Double zoom;
    
    // リレーション
    private DicomSeries series;
    
    // ========== コンストラクタ ==========
    
    public DicomImage() {
        // TODO: 初期化
    }
    
    // ========== 静的メソッド ==========
    
    /**
     * SOPInstanceUIDを文字列からエンコード
     */
    public static byte[] sopInstanceUIDEncodeString(String s) {
        // TODO: 実装
        return s != null ? s.getBytes() : null;
    }
    
    /**
     * オブジェクト配列からDicomImage配列を取得
     */
    public static List<DicomImage> dicomImagesInObjects(List<Object> objects) {
        // TODO: 実装
        List<DicomImage> result = new java.util.ArrayList<>();
        for (Object obj : objects) {
            if (obj instanceof DicomImage) {
                result.add((DicomImage) obj);
            }
        }
        return result;
    }
    
    // ========== インスタンスメソッド ==========
    
    /**
     * 画像ストレージかどうか
     */
    public Boolean isImageStorage() {
        // TODO: 実装
        return true;
    }
    
    /**
     * 一意のファイル名を取得
     */
    public String uniqueFilename() {
        // TODO: 実装
        return pathString != null ? pathString : "Image";
    }
    
    /**
     * パスセットを取得
     */
    public Set<String> paths() {
        // TODO: 実装
        return new java.util.HashSet<>();
    }
    
    /**
     * 完全パスを取得
     * HOROS-20240407準拠: [image valueForKey:@"completePath"]
     */
    public String completePath() {
        if (completePathCache != null && !completePathCache.isEmpty()) {
            return completePathCache;
        }
        // フォールバック: pathStringから構築
        if (pathString != null && !pathString.isEmpty()) {
            // pathStringが絶対パスの場合はそのまま返す
            java.io.File file = new java.io.File(pathString);
            if (file.isAbsolute()) {
                return pathString;
            }
            // 相対パスの場合は、現在のディレクトリから構築
            return new java.io.File(System.getProperty("user.dir"), pathString).getAbsolutePath();
        }
        return null;
    }
    
    /**
     * 完全パスを設定
     */
    public void setCompletePathCache(String path) {
        this.completePathCache = path;
    }
    
    /**
     * 完全パスを取得（getter形式）
     * HOROS-20240407: [image valueForKey:@"completePath"]
     */
    public String getCompletePath() {
        return completePath();
    }
    
    /**
     * 解決された完全パスを取得
     */
    public String completePathResolved() {
        // TODO: 実装
        return completePath();
    }
    
    /**
     * 画像を取得
     */
    public Image image() {
        // TODO: 実装
        return null;
    }
    
    /**
     * サムネイルを取得
     */
    public Image thumbnail() {
        return thumbnail;
    }
    
    /**
     * サムネイルを設定
     */
    public void setThumbnail(Image image) {
        this.thumbnail = image;
    }
    
    /**
     * 既に利用可能なサムネイルを取得
     */
    public Image thumbnailIfAlreadyAvailable() {
        return thumbnail;
    }
    
    /**
     * フレームのSRファイル名を取得
     */
    public String SRFilenameForFrame(int frameNo) {
        // TODO: 実装
        return null;
    }
    
    /**
     * フレームのSRパスを取得
     */
    public String SRPathForFrame(int frameNo) {
        // TODO: 実装
        return null;
    }
    
    /**
     * SRパスを取得
     */
    public String SRPath() {
        // TODO: 実装
        return null;
    }
    
    /**
     * SOPInstanceUIDを取得
     */
    public String sopInstanceUID() {
        return sopInstanceUID;
    }
    
    /**
     * パスを取得
     */
    public String path() {
        // TODO: 実装
        return pathString;
    }
    
    /**
     * 拡張子を取得
     */
    public String extension() {
        return extension;
    }
    
    /**
     * 高さを取得
     */
    public Integer height() {
        return height;
    }
    
    /**
     * 幅を取得
     */
    public Integer width() {
        return width;
    }
    
    /**
     * キー画像かどうか
     */
    public Boolean isKeyImage() {
        return isKeyImage;
    }
    
    /**
     * キー画像を設定
     */
    public void setIsKeyImage(Boolean keyImage) {
        this.isKeyImage = keyImage;
    }
    
    /**
     * ダウンロード付き完全パスを取得
     */
    public String completePathWithDownload(boolean download, boolean supportNonLocalDatabase) {
        // TODO: 実装
        return completePath();
    }
    
    /**
     * ローカルパスの完全パスを取得
     */
    public static String completePathForLocalPath(String path, String directory) {
        // TODO: 実装
        if (path == null) return null;
        if (directory != null && !directory.isEmpty()) {
            return directory + File.separator + path;
        }
        return path;
    }
    
    // ========== プロパティアクセサ ==========
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    public String getComment2() { return comment2; }
    public void setComment2(String comment2) { this.comment2 = comment2; }
    
    public String getComment3() { return comment3; }
    public void setComment3(String comment3) { this.comment3 = comment3; }
    
    public String getComment4() { return comment4; }
    public void setComment4(String comment4) { this.comment4 = comment4; }
    
    public byte[] getCompressedSopInstanceUID() { return compressedSopInstanceUID; }
    public void setCompressedSopInstanceUID(byte[] compressedSopInstanceUID) { this.compressedSopInstanceUID = compressedSopInstanceUID; }
    
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    
    public Integer getFrameID() { return frameID; }
    public void setFrameID(Integer frameID) { this.frameID = frameID; }
    
    public Integer getInstanceNumber() { return instanceNumber; }
    public void setInstanceNumber(Integer instanceNumber) { this.instanceNumber = instanceNumber; }
    
    public Boolean getImportedFile() { return importedFile; }
    public void setImportedFile(Boolean importedFile) { this.importedFile = importedFile; }
    
    public Integer getPathNumber() { return pathNumber; }
    public void setPathNumber(Integer pathNumber) { this.pathNumber = pathNumber; }
    
    public String getPathString() { return pathString; }
    public void setPathString(String pathString) { this.pathString = pathString; }
    
    public Double getRotationAngle() { return rotationAngle; }
    public void setRotationAngle(Double rotationAngle) { this.rotationAngle = rotationAngle; }
    
    public Double getScale() { return scale; }
    public void setScale(Double scale) { this.scale = scale; }
    
    public Double getSliceLocation() { return sliceLocation; }
    public void setSliceLocation(Double sliceLocation) { this.sliceLocation = sliceLocation; }
    
    public String getStateText() { return stateText; }
    public void setStateText(String stateText) { this.stateText = stateText; }
    
    public String getStoredExtension() { return storedExtension; }
    public void setStoredExtension(String storedExtension) { this.storedExtension = storedExtension; }
    
    public String getStoredFileType() { return storedFileType; }
    public void setStoredFileType(String storedFileType) { this.storedFileType = storedFileType; }
    
    public Integer getStoredHeight() { return storedHeight; }
    public void setStoredHeight(Integer storedHeight) { this.storedHeight = storedHeight; }
    
    public Boolean getStoredInDatabaseFolder() { return storedInDatabaseFolder; }
    public void setStoredInDatabaseFolder(Boolean storedInDatabaseFolder) { this.storedInDatabaseFolder = storedInDatabaseFolder; }
    
    public Boolean getStoredIsKeyImage() { return storedIsKeyImage; }
    public void setStoredIsKeyImage(Boolean storedIsKeyImage) { this.storedIsKeyImage = storedIsKeyImage; }
    
    public String getStoredModality() { return storedModality; }
    public void setStoredModality(String storedModality) { this.storedModality = storedModality; }
    
    @Deprecated
    public Boolean getStoredMountedVolume() { return storedMountedVolume; }
    @Deprecated
    public void setStoredMountedVolume(Boolean storedMountedVolume) { this.storedMountedVolume = storedMountedVolume; }
    
    public Integer getStoredNumberOfFrames() { return storedNumberOfFrames; }
    public void setStoredNumberOfFrames(Integer storedNumberOfFrames) { this.storedNumberOfFrames = storedNumberOfFrames; }
    
    public Integer getStoredNumberOfSeries() { return storedNumberOfSeries; }
    public void setStoredNumberOfSeries(Integer storedNumberOfSeries) { this.storedNumberOfSeries = storedNumberOfSeries; }
    
    public Integer getStoredWidth() { return storedWidth; }
    public void setStoredWidth(Integer storedWidth) { this.storedWidth = storedWidth; }
    
    public Double getWindowLevel() { return windowLevel; }
    public void setWindowLevel(Double windowLevel) { this.windowLevel = windowLevel; }
    
    public Double getWindowWidth() { return windowWidth; }
    public void setWindowWidth(Double windowWidth) { this.windowWidth = windowWidth; }
    
    public Boolean getXFlipped() { return xFlipped; }
    public void setXFlipped(Boolean xFlipped) { this.xFlipped = xFlipped; }
    
    public Double getXOffset() { return xOffset; }
    public void setXOffset(Double xOffset) { this.xOffset = xOffset; }
    
    public Boolean getYFlipped() { return yFlipped; }
    public void setYFlipped(Boolean yFlipped) { this.yFlipped = yFlipped; }
    
    public Double getYOffset() { return yOffset; }
    public void setYOffset(Double yOffset) { this.yOffset = yOffset; }
    
    public Double getZoom() { return zoom; }
    public void setZoom(Double zoom) { this.zoom = zoom; }
    
    public DicomSeries getSeries() { return series; }
    public void setSeries(DicomSeries series) { this.series = series; }
    
    /**
     * シリーズIDを取得
     * HOROS-20240407: [[image valueForKeyPath:@"series.id"] intValue]
     */
    public int getSeriesId() {
        if (series != null && series.getId() != null) {
            return series.getId();
        }
        return 0;
    }
    
    // 追加のプロパティ
    public Integer getNumberOfFrames() { return numberOfFrames; }
    public void setNumberOfFrames(Integer numberOfFrames) { this.numberOfFrames = numberOfFrames; }
    
    public String getModality() { return modality; }
    public void setModality(String modality) { this.modality = modality; }
    
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    
    public Boolean getInDatabaseFolder() { return inDatabaseFolder; }
    public void setInDatabaseFolder(Boolean inDatabaseFolder) { this.inDatabaseFolder = inDatabaseFolder; }
    
    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }
    
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    
    public String getSopInstanceUID() { return sopInstanceUID; }
    public void setSopInstanceUID(String sopInstanceUID) { this.sopInstanceUID = sopInstanceUID; }
    
    public LocalDateTime getDicomTime() { return dicomTime; }
    public void setDicomTime(LocalDateTime dicomTime) { this.dicomTime = dicomTime; }
    
    public Integer getNumberOfSeries() { return numberOfSeries; }
    public void setNumberOfSeries(Integer numberOfSeries) { this.numberOfSeries = numberOfSeries; }
}

