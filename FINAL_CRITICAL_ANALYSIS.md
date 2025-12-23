# 最終的な重要な分析：同期ロジックの根本的な問題

## 重大な発見

### 1. `newImageViewisKey:`の通知登録箇所が見つからない
- `DCMView.m:12810-12814`に`newImageViewisKey:`メソッドが定義されている
- `becomeFirstResponder`が`OsirixDCMViewDidBecomeFirstResponderNotification`を送信している（DCMView.m:12605）
- しかし、`newImageViewisKey:`がこの通知を監視している登録箇所が見つからない
- **可能性**: `newImageViewisKey:`は通知を監視していない、または別の方法で呼び出されている

### 2. `isKeyView`の管理が不完全
- `becomeFirstResponder`で`isKeyView = YES`を設定（DCMView.m:12576）
- `resignFirstResponder`で`isKeyView = NO`を設定（DCMView.m:12792）
- `newImageViewisKey:`で他のビューがキーになったら`isKeyView = NO`を設定（DCMView.m:12812-12813）
- しかし、この通知の登録箇所が見つからない

### 3. 同期ロジックの根本的な問題
- `sync:`メソッドの条件`isKeyView == YES`（DCMView.m:6934）は、**受信側**がキービューである必要がある
- `sendSyncMessage:`の条件`isKeyView`（DCMView.m:6725）は、**送信側**がキービューである必要がある
- この2つの条件が矛盾しているように見えるが、実際には：
  - 送信側がキービューでないと送信しない
  - 受信側がキービューでないと処理しない
  - つまり、**両方がキービューである必要はない**

### 4. `avoidRecursiveSync`の管理
- `sync:`の開始時にインクリメント（DCMView.m:6932）
- 早期リターン時にデクリメント（DCMView.m:6959, 6966）
- `sync:`の終了時にデクリメント（DCMView.m:7265）
- `blendingView`への再帰呼び出しの前後（DCMView.m:7263）

### 5. `numberOf2DViewer`の管理
- `viewerControllerInit`でインクリメント（ViewerController.m:20770）
- `dealloc`でデクリメント（ViewerController.m:3261）
- `sendSyncMessage:`の条件で使用される（DCMView.m:6725）

## 見落としている可能性のある重要な点

### 1. `newImageViewisKey:`の呼び出し方法
- 通知を監視していない可能性
- 別の方法で呼び出されている可能性
- または、実装されていない可能性

### 2. `isKeyView`の初期値
- `isKeyView`の初期値が何か
- 最初に表示されるビューが自動的にキービューになるのか

### 3. `sendSyncMessage:`の呼び出しタイミング
- `becomeKeyWindow`で呼ばれる（DCMView.m:12555-12572）
- `becomeMainWindow`で呼ばれる（DCMView.m:12539-12553）
- `resignFirstResponder`で呼ばれる（DCMView.m:12794）
- `scrollWheel:`で呼ばれる（DCMView.m:4847-5046）
- `setIndex:`や`setIndexWithReset:`の最後で呼ばれる可能性

### 4. `sync:`メソッドの条件の解釈
- `isKeyView == YES`は受信側がキービューである必要がある
- しかし、送信側がキービューでないと送信しない
- つまり、**送信側がキービューで、受信側もキービューである必要がある**という解釈は間違い

## 正しい理解

### `sync:`メソッドの条件（DCMView.m:6934）
```objective-c
if( [note object] != self && isKeyView == YES && matrix == 0 && newImage > -1)
```
- `[note object] != self`: 送信側が自分自身でない
- `isKeyView == YES`: **受信側**がキービューである
- `matrix == 0`: Image Tilingが無効
- `newImage > -1`: 有効な画像インデックス

### `sendSyncMessage:`の条件（DCMView.m:6725）
```objective-c
if( [ViewerController numberOf2DViewer] > 1 && isKeyView && [self is2DViewer])
```
- `numberOf2DViewer > 1`: 2Dビューアーが2つ以上
- `isKeyView`: **送信側**がキービューである
- `is2DViewer()`: 2Dビューアーである

### 正しい同期フロー
1. 送信側がキービューで、`sendSyncMessage:`を呼ぶ
2. すべてのビューアーが`sync:`メソッドで通知を受信
3. 受信側がキービューでない場合は、処理をスキップ
4. 受信側がキービューである場合のみ、同期処理を実行

## 次のステップ

1. `newImageViewisKey:`の呼び出し方法を完全に理解する
2. `isKeyView`の管理を完全に理解する
3. `sendSyncMessage:`のすべての呼び出し箇所を確認する
4. `sync:`メソッドの条件を完全に理解する
5. 同期ロジックを完全に再実装する

