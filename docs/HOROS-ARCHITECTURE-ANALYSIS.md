# HOROS-20240407 アーキテクチャとUI構造の完全分析

## 概要

このドキュメントは、HOROS-20240407の完全なアーキテクチャとUI構造を徹底的に解析したものです。Java Swingへの移植のための詳細な設計情報を提供します。

---

## アーキテクチャの階層構造

### レベル1: アプリケーション層

```
AppController (NSApplication delegate)
├── BrowserController (データベースブラウザ)
├── ViewerController[] (複数のビューワー)
└── その他のコントローラー（3D、MPR、VR等）
```

### レベル2: ウィンドウコントローラー層

#### BrowserController
- **継承**: `NSWindowController`
- **役割**: データベースブラウザウィンドウの管理
- **主要コンポーネント**:
  - `databaseOutline` (MyOutlineView) - スタディ/シリーズ/画像の階層表示
  - `oMatrix` (BrowserMatrix) - サムネイルマトリックス表示
  - `thumbnailsScrollView` (NSScrollView) - サムネイルスクロールビュー
  - `splitViewHorz`, `splitViewVert` (NSSplitView) - レイアウト分割
  - `_bottomSplit` (NSSplitView) - 下部パネル分割
  - `imageView` (PreviewView) - プレビュー画像表示
  - `albumTable` (NSTableView) - アルバム一覧テーブル
  - `_sourcesTableView` (NSTableView) - ソース一覧テーブル

#### ViewerController
- **継承**: `OSIWindowController` → `NSWindowController`
- **役割**: DICOM画像ビューワーウィンドウの管理
- **主要コンポーネント**:
  - `studyView` (StudyView) - スタディビュー（複数シリーズのタイル表示）
  - `seriesView` (SeriesView) - シリーズビュー（複数画像のタイル表示）
  - `imageView` (DCMView) - 画像表示ビュー（OpenGL）
  - `previewMatrix` (NSMatrix) - サムネイルプレビューマトリックス
  - `previewMatrixScrollView` (NSScrollView) - サムネイルスクロールビュー
  - `splitView` (NSSplitView) - レイアウト分割

### レベル3: ビューレイヤー

#### StudyView
- **継承**: `NSView`
- **役割**: スタディ内の複数シリーズをタイル表示
- **構造**:
  - `seriesRows`, `seriesColumns` - タイルの行数・列数
  - `seriesViews` (NSMutableArray) - SeriesViewの配列

#### SeriesView
- **継承**: `NSView`
- **役割**: シリーズ内の複数画像をタイル表示
- **構造**:
  - `seriesRows`, `seriesColumns` - シリーズタイルの行数・列数
  - `imageRows`, `imageColumns` - 画像タイルの行数・列数
  - `imageViews` (NSMutableArray) - DCMViewの配列
  - `dcmPixList` (NSMutableArray) - DCMPixオブジェクトの配列
  - `dcmFilesList` (NSArray) - ファイルパスの配列
  - `dcmRoiList` (NSMutableArray) - ROIオブジェクトの配列

#### DCMView
- **継承**: `NSOpenGLView`
- **役割**: 個別のDICOM画像をOpenGLで表示
- **主要プロパティ**:
  - `dcmPixList` (NSMutableArray) - 現在表示中のDCMPix配列
  - `dcmFilesList` (NSArray) - ファイルパス配列
  - `dcmRoiList` (NSMutableArray) - ROI配列
  - `_curDCM` (DCMPix) - 現在表示中の画像
  - `curImage` (short) - 現在の画像インデックス
  - `imageRows`, `imageColumns` - タイル表示の行数・列数
  - `currentTool` (ToolMode) - 現在のツールモード

---

## UI遷移とウィンドウ構造

### 主要なウィンドウタイプ

HOROS-20240407では、以下の3つの主要なウィンドウタイプが分離されています：

1. **BrowserController** - データベースブラウザウィンドウ（スタディ一覧表示）
2. **ViewerController** - 画像ビューワーウィンドウ（DICOM画像表示）
3. **ThumbnailsListPanel** - サムネイルパネル（過去画像のサムネイル表示、フローティングウィンドウ）

### BrowserController のUI構造

