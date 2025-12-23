package com.jj.dicomviewer.ui;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jj.dicomviewer.model.DicomStudy;
import com.jj.dicomviewer.model.DicomSeries;
import com.jj.dicomviewer.model.DicomImage;
import com.jj.dicomviewer.model.DicomDatabase;

/**
 * DatabaseOutlineView - データベースアウトラインビュー
 * 
 * HOROS-20240407のMyOutlineViewをJava Swingに移植
 * NSOutlineViewの代わりにJXTreeTableを使用（複数列のツリーテーブル）
 * 
 * HOROS-20240407 MainMenu.xibの列構成（順番通り）:
 * 1. name: Patient name（患者名）
 * 2. reportURL: Report
 * 3. lockedStudy: Lock
 * 4. patientID: Patient ID
 * 5. yearOld: Age（年齢）
 * 6. accessionNumber: Accession Number
 * 7. studyName: Study Description
 * 8. modality: Modality
 * 9. id: # Images
 * 10. comment: Comments
 * 11. stateText: State
 * 12. date: Date
 * 13. noFiles: # Files
 * 14. noSeries: # Series
 * 15. dateAdded: Date Added
 * 16. dateOpened: Date Opened
 * 17. referringPhysician: Referring Physician
 * 18. performingPhysician: Performing Physician
 * 19. institutionName: Institution
 * 20. albumsNames: Albums
 * 21. dateOfBirth: Date of Birth
 * 22. localstring: Local
 * 23. comment2-4: Comments 2-4
 */
public class DatabaseOutlineView extends JXTreeTable {
    
    private BrowserController browserController;
    private DatabaseOutlineTreeTableModel treeTableModel;
    
    // HOROS-20240407準拠: 列識別子（MainMenu.xibの順番通り）
    public static final String COL_NAME = "name";
    public static final String COL_REPORT_URL = "reportURL";
    public static final String COL_LOCKED_STUDY = "lockedStudy";
    public static final String COL_PATIENT_ID = "patientID";
    public static final String COL_YEAR_OLD = "yearOld";
    public static final String COL_ACCESSION_NUMBER = "accessionNumber";
    public static final String COL_STUDY_NAME = "studyName";
    public static final String COL_MODALITY = "modality";
    public static final String COL_ID = "id";
    public static final String COL_COMMENT = "comment";
    public static final String COL_STATE_TEXT = "stateText";
    public static final String COL_DATE = "date";
    public static final String COL_NO_FILES = "noFiles";
    public static final String COL_NO_SERIES = "noSeries";
    public static final String COL_DATE_ADDED = "dateAdded";
    public static final String COL_DATE_OPENED = "dateOpened";
    public static final String COL_REFERRING_PHYSICIAN = "referringPhysician";
    public static final String COL_PERFORMING_PHYSICIAN = "performingPhysician";
    public static final String COL_INSTITUTION_NAME = "institutionName";
    public static final String COL_ALBUMS_NAMES = "albumsNames";
    public static final String COL_DATE_OF_BIRTH = "dateOfBirth";
    
    // 列タイトル（HOROS-20240407 MainMenu.xib準拠）
    private static final String[] COLUMN_NAMES = {
        "Patient Name",       // name
        "Report",             // reportURL
        "Lock",               // lockedStudy
        "Patient ID",         // patientID
        "Age",                // yearOld
        "Accession Number",   // accessionNumber
        "Study Description",  // studyName
        "Modality",           // modality
        "# Images",           // id
        "Comments",           // comment
        "State",              // stateText
        "Date Acquired",      // date
        "# Files",            // noFiles
        "# Series",           // noSeries
        "Date Added",         // dateAdded
        "Date Opened",        // dateOpened
        "Referring Physician", // referringPhysician
        "Performing Physician", // performingPhysician
        "Institution",        // institutionName
        "Albums",             // albumsNames
        "Date of Birth"       // dateOfBirth
    };
    
    // 列識別子（HOROS-20240407準拠）
    private static final String[] COLUMN_IDENTIFIERS = {
        COL_NAME,
        COL_REPORT_URL,
        COL_LOCKED_STUDY,
        COL_PATIENT_ID,
        COL_YEAR_OLD,
        COL_ACCESSION_NUMBER,
        COL_STUDY_NAME,
        COL_MODALITY,
        COL_ID,
        COL_COMMENT,
        COL_STATE_TEXT,
        COL_DATE,
        COL_NO_FILES,
        COL_NO_SERIES,
        COL_DATE_ADDED,
        COL_DATE_OPENED,
        COL_REFERRING_PHYSICIAN,
        COL_PERFORMING_PHYSICIAN,
        COL_INSTITUTION_NAME,
        COL_ALBUMS_NAMES,
        COL_DATE_OF_BIRTH
    };
    
    // 列幅（HOROS-20240407 MainMenu.xib準拠）
    private static final int[] COLUMN_WIDTHS = {
        222,  // name
        100,  // reportURL
        50,   // lockedStudy
        100,  // patientID
        70,   // yearOld
        119,  // accessionNumber
        155,  // studyName
        65,   // modality
        60,   // id
        100,  // comment
        100,  // stateText
        130,  // date
        50,   // noFiles
        50,   // noSeries
        130,  // dateAdded
        130,  // dateOpened
        120,  // referringPhysician
        120,  // performingPhysician
        120,  // institutionName
        120,  // albumsNames
        90    // dateOfBirth
    };
    
