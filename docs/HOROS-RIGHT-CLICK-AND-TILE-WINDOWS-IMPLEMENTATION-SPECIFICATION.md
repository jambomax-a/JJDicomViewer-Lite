# HOROS-20240407 右クリック動作とtileWindows機能 実装仕様

## 概要

このドキュメントは、HOROS-20240407のサムネイル右クリック動作と複数ビューワーの自動配置（tileWindows）機能の実装仕様を詳細にまとめたものです。この2つの機能は密接に関連しており、同時に実装する必要があります。

最終更新: 2025年12月

---

## 右クリック動作の仕様

### 1. 右クリックの検出

**実装箇所**: `ViewerController.m:4413-4434` (`matrixPreviewPressed:`メソッド)

```objective-c
- (void) matrixPreviewPressed:(id) sender
{
    ThumbnailCell *cell = [sender selectedCell];
    id series = [[[sender selectedCell] representedObject] object];
    
    // cell.rightClickで右クリックかどうかを判定
    [self loadSelectedSeries: series rightClick: cell.rightClick];
}
```

**重要なポイント**:
- `ThumbnailCell`の`rightClick`プロパティで右クリックを判定
- `loadSelectedSeries:rightClick:`メソッドに`rightClick`フラグを渡す

### 2. 右クリック時の処理フロー

**実装箇所**: `ViewerController.m:4272-4380` (`loadSelectedSeries:rightClick:`メソッド)

#### 処理の順序

1. **新しいViewerControllerを作成**
   ```objective-c
   ViewerController *newViewer = [[BrowserController currentBrowser] loadSeries :series :nil :YES keyImagesOnly: displayOnlyKeyImages];
   ```
   - 第2引数が`nil`なので、新しいViewerControllerを作成
   - 第3引数が`YES`なので、キー画像のみ表示する設定を継承

2. **新しいViewerControllerをハイライト**
   ```objective-c
   [newViewer setHighLighted: 1.0];
   ```

3. **ウィンドウ配置処理**
   ```objective-c
   if( [[NSUserDefaults standardUserDefaults] boolForKey: @"AUTOTILING"])
       [NSApp sendAction: @selector(tileWindows:) to:nil from: self];
   else
       [[AppController sharedAppController] checkAllWindowsAreVisible: self makeKey: YES];
   ```
   - `AUTOTILING == YES`: `tileWindows:`を呼び出して、複数のビューワーを自動的に並べて配置
   - `AUTOTILING == NO`: `checkAllWindowsAreVisible:`を呼び出して、すべてのウィンドウが表示されるように配置（重なりを避ける）

4. **すべてのThumbnailsListPanelを非表示にする**
   ```objective-c
   for( int i = 0; i < [[NSScreen screens] count]; i++) 
       [thumbnailsListPanel[ i] setThumbnailsView: nil viewer: nil];
   ```
   - すべてのスクリーンのThumbnailsListPanelを非表示にする
   - これは重要なポイント：右クリックで新しいビューワーを作成したときは、既存のThumbnailsListPanelを非表示にする

5. **現在のViewerController（self）を前面に**
   ```objective-c
   [[self window] makeKeyAndOrderFront: self];
   [self refreshToolbar];
   [self updateNavigator];
   ```

6. **新しいViewerControllerのThumbnailsListPanelを表示**
   ```objective-c
   [newViewer showCurrentThumbnail: self];
   [self syncThumbnails];
   ```

### 3. 左クリックとの違い

**左クリック時**:
- 現在のViewerControllerでシリーズを切り替える（`loadSeries :series :self :YES`）
- 新しいViewerControllerは作成しない
- ThumbnailsListPanelは非表示にしない

**右クリック時**:
- 新しいViewerControllerを作成（`loadSeries :series :nil :YES`）
- すべてのThumbnailsListPanelを非表示にする
- 新しいViewerControllerのThumbnailsListPanelを表示
- ウィンドウ配置処理（`tileWindows:`または`checkAllWindowsAreVisible:`）を実行

---

## tileWindows機能の仕様

### 1. AUTOTILING設定

