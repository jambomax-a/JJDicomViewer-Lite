package com.jjdicomviewer.dicom;

import com.jjdicomviewer.core.Instance;
import com.jjdicomviewer.core.Series;
import com.jjdicomviewer.core.Study;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * DICOMファイルを読み込むクラス
 */
public class DicomReader {
    
    private static final Logger logger = LoggerFactory.getLogger(DicomReader.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    
    /**
     * 安全に文字列を取得（文字エンコーディングエラーが発生した場合はバイト列からデコードを試みる）
     * 日本語を含む可能性が高いフィールドについては、getString()で成功しても検証を行う
     */
    private String safeGetString(Attributes attrs, int tag, String tagName) {
        // 日本語を含む可能性が高いフィールドのタグ一覧
        int[] japaneseLikelyTags = {
            Tag.PatientName,
            Tag.StudyDescription,
            Tag.SeriesDescription,
            Tag.ReferringPhysicianName,
            Tag.BodyPartExamined,
            Tag.PatientPosition,
            Tag.InstitutionName,
            Tag.InstitutionalDepartmentName,
            Tag.PerformingPhysicianName
        };
        
        // 日本語を含む可能性が高いフィールドかどうかを判定
        boolean isJapaneseLikely = false;
        for (int japaneseTag : japaneseLikelyTags) {
            if (tag == japaneseTag) {
                isJapaneseLikely = true;
                break;
            }
        }
        
        // まず通常の取得方法を試す
        String stringResult = null;
        try {
            stringResult = attrs.getString(tag);
        } catch (AssertionError e) {
            // AssertionErrorはErrorのサブクラスなので別途キャッチ
            Throwable cause = e.getCause();
            if (cause instanceof java.io.UnsupportedEncodingException || 
                e.getMessage() != null && e.getMessage().contains("UnsupportedEncodingException")) {
                logger.debug("文字エンコーディングエラーにより{}のデコードに失敗、バイト列から取得を試みます: {}", tagName, e.getMessage());
                return getStringFromBytes(attrs, tag, tagName);
            }
            // その他のAssertionErrorは再スロー
            throw e;
        } catch (Exception e) {
            // 文字エンコーディングエラー（UnsupportedEncodingException等）をキャッチ
            if (e instanceof java.io.UnsupportedEncodingException || 
                (e.getCause() != null && e.getCause() instanceof java.io.UnsupportedEncodingException)) {
                logger.debug("文字エンコーディングエラーにより{}のデコードに失敗、バイト列から取得を試みます: {}", tagName, e.getMessage());
                return getStringFromBytes(attrs, tag, tagName);
            }
            // その他のエラーは再スロー
            throw e;
        }
        
        // 日本語を含む可能性が高いフィールドの場合、結果を検証
        if (isJapaneseLikely && stringResult != null && !stringResult.isEmpty()) {
            // 文字化けの可能性があるかチェック
            if (mightBeCorrupted(stringResult)) {
                logger.debug("{}で文字化けの可能性を検出、バイト列から再取得を試みます: {}", tagName, stringResult);
                String byteResult = getStringFromBytes(attrs, tag, tagName);
                if (byteResult != null && !byteResult.isEmpty() && !mightBeCorrupted(byteResult)) {
                    return byteResult;
                }
            }
            
            // 日本語を含む可能性があるのに、日本語文字が含まれていない場合は、
            // バイト列からも取得して比較する
            if (!containsJapanese(stringResult)) {
                // バイト列から取得を試みる
                String byteResult = getStringFromBytes(attrs, tag, tagName);
                if (byteResult != null && !byteResult.isEmpty()) {
                    // バイト列からの結果に日本語が含まれている場合、それを優先
                    if (containsJapanese(byteResult) && !mightBeCorrupted(byteResult)) {
                        logger.debug("{}で日本語が検出されました（バイト列から）: {}", tagName, byteResult);
                        return byteResult;
                    }
                    // 両方とも日本語を含まない場合は、元の結果を返す
                }
            }
        }
        
        return stringResult;
    }
    
    /**
     * 文字列に日本語文字（ひらがな、カタカナ、漢字）が含まれているかチェック
     */
    private boolean containsJapanese(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            // ひらがな（\u3040-\u309F）、カタカナ（\u30A0-\u30FF）、漢字（\u4E00-\u9FAF）
            if ((c >= '\u3040' && c <= '\u309F') ||  // ひらがな
                (c >= '\u30A0' && c <= '\u30FF') ||  // カタカナ
                (c >= '\u4E00' && c <= '\u9FAF')) {  // 漢字
                return true;
            }
        }
        return false;
    }
    
