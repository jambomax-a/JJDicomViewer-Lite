# sync:メソッドの完全1行ずつ解析

## DCMView.m:6916-7270 の完全解析

### 行6916-6919: メソッド開始とgDontListenToSyncMessageチェック
```objective-c
-(void) sync:(NSNotification*)note
{
    if( gDontListenToSyncMessage)
        return;
```
- `gDontListenToSyncMessage`がtrueの場合は即座にreturn

### 行6923: superviewチェックとis2DViewerチェック
```objective-c
if( ![[[note object] superview] isEqual:[self superview]] && [self is2DViewer])
{
```
- `[note object]`は送信側のDCMView
- `[[note object] superview]`と`[self superview]`が異なる場合のみ処理
- `[self is2DViewer]`がYESの場合のみ処理

### 行6925-6926: prevImageとnewImageの初期化
```objective-c
int prevImage = curImage;
int newImage = curImage;
```
- `prevImage`は現在の画像インデックス
- `newImage`は同期後の画像インデックス（初期値は`curImage`）

### 行6928-6929: windowWillCloseチェック
```objective-c
if( [[self windowController] windowWillClose])
    return;
```
- ウィンドウがクローズ中の場合は即座にreturn

### 行6931-6932: avoidRecursiveSyncチェック
```objective-c
if( avoidRecursiveSync > 1) return; // Keep this number, to have cross reference correctly displayed
avoidRecursiveSync++;
```
- `avoidRecursiveSync > 1`の場合は即座にreturn
- そうでない場合は`avoidRecursiveSync++`

### 行6934: メイン条件チェック
```objective-c
if( [note object] != self && isKeyView == YES && matrix == 0 && newImage > -1)
{
```
- `[note object] != self`: 送信側が自分自身でない
- `isKeyView == YES`: **受信側**がキービューである
- `matrix == 0`: Image Tilingが無効（`imageRows == 1 && imageColumns == 1`）
- `newImage > -1`: 有効な画像インデックス

**重要な発見**: この条件は**受信側**がキービューであることを要求している！

### 行6936: instructionsの取得
```objective-c
NSDictionary *instructions = [note userInfo];
```

### 行6938-6945: instructionsからの値の取得
```objective-c
int			diff = [[instructions valueForKey: @"Direction"] intValue];
int			pos = [[instructions valueForKey: @"Pos"] intValue];
float		loc = [[instructions valueForKey: @"Location"] floatValue];
NSString	*oStudyId = [instructions valueForKey: @"studyID"];
NSString	*oFrameofReferenceUID = [instructions valueForKey: @"frameofReferenceUID"];
DCMPix		*oPix = [instructions valueForKey: @"DCMPix"];
DCMPix		*oPix2 = [instructions valueForKey: @"DCMPix2"];
DCMView		*otherView = [instructions valueForKey: @"view"];
```

### 行6946-6948: 変数の初期化
```objective-c
float		destPoint3D[ 3];
BOOL		point3D = NO;
BOOL		same3DReferenceWorld = NO;
```

### 行6950-6954: blendingViewチェック
```objective-c
if( otherView == blendingView || self == [otherView blendingView])
{
    syncOnLocationImpossible = NO;
    [otherView setSyncOnLocationImpossible: NO];
}
```

### 行6956-6961: offsetsyncチェック
```objective-c
if( [instructions valueForKey: @"offsetsync"] == nil)
{
    NSLog(@"***** err offsetsync");
    avoidRecursiveSync--;
    return;
}
```
- `offsetsync`がnilの場合は`avoidRecursiveSync--`してreturn

### 行6963-6968: viewチェック
```objective-c
if( [instructions valueForKey: @"view"] == nil)
{
    NSLog(@"****** err view");
    avoidRecursiveSync--;
    return;
}
```
- `view`がnilの場合は`avoidRecursiveSync--`してreturn

### 行6970-6977: point3Dチェック
```objective-c
if( [instructions valueForKey: @"point3DX"])
{
    destPoint3D[ 0] = [[instructions valueForKey: @"point3DX"] floatValue];
    destPoint3D[ 1] = [[instructions valueForKey: @"point3DY"] floatValue];
    destPoint3D[ 2] = [[instructions valueForKey: @"point3DZ"] floatValue];
    
    point3D = YES;
}
```

### 行6979-6990: same3DReferenceWorldの判定
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
- `newImage`の画像の`StudyInstanceUID`と`oStudyId`を比較
- 同じ場合、`FrameOfReferenceUID`もチェック（`UseFrameofReferenceUID`が有効な場合）

