# HOROS-20240407 複数ビューワー配置とツールバー構造の解析

## 概要

HOROS-20240407では、サムネイルを右クリックすると新しいビューワーが作成され、複数のビューワーが画面に並べて表示されます。このドキュメントでは、複数ビューワーの配置ロジック（`tileWindows:`）とツールバーの扱いについて解析します。

## 重要なポイント

1. **右クリック時の動作**: 新しいViewerControllerを作成し、`tileWindows:`または`checkAllWindowsAreVisible:`を呼び出してウィンドウを配置
2. **ツールバーの扱い**: `USETOOLBARPANEL`フラグによって、ツールバーを別ウィンドウとして表示するか、ビューワーに統合するかが決まる
3. **ウィンドウ配置**: `AUTOTILING`設定によって、自動的にウィンドウを並べて表示する

---

## 右クリック時の処理フロー

### HOROS-20240407の実装

**ViewerController.m:4293-4322** (`loadSelectedSeries:rightClick:`メソッド):

```objective-c
if( (rightClick || ([[[NSApplication sharedApplication] currentEvent] modifierFlags] & NSCommandKeyMask)) && FullScreenOn == NO)
{
    // 右クリックまたはCommandキーが押されている場合
    // 新しいViewerControllerを作成
    ViewerController *newViewer = [[BrowserController currentBrowser] loadSeries :series :nil :YES keyImagesOnly: displayOnlyKeyImages];
    [newViewer setHighLighted: 1.0];
    
    if( [[NSUserDefaults standardUserDefaults] boolForKey: @"UseFloatingThumbnailsList"] == NO)
        [self showCurrentThumbnail: self];
    
    // ウィンドウ配置処理
    if( [[NSUserDefaults standardUserDefaults] boolForKey: @"AUTOTILING"])
        [NSApp sendAction: @selector(tileWindows:) to:nil from: self];
    else
        [[AppController sharedAppController] checkAllWindowsAreVisible: self makeKey: YES];
    
    // すべてのThumbnailsListPanelを非表示にする
    for( int i = 0; i < [[NSScreen screens] count]; i++) 
        [thumbnailsListPanel[ i] setThumbnailsView: nil viewer: nil];
    
    // 現在のViewerController（self）を前面に
    [[self window] makeKeyAndOrderFront: self];
    [self refreshToolbar];
    [self updateNavigator];
    
    // 新しいViewerControllerのThumbnailsListPanelを表示
    [newViewer showCurrentThumbnail: self];
    [self syncThumbnails];
}
```

**処理の順序**:
1. 新しいViewerControllerを作成（`loadSeries :series :nil :YES`）
2. `AUTOTILING`設定に応じて、`tileWindows:`または`checkAllWindowsAreVisible:`を呼び出し
3. すべてのThumbnailsListPanelを非表示にする
4. 現在のViewerController（`self`）を前面に
5. 新しいViewerControllerのThumbnailsListPanelを表示

---

## ウィンドウ配置ロジック

### `AUTOTILING`設定

**ViewerController.m:2931-2934** (`newWindow:frame:`メソッド):

```objective-c
if( [[NSUserDefaults standardUserDefaults] boolForKey: @"AUTOTILING"])
    [[AppController sharedAppController] tileWindows: nil];
else
    [[AppController sharedAppController] checkAllWindowsAreVisible: nil makeKey: YES];
```

**動作**:
- `AUTOTILING == YES`: `tileWindows:`を呼び出して、複数のビューワーを自動的に並べて配置
- `AUTOTILING == NO`: `checkAllWindowsAreVisible:`を呼び出して、すべてのウィンドウが表示されるように配置（重なりを避ける）

### `tileWindows:`メソッド

**実装場所**: `AppController.m`（推定）

**役割**: 複数のViewerControllerウィンドウを画面に並べて配置

**呼び出し箇所**:
- `ViewerController.m:2932` - 新しいビューワー作成時
- `ViewerController.m:3028` - ウィンドウタイル設定変更時
- `ViewerController.m:3282` - ウィンドウリサイズ時
- `ViewerController.m:3300` - ウィンドウ最小化時
- `ViewerController.m:3320` - ウィンドウ復元時
- `ViewerController.m:4308` - 右クリック時（新しいビューワー作成時）

