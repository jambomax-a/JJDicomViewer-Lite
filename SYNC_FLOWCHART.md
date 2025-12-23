# HOROS-20240407 同期システム フローチャート

## 1. 同期モードの設定フロー

```
ユーザーが同期ボタンをクリック
  ↓
SyncSeries: (ViewerController.m:16537)
  ↓
if (SyncButtonBehaviorIsBetweenStudies)
  → SYNCSERIES = !SYNCSERIES
  → OsirixSyncSeriesNotification を送信
else
  → if ([imageView syncro] == syncroOFF)
      → 通常クリック: setSyncro: syncroLOC
      → Alt+クリック: setSyncro: syncroREL
    else
      → setSyncro: syncroOFF
  → becomeMainWindow を呼び出し
```

## 2. 同期通知の送信フロー

```
becomeMainWindow (DCMView.m:12539)
  ↓
sendSyncMessage: 0 (DCMView.m:12550)
  ↓
if (numberOf2DViewer > 1 && isKeyView && is2DViewer)
  ↓
syncMessage: 0 を呼び出し
  ↓
OsirixSyncNotification を送信
```

## 3. 同期通知の受信フロー

```
OsirixSyncNotification を受信
  ↓
sync: (DCMView.m:6916)
  ↓
if (![[[note object] superview] isEqual:[self superview]] && [self is2DViewer])
  ↓
if (avoidRecursiveSync > 1) return
avoidRecursiveSync++
  ↓
if ([note object] != self && isKeyView == YES && matrix == 0 && newImage > -1)
  ↓
同期条件をチェック (DCMView.m:6997)
  → same3DReferenceWorld || registeredViewer || SAMESTUDY == NO || syncSeriesIndex != -1
  ↓
同期モードに応じて newImage を計算
  → syncroLOC: 位置情報で計算
  → syncroABS: newImage = pos
  → syncroRatio: ratio = pos / otherView.count, newImage = round(ratio * self.count)
  → syncroREL: newImage = prevImage + diff
  ↓
画像更新条件をチェック (DCMView.m:7200)
  → (selfViewer != frontMostViewer && otherViewer == frontMostViewer) || otherViewer.timer
  ↓
if (条件を満たす)
  → setIndex: newImage または setIndexWithReset: newImage :YES
  ↓
交線表示条件をチェック (DCMView.m:7211)
  → (selfViewer != frontMostViewer && otherViewer == frontMostViewer) || FullScreenON
  ↓
if (条件を満たす)
  → computeSlice: oPix :oPix2
  → setNeedsDisplay: YES
```

## 4. 重要な条件の関係性

### 4.1 isKeyView の設定
```
becomeFirstResponder (DCMView.m:12574)
  → isKeyView = YES
  → sendSyncMessage: 0

resignFirstResponder (DCMView.m:12790)
  → isKeyView = NO
  → sendSyncMessage: 0

newImageViewisKey (DCMView.m:12810)
  → if ([note object] != self) isKeyView = NO
```

### 4.2 frontMostViewer の取得
```
frontMostDisplayed2DViewer (ViewerController.m:486)
  → [NSApp orderedWindows] から ViewerController を探す
  → 最初に見つかった ViewerController を返す
```

### 4.3 同期モードの共有
```
syncro はグローバル変数 (DCMView.m:86)
  → すべてのビューアーで共有される
  → setSyncro: で変更すると、すべてのビューアーに影響
```

## 5. 問題点の特定

現在の実装では、以下の点が正しく実装されていない可能性があります：

1. **同期モードの共有**: `syncro`はグローバル変数だが、現在の実装では各ビューアーで独立している
2. **isKeyView の設定**: `becomeFirstResponder`と`resignFirstResponder`が正しく呼ばれていない
3. **frontMostViewer の取得**: `frontMostDisplayed2DViewer`の実装が正しくない
4. **同期条件**: `same3DReferenceWorld`の判定が正しく行われていない

