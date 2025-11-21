# JJ Dicom Viewer 簡易版

動物病院向けのスタンドアローンDICOMビューワーアプリケーション

## 概要

　JJ Dicom Viewer 簡易版は、中小規模の動物病院向けに開発されたDICOM画像ビューアです。
　Windows/macOS/Linuxで動作するクロスプラットフォームアプリケーションです。
　紹介報告CD/DVDの読み込みができないなどの画像確認に使用していただければと思い開発はじめました。
　JAVA（Swing）を開発言語にすることでマルチプラットフォーム対応し、どのデスクトップPCでも確認できるよう設計しています。
　バグ・要望対応は本職がありますので空き時間に行いますが、すぐにかつ全てには対応できないです。ご理解ください。
　Dicom通信機能や画像の回転・反転・測定などの機能はPROバージョンとして開発予定です。

## システム要件

- **OS**: Windows 10/11、macOS 10.15+、Linux（Ubuntu 20.04+）
- **Java**: Java 17 以上（JRE または JDK）

## ダウンロード

[Releases](https://github.com/jambomax-a/JJDicomViewer-Lite/releases) から最新版をダウンロードしてください。

## インストール

[Releases](https://github.com/jambomax-a/JJDicomViewer-Lite/releases) から、お使いのOS用の圧縮ファイルをダウンロードして解凍してください。

### Windows
1. `windowsinstaller.zip` をダウンロード
2. 解凍して `.msi` ファイルを実行
3. インストール後、スタートメニューから起動

### macOS
1. `macosinstaller.zip` をダウンロード
2. 解凍して `.dmg` ファイルを開く
3. アプリケーションフォルダにドラッグ&ドロップ

### Linux
1. `linuxinstaller.zip` をダウンロード
2. 解凍して `.deb` または `.rpm` ファイルを取得
3. パッケージマネージャーでインストール

## 使い方

### 基本的な使い方

1. **インポート**: 「インポート」ボタンからDICOMフォルダを選択
2. **スタディ選択**: 左側のスタディ一覧から選択
3. **シリーズ選択**: 中央のシリーズ一覧から選択
4. **画像表示**: 右側のビューアに画像が表示されます

### 操作

- **左ドラッグ**: パン（画像移動）
- **Shift + 左ドラッグ**: ウィンドウ/レベル調整
- **Ctrl + ホイール**: ズーム
- **ホイール**: スライス移動
- **リセットボタン**: ズーム、パン、ウィンドウ/レベルをリセット

### 設定

「設定」メニューから以下を変更できます：
- DICOMファイルの保存先フォルダ
- データベースファイルの保存先
- 言語（日本語/English）

### 言語ファイルの追加

アプリケーションは、初回起動時にJAR内の言語ファイルを外部ファイルに自動コピーします。新しい言語を追加する場合は、以下の手順で追加できます：

1. **言語ファイルの作成**: `messages_<言語コード>.yaml`形式で作成（例: `messages_fr.yaml`）
2. **言語ファイルをコピー**: 
   - Windows: `%USERPROFILE%\.jjdicomviewerlite\language\`（通常は`C:\Users\<ユーザー名>\.jjdicomviewerlite\language\`）
     - 注: `Program Files`にインストールされている場合は、ユーザーのホームディレクトリにフォールバックします
     - `%LOCALAPPDATA%\Programs\JJDicomViewer-Lite\app\language\`にインストールされている場合は、そちらが優先されます
   - macOS: `<App>.app/Contents/app/language/`
   - Linux: `<インストール先>/JJDicomViewer-Lite/lib/app/language/`
3. **アプリケーションを再起動**: 新しい言語が設定画面に表示されます

**注意**: 
- 初回起動時に`language`フォルダが自動作成され、JAR内の言語ファイル（日本語/英語）が自動コピーされます
- 言語ファイルが見つからない場合は、JAR内のリソース（日本語/英語）が使用されます
- ログファイル（`%USERPROFILE%\.jjdicomviewerlite\logs\jjdicomviewer.log`）で、言語ディレクトリのパスを確認できます

## 主な機能

- DICOMファイルの読み込みと表示（フォルダ指定対応）
- 2Dビューア（ウィンドウ/レベル調整、ズーム、パン、スライスナビゲーション）
- スタディ/シリーズ管理（SQLiteデータベース）
- ローカルストレージへの自動コピー
- 対応モダリティ: CR、DR、US、CT、MRI

## 技術スタック

- **UI**: Java Swing（Java標準）
- **DICOM処理**: 標準DICOMライブラリ
- **データベース**: SQLite
- **ビルド**: Gradle
- **パッケージング**: jpackage

> **注意**: エンドユーザーはインストーラーからインストールするだけです。開発環境の設定は不要です。DICOMファイルは標準的なDICOM形式に対応しています。

## ビルド

```bash
./gradlew build
```

## 実行

```bash
./gradlew run
```

## インストーラーの作成

### Windows
```bash
./gradlew createWindowsInstaller
```

### macOS
```bash
./gradlew createMacOSInstaller
```

### Linux
```bash
./gradlew createLinuxInstaller
```

生成されたインストーラーは `build/installer/` に保存されます。

## トラブルシューティング

### 画像が表示されない
- DICOMファイルが正しく読み込まれているか確認してください
- ログファイル（`~/.jjdicomviewerlite/logs/jjdicomviewerlite.log`）を確認してください

### インポートが遅い
- 大量のファイルをインポートする場合、時間がかかることがあります
- 進捗バーで進捗状況を確認できます

## ライセンス

MIT License

詳細は [LICENSE](LICENSE) ファイルを参照してください。

## 貢献

バグ報告や機能要望は [Issues](https://github.com/jambomax-a/JJDicomViewer-Lite/issues) までお願いします。

## 開発者向け

### インストーラー作成

インストーラー作成手順については [BUILD_INSTALLER.md](BUILD_INSTALLER.md) を参照してください。

**重要なポイント**:
- Fat JAR（依存関係を含む）を作成する必要があります
- `build.gradle`の設定は変更しないでください（再現性のため）

## 変更履歴

[CHANGELOG.md](CHANGELOG.md) を参照してください。
