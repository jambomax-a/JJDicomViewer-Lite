package com.jjdicomviewer.app;

import com.jjdicomviewer.ui.MainFrame;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JJ Dicom Viewer 簡易版 - メインアプリケーションクラス（Swing版）
 */
public class JJDicomViewerApp {
    private static final Logger logger = LoggerFactory.getLogger(JJDicomViewerApp.class);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                logger.info("JJ Dicom Viewer 簡易版（Swing版）を起動します");
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
                logger.info("アプリケーションの起動が完了しました");
            } catch (Exception e) {
                logger.error("アプリケーションの起動に失敗しました", e);
                System.err.println("エラーが発生しました: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}

