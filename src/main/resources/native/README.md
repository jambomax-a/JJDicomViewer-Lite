# OpenCV Native Library 配置ガイド

このディレクトリには、プラットフォーム別のOpenCVネイティブライブラリを配置します。

## ディレクトリ構造

```
native/
├── windows-x86-64/
│   └── opencv_java.dll
├── macosx-x86-64/
│   ├── libopencv_java.dylib  (推奨)
│   └── libopencv_java.jnilib  (代替、.dylibがない場合)
├── macosx-arm64/
│   ├── libopencv_java.dylib  (推奨)
│   └── libopencv_java.jnilib  (代替、.dylibがない場合)
├── linux-x86-64/
│   └── libopencv_java.so
└── linux-arm64/
    └── libopencv_java.so
```

## ライブラリの取得方法（推奨順）

### 方法1: HOROS-20240407のWeasis JARから抽出（最も簡単）

HOROS-20240407のBinariesフォルダに含まれているWeasisのJARファイルから抽出します。

#### Windowsの場合

1. **.xzファイルを解凍**（7-Zipまたはxzコマンドを使用）
   ```powershell
   # 7-Zipを使用する場合
   7z x "horos-20240407\Binaries\weasis-extracted\weasis\bundle\weasis-opencv-core-windows-x86-64-4.2.0-dcm.jar.xz"
   
   # または、xzコマンドを使用する場合（Git Bashなど）
   xz -d "horos-20240407/Binaries/weasis-extracted/weasis/bundle/weasis-opencv-core-windows-x86-64-4.2.0-dcm.jar.xz"
   ```

2. **JARファイルからネイティブライブラリを抽出**
   ```powershell
   # JARファイルはZIP形式なので、解凍ツールで開く
   # または、jarコマンドを使用
   jar -xf "weasis-opencv-core-windows-x86-64-4.2.0-dcm.jar"
   
   # ネイティブライブラリは通常、以下のパスに含まれています：
   # - META-INF/native/windows-x86-64/opencv_java.dll
   # - または、ルートディレクトリの opencv_java.dll
   ```

3. **適切なディレクトリにコピー**
   ```powershell
   Copy-Item "opencv_java.dll" "src\main\resources\native\windows-x86-64\opencv_java.dll"
   ```

#### macOSの場合

1. **.xzファイルを解凍**
   ```bash
   # x86_64版（Intel Mac）
   xz -d "horos-20240407/Binaries/weasis-extracted/weasis/bundle/weasis-opencv-core-macosx-x86-64-4.2.0-dcm.jar.xz"
   
   # 注意: HOROS-20240407のWeasis JARにはARM64版（Apple Silicon Mac用）が含まれていません
   # ARM64版が必要な場合の対処法は下記を参照してください
   ```

2. **JARファイルからネイティブライブラリを抽出**
   ```bash
   jar -xf "weasis-opencv-core-macosx-x86-64-4.2.0-dcm.jar"
   
   # ネイティブライブラリを探す（.dylibまたは.jnilib）
   # 注意: WeasisのJARには.jnilibが含まれている場合があります
   find . -name "libopencv_java.dylib" -o -name "libopencv_java.jnilib"
   
   # または、すべてのファイルを確認
   find . -type f | grep -i opencv
   
   # または、JARファイルの中身を直接確認（解凍前）
   jar -tf "weasis-opencv-core-macosx-x86-64-4.2.0-dcm.jar" | grep -i "opencv\|dylib\|jnilib"

   
   ```

3. **適切なディレクトリにコピー**
   ```bash
   # .dylibファイルが見つかった場合
   cp libopencv_java.dylib "src/main/resources/native/macosx-x86-64/libopencv_java.dylib"
   
   # または .jnilibファイルが見つかった場合（macOSでは.jnilibもサポート）
   cp libopencv_java.jnilib "src/main/resources/native/macosx-x86-64/libopencv_java.jnilib"
   
   # arm64版の場合
   cp libopencv_java.dylib "src/main/resources/native/macosx-arm64/libopencv_java.dylib"
   # または
   cp libopencv_java.jnilib "src/main/resources/native/macosx-arm64/libopencv_java.jnilib"
   ```

**注意**: macOSでは `.dylib` と `.jnilib` の両方がサポートされています。どちらか一方があれば動作します。

#### Linuxの場合

1. **.xzファイルを解凍**
   ```bash
   # x86_64版
   xz -d "horos-20240407/Binaries/weasis-extracted/weasis/bundle/weasis-opencv-core-linux-x86-64-4.2.0-dcm.jar.xz"
   
   # x86版（32bit）の場合
   xz -d "horos-20240407/Binaries/weasis-extracted/weasis/bundle/weasis-opencv-core-linux-x86-4.2.0-dcm.jar.xz"
   ```

