# HOROSと現在の実装の重要な違い

## 致命的な違い1: superviewチェックが欠けている

### HOROSの実装（DCMView.m:6923-6924）
```objective-c
if( ![[[note object] superview] isEqual:[self superview]] && [self is2DViewer])
{
    // 処理
}
```

**意味**: 通知を送信したビュー（otherView）と受信したビュー（self）が**同じsuperview（親ビュー）を持たない**場合のみ処理する。つまり、**同じウィンドウ内の複数のビュー間では同期しない**。

### 現在の実装
```java
if (imageView == null || imageList == null || imageList.isEmpty()) {
    return; // is2DViewer相当のチェック
}
// superviewチェックが完全に欠けている！
```

**問題**: 同じウィンドウ内の複数のビュー間でも同期してしまう可能性がある。

## 致命的な違い2: userInfoの取得タイミング

### HOROSの実装（DCMView.m:6934-6936）
```objective-c
if( [note object] != self && isKeyView == YES && matrix == 0 && newImage > -1)
{
    NSDictionary *instructions = [note userInfo];
    // 処理
}
```

**意味**: 条件チェックを**先に**行い、条件を満たす場合のみ`userInfo`を取得する。

### 現在の実装
```java
SyncNotificationCenter.SyncNotification.UserInfo userInfo = notification.getUserInfo();
if (userInfo == null) {
    return;
}
ImagePanel otherView = userInfo.getView();
// その後で条件チェック
if (otherView == imageView || !imageView.isKeyView() || ...) {
    return;
}
```

**問題**: 条件を満たさない場合でも`userInfo`を取得してしまう。

## 致命的な違い3: avoidRecursiveSyncの管理

### HOROSの実装（DCMView.m:6931-6977）
```objective-c
if( avoidRecursiveSync > 1) return;
avoidRecursiveSync++;

if( [note object] != self && isKeyView == YES && matrix == 0 && newImage > -1)
{
    // 条件を満たす場合のみ処理
    if( [instructions valueForKey: @"offsetsync"] == nil)
    {
        avoidRecursiveSync--;
        return;
    }
    // ...
}
// ...
avoidRecursiveSync--;
```

**意味**: 条件を満たさない場合は`avoidRecursiveSync--`を呼ばずに`return`する。

### 現在の実装
```java
if (avoidRecursiveSync > 1) {
    return;
}
avoidRecursiveSync++;

try {
    // ...
    if (otherView == imageView || !imageView.isKeyView() || ...) {
        return; // avoidRecursiveSync--が呼ばれない
    }
    // ...
} finally {
    avoidRecursiveSync--; // 常に呼ばれる
}
```

**問題**: `try-finally`を使っているため、条件を満たさない場合でも`avoidRecursiveSync--`が呼ばれてしまう。これはHOROSの動作と異なる。

## 修正方針

1. **superviewチェックを追加**: 同じウィンドウ内の複数のビュー間では同期しないようにする（ただし、Java/Swingでは`superview`の概念がないため、別の方法で判定する必要がある）

2. **userInfoの取得タイミングを修正**: 条件チェックを先に行い、条件を満たす場合のみ`userInfo`を取得する

3. **avoidRecursiveSyncの管理を修正**: `try-finally`を使わず、HOROSと同じように条件を満たす場合のみ`avoidRecursiveSync--`を呼ぶ

