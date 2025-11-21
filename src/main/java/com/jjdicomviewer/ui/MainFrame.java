package com.jjdicomviewer.ui;

import com.jjdicomviewer.core.ImportService;
import com.jjdicomviewer.core.Instance;
import com.jjdicomviewer.core.Series;
import com.jjdicomviewer.core.Study;
import com.jjdicomviewer.i18n.Messages;
import com.jjdicomviewer.storage.DatabaseManager;
import com.jjdicomviewer.storage.StudyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

/**
 * メインフレーム - アプリケーションのメインウィンドウ（Swing版）
 * JavaFX版と同じ構造：スタディ一覧、シリーズ一覧、ビューア
 */
public class MainFrame extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);
    private final Messages messages = Messages.getInstance(); // 静的フィールドではなくインスタンスフィールドに変更
    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 800;

    // データベース
    private DatabaseManager dbManager;
    private StudyRepository studyRepository;
    private ImportService importService;
    
    // UIコンポーネント
    private JList<Study> studyListView;
    private DefaultListModel<Study> studyListModel;
    private JList<String> seriesListView;
    private DefaultListModel<String> seriesListModel;
    private ImageViewerPanel imageViewerPanel;
    private JMenuBar menuBar;
    
    // コントロールパネル
    private JTextField windowCenterField;
    private JTextField windowWidthField;
    private JButton resetButton;
    private JSlider sliceSlider;
    private JLabel sliceLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    
    // 現在の選択
    private Study currentStudy;
    private Series currentSeries;
    private List<Instance> currentInstances;
    private int currentInstanceIndex = 0;

    public MainFrame() {
        initializeDatabase();
        initializeComponents();
        setupLayout();
        setupMenuBar();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle(messages.get("app.title"));
        setApplicationIcon();
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocationRelativeTo(null);
        
        // 初期データの読み込み
        refreshStudyList();
    }
    
    private void initializeDatabase() {
        try {
            dbManager = new DatabaseManager();
            studyRepository = new StudyRepository(dbManager);
            importService = new ImportService(dbManager);
            logger.info(messages.get("message.info.database_initialized"));
        } catch (Exception e) {
            logger.error(messages.get("message.error.database_init_failed", e.getMessage()), e);
            JOptionPane.showMessageDialog(this, 
                messages.get("message.error.database_init_failed", e.getMessage()), 
                messages.get("message.error.title"), 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initializeComponents() {
        // スタディ一覧
        studyListModel = new DefaultListModel<>();
        studyListView = new JList<>(studyListModel);
        studyListView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studyListView.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onStudySelected(studyListView.getSelectedValue());
            }
        });
        
        // シリーズ一覧
        seriesListModel = new DefaultListModel<>();
        seriesListView = new JList<>(seriesListModel);
        seriesListView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        seriesListView.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSeriesSelected(seriesListView.getSelectedIndex());
            }
        });
        
        // ビューア
        imageViewerPanel = new ImageViewerPanel();
        imageViewerPanel.setSliceChangeCallback(new ImageViewerPanel.SliceChangeCallback() {
            @Override
            public void onNextSlice() {
                if (currentInstances != null && currentInstanceIndex < currentInstances.size() - 1) {
                    gotoSlice(currentInstanceIndex + 1);
                }
            }
            
            @Override
            public void onPreviousSlice() {
                if (currentInstances != null && currentInstanceIndex > 0) {
                    gotoSlice(currentInstanceIndex - 1);
                }
            }
        });
        
        // WW/WL変更コールバックを設定
        imageViewerPanel.setWindowLevelChangeCallback((center, width) -> {
            updateControlPanel();
        });
        
        // コントロールパネル
        setupControlPanel();
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 上部にメニューバーとツールバー
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(createToolBar(), BorderLayout.NORTH);
        add(topPanel, BorderLayout.NORTH);
        
        // 中央にSplitPane（スタディ一覧、シリーズ一覧、ビューア）
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(400);
        mainSplitPane.setResizeWeight(0.4); // 左側が40%
        
        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        leftSplitPane.setDividerLocation(200);
        leftSplitPane.setResizeWeight(0.5); // 左右均等
        
        // 左側：スタディ一覧
        JPanel studyPanel = new JPanel(new BorderLayout());
        studyPanel.setBorder(BorderFactory.createTitledBorder(messages.get("panel.study_list")));
        studyPanel.add(new JScrollPane(studyListView), BorderLayout.CENTER);
        leftSplitPane.setLeftComponent(studyPanel);
        
        // 中央：シリーズ一覧
        JPanel seriesPanel = new JPanel(new BorderLayout());
        seriesPanel.setBorder(BorderFactory.createTitledBorder(messages.get("panel.series_list")));
        seriesPanel.add(new JScrollPane(seriesListView), BorderLayout.CENTER);
        leftSplitPane.setRightComponent(seriesPanel);
        
        mainSplitPane.setLeftComponent(leftSplitPane);
        mainSplitPane.setRightComponent(imageViewerPanel);
        
        add(mainSplitPane, BorderLayout.CENTER);
        
        // 下部にコントロールパネルとステータスバー
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(createControlPanel(), BorderLayout.CENTER);
        bottomPanel.add(createStatusBar(), BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        JButton importButton = new JButton(messages.get("toolbar.import"));
        importButton.addActionListener(e -> handleImport());
        toolBar.add(importButton);
        
        JButton refreshButton = new JButton(messages.get("toolbar.refresh"));
        refreshButton.addActionListener(e -> refreshStudyList());
        toolBar.add(refreshButton);
        
        JButton deleteButton = new JButton(messages.get("toolbar.delete"));
        deleteButton.addActionListener(e -> handleDelete());
        toolBar.add(deleteButton);
        
        toolBar.addSeparator();
        
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        toolBar.add(progressBar);
        
        return toolBar;
    }
    
    // WW/WL入力フィールドのイベントリスナー（無限ループ防止のため保持）
    private ActionListener wlActionListener;
    // スライダーのChangeListener（無限ループ防止のため保持）
    private javax.swing.event.ChangeListener sliceSliderChangeListener;
    
    private void setupControlPanel() {
        windowCenterField = new JTextField("128", 8);
        windowWidthField = new JTextField("256", 8);
        resetButton = new JButton(messages.get("control.reset"));
        
        // スライススライダー
        sliceSlider = new JSlider(0, 0, 0);
        sliceSlider.setMajorTickSpacing(10);
        sliceSlider.setPaintTicks(true);
        sliceSlider.setPaintLabels(false); // ラベルは非表示にしてはみ出しを防ぐ
        sliceSliderChangeListener = e -> {
            if (!sliceSlider.getValueIsAdjusting()) {
                gotoSlice(sliceSlider.getValue());
            }
        };
        sliceSlider.addChangeListener(sliceSliderChangeListener);
        
        sliceLabel = new JLabel(messages.get("control.slice.format", 0, 0));
        
        // ウィンドウ/レベル入力のイベントハンドラ
        wlActionListener = e -> {
            try {
                double center = Double.parseDouble(windowCenterField.getText());
                double width = Double.parseDouble(windowWidthField.getText());
                imageViewerPanel.setWindowLevel(center, width);
            } catch (NumberFormatException ex) {
                // 無効な値は無視
            }
        };
        
        windowCenterField.addActionListener(wlActionListener);
        windowWidthField.addActionListener(wlActionListener);
        
        resetButton.addActionListener(e -> {
            imageViewerPanel.resetView();
            updateControlPanel();
        });
    }
    
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder(messages.get("panel.control")));
        
        // 上部：スライスとWW/WLコントロール
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        // スライスコントロール
        topPanel.add(new JLabel(messages.get("control.slice.label")));
        sliceSlider.setPreferredSize(new Dimension(200, 40));
        topPanel.add(sliceSlider);
        topPanel.add(sliceLabel);
        
        topPanel.add(Box.createHorizontalStrut(20));
        
        // ウィンドウ/レベルコントロール
        topPanel.add(new JLabel(messages.get("control.window_level")));
        windowCenterField.setPreferredSize(new Dimension(80, 25));
        topPanel.add(windowCenterField);
        topPanel.add(new JLabel(messages.get("control.window_width")));
        windowWidthField.setPreferredSize(new Dimension(80, 25));
        topPanel.add(windowWidthField);
        topPanel.add(resetButton);
        
        controlPanel.add(topPanel, BorderLayout.CENTER);
        
        // 下部：操作ヘルプ
        JLabel helpLabel = new JLabel(messages.get("control.help"));
        helpLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        helpLabel.setForeground(new Color(100, 100, 100)); // より濃いグレーで見やすく
        helpLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        controlPanel.add(helpLabel, BorderLayout.SOUTH);
        
        return controlPanel;
    }
    
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusLabel = new JLabel(messages.get("panel.status_ready"));
        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        return statusBar;
    }
    
    /**
     * コントロールパネルを更新
     */
    private void updateControlPanel() {
        SwingUtilities.invokeLater(() -> {
            // イベントリスナーを一時的に無効化して無限ループを防ぐ
            windowCenterField.removeActionListener(wlActionListener);
            windowWidthField.removeActionListener(wlActionListener);
            
            // 値を更新
            windowCenterField.setText(String.format("%.1f", imageViewerPanel.getWindowCenter()));
            windowWidthField.setText(String.format("%.1f", imageViewerPanel.getWindowWidth()));
            
            // イベントリスナーを再登録
            windowCenterField.addActionListener(wlActionListener);
            windowWidthField.addActionListener(wlActionListener);
        });
    }

    private void setupMenuBar() {
        menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu(messages.get("menu.file.name"));
        fileMenu.setMnemonic('F');

        JMenuItem importMenuItem = new JMenuItem(messages.get("menu.file.import"));
        importMenuItem.setMnemonic('I');
        importMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, 
                java.awt.event.InputEvent.CTRL_DOWN_MASK));
        importMenuItem.addActionListener(e -> handleImport());

        JMenuItem exitMenuItem = new JMenuItem(messages.get("menu.file.exit"));
        exitMenuItem.setMnemonic('X');
        exitMenuItem.addActionListener(e -> {
            if (dbManager != null) {
                dbManager.close();
            }
            System.exit(0);
        });

        fileMenu.add(importMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);
        
        // 設定メニュー
        JMenu settingsMenu = new JMenu(messages.get("menu.settings.name"));
        settingsMenu.setMnemonic('S');
        
        JMenuItem settingsMenuItem = new JMenuItem(messages.get("menu.settings.open"));
        settingsMenuItem.setMnemonic('S');
        settingsMenuItem.addActionListener(e -> showSettingsDialog());
        
        settingsMenu.add(settingsMenuItem);
        menuBar.add(settingsMenu);

        JMenu helpMenu = new JMenu(messages.get("menu.help.name"));
        helpMenu.setMnemonic('H');

        JMenuItem aboutMenuItem = new JMenuItem(messages.get("menu.help.about"));
        aboutMenuItem.setMnemonic('A');
        aboutMenuItem.addActionListener(e -> showAboutDialog());

        helpMenu.add(aboutMenuItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }
    
    /**
     * 削除処理（2段階確認）
     */
    private void handleDelete() {
        Study selectedStudy = studyListView.getSelectedValue();
        if (selectedStudy == null) {
            JOptionPane.showMessageDialog(this, 
                messages.get("delete.no_study_selected"),
                messages.get("message.error.title"),
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 1段階目：削除するかどうかの確認
        String patientName = selectedStudy.getPatientName() != null ? selectedStudy.getPatientName() : "Unknown";
        String confirmMsg = messages.get("delete.confirm_message",
            patientName,
            selectedStudy.getStudyInstanceUID());
        int firstResult = JOptionPane.showConfirmDialog(this,
            confirmMsg,
            messages.get("delete.confirm_title"),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (firstResult != JOptionPane.OK_OPTION) {
            return; // キャンセル
        }
        
        // 2段階目：削除方法の選択（はい：DB+DCM、いいえ：DBのみ、キャンセル：何もしない）
        String[] options = {
            messages.get("delete.button.yes"),
            messages.get("delete.button.no"),
            messages.get("delete.button.cancel")
        };
        int secondResult = JOptionPane.showOptionDialog(this,
            messages.get("delete.method_message"),
            messages.get("delete.method_title"),
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[2]); // デフォルトはキャンセル
        
        if (secondResult == JOptionPane.CANCEL_OPTION || secondResult == JOptionPane.CLOSED_OPTION) {
            return; // キャンセルまたはウィンドウを閉じた
        }
        
        boolean deleteFiles = (secondResult == JOptionPane.YES_OPTION); // はいならファイルも削除
        
        try {
            String studyUID = selectedStudy.getStudyInstanceUID();
            int deletedFiles = 0;
            int failedFiles = 0;
            
            if (deleteFiles) {
                // ファイルも削除する場合
                // 削除対象のファイルパスを取得
                List<Path> filePaths = studyRepository.getFilePathsForStudy(studyUID);
                
                // ストレージに保存されているファイルを削除
                for (Path filePath : filePaths) {
                    try {
                        if (java.nio.file.Files.exists(filePath)) {
                            java.nio.file.Files.delete(filePath);
                            deletedFiles++;
                            logger.debug("DICOMファイルを削除しました: {}", filePath);
                        }
                    } catch (Exception e) {
                        failedFiles++;
                        logger.warn("DICOMファイルの削除に失敗: {} - エラー: {}", filePath, e.getMessage());
                    }
                }
                
                // 空のディレクトリを削除
                com.jjdicomviewer.config.AppConfig appConfig = com.jjdicomviewer.config.AppConfig.getInstance();
                Path storageBasePath = appConfig.getStorageBasePath();
                Path studyDir = storageBasePath.resolve(studyUID);
                if (java.nio.file.Files.exists(studyDir) && java.nio.file.Files.isDirectory(studyDir)) {
                    try {
                        // ディレクトリ内のファイルを再帰的に削除
                        java.nio.file.Files.walkFileTree(studyDir, new java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
                            @Override
                            public java.nio.file.FileVisitResult visitFile(java.nio.file.Path file, 
                                    java.nio.file.attribute.BasicFileAttributes attrs) throws java.io.IOException {
                                try {
                                    java.nio.file.Files.delete(file);
                                } catch (Exception e) {
                                    logger.warn("ファイルの削除に失敗: {} - エラー: {}", file, e.getMessage());
                                }
                                return java.nio.file.FileVisitResult.CONTINUE;
                            }
                            
                            @Override
                            public java.nio.file.FileVisitResult postVisitDirectory(java.nio.file.Path dir, 
                                    java.io.IOException exc) throws java.io.IOException {
                                try {
                                    java.nio.file.Files.delete(dir);
                                } catch (Exception e) {
                                    logger.warn("ディレクトリの削除に失敗: {} - エラー: {}", dir, e.getMessage());
                                }
                                return java.nio.file.FileVisitResult.CONTINUE;
                            }
                        });
                        logger.debug("ディレクトリを削除しました: {}", studyDir);
                    } catch (Exception e) {
                        logger.warn("ディレクトリの削除に失敗: {} - エラー: {}", studyDir, e.getMessage());
                    }
                }
            }
            
            // データベースから削除（ファイル削除の有無に関わらず）
            studyRepository.deleteStudy(studyUID);
            
            if (deleteFiles) {
                logger.info("スタディを削除しました: {} (DB+DCM、ファイル: {}件削除, {}件失敗)", 
                    studyUID, deletedFiles, failedFiles);
                
                String message = messages.get("delete.completed_both", deletedFiles);
                if (failedFiles > 0) {
                    message += messages.get("delete.failed_files", failedFiles);
                }
                statusLabel.setText(message);
            } else {
                logger.info("スタディを削除しました: {} (DBのみ)", studyUID);
                statusLabel.setText(messages.get("delete.completed_db_only"));
            }
            
            refreshStudyList();
        } catch (Exception e) {
            logger.error("スタディの削除に失敗しました", e);
            JOptionPane.showMessageDialog(this,
                messages.get("delete.error", e.getMessage()),
                messages.get("message.error.title"),
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * インポート処理
     */
    private void handleImport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(messages.get("import.dialog_title"));
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            importFromFolder(selectedDirectory.toPath());
        }
    }
    
    /**
     * フォルダからインポート（非同期）
     */
    private void importFromFolder(Path folderPath) {
        progressBar.setVisible(true);
        progressBar.setIndeterminate(false);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(0);
        statusLabel.setText(messages.get("import.progress"));
        
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                importService.importFromFolder(folderPath, (current, total, message) -> {
                    publish(message);
                    // currentとtotalが同じ場合（100%完了）を考慮
                    int progress = total > 0 ? (int) (100 * current / (double) total) : 0;
                    setProgress(Math.min(100, Math.max(0, progress)));
                });
                return null;
            }
            
            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    statusLabel.setText(chunks.get(chunks.size() - 1));
                }
            }
            
            @Override
            protected void done() {
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);
                
                try {
                    get(); // 例外が発生していた場合、ここで取得
                    statusLabel.setText(messages.get("import.completed"));
                } catch (Exception e) {
                    logger.error("インポート処理中にエラーが発生しました", e);
                    statusLabel.setText("インポートエラー: " + e.getMessage());
                    JOptionPane.showMessageDialog(MainFrame.this,
                        "インポート中にエラーが発生しました:\n" + e.getMessage(),
                        messages.get("message.error.title"),
                        JOptionPane.ERROR_MESSAGE);
                }
                
                refreshStudyList();
            }
        };
        
        worker.addPropertyChangeListener(e -> {
            if ("progress".equals(e.getPropertyName())) {
                progressBar.setValue((Integer) e.getNewValue());
            }
        });
        
        worker.execute();
    }
    
    /**
     * スタディリストを更新
     */
    private void refreshStudyList() {
        try {
            List<Study> studies = studyRepository.findAllStudies();
            studyListModel.clear();
            for (Study study : studies) {
                studyListModel.addElement(study);
            }
            statusLabel.setText(messages.get("status.studies_loaded", studies.size()));
        } catch (SQLException e) {
            logger.error(messages.get("status.study_list_update_failed"), e);
            JOptionPane.showMessageDialog(this, 
                messages.get("status.study_list_load_failed", e.getMessage()), 
                messages.get("message.error.title"), 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * スタディが選択されたときの処理
     */
    private void onStudySelected(Study study) {
        String studyUID = study != null ? study.getStudyInstanceUID() : "null";
        logger.info(messages.get("message.info.study_selected", studyUID));
        
        this.currentStudy = study;
        this.currentSeries = null;
        this.currentInstances = null;
        currentInstanceIndex = 0;
        
        // ビューアをクリア
        imageViewerPanel.loadSeries(null);
        sliceSlider.setMaximum(0);
        sliceSlider.setValue(0);
        sliceLabel.setText(messages.get("control.slice.format", 0, 0));
        
        if (study == null) {
            seriesListModel.clear();
            statusLabel.setText(messages.get("status.study_not_selected"));
            return;
        }
        
        try {
            // シリーズリストを更新
            study.setSeriesList(studyRepository.findSeriesByStudyUID(study.getStudyInstanceUID()));
            
            seriesListModel.clear();
            for (Series series : study.getSeriesList()) {
                String description = messages.get("series.description_format", 
                    series.getModality() != null ? series.getModality() : "Unknown",
                    series.getSeriesDescription() != null ? series.getSeriesDescription() : "No Description",
                    series.getInstanceCount());
                seriesListModel.addElement(description);
            }
            
            // シリーズ選択をクリア
            seriesListView.clearSelection();
            
            statusLabel.setText(messages.get("status.study_selected", 
                study.getPatientName() != null ? study.getPatientName() : "Unknown",
                study.getSeriesList().size()));
        } catch (SQLException e) {
            logger.error(messages.get("status.series_list_load_failed"), e);
            JOptionPane.showMessageDialog(this, 
                messages.get("status.series_list_load_failed", e.getMessage()), 
                messages.get("message.error.title"), 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * シリーズが選択されたときの処理
     */
    private void onSeriesSelected(int selectedIndex) {
        if (currentStudy == null || selectedIndex < 0) {
            return;
        }
        
        if (selectedIndex >= currentStudy.getSeriesList().size()) {
            return;
        }
        
        Series selectedSeries = currentStudy.getSeriesList().get(selectedIndex);
        logger.info(messages.get("message.info.series_selected", selectedSeries.getSeriesInstanceUID()));
        
        this.currentSeries = selectedSeries;
        this.currentInstances = selectedSeries.getInstanceList();
        currentInstanceIndex = 0;
        
        // インスタンスをソート（InstanceNumber順）
        if (currentInstances != null) {
            currentInstances.sort((a, b) -> {
                int numA = a.getInstanceNumber() != null ? a.getInstanceNumber() : 0;
                int numB = b.getInstanceNumber() != null ? b.getInstanceNumber() : 0;
                return Integer.compare(numA, numB);
            });
        }
        
        // スライダーを更新
        int instanceCount = currentInstances != null ? currentInstances.size() : 0;
        sliceSlider.setMaximum(Math.max(0, instanceCount - 1));
        sliceSlider.setValue(0);
        updateSliceLabel();
        
        // ビューアに読み込む（新しいシリーズなのでWW/WLをリセット）
        imageViewerPanel.loadSeries(selectedSeries);
        
        // WW/WLの入力ボックスを更新
        updateControlPanel();
        
        statusLabel.setText(messages.get("status.series_selected", 
            selectedSeries.getSeriesDescription() != null ? 
                selectedSeries.getSeriesDescription() : "No Description",
            selectedSeries.getInstanceCount()));
    }
    
    /**
     * 指定されたスライスに移動
     */
    private void gotoSlice(int index) {
        if (currentInstances == null || index < 0 || index >= currentInstances.size()) {
            return;
        }
        
        // スライダーの値が異なる場合のみ更新（無限ループを防ぐ）
        if (sliceSlider.getValue() != index) {
            // ChangeListenerを一時的に削除してから値を更新
            sliceSlider.removeChangeListener(sliceSliderChangeListener);
            sliceSlider.setValue(index);
            sliceSlider.addChangeListener(sliceSliderChangeListener);
        }
        
        currentInstanceIndex = index;
        Instance instance = currentInstances.get(index);
        // 同一シリーズ内のスクロールなので、WW/WLを維持
        imageViewerPanel.loadInstance(instance, false);
        updateSliceLabel();
    }
    
    /**
     * スライスラベルを更新
     */
    private void updateSliceLabel() {
        int total = currentInstances != null ? currentInstances.size() : 0;
        sliceLabel.setText(messages.get("control.slice.format", currentInstanceIndex + 1, total));
    }

    /**
     * 設定ダイアログを表示
     */
    private void showSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog(this);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            JOptionPane.showMessageDialog(this, 
                messages.get("dialog.settings.saved"), 
                messages.get("dialog.settings.saved_title"), 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showAboutDialog() {
        StringBuilder message = new StringBuilder();
        message.append(messages.get("app.title")).append("\n\n");
        message.append(messages.get("dialog.about.version_label")).append(" ").append(messages.get("app.version")).append("\n\n");
        message.append(messages.get("dialog.about.description")).append("\n\n");
        message.append("操作:\n");
        message.append(messages.get("dialog.about.operations.0")).append("\n");
        message.append(messages.get("dialog.about.operations.1")).append("\n");
        message.append(messages.get("dialog.about.operations.2")).append("\n");
        message.append(messages.get("dialog.about.operations.3")).append("\n");
        message.append("\n").append(messages.get("app.copyright"));
        
        JOptionPane.showMessageDialog(this, message.toString(), messages.get("dialog.about.title"), 
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * アプリケーションアイコンを設定
     */
    private void setApplicationIcon() {
        try (InputStream iconStream = getClass().getResourceAsStream("/icons/app-icon.png")) {
            if (iconStream != null) {
                Image icon = ImageIO.read(iconStream);
                if (icon != null) {
                    setIconImage(icon);
                    return;
                }
            }
            logger.warn("アプリアイコンの読み込みに失敗しました: /icons/app-icon.png が見つかりません");
        } catch (IOException e) {
            logger.warn("アプリアイコンの読み込み中にエラーが発生しました", e);
        }
    }
}
