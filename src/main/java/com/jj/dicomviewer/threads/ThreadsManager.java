package com.jj.dicomviewer.threads;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * スレッド管理クラス
 * HOROS-20240407準拠: ThreadsManager
 * 
 * バックグラウンドで実行されるスレッドを管理し、Activityパネルに表示する
 * HOROS-20240407準拠: NSArrayController相当の機能を提供
 */
public class ThreadsManager {
    
    private static ThreadsManager defaultManager;
    
    // HOROS-20240407準拠: _threadsController = [[NSArrayController alloc] init];
    // Java Swingでは、Listとリスナーで実装
    private final List<Thread> threads;
    private final List<Consumer<List<Thread>>> listeners;
    private Timer cleanupTimer;
    
    // HOROS-20240407準拠: スレッド情報を管理（NSThread+N2のthreadDictionary相当）
    // HOROS-20240407準拠: NSThread+N2.mm: threadDictionaryにprogress、statusなどを保存
    private final Map<Thread, ThreadInfo> threadInfoMap;
    
    /**
     * デフォルトマネージャーを取得
     * HOROS-20240407準拠: + (ThreadsManager*)defaultManager
     */
    public static synchronized ThreadsManager defaultManager() {
        if (defaultManager == null) {
            defaultManager = new ThreadsManager();
        }
        return defaultManager;
    }
    
    /**
     * コンストラクタ
     * HOROS-20240407準拠: - (id)init
     */
    private ThreadsManager() {
        // HOROS-20240407準拠: _threadsController = [[NSArrayController alloc] init];
        threads = new CopyOnWriteArrayList<>();
        listeners = new CopyOnWriteArrayList<>();
        threadInfoMap = new ConcurrentHashMap<>();
        
        // HOROS-20240407準拠: クリーンアップタイマー（0.1秒間隔）
        // HOROS-20240407準拠: _timer = [[NSTimer scheduledTimerWithTimeInterval:0.1 target:self selector:@selector(cleanupFinishedThreads:) userInfo:nil repeats:YES] retain];
        cleanupTimer = new Timer(100, e -> cleanupFinishedThreads());
        cleanupTimer.start();
    }
    
    /**
     * 終了したスレッドをクリーンアップ
     * HOROS-20240407準拠: - (void)cleanupFinishedThreads:(NSTimer*)timer
     */
    private void cleanupFinishedThreads() {
        synchronized (threads) {
            // HOROS-20240407準拠: for (NSThread* thread in [[_threadsController.content copy] autorelease])
            List<Thread> toRemove = new ArrayList<>();
            for (Thread thread : new ArrayList<>(threads)) {
                // HOROS-20240407準拠: if (thread.isFinished)
                if (!thread.isAlive()) {
                    // HOROS-20240407準拠: [self subRemoveThread:thread];
                    subRemoveThread(thread);
                }
            }
        }
    }
    
    /**
     * スレッドリストを取得（arrangedObjects相当）
     * HOROS-20240407準拠: - (NSArray*)threads
     */
    public List<Thread> getThreads() {
        synchronized (threads) {
            // HOROS-20240407準拠: return _threadsController.arrangedObjects;
            return new ArrayList<>(threads);
        }
    }
    
    /**
     * スレッド数を取得
     * HOROS-20240407準拠: - (NSUInteger)threadsCount
     */
    public int getThreadsCount() {
        synchronized (threads) {
            // HOROS-20240407準拠: return [_threadsController.arrangedObjects count];
            return threads.size();
        }
    }
    
    /**
     * インデックスでスレッドを取得
     * HOROS-20240407準拠: - (NSThread*)threadAtIndex:(NSUInteger)index
     */
    public Thread getThreadAtIndex(int index) {
        synchronized (threads) {
            // HOROS-20240407準拠: return [_threadsController.arrangedObjects objectAtIndex:index];
            if (index >= 0 && index < threads.size()) {
                return threads.get(index);
            }
            return null;
        }
    }
    
