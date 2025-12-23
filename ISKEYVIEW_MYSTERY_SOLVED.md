# isKeyViewの矛盾の解決

## 問題の整理

### 矛盾点
1. `sendSyncMessage:`の条件（DCMView.m:6725）: 送信側が`isKeyView == YES`でないと送信しない
2. `sync:`の条件（DCMView.m:6934）: 受信側が`isKeyView == YES`でないと受信しない

通常、1つのビューアだけがキービューであるため、この条件では同期が機能しないはずです。

## 重要な発見

### 1. `becomeFirstResponder`の動作（DCMView.m:12574-12610）
```objective-c
- (BOOL)becomeFirstResponder
{
    isKeyView = YES;
    // ...
    [[NSNotificationCenter defaultCenter] postNotificationName:OsirixDCMViewDidBecomeFirstResponderNotification object:self];
    // ...
}
```

### 2. `newImageViewisKey:`の動作（DCMView.m:12810-12814）
```objective-c
-(void)newImageViewisKey:(NSNotification *)note
{
    if ([note object] != self)
        isKeyView = NO;
}
```

### 3. `resignFirstResponder`の動作（DCMView.m:12790-12797）
```objective-c
- (BOOL)resignFirstResponder
{
    isKeyView = NO;
    [self setNeedsDisplay:YES];
    [self sendSyncMessage: 0];
    
    return [super resignFirstResponder];
}
```

## 解決策の仮説

### 仮説1: `newImageViewisKey:`が`OsirixDCMViewDidBecomeFirstResponderNotification`を監視している
- `becomeFirstResponder`で`OsirixDCMViewDidBecomeFirstResponderNotification`を送信
- 他のビューアがこの通知を受信して`newImageViewisKey:`を呼び出す
- `newImageViewisKey:`で`isKeyView = NO`を設定

しかし、`newImageViewisKey:`の通知登録が見つかりません。

### 仮説2: `sync:`メソッドの条件の解釈が間違っている
- `isKeyView == YES`の条件は、**送信側**がキービューであることを確認するためのものではない
- 実際には、**受信側**がキービューでないと同期メッセージを処理しないという意味

しかし、`sendSyncMessage:`の条件も`isKeyView == YES`を要求しています。

### 仮説3: タイミングの問題
- `becomeFirstResponder`で`isKeyView = YES`を設定
- `sendSyncMessage:`を呼び出す（この時点で`isKeyView == YES`）
- 他のビューアが`sync:`を受信（この時点で他のビューアの`isKeyView`はまだ`YES`のまま？）

しかし、`newImageViewisKey:`で`isKeyView = NO`を設定するはずです。

## 次のステップ

1. `newImageViewisKey:`の通知登録を探す（別のファイルにある可能性）
2. `OsirixDCMViewDidBecomeFirstResponderNotification`の監視を確認
3. `becomeFirstResponder`/`resignFirstResponder`の呼び出しタイミングを追跡
4. 実際のHOROS-20240407の動作を確認（デバッグログなど）


