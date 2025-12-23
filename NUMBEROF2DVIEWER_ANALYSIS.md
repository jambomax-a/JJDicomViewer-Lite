# numberOf2DViewerの完全解析

## 重要な発見

### 1. `numberOf2DViewer`の定義（ViewerController.m:202）
```objective-c
static int numberOf2DViewer = 0;
static NSMutableArray *arrayOf2DViewers = nil;
```
- 静的変数として定義されている
- 初期値は0

### 2. `numberOf2DViewer`のデクリメント（ViewerController.m:3261）
```objective-c
numberOf2DViewer--;
@synchronized( arrayOf2DViewers)
{
    [arrayOf2DViewers removeObject: self];
}
```
- `dealloc`メソッドでデクリメントされる
- `arrayOf2DViewers`からも削除される

### 3. `arrayOf2DViewers`への追加（ViewerController.m:20777）
```objective-c
@synchronized( arrayOf2DViewers)
{
    if( arrayOf2DViewers == nil)
        arrayOf2DViewers = [[NSMutableArray alloc] init];
    
    [arrayOf2DViewers addObject: self];
}
```
- 初期化時に`arrayOf2DViewers`に追加される
- しかし、`numberOf2DViewer`のインクリメント箇所が見つからない

### 4. `numberOf2DViewer`の使用箇所
- `sendSyncMessage:`の条件で使用される（`numberOf2DViewer > 1`）
- メニューの有効/無効判定で使用される（`numberOf2DViewer > 1`）

## 重要なポイント

### `numberOf2DViewer`のインクリメント箇所
- `arrayOf2DViewers`への追加と同時にインクリメントされる可能性がある
- または、別の場所でインクリメントされる
- さらに詳しく解析する必要がある

## 次のステップ

1. `numberOf2DViewer`のインクリメント箇所を探す
2. `getDisplayed2DViewers`の実装を確認
3. `frontMostDisplayed2DViewer`の実装を確認


