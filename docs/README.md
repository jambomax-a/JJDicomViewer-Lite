# HOROS-20240407 Java移植ドキュメント

## プロジェクト概要

本プロジェクトは、**獣医向けDICOMビューワー**として、HOROS-20240407のソースコードを詳細に解析し、Java Swingへの移植を行うものです。

**主な目的**：
- Windows環境での使い勝手の良いDICOMビューワーの提供
- 獣医向け電子カルテシステム（Vet-System）との連携
- スタンドアローン使用のサポート

詳細は[プロジェクト概要](00-Project-Overview.md)を参照してください。

## ドキュメント一覧

このディレクトリには、HOROS-20240407のソースコードを徹底的に解析し、Java SwingでのDICOMビューワー実装に向けた詳細なドキュメントが含まれています。

### 0. [プロジェクト概要](00-Project-Overview.md)
- プロジェクトの目的と背景
- 対象ユーザーと使用環境
- 機能範囲（実装する機能・除外する機能）
- Vet-Systemとの連携
- 開発方針

### 1. [アーキテクチャ概要](01-Architecture-Overview.md)
- HOROSの基本構造
- 主要コンポーネント
- データモデル階層
- データフロー
- Java実装時の考慮事項

### 2. [DICOMファイル処理](02-DICOM-File-Processing.md)
- DicomFileクラスの解析
- DICOMファイルの読み込み
- メタデータ抽出
- 非DICOM形式のサポート
- 文字エンコーディング処理

### 3. [画像表示・レンダリング](03-Image-Display-Rendering.md)
- DCMPixクラスの解析
- 画像データの読み込みと変換
- Window Level/Widthの適用
- DCMViewクラスの解析
- マウス操作（ズーム、パン、WL/WW調整）
- カラーマッピング（CLUT）

### 4. [DICOM通信](04-DICOM-Network-Communication.md)
- C-STORE（送信）の実装
- C-FIND（検索）の実装
- C-MOVE（取得）の実装
- C-ECHO（接続確認）の実装
- エラーハンドリングと進捗管理

### 5. [データベース管理](05-Database-Management.md)
- DicomDatabaseクラスの解析
- SQLiteスキーマ設計
- ファイルインポート機能
- 検索機能
- データモデルクラス

### 6. [UIコンポーネント](06-UI-Components.md)
- BrowserController（ブラウザウィンドウ）の実装
- ViewerController（ビューワーウィンドウ）の実装
- プレビューパネル
- サムネイルパネル
- ツールバーとメニュー

### 7. [画像解析機能](07-Image-Analysis.md)
- ROI（Region of Interest）の実装
- 統計計算（平均、標準偏差、最小、最大）
- 測定機能（距離、角度）
- ROI管理

### 8. [dcm4che使用ガイド](08-dcm4che-Usage-Guide.md)
- dcm4che-5.34.1の依存関係設定
- DICOMファイルの読み書き
- ネットワーク通信
- 画像処理
- パフォーマンス最適化

### 9. [設定管理](09-Configuration-Management.md)
- DICOMデータフォルダとDB保存フォルダの設定
- 設定ファイルの管理
- 設定ダイアログの実装
- フォルダ検証

### 10. [国際化（i18n）](10-Internationalization.md)
- 日本語と英語のリソースファイル
- リソースバンドル管理
- 動的な言語切り替え
- UIコンポーネントの国際化

### 11. [MPR/3D画像作成機能](11-MPR-3D-Image-Generation.md)
- MPR（Multi-Planar Reconstruction）の実装
- Volume Rendering（ボリュームレンダリング）
- Curved MPR（曲線平面再構成）
- 3D画像生成のアーキテクチャ

### 12. [AI画像解析機能](12-AI-Image-Analysis.md)
- AIモデルの統合方法
- ONNX、TensorFlow、DJLの使用
- 前処理・後処理の実装
- セグメンテーション、検出、分類機能

