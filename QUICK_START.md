# クイックスタートガイド

## 初回セットアップ

### 1. 前提条件の確認

- Java 17以上がインストールされていること
  ```bash
  java -version
  ```

### 2. Gradle Wrapperの準備

初回実行時、Gradle WrapperがGradleを自動的にダウンロードします。
Gradle Wrapper JARファイルが必要な場合は、以下のコマンドでダウンロードできます：

```bash
# Windows PowerShell
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar" -OutFile "gradle\wrapper\gradle-wrapper.jar"
```

または、Gradleがインストールされている場合は：

```bash
gradle wrapper
```

### 3. ビルド

```bash
# Windows
gradlew.bat build

# Linux/macOS
./gradlew build
```

### 4. 実行

```bash
# Windows
gradlew.bat run

# Linux/macOS
./gradlew run
```

## 基本的な使い方

### DICOMファイルのインポート

1. アプリケーションを起動
2. 「インポート」ボタンをクリック
3. DICOMファイルが含まれるフォルダを選択
4. インポートの完了を待つ

### 画像の表示

1. 左側のスタディ一覧からスタディを選択
2. 中央のシリーズ一覧からシリーズを選択
3. 右側のビューアに画像が表示されます

### 画像操作

- **ウィンドウ/レベル**: Shift + 左ドラッグ
- **ズーム**: Ctrl + ホイール
- **パン**: 左ドラッグ
- **スライス移動**: ホイール

## データベースの場所

アプリケーションのデータベースは以下の場所に保存されます：

- Windows: `C:\Users\<ユーザー名>\.jjdicomviewerlite\jjdicomviewerlite.db`
- macOS/Linux: `~/.jjdicomviewerlite/jjdicomviewerlite.db`

## DICOMファイルの保存先

インポートしたDICOMファイルは以下の場所に保存されます：

- Windows: `C:\Users\<ユーザー名>\.jjdicomviewerlite\data`
- macOS/Linux: `~/.jjdicomviewerlite/data`

設定ダイアログ（「設定」→「設定...」）から変更できます。

## ログファイル

ログファイルは以下の場所に保存されます：

- プロジェクトルートの `logs/` ディレクトリ（開発時）
- アプリケーションディレクトリ（本番環境）

## トラブルシューティング

### アプリケーションが起動しない

1. Java 17以上がインストールされているか確認
2. `JAVA_HOME`環境変数が設定されているか確認
3. ログファイルを確認

### 画像が表示されない

1. DICOMファイルが正しくインポートされているか確認
2. データベースにデータが登録されているか確認
3. ログファイルでエラーメッセージを確認

### ビルドエラー

1. インターネット接続を確認（依存関係のダウンロードに必要）
2. プロキシ設定を確認
3. Gradle Wrapperが正しく設定されているか確認

