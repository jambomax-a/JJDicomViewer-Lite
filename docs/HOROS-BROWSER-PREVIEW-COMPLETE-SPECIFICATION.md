# HOROS-20240407 BrowserController プレビュービューワー完全仕様

## 概要

BrowserControllerのプレビュービューワー（PreviewView）に関する完全な仕様書。レイアウト構造、コンポーネント、操作仕様、データフロー、実装上の注意点を包括的にまとめています。

## レイアウト構造

### スプリットビュー構成

BrowserControllerのサムネイルパネルは、左側にサムネイルマトリックス、右側にプレビュービューワーが配置された2分割レイアウトです。

- **`splitViewVert`** (垂直スプリットビュー)
  - **左側**: `matrixView` (サムネイルマトリックス、`BrowserMatrix`)
  - **右側**: `imageView` (`PreviewView`、プレビュービューワー)

### コンポーネント

1. **`matrixView`** (`NSView`)
   - サムネイルマトリックス（`BrowserMatrix`、`oMatrix`）を含む
   - `thumbnailsScrollView` (`NSScrollView`) でスクロール可能

2. **`imageView`** (`PreviewView`)
   - `DCMView`のサブクラス
   - 画像表示、WW/WL調整、マウスホイールでの画像遷移などの機能を持つ

3. **`animationSlider`** (`NSSlider`)
   - プレビュービューワーの下部に配置
   - 複数画像がある場合に有効化され、画像を選択できる
   - 複数画像がある場合: 有効化、最大値 = 画像数 - 1
   - 単一画像の場合: 無効化
   - スライダーの値が変更されると`previewSliderAction:`が呼ばれる

## 機能仕様

### 1. サムネイル選択時のプレビュー更新

**メソッド**: `previewSliderAction:`

- サムネイルマトリックス（`oMatrix`）でセルが選択されると、プレビュービューワーが更新される
- 選択されたセルのタグ（インデックス）に基づいて、対応する画像がプレビュービューワーに表示される

```objective-c
- (void) previewSliderAction:(id) sender
{
    NSButtonCell *cell = [oMatrix selectedCell];
    if( cell && dontUpdatePreviewPane == NO)
    {
        if( [cell tag] >= [matrixViewArray count]) return;
        
        // 画像を取得
        NSArray *images = [self imagesArray: [matrixViewArray objectAtIndex: [cell tag]]];
        
        // プレビュービューワーに画像を設定
        [imageView setPixels:previewPix files:images rois:nil firstImage:[cell tag] level:'i' reset:YES];
    }
}
```

### 2. WW/WL調整

**メソッド**: 
- `[imageView getWLWW:&wl :&ww]` - WW/WLを取得
- `[imageView setWLWW:wl :ww]` - WW/WLを設定

プレビュービューワー（`PreviewView`）は`DCMView`のサブクラスなので、`DCMView`のWW/WL調整機能を使用できます。

### 3. マウスホイールでの画像遷移

**メソッド**: `scrollWheel:`

- マウスホイールで画像を前後に移動できる
- 上に回す（deltaY > 0）: 次の画像（`animationSlider`の値を+1）
- 下に回す（deltaY < 0）: 前の画像（`animationSlider`の値を-1）
- 最大値を超えた場合: 0に戻る
- 0より小さい場合: 最大値に戻る
- `previewSliderAction:`を呼び出してプレビューを更新

## マウス操作

### 1. 左クリック（mouseDown:）

**基本動作**:
- WW/WL調整の開始（SHIFTキー + ドラッグ）
- パン操作の開始（通常のドラッグ）

**詳細**:
- `clickCount == 2`（ダブルクリック）の場合: `matrixDoublePressed:`を呼び出してビューワーを開く
- SHIFTキー + ドラッグ: WW/WL調整
- 通常のドラッグ: パン操作

### 2. 右クリック（rightMouseDown:）

**基本動作**:
- 左クリックと同じ動作（`mouseDown:`を呼び出す）

### 3. マウスホイール（scrollWheel:）

**基本動作**:
- 画像遷移（`animationSlider`の値を変更）

**詳細**:
- 上に回す（deltaY > 0）: 次の画像（`animationSlider`の値を+1）
- 下に回す（deltaY < 0）: 前の画像（`animationSlider`の値を-1）
- 最大値を超えた場合: 0に戻る
- 0より小さい場合: 最大値に戻る
- `previewSliderAction:`を呼び出してプレビューを更新

