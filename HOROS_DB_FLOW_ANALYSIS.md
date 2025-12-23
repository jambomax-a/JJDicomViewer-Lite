# HOROS-20240407 データベースフロー解析

## 1. DCMファイル読み込み時にDBに書き込む項目

### DicomStudy（スタディ）に書き込む項目
- `studyInstanceUID` (必須) - スタディインスタンスUID
- `patientUID` (必須) - 患者UID（patientName + patientID + dateOfBirthから生成）
- `name` - 患者名（PatientName）
- `patientID` - 患者ID
- `date` - スタディ日時（StudyDate + StudyTime）
- `dateAdded` - DB追加日時
- `modality` - モダリティ（複数シリーズの場合は結合）
- `numberOfImages` - 画像数
- その他多数のフィールド

### DicomSeries（シリーズ）に書き込む項目
- `seriesInstanceUID` (必須) - シリーズインスタンスUID
- `study` - 親スタディへの参照
- `name` - シリーズ説明（SeriesDescription）
- `modality` - モダリティ
- `date` - シリーズ日時
- `numberOfImages` - 画像数

### DicomImage（画像）に書き込む項目
- `sopInstanceUID` (必須) - SOPインスタンスUID
- `series` - 親シリーズへの参照
- `path` - ファイルパス
- その他多数のフィールド

## 2. HOROS起動時にDBから読み込む内容

### outlineViewRefreshメソッドで
```objective-c
outlineViewArray = [[_database objectsForEntity:_database.studyEntity predicate:nil error:&error] filteredArrayUsingPredicate:predicate];
```

- `_database.studyEntity`から全Studyを取得
- フィルタ（アルバム、時間間隔、モダリティ、検索）を適用
- `outlineViewArray`に設定

## 3. そのデータからツリーとして表示する内容

### NSOutlineViewのデータソースメソッド

#### outlineView:numberOfChildrenOfItem:
- `item == nil` → `outlineViewArray.count`（スタディ数）
- `item == Study` → `[[item valueForKey:@"imageSeries"] count]`（画像シリーズ数）
- `item == Series` → `[[item valueForKey:@"noFiles"] intValue]`（画像数）

#### outlineView:child:ofItem:
- `item == nil` → `[outlineViewArray objectAtIndex: index]`（スタディ）
- `item == Study` → `[[self childrenArray: item] objectAtIndex: index]`（シリーズ）
- `item == Series` → `[[item sortedImages] objectAtIndex: index]`（画像）

#### outlineView:isItemExpandable:
- `Study` → `YES`（展開可能）
- `Series` → `NO`（展開不可、ただし画像を表示する場合は展開可能）

## 現在の実装の問題点

1. **DICOMファイルを実際に読み込んでいない**
   - `addFilesAtPaths`でファイル名だけを設定
   - DICOMタグを読み込んでDBに書き込む処理がない

2. **StudyとSeriesの関係が正しく設定されていない**
   - `study.getSeries()`が空の可能性がある
   - `imageSeries()`メソッドが実装されていない

3. **DBから読み込む処理が簡易実装**
   - `getAllStudies()`は実装されているが、実際のDBから読み込んでいない可能性

