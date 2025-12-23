# DCMView.m 徹底解析計画

## 現状の問題
1. CT画像の同期が機能していない
2. MRのSliceIDの交線が全く仕様通りではない
3. ログを見ても仕様の間違いに気づけない
4. カスタムロジックが混入している可能性が高い
5. 最初のBrowserウィンドウの設計からHOROS-20240407に準拠していなかった

## 解析方針

### 1. 同期関連メソッドの完全洗い出し
- `sync:` メソッド（DCMView.m:6916-7270）
- `sendSyncMessage:` メソッド（DCMView.m:6721-6741）
- `syncMessage:` メソッド（DCMView.m:6669-6719）
- `computeSlice:oPix:oPix2:` メソッド（DCMView.m:6822-6902）
- `setIndex:` メソッド（画像遷移時の処理）
- `becomeFirstResponder` / `resignFirstResponder`（isKeyViewの設定）
- `becomeMainWindow`（isKeyViewの設定）

### 2. 同期フローの完全追跡
1. ユーザー操作（マウスホイール、キーボード、メニュー）
2. `setIndex:` の呼び出し
3. `sendSyncMessage:` の呼び出し
4. `OsirixSyncNotification` の送信
5. `sync:` メソッドの受信
6. `newImage` の計算
7. 画像更新の判定
8. 交線計算の判定

### 3. 重要な変数・フラグの追跡
- `isKeyView` - いつ、どこで設定されるか
- `frontMostViewer` - いつ、どこで設定されるか
- `same3DReferenceWorld` - どのように判定されるか
- `registeredViewer` - どのように判定されるか
- `syncSeriesIndex` - どのように設定されるか
- `avoidRecursiveSync` - どのように管理されるか

### 4. 同期モード別の動作確認
- `syncroLOC` (SLICE_POSITION) - 位置ベース同期
- `syncroABS` (SLICE_ID_ABSOLUTE) - 絶対インデックス同期
- `syncroRatio` (SLICE_ID_RATIO) - 比率ベース同期
- `syncroREL` (SLICE_ID_RELATIVE) - 相対インデックス同期

### 5. 交線表示の条件確認
- `same3DReferenceWorld || registeredViewer` の判定
- `(selfViewer != frontMostViewer && otherViewer == frontMostViewer) || [otherView.windowController FullScreenON]` の判定
- `computeSlice:oPix:oPix2:` の呼び出しタイミング
- `sliceFromTo`, `sliceFromTo2`, `sliceFromToS`, `sliceFromToE` の計算タイミング

## 解析手順

### Phase 1: メソッドの完全洗い出し
1. `DCMView.m` 全体を読み、同期関連メソッドを全てリストアップ
2. 各メソッドの呼び出し元を追跡
3. 各メソッドの呼び出し先を追跡

### Phase 2: フローの完全追跡
1. ユーザー操作から同期完了までの完全なフローを図示
2. 各ステップでの条件分岐を全て記録
3. 各ステップでの変数の状態を記録

### Phase 3: 実装との比較
1. 現在のJava実装とHOROS-20240407の実装を1行ずつ比較
2. 不一致箇所を全てリストアップ
3. カスタムロジックを全て特定

### Phase 4: 修正計画
1. 不一致箇所の修正計画を立案
2. カスタムロジックの削除計画を立案
3. 段階的な修正計画を立案

## 次のステップ
1. `DCMView.m` の同期関連メソッドを全て洗い出し
2. 各メソッドの完全な解析
3. フローの完全な追跡
4. 実装との比較と修正計画の立案


