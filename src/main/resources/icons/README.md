# アイコンファイル

このディレクトリに、以下の形式のアイコンファイルを配置してください：

- **Windows**: `app-icon.ico` (256x256 推奨、複数サイズを含む)
- **macOS**: `app-icon.icns` (512x512 推奨、複数サイズを含む)
- **Linux**: `app-icon.png` (512x512 推奨)

## アイコンファイルの入手方法

### 無料アイコンリソース
- [Flaticon](https://www.flaticon.com/) - 医療・DICOM関連のアイコン多数
- [IconFinder](https://www.iconfinder.com/) - 高品質なアイコン
- [Icons8](https://icons8.com/) - 多様なスタイルのアイコン
- [Material Icons](https://fonts.google.com/icons) - シンプルなアイコン

### アイコン変換ツール

#### macOS用 (.icns) の変換方法

**方法1: macOSの`iconutil`コマンド（推奨・無料・制限なし）**

1. 各サイズのPNGファイルを用意（16, 32, 64, 128, 256, 512, 1024）
2. 以下のような構造のフォルダを作成:
   ```
   icon.iconset/
   ├── icon_16x16.png
   ├── icon_16x16@2x.png (32x32)
   ├── icon_32x32.png
   ├── icon_32x32@2x.png (64x64)
   ├── icon_128x128.png
   ├── icon_128x128@2x.png (256x256)
   ├── icon_256x256.png
   ├── icon_256x256@2x.png (512x512)
   └── icon_512x512@2x.png (1024x1024)
   ```
3. ターミナルで以下のコマンドを実行:
   ```bash
   iconutil -c icns icon.iconset -o app-icon.icns
   ```

**方法2: オンライン変換ツール**
- [CloudConvert](https://cloudconvert.com/png-to-icns) - 1日10個まで無料

#### Windows用 (.ico) の変換方法
- [CloudConvert](https://cloudconvert.com/png-to-ico) - 複数サイズを含むマルチICOファイルを作成
- [ICO Convert](https://icoconvert.com/) - 無料で複数サイズ対応

#### Linux用 (.png) の変換方法
- 512x512または256x256のPNGファイルをそのまま使用
- 画像編集ソフト（GIMP、Photoshop、Inkscape）でリサイズ

### 推奨テーマ
- 医療・X線イメージ
- 画像ビューア
- DICOM関連

アイコンファイルを配置後、`build.gradle` の `--icon` オプションが自動的に使用されます。

