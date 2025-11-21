package com.jjdicomviewer.storage;

import com.jjdicomviewer.core.Instance;
import com.jjdicomviewer.core.Series;
import com.jjdicomviewer.core.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Study/Series/Instanceのデータベース操作を行うリポジトリ
 */
public class StudyRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(StudyRepository.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    
    private final DatabaseManager dbManager;
    
    public StudyRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    /**
     * Studyを保存（既に存在する場合は更新）
     */
    public void saveStudy(Study study) throws SQLException {
        Connection conn = dbManager.getConnection();
        
        try (PreparedStatement stmt = conn.prepareStatement("""
            INSERT OR REPLACE INTO studies (
                study_instance_uid, patient_id, patient_name, patient_birth_date,
                patient_sex, study_date, study_time, study_description,
                accession_number, referring_physician_name, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """)) {
            stmt.setString(1, study.getStudyInstanceUID());
            stmt.setString(2, study.getPatientID());
            stmt.setString(3, study.getPatientName());
            stmt.setString(4, study.getPatientBirthDate() != null ? 
                study.getPatientBirthDate().format(DATE_FORMATTER) : null);
            stmt.setString(5, study.getPatientSex());
            stmt.setString(6, study.getStudyDate() != null ? 
                study.getStudyDate().format(DATE_FORMATTER) : null);
            stmt.setString(7, study.getStudyTime() != null ? 
                study.getStudyTime().format(TIME_FORMATTER) : null);
            stmt.setString(8, study.getStudyDescription());
            stmt.setString(9, study.getAccessionNumber());
            stmt.setString(10, study.getReferringPhysicianName());
            
            stmt.executeUpdate();
            
            // SeriesとInstanceも保存
            for (Series series : study.getSeriesList()) {
                saveSeries(series);
            }
            
            logger.debug("Studyを保存しました: {}", study.getStudyInstanceUID());
        }
    }
    
    /**
     * Seriesを保存
     */
    public void saveSeries(Series series) throws SQLException {
        Connection conn = dbManager.getConnection();
        
        try (PreparedStatement stmt = conn.prepareStatement("""
            INSERT OR REPLACE INTO series (
                series_instance_uid, study_instance_uid, series_number,
                modality, series_date, series_time, series_description,
                body_part_examined, patient_position, instance_count, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """)) {
            stmt.setString(1, series.getSeriesInstanceUID());
            stmt.setString(2, series.getStudyInstanceUID());
            stmt.setInt(3, series.getSeriesNumber() != null ? series.getSeriesNumber() : 0);
            stmt.setString(4, series.getModality());
            stmt.setString(5, series.getSeriesDate() != null ? 
                series.getSeriesDate().format(DATE_FORMATTER) : null);
            stmt.setString(6, series.getSeriesTime() != null ? 
                series.getSeriesTime().format(TIME_FORMATTER) : null);
            stmt.setString(7, series.getSeriesDescription());
            stmt.setString(8, series.getBodyPartExamined());
            stmt.setString(9, series.getPatientPosition());
            stmt.setInt(10, series.getInstanceCount());
            
            stmt.executeUpdate();
            
            // Instanceも保存
            for (Instance instance : series.getInstanceList()) {
                saveInstance(instance);
            }
            
            logger.debug("Seriesを保存しました: {}", series.getSeriesInstanceUID());
        }
    }
    
    /**
     * Instanceを保存
     */
    public void saveInstance(Instance instance) throws SQLException {
        Connection conn = dbManager.getConnection();
        
        try (PreparedStatement stmt = conn.prepareStatement("""
            INSERT OR REPLACE INTO instances (
                sop_instance_uid, series_instance_uid, instance_number,
                sop_class_uid, file_path, file_size, transfer_syntax_uid,
                rows, columns, bits_allocated, bits_stored, samples_per_pixel,
                photometric_interpretation, window_center, window_width,
                rescale_slope, rescale_intercept, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """)) {
            stmt.setString(1, instance.getSopInstanceUID());
            stmt.setString(2, instance.getSeriesInstanceUID());
            stmt.setInt(3, instance.getInstanceNumber() != null ? instance.getInstanceNumber() : 0);
            stmt.setString(4, instance.getSopClassUID());
            stmt.setString(5, instance.getFilePath().toString());
            stmt.setLong(6, instance.getFileSize());
            stmt.setString(7, instance.getTransferSyntaxUID());
            stmt.setInt(8, instance.getRows() != null ? instance.getRows() : 0);
            stmt.setInt(9, instance.getColumns() != null ? instance.getColumns() : 0);
            stmt.setInt(10, instance.getBitsAllocated() != null ? instance.getBitsAllocated() : 0);
            stmt.setInt(11, instance.getBitsStored() != null ? instance.getBitsStored() : 0);
            stmt.setInt(12, instance.getSamplesPerPixel() != null ? instance.getSamplesPerPixel() : 1);
            stmt.setString(13, instance.getPhotometricInterpretation());
            stmt.setString(14, instance.getWindowCenter());
            stmt.setString(15, instance.getWindowWidth());
            stmt.setDouble(16, instance.getRescaleSlope() != null ? instance.getRescaleSlope() : 1.0);
            stmt.setDouble(17, instance.getRescaleIntercept() != null ? instance.getRescaleIntercept() : 0.0);
            
            stmt.executeUpdate();
            
            logger.debug("Instanceを保存しました: {}", instance.getSopInstanceUID());
        }
    }
    
    /**
     * すべてのStudyを取得
     */
    public List<Study> findAllStudies() throws SQLException {
        List<Study> studies = new ArrayList<>();
        Connection conn = dbManager.getConnection();
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM studies ORDER BY DATE(created_at) DESC, study_date DESC, study_time DESC")) {
            
            while (rs.next()) {
                Study study = mapStudyFromResultSet(rs);
                studies.add(study);
            }
        }
        
        return studies;
    }
    
    /**
     * StudyInstanceUIDでSeriesを取得
     */
    public List<Series> findSeriesByStudyUID(String studyInstanceUID) throws SQLException {
        List<Series> seriesList = new ArrayList<>();
        Connection conn = dbManager.getConnection();
        
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM series WHERE study_instance_uid = ? ORDER BY series_number")) {
            stmt.setString(1, studyInstanceUID);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Series series = mapSeriesFromResultSet(rs);
                    // Instanceも読み込む
                    series.setInstanceList(findInstancesBySeriesUID(series.getSeriesInstanceUID()));
                    seriesList.add(series);
                }
            }
        }
        
        return seriesList;
    }
    
    /**
     * SeriesInstanceUIDでInstanceを取得
     */
    public List<Instance> findInstancesBySeriesUID(String seriesInstanceUID) throws SQLException {
        List<Instance> instanceList = new ArrayList<>();
        java.util.Set<String> sopInstanceUIDs = new java.util.HashSet<>(); // 重複チェック用
        Connection conn = dbManager.getConnection();
        
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM instances WHERE series_instance_uid = ? ORDER BY instance_number")) {
            stmt.setString(1, seriesInstanceUID);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Instance instance = mapInstanceFromResultSet(rs);
                    String sopUID = instance.getSopInstanceUID();
                    
                    // 重複チェック（同じSOPInstanceUIDが既に存在する場合はスキップ）
                    if (sopUID != null && !sopUID.isEmpty()) {
                        if (sopInstanceUIDs.contains(sopUID)) {
                            logger.warn("重複するSOPInstanceUIDを検出してスキップ: {} (SeriesInstanceUID: {})", 
                                sopUID, seriesInstanceUID);
                            continue;
                        }
                        sopInstanceUIDs.add(sopUID);
                    }
                    
                    instanceList.add(instance);
                }
            }
        }
        
        logger.debug("SeriesInstanceUID {} から {} 件のインスタンスを取得（重複チェック済み）", 
            seriesInstanceUID, instanceList.size());
        
        return instanceList;
    }
    
    // ResultSetからStudyオブジェクトをマッピング
    private Study mapStudyFromResultSet(ResultSet rs) throws SQLException {
        Study study = new Study();
        study.setStudyInstanceUID(rs.getString("study_instance_uid"));
        study.setPatientID(rs.getString("patient_id"));
        study.setPatientName(rs.getString("patient_name"));
        study.setPatientSex(rs.getString("patient_sex"));
        study.setStudyDescription(rs.getString("study_description"));
        study.setAccessionNumber(rs.getString("accession_number"));
        study.setReferringPhysicianName(rs.getString("referring_physician_name"));
        
        String studyDateStr = rs.getString("study_date");
        if (studyDateStr != null && !studyDateStr.isEmpty()) {
            try {
                study.setStudyDate(LocalDate.parse(studyDateStr, DATE_FORMATTER));
            } catch (Exception e) {
                logger.warn("StudyDateのパースに失敗: {}", studyDateStr, e);
            }
        }
        
        String patientBirthDateStr = rs.getString("patient_birth_date");
        if (patientBirthDateStr != null && !patientBirthDateStr.isEmpty()) {
            try {
                study.setPatientBirthDate(LocalDate.parse(patientBirthDateStr, DATE_FORMATTER));
            } catch (Exception e) {
                logger.warn("PatientBirthDateのパースに失敗: {}", patientBirthDateStr, e);
            }
        }
        
        String studyTimeStr = rs.getString("study_time");
        if (studyTimeStr != null && !studyTimeStr.isEmpty()) {
            try {
                if (studyTimeStr.length() >= 6) {
                    study.setStudyTime(LocalTime.parse(studyTimeStr.substring(0, 6), TIME_FORMATTER));
                }
            } catch (Exception e) {
                logger.warn("StudyTimeのパースに失敗: {}", studyTimeStr, e);
            }
        }
        
        // created_at（DB登録日）を読み込む（日付のみ、時間は不要）
        java.sql.Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        if (createdAtTimestamp != null) {
            try {
                study.setCreatedAt(createdAtTimestamp.toLocalDateTime().toLocalDate());
            } catch (Exception e) {
                logger.warn("created_atのパースに失敗", e);
            }
        }
        
        return study;
    }
    
    // ResultSetからSeriesオブジェクトをマッピング
    private Series mapSeriesFromResultSet(ResultSet rs) throws SQLException {
        Series series = new Series();
        series.setSeriesInstanceUID(rs.getString("series_instance_uid"));
        series.setStudyInstanceUID(rs.getString("study_instance_uid"));
        series.setSeriesNumber(rs.getInt("series_number"));
        series.setModality(rs.getString("modality"));
        series.setSeriesDescription(rs.getString("series_description"));
        series.setBodyPartExamined(rs.getString("body_part_examined"));
        series.setPatientPosition(rs.getString("patient_position"));
        series.setInstanceCount(rs.getInt("instance_count"));
        
        String seriesDateStr = rs.getString("series_date");
        if (seriesDateStr != null && !seriesDateStr.isEmpty()) {
            try {
                series.setSeriesDate(LocalDate.parse(seriesDateStr, DATE_FORMATTER));
            } catch (Exception e) {
                logger.warn("SeriesDateのパースに失敗: {}", seriesDateStr, e);
            }
        }
        
        String seriesTimeStr = rs.getString("series_time");
        if (seriesTimeStr != null && !seriesTimeStr.isEmpty()) {
            try {
                if (seriesTimeStr.length() >= 6) {
                    series.setSeriesTime(LocalTime.parse(seriesTimeStr.substring(0, 6), TIME_FORMATTER));
                }
            } catch (Exception e) {
                logger.warn("SeriesTimeのパースに失敗: {}", seriesTimeStr, e);
            }
        }
        
        return series;
    }
    
    /**
     * Studyを削除（関連するSeriesとInstanceも自動的に削除される - CASCADE）
     * 削除前に、削除対象のファイルパスを取得して返す（ファイル削除用）
     */
    public List<java.nio.file.Path> getFilePathsForStudy(String studyInstanceUID) throws SQLException {
        List<java.nio.file.Path> filePaths = new ArrayList<>();
        Connection conn = dbManager.getConnection();
        
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT file_path FROM instances WHERE series_instance_uid IN " +
                "(SELECT series_instance_uid FROM series WHERE study_instance_uid = ?)")) {
            stmt.setString(1, studyInstanceUID);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String filePathStr = rs.getString("file_path");
                    if (filePathStr != null && !filePathStr.isEmpty()) {
                        filePaths.add(java.nio.file.Paths.get(filePathStr));
                    }
                }
            }
        }
        
        return filePaths;
    }
    
    /**
     * Studyを削除（関連するSeriesとInstanceも自動的に削除される - CASCADE）
     */
    public void deleteStudy(String studyInstanceUID) throws SQLException {
        Connection conn = dbManager.getConnection();
        
        try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM studies WHERE study_instance_uid = ?")) {
            stmt.setString(1, studyInstanceUID);
            int deleted = stmt.executeUpdate();
            logger.info("Studyをデータベースから削除しました: {} (削除件数: {})", studyInstanceUID, deleted);
        }
    }
    
    // ResultSetからInstanceオブジェクトをマッピング
    private Instance mapInstanceFromResultSet(ResultSet rs) throws SQLException {
        Instance instance = new Instance();
        instance.setSopInstanceUID(rs.getString("sop_instance_uid"));
        instance.setSeriesInstanceUID(rs.getString("series_instance_uid"));
        instance.setInstanceNumber(rs.getInt("instance_number"));
        instance.setSopClassUID(rs.getString("sop_class_uid"));
        instance.setFilePath(java.nio.file.Paths.get(rs.getString("file_path")));
        instance.setFileSize(rs.getLong("file_size"));
        instance.setTransferSyntaxUID(rs.getString("transfer_syntax_uid"));
        instance.setRows(rs.getInt("rows"));
        instance.setColumns(rs.getInt("columns"));
        instance.setBitsAllocated(rs.getInt("bits_allocated"));
        instance.setBitsStored(rs.getInt("bits_stored"));
        instance.setSamplesPerPixel(rs.getInt("samples_per_pixel"));
        instance.setPhotometricInterpretation(rs.getString("photometric_interpretation"));
        instance.setWindowCenter(rs.getString("window_center"));
        instance.setWindowWidth(rs.getString("window_width"));
        instance.setRescaleSlope(rs.getDouble("rescale_slope"));
        instance.setRescaleIntercept(rs.getDouble("rescale_intercept"));
        
        return instance;
    }
}

