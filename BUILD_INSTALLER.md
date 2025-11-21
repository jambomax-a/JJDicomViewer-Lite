# インストーラー作成手順

このドキュメントでは、JJDicomViewer-Liteのインストーラーを作成する手順を説明します。

## 前提条件

### Windows版
- **JDK 17以上**（JDK 25推奨）
- **WiX Toolset 3.14以上**（MSI作成用）
  - ダウンロード: https://wixtoolset.org/
  - インストール先: `C:\Program Files (x86)\WiX Toolset v3.14\bin`
- **Gradle 9.0以上**

### macOS版
- **JDK 17以上**
- **Gradle 9.0以上**

### Linux版
- **JDK 17以上**
- **Gradle 9.0以上**

## 重要な設定

### build.gradleの設定

#### 1. Fat JAR作成（依存関係を含むJAR）
```gradle
jar {
    manifest {
        attributes('Main-Class': 'com.jjdicomviewer.app.JJDicomViewerApp')
    }
    from {
        configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) }
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```
**重要**: この設定がないと、インストーラーに依存関係（SLF4J、dcm4che、Jackson等）が含まれず、「Failed to launch JVM」エラーが発生します。

#### 2. jpackageの設定
- `--runtime-image`を指定**しない**（jpackageに自動生成させる）
- `--java-options --add-modules=ALL-MODULE-PATH`を追加（モジュール化されていないJAR用）
- `--input build/libs`でfat JARを指定

## インストーラー作成手順

### Windows版（ローカル）

```powershell
# 1. クリーンビルド
./gradlew clean build

# 2. Windowsインストーラー作成
./gradlew createWindowsInstaller

# 3. 作成されたインストーラーの確認
dir build\installer\*.msi
```

**作成されるファイル**: `build/installer/JJDicomViewer-Lite-0.1.0.msi`

### macOS版（GitHub Actions）

GitHub Actionsで自動的に作成されます。手動で実行する場合：

```bash
./gradlew createMacOSInstaller
```

### Linux版（GitHub Actions）

GitHub Actionsで自動的に作成されます。手動で実行する場合：

```bash
./gradlew createLinuxInstaller
```

## GitHub Actionsでの自動ビルド

### 手動実行
1. GitHubのリポジトリページで「Actions」タブを開く
2. 「Build Installers」ワークフローを選択
3. 「Run workflow」をクリック
4. ブランチを選択して「Run workflow」をクリック

### タグプッシュで自動実行
```bash
git tag v0.1.0
git push origin v0.1.0
```

**注意**: Windows版は`continue-on-error: true`のため、失敗しても他のビルドは続行されます。失敗した場合はローカルで作成してください。

## トラブルシューティング

### 「Failed to launch JVM」エラー

**原因**:
1. Fat JARが作成されていない（依存関係が含まれていない）
2. ランタイムイメージに必要なモジュールが不足している

**解決方法**:
1. `build.gradle`の`jar`タスクで`from configurations.runtimeClasspath`が設定されているか確認
2. JARファイルのサイズを確認（依存関係を含むと18MB以上）
3. 新しいインストーラーを作成

### GitHub Actionsで作成したMSIインストーラーが起動しない

**症状**: インストールは成功するが、起動時に何も表示されず終了する

**原因**:
1. インストーラーに含まれるJARファイルがFat JARではない
2. ランタイムイメージに必要なモジュールが不足している
3. 起動スクリプトのパスが正しくない

**確認方法**:
1. GitHub Actionsのログで「Using JAR file」を確認（サイズが17MB以上であることを確認）
2. ローカルで作成したインストーラーと比較
3. インストール先の`app`フォルダ内のJARファイルのサイズを確認

