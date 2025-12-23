# HOROS-20240407 実装戦略と重要な注意事項

## 概要

このドキュメントは、HOROS-20240407準拠の実装を行う際の戦略と重要な注意事項をまとめたものです。

最終更新: 2024年

---

## 重要な認識

### ユーザーからの重要な指摘

1. **dcm4cheに依存するとバグの原因が分からなくなる**
   - dcm4che-imageioが内部的にWindow Level/WidthやRescale Slope/Interceptを適用している可能性
   - 位置ズレや縮尺異常の原因が特定できない

2. **位置ズレや縮尺異常が発生**
   - HOROS-20240407の処理に合わせても問題が発生
   - dcm4che経由で読み込んだ画像に対して処理を適用しているため

3. **成熟度の高いHOROS-20240407に慎重に合わせる必要がある**
   - 独自ソースだとバグ取りできなくなる
   - HOROS-20240407の処理を確認しながら実装する

---

## 実装方針

### 1. dcm4che-imageioへの依存を排除

**方針**:
- **dcm4che-imageioは使用しない**
- dcm4che-coreのみを使用してDICOMファイルを読み込む
- PixelDataを直接処理する

**理由**:
- dcm4che-imageioが内部的に処理を適用している可能性がある
- バグの原因を特定しにくくなる
- HOROSの実装と一致しない

### 2. HOROS-20240407の実装を忠実にJavaで再現

**データフロー**:
```
DICOMファイル
    ↓
loadDICOMDCMFramework (decodingPixelData:NO)
    ↓
PixelData属性を取得
    ↓
decodeFrameAtIndex:imageNb でフレームをデコード
    ↓
oImage (short配列) = ピクセルデータ
    ↓
fImage (32-bit float配列) = Rescale Slope/Intercept適用
    ↓
compute8bitRepresentation
    ↓
Window Level/Width適用
    ↓
baseAddr (8-bit画像) = 表示用画像
```

**重要な処理**:

1. **PixelDataの読み込み**
   ```objective-c
   DCMPixelDataAttribute *pixelAttr = (DCMPixelDataAttribute *)[dcmObject attributeWithName:@"PixelData"];
   NSData *pixData = [pixelAttr decodeFrameAtIndex:imageNb];
   short *oImage = malloc([pixData length]);
   [pixData getBytes:oImage];
   ```

2. **32-bit float画像（fImage）の生成**
   - oImage（short配列）からfImage（float配列）に変換
   - Rescale Slope/Interceptを適用
   - vImageConvert_16SToF() または vImageConvert_16UToF() を使用

3. **Window Level/Widthの適用**
   - compute8bitRepresentation()
   - min = wl - ww / 2, max = wl + ww / 2
   - vImageConvert_PlanarFtoPlanar8(&srcf, &dst8, max, min, 0)
   - baseAddr（8-bit画像）を生成

### 3. データ構造

**HOROSのデータ構造**:
```objective-c
float *fImage;  // 32-bit float配列（元のピクセルデータ）
char *baseAddr; // 8-bit画像（Window Level/Width適用後）

// 位置・縮尺情報
double pixelSpacingX, pixelSpacingY;  // ピクセル間隔（mm）
double originX, originY, originZ;     // 画像の位置（mm）
double orientation[9];                // 方向ベクトル
double sliceLocation;                 // スライス位置

// 変換パラメータ
float slope, offset;  // Rescale Slope/Intercept
float ww, wl;         // Window Width/Level
```

---

## 段階的な実装

### フェーズ1: HOROSの実装分析
1. HOROSの`loadDICOMDCMFramework`の詳細分析
2. PixelData読み込み処理の分析
3. 32-bit float画像生成の分析
4. Window Level/Width適用処理の分析

### フェーズ2: Java実装
1. PixelData読み込み処理の実装
2. 32-bit float画像生成の実装
3. Window Level/Width適用処理の実装
4. 位置・縮尺情報の正確な処理

### フェーズ3: テストと調整
1. 様々なモダリティでのテスト
2. 位置ズレの確認と修正
3. 縮尺異常の確認と修正

---

## 現在の実装状況

### 採用したアプローチ

実際の実装では、以下のアプローチを採用しました：

1. **dcm4che-imageioを使用**
   - DICOM画像の読み込みに`DicomImageReader`を使用
   - 実用性を優先

2. **Weasisのアプローチを参考**
   - Java Swingの座標系に合わせた実装
   - `AffineTransform`を使用した座標変換

3. **HOROSの仕様を可能な限り維持**
   - `scaleToFitForDCMPix`のロジックを正確に再現
   - ピクセル比（`pixelRatio`）の適用

詳細は以下のドキュメントを参照：
- `HOROS-IMAGE-RENDERING-ANALYSIS.md` - 画像描画・座標系の分析
- `VIEWER-IMPLEMENTATION-STATUS.md` - 実装状況と計画

---

## 参考実装

### HOROS-20240407
- `horos-20240407/Horos/Sources/DCMPix.m`
  - PixelDataの読み込み処理
  - fImage（32-bit float）の生成
  - Window Level/Widthの適用

---

## 変更履歴

- 2024年: HOROS実装戦略関連のドキュメント（18-22）をこのドキュメントに統合

