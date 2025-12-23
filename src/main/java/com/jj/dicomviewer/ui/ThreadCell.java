package com.jj.dicomviewer.ui;

import com.jj.dicomviewer.threads.ThreadsManager;
import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * スレッドセル
 * HOROS-20240407準拠: ThreadCell.mm の完全な写経
 */
public class ThreadCell {
    
    // HOROS-20240407準拠: NSProgressIndicator* _progressIndicator;
    private JProgressBar progressIndicator;
    // HOROS-20240407準拠: ThreadsManager* _manager;
    private final ThreadsManager manager;
    // HOROS-20240407準拠: NSButton* _cancelButton;
    private JButton cancelButton;
    // HOROS-20240407準拠: NSThread* _thread;
    private Thread thread;
    // HOROS-20240407準拠: id _retainedThreadDictionary;
    private Map<String, Object> retainedThreadDictionary;
    // HOROS-20240407準拠: NSTableView* _view;
    private final JTable view;
    
    // HOROS-20240407準拠: CGFloat _lastDisplayedProgress;
    private double lastDisplayedProgress = -1;
    // HOROS-20240407準拠: BOOL KVOObserving;
    private boolean kvoObserving = false;
    // HOROS-20240407準拠: JavaではKVOがないため、定期的に進捗をチェックするTimer
    private javax.swing.Timer progressUpdateTimer;
    
    /**
     * コンストラクタ
     * HOROS-20240407準拠: - (id)initWithThread:(NSThread*)thread manager:(ThreadsManager*)manager view:(NSTableView*)view (55-79行目)
     */
    public ThreadCell(Thread thread, ThreadsManager manager, JTable view) {
        // HOROS-20240407準拠: _view = view;
        this.view = view;
        // HOROS-20240407準拠: _manager = manager;
        this.manager = manager;
        
        // HOROS-20240407準拠: _progressIndicator = [[NSProgressIndicator alloc] initWithFrame:NSZeroRect];
        progressIndicator = new JProgressBar();
        // HOROS-20240407準拠: [_progressIndicator setUsesThreadedAnimation:YES];
        // HOROS-20240407準拠: [_progressIndicator setMinValue:0];
        progressIndicator.setMinimum(0);
        // HOROS-20240407準拠: [_progressIndicator setMaxValue:1];
        progressIndicator.setMaximum(100);
        // HOROS-20240407準拠: 初期状態でindeterminateモードに設定（アニメーション表示のため）
        progressIndicator.setIndeterminate(true);
        progressIndicator.setStringPainted(false); // 文字列を表示しない
        progressIndicator.setOpaque(false);
        progressIndicator.setBorderPainted(false);
        
        // HOROS-20240407準拠: _cancelButton = [[NSButton alloc] initWithFrame:NSZeroRect];
        cancelButton = new JButton();
        // HOROS-20240407準拠: [_cancelButton setImage:[NSImage imageNamed:@"Activity_Stop"]];
        // HOROS-20240407準拠: [_cancelButton setAlternateImage:[NSImage imageNamed:@"Activity_StopPressed"]];
        // HOROS-20240407準拠: [_cancelButton setBordered:NO];
        cancelButton.setBorderPainted(false);
        // HOROS-20240407準拠: [_cancelButton setButtonType:NSMomentaryChangeButton];
        // HOROS-20240407準拠: _cancelButton.target = self;
        // HOROS-20240407準拠: _cancelButton.action = @selector(cancelThreadAction:);
        cancelButton.addActionListener(e -> cancelThreadAction());
        cancelButton.setText("×");
        cancelButton.setPreferredSize(new Dimension(15, 15));
        cancelButton.setOpaque(false);
        
        // HOROS-20240407準拠: _lastDisplayedProgress = -1;
        lastDisplayedProgress = -1;
        
        // HOROS-20240407準拠: self.thread = thread;
        setThread(thread);
    }
    
