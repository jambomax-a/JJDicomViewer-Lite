# 同期ロジックに入る前の変数設定の完全解析

## 重要な発見

### 1. `syncro`の定義（DCMView.h:114）
```objective-c
enum { syncroOFF = 0, syncroABS = 1, syncroREL = 2, syncroLOC = 3, syncroRatio = 4};
```
- `syncroOFF = 0`: 同期OFF
- `syncroABS = 1`: SliceID Absolute（絶対位置）
- `syncroREL = 2`: SliceID Relative（相対位置）
- `syncroLOC = 3`: Slice Position（位置ベース、デフォルト）
- `syncroRatio = 4`: SliceID Ratio（比率）

### 2. `syncro`の初期値（DCMView.m:86）
```objective-c
short syncro = syncroLOC;
```
- デフォルトは`syncroLOC`（Slice Position同期）

### 3. `setSyncro:`メソッド（DCMView.m:7376-7380）
```objective-c
+ (void)setSyncro:(short) s
{
    syncro = s;
    [[NSNotificationCenter defaultCenter] postNotificationName: OsirixSyncSeriesNotification object:nil userInfo: nil];
}
```
- **クラスメソッド**（`+`）なので、すべてのDCMViewインスタンスで共有されるグローバル変数`syncro`を設定
- `OsirixSyncSeriesNotification`を送信する

### 4. `syncronize:`メソッド（DCMView.m:7368-7371）
```objective-c
-(void) syncronize:(id) sender
{
    [self setSyncro: [sender tag]];
}
```
- メニューアイテムの`tag`が`syncro`の値として使用される
- `setSyncro:`はクラスメソッドなので、`[DCMView setSyncro: [sender tag]]`とすべきだが、`[self setSyncro: ...]`でも動作する

### 5. `numberOf2DViewer`の管理

#### 定義（ViewerController.m:202）
```objective-c
static int numberOf2DViewer = 0;
static NSMutableArray *arrayOf2DViewers = nil;
```

#### インクリメント（ViewerController.m:20770）
```objective-c
- (void) viewerControllerInit
{
    // ...
    numberOf2DViewer++;
    
    @synchronized( arrayOf2DViewers)
    {
        if( arrayOf2DViewers == nil)
            arrayOf2DViewers = [[NSMutableArray alloc] init];
        
        [arrayOf2DViewers addObject: self];
    }
    // ...
}
```
- `viewerControllerInit`でインクリメントされる
- `arrayOf2DViewers`にも追加される

#### デクリメント（ViewerController.m:3261）
```objective-c
- (void) dealloc
{
    // ...
    numberOf2DViewer--;
    @synchronized( arrayOf2DViewers)
    {
        [arrayOf2DViewers removeObject: self];
    }
    // ...
}
```
- `dealloc`でデクリメントされる
- `arrayOf2DViewers`からも削除される

#### getter（ViewerController.m:3878）
```objective-c
+ (int) numberOf2DViewer
{
    return numberOf2DViewer;
}
```

### 6. `frontMostDisplayed2DViewer`の実装（ViewerController.m:518-534）
```objective-c
+ (ViewerController*) frontMostDisplayed2DViewer
{
    if( cachedFrontMostDisplayed2DViewer)
        return cachedFrontMostDisplayed2DViewer;
    
    for( NSWindow *w in [NSApp orderedWindows])
    {
        if( [[w windowController] isKindOfClass:[ViewerController class]] && w.isVisible)
        {
            cachedFrontMostDisplayed2DViewer = w.windowController;
            
            return cachedFrontMostDisplayed2DViewer;
        }
    }
    
    return nil;
}
```
- `NSApp orderedWindows`から最初に見つかった`ViewerController`を返す
- キャッシュを使用する

### 7. `becomeMainWindow`メソッド（DCMView.m:12539-12553）
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

### 8. `becomeKeyWindow`メソッド（DCMView.m:12555-12572）
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

## 重要なポイント

### 同期モードの設定フロー
1. メニューから`syncronize:`が呼び出される
2. `[DCMView setSyncro: [sender tag]]`が呼び出される（クラスメソッド）
3. グローバル変数`syncro`が更新される
4. `OsirixSyncSeriesNotification`が送信される
5. `becomeMainWindow`が呼び出される（ViewerController.m:16559）
6. `sendSyncMessage: 0`が呼び出される

### `numberOf2DViewer`の管理
- `viewerControllerInit`でインクリメント
- `dealloc`でデクリメント
- `sendSyncMessage:`の条件で使用される（`numberOf2DViewer > 1`）

### `frontMostDisplayed2DViewer`の取得
- `NSApp orderedWindows`から最初に見つかった`ViewerController`を返す
- キャッシュを使用する

## 次のステップ

1. メニューアイテムの`tag`値を確認（XIB/NIBファイルまたはメニュー作成コード）
2. `onSyncNotification`を完全削除してHOROS-20240407の構造を完全写経
3. `numberOf2DViewer`の管理を完全写経
4. `frontMostDisplayed2DViewer`の取得を完全写経