**設定名**: `AUTOTILING`（NSUserDefaults）

**デフォルト値**: 不明（設定で変更可能）

**動作**:
- `AUTOTILING == YES`: 複数のビューワーを自動的に並べて配置（タイル配置）
- `AUTOTILING == NO`: ウィンドウが重ならないように配置（カスケード配置またはグリッド配置）

### 2. tileWindows:メソッド

**実装場所**: `AppController.m:4857-5018`

**呼び出し箇所**:
- `ViewerController.m:2932` - 新しいビューワー作成時（`newWindow:frame:`）
- `ViewerController.m:3028` - ウィンドウタイル設定変更時
- `ViewerController.m:3282` - ウィンドウリサイズ時
- `ViewerController.m:3300` - ウィンドウ最小化時
- `ViewerController.m:3320` - ウィンドウ復元時
- `ViewerController.m:4308` - 右クリック時（新しいビューワー作成時）

**役割**: 複数のViewerControllerウィンドウを画面に均等に配置

**実装方針**:
1. すべてのViewerControllerウィンドウを取得
2. 画面の有効領域を取得（`usefullRectForScreen:`）
3. ウィンドウ数を基に、グリッドの行数と列数を計算
4. 各ウィンドウを均等に配置

**配置アルゴリズム**:
- ウィンドウ数を基に、グリッドの行数と列数を計算（例: 4つのウィンドウ → 2行×2列）
- 画面の有効領域をグリッドに分割
- 各ウィンドウをグリッドのセルに配置

### 3. checkAllWindowsAreVisible:メソッド

**実装場所**: `AppController.m:4483-4515`

**呼び出し箇所**:
- `ViewerController.m:2934` - 新しいビューワー作成時（`AUTOTILING == NO`の場合）
- `ViewerController.m:4310` - 右クリック時（`AUTOTILING == NO`の場合）

**役割**: すべてのウィンドウが表示されるように配置（重なりを避ける）

**実装方針**:
1. すべてのViewerControllerウィンドウを取得
2. 画面の有効領域を取得
3. ウィンドウが重ならないように配置（カスケード配置またはグリッド配置）
4. すべてのウィンドウが画面内に表示されるように調整

---

## 実装の詳細

### 1. WindowLayoutManagerクラスの作成

**新規作成**: `src/main/java/com/jj/dicomviewer/ui/WindowLayoutManager.java`

**機能**:
1. `tileWindows(List<ViewerController> viewers)`: 複数のビューワーを画面に均等に配置
2. `checkAllWindowsAreVisible(List<ViewerController> viewers, boolean makeKey)`: すべてのウィンドウが表示されるように配置（重なりを避ける）

**実装例**:
```java
public class WindowLayoutManager {
    private static final boolean AUTOTILING = true; // 設定から読み込む
    
    /**
     * 複数のビューワーを画面に均等に配置
     * HOROS-20240407のtileWindows:に相当
     */
    public static void tileWindows(List<ViewerController> viewers) {
        if (viewers.isEmpty()) return;
        
        // 最初のビューワーのスクリーンを取得
        GraphicsDevice screen = viewers.get(0).getGraphicsConfiguration().getDevice();
        Rectangle screenBounds = WindowSizeManager.getUsableScreenBounds(screen);
        
        int count = viewers.size();
        // グリッドの行数と列数を計算（例: 4つのウィンドウ → 2行×2列）
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
        if (viewers.isEmpty()) return;
        
        GraphicsDevice screen = viewers.get(0).getGraphicsConfiguration().getDevice();
        Rectangle screenBounds = WindowSizeManager.getUsableScreenBounds(screen);
        
        // カスケード配置またはグリッド配置を実装
        // ウィンドウが重ならないように配置
        int offsetX = 30;
        int offsetY = 30;
        int windowWidth = screenBounds.width / 2;
        int windowHeight = screenBounds.height / 2;
        
        for (int i = 0; i < viewers.size(); i++) {
            ViewerController viewer = viewers.get(i);
            int x = screenBounds.x + (i % 2) * windowWidth + offsetX;
            int y = screenBounds.y + (i / 2) * windowHeight + offsetY;
            
            viewer.setBounds(x, y, windowWidth, windowHeight);
            
            if (makeKey && i == viewers.size() - 1) {
                viewer.toFront();
                viewer.requestFocus();
            }
        }
    }
}
```

