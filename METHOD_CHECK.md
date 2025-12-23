# BrowserController.m メソッド実装状況チェック

## 統計
- HOROS-20240407 BrowserController.m: 約239個のメソッド定義
- 現在のJava実装: 約149個のメソッド
- **不足: 約90個のメソッド**

## 重要な未実装メソッド候補

以下は、grep結果から見つかった未実装の可能性がある重要なメソッド：

1. `dealloc` - メモリ管理
2. `vImageThread:` - 画像処理スレッド
3. `multiThreadedImageConvert:...` - 画像変換
4. `rebuildViewers:` - ビューア再構築
5. `addFilesToDatabase:...` (複数のオーバーロード) - ファイル追加
6. `checkForExistingReport:dbFolder:` - レポートチェック
7. `testAutorouting` - 自動ルーティングテスト
8. `selectedStudy` - 選択されたStudy取得
9. `applyRoutingRule:` - ルーティングルール適用
10. `regenerateAutoCommentsThread:` - コメント自動生成
11. `regenerateAutoComments:` - コメント自動生成
12. `databaseLastModification` - データベース最終更新時刻
13. `copyFilesThread:` - ファイルコピースレッド
14. `copyToDBFolder:` - DBフォルダへコピー
15. `copyFilesIntoDatabaseIfNeeded:options:` - ファイルコピー
16. `ReBuildDatabaseSheet:` - データベース再構築シート
17. `autoCleanDatabaseDate:` - 自動クリーン
18. `isHardDiskFull` - ハードディスク満杯チェック
19. `autoCleanDatabaseFreeSpaceWarning:` - 空き容量警告
20. `autoCleanDatabaseFreeSpace:` - 自動クリーン空き容量
21. `searchDeadProcesses` - デッドプロセス検索
22. `autoretrievePACSOnDemandSmartAlbum:` - PACS自動取得
23. `imagesArray:...` (複数のオーバーロード) - 画像配列取得
24. `imagesPathArray:` - 画像パス配列取得
25. `resetROIsAndKeysButton` - ROI/Keyボタンリセット
26. `comparativeServers` - 比較サーバー
27. `stringForSearchType:` - 検索タイプ文字列
28. `distantStudiesForSearchString:type:` - 遠隔Study検索
29. `searchForSearchField:` - 検索フィールド検索
30. `distantStudiesForIntervalFrom:to:` - 時間間隔で遠隔Study検索
31. `searchForTimeIntervalFromTo:` - 時間間隔検索
32. `distantStudiesForSmartAlbum:` - スマートアルバムで遠隔Study検索
33. `searchForSmartAlbumDistantStudies:` - スマートアルバム検索
34. `subSearchForComparativeStudies:` - 比較Studyサブ検索
35. `searchForComparativeStudies:` - 比較Study検索
36. `selectDatabaseOutline` - アウトライン選択
37. `mergeSeriesExecute:` - シリーズ統合実行
38. `mergeSeries:` - シリーズ統合
39. `unifyStudies:` - Study統合
40. `mergeStudies:` - Study統合
41. `proceedDeleteObjects:tree:` - オブジェクト削除実行
42. `proceedDeleteObjects:` - オブジェクト削除実行
43. `delObjects:tree:` - オブジェクト削除
44. `delObjects:` - オブジェクト削除
45. `columnsMenuAction:` - カラムメニューアクション
46. `databaseOpenStudy:withProtocol:` - Studyオープン（プロトコル付き）
47. `displayWaitWindowIfNecessary` - 待機ウィンドウ表示
48. `closeWaitWindowIfNecessary` - 待機ウィンドウ閉じる
49. `databaseOpenStudy:` - Studyオープン
50. `databasePressed:` - データベース押下
51. `displayStudy:object:command:` - Study表示
52. `findObject:table:execute:elements:` - オブジェクト検索
53. `loadNextPatient:...` - 次の患者読み込み
54. `loadNextSeries:...` - 次のシリーズ読み込み
55. `loadSeries:...` - シリーズ読み込み
56. `exportDBListOnlySelected:` - DBリストエクスポート
57. `pasteImageForSourceFile:` - 画像貼り付け
58. `paste:` - 貼り付け
59. `copy:` - コピー
60. `saveDBListAs:` - DBリスト保存
61. `getDCMPixFromViewerIfAvailable:frameNumber:` - DCMPix取得
62. `matrixPressed:` - マトリックス押下
63. `matrixDoublePressed:` - マトリックスダブルクリック
64. `matrixInit:` - マトリックス初期化
65. `matrixNewIcon::` - マトリックス新規アイコン
66. `pdfPreview:` - PDFプレビュー
67. `buildThumbnail:` - サムネイル構築
68. `buildAllThumbnails:` - 全サムネイル構築
69. `resetWindowsState:` - ウィンドウ状態リセット
70. `windowDidChangeScreen:` - ウィンドウスクリーン変更
71. `addAlbumsFile:` - アルバムファイル追加
72. `addAlbums:` - アルバム追加
73. `initContextualMenus` - コンテキストメニュー初期化
74. `annotMenu:` - 注釈メニュー
75. `addSmartAlbum:` - スマートアルバム追加
76. `addAlbum:` - アルバム追加
77. `deleteAlbum:` - アルバム削除
78. `albumTableDoublePressed:` - アルバムテーブルダブルクリック
79. `albumArray` - アルバム配列
80. `currentAlbumID:` - 現在のアルバムID
81. `findSeriesUID:` - Series UID検索
82. `sendFilesToCurrentBonjourDB:` - Bonjour DBへファイル送信
83. `sendFilesToCurrentBonjourGeneratedByOsiriXDB:` - Bonjour DBへファイル送信（OsiriX生成）
84. `comparativeRetrieve:` - 比較取得
85. `retrieveComparativeStudy:...` (複数のオーバーロード) - 比較Study取得
86. `doubleClickComparativeStudy:` - 比較Studyダブルクリック
87. `selectSubSeriesAndOpen:` - サブシリーズ選択してオープン
88. `selectAll3DSeries:` - 全3Dシリーズ選択
89. `reparseIn3D:` - 3D再解析
90. `reparseIn4D:` - 4D再解析
91. `selectAll4DSeries:` - 全4Dシリーズ選択

その他、多数のメソッドが未実装の可能性があります。