    /**
     * スレッドを追加（内部処理）
     * HOROS-20240407準拠: - (void)subAddThread:(NSThread*)thread
     */
    private void subAddThread(Thread thread) {
        synchronized (threads) {
            synchronized (thread) {
                // HOROS-20240407準拠: if (![NSThread isMainThread]) (116行目)
                if (!SwingUtilities.isEventDispatchThread()) {
                    // 警告ログは条件付きで出力（必要に応じて有効化）
                    // System.err.println("***** ThreadsManager.subAddThread should be on MAIN thread");
                }
                
                // HOROS-20240407準拠: if ([_threadsController.arrangedObjects containsObject:thread] || [thread isFinished]) (119行目)
                // HOROS-20240407準拠: Javaでは、thread.getState() == Thread.State.TERMINATEDの場合、スレッドは終了している
                // HOROS-20240407準拠: NEW状態のスレッドは、isAlive()がfalseだが、終了したわけではない
                // 重複登録を防ぐため、containsチェックを強化
                if (threads.contains(thread)) {
                    // 既に登録されているスレッドは追加しない（HOROS-20240407準拠）
                    // デバッグログは条件付きで出力（必要に応じて有効化）
                    // System.err.println("ThreadsManager.subAddThread: thread already registered: " + thread.getName() + " (id=" + thread.getId() + ")");
                    return;
                }
                if (thread.getState() == Thread.State.TERMINATED) {
                    // 終了したスレッドは追加しない（HOROS-20240407準拠）
                    return;
                }
                
                // HOROS-20240407準拠: else (123行目)
                // HOROS-20240407準拠: if (![thread isMainThread]/* && ![thread isExecuting]*/) (125行目)
                if (thread != Thread.currentThread()) {
                    // HOROS-20240407準拠: BOOL isExe = [thread isExecuting], isDone = [thread isFinished]; (127行目)
                    // HOROS-20240407準拠: NSThreadのisExecutingは、スレッドが実際に実行中かどうかを示す
                    // HOROS-20240407準拠: NSThreadのisFinishedは、スレッドが終了したかどうかを示す
                    // HOROS-20240407準拠: Javaでは、thread.isAlive()がtrueの場合、スレッドは実行中または実行可能
                    // HOROS-20240407準拠: ExecutorServiceのワーカースレッドは、タスクが完了してもisAlive()がtrueのまま（スレッドプールで再利用されるため）
                    // HOROS-20240407準拠: そのため、isExeとisDoneを正確に判定する必要がある
                    // HOROS-20240407準拠: NSThreadのisExecutingは、スレッドが実際にコードを実行中かどうかを示す
                    // HOROS-20240407準拠: NSThreadのisFinishedは、スレッドが終了したかどうかを示す
                    // HOROS-20240407準拠: Javaでは、Thread.State.RUNNABLEは実行中または実行可能を意味する
                    // HOROS-20240407準拠: ExecutorServiceのワーカースレッドは既に開始されているため、isExeは実際の実行状態を反映しない
                    // HOROS-20240407準拠: ただし、HOROS-20240407の実装に準拠するため、isExeとisDoneを正確に判定する
                    // HOROS-20240407準拠: NEW状態のスレッドは、isAlive()がfalseだが、isFinishedではない
                    // HOROS-20240407準拠: そのため、isDoneはTERMINATED状態の場合のみtrueとする
                    boolean isExe = thread.isAlive() && thread.getState() != Thread.State.TERMINATED && thread.getState() != Thread.State.NEW;
                    boolean isDone = thread.getState() == Thread.State.TERMINATED;
                    
                    try {
                        // HOROS-20240407準拠: if (!isDone) { (132行目)
                        if (!isDone) {
                            // HOROS-20240407準拠: [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(threadWillExit:) name:NSThreadWillExitNotification object:thread]; (133行目)
                            // TODO: スレッド終了通知を実装
                            // HOROS-20240407準拠: [_threadsController addObject:thread]; (134行目)
                            threads.add(thread);
                            // デバッグログは条件付きで出力（必要に応じて有効化）
                            // System.err.println("ThreadsManager.subAddThread: added thread: " + thread.getName() + " (id=" + thread.getId() + "), total=" + threads.size());
                            notifyListeners();
                        }
                        // HOROS-20240407準拠: if (!isExe && !isDone) { // not executing, not done executing... execute now (136行目)
                        // HOROS-20240407準拠: ExecutorServiceのワーカースレッドは既に開始されているため、start()を呼ぶ必要はない
                        // HOROS-20240407準拠: ただし、NEW状態のスレッド（通常のThread）の場合はstart()を呼ぶ
                        if (!isExe && !isDone && thread.getState() == Thread.State.NEW) {
                            // HOROS-20240407準拠: [thread start]; (137行目)
                            try {
                                thread.start();
                            } catch (IllegalThreadStateException e) {
                                // 既に開始されているスレッドの場合は無視
                            }
                        }
                        
                        // HOROS-20240407準拠: if ([thread isFinished]) // already done?? wtf.. (140行目)
                        if (isDone) {
                            // HOROS-20240407準拠: [[NSNotificationCenter defaultCenter] removeObserver:self name:NSThreadWillExitNotification object:thread]; (142行目)
                            // HOROS-20240407準拠: [_threadsController removeObject:thread]; (143行目)
                            threads.remove(thread);
                            notifyListeners();
                        }
                    } catch (Exception e) {
                        // HOROS-20240407準拠: @catch (NSException* e) (146行目)
                        // HOROS-20240407準拠: [[NSNotificationCenter defaultCenter] removeObserver:self name:NSThreadWillExitNotification object:thread]; (148行目)
                        // HOROS-20240407準拠: [_threadsController removeObject:thread]; (149行目)
                        e.printStackTrace();
                        threads.remove(thread);
                        notifyListeners();
                    }
                }
            }
        }
    }
    
