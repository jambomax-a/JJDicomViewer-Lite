# Pixel Spacing表示修正

## 問題

1. **CT画像**: きれいに表示されている
2. **US画像**: 左右が少しはみ出しているが、まあまあ見れている
3. **CR画像**: 4倍ぐらい拡大されている

## 原因

CR画像が4倍拡大されている原因は、**pixel spacing（ピクセル間隔）を考慮していない**ためです。

- CR画像のpixel spacingは通常、0.143mm/pixel程度（例：143μm/pixel）
- 現在の実装では、ピクセル数そのままで表示しているため、pixel spacingが小さい場合、画像が大きく表示される

## HOROSの実装

HOROS-20240407では、`rectCoordinates`メソッドで以下のようにpixel spacingを考慮しています：

```objective-c
- (NSRect) rectCoordinates
{
    if( self.pixelSpacingX && self.pixelSpacingY)
        return NSMakeRect( self.originX, self.originY, 
                          self.pixelSpacingX*self.pwidth, 
                          self.pixelSpacingY*self.pheight);
    else
        return NSMakeRect( self.originX, self.originY, 
                          self.pwidth, self.pheight);
}
```

## 解決策

1. Pixel Spacingの値を取得
2. Pixel Spacingを考慮した表示スケールを計算
3. モダリティごとの適切な表示方法を実装

## 実装方針

1. `DicomFile`から`PixelSpacing`を取得（既に取得している）
2. `ImageViewerPanel`でpixel spacingを考慮した表示スケールを計算
3. デフォルト表示時に適切なスケールを適用

