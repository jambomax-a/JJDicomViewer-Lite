# HOROS-20240407準拠実装戦略

## 重要な認識

### 問題点

1. **dcm4cheに依存するとバグの原因が分からなくなる**
   - dcm4che-imageioが内部的にWindow Level/WidthやRescale Slope/Interceptを適用している可能性
   - 位置ズレや縮尺異常の原因が特定できない

2. **位置ズレや縮尺異常が発生**
   - HOROS-20240407の処理に合わせても問題が発生
   - dcm4che経由で読み込んだ画像に対して処理を適用しているため

3. **成熟度の高いHOROS-20240407に慎重に合わせる必要がある**

## 実装方針

### 1. dcm4cheへの依存を最小化

- **dcm4che-imageioを使用しない**
- dcm4che-coreのみを使用
- PixelDataを直接処理する

### 2. HOROS-20240407の実装を忠実にJavaで再現

- PixelDataの直接読み込み
- 32-bit float配列（fImage相当）の生成
- Rescale Slope/Interceptの適用
- Window Level/Widthの適用

### 3. 段階的な実装

1. HOROSの`loadDICOMDCMFramework`の詳細分析
2. PixelData読み込み処理の実装
3. 32-bit float画像生成の実装
4. Window Level/Width適用処理の実装（HOROS準拠）
5. 位置・縮尺情報の正確な処理

## 次のステップ

HOROSの実装を詳細に分析し、各ステップを忠実にJavaで再現する。

