package com.jj.dicomviewer.ui;

import com.jj.dicomviewer.threads.ThreadsManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.JTable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import java.util.function.Consumer;
import java.lang.Thread;

/**
 * Activityパネルのテーブルモデル
 * HOROS-20240407準拠: BrowserController+Activity.mm の完全な写経
 * 
 * BrowserActivityHelperクラス（102-252行目）を完全に写経
 */
public class BrowserActivityHelper extends AbstractTableModel {
    
    // HOROS-20240407準拠: static NSString* const BrowserActivityHelperContext = @"BrowserActivityHelperContext"; (104行目)
    private static final String BrowserActivityHelperContext = "BrowserActivityHelperContext";
    
    // HOROS-20240407準拠: BrowserController* _browser; (53行目)
    private final BrowserController browser;
    
    // HOROS-20240407準拠: NSMutableArray* _cells; (54行目)
    private final List<ThreadCell> cells;
    
    // HOROS-20240407準拠: ThreadsManager.defaultManager.threadsController相当
    private final ThreadsManager threadsManager;
    private Consumer<List<Thread>> arrangedObjectsListener;
    
    /**
     * コンストラクタ
     * HOROS-20240407準拠: - (id)initWithBrowser:(BrowserController*)browser (106-116行目)
     */
    public BrowserActivityHelper(BrowserController browser) {
        // HOROS-20240407準拠: if ((self = [super init])) { (107行目)
        // HOROS-20240407準拠: _browser = browser; // no retaining here (108行目)
        this.browser = browser;
        
        // HOROS-20240407準拠: _cells = [[NSMutableArray alloc] init]; (109行目)
        this.cells = new ArrayList<>();
        this.threadsManager = ThreadsManager.defaultManager();
        
        // HOROS-20240407準拠: [ThreadsManager.defaultManager.threadsController addObserver:self forKeyPath:@"arrangedObjects" options:NSKeyValueObservingOptionNew|NSKeyValueObservingOptionOld|NSKeyValueObservingOptionInitial context:BrowserActivityHelperContext]; (112行目)
        arrangedObjectsListener = this::observeArrangedObjects;
        threadsManager.addArrangedObjectsListener(arrangedObjectsListener);
    }
    
