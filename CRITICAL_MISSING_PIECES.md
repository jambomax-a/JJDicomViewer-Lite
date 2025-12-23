# 同期ロジックの重要な見落とし

## 根本的な問題

### 1. `newImageViewisKey:`の通知登録箇所が見つかっていない
- `DCMView.m:12810-12814`に`newImageViewisKey:`メソッドが定義されている
- `becomeFirstResponder`が`OsirixDCMViewDidBecomeFirstResponderNotification`を送信している（DCMView.m:12605）
- しかし、`newImageViewisKey:`がこの通知を監視している登録箇所が見つかっていない

### 2. `isKeyView`の管理が不完全
- `becomeFirstResponder`で`isKeyView = YES`を設定（DCMView.m:12576）
- `resignFirstResponder`で`isKeyView = NO`を設定（DCMView.m:12792）
- `newImageViewisKey:`で他のビューがキーになったら`isKeyView = NO`を設定（DCMView.m:12812-12813）
- しかし、この通知の登録箇所が見つかっていない

### 3. `becomeKeyWindow`と`becomeMainWindow`の違い
- `becomeKeyWindow`（DCMView.m:12555-12572）: `sendSyncMessage: 0`を呼ぶ
- `becomeMainWindow`（DCMView.m:12539-12553）: `sendSyncMessage: 0`を呼ぶ
- `becomeFirstResponder`（DCMView.m:12574-12610）: `becomeKeyWindow`を呼ぶ

### 4. `computeMagnifyLens:`と`mouseMovedInView:`の関係
- `mouseMovedInView:`（DCMView.m:3720-3999）が`computeMagnifyLens:`を呼ぶ
- `computeMagnifyLens:`（DCMView.m:3522-3541）が`isKeyView == NO`の場合に`makeFirstResponder`を呼ぶ
- つまり、マウスがビューの上に来ただけで`isKeyView`が変わる可能性がある

## 見落としている可能性のある重要な点

### 1. 通知の登録タイミング
- `DCMView`の初期化時に通知を登録している可能性
- `initWithFrame:`や`awakeFromNib`で登録している可能性

### 2. `isKeyView`の初期値
- `isKeyView`の初期値が何か
- 最初に表示されるビューが自動的にキービューになるのか

### 3. `sendSyncMessage:`の呼び出しタイミング
- `becomeKeyWindow`で呼ばれる
- `becomeMainWindow`で呼ばれる
- `resignFirstResponder`で呼ばれる
- `scrollWheel:`で呼ばれる（DCMView.m:4847-5046）
- その他のタイミング

### 4. `avoidRecursiveSync`の管理
- `sync:`の開始時にインクリメント（DCMView.m:6932）
- 早期リターン時にデクリメント（DCMView.m:6959, 6966）
- `sync:`の終了時にデクリメント（DCMView.m:7265）
- `blendingView`への再帰呼び出しの前後（DCMView.m:7263）

## 次のステップ

1. `newImageViewisKey:`の通知登録箇所を徹底的に探す
2. すべての通知の登録と送信の流れを完全に把握する
3. `isKeyView`の管理を完全に理解する
4. `sendSyncMessage:`のすべての呼び出し箇所を確認する
5. `avoidRecursiveSync`の管理を完全に理解する

