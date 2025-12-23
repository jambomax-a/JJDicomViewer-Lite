# HOROS-20240407 tileWindows完全仕様

## 概要

このドキュメントは、HOROS-20240407の`tileWindows`機能を包括的に解析し、1〜30枚までのビューワー配置の詳細仕様をまとめたものです。

**最終更新**: 2025年12月

---

## 目次

1. [displayViewersメソッドの実装](#displayviewersメソッドの実装)
2. [tileWindowsメソッドの行数・列数計算](#tilewindowsメソッドの行数列数計算)
3. [各枚数での配置パターン](#各枚数での配置パターン)
4. [最大ビューワー数について](#最大ビューワー数について)
5. [実装上の注意点](#実装上の注意点)

---

## displayViewersメソッドの実装

### 実装箇所（AppController.m:4518-4591）

### 重要な計算ロジック

```objective-c
- (void) displayViewers: (NSArray*) viewers monitorIndex: (int) monitorIndex screens: (NSArray*) screens numberOfMonitors:(int) numberOfMonitors rowsPerScreen:(int) rowsPerScreen columnsPerScreen:(int) columnsPerScreen
{
    // 1. 有効領域を取得
    NSRect frame = [AppController usefullRectForScreen: [screens objectAtIndex: monitorIndex]];
    
    // 2. 隙間をなくすための計算（整数除算で端数を切り捨て）
    int temp;
    
    temp = frame.size.width / columnsPerScreen;
    frame.size.width = temp * columnsPerScreen;  // 端数を切り捨てた幅
    
    temp = frame.size.height / rowsPerScreen;
    frame.size.height = temp * rowsPerScreen;  // 端数を切り捨てた高さ
    
    // 3. 各ウィンドウのサイズを計算
    NSRect visibleFrame = frame;  // 後で使用するため保存
    frame.size.width /= columnsPerScreen;  // 各ウィンドウの幅
    frame.size.height /= rowsPerScreen;  // 各ウィンドウの高さ
    
    // 4. 各ウィンドウの位置を計算
    for (int i = 0; i < [viewers count]; i++) {
        int posInScreen = i % (columnsPerScreen * rowsPerScreen);
        int row = posInScreen / columnsPerScreen;
        int column = posInScreen % columnsPerScreen;
        
        // X位置: frame.origin.xから開始して、各ウィンドウの幅 * column
        int x = frame.origin.x + (frame.size.width * column);
        
        // Y位置: frame.origin.yから開始して、下から上に配置
        // row=0が一番下、row=rowsPerScreen-1が一番上
        int y = frame.origin.y + (frame.size.height * ((rowsPerScreen - 1) - row));
        
        NSRect windowFrame = NSMakeRect(x, y, frame.size.width, frame.size.height);
        [[viewers objectAtIndex:i] setWindowFrame:windowFrame showWindow:YES animate: YES];
    }
}
```

### 重要なポイント

1. **隙間をなくす計算**: 
   - `temp = frame.size.width / columnsPerScreen; frame.size.width = temp * columnsPerScreen;`
   - 整数除算で端数を切り捨て、その後、各ウィンドウのサイズを計算

2. **位置計算**:
   - X位置: `frame.origin.x + (frame.size.width * column)`
   - Y位置: `frame.origin.y + (frame.size.height * ((rowsPerScreen - 1) - row))`
   - 下から上に配置（row=0が一番下）

3. **setWindowFrameでの丸め処理**（ViewerController.m:3054-3057）:
   ```objective-c
   rect.origin.x =  roundf( rect.origin.x);
   rect.origin.y =  roundf(rect.origin.y);
   rect.size.width =  roundf(rect.size.width);
   rect.size.height =  roundf(rect.size.height);
   ```

---

## tileWindowsメソッドの行数・列数計算

### 基本的な計算ロジック（AppController.m:5091-5203）

```objective-c
// 画面のアスペクト比を取得
BOOL landscape = (screenRect.size.width/screenRect.size.height > 1) ? YES : NO;

float landscapeRatio = 1.5;
if( screenRect.size.width/screenRect.size.height > 1.7) // 16/9 screen or more
    landscapeRatio = 2.0;

float portraitRatio = 0.9;
if( screenRect.size.height/screenRect.size.width > 1.7) // 16/9 screen or more
    portraitRatio = 0.49;

// デフォルトの行数と列数
int rows = [[WindowLayoutManager sharedWindowLayoutManager] windowsRows];
int columns = [[WindowLayoutManager sharedWindowLayoutManager] windowsColumns];

// ビューワー数が多い場合の調整
if( viewerCount > (rows * columns))
{
    float ratioValue = landscape ? landscapeRatio : portraitRatio;
    float viewerCountPerScreen = (float) viewerCount / (float) numberOfMonitors;
    int columnsPerScreen = ceil( (float) columns / (float) numberOfMonitors);
    
    while (viewerCountPerScreen > (rows * columnsPerScreen))
    {
        float ratio = (float) columnsPerScreen / (float) rows;
        
        if (ratio > ratioValue)
            rows ++;
        else 
            columnsPerScreen ++;
    }
    
    columns = columnsPerScreen * numberOfMonitors;
}
```

### 最適化処理（AppController.m:5194-5200）

特定の条件で、より正方形に近い配置を選択する処理が実行されます。

---

## 各枚数での配置パターン

### 1〜10枚の配置

- **1枚**: 1行×1列
- **2枚**: 1行×2列（横並び）
- **3枚**: 1行×3列（横並び）
- **4枚**: 2行×2列（完全なグリッド）
- **5枚**: 2行×2列（4枚）+ 1枚（縦長、strechWindows）
- **6枚**: 2行×3列
- **7枚**: 3行×3列（2列目に1枚追加）
- **8枚**: 2行×4列
- **9枚**: 2行×5列（詳細は[9枚の配置分析](#9枚の配置分析)を参照）
- **10枚**: 2行×5列（完全なグリッド）

### 9枚の配置分析

#### 計算過程

**初期値**:
- landscape = true (16/9以上の画面を想定)
- rows = 1
- columns = 2
- viewerCount = 9
- numberOfMonitors = 1

**whileループ（AppController.m:5177-5192）**:

1. **イテレーション1**: 
   - 9 > (1 * 2) = 9 > 2 → true
   - ratio = 2 / 1 = 2.0
   - landscapeRatio = 2.0 の場合、ratio > landscapeRatio は false（等しい）
   - 等しい場合は columnsPerScreen++ → columnsPerScreen = 3

2. **イテレーション2**: 
   - 9 > (1 * 3) = 9 > 3 → true
   - ratio = 3 / 1 = 3.0
   - ratio > landscapeRatio (2.0) → rows++ → rows = 2

3. **イテレーション3**: 
   - 9 > (2 * 3) = 9 > 6 → true
   - ratio = 3 / 2 = 1.5
   - landscapeRatio = 2.0 の場合、ratio (1.5) < landscapeRatio (2.0) → columnsPerScreen++ → columnsPerScreen = 4

4. **イテレーション4**: 
   - 9 > (2 * 4) = 9 > 8 → true
   - ratio = 4 / 2 = 2.0
   - landscapeRatio = 2.0 の場合、ratio > landscapeRatio は false（等しい）
   - 等しい場合は columnsPerScreen++ → columnsPerScreen = 5

5. **イテレーション5**: 
   - 9 > (2 * 5) = 9 > 10 → false → 終了

**最適化処理（AppController.m:5194-5200）**:
- rows * columnsPerScreen = 2 * 5 = 10 > 9
- rows * (columnsPerScreen - 1) = 2 * 4 = 8 ≠ 9 → 条件不成立
- columnsPerScreen * (rows - 1) = 5 * 1 = 5 ≠ 9 → 条件不成立

**結果**: rows = 2, columnsPerScreen = 5, columns = 5

**実際の配置**:
- 1枚目: row=0, col=0
- 2枚目: row=0, col=1
- 3枚目: row=0, col=2
- 4枚目: row=0, col=3
- 5枚目: row=0, col=4
- 6枚目: row=1, col=0
- 7枚目: row=1, col=1
- 8枚目: row=1, col=2
- 9枚目: row=1, col=3（strechWindows処理により縦長になる可能性）

### 11〜20枚の配置（landscapeRatio = 1.5の場合）

#### 11枚の場合
- **結果**: 3行×4列（12枚分、11枚目が縦長）

#### 12枚の場合
- **結果**: 3行×4列（完全なグリッド）

#### 13枚の場合
- **結果**: 3行×5列（15枚分、13〜15枚目が縦長）

#### 14枚の場合
- **結果**: 3行×5列（15枚分、14〜15枚目が縦長）

#### 15枚の場合
- **結果**: 3行×5列（完全なグリッド）

#### 16枚の場合
- **結果**: 4行×4列（完全なグリッド）

#### 17枚の場合
- **結果**: 4行×5列（20枚分、17〜20枚目が縦長）

#### 18枚の場合
- **結果**: 4行×5列（20枚分、18〜20枚目が縦長）

#### 19枚の場合
- **結果**: 4行×5列（20枚分、19〜20枚目が縦長）

#### 20枚の場合
- **結果**: 4行×5列（完全なグリッド）

### 21〜30枚の配置（landscapeRatio = 1.5の場合）

#### 21枚の場合
- **期待**: 4行×6列 または 5行×5列（より正方形に近い）

#### 22枚の場合
- **期待**: 4行×6列（24枚分、22〜24枚目が縦長）

#### 23枚の場合
- **期待**: 4行×6列（24枚分、23〜24枚目が縦長）

#### 24枚の場合
- **期待**: 4行×6列（完全なグリッド）

#### 25枚の場合
- **期待**: 5行×5列（完全なグリッド）

#### 26枚の場合
- **期待**: 5行×6列（30枚分、26〜30枚目が縦長）

#### 27枚の場合
- **期待**: 5行×6列（30枚分、27〜30枚目が縦長）

#### 28枚の場合
- **期待**: 5行×6列（30枚分、28〜30枚目が縦長）

#### 29枚の場合
- **期待**: 5行×6列（30枚分、29〜30枚目が縦長）

#### 30枚の場合
- **期待**: 5行×6列（完全なグリッド）

### 重要なポイント

1. **ratio == ratioValue の場合**: HOROS-20240407では `else` 節に入るため、`columnsPerScreen++` が実行される
2. **最適化処理**: 特定の条件（rows*(columnsPerScreen-1) == viewerCount など）でのみ動作
3. **strechWindows処理**: displayViewers内で、最後のスクリーンの残りを縦長に配置
4. **自動計算**: 任意の枚数に対して、whileループと最適化処理により自動的に最適な行数・列数を決定

---

## 最大ビューワー数について

### 実装上の制限

HOROS-20240407のコード（AppController.m:5472-5473）では、以下の条件で"NO tiling"が表示されます：

```objective-c
else
    NSLog(@"NO tiling");
```

これは、以下の条件分岐の最後のelse節です：
1. viewerCount <= numberOfMonitors
2. (viewerCount <= columns) && (viewerCount % numberOfMonitors == 0)
3. viewerCount <= columns
4. viewerCount <= columns * rows
5. **else** → "NO tiling"

### 理論上の上限

しかし、**調整処理**（AppController.m:5159-5203）により、`viewerCount > (rows * columns)` の場合に動的に行数・列数が増やされるため、**理論的には上限はありません**。

実際の制限は：
1. **画面サイズ**: ウィンドウの最小サイズにより、実用的な上限が決まる
2. **メモリ**: 多数のビューワーを同時に開くとメモリ消費が増える
3. **パフォーマンス**: ウィンドウ数が多すぎると描画性能が低下する

### 実用的な上限

通常の使用では、10〜20枚程度までが実用的な範囲と考えられます。

---

## 実装上の注意点

1. **整数除算による端数の切り捨て**: 隙間をなくすため、必ず整数除算を使用
2. **位置の丸め**: `setWindowFrame`で位置とサイズを`roundf`で丸める
3. **下から上への配置**: Y位置の計算で`((rowsPerScreen - 1) - row)`を使用
4. **strechWindows処理**: 奇数枚数の場合、最後のウィンドウを縦長に配置

---

## 参考実装箇所

### HOROS-20240407ソースコード

- **AppController.m:4518-4591**: `displayViewers`メソッド
- **AppController.m:5091-5203**: `tileWindows`メソッドの行数・列数計算
- **AppController.m:5194-5200**: 最適化処理
- **ViewerController.m:3054-3057**: `setWindowFrame`での丸め処理

### Java実装

- **WindowLayoutManager.java**: `tileWindows`メソッド
- **WindowLayoutManager.java**: `displayViewers`メソッド

---

## 変更履歴

- 2025年12月: HOROS Tiling関連ドキュメントを統合（5ファイル → 1ファイル）
- 2024年12月: 初期解析とドキュメント作成
