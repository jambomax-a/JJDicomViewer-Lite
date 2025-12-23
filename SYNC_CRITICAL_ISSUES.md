# 同期機能の重大な問題点

## 発見された問題

### 1. 2つの異なる通知が存在する
- `OsirixDCMViewIndexChangedNotification` - `setIndex:`で送信（DCMView.m:2627）
- `OsirixSyncNotification` - `sendSyncMessage:`で送信（DCMView.m:6731）

現在のJava実装では、`OsirixSyncNotification`のみを実装していますが、`OsirixDCMViewIndexChangedNotification`は実装していない可能性があります。

### 2. `isKeyView`の条件の矛盾
- `sendSyncMessage:`の条件：送信側が`isKeyView == YES`でないと送信しない（DCMView.m:6725）
- `sync:`の条件：受信側が`isKeyView == YES`でないと受信しない（DCMView.m:6934）

これは矛盾しているように見えますが、実際には：
- 送信側がキービューでないと送信しない
- 受信側がキービューでないと受信しない

通常、1つのビューアだけがキービューであるため、同期が機能しないはずです。

### 3. `setIndex:`と`sendSyncMessage:`の関係
- `setIndex:`は`OsirixDCMViewIndexChangedNotification`を送信する
- `sendSyncMessage:`は`OsirixSyncNotification`を送信する
- これらの通知は異なる目的で使用される可能性がある

### 4. ログから見える問題
- `sendSyncMessage`は送信されているが、`onSyncNotification`のログが全く出ていない
- これは、`onSyncNotification`が呼ばれていないか、早期リターンしている可能性がある

## 次のステップ

1. `DCMView.m`の`sync:`メソッドの完全な解析
2. `setIndex:`メソッドの完全な解析
3. `sendSyncMessage:`メソッドの完全な解析
4. 通知の登録方法の確認
5. `isKeyView`の設定タイミングの確認
6. `becomeFirstResponder`/`resignFirstResponder`の実装確認


