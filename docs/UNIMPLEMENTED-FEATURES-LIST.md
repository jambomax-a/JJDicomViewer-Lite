# 未実装機能一覧

**最終更新**: 2025年12月10日

## 概要

HOROS-20240407の機能のうち、現在未実装の項目を一覧化したドキュメントです。
**重要**: DICOMノード/パーサー関連の機能が第一優先度です。

---

## DICOMノード・パーサー関連（第一優先度）

### 1. DCM Framework相当のDICOMパーサー実装
- **機能**: DICOMファイルの完全なパース機能
- **HOROS実装**: `DCM Framework/DCMObject.h/m` (約2000行超)
- **主要コンポーネント**:
  - `DCMObject` - DICOMファイルのメインオブジェクト表現
  - `DCMAttribute` - DICOM属性の表現
  - `DCMAttributeTag` - DICOMタグの管理
  - `DCMPixelDataAttribute` - ピクセルデータ属性（JPEG/JPEG2000/RLE対応）
  - `DCMTransferSyntax` - 転送構文の処理
  - `DCMCharacterSet` - 文字セットエンコーディング
  - `DCMSequenceAttribute` - シーケンス属性
  - `DCMTagDictionary` / `DCMTagForNameDictionary` - タグ辞書
- **現在のJava実装**: `DicomFile.java`はdcm4che3を使用した基本的な読み込みのみ
- **不足機能**:
  - 完全なDICOMデータセットの解析
  - 転送構文の完全対応（JPEG/JPEG2000/RLE等の圧縮形式）
  - シーケンス属性の処理
  - 属性の編集・書き込み機能
  - セカンダリキャプチャオブジェクトの生成
  - 匿名化機能
- **状態**: ❌ 未実装（部分実装のみ）
- **優先度**: **最高（第一優先度）**

### 2. DICOMノード階層構造（Query Node Hierarchy）
- **機能**: DICOMネットワーククエリ/リトリーブの階層的ノード構造
- **HOROS実装**: 
  - `DCMTKQueryNode.h/mm` - 基本クエリノードクラス
  - `DCMTKRootQueryNode.h/mm` - ルートノード
  - `DCMTKStudyQueryNode.h/mm` - スタディレベルノード
  - `DCMTKSeriesQueryNode.h/mm` - シリーズレベルノード
  - `DCMTKImageQueryNode.h/mm` - イメージレベルノード
- **機能**:
  - C-FIND: 階層的検索（Patient→Study→Series→Image）
  - C-MOVE: 画像取得
  - C-GET: 画像取得（代替手段）
  - ネットワーク接続管理
  - 転送構文ネゴシエーション
  - 進捗管理
- **現在のJava実装**: 
  - `DicomQuerySCU.java` - 基本的なクエリ機能のみ
  - `DicomMoveSCU.java` - 基本的なMove機能のみ
  - ノード階層構造なし
  - QueryController相当のUI統合なし
- **状態**: ❌ 未実装（基本機能のみ、階層構造なし）
- **優先度**: **最高（第一優先度）**

### 3. DicomDirParser（DICOMDIRパーサー）
- **機能**: DICOMDIRファイルの解析とファイル検索
- **HOROS実装**: `DicomDirParser.h/m` (Horos/Sources)
- **機能**:
  - DICOMDIRファイルからファイルリストを抽出
  - dcmdumpを使用したパース
  - ファイルパスの解決
  - 再帰的なディレクトリ検索
- **現在のJava実装**: ❌ 未実装
- **状態**: ❌ 未実装
- **優先度**: **高（第一優先度）**

### 4. QueryController（クエリコントローラーUI）
- **機能**: DICOMネットワーククエリのUI統合
- **HOROS実装**: `QueryController.h/mm` (Horos/Sources)
- **機能**:
  - DICOMノード設定UI
  - クエリ結果の表示
  - クエリ結果からの画像取得
  - 進捗表示
- **現在のJava実装**: ❌ 未実装
- **状態**: ❌ 未実装
- **優先度**: **高（第一優先度）**

### 5. DICOMノード管理機能
- **機能**: DICOMネットワークノードの保存・管理
- **HOROS実装**: 
  - `OSILocationsPreferencePane` - ノード設定UI
  - `NSUserDefaults` - ノード情報の保存
  - Bonjour対応（オプション）