    // 列最小幅（HOROS-20240407 MainMenu.xib準拠）
    private static final int[] COLUMN_MIN_WIDTHS = {
        50,   // name
        55,   // reportURL
        45,   // lockedStudy
        50,   // patientID
        50,   // yearOld
        50,   // accessionNumber
        50,   // studyName
        65,   // modality
        60,   // id
        100,  // comment
        100,  // stateText
        130,  // date
        50,   // noFiles
        50,   // noSeries
        130,  // dateAdded
        130,  // dateOpened
        40,   // referringPhysician
        40,   // performingPhysician
        40,   // institutionName
        40,   // albumsNames
        90    // dateOfBirth
    };
    
    // 列最大幅（HOROS-20240407 MainMenu.xib準拠）
    private static final int[] COLUMN_MAX_WIDTHS = {
        400,  // name
        120,  // reportURL
        55,   // lockedStudy
        200,  // patientID
        100,  // yearOld
        200,  // accessionNumber
        400,  // studyName
        100,  // modality
        100,  // id
        300,  // comment
        100,  // stateText
        180,  // date
        50,   // noFiles
        50,   // noSeries
        180,  // dateAdded
        180,  // dateOpened
        400,  // referringPhysician
        400,  // performingPhysician
        400,  // institutionName
        200,  // albumsNames
        140   // dateOfBirth
    };
    
    /**
     * コンストラクタ
     */
    public DatabaseOutlineView(BrowserController browserController) {
        super();
        this.browserController = browserController;
        
        // TreeModelを作成
        DatabaseOutlineTreeModel treeModel = new DatabaseOutlineTreeModel(browserController);
        // JXTreeTableはTreeModelを直接使用できないため、TreeTableModelアダプターが必要
        // 簡易実装: TreeModelをTreeTableModelに変換
        this.treeTableModel = new DatabaseOutlineTreeTableModel(browserController);
        setTreeTableModel(treeTableModel);
        
        // デフォルト設定
        setRootVisible(false);
        setShowsRootHandles(true);
        
        // HOROS-20240407準拠: 行の高さを設定（Regular mode: 17）
        setRowHeight(17);
        
        // HOROS-20240407準拠: 横スクロールを有効にする
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setAutoCreateColumnsFromModel(false);
        
        // HOROS-20240407準拠: 行選択を有効にする（どのカラムをクリックしても行が選択される）
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        
        // 選択リスナーを追加
        addSelectionChangeListener();
        
        // ダブルクリックリスナーを追加
        addDoubleClickListener();
        
        // HOROS-20240407準拠: NSOutlineViewが自動的にヘッダークリックを処理し、outlineView:sortDescriptorsDidChange:で検知（6665行目）
        // Java Swingでは、JXTreeTableが自動的にヘッダークリックを処理する機能を持っていないため、
        // 手動でリスナーを追加する必要がある（Java/WindowsとObjective-C/macOSの違いを埋めるための最小限のカスタムロジック）
        addColumnHeaderClickListener();
        
        // HOROS-20240407準拠: ソートインジケーター表示用のカスタムヘッダーレンダラーを設定
        setCustomHeaderRenderer();
        
        // HOROS-20240407準拠: 列幅を設定（リサイズ可能にする）
        // 列が作成された後に呼び出す必要があるため、SwingUtilities.invokeLaterを使用
        SwingUtilities.invokeLater(() -> {
            applyColumnWidths();
            // HOROS-20240407準拠: 初期化時はソートインジケーターを表示しない（ソートしていない状態）
            // ソートインジケーターは、ユーザーが列ヘッダーをクリックしてソートした時のみ表示される
        });
    }
    
    // HOROS-20240407準拠: ソート状態を保持
    private String currentSortColumn = null;
    private boolean currentSortAscending = true;
    
    /**
     * カスタムヘッダーレンダラーを設定（ソートインジケーター表示用）
     * HOROS-20240407準拠: NSOutlineViewのsetIndicatorImage:inTableColumn:相当
     */
    private void setCustomHeaderRenderer() {
        // デフォルトヘッダーレンダラーを設定
        javax.swing.table.JTableHeader header = getTableHeader();
        if (header != null) {
            header.setDefaultRenderer(new SortableHeaderRenderer());
        }
    }
    
    /**
     * ソートインジケーターを更新
     * HOROS-20240407準拠: setIndicatorImage:inTableColumn:相当
     * 
     * @param sortColumn ソート列の識別子
     * @param ascending 昇順の場合true
     */
    public void updateSortIndicator(String sortColumn, boolean ascending) {
        currentSortColumn = sortColumn;
        currentSortAscending = ascending;
        
        javax.swing.table.TableColumnModel columnModel = getColumnModel();
        javax.swing.table.JTableHeader header = getTableHeader();
        
        if (columnModel == null || header == null) {
            return;
        }
        
        // すべての列のヘッダーレンダラーを更新
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            javax.swing.table.TableColumn column = columnModel.getColumn(i);
            // 各列にソート状態を保持したヘッダーレンダラーを設定
            column.setHeaderRenderer(new SortableHeaderRenderer());
        }
        
