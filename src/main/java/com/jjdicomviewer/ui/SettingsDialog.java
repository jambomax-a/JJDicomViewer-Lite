package com.jjdicomviewer.ui;

import com.jjdicomviewer.config.AppConfig;
import com.jjdicomviewer.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 設定ダイアログ（Swing版）
 */
public class SettingsDialog extends JDialog {
    
    private static final Logger logger = LoggerFactory.getLogger(SettingsDialog.class);
    
    private final AppConfig appConfig;
    private final Messages messages;
    private JTextField storagePathField;
    private JTextField databasePathField;
    private JComboBox<String> languageComboBox;
    private boolean saved = false;
    
    public SettingsDialog(JFrame owner) {
        super(owner, Messages.getInstance().get("dialog.settings.title"), true);
        this.appConfig = AppConfig.getInstance();
        this.messages = Messages.getInstance(); // インスタンスフィールドとして保持
        
        initializeComponents();
        setupLayout();
        
        setSize(600, 250);
        setLocationRelativeTo(owner);
    }
    
    private void initializeComponents() {
        storagePathField = new JTextField(30);
        storagePathField.setText(appConfig.getStorageBasePath().toString());
        storagePathField.setEditable(false);
        
        databasePathField = new JTextField(30);
        databasePathField.setText(appConfig.getDatabasePath().toString());
        databasePathField.setEditable(false);
        
        // 言語選択
        languageComboBox = new JComboBox<>(new String[]{"ja", "en"});
        languageComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null && component instanceof JLabel) {
                    JLabel label = (JLabel) component;
                    String lang = value.toString();
                    // 最新のMessagesインスタンスを取得
                    Messages currentMessages = Messages.getInstance();
                    if ("ja".equals(lang)) {
                        label.setText(currentMessages.get("language.japanese"));
                    } else if ("en".equals(lang)) {
                        label.setText(currentMessages.get("language.english"));
                    } else {
                        label.setText(lang);
                    }
                }
                return component;
            }
        });
        String currentLang = appConfig.getLanguage();
        languageComboBox.setSelectedItem(currentLang != null ? currentLang : "ja");
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // ストレージパス
        gbc.gridx = 0;
        gbc.gridy = 0;
        contentPanel.add(new JLabel(messages.get("dialog.settings.storage_path")), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        contentPanel.add(storagePathField, gbc);
        
        JButton storageBrowseButton = new JButton(messages.get("dialog.settings.browse"));
        storageBrowseButton.addActionListener(e -> browseStoragePath());
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        contentPanel.add(storageBrowseButton, gbc);
        
        // データベースパス
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        contentPanel.add(new JLabel(messages.get("dialog.settings.database_path")), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        contentPanel.add(databasePathField, gbc);
        
        JButton databaseBrowseButton = new JButton(messages.get("dialog.settings.browse"));
        databaseBrowseButton.addActionListener(e -> browseDatabasePath());
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        contentPanel.add(databaseBrowseButton, gbc);
        
        // 言語設定
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        contentPanel.add(new JLabel(messages.get("dialog.settings.language")), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        contentPanel.add(languageComboBox, gbc);
        
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        contentPanel.add(Box.createHorizontalStrut(80), gbc); // ボタンと同じ幅のスペース
        
        // 注意事項
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel noteLabel = new JLabel(
            "<html>" + messages.get("dialog.settings.note").replace("\n", "<br>") + "</html>"
        );
        noteLabel.setForeground(new Color(200, 50, 50)); // 注意事項らしい赤に近い色合い
        noteLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        contentPanel.add(noteLabel, gbc);
        
        add(contentPanel, BorderLayout.CENTER);
        
        // ボタンパネル
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton(messages.get("dialog.settings.save"));
        saveButton.addActionListener(e -> {
            if (validateAndSave()) {
                saved = true;
                dispose();
            }
        });
        JButton cancelButton = new JButton(messages.get("dialog.settings.cancel"));
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void browseStoragePath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(messages.get("dialog.settings.storage_path"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        String currentPath = storagePathField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            try {
                Path path = Paths.get(currentPath);
                if (path.toFile().exists() && path.toFile().isDirectory()) {
                    chooser.setCurrentDirectory(path.toFile());
                }
            } catch (Exception ex) {
                // 無視
            }
        }
        
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            storagePathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void browseDatabasePath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(messages.get("dialog.settings.database_path"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        String currentPath = databasePathField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            try {
                Path path = Paths.get(currentPath);
                if (path.toFile().exists() && path.toFile().isFile()) {
                    chooser.setCurrentDirectory(path.getParent().toFile());
                    chooser.setSelectedFile(path.toFile());
                } else if (path.getParent() != null && path.getParent().toFile().exists()) {
                    chooser.setCurrentDirectory(path.getParent().toFile());
                }
            } catch (Exception ex) {
                // 無視
            }
        }
        
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            // .db拡張子がない場合は追加
            String fileName = selectedFile.getName();
            if (!fileName.toLowerCase().endsWith(".db") && 
                !fileName.toLowerCase().endsWith(".sqlite") && 
                !fileName.toLowerCase().endsWith(".sqlite3")) {
                selectedFile = new File(selectedFile.getParent(), fileName + ".db");
            }
            databasePathField.setText(selectedFile.getAbsolutePath());
        }
    }
    
    private boolean validateAndSave() {
        String storagePath = storagePathField.getText().trim();
        String databasePath = databasePathField.getText().trim();
        
        if (storagePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                messages.get("message.error.storage_path_required"), 
                messages.get("message.error.title"), 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (databasePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                messages.get("message.error.database_path_required"), 
                messages.get("message.error.title"), 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        try {
            // パスの検証
            Path storage = Paths.get(storagePath);
            Path database = Paths.get(databasePath);
            
            // ストレージディレクトリが存在しない場合は作成を試みる
            if (!storage.toFile().exists()) {
                try {
                    java.nio.file.Files.createDirectories(storage);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, 
                        messages.get("message.error.storage_dir_create_failed", ex.getMessage()), 
                        messages.get("message.error.title"), 
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            
            // データベースディレクトリが存在しない場合は作成を試みる
            if (database.getParent() != null && !database.getParent().toFile().exists()) {
                try {
                    java.nio.file.Files.createDirectories(database.getParent());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, 
                        messages.get("message.error.database_dir_create_failed", ex.getMessage()), 
                        messages.get("message.error.title"), 
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            
            // 設定を保存
            appConfig.setStoragePath(storagePath);
            appConfig.setDatabasePath(databasePath);
            
            // 言語設定を保存
            String selectedLang = (String) languageComboBox.getSelectedItem();
            if (selectedLang != null) {
                appConfig.setLanguage(selectedLang);
                // メッセージを再読み込み
                Messages.reload();
                // コンボボックスの表示を更新
                languageComboBox.repaint();
            }
            
            JOptionPane.showMessageDialog(this, 
                messages.get("dialog.settings.saved"), 
                messages.get("dialog.settings.saved_title"), 
                JOptionPane.INFORMATION_MESSAGE);
            
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                messages.get("message.error.settings_save_failed", e.getMessage()), 
                messages.get("message.error.title"), 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    public boolean isSaved() {
        return saved;
    }
}