- **機能**:
  - ノードの追加・編集・削除
  - AET (Application Entity Title)管理
  - ホスト名・ポート管理
  - 転送構文設定
  - 認証設定
- **現在のJava実装**: ❌ 未実装
- **状態**: ❌ 未実装
- **優先度**: **高（第一優先度）**

### 6. DICOM Meta Data（DICOMメタデータ表示・編集）
- **機能**: DICOMファイルの全属性を階層的に表示し、編集可能にする
- **HOROS実装**: 
  - `XMLController.h/m` - メタデータ表示・編集コントローラー（約1800行超）
  - `XMLControllerDCMTKCategory` - DCMTK統合
  - メニュー項目: 「DICOM Meta-Data」（キーボードショートカット: Shift+I）
  - `BrowserController.viewXML:` - ブラウザー側からの呼び出し
  - `ViewerController.viewXML:` - ビューワー側からの呼び出し
- **機能**:
  - DICOMファイルの全属性を階層的ツリー表示（NSOutlineView）
  - 属性の検索機能
  - 属性の編集機能（グループ・エレメント・値の変更）
  - DICOMフィールドの追加機能
  - 編集レベル選択（Image/Series/Study/Patientレベル）
  - 変更内容の保存
  - データベースの再読み込み
  - XML形式でのエクスポート
  - バリデーション機能
  - DCMObjectとの統合
- **主要メソッド**:
  - `initWithImage:windowName:viewer:` - 初期化
  - `reloadFromDCMDocument` - DCMObjectからの再読み込み
  - `executeAdd:` - DICOMフィールドの追加
  - `updateDB:objects:` - データベース更新
- **現在のJava実装**: ❌ 未実装
- **状態**: ❌ 未実装
- **優先度**: **最高（第一優先度）** - DCM Framework実装と密接に関連

---

## ビューアウィンドウ側「2D Viewer」メニュー

### 1. Image Tiling（画像タイル表示）
- **機能**: 1つのViewerControllerウィンドウ内で複数の画像をグリッド表示
- **実装**: `setImageTiling:` (ViewerController.m:22707)
- **メソッド**: `setImageRows:columns:rescale:` (ViewerController.m:22694)
- **対応**: 1x1, 1x2, 2x1, 2x2, 3x3など（最大8x8）
- **実装クラス**: `SeriesView.setImageViewMatrixForRows:columns:rescale:` (SeriesView.m:185)
- **状態**: ❌ 未実装
- **優先度**: 高（実装優先度2）

### 2. Convolution Filters（畳み込みフィルター）
- **機能**: 画像に畳み込みフィルターを適用
- **実装**: 
  - `ApplyConv:` - フィルター適用
  - `AddConv:` - フィルター追加
- **状態**: ❌ 未実装
- **優先度**: 中（実装優先度4）

### 3. Color Look Up Table（カラールックアップテーブル）
- **機能**: カラーマッピングの変更
- **実装**: 
  - `ApplyCLUT:` - CLUT適用
  - `AddCLUT:` - CLUT追加（8-bit CLUT Editor）
- **状態**: ❌ 未実装
- **優先度**: 中（実装優先度4）

### 4. Opacity（透明度）
- **機能**: 透明度テーブルの適用
- **実装**: 
  - `ApplyOpacity:` - 透明度テーブル適用
  - `AddOpacity:` - 透明度テーブル追加
- **状態**: ❌ 未実装
- **優先度**: 中（実装優先度4）

### 5. Resize Window（ウィンドウサイズ変更）
- **機能**: ウィンドウサイズをプリセット値に変更
- **実装**: `resizeWindow:` (ViewerController.m:2709)
- **オプション**: 
  - No Rescale Size (100%)
  - Actual size
  - Scale To Fit
- **状態**: ❌ 未実装
- **優先度**: 低

### 6. Workspace State（ワークスペース状態）
- **機能**: ウィンドウ配置や表示状態を保存・読み込み
- **実装**: 
  - `saveWindowsState:` (ViewerController.m:1028)
  - `saveWindowsStateAsDICOMSR:` (ViewerController.m:1033)
  - `loadWindowsState:` (ViewerController.m:1017)
  - `resetWindowsState:` (ViewerController.m:1007)
