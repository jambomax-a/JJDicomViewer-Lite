# マウス進入時のisKeyView設定

## 重要な発見

### 1. `computeMagnifyLens:`メソッド（DCMView.m:3530-3531）
```objective-c
if( isKeyView == NO)
    [[self window] makeFirstResponder: self];
```

これは、マウスがビューに入った時点で自動的に`isKeyView`を設定する可能性があります。

### 2. `acceptsFirstMouse:`メソッド（DCMView.m:2656-2660）
```objective-c
-(BOOL) acceptsFirstMouse:(NSEvent*) theEvent
{
    if (currentTool >= 5) return NO;  // A ROI TOOL !
    else return YES;
}
```

`acceptsFirstMouse`が`YES`を返すということは、マウスがビューに入った時点で自動的に`becomeFirstResponder`が呼び出される可能性があります（macOSの標準動作）。

### 3. `mouseMovedInView:`メソッド
`mouseMovedInView:`が呼び出される可能性があります。確認する必要があります。

## 重要なポイント

### macOSの動作
- `acceptsFirstMouse`が`YES`を返すと、マウスがビューに入った時点で自動的に`becomeFirstResponder`が呼び出される可能性があります
- `computeMagnifyLens:`で`isKeyView == NO`の場合に`makeFirstResponder`を呼び出すということは、マウスがビューに入った時点で自動的に`isKeyView`を設定する可能性があります

### つまり
1. マウスがビューに入る
2. `acceptsFirstMouse`が`YES`を返す
3. 自動的に`becomeFirstResponder`が呼び出される（macOSの標準動作）
4. `becomeFirstResponder`で`isKeyView = YES`が設定される
5. `becomeKeyWindow`で`sendSyncMessage: 0`が呼び出される
6. または、`computeMagnifyLens:`で`isKeyView == NO`の場合、`makeFirstResponder`を呼び出す
7. `becomeFirstResponder`で`isKeyView = YES`が設定される
8. `becomeKeyWindow`で`sendSyncMessage: 0`が呼び出される

## 矛盾の解決

### `sync:`メソッドの条件`isKeyView == YES`の意味
- 受信側がキービューでないと同期メッセージを処理しないという意味
- しかし、送信側もキービューでないと送信しない

### 実際の動作フロー
1. ビューアAにマウスが入る
2. 自動的に`becomeFirstResponder`が呼び出される
3. `isKeyView = YES`が設定される
4. `becomeKeyWindow`で`sendSyncMessage: 0`が呼び出される
5. ビューアBが`sync:`を受信する
6. ビューアBの`isKeyView`の状態は？

### 重要な疑問
- ビューアBが`sync:`を受信する時点で、ビューアBの`isKeyView`は何か？
- `becomeFirstResponder`で`isKeyView = YES`になるが、他のビューアの`isKeyView`はどうなるか？

### 可能性
- `newImageViewisKey:`が`OsirixDCMViewDidBecomeFirstResponderNotification`を監視している
- ビューアAが`becomeFirstResponder`になると、`OsirixDCMViewDidBecomeFirstResponderNotification`が送信される
- ビューアBがこの通知を受信して`newImageViewisKey:`を呼び出す
- `newImageViewisKey:`で`isKeyView = NO`を設定する

しかし、`newImageViewisKey:`の通知登録が見つかりません。

## 次のステップ

1. `mouseMovedInView:`メソッドを確認
2. `newImageViewisKey:`の通知登録を探す（別のファイルにある可能性）
3. `OsirixDCMViewDidBecomeFirstResponderNotification`の監視を確認
4. `becomeFirstResponder`/`resignFirstResponder`の呼び出しタイミングを追跡


