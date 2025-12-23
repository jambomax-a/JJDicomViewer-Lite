# HOROS-20240407 アーキテクチャ概要 - Java移植ドキュメント

## 1. 概要

このドキュメントは、HOROS-20240407のソースコードを徹底的に解析し、Java SwingでのDICOMビューワー実装に向けたアーキテクチャ概要をまとめたものです。

**注意**: 本プロジェクトは獣医向けDICOMビューワーとして開発されており、Vet-Systemとの連携を主目的としています。HOROS/OsirixのDB共通性によるインポート・エクスポート連携やBonjour機能は実装しません。

## 2. HOROSの基本構造

### 2.1 技術スタック（元実装）

- **言語**: Objective-C / Objective-C++
- **フレームワーク**: Cocoa (macOS)
- **グラフィックス**: OpenGL
- **DICOM処理**: DCMTK (C++ライブラリ)
- **データベース**: Core Data (SQLite)
- **画像処理**: vImage (Accelerate Framework), VTK

### 2.2 Java移植時の技術スタック

- **言語**: Java 21
- **UIフレームワーク**: Java Swing
  - **選択理由**: マルチプラットフォーム対応（Windows、macOS、Linuxで同一バイナリが動作）
  - **除外**: JavaFX（インストーラー作成時の問題により除外）
- **グラフィックス**: Java2D / BufferedImage
- **DICOM処理**: dcm4che-5.34.1
- **データベース**: SQLite (JDBC)
- **画像処理**: Java2D, 必要に応じてJNIでOpenCV連携

## 3. 主要コンポーネント

### 3.1 コアクラス階層

```
AppController (アプリケーション制御)
├── BrowserController (ブラウザウィンドウ制御)
│   ├── DicomDatabase (データベース管理)
│   ├── DicomStudy (スタディ)
│   ├── DicomSeries (シリーズ)
│   └── DicomImage (画像)
│
└── ViewerController (ビューワーウィンドウ制御)
    ├── DCMView (画像表示ビュー - OpenGL)
    ├── DCMPix (画像データ)
    └── ROI (領域 of Interest)
```

### 3.2 データモデル階層

```
DicomDatabase
└── DicomStudy (患者単位)
    └── DicomSeries (シリーズ単位)
        └── DicomImage (画像単位)
```

**主要属性**:

- **DicomStudy**: 
  - patientID, patientName, patientUID
  - studyInstanceUID, studyDate
  - modality, accessionNumber
  
- **DicomSeries**:
  - seriesInstanceUID, seriesDescription
  - numberOfImages, modality
  - windowLevel, windowWidth
  
- **DicomImage**:
  - sopInstanceUID, instanceNumber
  - pathString, completePath
  - width, height, numberOfFrames
  - sliceLocation

## 4. 主要機能モジュール

### 4.1 DICOMファイル処理 (`DicomFile`)

**責務**:
- DICOMファイルの検証
- DICOMタグの読み込み
- メタデータ抽出
- 非DICOM形式（TIFF, JPEG, NIfTI等）のサポート

**主要メソッド**:
- `isDICOMFile:` - DICOMファイル判定
- `getDicomFile` - DICOMファイル解析
- `dicomElements` - DICOM要素辞書取得

**Java実装時の対応**:
```java
public class DicomFile {
    public static boolean isDicomFile(Path file);
    public DicomElements parseDicomFile(Path file);
    public Map<String, Object> getDicomElements();
}
```

### 4.2 画像データ処理 (`DCMPix`)

**責務**:
- DICOM画像データの読み込み
- ピクセルデータの変換（16bit→float）
- Window Level/Widthの適用
- VOI LUTの適用
- 画像変換（回転、スケール、反転）
- SUV計算（PET用）

**主要プロパティ**:
- `fImage` - float配列のピクセルデータ
- `width`, `height` - 画像サイズ
- `pixelSpacingX`, `pixelSpacingY` - ピクセル間隔
- `originX`, `originY`, `originZ` - 原点座標
- `orientation[9]` - 方向ベクトル（3x3行列）
- `slope`, `offset` - リスケール係数
- `ww`, `wl` - Window Width/Level

