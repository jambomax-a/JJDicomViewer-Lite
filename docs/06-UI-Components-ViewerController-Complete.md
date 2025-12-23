# UIコンポーネント - ViewerController完全版

## 概要

このドキュメントは、ViewerController（ビューワーウィンドウ）の実装状況、機能仕様、操作方法を統合した完全版ドキュメントです。HOROS-20240407の実装を参考に、Java Swingで実装されています。

最終更新: 2025年12月

---

## 目次

1. [ViewerControllerの基本機能](#viewercontrollerの基本機能)
2. [ウィンドウ操作](#ウィンドウ操作)
3. [画像表示と操作](#画像表示と操作)
4. [複数ビューワーの配置（tileWindows）](#複数ビューワーの配置tilewindows)
5. [実装状況](#実装状況)
6. [今後の実装計画](#今後の実装計画)

---

## ViewerControllerの基本機能

### 機能概要

- 複数画像の表示
- スライダーによる画像ナビゲーション
- ツールバー（Window Level/Width、ズーム等）
- キーボード・マウス操作
- ウィンドウの最大化/復元
- 複数ビューワーの自動配置

### 基本構造

```java
public class ViewerController extends JFrame {
    private ImageViewerPanel imageView;
    private JSlider imageSlider;
    private List<Path> imageList;
    private int currentIndex = 0;
    
    // ウィンドウ最大化状態の管理
    private boolean isMaximized = false;
    private Rectangle normalBounds = null;
    
    // 現在表示中のスタディとシリーズ情報
    private DicomDatabase.StudyRecord currentStudy;
    private DicomDatabase.SeriesRecord currentSeries;
}
```

---

## ウィンドウ操作

### 1. ダブルクリックで最大化/復元

#### 機能仕様

- **画像をダブルクリックで最大化**: 画像表示パネル（`ImageViewerPanel`）上で左クリックをダブルクリックすると、ウィンドウが最大化されます
- **最大化画像をダブルクリックで元のサイズに戻す**: 最大化されたウィンドウ上で再度ダブルクリックすると、元のサイズと位置に戻ります
- **修飾キーの扱い**: SHIFT、CTRL、ALT、METAキーが押されている場合は最大化処理を行いません

#### 実装詳細

**ImageViewerPanel.java** - ダブルクリックの検出:

```java
@Override
public void mouseClicked(MouseEvent e) {
    // ダブルクリックでウィンドウを最大化/元に戻す
    if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
        // 修飾キーが押されていない場合のみ処理
        boolean hasModifiers = (e.getModifiersEx() & (
            InputEvent.SHIFT_DOWN_MASK | 
            InputEvent.CTRL_DOWN_MASK | 
            InputEvent.ALT_DOWN_MASK | 
            InputEvent.META_DOWN_MASK)) != 0;
        if (!hasModifiers) {
            toggleWindowMaximize();
        }
    }
}
```

**ViewerController.java** - 最大化/復元処理:

```java
public void toggleMaximize() {
    if (isMaximized) {
        // 元のサイズに戻す
        if (normalBounds != null) {
            setBounds(normalBounds);
        }
        setExtendedState(JFrame.NORMAL);
        isMaximized = false;
    } else {
        // 最大化前のサイズと位置を保存
        normalBounds = getBounds();
        // ウィンドウを最大化
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        isMaximized = true;
    }
}
```

#### HOROS-20240407との対応

HOROS-20240407の`DCMView.m`では、`mouseDown:`メソッド内で`clickCount == 2`をチェックしてダブルクリックを検出しています。本実装では、Java Swingの`MouseAdapter.mouseClicked()`を使用して同様の機能を実現しています。

**参考実装箇所**:
- `horos-20240407/Horos/Sources/DCMView.m:4220-4334`
- `clickCount == 2`でダブルクリックを検出
- 修飾キーのチェックも同様に実装

---

### 2. 右クリックで新しいビューワーを開く

#### 機能仕様

- **右クリックの検出**: サムネイルセルを右クリックすると、新しいViewerControllerが作成されます
- **新しいViewerControllerの作成**: `openViewerWindow(seriesImages, true)`が呼び出され、`skipPositioning=true`で初期配置をスキップします
- **自動配置**: `AUTOTILING`が有効な場合、`tileWindows`が呼び出されて複数のビューワーが自動的に配置されます

#### 実装詳細

**ViewerController.java** - 右クリック処理:

```java
private MouseListener createSeriesClickAdapter(DicomDatabase.SeriesRecord series) {
    return new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            boolean rightClick = SwingUtilities.isRightMouseButton(e);
            
            if (rightClick) {
                // 新しいViewerControllerを作成
                List<Path> seriesImages = database.getImagePathsBySeries(series.getId());
                ApplicationController.getInstance().openViewerWindow(seriesImages, true);
            } else {
                // 既存のViewerControllerでシリーズを切り替え
                // ...
            }
        }
    };
}
```

**ApplicationController.java** - 新しいビューワーの作成:

```java
public void openViewerWindow(List<Path> imagePaths, boolean skipPositioning) {
    ViewerController viewer = new ViewerController(imagePaths);
    
    if (!skipPositioning && WindowLayoutManager.isAutoTiling()) {
        // 自動配置をスキップ
    }
    
    viewer.setVisible(true);
    
    if (WindowLayoutManager.isAutoTiling()) {
        // すべてのビューワーを再配置
        WindowLayoutManager.tileWindows(new ArrayList<>(viewerControllers));
    }
}
```

---

## 画像表示と操作

### キーボード操作

| 操作 | キー | 説明 |
|------|------|------|
| 画像移動 | ↑/↓ | 前/次の画像に移動 |
| 画像移動 | Page Up/Down | 前/次の画像に移動 |
| 画像移動 | Home/End | 最初/最後の画像に移動 |
| ズーム | +/- | ズームイン/アウト |
| ズーム | 0 | フィット表示 |
| 回転 | R | 右回転（90度） |
| 回転 | L | 左回転（90度） |
| 反転 | H | 左右反転 |
| 反転 | V | 上下反転 |

### マウス操作

| 操作 | 動作 | 説明 |
|------|------|------|
| パン | 左クリック + ドラッグ | 画像を移動 |
| Window Level/Width調整 | SHIFT + 左クリック + ドラッグ | 縦方向=Window Center、横方向=Window Width |
| ズーム | CTRL + マウスホイール | ズームイン/アウト |
| シリーズ移動 | マウスホイール | 前/次の画像に移動 |
| 最大化/復元 | ダブルクリック | ウィンドウを最大化/元に戻す |

### Window Level/Width調整

- **SHIFT + 左クリック + ドラッグ**: 縦方向でWindow Center、横方向でWindow Widthを調整
- **テキストボックス**: 手入力で数値を入力して反映
- **自動同期**: 画像変更時や調整時にテキストボックスが自動更新されます

### モダリティ別の調整

- **CR画像**: パン操作の感度2.5倍、WW/WL調整の感度1.0
- **US画像**: WW感度0.1、WL感度0.5
- **その他**: 標準感度（1.0）

---

## 複数ビューワーの配置（tileWindows）

### 機能概要

複数のViewerControllerウィンドウを自動的に配置する機能です。HOROS-20240407の`tileWindows`機能を再現しています。

### AUTOTILING設定

- **設定名**: `AUTOTILING`（現在は常に`true`）
- **動作**: 有効な場合、ビューワーが開かれたり閉じられたりしたときに自動的に配置されます
- **実装箇所**: `WindowLayoutManager.isAutoTiling()`（常に`true`を返す）

### 右クリック時の処理フロー

**実装箇所**: `ViewerController.createSeriesClickAdapter()`

1. **新しいViewerControllerを作成**
   ```java
   ApplicationController.getInstance().openViewerWindow(seriesImages, true);
   ```
   - `skipPositioning=true`で初期配置をスキップ

2. **ウィンドウ配置処理**
   ```java
   if (WindowLayoutManager.isAutoTiling()) {
       WindowLayoutManager.tileWindows(new ArrayList<>(viewerControllers));
   }
   ```
   - `AUTOTILING == true`: `tileWindows`を呼び出して、複数のビューワーを自動的に並べて配置
   - `AUTOTILING == false`: `checkAllWindowsAreVisible`を呼び出して、すべてのウィンドウが表示されるように配置（重なりを避ける）

3. **すべてのThumbnailsListPanelを非表示にする**
   - 右クリックで新しいビューワーを作成したときは、既存のThumbnailsListPanelを非表示にする
   - （現在は未実装）

### 配置ロジック

#### ビューワー数の計算

HOROS-20240407の`AppController.m:5342-5473`を参考に、ビューワー数に応じて行数と列数を計算します。

**主要な配置パターン**:

- **1枚**: 1x1
- **2枚**: 1x2（横並び）
- **3枚**: 1x3（横並び）
- **4枚**: 2x2
- **5枚**: 4枚を2x2グリッド、1枚を縦に伸ばして配置（strechWindows）
- **6枚**: 2x3
- **7枚**: 3x3（2列目に1枚追加）
- **8枚**: 2x4
- **9枚**: 3x3
- **10枚以上**: 画面アスペクト比に応じて最適な配置を計算
- **11-30枚**: 自動的に最適な配置を計算

**詳細な配置パターン**:

詳細な配置パターンについては、以下のドキュメントを参照してください：
- `HOROS-TILEWINDOWS-DETAILED-SPECIFICATION.md`: 基本的な配置ロジック
- `HOROS-TILEWINDOWS-9-VIEWERS-ANALYSIS.md`: 9枚の配置の詳細
- `HOROS-TILEWINDOWS-11-20-VIEWERS-ANALYSIS.md`: 11-20枚の配置の詳細
- `HOROS-TILEWINDOWS-21-30-VIEWERS-ANALYSIS.md`: 21-30枚の配置の詳細

#### 隙間のない配置

**WindowLayoutManager.java** - `displayViewers`メソッド:

HOROS-20240407の`AppController.m:4518-4591`を参考に実装しています。

```java
private static void displayViewers(List<ViewerController> viewers, 
                                    Rectangle frame,
                                    int rowsPerScreen, 
                                    int columnsPerScreen) {
    // 1. 有効領域を取得
    Rectangle visibleFrame = frame;
    
    // 2. 隙間をなくすための計算（整数除算で端数を切り捨て）
    int totalWidth = (frame.width / columnsPerScreen) * columnsPerScreen;
    int totalHeight = (frame.height / rowsPerScreen) * rowsPerScreen;
    
    // 3. 各ウィンドウのサイズを計算
    int windowWidth = totalWidth / columnsPerScreen;
    int windowHeight = totalHeight / rowsPerScreen;
    
    // 4. 各ウィンドウの位置を計算（下から上に配置）
    for (int i = 0; i < viewers.size(); i++) {
        int posInScreen = i % (columnsPerScreen * rowsPerScreen);
        int row = posInScreen / columnsPerScreen;
        int column = posInScreen % columnsPerScreen;
        
        // X位置: frame.xから開始して、各ウィンドウの幅 * column
        int x = Math.round(frame.x + (windowWidth * column));
        
        // Y位置: frame.yから開始して、下から上に配置
        // row=0が一番下、row=rowsPerScreen-1が一番上
        int y = Math.round(frame.y + (windowHeight * ((rowsPerScreen - 1) - row)));
        
        Rectangle windowBounds = new Rectangle(x, y, windowWidth, windowHeight);
        viewers.get(i).setBounds(windowBounds);
    }
    
    // 5. 隙間を完全に埋めるための後処理（最大3回繰り返し）
    // 隣接するウィンドウ間の隙間を検出して修正
    for (int pass = 0; pass < 3; pass++) {
        boolean gapsFound = false;
        // 隙間検出と修正ロジック
        // ...
        if (!gapsFound) break;
    }
}
```

**重要なポイント**:

1. **隙間をなくす計算**: 
   - `totalWidth = (frame.width / columnsPerScreen) * columnsPerScreen`
   - 整数除算で端数を切り捨て、その後、各ウィンドウのサイズを計算

2. **位置の計算**:
   - X位置: `frame.x + (windowWidth * column)`
   - Y位置: `frame.y + (windowHeight * ((rowsPerScreen - 1) - row))`（下から上に配置）

3. **丸め処理**:
   - `Math.round()`を使用して最終的な位置とサイズを丸める（HOROSの`roundf`に相当）

4. **隙間の完全な除去**:
   - 後処理で隣接するウィンドウ間の隙間を検出して修正
   - 最大3回繰り返して完全に隙間を埋める

#### 奇数枚数の特殊処理（strechWindows）

HOROS-20240407の`AppController.m:4534-4554`を参考に実装しています。

5枚の場合など、グリッドに収まらない場合は、最後のウィンドウを縦に伸ばして配置します。

```java
if (viewers.size() == 5 && rowsPerScreen >= 2 && columnsPerScreen >= 2) {
    // 最初の4枚を2x2グリッドに配置
    for (int i = 0; i < 4; i++) {
        int row = i / 2;
        int column = i % 2;
        int x = frame.x + (windowWidth * column);
        int y = frame.y + (windowHeight * ((rowsPerScreen - 1) - row));
        Rectangle windowBounds = new Rectangle(x, y, windowWidth, windowHeight);
        viewers.get(i).setBounds(windowBounds);
    }
    
    // 5枚目を縦に伸ばして配置（右側の列）
    int stretchedHeight = windowHeight * 2;
    int x = frame.x + (windowWidth * (columnsPerScreen - 1));
    int y = frame.y;
    Rectangle windowBounds = new Rectangle(x, y, windowWidth, stretchedHeight);
    viewers.get(4).setBounds(windowBounds);
}
```

**strechWindowsの適用条件**:
- ビューワー数が奇数
- `rowsPerScreen >= 2`かつ`columnsPerScreen >= 2`
- 最後のウィンドウを縦に伸ばして配置することで、グリッドに収まらない場合でも隙間なく配置

### ビューワーが閉じられたときの処理

**ApplicationController.java** - `onViewerClosed`:

```java
public void onViewerClosed(ViewerController viewer) {
    viewerControllers.remove(viewer);
    
    if (WindowLayoutManager.isAutoTiling()) {
        // 300ms後に再配置（HOROS-20240407のperformSelector:withObject:afterDelay: 0.3に相当）
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(300);
                SwingUtilities.invokeLater(() -> {
                    WindowLayoutManager.tileWindows(new ArrayList<>(viewerControllers));
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
```

---

## 実装状況

### ✅ 実装済み機能

1. **キーボード・マウス操作**
   - 画像移動（↑/↓、Page Up/Down、Home/End）
   - ズーム（+/-, 0、マウスホイール）
   - 回転・反転（R, L, H, V）
   - パン（左クリック + ドラッグ）
   - Window Level/Width調整（SHIFT + 左クリック + ドラッグ）

2. **WW/WLテキストボックスの同期**
   - 画像変更時にテキストボックスを自動更新
   - 右クリック + ドラッグでWW/WL調整時にテキストボックスを自動更新
   - 手入力で数値を入力して反映

3. **画像表示の改善**
   - RGBカラー画像の表示対応（カラーを保持）
   - 位置ズレの修正（Weasisのアプローチを採用）
   - 座標変換の正確な実装

4. **ウィンドウ操作**
   - ダブルクリックで最大化/復元
   - 右クリックで新しいビューワーを開く
   - 複数ビューワーの自動配置（tileWindows）

5. **複数ビューワーの配置**
   - 1-30枚までのビューワー配置ロジック
   - 隙間のない配置
   - 奇数枚数の特殊処理（strechWindows）

### ⚠️ 現在の問題点

1. **CT/MRの初期表示が白飛びしている**
   - Weasis方式の実装を追加（改善中）

2. **USのWW/WLが不安定**
   - RGB画像に対する変換テーブルの計算を修正（改善中）

### 🔍 確認待ち項目

- CR画像のパン反応性（現在は感度2.5倍）

---

## 今後の実装計画

### 優先度高

1. **過去画像のサムネイル表示**
   - ViewerController内のサムネイル表示（`previewMatrixScrollView`相当）
   - ThumbnailsListWindow（フローティングウィンドウ）

2. **ウィンドウサイズの自動調整**
   - 画面解像度に合わせて自動調整
   - 最小サイズ・最大サイズの設定

### 優先度中

3. **複数画像表示**
   - タイル表示機能（`setImageRows:columns:`相当）
   - 同期操作（ズーム・パン・WL/WWを同期）

### 優先度低

4. **その他の機能**
   - アノテーション機能
   - 測定機能
   - キーボードショートカットの追加（F11で最大化など）

---

## 関連ファイル

### 主要な実装ファイル

- `src/main/java/com/jj/dicomviewer/ui/ViewerController.java`
  - ビューワーウィンドウの実装
  - ウィンドウの最大化/復元処理
  - 右クリック処理

- `src/main/java/com/jj/dicomviewer/ui/ImageViewerPanel.java`
  - 画像表示パネル
  - マウス・キーボード操作
  - ダブルクリックの検出

- `src/main/java/com/jj/dicomviewer/ui/WindowLayoutManager.java`
  - 複数ビューワーの自動配置
  - `tileWindows`メソッド
  - `displayViewers`メソッド

- `src/main/java/com/jj/dicomviewer/ApplicationController.java`
  - ビューワーの作成と管理
  - `openViewerWindow`メソッド
  - `onViewerClosed`メソッド

### 参考実装（HOROS-20240407）

- `horos-20240407/Horos/Sources/ViewerController.m`
  - ビューワーウィンドウの実装
  - `loadSelectedSeries:rightClick:`メソッド
  - `matrixPreviewPressed:`メソッド

- `horos-20240407/Horos/Sources/DCMView.m`
  - 画像表示とマウス操作
  - `mouseDown:`メソッド（ダブルクリック検出）
  - `mouseDragged:`メソッド

- `horos-20240407/Horos/Sources/AppController.m`
  - `tileWindows:`メソッド
  - `displayViewers:`メソッド
  - `usefullRectForScreen:`メソッド

---

## 変更履歴

- 2025年12月: ダブルクリック最大化機能を追加、ドキュメントを統合
- 2024年12月: 複数ビューワーの自動配置機能を実装
- 2024年: ViewerControllerの基本実装を完了

