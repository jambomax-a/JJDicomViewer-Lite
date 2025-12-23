# Weasis-4.6.5 実装分析とアプローチ

## 概要

Weasis-4.6.5の画像読み込み処理とWindow Level/Width適用方法を分析し、Java実装への適用方法をまとめます。

最終更新: 2024年12月

---

## Weasisの実装詳細

### 1. dcm4che3の使用

Weasisは**dcm4che3**を使用してDICOM画像を読み込んでいます：

```java
// DicomMediaIO.java:664-670
DicomImageReader reader = new DicomImageReader(Transcoder.dicomImageReaderSpi);
reader.setInput(inputStream);
ImageDescriptor desc = reader.getImageDescriptor();
DicomImageReadParam param = new DicomImageReadParam();
param.setAllowFloatImageConversion(true);
PlanarImage img = reader.getPlanarImage(frame, param);
```

**重要なポイント**:
- `org.dcm4che3.img.DicomImageReader`を使用
- `param.setAllowFloatImageConversion(true)`により、元のピクセル値範囲が保持される
- `PlanarImage`（OpenCV形式）で画像を保持

### 2. 画像データの形式

Weasisでは、画像データは`PlanarImage`（OpenCVの形式）として保持されています：

```java
// DicomImageElement.java:57
import org.weasis.opencv.data.PlanarImage;

// DicomMediaIO.java:670
PlanarImage img = reader.getPlanarImage(frame, param);
```

**重要なポイント**:
- `PlanarImage`はOpenCVの形式で、16-bitや32-bit floatの画像データを保持できる
- `setAllowFloatImageConversion(true)`により、元のピクセル値範囲が保持される可能性がある

### 3. Window Level/Widthの適用

Weasisでは、`ImageRendering`クラスを使用してWindow Level/Widthを適用します：

```java
// DicomImageElement.java:433-456
public PlanarImage getRenderedImage(PlanarImage imageSource, Map<String, Object> params) {
    DicomImageReadParam readParams = new DicomImageReadParam();
    readParams.setWindowCenter((Double) params.get(ActionW.LEVEL.cmd()));
    readParams.setWindowWidth((Double) params.get(ActionW.WINDOW.cmd()));
    // ...
    return ImageRendering.getVoiLutImage(imageSource, adapter, readParams);
}
```

**重要なポイント**:
- `org.dcm4che3.img.ImageRendering.getVoiLutImage`を使用
- `DicomImageReadParam`にWindow Center/Widthを設定
- `PlanarImage`に対してWindow Level/Widthが適用される

### 4. 8-bit変換処理

Weasisでは、`ImageRendering`クラスがWindow Level/Widthを適用した後、表示用に8-bit画像に変換する処理を行っている可能性があります。

**重要なポイント**:
- `PlanarImage`は16-bitや32-bit floatの画像データを保持できる
- Window Level/Widthは`PlanarImage`に対して適用される
- 表示時に8-bit画像に変換される

---

## 現在のJava実装との違い

### Weasisの実装
1. `org.dcm4che3.img.DicomImageReader`を使用
2. `PlanarImage`（OpenCV形式）で画像を保持
3. `ImageRendering.getVoiLutImage`でWindow Level/Widthを適用
4. `setAllowFloatImageConversion(true)`により、元のピクセル値範囲が保持される可能性がある

### 現在のJava実装
1. `org.dcm4che3.imageio.plugins.dcm.DicomImageReader`を使用（Weasis方式）
2. `BufferedImage`（8-bit）で画像を保持
3. `DicomImageReadParam`でWindow Level/Widthを設定して読み込み時に適用（Weasis方式）
4. 12-bit/16-bit範囲のWindow Level/Width値もdcm4che3が適切に処理

---

## 実装アプローチ

### 採用したアプローチ: ハイブリッド方式

**理由**:
1. **既存の依存関係を活用**
   - `dcm4che-image:5.34.1`が既に含まれている
   - 追加の依存関係が不要

2. **Weasisのアプローチに準拠**
   - `org.dcm4che3.imageio.plugins.dcm.DicomImageReader`を使用
   - `DicomImageReadParam`でWindow Level/Widthを設定

