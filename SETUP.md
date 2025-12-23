# セットアップガイド

## Java 25のパース問題について

現在、Java 25がインストールされている場合、GradleのKotlin DSLがJava 25のバージョン文字列"25.0.1"をパースできない問題が発生しています。

## 解決方法

### 方法1: Java 21を使用する（推奨）

Java 21をインストールし、JAVA_HOMEをJava 21に設定してください。

```powershell
# Java 21のインストールパスを確認
# 例: C:\Program Files\Java\jdk-21

# JAVA_HOMEを設定
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
```

### 方法2: Gradleのバージョンを上げる

Gradleの最新バージョンを使用することで、Java 25のサポートが改善される可能性があります。

`gradle/wrapper/gradle-wrapper.properties`を編集して、最新のGradleバージョンを指定してください。

### 方法3: 一時的な回避策

Java 21がインストールされている場合、以下のコマンドでJava 21を使用してビルドできます：

```powershell
# Java 21のパスを指定してビルド
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\gradlew.bat build
```

## 現在の状況

- **Javaバージョン**: 25.0.1
- **Gradleバージョン**: 8.11
- **Kotlinバージョン**: 2.0.20
- **問題**: KotlinのJavaVersion.parse()が"25.0.1"をパースできない

## 推奨事項

プロジェクトはJava 21をターゲットとしているため、Java 21を使用することを強く推奨します。