    /**
     * スレッドを設定
     * HOROS-20240407準拠: - (void)setThread:(NSThread*)thread (123-163行目)
     */
    public void setThread(Thread thread) {
        // HOROS-20240407準拠: @synchronized( _thread)
        synchronized (this) {
            try {
            // HOROS-20240407準拠: if( KVOObserving)
            if (kvoObserving) {
                // HOROS-20240407準拠: [_thread removeObserver:self forKeyPath:NSThreadSupportsCancelKey];
                // HOROS-20240407準拠: [_thread removeObserver:self forKeyPath:NSThreadProgressKey];
                // HOROS-20240407準拠: [_thread removeObserver:self forKeyPath:NSThreadStatusKey];
                // HOROS-20240407準拠: [_thread removeObserver:self forKeyPath:NSThreadIsCancelledKey];
                // HOROS-20240407準拠: KVOObserving = NO;
                kvoObserving = false;
                
                // HOROS-20240407準拠: Timerを停止
                stopProgressUpdateTimer();
            }
            } catch (Exception e) {
                // HOROS-20240407準拠: @catch ( NSException *e)
                e.printStackTrace();
            }
            
            // HOROS-20240407準拠: [_thread autorelease];
            // HOROS-20240407準拠: [_retainedThreadDictionary autorelease];
            this.thread = thread;
            retainedThreadDictionary = null;
            
            // HOROS-20240407準拠: @synchronized( _thread)
            if (thread != null) {
                synchronized (thread) {
                    // HOROS-20240407準拠: _retainedThreadDictionary = [_thread.threadDictionary retain];
                    // TODO: スレッドの辞書を取得
                    
                    // HOROS-20240407準拠: if( _retainedThreadDictionary)
                    if (true) { // 仮実装
                        // HOROS-20240407準拠: [_thread addObserver:self forKeyPath:NSThreadIsCancelledKey options:NSKeyValueObservingOptionInitial context:NULL];
                        // HOROS-20240407準拠: [_thread addObserver:self forKeyPath:NSThreadStatusKey options:NSKeyValueObservingOptionInitial context:NULL];
                        // HOROS-20240407準拠: [_thread addObserver:self forKeyPath:NSThreadProgressKey options:NSKeyValueObservingOptionInitial context:NULL];
                        // HOROS-20240407準拠: [_thread addObserver:self forKeyPath:NSThreadSupportsCancelKey options:NSKeyValueObservingOptionInitial context:NULL];
                        // HOROS-20240407準拠: KVOObserving = YES;
                        kvoObserving = true;
                        
                        // HOROS-20240407準拠: JavaではKVOがないため、定期的に進捗をチェックして更新
                        // observeValueForKeyPath:NSThreadProgressKey相当の処理
                        startProgressUpdateTimer();
                    }
                }
            }
        }
    }
    
    /**
     * スレッドを取得
     * HOROS-20240407準拠: @property(nonatomic, retain) NSThread* thread;
     */
    public Thread getThread() {
        return thread;
    }
    
    /**
     * プログレスインジケーターを取得
     * HOROS-20240407準拠: @property(retain) NSProgressIndicator* progressIndicator;
     */
    public JProgressBar getProgressIndicator() {
        return progressIndicator;
    }
    
    /**
     * キャンセルボタンを取得
     * HOROS-20240407準拠: @property(retain) NSButton* cancelButton;
     */
    public JButton getCancelButton() {
        return cancelButton;
    }
    
    /**
     * マネージャーを取得
     * HOROS-20240407準拠: @property(assign, readonly) ThreadsManager* manager;
     */
    public ThreadsManager getManager() {
        return manager;
    }
    
    /**
     * ビューを取得
     * HOROS-20240407準拠: @property(assign, readonly) NSTableView* view;
     */
    public JTable getView() {
        return view;
    }
    