**解決方法**:
1. **コマンドプロンプトから直接起動してエラーメッセージを確認**:
   
   まず、ファイルが存在するか確認:
   ```cmd
   dir "C:\Program Files\JJDicomViewer-Lite\runtime\bin\java.exe"
   dir "C:\Program Files\JJDicomViewer-Lite\app\JJDicomViewer-Lite-0.1.0.jar"
   ```
   
   JARファイルを直接実行（引用符を使用）:
   ```cmd
   cd "C:\Program Files\JJDicomViewer-Lite"
   ".\runtime\bin\java.exe" -jar ".\app\JJDicomViewer-Lite-0.1.0.jar"
   ```
   
   または、フルパスを使用:
   ```cmd
   "C:\Program Files\JJDicomViewer-Lite\runtime\bin\java.exe" -jar "C:\Program Files\JJDicomViewer-Lite\app\JJDicomViewer-Lite-0.1.0.jar"
   ```
   
   起動スクリプトを使用（管理者権限が必要な場合あり）:
   ```cmd
   cd "C:\Program Files\JJDicomViewer-Lite"
   ".\JJDicomViewer-Lite.exe"
   ```
   
2. **イベントビューアーでエラーログを確認**:
   - Windowsキー + R で「eventvwr.msc」を実行
   - 「Windowsログ」→「アプリケーション」を開く
   - エラーまたは警告を確認

3. **ローカルで作成したインストーラーと比較**:
   - ローカルで`./gradlew clean build createWindowsInstaller`を実行
   - 作成されたインストーラーが動作するか確認
   - GitHub Actions版との違いを確認

4. **JARファイルの内容を確認**:
   - `jar tf JJDicomViewer-Lite-0.1.0.jar | findstr "org.slf4j"`
   - 依存関係が含まれているか確認

### 「ClassNotFoundException: com.jjdicomviewer.app.JJDicomViewerApp」エラー

**症状**: インストール後、起動時に「メイン・クラスを検出およびロードできませんでした」というエラーが表示される

**原因**:
1. JARファイル作成時に、自分のクラスファイルが依存関係のJARファイルに含まれる同名クラスで上書きされている
2. `from sourceSets.main.output`が明示的に追加されていない

**解決方法**:
1. `build.gradle`の`jar`タスクで`from sourceSets.main.output`を明示的に追加しているか確認
2. 新しいJARファイルを作成: `./gradlew clean jar`
3. 新しいインストーラーを作成: `./gradlew createWindowsInstaller`

**確認方法**:
```cmd
jar tf build\libs\JJDicomViewer-Lite-0.1.0.jar | findstr "JJDicomViewerApp.class"
```
`com/jjdicomviewer/app/JJDicomViewerApp.class`が表示されればOK。

### JARファイルにMain-Class属性がない

**解決方法**:
`build.gradle`の`jar`タスクで`manifest { attributes('Main-Class': ...) }`が設定されているか確認

### WiX Toolsetが見つからない

**解決方法**:
1. WiX Toolset 3.14以上をインストール
2. インストール先を確認（通常は`C:\Program Files (x86)\WiX Toolset v3.14\bin`）
3. 環境変数`WIX_HOME`を設定（オプション）

## 確認事項

インストーラー作成後、以下を確認してください：

### Windows
1. **JARファイルのサイズ**: 18MB以上（依存関係を含む）
2. **ランタイムイメージ**: `build/runtime-image`フォルダが作成されているか
3. **インストール**: 正常にインストールできるか
4. **起動**: スタートメニューから起動できるか
5. **基本機能**: インポート、画像表示、WW/WL調整が動作するか

### macOS / Linux
**注意**: macOS/Linuxのインストーラーの動作確認は、実際の環境が必要です。
- **macOS**: Intel MacまたはApple Silicon Macが必要（AMD PCのVMでは動作しません）
- **Linux**: VMでテスト可能

**GitHub Actionsでの確認**:
- ビルドログで「Using runtime image」が表示されることを確認
- インストーラーファイル（`.dmg`、`.deb`、`.rpm`）が作成されることを確認
- ビルドが成功すれば、基本的には問題ありません

**実際の動作確認**:
- エンドユーザーやテスターに依頼する
- 可能であれば、実機でテストする

## バージョンアップ時の注意

1. `version`を`build.gradle`で更新
2. `./gradlew clean build createWindowsInstaller`を実行
3. 動作確認後、コミット・プッシュ

## 参考情報

- [jpackage公式ドキュメント](https://docs.oracle.com/en/java/javase/17/docs/specs/man/jpackage.html)
- [Gradle Application Plugin](https://docs.gradle.org/current/userguide/application_plugin.html)

