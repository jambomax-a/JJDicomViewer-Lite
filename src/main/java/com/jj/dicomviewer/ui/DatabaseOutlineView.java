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
    
    
    /**
     * statesArrayを取得
     * HOROS-20240407準拠: BrowserController.m 14293行目
     */
    private static String[] getStatesArray() {
        // HOROS-20240407準拠: BrowserController.m 14293行目
        // statesArray = [@"empty", @"unread", @"reviewed", @"dictated", @"validated"]
        return new String[] {
            "",           // インデックス0は空（stateText == 0の場合は空文字列）
            "empty",      // インデックス1
            "unread",     // インデックス2
            "reviewed",   // インデックス3
            "dictated",   // インデックス4
            "validated"   // インデックス5
        };
    }
    
    /**
     * prepareRendererをオーバーライド（UIDハイライトを処理）
     * JXTreeTableでは、最初の列（階層表示用）にTreeTableCellRendererが使用されるため、
     * prepareRendererで背景色を設定する
     * HOROS-20240407準拠: BrowserController.m 6739-6756行目 - willDisplayCell
     * 
     * 注意: 最初の列（インデックス0）は開閉マーク専用で、データは表示しない
     */
    @Override
    public java.awt.Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
        java.awt.Component component = super.prepareRenderer(renderer, row, column);
        
        // HOROS-20240407準拠: 最初の列（インデックス0）は開閉マーク専用で、データは表示しない
        // カスタムレンダラーで開閉マークのみを表示する
        if (column == 0 && component instanceof javax.swing.JLabel) {
            javax.swing.JLabel label = (javax.swing.JLabel) component;
            label.setText(""); // データを非表示（開閉マークのみ表示）
        }
        
        // HOROS-20240407準拠: name列（識別子が"name"の列、2番目の列）のみで背景色を設定
        // 注意: 最初の列（インデックス0）は開閉マーク専用、Patient Name列は2番目の列（インデックス1）
        // 列が移動しても識別子で判定するため、Patient Name列が4列目に移動してもUIDハイライトが動作する
        // 最初の列（開閉マーク専用列）ではハイライトを適用しない
        if (component != null && browserController != null && column != 0) {
            // 列の識別子を確認（列が移動しても識別子で判定）
            javax.swing.table.TableColumn tableColumn = getColumnModel().getColumn(column);
            Object columnIdentifier = tableColumn != null ? tableColumn.getIdentifier() : null;
            boolean isNameColumn = columnIdentifier != null && (COL_NAME.equals(columnIdentifier.toString()) || "name".equals(columnIdentifier.toString()));
            
            // name列の場合のみUIDハイライトを処理
            if (isNameColumn && !isRowSelected(row)) {
                // HOROS-20240407準拠: displaySamePatientWithColorBackground設定を確認
                boolean displaySamePatientWithColorBackground = true;
                boolean isFocused = true;
                
                if (displaySamePatientWithColorBackground && isFocused) {
                    Object previousItem = browserController.getPreviousItem();
                    if (previousItem != null && previousItem instanceof com.jj.dicomviewer.model.DicomStudy) {
                        try {
                            TreePath path = getPathForRow(row);
                            if (path != null) {
                                Object item = path.getLastPathComponent();
                                
                                // 選択行自体は除外
                                boolean isSameItem = (previousItem == item) || (previousItem.equals(item));
                                if (!isSameItem && item instanceof com.jj.dicomviewer.model.DicomStudy) {
                                    com.jj.dicomviewer.model.DicomStudy currentStudy = (com.jj.dicomviewer.model.DicomStudy) item;
                                    com.jj.dicomviewer.model.DicomStudy previousStudy = (com.jj.dicomviewer.model.DicomStudy) previousItem;
                                    
                                    String patientUID = currentStudy.getPatientUID();
                                    String previousPatientUID = previousStudy.getPatientUID();
                                    
                                    // 患者UIDが一致する場合、背景色を設定
                                    if (patientUID != null && patientUID.length() > 1 &&
                                        previousPatientUID != null && previousPatientUID.length() > 1 &&
                                        patientUID.equalsIgnoreCase(previousPatientUID)) {
                                        
                                        // HOROS-20240407準拠: 薄いグレーの背景色を設定
                                        // 薄いグレー（RGB: 220, 220, 220）を使用
                                        java.awt.Color highlightColor = new java.awt.Color(220, 220, 220);
                                        
                                        if (component instanceof javax.swing.JLabel) {
                                            javax.swing.JLabel label = (javax.swing.JLabel) component;
                                            label.setOpaque(true);
                                            label.setBackground(highlightColor);
                                        } else if (component instanceof javax.swing.JPanel) {
                                            javax.swing.JPanel panel = (javax.swing.JPanel) component;
                                            panel.setOpaque(true);
                                            panel.setBackground(highlightColor);
                                        } else {
                                            // その他のコンポーネントタイプの場合、背景色を設定を試みる
                                            component.setBackground(highlightColor);
                                            if (component instanceof javax.swing.JComponent) {
                                                ((javax.swing.JComponent) component).setOpaque(true);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // エラーが発生した場合はスキップ
                        }
                    }
                }
            }
        }
        
        return component;
    }
    
    /**
     * paintComponentをオーバーライド（UIDハイライトはセルレンダラーで処理）
     * HOROS-20240407準拠: BrowserController.m 6739-6756行目 - willDisplayCell
     * willDisplayCellでセルに背景色を設定するため、セルレンダラーで処理する
     * paintComponentでの描画は削除（セルレンダラーと競合するため）
     */
    @Override
    protected void paintComponent(java.awt.Graphics g) {
        // HOROS-20240407準拠: willDisplayCellでセルに背景色を設定するため、
        // セルレンダラー（ComparativePatientCellRenderer）で処理する
        // paintComponentでの描画は削除（セルレンダラーと競合するため）
        super.paintComponent(g);
    }
    
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
    public static final String COL_LOCALSTRING = "localstring";
    public static final String COL_COMMENT2 = "comment2";
    public static final String COL_COMMENT3 = "comment3";
    public static final String COL_COMMENT4 = "comment4";
    
    // 列タイトル（HOROS-20240407 MainMenu.xib準拠: 4300-4644行目）
    private static final String[] COLUMN_NAMES = {
        "Patient name",       // name (4300行目)
        "Report",             // reportURL (4314行目)
        "Lock",               // lockedStudy (4328行目)
        "Patient ID",         // patientID (4341行目)
        "Age",                // yearOld (4355行目)
        "Accession Number",   // accessionNumber (4369行目)
        "Study Description",  // studyName (4383行目)
        "Modality",           // modality (4397行目)
        "ID",                 // id (4411行目)
        "Comments",           // comment (4425行目)
        "Status",             // stateText (4439行目)
        "Date Acquired",      // date (4453行目)
        "# im",               // noFiles (4467行目)
        "# series",           // noSeries (4480行目)
        "Date Added",         // dateAdded (4493行目)
        "Date Opened",        // dateOpened (4507行目)
        "Referring Physician", // referringPhysician (4521行目)
        "Performing Physician", // performingPhysician (4535行目)
        "Institution",        // institutionName (4549行目)
        "Albums",             // albumsNames (4563行目)
        "Date of Birth",      // dateOfBirth (4577行目)
        "∆",                  // localstring (4591行目)
        "Comments 2",         // comment2 (4603行目)
        "Comments 3",         // comment3 (4617行目)
        "Comments 4"          // comment4 (4631行目)
    };
    
    // 列識別子（HOROS-20240407 MainMenu.xib準拠: 4300-4644行目）
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
        COL_DATE_OF_BIRTH,
        COL_LOCALSTRING,
        COL_COMMENT2,
        COL_COMMENT3,
        COL_COMMENT4
    };
    
    // 列幅（HOROS-20240407 MainMenu.xib準拠: 4300-4644行目）
    private static final int[] COLUMN_WIDTHS = {
        222,  // name (4300行目: width="222")
        100,  // reportURL (4314行目: width="100")
        50,   // lockedStudy (4328行目: width="50")
        100,  // patientID (4341行目: width="100")
        70,   // yearOld (4355行目: width="70")
        119,  // accessionNumber (4369行目: width="119")
        155,  // studyName (4383行目: width="155")
        65,   // modality (4397行目: width="65")
        60,   // id (4411行目: width="60")
        100,  // comment (4425行目: width="100")
        100,  // stateText (4439行目: width="100")
        130,  // date (4453行目: width="130")
        50,   // noFiles (4467行目: width="50")
        50,   // noSeries (4480行目: width="50")
        130,  // dateAdded (4493行目: width="130")
        130,  // dateOpened (4507行目: width="130")
        120,  // referringPhysician (4521行目: width="120")
        120,  // performingPhysician (4535行目: width="120")
        120,  // institutionName (4549行目: width="120")
        120,  // albumsNames (4563行目: width="120")
        90,   // dateOfBirth (4577行目: width="90")
        16,   // localstring (4591行目: width="16")
        100,  // comment2 (4603行目: width="100")
        100,  // comment3 (4617行目: width="100")
        100   // comment4 (4631行目: width="100")
    };
    
    // 列最小幅（HOROS-20240407 MainMenu.xib準拠: 4300-4644行目）
    private static final int[] COLUMN_MIN_WIDTHS = {
        50,   // name (4300行目: minWidth="50")
        55,   // reportURL (4314行目: minWidth="55")
        45,   // lockedStudy (4328行目: minWidth="45")
        50,   // patientID (4341行目: minWidth="50")
        50,   // yearOld (4355行目: minWidth="50")
        50,   // accessionNumber (4369行目: minWidth="50")
        50,   // studyName (4383行目: minWidth="50")
        65,   // modality (4397行目: minWidth="65")
        60,   // id (4411行目: minWidth="60")
        100,  // comment (4425行目: minWidth="100")
        100,  // stateText (4439行目: minWidth="100")
        130,  // date (4453行目: minWidth="130")
        50,   // noFiles (4467行目: minWidth="50")
        50,   // noSeries (4480行目: minWidth="50")
        130,  // dateAdded (4493行目: minWidth="130")
        130,  // dateOpened (4507行目: minWidth="130")
        40,   // referringPhysician (4521行目: minWidth="40")
        40,   // performingPhysician (4535行目: minWidth="40")
        40,   // institutionName (4549行目: minWidth="40")
        40,   // albumsNames (4563行目: minWidth="40")
        90,   // dateOfBirth (4577行目: minWidth="90")
        15,   // localstring (4591行目: minWidth="15.703120231628418" -> 15)
        100,  // comment2 (4603行目: minWidth="100")
        100,  // comment3 (4617行目: minWidth="100")
        100   // comment4 (4631行目: minWidth="100")
    };
    
    // 列最大幅（HOROS-20240407 MainMenu.xib準拠: 4300-4644行目）
    private static final int[] COLUMN_MAX_WIDTHS = {
        400,  // name (4300行目: maxWidth="400")
        120,  // reportURL (4314行目: maxWidth="120")
        55,   // lockedStudy (4328行目: maxWidth="55")
        200,  // patientID (4341行目: maxWidth="200")
        100,  // yearOld (4355行目: maxWidth="100")
        200,  // accessionNumber (4369行目: maxWidth="200")
        400,  // studyName (4383行目: maxWidth="400")
        100,  // modality (4397行目: maxWidth="100")
        100,  // id (4411行目: maxWidth="100")
        300,  // comment (4425行目: maxWidth="300")
        100,  // stateText (4439行目: maxWidth="100")
        180,  // date (4453行目: maxWidth="180")
        50,   // noFiles (4467行目: maxWidth="50")
        50,   // noSeries (4480行目: maxWidth="50")
        180,  // dateAdded (4493行目: maxWidth="180")
        180,  // dateOpened (4507行目: maxWidth="180")
        400,  // referringPhysician (4521行目: maxWidth="400")
        400,  // performingPhysician (4535行目: maxWidth="400")
        400,  // institutionName (4549行目: maxWidth="400")
        200,  // albumsNames (4563行目: maxWidth="200")
        140,  // dateOfBirth (4577行目: maxWidth="140")
        16,   // localstring (4591行目: maxWidth="16")
        300,  // comment2 (4603行目: maxWidth="300")
        300,  // comment3 (4617行目: maxWidth="300")
        300   // comment4 (4631行目: maxWidth="300")
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
        // HOROS-20240407準拠: TreeTableModelにDatabaseOutlineViewへの参照を設定（列の識別子を取得するため）
        treeTableModel.setOutlineView(this);
        setTreeTableModel(treeTableModel);
        
        // デフォルト設定
        setRootVisible(false);
        setShowsRootHandles(true);
        
        // HOROS-20240407準拠: 行の高さを設定（Regular mode: 17）
        setRowHeight(17);
        
        // HOROS-20240407準拠: 横スクロールを有効にする
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // HOROS-20240407準拠: 列を自動的に作成（NSOutlineViewは自動的に列を作成）
        // ただし、列の識別子を確実に設定するため、falseにして手動で列を作成
        setAutoCreateColumnsFromModel(false);
        
        // HOROS-20240407準拠: 行選択を有効にする（どのカラムをクリックしても行が選択される）
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        
        // 選択リスナーを追加
        addSelectionChangeListener();
        
        // ダブルクリックリスナーを追加
        addDoubleClickListener();
        
        // HOROS-20240407準拠: stateText列のクリックイベントを追加
        addStateTextClickListener();
        
        // HOROS-20240407準拠: name列（識別子が"name"の列）のみでUIDハイライトを適用
        // prepareRendererで処理するため、TreeCellRendererのカスタマイズは不要
        
        // HOROS-20240407準拠: BrowserController.m 6665行目 - outlineView:sortDescriptorsDidChange:
        // NSOutlineViewは自動的にヘッダークリックでソートし、sortDescriptorsDidChange:を呼び出す
        // Java SwingのJXTreeTableにはこの自動機能がないため、手動でリスナーを追加
        // 【カスタムロジック】プラットフォーム差を埋めるための必要最小限の実装
        // 注意: MouseListenerを追加すると列の移動とリサイズが妨害されるため、一旦無効化
        // TODO: 列の移動とリサイズが動作することを確認後、ソート機能を再実装
        // addColumnHeaderClickListener();
        
        // HOROS-20240407準拠: ソートインジケーター表示用のカスタムヘッダーレンダラーを設定
        setCustomHeaderRenderer();
        
        // HOROS-20240407準拠: BrowserController.m 6739-6756行目 - willDisplayCell
        // 履歴パネルに表示されているスタディと同じ患者UIDを持つDBリスト行の背景色を変更
        // HOROS-20240407準拠: name列（最初の列）のみで背景色を設定（6739行目）
        // 列が作成された後にレンダラーを設定する必要があるため、SwingUtilities.invokeLaterを使用
        
        // HOROS-20240407準拠: 列幅を設定（リサイズ可能にする）
        // 列が作成された後に呼び出す必要があるため、SwingUtilities.invokeLaterを使用
        SwingUtilities.invokeLater(() -> {
            // HOROS-20240407準拠: name列（最初の列）のみで背景色を設定（6739行目）
            // JXTreeTableでは、列が移動した場合でも正しい列を識別するため、
            // すべての列にレンダラーを設定し、レンダラー内でname列かどうかを判定
            ComparativePatientCellRenderer comparativeRenderer = new ComparativePatientCellRenderer();
            // すべての列にレンダラーを設定（列が移動した場合でも正しく動作するため）
            javax.swing.table.TableColumnModel colModel = getColumnModel();
            // デフォルトレンダラーも設定（JXTreeTableが最初の列に特別なレンダラーを使用する可能性があるため）
            setDefaultRenderer(Object.class, comparativeRenderer);
            for (int i = 0; i < colModel.getColumnCount(); i++) {
                javax.swing.table.TableColumn column = colModel.getColumn(i);
                Object identifier = column.getIdentifier();
                // 識別子が正しく設定されていない場合は、COLUMN_IDENTIFIERSから設定
                if (i < COLUMN_IDENTIFIERS.length && (identifier == null || !COLUMN_IDENTIFIERS[i].equals(identifier.toString()))) {
                    column.setIdentifier(COLUMN_IDENTIFIERS[i]);
                }
                column.setCellRenderer(comparativeRenderer);
            }
            // HOROS-20240407準拠: BrowserController.m 11646行目、MyOutlineView.m 127行目 - moveColumn:toColumn:
            // NSOutlineViewは標準で列の移動をサポート（columnReordering）
            // Java Swingでも標準機能（setReorderingAllowed）を使用
            // 【標準機能】カスタムロジックではない
            javax.swing.table.JTableHeader header = getTableHeader();
            if (header != null) {
                header.setReorderingAllowed(true);
            }
            
            applyColumnWidths();
            
            // 列の移動とリサイズを確実に有効にする（applyColumnWidths()の後に再設定）
            if (header != null) {
                header.setReorderingAllowed(true);
            }
            // HOROS-20240407準拠: 最初の列（階層表示用）は固定（移動不可、リサイズ不可）
            // Patient Name列は2番目の列として作成される
            javax.swing.table.TableColumnModel columnModel = getColumnModel();
            if (columnModel != null) {
                for (int i = 0; i < columnModel.getColumnCount(); i++) {
                    javax.swing.table.TableColumn column = columnModel.getColumn(i);
                    
                    // HOROS-20240407準拠: 最初の列（インデックス0）は開閉マーク専用で固定
                    // 移動不可、リサイズ不可
                    if (i == 0) {
                        column.setResizable(false);
                        column.setMinWidth(20); // 開閉マークのみの幅
                        column.setMaxWidth(20); // 固定幅
                        column.setPreferredWidth(20);
                        column.setWidth(20);
                    } else {
                        // その他の列は移動可能、リサイズ可能
                        column.setResizable(true);
                        // 列の最小幅を設定（リサイズを確実に有効にする）
                        if (column.getMinWidth() < 20) {
                            column.setMinWidth(20);
                        }
                    }
                }
            }
            
            // HOROS-20240407準拠: 列の移動を制限（最初の列は移動不可）
            // カスタムヘッダーで列移動を制御
            setupColumnReorderingRestrictions();
            
            // HOROS-20240407準拠: BrowserController.m 6665行目 - outlineView:sortDescriptorsDidChange:
            // NSOutlineViewは自動的にヘッダークリックでソートし、sortDescriptorsDidChange:を呼び出す
            // Java SwingのJXTreeTableにはこの自動機能がないため、手動でリスナーを追加する必要がある
            // 【重要】列の移動とリサイズを妨害しないよう、イベントを消費しないように実装
            addColumnHeaderClickListener();
        });
    }
    
    // HOROS-20240407準拠: ソート状態を保持
    private String currentSortColumn = null;
    private boolean currentSortAscending = true;
    
    // 列の復元中かどうかを示すフラグ（columnMovedイベントを無視するため）
    private boolean isRestoringColumns = false;
    
    /**
     * カスタムヘッダーレンダラーを設定（ソートインジケーター表示用）
     * HOROS-20240407準拠: NSOutlineViewのsetIndicatorImage:inTableColumn:相当
     * 
     * 注意: デフォルトヘッダーレンダラーを設定すると列の移動とリサイズが妨害されるため、
     * ソートインジケーターはupdateSortIndicator()で個別の列に設定する
     */
    private void setCustomHeaderRenderer() {
        // デフォルトヘッダーレンダラーは設定しない（列の移動とリサイズを妨害しないため）
        // ソートインジケーターはupdateSortIndicator()で個別の列に設定する
    }
    
    /**
     * ソートインジケーターを更新
     * HOROS-20240407準拠: setIndicatorImage:inTableColumn:相当
     * 
     * 列の移動とリサイズを妨害しないよう、DefaultTableCellRendererを継承した
     * カスタムヘッダーレンダラーを使用してソートインジケーターを表示
     * 
     * @param sortColumn ソート列の識別子
     * @param ascending 昇順の場合true
     */
    public void updateSortIndicator(String sortColumn, boolean ascending) {
        currentSortColumn = sortColumn;
        currentSortAscending = ascending;
        
        // HOROS-20240407準拠: ソートインジケーターを表示
        // 列の移動とリサイズを妨害しないよう、DefaultTableCellRendererを継承したカスタムヘッダーレンダラーを使用
        javax.swing.table.TableColumnModel columnModel = getColumnModel();
        javax.swing.table.JTableHeader header = getTableHeader();
        
        if (columnModel == null || header == null) {
            return;
        }
        
        // すべての列のヘッダーレンダラーを設定（列の移動とリサイズを妨害しない）
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            javax.swing.table.TableColumn column = columnModel.getColumn(i);
            Object identifier = column.getIdentifier();
            String columnId = (identifier != null) ? identifier.toString() : null;
            
            // ソート列かどうかを判定
            boolean isSortColumn = (currentSortColumn != null && columnId != null && 
                                   currentSortColumn.equals(columnId));
            
            if (isSortColumn) {
                // ソート列の場合、ソートインジケーター付きのヘッダーレンダラーを設定
                column.setHeaderRenderer(new SortableHeaderRenderer());
            } else {
                // ソート列でない場合も、フラットなスタイルのヘッダーレンダラーを使用
                column.setHeaderRenderer(new FlatHeaderRenderer());
            }
        }
        
        header.repaint();
    }
    
    /**
     * フラットなヘッダーレンダラー（ソート列でない場合）
     * HOROS-20240407準拠: 全てのヘッダーをフラットなスタイルにする
     */
    private class FlatHeaderRenderer implements javax.swing.table.TableCellRenderer {
        
        @Override
        public java.awt.Component getTableCellRendererComponent(
                javax.swing.JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            
            // ヘッダーテキストを設定
            String headerText = value != null ? value.toString() : "";
            
            // HOROS-20240407準拠: フラットなヘッダースタイル（ボーダーなし）
            javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout()) {
                @Override
                protected void processMouseEvent(java.awt.event.MouseEvent e) {
                    // HOROS-20240407準拠: 列の移動とリサイズを妨害しないように、イベントを親に転送
                    java.awt.Component parent = getParent();
                    if (parent != null) {
                        java.awt.Point point = javax.swing.SwingUtilities.convertPoint(this, e.getPoint(), parent);
                        java.awt.event.MouseEvent newEvent = new java.awt.event.MouseEvent(
                            parent, e.getID(), e.getWhen(), e.getModifiersEx(),
                            point.x, point.y, e.getClickCount(), e.isPopupTrigger(), e.getButton()
                        );
                        parent.dispatchEvent(newEvent);
                    }
                }
                
                @Override
                protected void processMouseMotionEvent(java.awt.event.MouseEvent e) {
                    // HOROS-20240407準拠: 列の移動とリサイズを妨害しないように、イベントを親に転送
                    java.awt.Component parent = getParent();
                    if (parent != null) {
                        java.awt.Point point = javax.swing.SwingUtilities.convertPoint(this, e.getPoint(), parent);
                        java.awt.event.MouseEvent newEvent = new java.awt.event.MouseEvent(
                            parent, e.getID(), e.getWhen(), e.getModifiersEx(),
                            point.x, point.y, e.getClickCount(), e.isPopupTrigger(), e.getButton()
                        );
                        parent.dispatchEvent(newEvent);
                    }
                }
            };
            
            // HOROS-20240407準拠: フラットなヘッダースタイル（ボーダーなし、背景色のみ）
            panel.setOpaque(true);
            panel.setBackground(table.getTableHeader().getBackground());
            panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5));
            
            // ヘッダーテキスト（左寄せ）
            javax.swing.JLabel textLabel = new javax.swing.JLabel(headerText);
            textLabel.setForeground(table.getTableHeader().getForeground());
            textLabel.setFont(table.getTableHeader().getFont());
            textLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            panel.add(textLabel, java.awt.BorderLayout.WEST);
            
            return panel;
        }
    }
    
    /**
     * ソート可能なヘッダーレンダラー
     * HOROS-20240407準拠: NSOutlineViewのソートインジケーター表示を再現
     * 
     * 列の移動とリサイズを妨害しないよう、JPanelを使用してレイアウトを制御し、
     * マウスイベントを親に転送する
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
            
            // ヘッダーテキストを設定
            String headerText = value != null ? value.toString() : "";
            
            // HOROS-20240407準拠: フラットなヘッダースタイル（ボーダーなし）
            javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout()) {
                @Override
                protected void processMouseEvent(java.awt.event.MouseEvent e) {
                    // HOROS-20240407準拠: 列の移動とリサイズを妨害しないように、イベントを親に転送
                    java.awt.Component parent = getParent();
                    if (parent != null) {
                        java.awt.Point point = javax.swing.SwingUtilities.convertPoint(this, e.getPoint(), parent);
                        java.awt.event.MouseEvent newEvent = new java.awt.event.MouseEvent(
                            parent, e.getID(), e.getWhen(), e.getModifiersEx(),
                            point.x, point.y, e.getClickCount(), e.isPopupTrigger(), e.getButton()
                        );
                        parent.dispatchEvent(newEvent);
                    }
                }
                
                @Override
                protected void processMouseMotionEvent(java.awt.event.MouseEvent e) {
                    // HOROS-20240407準拠: 列の移動とリサイズを妨害しないように、イベントを親に転送
                    java.awt.Component parent = getParent();
                    if (parent != null) {
                        java.awt.Point point = javax.swing.SwingUtilities.convertPoint(this, e.getPoint(), parent);
                        java.awt.event.MouseEvent newEvent = new java.awt.event.MouseEvent(
                            parent, e.getID(), e.getWhen(), e.getModifiersEx(),
                            point.x, point.y, e.getClickCount(), e.isPopupTrigger(), e.getButton()
                        );
                        parent.dispatchEvent(newEvent);
                    }
                }
            };
            
            // HOROS-20240407準拠: フラットなヘッダースタイル（ボーダーなし、背景色のみ）
            panel.setOpaque(true);
            panel.setBackground(table.getTableHeader().getBackground());
            panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5));
            
            // ヘッダーテキスト（左寄せ）
            javax.swing.JLabel textLabel = new javax.swing.JLabel(headerText);
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
     * 
     * 注意: 列ヘッダーのクリックを除外し、列の移動とリサイズを妨害しない
     */
    private void addDoubleClickListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // HOROS-20240407準拠: 列ヘッダーのクリックを除外（列の移動とリサイズを妨害しない）
                if (e.getSource() instanceof javax.swing.table.JTableHeader) {
                    return;
                }
                
                // HOROS-20240407準拠: テーブル本体のクリックのみ処理
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
     * stateText列のクリックイベントを追加
     * HOROS-20240407準拠: NSPopUpButtonCellの動作を再現
     */
    private void addStateTextClickListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // HOROS-20240407準拠: 列ヘッダーのクリックを除外
                if (e.getSource() instanceof javax.swing.table.JTableHeader) {
                    return;
                }
                
                // HOROS-20240407準拠: stateText列のクリックを処理
                int row = rowAtPoint(e.getPoint());
                int column = columnAtPoint(e.getPoint());
                
                if (row >= 0 && column >= 0) {
                    javax.swing.table.TableColumn tableColumn = getColumnModel().getColumn(column);
                    Object identifier = tableColumn != null ? tableColumn.getIdentifier() : null;
                    
                    if (identifier != null && COL_STATE_TEXT.equals(identifier.toString())) {
                        // HOROS-20240407準拠: stateText列がクリックされた場合、ポップアップメニューを表示
                        TreePath path = getPathForRow(row);
                        if (path != null) {
                            Object item = path.getLastPathComponent();
                            if (item instanceof DicomStudy) {
                                showStateTextPopupMenu((DicomStudy) item, e.getComponent(), e.getX(), e.getY());
                            } else if (item instanceof DicomSeries) {
                                DicomSeries series = (DicomSeries) item;
                                DicomStudy study = series.getStudy();
                                if (study != null) {
                                    showStateTextPopupMenu(study, e.getComponent(), e.getX(), e.getY());
                                }
                            }
                        }
                    }
                }
            }
        });
    }
    
    /**
     * stateText列のポップアップメニューを表示
     * HOROS-20240407準拠: NSPopUpButtonCellの動作を再現
     */
    private void showStateTextPopupMenu(DicomStudy study, java.awt.Component component, int x, int y) {
        String[] statesArray = getStatesArray();
        javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
        
        for (int i = 1; i < statesArray.length; i++) {
            String state = statesArray[i];
            javax.swing.JMenuItem menuItem = new javax.swing.JMenuItem(state);
            final int stateIndex = i;
            menuItem.addActionListener(evt -> {
                // HOROS-20240407準拠: stateTextを更新
                study.setStateText(stateIndex);
                // テーブルを再描画
                repaint();
            });
            popup.add(menuItem);
        }
        
        popup.show(component, x, y);
    }
    
    /**
     * 列の移動を制限（最初の列は移動不可）
     * HOROS-20240407準拠: 最初の列（階層表示用）は固定され、移動やリサイズができない
     */
    private void setupColumnReorderingRestrictions() {
        javax.swing.table.TableColumnModel columnModel = getColumnModel();
        if (columnModel == null) {
            return;
        }
        
        // 列移動イベントを監視して、最初の列の移動を防ぐ
        columnModel.addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override
            public void columnAdded(javax.swing.event.TableColumnModelEvent e) {
                // 列が追加された後、最初の列を固定
                fixFirstColumn();
            }
            
            @Override
            public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {
                // 列が削除された後、最初の列を固定
                fixFirstColumn();
            }
            
            @Override
            public void columnMoved(javax.swing.event.TableColumnModelEvent e) {
                // 列の復元中はイベントを無視（ループを防ぐため）
                if (isRestoringColumns) {
                    return;
                }
                
                // HOROS-20240407準拠: 最初の列（階層表示用）は移動不可
                // 列が移動された場合、最初の列が移動されていないか確認
                int fromIndex = e.getFromIndex();
                int toIndex = e.getToIndex();
                
                // 最初の列（インデックス0）が移動された場合、元の位置に戻す
                if (toIndex == 0 && fromIndex != 0) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        try {
                            columnModel.moveColumn(0, fromIndex);
                        } catch (Exception ex) {
                            // エラーが発生した場合は無視
                        }
                    });
                } else if (fromIndex == 0 && toIndex != 0) {
                    // 最初の列が移動された場合、元の位置に戻す
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        try {
                            columnModel.moveColumn(toIndex, 0);
                        } catch (Exception ex) {
                            // エラーが発生した場合は無視
                        }
                    });
                }
                
                // 最初の列を固定
                fixFirstColumn();
            }
            
            @Override
            public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                // 列のマージンが変更された後、最初の列を固定
                fixFirstColumn();
            }
            
            @Override
            public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {
                // 列の選択が変更された場合は何もしない
            }
        });
        
        // 初期設定で最初の列を固定
        fixFirstColumn();
    }
    
    /**
     * 最初の列（階層表示用）を固定
     * HOROS-20240407準拠: 最初の列は移動不可、リサイズ不可
     */
    private void fixFirstColumn() {
        javax.swing.table.TableColumnModel columnModel = getColumnModel();
        if (columnModel == null || columnModel.getColumnCount() == 0) {
            return;
        }
        
        // 最初の列を取得
        javax.swing.table.TableColumn firstColumn = columnModel.getColumn(0);
        if (firstColumn != null) {
            // 最初の列は開閉マーク専用で固定（移動不可、リサイズ不可）
            firstColumn.setResizable(false);
            firstColumn.setMinWidth(20); // 開閉マークのみの幅
            firstColumn.setMaxWidth(20); // 固定幅
            firstColumn.setPreferredWidth(20);
            if (firstColumn.getWidth() != 20) {
                firstColumn.setWidth(20);
            }
        }
    }
    
    /**
     * 列ヘッダークリックリスナーを追加
     * HOROS-20240407準拠: BrowserController.m 6665行目 - outlineView:sortDescriptorsDidChange:
     * 
     * 【カスタムロジックの理由】
     * - NSOutlineView: 自動的にヘッダークリックでソートし、sortDescriptorsDidChange:を呼び出す
     * - JXTreeTable: 自動機能がないため、手動でMouseListenerを追加する必要がある
     * 
     * 【実装方針】
     * - HOROS-20240407の動作を可能な限り忠実に再現
     * - ドラッグ操作（列の移動）を妨害しないよう、MouseMotionListenerで検出
     * - 右クリック（列メニュー）を妨害しないよう、isPopupTriggerで判定
     */
    // HOROS-20240407準拠: 列ヘッダーのドラッグ操作を検出するためのフラグ
    // 【カスタムロジック】列の移動とソートを区別するため（NSOutlineViewは自動的に処理）
    private boolean isDraggingColumn = false;
    private java.awt.Point dragStartPoint = null;
    
    private void addColumnHeaderClickListener() {
        javax.swing.table.JTableHeader header = getTableHeader();
        if (header == null) {
            return;
        }
        
        // HOROS-20240407準拠: 列ヘッダークリックでソート（outlineView:sortDescriptorsDidChange:相当）
        // NSOutlineViewは標準で列ヘッダークリックでソートし、列の移動とリサイズも標準機能
        // Java Swingでは、MouseListenerを追加すると標準機能が妨害される可能性があるため、
        // イベントを消費しないように注意する
        // 【カスタムロジック】プラットフォーム差を埋めるための必要最小限の実装
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // HOROS-20240407準拠: 右クリックの場合は処理をスキップ（列メニューを表示するため）
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    return;
                }
                
                // HOROS-20240407準拠: ドラッグ操作（列の移動）の場合はソート処理をスキップ
                if (isDraggingColumn) {
                    isDraggingColumn = false;
                    dragStartPoint = null;
                    return;
                }
                
                // HOROS-20240407準拠: 左クリックの場合のみソート処理を実行
                // mouseClickedは、mousePressedとmouseReleasedの後に呼ばれるため、
                // 列の移動とリサイズが完了した後にのみソート処理を実行
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1 && browserController != null) {
                    int columnIndex = header.columnAtPoint(e.getPoint());
                    if (columnIndex >= 0) {
                        // 列の境界線付近（リサイズ領域）のクリックは除外
                        int x = e.getPoint().x;
                        int columnX = 0;
                        for (int i = 0; i < columnIndex; i++) {
                            columnX += header.getColumnModel().getColumn(i).getWidth();
                        }
                        int columnWidth = header.getColumnModel().getColumn(columnIndex).getWidth();
                        
                        // 列の左端または右端から3ピクセル以内かどうか
                        int distanceFromLeft = Math.abs(x - columnX);
                        int distanceFromRight = Math.abs(x - (columnX + columnWidth));
                        boolean isNearBorder = distanceFromLeft <= 3 || distanceFromRight <= 3;
                        
                        // 列の境界線付近でない場合のみソート処理を実行
                        if (!isNearBorder) {
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
                
                dragStartPoint = null;
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                // HOROS-20240407準拠: 右クリックの場合は処理をスキップ（列メニューを表示するため）
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    return;
                }
                
                // HOROS-20240407準拠: 左クリックの場合、ドラッグ開始位置を記録
                // ただし、イベントを消費しないようにする（列の移動とリサイズを妨害しない）
                if (e.getButton() == MouseEvent.BUTTON1) {
                    dragStartPoint = e.getPoint();
                    isDraggingColumn = false;
                }
            }
        });
        
        // HOROS-20240407準拠: ドラッグ操作（列の移動）を検出するためのMouseMotionListener
        header.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                // HOROS-20240407準拠: ドラッグ操作を検出した場合はフラグを設定
                // ただし、イベントを消費しないようにする（列の移動とリサイズを妨害しない）
                if (dragStartPoint != null) {
                    // ドラッグ開始位置からの移動距離を計算
                    double distance = dragStartPoint.distance(e.getPoint());
                    // 5ピクセル以上移動した場合はドラッグ操作（列の移動）とみなす
                    if (distance > 5) {
                        isDraggingColumn = true;
                    }
                }
            }
        });
    }
    
    /**
     * HOROS-20240407準拠: どのカラムをクリックしても行が選択されるようにする
     * JXTreeTableのchangeSelectionメソッドをオーバーライド
     * 
     * 注意: 列ヘッダーの操作（列の移動とリサイズ）を妨害しないため、一旦標準動作に戻す
     * TODO: 列の移動とリサイズが動作することを確認後、「どのカラムをクリックしても行が選択される」機能を再実装
     */
    // @Override
    // public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
    //     // HOROS-20240407準拠: 列ヘッダーのクリック（rowIndex < 0 または columnIndex == -1）の場合は標準処理を実行
    //     // 列の移動とリサイズを妨害しない
    //     if (rowIndex < 0 || columnIndex == -1) {
    //         super.changeSelection(rowIndex, columnIndex, toggle, extend);
    //         return;
    //     }
    //     
    //     // HOROS-20240407準拠: NSOutlineViewでは、どのカラムをクリックしても行が選択される
    //     // 最初のカラム（階層表示用）を指定して行選択を実行
    //     // 通常の行選択処理（最初のカラムを指定して行選択を実行）
    //     super.changeSelection(rowIndex, 0, toggle, extend);
    // }
    
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
        // 最初の列（インデックス0）は開閉マーク専用、2番目以降がデータ列
        // ユーザー確認: HOROSでは開閉マークは単独列で、その隣にPatient Name列がある
        if (columnModel.getColumnCount() == 0) {
            // JXTreeTableでは、最初の列（インデックス0）は階層表示用で自動的に作成される
            // この列を開閉マーク専用として使用（データは表示しない）
            
            // 2番目以降の列（データ列）を手動で作成
            for (int i = 0; i < COLUMN_NAMES.length; i++) {
                javax.swing.table.TableColumn column = new javax.swing.table.TableColumn(i + 1); // インデックスは+1（最初の列を除く）
                column.setIdentifier(COLUMN_IDENTIFIERS[i]);
                column.setHeaderValue(COLUMN_NAMES[i]);
                columnModel.addColumn(column);
            }
        }
        
        // HOROS-20240407準拠: 各列に識別子と幅を設定
        // 最初の列（インデックス0）は開閉マーク専用、2番目以降がデータ列
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            javax.swing.table.TableColumn column = columnModel.getColumn(i);
            
            // HOROS-20240407準拠: 最初の列（インデックス0）は開閉マーク専用で固定
            if (i == 0) {
                // 最初の列は移動不可、リサイズ不可（開閉マーク専用）
                column.setResizable(false);
                column.setMinWidth(20); // 開閉マークのみの幅
                column.setMaxWidth(20); // 固定幅
                column.setPreferredWidth(20);
                column.setWidth(20);
                // 識別子は設定しない（開閉マーク専用のため）
                column.setHeaderValue(""); // ヘッダーなし
            } else {
                // 2番目以降の列（データ列）に識別子と幅を設定
                int dataColumnIndex = i - 1; // データ列のインデックス（最初の列を除く）
                if (dataColumnIndex < COLUMN_IDENTIFIERS.length) {
                    column.setIdentifier(COLUMN_IDENTIFIERS[dataColumnIndex]);
                }
                if (dataColumnIndex < COLUMN_NAMES.length) {
                    column.setHeaderValue(COLUMN_NAMES[dataColumnIndex]);
                }
                if (dataColumnIndex < COLUMN_WIDTHS.length) {
                    column.setMinWidth(COLUMN_MIN_WIDTHS[dataColumnIndex]);
                    column.setMaxWidth(COLUMN_MAX_WIDTHS[dataColumnIndex]);
                    column.setPreferredWidth(COLUMN_WIDTHS[dataColumnIndex]);
                    column.setResizable(true);
                    column.setWidth(COLUMN_WIDTHS[dataColumnIndex]);
                }
            }
        }
        
        // 列の移動を有効にする（applyColumnWidths()の後に設定）
        javax.swing.table.JTableHeader header = getTableHeader();
        if (header != null) {
            header.setReorderingAllowed(true);
        }
        
        revalidate();
        repaint();
    }
    
    /**
     * カラム状態を取得（保存用）
     * HOROS-20240407準拠: MyOutlineView.m 68-82行目 - (NSObject<NSCoding>*)columnState
     * HOROS-20240407準拠: BrowserController.m 11646行目 - moveColumn:toColumn:で列の順序を復元
     * 【カスタムロジック】NSOutlineViewはautosaveColumnsで自動保存、Java Swingでは手動実装が必要
     * 
     * @return カラム状態（識別子、幅、順序のマップのリスト）
     */
    public List<Map<String, Object>> getColumnState() {
        List<Map<String, Object>> state = new ArrayList<>();
        javax.swing.table.TableColumnModel columnModel = getColumnModel();
        
        // HOROS-20240407準拠: 最初の列（インデックス0）は開閉マーク専用列で、識別子がないため保存しない
        // 2番目以降の列（データ列）のみを保存する
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            javax.swing.table.TableColumn column = columnModel.getColumn(i);
            Object identifier = column.getIdentifier();
            
            // 最初の列（開閉マーク専用列）はスキップ
            if (i == 0) {
                continue;
            }
            
            // 非表示のカラムは除外（幅が0の場合は非表示とみなす）
            if (identifier != null && column.getWidth() > 0) {
                Map<String, Object> columnInfo = new HashMap<>();
                columnInfo.put("Identifier", identifier.toString());
                columnInfo.put("Width", column.getPreferredWidth());
                // HOROS-20240407準拠: 列の順序（位置）を保存（実際の表示位置を保存）
                // 最初の列（開閉マーク専用列）を考慮して、実際の表示位置を保存
                columnInfo.put("Index", i);
                state.add(columnInfo);
            }
        }
        
        return state;
    }
    
    /**
     * カラム状態を復元
     * HOROS-20240407準拠: MyOutlineView.m 84-111行目 - (void)restoreColumnState:(NSArray*)state
     * HOROS-20240407準拠: BrowserController.m 11646行目 - moveColumn:toColumn:で列の順序を復元
     * 【カスタムロジック】NSOutlineViewはautosaveColumnsで自動復元、Java Swingでは手動実装が必要
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
        
        // 列の復元中フラグを設定（columnMovedイベントを無視するため）
        isRestoringColumns = true;
        
        // 最初の列（開閉マーク専用列）を確実に固定（復元前に固定）
        fixFirstColumn();
        
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
            
            // HOROS-20240407準拠: 最初の列（インデックス0）は開閉マーク専用列で、識別子がない
            // nameカラム（Patient Name列）は2番目以降の列にあるため、識別子で検索する
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
        
        // HOROS-20240407準拠: 列の順序（位置）を復元
        // 保存された順序に従って列を並び替える
        // 最初の列（インデックス0）は開閉マーク専用列で固定、2番目以降の列を復元
        
        // まず、すべての列の現在位置を取得
        Map<String, Integer> currentPositions = new HashMap<>();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            javax.swing.table.TableColumn column = columnModel.getColumn(i);
            Object colIdentifier = column.getIdentifier();
            if (colIdentifier != null) {
                currentPositions.put(colIdentifier.toString(), i);
            }
        }
        
        // 保存された状態から、目標位置と現在位置のマッピングを作成
        List<Map<String, Object>> moveOperations = new ArrayList<>();
        for (Map<String, Object> params : state) {
            String identifier = (String) params.get("Identifier");
            if (identifier == null) {
                continue;
            }
            
            // 保存されたIndex（目標位置）を取得
            Object indexObj = params.get("Index");
            int targetIndex = -1;
            if (indexObj instanceof Number) {
                targetIndex = ((Number) indexObj).intValue();
            } else if (indexObj instanceof String) {
                try {
                    targetIndex = Integer.parseInt((String) indexObj);
                } catch (NumberFormatException e) {
                    continue;
                }
            } else {
                continue;
            }
            
            // 現在位置を取得
            Integer currentIndex = currentPositions.get(identifier);
            if (currentIndex != null && targetIndex > 0 && currentIndex != targetIndex) {
                Map<String, Object> moveOp = new HashMap<>();
                moveOp.put("identifier", identifier);
                moveOp.put("currentIndex", currentIndex);
                moveOp.put("targetIndex", targetIndex);
                moveOperations.add(moveOp);
            }
        }
        
        // HOROS-20240407準拠: 列の移動を目標位置の昇順（小さい順）で行う
        // これにより、前の列から順に移動するため、後続の列の位置に影響を与えない
        // ただし、各移動後に現在位置を更新する必要がある
        moveOperations.sort((a, b) -> {
            int targetA = (Integer) a.get("targetIndex");
            int targetB = (Integer) b.get("targetIndex");
            return Integer.compare(targetA, targetB); // 昇順
        });
        
        // 列を目標位置の昇順で移動（各移動後に現在位置を更新）
        // すべての列が正しい位置に移動するまで繰り返す（最大10回）
        int maxIterations = 10;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            boolean allCorrect = true;
            
            for (Map<String, Object> moveOp : moveOperations) {
                String identifier = (String) moveOp.get("identifier");
                int targetIndex = (Integer) moveOp.get("targetIndex");
                
                // 現在位置を再取得（前の移動によって位置が変わっている可能性があるため）
                Integer currentIndex = null;
                for (int i = 0; i < columnModel.getColumnCount(); i++) {
                    javax.swing.table.TableColumn column = columnModel.getColumn(i);
                    Object colIdentifier = column.getIdentifier();
                    if (identifier.equals(colIdentifier != null ? colIdentifier.toString() : "")) {
                        currentIndex = i;
                        break;
                    }
                }
                
                // 最初の列（インデックス0）は開閉マーク専用列で固定のため、移動しない
                // また、最初の列（インデックス0）に移動しようとしている列も除外する
                if (currentIndex != null && targetIndex > 0 && currentIndex != targetIndex && currentIndex != 0) {
                    allCorrect = false;
                    try {
                        columnModel.moveColumn(currentIndex, targetIndex);
                    } catch (IllegalArgumentException e) {
                        // 移動に失敗した場合はスキップ（列が存在しない、または範囲外）
                    }
                }
            }
            
            // すべての列が正しい位置にある場合は終了
            if (allCorrect) {
                break;
            }
        }
        
        // HOROS-20240407準拠: 列の移動とリサイズを確実に有効にする
        javax.swing.table.JTableHeader header = getTableHeader();
        if (header != null) {
            header.setReorderingAllowed(true);
        }
        // すべての列のリサイズを有効にする
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            javax.swing.table.TableColumn column = columnModel.getColumn(i);
            column.setResizable(true);
        }
        
        // 列の復元完了（columnMovedイベントを再度有効化）
        isRestoringColumns = false;
        
        // 最初の列を固定（復元後に確実に固定する）
        // 最初の列が正しく開閉マーク専用列であることを確認
        fixFirstColumn();
        
        // 最初の列に識別子が設定されている場合は削除（開閉マーク専用列のため）
        javax.swing.table.TableColumn firstColumn = columnModel.getColumn(0);
        if (firstColumn != null && firstColumn.getIdentifier() != null) {
            firstColumn.setIdentifier(null);
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
     * 指定されたスタディを選択
     * HOROS-20240407準拠: - (BOOL) selectThisStudy: (NSManagedObject*)study (BrowserController.m 2034行目)
     */
    public boolean selectStudy(com.jj.dicomviewer.model.DicomStudy study) {
        if (study == null) {
            return false;
        }
        
        try {
            // HOROS-20240407準拠: outlineViewArrayから同じstudyInstanceUIDを持つスタディを検索
            List<Object> outlineArray = browserController.getOutlineViewArray();
            if (outlineArray != null) {
                for (int i = 0; i < outlineArray.size(); i++) {
                    Object item = outlineArray.get(i);
                    if (item instanceof com.jj.dicomviewer.model.DicomStudy) {
                        com.jj.dicomviewer.model.DicomStudy s = (com.jj.dicomviewer.model.DicomStudy) item;
                        if (study.getStudyInstanceUID() != null && 
                            study.getStudyInstanceUID().equals(s.getStudyInstanceUID())) {
                            // HOROS-20240407準拠: スタディを選択
                            // JXTreeTableでは、TreePathを作成して選択する
                            TreePath path = new TreePath(new Object[] { treeTableModel.getRoot(), s });
                            getTreeSelectionModel().setSelectionPath(path);
                            scrollPathToVisible(path);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // エラーは無視
        }
        
        return false;
    }
    
    /**
     * TreeTableModel実装
     * HOROS-20240407準拠: NSOutlineViewDataSource/NSOutlineViewDelegate
     */
    public static class DatabaseOutlineTreeTableModel extends AbstractTreeTableModel {
        
        private BrowserController browserController;
        private DatabaseOutlineView outlineView; // HOROS-20240407準拠: 列の識別子を取得するため
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        
        public DatabaseOutlineTreeTableModel(BrowserController browserController) {
            super("Root");
            this.browserController = browserController;
        }
        
        /**
         * DatabaseOutlineViewへの参照を設定
         * HOROS-20240407準拠: 列が移動された場合でも正しい識別子を取得するため
         * 
         * 【カスタムロジックの理由】
         * - NSOutlineView: tableColumnWithIdentifier:で識別子から列を取得可能
         * - JXTreeTable: 列移動後も正しい識別子を取得するため、TableColumnModelへのアクセスが必要
         * 
         * 【実装方針】
         * - TreeTableModelから直接TableColumnModelにアクセスできないため、参照を保持
         * - 列移動後もCOLUMN_IDENTIFIERS[column]ではなく、実際の列の識別子を使用
         */
        public void setOutlineView(DatabaseOutlineView outlineView) {
            this.outlineView = outlineView;
        }
        
        @Override
        public int getColumnCount() {
            // HOROS-20240407準拠: 開閉マーク専用列 + データ列
            // 最初の列（インデックス0）は開閉マーク専用、2番目以降がデータ列
            // ユーザー確認: HOROSでは開閉マークは単独列で、その隣にPatient Name列がある
            return COLUMN_NAMES.length + 1; // 開閉マーク専用列を追加
        }
        
        @Override
        public String getColumnName(int column) {
            // HOROS-20240407準拠: 最初の列（インデックス0）は開閉マーク専用
            if (column == 0) {
                return ""; // 開閉マーク専用列はヘッダーなし
            }
            
            // 2番目以降の列はデータ列（column-1がデータ列のインデックス）
            int dataColumnIndex = column - 1;
            
            // HOROS-20240407準拠: 列が移動された場合でも正しい列名を取得するため、
            // TableColumnModelから直接識別子を取得して列名を返す
            // 【重要】columnパラメータはモデルインデックス（TreeTableModelの列インデックス）
            // 表示インデックス（view index）に変換する必要がある
            if (outlineView != null) {
                javax.swing.table.TableColumnModel columnModel = outlineView.getColumnModel();
                if (columnModel != null) {
                    // モデルインデックスを表示インデックスに変換
                    int viewColumn = outlineView.convertColumnIndexToView(column);
                    if (viewColumn >= 0 && viewColumn < columnModel.getColumnCount()) {
                        javax.swing.table.TableColumn tableColumn = columnModel.getColumn(viewColumn);
                        Object identifier = tableColumn.getIdentifier();
                        if (identifier != null) {
                            String columnId = identifier.toString();
                            // 識別子から列名を取得
                            for (int i = 0; i < COLUMN_IDENTIFIERS.length; i++) {
                                if (COLUMN_IDENTIFIERS[i].equals(columnId)) {
                                    return COLUMN_NAMES[i];
                                }
                            }
                        }
                    }
                }
            }
            // フォールバック: 配列インデックスを使用（列が移動されていない場合）
            if (dataColumnIndex >= 0 && dataColumnIndex < COLUMN_NAMES.length) {
                return COLUMN_NAMES[dataColumnIndex];
            }
            return "";
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
            if (node == null || column < 0) return "";
            
            // HOROS-20240407準拠: 最初の列（インデックス0）は開閉マーク専用でデータは表示しない
            if (column == 0) {
                return ""; // 開閉マーク専用列はデータを返さない
            }
            
            // 2番目以降の列はデータ列（column-1がデータ列のインデックス）
            int dataColumnIndex = column - 1;
            
            // HOROS-20240407準拠: 列が移動された場合でも正しい識別子を取得するため、
            // TableColumnModelから直接識別子を取得
            // 【重要】columnパラメータはモデルインデックス（TreeTableModelの列インデックス）
            // 表示インデックス（view index）に変換する必要がある
            String columnId = null;
            if (outlineView != null) {
                // HOROS-20240407準拠: 列が移動された場合でも正しい識別子を取得
                javax.swing.table.TableColumnModel columnModel = outlineView.getColumnModel();
                if (columnModel != null) {
                    // モデルインデックスを表示インデックスに変換
                    int viewColumn = outlineView.convertColumnIndexToView(column);
                    if (viewColumn >= 0 && viewColumn < columnModel.getColumnCount()) {
                        javax.swing.table.TableColumn tableColumn = columnModel.getColumn(viewColumn);
                        Object identifier = tableColumn.getIdentifier();
                        columnId = identifier != null ? identifier.toString() : null;
                    }
                }
            }
            
            // フォールバック: 配列インデックスを使用（列が移動されていない場合）
            if (columnId == null || columnId.isEmpty()) {
                if (dataColumnIndex >= 0 && dataColumnIndex < COLUMN_IDENTIFIERS.length) {
                    columnId = COLUMN_IDENTIFIERS[dataColumnIndex];
                } else {
                    return "";
                }
            }
            
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
                    // HOROS-20240407準拠: BrowserController.m 6477-6493行目
                    // HIDEPATIENTNAME設定を確認
                    // TODO: NSUserDefaultsから取得（現在はデフォルトfalseと仮定）
                    boolean hidePatientName = false;
                    if (hidePatientName) {
                        return "Name hidden"; // HOROS-20240407準拠: NSLocalizedString(@"Name hidden", nil)
                    }
                    String name = study.getName();
                    if (name == null || name.isEmpty()) {
                        name = study.getPatientID();
                    }
                    return name != null ? name : "";
                    
                case COL_REPORT_URL:
                    // HOROS-20240407準拠: BrowserController.m 6440-6457行目
                    // Studyの場合、reportImageのdateを返す
                    if (study.getReportURL() != null && !study.getReportURL().isEmpty()) {
                        // HOROS-20240407準拠: DicomStudy *study = (DicomStudy*) item;
                        // DicomImage *report = [study reportImage];
                        // if( [report valueForKey: @"date"]) return [report valueForKey: @"date"];
                        // TODO: reportImageメソッドの実装が必要
                        // 現在はreportURLが存在する場合のみ空文字列を返す（dateは後で実装）
                        return ""; // TODO: reportImageのdateを返す
                    }
                    return "";
                    
                case COL_LOCKED_STUDY:
                    // HOROS-20240407準拠: BrowserController.m 6467-6470行目
                    // Studyでない場合はnil（ここではStudyなので値を返す）
                    // TODO: lockedStudyの実装が必要
                    return ""; // TODO: lockedStudyの値を返す
                    
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
                    // HOROS-20240407準拠: BrowserController.m 6472-6475行目
                    // return [item valueForKey:@"modality"];
                    // Studyのmodalityプロパティを直接返す（modalities()ではない）
                    String modality = study.getModality();
                    return modality != null && !modality.isEmpty() ? modality : "";
                    
                case COL_ID:
                    // HOROS-20240407準拠: BrowserController.m 6560-6561行目
                    // value = [item valueForKey:[tableColumn identifier]];
                    // idプロパティの値を返す（画像数ではない）
                    String id = study.getId();
                    // HOROS-20240407準拠: idがnullまたは空の場合は空文字列を返す
                    if (id == null || id.isEmpty()) {
                        return "";
                    }
                    // HOROS-20240407準拠: idを文字列として返す（数値として処理されないように）
                    return id;
                    
                case COL_COMMENT:
                    return study.getComment() != null ? study.getComment() : "";
                    
                case COL_STATE_TEXT:
                    // HOROS-20240407準拠: BrowserController.m 6459-6465行目
                    // intValueが0の場合はnil、それ以外はstateTextを返す
                    // HOROS-20240407準拠: BrowserController.m 14293行目 - statesArrayから文字列を取得
                    Integer stateText = study.getStateText();
                    if (stateText == null || stateText == 0) {
                        return ""; // HOROS-20240407準拠: nilの場合は空文字列
                    }
                    // HOROS-20240407準拠: BrowserController.m 14293行目
                    // statesArray = [@"empty", @"unread", @"reviewed", @"dictated", @"validated"]
                    String[] statesArray = getStatesArray();
                    if (stateText.intValue() > 0 && stateText.intValue() < statesArray.length) {
                        return statesArray[stateText.intValue()];
                    }
                    return String.valueOf(stateText);
                    
                case COL_DATE:
                    LocalDateTime date = study.getDate();
                    return date != null ? date.format(DATE_FORMATTER) : "";
                    
                case COL_NO_FILES:
                    // HOROS-20240407準拠: BrowserController.m 6560-6561行目
                    // value = [item valueForKey:[tableColumn identifier]];
                    // noFilesプロパティの値を返す
                    Integer noFiles = study.noFiles();
                    return noFiles != null ? String.valueOf(noFiles) : "";
                    
                case COL_NO_SERIES:
                    // HOROS-20240407準拠: BrowserController.m 6540-6546行目
                    // imageSeriesのcountを返す
                    // HOROS-20240407準拠: [item valueForKey:@"imageSeries"]でKVO経由でimageSeriesメソッドを呼び出す
                    // HOROS-20240407準拠: imageSeries()を呼ぶ前に、seriesを読み込む必要がある
                    if (browserController != null && browserController.getDatabase() != null) {
                        browserController.getDatabase().loadSeriesForStudyIfNeeded(study);
                    }
                    try {
                        List<DicomSeries> imageSeriesList = study.imageSeries();
                        if (imageSeriesList != null && !imageSeriesList.isEmpty()) {
                            return String.valueOf(imageSeriesList.size());
                        }
                        // HOROS-20240407準拠: imageSeriesがnullまたは空の場合は0を返す（空文字列ではなく）
                        return "0";
                    } catch (Exception e) {
                        // エラーが発生した場合は0を返す
                        return "0";
                    }
                    
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
                    // HOROS-20240407準拠: 生年月日
                    java.time.LocalDate dateOfBirth = study.getDateOfBirth();
                    return dateOfBirth != null ? dateOfBirth.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
                    
                case COL_LOCALSTRING:
                    // HOROS-20240407準拠: DicomStudy.m 1291-1310行目
                    // inDatabaseFolderがtrueの場合は"L"を返し、falseの場合は空文字列を返す
                    String localstring = study.localstring();
                    return localstring != null ? localstring : "";
                    
                case COL_COMMENT2:
                    // HOROS-20240407準拠: MainMenu.xib 4603行目 - comment2列
                    return study.getComment2() != null ? study.getComment2() : "";
                    
                case COL_COMMENT3:
                    // HOROS-20240407準拠: MainMenu.xib 4617行目 - comment3列
                    return study.getComment3() != null ? study.getComment3() : "";
                    
                case COL_COMMENT4:
                    // HOROS-20240407準拠: MainMenu.xib 4631行目 - comment4列
                    return study.getComment4() != null ? study.getComment4() : "";
                    
                default:
                    return "";
            }
        }
        
        /**
         * Seriesの値を取得
         * HOROS-20240407準拠: Seriesの場合、一部の列のみ表示
         */
        private String getSeriesValue(DicomSeries series, String columnId) {
            // HOROS-20240407準拠: BrowserController.m 6495-6505行目
            // Seriesの場合、特定の列は空文字列を返す
            if (COL_DATE_OF_BIRTH.equals(columnId) ||
                COL_REFERRING_PHYSICIAN.equals(columnId) ||
                COL_PERFORMING_PHYSICIAN.equals(columnId) ||
                COL_INSTITUTION_NAME.equals(columnId) ||
                COL_PATIENT_ID.equals(columnId) ||
                COL_YEAR_OLD.equals(columnId) ||
                COL_ACCESSION_NUMBER.equals(columnId) ||
                COL_NO_SERIES.equals(columnId)) {
                return ""; // HOROS-20240407準拠: Seriesでは空文字列を返す
            }
            
            // HOROS-20240407準拠: BrowserController.m 6551-6558行目
            // Seriesの場合、studyName列はseriesDescriptionを返す
            if (COL_STUDY_NAME.equals(columnId)) {
                String seriesDescription = series.getSeriesDescription();
                // HOROS-20240407準拠: BrowserController.m 6563-6570行目
                // Seriesの場合、name, studyName, modalityが空の場合は"unknown"を返す
                if (seriesDescription == null || seriesDescription.isEmpty()) {
                    return "unknown"; // HOROS-20240407準拠: NSLocalizedString(@"unknown", nil)
                }
                return seriesDescription;
            }
            
            String value = null;
            
            switch (columnId) {
                case COL_NAME:
                    // HOROS-20240407準拠: BrowserController.m 6560-6571行目
                    // value = [item valueForKey:[tableColumn identifier]];
                    value = series.getName();
                    // HOROS-20240407準拠: BrowserController.m 6563-6570行目
                    // Seriesの場合、name, studyName, modalityが空の場合は"unknown"を返す
                    if (value == null || value.isEmpty()) {
                        return "unknown"; // HOROS-20240407準拠: NSLocalizedString(@"unknown", nil)
                    }
                    return value;
                    
                case COL_MODALITY:
                    // HOROS-20240407準拠: BrowserController.m 6560-6571行目
                    value = series.getModality();
                    // HOROS-20240407準拠: BrowserController.m 6563-6570行目
                    // Seriesの場合、name, studyName, modalityが空の場合は"unknown"を返す
                    if (value == null || value.isEmpty()) {
                        return "unknown"; // HOROS-20240407準拠: NSLocalizedString(@"unknown", nil)
                    }
                    return value;
                    
                case COL_ID:
                    // HOROS-20240407準拠: BrowserController.m 6560-6561行目
                    // value = [item valueForKey:[tableColumn identifier]];
                    // idプロパティの値を返す（画像数ではない）
                    Integer seriesId = series.getId();
                    return seriesId != null ? String.valueOf(seriesId) : "";
                    
                case COL_DATE:
                    LocalDateTime date = series.getDate();
                    return date != null ? date.format(DATE_FORMATTER) : "";
                    
                case COL_DATE_ADDED:
                    LocalDateTime dateAdded = series.getDateAdded();
                    return dateAdded != null ? dateAdded.format(DATE_FORMATTER) : "";
                    
                case COL_COMMENT:
                    return series.getComment() != null ? series.getComment() : "";
                    
                case COL_STATE_TEXT:
                    // HOROS-20240407準拠: BrowserController.m 6459-6465行目
                    // intValueが0の場合はnil、それ以外はstateTextを返す
                    // HOROS-20240407準拠: BrowserController.m 14293行目 - statesArrayから文字列を取得
                    Integer stateText = series.getStateText();
                    if (stateText == null || stateText == 0) {
                        return ""; // HOROS-20240407準拠: nilの場合は空文字列
                    }
                    // HOROS-20240407準拠: BrowserController.m 14293行目
                    // statesArray = [@"empty", @"unread", @"reviewed", @"dictated", @"validated"]
                    String[] statesArray = getStatesArray();
                    if (stateText.intValue() > 0 && stateText.intValue() < statesArray.length) {
                        return statesArray[stateText.intValue()];
                    }
                    return String.valueOf(stateText);
                    
                case COL_LOCALSTRING:
                    // HOROS-20240407準拠: DicomSeries.m 510-530行目
                    // inDatabaseFolderがtrueの場合は"L"を返し、falseの場合は空文字列を返す
                    String localstring = series.localstring();
                    return localstring != null ? localstring : "";
                    
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
    
    /**
     * 履歴パネルに表示されているスタディと同じ患者UIDを持つDBリスト行の背景色を変更するセルレンダラー
     * HOROS-20240407準拠: BrowserController.m 6739-6756行目 - willDisplayCell
     * HOROS-20240407準拠: displaySamePatientWithColorBackground設定を確認し、comparativePatientUIDと比較
     * HOROS-20240407準拠: name列（最初の列）のみで背景色を設定（6739行目）
     */
    private class ComparativePatientCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(
                javax.swing.JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            
            // HOROS-20240407準拠: 列の識別子を取得（ID列とStatus列の処理のため）
            javax.swing.table.TableColumn tableColumnForCheck = table.getColumnModel().getColumn(column);
            Object columnIdentifierForCheck = tableColumnForCheck != null ? tableColumnForCheck.getIdentifier() : null;
            String columnIdForCheck = columnIdentifierForCheck != null ? columnIdentifierForCheck.toString() : null;
            
            // HOROS-20240407準拠: Status列はカスタムレンダラーでJPanelを使用
            if (COL_STATE_TEXT.equals(columnIdForCheck)) {
                return createStatusCellRenderer(table, value, isSelected, hasFocus, row, column);
            }
            
            // HOROS-20240407準拠: ID列はカスタムレンダラーでJPanelを使用（数値として処理されないように）
            if (COL_ID.equals(columnIdForCheck)) {
                return createIdCellRenderer(table, value, isSelected, hasFocus, row, column);
            }
            
            // HOROS-20240407準拠: 値がnullの場合は空文字列に変換
            // これにより、選択行ハイライトでない場合でも値が表示される
            if (value == null) {
                value = "";
            }
            
            javax.swing.JLabel label = (javax.swing.JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            
            // HOROS-20240407準拠: BrowserController.m 6739-6756行目
            // displaySamePatientWithColorBackground設定を確認（デフォルトはtrueと仮定）
            // HOROS-20240407準拠: 選択されていない行で、previousItem（選択された行）の患者UIDと一致する場合のみ背景色を変更
            // HOROS-20240407準拠: BrowserController.m 6743-6753行目
            // if( [[previousItem valueForKey: @"type"] isEqualToString:@"Study]])
            // {
            //     NSString *uid = [item valueForKey: @"patientUID"];
            //     if( previousItem != item && [uid length] > 1 && [uid compare: [previousItem valueForKey: @"patientUID"] options: NSCaseInsensitiveSearch | NSDiacriticInsensitiveSearch | NSWidthInsensitiveSearch] == NSOrderedSame)
            //     {
            //         [cell setDrawsBackground: YES];
            //         [cell setBackgroundColor: [NSColor disabledControlTextColor]];
            //     }
            // }
            // HOROS-20240407準拠: name列（識別子が"name"の列）のみで背景色を設定（6739行目）
            // 列が移動した場合でも正しい列を識別するため、識別子で判定
            // 最初の列（開閉マーク専用列）ではハイライトを適用しない
            if (column == 0) {
                // 最初の列は開閉マーク専用で、ハイライトを適用しない
                return label;
            }
            
            javax.swing.table.TableColumn tableColumn = table.getColumnModel().getColumn(column);
            Object columnIdentifier = tableColumn != null ? tableColumn.getIdentifier() : null;
            // COL_NAME定数を使用して比較（"name"）
            boolean isNameColumn = columnIdentifier != null && (COL_NAME.equals(columnIdentifier.toString()) || "name".equals(columnIdentifier.toString()));
            
            if (isNameColumn && !isSelected && browserController != null) {
                // HOROS-20240407準拠: BrowserController.m 6743行目
                // displaySamePatientWithColorBackground設定を確認（デフォルトはtrueと仮定）
                // TODO: NSUserDefaultsから取得（現在はデフォルトtrueと仮定）
                boolean displaySamePatientWithColorBackground = true;
                
                // HOROS-20240407準拠: BrowserController.m 6743行目
                // [[self window] firstResponder] == outlineView でウィンドウがフォーカスされているか確認
                // Java Swingでは、ウィンドウがアクティブであればフォーカスされているとみなす
                boolean isFocused = true; // フォーカスチェックを緩和（ウィンドウがアクティブであれば表示）
                
                if (displaySamePatientWithColorBackground && isFocused) {
                    // HOROS-20240407準拠: BrowserController.m 6745行目
                    // if( [[previousItem valueForKey: @"type"] isEqualToString:@"Study]])
                    Object previousItem = browserController.getPreviousItem();
                    if (previousItem != null && previousItem instanceof com.jj.dicomviewer.model.DicomStudy) {
                        // 現在の行のアイテムを取得
                        // HOROS-20240407準拠: JXTreeTableではgetPathForRowを使用
                        try {
                            TreePath path = DatabaseOutlineView.this.getPathForRow(row);
                            if (path != null) {
                                Object item = path.getLastPathComponent();
                                
                                // HOROS-20240407準拠: BrowserController.m 6749行目
                                // previousItem != item で選択行自体は除外
                                // オブジェクトの同一性をチェック（equalsではなく==）
                                boolean isSameItem = (previousItem == item) || 
                                                    (previousItem.equals(item));
                                
                                if (!isSameItem) {
                                    // 現在の行のアイテムがStudyタイプか、Studyを含むか確認
                                    com.jj.dicomviewer.model.DicomStudy currentStudy = null;
                                    if (item instanceof com.jj.dicomviewer.model.DicomStudy) {
                                        currentStudy = (com.jj.dicomviewer.model.DicomStudy) item;
                                    } else if (item instanceof com.jj.dicomviewer.model.DicomSeries) {
                                        currentStudy = ((com.jj.dicomviewer.model.DicomSeries) item).getStudy();
                                    } else if (item instanceof com.jj.dicomviewer.model.DicomImage) {
                                        com.jj.dicomviewer.model.DicomSeries series = ((com.jj.dicomviewer.model.DicomImage) item).getSeries();
                                        if (series != null) {
                                            currentStudy = series.getStudy();
                                        }
                                    }
                                    
                                    if (currentStudy != null) {
                                        String patientUID = currentStudy.getPatientUID();
                                        com.jj.dicomviewer.model.DicomStudy previousStudy = (com.jj.dicomviewer.model.DicomStudy) previousItem;
                                        String previousPatientUID = previousStudy.getPatientUID();
                                        
                                        // HOROS-20240407準拠: BrowserController.m 6749行目
                                        // 大文字小文字を区別せず、アクセント記号を無視して比較
                                        // [uid length] > 1 のチェック
                                        if (patientUID != null && patientUID.length() > 1 && 
                                            previousPatientUID != null && previousPatientUID.length() > 1 &&
                                            patientUID.equalsIgnoreCase(previousPatientUID)) {
                                            // HOROS-20240407準拠: BrowserController.m 6751-6752行目
                                            // [cell setDrawsBackground: YES];
                                            // [cell setBackgroundColor: [NSColor disabledControlTextColor]];
                                            label.setOpaque(true);
                                            // HOROS-20240407準拠: 薄いグレーの背景色を設定
                                            // 薄いグレー（RGB: 220, 220, 220）を使用
                                            java.awt.Color highlightColor = new java.awt.Color(220, 220, 220);
                                            label.setBackground(highlightColor);
                                            // HOROS-20240407準拠: alignmentを設定（returnの前に）
                                            if (tableColumn != null && columnIdentifier != null) {
                                                String columnId = columnIdentifier.toString();
                                                if (COL_YEAR_OLD.equals(columnId) ||
                                                    COL_MODALITY.equals(columnId) ||
                                                    COL_ID.equals(columnId) ||
                                                    COL_DATE.equals(columnId) ||
                                                    COL_NO_FILES.equals(columnId) ||
                                                    COL_NO_SERIES.equals(columnId) ||
                                                    COL_DATE_ADDED.equals(columnId) ||
                                                    COL_DATE_OPENED.equals(columnId) ||
                                                    COL_DATE_OF_BIRTH.equals(columnId) ||
                                                    COL_LOCALSTRING.equals(columnId)) {
                                                    label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                                                } else {
                                                    label.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                                                }
                                            }
                                            return label;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // エラーが発生した場合は通常の背景色を使用
                        }
                    }
                }
            }
            
            // HOROS-20240407準拠: MainMenu.xibの各列のalignment設定
            // 列の識別子に応じてalignmentを設定（すべてのreturnの前に設定）
            if (tableColumn != null && columnIdentifier != null) {
                String columnId = columnIdentifier.toString();
                // HOROS-20240407準拠: MainMenu.xib 4300-4644行目
                // center: yearOld, modality, id, date, noFiles, noSeries, dateAdded, dateOpened, dateOfBirth, localstring
                // left: その他すべて
                if (COL_YEAR_OLD.equals(columnId) ||
                    COL_MODALITY.equals(columnId) ||
                    COL_ID.equals(columnId) ||
                    COL_DATE.equals(columnId) ||
                    COL_NO_FILES.equals(columnId) ||
                    COL_NO_SERIES.equals(columnId) ||
                    COL_DATE_ADDED.equals(columnId) ||
                    COL_DATE_OPENED.equals(columnId) ||
                    COL_DATE_OF_BIRTH.equals(columnId) ||
                    COL_LOCALSTRING.equals(columnId)) {
                    label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                } else {
                    label.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                }
                
                // HOROS-20240407準拠: Status列はcreateStatusCellRendererで処理されるため、ここでは処理しない
            }
            
            // HOROS-20240407準拠: 選択行は行全体がハイライト
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(table.getSelectionBackground());
                return label;
            }
            
            // HOROS-20240407準拠: UID一致の場合はPatient Name列のみがハイライト
            // 一致しない場合は通常の背景色（透明）
            label.setOpaque(false);
            label.setBackground(table.getBackground());
            
            return label;
        }
        
        // HOROS-20240407準拠: Status列のカスタムレンダラー（NSPopUpButtonCellの動作を再現）
        private java.awt.Component createStatusCellRenderer(
                javax.swing.JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            
            // HOROS-20240407準拠: JPanelを使用して左側にテキスト、右側にドロップダウンマークを配置
            javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout());
            panel.setOpaque(false);
            
            // テキストラベル（左側）
            javax.swing.JLabel textLabel = new javax.swing.JLabel();
            String text = value != null ? value.toString() : "";
            textLabel.setText(text);
            textLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            textLabel.setOpaque(false);
            
            // ドロップダウンマーク（右側）
            javax.swing.JLabel markLabel = new javax.swing.JLabel("▼");
            markLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
            markLabel.setOpaque(false);
            
            // レイアウト: 左側にテキスト、右側にマーク
            panel.add(textLabel, java.awt.BorderLayout.WEST);
            panel.add(markLabel, java.awt.BorderLayout.EAST);
            
            // 選択状態の背景色を設定
            if (isSelected) {
                panel.setOpaque(true);
                panel.setBackground(table.getSelectionBackground());
                textLabel.setForeground(table.getSelectionForeground());
                markLabel.setForeground(table.getSelectionForeground());
            } else {
                panel.setOpaque(false);
                panel.setBackground(table.getBackground());
                textLabel.setForeground(table.getForeground());
                markLabel.setForeground(table.getForeground());
            }
            
            return panel;
        }
        
        // HOROS-20240407準拠: ID列のカスタムレンダラー（数値として処理されないように）
        private java.awt.Component createIdCellRenderer(
                javax.swing.JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            
            // HOROS-20240407準拠: JLabelを使用して中央揃えで表示
            javax.swing.JLabel label = new javax.swing.JLabel();
            
            // HOROS-20240407準拠: ID列の値を明示的に文字列として設定（数値として処理されないように）
            String idValue = "";
            if (value != null) {
                // 数値の場合は文字列に変換
                if (value instanceof Number) {
                    idValue = String.valueOf(value);
                } else {
                    idValue = value.toString();
                }
            }
            
            // HOROS-20240407準拠: MainMenu.xib 4417行目 - lineBreakMode="clipping"
            // HOROS-20240407準拠: BrowserController.m 6816行目 - NSLineBreakByTruncatingMiddle
            // セルの幅に応じてテキストを省略表示（中央部分を省略）
            if (idValue != null && !idValue.isEmpty()) {
                // セルの幅を取得
                int cellWidth = table.getColumnModel().getColumn(column).getWidth();
                // フォントメトリクスを取得してテキストの幅を計算
                java.awt.FontMetrics fm = label.getFontMetrics(label.getFont());
                int textWidth = fm.stringWidth(idValue);
                
                // テキストがセルの幅を超える場合は省略表示
                if (textWidth > cellWidth - 10) { // 10ピクセルのマージンを確保
                    // 中央部分を省略（NSLineBreakByTruncatingMiddleに相当）
                    // 先頭と末尾を表示し、中央を"..."で置き換え
                    int maxChars = (cellWidth - 10) / fm.charWidth('0'); // 数字1文字の幅で計算
                    if (maxChars > 3) {
                        int startChars = maxChars / 2 - 1;
                        int endChars = maxChars / 2 - 1;
                        if (idValue.length() > startChars + endChars + 3) {
                            idValue = idValue.substring(0, startChars) + "..." + idValue.substring(idValue.length() - endChars);
                        }
                    } else {
                        // セルが非常に狭い場合は先頭のみ表示
                        idValue = idValue.substring(0, Math.min(maxChars, idValue.length()));
                    }
                }
            }
            
            label.setText(idValue);
            label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            label.setOpaque(false);
            
            // 選択状態の背景色を設定
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(table.getSelectionBackground());
                label.setForeground(table.getSelectionForeground());
            } else {
                label.setOpaque(false);
                label.setBackground(table.getBackground());
                label.setForeground(table.getForeground());
            }
            
            return label;
        }
    }
}
