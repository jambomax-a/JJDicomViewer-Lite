# HOROS-20240407 sync:メソッド徹底解析

## 重要な条件チェック

### 1. sync:メソッドの最初の条件（DCMView.m:6934）
```objective-c
if( [note object] != self && isKeyView == YES && matrix == 0 && newImage > -1)
```
- `isKeyView == YES`の条件がある！
- この条件を満たさない場合は、sync:メソッドの処理全体がスキップされる
- **現在の実装ではこの条件がチェックされていない**

### 2. 同期条件（DCMView.m:6997）
```objective-c
if( same3DReferenceWorld || registeredViewer || [[NSUserDefaults standardUserDefaults] boolForKey:@"SAMESTUDY"] == NO || syncSeriesIndex != -1)
```
- `SAMESTUDY == NO`の場合も同期する
- **現在の実装では`SAMESTUDY == NO`の条件が実装されていない**

### 3. 画像更新条件（DCMView.m:7200）
```objective-c
if((selfViewer != frontMostViewer && otherViewer == frontMostViewer) || otherViewer.timer)
```
- この条件を満たす場合のみ画像を更新
- 現在の実装は正しい

### 4. 交線表示条件（DCMView.m:7211）
```objective-c
if( (selfViewer != frontMostViewer && otherViewer == frontMostViewer) || [otherView.windowController FullScreenON])
```
- この条件を満たす場合のみ交線を表示
- 現在の実装は正しい

## 問題点

1. **`isKeyView == YES`の条件がチェックされていない**
   - これが原因で、`isKeyView == NO`の場合でも同期処理が実行されている可能性がある

2. **`SAMESTUDY == NO`の条件が実装されていない**
   - これが原因で、異なるスタディ間での同期が正しく動作していない可能性がある

3. **`syncImageIndexToOtherViewers`の直接呼び出し**
   - HOROS-20240407では、`setIndex:`で通知を送信するだけ
   - 全ての同期処理は`sync:`メソッドで行われる
   - 現在の実装では`syncImageIndexToOtherViewers`を直接呼び出しているが、これはHOROS-20240407の実装と異なる

## 修正が必要な点

1. `onSyncNotification`の最初に`isKeyView == YES`の条件を追加
2. `SAMESTUDY == NO`の条件を実装
3. `syncImageIndexToOtherViewers`の直接呼び出しを削除（既に修正済み）