### 13. [PACS機能](13-PACS-Functionality.md)
- DICOMサーバー機能（SCP）
- C-STORE SCP（画像受信）
- C-FIND SCP（検索サーバー）
- C-MOVE SCP（取得サーバー）
- 自動ルーティング
- スケジュール機能
- PACS設定管理

### 14. [Vet-System連携と操作性の継承](14-Vet-System-Integration.md)
- Vet-Systemの操作性パターン
- UIレイアウトとカラースキーム
- スタディ一覧の表示
- ツールバーの実装
- Vet-Systemとのデータ連携
- テキストベースの連携方式

### 15. [JJDICOMViewer-Lite連携](15-JJDICOMViewer-Lite-Integration.md)
- JJDICOMViewer-Liteの位置づけ（CD/DVD同梱用）
- 操作性の統一
- ポータブル実行の実装
- 軽量化の要件
- CD/DVD構成の推奨

### 16. [CD/DVD自動インポート機能](16-Auto-Import-CD-DVD.md)
- CD/DVDドライブの自動検出（Windows、macOS、Linux対応）
- DICOMファイルの自動スキャン
- 自動インポート機能
- お渡し用CD/DVD作成機能
- Vet-Systemとの連携（共通ライブラリ）
- 進捗表示とエラーハンドリング

### 17. [追加機能](17-Additional-Features.md)
- 匿名化機能
- レポート機能（Structured Report含む）
- 印刷機能（DICOM Print SCU）
- WADO機能（Web Access to DICOM Objects）
- ヒストグラム機能
- ログ機能

## 実装分析ドキュメント

### [Weasis実装分析](WEASIS-IMPLEMENTATION-ANALYSIS.md)
- Weasis-4.6.5の画像読み込み処理の分析
- Window Level/Width適用方法の分析
- Java実装への適用方法

### [HOROS Window Level/Width完全分析](HOROS-WW-WL-COMPLETE-ANALYSIS.md)
- HOROS-20240407のWindow Level/Width処理の包括的分析
- RGB画像へのWW/WL適用
- 8-bit変換処理
- 初期値の決定方法

### [HOROS-20240407「2D Viewer」メニュー完全統合ドキュメント](02-2D-Viewer-Menu-Complete.md)
- ビューアウィンドウ側「2D Viewer」メニュー（全21項目）
- ブラウザー/ツールウィンドウ側「2D Viewer」メニュー
- 設定伝播・同期機能の詳細
- 実装状況と優先度

### [HOROS-20240407 画像読み込み処理の徹底解析と修正計画](56-HOROS-Image-Loading-Analysis.md)
- HOROS-20240407の処理順序（setIndex:メソッド）
- 現在の実装との違い
- 現在の不具合（CR/CT/MR画像の問題）
- 修正方針とリファクタリング計画

### [HOROS-20240407 tileWindows完全仕様](HOROS-TILEWINDOWS-DETAILED-SPECIFICATION.md)
- displayViewersメソッドの実装
- tileWindowsメソッドの行数・列数計算
- 各枚数での配置パターン（1〜30枚）
- 最大ビューワー数について

### [HOROS-20240407 ウィンドウサイズ変更時の画像倍率調整の仕様](HOROS-WINDOW-RESIZE-IMAGE-SCALE.md)
- setWindowFrameでの画像倍率の扱い
- ウィンドウサイズ変更時の挙動
- 現在のJava実装との比較

### [UIコンポーネント - ViewerController完全版](06-UI-Components-ViewerController-Complete.md)
- ViewerControllerの実装状況
- ウィンドウ操作（ダブルクリック最大化、右クリック）
- 画像表示と操作
- 複数ビューワーの配置（tileWindows）

## 実装の順序

推奨される実装順序：