3. **既存のコードとの互換性**
   - `ImageViewerPanel`は`BufferedImage`を使用している
   - `DicomImageReadParam`でWindow Level/Widthを設定して読み込み時に適用

### 実装内容

1. **`DicomImageReader`の実装**
   - `org.dcm4che3.imageio.plugins.dcm.DicomImageReader`を使用
   - `DicomImageReadParam`でWindow Level/Widthを設定
   - 12-bit/16-bit範囲の値もそのまま渡す（dcm4che3が処理）

2. **`ImageViewerPanel`の実装**
   - `DicomImageReader.readImage(dicomFile, windowCenter, windowWidth)`で読み込み
   - Window Level/Widthが既に適用された`BufferedImage`を受け取る
   - 二重適用を防止（Weasis方式で読み込んだ場合は`updateDisplayImage`で適用しない）

---

## 実装の選択肢（検討済み）

### オプションA: Weasisの`PlanarImage`を完全に移植

**メリット**:
- Weasisの実装と完全に一致
- 元のピクセル値範囲が保持される

**デメリット**:
- Weasisのソースコードから大量のクラスを移植する必要がある
- OpenCVへの依存が必要

**結論**: 採用せず

### オプションB: dcm4che3の標準的な方法を使用（採用）

**メリット**:
- dcm4che3の標準的なAPIを使用
- 追加の依存関係が不要（既に`dcm4che-image`が含まれている）
- `DicomImageReadParam`でWindow Level/Widthを適用できる

**デメリット**:
- `PlanarImage`の代わりに`BufferedImage`を使用する必要がある

**結論**: 採用（ハイブリッド方式）

### オプションC: ハイブリッドアプローチ

**メリット**:
- `org.dcm4che3.imageio.plugins.dcm.DicomImageReader`を使用して画像を読み込む
- `DicomImageReadParam`でWindow Level/Widthを設定
- 最終的に`BufferedImage`に変換して表示

**デメリット**:
- `PlanarImage`から`BufferedImage`への変換が必要（ただし、`read`メソッドが直接`BufferedImage`を返す）

**結論**: 採用（実際の実装はオプションBに近い）

---

## 実装状況

### 完了した実装

1. **Weasis方式の画像読み込み**
   - `readImageWithWindowLevel`メソッドを追加
   - `DicomImageReadParam`でWindow Level/Widthを設定

2. **Window Level/Widthの適用**
   - 読み込み時に`DicomImageReadParam`で設定
   - 12-bit/16-bit範囲の値もそのまま渡す

3. **二重適用の防止**
   - Weasis方式で読み込んだ場合は`updateDisplayImage`で適用しない

### 今後の改善点

1. **CT/MRの初期表示**
   - Weasis方式で読み込む際のWindow Level/Width設定の確認

2. **USのWW/WL安定性**
   - RGB画像に対するWindow Level/Widthの適用方法の確認

---

## 参考実装

### Weasis-4.6.5

- `Weasis-4.6.5/weasis-dicom/weasis-dicom-codec/src/main/java/org/weasis/dicom/codec/DicomMediaIO.java`
  - `getImageFragment` (line 647-688)
  - `DicomImageReader.getPlanarImage` (line 670)
- `Weasis-4.6.5/weasis-dicom/weasis-dicom-codec/src/main/java/org/weasis/dicom/codec/DicomImageElement.java`
  - `getRenderedImage` (line 433-460)
  - `ImageRendering.getVoiLutImage` (line 456)

---

## 結論

Weasis-4.6.5では、**dcm4che3の`DicomImageReader`を使用して画像を読み込んでいます**。`PlanarImage`（OpenCV形式）で画像を保持し、`ImageRendering`クラスでWindow Level/Widthを適用する方式です。

現在のJava実装では、`org.dcm4che3.imageio.plugins.dcm.DicomImageReader`を使用し、`DicomImageReadParam`でWindow Level/Widthを設定して読み込み時に適用する方式を採用しています。これにより、12-bit/16-bit範囲のWindow Level/Width値もdcm4che3が適切に処理します。

---

## 変更履歴

- 2024年12月: Weasis実装分析とアプローチを統合
- 2024年: Weasis-4.6.5の画像読み込み処理を詳細分析
- 2024年: Weasisアプローチ実装計画を作成

