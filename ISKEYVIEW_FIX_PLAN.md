# isKeyView条件の修正計画

## 問題

現在の実装では、`isKeyView == YES`の条件により、受信側が`isKeyView=false`の場合に処理がスキップされています。しかし、送信側がキービューになった時に他のビューアーに同期するためには、受信側がキービューでなくても処理する必要があります。

## HOROSの実装の再確認

HOROSの実装では：
- `sync:`メソッドで`isKeyView == YES`で受信側が処理するという条件がある
- しかし、画像更新条件では`selfViewer != frontMostViewer && otherViewer == frontMostViewer`で、受信側がfrontMostでない場合に更新する

## 可能性

1. `isKeyView == YES`の条件は、**同期処理のエントリーポイント**として機能しているが、実際には受信側がキービューでなくても処理する必要がある
2. HOROSの実装が間違っている、または条件が変更された
3. `isKeyView`の状態管理が間違っている

## 修正案

`isKeyView == YES`の条件を削除または緩和する。しかし、HOROSの実装を完全に写経するという方針があるため、まずはHOROSの実装が正しく動作する理由を理解する必要があります。

可能性として、HOROSでは`newImageViewisKey:`通知によって`isKeyView`が更新されるため、通知のタイミングが異なる可能性があります。

