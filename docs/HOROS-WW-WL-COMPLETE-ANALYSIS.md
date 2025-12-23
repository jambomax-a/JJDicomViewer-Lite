# HOROS-20240407 Window Level/Width 完全分析

## 概要

HOROS-20240407のWindow Level/Width（WW/WL）処理を包括的に分析します。USカラー画像、CT/MRの初期表示、8-bit変換処理を含む全体的な実装をまとめます。

最終更新: 2024年12月

---

## HOROSの実装詳細

### 1. 画像データの保持方法

HOROSでは、元のDICOM画像データは**32-bit float（`fImage`）**として保持されています：

```objective-c
// DCMPix.m:9961-9962
// ***** SOURCE IMAGE IS 32 BIT FLOAT
// ***** ***** ***** ***** *****

srcf.data = [self computefImage];  // 32-bit float画像を取得
```

**重要なポイント**:
- 元の12-bit/16-bitのピクセル値が32-bit floatとして保持される
- これにより、元のピクセル値範囲（0-4095など）が失われない

### 2. 8-bit変換処理（`compute8bitRepresentation`）

HOROSでは、表示用に8-bit画像（`baseAddr`）を生成します：

```objective-c
// DCMPix.m:9932-10008
- (void) compute8bitRepresentation
{
    float iwl, iww;
    
    if( fixed8bitsWLWW)
    {
        iww = 256;
        iwl = 127;
    }
    else
    {
        iww = ww;  // Window Width（12-bit/16-bit範囲の値がそのまま使用される）
        iwl = wl;  // Window Level（12-bit/16-bit範囲の値がそのまま使用される）
    }
    
    float min = iwl - iww / 2; 
    float max = iwl + iww / 2;
    
    // 32-bit float画像から8-bit画像に変換
    vImageConvert_PlanarFtoPlanar8( &srcf, &dst8, max, min, 0);
}
```

**重要なポイント**:
- Window Level/Width（`iwl`, `iww`）は**12-bit/16-bit範囲の値がそのまま使用される**
- `vImageConvert_PlanarFtoPlanar8`関数で、32-bit float画像から8-bit画像に変換される
- `max`と`min`は、Window Level/Widthから計算された値（12-bit/16-bit範囲）

### 3. Window Level/Widthの適用

HOROSでは、Window Level/Widthは32-bit float画像に対して直接適用されます：

```objective-c
// DCMPix.m:9957-9958
min = iwl - iww / 2;  // 例: 2048 - 4096/2 = 0
max = iwl + iww / 2;  // 例: 2048 + 4096/2 = 4096

// 32-bit float画像から8-bit画像に変換
vImageConvert_PlanarFtoPlanar8( &srcf, &dst8, max, min, 0);
```

**重要なポイント**:
- Window Center=2048, Window Width=4096（12-bit範囲）の場合、`min=0`, `max=4096`として使用される
- 32-bit float画像のピクセル値が0-4096の範囲でマッピングされ、8-bit画像（0-255）に変換される
- **スケーリング処理は不要**（32-bit float画像に対して直接適用されるため）

### 4. 初期WW/WL値の決定（DCMView.m:13132-13154）

HOROSでは、以下の優先順位でWW/WL値を決定します：

1. **imageオブジェクトから取得**（データベースに保存されている値）
2. **seriesオブジェクトから取得**（シリーズレベルで保存されている値）
3. **curWW/curWL**（2Dビューワー以外の場合）
4. **savedWW/savedWL**（DICOMファイルから読み込んだ値）
5. **シリーズ内の中間画像のsavedWW/savedWL**

**重要なポイント**:
- `ww == 0`の場合のみ、`savedWW`/`savedWL`を使用
- `savedWW`/`savedWL`が0の場合は、`computePixMinPixMax()`で自動計算

### 5. RGB画像のWW/WL適用（DCMPix.m:10095-10134）

HOROSでは、**RGB画像に対してもWW/WLが適用されます**：

```objective-c
// DCMPix.m:10080-10134 (inside compute8bitRepresentation)
if( isRGB)
{
    vImage_Buffer   src, dst;
    Pixel_8			convTable[256];
    long			diff = max - min, val;
    
    // APPLY WINDOW LEVEL TO RGB IMAGE
    if( transferFunctionPtr == nil)	// LINEAR
    {
        for( long i = 0; i < 256; i++)
        {
            val = (((i-min) * 255L) / diff);
            if( val < 0) val = 0;
            else if( val > 255) val = 255;
            convTable[i] = val;
        }
    }
    // ...
    vImageTableLookUp_ARGB8888 ( &src,  &dst,  convTable,  convTable,  convTable,  convTable,  0);
}
```

**重要なポイント**:
- 変換テーブルは0-255の範囲で作成
- `i`は入力値（0-255）
- `min`と`max`は`iwl`（Window Level）と`iww`（Window Width）から計算
- RGB画像の場合、入力値は0-255の範囲
- `vImageTableLookUp_ARGB8888`で各カラーチャンネルに適用

### 6. Window Center/Widthの読み込み条件

DCMPix.m:5637-5638行目では、Window Center/Widthの読み込み時に`isRGB == NO`の条件があります：

