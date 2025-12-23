# HOROS-20240407 同期システム完全リファレンス

## sync:メソッドの完全な構造（DCMView.m:6916-7265）

### 1. 初期チェック（6916-6934）
```objective-c
-(void) sync:(NSNotification*)note
{
    if( gDontListenToSyncMessage) return;
    
    if( ![[[note object] superview] isEqual:[self superview]] && [self is2DViewer])
    {
        int prevImage = curImage;
        int newImage = curImage;
        
        if( [[self windowController] windowWillClose]) return;
        
        if( avoidRecursiveSync > 1) return; // Keep this number, to have cross reference correctly displayed
        avoidRecursiveSync++;
        
        if( [note object] != self && isKeyView == YES && matrix == 0 && newImage > -1)
        {
            // 同期処理
        }
    }
}
```

**重要な条件**:
- `superview`が異なる
- `is2DViewer`である
- `windowWillClose == NO`
- `avoidRecursiveSync <= 1`（>1の場合はreturn）
- `[note object] != self`
- `isKeyView == YES`（受信側がキービューである必要がある）
- `matrix == 0`（Image Tilingが無効）
- `newImage > -1`

### 2. 同期条件（6997）
```objective-c
if( same3DReferenceWorld || registeredViewer || [[NSUserDefaults standardUserDefaults] boolForKey:@"SAMESTUDY"] == NO || syncSeriesIndex != -1)
```

### 3. syncroLOC実装（7059-7180）

#### 3.1 条件
```objective-c
if( (syncro == syncroLOC && point3D == NO) || syncSeriesIndex != -1)
```

#### 3.2 volumicSeries == YES && [otherView volumicSeries] == YES
```objective-c
if( volumicSeries == YES && [otherView volumicSeries] == YES)
{
    float orientA[9], orientB[9];
    [self.curDCM orientation:orientA];
    [otherView.curDCM orientation:orientB];
    
    float planeTolerance = [[NSUserDefaults standardUserDefaults] floatForKey: @"PARALLELPLANETOLERANCE-Sync"];
    if( syncSeriesIndex != -1) // Manual Sync !
        planeTolerance = 0.78; // 0.78 is about 45 degrees
    
    if( [DCMView angleBetweenVector: orientA+6 andVector:orientB+6] < planeTolerance)
    {
        // 平面が平行な場合の処理
    }
}
```

**重要な点**:
- `volumicSeries == YES`かつ`[otherView volumicSeries] == YES`の場合のみ
- `angleBetweenVector`で平面の角度をチェック
- `planeTolerance`はデフォルト値または手動同期時は0.78（約45度）

#### 3.3 スライス位置の計算
```objective-c
BOOL noSlicePosition = NO, everythingLoaded = YES;
int index = -1, i;
float smallestdiff = -1, fdiff, slicePosition;

if( [[self windowController] isEverythingLoaded] && [[otherView windowController] isEverythingLoaded] && (syncSeriesIndex == -1 || [otherView syncSeriesIndex] == -1))
{
    // findPlaneForPointを使用
    float centerPix[ 3];
    [oPix convertPixX: oPix.pwidth/2 pixY: oPix.pheight/2 toDICOMCoords: centerPix];
    float oPixOrientation[9]; [oPix orientation:oPixOrientation];
    index = [self findPlaneForPoint: centerPix preferParallelTo:oPixOrientation localPoint: nil distanceWithPlane: &smallestdiff];
}
else
{
    // sliceLocation差分で最小を探す
    for( i = 0; i < [dcmFilesList count]; i++)
    {
        everythingLoaded = [[dcmPixList objectAtIndex: i] isLoaded];
        if( everythingLoaded)
            slicePosition = [(DCMPix*)[dcmPixList objectAtIndex: i] sliceLocation];
        else
            slicePosition = [[[dcmFilesList objectAtIndex: i] valueForKey:@"sliceLocation"] floatValue];
        
        fdiff = slicePosition - loc;
        
        if( registeredViewer == NO)
        {
            // Manual sync
            if( same3DReferenceWorld == NO)
            {
                if( [otherView syncSeriesIndex] != -1 && syncSeriesIndex != -1)
                {
                    slicePosition -= (syncRelativeDiff - otherView.syncRelativeDiff);
                    fdiff = slicePosition - loc;
                }
                else if( [[NSUserDefaults standardUserDefaults] boolForKey:@"SAMESTUDY"]) noSlicePosition = YES;
            }
        }
        
        if( fdiff < 0) fdiff = -fdiff;
        
        if( fdiff < smallestdiff || smallestdiff == -1)
        {
            smallestdiff = fdiff;
            index = i;
        }
    }
}
```