```
BrowserController Window
├── splitViewHorz (水平分割)
│   ├── Left Panel (約20-25%)
│   │   ├── splitAlbums (垂直分割)
│   │   │   ├── albumTable (アルバム/フォルダ構造 - 左上)
│   │   │   └── _activityTableView (リアルタイムアクション表示 - 左下)
│   │   └── _sourcesTableView (ソース一覧)
│   │
│   └── Right Panel (約75-80%)
│       ├── splitViewVert (垂直分割)
│       │   ├── Top Panel
│       │   │   ├── searchView (検索ビュー)
│       │   │   └── modalityFilterView (モダリティフィルター)
│       │   │
│       │   └── Bottom Panel
│       │       ├── _bottomSplit (下部分割)
│       │       │   ├── oMatrix (サムネイルマトリックス - 約70-80%)
│       │       │   └── imageView (プレビュー画像 - 約20-30%)
│       │       └── thumbnailsScrollView (サムネイルスクロール)
│       │
│       └── databaseOutline (DB構造 - OutlineView - 右上、大部分)
```

#### レイアウト詳細

**SplitView構造**:
1. **splitViewHorz** (水平分割)
   - 左側: **splitAlbums** (垂直分割)
   - 右側: **splitViewVert** (垂直分割)

2. **splitAlbums** (垂直分割 - 左側)
   - 上: **albumTable** (アルバム/フォルダ構造)
   - 下: **_activityTableView** (リアルタイムアクション表示)

3. **splitViewVert** (垂直分割 - 右側)
   - 上: **databaseOutline** (DB構造 - OutlineView)
   - 下: **oMatrix** (サムネイル - Matrix)

**サイズ比率**:
- 左側（splitAlbums）: 約20-25%
- 右側（splitViewVert）: 約75-80%
  - 右上（databaseOutline）: 約70-80%
  - 下（oMatrix）: 約20-30%

#### OutlineView（databaseOutline）のカラム構成

HOROSでは`NSOutlineView`を使用して、複数のカラムで詳細な情報を表示しています。

**カラム識別子一覧**:

1. **name** - 患者名/シリーズ名
   - Study: 患者名
   - Series: シリーズ説明

2. **modality** - モダリティ
   - Study: 複数モダリティの表示
   - Series: モダリティ（CT, MR, CR, USなど）

3. **date** - 検査日（日付と時間を含む可能性）
   - Study: スタディ日付
   - Series: シリーズ日付
   - フォーマッター: dateTimeFormatter

4. **dateAdded** - 登録日
   - フォーマッター: dateTimeFormatter

5. **dateOpened** - 開いた日
   - フォーマッター: dateTimeFormatter

6. **dateOfBirth** - 生年月日
   - フォーマッター: dateFormatter

7. **patientID** - 患者ID

8. **yearOld** - 年齢（複数の表示モードあり）
   - Study: 患者年齢

9. **noSeries** - シリーズ数
   - Study: シリーズ数

10. **noFiles** - ファイル数
    - フォーマッター: decimalNumberFormatter

11. **studyName** - スタディ説明（Seriesの場合はseriesDescription）
    - Study: スタディ説明
    - Series: シリーズ説明

12. **referringPhysician** - 紹介医師

13. **performingPhysician** - 実施医師

14. **institutionName** - 施設名

15. **accessionNumber** - アクセッション番号

16. **reportURL** - レポートURL/日付
    - フォーマッター: dateFormatter

17. **stateText** - 状態テキスト

18. **lockedStudy** - ロック状態（Studyのみ）
    - Study: ロックアイコン

#### Matrix（oMatrix）の詳細

`BrowserMatrix`を使用して、サムネイルをグリッド表示しています。

- セルサイズ: 105x113 (デフォルト)
- サムネイル画像を表示
- ダブルクリックでViewerControllerを開く

### ViewerController のUI構造

```
ViewerController Window
├── splitView (レイアウト分割)
│   ├── Main Panel
│   │   ├── studyView (StudyView) - スタディビュー（複数シリーズ）
│   │   │   └── seriesViews[] (SeriesView[]) - シリーズビューの配列
│   │   │       └── imageViews[] (DCMView[]) - 画像ビューの配列
│   │   │
│   │   └── seriesView (SeriesView) - シリーズビュー（複数画像）
│   │       └── imageViews[] (DCMView[]) - 画像ビューの配列
│   │
│   └── Side Panel
│       └── previewMatrixScrollView (NSScrollView)
│           └── previewMatrix (NSMatrix) - サムネイルプレビュー
│
└── (ツールバー、メニューバー等)
```

### ThumbnailsListPanel

