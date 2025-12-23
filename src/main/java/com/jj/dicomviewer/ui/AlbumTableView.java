package com.jj.dicomviewer.ui;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.MouseEvent;

/**
 * AlbumTableView - アルバムテーブルビュー
 * 
 * HOROS-20240407のalbumTableをJava Swingに移植
 * NSTableViewの代わりにJTableを使用
 */
public class AlbumTableView extends JTable {
    
    private BrowserController browserController;
    
    /**
     * コンストラクタ
     * HOROS-20240407準拠
     */
    public AlbumTableView(BrowserController browserController) {
        super();
        this.browserController = browserController;
        
        // HOROS-20240407準拠: デフォルト設定
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // HOROS-20240407準拠: デフォルトのアルバムモデルを設定（初期は空）
        // HOROS-20240407準拠: albumArrayはrefreshAlbums()で設定される
        DefaultTableModel model = new DefaultTableModel(
            new String[] { "Albums" },
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        setModel(model);
        
        // HOROS-20240407準拠: 選択変更リスナー
        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    albumTableSelectionChanged();
                }
            }
        });
        
        // HOROS-20240407準拠: コンテキストメニューを設定
        setComponentPopupMenu(createContextMenu());
    }
    
    /**
     * コンテキストメニューを作成
     * HOROS-20240407準拠
     */
    private javax.swing.JPopupMenu createContextMenu() {
        javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
        
        javax.swing.JMenuItem addItem = new javax.swing.JMenuItem("Add Album");
        addItem.addActionListener(e -> {
            if (browserController != null) {
                browserController.addAlbum(e.getSource());
            }
        });
        menu.add(addItem);
        
        javax.swing.JMenuItem addSmartItem = new javax.swing.JMenuItem("Add Smart Album");
        addSmartItem.addActionListener(e -> {
            if (browserController != null) {
                browserController.addSmartAlbum(e.getSource());
            }
        });
        menu.add(addSmartItem);
        
        menu.addSeparator();
        
        javax.swing.JMenuItem removeItem = new javax.swing.JMenuItem("Delete Album");
        removeItem.addActionListener(e -> {
            if (browserController != null) {
                browserController.removeAlbum(e.getSource());
            }
        });
        menu.add(removeItem);
        
        return menu;
    }
    
    /**
     * アルバム選択が変更された
     * HOROS-20240407準拠
     */
    private void albumTableSelectionChanged() {
        if (browserController != null) {
            // HOROS-20240407準拠: outlineViewRefreshを呼び出す
            browserController.outlineViewRefresh();
        }
    }
    
    /**
     * 選択された行のインデックスを取得
     */
    public int[] getSelectedRowIndexes() {
        return getSelectedRows();
    }
    
    /**
     * 行を選択
     */
    public void selectRowIndexes(int[] indexes, boolean extend) {
        if (indexes.length == 0) {
            clearSelection();
            return;
        }
        
        if (!extend) {
            clearSelection();
        }
        
        for (int index : indexes) {
            if (index >= 0 && index < getRowCount()) {
                addRowSelectionInterval(index, index);
            }
        }
    }
    
    /**
     * 識別子で列を取得
     */
    public TableColumn getTableColumnWithIdentifier(String identifier) {
        for (int i = 0; i < getColumnCount(); i++) {
            TableColumn column = getColumnModel().getColumn(i);
            if (identifier.equals(column.getIdentifier())) {
                return column;
            }
        }
        return null;
    }
    
    /**
     * ドラッグタイプを登録
     */
    public void registerForDraggedTypes(String[] types) {
        // TODO: ドラッグ&ドロップで受け入れるデータタイプを設定
    }
    
    /**
     * データを再読み込み
     * HOROS-20240407準拠
     */
    public void reloadData() {
        DefaultTableModel model = (DefaultTableModel) getModel();
        if (model != null) {
            model.fireTableDataChanged();
        }
    }
    
    /**
     * マウスダウンイベント
     * HOROS-20240407準拠
     */
    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        
        if (e.getID() == MouseEvent.MOUSE_CLICKED && e.getClickCount() == 2) {
            // HOROS-20240407準拠: albumTableDoublePressedアクションを実行
            if (browserController != null) {
                // TODO: アルバム編集ダイアログを開く
            }
        }
    }
}