**重要な点**:
- `isEverythingLoaded`かつ`syncSeriesIndex == -1`の場合は`findPlaneForPoint`を使用
- それ以外は`sliceLocation`差分で最小を探す
- 手動同期時（`syncSeriesIndex != -1`）は`syncRelativeDiff`で補正
- `same3DReferenceWorld == NO`かつ`SAMESTUDY == YES`の場合は`noSlicePosition = YES`

#### 3.4 スライス距離チェック
```objective-c
if( noSlicePosition == NO)
{
    if( index >= 0)
        newImage = index;
    
    if( [dcmPixList count] > 1)
    {
        float sliceDistance;
        if( [[dcmPixList objectAtIndex: 1] isLoaded] && [[dcmPixList objectAtIndex: 0] isLoaded]) everythingLoaded = YES;
        else everythingLoaded = NO;
        
        if( everythingLoaded) sliceDistance = fabs( [(DCMPix*)[dcmPixList objectAtIndex: 1] sliceLocation] - [(DCMPix*)[dcmPixList objectAtIndex: 0] sliceLocation]);
        else sliceDistance = fabs( [[[dcmFilesList objectAtIndex: 1] valueForKey:@"sliceLocation"] floatValue] - [[[dcmFilesList objectAtIndex: 0] valueForKey:@"sliceLocation"] floatValue]);
        
        if( fabs( smallestdiff) > sliceDistance * 2)
        {
            if( otherView == blendingView || self == [otherView blendingView])
            {
                syncOnLocationImpossible = YES;
                [otherView setSyncOnLocationImpossible: YES];
            }
        }
    }
    
    if( newImage >= [dcmFilesList count]) newImage = (long)[dcmFilesList count]-1;
    if( newImage < 0) newImage = 0;
}
```

#### 3.5 volumicSeries == NO && [otherView volumicSeries] == NO
```objective-c
else if( volumicSeries == NO && [otherView volumicSeries] == NO)	// For example time or functional series
{
    if( [[NSUserDefaults standardUserDefaults] integerForKey: @"DefaultModeForNonVolumicSeries"] == syncroRatio)
    {
        float ratio = (float) pos / (float) [[otherView dcmPixList] count];
        int ratioPos = round( ratio * (float) [dcmPixList count]);
        if( flippedData) newImage = (long)[dcmPixList count] -1 -ratioPos;
        else newImage = ratioPos;
    }
    else if( [[NSUserDefaults standardUserDefaults] integerForKey: @"DefaultModeForNonVolumicSeries"] == syncroABS)
    {
        if( flippedData) newImage = (long)[dcmPixList count] -1 -pos;
        else newImage = pos;
    }
    
    if( newImage >= [dcmPixList count]) newImage = [dcmPixList count] - 1;
    if( newImage < 0) newImage = 0;
}
```

### 4. 画像更新条件（7196-7207）
```objective-c
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

**重要な点**:
- `newImage != prevImage`の場合のみ
- `avoidRecursiveSync <= 1`の場合のみ
- `(selfViewer != frontMostViewer && otherViewer == frontMostViewer) || otherViewer.timer`の場合のみ画像更新
- 同期モードに関係なく同じ条件

### 5. 交線表示条件（7209-7243）
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
            // 交線をリセット
        }
    }
    else
    {
        // 交線をリセット
    }
}
```

**重要な点**:
- `same3DReferenceWorld || registeredViewer`の場合のみ
- `(selfViewer != frontMostViewer && otherViewer == frontMostViewer) || FullScreenON`の場合のみ交線表示
- 条件を満たさない場合は交線をリセット