    /**
     * クリーンアップ
     * HOROS-20240407準拠: - (void)cleanup (81-111行目)
     */
    public void cleanup() {
        // HOROS-20240407準拠: if( _progressIndicator == nil && _cancelButton == nil && KVOObserving == NO)
        if (progressIndicator == null && cancelButton == null && !kvoObserving) {
            return;
        }
        
        // HOROS-20240407準拠: if( [NSThread isMainThread] == NO)
        if (!SwingUtilities.isEventDispatchThread()) {
            System.err.println("ThreadCell.cleanup should be on MAIN thread");
        }
        
        // HOROS-20240407準拠: @synchronized( _thread)
        synchronized (this) {
            // HOROS-20240407準拠: [_progressIndicator removeFromSuperview];
            if (progressIndicator != null && progressIndicator.getParent() != null) {
                progressIndicator.getParent().remove(progressIndicator);
            }
            // HOROS-20240407準拠: [_progressIndicator autorelease]; _progressIndicator = nil;
            progressIndicator = null;
            
            // HOROS-20240407準拠: _cancelButton.target = nil;
            // HOROS-20240407準拠: _cancelButton.action = nil;
            if (cancelButton != null) {
                for (java.awt.event.ActionListener listener : cancelButton.getActionListeners()) {
                    cancelButton.removeActionListener(listener);
                }
            }
            // HOROS-20240407準拠: [_cancelButton removeFromSuperview];
            if (cancelButton != null && cancelButton.getParent() != null) {
                cancelButton.getParent().remove(cancelButton);
            }
            // HOROS-20240407準拠: [_cancelButton autorelease]; _cancelButton = nil;
            cancelButton = null;
            
            // HOROS-20240407準拠: [self.view reloadData];
            // HOROS-20240407準拠: [self.view setNeedsDisplay: YES];
            if (view != null) {
                SwingUtilities.invokeLater(() -> {
                    if (view.getModel() instanceof javax.swing.table.AbstractTableModel) {
                        ((javax.swing.table.AbstractTableModel) view.getModel()).fireTableDataChanged();
                    }
                    view.repaint();
                });
            }
            
            // HOROS-20240407準拠: if( KVOObserving)
            if (kvoObserving) {
                // HOROS-20240407準拠: [_thread removeObserver:self forKeyPath:NSThreadSupportsCancelKey];
                // HOROS-20240407準拠: [_thread removeObserver:self forKeyPath:NSThreadProgressKey];
                // HOROS-20240407準拠: [_thread removeObserver:self forKeyPath:NSThreadStatusKey];
                // HOROS-20240407準拠: [_thread removeObserver:self forKeyPath:NSThreadIsCancelledKey];
                // HOROS-20240407準拠: KVOObserving = NO;
                kvoObserving = false;
                
                // HOROS-20240407準拠: Timerを停止
                stopProgressUpdateTimer();
            }
        }
    }
    
    /**
     * 進捗更新タイマーを開始
     * HOROS-20240407準拠: observeValueForKeyPath:NSThreadProgressKey相当の処理を定期的に実行
     */
    private void startProgressUpdateTimer() {
        stopProgressUpdateTimer(); // 既存のTimerを停止
        
        // HOROS-20240407準拠: observeValueForKeyPathが呼ばれるタイミングをシミュレート
        // 0.1秒ごとに進捗をチェックして更新
        progressUpdateTimer = new javax.swing.Timer(100, e -> {
            if (thread != null && thread.isAlive() && progressIndicator != null) {
                updateProgressIndicator();
            } else {
                stopProgressUpdateTimer();
            }
        });
        progressUpdateTimer.start();
    }
    
    /**
     * 進捗更新タイマーを停止
     */
    private void stopProgressUpdateTimer() {
        if (progressUpdateTimer != null) {
            progressUpdateTimer.stop();
            progressUpdateTimer = null;
        }
    }
    
