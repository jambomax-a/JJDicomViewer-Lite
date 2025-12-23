package com.jj.dicomviewer.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * ThreadCellのカスタムレンダラー
 * HOROS-20240407準拠: NSTableViewのdataCellForTableColumnでThreadCellを返す実装に対応
 */
public class ThreadCellRenderer extends DefaultTableCellRenderer {
    
    private final BrowserActivityHelper activityHelper;
    
    public ThreadCellRenderer(BrowserActivityHelper activityHelper) {
        this.activityHelper = activityHelper;
        setOpaque(false);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                                                   boolean hasFocus, int row, int column) {
        // HOROS-20240407準拠: ThreadCellを取得
        ThreadCell cell = activityHelper.getDataCellForTableColumn(row);
        if (cell != null) {
            // HOROS-20240407準拠: willDisplayCellを呼び出す（progressIndicatorとcancelButtonをtableViewに追加）
            activityHelper.willDisplayCell(table, cell, row);
            
            // HOROS-20240407準拠: NSCellのdrawWithFrameを呼び出すためのカスタムパネル
            java.awt.Rectangle cellRect = table.getCellRect(row, column, false);
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    // HOROS-20240407準拠: drawWithFrame相当の処理
                    // paintComponentでは、getBounds()はパネルのサイズ（セルのサイズと一致）を返す
                    // ただし、描画は(0,0)から始まるため、getBounds()ではなくgetSize()を使用
                    java.awt.Dimension size = getSize();
                    java.awt.Rectangle bounds = new java.awt.Rectangle(0, 0, size.width, size.height);
                    cell.drawWithFrame(bounds, g);
                }
            };
            panel.setOpaque(false);
            panel.setLayout(null);
            // HOROS-20240407準拠: セルのサイズに合わせてパネルのサイズを設定
            panel.setPreferredSize(cellRect.getSize());
            panel.setSize(cellRect.getSize());
            panel.setMinimumSize(cellRect.getSize());
            panel.setMaximumSize(cellRect.getSize());
            
            return panel;
        }
        
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