**重要なポイント**:
- 複数のビューワーを画面に均等に配置
- 画面サイズに応じて、ビューワーのサイズと位置を自動調整
- ビューワー同士が重ならないように配置

### `checkAllWindowsAreVisible:`メソッド

**実装場所**: `AppController.m`（推定）

**役割**: すべてのウィンドウが表示されるように配置（重なりを避ける）

**呼び出し箇所**:
- `ViewerController.m:2934` - 新しいビューワー作成時（`AUTOTILING == NO`の場合）
- `ViewerController.m:4310` - 右クリック時（`AUTOTILING == NO`の場合）

**重要なポイント**:
- `AUTOTILING`が無効な場合でも、ウィンドウが重ならないように配置
- すべてのウィンドウが画面内に表示されるように調整

---

## ツールバーの構造

### `USETOOLBARPANEL`フラグ

**AppController.m:136**:
```objective-c
BOOL USETOOLBARPANEL = NO;
```

**デフォルト値**: `NO`（ツールバーはビューワーに統合）

### ツールバーの扱い

**ViewerController.m:3303-3304, 3323-3324**:
```objective-c
if( [AppController USETOOLBARPANEL])
    [[toolbarPanel window] orderOut: self];  // 最小化時
    // または
    [[toolbarPanel window] orderFront: self];  // 復元時
```

**動作**:
- `USETOOLBARPANEL == NO`（デフォルト）: ツールバーはビューワーウィンドウに統合されている
- `USETOOLBARPANEL == YES`: ツールバーは別ウィンドウ（`ToolbarPanel`）として表示される

**重要なポイント**:
- **デフォルトでは、ツールバーはビューワーウィンドウに統合されている**
- 各ビューワーウィンドウに独自のツールバーが含まれる
- `USETOOLBARPANEL == YES`の場合のみ、ツールバーが別ウィンドウとして表示される

### ツールバーの実装

**ViewerController.m:391**:
```objective-c
@synthesize currentOrientationTool, speedSlider, speedText, toolbarPanel;
```

**ViewerController.m:3250-3252**:
```objective-c
[toolbarPanel close];
[toolbarPanel release];
toolbarPanel = nil;
```

**構造**:
- `toolbarPanel`は`ViewerController`のプロパティ
- `USETOOLBARPANEL == NO`の場合、ツールバーはビューワーウィンドウ内に統合
- `USETOOLBARPANEL == YES`の場合、ツールバーは別ウィンドウ（`ToolbarPanel`）として表示

---

## 複数ビューワーの配置仕様

### 配置の原則

1. **重なりを避ける**: 複数のビューワーが重ならないように配置
2. **画面内に収める**: すべてのビューワーが画面内に表示されるように調整
3. **均等配置**: `AUTOTILING == YES`の場合、ビューワーを均等に配置

### ウィンドウサイズの調整

**ViewerController.m:3033-3044** (`windowWillUseStandardFrame:defaultFrame:`メソッド):

```objective-c
- (NSRect)windowWillUseStandardFrame:(NSWindow *)sender defaultFrame:(NSRect)defaultFrame
{
    NSRect currentFrame = [sender frame];
    NSRect screenRect = [AppController usefullRectForScreen: [sender screen]];
    
    if( NSIsEmptyRect( standardRect)) standardRect = currentFrame;
    
    if (currentFrame.size.height >= screenRect.size.height - 20 && currentFrame.size.width >= screenRect.size.width - 20)
        return standardRect;
    else
        return screenRect;
}
```

**動作**:
- 画面の有効領域（`usefullRectForScreen:`）を取得
- ウィンドウが画面サイズに近い場合、標準フレームを返す
- それ以外の場合、画面サイズを返す

---

## Java Swingへの移植方針

### 1. ツールバーの扱い

**推奨実装**: HOROS-20240407のデフォルトに合わせて、ツールバーをビューワーウィンドウに統合

**理由**:
- HOROS-20240407のデフォルトは`USETOOLBARPANEL == NO`
- 各ビューワーウィンドウに独自のツールバーが含まれる
- シンプルで理解しやすい実装

