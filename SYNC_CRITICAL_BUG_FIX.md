# 同期ロジックの重要な不具合と修正方針

## 発見された重要な不具合

### 1. `onSyncNotification`の条件チェックの問題
- **現在の実装**（3473行目）: `isKeyView == YES`で**受信側**がキービューである必要がある
- **HOROSの実装**（6934行目）: `isKeyView == YES`で**受信側**がキービューである必要がある
- **しかし、重要な点**: 送信側がキービューでないと送信しない（`sendSyncMessage:`の条件）、受信側がキービューでないと処理しない（`sync:`の条件）
- **問題**: 送信側と受信側の両方がキービューである必要はないが、現在の実装ではこの条件が正しく機能していない可能性がある

### 2. ウィンドウフォーカス管理の問題
- **現在の実装**: `windowGainedFocus`で`setThisViewKeyAndOthersNot()`を呼び、`sendSyncMessage: 0`を呼んでいる
- **HOROSの実装**: `becomeFirstResponder`で`isKeyView = YES`を設定し、`becomeKeyWindow`で`sendSyncMessage: 0`を呼んでいる
- **問題**: `becomeKeyWindow`で交線をリセットしているが、現在の実装では`setKeyView`で交線をリセットしている（2229行目）。これは正しいが、タイミングが異なる可能性がある

### 3. `avoidRecursiveSync`の管理の問題
- **現在の実装**: 早期リターン時に`avoidRecursiveSync--`を呼んでいるが、HOROSの実装でも同様
- **問題**: しかし、`avoidRecursiveSync`のインクリメント/デクリメントのタイミングが正しいか確認する必要がある

## 修正方針

### 1. `onSyncNotification`の条件チェックを確認
- HOROSの実装では、`isKeyView == YES`で**受信側**がキービューである必要がある
- しかし、送信側がキービューでないと送信しない（`sendSyncMessage:`の条件）
- つまり、送信側はキービュー、受信側もキービューで処理する必要がある
- しかし、両方が同時にキービューである必要はない

### 2. ウィンドウフォーカス管理を完全に実装
- `becomeFirstResponder`相当の処理で`isKeyView = YES`を設定
- `becomeKeyWindow`相当の処理で交線をリセットし、`sendSyncMessage: 0`を呼ぶ
- `resignFirstResponder`相当の処理で`isKeyView = NO`を設定し、`sendSyncMessage: 0`を呼ぶ

### 3. `avoidRecursiveSync`の管理を確認
- HOROSの実装を完全に写経する

## 次のステップ

1. `onSyncNotification`の条件チェックを確認・修正する
2. ウィンドウフォーカス管理を完全に実装する
3. `avoidRecursiveSync`の管理を確認・修正する
4. テストして動作確認する