    /**
     * 進捗インジケーターを更新
     * HOROS-20240407準拠: observeValueForKeyPath:NSThreadProgressKey (190-198行目)
     */
    private void updateProgressIndicator() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateProgressIndicator);
            return;
        }
        
        if (thread == null || !thread.isAlive() || progressIndicator == null) {
            return;
        }
        
        // HOROS-20240407準拠: [self.progressIndicator setDoubleValue:self.thread.subthreadsAwareProgress]; (191行目)
        // HOROS-20240407準拠: [self.progressIndicator setIndeterminate: self.thread.progress < 0]; (192行目)
        double threadProgress = manager.getThreadProgress(thread);
        // HOROS-20240407準拠: プログレスバーには文字列を表示しない
        progressIndicator.setStringPainted(false);
        if (threadProgress < 0) {
            // HOROS-20240407準拠: progress < 0の場合はindeterminateモード（アニメーション）
            if (!progressIndicator.isIndeterminate()) {
                progressIndicator.setIndeterminate(true);
            }
        } else {
            // HOROS-20240407準拠: progress >= 0の場合はdeterminateモード（進捗バー）
            if (progressIndicator.isIndeterminate()) {
                progressIndicator.setIndeterminate(false);
            }
            // HOROS-20240407準拠: setDoubleValueは0.0-1.0の範囲、JProgressBarは0-100の範囲
            progressIndicator.setValue((int)(Math.max(0, Math.min(1, threadProgress)) * 100));
        }
        
        // HOROS-20240407準拠: if (fabs(_lastDisplayedProgress-obj.progress) > 1.0/self.progressIndicator.frame.size.width) { (194行目)
        // HOROS-20240407準拠: _lastDisplayedProgress = obj.progress; (195行目)
        // HOROS-20240407準拠: [self.progressIndicator setNeedsDisplay: YES]; (196行目)
        if (progressIndicator.getWidth() > 0 && Math.abs(lastDisplayedProgress - threadProgress) > 1.0 / progressIndicator.getWidth()) {
            lastDisplayedProgress = threadProgress;
            progressIndicator.repaint();
        }
        
        // HOROS-20240407準拠: ステータスの変更を検知してviewを更新 (187-189行目)
        // observeValueForKeyPath:NSThreadStatusKey相当
        // view.repaint()はwillDisplayCellで行われるため、ここでは行わない
    }
    
    /**
     * キャンセルアクション
     * HOROS-20240407準拠: - (void)cancelThreadAction:(id)source (209-219行目)
     */
    private void cancelThreadAction() {
        // HOROS-20240407準拠: @synchronized( _thread)
        synchronized (this) {
            // HOROS-20240407準拠: if( [self.thread isFinished] == NO)
            if (thread != null && thread.isAlive()) {
                // HOROS-20240407準拠: _thread.status = NSLocalizedString( @"Cancelling...", nil);
                // HOROS-20240407準拠: [_thread setIsCancelled:YES];
                manager.setThreadStatus(thread, "Cancelling...");
                manager.setIsCancelled(thread, true);
            }
        }
    }
    
    /**
     * セルの内部を描画
     * HOROS-20240407準拠: - (void)drawInteriorWithFrame:(NSRect)frame inView:(NSView*)view (222-264行目)
     */
    public void drawInteriorWithFrame(Rectangle frame, Graphics g) {
        // HOROS-20240407準拠: @synchronized( _thread)
        synchronized (this) {
            // HOROS-20240407準拠: if ([_thread isFinished])
            if (thread == null || !thread.isAlive()) {
                return;
            }
        }
        
        // HOROS-20240407準拠: NSMutableParagraphStyle* paragraphStyle = [[[NSParagraphStyle defaultParagraphStyle] mutableCopy] autorelease];
        // HOROS-20240407準拠: [paragraphStyle setLineBreakMode:NSLineBreakByTruncatingTail];
        // HOROS-20240407準拠: NSMutableDictionary* textAttributes = [NSMutableDictionary dictionaryWithObjectsAndKeys: [self textColor], NSForegroundColorAttributeName, [NSFont labelFontOfSize:[[BrowserController currentBrowser] fontSize: @"threadNameSize"]], NSFontAttributeName, paragraphStyle, NSParagraphStyleAttributeName, NULL];
        
        // HOROS-20240407準拠: [NSGraphicsContext saveGraphicsState];
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            // HOROS-20240407準拠: NSString* tempName;
            // HOROS-20240407準拠: NSString* tempStatus;
            String tempName;
            String tempStatus;
            // HOROS-20240407準拠: @synchronized (_thread) {
            synchronized (this) {
                // HOROS-20240407準拠: tempName = [[_thread.name retain] autorelease];
                tempName = thread != null ? thread.getName() : null;
                // HOROS-20240407準拠: tempStatus = [[_thread.status retain] autorelease];
                tempStatus = getThreadStatus();
            }
            
            // HOROS-20240407準拠: NSRect nameFrame = NSMakeRect(frame.origin.x+3, frame.origin.y-1, frame.size.width-23, frame.size.height);
            // 上段：名前（"Indexing files..." や "Adding files..."）
            Rectangle nameFrame = new Rectangle(3, 0, frame.width - 23, frame.height);
            // HOROS-20240407準拠: if (!tempName) tempName = NSLocalizedString(@"Unspecified Task", nil);
            if (tempName == null || tempName.isEmpty()) {
                tempName = "Unspecified Task";
            }
            
            // HOROS-20240407準拠: [tempName drawWithRect:nameFrame options:NSStringDrawingUsesLineFragmentOrigin+NSStringDrawingTruncatesLastVisibleLine attributes:textAttributes];
            g2d.setColor(UIManager.getColor("Label.foreground"));
            // HOROS-20240407準拠: BrowserController.m 446-447行目: threadNameSize = 13
            Font nameFont = UIManager.getFont("Label.font").deriveFont(13f);
            g2d.setFont(nameFont);
            FontMetrics fm = g2d.getFontMetrics();
            String truncatedName = truncateString(tempName, nameFrame.width, fm);
            // 上段に名前を描画
            g2d.drawString(truncatedName, nameFrame.x, nameFrame.y + fm.getAscent() + 1);
            
            // HOROS-20240407準拠: NSRect statusFrame = [self statusFrame];
            // 下段：ステータス（"Waiting for subtasks to complete..." や "Scanning 11603 files"）
            Rectangle statusFrame = getStatusFrame(frame);
            // HOROS-20240407準拠: [textAttributes setObject:[NSFont labelFontOfSize: [[BrowserController currentBrowser] fontSize: @"threadNameStatus"]] forKey:NSFontAttributeName];
            // HOROS-20240407準拠: BrowserController.m 449-450行目: threadNameStatus = 11
            // HOROS-20240407準拠: if (!tempStatus) tempStatus = @"";
            if (tempStatus == null) {
                tempStatus = "";
            }
            // HOROS-20240407準拠: [tempStatus drawWithRect:statusFrame options:NSStringDrawingUsesLineFragmentOrigin attributes:textAttributes];
            if (!tempStatus.isEmpty()) {
                Font statusFont = UIManager.getFont("Label.font").deriveFont(11f);
                g2d.setFont(statusFont);
                FontMetrics statusFm = g2d.getFontMetrics();
                // 下段にステータスを描画
                g2d.drawString(tempStatus, statusFrame.x, statusFrame.y + statusFm.getAscent());
            }
            
            // HOROS-20240407準拠: プログレスバーの位置設定はwillDisplayCellで行われるため、ここでは行わない
            // HOROS-20240407準拠: drawInteriorWithFrameでは、テキストの描画のみを行う
            // プログレスバーに文字列を表示しない（setStringPainted(false)）
        } finally {
            // HOROS-20240407準拠: [NSGraphicsContext restoreGraphicsState];
            g2d.dispose();
        }
    }
    
    /**
     * セルを描画
     * HOROS-20240407準拠: - (void)drawWithFrame:(NSRect)frame inView:(NSView*)view (266-284行目)
     */
    public void drawWithFrame(Rectangle frame, Graphics g) {
        // HOROS-20240407準拠: [self drawInteriorWithFrame:frame inView:view];
        drawInteriorWithFrame(frame, g);
        
        // HOROS-20240407準拠: [NSGraphicsContext saveGraphicsState];
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            // HOROS-20240407準拠: [[[NSColor grayColor] colorWithAlphaComponent:0.5] set];
            g2d.setColor(new Color(128, 128, 128, 128));
            // HOROS-20240407準拠: [NSBezierPath strokeLineFromPoint:frame.origin+NSMakeSize(-2, frame.size.height) toPoint:frame.origin+frame.size+NSMakeSize(2,0)];
            g2d.drawLine(frame.x - 2, frame.y + frame.height, frame.x + frame.width + 2, frame.y + frame.height);
        } finally {
            // HOROS-20240407準拠: [NSGraphicsContext restoreGraphicsState];
            g2d.dispose();
        }
    }
    
    
    /**
     * 文字列を切り詰める
     */
    private String truncateString(String text, int maxWidth, FontMetrics fm) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        for (int i = text.length() - 1; i >= 0; i--) {
            String truncated = text.substring(0, i) + ellipsis;
            if (fm.stringWidth(truncated) <= maxWidth) {
                return truncated;
            }
        }
        return ellipsis;
    }
    
    /**
     * ステータスフレームを取得
     * HOROS-20240407準拠: - (NSRect)statusFrame (286-291行目)
     * 
     * 注意: HOROS-20240407ではview座標系を使っているが、drawInteriorWithFrameは相対座標で描画するため、
     * ここでは相対座標で計算する
     */
    private Rectangle getStatusFrame(Rectangle frame) {
        // HOROS-20240407準拠: NSRect frame = [self.view rectOfRow:[self.manager.threads indexOfObject:self.thread]];
        // HOROS-20240407準拠: return NSMakeRect(frame.origin.x+3, frame.origin.y + [[BrowserController currentBrowser] fontSize: @"threadCellLineSpace"], frame.size.width-22, frame.size.height- (frame.origin.y + [[BrowserController currentBrowser] fontSize: @"threadCellLineSpace"]));
        // HOROS-20240407準拠: BrowserController.m 413-414行目: threadCellLineSpace = 13 (Retina display)
        int lineSpace = 13; // HOROS-20240407準拠: threadCellLineSpace
        // HOROS-20240407準拠: drawInteriorWithFrameのframeは相対座標なので、frame.origin.yは0
        // frame.size.height - lineSpace は、ステータステキストの描画領域の高さ（残りの高さ）
        return new Rectangle(
            3,  // frame.x + 3（相対座標なので3）
            lineSpace,  // frame.y + lineSpace（相対座標なのでlineSpace）
            frame.width - 22,
            frame.height - lineSpace
        );
    }
    
    /**
     * スレッドのステータスを取得
     * HOROS-20240407準拠: NSThread+N2.mm 278-296行目: statusはthreadDictionaryから取得
     */
    private String getThreadStatus() {
        if (thread != null) {
            return manager.getThreadStatus(thread);
        }
        return null;
    }
}
