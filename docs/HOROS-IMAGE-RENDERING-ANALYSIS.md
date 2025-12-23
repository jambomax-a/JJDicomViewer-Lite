# HOROS-20240407 画像描画・座標系の完全分析

## 概要

このドキュメントは、HOROS-20240407の画像描画処理と座標変換を徹底的に解析したものです。Java Swingへの移植のための詳細な実装情報を提供します。

---

## HOROSの座標系と描画処理

### OpenGL座標系の特徴

HOROSでは、OpenGLを使用して画像を描画しています：

- **原点**: 画面中央
- **X軸**: 右方向が正
- **Y軸**: 上方向が正（Java Swingとは逆）
- **Z軸**: 手前方向が正

### DCMViewの座標変換順序

**DCMView.m の描画処理** (`drawRectIn:`):

```objective-c
// 1. ビューポートへのスケーリング（正規化）
glScalef (2.0f /(xFlipped ? -(size.size.width) : size.size.width), 
          -2.0f / (yFlipped ? -(size.size.height) : size.size.height), 
          1.0f);

// 2. 回転
glRotatef (rotation, 0.0f, 0.0f, 1.0f);

// 3. パン（平行移動）
glTranslatef( origin.x - offset.x , -origin.y - offset.y, 0.0f);

// 4. ピクセル比の適用
if( self.curDCM.pixelRatio != 1.0) 
    glScalef( 1.f, self.curDCM.pixelRatio, 1.f);
```

**重要なポイント**:
- Y軸は反転（`-2.0f / size.size.height`）
- パン時もY軸は反転（`-origin.y`）
- ピクセル比（`pixelRatio = pixelSpacingY / pixelSpacingX`）はY軸のみに適用
- 画像の中心を基準に描画

### scaleToFitForDCMPix の実装

```objective-c
- (float) scaleToFitForDCMPix: (DCMPix*) d
{
    NSRect sizeView = [self convertRectToBacking: [self bounds]]; // Retina
    int w = d.pwidth;
    int h = d.pheight;
    if( d.shutterEnabled) { w = d.shutterRect.size.width; h = d.shutterRect.size.height; }
    if( sizeView.size.width / w < sizeView.size.height / h / d.pixelRatio )
        return sizeView.size.width / w;
    else
        return sizeView.size.height / h / d.pixelRatio;
}
```

**ポイント**:
- ピクセル比（`pixelRatio = pixelSpacingY / pixelSpacingX`）を考慮
- Y軸方向のスケールを`pixelRatio`で調整
- 画像全体が表示されるように小さい方を選択

---

## Java Swing座標系との違い

### Java Swingの座標系

- **原点**: 左上（0, 0）
- **X軸**: 右方向が正
- **Y軸**: 下方向が正（HOROS/OpenGLとは逆）

### 座標変換の困難さ

1. **Y軸の方向が逆**: HOROSはY軸上向き、Java SwingはY軸下向き
2. **原点の位置が異なる**: HOROSは中心、Java Swingは左上
3. **変換順序の複雑さ**: OpenGLの変換順序をJava Swingで再現するには複数の変換が必要

---

## Weasisの実装アプローチ

### Weasisの描画構造

1. **View2d** - DICOM画像表示専用
2. **DefaultView2d** - 画像表示の基本実装
3. **GraphicsPane** - `AffineTransform`の計算と管理
4. **RenderedImageLayer** - 実際の画像描画

### WeasisのAffineTransform構築順序

```java
// GraphicsPane.updateAffineTransform

// 1. スケーリング（フリップ時はX軸を反転）
affineTransform.setToScale(flip ? -viewScale : viewScale, viewScale);

// 2. 回転（画像の中心を基準）
if (rotationAngle != null && rotationAngle > 0) {
    affineTransform.rotate(Math.toRadians(rotationAngle), rWidth / 2.0, rHeight / 2.0);
}

// 3. フリップ時の平行移動
if (flip) {
    affineTransform.translate(-rWidth, 0.0);
}

// 4. 画像の中心調整（ビューポート内に収めるため）
// 変換行列の[4]と[5]（平行移動成分）を調整
```

**重要なポイント**:
- Weasisは`imageLayer.drawImage()`で画像を`(0, 0)`から描画
- 座標変換は`Graphics2D`の変換行列（`AffineTransform`）で行われる
- 画像の中心を基準に回転・変換を実行

---

## 現在のプロジェクトでの実装

### 採用したアプローチ

**Weasisのアプローチを参考に、HOROSの仕様を維持しながらJava Swingで実装**

1. **AffineTransformを事前に構築**
   - スケーリング（フリップ時はX軸を反転）
   - 回転（画像の中心を基準）
   - フリップ時の平行移動
   - ピクセル比の適用（Y軸のみ、HOROSの仕様を維持）
   - Y軸フリップ

2. **画像を(0, 0)から描画**
   - 座標変換は`AffineTransform`で実行
   - 変換後の画像サイズを計算
   - 画像を中央に配置するためのオフセットを計算

3. **fitToWindowメソッド**
   - HOROSの`scaleToFitForDCMPix`に正確に合わせた実装
   - ピクセル比を考慮

---

## 過去の問題と解決策

### 問題: 画像位置ずれ

**症状**:
- CT、MR、CRが微妙に上にズレている
- 画像をシリーズで進めると戻る
- CRは拡大・縮小するとフィットする

**原因**:
- HOROSのOpenGL座標系とJava Swingの座標系の根本的な違い
- 座標変換の順序がHOROSと異なるため、微妙なズレが発生

**解決策**:
- Weasisのアプローチを採用
- Java Swingの座標系に合わせて実装
- HOROSの座標変換を完全再現するのではなく、実用的な解決策を採用

---

## 参考実装

### HOROS-20240407
- `horos-20240407/Horos/Sources/DCMView.m`
  - `scaleToFitForDCMPix` (line 2056)
  - `drawRectIn:` (line 7639)
  - `drawRect:withContext:` (line 8929)
  - 座標変換の順序

### Weasis
- `Weasis-4.6.5/weasis-core/src/main/java/org/weasis/core/ui/editor/image/DefaultView2d.java`
  - `paintComponent`メソッド
- `Weasis-4.6.5/weasis-core/src/main/java/org/weasis/core/ui/editor/image/GraphicsPane.java`
  - `updateAffineTransform`メソッド
- `Weasis-4.6.5/weasis-core/src/main/java/org/weasis/core/ui/model/layer/imp/RenderedImageLayer.java`
  - `drawImage`メソッド

### 現在の実装
- `src/main/java/com/jj/dicomviewer/ui/ImageViewerPanel.java`
  - `paintComponent`メソッド
  - `fitToWindow`メソッド
  - 座標変換の実装

---

## 変更履歴

- 2024年: 座標系・描画関連の分析ドキュメント（41, 42, 43, 44, 47）をこのドキュメントに統合