- **状態**: ❌ 未実装
- **優先度**: 低

### 7. Apply Window Protocol（ウィンドウプロトコル適用）
- **機能**: モダリティやシリーズに応じたウィンドウ設定を自動適用
- **実装**: `applyWindowProtocol:` (ViewerController.m:2747)
- **状態**: ❌ 未実装
- **優先度**: 低

### 8. Key Images（キー画像）
- **機能**: キー画像のマーク付けとナビゲーション
- **実装**: 
  - `setKeyImage:` - キー画像としてマーク
  - `setAllKeyImages:` - すべてをキー画像に
  - `setAllNonKeyImages:` - すべてを非キー画像に
  - `findNextPreviousKeyImage:` - 次の/前のキー画像へ移動
- **状態**: ❌ 未実装
- **優先度**: 低

### 9. Export（エクスポート）
- **機能**: 画像のエクスポート
- **実装**: 各種エクスポート機能
- **状態**: ❌ 未実装
- **優先度**: 低

### 10. Flip Data Series（データシリーズの反転）
- **機能**: データシリーズ全体を反転
- **実装**: `flipDataSeries:` (ViewerController.m:10400)
- **状態**: ❌ 未実装
- **優先度**: 低

### 11. Convert to RGB/BW（RGB/モノクロ変換）
- **機能**: 画像をRGBまたはモノクロに変換
- **実装**: 
  - `ConvertToRGBMenu:` (ViewerController.m:10250)
  - `ConvertToBWMenu:` (ViewerController.m:10280)
- **状態**: ❌ 未実装
- **優先度**: 低

### 3D機能関連（未実装）

### 12. Navigator（ナビゲーター）
- **機能**: 3Dデータのナビゲーションウィンドウ
- **実装**: `navigator:` (ViewerController.m:22887)
- **状態**: ❌ 未実装（3D機能）
- **優先度**: 低（3D機能は後回し）

### 13. 3D Panel（3Dパネル）
- **機能**: 3D位置コントローラー
- **実装**: `threeDPanel:` (ViewerController.m:22812)
- **状態**: ❌ 未実装（3D機能）
- **優先度**: 低（3D機能は後回し）

### 14. Display SUV（SUV表示）
- **機能**: PET画像のSUV値を表示
- **実装**: `displaySUV:` (ViewerController.m:664)
- **状態**: ❌ 未実装
- **優先度**: 低（PET画像専用）

### 15. Use VOILUT（VOI LUT使用）
- **機能**: VOI LUTテーブルの使用/不使用を切り替え
- **実装**: `useVOILUT:` (ViewerController.m:684)
- **状態**: ❌ 未実装
- **優先度**: 低

### 16. Blend Windows（ウィンドウブレンド）
- **機能**: 複数のビューワーウィンドウをブレンド
- **実装**: `blendWindows:` (ViewerController.m:754)
- **状態**: ❌ 未実装
- **優先度**: 低

### 17. ViewerControllerのメニューバーへのSCALEサブメニュー追加
- **機能**: Windows環境ではメニューバーがウィンドウごとのため、ViewerControllerのメニューバーにもSCALEサブメニューを追加
- **状態**: ❌ 未実装（Windows環境ではメニューバーがウィンドウごと）
- **優先度**: 低（BrowserControllerには実装済み）

---

## ブラウザー/ツールウィンドウ側「2D Viewer」メニュー

### 1. Close All Viewers（すべてのビューワーを閉じる）
- **実装**: `closeAllViewers:` (AppController.m:5554)
- **機能**: すべてのViewerControllerウィンドウを閉じる
- **Java実装**: `ApplicationController.closeAllViewers()` (ApplicationController.java:415)
- **メニュー実装**: `BrowserController.java:270` に「すべてのビューワーを閉じる」メニュー項目を追加済み
- **状態**: ✅ 実装済み
- **優先度**: -（完了）

### 2. Windows Tiling（ウィンドウタイル表示 - 基本版）
- **実装**: `tileWindows:` (AppController.m:4915-5550)
- **機能**: 複数のViewerControllerウィンドウを画面に均等配置
- **Java実装**: `WindowLayoutManager.tileWindows()` (WindowLayoutManager.java:58)
- **メニュー実装**: `BrowserController.java:274` に「ウィンドウをタイル表示」メニュー項目を追加済み
- **状態**: ✅ 実装済み
- **優先度**: -（完了）

