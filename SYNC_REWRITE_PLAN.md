# 同期ロジック完全写経計画

## 現状の問題点
1. 同期条件の判定がHOROS-20240407と完全一致していない
2. 画像更新の条件がHOROS-20240407と完全一致していない
3. 交線表示の条件がHOROS-20240407と完全一致していない
4. `same3DReferenceWorld`の判定がHOROS-20240407と完全一致していない

## HOROS-20240407の`sync:`メソッドの構造（DCMView.m:6916-7270）

### 1. 初期チェック（DCMView.m:6918-6932）
- `gDontListenToSyncMessage`チェック
- `superview`チェック（Javaでは不要）
- `is2DViewer`チェック
- `windowWillClose`チェック
- `avoidRecursiveSync > 1`チェック
- `avoidRecursiveSync++`

### 2. メイン条件チェック（DCMView.m:6934）
- `[note object] != self && isKeyView == YES && matrix == 0 && newImage > -1`

### 3. 必須パラメータチェック（DCMView.m:6956-6968）
- `offsetsync`チェック（nullの場合は`avoidRecursiveSync--`してreturn）
- `view`チェック（nullの場合は`avoidRecursiveSync--`してreturn）

### 4. `same3DReferenceWorld`の判定（DCMView.m:6979-6990）
- `newImage`の画像から`StudyInstanceUID`を取得
- `self.curDCM.frameofReferenceUID`を使用（現在表示中の画像）

### 5. `registeredViewer`の判定（DCMView.m:6992-6995）

### 6. 同期条件の判定（DCMView.m:6997）
- `same3DReferenceWorld || registeredViewer || SAMESTUDY == NO || syncSeriesIndex != -1`
- 条件を満たさない場合は交線をクリアして終了（DCMView.m:7245-7256）

### 7. point3D処理（DCMView.m:6999-7032）
- `same3DReferenceWorld || registeredViewer`の場合のみ実行

### 8. 同期モード別の`newImage`計算（DCMView.m:7034-7190）
- `syncroABS`（DCMView.m:7035-7042）
- `syncroRatio`（DCMView.m:7045-7056）
- `syncroLOC`（DCMView.m:7059-7180）
- `syncroREL`（DCMView.m:7183-7190）

### 9. 画像更新処理（DCMView.m:7192-7207）
- `newImage != prevImage`の場合のみ
- `avoidRecursiveSync <= 1`の場合のみ
- `(selfViewer != frontMostViewer && otherViewer == frontMostViewer) || otherViewer.timer`の場合のみ

### 10. 交線表示処理（DCMView.m:7209-7243）
- `same3DReferenceWorld || registeredViewer`の場合のみ
- `(selfViewer != frontMostViewer && otherViewer == frontMostViewer) || [otherView.windowController FullScreenON]`の場合のみ
- `same3DReferenceWorld || registeredViewer`の場合のみ`computeSlice`を呼ぶ

### 11. 後処理（DCMView.m:7262-7265）
- `blendingView`の処理（未実装）
- `avoidRecursiveSync--`

## 実装方針
1. `onSyncNotification`を完全削除
2. HOROS-20240407の`sync:`メソッドを1行ずつ完全写経
3. 条件チェックの順序を完全一致
4. 変数名とロジックを完全一致

