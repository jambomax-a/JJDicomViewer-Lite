# isKeyViewの矛盾の最終的な解決

## 重要な発見

### 1. `initWithFrame:imageRows:imageColumns:`で`makeFirstResponder`を呼び出している（DCMView.m:12653）
```objective-c
[self.window makeFirstResponder: self];
```

これは、初期化時に`isKeyView = YES`を設定する可能性があります。

### 2. `computeMagnifyLens:`で`isKeyView == NO`の場合に`makeFirstResponder`を呼び出している（DCMView.m:3530-3531）
```objective-c
if( isKeyView == NO)
    [[self window] makeFirstResponder: self];
```

### 3. `mouseMovedInView:`が`computeMagnifyLens:`を呼び出している（DCMView.m:3788）
```objective-c
[self computeMagnifyLens: imageLocation];
```

つまり、マウスがビューに入った時点で自動的に`isKeyView = YES`が設定される可能性があります。

## 矛盾の解決

### `sync:`メソッドの条件`isKeyView == YES`の意味
- 受信側がキービューでないと同期メッセージを処理しないという意味
- しかし、送信側もキービューでないと送信しない

### 実際の動作フロー
1. ビューアAにマウスが入る
2. `mouseMovedInView:`が呼び出される
3. `computeMagnifyLens:`が呼び出される
4. `isKeyView == NO`の場合、`makeFirstResponder`を呼び出す
5. `becomeFirstResponder`で`isKeyView = YES`が設定される
6. `becomeKeyWindow`で`sendSyncMessage: 0`が呼び出される
7. ビューアBが`sync:`を受信する
8. ビューアBの`isKeyView`の状態は？

### 重要な疑問
- ビューアBが`sync:`を受信する時点で、ビューアBの`isKeyView`は何か？
- `becomeFirstResponder`で`isKeyView = YES`になるが、他のビューアの`isKeyView`はどうなるか？

### 可能性1: `newImageViewisKey:`が`OsirixDCMViewDidBecomeFirstResponderNotification`を監視している
- ビューアAが`becomeFirstResponder`になると、`OsirixDCMViewDidBecomeFirstResponderNotification`が送信される
- ビューアBがこの通知を受信して`newImageViewisKey:`を呼び出す
- `newImageViewisKey:`で`isKeyView = NO`を設定する

しかし、`newImageViewisKey:`の通知登録が見つかりません。

### 可能性2: `sync:`メソッドの条件の解釈が間違っている
- `isKeyView == YES`の条件は、**送信側**がキービューであることを確認するためのものではない
- 実際には、**受信側**がキービューでないと同期メッセージを処理しないという意味

しかし、`sendSyncMessage:`の条件も`isKeyView == YES`を要求しています。

### 可能性3: タイミングの問題
- `becomeFirstResponder`で`isKeyView = YES`を設定
- `becomeKeyWindow`で`sendSyncMessage: 0`を呼び出す（この時点で`isKeyView == YES`）
- 他のビューアが`sync:`を受信（この時点で他のビューアの`isKeyView`はまだ`YES`のまま？）

しかし、`newImageViewisKey:`で`isKeyView = NO`を設定するはずです。

### 可能性4: `newImageViewisKey:`が使われていない
- `newImageViewisKey:`メソッドは存在するが、実際には使われていない可能性
- `isKeyView`の管理は、`becomeFirstResponder`/`resignFirstResponder`のみで行われている可能性

## 次のステップ

1. `newImageViewisKey:`の通知登録を探す（別のファイルにある可能性）
2. `OsirixDCMViewDidBecomeFirstResponderNotification`の監視を確認
3. `becomeFirstResponder`/`resignFirstResponder`の呼び出しタイミングを追跡
4. 実際のHOROS-20240407の動作を確認

## 重要な発見：`computeMagnifyLens:`で`isKeyView`を設定

これは、マウスがビューに入った時点で自動的に`isKeyView = YES`が設定されることを意味します。

しかし、`sync:`メソッドの条件`isKeyView == YES`は、**受信側**がキービューであることを確認します。

これは矛盾しているように見えますが、実際には：
- 送信側がキービューでないと送信しない
- 受信側がキービューでないと受信しない

通常、1つのビューアだけがキービューであるため、同期が機能しないはずです。

しかし、HOROS-20240407では同期が機能しているので、何か見落としがあるはずです。


