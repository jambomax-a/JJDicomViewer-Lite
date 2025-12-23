# HOROS-20240407 同期システム徹底解析

## 1. 同期モードの定義（DCMView.h:114）
```objective-c
enum { syncroOFF = 0, syncroABS = 1, syncroREL = 2, syncroLOC = 3, syncroRatio = 4};
```

## 2. 同期モードの設定方法

### 2.1 グローバル変数（DCMView.m:86）
```objective-c
short syncro = syncroLOC;  // デフォルトはsyncroLOC
```

### 2.2 同期モードの設定（DCMView.m:7376-7385）
```objective-c
+ (void)setSyncro:(short) s
{
    syncro = s;
    [[NSNotificationCenter defaultCenter] postNotificationName: OsirixSyncSeriesNotification object:nil userInfo: nil];
}
- (void)setSyncro:(short) s
{
    syncro = s;
    [[NSNotificationCenter defaultCenter] postNotificationName: OsirixSyncSeriesNotification object:nil userInfo: nil];
}
```

### 2.3 同期ボタンの動作（ViewerController.m:16537-16561）
```objective-c
- (void) SyncSeries:(id) sender
{
    if( SyncButtonBehaviorIsBetweenStudies)
    {
        SYNCSERIES = !SYNCSERIES;
        // ... 通知を送信
    }
    else
    {
        if( [imageView syncro] == syncroOFF)
        {
            if( [[[NSApplication sharedApplication] currentEvent] modifierFlags] & NSAlternateKeyMask)
                [imageView setSyncro: syncroREL];
            else
                [imageView setSyncro: syncroLOC];
        }
        else [imageView setSyncro: syncroOFF];
        
        [imageView becomeMainWindow];
    }
}
```

**重要な点**:
- 通常クリック: `syncroOFF` → `syncroLOC`
- Alt+クリック: `syncroOFF` → `syncroREL`
- 既に同期ONの場合: 同期OFF

## 3. 同期通知の送信（DCMView.m:6721-6741）

### 3.1 sendSyncMessage:の条件
```objective-c
-(void) sendSyncMessage:(short) inc
{
    if( dcmPixList == nil) return;
    
    if( [ViewerController numberOf2DViewer] > 1 && isKeyView && [self is2DViewer])
    {
        NSDictionary *instructions = [self syncMessage: inc];
        
        if( instructions)
        {
            [[NSNotificationCenter defaultCenter] postNotificationName: OsirixSyncNotification object: self userInfo: instructions];
        }
    }
}
```

**条件**:
- `numberOf2DViewer > 1`: 2Dビューアーが2つ以上
- `isKeyView`: キービューである
- `is2DViewer`: 2Dビューアーである

### 3.2 sendSyncMessage:が呼ばれるタイミング
1. `becomeMainWindow`（DCMView.m:12550）
2. `becomeFirstResponder`（DCMView.m:12567）
3. `setIndex:`（DCMView.m:2627）→ `OsirixDCMViewIndexChangedNotification`を送信（これは別の通知）

## 4. 同期通知の受信（DCMView.m:6916-7265）

### 4.1 sync:メソッドの条件（DCMView.m:6923-6934）
```objective-c
if( ![[[note object] superview] isEqual:[self superview]] && [self is2DViewer])
{
    // ...
    if( [note object] != self && isKeyView == YES && matrix == 0 && newImage > -1)
    {
        // 同期処理
    }
}
```

**条件**:
- `superview`が異なる
- `is2DViewer`である
- `[note object] != self`: 自分自身からの通知ではない
- `isKeyView == YES`: キービューである
- `matrix == 0`: Image Tilingが無効
- `newImage > -1`: 有効な画像インデックス

### 4.2 同期条件（DCMView.m:6997）
```objective-c
if( same3DReferenceWorld || registeredViewer || [[NSUserDefaults standardUserDefaults] boolForKey:@"SAMESTUDY"] == NO || syncSeriesIndex != -1)
```

**条件**:
- `same3DReferenceWorld`: 同じ3D参照世界
- `registeredViewer`: 登録されたビューアー
- `SAMESTUDY == NO`: 異なるスタディ間でも同期
- `syncSeriesIndex != -1`: 手動同期

## 5. 同期モード別の動作

### 5.1 syncroLOC（Based on Location）（DCMView.m:7059-7180）
- 位置情報（sliceLocation）で同期
- 同じスタディ内の異なるシリーズ間でも同期可能
- CT画像の同期に使用

### 5.2 syncroABS（Absolute Vodka）（DCMView.m:7035-7042）
- スライスIDで完全一致
- `newImage = pos`

### 5.3 syncroRatio（Absolute Ratio）（DCMView.m:7045-7056）
- スライスIDで比率一致
- `ratio = pos / otherView.count`, `newImage = round(ratio * self.count)`

### 5.4 syncroREL（Relative）（DCMView.m:7183-7190）
- スライスIDで相対一致
- `newImage = prevImage + diff`

## 6. 画像更新条件（DCMView.m:7196-7207）
```objective-c
if( newImage != prevImage)
{
    if( avoidRecursiveSync <= 1)
    {
        if((selfViewer != frontMostViewer && otherViewer == frontMostViewer) || otherViewer.timer)
        {
            if( listType == 'i') [self setIndex:newImage];
            else [self setIndexWithReset:newImage :YES];
            [[self windowController] adjustSlider];
        }
    }
}
```

**条件**:
- `selfViewer != frontMostViewer && otherViewer == frontMostViewer`: 非アクティブビューアーがアクティブビューアーからの通知を受信
- `otherViewer.timer`: タイマーによる自動再生

## 7. 交線表示条件（DCMView.m:7209-7243）
```objective-c
if( same3DReferenceWorld || registeredViewer)
{
    if( (selfViewer != frontMostViewer && otherViewer == frontMostViewer) || [otherView.windowController FullScreenON])
    {
        if( same3DReferenceWorld || registeredViewer)
        {
            if( [self computeSlice: oPix :oPix2])
                [self setNeedsDisplay:YES];
        }
    }
}
```

**条件**:
- `same3DReferenceWorld || registeredViewer`: 同じ3D参照世界または登録されたビューアー
- `selfViewer != frontMostViewer && otherViewer == frontMostViewer`: 非アクティブビューアーがアクティブビューアーからの通知を受信
- `FullScreenON`: フルスクリーンモード

## 8. 重要な発見

1. **同期モードはグローバル変数**: `syncro`はグローバル変数で、すべてのビューアーで共有される
2. **同期ボタンの動作**: 通常クリックで`syncroLOC`、Alt+クリックで`syncroREL`
3. **同期条件**: `same3DReferenceWorld || registeredViewer || SAMESTUDY == NO || syncSeriesIndex != -1`
4. **画像更新条件**: `selfViewer != frontMostViewer && otherViewer == frontMostViewer`
5. **交線表示条件**: `selfViewer != frontMostViewer && otherViewer == frontMostViewer || FullScreenON`

## 9. 問題点

現在の実装では、これらの条件が正しく実装されていない可能性があります。特に：
- `isKeyView`の条件が正しく設定されていない
- `same3DReferenceWorld`の判定が正しく行われていない
- `frontMostViewer`の判定が正しく行われていない