    /**
     * スレッドを追加して開始
     * HOROS-20240407準拠: - (void)addThreadAndStart:(NSThread*)thread
     */
    public void addThreadAndStart(Thread thread) {
        // HOROS-20240407準拠: if (![NSThread isMainThread]) (159行目)
        if (!SwingUtilities.isEventDispatchThread()) {
            // HOROS-20240407準拠: if( [thread isExecuting] == NO && [thread isFinished] == NO) (161行目)
            // HOROS-20240407準拠: Javaでは、thread.getState() == Thread.State.NEWの場合、スレッドは実行中でなく、終了もしていない
            // HOROS-20240407準拠: ExecutorServiceのワーカースレッドは既に開始されているため、NEW状態のスレッドのみstart()を呼ぶ
            if (thread.getState() == Thread.State.NEW) {
                // HOROS-20240407準拠: [thread start]; // We want to start it immediately: subAddThread must add it on main thread: the main thread is maybe locked. (162行目)
                thread.start();
            }
            // HOROS-20240407準拠: [self performSelectorOnMainThread:@selector(subAddThread:) withObject:thread waitUntilDone: NO]; (163行目)
            SwingUtilities.invokeLater(() -> subAddThread(thread));
        } else {
            // HOROS-20240407準拠: else [self subAddThread:thread]; (165行目)
            subAddThread(thread);
        }
    }
    
    /**
     * スレッドを削除（内部処理）
     * HOROS-20240407準拠: - (void) subRemoveThread:(NSThread*)thread
     */
    private void subRemoveThread(Thread thread) {
        synchronized (threads) {
            synchronized (thread) {
                // HOROS-20240407準拠: if (![NSThread isMainThread])
                if (!SwingUtilities.isEventDispatchThread()) {
                    // 警告ログは条件付きで出力（必要に応じて有効化）
                    // System.err.println("***** ThreadsManager.subRemoveThread should be on MAIN thread");
                }
                
                // HOROS-20240407準拠: if ([_threadsController.content containsObject:thread]) {
                if (threads.contains(thread)) {
                    // HOROS-20240407準拠: [[NSNotificationCenter defaultCenter] removeObserver:self name:NSThreadWillExitNotification object:thread];
                    // TODO: スレッド終了通知を削除
                    // HOROS-20240407準拠: [_threadsController removeObject:thread];
                    threads.remove(thread);
                    // HOROS-20240407準拠: スレッド情報も削除
                    threadInfoMap.remove(thread);
                    // デバッグログは条件付きで出力（必要に応じて有効化）
                    // System.err.println("ThreadsManager.subRemoveThread: removed thread: " + thread.getName() + " (id=" + thread.getId() + "), total=" + threads.size());
                    // HOROS-20240407準拠: リスナーに通知（重複登録を防ぐため重要）
                    notifyListeners();
                } else {
                    // デバッグログは条件付きで出力（必要に応じて有効化）
                    // System.err.println("ThreadsManager.subRemoveThread: thread not found: " + thread.getName() + " (id=" + thread.getId() + ")");
                }
            }
        }
    }
    
