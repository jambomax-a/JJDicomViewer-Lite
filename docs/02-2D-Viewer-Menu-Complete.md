# HOROS-20240407「2D Viewer」メニュー完全統合ドキュメント

## 概要

このドキュメントは、HOROS-20240407の「2D Viewer」メニュー機能を包括的に解析し、Java実装に向けた完全な仕様をまとめたものです。

**最終更新**: 2025年1月

---

## 目次

1. [デフォルト同期設定](#デフォルト同期設定)
2. [ビューアウィンドウ側「2D Viewer」メニュー](#ビューアウィンドウ側2d-viewerメニュー)
3. [ブラウザー/ツールウィンドウ側「2D Viewer」メニュー](#ブラウザーツールウィンドウ側2d-viewerメニュー)
4. [設定伝播・同期機能の詳細](#設定伝播同期機能の詳細)
5. [実装状況と優先度](#実装状況と優先度)
6. [参考実装箇所](#参考実装箇所)

---

## デフォルト同期設定

### 根拠（HOROS-20240407ソースコード）

- **グローバル自動伝播 `COPYSETTINGS`**: 既定値 **ON**（`DefaultsOsiriX.m:1243` = @"1"）
- **シリーズ内コピー `COPYSETTINGSINSERIES`**: 既定値 **YES**（`DCMView.m:6486` = YES）
- **メニュー紐づけ**: MainMenu.xib の「2D Viewer」で `values.COPYSETTINGS` バインド、`switchCopySettingsInSeries:` でトグル
- **手動コピー**: `copySettingsToOthers:`（ViewerController.m:12984付近）
- **自動伝播ロジック**: `propagateSettings` / `propagateSettingsToViewer`（ViewerController.m:16613付近）

### Java実装での対応

- `globalCopySettings`: デフォルト `true`（ON）
- `copySettingsInSeries`: デフォルト `true`（ON）
- メニュー項目は「2D Viewer」メニューに配置

---

## ビューアウィンドウ側「2D Viewer」メニュー

### 主要機能一覧

#### 1. Image Tiling（画像タイル表示）
- **機能**: 1つのViewerControllerウィンドウ内で複数の画像をグリッド表示
- **実装**: `setImageTiling:` (ViewerController.m:22707)
- **メソッド**: `setImageRows:columns:rescale:` (ViewerController.m:22694)
- **対応**: 1x1, 1x2, 2x1, 2x2, 3x3など（最大8x8）
- **実装クラス**: `SeriesView.setImageViewMatrixForRows:columns:rescale:` (SeriesView.m:185)
- **状態**: ❌ 未実装

#### 2. Windows Tiling（ウィンドウタイル表示）
- **機能**: 複数のViewerControllerウィンドウを画面に均等配置
- **実装**: `tileWindows:` (AppController.m)
- **状態**: ✅ 既に実装済み

#### 3. Orientation（画像の向き変更）
- **機能**: 画像データセットの向きを変更
- **実装**: 
  - `setOrientationTool:` (ViewerController.m:2284)
  - `setOrientation:` (ViewerController.m:2305)
  - `vertFlipDataSet:` (ViewerController.m:2019) - 上下反転
  - `horzFlipDataSet:` (ViewerController.m:2082) - 左右反転
  - `rotateDataSet:` (ViewerController.m:2132) - 回転（90°, 180°, 270°）
  - `squareDataSet:` (ViewerController.m:2260) - 正方形化（pixel spacingを統一）
- **状態**: ✅ 基本実装済み（H/V/R/Lキー）

#### 4. Window Width & Level（ウィンドウ幅・レベル調整）
- **機能**: ウィンドウ幅・レベルの調整とプリセット管理
- **実装**: 
  - `ApplyWLWW:` - プリセット適用
  - `AddCurrentWLWW:` - 現在のWL/WWをプリセットに追加
  - `SetWLWW:` - 手動設定
- **状態**: ✅ 基本的な機能は実装済み（ToolbarPanel経由）

#### 5. Propagate Settings / Copy Settings（同期・伝播）
- **機能**: 表示設定（WL/WW, Zoom, Pan, Rotation, Flip等）を自動／手動で他ビューに伝播、シリーズ内で保持
- **実装（HOROS-20240407根拠）**:
  - グローバル自動伝播: `COPYSETTINGS` 既定値=ON（`DefaultsOsiriX.m:1243`）
  - シリーズ内コピー: `COPYSETTINGSINSERIES` 既定値=YES（`DCMView.m:6486`）
  - メニュー項目: MainMenu.xib の「2D Viewer」にバインド（`values.COPYSETTINGS`、`switchCopySettingsInSeries:`）
  - 手動コピー: `copySettingsToOthers:`（ViewerController.m:12984 付近）
  - 自動伝播判定: `propagateSettings` / `propagateSettingsToViewer`（ViewerController.m:16613 付近）
- **状態**: ✅ 実装済み（Java側もデフォルトONに調整済み、同期メニューは「2D Viewer」に配置済み）

詳細は[設定伝播・同期機能の詳細](#設定伝播同期機能の詳細)を参照。

#### 6. Convolution Filters（畳み込みフィルター）
- **機能**: 画像に畳み込みフィルターを適用
- **実装**: 
  - `ApplyConv:` - フィルター適用
  - `AddConv:` - フィルター追加
- **状態**: ❌ 未実装

#### 7. Color Look Up Table（カラールックアップテーブル）
- **機能**: カラーマッピングの変更
- **実装**: 
  - `ApplyCLUT:` - CLUT適用
  - `AddCLUT:` - CLUT追加（8-bit CLUT Editor）
- **状態**: ❌ 未実装

#### 8. Opacity（透明度）
- **機能**: 透明度テーブルの適用
- **実装**: 
  - `ApplyOpacity:` - 透明度テーブル適用
  - `AddOpacity:` - 透明度テーブル追加
- **状態**: ❌ 未実装

#### 9. SCALE（スケール調整）

##### 概要
- **機能**: 画像の表示スケールを調整
- **実装場所**: DCMView.m
- **メニュー項目**:
  - **No Rescale Size (100%)**: `actualSize:` (DCMView.m:12954-12965)
  - **Actual size**: `realSize:` (DCMView.m:12926-12952)
  - **Scale To Fit**: `scaleToFit:` (DCMView.m:12967-12978)
- **メニューの場所**:
  - **メインメニューバー**: MainMenu.xib（アプリケーション全体で共有）
  - **コンテキストメニュー**: ViewerController.m:2720-2722（右クリックメニュー）
- **動作**:
  - BrowserControllerがアクティブな時: プレビュービューワーに対して動作
  - ViewerControllerがアクティブな時: ビューワーのImageViewerPanelに対して動作

##### 実装詳細

**1. actualSize:（No Rescale Size (100%)）**
- **実装**: DCMView.m:12954-12965
- **機能**: 
  - ズームを1.0に設定
  - パンと回転をリセット
- **Java実装**: ✅ `ImageViewerPanel.actualSize()` (717-728行目)
  - `zoomFactor = 1.0`
  - `panX = 0.0`, `panY = 0.0`
  - `rotation = 0.0`

**2. realSize:（Actual Size）**
- **実装**: DCMView.m:12926-12952
- **機能**: 
  - pixel spacingと画面の物理サイズを考慮して実サイズ表示
  - 画面の物理サイズと解像度を取得して計算
  - pixel spacingが0の場合はエラーダイアログを表示
  - 画面のピクセルが正方形でない場合はエラーダイアログを表示
  - 計算式: `scale = pixelSpacingX / (screenPhysicalWidth / screenPixelWidth)`
- **Java実装**: ✅ `ImageViewerPanel.realSize()` (730-770行目)
  - Windows環境では画面の物理サイズを正確に取得できないため、簡易実装（DPIベース）
  - `pixelSpacingX`と`pixelSpacingY`を考慮してズームを計算

**3. scaleToFit:（Scale To Fit）**
- **実装**: DCMView.m:12967-12978
- **機能**:
  - パン位置を(0, 0)にリセット
  - 回転を0度にリセット
  - `scaleToFit`を呼び出してウィンドウにフィット
  - 2D Viewerの場合、設定を他のビューワーに伝播
- **scaleToFitForDCMPix:の実装**: DCMView.m:2056-2073
  ```objective-c
  - (float) scaleToFitForDCMPix: (DCMPix*) d
  {
      NSRect  sizeView = [self convertRectToBacking: [self bounds]]; // Retina
      
      int w = d.pwidth;
      int h = d.pheight;
      
      if( d.shutterEnabled)
      {
          w = d.shutterRect.size.width;
          h = d.shutterRect.size.height;
      }
      
      if( sizeView.size.width / w < sizeView.size.height / h / d.pixelRatio )
          return sizeView.size.width / w;
      else
          return sizeView.size.height / h / d.pixelRatio;
  }
  ```
- **Java実装**: ✅ `ImageViewerPanel.fitToWindow()` (650-692行目)
  - HOROS-20240407の`scaleToFitForDCMPix`の計算式を正確に再現
  - `widthRatio < heightRatio`の比較を使用
  - `pixelRatio`を考慮（0.00001〜1000の範囲で検証、無効な場合は1.0）

**4. マウスホイールズーム**
- **HOROS実装**: DCMView.m:4847-5044 (`scrollWheel:`)
- **重要な発見**:
  - **縦スクロール（deltaY）**: 画像の切り替え（前/次の画像へ移動）
  - **横スクロール（deltaX）**: ズームイン/アウト
  - **ズームの基準点**: 画像の中心（マウス位置は考慮しない）
  - **originの調整**: `panX = (panX * zoomFactor) / oldZoom`
- **Java実装**: ✅ `ImageViewerPanel.MouseWheelListener` (350-382行目)
  - 画像の中心を基準にズーム（マウス位置は考慮しない）
  - HOROS-20240407の`scrollWheel:`を再現
  - originの調整: `panX = (panX * zoomFactor) / oldZoom`

##### 実装状況
- ✅ `fitToWindow()`: 実装済み（`scaleToFit`相当）
- ✅ `actualSize()`: 実装済み（2025年1月）
- ✅ `realSize()`: 実装済み（2025年1月）
- ✅ マウスホイールズーム: 実装済み（2025年1月、画像中心基準）
- ✅ BrowserControllerの「2D Viewer」メニューに「SCALE」サブメニュー追加済み（2025年1月）
- ❌ ViewerControllerのメニューバーへの追加: 未実装（Windows環境ではメニューバーがウィンドウごと）

##### 実装時の問題点と解決策

**問題1: fitToWindow()の計算式の不一致**
- **問題**: HOROSの計算式とJava実装が異なっていた
- **解決**: HOROSの計算式を正確に再現（`widthRatio < heightRatio`の比較）

**問題2: 拡大縮小時の基準点が右下になっている**
- **問題**: マウス位置を中心にズームしようとしていた
- **解決**: HOROS-20240407に合わせて画像の中心を基準にズームするように変更

**問題3: CR画像が拡大、CT/MR画像が縮小**
- **問題**: `pixelRatio`の計算が不適切だった
- **解決**: `pixelRatio`の検証を追加（0.00001〜1000の範囲、無効な場合は1.0）

#### 10. Resize Window（ウィンドウサイズ変更）
- **機能**: ウィンドウサイズをプリセット値に変更
- **実装**: `resizeWindow:` (ViewerController.m:2709)
- **オプション**: 
  - No Rescale Size (100%)
  - Actual size
  - Scale To Fit
- **状態**: ❌ 未実装

#### 11. Workspace State（ワークスペース状態）
- **機能**: ウィンドウ配置や表示状態を保存・読み込み
- **実装**: 
  - `saveWindowsState:` (ViewerController.m:1028)
  - `saveWindowsStateAsDICOMSR:` (ViewerController.m:1033)
  - `loadWindowsState:` (ViewerController.m:1017)
  - `resetWindowsState:` (ViewerController.m:1007)
- **状態**: ❌ 未実装

#### 12. Apply Window Protocol（ウィンドウプロトコル適用）
- **機能**: モダリティやシリーズに応じたウィンドウ設定を自動適用
- **実装**: `applyWindowProtocol:` (ViewerController.m:2747)
- **状態**: ❌ 未実装

#### 13. Key Images（キー画像）
- **機能**: キー画像のマーク付けとナビゲーション
- **実装**: 
  - `setKeyImage:` - キー画像としてマーク
  - `setAllKeyImages:` - すべてをキー画像に
  - `setAllNonKeyImages:` - すべてを非キー画像に
  - `findNextPreviousKeyImage:` - 次の/前のキー画像へ移動
- **状態**: ❌ 未実装

#### 14. Export（エクスポート）
- **機能**: 画像のエクスポート
- **実装**: 各種エクスポート機能
- **状態**: ❌ 未実装

#### 15. Series Popup（シリーズ選択）
- **機能**: 表示中のシリーズを切り替え
- **実装**: `seriesPopupSelect:` (ViewerController.m:4381)
- **状態**: ✅ 部分的に実装済み（ThumbnailsListPanel経由）

#### 15. Flip Data Series（データシリーズの反転）
- **機能**: データシリーズ全体を反転
- **実装**: `flipDataSeries:` (ViewerController.m:10400)
- **状態**: ❌ 未実装

#### 16. Convert to RGB/BW（RGB/モノクロ変換）
- **機能**: 画像をRGBまたはモノクロに変換
- **実装**: 
  - `ConvertToRGBMenu:` (ViewerController.m:10250)
  - `ConvertToBWMenu:` (ViewerController.m:10280)
- **状態**: ❌ 未実装

#### 17. Navigator（ナビゲーター）
- **機能**: 3Dデータのナビゲーションウィンドウ
- **実装**: `navigator:` (ViewerController.m:22887)
- **状態**: ❌ 未実装（3D機能）

#### 18. 3D Panel（3Dパネル）
- **機能**: 3D位置コントローラー
- **実装**: `threeDPanel:` (ViewerController.m:22812)
- **状態**: ❌ 未実装（3D機能）

#### 19. Display SUV（SUV表示）
- **機能**: PET画像のSUV値を表示
- **実装**: `displaySUV:` (ViewerController.m:664)
- **状態**: ❌ 未実装

#### 20. Use VOILUT（VOI LUT使用）
- **機能**: VOI LUTテーブルの使用/不使用を切り替え
- **実装**: `useVOILUT:` (ViewerController.m:684)
- **状態**: ❌ 未実装

#### 21. Blend Windows（ウィンドウブレンド）
- **機能**: 複数のビューワーウィンドウをブレンド
- **実装**: `blendWindows:` (ViewerController.m:754)
- **状態**: ❌ 未実装

---

## ブラウザー/ツールウィンドウ側「2D Viewer」メニュー

### ビューワー管理機能

#### 1. Close All Viewers（すべてのビューワーを閉じる）
- **実装**: `closeAllViewers:` (AppController.m:5554)
- **機能**: すべてのViewerControllerウィンドウを閉じる
- **状態**: ❌ 未実装

#### 2. Tile Windows（ウィンドウをタイル表示）
- **実装**: `tileWindows:` (AppController.m:4857)
- **機能**: すべてのViewerControllerウィンドウを画面に均等配置
- **状態**: ✅ 既に実装済み（WindowLayoutManager経由）

#### 3. Windows Tiling - Rows（ウィンドウタイル表示 - 行数指定）
- **実装**: `setFixedTilingRows:` (AppController.m:4760)
- **機能**: 指定した行数でビューワーをタイル表示
- **メニュー**: `windowsTilingMenuRows` (AppController.m:660)
- **状態**: ❌ 未実装

#### 4. Windows Tiling - Columns（ウィンドウタイル表示 - 列数指定）
- **実装**: `setFixedTilingColumns:` (AppController.m:4765)
- **機能**: 指定した列数でビューワーをタイル表示
- **メニュー**: `windowsTilingMenuColumns` (AppController.m:660)
- **状態**: ❌ 未実装

### ビューワー表示機能

#### 5. Image Tiling（画像タイル表示）
- **IBOutlet**: `imageTileMenu` (BrowserController.h:185)
- **検証ロジック**: BrowserController.m:15025-15028
- **機能**: メインウィンドウがViewerControllerの場合のみ有効化される
- **実装**: ViewerControllerの`setImageTiling:`メソッドを呼び出す
- **状態**: ❌ 未実装

#### 6-15. その他のビューワー表示機能
- Orientation、Window Width & Level、Convolution Filters、Color Look Up Table、Opacity、Resize Window、Workspace State、Apply Window Protocol、Key Images、Export
- **状態**: ビューアウィンドウ側と同様（上記参照）

### ブラウザーウィンドウから「2D Viewer」メニューにアクセスする機能

BrowserControllerがアクティブな時に、「2D Viewer」メニューから以下の機能にアクセスできます：

1. **Close All Viewers** - すべてのViewerControllerウィンドウを閉じる
2. **Tile Windows** - すべてのViewerControllerウィンドウを画面に均等配置
3. **Windows Tiling - Rows/Columns** - 指定した行数/列数でビューワーをタイル表示
4. **Image Tiling** - アクティブなViewerControllerウィンドウの画像タイル表示を変更（メインウィンドウがViewerControllerの場合のみ有効）
5. **その他のビューワー表示機能** - アクティブなViewerControllerウィンドウに対して機能を適用

### 実装のポイント

- BrowserControllerの`validateMenuItem:`メソッドで、`imageTileMenu`の有効/無効を制御
- メインウィンドウがViewerControllerの場合のみ、Image Tilingメニューが有効になる
- `closeAllViewers:`は、フルスクリーンウィンドウが表示されている場合は実行されない
- `tileWindows:`は、すべてのViewerControllerウィンドウを取得してタイル表示する

---

## 設定伝播・同期機能の詳細

### 主要機能

#### 1. Propagate Settings（設定の伝播）
- **実装**: `propagateSettings` (ViewerController.m:16804)
- **機能**: 現在のビューワーの設定を他のすべてのビューワーに自動的に伝播
- **伝播される設定**:
  - Window Width & Level (WL/WW)
  - Zoom (Scale)
  - Rotation
  - Pan (Origin)
  - CLUT (Color Look Up Table)
  - Opacity
  - Convolution Filter
  - その他の表示設定

#### 2. Copy Settings to Others（設定を他のビューワーにコピー）
- **実装**: `copySettingsToOthers:` (ViewerController.m:12984)
- **機能**: 手動で現在のビューワーの設定を他のすべてのビューワーにコピー
- **呼び出し**: `propagateSettings`を呼び出す

#### 3. Copy Settings in Series（シリーズ内で設定をコピー）
- **実装**: `switchCopySettingsInSeries:` (DCMView.m:2480)
- **機能**: シリーズ内の画像間で設定を継承するかどうかを切り替え
- **設定**: `COPYSETTINGSINSERIES` (DCMView.m:2424)
- **動作**:
  - `COPYSETTINGSINSERIES = YES`: シリーズ内のすべての画像で同じ設定を使用（設定を保存しない）
  - `COPYSETTINGSINSERIES = NO`: 各画像で個別の設定を保存

#### 4. Copy Settings（設定のコピー - グローバル設定）
- **設定キー**: `COPYSETTINGS` (NSUserDefaults)
- **機能**: ビューワー間で設定を自動的に伝播するかどうかを制御
- **デフォルト値**: `YES` (DefaultsOsiriX.m:1243)
- **使用箇所**:
  - `propagateSettingsToViewer:` (ViewerController.m:16679)
  - 各種設定変更時に自動伝播を制御

### 実装詳細

#### propagateSettingsToViewer: (ViewerController.m:16663)

他のビューワーに設定を伝播する詳細ロジック：

1. **条件チェック**:
   - `COPYSETTINGS`が`YES`の場合のみ伝播
   - モダリティが同じ場合のみ伝播（一部例外あり）
   - CLUTが同じ場合のみ伝播
   - RGB/モノクロの一致を確認
   - 減算処理の状態を確認

2. **伝播される設定**:
   - **WL/WW**: `DONTCOPYWLWWSETTINGS`が`NO`の場合のみ
   - **Zoom (Scale)**: `scaleValue`を伝播
   - **Rotation**: `rotation`を伝播
   - **Pan (Origin)**: 平行平面の場合、原点の差分を計算して伝播
   - **CLUT**: `curCLUTMenu`を伝播
   - **Opacity**: `curOpacityMenu`を伝播
   - **Convolution Filter**: `curConvMenu`を伝播

3. **特殊ケース**:
   - 同じスタディ内のビューワー間で、平行平面の場合、パン（原点）を自動調整
   - ブレンディングコントローラーがある場合、そのビューワーにも伝播

#### switchCopySettingsInSeries: (DCMView.m:2480)

シリーズ内で設定をコピーする機能：

1. **動作**:
   - `COPYSETTINGSINSERIES`をトグル
   - シリーズ内のすべての`DCMView`に同じ設定を適用

2. **設定の保存/削除**:
   - `COPYSETTINGSINSERIES = YES`: 画像オブジェクトから設定を削除（`nil`に設定）
     - `windowWidth`, `windowLevel`, `scale`, `rotationAngle`, `yFlipped`, `xFlipped`, `xOffset`, `yOffset`
   - `COPYSETTINGSINSERIES = NO`: 画像オブジェクトに設定を保存

### メニュー項目

#### 必要なメニュー項目

1. **Propagate Settings（設定を伝播）**
   - メニュー項目: "設定を他のビューワーにコピー" または "Copy Settings to Others"
   - アクション: `copySettingsToOthers:`
   - 機能: 現在のビューワーの設定を他のすべてのビューワーにコピー

2. **Copy Settings in Series（シリーズ内で設定をコピー）**
   - メニュー項目: "シリーズ内で設定をコピー"
   - アクション: `switchCopySettingsInSeries:`
   - 機能: シリーズ内の画像間で設定を継承するかどうかを切り替え
   - 状態表示: チェックマークで有効/無効を表示

3. **Copy Settings（設定のコピー - グローバル設定）**
   - メニュー項目: "設定を自動伝播" または "Auto Propagate Settings"
   - アクション: `toggleCopySettings:` (新規実装が必要)
   - 機能: ビューワー間で設定を自動的に伝播するかどうかを切り替え
   - 状態表示: チェックマークで有効/無効を表示

### 実装のポイント

1. **設定の伝播タイミング**:
   - WL/WW変更時
   - ズーム変更時
   - 回転変更時
   - パン変更時
   - CLUT変更時
   - Opacity変更時
   - Convolution Filter変更時

2. **伝播の条件**:
   - `COPYSETTINGS`が`YES`の場合のみ自動伝播
   - モダリティが同じ場合のみ伝播（一部例外あり）
   - CLUTが同じ場合のみ伝播
   - RGB/モノクロの一致を確認

3. **シリーズ内の設定継承**:
   - `COPYSETTINGSINSERIES`が`YES`の場合、シリーズ内のすべての画像で同じ設定を使用
   - 設定を画像オブジェクトに保存しない（`nil`に設定）

---

## 実装状況と優先度

### 実装状況（JJ側・簡易）

- **Windows Tiling**: ✅
- **Image Tiling**: ❌ 未実装（同等化要）
- **Orientation**: ✅（基本）
- **WL/WW**: ✅（Toolbar経由）
- **Propagate/Copy Settings**: ✅（デフォルトONに合わせ済み・要再検証）
- **ブラウザ側「2D Viewer」に同期/タイル/クローズメニューを追加済み**（簡易）
- **他の項目**: ❌ 未実装

### 実装優先度（再整理）

1. **同期系をメニューに反映**（ブラウザー/ツールウィンドウ側にも追加し、Horos同等の動線にする）  
2. **Image Tiling**（ブラウザー側メニュー経由も有効化）、Windows Tiling Rows/Columns  
3. **Orientation／WL/WWの安定化と伝播検証**  
4. **そのほか未実装群**（Convolution, CLUT, Opacity など）を段階的に

### 最新の改修予定（抜粋・ここだけ見れば進捗が分かる）

- 同期ロジックをHOROS準拠で再構築（自動伝播条件・シリーズ内コピー・パン補正を再現）
- ブラウザ/ツール側「2D Viewer」メニューに同期項目を追加し、Horos同等の動線にする
- ROI/プレゼンテーション状態の永続化はHOROS仕様を確認の上で設計・実装（後続）

---

## 参考実装箇所

### HOROSソースコード

- **ViewerController.m**: 主要なメニューアクション実装
  - `copySettingsToOthers:` (12984)
  - `propagateSettings` (16804)
  - `propagateSettingsToViewer:` (16663)
  - `setImageTiling:` (22707)
  - `setImageRows:columns:rescale:` (22694)
- **DCMView.m**: 
  - `switchCopySettingsInSeries:` (2480)
  - `COPYSETTINGSINSERIES` (2424)
  - `actualSize:` (12954-12965)
  - `realSize:` (12926-12952)
  - `scaleToFit:` (12967-12978)
  - `scaleToFit` (2075-2093)
  - `scaleToFitForDCMPix:` (2056-2073)
  - `scrollWheel:` (4847-5044)
- **AppController.m**: 
  - `tileWindows:` (4857)
  - `closeAllViewers:` (5554)
  - `setFixedTilingRows:` (4760)
  - `setFixedTilingColumns:` (4765)
- **BrowserController.m**: 
  - `validateMenuItem:` (15025-15028)
  - `IBOutlet NSMenu *imageTileMenu;` (BrowserController.h:185)
- **SeriesView.m**: Image Tilingの実装（`setImageViewMatrixForRows:columns:rescale:` (185)）
- **DefaultsOsiriX.m**: `COPYSETTINGS`のデフォルト値 (1243)
- **MainMenu.xib / Viewer.xib**: メニュー構造

### Java実装

- **ViewerController.java**: ビューワーウィンドウの実装
- **BrowserController.java**: ブラウザーウィンドウの実装
- **ApplicationController.java**: アプリケーション制御
- **WindowLayoutManager.java**: ウィンドウ配置管理

---

## 変更履歴

- 2025年12月: 全「2D Viewer」メニュー関連ドキュメントを統合（4ファイル → 1ファイル）
- 2024年12月: 初期実装とドキュメント作成