### 3. Windows Tiling - Rows（ウィンドウタイル表示 - 行数指定）
- **実装**: `setFixedTilingRows:` (AppController.m:4760)
- **機能**: 指定した行数でビューワーをタイル表示（HOROSの固定行数メニュー）
- **メニュー**: `windowsTilingMenuRows` (AppController.m:660)
- **状態**: ❌ 未実装（基本版のtileWindowsは実装済みだが、行数固定版は未実装）
- **優先度**: 高（実装優先度2）

### 4. Windows Tiling - Columns（ウィンドウタイル表示 - 列数指定）
- **実装**: `setFixedTilingColumns:` (AppController.m:4765)
- **機能**: 指定した列数でビューワーをタイル表示（HOROSの固定列数メニュー）
- **メニュー**: `windowsTilingMenuColumns` (AppController.m:660)
- **状態**: ❌ 未実装（基本版のtileWindowsは実装済みだが、列数固定版は未実装）
- **優先度**: 高（実装優先度2）

### 5. Image Tiling（画像タイル表示）
- **IBOutlet**: `imageTileMenu` (BrowserController.h:185)
- **検証ロジック**: BrowserController.m:15025-15028
- **機能**: メインウィンドウがViewerControllerの場合のみ有効化される
- **実装**: ViewerControllerの`setImageTiling:`メソッドを呼び出す
- **状態**: ❌ 未実装
- **優先度**: 高（実装優先度2）

---

## 実装優先度（再整理）

### 優先度0（第一優先度）: DICOMノード・パーサー関連
- ❌ **DCM Framework相当のDICOMパーサー**: 完全なDICOMファイル解析機能
- ❌ **DICOM Meta Data（XMLController）**: DICOMメタデータ表示・編集機能（メニュー: 「DICOM Meta-Data」）
- ❌ **DICOMノード階層構造**: Query Node Hierarchyの実装
- ❌ **DicomDirParser**: DICOMDIRファイルの解析
- ❌ **QueryController**: クエリUI統合
- ❌ **DICOMノード管理**: ノード設定・保存機能

**注意**: これらは他の機能の基盤となるため、最優先で実装する必要があります。

### 優先度1: 同期系をメニューに反映
- ✅ **完了**: ブラウザー/ツールウィンドウ側にも追加し、Horos同等の動線にする

### 優先度2: Image Tiling / Windows Tiling Rows/Columns
- ❌ **Image Tiling**: ブラウザー側メニュー経由も有効化
- ✅ **Windows Tiling（基本版）**: 実装済み（WindowLayoutManager.tileWindows()）
- ❌ **Windows Tiling Rows**: 行数指定でのタイル表示（未実装）
- ❌ **Windows Tiling Columns**: 列数指定でのタイル表示（未実装）

### 優先度3: Orientation／WL/WWの安定化と伝播検証
- ✅ **基本実装済み**: 動作確認と安定化が必要

### 優先度4: そのほか未実装群
- ❌ **Convolution Filters**: 畳み込みフィルター
- ❌ **Color Look Up Table**: カラールックアップテーブル
- ❌ **Opacity**: 透明度テーブル
- ❌ **その他**: Resize Window, Workspace State, Apply Window Protocol, Key Images, Export, Flip Data Series, Convert to RGB/BW, Display SUV, Use VOILUT, Blend Windows

---

## 計測・ROI描画機能（将来実装予定）

以下の機能は、計測・ROI描画機能の実装後に追加される予定です：

- **ROI描画**: 矩形、円、多角形、フリーハンドなど
- **計測**: 距離、角度、面積、体積など
- **ROI管理**: ROIの保存、読み込み、削除
- **ROI同期**: 複数ビュー間でのROI同期

---

## 参考実装箇所

### HOROSソースコード

#### DICOMノード・パーサー関連（第一優先度）
- **DCM Framework/DCMObject.h/m**: DICOMファイルの完全なパース実装（約2000行超）
  - `+isDICOM:` - DICOMファイル判定
  - `+objectWithContentsOfFile:decodingPixelData:` - ファイル読み込み
  - `-parseDataContainer:lengthToRead:byteOffset:characterSet:decodingPixelData:` - データ解析
  - 属性の読み書き、シーケンス処理、転送構文対応
