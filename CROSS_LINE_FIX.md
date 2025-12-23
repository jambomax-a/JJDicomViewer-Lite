# 交線表示の修正

## 問題

1. MRのSliceIDで交線が表示されるようになったが、交線の仕様がめちゃくちゃ
2. 完全一致にすると複数画面全部遷移する

## HOROSの実装

### 同期条件（DCMView.m:6997）
```objective-c
if( same3DReferenceWorld || registeredViewer || [[NSUserDefaults standardUserDefaults] boolForKey:@"SAMESTUDY"] == NO || syncSeriesIndex != -1)
```
- `SAMESTUDY == YES`: 同じスタディ内でのみ同期
- `SAMESTUDY == NO`: 異なるスタディ間でも同期

### 画像更新条件（DCMView.m:7200）
```objective-c
if((selfViewer != frontMostViewer && otherViewer == frontMostViewer) || otherViewer.timer)
```
- 受信側がfrontMostでなく、送信側がfrontMostの場合に画像を更新

### 交線表示条件（DCMView.m:7209-7213）
```objective-c
if( same3DReferenceWorld || registeredViewer)
{
    if( (selfViewer != frontMostViewer && otherViewer == frontMostViewer) || [otherView.windowController FullScreenON])
    {
        if( same3DReferenceWorld || registeredViewer)
        {
            if( [self computeSlice: oPix :oPix2])
                [self setNeedsDisplay:YES];
        }
        ...
    }
}
```
- 交線は`same3DReferenceWorld || registeredViewer`の場合のみ表示
- さらに、`(selfViewer != frontMostViewer && otherViewer == frontMostViewer) || FullScreenON`の条件が必要

### 位置同期時のnoSlicePosition（DCMView.m:7116）
```objective-c
else if( [[NSUserDefaults standardUserDefaults] boolForKey:@"SAMESTUDY"]) noSlicePosition = YES;
```
- `registeredViewer == NO`かつ`same3DReferenceWorld == NO`かつ`SAMESTUDY == YES`の場合、`noSlicePosition = YES`になり、位置同期が無効になる

## 現在の問題

1. `same3DReferenceWorld`の判定が正しくない可能性
2. 交線表示条件が正しく実装されていない可能性
3. 「完全一致」の意味が不明確（SliceID同期モードのこと？）

## 修正案

1. `same3DReferenceWorld`の判定を確認・修正
2. 交線表示条件をHOROSに完全準拠させる
3. 位置同期時の`noSlicePosition`処理を実装

