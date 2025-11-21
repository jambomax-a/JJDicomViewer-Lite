# テストガイド

## 前提条件

- Java 17以上がインストールされていること
- Gradle Wrapperが利用可能であること（初回実行時に自動ダウンロード）

## テストの実行

### 1. ユニットテストの実行

```bash
# Windows
gradlew.bat test

# Linux/macOS
./gradlew test
```

### 2. ビルドとコンパイルチェック

```bash
# Windows
gradlew.bat build

# Linux/macOS
./gradlew build
```

### 3. アプリケーションの実行

```bash
# Windows
gradlew.bat run

# Linux/macOS
./gradlew run
```

## 手動テスト手順

### 1. アプリケーション起動の確認

1. `gradlew run` を実行
2. メインウィンドウが表示されることを確認
3. スタディ一覧が空であることを確認（初回起動時）

### 2. DICOMファイルのインポート

1. 「インポート」ボタンをクリック
2. DICOMファイルが含まれるフォルダを選択
3. 進捗バーでインポートの進行状況を確認
4. インポート完了後、スタディ一覧にデータが表示されることを確認

### 3. スタディ・シリーズの選択

1. スタディ一覧からスタディを選択
2. シリーズ一覧にシリーズが表示されることを確認
3. シリーズを選択
4. ビューアに画像が表示されることを確認

### 4. ビューア機能のテスト

#### ウィンドウ/レベル調整
- **Shift + 左ドラッグ**: ウィンドウ/レベルを調整
- 横方向: ウィンドウ幅
- 縦方向: ウィンドウセンター
- 下部の入力フィールドで直接数値を入力可能
- リセットボタンでデフォルト値に戻る

#### ズーム
- **Ctrl + ホイール**: ズームイン/アウト
- 「リセット」ボタンでズームをリセット

#### パン
- **左ドラッグ**: 画像を移動
- 「リセット」ボタンでパンをリセット

#### スライスナビゲーション
- ホイールでスライスを移動
- 下部のスライダーでスライスを移動
- 複数スライスがある場合、前後のスライスに移動可能

## テスト用DICOMファイル

テスト用のDICOMファイルが必要な場合、以下のリソースを利用できます：

- [DICOM Sample Images](https://www.dclunie.com/images/)
- [Osirix Sample Data](https://www.osirix-viewer.com/resources/dicom-image-library/)

## トラブルシューティング

### ビルドエラー

- Java 17以上がインストールされているか確認
- `JAVA_HOME`環境変数が正しく設定されているか確認

### 実行時エラー

- ログファイル（`logs/jjdicomviewerlite.log`）を確認
- データベースファイル（`~/.jjdicomviewerlite/jjdicomviewerlite.db`）の権限を確認

### 画像が表示されない

- DICOMファイルが正しくインポートされているか確認
- データベースにインスタンスが登録されているか確認
- ログファイルでエラーメッセージを確認

## テストカバレッジ

現在のテストクラス：

- `StudyTest`: Studyクラスの基本機能
- `DatabaseManagerTest`: データベースの初期化
- `DicomReaderTest`: DICOMリーダーの基本機能

今後追加予定：

- `SeriesTest`: Seriesクラスのテスト
- `InstanceTest`: Instanceクラスのテスト
- `StudyRepositoryTest`: リポジトリのCRUD操作
- `ImportServiceTest`: インポート機能のテスト
- `ImageViewerPanelTest`: ビューアパネルのテスト