**Java実装時の対応**:
```java
public class DicomPix {
    private float[] pixelData;
    private int width, height;
    private double pixelSpacingX, pixelSpacingY;
    private double[] origin = new double[3];
    private double[] orientation = new double[9];
    private double slope, offset;
    private float windowWidth, windowLevel;
    
    public BufferedImage renderImage(float ww, float wl);
    public void applyWindowLevel(float ww, float wl);
}
```

### 4.3 画像表示 (`DCMView`)

**責務**:
- OpenGLによる画像レンダリング
- マウス操作（ズーム、パン、回転）
- Window Level/Width調整
- ROI描画
- アノテーション表示
- ブレンディング（複数画像の重ね合わせ）

**主要機能**:
- テクスチャ管理
- 座標変換（ピクセル座標↔DICOM座標）
- ツール管理（ROI、測定、テキスト等）
- 同期機能（複数ビューの同期）

**Java実装時の対応**:
```java
public class DicomView extends JPanel {
    private DicomPix currentPix;
    private BufferedImage renderedImage;
    private float scale = 1.0f;
    private Point origin = new Point(0, 0);
    private float windowWidth, windowLevel;
    
    @Override
    protected void paintComponent(Graphics g);
    public void setPixels(List<DicomPix> pixels);
    public void setWindowLevel(float wl, float ww);
}
```

### 4.4 ビューワー制御 (`ViewerController`)

**責務**:
- 複数画像の管理
- スライダーによる画像切り替え
- ツールバー管理
- ROI管理
- エクスポート機能

**主要コンポーネント**:
- `imageView` - メイン画像ビュー
- `slider` - 画像ナビゲーション
- `previewMatrix` - サムネイル表示

**Java実装時の対応**:
```java
public class ViewerController extends JFrame {
    private DicomView imageView;
    private JSlider imageSlider;
    private List<DicomPix> imageList;
    private int currentImageIndex;
    
    public void loadSeries(DicomSeries series);
    public void setCurrentImage(int index);
}
```

### 4.5 ブラウザ制御 (`BrowserController`)

**責務**:
- データベースブラウザ表示
- スタディ/シリーズ/画像の階層表示
- 検索機能
- インポート機能
- エクスポート機能

**主要コンポーネント**:
- `databaseOutline` - 階層表示
- `imageView` - プレビュー表示
- `oMatrix` - サムネイル表示

**Java実装時の対応**:
```java
public class BrowserController extends JFrame {
    private JTree studyTree;
    private DicomDatabase database;
    private PreviewPanel previewPanel;
    
    public void refreshDatabase();
    public void searchStudies(String query);
}
```

### 4.6 データベース管理 (`DicomDatabase`)

**責務**:
- Core Data管理
- ファイルインポート
- ファイルスキャン
- データベースクリーンアップ
- ルーティング（自動送信）

**主要機能**:
- `addFilesAtPaths:` - ファイル追加
- `scanAtPath:` - ディレクトリスキャン
- `objects:` - クエリ実行

**Java実装時の対応**:
```java
public class DicomDatabase {
    private Connection connection;
    
    public void importFiles(List<Path> files);
    public List<DicomStudy> searchStudies(String patientName);
    public DicomStudy getStudy(String studyInstanceUID);
}
```

## 5. DICOM通信

### 5.1 C-STORE (送信) (`DCMTKStoreSCU`)

**責務**:
- DICOMファイルの送信
- 転送構文のネゴシエーション
- 圧縮オプション
- TLS対応

**主要メソッド**:
- `initWithCallingAET:calledAET:hostname:port:filesToSend:`
- `run:`

**Java実装時の対応**:
```java
public class DicomStoreSCU {
    public void sendFiles(String callingAET, String calledAET, 
                         String hostname, int port, 
                         List<Path> files);
}
```

### 5.2 C-FIND (検索) (`DCMTKQueryNode`)

