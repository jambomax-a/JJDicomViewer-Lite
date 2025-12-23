# HOROS-20240407のピクセルデータ読み込み処理の分析

## 問題点の認識

1. **dcm4cheに依存するとバグの原因が分からなくなる**
   - dcm4che-imageioが内部的にWindow Level/Widthを適用している可能性
   - 位置ズレや縮尺異常の原因が特定できない

2. **HOROS-20240407の処理に合わせても位置ズレや縮尺異常が発生**
   - dcm4che経由で読み込んだ画像に対して処理を適用しているため
   - HOROSの実装を忠実に再現する必要がある

## HOROSの実装アーキテクチャ

### 1. データフロー

```
DICOMファイル
    ↓
loadDICOMDCMFramework / loadDICOMPapyrus
    ↓
PixelData読み込み（生データ）
    ↓
fImage（32-bit float配列）生成
    ↓
compute8bitRepresentation
    ↓
Window Level/Width適用
    ↓
baseAddr（8-bit画像）生成
```

### 2. 重要なポイント

#### fImage（32-bit float画像）
- 元のDICOMピクセルデータを32-bit floatとして保持
- Rescale Slope/Interceptを適用
- Window Level/Widthはまだ適用されていない

#### baseAddr（8-bit画像）
- Window Level/Widthを適用した8-bit表現
- 表示用に最適化

### 3. 位置・縮尺の処理

HOROSは以下の情報を保持：
- `pixelSpacingX`, `pixelSpacingY`: ピクセル間隔（mm）
- `originX`, `originY`, `originZ`: 画像の位置（mm）
- `orientation[9]`: 方向ベクトル
- `sliceLocation`: スライス位置

## 解決策

### 1. dcm4cheへの依存を最小化

- dcm4che-imageioではなく、dcm4che-coreのみを使用
- ピクセルデータを直接読み込む
- HOROSの実装をJavaで忠実に再現

### 2. ピクセルデータの読み込み処理をHOROSに合わせる

- PixelDataを生データとして読み込む
- 32-bit float配列（fImage相当）を生成
- Rescale Slope/Interceptを適用
- Window Level/Widthを適用して8-bit表現を生成

### 3. 位置・縮尺の処理をHOROSに合わせる

- pixelSpacingX, pixelSpacingYの正確な取得
- originX, originY, originZの正確な取得
- orientationベクトルの正確な取得

## 次のステップ

1. HOROSの`loadDICOMDCMFramework`の詳細な分析
2. PixelData読み込み処理のJava実装
3. 32-bit float画像生成の実装
4. Window Level/Width適用処理の実装（HOROSに忠実に）
5. 位置・縮尺情報の正確な取得と適用

