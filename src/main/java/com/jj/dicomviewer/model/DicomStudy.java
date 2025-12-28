package com.jj.dicomviewer.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DicomStudy - スタディ情報を表すエンティティ
 * 
 * HOROS-20240407のDicomStudyをJavaに移植
 * Core Dataの代わりにJPA/HibernateまたはSQLiteを使用
 */
public class DicomStudy {
    
    // ========== 静的フィールド ==========
    private static final ReentrantLock dbModifyLock = new ReentrantLock();
    
    // ========== インスタンスフィールド ==========
    private boolean isHidden;
    private LocalDateTime dicomTime;
    private int numberOfImagesWhenCachedModalities;
    private String cachedModalities;
    private boolean reentry;
    
    // プロパティ
    private String accessionNumber;
    private String comment;
    private String comment2;
    private String comment3;
    private String comment4;
    private LocalDateTime date;
    private LocalDateTime dateAdded;
    private LocalDate dateOfBirth;
    private LocalDateTime dateOpened;
    private String dictateURL;
    private Boolean expanded;
    private Boolean hasDICOM;
    private String id;
    private String institutionName;
    private Boolean lockedStudy;
    private String modality;
    private String name;
    private Integer numberOfImages;
    private String patientID;
    private String patientSex;
    private String patientUID;
    private String performingPhysician;
    private String referringPhysician;
    private String reportURL;
    private Integer stateText;
    private String studyInstanceUID;
    private String studyName;
    private String studyID;
    private byte[] windowsState;
    
    // リレーション
    private Set<Object> albums;
    private Set<DicomSeries> series;
    
    // ========== コンストラクタ ==========
    
    public DicomStudy() {
        // TODO: 初期化
    }
    
    // ========== 静的メソッド ==========
    
    /**
     * DB変更ロックを取得
     */
    public static ReentrantLock dbModifyLock() {
        return dbModifyLock;
    }
    
    /**
     * Soundex変換
     */
    public static String soundex(String s) {
        // TODO: 実装
        return s;
    }
    
