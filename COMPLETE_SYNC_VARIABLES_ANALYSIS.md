# 同期関連変数の完全解析（続き）

## 重要な発見

### 1. `setSyncro:`メソッド（DCMView.m:7376-7380）
```objective-c
+ (void)setSyncro:(short) s
{
    syncro = s;
    [[NSNotificationCenter defaultCenter] postNotificationName: OsirixSyncSeriesNotification object:nil userInfo: nil];
}
```
- **クラスメソッド**（`+`）なので、インスタンスではなくクラスレベルで`syncro`を設定
- `OsirixSyncSeriesNotification`を送信する
- すべてのDCMViewインスタンスで共有されるグローバル変数`syncro`を更新

### 2. `becomeMainWindow`メソッド（DCMView.m:12539-12553）
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
- `becomeKeyWindow`とは異なる（`becomeKeyWindow`は`erase2DPointMarker`も呼ぶ）

### 3. `becomeKeyWindow`メソッド（DCMView.m:12555-12572）
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
- `erase2DPointMarker`を呼ぶ
- `sendSyncMessage: 0`を呼び出す

### 4. `syncronize:`メソッド（DCMView.m:7368-7371）
```objective-c
-(void) syncronize:(id) sender
{
    [self setSyncro: [sender tag]];
}
```
- メニューアイテムの`tag`が`syncro`の値として使用される
- `setSyncro:`はクラスメソッドなので、`[DCMView setSyncro: [sender tag]]`とすべきだが、`[self setSyncro: ...]`でも動作する（Objective-Cの仕様）

## 重要なポイント

### `syncro`の設定フロー
1. メニューから`syncronize:`が呼び出される
2. `[DCMView setSyncro: [sender tag]]`が呼び出される（クラスメソッド）
3. グローバル変数`syncro`が更新される
4. `OsirixSyncSeriesNotification`が送信される
5. `becomeMainWindow`が呼び出される（ViewerController.m:16559）
6. `sendSyncMessage: 0`が呼び出される

### `numberOf2DViewer`の管理
- 静的変数として定義されているが、インクリメント/デクリメント箇所が見つからない
- `getDisplayed2DViewers`や`arrayOf2DViewers`を使用している可能性がある
- さらに詳しく解析する必要がある

## 次のステップ

1. `numberOf2DViewer`のインクリメント/デクリメント箇所を探す
2. `getDisplayed2DViewers`の実装を確認
3. `arrayOf2DViewers`の管理を確認
4. メニューアイテムの`tag`値を確認


