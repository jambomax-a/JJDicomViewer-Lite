package com.jj.dicomviewer.threads;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * スレッド情報管理クラス
 * HOROS-20240407準拠: NSThread+N2.mmのthreadDictionary相当
 * 
 * スレッドのprogress、status、その他の属性を管理
 */
public class ThreadInfo {
    
    // HOROS-20240407準拠: NSThreadProgressKey = @"progress"
    public static final String PROGRESS_KEY = "progress";
    
    // HOROS-20240407準拠: NSThreadStatusKey = @"status"
    public static final String STATUS_KEY = "status";
    
    // HOROS-20240407準拠: NSThreadSupportsCancelKey = @"supportsCancel"
    public static final String SUPPORTS_CANCEL_KEY = "supportsCancel";
    
    // HOROS-20240407準拠: NSThreadIsCancelledKey = @"isCancelled"
    public static final String IS_CANCELLED_KEY = "isCancelled";
    
    // HOROS-20240407準拠: threadDictionary（NSMutableDictionary相当）
    private final Map<String, Object> dictionary;
    
    /**
     * コンストラクタ
     * HOROS-20240407準拠: threadDictionaryの初期化
     */
    public ThreadInfo() {
        // HOROS-20240407準拠: threadDictionary = [NSMutableDictionary dictionary];
        dictionary = new ConcurrentHashMap<>();
        // HOROS-20240407準拠: デフォルトでprogress = -1（indeterminateモード）
        setProgress(-1.0);
    }
    
    /**
     * 進捗を取得
     * HOROS-20240407準拠: NSThread+N2.mm 321-333行目: - (CGFloat)progress
     */
    public double getProgress() {
        // HOROS-20240407準拠: @synchronized (self) { NSNumber* progress = [self.threadDictionary objectForKey:NSThreadProgressKey]; return progress? progress.floatValue : -1; }
        synchronized (dictionary) {
            Object progress = dictionary.get(PROGRESS_KEY);
            if (progress instanceof Number) {
                return ((Number) progress).doubleValue();
            }
            return -1.0;
        }
    }
    
    /**
     * 進捗を設定
     * HOROS-20240407準拠: NSThread+N2.mm 335-351行目: - (void)setProgress:(CGFloat)progress
     */
    public void setProgress(double progress) {
        // HOROS-20240407準拠: @synchronized (self) { if (self.progress == progress) return; [self willChangeValueForKey:NSThreadProgressKey]; [self.threadDictionary setObject:[NSNumber numberWithFloat:progress] forKey:NSThreadProgressKey]; [self didChangeValueForKey:NSThreadProgressKey]; }
        synchronized (dictionary) {
            if (getProgress() == progress) {
                return;
            }
            dictionary.put(PROGRESS_KEY, progress);
            // TODO: KVO相当の通知処理（現在は実装しない）
        }
    }
    
    /**
     * ステータスを取得
     * HOROS-20240407準拠: NSThread+N2.mm 278-296行目: - (NSString*)status
     */
    public String getStatus() {
        // HOROS-20240407準拠: @synchronized (self) { NSString* status = [d objectForKey:NSThreadStatusKey]; if (status) return [[status copy] autorelease]; } return nil;
        synchronized (dictionary) {
            Object status = dictionary.get(STATUS_KEY);
            if (status instanceof String) {
                return (String) status;
            }
            return null;
        }
    }
    
    /**
     * ステータスを設定
     * HOROS-20240407準拠: NSThread+N2.mm 298-316行目: - (void)setStatus:(NSString*)status
     */
    public void setStatus(String status) {
        // HOROS-20240407準拠: @synchronized (self) { NSString* previousStatus = self.status; if (previousStatus == status || [status isEqualToString:previousStatus]) return; [self willChangeValueForKey:NSThreadStatusKey]; if (status) [self.currentOperationDictionary setObject:[[status copy] autorelease] forKey:NSThreadStatusKey]; else [self.currentOperationDictionary removeObjectForKey:NSThreadStatusKey]; [self didChangeValueForKey:NSThreadStatusKey]; }
        synchronized (dictionary) {
            String previousStatus = getStatus();
            if (previousStatus == status || (status != null && status.equals(previousStatus))) {
                return;
            }
            if (status != null) {
                dictionary.put(STATUS_KEY, status);
            } else {
                dictionary.remove(STATUS_KEY);
            }
            // TODO: KVO相当の通知処理（現在は実装しない）
        }
    }
    
