# DCMView.m sync:メソッド完全解析

## メソッド構造（DCMView.m:6916-7270）

### 1. 初期チェック（DCMView.m:6918-6929）
```objective-c
if( gDontListenToSyncMessage)
    return;

if( ![[[note object] superview] isEqual:[self superview]] && [self is2DViewer])
{
    int prevImage = curImage;
    int newImage = curImage;
    
    if( [[self windowController] windowWillClose])
        return;
```

### 2. 再帰防止（DCMView.m:6931-6932）
```objective-c
if( avoidRecursiveSync > 1) return;
avoidRecursiveSync++;
```

### 3. メイン条件チェック（DCMView.m:6934）
```objective-c
if( [note object] != self && isKeyView == YES && matrix == 0 && newImage > -1)
```

**重要**: この条件は、**受信側のビューア**（`self`）が`isKeyView == YES`である必要があります。

### 4. 必須パラメータチェック（DCMView.m:6956-6968）
```objective-c
if( [instructions valueForKey: @"offsetsync"] == nil)
{
    NSLog(@"***** err offsetsync");
    avoidRecursiveSync--;
    return;
}

if( [instructions valueForKey: @"view"] == nil)
{
    NSLog(@"****** err view");
    avoidRecursiveSync--;
    return;
}
```

### 5. same3DReferenceWorldの判定（DCMView.m:6979-6990）
```objective-c
if( [oStudyId isEqualToString:[[dcmFilesList objectAtIndex: newImage] valueForKeyPath:@"series.study.studyInstanceUID"]])
{
    if( self.curDCM.frameofReferenceUID && oFrameofReferenceUID && [[NSUserDefaults standardUserDefaults] boolForKey: @"UseFrameofReferenceUID"])
    {
        if( oFrameofReferenceUID == nil || self.curDCM.frameofReferenceUID == nil || [oFrameofReferenceUID isEqualToString: self.curDCM.frameofReferenceUID])
            same3DReferenceWorld = YES;
        else
            NSLog( @"-- same studyInstanceUID, but different frameofReferenceUID : NO cross reference lines displayed:\r%@\r%@",oFrameofReferenceUID,self.curDCM.frameofReferenceUID);
    }
    else
        same3DReferenceWorld = YES;
}
```

**重要**: `newImage`の画像から`StudyInstanceUID`を取得し、`self.curDCM.frameofReferenceUID`（現在表示中の画像）と比較します。

### 6. 同期条件の判定（DCMView.m:6997）
```objective-c
if( same3DReferenceWorld || registeredViewer || [[NSUserDefaults standardUserDefaults] boolForKey:@"SAMESTUDY"] == NO || syncSeriesIndex != -1)
```

### 7. 同期モード別のnewImage計算（DCMView.m:7034-7190）
- `syncroABS` (DCMView.m:7035-7042)
- `syncroRatio` (DCMView.m:7045-7056)
- `syncroLOC` (DCMView.m:7059-7180)
- `syncroREL` (DCMView.m:7183-7190)

### 8. 画像更新処理（DCMView.m:7192-7207）
```objective-c
ViewerController *frontMostViewer = [ViewerController frontMostDisplayed2DViewer];
ViewerController *selfViewer = self.window.windowController;
ViewerController *otherViewer = otherView.window.windowController;
if( newImage != prevImage)
{
    if( avoidRecursiveSync <= 1)
    {
        if((selfViewer != frontMostViewer && otherViewer == frontMostViewer) || otherViewer.timer)
        {
            if( listType == 'i') [self setIndex:newImage];
            else [self setIndexWithReset:newImage :YES];
            [[self windowController] adjustSlider];
        }
    }
}
```

### 9. 交線表示処理（DCMView.m:7209-7243）
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
        else
        {
            // 交線をクリア
        }
    }
    else
    {
        // 交線をクリア
    }
}
```

## 重要な発見

### isKeyViewの条件の矛盾
- `sendSyncMessage:`の条件：送信側が`isKeyView == YES`でないと送信しない（DCMView.m:6725）
- `sync:`の条件：受信側が`isKeyView == YES`でないと受信しない（DCMView.m:6934）

これは矛盾しているように見えますが、実際には：
- 送信側がキービューでないと送信しない
- 受信側がキービューでないと受信しない

通常、1つのビューアだけがキービューであるため、同期が機能しないはずです。

### しかし、HOROS-20240407では同期が機能している
これは、何か見落としがあることを示しています。

### 可能性のある解決策
1. `isKeyView`の設定タイミングが異なる可能性
2. `becomeFirstResponder`/`resignFirstResponder`の呼び出しタイミングが異なる可能性
3. `newImageViewisKey:`の通知が正しく処理されていない可能性
4. 現在のJava実装で`isKeyView`の管理が正しく実装されていない可能性

## 次のステップ
1. `becomeFirstResponder`/`resignFirstResponder`の呼び出しタイミングの確認
2. `newImageViewisKey:`の通知の登録と処理の確認
3. `isKeyView`の設定タイミングの完全な追跡
4. 現在のJava実装との比較