**責務**:
- DICOMサーバーへのクエリ
- 階層的クエリ（Patient→Study→Series→Image）
- 結果の階層構造構築

**主要メソッド**:
- `queryWithValues:` - クエリ実行
- `findSCU:` - C-FIND実行

**Java実装時の対応**:
```java
public class DicomQueryNode {
    public List<DicomStudy> queryStudies(String patientName);
    public List<DicomSeries> querySeries(String studyInstanceUID);
}
```

### 5.3 C-MOVE (取得) (`DCMTKQueryNode`)

**責務**:
- DICOMサーバーからの画像取得
- 転送先の指定
- 進捗管理

**主要メソッド**:
- `move:` - C-MOVE実行
- `moveSCU:` - C-MOVE実装

**Java実装時の対応**:
```java
public class DicomMoveSCU {
    public void retrieveImages(String studyInstanceUID, 
                              String seriesInstanceUID,
                              String destinationAET);
}
```

## 6. 画像処理機能

### 6.1 Window Level/Width

**実装**:
- `changeWLWW:newWL:newWW:` - WL/WW変更
- `compute8bitRepresentation` - 8bit変換
- プリセット管理

### 6.2 ROI (Region of Interest)

**機能**:
- 多角形、楕円、矩形ROI
- ROI統計計算（平均、標準偏差、最大、最小）
- ROI塗りつぶし
- ROIエクスポート

### 6.3 画像変換

**機能**:
- 回転
- スケール
- 反転（X/Y）
- リサンプリング

### 6.4 ブレンディング

**機能**:
- 複数シリーズの重ね合わせ
- ブレンディング係数調整
- カラーマッピング

## 7. データフロー

### 7.1 ファイルインポート

```
ファイル選択
  ↓
DicomFile.parse()
  ↓
DICOMタグ抽出
  ↓
DicomDatabase.addFilesAtPaths()
  ↓
Core Data保存
  ↓
DCMPix生成
  ↓
画像表示
```

### 7.2 画像表示

```
DicomImage選択
  ↓
DCMPix.initWithPath()
  ↓
DICOMファイル読み込み
  ↓
ピクセルデータ変換
  ↓
Window Level/Width適用
  ↓
8bit変換
  ↓
テクスチャ生成
  ↓
OpenGL描画
```

### 7.3 DICOM通信

```
C-FIND要求
  ↓
DCMTKQueryNode.queryWithValues()
  ↓
DICOMサーバー接続
  ↓
クエリ実行
  ↓
結果受信
  ↓
階層構造構築
  ↓
UI更新
```

## 8. Java実装時の考慮事項

### 8.1 メモリ管理

- Objective-CのARC → Javaのガベージコレクション
- 大きな画像データの適切な解放
- 画像キャッシュの実装

### 8.2 スレッド処理

- 画像読み込みはバックグラウンドスレッドで実行
- SwingのEDT（Event Dispatch Thread）でのUI更新
- スレッドセーフなデータ構造の使用

### 8.3 パフォーマンス

- 画像レンダリングの最適化
- サムネイル生成の非同期処理
- データベースクエリの最適化

### 8.4 プラットフォーム依存性

- macOS固有機能（Core Data、Accelerate）の代替
- ファイルパスの処理
- 文字エンコーディング

## 9. 依存関係

### 9.1 HOROSの依存ライブラリ

- DCMTK (C++)
- VTK (3D可視化)
- OpenGL
- Core Data
- Accelerate Framework

### 9.2 Java実装時の依存ライブラリ

- dcm4che-5.34.1 (DICOM処理)
- SQLite JDBC (データベース)
- Java Swing (UI)
- Java2D (画像処理)
- SLF4J/Logback (ロギング)

## 10. 次のステップ

1. **DICOMファイル処理モジュール**の実装
2. **画像表示コンポーネント**の実装
3. **データベース管理**の実装
4. **DICOM通信**の実装
5. **UIコンポーネント**の実装

各モジュールの詳細は、個別のドキュメントを参照してください。

