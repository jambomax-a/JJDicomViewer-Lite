# ウィンドウフォーカス管理の詳細解析

## 重要な発見

### 1. `ToolbarPanel`が`makeKeyAndOrderFront:`を呼び出す
- `ToolbarPanel.m:161`で`[[viewer window] makeKeyAndOrderFront: self]`を呼び出す
- `windowDidBecomeKey:`で呼ばれる（ToolbarPanel.m:153-168）

### 2. `ThumbnailsListPanel`が`makeKeyAndOrderFront:`を呼び出す
- `ThumbnailsListPanel.m:158`で`[[viewer window] makeKeyAndOrderFront: self]`を呼び出す
- `windowDidBecomeKey:`で呼ばれる（ThumbnailsListPanel.m:151-168）

### 3. `DCMView`の`becomeKeyWindow`の実装（DCMView.m:12555-12572）
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

### 4. `ViewerController`の`windowDidBecomeKey:`はコメントアウトされている
- `ViewerController.m:3475-3488`でコメントアウトされている
- 実装されていない

### 5. `makeKeyAndOrderFront:`が呼ばれると、ウィンドウがキーになる
- NSWindowの仕組みにより、`makeKeyAndOrderFront:`が呼ばれると、ウィンドウがキーになる
- ウィンドウがキーになると、そのウィンドウの`firstResponder`が自動的に設定される
- `firstResponder`が設定されると、`becomeFirstResponder`が呼ばれる

### 6. `DCMView`の`becomeKeyWindow`は`NSView`のメソッドをオーバーライドしている
- `becomeKeyWindow`は`NSView`のメソッドではなく、`NSWindow`のメソッド
- しかし、`DCMView`で実装されているということは、何らかの仕組みがある

## 重要なポイント

### NSWindowの仕組み
1. `makeKeyAndOrderFront:`が呼ばれると、ウィンドウがキーになる
2. ウィンドウがキーになると、そのウィンドウの`firstResponder`が自動的に設定される
3. `firstResponder`が設定されると、`becomeFirstResponder`が呼ばれる
4. `becomeFirstResponder`が`isKeyView = YES`を設定する

### `becomeKeyWindow`の呼び出しタイミング
- `becomeKeyWindow`は`NSWindow`のメソッド
- ウィンドウがキーになったときに呼ばれる
- しかし、`DCMView`で実装されているということは、何らかの仕組みがある

### `ToolbarPanel`と`ThumbnailsListPanel`の役割
- `ToolbarPanel`と`ThumbnailsListPanel`が`windowDidBecomeKey:`で`makeKeyAndOrderFront:`を呼び出す
- これにより、ウィンドウがキーになり、`becomeFirstResponder`が呼ばれる
- これが、マウスがビューワーの上に乗ったときにビューワーが選択されるメカニズムの可能性

## 疑問点

### `becomeKeyWindow`が`DCMView`で実装されている理由
- `becomeKeyWindow`は`NSWindow`のメソッド
- しかし、`DCMView`で実装されている
- これは、`NSView`のサブクラスが`becomeKeyWindow`を実装できるという意味か？

### `makeKeyAndOrderFront:`が`becomeFirstResponder`を呼び出すかどうか
- NSWindowの仕組みにより、`makeKeyAndOrderFront:`が呼ばれると、ウィンドウがキーになる
- ウィンドウがキーになると、そのウィンドウの`firstResponder`が自動的に設定される
- `firstResponder`が設定されると、`becomeFirstResponder`が呼ばれる
- しかし、これが確実かどうかは不明

## 次のステップ

1. `makeKeyAndOrderFront:`が`becomeFirstResponder`を呼び出すかどうかを確認する
2. `becomeKeyWindow`が`DCMView`で実装されている理由を確認する
3. ウィンドウがキーになったときに`becomeFirstResponder`が呼ばれるメカニズムを完全に理解する
4. 同期ロジックを完全に再実装する

