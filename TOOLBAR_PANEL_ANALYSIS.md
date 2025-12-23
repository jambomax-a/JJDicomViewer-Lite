# ToolbarPanelとThumbnailsListPanelの完全解析

## 重要な発見

### 1. `ToolbarPanel`がウィンドウフォーカスを監視
- `NSWindowDidBecomeMainNotification`を監視（ToolbarPanel.m:125）
- `NSWindowDidBecomeKeyNotification`を監視（ToolbarPanel.m:126）
- `windowDidBecomeKey:`で、対応するビューワーウィンドウを前面に表示（ToolbarPanel.m:153-168）
- `windowDidBecomeMain:`で、対応するビューワーウィンドウを前面に表示（ToolbarPanel.m:170-185）

### 2. `ThumbnailsListPanel`もウィンドウフォーカスを監視
- `NSWindowDidBecomeMainNotification`を監視（ThumbnailsListPanel.m:119）
- `NSWindowDidBecomeKeyNotification`を監視（ThumbnailsListPanel.m:121）
- `windowDidBecomeKey:`で、対応するビューワーウィンドウを前面に表示（ThumbnailsListPanel.m:151-168）
- `windowDidBecomeMain:`で、対応するビューワーウィンドウを前面に表示（ThumbnailsListPanel.m:179-200）

### 3. しかし、`newImageViewisKey:`の通知登録箇所が見つからない
- `DCMView.m:12810-12814`に`newImageViewisKey:`メソッドが定義されている
- `becomeFirstResponder`が`OsirixDCMViewDidBecomeFirstResponderNotification`を送信している
- しかし、`newImageViewisKey:`がこの通知を監視している登録箇所が見つからない

**可能性**:
1. `newImageViewisKey:`は通知を監視していない（未使用のコード）
2. 別の方法で呼び出されている
3. 実装されていない

### 4. ウィンドウフォーカス管理の仕組み
- `ToolbarPanel`と`ThumbnailsListPanel`は、ウィンドウがメイン/キーになったときに、対応するビューワーウィンドウを前面に表示
- これは、ウィンドウのフォーカス管理に関係している
- しかし、`isKeyView`の管理は`becomeFirstResponder`/`resignFirstResponder`で行われる

### 5. `isKeyView`の管理
- `becomeFirstResponder`で`isKeyView = YES`を設定（DCMView.m:12576）
- `resignFirstResponder`で`isKeyView = NO`を設定（DCMView.m:12792）
- `newImageViewisKey:`で他のビューがキーになったら`isKeyView = NO`を設定（DCMView.m:12812-12813）
- しかし、この通知の登録箇所が見つからない

## 重要なポイント

### ブラウザーウインドウとビューワーウインドウが独立している
- `BrowserController`と`ViewerController`は独立したウィンドウ
- `frontMostDisplayed2DViewer`は`NSApp orderedWindows`から取得
- ウィンドウのフォーカス管理が`isKeyView`の管理に関係している可能性

### フローティングウインドウやサムネイルウインドウのロジック
- `ThumbnailsListPanel`がウィンドウフォーカスを監視
- `ToolbarPanel`もウィンドウフォーカスを監視
- ウィンドウがメイン/キーになったときに、対応するビューワーウィンドウを前面に表示
- しかし、`isKeyView`の管理はしていない

### `newImageViewisKey:`の謎
- `newImageViewisKey:`の通知登録箇所が見つからない
- もしかしたら、実装されていないか、別の方法で実装されている可能性
- または、`becomeFirstResponder`/`resignFirstResponder`だけで`isKeyView`の管理が十分な可能性

## 次のステップ

1. `newImageViewisKey:`の通知登録箇所を探す（見つからない場合は未使用のコードと判断）
2. `becomeFirstResponder`/`resignFirstResponder`の呼び出し箇所を完全に確認
3. ウィンドウフォーカスと`isKeyView`の関係を完全に理解する
4. 同期ロジックを完全に再実装する

