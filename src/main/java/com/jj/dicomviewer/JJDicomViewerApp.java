package com.jj.dicomviewer;

import com.jj.dicomviewer.ui.BrowserController;

import javax.swing.SwingUtilities;

/**
 * JJDicomViewerApp - HOROS-20240407準拠のメインアプリケーションクラス
 * 
 * HOROS-20240407のAppControllerの起動処理を参考に実装
 */
public class JJDicomViewerApp {
    
    /**
     * メインメソッド
     * HOROS-20240407準拠でBrowserControllerを起動
     */
    public static void main(String[] args) {
        // 未処理例外ハンドラーを設定（スタックトレースを抑制）
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            // ConcurrentModificationException、ArrayIndexOutOfBoundsException、NullPointerExceptionは抑制
            if (e instanceof java.util.ConcurrentModificationException ||
                e instanceof ArrayIndexOutOfBoundsException ||
                e instanceof NullPointerException) {
                // ログを抑制（重要なログが見やすくなるように）
                return;
            }
            // その他の例外はログ出力
            System.err.println("Uncaught exception in thread: " + t.getName());
            e.printStackTrace();
        });
        
        // Swingイベントディスパッチスレッドで実行
        SwingUtilities.invokeLater(() -> {
            try {
                // HOROS-20240407準拠：BrowserControllerを起動
                // コンストラクタ内でinitializeUIComponents()が呼び出され、
                // その中でawakeFromNib()が呼び出されるため、ここでは呼び出す必要がない
                BrowserController browser = new BrowserController();
                
                // ウィンドウを表示
                // awakeFromNib()内で既にsetVisible(true)が呼び出されるが、
                // 念のためここでも呼び出す
                if (!browser.isVisible()) {
                    browser.setVisible(true);
                }
                
            } catch (Exception e) {
                // デバッグログは条件付きで出力（必要に応じて有効化）
                // e.printStackTrace();
                System.err.println("Failed to start JJDicomViewer: " + e.getMessage());
                System.exit(1);
            }
        });
    }
}

