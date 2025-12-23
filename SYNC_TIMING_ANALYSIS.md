# 同期タイミング分析

## ログから分かったこと

### タイムスタンプ 15:20:21.496
```
setThisViewKeyAndOthersNot: 開始 this=frame2
ImageViewerPanel.setKeyView: oldKeyView=true -> newKeyView=true (frame2)
setThisViewKeyAndOthersNot: このビューアーにisKeyView=trueを設定完了 (frame2)
setThisViewKeyAndOthersNot: 他のビューアー(frame3)にisKeyView=falseを設定
```

### タイムスタンプ 15:20:21.511
```
onSyncNotification: 受信開始 (frame0, frame1, frame3)
onSyncNotification: 条件チェック isKeyView=false (全ての受信側)
onSyncNotification: 条件を満たさないため終了
```

## 問題の本質

1. `setThisViewKeyAndOthersNot`で他のビューアーを`isKeyView=false`に設定
2. その直後に`sendSyncMessage(0)`が呼ばれて通知が送信される
3. しかし、受信側のビューアーは既に`isKeyView=false`になっている
4. その結果、`onSyncNotification`で条件を満たさずに処理がスキップされる

## HOROSの実装との比較

HOROSの実装では：
- `becomeFirstResponder`で`isKeyView = YES`を設定
- `becomeKeyWindow`で`sendSyncMessage: 0`を呼ぶ
- `newImageViewisKey:`通知で他のビューアーの`isKeyView = NO`を設定

しかし、現在の実装では：
- `windowGainedFocus`で`setThisViewKeyAndOthersNot`を呼び、他のビューアーを`isKeyView=false`に設定
- その後に`sendSyncMessage(0)`を呼ぶ

**問題**: `setThisViewKeyAndOthersNot`で他のビューアーを`isKeyView=false`に設定してから通知を送信するため、受信側が既に`isKeyView=false`になっている。

## 解決策

HOROSの実装を正確に再現する必要があります。`newImageViewisKey:`通知を実装するか、`isKeyView`の条件を再検討する必要があります。

しかし、HOROSの`sync:`メソッドでは`isKeyView == YES`で受信側が処理するという条件があります。これは、**受信側がキービューである必要がある**という意味ですが、これでは送信側がキービューになった時に他のビューアーに同期できないことになります。

可能性：
1. `isKeyView`の条件が間違っている（受信側がキービューである必要はない）
2. `newImageViewisKey:`通知の実装が間違っている
3. HOROSの実装を誤解している

