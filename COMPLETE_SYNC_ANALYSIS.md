# HOROS-20240407 同期システム完全解析

## 重要な発見

### 1. 同期モードはグローバル変数
- **HOROS-20240407**: `syncro`はグローバル変数（DCMView.m:86）
- **現在の実装**: 各ビューアーで独立した`sameStudySyncMode`
- **問題**: 同期モードが共有されていないため、同期が機能しない

### 2. 同期ボタンの動作
- **通常クリック**: `syncroOFF` → `syncroLOC`
- **Alt+クリック**: `syncroOFF` → `syncroREL`
- **既に同期ON**: 同期OFF

### 3. 同期通知の送信条件
- `numberOf2DViewer > 1`: 2Dビューアーが2つ以上
- `isKeyView`: キービューである
- `is2DViewer`: 2Dビューアーである

### 4. 同期通知の受信条件
- `superview`が異なる
- `is2DViewer`である
- `[note object] != self`: 自分自身からの通知ではない
- `isKeyView == YES`: **受信側がキービューである必要がある**
- `matrix == 0`: Image Tilingが無効
- `newImage > -1`: 有効な画像インデックス

### 5. 同期条件
- `same3DReferenceWorld || registeredViewer || SAMESTUDY == NO || syncSeriesIndex != -1`

### 6. 画像更新条件
- `(selfViewer != frontMostViewer && otherViewer == frontMostViewer) || otherViewer.timer`

### 7. 交線表示条件
- `(selfViewer != frontMostViewer && otherViewer == frontMostViewer) || FullScreenON`

## 根本的な問題

1. **同期モードの共有**: `syncro`はグローバル変数だが、現在の実装では各ビューアーで独立している
2. **isKeyViewの設定**: `becomeFirstResponder`と`resignFirstResponder`が正しく呼ばれていない
3. **frontMostViewerの取得**: `frontMostDisplayed2DViewer`の実装が正しくない可能性がある
4. **同期条件**: `same3DReferenceWorld`の判定が正しく行われていない

## 修正が必要な点

1. **同期モードをグローバル変数にする**: `syncro`をグローバル変数として実装
2. **isKeyViewの設定**: `becomeFirstResponder`と`resignFirstResponder`を正しく実装
3. **frontMostViewerの取得**: `frontMostDisplayed2DViewer`を正しく実装
4. **同期条件**: `same3DReferenceWorld`の判定を正しく実装

