# isKeyView条件の分析

## 現在の問題

ログから分かったこと：
1. 送信側（frame2）は `isKeyView=true` で送信している
2. 受信側（frame0, frame1, frame3）は全て `isKeyView=false` になっている
3. `onSyncNotification`で `isKeyView=false` のため条件を満たさずに処理がスキップされている

## HOROSの実装（DCMView.m:6934）

```objective-c
if( [note object] != self && isKeyView == YES && matrix == 0 && newImage > -1)
```

**条件**: 受信側の`isKeyView == YES`の場合のみ処理する

## 問題の本質

`windowGainedFocus` → `setThisViewKeyAndOthersNot` → 他のビューアーを `isKeyView=false` に設定した直後に、そのビューアーが `onSyncNotification` を受信すると、既に `isKeyView=false` になっているため処理されない。

しかし、送信側がキービューで送信しているということは、**送信側の変更を他のビューアーに反映させたい**ということです。

## 考えられる解釈

HOROSの実装を見ると、`isKeyView == YES` で受信側が処理するという条件があります。これは、**受信側がキービューである必要がある**という意味ですが、これは矛盾しているように見えます。

しかし、よく考えると：
- 送信側がキービューで変更を送信
- 受信側もキービューである必要がある

これは、**両方のビューアーがキービューである必要がある**ということになりますが、これは不可能です（同時に1つのビューアーしかキービューになれない）。

## 正しい解釈の可能性

HOROSの実装を見直す必要があります。おそらく：
- `isKeyView == YES` は、**送信側**がキービューであることを確認する条件
- 受信側については、別の条件で判定する必要がある可能性

または、`isKeyView`の意味が異なる可能性があります。

