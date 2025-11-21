# 変更履歴

このプロジェクトのすべての重要な変更は、このファイルに記録されます。

形式は [Keep a Changelog](https://keepachangelog.com/ja/1.0.0/) に基づいています。

## [0.1.0] - 2024-XX-XX

### 追加
- DICOMファイルの読み込みと表示機能
- フォルダ指定によるインポート機能
- スタディ/シリーズ一覧表示
- 2Dビューア（ウィンドウ/レベル調整、ズーム、パン、スライスナビゲーション）
- SQLiteデータベースによるメタデータ管理
- ローカルストレージへの自動コピー機能
- 設定ダイアログ（保存先フォルダの変更）
- RAWピクセルデータの直接読み込みによる高画質表示
- 対応モダリティ: CR、DR、US、CT、MRI

### 技術仕様
- Java 17+ 対応
- Java Swing を使用（Java標準）
- 標準DICOMライブラリによるDICOM処理
- RAWピクセルデータの直接読み込みによる高解像度表示
- クロスプラットフォーム対応（Windows/macOS/Linux）

[0.1.0]: https://github.com/jambomax-a/JJDicomViewer-Lite/releases/tag/v0.1.0