**実装**:
- `ViewerController`のツールバーは、`JFrame`内の`JToolBar`として実装
- 各ビューワーウィンドウに独自のツールバーが含まれる
- ツールバーを別ウィンドウとして表示する機能は、将来的な拡張として実装可能

### 2. 複数ビューワーの配置ロジック

**実装クラス**: `WindowLayoutManager`（新規作成）

**機能**:
1. **`tileWindows()`メソッド**: 複数のビューワーを画面に均等に配置
2. **`checkAllWindowsAreVisible()`メソッド**: すべてのウィンドウが表示されるように配置（重なりを避ける）

**実装方針**:
```java
public class WindowLayoutManager {
    /**
     * 複数のビューワーを画面に均等に配置
     * HOROS-20240407のtileWindows:に相当
     */
    public static void tileWindows(List<ViewerController> viewers) {
        if (viewers.isEmpty()) return;
        
        GraphicsDevice screen = viewers.get(0).getGraphicsConfiguration().getDevice();
        Rectangle screenBounds = WindowSizeManager.getUsableScreenBounds(screen);
        
        int count = viewers.size();
        int cols = (int) Math.ceil(Math.sqrt(count));
        int rows = (int) Math.ceil((double) count / cols);
        
        int windowWidth = screenBounds.width / cols;
        int windowHeight = screenBounds.height / rows;
        
        for (int i = 0; i < count; i++) {
            ViewerController viewer = viewers.get(i);
            int row = i / cols;
            int col = i % cols;
            
            int x = screenBounds.x + col * windowWidth;
            int y = screenBounds.y + row * windowHeight;
            
            viewer.setBounds(x, y, windowWidth, windowHeight);
        }
    }
    
    /**
     * すべてのウィンドウが表示されるように配置（重なりを避ける）
     * HOROS-20240407のcheckAllWindowsAreVisible:makeKey:に相当
     */
    public static void checkAllWindowsAreVisible(List<ViewerController> viewers, boolean makeKey) {
        // ウィンドウが重ならないように配置
        // カスケード配置またはグリッド配置を実装
    }
}
```

### 3. 右クリック時の処理

**実装箇所**: `ViewerController.createSeriesClickAdapter()`

**修正内容**:
```java
// 右クリックまたはControlキー: 新しいViewerControllerを作成
if (rightClick) {
    ViewerController newViewer = ApplicationController.getInstance().openViewerWindow(seriesImages);
    
    // ウィンドウ配置処理
    if (AUTOTILING) {
        WindowLayoutManager.tileWindows(ApplicationController.getInstance().getViewerControllers());
    } else {
        WindowLayoutManager.checkAllWindowsAreVisible(
            ApplicationController.getInstance().getViewerControllers(), true);
    }
    
    // すべてのThumbnailsListPanelを非表示にする
    ApplicationController.getInstance().hideAllThumbnailsListPanels();
    
    // 現在のViewerController（this）を前面に
    this.toFront();
    this.requestFocus();
    
    // 新しいViewerControllerのThumbnailsListPanelを表示
    ApplicationController.getInstance().showThumbnailsListPanel(newViewer);
}
```

---

## 実装の優先順位

1. **優先度1**: ツールバーをビューワーウィンドウに統合（現在の実装を確認）
2. **優先度2**: `WindowLayoutManager`クラスの作成と`tileWindows()`メソッドの実装
3. **優先度3**: `checkAllWindowsAreVisible()`メソッドの実装
4. **優先度4**: 右クリック時の処理に`tileWindows()`を追加

---

## 参考ソースコード

- `horos-20240407/Horos/Sources/ViewerController.m`:
  - `loadSelectedSeries:rightClick:` (4272行目～)
  - `newWindow:frame:` (2921行目～)
  - `windowWillUseStandardFrame:defaultFrame:` (3033行目～)
- `horos-20240407/Horos/Sources/AppController.m`:
  - `tileWindows:`メソッド（推定）
  - `checkAllWindowsAreVisible:makeKey:`メソッド（推定）
  - `USETOOLBARPANEL`フラグ（136行目）

---

## 変更履歴

- 2024年12月: 初版作成