    /**
     * キャンセル可能かどうかを取得
     * HOROS-20240407準拠: NSThread+N2.mm 219-230行目: - (BOOL)supportsCancel
     */
    public boolean supportsCancel() {
        // HOROS-20240407準拠: @synchronized (self) { return [[self.currentOperationDictionary objectForKey:NSThreadSupportsCancelKey] boolValue]; }
        synchronized (dictionary) {
            Object supportsCancel = dictionary.get(SUPPORTS_CANCEL_KEY);
            if (supportsCancel instanceof Boolean) {
                return (Boolean) supportsCancel;
            }
            return false;
        }
    }
    
    /**
     * キャンセル可能かどうかを設定
     * HOROS-20240407準拠: NSThread+N2.mm 232-249行目: - (void)setSupportsCancel:(BOOL)supportsCancel
     */
    public void setSupportsCancel(boolean supportsCancel) {
        // HOROS-20240407準拠: @synchronized (self) { [self willChangeValueForKey:NSThreadSupportsCancelKey]; [self.currentOperationDictionary setObject:[NSNumber numberWithBool:supportsCancel] forKey:NSThreadSupportsCancelKey]; [self didChangeValueForKey:NSThreadSupportsCancelKey]; }
        synchronized (dictionary) {
            if (supportsCancel == supportsCancel()) {
                return;
            }
            dictionary.put(SUPPORTS_CANCEL_KEY, supportsCancel);
            // TODO: KVO相当の通知処理（現在は実装しない）
        }
    }
    
    /**
     * キャンセルされているかどうかを取得
     * HOROS-20240407準拠: NSThread+N2.mm 102-115行目: - (BOOL)isCancelled
     */
    public boolean isCancelled() {
        // HOROS-20240407準拠: @synchronized (self) { NSNumber* cancelled = [self.threadDictionary objectForKey:NSThreadIsCancelledKey]; return cancelled? cancelled.boolValue : NO; }
        synchronized (dictionary) {
            Object cancelled = dictionary.get(IS_CANCELLED_KEY);
            if (cancelled instanceof Boolean) {
                return (Boolean) cancelled;
            }
            return false;
        }
    }
    
    /**
     * キャンセル状態を設定
     * HOROS-20240407準拠: NSThread+N2.mm 117-130行目: - (void)setIsCancelled:(BOOL)isCancelled
     */
    public void setIsCancelled(boolean isCancelled) {
        // HOROS-20240407準拠: @synchronized (self) { [self willChangeValueForKey:NSThreadIsCancelledKey]; if (isCancelled) [self.threadDictionary setObject:[NSNumber numberWithBool:YES] forKey:NSThreadIsCancelledKey]; else [self.threadDictionary removeObjectForKey:NSThreadIsCancelledKey]; [self didChangeValueForKey:NSThreadIsCancelledKey]; }
        synchronized (dictionary) {
            if (isCancelled) {
                dictionary.put(IS_CANCELLED_KEY, true);
            } else {
                dictionary.remove(IS_CANCELLED_KEY);
            }
            // TODO: KVO相当の通知処理（現在は実装しない）
        }
    }
    
    /**
     * 辞書から値を取得（汎用）
     * HOROS-20240407準拠: threadDictionaryの直接アクセス
     */
    public Object get(String key) {
        synchronized (dictionary) {
            return dictionary.get(key);
        }
    }
    
    /**
     * 辞書に値を設定（汎用）
     * HOROS-20240407準拠: threadDictionaryの直接アクセス
     */
    public void put(String key, Object value) {
        synchronized (dictionary) {
            if (value != null) {
                dictionary.put(key, value);
            } else {
                dictionary.remove(key);
            }
        }
    }
}

