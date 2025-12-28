package com.jj.dicomviewer.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * DicomSeries - シリーズ情報を表すエンティティ
 * 
 * HOROS-20240407のDicomSeriesをJavaに移植
 * Core Dataの代わりにJPA/HibernateまたはSQLiteを使用
 */
public class DicomSeries {
    
    public static final int THUMBNAIL_SIZE = 70;
    
    // ========== インスタンスフィールド ==========
    private LocalDateTime dicomTime;
    
    // プロパティ
    private String comment;
    private String comment2;
    private String comment3;
    private String comment4;
    private LocalDateTime date;
    private LocalDateTime dateAdded;
    private LocalDateTime dateOpened;
    private Integer displayStyle;
    private Integer id;
    private String modality;
    @Deprecated
    private Boolean mountedVolume;
    private String name;
    private Integer numberOfImages;
    private Integer numberOfKeyImages;
    private Double rotationAngle;
    private Double scale;
    private String seriesDescription;
    private String seriesDICOMUID;
    private String seriesInstanceUID;
    private String seriesSOPClassUID;
    private Integer seriesNumber;
    private Integer stateText;
    private byte[] thumbnail;
    private Double windowLevel;
    private Double windowWidth;
    private Boolean xFlipped;
    private Double xOffset;
    private Boolean yFlipped;
    private Double yOffset;
    
    // リレーション
    private Set<DicomImage> images;
    private DicomStudy study;
    
    // ========== コンストラクタ ==========
    
    public DicomSeries() {
        // TODO: 初期化
    }
    
    // ========== インスタンスメソッド ==========
    
    /**
     * パスセットを取得
     */
    public Set<String> paths() {
        // TODO: 実装
        return new java.util.HashSet<>();
    }
    
    /**
     * キー画像セットを取得
     */
    public Set<DicomImage> keyImages() {
        // TODO: 実装
        return new java.util.HashSet<>();
    }
    