- **DCMTKQueryNode.h/mm**: DICOMネットワーククエリの基本クラス
  - `+queryNodeWithDataset:callingAET:calledAET:hostname:port:transferSyntax:compression:extraParameters:`
  - `-queryWithValues:` - クエリ実行
  - `-move:retrieveMode:` - C-MOVE実行
  - `-cfind:` / `-cmove:` / `-cget:` - DICOMサービス
- **DCMTKRootQueryNode.h/mm**: ルートクエリノード
- **DCMTKStudyQueryNode.h/mm**: スタディレベルクエリノード
- **DCMTKSeriesQueryNode.h/mm**: シリーズレベルクエリノード
- **DCMTKImageQueryNode.h/mm**: イメージレベルクエリノード
- **DicomDirParser.h/m**: DICOMDIRファイルのパーサー
  - `-init:` - dcmdumpを使用したパース
  - `-parseArray:` - ファイルリストの抽出
- **QueryController.h/mm**: クエリUI統合
- **OSILocationsPreferencePane**: DICOMノード設定UI
- **XMLController.h/m**: DICOMメタデータ表示・編集（約1800行超）
  - `initWithImage:windowName:viewer:` - 初期化
  - `viewXML:` - メニューアクション（BrowserController/ViewerController）
  - `reloadFromDCMDocument` - DCMObjectからの再読み込み
  - `executeAdd:` - DICOMフィールドの追加
  - `updateDB:objects:` - データベース更新
  - ツールバー: Export, Expand/Collapse, Search, Editing, Verify

#### 2D Viewerメニュー関連
- **ViewerController.m**: 主要なメニューアクション実装
  - `setImageTiling:` (22707)
  - `setImageRows:columns:rescale:` (22694)
  - `setFixedTilingRows:` (4760)
  - `setFixedTilingColumns:` (4765)
  - `closeAllViewers:` (5554)
- **SeriesView.m**: Image Tilingの実装（`setImageViewMatrixForRows:columns:rescale:` (185)）
- **BrowserController.m**: 
  - `validateMenuItem:` (15025-15028)
  - `IBOutlet NSMenu *imageTileMenu;` (BrowserController.h:185)

### Java実装

#### DICOMノード・パーサー関連（未実装/部分実装）
- **DicomFile.java**: dcm4che3を使用した基本的な読み込みのみ（完全なDCM Framework相当は未実装）
- ❌ **XMLController相当（DICOM Meta Data）**: 未実装 - DICOMメタデータ表示・編集ウィンドウ
- **DicomQuerySCU.java**: 基本的なクエリ機能のみ（ノード階層構造なし）
- **DicomMoveSCU.java**: 基本的なMove機能のみ
- **DicomEchoSCU.java**: C-ECHO機能
- ❌ **DicomDirParser相当**: 未実装
- ❌ **QueryController相当**: 未実装
- ❌ **DICOMノード管理UI**: 未実装

#### UI関連（実装済み/部分実装済み）
- **ViewerController.java**: ビューワーウィンドウの実装
- **BrowserController.java**: ブラウザーウィンドウの実装
- **ApplicationController.java**: アプリケーション制御
- **WindowLayoutManager.java**: ウィンドウ配置管理

---

## 変更履歴

- 2025年12月10日: DICOMノード・パーサー関連を第一優先度として追加
  - DCM Framework相当のDICOMパーサー実装を追加
  - **DICOM Meta Data（XMLController）を追加** - メニュー項目「DICOM Meta-Data」
  - DICOMノード階層構造（Query Node Hierarchy）を追加
  - DicomDirParserを追加
  - QueryControllerを追加
  - DICOMノード管理機能を追加
  - 実装優先度を再整理（優先度0を第一優先度として追加）
- 2025年12月10日: 実装状況を更新
  - Close All Viewersを実装済みに変更
  - Windows Tiling（基本版）を実装済みに変更
  - Windows Tiling Rows/Columns（行数/列数指定版）を明確化
- 2025年1月: 未実装機能一覧を作成