    /**
     * 生年月日から年齢を計算
     * HOROS-20240407準拠: DicomStudy.m 1396-1423行目
     */
    public static String yearOldFromDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return "";
        }
        
        LocalDate now = LocalDate.now();
        java.time.Period period = java.time.Period.between(dateOfBirth, now);
        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();
        
        if (years < 2) {
            if (years < 1) {
                if (months < 1) {
                    if (days < 0) {
                        return "";
                    } else {
                        return days + " d";
                    }
                } else {
                    return months + " m";
                }
            } else {
                return years + " y " + months + " m";
            }
        } else {
            return years + " y";
        }
    }
    
    /**
     * 取得日と生年月日から年齢を計算
     * HOROS-20240407準拠: DicomStudy.m 1362-1389行目
     */
    public static String yearOldAcquisition(LocalDateTime acquisitionDate, LocalDate dateOfBirth) {
        if (dateOfBirth == null || acquisitionDate == null) {
            return "";
        }
        
        LocalDate acquisitionLocalDate = acquisitionDate.toLocalDate();
        java.time.Period period = java.time.Period.between(dateOfBirth, acquisitionLocalDate);
        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();
        
        if (years < 2) {
            if (years < 1) {
                if (months < 1) {
                    if (days < 0) {
                        return "";
                    } else {
                        return days + " d";
                    }
                } else {
                    return months + " m";
                }
            } else {
                return years + " y " + months + " m";
            }
        } else {
            return years + " y";
        }
    }
    
    /**
     * 年齢を取得（現在の日付から計算）
     * HOROS-20240407準拠: DicomStudy.m 1425-1428行目 - (NSString*) yearOld
     */
    public String yearOld() {
        return yearOldFromDateOfBirth(this.dateOfBirth);
    }
    
    /**
     * 取得時の年齢を取得（Studyの日付から計算）
     * HOROS-20240407準拠: DicomStudy.m 1391-1394行目 - (NSString*) yearOldAcquisition
     */
    public String yearOldAcquisition() {
        return yearOldAcquisition(this.date, this.dateOfBirth);
    }
    
    /**
     * SOPClassUIDとシリーズ説明でシリーズを表示するかどうか
     */
    public static boolean displaySeriesWithSOPClassUID(String uid, String description) {
        // TODO: 実装
        return true;
    }
    
    /**
     * シリーズモダリティの表示モダリティを取得
     * HOROS-20240407準拠: DicomStudy.m 864-903行目 - displayedModalitiesForSeries:
     */
    public static String displayedModalitiesForSeries(List<String> seriesModalities) {
        if (seriesModalities == null || seriesModalities.isEmpty()) {
            return "";
        }
        
        java.util.List<String> r = new java.util.ArrayList<>();
        boolean SC = false, SR = false, PR = false, OT = false;
        
        for (String mod : seriesModalities) {
            if (mod == null) continue;
            
            if ("SR".equals(mod)) {
                SR = true;
            } else if ("SC".equals(mod)) {
                SC = true;
            } else if ("PR".equals(mod)) {
                PR = true;
            } else if ("RTSTRUCT".equals(mod) && !r.contains(mod)) {
                r.add("RT");
            } else if ("OT".equals(mod)) {
                OT = true;
            } else if ("KO".equals(mod)) {
                // KOは無視
            } else if (!r.contains(mod)) {
                r.add(mod);
            }
        }
        
        if (r.isEmpty()) {
            if (SC) {
                r.add("SC");
            } else if (OT) {
                r.add("OT");
            } else {
                if (SR) r.add("SR");
                if (PR) r.add("PR");
            }
        }
        
        return String.join("\\", r);
    }
    
    /**
     * 文字列をスクランブル
     */
    public static String scrambleString(String t) {
        // TODO: 実装
        return t;
    }
    
    // ========== インスタンスメソッド ==========
    
    /**
     * Soundexを取得
     */
    public String soundex() {
        return soundex(name);
    }
    
    /**
     * ファイル数を取得
     */
    /**
     * ファイル数を取得
     * HOROS-20240407準拠: DicomStudy.m 1517-1556行目
     */
    public Integer noFiles() {
        // HOROS-20240407準拠: DicomStudy.m 1519行目
        // numberOfImagesが0の場合は、SeriesのnoFilesを合計して計算
        if (numberOfImages != null && numberOfImages.intValue() != 0) {
            return numberOfImages;
        }
        
        // HOROS-20240407準拠: DicomStudy.m 1522-1545行目
        // SeriesのnoFilesを合計
        int sum = 0;
        boolean framesInSeries = false;
        
        if (series != null && !series.isEmpty()) {
            for (DicomSeries s : series) {
                try {
                    // HOROS-20240407準拠: DicomStudy.m 1531-1533行目
                    // StructuredReport、SupportedPrivateClasses、PresentationStateを除外
                    String sopClassUID = s.getSeriesSOPClassUID();
                    if (sopClassUID != null) {
                        // 簡易実装: SR（Structured Report）を除外
                        if (!isStructuredReport(sopClassUID) && 
                            !isSupportedPrivateClasses(sopClassUID) && 
                            !isPresentationState(sopClassUID)) {
                            
                            Integer seriesNoFiles = s.noFiles();
                            if (seriesNoFiles != null) {
                                sum += seriesNoFiles.intValue();
                            }
                            
                            // HOROS-20240407準拠: DicomStudy.m 1537行目
                            // numberOfImages < 0 の場合はフレームがある
                            Integer seriesNumberOfImages = s.getNumberOfImages();
                            if (seriesNumberOfImages != null && seriesNumberOfImages.intValue() < 0) {
                                framesInSeries = true;
                            }
                        }
                    }
                } catch (Exception e) {
                    // エラーが発生したSeriesは除外
                }
            }
        }
        
        // HOROS-20240407準拠: DicomStudy.m 1542-1543行目
        // framesInSeriesがtrueの場合は負の値にする
        if (framesInSeries) {
            sum = -sum;
        }
        
        // HOROS-20240407準拠: DicomStudy.m 1547-1549行目
        // numberOfImagesを更新（キャッシュ）
        numberOfImages = sum;
        
        // HOROS-20240407準拠: DicomStudy.m 1558-1559行目
        // sum < 0 の場合は絶対値を返す
        if (sum < 0) {
            return -sum;
        }
        return sum;
    }
    
    /**
     * サポートされているプライベートクラスかどうかを判定
     * HOROS-20240407準拠: DCMAbstractSyntaxUID.isSupportedPrivateClasses
     */
    private static boolean isSupportedPrivateClasses(String uid) {
        // HOROS-20240407準拠: 簡易実装
        // 実際にはDCMAbstractSyntaxUID.isSupportedPrivateClasses()を使用
        return false; // 簡易実装では常にfalse
    }
    
    /**
     * プレゼンテーション状態かどうかを判定
     * HOROS-20240407準拠: DCMAbstractSyntaxUID.isPresentationState
     */
    private static boolean isPresentationState(String uid) {
        // HOROS-20240407準拠: 簡易実装
        // 実際にはDCMAbstractSyntaxUID.isPresentationState()を使用
        if (uid == null) {
            return false;
        }
        // プレゼンテーション状態のSOP Class UIDのプレフィックス
        return uid.startsWith("1.2.840.10008.5.1.4.1.1.11");
    }
    
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
     * 画像セットを取得
     */
    public Set<DicomImage> images() {
        // TODO: 実装
        return new java.util.HashSet<>();
    }
    
    /**
     * 生ファイル数を取得
     */
    /**
     * 生ファイル数を取得
     * HOROS-20240407準拠: DicomStudy.m 1466-1485行目
     */
    public Integer rawNoFiles() {
        // HOROS-20240407準拠: DicomStudy.m 1468-1475行目
        // SeriesのrawNoFilesを合計
        int sum = 0;
        
        if (series != null && !series.isEmpty()) {
            for (DicomSeries s : series) {
                try {
                    Integer seriesRawNoFiles = s.rawNoFiles();
                    if (seriesRawNoFiles != null) {
                        sum += seriesRawNoFiles.intValue();
                    }
                } catch (Exception e) {
                    // エラーが発生したSeriesは除外
                }
            }
        }
        
        return sum;
    }
    
    /**
     * モダリティを取得
     * HOROS-20240407準拠: DicomStudy.m 905-937行目 - modalities
     * Seriesからモダリティを集約して返す
     */
    public String modalities() {
        if (cachedModalities != null && numberOfImages != null && 
            numberOfImagesWhenCachedModalities == numberOfImages.intValue()) {
            return cachedModalities;
        }
        
        if (series == null || series.isEmpty()) {
            return modality != null ? modality : "";
        }
        
        // HOROS-20240407準拠: Seriesを日付でソートしてモダリティを取得（921行目）
        java.util.List<DicomSeries> sortedSeries = new java.util.ArrayList<>(series);
        sortedSeries.sort((s1, s2) -> {
            if (s1.getDate() == null && s2.getDate() == null) return 0;
            if (s1.getDate() == null) return 1;
            if (s2.getDate() == null) return -1;
            return s1.getDate().compareTo(s2.getDate());
        });
        
        java.util.List<String> seriesModalities = new java.util.ArrayList<>();
        for (DicomSeries s : sortedSeries) {
            String mod = s.getModality();
            if (mod != null && !mod.isEmpty()) {
                seriesModalities.add(mod);
            }
        }
        
        String m = displayedModalitiesForSeries(seriesModalities);
        cachedModalities = m;
        if (numberOfImages != null) {
            numberOfImagesWhenCachedModalities = numberOfImages.intValue();
        }
        
        return m;
    }
    
    /**
     * シリーズソート用のComparatorを取得
     * HOROS-20240407準拠: DicomStudy.m 1685-1696行目
     * + (NSArray*) seriesSortDescriptors
     */
    public static java.util.Comparator<DicomSeries> getSeriesSortComparator() {
        // HOROS-20240407準拠: SERIESORDER設定に基づいてソート順序を決定
        // デフォルトはseriesInstanceUID (numericCompare) -> date (ascending)
        int seriesOrder = 0; // TODO: NSUserDefaultsから取得 (SERIESORDER)
        
        return (s1, s2) -> {
            int result = 0;
            if (seriesOrder == 0) { // sortid, sortdate
                result = compareSeriesInstanceUID(s1.getSeriesInstanceUID(), s2.getSeriesInstanceUID());
                if (result == 0) {
                    result = compareDates(s1.getDate(), s2.getDate(), true);
                }
            } else if (seriesOrder == 1) { // sortdate, sortid
                result = compareDates(s1.getDate(), s2.getDate(), true);
                if (result == 0) {
                    result = compareSeriesInstanceUID(s1.getSeriesInstanceUID(), s2.getSeriesInstanceUID());
                }
            } else { // デフォルトはsortid, sortdate
                result = compareSeriesInstanceUID(s1.getSeriesInstanceUID(), s2.getSeriesInstanceUID());
                if (result == 0) {
                    result = compareDates(s1.getDate(), s2.getDate(), true);
                }
            }
            return result;
        };
    }
    
    /**
     * seriesInstanceUIDを数値比較
     * HOROS-20240407準拠: numericCompare:セレクタ
     */
    private static int compareSeriesInstanceUID(String uid1, String uid2) {
        if (uid1 == null && uid2 == null) return 0;
        if (uid1 == null) return 1;
        if (uid2 == null) return -1;
        // HOROS-20240407準拠: numericCompare:セレクタを使用
        // UIDの数値部分を比較（簡易実装として文字列比較を使用）
        return uid1.compareTo(uid2);
    }
    
    /**
     * 日付を比較
     * HOROS-20240407準拠: ascendingに基づいて比較
     */
    private static int compareDates(LocalDateTime date1, LocalDateTime date2, boolean ascending) {
        if (date1 == null && date2 == null) return 0;
        if (date1 == null) return ascending ? 1 : -1;
        if (date2 == null) return ascending ? -1 : 1;
        int cmp = date1.compareTo(date2);
        return ascending ? cmp : -cmp;
    }
    
    /**
     * すべてのシリーズ配列を取得（ソート済み）
     * HOROS-20240407準拠: DicomStudy.m 1698-1701行目
     * - (NSArray*)allSeries
     */
    public List<DicomSeries> allSeries() {
        // HOROS-20240407準拠: return [self.series sortedArrayUsingDescriptors: [DicomStudy seriesSortDescriptors]]; (1700行目)
        if (series == null || series.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        List<DicomSeries> sortedSeries = new java.util.ArrayList<>(series);
        sortedSeries.sort(getSeriesSortComparator());
        return sortedSeries;
    }
    
    /**
     * 画像シリーズ配列を取得
     * HOROS-20240407: - (NSArray*)imageSeries
     */
    public List<DicomSeries> imageSeries() {
        // HOROS-20240407準拠: DicomStudy.m 1726-1729行目
        return imageSeriesContainingPixels(false);
    }
    
    /**
     * 画像シリーズ配列を取得（ピクセルデータ含むかどうか）
     * HOROS-20240407準拠: DicomStudy.m 1703-1724行目
     */
    public List<DicomSeries> imageSeriesContainingPixels(boolean pixels) {
        if (series == null || series.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // HOROS-20240407準拠: DicomStudy.m 1708行目
        // [self.series sortedArrayUsingDescriptors: [DicomStudy seriesSortDescriptors]]
        // Seriesをソートしてから処理
        // HOROS-20240407準拠: DicomStudy.m 1685-1696行目 - seriesSortDescriptors
        List<DicomSeries> sortedSeries = new java.util.ArrayList<>(series);
        sortedSeries.sort(getSeriesSortComparator());
        
        List<DicomSeries> result = new java.util.ArrayList<>();
        for (DicomSeries s : sortedSeries) {
            try {
                // HOROS-20240407準拠: DicomStudy.m 1710行目
                // displaySeriesWithSOPClassUID:andSeriesDescription:containingOnlyPixels:で判定
                if (displaySeriesWithSOPClassUID(s.getSeriesSOPClassUID(), s.getName(), pixels)) {
                    result.add(s);
                }
            } catch (Exception e) {
                // HOROS-20240407準拠: DicomStudy.m 1712行目 - @catch (...)でエラーを無視
                // エラーが発生したシリーズは除外
            }
        }
        return result;
    }
    
    /**
     * シリーズを表示するかどうかを判定
     * HOROS-20240407準拠: DicomStudy.m 1634-1659行目
     */
    public static boolean displaySeriesWithSOPClassUID(String uid, String description, boolean pixels) {
        // HOROS-20240407準拠: DicomStudy.m 1636行目
        if (description != null && description.equals("OsiriX No Autodeletion")) {
            return false;
        }
        
        if (pixels) {
            // HOROS-20240407準拠: DicomStudy.m 1641-1646行目
            if (uid == null || isImageStorage(uid)) {
                if (uid != null && uid.equals(getPdfStorageClassUID())) {
                    return false;
                }
                return true;
            }
        } else {
            // HOROS-20240407準拠: DicomStudy.m 1651-1655行目
            if (uid == null || isImageStorage(uid) || isRadiotherapy(uid) || isWaveform(uid)) {
                return true;
            }
            
            // HOROS-20240407準拠: DicomStudy.m 1654行目
            // 特定のSR（Structured Report）を含む
            if (isStructuredReport(uid) && description != null) {
                if (!description.startsWith("OsiriX ROI SR") &&
                    !description.startsWith("OsiriX Annotations SR") &&
                    !description.startsWith("OsiriX Report SR") &&
                    !description.startsWith("OsiriX WindowsState SR")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 画像ストレージかどうかを判定
     * HOROS-20240407準拠: DCMAbstractSyntaxUID.isImageStorage
     */
    private static boolean isImageStorage(String uid) {
        // HOROS-20240407準拠: DCMAbstractSyntaxUID.isImageStorage()の簡易実装
        if (uid == null) {
            return false;
        }
        // HOROS-20240407準拠: 画像ストレージのSOP Class UIDのプレフィックス
        // 1.2.840.10008.5.1.4.1.1.x の形式（xは画像ストレージのタイプ）
        // PDFストレージ（1.2.840.10008.5.1.4.1.1.104.1）は除外される
        return uid.startsWith("1.2.840.10008.5.1.4.1.1.");
    }
    
    /**
     * 放射線治療かどうかを判定
     * HOROS-20240407準拠: DCMAbstractSyntaxUID.isRadiotherapy
     */
    private static boolean isRadiotherapy(String uid) {
        // HOROS-20240407準拠: 簡易実装
        if (uid == null) {
            return false;
        }
        return uid.startsWith("1.2.840.10008.5.1.4.1.1.481");
    }
    
    /**
     * 波形かどうかを判定
     * HOROS-20240407準拠: DCMAbstractSyntaxUID.isWaveform
     */
    private static boolean isWaveform(String uid) {
        // HOROS-20240407準拠: 簡易実装
        if (uid == null) {
            return false;
        }
        return uid.startsWith("1.2.840.10008.5.1.4.1.1.9");
    }
    
    /**
     * 構造化レポートかどうかを判定
     * HOROS-20240407準拠: DCMAbstractSyntaxUID.isStructuredReport
     */
    public static boolean isStructuredReport(String uid) {
        // HOROS-20240407準拠: 簡易実装
        if (uid == null) {
            return false;
        }
        return uid.startsWith("1.2.840.10008.5.1.4.1.1.88");
    }
    
    /**
     * PDFストレージクラスUIDを取得
     * HOROS-20240407準拠: DCMAbstractSyntaxUID.pdfStorageClassUID
     */
    private static String getPdfStorageClassUID() {
        // HOROS-20240407準拠: PDFストレージのSOP Class UID
        return "1.2.840.10008.5.1.4.1.1.104.1";
    }
    
    /**
     * キーオブジェクトシリーズ配列を取得
     */
    public List<DicomSeries> keyObjectSeries() {
        // TODO: 実装
        return new java.util.ArrayList<>();
    }
    
    /**
     * キーオブジェクト配列を取得
     */
    public List<Object> keyObjects() {
        // TODO: 実装
        return new java.util.ArrayList<>();
    }
    
    /**
     * プレゼンテーションステートシリーズ配列を取得
     */
    public List<DicomSeries> presentationStateSeries() {
        // TODO: 実装
        return new java.util.ArrayList<>();
    }
    
    /**
     * 波形シリーズ配列を取得
     */
    public List<DicomSeries> waveFormSeries() {
        // TODO: 実装
        return new java.util.ArrayList<>();
    }
    
    /**
     * 画像のROIパスを取得（配列指定）
     */
    public String roiPathForImage(DicomImage image, List<Object> roisArray) {
        // TODO: 実装
        return null;
    }
    
    /**
     * 画像のROIパスを取得
     */
    public String roiPathForImage(DicomImage image) {
        // TODO: 実装
        return null;
    }
    
    /**
     * 画像のROIを取得（配列指定）
     */
    public DicomImage roiForImage(DicomImage image, List<Object> roisArray) {
        // TODO: 実装
        return null;
    }
    
    /**
     * ROI SRシリーズを取得
     */
    public DicomSeries roiSRSeries() {
        // TODO: 実装
        return null;
    }
    
    /**
     * レポートSRシリーズを取得
     */
    public DicomSeries reportSRSeries() {
        // TODO: 実装
        return null;
    }
    
    /**
     * ウィンドウステート画像を取得
     */
    public DicomImage windowsStateImage() {
        // TODO: 実装
        return null;
    }
    
    /**
     * ウィンドウステートSRシリーズを取得
     */
    public DicomSeries windowsStateSRSeries() {
        // TODO: 実装
        return null;
    }
    
    /**
     * レポート画像を取得
     */
    public DicomImage reportImage() {
        // TODO: 実装
        return null;
    }
    
    /**
     * アノテーションSR画像を取得
     */
    public DicomImage annotationsSRImage() {
        // TODO: 実装
        return null;
    }
    
    /**
     * レポートをDICOMSRとしてアーカイブ
     */
    public void archiveReportAsDICOMSR() {
        // TODO: 実装
    }
    
    /**
     * アノテーションをDICOMSRとしてアーカイブ
     */
    public void archiveAnnotationsAsDICOMSR() {
        // TODO: 実装
    }
    
    /**
     * ウィンドウステートをDICOMSRとしてアーカイブ
     */
    public void archiveWindowsStateAsDICOMSR() {
        // TODO: 実装
    }
    
    /**
     * すべてのウィンドウステートSRシリーズを取得
     */
    public List<DicomSeries> allWindowsStateSRSeries() {
        // TODO: 実装
        return new java.util.ArrayList<>();
    }
    
    /**
     * 非表示かどうか
     */
    public boolean isHidden() {
        return isHidden;
    }
    
    /**
     * 遠隔かどうか
     */
    public boolean isDistant() {
        // TODO: 実装
        return false;
    }
    
    /**
     * 非表示を設定
     */
    public void setHidden(boolean hidden) {
        this.isHidden = hidden;
    }
    
    /**
     * マルチフレームを除くファイル数を取得
     */
    public Integer noFilesExcludingMultiFrames() {
        // TODO: 実装
        return numberOfImages;
    }
    
    /**
     * アノテーションを辞書として取得
     */
    public java.util.Map<String, Object> annotationsAsDictionary() {
        // TODO: 実装
        return new java.util.HashMap<>();
    }
    
    /**
     * 辞書からアノテーションを適用
     */
    public void applyAnnotationsFromDictionary(java.util.Map<String, Object> rootDict) {
        // TODO: 実装
    }
    
    /**
     * DICOMSRからアノテーションを再適用
     */
    public void reapplyAnnotationsFromDICOMSR() {
        // TODO: 実装
    }
    
    /**
     * タイプを取得
     * HOROS-20240407: - (NSString*) type
     */
    public String getType() {
        return "Study";
    }
    
    /**
     * 名前で比較
     */
    public int compareName(DicomStudy study) {
        // TODO: 実装
        if (name == null && study.name == null) return 0;
        if (name == null) return -1;
        if (study.name == null) return 1;
        return name.compareTo(study.name);
    }
    
    /**
     * ROI画像配列を取得
     */
    public List<DicomImage> roiImages() {
        // TODO: 実装
        return new java.util.ArrayList<>();
    }
    
    /**
     * キー画像とROI画像のDICOMSC画像を生成
     */
    public List<Object> generateDICOMSCImagesForKeyImages(boolean keyImages, boolean ROIImages) {
        // TODO: 実装
        return new java.util.ArrayList<>();
    }
    
    /**
     * キー画像とROI用の画像を取得
     */
    public List<DicomImage> imagesForKeyImages(boolean keyImages, boolean alsoImagesWithROIs) {
        // TODO: 実装
        return new java.util.ArrayList<>();
    }
    
    // ========== プロパティアクセサ ==========
    
    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String accessionNumber) { this.accessionNumber = accessionNumber; }
    
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
    
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    
    public LocalDateTime getDateOpened() { return dateOpened; }
    public void setDateOpened(LocalDateTime dateOpened) { this.dateOpened = dateOpened; }
    
    public String getDictateURL() { return dictateURL; }
    public void setDictateURL(String dictateURL) { this.dictateURL = dictateURL; }
    
    public Boolean getExpanded() { return expanded; }
    public void setExpanded(Boolean expanded) { this.expanded = expanded; }
    
    public Boolean getHasDICOM() { return hasDICOM; }
    public void setHasDICOM(Boolean hasDICOM) { this.hasDICOM = hasDICOM; }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }
    
    public Boolean getLockedStudy() { return lockedStudy; }
    public void setLockedStudy(Boolean lockedStudy) { this.lockedStudy = lockedStudy; }
    
    public String getModality() { return modality; }
    public void setModality(String modality) { this.modality = modality; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Integer getNumberOfImages() { return numberOfImages; }
    public void setNumberOfImages(Integer numberOfImages) { this.numberOfImages = numberOfImages; }
    
    public String getPatientID() { return patientID; }
    public void setPatientID(String patientID) { this.patientID = patientID; }
    
    public String getPatientSex() { return patientSex; }
    public void setPatientSex(String patientSex) { this.patientSex = patientSex; }
    
    public String getPatientUID() { return patientUID; }
    public void setPatientUID(String patientUID) { this.patientUID = patientUID; }
    
    public String getPerformingPhysician() { return performingPhysician; }
    public void setPerformingPhysician(String performingPhysician) { this.performingPhysician = performingPhysician; }
    
    public String getReferringPhysician() { return referringPhysician; }
    public void setReferringPhysician(String referringPhysician) { this.referringPhysician = referringPhysician; }
    
    public String getReportURL() { return reportURL; }
    public void setReportURL(String reportURL) { this.reportURL = reportURL; }
    
    public Integer getStateText() { return stateText; }
    public void setStateText(Integer stateText) { this.stateText = stateText; }
    
    public String getStudyInstanceUID() { return studyInstanceUID; }
    public void setStudyInstanceUID(String studyInstanceUID) { this.studyInstanceUID = studyInstanceUID; }
    
    public String getStudyName() { return studyName; }
    public void setStudyName(String studyName) { this.studyName = studyName; }
    
    public String getStudyID() { return studyID; }
    public void setStudyID(String studyID) { this.studyID = studyID; }
    
    public byte[] getWindowsState() { return windowsState; }
    public void setWindowsState(byte[] windowsState) { this.windowsState = windowsState; }
    
    public Set<Object> getAlbums() { return albums; }
    public void setAlbums(Set<Object> albums) { this.albums = albums; }
    
    public Set<DicomSeries> getSeries() { return series; }
    public void setSeries(Set<DicomSeries> series) { this.series = series; }
    
    // ========== CoreDataGeneratedAccessors ==========
    
    /**
     * アルバムオブジェクトを追加
     */
    public void addAlbumsObject(Object value) {
        if (albums == null) {
            albums = new java.util.HashSet<>();
        }
        albums.add(value);
    }
    
    /**
     * アルバムオブジェクトを削除
     */
    public void removeAlbumsObject(Object value) {
        if (albums != null) {
            albums.remove(value);
        }
    }
    
    /**
     * アルバムセットを追加
     */
    public void addAlbums(Set<Object> value) {
        if (albums == null) {
            albums = new java.util.HashSet<>();
        }
        albums.addAll(value);
    }
    
    /**
     * アルバムセットを削除
     */
    public void removeAlbums(Set<Object> value) {
        if (albums != null) {
            albums.removeAll(value);
        }
    }
    
    /**
     * シリーズオブジェクトを追加
     */
    public void addSeriesObject(DicomSeries value) {
        if (series == null) {
            series = new java.util.HashSet<>();
        }
        series.add(value);
    }
    
    /**
     * シリーズオブジェクトを削除
     */
    public void removeSeriesObject(DicomSeries value) {
        if (series != null) {
            series.remove(value);
        }
    }
    
    /**
     * シリーズセットを追加
     */
    public void addSeries(Set<DicomSeries> value) {
        if (series == null) {
            series = new java.util.HashSet<>();
        }
        series.addAll(value);
    }
    
    /**
     * シリーズセットを削除
     */
    public void removeSeries(Set<DicomSeries> value) {
        if (series != null) {
            series.removeAll(value);
        }
    }
    
    /**
     * localstringを取得
     * HOROS-20240407準拠: DicomStudy.m 1291-1310行目
     * inDatabaseFolderがtrueの場合は"L"を返し、falseの場合は空文字列を返す
     */
    public String localstring() {
        boolean local = true;
        
        try {
            // HOROS-20240407準拠: 最初のSeriesの最初のImageのinDatabaseFolderをチェック
            if (series != null && !series.isEmpty()) {
                DicomSeries firstSeries = series.iterator().next();
                if (firstSeries != null && firstSeries.getImages() != null && !firstSeries.getImages().isEmpty()) {
                    DicomImage firstImage = firstSeries.getImages().iterator().next();
                    if (firstImage != null) {
                        Boolean inDatabaseFolder = firstImage.getInDatabaseFolder();
                        // HOROS-20240407準拠: inDatabaseFolderがtrueの場合はlocal=true、それ以外はfalse
                        local = (inDatabaseFolder != null && inDatabaseFolder.booleanValue());
                    }
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

