# ウィンドウフォーカスとisKeyViewの管理の完全解析

## 重要な発見

### 1. `ThumbnailsListPanel`がウィンドウフォーカスを監視
- `NSWindowDidBecomeMainNotification`を監視（ThumbnailsListPanel.m:119）
- `NSWindowDidBecomeKeyNotification`を監視（ThumbnailsListPanel.m:121）
- ウィンドウがメイン/キーになったときに、対応するビューワーウィンドウを前面に表示

### 2. `ViewerController`の`windowDidBecomeMain:`
- `ViewerController.m:3460-3473`で実装
- `clearFrontMost2DViewerCache`を呼び出す
- しかし、`isKeyView`の管理はしていない

### 3. `becomeMainWindow`の呼び出し
- `ViewerController.m:16559`で`[imageView becomeMainWindow]`を呼び出し
- 同期ボタンの動作で呼ばれる

### 4. `becomeMainWindow`の実装（DCMView.m:12539-12553）
```objective-c
-(void) becomeMainWindow
{
    [self updateTilingViews];
    
    sliceFromTo[ 0][ 0] = HUGE_VALF;
    sliceFromTo2[ 0][ 0] = HUGE_VALF;
    sliceFromToS[ 0][ 0] = HUGE_VALF;
    sliceFromToE[ 0][ 0] = HUGE_VALF;
    sliceVector[ 0] = sliceVector[ 1] = sliceVector[ 2] = 0;
    slicePoint3D[ 0] = HUGE_VALF;
    
    [self sendSyncMessage: 0];
    [self computeColor];
    [self setNeedsDisplay:YES];
}
```
- 交線をクリア
- `sendSyncMessage: 0`を呼び出す
- しかし、`isKeyView`の設定はしていない

### 5. `becomeKeyWindow`の実装（DCMView.m:12555-12572）
```objective-c
-(void) becomeKeyWindow
{
    sliceFromTo[ 0][ 0] = HUGE_VALF;
    sliceFromTo2[ 0][ 0] = HUGE_VALF;
    sliceFromToS[ 0][ 0] = HUGE_VALF;
    sliceFromToE[ 0][ 0] = HUGE_VALF;
    sliceVector[ 0] = sliceVector[ 1] = sliceVector[ 2] = 0;
    slicePoint3D[ 0] = HUGE_VALF;
    
    [self erase2DPointMarker];
    if( blendingView) [blendingView erase2DPointMarker];
    
    [self sendSyncMessage: 0];
    
    [self flagsChanged: [[NSApplication sharedApplication] currentEvent]];
    
    [self setNeedsDisplay:YES];
}
```
- 交線をクリア
- `sendSyncMessage: 0`を呼び出す
- しかし、`isKeyView`の設定はしていない

### 6. `becomeFirstResponder`の実装（DCMView.m:12574-12610）
```objective-c
- (BOOL)becomeFirstResponder
{
    isKeyView = YES;
    
    [self updateTilingViews];
    
    if (curImage < 0)
    {
        // ... 画像インデックスの設定 ...
    }
    
    [self becomeKeyWindow];
    [self setNeedsDisplay:YES];
    
    if( [self is2DViewer])
    {
        [[self windowController] adjustSlider];
        [[self windowController] propagateSettings];
    }
    
    [[NSNotificationCenter defaultCenter] postNotificationName:OsirixDCMViewDidBecomeFirstResponderNotification object:self];
    
    [self flagsChanged: [[NSApplication sharedApplication] currentEvent]];
    
    return YES;
}
```
- **`isKeyView = YES`を設定**
- `becomeKeyWindow`を呼び出す
- `OsirixDCMViewDidBecomeFirstResponderNotification`を送信

### 7. `resignFirstResponder`の実装（DCMView.m:12790-12797）
```objective-c
- (BOOL)resignFirstResponder
{
    isKeyView = NO;
    [self setNeedsDisplay:YES];
    [self sendSyncMessage: 0];
    
    return [super resignFirstResponder];
}
```
- **`isKeyView = NO`を設定**
- `sendSyncMessage: 0`を呼び出す

## 重要なポイント

### `isKeyView`の管理は`becomeFirstResponder`/`resignFirstResponder`で行われる
- `becomeFirstResponder`で`isKeyView = YES`を設定
- `resignFirstResponder`で`isKeyView = NO`を設定
- `becomeMainWindow`や`becomeKeyWindow`では設定しない

### `newImageViewisKey:`の通知登録箇所が見つからない
- `DCMView.m:12810-12814`に`newImageViewisKey:`メソッドが定義されている
- `becomeFirstResponder`が`OsirixDCMViewDidBecomeFirstResponderNotification`を送信している
- しかし、`newImageViewisKey:`がこの通知を監視している登録箇所が見つからない

**可能性**:
1. `newImageViewisKey:`は通知を監視していない（未使用のコード）
2. 別の方法で呼び出されている
3. 実装されていない

### ブラウザーウインドウとビューワーウインドウが独立している
- `BrowserController`と`ViewerController`は独立したウィンドウ
- `frontMostDisplayed2DViewer`は`NSApp orderedWindows`から取得
- ウィンドウのフォーカス管理が`isKeyView`の管理に関係している可能性

### フローティングウインドウやサムネイルウインドウのロジック
- `ThumbnailsListPanel`がウィンドウフォーカスを監視
- ウィンドウがメイン/キーになったときに、対応するビューワーウィンドウを前面に表示
- しかし、`isKeyView`の管理はしていない

## 次のステップ

1. `newImageViewisKey:`の通知登録箇所を探す（見つからない場合は未使用のコードと判断）
2. `becomeFirstResponder`/`resignFirstResponder`の呼び出し箇所を完全に確認
3. ウィンドウフォーカスと`isKeyView`の関係を完全に理解する
4. 同期ロジックを完全に再実装する