    /**
     * ソートされた画像配列を取得
     * HOROS-20240407: - (NSArray *)sortedImages
     */
    public List<DicomImage> sortedImages() {
        if (images == null || images.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        List<DicomImage> result = new java.util.ArrayList<>(images);
        
        // HOROS-20240407準拠: sortDescriptorsForImagesでソート
        // 簡易実装として、instanceNumberでソート
        result.sort((a, b) -> {
            Integer aInst = a.getInstanceNumber();
            Integer bInst = b.getInstanceNumber();
            if (aInst == null && bInst == null) return 0;
            if (aInst == null) return -1;
            if (bInst == null) return 1;
            return aInst.compareTo(bInst);
        });
        
        return result;
    }
    
    /**
     * タイプを取得
     * HOROS-20240407: - (NSString*) type
     */
    public String getType() {
        return "Series";
    }
    
    /**
     * 名前で比較
     */
    public int compareName(DicomSeries series) {
        // TODO: 実装
        if (name == null && series.name == null) return 0;
        if (name == null) return -1;
        if (series.name == null) return 1;
        return name.compareTo(series.name);
    }
    
    /**
     * マルチフレームを除くファイル数を取得
     */
    public Integer noFilesExcludingMultiFrames() {
        // TODO: 実装
        return numberOfImages;
    }
    
    /**
     * 生ファイル数を取得
     */
    /**
     * 生ファイル数を取得
     * HOROS-20240407準拠: DicomSeries.m 532-554行目
     */
    public Integer rawNoFiles() {
        // HOROS-20240407準拠: DicomSeries.m 539-544行目
        // 最初のImageのnumberOfFramesを確認
        if (images != null && !images.isEmpty()) {
            DicomImage firstImage = images.iterator().next();
            if (firstImage != null) {
                Integer numberOfFrames = firstImage.getNumberOfFrames();
                if (numberOfFrames != null && numberOfFrames.intValue() > 1) {
                    // HOROS-20240407準拠: DicomSeries.m 542行目
                    // numberOfFrames > 1 の場合は images.count - numberOfFrames + 1
                    return images.size() - numberOfFrames.intValue() + 1;
                } else {
                    // HOROS-20240407準拠: DicomSeries.m 544行目
                    // それ以外の場合は images.count
                    return images.size();
                }
            }
        }
        
        // 画像がない場合は0
        return 0;
    }
    
    /**
     * ファイル数を取得
     * HOROS-20240407準拠: DicomSeries.m のnoFilesメソッド
     */
    public Integer noFiles() {
        // HOROS-20240407準拠: DicomSeriesのnoFilesはnumberOfImagesを返す
        // またはrawNoFilesを返す（実装によって異なる）
        if (numberOfImages != null) {
            return numberOfImages;
        }
        // numberOfImagesがnullの場合はrawNoFilesを返す
        return rawNoFiles();
    }
    
    /**
     * 前のシリーズを取得
     */
    public DicomSeries previousSeries() {
        // TODO: 実装
        return null;
    }
    
    /**
     * 次のシリーズを取得
     */
    public DicomSeries nextSeries() {
        // TODO: 実装
        return null;
    }
    
    /**
     * 画像のソート記述子を取得
     */
    public List<Object> sortDescriptorsForImages() {
        // TODO: 実装
        return new java.util.ArrayList<>();
    }
    
    /**
     * 一意のファイル名を取得
     */
    public String uniqueFilename() {
        // TODO: 実装
        return name != null ? name : "Series";
    }
    
    // ========== プロパティアクセサ ==========
    
    public LocalDateTime getDicomTime() { return dicomTime; }
    public void setDicomTime(LocalDateTime dicomTime) { this.dicomTime = dicomTime; }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    public String getComment2() { return comment2; }
    public void setComment2(String comment2) { this.comment2 = comment2; }
    
    public String getComment3() { return comment3; }
    public void setComment3(String comment3) { this.comment3 = comment3; }
    
    public String getComment4() { return comment4; }
    public void setComment4(String comment4) { this.comment4 = comment4; }
    
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    
    public LocalDateTime getDateAdded() { return dateAdded; }
    public void setDateAdded(LocalDateTime dateAdded) { this.dateAdded = dateAdded; }
    
    public LocalDateTime getDateOpened() { return dateOpened; }
    public void setDateOpened(LocalDateTime dateOpened) { this.dateOpened = dateOpened; }
    
    public Integer getDisplayStyle() { return displayStyle; }
    public void setDisplayStyle(Integer displayStyle) { this.displayStyle = displayStyle; }
    
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getModality() { return modality; }
    public void setModality(String modality) { this.modality = modality; }
    
    @Deprecated
    public Boolean getMountedVolume() { return mountedVolume; }
    @Deprecated
    public void setMountedVolume(Boolean mountedVolume) { this.mountedVolume = mountedVolume; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Integer getNumberOfImages() { return numberOfImages; }
    public void setNumberOfImages(Integer numberOfImages) { this.numberOfImages = numberOfImages; }
    
    public Integer getNumberOfKeyImages() { return numberOfKeyImages; }
    public void setNumberOfKeyImages(Integer numberOfKeyImages) { this.numberOfKeyImages = numberOfKeyImages; }
    
    public Double getRotationAngle() { return rotationAngle; }
    public void setRotationAngle(Double rotationAngle) { this.rotationAngle = rotationAngle; }
    
    public Double getScale() { return scale; }
    public void setScale(Double scale) { this.scale = scale; }
    
    public String getSeriesDescription() { return seriesDescription; }
    public void setSeriesDescription(String seriesDescription) { this.seriesDescription = seriesDescription; }
    
    public String getSeriesDICOMUID() { return seriesDICOMUID; }
    public void setSeriesDICOMUID(String seriesDICOMUID) { this.seriesDICOMUID = seriesDICOMUID; }
    
    public String getSeriesInstanceUID() { return seriesInstanceUID; }
    public void setSeriesInstanceUID(String seriesInstanceUID) { this.seriesInstanceUID = seriesInstanceUID; }
    
    public String getSeriesSOPClassUID() { return seriesSOPClassUID; }
    public void setSeriesSOPClassUID(String seriesSOPClassUID) { this.seriesSOPClassUID = seriesSOPClassUID; }
    
    public Integer getSeriesNumber() { return seriesNumber; }
    public void setSeriesNumber(Integer seriesNumber) { this.seriesNumber = seriesNumber; }
    
    public Integer getStateText() { return stateText; }
    public void setStateText(Integer stateText) { this.stateText = stateText; }
    
    public byte[] getThumbnail() { return thumbnail; }
    public void setThumbnail(byte[] thumbnail) { this.thumbnail = thumbnail; }
    
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
    
    public Set<DicomImage> getImages() { return images; }
    public void setImages(Set<DicomImage> images) { this.images = images; }
    
    public DicomStudy getStudy() { return study; }
    public void setStudy(DicomStudy study) { this.study = study; }
    
    // ========== CoreDataGeneratedAccessors ==========
    
    /**
     * 画像オブジェクトを追加
     */
    public void addImagesObject(DicomImage value) {
        if (images == null) {
            images = new java.util.HashSet<>();
        }
        images.add(value);
        if (value != null) {
            value.setSeries(this);
        }
    }
    
    /**
     * 画像オブジェクトを削除
     */
    public void removeImagesObject(DicomImage value) {
        if (images != null) {
            images.remove(value);
        }
        if (value != null) {
            value.setSeries(null);
        }
    }
    
    /**
     * 画像セットを追加
     */
    public void addImages(Set<DicomImage> value) {
        if (images == null) {
            images = new java.util.HashSet<>();
        }
        images.addAll(value);
        for (DicomImage image : value) {
            if (image != null) {
                image.setSeries(this);
            }
        }
    }
    
    /**
     * 画像セットを削除
     */
    public void removeImages(Set<DicomImage> value) {
        if (images != null) {
            images.removeAll(value);
        }
        for (DicomImage image : value) {
            if (image != null) {
                image.setSeries(null);
            }
        }
    }
    
    /**
     * localstringを取得
     * HOROS-20240407準拠: DicomSeries.m 510-530行目
     * inDatabaseFolderがtrueの場合は"L"を返し、falseの場合は空文字列を返す
     */
    public String localstring() {
        boolean local = true;
        
        try {
            // HOROS-20240407準拠: 最初のImageのinDatabaseFolderをチェック
            if (images != null && !images.isEmpty()) {
                DicomImage firstImage = images.iterator().next();
                if (firstImage != null) {
                    Boolean inDatabaseFolder = firstImage.getInDatabaseFolder();
                    // HOROS-20240407準拠: inDatabaseFolderがtrueの場合はlocal=true、それ以外はfalse
                    local = (inDatabaseFolder != null && inDatabaseFolder.booleanValue());
                }
            }
        } catch (Exception e) {
            // エラーが発生した場合はデフォルト値（true）を使用
            local = true;
        }
        
        // HOROS-20240407準拠: localがtrueの場合は"L"を返し、falseの場合は空文字列を返す
        return local ? "L" : "";
    }
}