### 4. マウスドラッグ（mouseDragged:）

**基本動作**:
- WW/WL調整（SHIFTキー + ドラッグ）
- パン操作（通常のドラッグ）

## キーボード操作

### 1. ホットキー（actionForHotKey:）

- プレビュービューワーでもホットキーが使用可能
- `DCMView`のホットキー機能を継承
- `PreviewView`は`actionForHotKey:`をオーバーライドして親クラスを呼び出す

**注意**: HOROS-20240407のコードでは、`BrowserController`のプレビュービューワーでは`keyDown:`が親クラスに委譲されるため、通常のキー操作（矢印キー、PageUp/Down、Home/End、Space、Return、Escape、Tabなど）は動作しません。

ただし、`DCMView`でサポートされているキー操作は以下の通りです（通常のビューワーでは使用可能）:

- **矢印キー（Left/Right）**: 画像遷移
  - Left: 前の画像
  - Right: 次の画像
  - Ctrl + Left/Right: スタック単位で画像遷移
  - Alt + Left/Right: キー画像設定
- **矢印キー（Up/Down）**: ズーム調整
  - Up: ズームイン（scaleValue + 1/50）
  - Down: ズームアウト（scaleValue - 1/50）
- **PageUp/PageDown**: 画像遷移（複数画像単位）
- **Home/End**: 最初/最後の画像に移動
- **Space/Return/Enter**: アニメーション再生/停止（2Dビューワーのみ）
- **Escape**: フルスクリーン解除（2Dビューワーのみ）
- **Tab**: アノテーション表示切り替え
- **Delete/Backspace**: ROI削除

## データフロー

1. **サムネイルマトリックスの初期化**
   - `matrixInit:` - マトリックスの行数と列数を設定
   - `matrixLoadIcons:` - サムネイル画像を非同期で読み込み

2. **サムネイル選択**
   - `matrixPressed:` - サムネイルがクリックされたとき
   - `previewSliderAction:` - プレビュービューワーを更新

3. **スライダー操作**
   - `animationSlider`の値変更 → `previewSliderAction:` → プレビュービューワー更新

4. **マウスホイール操作**
   - `scrollWheel:` → `animationSlider`の値を変更 → `previewSliderAction:` → プレビュービューワー更新

5. **ダブルクリック**
   - `mouseDown:`（`clickCount == 2`） → `matrixDoublePressed:` → ビューワーを開く

6. **プレビュービューワーの更新**
   - `[imageView setPixels:files:rois:firstImage:level:reset:]` - 画像データを設定
   - `[imageView setIndex:]` - 表示する画像インデックスを設定

## 実装上の注意点

1. **スプリットビューの保存/復元**
   - `[splitViewVert restoreDefault: @"SplitVert2"]` - スプリットビューの位置を復元
   - `[splitViewVert saveDefault: @"SplitVert2"]` - スプリットビューの位置を保存

2. **プレビューピクセルデータの管理**
   - `previewPix` (`NSMutableArray`) - プレビュー用の`DCMPix`オブジェクトの配列
   - `previewPixThumbnails` (`NSMutableArray`) - サムネイル画像の配列

3. **非同期読み込み**
   - `matrixLoadIconsThread` - サムネイル画像の非同期読み込みスレッド
   - `@synchronized( previewPixThumbnails)` - スレッドセーフなアクセス

4. **スライダーの有効化/無効化**
   - `initAnimationSlider`で画像数に応じて有効化/無効化
   - 複数画像がある場合のみ有効化

5. **マウスホイールの方向**
   - `Scroll Wheel Reversed`設定に応じて方向を反転
   - デフォルト: 上に回す = 次の画像

6. **ダブルクリックの判定**
   - `clickCount == 2`で判定
   - `BrowserController`のウィンドウの場合のみ`matrixDoublePressed:`を呼び出す

## 参考実装

- `BrowserController.m`: `previewSliderAction:`, `scrollWheel:`, `initAnimationSlider`, `previewPerformAnimation:`, `matrixPressed:`, `matrixInit:`, `matrixLoadIcons:`
- `DCMView.m`: `mouseDown:`, `rightMouseDown:`, `mouseDragged:`, `keyDown:`
- `PreviewView.m`: `actionForHotKey:`

