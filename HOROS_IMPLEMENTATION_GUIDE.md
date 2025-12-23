# HOROS-20240407準拠実装ガイド

## 重要な認識

### 問題点

1. **dcm4cheに依存するとバグの原因が分からなくなる**
   - dcm4che-imageioが内部的にWindow Level/WidthやRescale Slope/Interceptを適用している可能性
   - 位置ズレや縮尺異常の原因が特定できない

2. **位置ズレや縮尺異常が発生**
   - HOROS-20240407の処理に合わせても問題が発生
   - dcm4che経由で読み込んだ画像に対して処理を適用しているため

3. **成熟度の高いHOROS-20240407に慎重に合わせる必要がある**

## HOROSの実装アーキテクチャ

### データフロー

```
DICOMファイル
    ↓
loadDICOMDCMFramework (decodingPixelData:NO)
    ↓
DCMPixelDataAttribute *pixelAttr = [dcmObject attributeWithName:@"PixelData"]
    ↓
NSData *pixData = [pixelAttr decodeFrameAtIndex:imageNb]
    ↓
short *oImage = malloc([pixData length])
[pixData getBytes:oImage]
    ↓
float *fImage = malloc(width*height*sizeof(float))
    ↓
Rescale Slope/Interceptを適用してoImageからfImageに変換
    ↓
compute8bitRepresentation()
    ↓
Window Level/Widthを適用
    ↓
vImageConvert_PlanarFtoPlanar8(&srcf, &dst8, max, min, 0)
    ↓
baseAddr (8-bit画像) = 表示用画像
```

### 重要なポイント

1. **PixelDataの読み込み**
   - `decodingPixelData:NO`でDICOMファイルを読み込む（ピクセルデータはまだデコードしない）
   - `decodeFrameAtIndex:`でフレームごとにピクセルデータをデコード
   - `short *oImage`として生データを取得

2. **32-bit float画像（fImage）の生成**
   - `oImage`（short配列）から`fImage`（float配列）に変換
   - Rescale Slope/Interceptを適用: `pixelValue = oImage[i] * slope + offset`

3. **8-bit画像（baseAddr）の生成**
   - `fImage`にWindow Level/Widthを適用
   - `vImageConvert_PlanarFtoPlanar8(&srcf, &dst8, max, min, 0)`
   - `min = wl - ww / 2`, `max = wl + ww / 2`

## 実装方針

### 1. dcm4cheへの依存を最小化

- **dcm4che-imageioを使用しない**
- dcm4che-coreのみを使用してDICOMファイルを読み込む
- PixelDataを直接処理する

### 2. HOROSの実装を忠実にJavaで再現

- PixelDataを生データとして読み込む
- 32-bit float配列（fImage相当）を生成
- Rescale Slope/Interceptを適用
- Window Level/Widthを適用して8-bit表現を生成

### 3. 位置・縮尺情報の正確な処理

- pixelSpacingX, pixelSpacingYの取得
- originX, originY, originZの取得
- orientationベクトルの取得と適用

## 実装の優先順位

1. **最優先**: PixelDataの直接読み込みと32-bit float画像の生成
2. **高優先**: Rescale Slope/Interceptの適用
3. **高優先**: Window Level/Widthの適用（HOROS準拠）
4. **中優先**: 位置・縮尺情報の正確な処理

