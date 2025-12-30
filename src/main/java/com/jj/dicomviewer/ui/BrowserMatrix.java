package com.jj.dicomviewer.ui;

import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.LayoutManager;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.LayoutManager;
import java.awt.Container;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * BrowserMatrix - ブラウザマトリックス
 * 
 * HOROS-20240407のBrowserMatrixをJava Swingに移植
 * NSMatrixの代わりにJPanelとGridLayoutを使用
 */
public class BrowserMatrix extends JPanel implements DragGestureListener, DragSourceListener, KeyListener {
    
    private boolean avoidRecursive = false;
    private BrowserController browserController;
    private List<JButton> cells;
    
    /**
     * BrowserControllerを設定
     */
    public void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
    }
    private int rows = 0;
    private int columns = 0;
    private Dimension cellSize = new Dimension(105, 113);
    private int intercellSpacingX = 0;
    private int intercellSpacingY = 0;
    // HOROS-20240407準拠: 垂直スクロールバーの幅分の余白を確保
    // MainMenu.xib準拠: マトリックスの幅がスクロールビューの幅より広い（424px vs 384px）
    private int scrollbarWidth = 0;
    
    /**
     * カスタムレイアウトマネージャー：セルサイズを固定
     * HOROS-20240407準拠：セルサイズを自動調整させない
     */
    private class FixedCellSizeLayout implements LayoutManager {
        @Override
        public void addLayoutComponent(String name, java.awt.Component comp) {
        }
        
        @Override
        public void removeLayoutComponent(java.awt.Component comp) {
        }
        
        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return sizeToCells();
        }
        
        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return sizeToCells();
        }
        
        @Override
        public void layoutContainer(Container parent) {
            if (rows == 0 || columns == 0) {
                return;
            }
            
            int x = 0;
            int y = 0;
            int cellWidth = cellSize.width;
            int cellHeight = cellSize.height;
            int spacingX = Math.max(0, intercellSpacingX);
            int spacingY = Math.max(0, intercellSpacingY);
            
            // HOROS-20240407準拠：NSMatrixの動作を再現
            // NSMatrixはsetCellSize:でセルサイズを固定し、自動調整しない
            // Java Swingでは、カスタムレイアウトマネージャーで同じ動作を実現
            for (int i = 0; i < cells.size(); i++) {
                JButton cell = cells.get(i);
                if (cell != null) {
                    // HOROS-20240407準拠：setCellSize:で設定されたセルサイズを強制的に適用
                    // NSMatrixの動作を再現：セルサイズは固定され、自動調整されない
                    cell.setBounds(x, y, cellWidth, cellHeight);
                    // HOROS-20240407準拠：セルサイズを固定（自動調整を防ぐ）
                    cell.setPreferredSize(cellSize);
                    cell.setMinimumSize(cellSize);
                    cell.setMaximumSize(cellSize);
                }
                
                // HOROS-20240407準拠：次のセルの位置を計算（NSMatrixのレイアウト動作を再現）
                if ((i + 1) % columns == 0) {
                    // 次の行
                    x = 0;
                    y += cellHeight + spacingY;
                } else {
                    // 次の列
                    x += cellWidth + spacingX;
                }
            }
        }
        
        private Dimension sizeToCells() {
            if (rows > 0 && columns > 0) {
                int width = columns * cellSize.width + (columns - 1) * Math.max(0, intercellSpacingX);
                // HOROS-20240407準拠: 垂直スクロールバーの幅分の余白を追加
                // MainMenu.xib準拠: マトリックスの幅がスクロールビューの幅より広い
                width += scrollbarWidth;
                int height = rows * cellSize.height + (rows - 1) * Math.max(0, intercellSpacingY);
                return new Dimension(width, height);
            }
            return new Dimension(0, 0);
        }
    }
    
    /**
     * セル間のスペーシングを取得
     * HOROS-20240407準拠
     */
    public Dimension getIntercellSpacing() {
        return new Dimension(intercellSpacingX, intercellSpacingY);
    }
    
    /**
     * セルサイズを取得
     * HOROS-20240407準拠
     */
    public Dimension getCellSize() {
        return new Dimension(cellSize);
    }
    
    /**
     * マトリックスのサイズをセルに合わせて調整
     * HOROS-20240407準拠: [oMatrix sizeToCells] (BrowserMatrix.m - NSMatrixのデフォルト実装を使用)
     * セルのサイズと間隔に基づいてマトリックスのサイズを計算する
     * スクロールバーの幅は考慮しない（previewMatrixScrollViewFrameDidChangeで考慮される）
     */
    public void sizeToCells() {
        if (rows > 0 && columns > 0) {
            // セルに基づく幅を計算
            int width = columns * cellSize.width + (columns - 1) * Math.max(0, intercellSpacingX);
            int height = rows * cellSize.height + (rows - 1) * Math.max(0, intercellSpacingY);
            java.awt.Dimension size = new java.awt.Dimension(width, height);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(new java.awt.Dimension(width, Integer.MAX_VALUE));
            // HOROS-20240407準拠: マトリックスのサイズを直接設定
            // JScrollPaneのJViewportが正しく認識するように
            setSize(size);
            // レイアウトを強制的に再計算
            invalidate();
            revalidate();
            repaint();
        }
    }
    
    /**
     * マトリックスのサイズをセルに合わせて調整（互換性のためのオーバーロード）
     * HOROS-20240407準拠: 引数なしのsizeToCells()を使用することを推奨
     */
    @Deprecated
    public void sizeToCells(int scrollbarWidth) {
        // 互換性のため、引数は無視してsizeToCells()を呼び出す
        sizeToCells();
    }
    
    /**
     * コンストラクタ
     */
    public BrowserMatrix(BrowserController browserController) {
        super();
        this.browserController = browserController;
        this.cells = new ArrayList<>();
        // HOROS-20240407準拠：セルサイズを固定するカスタムレイアウトマネージャーを使用
        setLayout(new FixedCellSizeLayout());
        setFocusable(true);
    }
    
    /**
     * セル間のスペースを設定
     * HOROS-20240407: [oMatrix setIntercellSpacing:NSMakeSize(-1, -1)]
     */
    public void setIntercellSpacing(int x, int y) {
        this.intercellSpacingX = x;
        this.intercellSpacingY = y;
        // HOROS-20240407準拠: NSMakeSize(-1, -1)は最小スペーシングを意味する
        // カスタムレイアウトマネージャーで処理されるため、ここでは値を保存するだけ
        revalidate();
        repaint();
    }
    
    /**
     * 行と列を更新
     * HOROS-20240407: - (void) renewRows:(NSInteger)rows columns:(NSInteger)columns
     */
    public void renewRows(int rows, int columns) {
        // HOROS-20240407準拠: rowsとcolsが両方0の場合は、セルをクリアするだけ
        if (rows == 0 && columns == 0) {
            this.rows = 0;
            this.columns = 0;
            
            // 既存のセルを削除
            removeAll();
            cells.clear();
            
            revalidate();
            repaint();
            return;
        }
        
        // rowsまたはcolumnsが0の場合は、デフォルト値を使用
        if (rows == 0) rows = 1;
        if (columns == 0) columns = 1;
        
        this.rows = rows;
        this.columns = columns;
        
        // 既存のセルを削除
        removeAll();
        cells.clear();
        
        // 新しいセルを作成
        for (int i = 0; i < rows * columns; i++) {
            JButton cell = new JButton();
            // HOROS-20240407準拠：セルサイズを強制的に固定
            cell.setPreferredSize(cellSize);
            cell.setMinimumSize(cellSize);
            cell.setMaximumSize(cellSize);
            cell.setOpaque(false);
            cell.setContentAreaFilled(false);
            cell.setBorderPainted(false);
            cell.setFocusPainted(false);
            // HOROS-20240407準拠: renewRowsでセルが再生成された場合、すべてのセルを無効化
            // matrixDisplayIconsで必要なセルを有効化する
            cell.setEnabled(false);
            cell.putClientProperty("tag", i); // タグとしてインデックスを設定
            
            // HOROS-20240407準拠: BrowserController.m 9503行目 - [cell setAction: @selector(matrixPressed:)];
            // セルがクリックされたときに、セルを選択してハイライトし、matrixPressedを呼び出す
            final int cellIndex = i;
            cell.addActionListener(e -> {
                // HOROS-20240407準拠: セルを選択してハイライト
                selectCellWithTag(cellIndex);
                // HOROS-20240407準拠: matrixPressedを呼び出す
                // 注: selectCellWithTagを呼び出した後、selectedCell()が正しく動作するようにする
                System.out.println("[DEBUG] BrowserMatrix.addActionListener() - cellIndex: " + cellIndex + ", cell: " + cell);
                if (browserController != null) {
                    // 少し遅延させてからmatrixPressedを呼び出す（selectedCell()が正しく動作するように）
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        browserController.matrixPressed(this);
                    });
                }
            });
            
            // HOROS-20240407準拠: ダブルクリック処理
            // BrowserController.m 14245行目 - [oMatrix setDoubleAction:@selector(matrixDoublePressed:)];
            cell.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        // HOROS-20240407準拠: matrixDoublePressedを呼び出す
                        if (browserController != null) {
                            browserController.matrixDoublePressed(BrowserMatrix.this);
                        }
                    }
                }
            });
            
            cells.add(cell);
            add(cell);
        }
        
        revalidate();
        repaint();
    }
    
    /**
     * 行数を取得
     * HOROS-20240407準拠: [oMatrix getNumberOfRows:&rows columns:&columns]
     */
    public int getRows() {
        return rows;
    }
    
    /**
     * 列数を取得
     * HOROS-20240407準拠: [oMatrix getNumberOfRows:&rows columns:&columns]
     */
    public int getColumns() {
        return columns;
    }
    
    /**
     * セルサイズを設定
     */
    public void setCellSize(Dimension size) {
        this.cellSize = size;
        for (JButton cell : cells) {
            cell.setPreferredSize(size);
            cell.setMinimumSize(size);
            cell.setMaximumSize(size);
        }
        revalidate();
        repaint();
    }
    
    /**
     * セルサイズを設定（幅と高さを指定）
     * HOROS-20240407準拠
     */
    public void setCellSize(int width, int height) {
        setCellSize(new Dimension(width, height));
    }
    
    /**
     * セル間隔を設定
     */
    public void setIntercellSpacing(Dimension spacing) {
        setIntercellSpacing(spacing.width, spacing.height);
    }
    
    /**
     * 最初のマウスイベントを受け入れるかどうか
     */
    public boolean acceptsFirstMouse(MouseEvent event) {
        return true;
    }
    
    /**
     * 選択されたセルを取得
     * HOROS-20240407準拠: - (id) selectedCell
     */
    public JButton selectedCell() {
        for (JButton cell : cells) {
            // HOROS-20240407準拠: selectCellWithTag()でsetOpaque(true)とsetBackground()を設定しているため、
            // isOpaque()とgetBackground()をチェックする
            if (cell.isVisible() && cell.isOpaque() && cell.getBackground() != null) {
                System.out.println("[DEBUG] BrowserMatrix.selectedCell() - found selected cell: " + cell + ", tag: " + cell.getClientProperty("tag"));
                return cell;
            }
        }
        System.out.println("[DEBUG] BrowserMatrix.selectedCell() - no selected cell found");
        return null;
    }
    
    /**
     * 選択されたセル配列を取得
     */
    public List<JButton> selectedCells() {
        List<JButton> selected = new ArrayList<>();
        for (JButton cell : cells) {
            if (cell.isSelected() && cell.isVisible()) {
                selected.add(cell);
            }
        }
        return selected;
    }
    
    /**
     * すべてのセルを取得
     */
    public List<JButton> getCells() {
        return new ArrayList<>(cells);
    }
    
    /**
     * タグでセルを選択
     * HOROS-20240407準拠: セルを選択してハイライト表示
     */
    public void selectCellWithTag(int tag) {
        deselectAllCells();
        if (tag >= 0 && tag < cells.size()) {
            JButton cell = cells.get(tag);
            cell.setSelected(true);
            // HOROS-20240407準拠: セルをハイライト表示するために背景色を変更
            // NSButtonCellのsetState:NSOnStateとsetHighlighted:YESに相当
            cell.setOpaque(true);
            cell.setBackground(new java.awt.Color(0.7f, 0.8f, 1.0f)); // 薄い青でハイライト
            cell.repaint();
            
            // HOROS-20240407準拠: 選択されたセルが表示されるようにスクロール
            // Java Swingでは、JComponentのscrollRectToVisibleメソッドを使用
            // レイアウトが完了した後にスクロールするため、SwingUtilities.invokeLaterを使用
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (cell.isVisible() && cell.getParent() == BrowserMatrix.this) {
                        java.awt.Rectangle cellBounds = cell.getBounds();
                        // scrollRectToVisibleは、このコンポーネント（BrowserMatrix）の座標系での矩形を受け取る
                        // 親のJScrollPaneが自動的にスクロールして、指定された矩形が表示されるようになる
                        scrollRectToVisible(cellBounds);
                    }
                }
            });
        }
    }
    
    /**
     * すべてのセルの選択を解除
     * HOROS-20240407準拠: セルの選択を解除してハイライトを削除
     */
    public void deselectAllCells() {
        for (JButton cell : cells) {
            cell.setSelected(false);
            // HOROS-20240407準拠: ハイライトを削除
            cell.setOpaque(false);
            cell.setBackground(null);
            cell.repaint();
        }
    }
    
    /**
     * キーリスナーを設定
     * HOROS-20240407準拠: 左右の矢印キーで画像を遷移
     */
    public void setupKeyListener() {
        setFocusable(true);
        addKeyListener(this);
    }
    
    /**
     * キーが押されたときの処理
     * HOROS-20240407準拠: 左右の矢印キーで画像を遷移
     */
    @Override
    public void keyPressed(KeyEvent e) {
        JButton selectedCell = selectedCell();
        if (selectedCell == null) {
            // 選択されたセルがない場合、最初の有効なセルを選択
            for (JButton cell : cells) {
                if (cell.isEnabled() && cell.isVisible()) {
                    Object tagObj = cell.getClientProperty("tag");
                    if (tagObj instanceof Integer) {
                        int tag = (Integer) tagObj;
                        selectCellWithTag(tag);
                        if (browserController != null) {
                            browserController.matrixPressed(this);
                        }
                    }
                    return;
                }
            }
            return;
        }
        
        Object tagObj = selectedCell.getClientProperty("tag");
        if (!(tagObj instanceof Integer)) {
            return;
        }
        
        int currentTag = (Integer) tagObj;
        int newTag = -1;
        
        // HOROS-20240407準拠: 左右の矢印キーで画像を遷移
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            // 左矢印: 前の画像へ
            newTag = currentTag - 1;
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            // 右矢印: 次の画像へ
            newTag = currentTag + 1;
        } else {
            return; // 他のキーは無視
        }
        
        // 有効な範囲内かチェック
        if (newTag >= 0 && newTag < cells.size()) {
            JButton newCell = cells.get(newTag);
            if (newCell.isEnabled() && newCell.isVisible()) {
                selectCellWithTag(newTag);
                if (browserController != null) {
                    browserController.matrixPressed(this);
                }
            }
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        // 何もしない
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // 何もしない
    }
    
    /**
     * 行と列でセルを取得
     */
    public JButton cellAtRowColumn(int row, int column) {
        if (row >= 0 && row < rows && column >= 0 && column < columns) {
            int index = row * columns + column;
            if (index >= 0 && index < cells.size()) {
                return cells.get(index);
            }
        }
        return null;
    }
    
    /**
     * セルの行と列を取得
     */
    public boolean getRowColumnForPoint(Point point, int[] rowColumn) {
        if (rowColumn.length < 2) return false;
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                JButton cell = cellAtRowColumn(r, c);
                if (cell != null && cell.getBounds().contains(point)) {
                    rowColumn[0] = r;
                    rowColumn[1] = c;
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * セルのインデックスを取得
     */
    public int indexOfCell(JButton cell) {
        return cells.indexOf(cell);
    }
    
    /**
     * セル選択イベント
     */
    public void selectCellEvent(MouseEvent event) {
        // TODO: 実装
        // ShiftキーやCommandキーによる複数選択を処理
    }
    
    /**
     * ドラッグを開始
     */
    @Override
    public void dragGestureRecognized(DragGestureEvent event) {
        // TODO: 実装
        // 選択されたセルのサムネイル画像を作成してドラッグを開始
    }
    
    /**
     * ドラッグ操作マスクを取得
     */
    public int draggingSessionSourceOperationMaskForDraggingContext(int context) {
        return DnDConstants.ACTION_COPY;
    }
    
    /**
     * ペーストボードにデータを提供
     */
    public void pasteboardProvideDataForType(Object pasteboard, Object item, String type) {
        // TODO: 実装
        // ファイルURLプロミスやデータベースオブジェクトXIDsを提供
    }
    
    /**
     * 元のフレームからドラッグを開始
     */
    public void startDragOriginalFrame(MouseEvent event) {
        selectCellEvent(event);
        if (browserController != null) {
            browserController.matrixPressed(this);
        }
        // TODO: 実装
        // 選択されたセルの画像をドラッグ
    }
    
    // DragSourceListenerの実装
    @Override
    public void dragEnter(java.awt.dnd.DragSourceDragEvent dsde) {
        // TODO: 実装
    }
    
    @Override
    public void dragOver(java.awt.dnd.DragSourceDragEvent dsde) {
        // TODO: 実装
    }
    
    @Override
    public void dropActionChanged(java.awt.dnd.DragSourceDragEvent dsde) {
        // TODO: 実装
    }
    
    @Override
    public void dragExit(java.awt.dnd.DragSourceEvent dse) {
        // TODO: 実装
    }
    
    @Override
    public void dragDropEnd(DragSourceDropEvent dsde) {
        // TODO: 実装
    }
    
    /**
     * セルを行と列で選択
     */
    public void selectCellAtRowColumn(int row, int column) {
        JButton cell = cellAtRowColumn(row, column);
        if (cell != null) {
            cell.setSelected(true);
        }
    }
    
    /**
     * 選択範囲を設定
     */
    public void setSelectionFromToAnchor(int from, int to, int anchor, boolean highlight) {
        deselectAllCells();
        int start = Math.min(from, to);
        int end = Math.max(from, to);
        for (int i = start; i <= end && i < cells.size(); i++) {
            cells.get(i).setSelected(true);
        }
    }
    
    /**
     * セルのタグを取得
     */
    public int getTag(JButton cell) {
        Object tag = cell.getClientProperty("tag");
        return tag != null ? (Integer) tag : -1;
    }
    
    /**
     * セルのタグを設定
     */
    public void setTag(JButton cell, int tag) {
        cell.putClientProperty("tag", tag);
    }
    
    /**
     * セルを選択
     */
    public void selectCell(JButton cell) {
        if (cell != null) {
            cell.setSelected(true);
        }
    }
    
    /**
     * フォーカスリングタイプを設定
     */
    public void setFocusRingType(int type) {
        // TODO: 実装
        // Java Swingではフォーカスリングのタイプ設定は異なる
    }
    
    /**
     * ダブルアクションを設定
     */
    public void setDoubleAction(Runnable action) {
        // TODO: 実装
        // ダブルクリック時にアクションを実行
        // 各セルにダブルクリックリスナーを追加する必要がある
    }
    
    /**
     * デリゲートを設定
     */
    public void setDelegate(Object delegate) {
        // TODO: 実装
    }
    
    /**
     * 矩形選択を設定
     */
    public void setSelectionByRect(boolean selectionByRect) {
        // TODO: 実装
    }
}