### 2. ApplicationControllerの拡張

**追加メソッド**:
1. `getViewerControllers()`: すべてのViewerControllerのリストを取得
2. `hideAllThumbnailsListPanels()`: すべてのThumbnailsListPanelを非表示にする

**実装例**:
```java
public class ApplicationController {
    private List<ViewerController> viewerControllers = new ArrayList<>();
    
    public List<ViewerController> getViewerControllers() {
        return new ArrayList<>(viewerControllers);
    }
    
    public void hideAllThumbnailsListPanels() {
        for (int i = 0; i < thumbnailsListPanels.length; i++) {
            if (thumbnailsListPanels[i] != null) {
                thumbnailsListPanels[i].setVisible(false);
            }
        }
    }
}
```

### 3. ViewerControllerの右クリック処理の修正

**修正箇所**: `ViewerController.createSeriesClickAdapter()`

**修正内容**:
```java
private MouseAdapter createSeriesClickAdapter(DicomDatabase.SeriesRecord series) {
    return new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            boolean rightClick = SwingUtilities.isRightMouseButton(e) || 
                               (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
            
            if (rightClick) {
                // 右クリックまたはControlキー: 新しいViewerControllerを作成
                List<Path> seriesImages = database.getImagePathsBySeriesId(series.getId());
                ViewerController newViewer = ApplicationController.getInstance().openViewerWindow(seriesImages);
                
                // ウィンドウ配置処理
                List<ViewerController> allViewers = ApplicationController.getInstance().getViewerControllers();
                if (AUTOTILING) {
                    WindowLayoutManager.tileWindows(allViewers);
                } else {
                    WindowLayoutManager.checkAllWindowsAreVisible(allViewers, true);
                }
                
                // すべてのThumbnailsListPanelを非表示にする
                ApplicationController.getInstance().hideAllThumbnailsListPanels();
                
                // 現在のViewerController（this）を前面に
                this.toFront();
                this.requestFocus();
                
                // 新しいViewerControllerのThumbnailsListPanelを表示
                ApplicationController.getInstance().showThumbnailsListPanel(newViewer);
            } else {
                // 左クリック: 現在のViewerControllerでシリーズを切り替え
                // （既存の実装）
            }
        }
    };
}
```

---

## 実装の優先順位

1. **優先度1**: `WindowLayoutManager`クラスの作成
   - `tileWindows()`メソッドの実装
   - `checkAllWindowsAreVisible()`メソッドの実装

2. **優先度2**: `ApplicationController`の拡張
   - `getViewerControllers()`メソッドの追加
   - `hideAllThumbnailsListPanels()`メソッドの追加

3. **優先度3**: `ViewerController`の右クリック処理の修正
   - 右クリック検出の実装
   - 新しいViewerControllerの作成
   - ウィンドウ配置処理の呼び出し
   - ThumbnailsListPanelの非表示/表示

---

## 参考ソースコード

### HOROS-20240407
- `horos-20240407/Horos/Sources/ViewerController.m`:
  - `loadSelectedSeries:rightClick:` (4272行目～)
  - `matrixPreviewPressed:` (4413行目～)
  - `newWindow:frame:` (2921行目～)
  - `windowWillUseStandardFrame:defaultFrame:` (3033行目～)
- `horos-20240407/Horos/Sources/AppController.m`:
  - `tileWindows:` (4857行目～)
  - `tileWindows:windows:display2DViewerToolbar:displayThumbnailsList:` (4915行目～)
  - `checkAllWindowsAreVisible:makeKey:` (4483行目～)
- `horos-20240407/Horos/Sources/ThumbnailsListPanel.m`:
  - `setThumbnailsView:viewer:`メソッド

---

## 変更履歴

- 2025年12月: 右クリック動作とtileWindows機能の実装仕様を統合したドキュメントを作成