### 行6992-6995: registeredViewerの判定
```objective-c
BOOL registeredViewer = NO;

if( [[self windowController] registeredViewer] == [otherView windowController] || [[otherView windowController] registeredViewer] == [self windowController])
    registeredViewer = YES;
```

### 行6997: 同期条件チェック
```objective-c
if( same3DReferenceWorld || registeredViewer || [[NSUserDefaults standardUserDefaults] boolForKey:@"SAMESTUDY"] == NO || syncSeriesIndex != -1)
{
```
- `same3DReferenceWorld`: 同じ3D参照世界
- `registeredViewer`: 登録されたビューア
- `SAMESTUDY == NO`: 異なるスタディ間でも同期する設定
- `syncSeriesIndex != -1`: 手動同期

### 行6999-7032: point3D処理
```objective-c
if( same3DReferenceWorld || registeredViewer)
{
    // Double-Click -> find the nearest point on our plane, go to this plane and draw the intersection!
    if( point3D)
    {
        // ... point3D処理 ...
    }
    else
    {
        if( slicePoint3D[ 0] != HUGE_VALF)
        {
            slicePoint3D[ 0] = HUGE_VALF;
            [self setNeedsDisplay:YES];
        }
    }
}
```

### 行7034-7042: syncroABS処理
```objective-c
// Absolute Vodka
if( syncro == syncroABS && point3D == NO && syncSeriesIndex == -1)
{
    if( flippedData) newImage = (long)[dcmPixList count] -1 -pos;
    else newImage = pos;
    
    if( newImage >= [dcmPixList count]) newImage = [dcmPixList count] - 1;
    if( newImage < 0) newImage = 0;
}
```

### 行7044-7056: syncroRatio処理
```objective-c
// Absolute Ratio
if( syncro == syncroRatio && point3D == NO && syncSeriesIndex == -1)
{
    float ratio = (float) pos / (float) [[otherView dcmPixList] count];
    
    int ratioPos = round( ratio * (float) [dcmPixList count]);
    
    if( flippedData) newImage = (long)[dcmPixList count] -1 -ratioPos;
    else newImage = ratioPos;
    
    if( newImage >= [dcmPixList count]) newImage = [dcmPixList count] - 1;
    if( newImage < 0) newImage = 0;
}
```

### 行7058-7180: syncroLOC処理
```objective-c
// Based on Location
if( (syncro == syncroLOC && point3D == NO) || syncSeriesIndex != -1)
{
    // ... 複雑な処理 ...
}
```

### 行7182-7190: syncroREL処理
```objective-c
// Relative
if( syncro == syncroREL && point3D == NO && syncSeriesIndex == -1)
{
    if( flippedData) newImage -= diff;
    else newImage += diff;
    
    if( newImage < 0) newImage += [dcmPixList count];
    if( newImage >= [dcmPixList count]) newImage -= [dcmPixList count];
}
```

### 行7192-7207: 画像更新処理
```objective-c
// Relatif
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

### 行7209-7243: 交線表示処理
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

### 行7245-7256: 同期条件を満たさない場合の交線クリア
```objective-c
else
{
    if( sliceFromTo[ 0][ 0] != HUGE_VALF && (sliceVector[ 0] != 0 || sliceVector[ 1] != 0  || sliceVector[ 2] != 0))
    {
        sliceFromTo[ 0][ 0] = HUGE_VALF;
        sliceFromTo2[ 0][ 0] = HUGE_VALF;
        sliceFromToS[ 0][ 0] = HUGE_VALF;
        sliceFromToE[ 0][ 0] = HUGE_VALF;
        sliceVector[0] = sliceVector[1] = sliceVector[2] = 0;
        [self setNeedsDisplay:YES];
    }
}
```

### 行7262-7263: blendingViewの同期
```objective-c
if( blendingView && [note object] != blendingView)
    [blendingView sync: [NSNotification notificationWithName: OsirixSyncNotification object: self userInfo: [self syncMessage: 0]]];
```

### 行7265: avoidRecursiveSyncのデクリメント
```objective-c
avoidRecursiveSync --;
```

## 重要な発見

### `isKeyView == YES`の条件の意味
- 行6934の条件`isKeyView == YES`は、**受信側**がキービューであることを要求している
- しかし、`sendSyncMessage:`の条件も`isKeyView`を要求している
- これは矛盾しているように見えるが、実際には：
  - 送信側がキービューでないと送信しない
  - 受信側がキービューでないと受信しない

### 解決策
- `computeMagnifyLens:`で`isKeyView == NO`の場合に`makeFirstResponder`を呼び出す
- マウスがビューに入った時点で自動的に`isKeyView = YES`が設定される
- これにより、マウスがビューに入った時点で同期が機能する


