# 最終的な発見：isKeyViewの条件の真の意味

## 重要な発見

### `becomeKeyWindow`メソッド（DCMView.m:12555-12570）
```objective-c
-(void) becomeKeyWindow
{
    sliceFromTo[ 0][ 0] = HUGE_VALF;
    sliceFromTo2[ 0][ 0] = HUGE_VALF;
    sliceFromToS[ 0][ 0] = HUGE_VALF;
    sliceFromToE[ 0][ 0] = HUGE_VALF;
    sliceVector[ 0] = sliceVector[ 1] = sliceVector[ 2] = 0;
    slicePoint3D[ 0] = HUGE_VALF;
    
    [self erase2DPointMarker];
    if( blendingView) [blendingView erase2DPointMarker];
    
    [self sendSyncMessage: 0];  // ← ここで送信！
    
    [self flagsChanged: [[NSApplication sharedApplication] currentEvent]];
}
```

### `becomeFirstResponder`の流れ（DCMView.m:12574-12610）
1. `isKeyView = YES`を設定
2. `becomeKeyWindow`を呼び出す
3. `becomeKeyWindow`で`sendSyncMessage: 0`を呼び出す（この時点で`isKeyView == YES`なので送信される）
4. `OsirixDCMViewDidBecomeFirstResponderNotification`を送信

## 矛盾の解決

### 実際の動作フロー
1. ビューアAが`becomeFirstResponder`で`isKeyView = YES`になる
2. ビューアAが`becomeKeyWindow`で`sendSyncMessage: 0`を呼び出す（`isKeyView == YES`なので送信される）
3. ビューアBが`sync:`を受信する
4. ビューアBの`isKeyView`の状態は？

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

## 次のステップ

1. `newImageViewisKey:`の通知登録を探す（別のファイルにある可能性）
2. `OsirixDCMViewDidBecomeFirstResponderNotification`の監視を確認
3. `becomeFirstResponder`/`resignFirstResponder`の呼び出しタイミングを追跡
4. 実際のHOROS-20240407の動作を確認（デバッグログなど）

## 重要な発見：`becomeKeyWindow`で`sendSyncMessage: 0`を呼び出している

これは、`becomeFirstResponder`の流れで`sendSyncMessage: 0`が呼び出されることを意味します。

しかし、`sync:`メソッドの条件`isKeyView == YES`は、**受信側**がキービューであることを確認します。

これは矛盾しているように見えますが、実際には：
- 送信側がキービューでないと送信しない
- 受信側がキービューでないと受信しない

通常、1つのビューアだけがキービューであるため、同期が機能しないはずです。

しかし、HOROS-20240407では同期が機能しているので、何か見落としがあるはずです。


