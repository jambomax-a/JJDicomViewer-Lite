# 同期ロジック不具合修正内容

## 適用した修正

### 1. `onSyncNotification`の条件チェック修正
- **修正前**: `newImage < 0`でチェックしていたが、`newImage`はまだ計算されていない
- **修正後**: `currentIndex < 0`でチェック（HOROSの`newImage > -1`に相当）
- **理由**: HOROSの実装では、`newImage`は`curImage`で初期化され、条件チェック時に使用される（DCMView.m:6925-6926, 6934）

### 2. `setKeyView`の実装修正
- **修正前**: `keyView`が`true`の場合、交線をリセットしてから`sendSyncMessage: 0`を呼んでいた
- **修正後**: `keyView`が`true`の場合、交線をリセットしてから`sendSyncMessage: 0`を呼ぶ（`becomeKeyWindow`相当）
  - `keyView`が`false`の場合、`sendSyncMessage: 0`を呼ぶ（`resignFirstResponder`相当）
- **理由**: HOROSの実装では、`becomeKeyWindow`で交線をリセットしてから`sendSyncMessage: 0`を呼び、`resignFirstResponder`で`isKeyView = NO`を設定してから`sendSyncMessage: 0`を呼ぶ

## 残っている問題

### 1. `blendingView`への再帰呼び出し
- HOROSの実装では、`blendingView`への再帰呼び出しがある（DCMView.m:7262-7263）
- 現在の実装では、`blendingView`機能が未実装のため、この処理はスキップされる
- 将来的に`blendingView`機能を実装する場合は、この処理を追加する必要がある

### 2. ウィンドウフォーカス管理
- 現在の実装では、`windowGainedFocus`で`setThisViewKeyAndOthersNot()`を呼んでいる
- HOROSの実装では、`becomeFirstResponder`で`isKeyView = YES`を設定し、`becomeKeyWindow`で交線をリセットして`sendSyncMessage: 0`を呼んでいる
- 現在の実装は動作するが、HOROSの実装と完全に一致していない可能性がある

## 次のステップ

1. テストして動作確認する
2. ウィンドウフォーカス管理を完全に実装する（必要に応じて）
3. `blendingView`機能を実装する（将来的に）

