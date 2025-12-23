# 同期ロジック完全再実装計画（最終版）

## 根本的な問題の認識

ユーザーの指摘通り、私は同期ロジックの根本的な理解が不足していました。以下を完全に把握する必要があります：

1. **通知の登録と送信の完全な流れ**
2. **`isKeyView`の管理の完全な理解**
3. **`sendSyncMessage:`のすべての呼び出し箇所**
4. **`sync:`メソッドの条件の正確な解釈**
5. **`avoidRecursiveSync`の管理**
6. **`numberOf2DViewer`の管理**
7. **`newImageViewisKey:`の呼び出し方法**

## 完全再実装の手順

### ステップ1: HOROS-20240407の完全解析

1. **すべての通知の登録箇所を確認**
   - `DCMView.m:6530-6650`の通知登録を完全に確認
   - `newImageViewisKey:`の通知登録箇所を探す（見つからない場合は別の方法を確認）

2. **`isKeyView`の管理を完全に理解**
   - `becomeFirstResponder`での設定
   - `resignFirstResponder`での設定
   - `newImageViewisKey:`での設定
   - `computeMagnifyLens:`での設定（`makeFirstResponder`呼び出し）

3. **`sendSyncMessage:`のすべての呼び出し箇所を確認**
   - `becomeKeyWindow`
   - `becomeMainWindow`
   - `resignFirstResponder`
   - `scrollWheel:`
   - `setIndex:`や`setIndexWithReset:`の最後

4. **`sync:`メソッドの条件を完全に理解**
   - `isKeyView == YES`の意味（受信側がキービューである必要がある）
   - `sendSyncMessage:`の条件（送信側がキービューである必要がある）

5. **`avoidRecursiveSync`の管理を完全に理解**
   - インクリメント/デクリメントのタイミング
   - `blendingView`への再帰呼び出し

6. **`numberOf2DViewer`の管理を完全に理解**
   - インクリメント/デクリメントのタイミング
   - `getDisplayed2DViewers`の実装

### ステップ2: Java実装の完全削除と再実装

1. **`onSyncNotification`を完全削除**
2. **HOROS-20240407の`sync:`メソッドを完全写経**
3. **`sendSyncMessage:`を完全写経**
4. **`isKeyView`の管理を完全写経**
5. **`avoidRecursiveSync`の管理を完全写経**
6. **`numberOf2DViewer`の管理を完全写経**

### ステップ3: テストと検証

1. **同期が正しく動作することを確認**
2. **交線が正しく表示されることを確認**
3. **無限ループが発生しないことを確認**

## 重要な発見

### `newImageViewisKey:`の通知登録箇所が見つからない
- `DCMView.m:12810-12814`に`newImageViewisKey:`メソッドが定義されている
- `becomeFirstResponder`が`OsirixDCMViewDidBecomeFirstResponderNotification`を送信している
- しかし、`newImageViewisKey:`がこの通知を監視している登録箇所が見つからない

**可能性**:
1. `newImageViewisKey:`は通知を監視していない
2. 別の方法で呼び出されている
3. 実装されていない（未使用のコード）

### `sync:`メソッドの条件の正確な解釈
- `isKeyView == YES`は**受信側**がキービューである必要がある
- `sendSyncMessage:`の条件`isKeyView`は**送信側**がキービューである必要がある
- つまり、**送信側がキービューで、受信側もキービューである必要がある**という解釈は間違い

**正しい理解**:
- 送信側がキービューでないと送信しない
- 受信側がキービューでないと処理しない
- つまり、**両方がキービューである必要はない**

## 次のアクション

1. HOROS-20240407のソースを完全に再解析する
2. すべての通知の登録と送信の流れを完全に把握する
3. `isKeyView`の管理を完全に理解する
4. `onSyncNotification`を完全削除して再実装する