        header.repaint();
    }
    
    /**
     * ソート可能なヘッダーレンダラー
     * HOROS-20240407準拠: NSOutlineViewのソートインジケーター表示を再現
     */
    private class SortableHeaderRenderer implements javax.swing.table.TableCellRenderer {
        
        @Override
        public java.awt.Component getTableCellRendererComponent(
                javax.swing.JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            
            // 列の識別子を取得
            javax.swing.table.TableColumn tableColumn = table.getColumnModel().getColumn(column);
            Object identifier = tableColumn.getIdentifier();
            String columnId = (identifier != null) ? identifier.toString() : null;
            
            // ソート列かどうかを判定
            boolean isSortColumn = (currentSortColumn != null && columnId != null && 
                                   currentSortColumn.equals(columnId));
            
            // ヘッダーパネルを作成（テキスト左寄せ、インジケーター右寄せ）
            javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout());
            panel.setOpaque(true);
            panel.setBackground(table.getTableHeader().getBackground());
            panel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createEtchedBorder(),
                javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5)
            ));
            
            // ヘッダーテキスト（左寄せ）
            javax.swing.JLabel textLabel = new javax.swing.JLabel();
            textLabel.setText(value != null ? value.toString() : "");
            textLabel.setForeground(table.getTableHeader().getForeground());
            textLabel.setFont(table.getTableHeader().getFont());
            textLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            panel.add(textLabel, java.awt.BorderLayout.WEST);
            
            // ソートインジケーター（右寄せ）
            // HOROS-20240407準拠: NSOutlineViewの標準ソートインジケーター（アクセント記号）
            if (isSortColumn) {
                javax.swing.JLabel indicatorLabel = new javax.swing.JLabel();
                // HOROS-20240407準拠: 昇順: ˄ (U+02C4 MODIFIER LETTER UP ARROWHEAD)
                // HOROS-20240407準拠: 降順: ˅ (U+02C5 MODIFIER LETTER DOWN ARROWHEAD)
                String indicator = currentSortAscending ? "\u02C4" : "\u02C5";
                indicatorLabel.setText(indicator);
                indicatorLabel.setForeground(table.getTableHeader().getForeground());
                indicatorLabel.setFont(table.getTableHeader().getFont());
                indicatorLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                panel.add(indicatorLabel, java.awt.BorderLayout.EAST);
            }
            
            return panel;
        }
    }
    
    /**
     * 選択変更リスナーを追加
     * HOROS-20240407準拠: databasePressed:に相当
     */
    private void addSelectionChangeListener() {
        getTreeSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if (browserController != null) {
                    browserController.databasePressed(DatabaseOutlineView.this);
                }
            }
        });
    }
    
    /**
     * ダブルクリックリスナーを追加
     * HOROS-20240407準拠: databaseDoublePressed:に相当
     */
    private void addDoubleClickListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && browserController != null) {
                    browserController.databaseDoublePressed(DatabaseOutlineView.this);
                }
            }
        });
        
        // Enterキーでも開く
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER && browserController != null) {
                    browserController.databaseDoublePressed(DatabaseOutlineView.this);
                }
            }
        });
    }
    
    /**
     * 列ヘッダークリックリスナーを追加
     * HOROS-20240407準拠: NSOutlineViewが自動的にヘッダークリックを処理し、outlineView:sortDescriptorsDidChange:で検知（6665行目）
     * Java Swingでは、JXTreeTableが自動的にヘッダークリックを処理する機能を持っていないため、
     * 手動でリスナーを追加する必要がある（Java/WindowsとObjective-C/macOSの違いを埋めるための最小限のカスタムロジック）
     */
    private void addColumnHeaderClickListener() {
        javax.swing.table.JTableHeader header = getTableHeader();
        if (header == null) {
            return;
        }
        
        // HOROS-20240407準拠: 列ヘッダークリックでソート（outlineView:sortDescriptorsDidChange:相当）
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // HOROS-20240407準拠: 右クリックの場合は処理をスキップ（列メニューを表示するため）
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    return;
                }
                
                // HOROS-20240407準拠: 左クリックの場合のみソート処理を実行
                if (browserController != null) {
                    int columnIndex = header.columnAtPoint(e.getPoint());
                    if (columnIndex >= 0) {
                        // 列の識別子を取得
                        javax.swing.table.TableColumn column = header.getColumnModel().getColumn(columnIndex);
                        Object identifier = column.getIdentifier();
                        String columnId = identifier != null ? identifier.toString() : null;
                        
                        if (columnId != null && !columnId.isEmpty()) {
                            // 現在のソート状態を確認
                            boolean ascending = browserController.isSortAscending();
                            String currentSortColumn = browserController.getSortColumn();
                            
                            // 同じ列をクリックした場合は昇順/降順を切り替え
                            if (columnId.equals(currentSortColumn)) {
                                ascending = !ascending;
                            } else {
                                ascending = true; // 新しい列の場合は昇順から
                            }
                            
                            // HOROS-20240407準拠: outlineView:sortDescriptorsDidChange:相当（6665行目）
                            // ソートを実行
                            browserController.setSortColumn(columnId, ascending);
                        }
                    }
                }
            }
        });
    }
    
    /**
     * HOROS-20240407準拠: どのカラムをクリックしても行が選択されるようにする
     * JXTreeTableのchangeSelectionメソッドをオーバーライド
     */
    @Override
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        // HOROS-20240407準拠: NSOutlineViewでは、どのカラムをクリックしても行が選択される
        // 最初のカラム（階層表示用）を指定して行選択を実行
        
        // 同じ行をクリックした場合でも、TreeSelectionEventを発火させるために
        // 一度選択を解除してから再選択する
        int[] currentSelection = getSelectedRows();
        boolean isSameRow = (currentSelection.length == 1 && currentSelection[0] == rowIndex);
        
        if (isSameRow && columnIndex != 0 && columnIndex != -1) {
            // 同じ行で、最初のカラム以外をクリックした場合は、選択を更新してイベントを発火させる
            TreePath path = getPathForRow(rowIndex);
            if (path != null) {
                javax.swing.tree.TreeSelectionModel treeSelModel = getTreeSelectionModel();
                if (treeSelModel != null) {
                    // 一度選択を解除してから再選択することで、TreeSelectionEventを発火させる
                    treeSelModel.removeSelectionPath(path);
                    // 同期的に再選択してイベントを発火させる
                    treeSelModel.setSelectionPath(path);
                    return;
                }
            }
        }
        
        // 通常の行選択処理（最初のカラムを指定して行選択を実行）
        super.changeSelection(rowIndex, 0, toggle, extend);
    }
    
    /**
     * データを再読み込み
     * HOROS-20240407準拠: reloadData
     */
    public void reloadData() {
// デバッグログ: 
        treeTableModel.fireTreeStructureChanged();
        revalidate();
        repaint();
    }
    
    /**
     * 列幅を設定（外部から呼び出し用）
     * HOROS-20240407準拠
     */
    public void applyColumnWidths() {
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        // 各列の幅を設定（リサイズ可能）
        javax.swing.table.TableColumnModel columnModel = getColumnModel();
        
        // HOROS-20240407準拠: 列が作成されていない場合は、列を作成
        if (columnModel.getColumnCount() == 0) {
            // JXTreeTableでは列が自動的に作成されるが、明示的に列を作成する必要がある場合がある
            // ここでは列が既に作成されていることを前提とする
            return;
        }
        
        // HOROS-20240407準拠: 各列に識別子と幅を設定
        for (int i = 0; i < COLUMN_WIDTHS.length && i < columnModel.getColumnCount(); i++) {
            javax.swing.table.TableColumn column = columnModel.getColumn(i);
            
            // HOROS-20240407準拠: 列の識別子を設定（カラム状態の保存/復元に使用）
            if (i < COLUMN_IDENTIFIERS.length) {
                column.setIdentifier(COLUMN_IDENTIFIERS[i]);
            }
            
            // HOROS-20240407準拠: 列のヘッダー名を設定
            if (i < COLUMN_NAMES.length) {
                column.setHeaderValue(COLUMN_NAMES[i]);
            }
            
            // HOROS-20240407準拠: 列幅を設定（リサイズ可能）
            column.setMinWidth(COLUMN_MIN_WIDTHS[i]);
            column.setMaxWidth(COLUMN_MAX_WIDTHS[i]);
            column.setPreferredWidth(COLUMN_WIDTHS[i]);
            column.setResizable(true); // ユーザーが列幅を調整できるようにする
        }
        
        revalidate();
        repaint();
    }
    
    /**
     * カラム状態を取得（保存用）
     * HOROS-20240407準拠: MyOutlineView.m 68-82行目 - (NSObject<NSCoding>*)columnState
     * HOROS-20240407準拠: 列の順序（位置）も保存する（Java/WindowsとObjective-C/macOSの違いを埋めるための最小限のカスタムロジック）
     * 
     * @return カラム状態（識別子、幅、順序のマップのリスト）
     */
    public List<Map<String, Object>> getColumnState() {
        List<Map<String, Object>> state = new ArrayList<>();
        javax.swing.table.TableColumnModel columnModel = getColumnModel();
        
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            javax.swing.table.TableColumn column = columnModel.getColumn(i);
            Object identifier = column.getIdentifier();
            
            // 非表示のカラムは除外（幅が0の場合は非表示とみなす）
            if (identifier != null && column.getWidth() > 0) {
                Map<String, Object> columnInfo = new HashMap<>();
                columnInfo.put("Identifier", identifier.toString());
                columnInfo.put("Width", column.getPreferredWidth());
                // HOROS-20240407準拠: 列の順序（位置）を保存（Java Swingでは明示的に保存する必要がある）
                columnInfo.put("Index", i);
                state.add(columnInfo);
            }
        }
        
        return state;
    }
    
    /**
     * カラム状態を復元
     * HOROS-20240407準拠: MyOutlineView.m 84-111行目 - (void)restoreColumnState:(NSArray*)state
     * HOROS-20240407準拠: 列の順序（位置）も復元する（Java/WindowsとObjective-C/macOSの違いを埋めるための最小限のカスタムロジック）
     * 
     * @param state カラム状態（識別子、幅、順序のマップのリスト）
     */
    public void restoreColumnState(List<Map<String, Object>> state) {
        if (state == null || state.isEmpty()) {
            return;
        }
        
        javax.swing.table.TableColumnModel columnModel = getColumnModel();
        if (columnModel == null) {
            return;
        }
        
        // HOROS-20240407準拠: まず列の幅を復元
        for (Map<String, Object> params : state) {
            String identifier = (String) params.get("Identifier");
            Object widthObj = params.get("Width");
            
            if (identifier == null || widthObj == null) {
                continue;
            }
            
            int width = 0;
            if (widthObj instanceof Number) {
                width = ((Number) widthObj).intValue();
            } else if (widthObj instanceof String) {
                try {
                    width = Integer.parseInt((String) widthObj);
                } catch (NumberFormatException e) {
                    continue;
                }
            } else {
                continue;
            }
            
            // nameカラムの場合は特別処理（最初のカラム）
            if (COL_NAME.equals(identifier)) {
                if (columnModel.getColumnCount() > 0) {
                    javax.swing.table.TableColumn nameColumn = columnModel.getColumn(0);
                    nameColumn.setPreferredWidth(width);
                }
            } else {
                // 識別子でカラムを検索
                for (int i = 0; i < columnModel.getColumnCount(); i++) {
                    javax.swing.table.TableColumn column = columnModel.getColumn(i);
                    Object colIdentifier = column.getIdentifier();
                    
                    if (identifier.equals(colIdentifier != null ? colIdentifier.toString() : "")) {
                        column.setPreferredWidth(width);
                        // カラムを表示（幅が0より大きい場合は表示）
                        if (width > 0) {
                            column.setWidth(width);
                        }
                        break;
                    }
                }
            }
        }
        
        // HOROS-20240407準拠: 列の順序（位置）を復元
        // 保存された順序に従って列を並び替える
        // nameカラムは常に最初の位置に固定
        List<Map<String, Object>> sortedState = new ArrayList<>(state);
        sortedState.sort((a, b) -> {
            // nameカラムは常に最初
            String idA = (String) a.get("Identifier");
            String idB = (String) b.get("Identifier");
            if (COL_NAME.equals(idA)) return -1;
            if (COL_NAME.equals(idB)) return 1;
            
            // その他の列は保存されたIndex順
            Object indexA = a.get("Index");
            Object indexB = b.get("Index");
            if (indexA instanceof Number && indexB instanceof Number) {
                return Integer.compare(((Number) indexA).intValue(), ((Number) indexB).intValue());
            }
            return 0;
        });
        
        // 列の順序を復元（nameカラムを除く）
        for (int targetIndex = 1; targetIndex < sortedState.size(); targetIndex++) {
            Map<String, Object> params = sortedState.get(targetIndex);
            String identifier = (String) params.get("Identifier");
            
            if (identifier == null || COL_NAME.equals(identifier)) {
                continue;
            }
            
            // 識別子でカラムを検索
            int currentIndex = -1;
            for (int i = 0; i < columnModel.getColumnCount(); i++) {
                javax.swing.table.TableColumn column = columnModel.getColumn(i);
                Object colIdentifier = column.getIdentifier();
                if (identifier.equals(colIdentifier != null ? colIdentifier.toString() : "")) {
                    currentIndex = i;
                    break;
                }
            }
            
            // 列が見つかり、現在の位置が目標位置と異なる場合は移動
            if (currentIndex >= 0 && currentIndex != targetIndex) {
                try {
                    columnModel.moveColumn(currentIndex, targetIndex);
                } catch (IllegalArgumentException e) {
                    // 移動に失敗した場合はスキップ（列が存在しない、または範囲外）
                }
            }
        }
        
        revalidate();
        repaint();
    }
    
    /**
     * すべてのカラムを非表示にする（nameカラム以外）
     * HOROS-20240407準拠: MyOutlineView.m 145-150行目 - (void)hideAllColumns
     */
    private void hideAllColumns() {
        // Swingではカラムの表示/非表示は直接制御できないため、
        // ここでは何もしない（幅の復元のみを行う）
        // 必要に応じて、カラムモデルから削除するなどの方法を検討
    }
    
    /**
     * 列の識別子を取得
     * @param columnIndex 列のインデックス
     * @return 列の識別子、存在しない場合はnull
     */
    public String getColumnIdentifier(int columnIndex) {
        if (columnIndex >= 0 && columnIndex < COLUMN_IDENTIFIERS.length) {
            return COLUMN_IDENTIFIERS[columnIndex];
        }
        return null;
    }
    
    /**
     * 列が表示されているかどうかを確認
     * HOROS-20240407準拠: MyOutlineView.m 134-138行目 - (BOOL)isColumnWithIdentifierVisible:(id)identifier
     * HOROS-20240407準拠: NSTableColumn* column = [self tableColumnWithIdentifier:identifier]; return column && !column.isHidden;
     * 
     * @param identifier 列の識別子
     * @return 列が存在し、表示されている場合true
     */
    public boolean isColumnWithIdentifierVisible(String identifier) {
        if (identifier == null) {
            return false;
        }
        
        javax.swing.table.TableColumnModel columnModel = getColumnModel();
        if (columnModel == null) {
            return false;
        }
        
        // HOROS-20240407準拠: 識別子で列を検索
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            javax.swing.table.TableColumn column = columnModel.getColumn(i);
            Object colIdentifier = column.getIdentifier();
            
            if (identifier.equals(colIdentifier != null ? colIdentifier.toString() : "")) {
                // HOROS-20240407準拠: 列が存在し、非表示でない場合にtrueを返す
                // Swingでは列の表示/非表示は列幅が0より大きいかどうかで判断
                // 列がカラムモデルに存在し、幅が0より大きい場合は表示されている
                return column.getWidth() > 0;
            }
        }
        
        return false;
    }
    
    /**
     * 列の表示/非表示を設定
     * HOROS-20240407準拠: MyOutlineView.m 113-132行目 - (void)setColumnWithIdentifier:(id)identifier visible:(BOOL)visible
     * 
     * @param identifier 列の識別子
     * @param visible 表示する場合true、非表示にする場合false
     */
    public void setColumnWithIdentifierVisible(String identifier, boolean visible) {
        // HOROS-20240407準拠: "name"列は常に表示（非表示にできない）
        if (COL_NAME.equals(identifier)) {
            return;
        }
        
        javax.swing.table.TableColumnModel columnModel = getColumnModel();
        if (columnModel == null) {
            return;
        }
        
        // HOROS-20240407準拠: 識別子で列を検索
        javax.swing.table.TableColumn column = null;
        int columnIndex = -1;
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            javax.swing.table.TableColumn col = columnModel.getColumn(i);
            Object colIdentifier = col.getIdentifier();
            if (identifier.equals(colIdentifier != null ? colIdentifier.toString() : "")) {
                column = col;
                columnIndex = i;
                break;
            }
        }
        
        if (column == null) {
            return;
        }
        
        // HOROS-20240407準拠: 列の表示/非表示を設定
        // Swingでは列幅を0にするか、列を削除/追加する方法がある
        // 列幅を0にする方法を使用（列は残るが表示されない）
        if (visible) {
            // HOROS-20240407準拠: 表示する場合は列幅を復元（デフォルト幅を使用）
            if (column.getWidth() == 0) {
                // デフォルト幅を設定（列の識別子に応じて適切な幅を設定）
                int defaultWidth = getDefaultColumnWidth(identifier);
                column.setPreferredWidth(defaultWidth);
                column.setWidth(defaultWidth);
                column.setMinWidth(0);
                column.setMaxWidth(Integer.MAX_VALUE);
            }
            // HOROS-20240407準拠: 表示する場合は列を適切な位置に移動
            // Swingでは列の順序を変更する必要がある場合があるが、ここでは簡易実装
        } else {
            // HOROS-20240407準拠: 非表示にする場合は列幅を0にする
            column.setWidth(0);
            column.setPreferredWidth(0);
            column.setMinWidth(0);
            column.setMaxWidth(0);
        }
        
        // HOROS-20240407準拠: テーブルを再描画
        repaint();
    }
    
    /**
     * 列のデフォルト幅を取得
     * @param identifier 列の識別子
     * @return デフォルト幅
     */
    private int getDefaultColumnWidth(String identifier) {
        // HOROS-20240407準拠: 列の識別子に応じて適切なデフォルト幅を返す
        switch (identifier) {
            case COL_PATIENT_ID:
                return 100;
            case COL_YEAR_OLD:
                return 80;
            case COL_ACCESSION_NUMBER:
                return 120;
            case COL_STUDY_NAME:
                return 150;
            case COL_MODALITY:
                return 80;
            case COL_ID:
                return 80;
            case COL_COMMENT:
                return 150;
            case COL_STATE_TEXT:
                return 100;
            case COL_DATE:
                return 150;
            case COL_NO_FILES:
                return 80;
            case COL_NO_SERIES:
                return 80;
            case COL_DATE_ADDED:
                return 150;
            case COL_DATE_OPENED:
                return 150;
            case COL_REFERRING_PHYSICIAN:
                return 150;
            case COL_PERFORMING_PHYSICIAN:
                return 150;
            case COL_INSTITUTION_NAME:
                return 150;
            case COL_ALBUMS_NAMES:
                return 150;
            case COL_DATE_OF_BIRTH:
                return 120;
            default:
                return 100;
        }
    }
    
    /**
     * 選択されたアイテムを取得
     */
    public List<Object> getSelectedItems() {
        List<Object> selectedItems = new ArrayList<>();
        int[] selectedRows = getSelectedRows();
        for (int row : selectedRows) {
            TreePath path = getPathForRow(row);
            if (path != null) {
                Object item = path.getLastPathComponent();
                if (item != null) {
                    selectedItems.add(item);
                }
            }
        }
        return selectedItems;
    }
    
    /**
     * 最初に選択されたアイテムを取得
     * HOROS-20240407準拠: [databaseOutline itemAtRow:[databaseOutline selectedRow]]
     */
    public Object getSelectedItem() {
        List<Object> selectedItems = getSelectedItems();
        return selectedItems.isEmpty() ? null : selectedItems.get(0);
    }
    
    /**
     * TreeTableModel実装
     * HOROS-20240407準拠: NSOutlineViewDataSource/NSOutlineViewDelegate
     */
    public static class DatabaseOutlineTreeTableModel extends AbstractTreeTableModel {
        
        private BrowserController browserController;
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        
        public DatabaseOutlineTreeTableModel(BrowserController browserController) {
            super("Root");
            this.browserController = browserController;
        }
        
        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }
        
        @Override
        public Class<?> getColumnClass(int column) {
            if (column == 0) {
                return String.class; // TreeTableではhierarchical column
            }
            return String.class;
        }
        
        @Override
        public Object getValueAt(Object node, int column) {
            if (node == null || column < 0 || column >= COLUMN_IDENTIFIERS.length) return "";
            
            String columnId = COLUMN_IDENTIFIERS[column];
            
            // HOROS-20240407準拠: intOutlineView:objectValueForTableColumn:byItem:
            if (node instanceof DicomStudy) {
                DicomStudy study = (DicomStudy) node;
                return getStudyValue(study, columnId);
            } else if (node instanceof DicomSeries) {
                DicomSeries series = (DicomSeries) node;
                return getSeriesValue(series, columnId);
            } else if (node instanceof DicomImage) {
                DicomImage image = (DicomImage) node;
                return getImageValue(image, columnId);
            }
            
            return "";
        }
        
        /**
         * Studyの値を取得
         * HOROS-20240407準拠
         */
        private String getStudyValue(DicomStudy study, String columnId) {
            switch (columnId) {
                case COL_NAME:
                    String name = study.getName();
                    if (name == null || name.isEmpty()) {
                        name = study.getPatientID();
                    }
                    return name != null ? name : "Unknown";
                    
                case COL_REPORT_URL:
                    // TODO: レポートURL
                    return "";
                    
                case COL_LOCKED_STUDY:
                    // TODO: ロック状態
                    return "";
                    
                case COL_PATIENT_ID:
                    return study.getPatientID() != null ? study.getPatientID() : "";
                    
                case COL_YEAR_OLD:
                    // HOROS-20240407準拠: BrowserController.m 6507-6536行目
                    // yearOldDatabaseDisplay設定に基づいて年齢を表示
                    // デフォルト値は2（DefaultsOsiriX.m 1164行目）
                    int yearOldDatabaseDisplay = 2; // TODO: NSUserDefaultsから取得（デフォルトは2）
                    
                    String yearOld = study.yearOld();
                    String yearOldAcquisition = study.yearOldAcquisition();
                    
                    switch (yearOldDatabaseDisplay) {
                        case 0:
                            // HOROS-20240407準拠: case 0 - yearOldを表示（6512行目）
                            return yearOld != null ? yearOld : "";
                            
                        case 1:
                            // HOROS-20240407準拠: case 1 - yearOldAcquisitionを表示（6516行目）
                            return yearOldAcquisition != null ? yearOldAcquisition : "";
                            
                        case 2:
                        default:
                            // HOROS-20240407準拠: case 2/default - 両方を表示（6519-6533行目）
                            if (yearOld == null || yearOld.isEmpty()) {
                                return yearOldAcquisition != null ? yearOldAcquisition : "";
                            }
                            if (yearOldAcquisition == null || yearOldAcquisition.isEmpty()) {
                                return yearOld;
                            }
                            
                            // HOROS-20240407準拠: 同じ場合は1つだけ表示（6525-6526行目）
                            if (yearOld.equals(yearOldAcquisition)) {
                                return yearOld;
                            }
                            
                            // HOROS-20240407準拠: 両方が " y" で終わる場合は "12/11 y" 形式で表示（6529-6530行目）
                            if (yearOld.endsWith(" y") && yearOldAcquisition.endsWith(" y")) {
                                String yearOldNum = yearOld.substring(0, yearOld.length() - 2);
                                String yearOldAcquisitionNum = yearOldAcquisition.substring(0, yearOldAcquisition.length() - 2);
                                return yearOldNum + "/" + yearOldAcquisitionNum + " y";
                            }
                            
                            // HOROS-20240407準拠: それ以外の場合は "12 y/11 y" 形式で表示（6532行目）
                            return yearOld + "/" + yearOldAcquisition;
                    }
                    
                case COL_ACCESSION_NUMBER:
                    return study.getAccessionNumber() != null ? study.getAccessionNumber() : "";
                    
                case COL_STUDY_NAME:
                    return study.getStudyName() != null ? study.getStudyName() : "";
                    
                case COL_MODALITY:
                    return study.getModality() != null ? study.getModality() : "";
                    
                case COL_ID:
                    // HOROS-20240407準拠: 画像数
                    Integer numImages = study.getNumberOfImages();
                    return numImages != null ? String.valueOf(numImages) : "0";
                    
                case COL_COMMENT:
                    return study.getComment() != null ? study.getComment() : "";
                    
                case COL_STATE_TEXT:
                    // TODO: 状態テキスト
                    return "";
                    
                case COL_DATE:
                    LocalDateTime date = study.getDate();
                    return date != null ? date.format(DATE_FORMATTER) : "";
                    
                case COL_NO_FILES:
                    // TODO: ファイル数
                    return "";
                    
                case COL_NO_SERIES:
                    // HOROS-20240407準拠: シリーズ数
                    if (study.getSeries() != null) {
                        return String.valueOf(study.getSeries().size());
                    }
                    return "0";
                    
                case COL_DATE_ADDED:
                    LocalDateTime dateAdded = study.getDateAdded();
                    return dateAdded != null ? dateAdded.format(DATE_FORMATTER) : "";
                    
                case COL_DATE_OPENED:
                    LocalDateTime dateOpened = study.getDateOpened();
                    return dateOpened != null ? dateOpened.format(DATE_FORMATTER) : "";
                    
                case COL_REFERRING_PHYSICIAN:
                    return study.getReferringPhysician() != null ? study.getReferringPhysician() : "";
                    
                case COL_PERFORMING_PHYSICIAN:
                    return study.getPerformingPhysician() != null ? study.getPerformingPhysician() : "";
                    
                case COL_INSTITUTION_NAME:
                    return study.getInstitutionName() != null ? study.getInstitutionName() : "";
                    
                case COL_ALBUMS_NAMES:
                    // TODO: アルバム名
                    return "";
                    
                case COL_DATE_OF_BIRTH:
                    // TODO: 生年月日
                    return "";
                    
                default:
                    return "";
            }
        }
        
        /**
         * Seriesの値を取得
         * HOROS-20240407準拠: Seriesの場合、一部の列のみ表示
         */
        private String getSeriesValue(DicomSeries series, String columnId) {
            switch (columnId) {
                case COL_NAME:
                    // HOROS-20240407準拠: Series名またはSeriesDescription
                    String name = series.getName();
                    if (name == null || name.isEmpty()) {
                        name = series.getSeriesDescription();
                    }
                    if (name == null || name.isEmpty()) {
                        name = "Series";
                    }
                    // シリーズ番号を追加
                    Integer seriesNum = series.getSeriesNumber();
                    if (seriesNum != null && seriesNum > 0) {
                        name = name + " #" + seriesNum;
                    }
                    return name;
                    
                case COL_MODALITY:
                    return series.getModality() != null ? series.getModality() : "";
                    
                case COL_ID:
                    // HOROS-20240407準拠: 画像数
                    Integer numImages = series.getNumberOfImages();
                    return numImages != null ? String.valueOf(numImages) : "0";
                    
                case COL_DATE:
                    LocalDateTime date = series.getDate();
                    return date != null ? date.format(DATE_FORMATTER) : "";
                    
                case COL_DATE_ADDED:
                    LocalDateTime dateAdded = series.getDateAdded();
                    return dateAdded != null ? dateAdded.format(DATE_FORMATTER) : "";
                    
                default:
                    // HOROS-20240407準拠: Seriesでは他の列は空
                    return "";
            }
        }
        
        /**
         * Imageの値を取得
         */
        private String getImageValue(DicomImage image, String columnId) {
            switch (columnId) {
                case COL_NAME:
                    String path = image.getPathString();
                    if (path != null) {
                        int lastSep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
                        return lastSep >= 0 ? path.substring(lastSep + 1) : path;
                    }
                    return image.getSopInstanceUID() != null ? image.getSopInstanceUID() : "Image";
                    
                default:
                    return "";
            }
        }
        
        @Override
        public Object getChild(Object parent, int index) {
            if (parent == getRoot()) {
                // HOROS-20240407準拠: ルートの子はStudy
                // outlineViewArrayが空でない場合はそれを使用、そうでない場合は直接データベースから取得
                List<Object> outlineArray = browserController.getOutlineViewArray();
                if (outlineArray != null && !outlineArray.isEmpty()) {
                    // HOROS-20240407準拠: スマートアルバムの場合、outlineViewArrayを使用
                    if (index >= 0 && index < outlineArray.size()) {
                        return outlineArray.get(index);
                    }
                } else {
                    // HOROS-20240407準拠: 通常のアルバムの場合、直接データベースから取得
                    DicomDatabase database = browserController.getDatabase();
                    if (database != null) {
                        List<com.jj.dicomviewer.model.DicomStudy> allStudies = database.getAllStudies();
                        if (index >= 0 && index < allStudies.size()) {
                            return allStudies.get(index);
                        }
                    }
                }
            } else if (parent instanceof DicomStudy) {
                // Studyの子はSeries
                DicomStudy study = (DicomStudy) parent;
                // 遅延読み込み
                browserController.getDatabase().loadSeriesForStudyIfNeeded(study);
                List<Object> children = browserController.childrenArray(parent, true);
                if (index >= 0 && index < children.size()) {
                    return children.get(index);
                }
            }
            // HOROS-20240407準拠: Seriesは展開不可
            return null;
        }
        
        @Override
        public int getChildCount(Object parent) {
            if (parent == getRoot()) {
                // HOROS-20240407準拠: outlineViewArrayが空でない場合はそれを使用、そうでない場合は直接データベースから取得
                List<Object> outlineArray = browserController.getOutlineViewArray();
                if (outlineArray != null && !outlineArray.isEmpty()) {
                    // HOROS-20240407準拠: スマートアルバムの場合、outlineViewArrayを使用
                    return outlineArray.size();
                } else {
                    // HOROS-20240407準拠: 通常のアルバムの場合、直接データベースから取得
                    DicomDatabase database = browserController.getDatabase();
                    if (database != null) {
                        return database.getAllStudies().size();
                    }
                }
                return 0;
            } else if (parent instanceof DicomStudy) {
                DicomStudy study = (DicomStudy) parent;
                // 遅延読み込み
                browserController.getDatabase().loadSeriesForStudyIfNeeded(study);
                return browserController.childrenArray(parent, true).size();
            }
            // HOROS-20240407準拠: Seriesは展開不可（子の数は0）
            return 0;
        }
        
        @Override
        public int getIndexOfChild(Object parent, Object child) {
            if (parent == getRoot()) {
                return browserController.getOutlineViewArray().indexOf(child);
            } else if (parent instanceof DicomStudy) {
                return browserController.childrenArray(parent, true).indexOf(child);
            }
            return -1;
        }
        
        @Override
        public boolean isLeaf(Object node) {
            // HOROS-20240407準拠: isItemExpandable
            // Seriesは展開不可（leaf）
            if (node instanceof DicomSeries) {
                return true;
            }
            if (node instanceof DicomImage) {
                return true;
            }
            return false;
        }
        
        /**
         * ツリー構造変更を通知
         */
        public void fireTreeStructureChanged() {
            modelSupport.fireTreeStructureChanged(new TreePath(getRoot()));
        }
    }
}