2. **JARファイルからネイティブライブラリを抽出**
   ```bash
   jar -xf "weasis-opencv-core-linux-x86-64-4.2.0-dcm.jar"
   
   # ネイティブライブラリを探す
   find . -name "libopencv_java.so"
   ```

3. **適切なディレクトリにコピー**
   ```bash
   cp libopencv_java.so "src/main/resources/native/linux-x86-64/libopencv_java.so"
   ```

### 方法2: Weasisの公式バイナリリリースから取得（推奨）

**重要**: バイナリリリース（実行可能なアプリケーション）から取得することを推奨します。ソースコードからビルドする必要はありません。

1. **WeasisのGitHubリリースページにアクセス**
   - https://github.com/nroduit/Weasis/releases
   - 最新のリリース（例: `Weasis-4.6.5`）を選択

2. **バイナリリリースをダウンロード**
   - **Windows**: `Weasis-*-windows-x86-64.exe` または `Weasis-*-windows-x86-64.zip`
   - **macOS**: 
     - **推奨**: `weasis-native.zip`（全プラットフォーム用のネイティブライブラリが含まれている）
     - **代替**: `Weasis-*-macosx-aarch64.dmg` または `Weasis-*-aarch64.pkg`（Apple Silicon用）
     - **Intel Mac**: `Weasis-*-macosx-x86-64.dmg` または `Weasis-*-x86-64.pkg`
   - **Linux**: `Weasis-*-linux-x86-64.tar.gz` または `Weasis-*-linux-x86-64.AppImage`

3. **ダウンロードしたファイルからJARファイルを抽出**
   - **ZIP/TAR.GZの場合**: 解凍して `bundle/` または `weasis/bundle/` ディレクトリ内の `weasis-opencv-core-*-*.jar` を探す
   - **DMGの場合**: DMGをマウントして、アプリケーションバンドル内の `Contents/Java/bundle/` から `weasis-opencv-core-*-*.jar` を探す
   - **EXEの場合**: インストーラーを実行せず、7-Zipなどで開いて `bundle/` ディレクトリ内の `weasis-opencv-core-*-*.jar` を探す

4. **JARファイルからネイティブライブラリを抽出**
   - 上記の「方法1」と同じ手順でJARからネイティブライブラリを抽出

**注意**: 
- バイナリリリースには既にコンパイル済みのネイティブライブラリが含まれています
- ソースコードからビルドする必要はありません（時間がかかり、複雑です）

### 方法3: パッケージマネージャーを使用（macOS/Linuxのみ）

#### macOS (Homebrew)
```bash
brew install opencv

# インストール後、ライブラリの場所を確認
find /usr/local/opt/opencv -name "libopencv_java.dylib"
# または
find /opt/homebrew/opt/opencv -name "libopencv_java.dylib"  # Apple Siliconの場合

# 見つかったライブラリをコピー
cp /usr/local/opt/opencv/lib/libopencv_java.dylib "src/main/resources/native/macosx-x86-64/libopencv_java.dylib"
```

**注意**: HomebrewでインストールしたOpenCVには `libopencv_java.dylib` が含まれていない場合があります。その場合は、ソースからビルドする必要があります。

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install libopencv-dev

# インストール後、ライブラリの場所を確認
find /usr/lib -name "libopencv_java.so" 2>/dev/null
find /usr/local/lib -name "libopencv_java.so" 2>/dev/null

# 見つかったライブラリをコピー
cp /usr/lib/x86_64-linux-gnu/libopencv_java.so "src/main/resources/native/linux-x86-64/libopencv_java.so"
```

**注意**: パッケージマネージャーでインストールしたOpenCVには `libopencv_java.so` が含まれていない場合があります。その場合は、ソースからビルドするか、WeasisのJARから抽出する方法を推奨します。

### 方法4: OpenCVソースからビルド（上級者向け）

1. https://opencv.org/releases/ からOpenCVのソースコードをダウンロード
2. CMakeを使用してJavaバインディングを含めてビルド
3. ビルド後、`opencv/build/java/` からライブラリを取得

詳細なビルド手順は、OpenCVの公式ドキュメントを参照してください。

## 注意事項

- ライブラリファイル名の `XXX` はバージョン番号（例: `opencv_java452.dll`）の場合があります
- ファイル名を上記のディレクトリ構造に合わせてリネームしてください（例: `opencv_java452.dll` → `opencv_java.dll`）
- ビルド時にJARファイルに含まれ、実行時に自動的に抽出されます
- **推奨**: 方法1（HOROS-20240407のWeasis JARから抽出）が最も確実で簡単です

