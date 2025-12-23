# HOROS-20240407 ThumbnailsListPanel 完全仕様書

## 目次

1. [概要](#概要)
2. [アーキテクチャ](#アーキテクチャ)
3. [データフローと機能仕様](#データフローと機能仕様)
4. [階層構造と展開/折りたたみ](#階層構造と展開折りたたみ)
5. [ソートと番号付け](#ソートと番号付け)
6. [セルの詳細仕様](#セルの詳細仕様)
   - [セルのレイアウト構造](#セルのレイアウト構造)
   - [サムネイルサイズとレイアウト修正](#サムネイルサイズとレイアウト修正)
7. [動作仕様詳細](#動作仕様詳細)
   - [ウィンドウの閉じるボタン](#ウィンドウの閉じるボタン)
   - [スクロール動作](#スクロール動作)
   - [左クリックと右クリックの動作](#左クリックと右クリックの動作)
   - [ビューワーの立ち上げ動作](#ビューワーの立ち上げ動作)
8. [Java Swingへの移植方針](#java-swingへの移植方針)

---

## 概要

HOROS-20240407のThumbnailsListPanelは、同じ患者の全スタディとシリーズを階層構造で表示するフローティングウィンドウです。スタディセルをクリックすることでシリーズの表示/非表示を切り替える展開/折りたたみ機能を実装しています。

**重要なポイント**:
- **各シリーズに対して1つのサムネイルセルしか作成されない**（全画像を表示するのではなく、シリーズの代表画像を1枚だけ表示）
- スタディは最新から過去の順に並ぶ（日付と時刻の降順でソート）
- スタディセルをクリックすると、そのスタディのシリーズセルが表示/非表示に切り替わる

---

## アーキテクチャ

### 1. コンポーネント構成

```
ThumbnailsListPanel (フローティングウィンドウ)
  └── previewMatrixScrollView (NSScrollView)
        └── O2ViewerThumbnailsMatrix (NSMatrixのサブクラス、1列のみサポート)
              └── ThumbnailCell (NSButtonCellのサブクラス)
                    ├── スタディセル (Study Cell)
                    └── シリーズセル (Series Cell)
```

### 2. ThumbnailsListPanelの役割

**ThumbnailsListPanel.m**:
- フローティングウィンドウとして機能
- `previewMatrixScrollView`を表示するだけのコンテナ
- スクリーンごとに1つのインスタンス（最大10スクリーン）
- 固定幅（`fixedWidth`メソッドで計算: 90, 110, 140px）
- 画面の左端に配置、全画面高さ
- **タイトルバーなし、閉じるボタンなし**（`setUndecorated(true)`相当）

**重要なポイント**:
- ThumbnailsListPanel自体はコンテンツを生成しない
- `setThumbnailsView:viewer:`で`previewMatrixScrollView`を受け取り、それを表示するだけ
- 実際のコンテンツは`ViewerController`の`buildMatrixPreview:`で生成される

### 3. O2ViewerThumbnailsMatrixの役割

**O2ViewerThumbnailsMatrix.mm**:
- NSMatrixのサブクラスだが、**1列のみサポート**（実際にはリスト）
- セルの配置は縦方向のみ
- セルの高さは可変（スタディセル: 120px、シリーズセル: 60px）
- セルの幅は固定（`thumbnailCellWidth`で計算）

**重要なポイント**:
- セルは`ThumbnailCell`（NSButtonCellのサブクラス）
- セルの種類は`representedObject`で判定:
  - `DicomStudy` → スタディセル
  - `DicomSeries` → シリーズセル

### 4. データ構造

```objective-c
O2ViewerThumbnailsMatrixRepresentedObject
  - object: DicomStudy または DicomSeries
  - children: NSArray (スタディの場合はシリーズの配列)
```

---

## データフローと機能仕様

### 1. buildMatrixPreviewメソッドの処理フロー

```
buildMatrixPreview: (BOOL) showSelected
  ↓
1. 現在表示中の画像からスタディを取得
   - curImage = [fileList[0] objectAtIndex:0]
   - study = [curImage valueForKeyPath:@"series.study"]
  ↓
2. 現在表示中のシリーズを取得（viewerSeries）
   - for (int i = 0; i < maxMovieIndex; i++)
     viewerSeries.addObject([[fileList[i] objectAtIndex:0] valueForKey:@"series"])
  ↓
3. 同じ患者の全スタディをデータベースから検索
   - searchString = [study valueForKey:@"patientUID"]
   - if (patientUIDが空または"0")
     searchString = [study valueForKey:@"name"]
   - predicate = NSPredicate("patientUID BEGINSWITH[cd] %@", searchString)
     または
     predicate = NSPredicate("name == %@", searchString)
   - studiesArray = [db objectsForEntity:db.studyEntity predicate:predicate]
   - studiesArray = [studiesArray sortedArrayUsingDescriptors: 
       [NSSortDescriptor sortDescriptorWithKey: @"date" ascending: NO]]
  ↓
4. 各スタディのシリーズを取得
   - for (id s in studiesArray)
     seriesArray.addObject([[BrowserController currentBrowser] childrenArray: s])
  ↓
5. 表示中のシリーズを取得（displayedSeries）
   - displayedSeries = [ViewerController getDisplayedSeries]
  ↓
6. セルを構築
   - previewMatrix.renewRows(i + studiesArray.count, columns: 1)
   - for (curStudy in studiesArray)
     - スタディセルを作成
     - if (curStudy.isHidden == NO)
       for (curSeries in series)
         - シリーズセルを作成（各シリーズに対して1つだけ）
```

### 2. スタディの取得

**ソース**: `buildMatrixPreview:` (ViewerController.m:4946-4982)

```objective-c
// 現在表示中の画像からスタディを取得
NSManagedObject *curImage = [fileList[0] objectAtIndex:0];
DicomStudy *study = [curImage valueForKeyPath:@"series.study"];

// 患者UIDまたは患者名で検索
NSString *searchString = [study valueForKey:@"patientUID"];
if ([searchString length] == 0 || [searchString isEqualToString:@"0"]) {
    searchString = [study valueForKey:@"name"];
    predicate = [NSPredicate predicateWithFormat: @"(name == %@)", searchString];
} else {
    predicate = [NSPredicate predicateWithFormat: @"(patientUID BEGINSWITH[cd] %@)", searchString];
}

// データベースからスタディを取得（日付の降順でソート）
studiesArray = [db objectsForEntity:db.studyEntity predicate:predicate];
studiesArray = [studiesArray sortedArrayUsingDescriptors: 
    [NSArray arrayWithObject: [NSSortDescriptor sortDescriptorWithKey: @"date" ascending: NO]]];
```

### 3. シリーズの取得

**ソース**: `buildMatrixPreview:` (ViewerController.m:5012-5014)

```objective-c
if ([s isKindOfClass: [DicomStudy class]]) {
    [seriesArray addObject: [[BrowserController currentBrowser] childrenArray: s]];
}
```

### 4. シリーズ情報の取得

**取得するデータ**:
- `name`: シリーズ名（DICOMタグ: SeriesDescription）
- `date`: シリーズ日時（DICOMタグ: SeriesDate + SeriesTime）
- `noFiles`: 画像ファイル数
- `rawNoFiles`: 生のファイル数
- `images`: 画像オブジェクトの配列
- `numberOfFrames`: フレーム数（動画の場合）

### 5. サムネイル画像の取得と生成

**処理の流れ**:
1. データベースからサムネイルを取得（`curSeries.thumbnail`）
2. 存在しない場合、シリーズの最初の画像から生成
3. `DCMPix.generateThumbnailImageWithWW:WL:`でサムネイルを生成（WW=0, WL=0で自動計算）
4. オプションでデータベースに保存
5. 表示設定に応じてスケール（0.6倍、1.0倍、1.3倍）

**重要なポイント**:
- `imagesArray:curStudy preferredObject:oAny`は、スタディに属する全シリーズの最初の画像の配列を返す
- `images[i]`は`series[i]`の最初の画像に対応
- **各シリーズに対して1つのサムネイルセルしか作成されない**

---

## 階層構造と展開/折りたたみ

### 1. セルの種類と配置

```
previewMatrix (O2ViewerThumbnailsMatrix)
├── スタディセル1 (Study Cell)
│   ├── シリーズセル1-1 (Series Cell) [スタディが非表示でない場合のみ表示]
│   ├── シリーズセル1-2 (Series Cell)
│   └── ...
├── スタディセル2 (Study Cell)
│   ├── シリーズセル2-1 (Series Cell)
│   └── ...
└── ...
```

### 2. セルの行数計算

```objective-c
i = 0;  // シリーズセルの行数カウンタ
for (id s in studiesArray) {
    if ([s isKindOfClass: [DicomStudy class]]) {
        [seriesArray addObject: [[BrowserController currentBrowser] childrenArray: s]];
        
        if ([s isHidden] == NO)  // スタディが非表示でない場合
            i += [[seriesArray lastObject] count];  // シリーズ数を加算
    }
}

// セルの総行数 = スタディ数 + 表示されているシリーズ数
[previewMatrix renewRows: i + [studiesArray count] columns: 1];
```

### 3. 展開/折りたたみ機能

**スタディのhidden状態**:
- `isHidden == NO`: スタディが展開されている（シリーズセルが表示される）
- `isHidden == YES`: スタディが折りたたまれている（シリーズセルが非表示）

**matrixPreviewSwitchHidden:メソッド**:
1. クリックされたスタディセルからスタディを取得
2. スタディのhidden状態を反転（`setHidden: ![curStudy isHidden]`）
3. すべてのViewerControllerで`buildMatrixPreview:`を呼び出してマトリックスを再構築

**シリーズセルの表示条件**:
- スタディが非表示でない場合、シリーズセルを表示
- スタディが非表示の場合、シリーズセルは表示しない（代わりに、選択されているシリーズがある場合はスタディセルの背景色を変更）

**注意**: HOROS-20240407のソースコードでは、シリーズの折りたたみ機能は実装されていません。ユーザーの記憶では「シリーズごとも引き込みできた」とのことですが、ソースコードには見当たりません。

---

## ソートと番号付け

### 1. スタディのソート順

**ソート条件**:
```objective-c
studiesArray = [studiesArray sortedArrayUsingDescriptors: 
    [NSArray arrayWithObject: [NSSortDescriptor sortDescriptorWithKey: @"date" ascending: NO]]];
```

- キー: `@"date"`（スタディの日付）
- 順序: `ascending: NO`（降順 = 最新が先）
- **日付と時刻を組み合わせて比較**（YYYYMMDD + HHMMSS）

### 2. スタディセルの番号付け

**番号の計算**:
```objective-c
NSUInteger curStudyIndexAll = [allStudiesArray indexOfObject: curStudy];
[components addObject: [NSString stringWithFormat: @" %d ", (int) curStudyIndexAll+1]];
```

- `curStudyIndexAll`: ソート後の配列でのインデックス（0から始まる）
- 表示される番号: `curStudyIndexAll + 1`（1から始まる）
- **最新のスタディが1、次が2、...**

**番号の背景色**:
- 番号の背景色は`curStudyIndexAll`（ソート後のインデックス）で決定
- `ViewerController.studyColors`から色を取得
- スタディごとに異なる色が割り当てられる

---

## セルの詳細仕様

### 1. スタディセル

**作成条件**: 各スタディに対して1つ

**表示内容**:
- **画像**: 番号付きアイコン（1, 2, 3...）
  - 背景色はスタディのインデックスに応じて変化
  - サイズ: SERIESPOPUPSIZE（約40ピクセル）
  - 位置: `NSImageOverlaps`（テキストと重ねて表示）
- **テキスト**: 複数行（\rで区切る）
  1. 番号: `" %d "`（スタディのインデックス+1）
  2. 空行: `""`
  3. 患者名 + 生年月日（同じ患者の場合は表示しない）
  4. スタディ名
  5. 日時（dateTimeFormatterでフォーマット）
  6. モダリティ: シリーズ数（"CT: 3 series"など）
  7. 状態テキスト（stateText）
  8. コメント
  9. アクション（"Show Series"または"Hide Series"）

**背景色**:
- **同じスタディ**: 背景色なし（`nil`）
- **異なるスタディ**: グレー（`_differentStudyColor`）
- **シリーズが選択されている場合**: 選択状態に応じた色（赤、緑、黄）

**アクション**: `matrixPreviewSwitchHidden:` - スタディの展開/折りたたみを切り替え

### 2. シリーズセル

**作成条件**: 各シリーズに対して1つ（スタディが展開されている場合のみ）

**表示内容**:
- **画像**: シリーズのサムネイル（1枚のみ）
  - データベースから取得、なければシリーズの最初の画像から生成
  - フォントサイズに応じてスケール（0.6倍、1.0倍、1.3倍）
  - **サイズ: SERIESPOPUPSIZE = 35x35ピクセル**（Windowsでは70x70ピクセルに拡大）
- **テキスト**: 3行（\rで区切る）
  1. シリーズ名（18文字を超える場合は34文字に切り詰め、太字フォント）
  2. 日時（dateTimeFormatterでフォーマット）
  3. 画像数（"X Images"、"X Frames"、"X Objects"）

**背景色**:
- **選択中**（viewerSeriesに含まれる）: 赤 - RGB(252, 177, 141), 75%透明度
- **ブレンディング中**: 緑 - RGB(195, 249, 145), 75%透明度
- **表示中**（displayedSeriesに含まれる）: 黄 - RGB(249, 240, 140), 75%透明度
- **異なるスタディ**: グレー - RGB(0.55, 0.55, 0.55)
- **同じスタディ**: 背景色なし

**アクション**: `matrixPreviewPressed:` - シリーズを表示

**重要なポイント**:
- **シリーズごとに1つのセルしか作成されない**
- **全画像を表示するのではなく、シリーズの代表画像（サムネイル）を1枚だけ表示**

### 3. セルの設定

**NSButtonCellの設定**:
```objective-c
[cell setImagePosition: NSImageBelow];  // 画像は下、テキストは上
[cell setTransparent:NO];
[cell setEnabled:YES];
[cell setButtonType:NSMomentaryPushInButton];
[cell setBezelStyle:NSShadowlessSquareBezelStyle];
[cell setHighlightsBy:NSContentsCellMask];
[cell setImageScaling:NSImageScaleProportionallyDown];
[cell setBordered:YES];
[cell setLineBreakMode: NSLineBreakByCharWrapping];  // 文字単位で折り返し
```

**背景色の描画**:
- 75%透明度で1ピクセル内側に描画（`NSInsetRect(frame, 1, 1)`）

### セルのレイアウト構造

#### NSButtonCellの画像位置の意味

HOROS-20240407の`ViewerController.m`の`matrixPreviewPressed:`メソッド（4413-4434行目）で：

```objective-c
[cell setImagePosition: NSImageBelow];
```

これにより、**NSButtonCellの画像位置が「下」に設定**されています。

- `NSImageBelow`: 画像が下、テキスト（タイトル）が上
- `NSImageAbove`: 画像が上、テキスト（タイトル）が下

つまり、HOROS-20240407では：
- **テキスト（タイトル）が上**
- **画像が下**

という縦方向の配置になっています。

#### セルの種類とレイアウト

**スタディセル（Study Cell）**:
- **高さ**: 120px（FULLSIZEHEIGHT）
- **幅**: 100px（SIZEWIDTH、フォントサイズで調整可能）
- **レイアウト**:
  1. **上**: 番号付きアイコン（NSImage、SERIESPOPUPSIZE x SERIESPOPUPSIZE）
  2. **下**: テキスト（NSAttributedString、複数行）

**シリーズセル（Series Cell）**:
- **高さ**: 60px（HALFSIZEHEIGHT）
- **幅**: 100px（SIZEWIDTH、フォントサイズで調整可能）
- **レイアウト**:
  1. **上**: テキスト（NSAttributedString、3行）
     - シリーズ名 / 日時 / 画像数
  2. **下**: サムネイル画像（NSImage、SERIESPOPUPSIZE x SERIESPOPUPSIZE）

**重要なポイント**: シリーズセルでは、**シリーズラベル（テキスト）とサムネイル（画像）が独立したセルとして存在**します。HOROS-20240407の実装では、シリーズラベルとサムネイルは別々の`ThumbnailCell`として作成されます。

### サムネイルサイズとレイアウト修正

#### 1. サムネイル画像のサイズ

HOROS-20240407の`ViewerController.m`（3893行目）で：
```objective-c
#define SERIESPOPUPSIZE 35
```

**サムネイル画像は35x35ピクセル**です。

**Windowsでの実装**:
- Windowsでは小さすぎるため、**70x70ピクセル（2倍）**に拡大
- `SERIESPOPUPSIZE = 70`として実装

#### 2. セルの高さとレイアウト

- **シリーズセル**: HALFSIZEHEIGHT = 60px
- **スタディセル**: FULLSIZEHEIGHT = 120px

**シリーズセルの内訳**:
- **シリーズラベルセル**: 70px（3行のテキスト表示用）
- **サムネイルセル**: 70px（70x70ピクセルの画像表示用）

#### 3. テキストの内容

シリーズセルのテキスト（5370行目）：
```objective-c
[cell setTitle:[NSString stringWithFormat:@"%@\r%@\r%@", 
    name,  // シリーズ名（18文字を超える場合は34文字に切り詰め）
    [[NSUserDefaults dateTimeFormatter] stringFromDate: [curSeries valueForKey:@"date"]],  // 日時
    N2LocalizedSingularPluralCount(count, singleType, pluralType)]];  // 画像数
```

3行のテキストで、`\r`（キャリッジリターン）で区切られています。

#### 4. サムネイル画像のスケーリング

HOROS-20240407の`buildMatrixPreview:`メソッド（5423-5437行目）で：
```objective-c
switch( [[NSUserDefaults standardUserDefaults] integerForKey: @"dbFontSize"])
{
    case -1:
        [cell setImage: [img imageByScalingProportionallyUsingNSImage: 0.6]];
        break;
    case 0:
        [cell setImage: img];  // 元のサイズ（SERIESPOPUPSIZE = 35）
        break;
    case 1:
        [cell setImage: [img imageByScalingProportionallyUsingNSImage: 1.3]];
        break;
}
```

デフォルト（`dbFontSize = 0`）では、サムネイル画像は**元のサイズ（35x35）**で表示されます。

#### 5. テキスト表示の無効化

**重要なポイント**: サムネイルセル（画像のみのセル）では、テキストは一切表示しません。
- `JLabel`の`setText(null)`を使用してテキスト表示を完全に無効化
- アイコン設定後も`setText(null)`を呼び出してテキストが表示されないようにする

---

## 動作仕様詳細

### ウィンドウの閉じるボタン

#### HOROS-20240407の実装

**ThumbnailsListPanel.m**:
- 閉じるボタンは**存在しない**
- `setDefaultCloseOperation`の設定は見つからない
- ウィンドウを閉じるには、`orderOut:`で非表示にするだけ
- `windowClosing`イベントで閉じる処理は行わない

**重要なポイント**:
- ThumbnailsListPanelはユーザーが直接閉じることはできない
- ViewerControllerが閉じられると、自動的に`setThumbnailsView: nil viewer: nil`が呼ばれて非表示になる
- `thumbnailsListWillClose:`メソッドで非表示処理を行う

**Java Swingでの実装**:
- `setUndecorated(true)`でタイトルバー全体を削除
- `setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE)`で閉じる操作を無効化

### スクロール動作

#### HOROS-20240407の実装

**buildMatrixPreview:メソッド** (ViewerController.m:5477-5488):

```objective-c
NSPoint origin = [[previewMatrix superview] bounds].origin;  // スクロール位置を保存

// ... マトリックスを構築 ...

if( showSelected)
{
    // 選択されたセルまでスクロール
    NSInteger index = [[[previewMatrix cells] valueForKeyPath:@"representedObject.object"] 
        indexOfObject: [[fileList[ curMovieIndex] objectAtIndex:0] valueForKey:@"series"]];
    
    if( index != NSNotFound)
        [previewMatrix scrollCellToVisibleAtRow: index column:0];
}
else
{
    // 元の位置に戻す
    [[previewMatrixScrollView contentView] scrollToPoint: origin];
    [previewMatrixScrollView reflectScrolledClipView: [previewMatrixScrollView contentView]];
}
```

**重要なポイント**:
- `showSelected == YES`: 選択されたセルまでスクロール（`scrollCellToVisibleAtRow`）
- `showSelected == NO`: 元のスクロール位置（`origin`）に戻す
- **マトリックスを再構築する前に、現在のスクロール位置を保存する**
- **セルが既に表示領域内にある場合は、スクロールしない**（必要最小限のスクロールのみ）

### 左クリックと右クリックの動作

#### HOROS-20240407の実装

**matrixPreviewPressed:メソッド** (ViewerController.m:4413-4434):

```objective-c
- (void) matrixPreviewPressed:(id) sender
{
    ThumbnailCell *cell = [sender selectedCell];
    id series = [[[sender selectedCell] representedObject] object];
    
    // cell.rightClickで右クリックかどうかを判定
    [self loadSelectedSeries: series rightClick: cell.rightClick];
}
```

**loadSelectedSeries:rightClick:メソッド** (ViewerController.m:4272-4380):

#### 左クリック時の動作

```objective-c
else  // 左クリックの場合
{
    if( [viewerSeries containsObject: series] == NO)
    {
        // 現在のViewerControllerに表示されていないシリーズの場合
        
        BOOL found = NO;
        BOOL showWindowIfDisplayed = [[NSUserDefaults standardUserDefaults] boolForKey: @"showWindowInsteadOfSwitching"];
        
        if( showWindowIfDisplayed)
        {
            // 既に他のViewerControllerで表示されているかチェック
            for( ViewerController *v in [ViewerController getDisplayed2DViewers])
            {
                if( [[v imageView] seriesObj] == series && v != self)
                {
                    [[v window] makeKeyAndOrderFront: self];
                    [v setHighLighted: 1.0];
                    found = YES;
                }
            }
        }
        
        if( found == NO)
        {
            // 現在のViewerControllerでシリーズを切り替え
            [[BrowserController currentBrowser] loadSeries :series :self :YES keyImagesOnly: displayOnlyKeyImages];
            [self showCurrentThumbnail:self];
            [self updateNavigator];
        }
    }
    else if( series != [[fileList[ curMovieIndex] objectAtIndex:0] valueForKey:@"series"])
    {
        // 4Dデータの場合、別のシリーズを選択
        NSUInteger idx = [viewerSeries indexOfObject: series];
        if( idx != NSNotFound)
        {
            [self setMovieIndex: idx];
            [self propagateSettings];
        }
    }
}
```

**重要なポイント**:
1. **`loadSeries :series :self :YES`**: 第2引数が`self`なので、現在のViewerControllerでシリーズを切り替える
2. **`showCurrentThumbnail:self`**: サムネイルパネルを更新（現在のViewerControllerのスクリーンに対応するThumbnailsListPanelに設定）
3. **新しいViewerControllerは作成しない**
4. **既に表示されている場合は、そのウィンドウを前面に**（`showWindowIfDisplayed`設定による）

#### 右クリック時の動作

```objective-c
if( (rightClick || ([[[NSApplication sharedApplication] currentEvent] modifierFlags] & NSCommandKeyMask)) && FullScreenOn == NO)
{
    // 右クリックまたはCommandキーが押されている場合
    // 新しいViewerControllerを開く
    ViewerController *newViewer = [[BrowserController currentBrowser] loadSeries :series :nil :YES keyImagesOnly: displayOnlyKeyImages];
    [newViewer setHighLighted: 1.0];
    
    // すべてのThumbnailsListPanelを非表示にする
    for( int i = 0; i < [[NSScreen screens] count]; i++) 
        [thumbnailsListPanel[ i] setThumbnailsView: nil viewer: nil];
    
    // 現在のViewerController（self）を前面に
    [[self window] makeKeyAndOrderFront: self];
    [self refreshToolbar];
    [self updateNavigator];
    
    // 新しいViewerControllerのThumbnailsListPanelを表示
    [newViewer showCurrentThumbnail: self];
    [self syncThumbnails];
}
```

**重要なポイント**:
1. **`loadSeries :series :nil :YES`**: 第2引数が`nil`なので、新しいViewerControllerを作成
2. **すべてのThumbnailsListPanelを非表示にする**（4312行目）: これは重要なポイント
3. **現在のViewerController（`self`）を前面に**（4314行目）
4. **新しいViewerControllerのThumbnailsListPanelを表示**（4318行目）: `[newViewer showCurrentThumbnail: self]`

**処理の順序**:
1. 新しいViewerControllerを作成
2. すべてのThumbnailsListPanelを非表示にする
3. 現在のViewerController（`self`）を前面に
4. 新しいViewerControllerのThumbnailsListPanelを表示

**重要なポイント**: 右クリック時は、**すべてのThumbnailsListPanelを一度非表示にしてから、新しいViewerControllerのThumbnailsListPanelだけを表示する**。これにより、古いViewerControllerのThumbnailsListPanelが残らない。

#### ThumbnailCellのrightClickプロパティ

**ThumbnailCell.m** (61-72行目):
```objective-c
- (NSMenu *)menuForEvent:(NSEvent *)anEvent inRect:(NSRect)cellFrame ofView:(NSView *)aView
{
    [self retain];
    
    rightClick = YES;
    [self performClick: self];
    rightClick = NO;
    
    [self autorelease];
    
    return nil;
}
```

右クリック（コンテキストメニュー）が表示されたときに`rightClick = YES`を設定し、`performClick:`を呼び出してから`rightClick = NO`に戻します。

#### 動作の違い

**左クリック**:
- 現在のViewerControllerでシリーズを切り替え
- 既に表示されている場合は、そのウィンドウを前面に（`showWindowIfDisplayed`設定による）
- 新しいViewerControllerは作成しない

**右クリック（またはCommandキー）**:
- **常に新しいViewerControllerを作成**
- すべてのThumbnailsListPanelを非表示にする
- 現在のViewerController（`self`）を前面に
- 新しいViewerControllerのThumbnailsListPanelを表示

**注意**: 現在の実装では、右クリックの動作が正しく実装されていない可能性があります。HOROS-20240407の仕様を再確認して、正しい実装に修正する必要があります。

### ビューワーの立ち上げ動作

#### HOROS-20240407の正しい動作

1. **左クリック**: 現在のViewerControllerでシリーズを切り替え（新しいウィンドウは作成しない）
2. **右クリック**: 新しいViewerControllerを作成（既存のウィンドウは閉じない）

---

## Java Swingへの移植方針

### 1. データ構造

```java
// スタディとシリーズの階層構造
for (StudyRecord study : studies) {
    // スタディセルを作成
    StudyThumbnailCellPanel studyCell = new StudyThumbnailCellPanel(study, ...);
    
    if (study.isExpanded()) {
        List<SeriesRecord> seriesList = database.getSeriesByStudyId(study.getId());
        
        for (SeriesRecord series : seriesList) {
            // シリーズラベルセルを作成
            SeriesLabelPanel seriesLabelCell = new SeriesLabelPanel(series, ...);
            
            // シリーズサムネイルセルを作成（各シリーズに対して1つだけ）
            SeriesThumbnailPanel seriesThumbnailCell = new SeriesThumbnailPanel(series, ...);
            
            // サムネイル画像を取得（シリーズの最初の画像から）
            Path firstImagePath = database.getFirstImagePathBySeriesId(series.getId());
            BufferedImage thumbnail = generateThumbnail(firstImagePath);
            
            seriesThumbnailCell.setThumbnail(thumbnail);
        }
    }
}
```

### 2. 重要な実装ポイント

1. **シリーズごとに1つのセル**: 全画像を表示するのではなく、シリーズの代表画像を1枚だけ表示
2. **サムネイルの取得**: データベースから取得、なければシリーズの最初の画像から生成
3. **階層構造**: スタディセルとシリーズセルの親子関係を維持
4. **展開/折りたたみ**: スタディセルをクリックすると、そのスタディのシリーズセルが表示/非表示に切り替わる
5. **ソート**: 日付と時刻の降順でソート（最新が先）
6. **番号付け**: ソート後のインデックス+1で番号を表示
7. **シリーズラベルとサムネイルの分離**: シリーズラベル（テキスト）とサムネイル（画像）を独立したセルとして作成

### 3. 動作仕様の実装

#### 閉じるボタンの無効化

```java
// タイトルバーと閉じるボタンを削除
setUndecorated(true);
setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
```

#### スクロール位置の保存と復元

```java
private Point savedScrollPosition;
private boolean showSelected = true;

private void rebuildThumbnailPanel() {
    // スクロール位置を保存
    if (thumbnailScrollPane != null) {
        JViewport viewport = thumbnailScrollPane.getViewport();
        if (viewport != null) {
            savedScrollPosition = viewport.getViewPosition();
        }
    }
    
    // マトリックスを再構築
    // ...
    
    // スクロール位置を復元
    SwingUtilities.invokeLater(() -> {
        if (showSelected && currentSeries != null) {
            // 選択されたセルまでスクロール（必要最小限のスクロールのみ）
            scrollToSelectedSeries();
        } else if (savedScrollPosition != null) {
            // 元の位置に戻す（初期表示時は(0,0)の場合はスクロールしない）
            if (savedScrollPosition.y > 0 || savedScrollPosition.x > 0) {
                viewport.setViewPosition(savedScrollPosition);
            }
        }
    });
}
```

#### 左クリックと右クリックの処理

```java
// シリーズセルにマウスリスナーを追加
seriesCell.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseClicked(MouseEvent e) {
        boolean rightClick = SwingUtilities.isRightMouseButton(e) || e.isControlDown();
        
        if (rightClick) {
            // 右クリックまたはControlキー: 新しいViewerControllerを作成
            // 注意: 現在の実装は仕様が間違っているため、再実装が必要
            ApplicationController.getInstance().openViewerWindow(seriesImages);
        } else {
            // 左クリック: 現在のViewerControllerでシリーズを切り替え
            // 既に表示されている場合は、そのウィンドウを前面に
            ViewerController existingViewer = ApplicationController.getInstance()
                .findViewerForSeries(series.getId());
            
            if (existingViewer != null && existingViewer != ViewerController.this) {
                existingViewer.toFront();
                existingViewer.requestFocus();
            } else {
                // 現在のViewerControllerでシリーズを切り替え
                loadSeriesInCurrentViewer(seriesImages, series.getId());
            }
        }
    }
});
```

### 4. 実装時の注意点

- **フォールバック処理**: スタディ情報がない場合、全画像を表示するフォールバックは実装しない（HOROSの仕様に反する）
- **セルの行数**: 動的に計算（スタディ数 + 表示されているシリーズ数）
- **背景色**: 75%透明度で1ピクセル内側に描画
- **テキストの区切り**: `\r`（キャリッジリターン）で複数行に区切る
- **スクロール位置**: マトリックスを再構築する前に必ず保存し、`showSelected`に応じて復元または選択セルまでスクロール
- **クリック動作**: 左クリックと右クリックで異なる動作を実装（左: 現在のViewerControllerで切り替え、右: 新しいViewerControllerを作成）
- **テキスト表示の無効化**: サムネイルセルでは`setText(null)`を使用してテキスト表示を完全に無効化
- **シリーズラベルとサムネイルの分離**: シリーズラベル（テキスト）とサムネイル（画像）を独立したセルとして作成

---

## 参考ソースコード

- `horos-20240407/Horos/Sources/ViewerController.m`: 
  - `buildMatrixPreview:`メソッド（4903行目～、特に5477-5488行目のスクロール処理）
  - `loadSelectedSeries:rightClick:`メソッド（4272行目～）
  - `matrixPreviewPressed:`メソッド（4413行目～）
  - `showCurrentThumbnail:`メソッド（3390行目～）
- `horos-20240407/Horos/Sources/ThumbnailsListPanel.m`: フローティングウィンドウの実装
- `horos-20240407/Horos/Sources/O2ViewerThumbnailsMatrix.mm`: マトリックスの実装、マウスイベント処理
- `horos-20240407/Horos/Sources/ThumbnailCell.m`: セルの実装、`rightClick`プロパティの処理
- `horos-20240407/Horos/Sources/BrowserController.m`:
  - `loadSeries:viewer:firstViewer:keyImagesOnly:`メソッド（8640行目～）

---

## 変更履歴

- 2024年12月: サムネイル関連のドキュメントを統合（HOROS-THUMBNAIL-CLICK-BEHAVIOR-ANALYSIS.md、HOROS-THUMBNAIL-CELL-LAYOUT-ANALYSIS.md、HOROS-THUMBNAIL-SIZE-AND-LAYOUT-FIX.mdの内容を統合）
- 2024年: 完全仕様書を作成