    /**
     * スレッドを削除
     * HOROS-20240407準拠: - (void)removeThread:(NSThread*)thread
     */
    public void removeThread(Thread thread) {
        // HOROS-20240407準拠: if (![NSThread isMainThread]) (186行目)
        if (!SwingUtilities.isEventDispatchThread()) {
            // HOROS-20240407準拠: [self performSelectorOnMainThread:@selector(subRemoveThread:) withObject:thread waitUntilDone:NO]; (187行目)
            SwingUtilities.invokeLater(() -> subRemoveThread(thread));
        } else {
            // HOROS-20240407準拠: else [self subRemoveThread:thread]; (188行目)
            subRemoveThread(thread);
        }
    }
    
    /**
     * リスナーに通知
     */
    private void notifyListeners() {
        List<Thread> currentThreads = new ArrayList<>(threads);
        for (Consumer<List<Thread>> listener : listeners) {
            listener.accept(currentThreads);
        }
    }
    
    /**
     * リスナーを追加（arrangedObjectsの変更を通知）
     * HOROS-20240407準拠: NSArrayControllerのarrangedObjectsを監視する機能
     */
    public void addArrangedObjectsListener(Consumer<List<Thread>> listener) {
        listeners.add(listener);
    }
    
    /**
     * リスナーを削除
     */
    public void removeArrangedObjectsListener(Consumer<List<Thread>> listener) {
        listeners.remove(listener);
    }
    
    /**
     * スレッド情報を取得
     * HOROS-20240407準拠: NSThread+N2のthreadDictionary相当
     */
    public ThreadInfo getThreadInfo(Thread thread) {
        // HOROS-20240407準拠: threadDictionaryが存在しない場合は作成
        return threadInfoMap.computeIfAbsent(thread, t -> new ThreadInfo());
    }
    
    /**
     * スレッドの進捗を取得
     * HOROS-20240407準拠: NSThread+N2.mm 321-333行目: - (CGFloat)progress
     */
    public double getThreadProgress(Thread thread) {
        ThreadInfo info = threadInfoMap.get(thread);
        if (info != null) {
            return info.getProgress();
        }
        return -1.0; // HOROS-20240407準拠: デフォルトは-1（indeterminateモード）
    }
    
    /**
     * スレッドの進捗を設定
     * HOROS-20240407準拠: NSThread+N2.mm 335-351行目: - (void)setProgress:(CGFloat)progress
     */
    public void setThreadProgress(Thread thread, double progress) {
        getThreadInfo(thread).setProgress(progress);
    }
    
    /**
     * スレッドのステータスを取得
     * HOROS-20240407準拠: NSThread+N2.mm 278-296行目: - (NSString*)status
     */
    public String getThreadStatus(Thread thread) {
        ThreadInfo info = threadInfoMap.get(thread);
        if (info != null) {
            return info.getStatus();
        }
        return null; // HOROS-20240407準拠: デフォルトはnil
    }
    
    /**
     * スレッドのステータスを設定
     * HOROS-20240407準拠: NSThread+N2.mm 298-316行目: - (void)setStatus:(NSString*)status
     */
    public void setThreadStatus(Thread thread, String status) {
        getThreadInfo(thread).setStatus(status);
    }
    
    /**
     * スレッドがキャンセル可能かどうかを取得
     * HOROS-20240407準拠: NSThread+N2.mm 219-230行目: - (BOOL)supportsCancel
     */
    public boolean supportsCancel(Thread thread) {
        ThreadInfo info = threadInfoMap.get(thread);
        if (info != null) {
            return info.supportsCancel();
        }
        return false;
    }
    
    /**
     * スレッドがキャンセル可能かどうかを設定
     * HOROS-20240407準拠: NSThread+N2.mm 232-249行目: - (void)setSupportsCancel:(BOOL)supportsCancel
     */
    public void setSupportsCancel(Thread thread, boolean supportsCancel) {
        getThreadInfo(thread).setSupportsCancel(supportsCancel);
    }
    
    /**
     * スレッドがキャンセルされているかどうかを取得
     * HOROS-20240407準拠: NSThread+N2.mm 102-115行目: - (BOOL)isCancelled
     */
    public boolean isCancelled(Thread thread) {
        ThreadInfo info = threadInfoMap.get(thread);
        if (info != null) {
            return info.isCancelled();
        }
        return false;
    }
    
    /**
     * スレッドのキャンセル状態を設定
     * HOROS-20240407準拠: NSThread+N2.mm 117-130行目: - (void)setIsCancelled:(BOOL)isCancelled
     */
    public void setIsCancelled(Thread thread, boolean isCancelled) {
        getThreadInfo(thread).setIsCancelled(isCancelled);
    }
}

