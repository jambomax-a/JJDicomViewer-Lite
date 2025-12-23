# 実装優先順位 - HOROS-20240407準拠

## 重要な指摘事項

1. **dcm4cheに依存するとバグの原因が分からなくなる**
2. **位置ズレや縮尺異常が発生**
3. **成熟度の高いHOROS-20240407に慎重に合わせる必要がある**

## 実装方針

### 1. dcm4che-imageioへの依存を排除

- dcm4che-imageioは使用しない
- dcm4che-coreのみを使用
- PixelDataを直接処理する

### 2. HOROS-20240407の実装を忠実にJavaで再現

- 各ステップをHOROSに合わせる
- 位置ズレや縮尺異常を防ぐ

## 実装の優先順位

1. **最優先**: HOROSの`loadDICOMDCMFramework`の詳細分析
2. **最優先**: PixelDataの直接読み込み処理の実装
3. **高優先**: 32-bit float画像（fImage相当）生成の実装
4. **高優先**: Rescale Slope/Interceptの適用
5. **高優先**: Window Level/Widthの適用（HOROS準拠）
6. **中優先**: 位置・縮尺情報の正確な処理

