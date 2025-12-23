# isKeyView条件の詳細分析

## HOROSの実装（DCMView.m:6934）

```objective-c
if( [note object] != self && isKeyView == YES && matrix == 0 && newImage > -1)
```

**条件**: `isKeyView == YES` は受信側（self）の状態

## 画像更新条件（DCMView.m:7200）

```objective-c
if((selfViewer != frontMostViewer && otherViewer == frontMostViewer) || otherViewer.timer)
```

**条件**: 受信側がfrontMostでなく、送信側がfrontMostの場合に画像を更新する

## 矛盾

- `isKeyView == YES` で受信側がキービューである必要がある
- しかし、画像更新条件では `selfViewer != frontMostViewer` で、受信側がfrontMostでない場合に更新する

これは矛盾しているように見えます。

## 可能性

1. `isKeyView`と`frontMostViewer`は異なる概念である可能性
   - `isKeyView`: ビューがフォーカスを持っているか
   - `frontMostViewer`: ウィンドウが最前面にあるか

2. `isKeyView == YES`の条件は、**同期処理のエントリーポイント**として機能している可能性
   - 受信側がキービューでない場合、同期処理自体をスキップする
   - しかし、実際には受信側がキービューでなくても処理する必要がある

3. HOROSの実装が間違っている、または条件が変更された可能性

## 現在の問題

現在の実装では、`isKeyView == YES`の条件により、受信側が`isKeyView=false`の場合に処理がスキップされています。しかし、送信側がキービューになった時に他のビューアーに同期するためには、受信側がキービューでなくても処理する必要があります。

