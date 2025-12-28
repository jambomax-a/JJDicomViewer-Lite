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
     * HOROS-20240407準拠: DicomImage.m 1096-1099行目
     * - (NSString*) completePath { return [self completePathWithDownload: NO]; }
     */
    public String completePath() {
        return completePathWithDownload(false);
    }
    
    /**
     * ダウンロード付き完全パスを取得
     * HOROS-20240407準拠: - (NSString*) completePathWithDownload:(BOOL) download (1081行目)
     */
    public String completePathWithDownload(boolean download) {
        return completePathWithDownload(download, true);
    }
    
    /**
     * ダウンロード付き完全パスを取得（非ローカルDB対応）
     * HOROS-20240407準拠: - (NSString *)completePathWithDownload:(BOOL)download supportNonLocalDatabase:(BOOL)supportNonLocalDatabase (1024-1079行目)
     */
    public String completePathWithDownload(boolean download, boolean supportNonLocalDatabase) {
        try {
            // HOROS-20240407準拠: if(self.completePathCache && download == NO) return self.completePathCache; (1028-1029行目)
            if (completePathCache != null && !completePathCache.isEmpty() && !download) {
                return completePathCache;
            }
            
            // HOROS-20240407準拠: DicomDatabase* db = [DicomDatabase databaseForContext: self.managedObjectContext]; (1031行目)
            com.jj.dicomviewer.model.DicomDatabase db = getDatabase();
            
            // HOROS-20240407準拠: BOOL isLocal = YES; (1033行目)
            // HOROS-20240407準拠: if (supportNonLocalDatabase) isLocal = [db isLocal]; (1034-1035行目)
            boolean isLocal = true;
            if (supportNonLocalDatabase && db != null) {
                isLocal = db.isLocal();
            }
            
            // HOROS-20240407準拠: if (self.completePathCache) { ... } (1037-1042行目)
            if (completePathCache != null && !completePathCache.isEmpty()) {
                if (!download) {
                    return completePathCache;
                } else if (isLocal) {
                    return completePathCache;
                }
            }
            
            // HOROS-20240407準拠: #ifdef OSIRIX_VIEWER (1044行目)
            // HOROS-20240407準拠: if( [self.inDatabaseFolder boolValue] == YES) (1045行目)
            if (inDatabaseFolder != null && inDatabaseFolder && db != null) {
                // HOROS-20240407準拠: NSString *path = self.path; (1047行目)
                String pathValue = path();
                
                // HOROS-20240407準拠: if( !isLocal) { ... } (1049-1061行目)
                if (!isLocal) {
                    // HOROS-20240407準拠: NSString* temp = [DicomImage completePathForLocalPath:path directory:db.dataBaseDirPath]; (1051行目)
                    String temp = completePathForLocalPath(pathValue, db.getDataBaseDirPath());
                    // HOROS-20240407準拠: if ([[NSFileManager defaultManager] fileExistsAtPath:temp]) return temp; (1052-1053行目)
                    java.io.File tempFile = new java.io.File(temp);
                    if (tempFile.exists()) {
                        return temp;
                    }
                    
                    // HOROS-20240407準拠: if (download) self.completePathCache = [[(RemoteDicomDatabase*)db cacheDataForImage:self maxFiles:1] retain]; (1055-1056行目)
                    // HOROS-20240407準拠: else self.completePathCache = [[(RemoteDicomDatabase*)db localPathForImage:self] retain]; (1057-1058行目)
                    // HOROS-20240407準拠: return self.completePathCache; (1060行目)
                    // RemoteDicomDatabaseの処理（TODO: 実装が必要）
                    // 現在はtempを返す（HOROS-20240407準拠: RemoteDicomDatabaseが実装されていない場合）
                    completePathCache = temp;
                    return temp;
                } else {
                    // HOROS-20240407準拠: if( [path characterAtIndex: 0] != '/') (1064行目)
                    if (pathValue != null && !pathValue.isEmpty() && pathValue.charAt(0) != '/') {
                        // HOROS-20240407準拠: return (self.completePathCache = [[DicomImage completePathForLocalPath:path directory:db.dataBaseDirPath] retain]); (1066行目)
                        String completePath = completePathForLocalPath(pathValue, db.getDataBaseDirPath());
                        completePathCache = completePath;
                        return completePath;
                    }
                }
            }
            
            // HOROS-20240407準拠: return self.path; (1072行目)
            return path();
        } catch (Exception e) {
            // HOROS-20240407準拠: @catch (NSException *e) { N2LogExceptionWithStackTrace(e); } (1074-1076行目)
            // エラーログは出力しない（HOROS-20240407準拠）
            return path();
        }
    }
    
    /**
     * データベースを取得
     * HOROS-20240407準拠: DicomImage.m 1031行目
     * DicomDatabase* db = [DicomDatabase databaseForContext: self.managedObjectContext];
     */
    private com.jj.dicomviewer.model.DicomDatabase getDatabase() {
        // HOROS-20240407準拠: databaseForContextが失敗した場合はdefaultDatabaseを使用
        return com.jj.dicomviewer.model.DicomDatabase.defaultDatabase();
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
     * HOROS-20240407準拠: - (NSString*) path (970-977行目)
     */
    public String path() {
        // HOROS-20240407準拠: NSNumber *pathNumber = [self primitiveValueForKey: @"pathNumber"]; (972行目)
        // HOROS-20240407準拠: if( pathNumber) return [NSString stringWithFormat:@"%d.dcm", [pathNumber intValue]]; (974-975行目)
        if (pathNumber != null) {
            return String.format("%d.dcm", pathNumber);
        }
        // HOROS-20240407準拠: else return [self primitiveValueForKey: @"pathString"]; (976行目)
        return pathString;
    }
    
    /**
     * パスを設定
     * HOROS-20240407準拠: - (void) setPath:(NSString*) p (979-1005行目)
     */
    public void setPath(String p) {
        if (p == null || p.isEmpty()) {
            pathNumber = null;
            pathString = null;
            return;
        }
        
        // HOROS-20240407準拠: if( [p characterAtIndex: 0] != '/') (983行目)
        java.io.File pathFile = new java.io.File(p);
        if (!pathFile.isAbsolute()) {
            // HOROS-20240407準拠: if( [[p pathExtension] isEqualToString:@"dcm"]) (985行目)
            String extension = "";
            int lastDot = p.lastIndexOf('.');
            if (lastDot > 0 && lastDot < p.length() - 1) {
                extension = p.substring(lastDot + 1).toLowerCase();
            }
            
            if ("dcm".equals(extension)) {
                // HOROS-20240407準拠: [self setPrimitiveValue: [NSNumber numberWithInt: [p intValue]] forKey:@"pathNumber"]; (988行目)
                // HOROS-20240407準拠: intValueは文字列の先頭から数値を読み取り、数値でない文字が現れたら停止
                // 例: "0001" -> 1, "IM-0001-0001" -> 0 (先頭が数値でないため)
                // HOROS-20240407準拠: intValueが0でもpathNumberに0を設定する
                String pathWithoutExt = p.substring(0, lastDot);
                int intValue = 0;
                try {
                    // 先頭から数値を読み取る（HOROS-20240407準拠: intValueの動作）
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^\\s*(\\d+)");
                    java.util.regex.Matcher matcher = pattern.matcher(pathWithoutExt);
                    if (matcher.find()) {
                        intValue = Integer.parseInt(matcher.group(1));
                    }
                    // 数値が見つからない場合は0（HOROS-20240407準拠: intValueは0を返す）
                } catch (NumberFormatException e) {
                    intValue = 0;
                }
                
                // HOROS-20240407準拠: intValueが0でもpathNumberに設定する
                pathNumber = intValue;
                // HOROS-20240407準拠: [self setPrimitiveValue: nil forKey:@"pathString"]; (992行目)
                pathString = null;
                return;
            }
        }
        
        // HOROS-20240407準拠: [self setPrimitiveValue: nil forKey:@"pathNumber"]; (998-1000行目)
        // HOROS-20240407準拠: [self setPrimitiveValue: p forKey:@"pathString"]; (1002-1004行目)
        pathNumber = null;
        pathString = p;
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
     * ローカルパスの完全パスを取得
     * HOROS-20240407準拠: + (NSString*) completePathForLocalPath:(NSString*) path directory:(NSString*) directory (954-968行目)
     */
    public static String completePathForLocalPath(String path, String directory) {
        if (path == null || directory == null || directory.isEmpty()) {
            return path;
        }
        
        // HOROS-20240407準拠: if( [path characterAtIndex: 0] != '/') (956行目)
        java.io.File pathFile = new java.io.File(path);
        if (!pathFile.isAbsolute()) {
            // HOROS-20240407準拠: long val = [[path stringByDeletingPathExtension] intValue]; (958行目)
            // 拡張子を削除
            String pathWithoutExt = path;
            int lastDot = path.lastIndexOf('.');
            if (lastDot > 0) {
                pathWithoutExt = path.substring(0, lastDot);
            }
            
            // HOROS-20240407準拠: intValueは文字列の先頭から数値を読み取り、数値でない文字が現れたら停止
            // 例: "0001" -> 1, "IM-0001-0001" -> 0 (先頭が数値でないため)
            // しかし、HOROS-20240407では通常 "0001.dcm" のような形式なので、先頭から数値を読み取る
            long val = 0;
            try {
                // 先頭から数値を読み取る（HOROS-20240407準拠: intValueの動作）
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^\\s*(\\d+)");
                java.util.regex.Matcher matcher = pattern.matcher(pathWithoutExt);
                if (matcher.find()) {
                    val = Long.parseLong(matcher.group(1));
                } else {
                    // 数値が見つからない場合は0（HOROS-20240407準拠: intValueは0を返す）
                    val = 0;
                }
            } catch (NumberFormatException e) {
                val = 0;
            }
            
            // HOROS-20240407準拠: NSString *dbLocation = [directory stringByAppendingPathComponent: @"DATABASE.noindex"]; (959行目)
            String dbLocation = directory + File.separator + "DATABASE.noindex";
            
            // HOROS-20240407準拠: val /= [BrowserController DefaultFolderSizeForDB]; (961行目)
            // HOROS-20240407準拠: val++; (962行目)
            // HOROS-20240407準拠: val *= [BrowserController DefaultFolderSizeForDB]; (963行目)
            int defaultFolderSizeForDB = 10000; // HOROS-20240407準拠: BrowserController.m 535行目
            val = val / defaultFolderSizeForDB;
            val++;
            val = val * defaultFolderSizeForDB;
            
            // HOROS-20240407準拠: return [[dbLocation stringByAppendingPathComponent: [NSString stringWithFormat: @"%d", (int) val]] stringByAppendingPathComponent: path]; (965行目)
            return dbLocation + File.separator + val + File.separator + path;
        } else {
            // HOROS-20240407準拠: else return path; (967行目)
            return path;
        }
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