```objective-c
if ([dcmObject attributeValueWithName:@"WindowCenter"] && isRGB == NO) 
    savedWL = (float)[[dcmObject attributeValueWithName:@"WindowCenter"] floatValue];
if ([dcmObject attributeValueWithName:@"WindowWidth"] && isRGB == NO) 
    savedWW = (float)[[dcmObject attributeValueWithName:@"WindowWidth"] floatValue];
```

**重要なポイント**:
- RGB画像の場合、DICOMファイルからWindow Center/Widthの初期値は読み込まない
- ただし、表示処理（`compute8bitRepresentation`）ではRGB画像に対してもWW/WLが適用される
- これは、初期値の読み込みと表示処理で異なる条件を使用していることを意味する

### 7. computePixMinPixMax()の実装（DCMPix.m:8989-9029）

```objective-c
- (void)computePixMinPixMax
{
    float pixmin, pixmax;
    if( isRGB)
    {
        pixmax = 255;
        pixmin = 0;
    }
    else
    {
        // fImageから最小値と最大値を取得（32-bit float画像）
        vDSP_minv ( fImage,  1, &fmin, width * height);
        vDSP_maxv ( fImage , 1, &fmax, width * height);
        
        pixmax = fmax;
        pixmin = fmin;
        
        if( pixmin == pixmax)
        {
            pixmax = pixmin + 20;
        }
    }
    
    fullwl = pixmin + (pixmax - pixmin)/2;
    fullww = (pixmax - pixmin);
}
```

**重要なポイント**:
- RGB画像の場合: `pixmax = 255`, `pixmin = 0` → `fullwl = 127.5`, `fullww = 255`
- グレースケール画像の場合: `fImage`（32-bit float）から最小値と最大値を取得
- `fullww`と`fullwl`は元のピクセル値範囲（12-bit/16-bit範囲）から計算

### 8. savedWW/savedWLが0の場合（DCMPix.m:10175-10181）

```objective-c
else  // need to compute best values
{
    [self computePixMinPixMax];
    ww = fullww;
    wl = fullwl;
}
```

**重要なポイント**:
- `savedWW`/`savedWL`が0の場合、`computePixMinPixMax()`で自動計算
- `fullww`と`fullwl`は画像の実際のピクセル値範囲から計算

---

## 現在の実装の問題点と解決策

### 1. CT/MRの初期画像白飛び状態

**問題**: 現在の実装では、8-bitに変換された画像から最小値/最大値を計算しているため、実際の12-bit/16-bit範囲の値が失われている可能性があります。

**原因**:
- `originalImage`は既に8-bitに変換されている
- 8-bit画像から計算すると、実際のピクセル値範囲（0-4095など）が失われる
- HOROSでは、`fImage`（32-bit float）から計算しているため、元のピクセル値範囲が保持されている

**解決策（Weasis方式）**:
- `DicomImageReadParam`でWindow Level/Widthを設定して読み込み時に適用
- 12-bit/16-bit範囲の値もそのまま渡す（dcm4che3が適切に処理）

### 2. USのWW/WLの不安定感

**問題**: RGB画像の変換テーブルの計算に問題がある可能性があります。

**原因**:
- `windowMin`/`windowMax`が0-255の範囲を超えている場合、変換テーブルが正しく機能しない
- HOROSでは、RGB画像の場合でも`min = iwl - iww / 2`、`max = iwl + iww / 2`で計算
- しかし、入力値は0-255の範囲なので、変換テーブルも0-255の範囲で作成する必要がある

**解決策**:
- RGB画像の場合、`windowMin`/`windowMax`を0-255の範囲に制限
- 変換テーブルの計算は、入力値（0-255）に対して正しく機能するようにする
- Weasis方式で読み込む際に、`DicomImageReadParam`で適切に設定

---

## HOROSと現在の実装の比較

| 項目 | HOROS-20240407 | 現在のJava実装 |
|------|---------------|---------------|
| **画像データ保持** | 32-bit float | BufferedImage (8-bit) |
| **WW/WL適用** | 32-bit float画像に対して直接適用 | `DicomImageReadParam`で読み込み時に適用 |
| **12-bit/16-bit対応** | 32-bit floatとして保持されるため問題なし | `DicomImageReadParam`で12-bit/16-bit範囲の値をそのまま渡す |
| **RGB画像WW/WL** | 適用される（変換テーブル方式） | 適用される（変換テーブル方式） |
| **初期値読み込み** | RGB画像の場合は読み込まない | RGB画像の場合は読み込まない（HOROSに合わせる） |

---

## 参考実装

### HOROS-20240407

- `horos-20240407/Horos/Sources/DCMPix.m`
  - `compute8bitRepresentation` (line 9932-10138)
  - `computePixMinPixMax` (line 8989-9029)
  - `changeWLWW` (line 10140-10181)
  - Window Center/Widthの読み込み (line 5637-5638)
- `horos-20240407/Horos/Sources/DCMView.m`
  - WW/WL値の決定 (line 13132-13154)

---

## 変更履歴

- 2024年12月: Window Level/Width関連の分析を統合
- 2024年: USカラー画像とCT/MRの初期表示問題を解決するため、HOROS-20240407の実装を詳細分析
- 2024年: OSIRIXでの確認結果を反映し、HOROS-20240407の実装を再分析
- 2024年: HOROS-20240407の8-bit変換処理を詳細分析

