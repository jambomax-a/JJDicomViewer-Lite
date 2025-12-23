# 同期関連変数の完全解析

## 重要な発見

### 1. `syncro`はグローバル変数（DCMView.m:86）
```objective-c
short syncro = syncroLOC;
```
- デフォルト値は`syncroLOC`（Slice Position同期）
- グローバル変数なので、すべてのDCMViewインスタンスで共有される

### 2. `setSyncro:`メソッド（DCMView.m:7370）
```objective-c
-(void) syncronize:(id) sender
{
    [self setSyncro: [sender tag]];
}
```
- メニューアイテムの`tag`が`syncro`の値として使用される
- `syncronize:`メソッドがメニューから呼び出される

### 3. メニューアイテムの状態管理（DCMView.m:1398-1402）
```objective-c
else if( [item action] == @selector(syncronize:))
{
    valid = YES;
    if( [item tag] == syncro) [item setState: NSOnState];
    else [item setState: NSOffState];
}
```
- メニューアイテムの`tag`が現在の`syncro`値と一致する場合、チェックマークが表示される

### 4. `numberOf2DViewer`は静的変数（ViewerController.m:202）
```objective-c
static int numberOf2DViewer = 0;
static NSMutableArray *arrayOf2DViewers = nil;
```
- すべてのViewerControllerインスタンスで共有される
- ビューアの作成/削除時にインクリメント/デクリメントされる

### 5. `becomeMainWindow`の呼び出し（ViewerController.m:16559）
```objective-c
[imageView becomeMainWindow];
```
- `setSyncro:`の後に`becomeMainWindow`が呼び出される
- これにより、同期メッセージが送信される

## 重要なポイント

### `syncro`の設定タイミング
1. メニューから`syncronize:`が呼び出される
2. `setSyncro:`が呼び出される
3. `becomeMainWindow`が呼び出される
4. 同期メッセージが送信される

### `numberOf2DViewer`の管理
- ビューアの作成時にインクリメント
- ビューアの削除時にデクリメント
- `sendSyncMessage:`の条件で使用される

## 次のステップ

1. `setSyncro:`メソッドの実装を確認
2. `becomeMainWindow`メソッドの実装を確認
3. `numberOf2DViewer`のインクリメント/デクリメント箇所を確認
4. メニューアイテムの`tag`値を確認


