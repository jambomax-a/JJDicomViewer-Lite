# Javaセットアップガイド

## Javaのインストール確認

### 1. Javaのバージョン確認

新しいPowerShellまたはコマンドプロンプトを開いて、以下を実行：

```powershell
java -version
```

正常に表示されれば、Javaは正しくインストールされています。

### 2. Javaが見つからない場合

#### 方法1: 環境変数の再読み込み

1. PowerShellを**管理者として実行**
2. 以下のコマンドで環境変数を再読み込み：

```powershell
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
```

3. 再度確認：

```powershell
java -version
```

#### 方法2: JAVA_HOMEの手動設定

Javaのインストール場所を確認：

```powershell
# 一般的な場所を確認
Get-ChildItem "C:\Program Files\Java" -ErrorAction SilentlyContinue
Get-ChildItem "C:\Program Files (x86)\Java" -ErrorAction SilentlyContinue
Get-ChildItem "$env:LOCALAPPDATA\Programs" -ErrorAction SilentlyContinue | Where-Object { $_.Name -like '*java*' }
```

見つかったら、JAVA_HOMEを設定：

```powershell
# 例: Java 21が C:\Program Files\Java\jdk-21 にインストールされている場合
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-21", "User")
[System.Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\Program Files\Java\jdk-21\bin", "User")
```

#### 方法3: システムの環境変数設定（GUI）

1. Windowsキー + R を押す
2. `sysdm.cpl` と入力してEnter
3. 「詳細設定」タブを開く
4. 「環境変数」ボタンをクリック
5. 「システム環境変数」で「新規」をクリック
6. 変数名: `JAVA_HOME`、変数値: Javaのインストールパス（例: `C:\Program Files\Java\jdk-21`）
7. `Path`変数を編集し、`%JAVA_HOME%\bin` を追加
8. すべてのウィンドウを閉じて、新しいPowerShellを開く

## Javaのインストール（未インストールの場合）

### OpenJDK（推奨）

1. [Adoptium](https://adoptium.net/) にアクセス
2. Java 17 LTS または Java 21 LTS をダウンロード
3. インストーラーを実行
4. 「Set JAVA_HOME variable」オプションを有効にする

### Oracle JDK

1. [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) にアクセス
2. 最新のLTSバージョンをダウンロード
3. インストーラーを実行

## インストール後の確認

新しいPowerShellウィンドウを開いて：

```powershell
java -version
javac -version
echo $env:JAVA_HOME
```

すべて正常に表示されれば、準備完了です。

## JJDicomViewer-Liteのテスト実行

Javaの設定が完了したら：

```powershell
# テスト実行
.\gradlew.bat test

# ビルド
.\gradlew.bat build

# アプリケーション実行
.\gradlew.bat run
```