    /**
     * 文字列が文字化けしている可能性があるかチェック
     */
    private boolean mightBeCorrupted(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        // 疑問符や制御文字が多く含まれている場合は文字化けの可能性
        int suspiciousCharCount = 0;
        for (char c : str.toCharArray()) {
            // 疑問符、置換文字（）、制御文字（改行・タブ以外）が含まれている場合
            if (c == '?' || c == '\ufffd' || (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t')) {
                suspiciousCharCount++;
            }
        }
        
        // 文字列の30%以上が疑問符や制御文字の場合、文字化けの可能性が高い
        return suspiciousCharCount > str.length() * 0.3;
    }
    
    /**
     * バイト列から文字列を取得（フォールバック用）
     * 日本語の文字コード（Shift-JIS、UTF-8、ISO 2022 IR 87など）をサポート
     */
    private String getStringFromBytes(Attributes attrs, int tag, String tagName) {
        try {
            byte[] bytes = attrs.getBytes(tag);
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            
            // DICOMファイルから文字コードセットを取得（可能であれば）
            // SpecificCharacterSetタグはバイト列から取得する（再帰呼び出しを避けるため、直接取得）
            String charsetName = null;
            try {
                byte[] charsetBytes = attrs.getBytes(Tag.SpecificCharacterSet);
                if (charsetBytes != null && charsetBytes.length > 0) {
                    // 複数の文字コードセットで試行してSpecificCharacterSetを取得
                    String specificCharacterSet = null;
                    String[] charsetOptions = {"UTF-8", "Shift_JIS", "MS932", "ISO-8859-1"};
                    for (String charset : charsetOptions) {
                        try {
                            String test = new String(charsetBytes, charset);
                            if (isValidString(test)) {
                                specificCharacterSet = test;
                                break;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                    
                    if (specificCharacterSet != null && !specificCharacterSet.isEmpty()) {
                        // SpecificCharacterSetタグを解析（複数の文字コードセットが指定されている場合も考慮）
                        // 例: "ISO_IR 100\\ISO 2022 IR 87" のような形式
                        String[] charsets = specificCharacterSet.split("[\\\\/]");
                        for (String cs : charsets) {
                            cs = cs.trim();
                            if (cs.contains("ISO 2022 IR 87") || cs.contains("ISO_IR 87")) {
                                charsetName = "ISO-2022-JP";
                                break;
                            } else if (cs.contains("ISO 2022 IR 13") || cs.contains("ISO_IR 13")) {
                                charsetName = "ISO-2022-JP";
                                break;
                            } else if (cs.contains("ISO 2022 IR 149") || cs.contains("ISO_IR 149")) {
                                charsetName = "ISO-2022-JP";
                                break;
                            } else if (cs.contains("ISO_IR 100") || cs.contains("ISO 2022 IR 100")) {
                                charsetName = "ISO-8859-1";
                                break;
                            } else if (cs.contains("ISO_IR 192") || cs.contains("ISO 2022 IR 192")) {
                                charsetName = "UTF-8";
                                break;
                            } else if (cs.contains("GBK")) {
                                charsetName = "GBK";
                                break;
                            } else if (cs.contains("GB18030")) {
                                charsetName = "GB18030";
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // SpecificCharacterSetの取得に失敗しても続行
                logger.debug("SpecificCharacterSetの取得に失敗: {}", e.getMessage());
            }
            
            // 文字コードセットが特定できた場合、それを使用
            if (charsetName != null) {
                try {
                    String result = new String(bytes, charsetName);
                    logger.debug("バイト列から{}を取得しました（{}）: {}", tagName, charsetName, result);
                    return result;
                } catch (Exception e) {
                    logger.warn("文字コードセット{}でのデコードに失敗: {}", charsetName, e.getMessage());
                }
            }
            
            // 複数の文字コードセットを試行（日本語優先）
            String[] charsetsToTry = {
                "Shift_JIS",      // 日本語（最も一般的）
                "MS932",          // Windows日本語
                "UTF-8",          // UTF-8
                "ISO-2022-JP",    // ISO 2022 IR 87/IR 13/IR 149
                "EUC-JP",         // EUC-JP
                "ISO-8859-1"      // 最後のフォールバック
            };
            
            String bestResult = null;
            boolean bestHasJapanese = false;
            boolean bestIsValid = false;
            boolean bestIsCorrupted = true;
            
            for (String charset : charsetsToTry) {
                try {
                    String result = new String(bytes, charset);
                    // 結果が有効かどうかをチェック（制御文字や不正な文字がないか）
                    boolean isValid = isValidString(result);
                    boolean hasJapanese = containsJapanese(result);
                    boolean isCorrupted = mightBeCorrupted(result);
                    
                    // 優先順位：
                    // 1. 日本語を含み、有効で、文字化けしていない（最優先）
                    // 2. 有効で、文字化けしていない
                    // 3. 有効（文字化けの可能性あり、ただし最悪の場合のみ）
                    if (isValid && !isCorrupted) {
                        if (hasJapanese) {
                            // 日本語を含み、文字化けしていない最良の結果
                            logger.debug("バイト列から{}を取得しました（{}、日本語含む）: {}", tagName, charset, result);
                            return result;
                        } else if (bestResult == null || bestIsCorrupted || !bestIsValid) {
                            // 日本語を含まないが、有効で文字化けしていない結果を保存
                            bestResult = result;
                            bestHasJapanese = false;
                            bestIsValid = true;
                            bestIsCorrupted = false;
                        }
                    } else if (isValid && (bestResult == null || bestIsCorrupted)) {
                        // 有効だが、文字化けの可能性がある結果（フォールバック用）
                        bestResult = result;
                        bestHasJapanese = hasJapanese;
                        bestIsValid = true;
                        bestIsCorrupted = isCorrupted;
                    }
                } catch (Exception e) {
                    // 次の文字コードセットを試行
                    continue;
                }
            }
            
            // 最良の結果を返す
            if (bestResult != null && bestIsValid) {
                if (bestIsCorrupted) {
                    logger.warn("バイト列から{}を取得しました（文字化けの可能性あり）: {}", tagName, bestResult);
                } else {
                    logger.debug("バイト列から{}を取得しました: {}", tagName, bestResult);
                }
                return bestResult;
            }
            
            // すべての文字コードセットで失敗した場合、ISO-8859-1でデコード（最悪の場合）
            String result = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
            logger.warn("バイト列から{}を取得しました（ISO-8859-1、フォールバック）: {}", tagName, result);
            return result;
        } catch (Exception e2) {
            logger.warn("バイト列からの{}取得にも失敗: {}", tagName, e2.getMessage());
        }
        return null;
    }
    
    /**
     * 文字列が有効かどうかをチェック（制御文字や不正な文字がないか）
     */
    private boolean isValidString(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        // 制御文字（改行やタブ以外）が含まれている場合は無効とみなす
        for (char c : str.toCharArray()) {
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                return false;
            }
        }
        return true;
    }
    
    /**
     * DICOMファイルを読み込み、Studyオブジェクトを返す
     */
    @SuppressWarnings("deprecation")
    public Study readStudy(Path dicomFile) throws IOException {
        logger.info("DICOMファイルを読み込み開始: {}", dicomFile.getFileName());
        
        // ファイルの存在確認とサイズ確認
        if (!dicomFile.toFile().exists()) {
            String errorMsg = "DICOMファイルが存在しません: " + dicomFile;
            logger.error(errorMsg);
            throw new IOException(errorMsg);
        }
        
        long fileSize = dicomFile.toFile().length();
        if (fileSize == 0) {
            String errorMsg = "DICOMファイルのサイズが0です: " + dicomFile;
            logger.error(errorMsg);
            throw new IOException(errorMsg);
        }
        
        logger.debug("ファイルサイズ: {} バイト", fileSize);
        
        try (DicomInputStream dis = new DicomInputStream(dicomFile.toFile())) {
            logger.debug("DicomInputStreamを作成しました。readDatasetを実行します...");
            
            Attributes attrs = null;
            try {
                attrs = dis.readDataset(-1, -1);
                logger.debug("readDatasetが完了しました。Attributesのサイズ: {}", attrs != null ? attrs.size() : 0);
            } catch (Exception e) {
                logger.error("readDataset()でエラーが発生しました。ファイル: {}, ファイルサイズ: {} バイト, エラー: {}",
                    dicomFile.getFileName(), fileSize, e.getMessage(), e);
                // readDataset()に失敗した場合でも、ファイルが破損している可能性があるので、
                // より詳細な情報を出力してから再スロー
                throw new IOException("DICOMデータセットの読み込みに失敗: " + e.getMessage() + 
                    " (ファイル: " + dicomFile.getFileName() + ", サイズ: " + fileSize + " バイト)", e);
            }
            
            if (attrs == null) {
                String errorMsg = "Attributesがnullです。ファイル: " + dicomFile.getFileName() + ", サイズ: " + fileSize + " バイト";
                logger.error(errorMsg);
                throw new IOException(errorMsg);
            }
            
            Study study = new Study();
            study.setStudyInstanceUID(safeGetString(attrs, Tag.StudyInstanceUID, "StudyInstanceUID"));
            study.setPatientID(safeGetString(attrs, Tag.PatientID, "PatientID"));
            study.setPatientName(safeGetString(attrs, Tag.PatientName, "PatientName"));
            study.setPatientSex(safeGetString(attrs, Tag.PatientSex, "PatientSex"));
            study.setStudyDescription(safeGetString(attrs, Tag.StudyDescription, "StudyDescription"));
            study.setAccessionNumber(safeGetString(attrs, Tag.AccessionNumber, "AccessionNumber"));
            study.setReferringPhysicianName(safeGetString(attrs, Tag.ReferringPhysicianName, "ReferringPhysicianName"));
            
            // 日付のパース
            String studyDateStr = safeGetString(attrs, Tag.StudyDate, "StudyDate");
            if (studyDateStr != null && !studyDateStr.isEmpty()) {
                try {
                    study.setStudyDate(LocalDate.parse(studyDateStr, DATE_FORMATTER));
                } catch (Exception e) {
                    logger.warn("StudyDateのパースに失敗: {}", studyDateStr, e);
                }
            }
            
            String patientBirthDateStr = safeGetString(attrs, Tag.PatientBirthDate, "PatientBirthDate");
            if (patientBirthDateStr != null && !patientBirthDateStr.isEmpty()) {
                try {
                    study.setPatientBirthDate(LocalDate.parse(patientBirthDateStr, DATE_FORMATTER));
                } catch (Exception e) {
                    logger.warn("PatientBirthDateのパースに失敗: {}", patientBirthDateStr, e);
                }
            }
            
            // 時刻のパース
            String studyTimeStr = safeGetString(attrs, Tag.StudyTime, "StudyTime");
            if (studyTimeStr != null && !studyTimeStr.isEmpty()) {
                try {
                    if (studyTimeStr.length() >= 6) {
                        study.setStudyTime(LocalTime.parse(studyTimeStr.substring(0, 6), TIME_FORMATTER));
                    }
                } catch (Exception e) {
                    logger.warn("StudyTimeのパースに失敗: {}", studyTimeStr, e);
                }
            }
            
            // Series情報
            Series series = new Series();
            series.setSeriesInstanceUID(safeGetString(attrs, Tag.SeriesInstanceUID, "SeriesInstanceUID"));
            series.setStudyInstanceUID(study.getStudyInstanceUID());
            series.setSeriesNumber(attrs.getInt(Tag.SeriesNumber, 0));
            series.setModality(safeGetString(attrs, Tag.Modality, "Modality"));
            series.setSeriesDescription(safeGetString(attrs, Tag.SeriesDescription, "SeriesDescription"));
            series.setBodyPartExamined(safeGetString(attrs, Tag.BodyPartExamined, "BodyPartExamined"));
            series.setPatientPosition(safeGetString(attrs, Tag.PatientPosition, "PatientPosition"));
            
            String seriesDateStr = safeGetString(attrs, Tag.SeriesDate, "SeriesDate");
            if (seriesDateStr != null && !seriesDateStr.isEmpty()) {
                try {
                    series.setSeriesDate(LocalDate.parse(seriesDateStr, DATE_FORMATTER));
                } catch (Exception e) {
                    logger.warn("SeriesDateのパースに失敗: {}", seriesDateStr, e);
                }
            }
            
            String seriesTimeStr = safeGetString(attrs, Tag.SeriesTime, "SeriesTime");
            if (seriesTimeStr != null && !seriesTimeStr.isEmpty()) {
                try {
                    if (seriesTimeStr.length() >= 6) {
                        series.setSeriesTime(LocalTime.parse(seriesTimeStr.substring(0, 6), TIME_FORMATTER));
                    }
                } catch (Exception e) {
                    logger.warn("SeriesTimeのパースに失敗: {}", seriesTimeStr, e);
                }
            }
            
            // Instance情報
            Instance instance = new Instance();
            instance.setSopInstanceUID(safeGetString(attrs, Tag.SOPInstanceUID, "SOPInstanceUID"));
            instance.setSeriesInstanceUID(series.getSeriesInstanceUID());
            instance.setInstanceNumber(attrs.getInt(Tag.InstanceNumber, 0));
            instance.setSopClassUID(safeGetString(attrs, Tag.SOPClassUID, "SOPClassUID"));
            instance.setFilePath(dicomFile);
            instance.setFileSize(dicomFile.toFile().length());
            instance.setTransferSyntaxUID(dis.getTransferSyntax().toString());
            
            // 画像属性（文字エンコーディングエラーが発生しても取得できるように保護）
            try {
                instance.setRows(attrs.getInt(Tag.Rows, 0));
            } catch (Exception e) {
                logger.warn("Rowsの取得に失敗: {}", e.getMessage());
                instance.setRows(0);
            }
            
            try {
                instance.setColumns(attrs.getInt(Tag.Columns, 0));
            } catch (Exception e) {
                logger.warn("Columnsの取得に失敗: {}", e.getMessage());
                instance.setColumns(0);
            }
            
            try {
                instance.setBitsAllocated(attrs.getInt(Tag.BitsAllocated, 0));
            } catch (Exception e) {
                logger.warn("BitsAllocatedの取得に失敗: {}", e.getMessage());
                instance.setBitsAllocated(0);
            }
            
            try {
                instance.setBitsStored(attrs.getInt(Tag.BitsStored, 0));
            } catch (Exception e) {
                logger.warn("BitsStoredの取得に失敗: {}", e.getMessage());
                instance.setBitsStored(0);
            }
            
            try {
                instance.setSamplesPerPixel(attrs.getInt(Tag.SamplesPerPixel, 1));
            } catch (Exception e) {
                logger.warn("SamplesPerPixelの取得に失敗: {}", e.getMessage());
                instance.setSamplesPerPixel(1);
            }
            
            instance.setPhotometricInterpretation(safeGetString(attrs, Tag.PhotometricInterpretation, "PhotometricInterpretation"));
            
            try {
                instance.setPixelRepresentation(attrs.getInt(Tag.PixelRepresentation, 0));
            } catch (Exception e) {
                logger.warn("PixelRepresentationの取得に失敗: {}", e.getMessage());
                instance.setPixelRepresentation(0);
            }
            
            // ウィンドウ/レベル
            instance.setWindowCenter(safeGetString(attrs, Tag.WindowCenter, "WindowCenter"));
            instance.setWindowWidth(safeGetString(attrs, Tag.WindowWidth, "WindowWidth"));
            
            // Rescale Slope/Intercept（エラーが発生しても取得を試みる）
            try {
                if (attrs.contains(Tag.RescaleSlope)) {
                    instance.setRescaleSlope(attrs.getDouble(Tag.RescaleSlope, 1.0));
                }
            } catch (Exception e) {
                logger.warn("RescaleSlopeの取得に失敗: {}", e.getMessage());
                instance.setRescaleSlope(1.0);
            }
            
            try {
                if (attrs.contains(Tag.RescaleIntercept)) {
                    instance.setRescaleIntercept(attrs.getDouble(Tag.RescaleIntercept, 0.0));
                }
            } catch (Exception e) {
                logger.warn("RescaleInterceptの取得に失敗: {}", e.getMessage());
                instance.setRescaleIntercept(0.0);
            }
            
            series.addInstance(instance);
            study.addSeries(series);
            
            // 画像属性が正しく取得できているか確認
            if (instance.getRows() == null || instance.getRows() == 0 || 
                instance.getColumns() == null || instance.getColumns() == 0) {
                logger.warn("画像属性が不正です: Rows={}, Columns={}, BitsAllocated={}, BitsStored={}, SOPInstanceUID={}", 
                    instance.getRows(), instance.getColumns(), instance.getBitsAllocated(), instance.getBitsStored(), instance.getSopInstanceUID());
            } else {
                logger.info("画像属性を取得しました: Rows={}, Columns={}, BitsAllocated={}, BitsStored={}", 
                    instance.getRows(), instance.getColumns(), instance.getBitsAllocated(), instance.getBitsStored());
            }
            
            logger.info("DICOMファイルの読み込みが完了しました: {} (StudyInstanceUID: {})", 
                dicomFile.getFileName(), study.getStudyInstanceUID());
            return study;
        } catch (IOException e) {
            logger.error("DICOMファイルの読み込み中にIOExceptionが発生: {} (ファイルサイズ: {} バイト)", 
                dicomFile, dicomFile.toFile().exists() ? dicomFile.toFile().length() : 0, e);
            throw e;
        } catch (Exception e) {
            logger.error("DICOMファイルの読み込み中に予期しないエラーが発生: {} (ファイルサイズ: {} バイト)", 
                dicomFile, dicomFile.toFile().exists() ? dicomFile.toFile().length() : 0, e);
            throw new IOException("DICOMファイルの読み込みに失敗: " + e.getMessage() + 
                " (ファイル: " + dicomFile.getFileName() + ", サイズ: " + 
                (dicomFile.toFile().exists() ? dicomFile.toFile().length() : 0) + " バイト)", e);
        }
    }
    
    /**
     * DICOMファイルが有効かどうかをチェック
     */
    @SuppressWarnings("deprecation")
    public boolean isValidDicomFile(Path file) {
        try (DicomInputStream dis = new DicomInputStream(file.toFile())) {
            Attributes attrs = dis.readDataset(-1, -1);
            return attrs.contains(Tag.SOPClassUID) && attrs.contains(Tag.SOPInstanceUID);
        } catch (Exception e) {
            logger.debug("DICOMファイルの検証に失敗: {}", file, e);
            return false;
        }
    }
}

