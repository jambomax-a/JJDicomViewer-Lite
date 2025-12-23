# 黒画面問題のデバッグ手順

## 実装した改善

1. **ログバック設定の追加**
   - `src/main/resources/logback.xml` を作成
   - コンソールとファイル（`logs/jjdicomviewer.log`）にログを出力
   - デバッグレベルで詳細なログを出力

2. **画像読み込み処理の改善**
   - 複数の方法でDICOM画像を読み込みを試行
   - ImageIOを使用した読み込み
   - dcm4che-imageioプラグインを直接使用した読み込み
   - 各ステップで詳細なログを出力

3. **Window Level/Width適用処理の改善**
   - より効率的なピクセルデータ処理
   - エラーハンドリングの改善
   - フォールバック処理（元の画像をそのまま使用）

4. **デバッグ情報の追加**
   - 画像読み込みの各ステップでログを出力
   - 画像サイズ、Window Level/Widthなどの情報を出力
   - エラー時の詳細なスタックトレース

## デバッグ方法

### 1. アプリケーションを実行してログを確認

```powershell
.\gradlew.bat run
```

または、実行可能JARを作成して実行：

```powershell
.\gradlew.bat build
java -jar build\libs\JJDicomViewer-1.0.0.jar
```

### 2. ログファイルを確認

ログファイルは `logs/jjdicomviewer.log` に出力されます。

### 3. コンソール出力を確認

アプリケーション起動時とDICOMファイルを開いた時に、以下のようなログが出力されます：

```
HH:mm:ss.SSS [main] DEBUG com.jj.dicomviewer.dicom.DicomImageReader - DICOM画像の読み込みを開始: ...
HH:mm:ss.SSS [main] INFO  com.jj.dicomviewer.dicom.DicomImageReader - ImageIOでDICOM画像を読み込みました: ... (512x512)
HH:mm:ss.SSS [AWT-EventQueue-0] INFO  com.jj.dicomviewer.ui.ImageViewerPanel - DICOMファイルを読み込みました: ...
```

### 4. 問題の特定

黒画面が表示される場合、以下のログを確認してください：

- **画像が読み込まれていない場合**:
  ```
  ERROR com.jj.dicomviewer.dicom.DicomImageReader - すべての方法でDICOM画像の読み込みに失敗しました
  ERROR com.jj.dicomviewer.ui.ImageViewerPanel - 画像の読み込みに失敗しました
  ```

- **画像は読み込まれているが表示されていない場合**:
  ```
  INFO  com.jj.dicomviewer.dicom.DicomImageReader - ImageIOでDICOM画像を読み込みました: ... (512x512)
  INFO  com.jj.dicomviewer.ui.ImageViewerPanel - 表示画像を更新しました: 512x512
  DEBUG com.jj.dicomviewer.ui.ImageViewerPanel - paintComponent: 画像を描画します (512x512)
  ```

## よくある問題と対処法

### 1. DICOM画像リーダーが見つからない

**症状**: ログに "ImageIOでDICOM画像リーダーが見つかりません" と表示される

**原因**: dcm4che-imageioのサービスプロバイダーが正しく登録されていない

**対処法**: 
- `build.gradle.kts`でdcm4che-imageioの依存関係を確認
- `META-INF/services/javax.imageio.spi.ImageReaderSpi`ファイルがクラスパスに含まれているか確認

### 2. ピクセルデータが見つからない

**症状**: ログに "ピクセルデータが見つかりません" と表示される

**原因**: DICOMファイルにピクセルデータが含まれていない、または読み込み方法が間違っている

**対処法**: 
- DICOMファイルが正しい形式であることを確認
- 他のDICOMビューワーで同じファイルを開いて確認

### 3. Window Level/Widthの適用で画像が黒くなる

**症状**: 画像は読み込まれているが、Window Level/Widthを適用すると黒くなる

**原因**: Window Level/Widthの値が不適切

**対処法**: 
- ログでWindow Level/Widthの値を確認
- デフォルト値を調整するか、画像の輝度範囲から自動計算する処理を改善

## 次のステップ

問題が解決しない場合、以下の情報を確認してください：

1. DICOMファイルの形式（モダリティ、ビット深度、圧縮形式など）
2. ログファイルの完全な内容
3. エラーメッセージの詳細

