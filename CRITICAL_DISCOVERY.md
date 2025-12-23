# 重大な発見：isKeyViewの条件の真の意味

## 重要な発見

### `sync:`メソッドの条件（DCMView.m:6934）
```objective-c
if( [note object] != self && isKeyView == YES && matrix == 0 && newImage > -1)
```

### `sendSyncMessage:`の条件（DCMView.m:6725）
```objective-c
if( [ViewerController numberOf2DViewer] > 1 && isKeyView && [self is2DViewer])
```

## 重要なポイント

### `[note object]`の意味
- `OsirixSyncNotification`の`object`は、**送信側のビューア**（`self`）です（DCMView.m:6731）
- `sync:`メソッドの`[note object]`は、**送信側のビューア**を指します
- `[note object] != self`は、**自分自身からの通知ではない**ことを確認します

### `isKeyView == YES`の意味
- `sync:`メソッドの`isKeyView == YES`は、**受信側のビューア**（`self`）がキービューであることを確認します
- `sendSyncMessage:`の`isKeyView`は、**送信側のビューア**（`self`）がキービューであることを確認します

## 矛盾の解決

### 実際の動作フロー
1. ビューアAが`becomeFirstResponder`で`isKeyView = YES`になる
2. ビューアAが`sendSyncMessage:`を呼び出す（`isKeyView == YES`なので送信される）
3. ビューアBが`sync:`を受信する
4. ビューアBの`isKeyView`の状態は？

### 重要な疑問
- ビューアBが`sync:`を受信する時点で、ビューアBの`isKeyView`は何か？
- `becomeFirstResponder`で`isKeyView = YES`になるが、他のビューアの`isKeyView`はどうなるか？

### 可能性
1. `newImageViewisKey:`が`OsirixDCMViewDidBecomeFirstResponderNotification`を監視している
2. ビューアAが`becomeFirstResponder`になると、`OsirixDCMViewDidBecomeFirstResponderNotification`が送信される
3. ビューアBがこの通知を受信して`newImageViewisKey:`を呼び出す
4. `newImageViewisKey:`で`isKeyView = NO`を設定する

しかし、`newImageViewisKey:`の通知登録が見つかりません。

### 別の可能性
- `sync:`メソッドの`isKeyView == YES`の条件は、実際には**送信側**がキービューであることを確認するためのものではない
- 実際には、**受信側**がキービューでないと同期メッセージを処理しないという意味

しかし、これでは同期が機能しないはずです。

## 次のステップ

1. `newImageViewisKey:`の通知登録を探す（ViewerController.mなど）
2. `OsirixDCMViewDidBecomeFirstResponderNotification`の監視を確認
3. `becomeFirstResponder`/`resignFirstResponder`の呼び出しタイミングを追跡
4. 実際のHOROS-20240407の動作を確認