    /**
     * arrangedObjectsの変更を監視
     * HOROS-20240407準拠: - (void)observeValueForKeyPath:(NSString*)keyPath ofObject:(NSArrayController*)object change:(NSDictionary*)change context:(void*)context (138-192行目)
     */
    private void observeArrangedObjects(List<Thread> arrangedObjects) {
        // HOROS-20240407準拠: if (![NSThread isMainThread]) { (139行目)
        if (!SwingUtilities.isEventDispatchThread()) {
            // HOROS-20240407準拠: [self performSelectorOnMainThread:@selector(_observeValueForKeyPathOfObjectChangeContext:) withObject:[NSArray arrayWithObjects: keyPath, object, change, [NSValue valueWithPointer:context], nil] waitUntilDone:NO]; (140行目)
            SwingUtilities.invokeLater(() -> observeArrangedObjects(arrangedObjects));
            return;
        }
        
        // HOROS-20240407準拠: if (context == BrowserActivityHelperContext) { (144行目)
        // HOROS-20240407準拠: @synchronized (ThreadsManager.defaultManager.threadsController) (145行目)
        synchronized (threadsManager) {
            // HOROS-20240407準拠: // we are looking for removed threads (146行目)
            // HOROS-20240407準拠: NSMutableArray* threadsThatHaveCellsToRemove = [[[_cells valueForKey:@"thread"] mutableCopy] autorelease]; (147行目)
            List<Thread> threadsThatHaveCellsToRemove = new ArrayList<>();
            for (ThreadCell cell : cells) {
                threadsThatHaveCellsToRemove.add(cell.getThread());
            }
            
            // HOROS-20240407準拠: [threadsThatHaveCellsToRemove removeObjectsInArray:object.arrangedObjects]; (148行目)
            threadsThatHaveCellsToRemove.removeAll(arrangedObjects);
            
            // HOROS-20240407準拠: NSMutableArray *cellsToRemove = [NSMutableArray array]; (150行目)
            List<ThreadCell> cellsToRemove = new ArrayList<>();
            
            // HOROS-20240407準拠: for (NSThread* thread in threadsThatHaveCellsToRemove) (151行目)
            for (Thread thread : threadsThatHaveCellsToRemove) {
                // HOROS-20240407準拠: ThreadCell* cell = (ThreadCell*)[self cellForThread:thread]; (153行目)
                ThreadCell cell = cellForThread(thread);
                // HOROS-20240407準拠: if (cell) (154行目)
                if (cell != null) {
                    // HOROS-20240407準拠: [cell cleanup]; (156行目)
                    cell.cleanup();
                    // HOROS-20240407準拠: [cell retain]; (157行目)
                    // HOROS-20240407準拠: [cellsToRemove addObject:cell]; (158行目)
                    cellsToRemove.add(cell);
                    
                    // HOROS-20240407準拠: [NSObject cancelPreviousPerformRequestsWithTarget: cell selector: @selector( autorelease) object: nil]; (160行目)
                    // HOROS-20240407準拠: [cell performSelector: @selector( autorelease) withObject: nil afterDelay: 60]; //Yea... I know... not very nice, but avoid a zombie crash, if a thread is cancelled (GUI) AFTER released here... (161行目)
                    // TODO: 60秒後にクリーンアップする処理を実装（Javaでは不要な可能性がある）
                }
            }
            
            // HOROS-20240407準拠: BOOL needToReloadData = NO; (165行目)
            boolean needToReloadData = false;
            
            // HOROS-20240407準拠: if( cellsToRemove.count) (167行目)
            if (!cellsToRemove.isEmpty()) {
                // HOROS-20240407準拠: [_cells removeObjectsInArray: cellsToRemove]; (169行目)
                cells.removeAll(cellsToRemove);
                needToReloadData = true;
            }
            
            // HOROS-20240407準拠: // Check for new added threads (173行目)
            // HOROS-20240407準拠: for (NSThread *thread in object.arrangedObjects) (174行目)
            for (Thread thread : arrangedObjects) {
                // HOROS-20240407準拠: id cell = [self cellForThread: thread]; (176行目)
                ThreadCell cell = cellForThread(thread);
                // HOROS-20240407準拠: if (cell == nil) (177行目)
                if (cell == null) {
                    // HOROS-20240407準拠: [_cells addObject: [[[ThreadCell alloc] initWithThread:thread manager:ThreadsManager.defaultManager view:_browser._activityTableView] autorelease]]; (179行目)
                    // 重複チェック: 同じスレッドに対するセルが既に存在しないことを確認
                    boolean alreadyExists = false;
                    for (ThreadCell existingCell : cells) {
                        if (existingCell.getThread() == thread) {
                            alreadyExists = true;
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        cells.add(new ThreadCell(thread, threadsManager, browser.getActivityTableView()));
                        needToReloadData = true;
                    }
                }
            }
            
            // HOROS-20240407準拠: if( needToReloadData) (184行目)
            if (needToReloadData) {
                // HOROS-20240407準拠: [_browser._activityTableView reloadData]; (185行目)
                SwingUtilities.invokeLater(() -> {
                    fireTableDataChanged();
                    if (browser.getActivityTableView() != null) {
                        browser.getActivityTableView().repaint();
                    }
                });
            }
            
            // HOROS-20240407準拠: return; (187行目)
            return;
        }
    }
    
    /**
     * スレッドのセルを取得
     * HOROS-20240407準拠: - (NSCell*)cellForThread:(NSThread*)thread (124-132行目)
     */
    private ThreadCell cellForThread(Thread thread) {
        // HOROS-20240407準拠: @synchronized (ThreadsManager.defaultManager.threadsController) (126行目)
        synchronized (threadsManager) {
            // HOROS-20240407準拠: for (ThreadCell* cell in _cells) (127行目)
            for (ThreadCell cell : cells) {
                // HOROS-20240407準拠: if (cell.thread == thread) (128行目)
                if (cell.getThread() == thread) {
                    // HOROS-20240407準拠: return cell; (129行目)
                    return cell;
                }
            }
        }
        // HOROS-20240407準拠: return nil; (131行目)
        return null;
    }
    
    /**
     * テーブルのセルを取得
     * HOROS-20240407準拠: - (NSCell*)tableView:(NSTableView*)tableView dataCellForTableColumn:(NSTableColumn*)tableColumn row:(NSInteger)row (194-207行目)
     */
    public ThreadCell getDataCellForTableColumn(int row) {
        // HOROS-20240407準拠: @try (195行目)
        try {
            // HOROS-20240407準拠: @synchronized (ThreadsManager.defaultManager.threadsController) (197行目)
            synchronized (threadsManager) {
                // HOROS-20240407準拠: id cell = [[_cells objectAtIndex: row] retain]; (199行目)
                // HOROS-20240407準拠: return [cell autorelease]; (200行目)
                if (row >= 0 && row < cells.size()) {
                    return cells.get(row);
                }
            }
        } catch (Exception e) {
            // HOROS-20240407準拠: @catch (...) { (203行目)
        }
        // HOROS-20240407準拠: return NULL; (206行目)
        return null;
    }
    
    @Override
    public int getRowCount() {
        // HOROS-20240407準拠: - (NSInteger)numberOfRowsInTableView:(NSTableView *)aTableView (209-216行目)
        // HOROS-20240407準拠: @synchronized (ThreadsManager.defaultManager.threadsController) (211行目)
        synchronized (threadsManager) {
            // HOROS-20240407準拠: return _cells.count; (212行目)
            return cells.size();
        }
    }
    
    @Override
    public int getColumnCount() {
        return 1; // Activity列のみ
    }
    
    @Override
    public String getColumnName(int column) {
        return "Activity";
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        synchronized (threadsManager) {
            if (rowIndex >= 0 && rowIndex < cells.size()) {
                return cells.get(rowIndex);
            }
        }
        return null;
    }
    
    /**
     * セルを表示する前に呼ばれる
     * HOROS-20240407準拠: - (void)tableView:(NSTableView*)tableView willDisplayCell:(ThreadCell*)cell forTableColumn:(NSTableColumn*)tableColumn row:(NSInteger)row (218-250行目)
     */
    public void willDisplayCell(JTable tableView, ThreadCell cell, int row) {
        // HOROS-20240407準拠: NSRect frame; (219行目)
        // HOROS-20240407準拠: if (tableColumn) frame = [tableView frameOfCellAtColumn:[tableView.tableColumns indexOfObject:tableColumn] row:row]; (220行目)
        // HOROS-20240407準拠: else frame = [tableView rectOfRow:row]; (221行目)
        java.awt.Rectangle frame = tableView.getCellRect(row, 0, true);
        
        // HOROS-20240407準拠: @synchronized (ThreadsManager.defaultManager.threadsController) (223行目)
        synchronized (threadsManager) {
            // HOROS-20240407準拠: if( [_cells containsObject: cell]) (225行目)
            if (cells.contains(cell)) {
                // HOROS-20240407準拠: // cancel (227行目)
                // HOROS-20240407準拠: if (![cell.cancelButton superview]) (228行目)
                if (cell.getCancelButton().getParent() == null) {
                    // HOROS-20240407準拠: [tableView addSubview:cell.cancelButton]; (229行目)
                    tableView.add(cell.getCancelButton());
                    cell.getCancelButton().setOpaque(false);
                }
                
                // HOROS-20240407準拠: NSRect cancelFrame = NSMakeRect(frame.origin.x+frame.size.width-15-5, frame.origin.y+5, 15, 15); (231行目)
                java.awt.Rectangle cancelFrame = new java.awt.Rectangle(
                    frame.x + frame.width - 15 - 5, 
                    frame.y + 5, 
                    15, 
                    15
                );
                // HOROS-20240407準拠: if (!NSEqualRects(cell.cancelButton.frame, cancelFrame)) (232行目)
                if (!cell.getCancelButton().getBounds().equals(cancelFrame)) {
                    // HOROS-20240407準拠: [cell.cancelButton setFrame:cancelFrame]; (233行目)
                    cell.getCancelButton().setBounds(cancelFrame);
                }
                
                // HOROS-20240407準拠: // progress (235行目)
                // HOROS-20240407準拠: if (![cell.progressIndicator superview]) { (236行目)
                if (cell.getProgressIndicator().getParent() == null) {
                    // HOROS-20240407準拠: [tableView addSubview:cell.progressIndicator]; (237行目)
                    tableView.add(cell.getProgressIndicator());
                    cell.getProgressIndicator().setOpaque(false);
                    cell.getProgressIndicator().setBorderPainted(false);
                    // ユーザー要求: プログレスバーのラベル表示はThreadCell.updateProgressIndicatorで管理
                    // HOROS-20240407準拠: //		[self.progressIndicator startAnimation:self]; (238行目)
                    // 注意: startAnimationはThreadCell.drawInteriorWithFrameで呼ばれる
                }
                
                // HOROS-20240407準拠: NSRect progressFrame; (241行目)
                // HOROS-20240407準拠: if ([AppController hasMacOSXLion]) (242行目)
                // HOROS-20240407準拠: progressFrame = NSMakeRect(frame.origin.x+3, frame.origin.y+27, frame.size.width-6, frame.size.height-32); (243行目)
                // HOROS-20240407準拠: else progressFrame = NSMakeRect(frame.origin.x+1, frame.origin.y+26, frame.size.width-2, frame.size.height-28); (244行目)
                // Java Swingでは常にelseの値を使用
                java.awt.Rectangle progressFrame = new java.awt.Rectangle(
                    frame.x + 1, 
                    frame.y + 26, 
                    frame.width - 2, 
                    frame.height - 28
                );
                
                // HOROS-20240407準拠: if (!NSEqualRects(cell.progressIndicator.frame, progressFrame)) (246行目)
                if (!cell.getProgressIndicator().getBounds().equals(progressFrame)) {
                    // HOROS-20240407準拠: [cell.progressIndicator setFrame:progressFrame]; (247行目)
                    cell.getProgressIndicator().setBounds(progressFrame);
                }
                
                // HOROS-20240407準拠: observeValueForKeyPathでNSThreadProgressKeyが変更されたときに更新 (ThreadCell.mm 190-198行目)
                // JavaではKVOがないため、ThreadCellが定期的に進捗をチェックして更新する
                // willDisplayCellでは、progressIndicatorの位置設定のみ行う
            }
        }
    }
    
    /**
     * クリーンアップ
     * HOROS-20240407準拠: - (void)dealloc (118-122行目)
     */
    public void cleanup() {
        // HOROS-20240407準拠: [_cells release]; (119行目)
        // HOROS-20240407準拠: [ThreadsManager.defaultManager.threadsController removeObserver:self forKeyPath: @"arrangedObjects"]; (120行目)
        if (arrangedObjectsListener != null) {
            threadsManager.removeArrangedObjectsListener(arrangedObjectsListener);
            arrangedObjectsListener = null;
        }
    }
}