- **タイプ**: フローティングウィンドウ（`NSPanel`）
- **役割**: 過去画像のサムネイルを表示
- **特徴**: 
  - 各スクリーン用のインスタンス（`thumbnailsListPanel[MAXSCREENS]`）
  - ビューワーと関連付け（`setThumbnailsView:viewer:`）
  - 常に最前面表示

---

## データフロー

### 1. データベースからビューワーへのデータフロー

```
DicomDatabase
  ↓
BrowserController.databaseOutline (スタディ選択)
  ↓
BrowserController.displayStudy:object:command:
  ↓
BrowserController.loadSeries:viewer:firstViewer:keyImagesOnly:
  ↓
BrowserController.openViewerFromImages:movie:viewer:keyImagesOnly:
  ↓
BrowserController.processOpenViewerDICOMFromArray:movie:viewer:
  ↓
[画像データの読み込み]
  - DCMPix配列の作成
  - ファイルパス配列の作成
  - ボリュームデータの準備
  ↓
ViewerController.newWindow:pixList:fileList:volumeData:
  ↓
ViewerController.initWithPix:withFiles:withVolume:
  ↓
ViewerController.viewerControllerInit
  ↓
SeriesView.setDCM:files:rois:firstImage:type:reset:
  ↓
DCMView.setPixels:files:rois:firstImage:level:reset:
  ↓
DCMView.drawRect: (OpenGL描画)
```

### 2. ビューワー内のデータフロー

```
ViewerController
  ├── studyView (StudyView)
  │   └── seriesViews[] (SeriesView[])
  │       └── imageViews[] (DCMView[])
  │           ├── dcmPixList (DCMPix[])
  │           ├── dcmFilesList (NSString[])
  │           └── dcmRoiList (ROI[])
  │
  └── previewMatrix (NSMatrix)
      └── サムネイル表示
```

---

## 主要メソッド

### BrowserController

```objective-c
// スタディを表示（選択またはビューワーを開く）
- (BOOL) displayStudy: (DicomStudy*) study 
               object:(NSManagedObject*) element 
             command:(NSString*) execute;

// 画像配列からビューワーを開く
- (ViewerController*) openViewerFromImages:(NSArray*) toOpenArray 
                                      movie:(BOOL) movieViewer 
                                     viewer:(ViewerController*) viewer 
                            keyImagesOnly:(BOOL) keyImages;

// DICOM画像配列からビューワーを開く処理
- (void) processOpenViewerDICOMFromArray:(NSArray*) toOpenArray 
                                   movie:(BOOL) movieViewer 
                                  viewer: (ViewerController*) viewer;
```

### ViewerController

```objective-c
// 新しいビューワーウィンドウを作成
+ (ViewerController*) newWindow:(NSArray*) pixList 
                        fileList:(NSArray*) fileList 
                     volumeData:(id) volumeData;

// ビューワーの初期化
- (id) initWithPix:(NSMutableArray*) pix 
          withFiles:(NSMutableArray*) files 
        withVolume:(NSData*) volumeData;

// サムネイルを構築
- (void) buildMatrixPreview: (NSArray*) dicomImages;

// 画像の行数・列数を設定（タイル表示）
- (void) setImageRows:(NSInteger) rows columns:(NSInteger) columns;
```

---

## 参考実装

### HOROS-20240407
- `horos-20240407/Horos/Sources/BrowserController.h` / `.m`
  - ブラウザウィンドウの実装
  - スタディ表示、ビューワーへの遷移
- `horos-20240407/Horos/Sources/ViewerController.h` / `.m`
  - ビューワーウィンドウの実装
  - `buildMatrixPreview:` (line 4903)
  - `setImageRows:columns:` (line 3584)
- `horos-20240407/Horos/Sources/ThumbnailsListPanel.h`
  - フローティングウィンドウの実装

---

---

## アプリケーション起動時の構造

### ウィンドウ遷移フロー

```
アプリケーション起動
  ↓
スプラッシュスクリーン表示
  ↓
BrowserControllerウィンドウ表示（メインウィンドウ）
  ↓
スタディ/シリーズ選択
  ↓
ViewerControllerウィンドウを開く（別ウィンドウ）
  ↓
画像表示・操作
```

### AppController（アプリケーション制御）

- **役割**: アプリケーション全体のライフサイクル管理
- **初期化**: 
  - スプラッシュスクリーン表示
  - BrowserControllerの作成と表示
  - メニューバーの管理

---

## 変更履歴

- 2024年: UI遷移とアーキテクチャ解析ドキュメント（35, 36, 37, 38, 45, 46）をこのドキュメントに統合

