# HOROSのWindow Level/Width処理の分析

## HOROSの実装（DCMPix.m）

### 1. `compute8bitRepresentation`メソッド（行9932-10138）

```objective-c
- (void) compute8bitRepresentation
{
    float iwl, iww;
    
    if( fixed8bitsWLWW) {
        iww = 256;
        iwl = 127;
    } else {
        iww = ww;  // Window Width
        iwl = wl;  // Window Level (Center)
    }
    
    float min, max;
    min = iwl - iww / 2; 
    max = iwl + iww / 2;
    
    // 32-bit float画像から8-bitに変換
    if( transferFunctionPtr == nil) {  // LINEAR
        vImageConvert_PlanarFtoPlanar8( &srcf, &dst8, max, min, 0);
    }
}
```

### 2. `changeWLWW`メソッド（行10140-10189）

```objective-c
- (void) changeWLWW:(float)newWL :(float)newWW
{
    if( newWW !=0 || newWL != 0) {
        if( fullww > 256) {
            if( newWW < 1) newWW = 2;
            
            if( newWL - newWW/2 == 0) {
                newWL = newWW/2;
            } else {
                newWW = (int) newWW;
                newWL = (int) newWL;
            }
        }
        
        if( newWW < 0.001 * slope) newWW = 0.001 * slope;
        
        ww = newWW;
        wl = newWL;
    } else {
        // 自動計算
        [self computePixMinPixMax];
        ww = fullww;
        wl = fullwl;
    }
}
```

## 重要なポイント

1. **HOROSは32-bit float画像（`fImage`）を保持**
   - 元のDICOM画像のピクセルデータを32-bit floatとして保持
   - Window Level/Widthを適用して8-bit表現（`baseAddr`）を生成

2. **Window Level/Widthの適用方法**
   - `min = wl - ww / 2`
   - `max = wl + ww / 2`
   - `vImageConvert_PlanarFtoPlanar8(&srcf, &dst8, max, min, 0)` で変換
   - これは、`max`以上の値は255、`min`以下の値は0にマッピングされる

3. **12-bit/16-bit画像の処理**
   - `fullww > 256` の場合は、12-bit/16-bit画像と判断
   - Window Level/Widthをそのまま使用（スケーリングしない）

## Java実装での問題点

1. **dcm4che-imageioが既に8-bitに変換している**
   - dcm4che-imageioが読み込んだBufferedImageは、既にWindow Level/Widthが適用されているか、8-bitに変換されている可能性がある
   - 元の12-bit/16-bitピクセルデータにアクセスできない

2. **Window Level/Widthの適用方法が間違っている**
   - 現在の実装は、8-bit画像（0-255）に対して12-bit範囲のWindow Level/Width（Center=2047, Width=4096）を適用しようとしている
   - これでは画像が黒くなる

## 解決策

1. **dcm4che-imageioで読み込む際にWindow Level/Widthを考慮しない**
   - ImageIOで読み込む際に、Window Level/Widthを適用せず、生のピクセルデータを取得する
   - または、16-bit画像として読み込む

2. **HOROSの実装に合わせたWindow Level/Width適用**
   - 元のピクセル値範囲を考慮
   - `min = wl - ww / 2`, `max = wl + ww / 2` で変換
   - 12-bit/16-bit画像の場合は、元の範囲を考慮してスケーリング

3. **dcm4che-imageioの設定を調整**
   - Window Level/Widthを適用せずに読み込む
   - Rescale Slope/Interceptを考慮する

