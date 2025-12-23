# HOROS-20240407準拠実装計画

## 問題認識

1. **dcm4cheに依存するとバグの原因が分からなくなる**
   - dcm4che-imageioが内部的に処理を適用している可能性
   - 位置ズレや縮尺異常の原因が特定できない

2. **位置ズレや縮尺異常が発生**
   - HOROS-20240407の処理に合わせても問題が発生

3. **成熟度の高いHOROS-20240407に慎重に合わせる必要がある**

## 実装方針

### 1. HOROSの実装を詳細に分析

- PixelDataの読み込み方法
- 32-bit float画像（fImage）の生成方法
- Rescale Slope/Interceptの適用方法
- Window Level/Widthの適用方法
- 位置・縮尺情報の取得方法

### 2. dcm4cheへの依存を最小化

- dcm4che-imageioを使用しない
- dcm4che-coreのみを使用
- ピクセルデータを直接処理

### 3. HOROSの実装を忠実にJavaで再現

- 各処理ステップをHOROSに合わせる
- 位置ズレや縮尺異常を防ぐ

## 次のステップ

1. HOROSの`loadDICOMDCMFramework`の詳細分析
2. PixelData読み込み処理の実装
3. 32-bit float画像生成の実装
4. Window Level/Width適用処理の実装
5. 位置・縮尺情報の正確な処理

