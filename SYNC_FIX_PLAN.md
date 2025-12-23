# 同期ロジック不具合修正計画

## これまでの解析結果

### 重要な発見
1. `newImageViewisKey:`の通知登録箇所が見つからない（未使用のコードの可能性）
2. `isKeyView`の管理は`becomeFirstResponder`/`resignFirstResponder`で行われている
3. ウィンドウフォーカス管理が重要（`ToolbarPanel`や`ThumbnailsListPanel`が`makeKeyAndOrderFront:`を呼び出す）
4. `mouseMovedInView:`が`computeMagnifyLens:`を呼び出し、それが`makeFirstResponder`を呼び出す（Shiftキーが押されている場合のみ）
5. `becomeFirstResponder`が`becomeKeyWindow`を呼び出し、それが`sendSyncMessage: 0`を呼び出す

### 同期フローの完全な理解
```
1. makeKeyAndOrderFront: が呼ばれる
   ↓
2. ウィンドウがキーになる
   ↓
3. ウィンドウのfirstResponderが自動的に設定される
   ↓
4. becomeFirstResponder が呼ばれる
   ↓
5. isKeyView = YES が設定される
   ↓
6. becomeKeyWindow が呼ばれる
   ↓
7. sendSyncMessage: 0 が呼ばれる
```

## 修正が必要な箇所

### 1. `isKeyView`の管理
- `becomeFirstResponder`で`isKeyView = YES`を設定
- `resignFirstResponder`で`isKeyView = NO`を設定
- ウィンドウフォーカス管理と連携

### 2. `onSyncNotification`の完全再実装
- HOROS-20240407の`sync:`メソッドを完全写経
- `avoidRecursiveSync`の管理を完全写経
- `same3DReferenceWorld`の判定を完全写経
- 交線表示処理を完全写経

### 3. `sendSyncMessage:`の条件
- `numberOf2DViewer > 1 && isKeyView && is2DViewer()`の場合のみ送信
- `isKeyView`の管理が正しく行われている必要がある

### 4. ウィンドウフォーカス管理
- ウィンドウがキーになったときに、適切に`becomeFirstResponder`が呼ばれるようにする
- `ToolbarPanel`や`ThumbnailsListPanel`相当の機能が必要

## 次のステップ

1. `isKeyView`の管理を完全に修正する
2. `onSyncNotification`を完全に再実装する
3. ウィンドウフォーカス管理を完全に実装する
4. テストして動作確認する

