# isKeyView設定の発見

## 重要な発見

### 1. 右クリック時（DCMView.m:5067-5085）
```objective-c
- (void) rightMouseDown:(NSEvent *)event
{
    if ([self eventToPlugins:event]) return;
    
    if( curImage < 0) return;
    
    [[self window] makeKeyAndOrderFront: self];
    [[self window] makeFirstResponder: self];  // ← ここでisKeyViewがYESになる！
    [self sendSyncMessage: 0];
    
    // ...
}
```

### 2. マウスダウン時（DCMView.m:5059-5065付近）
```objective-c
[[self window] makeKeyAndOrderFront: self];
[[self window] makeFirstResponder: self];  // ← ここでisKeyViewがYESになる！
[self sendSyncMessage: 0];
```

### 3. scrollWheel時
`scrollWheel:`メソッドを確認する必要があります。

## 重要なポイント

### `makeFirstResponder:`の動作
- `makeFirstResponder:`を呼び出すと、`becomeFirstResponder`が呼び出される
- `becomeFirstResponder`で`isKeyView = YES`が設定される
- `becomeKeyWindow`で`sendSyncMessage: 0`が呼び出される

### つまり
1. ユーザーが右クリックやマウスダウンを行う
2. `makeFirstResponder:`が呼び出される
3. `becomeFirstResponder`で`isKeyView = YES`が設定される
4. `becomeKeyWindow`で`sendSyncMessage: 0`が呼び出される
5. 他のビューアが`sync:`を受信する

### しかし、問題は
- `sync:`メソッドの条件`isKeyView == YES`は、**受信側**がキービューであることを確認します
- 送信側がキービューになった直後、受信側の`isKeyView`はまだ`YES`のままかもしれません

### 可能性
- `newImageViewisKey:`が`OsirixDCMViewDidBecomeFirstResponderNotification`を監視している
- ビューアAが`becomeFirstResponder`になると、`OsirixDCMViewDidBecomeFirstResponderNotification`が送信される
- ビューアBがこの通知を受信して`newImageViewisKey:`を呼び出す
- `newImageViewisKey:`で`isKeyView = NO`を設定する

しかし、`newImageViewisKey:`の通知登録が見つかりません。

## 次のステップ

1. `scrollWheel:`メソッドを確認
2. `newImageViewisKey:`の通知登録を探す
3. `OsirixDCMViewDidBecomeFirstResponderNotification`の監視を確認


