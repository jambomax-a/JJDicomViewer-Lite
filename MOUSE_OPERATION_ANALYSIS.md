# マウス操作とisKeyViewの管理の完全解析

## 重要な発見

### 1. `mouseMovedInView:`が`computeMagnifyLens:`を呼び出す
- `DCMView.m:3720-3999`に`mouseMovedInView:`メソッドが定義されている
- `DCMView.m:3788`で`computeMagnifyLens:`を呼び出す（Shiftキーが押されている場合）

### 2. `computeMagnifyLens:`が`makeFirstResponder`を呼び出す
- `DCMView.m:3522-3541`に`computeMagnifyLens:`メソッドが定義されている
- `DCMView.m:3530-3531`で`isKeyView == NO`の場合に`[[self window] makeFirstResponder: self]`を呼び出す
- `makeFirstResponder`が`becomeFirstResponder`を呼び出す
- `becomeFirstResponder`が`isKeyView = YES`を設定し、`OsirixDCMViewDidBecomeFirstResponderNotification`を送信

### 3. マウス操作の流れ
```
mouseMoved: (NSEvent)
  → mouseMovedInView: (NSPoint)
    → computeMagnifyLens: (NSPoint) [Shiftキーが押されている場合]
      → makeFirstResponder: self [isKeyView == NOの場合]
        → becomeFirstResponder
          → isKeyView = YES
          → OsirixDCMViewDidBecomeFirstResponderNotificationを送信
```

### 4. `mouseEntered:`と`mouseExited:`
- `DCMView.m:12818-12838`に`mouseEntered:`と`mouseExited:`が定義されている
- `mouseEntered:`は`cursorSet = YES`を設定
- `mouseExited:`は`deleteLens`を呼び出し、`mouseXPos`と`mouseYPos`を0に設定

### 5. `acceptsFirstResponder`
- `DCMView.m:2662-2667`に`acceptsFirstResponder`が定義されている
- `curDCM == nil`の場合は`NO`を返す
- それ以外の場合は`YES`を返す

## 重要なポイント

### マウスがビューワーの上に乗ったときの処理
1. `mouseMoved:`が呼ばれる
2. `mouseMovedInView:`が呼ばれる
3. Shiftキーが押されている場合、`computeMagnifyLens:`が呼ばれる
4. `isKeyView == NO`の場合、`makeFirstResponder: self`が呼ばれる
5. `becomeFirstResponder`が呼ばれ、`isKeyView = YES`が設定される

### `isKeyView`の管理
- `becomeFirstResponder`で`isKeyView = YES`を設定（DCMView.m:12576）
- `resignFirstResponder`で`isKeyView = NO`を設定（DCMView.m:12792）
- `computeMagnifyLens:`で`isKeyView == NO`の場合に`makeFirstResponder`を呼ぶ（DCMView.m:3530-3531）

### 画面遷移のトリガー
- マウスがビューワーの上に乗ったときに、`computeMagnifyLens:`が呼ばれる
- `isKeyView == NO`の場合、`makeFirstResponder`が呼ばれる
- `becomeFirstResponder`が呼ばれ、`isKeyView = YES`が設定される
- これにより、ビューワーが選択され、同期メッセージが送信される可能性がある

## 次のステップ

1. `mouseMoved:`の実装を確認する
2. `makeFirstResponder`の呼び出し箇所を完全に確認する
3. マウス操作と`isKeyView`の関係を完全に理解する
4. 同期ロジックを完全に再実装する