1. **設定管理と国際化** - 設定ファイルとリソースバンドルの実装
2. **dcm4cheライブラリの統合** - 基本的なDICOM処理機能
3. **DICOMファイル処理** - ファイル読み込みとメタデータ抽出
4. **データベース管理** - SQLiteデータベースの実装
5. **画像表示** - 基本的な画像表示機能（JJDICOMViewer-Liteと共通の操作仕様）
6. **UIコンポーネント** - ブラウザとビューワーの実装
7. **DICOM通信** - ネットワーク機能の実装
8. **画像解析** - ROIと測定機能の実装
9. **PACS機能** - DICOMサーバー機能と自動ルーティングの実装
10. **MPR/3D画像作成** - MPRとVolume Renderingの実装（オプション）
11. **AI画像解析** - AIモデルの統合（オプション）

## 共通化事項

### マウス操作（JJDICOMViewer-Liteと共通）

以下の操作はJJDICOMViewer-Liteと共通化されています：

- **パンニング**: 左クリック + ドラッグ（SHIFTキーが押されていない場合）
- **ズーム**: CTRL + マウスホイール
- **Window Level/Width**: SHIFT + 左クリック + ドラッグ
  - 縦方向: Window Center（上にドラッグで増加）
  - 横方向: Window Width（右にドラッグで増加）
- **スライス移動**: 通常のマウスホイール（CTRLが押されていない場合）

詳細は[画像表示・レンダリング](03-Image-Display-Rendering.md)を参照してください。

**注意**: JJDICOMViewer-Liteの実装（`C:\Users\jam11\JJDicomViewer-Lite\src\main\java\com\jjdicomviewer\ui\ImageViewerPanel.java`）を基準としています。

### キーボードショートカット（HOROS準拠）

HOROS-20240407で実装されているキーボードショートカット：

- **Hキー**: 左右反転（FlipHorizontal）
- **Vキー**: 上下反転（FlipVertical）
- **Rキー**: 右回転（90度時計回り）- 実装推奨
- **Lキー**: 左回転（90度反時計回り）- 実装推奨

詳細は[画像表示・レンダリング](03-Image-Display-Rendering.md)を参照してください。

## 技術スタック

### 元実装（HOROS）
- Objective-C / Objective-C++
- Cocoa Framework
- OpenGL
- DCMTK
- Core Data

### Java実装
- Java 21
- Java Swing（マルチプラットフォーム対応：Windows、macOS、Linux）
  - **注意**: JavaFXはインストーラー作成時の問題により除外
- Java2D
- dcm4che-5.34.1
- SQLite (JDBC)

## 注意事項

1. **メモリ管理**: 大きなDICOM画像の適切な処理
2. **スレッド処理**: バックグラウンド処理とEDTでのUI更新
3. **パフォーマンス**: 画像レンダリングとデータベースクエリの最適化
4. **エラーハンドリング**: 堅牢なエラー処理の実装

## 関連プロジェクト

### Vet-system

獣医向け受付・電子カルテシステム。本プロジェクトは、Vet-Systemの一部として機能します。

### JJDICOMViewer-Lite

**位置づけ**: CD/DVD同梱用の簡易DICOMビューワー

**用途**:
- お渡し用のCD/DVD作成時に同梱する簡易ビューワー
- 患者へのDICOM画像の提供時に使用
- 軽量で起動が速い

**要件**:
- 軽量でコンパクトな実装
- インストール不要で実行可能（ポータブル）
- CD/DVDから直接実行可能
- 基本的な画像表示機能（ズーム、パン、WL/WW調整）
- 操作性はJJDICOMViewerと共通

**操作性の共通化**:
- パンニング: 左クリック + ドラッグ（SHIFTキーが押されていない場合）
- ズーム: CTRL + マウスホイール
- Window Level/Width: SHIFT + 左クリック + ドラッグ
- スライス移動: 通常のマウスホイール（CTRLが押されていない場合）

詳細は[プロジェクト概要](00-Project-Overview.md)を参照してください。

## ライセンス

HOROSはLGPLライセンスの下で公開されています。Java実装時も同様のライセンスを遵守してください。

