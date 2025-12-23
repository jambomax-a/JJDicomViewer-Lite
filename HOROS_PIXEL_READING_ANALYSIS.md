# HOROS-20240407 ピクセルデータ読み込み処理の詳細分析

## 重要な認識

1. **dcm4cheに依存するとバグの原因が分からなくなる**
2. **位置ズレや縮尺異常が発生**
3. **成熟度の高いHOROS-20240407に慎重に合わせる必要がある**

## HOROSの実装フロー（loadDICOMDCMFramework）

### 1. PixelDataの読み込み

```objective-c
DCMPixelDataAttribute *pixelAttr = (DCMPixelDataAttribute *)[dcmObject attributeWithName:@"PixelData"];
NSData *pixData = [pixelAttr decodeFrameAtIndex:imageNb];
short *oImage = malloc([pixData length]);
[pixData getBytes:oImage];
```

### 2. fImage（32-bit float配列）の生成

- oImage（short配列）からfImage（float配列）に変換
- Rescale Slope/Interceptを適用

### 3. Window Level/Widthの適用

- compute8bitRepresentation()
- vImageConvert_PlanarFtoPlanar8(&srcf, &dst8, max, min, 0)
- baseAddr（8-bit画像）を生成

## 実装方針

### 1. dcm4cheへの依存を最小化
- dcm4che-imageioを使用しない
- dcm4che-coreのみを使用
- PixelDataを直接処理

### 2. HOROSの実装を忠実にJavaで再現
- 各ステップをHOROSに合わせる
- 位置ズレや縮尺異常を防ぐ

