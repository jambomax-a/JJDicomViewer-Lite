# マウス操作とisKeyViewの管理の完全解析（最終版）

## 重要な発見

### 1. `mouseMoved:`の実装（DCMView.m:3954-3992）
```objective-c
-(void) mouseMoved: (NSEvent*) theEvent
{
    if( CGCursorIsVisible() == NO && lensTexture == nil) return;
    if( ![[self window] isVisible]) return;
    if ([self eventToPlugins:theEvent]) return;
    if( !drawing) return;
    if( [self is2DViewer] == YES)
    {
        if( [[self windowController] windowWillClose]) return;
    }
    if (self.curDCM == nil) return;
    if( dcmPixList == nil) return;
    if( avoidMouseMovedRecursive) return;
    
    avoidMouseMovedRecursive = YES;
    // ...
    NSPoint eventLocation = [theEvent locationInWindow];
    [self mouseMovedInView: eventLocation];
    // ...
    avoidMouseMovedRecursive = NO;
}
```

### 2. `mouseMovedInView:`が`computeMagnifyLens:`を呼び出す（DCMView.m:3788）
```objective-c
else if( (modifierFlags & (NSShiftKeyMask|NSCommandKeyMask|NSControlKeyMask|NSAlternateKeyMask)) == NSShiftKeyMask && mouseDragging == NO)
{
    if( [self roiTool: currentTool] == NO)
    {
        [self computeMagnifyLens: imageLocation];
    }
}
```
- Shiftキーが押されている場合のみ`computeMagnifyLens:`を呼び出す

### 3. `computeMagnifyLens:`が`makeFirstResponder`を呼び出す（DCMView.m:3530-3531）
```objective-c
if( isKeyView == NO)
    [[self window] makeFirstResponder: self];
```
- `isKeyView == NO`の場合に`makeFirstResponder: self`を呼び出す
- `makeFirstResponder`が`becomeFirstResponder`を呼び出す
- `becomeFirstResponder`が`isKeyView = YES`を設定し、`OsirixDCMViewDidBecomeFirstResponderNotification`を送信

### 4. マウス操作の完全な流れ
```
1. マウスがビューワーの上で移動
   ↓
2. mouseMoved: (NSEvent*) が呼ばれる
   ↓
3. mouseMovedInView: (NSPoint) が呼ばれる
   ↓
4. Shiftキーが押されている場合、computeMagnifyLens: (NSPoint) が呼ばれる
   ↓
5. isKeyView == NO の場合、makeFirstResponder: self が呼ばれる
   ↓
6. becomeFirstResponder が呼ばれる
   ↓
7. isKeyView = YES が設定される
   ↓
8. OsirixDCMViewDidBecomeFirstResponderNotification が送信される
   ↓
9. becomeKeyWindow が呼ばれる
   ↓
10. sendSyncMessage: 0 が呼ばれる
```

### 5. 重要なポイント
- **Shiftキーが押されている場合のみ**`computeMagnifyLens:`が呼ばれる
- `computeMagnifyLens:`が`isKeyView == NO`の場合に`makeFirstResponder`を呼ぶ
- これにより、マウスがビューワーの上に乗ったときに、ビューワーが選択される

### 6. しかし、通常のマウス移動では`computeMagnifyLens:`が呼ばれない
- Shiftキーが押されていない場合、`computeMagnifyLens:`は呼ばれない
- つまり、通常のマウス移動では`makeFirstResponder`が呼ばれない
- しかし、他の方法で`becomeFirstResponder`が呼ばれる可能性がある

## 疑問点

### 通常のマウス移動でビューワーが選択されるメカニズム
- Shiftキーが押されていない場合、`computeMagnifyLens:`は呼ばれない
- しかし、ユーザーは「マウスがビューワーの上に乗ったところでビューワーを選択して画面遷移とかしてるはず」と言っている
- 他の方法で`becomeFirstResponder`が呼ばれる可能性がある

### 可能性
1. ウィンドウのフォーカス管理（`windowDidBecomeKey:`など）
2. マウスクリック（`mouseDown:`など）
3. その他のイベント

## 次のステップ

1. `mouseDown:`の実装を確認する
2. ウィンドウのフォーカス管理を確認する
3. 通常のマウス移動でビューワーが選択されるメカニズムを完全に理解する
4. 同期ロジックを完全に再実装する

