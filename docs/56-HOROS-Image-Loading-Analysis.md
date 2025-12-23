# HOROS-20240407 画像読み込み処理の徹底解析と修正計画

## 概要

このドキュメントは、HOROS-20240407の画像読み込み処理（`setIndex:`メソッド）を徹底的に解析し、現在のJava実装との違いを明確化し、不具合修正計画とリファクタリング計画を統合したものです。

**最終更新**: 2025年12月

---

## 目次

1. [HOROS-20240407の処理順序](#horos-20240407の処理順序)
2. [現在の実装との違い](#現在の実装との違い)
3. [現在の不具合](#現在の不具合)
4. [修正方針](#修正方針)
5. [リファクタリング計画](#リファクタリング計画)
6. [修正手順](#修正手順)
7. [テスト項目](#テスト項目)

---

## HOROS-20240407の処理順序

### setIndex:メソッドの処理順序（DCMView.m:2536-2654）

#### 1. 基本処理

```
1. curImage = index でインデックスを設定
2. self.curDCM = [dcmPixList objectAtIndex:curImage] で新しい画像を取得
3. [self.curDCM CheckLoad] で画像を読み込む
4. ROIの処理
```

#### 2. Window Level/Widthの処理（重要）

##### COPYSETTINGSINSERIES == NO の場合（2592-2603行目）

```objective-c
if( curImage >= 0 && COPYSETTINGSINSERIES == NO)
{
    if( curWW != self.curDCM.ww || curWL != self.curDCM.wl || [self.curDCM updateToApply] == YES)
    {
        [self reapplyWindowLevel];  // 前のcurWW/curWLを新しい画像に適用
    }
    else [self.curDCM checkImageAvailble :curWW :curWL];
    
    [self updatePresentationStateFromSeriesOnlyImageLevel: YES];  // 画像/シリーズから設定を読み込む
    
    done = YES;
}
```

##### COPYSETTINGSINSERIES == YES の場合（2609-2616行目）

```objective-c
if( done == NO)
{
    if( curWW != self.curDCM.ww || curWL != self.curDCM.wl || [self.curDCM updateToApply] == YES)
    {
        [self reapplyWindowLevel];  // 前のcurWW/curWLを新しい画像に適用
    }
    else [self.curDCM checkImageAvailble :curWW :curWL];
}
```

#### 3. reapplyWindowLevelメソッド（893-922行目）

```objective-c
- (void) reapplyWindowLevel
{
    // SUV変換の処理（PET画像用）
    if( curWL != 0 && curWW != 0 && curWLWWSUVConverted != self.curDCM.SUVConverted)
    {
        // SUV変換のファクターを調整
        ...
    }
    
    // 前のcurWW/curWLを新しい画像（self.curDCM）に適用
    [self.curDCM changeWLWW :curWL :curWW];
}
```

**重要**: `reapplyWindowLevel`は**前の画像の`curWW`/`curWL`を新しい画像に適用**する

#### 4. updatePresentationStateFromSeriesOnlyImageLevelメソッド（13041-13270行目）

##### Window Level/Widthの読み込み順序（13130-13154行目）

```objective-c
float ww = 0, wl = 0;

// 1. 画像レベルから読み込む
if( [image valueForKey:@"windowWidth"]) ww = [[image valueForKey:@"windowWidth"] floatValue];
else if( !onlyImage && [series valueForKey:@"windowWidth"]) ww = [[series valueForKey:@"windowWidth"] floatValue];
else if( ![self is2DViewer]) ww = curWW;

if( [image valueForKey:@"windowLevel"]) wl = [[image valueForKey:@"windowLevel"] floatValue];
else if( !onlyImage && [series valueForKey:@"windowLevel"]) wl= [[series valueForKey:@"windowLevel"] floatValue];
else if( ![self is2DViewer]) wl = curWL;

// 2. 保存された値（savedWW/savedWL）を使用
if( ww == 0)
{
    if( (curImage >= 0) || COPYSETTINGSINSERIES == NO || [self is2DViewer] == NO)
    {
        ww = self.curDCM.savedWW;  // 現在の画像の保存された値
        wl = self.curDCM.savedWL;
    }
    else
    {
        ww = [[dcmPixList objectAtIndex: [dcmPixList count]/2] savedWW];  // シリーズ中央の画像の保存された値
        wl = [[dcmPixList objectAtIndex: [dcmPixList count]/2] savedWL];
    }
}
```

**重要**: `COPYSETTINGSINSERIES == NO`の場合、`updatePresentationStateFromSeriesOnlyImageLevel`で画像/シリーズから設定を読み込む

#### 5. 処理順序のまとめ

##### COPYSETTINGSINSERIES == NO の場合

1. 新しい画像を取得（`self.curDCM`）
2. 前の`curWW`/`curWL`が新しい画像の`ww`/`wl`と異なる場合、`reapplyWindowLevel`を呼ぶ
3. `updatePresentationStateFromSeriesOnlyImageLevel: YES`を呼んで、画像/シリーズから設定を読み込む
4. `loadTextures`でテクスチャを読み込む

##### COPYSETTINGSINSERIES == YES の場合

1. 新しい画像を取得（`self.curDCM`）
2. 前の`curWW`/`curWL`が新しい画像の`ww`/`wl`と異なる場合、`reapplyWindowLevel`を呼ぶ（前の値を維持）
3. `loadTextures`でテクスチャを読み込む

---

## 現在の実装との違い

### 問題点1: 画像読み込みのタイミング

- **HOROS**: 画像は`[self.curDCM CheckLoad]`で読み込まれる（`setIndex:`内）
- **現在の実装**: `loadDicomFile`で画像を読み込む（`loadImage`内）

### 問題点2: WW/WLの適用タイミング

- **HOROS**: `reapplyWindowLevel`で前の`curWW`/`curWL`を新しい画像に適用
- **現在の実装**: `setWindowLevel`で前の値を適用

### 問題点3: COPYSETTINGSINSERIES == NO の場合の処理

- **HOROS**: `updatePresentationStateFromSeriesOnlyImageLevel`で画像/シリーズから設定を読み込む
- **現在の実装**: `imageSettingsMap`から設定を読み込む（HOROS-20240407には存在しない）

### 問題点4: initialWindowCenter/initialWindowWidthの取得タイミング

- **HOROS**: `savedWW`/`savedWL`を使用（画像読み込み時に取得）
- **現在の実装**: `initialWindowCenter`/`initialWindowWidth`を取得する際に、自動計算が実行され、不適切な値になる可能性がある

---

## 現在の不具合

1. **CR画像**: 真っ白になる、拡大される
2. **CT画像**: WW/WLを変えて画像遷移すると白黒反転されたWW/WLになる
3. **MR画像**: 白抜け、画像遷移すると白抜けになる、縮尺も変わる

---

## 修正方針

### 1. COPYSETTINGSINSERIES == YES の場合の修正

**HOROS-20240407**:
- 前の`curWW`/`curWL`を維持
- `reapplyWindowLevel`で前の値を新しい画像に適用
- 新しい画像の`initialWindowCenter`/`initialWindowWidth`は取得しない（`resetWindowLevel`用）

**現在の実装**:
- 前の`prevWL`/`prevWW`を維持
- `setWindowLevel`で前の値を適用
- 新しい画像の`initialWindowCenter`/`initialWindowWidth`を取得（`resetWindowLevel`用）

**問題**: 新しい画像の`initialWindowCenter`/`initialWindowWidth`を取得する際に、自動計算が実行され、`initialWindowCenter`/`initialWindowWidth`が不適切な値になる可能性がある

**修正**:
```java
// 修正後（HOROS-20240407準拠）
if (copySettingsInSeries && imageView != null) {
    // 前の画像の設定を維持（curWW/curWLを維持）
    // 新しい画像のinitialWindowCenter/initialWindowWidthは取得するが、
    // windowCenter/windowWidthは前の値を維持
    imageView.setWindowLevel(prevWL, prevWW);
    // 注意: 新しい画像のinitialWindowCenter/initialWindowWidthは
    // loadDicomFile内で取得済み（resetWindowLevel用）
}
```

### 2. COPYSETTINGSINSERIES == NO の場合の修正

**HOROS-20240407**:
- `updatePresentationStateFromSeriesOnlyImageLevel: YES`を呼ぶ
- 画像/シリーズから設定を読み込む（imageObj/seriesObjから）
- 保存された値（savedWW/savedWL）を使用
- 画像/シリーズに設定がない場合、`savedWW`/`savedWL`を使用

**現在の実装**:
- `imageSettingsMap`から設定を読み込む
- 保存された設定がない場合、`fitToWindow`を呼ぶ

**問題**: `imageSettingsMap`はHOROS-20240407には存在しない。HOROS-20240407では、画像/シリーズオブジェクトから直接読み込む

**修正**:
```java
// 修正後（HOROS-20240407準拠）
// HOROS-20240407では、画像/シリーズオブジェクトから直接読み込む
// 現在の実装では、imageSettingsMapを使用しているが、
// これはHOROS-20240407には存在しない
// 修正: 画像/シリーズから設定を読み込む（DBから読み込む必要がある）
// ただし、現在の実装ではDBに保存していないため、
// 一時的にimageSettingsMapを使用する
```

### 3. initialWindowCenter/initialWindowWidthの取得タイミング

**問題**: `skipReset == true`の場合、自動計算が実行され、`initialWindowCenter`/`initialWindowWidth`が不適切な値になる

**修正**: `skipReset == true`の場合でも、`initialWindowCenter`/`initialWindowWidth`を正しく取得するが、自動計算は実行しない（画像読み込み後に実行）

### 4. 画像読み込み後の自動計算

**問題**: `skipReset == true`の場合、自動計算がスキップされるが、`initialWindowWidth <= 0`の場合、`resetWindowLevel`で正しい値が設定されない

**修正**: `skipReset == true`の場合でも、`initialWindowWidth <= 0`の場合は自動計算を実行し、`initialWindowCenter`/`initialWindowWidth`を更新する

---

## リファクタリング計画

### 問題点

1. `loadDicomFile`メソッドが長すぎて複数の責任を持っている（300行以上）
2. `skipReset`の条件分岐が複数箇所に散らばっている
3. `initialWindowCenter`/`initialWindowWidth`の更新タイミングが複雑
4. 画像読み込みとWW/WL設定の順序が複雑で、不具合が発生しやすい

### リファクタリング方針

#### 1. メソッド分割

`loadDicomFile`を以下のメソッドに分割：

- `loadDicomMetadata(Path filePath, DicomFile dicomFile)` - メタデータ（モダリティ、WW/WL、Pixel Spacing等）の取得
- `loadDicomImage(DicomFile dicomFile, boolean skipReset)` - 画像の読み込み（Weasis方式/通常方式）
- `computeWindowLevelFromImage()` - 画像からWW/WLを自動計算
- `applyWindowLevelSettings(boolean skipReset)` - WW/WLの適用
- `updateDisplayImage()` - 表示画像の更新（既存）

#### 2. 処理順序の明確化

```
1. メタデータ取得（モダリティ、WW/WL、Pixel Spacing等）
2. initialWindowCenter/initialWindowWidthの更新（常に実行）
3. 画像読み込み
4. WW/WLの自動計算（必要な場合）
5. WW/WLの適用（skipResetに応じて）
6. 表示更新
```

#### 3. 条件分岐の簡潔化

- `skipReset == true`の場合：
  - `windowCenter`/`windowWidth`は前の値を維持
  - `initialWindowCenter`/`initialWindowWidth`は常に更新（resetWindowLevel用）
  
- `skipReset == false`の場合：
  - `windowCenter`/`windowWidth`を`initialWindowCenter`/`initialWindowWidth`で初期化
  - 自動計算が必要な場合は実行

### 実装手順

1. 新しいメソッドを追加（既存の`loadDicomFile`は残す）
2. 段階的に既存の`loadDicomFile`を新しいメソッドに置き換え
3. テストして動作確認
4. 既存の`loadDicomFile`を削除

---

## 修正手順

1. **COPYSETTINGSINSERIES == YES の場合の修正**
   - `loadDicomFile`内で、`skipReset == true`の場合、自動計算をスキップ
   - ただし、`initialWindowWidth <= 0`の場合は、画像読み込み後に自動計算を実行

2. **COPYSETTINGSINSERIES == NO の場合の修正**
   - `imageSettingsMap`の使用を維持（HOROS-20240407には存在しないが、現在の実装では必要）
   - ただし、保存された設定がない場合、新しい画像の`initialWindowCenter`/`initialWindowWidth`を使用

3. **initialWindowCenter/initialWindowWidthの取得タイミングの修正**
   - `loadDicomFile`内で、常に`initialWindowCenter`/`initialWindowWidth`を取得
   - `skipReset == true`の場合でも、`initialWindowWidth <= 0`の場合は自動計算を実行

4. **画像読み込み後の処理順序の明確化**
   - メタデータ取得 → 画像読み込み → WW/WL設定 → 表示更新

---

## テスト項目

1. **CR画像**: 正常に表示されるか
2. **CT画像**: WW/WLを変えて画像遷移しても、正しいWW/WLが維持されるか
3. **MR画像**: 白抜けが解消され、画像遷移しても正常に表示されるか
4. **縮尺**: 画像遷移しても縮尺が正しく維持されるか

---

## 参考実装箇所

### HOROS-20240407ソースコード

- **DCMView.m:2536-2654**: `setIndex:`メソッド
- **DCMView.m:893-922**: `reapplyWindowLevel`メソッド
- **DCMView.m:13041-13270**: `updatePresentationStateFromSeriesOnlyImageLevel`メソッド
- **DCMView.m:2424**: `COPYSETTINGSINSERIES`プロパティ
- **DCMView.m:6486**: `COPYSETTINGSINSERIES`のデフォルト値

### Java実装

- **ImageViewerPanel.java**: `loadDicomFile`メソッド
- **ViewerController.java**: `loadImage`メソッド

---

## 実装完了状況

### ✅ 実装完了項目（2024年実装完了）

1. **フェーズ1: loadDicomFileメソッドの簡素化** ✅
   - `skipReset`パラメータを削除
   - 画像の読み込みのみを実行（WW/WLは適用しない）
   - `CheckLoad`相当の処理のみを実装

2. **フェーズ2: reapplyWindowLevel相当のメソッドを実装** ✅
   - `ImageViewerPanel.reapplyWindowLevel(double prevWindowLevel, double prevWindowWidth)`を実装
   - 前の画像の`curWW`/`curWL`を新しい画像に適用

3. **フェーズ3: compute8bitRepresentation相当のメソッドを実装** ✅
   - `ImageViewerPanel.compute8bitRepresentation()`を実装
   - WW/WLを適用して8-bit表現を生成
   - `needToCompute8bitRepresentation`フラグで制御

4. **フェーズ4: changeWLWW相当のメソッドを実装** ✅
   - `ImageViewerPanel.changeWLWW(double newWL, double newWW)`を実装
   - `reapplyWindowLevel`から呼び出される

5. **フェーズ5: loadImageメソッドをHOROSのsetIndex:に忠実に再現** ✅
   - HOROS-20240407の`setIndex:`メソッド（DCMView.m:2536-2654）の処理順序に従って実装
   - `COPYSETTINGSINSERIES == NO`と`COPYSETTINGSINSERIES == YES`の分岐を実装
   - `reapplyWindowLevel`と`updatePresentationStateFromSeriesOnlyImageLevel`を適切に呼び出し

6. **フェーズ6: updatePresentationStateFromSeriesOnlyImageLevel相当のメソッドを実装** ✅
   - `ViewerController.updatePresentationStateFromSeriesOnlyImageLevel(boolean onlyImage)`を実装
   - 画像レベル → シリーズレベル → デフォルトの順で設定を読み込む
   - xFlipped, yFlipped, scale, rotation, offset, windowWidth, windowLevelを読み込む

7. **フェーズ7: ImageSettingsクラスとimageSettingsMapの削除** ✅
   - `ImageSettings`クラスを削除
   - `imageSettingsMap`を削除
   - データベースから直接設定を読み込む実装に変更

8. **データベース実装** ✅
   - `series`テーブルと`image`テーブルに設定用カラムを追加
   - `DicomDatabase.migrateSeriesTable()`と`DicomDatabase.migrateImageTable()`を実装
   - `DicomDatabase.updateImageSettings()`と`DicomDatabase.updateSeriesSettings()`を実装

### 実装の詳細

#### loadImageメソッドの処理順序（HOROS-20240407準拠）

1. **画像ロード前に現在の表示設定を取得**（シリーズ内コピー用）
2. **現在表示中の設定を保存**（`COPYSETTINGSINSERIES == NO`の場合）
   - データベースの`image`テーブルに保存
3. **インデックスを設定**
4. **画像の読み込み**（`CheckLoad`相当）
   - `imageView.loadDicomFile(imagePath)`を呼び出し
   - WW/WLは適用しない
5. **Window Level/Widthの処理**
   - **`COPYSETTINGSINSERIES == NO`の場合**:
     - `reapplyWindowLevel()`を呼び出し
     - `updatePresentationStateFromSeriesOnlyImageLevel(true)`を呼び出し
   - **`COPYSETTINGSINSERIES == YES`の場合**:
     - 前の画像の設定（Zoom, Pan, Rotation, Flip, WL/WW）をそのまま適用
     - `reapplyWindowLevel()`を呼び出し
6. **テクスチャの読み込み**（`loadTextures`相当）
   - `paintComponent`で`compute8bitRepresentation`が呼ばれる
7. **その他の処理**
   - スライダー更新
   - 情報ラベル更新
   - 自動伝播

#### updatePresentationStateFromSeriesOnlyImageLevelメソッドの処理順序

1. **xFlipped/yFlippedの読み込み**
   - 画像レベル → シリーズレベル → デフォルト（false）

2. **Scale（ズーム）の読み込み**
   - `COPYSETTINGSINSERIES == NO`の場合のみ
   - 画像レベル → シリーズレベル → scaleToFit

3. **Rotationの読み込み**
   - 画像レベル → シリーズレベル → デフォルト（0）

4. **Offset（パン）の読み込み**
   - `COPYSETTINGSINSERIES == NO`の場合のみ
   - 画像レベル → シリーズレベル → デフォルト（0, 0）

5. **Window Level/Widthの読み込み**
   - 画像レベル → シリーズレベル → initialWindowCenter/initialWindowWidth

## 変更履歴

- 2025年1月: 実装完了報告を統合（58, 59, 60×2の内容を統合）
- 2025年12月: 画像読み込み関連ドキュメントを統合（3ファイル → 1ファイル）
- 2024年12月: 初期解析とドキュメント作成
