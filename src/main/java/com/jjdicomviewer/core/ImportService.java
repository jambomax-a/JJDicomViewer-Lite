package com.jjdicomviewer.core;

import com.jjdicomviewer.config.AppConfig;
import com.jjdicomviewer.dicom.DicomReader;
import com.jjdicomviewer.storage.DatabaseManager;
import com.jjdicomviewer.storage.StudyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DICOMファイルのインポートサービス
 */
public class ImportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);
    
    private final DicomReader dicomReader;
    private final StudyRepository studyRepository;
    private final AppConfig appConfig;
    
    public ImportService(DatabaseManager dbManager) {
        this.dicomReader = new DicomReader();
        this.studyRepository = new StudyRepository(dbManager);
        this.appConfig = AppConfig.getInstance();
    }
    
    /**
     * フォルダからDICOMファイルをインポート
     */
    public void importFromFolder(Path folderPath, ProgressCallback callback) throws IOException {
        logger.info("フォルダからのインポートを開始: {}", folderPath);
        
        // フォルダの存在確認
        if (!Files.exists(folderPath)) {
            String errorMsg = "フォルダが存在しません: " + folderPath;
            logger.error(errorMsg);
            throw new IOException(errorMsg);
        }
        
        if (!Files.isDirectory(folderPath)) {
            String errorMsg = "パスはディレクトリではありません: " + folderPath;
            logger.error(errorMsg);
            throw new IOException(errorMsg);
        }
        
        List<Path> dicomFiles;
        try {
            dicomFiles = findDicomFiles(folderPath);
        } catch (Exception e) {
            String errorMsg = "DICOMファイルの検索中にエラーが発生: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new IOException(errorMsg, e);
        }
        
        int totalFiles = dicomFiles.size();
        int processedFiles = 0;
        int errorCount = 0;
        
        if (callback != null) {
            callback.onProgress(0, totalFiles, "DICOMファイルを検索中...");
        }
        
        logger.info("{}件のDICOMファイルが見つかりました", totalFiles);
        
        if (totalFiles == 0) {
            String errorMsg = "DICOMファイルが見つかりませんでした: " + folderPath;
            logger.warn(errorMsg);
            throw new IOException(errorMsg);
        }
        
        // StudyInstanceUIDでグループ化するMap
        Map<String, Study> studyMap = new HashMap<>();
        
        logger.info("DICOMファイルの読み込みを開始します...");
        
        // すべてのファイルを読み込んで、Studyごとにグループ化
        // ファイル読み込みは全体の70%を占める
        for (Path dicomFile : dicomFiles) {
            try {
                int currentProcessed = processedFiles + errorCount;
                // ファイル読み込み進捗は全体の70%まで
                int readingProgress = (int) (totalFiles * 0.7 * (double) currentProcessed / totalFiles);
                
                if (callback != null) {
                    callback.onProgress(readingProgress, totalFiles, 
                        "読み込み中: " + dicomFile.getFileName());
                }
                
                // 100ファイルごとに進捗をログ出力（最初の10ファイルは毎回出力）
                if (currentProcessed <= 10 || currentProcessed % 100 == 0) {
                    logger.info("読み込み進捗: {}/{} ファイル処理済み (成功: {}, 失敗: {})", 
                        currentProcessed, totalFiles, processedFiles, errorCount);
                }
                
                logger.debug("DICOMファイルを読み込み中: {}", dicomFile);
                
                // DICOMファイルを読み込んでメタデータを取得
                Study fileStudy = dicomReader.readStudy(dicomFile);
                
                if (fileStudy == null || fileStudy.getStudyInstanceUID() == null) {
                    logger.warn("StudyInstanceUIDが取得できませんでした: {}", dicomFile);
                    errorCount++;
                    continue;
                }
                
                String studyUID = fileStudy.getStudyInstanceUID();
                
                // 既存のStudyがある場合は、SeriesとInstanceを追加
                Study existingStudy = studyMap.get(studyUID);
                if (existingStudy != null) {
                    // 既存のStudyにSeriesとInstanceを追加
                    mergeStudy(existingStudy, fileStudy);
                    logger.debug("既存のStudyに追加: {} (Series数: {})", studyUID, existingStudy.getSeriesList().size());
                } else {
                    // 新しいStudyとして追加
                    studyMap.put(studyUID, fileStudy);
                    logger.debug("新しいStudyを追加: {} (Series数: {})", studyUID, fileStudy.getSeriesList().size());
                }
                
                processedFiles++;
                
            } catch (Exception e) {
                errorCount++;
                // エラーが発生しても続行
                // 最初の10件のエラーのみ詳細ログを出力
                if (errorCount <= 10) {
                    logger.error("DICOMファイルの読み込みに失敗 ({}件目): {} - エラー: {}", 
                        errorCount, dicomFile, e.getMessage(), e);
                } else if (errorCount == 11) {
                    logger.warn("エラーが11件以上発生しています。以降のエラーは簡潔にログ出力します。");
                } else {
                    logger.warn("DICOMファイルの読み込みに失敗 ({}件目): {} - エラー: {}", 
                        errorCount, dicomFile, e.getMessage());
                }
            }
        }
        
        logger.info("ファイル読み込み完了: {}件成功, {}件失敗, {}件のStudy", 
            processedFiles, errorCount, studyMap.size());
        
        if (studyMap.isEmpty()) {
            logger.error("読み込んだStudyが0件です。すべてのファイルの読み込みに失敗した可能性があります。");
            if (callback != null) {
                callback.onProgress(totalFiles, totalFiles, "インポート完了（エラー: 読み込めたStudyが0件）");
            }
            return;
        }
        
        // すべてのStudyをストレージにコピーして保存
        // 進捗バー: ファイル読み込みが70%、保存処理が30%を占める
        int totalStudies = studyMap.size();
        int savedStudies = 0;
        int studyIndex = 0;
        
        for (Study study : studyMap.values()) {
            studyIndex++;
            try {
                // ファイル読み込み完了分（70%）+ 保存処理の進捗（30% * (studyIndex / totalStudies)）
                int readingProgress = (int) (totalFiles * 0.7);
                int savingProgress = (int) (totalFiles * 0.3 * studyIndex / totalStudies);
                int currentProgress = readingProgress + savingProgress;
                
                if (callback != null) {
                    callback.onProgress(currentProgress, totalFiles, 
                        String.format("保存中: %s (%d/%d)", 
                            study.getPatientName() != null ? study.getPatientName() : "Unknown",
                            studyIndex, totalStudies));
                }
                
                logger.info("Studyを保存中: {} (Series数: {}) [{}/{}]", 
                    study.getStudyInstanceUID(), study.getSeriesList().size(), studyIndex, totalStudies);
                
                // コピー前のパスをログ出力（デバッグ用）
                for (Series series : study.getSeriesList()) {
                    for (Instance instance : series.getInstanceList()) {
                        logger.debug("コピー前のファイルパス: {} (SOPInstanceUID: {})", 
                            instance.getFilePath(), instance.getSopInstanceUID());
                    }
                }
                
                // DICOMファイルをストレージベースパス配下にコピーし、パスを更新
                copyDicomFilesToStorage(study);
                
                // コピー後のパスをログ出力（デバッグ用）
                for (Series series : study.getSeriesList()) {
                    for (Instance instance : series.getInstanceList()) {
                        logger.debug("コピー後のファイルパス: {} (SOPInstanceUID: {})", 
                            instance.getFilePath(), instance.getSopInstanceUID());
                        // ファイルが存在するか確認
                        if (!Files.exists(instance.getFilePath())) {
                            logger.error("コピー後のファイルが存在しません: {} (SOPInstanceUID: {})", 
                                instance.getFilePath(), instance.getSopInstanceUID());
                        }
                    }
                }
                
                // データベースに保存（コピー先のパスで保存される）
                studyRepository.saveStudy(study);
                
                savedStudies++;
                logger.info("Studyを保存しました: {} (Series数: {}, Instance数: {})", 
                    study.getStudyInstanceUID(), 
                    study.getSeriesList().size(),
                    study.getSeriesList().stream()
                        .mapToInt(s -> s.getInstanceList().size())
                        .sum());
                
            } catch (Exception e) {
                logger.error("Studyの保存に失敗: {} - エラー: {}", study.getStudyInstanceUID(), e.getMessage(), e);
                errorCount++;
            }
        }
        
        // 最後に100%を通知
        if (callback != null) {
            callback.onProgress(totalFiles, totalFiles, 
                String.format("インポート完了: %d件のStudyを保存", savedStudies));
        }
        logger.info("インポート完了: {}件のStudyを保存 ({}件のファイルを処理, {}件のエラー)", 
            savedStudies, processedFiles, errorCount);
    }
    
    /**
     * 既存のStudyに新しいStudyのSeriesとInstanceをマージ
     */
    private void mergeStudy(Study existingStudy, Study newStudy) {
        for (Series newSeries : newStudy.getSeriesList()) {
            String seriesUID = newSeries.getSeriesInstanceUID();
            
            // 既存のSeriesを検索
            Series existingSeries = null;
            for (Series s : existingStudy.getSeriesList()) {
                if (seriesUID.equals(s.getSeriesInstanceUID())) {
                    existingSeries = s;
                    break;
                }
            }
            
            if (existingSeries != null) {
                // 既存のSeriesにInstanceを追加
                for (Instance newInstance : newSeries.getInstanceList()) {
                    // 同じSOPInstanceUIDのInstanceが既に存在するかチェック
                    boolean exists = false;
                    String newSopUID = newInstance.getSopInstanceUID();
                    if (newSopUID == null || newSopUID.isEmpty()) {
                        logger.warn("SOPInstanceUIDが空のInstanceをスキップします");
                        continue;
                    }
                    for (Instance inst : existingSeries.getInstanceList()) {
                        if (newSopUID.equals(inst.getSopInstanceUID())) {
                            exists = true;
                            logger.debug("同じSOPInstanceUIDのInstanceが既に存在するためスキップ: {} (Series: {})", 
                                newSopUID, seriesUID);
                            break;
                        }
                    }
                    if (!exists) {
                        existingSeries.addInstance(newInstance);
                        logger.debug("既存のSeriesにInstanceを追加: {} (Series: {})", 
                            newSopUID, seriesUID);
                    }
                }
            } else {
                // 新しいSeriesとして追加
                existingStudy.addSeries(newSeries);
                logger.debug("新しいSeriesを追加: {} (Instance数: {})", 
                    seriesUID, newSeries.getInstanceList().size());
            }
        }
    }
    
    /**
     * フォルダ内のDICOMファイルを再帰的に検索
     */
    private List<Path> findDicomFiles(Path folderPath) throws IOException {
        List<Path> dicomFiles = new ArrayList<>();
        
        // フォルダの存在確認
        if (!Files.exists(folderPath)) {
            logger.error("フォルダが存在しません: {}", folderPath);
            throw new IOException("フォルダが存在しません: " + folderPath);
        }
        
        if (!Files.isDirectory(folderPath)) {
            logger.error("パスはディレクトリではありません: {}", folderPath);
            throw new IOException("パスはディレクトリではありません: " + folderPath);
        }
        
        try {
            Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    try {
                        // シンボリックリンクの場合はスキップ（循環参照を避ける）
                        if (Files.isSymbolicLink(dir)) {
                            logger.debug("シンボリックリンクをスキップ: {}", dir);
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    } catch (Exception e) {
                        logger.warn("ディレクトリアクセスに失敗: {} - エラー: {}", dir, e.getMessage());
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        if (isDicomFile(file)) {
                            dicomFiles.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    } catch (Exception e) {
                        logger.warn("ファイル処理に失敗: {} - エラー: {}", file, e.getMessage());
                        return FileVisitResult.CONTINUE;
                    }
                }
                
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    // アクセス拒否やパーミッションエラーの場合でも続行
                    if (exc instanceof AccessDeniedException) {
                        logger.debug("アクセス拒否によりスキップ: {}", file);
                    } else if (exc instanceof java.nio.file.NoSuchFileException) {
                        logger.debug("ファイルが見つかりません（削除された可能性）: {}", file);
                    } else {
                        logger.warn("ファイルアクセスに失敗: {} - エラー: {}", file, exc.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            logger.error("ファイル検索中にエラーが発生: {} - エラー: {}", folderPath, e.getMessage(), e);
            throw new IOException("ファイル検索中にエラーが発生: " + e.getMessage(), e);
        }
        
        logger.info("DICOMファイル検索完了: {}件のファイルが見つかりました", dicomFiles.size());
        return dicomFiles;
    }
    
    /**
     * ファイルがDICOMファイルかどうかを判定
     */
    private boolean isDicomFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        
        // .dcm拡張子または拡張子なし
        if (fileName.endsWith(".dcm") || fileName.endsWith(".dicom")) {
            return true;
        }
        
        // 拡張子がない場合、DICOMファイルかどうかを検証
        if (!fileName.contains(".")) {
            return dicomReader.isValidDicomFile(file);
        }
        
        return false;
    }
    
    /**
     * Study内のすべてのInstanceファイルをストレージベースパス配下にコピーし、パスを更新
     */
    private void copyDicomFilesToStorage(Study study) throws IOException {
        Path storageBasePath = appConfig.getStorageBasePath();
        
        logger.info("DICOMファイルをストレージにコピーします。ストレージベースパス: {}", storageBasePath);
        
        // ストレージディレクトリが存在しない場合は作成
        if (!Files.exists(storageBasePath)) {
            Files.createDirectories(storageBasePath);
            logger.info("ストレージディレクトリを作成しました: {}", storageBasePath);
        }
        
        for (Series series : study.getSeriesList()) {
            for (Instance instance : series.getInstanceList()) {
                Path sourceFile = instance.getFilePath();
                
                // 既にストレージベースパス配下にある場合はスキップ
                if (sourceFile.toString().startsWith(storageBasePath.toString())) {
                    logger.debug("ファイルは既にストレージ配下にあります: {}", sourceFile);
                    continue;
                }
                
                // コピー先のパスを決定: {storageBasePath}/{StudyInstanceUID}/{SeriesInstanceUID}/{SOPInstanceUID}.dcm
                Path targetDir = storageBasePath
                    .resolve(study.getStudyInstanceUID())
                    .resolve(series.getSeriesInstanceUID());
                
                // ディレクトリが存在しない場合は作成
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                
                // ファイル名を決定（SOPInstanceUIDを使用）
                String fileName = instance.getSopInstanceUID() + ".dcm";
                Path targetFile = targetDir.resolve(fileName);
                
                // ファイルをコピー（既に存在する場合は上書きしない）
                if (!Files.exists(targetFile)) {
                    try {
                        Files.copy(sourceFile, targetFile, StandardCopyOption.COPY_ATTRIBUTES);
                        logger.info("DICOMファイルをコピーしました: {} -> {} (サイズ: {} バイト)", 
                            sourceFile, targetFile, Files.size(targetFile));
                    } catch (IOException e) {
                        logger.error("DICOMファイルのコピーに失敗: {} -> {}", sourceFile, targetFile, e);
                        // コピーに失敗した場合は元のパスを維持
                        continue;
                    }
                } else {
                    logger.debug("ファイルは既に存在します: {} (サイズ: {} バイト)", targetFile, Files.size(targetFile));
                }
                
                // InstanceのfilePathをコピー先のパスに更新
                instance.setFilePath(targetFile);
                instance.setFileSize(targetFile.toFile().length());
                logger.debug("InstanceのfilePathを更新しました: {} (SOPInstanceUID: {})", 
                    targetFile, instance.getSopInstanceUID());
            }
        }
    }
    
    /**
     * 進捗コールバックインターフェース
     */
    public interface ProgressCallback {
        void onProgress(int current, int total, String message);
    }
}

