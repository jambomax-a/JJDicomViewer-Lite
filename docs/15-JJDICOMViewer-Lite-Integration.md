# JJDICOMViewer-Lite連携 - Java移植ドキュメント

## 1. 概要

このドキュメントは、JJDICOMViewer-Lite（CD/DVD同梱用の簡易DICOMビューワー）との連携と操作性の統一についてまとめたものです。

## 2. JJDICOMViewer-Liteの位置づけ

### 2.1 用途

JJDICOMViewer-Liteは、**CD/DVD同梱用の簡易DICOMビューワー**として開発されています：

- **お渡し用のCD/DVD作成時に同梱**
  - 患者へのDICOM画像の提供時に使用
  - インストール不要で実行可能（ポータブル）
  - CD/DVDから直接実行可能

- **軽量で起動が速い**
  - 最小限の機能に絞る
  - 高速な起動と動作

### 2.2 機能範囲

**実装する機能**:
- 基本的な画像表示機能
- ズーム、パン、WL/WW調整
- スライス移動
- キーボードショートカット（H/V/R/Lキー）

**除外する機能**:
- ROI機能
- PACS機能
- データベース管理
- 複雑な画像解析機能

### 2.3 操作性の統一

JJDICOMViewer-LiteとJJDICOMViewerの操作性を統一することで、ユーザーの学習コストを低減します。

**共通の操作性**:
- **パンニング**: 左クリック + ドラッグ（SHIFTキーが押されていない場合）
- **ズーム**: CTRL + マウスホイール
- **Window Level/Width**: SHIFT + 左クリック + ドラッグ
  - 縦方向: Window Center（上にドラッグで増加）
  - 横方向: Window Width（右にドラッグで増加）
- **スライス移動**: 通常のマウスホイール（CTRLが押されていない場合）

**キーボードショートカット**:
- **Hキー**: 左右反転（FlipHorizontal）
- **Vキー**: 上下反転（FlipVertical）
- **Rキー**: 右回転（90度時計回り）
- **Lキー**: 左回転（90度反時計回り）

## 3. 操作性の実装

### 3.1 マウス操作の実装

**JJDICOMViewer-Lite実装**を基準とします：

```java
import javax.swing.*;
import java.awt.event.*;

public class ImageViewerPanel extends JPanel {
    private int lastMouseX, lastMouseY;
    private boolean isPanning = false;
    private boolean isWindowLevelAdjusting = false;
    
    public ImageViewerPanel() {
        // パンニング: 左クリック + ドラッグ（SHIFTキーが押されていない場合）
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                if (SwingUtilities.isLeftMouseButton(e) && !e.isShiftDown()) {
                    isPanning = true;
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isPanning = false;
                isWindowLevelAdjusting = false;
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isPanning) {
                    int deltaX = e.getX() - lastMouseX;
                    int deltaY = e.getY() - lastMouseY;
                    panX += deltaX;
                    panY += deltaY;
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    repaint();
                } else if (isWindowLevelAdjusting) {
                    int deltaX = e.getX() - lastMouseX;
                    int deltaY = e.getY() - lastMouseY;
                    adjustWindowLevel(-deltaY * 2, deltaX * 2);
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    repaint();
                }
            }
        });
        
        // ズーム: CTRL + マウスホイール
        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                double zoomDelta = e.getWheelRotation() < 0 ? 1.1 : 0.9;
                setZoom(zoomFactor * zoomDelta);
            } else {
                // スライス移動: 通常のマウスホイール（CTRLが押されていない場合）
                int delta = e.getWheelRotation();
                if (delta < 0) {
                    moveToNextSlice();
                } else {
                    moveToPreviousSlice();
                }
            }
        });
        
        // Window Level/Width: SHIFT + 左クリック + ドラッグ
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.isShiftDown()) {
                    isWindowLevelAdjusting = true;
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                }
            }
        });
    }
}
```

### 3.2 キーボードショートカットの実装

```java
public class ImageViewerPanel extends JPanel {
    public ImageViewerPanel() {
        // Hキー (左右反転)
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), "flipHorizontal");
        getActionMap().put("flipHorizontal", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setXFlipped(!isXFlipped());
                repaint();
            }
        });
        
        // Vキー (上下反転)
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_V, 0), "flipVertical");
        getActionMap().put("flipVertical", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setYFlipped(!isYFlipped());
                repaint();
            }
        });
        
        // Rキー (右回転 90度)
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "rotateRight");
        getActionMap().put("rotateRight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setRotation((getRotation() + 90) % 360);
                repaint();
            }
        });
        
        // Lキー (左回転 90度)
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "rotateLeft");
        getActionMap().put("rotateLeft", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setRotation((getRotation() - 90 + 360) % 360);
                repaint();
            }
        });
    }
}
```

## 4. CD/DVD同梱用の要件

### 4.1 ポータブル実行

**要件**:
- インストール不要で実行可能
- CD/DVDから直接実行可能
- 設定ファイルは実行ファイルと同じディレクトリに保存

**Java実装**:
```java
import java.nio.file.Path;
import java.nio.file.Paths;

public class PortableConfig {
    private Path configDir;
    
    public PortableConfig() {
        // 実行ファイルのディレクトリを取得
        String jarPath = getClass().getProtectionDomain()
            .getCodeSource().getLocation().getPath();
        Path jarDir = Paths.get(jarPath).getParent();
        
        // 設定ディレクトリ（実行ファイルと同じディレクトリ）
        this.configDir = jarDir.resolve("config");
        
        // 設定ディレクトリが存在しない場合は作成
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }
    }
    
    public Path getConfigPath(String filename) {
        return configDir.resolve(filename);
    }
}
```

### 4.2 軽量化

**要件**:
- 最小限の依存関係
- 小さなJARファイルサイズ
- 高速な起動

**実装方針**:
```gradle
// build.gradle.kts
plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

tasks.shadowJar {
    archiveBaseName.set("JJDICOMViewer-Lite")
    archiveClassifier.set("")
    
    // 不要な依存関係を除外
    dependencies {
        exclude(dependency("org.slf4j:slf4j-log4j12"))
        exclude(dependency("ch.qos.logback:logback-classic"))
    }
    
    // マニフェストの設定
    manifest {
        attributes(
            "Main-Class" to "com.jjdicomviewer.lite.Main",
            "Implementation-Title" to "JJDICOMViewer-Lite",
            "Implementation-Version" to project.version
        )
    }
    
    // 最小限のリソースのみを含める
    minimize()
}
```

### 4.3 CD/DVD構成

**推奨構成**:
```
CD/DVD/
├── JJDICOMViewer-Lite.jar          # 実行ファイル
├── README.txt                      # 使用方法
├── DICOM/                          # DICOM画像フォルダ
│   ├── Study1/
│   │   ├── Series1/
│   │   │   ├── IMG001.dcm
│   │   │   └── IMG002.dcm
│   │   └── Series2/
│   └── Study2/
└── config/                         # 設定ファイル（オプション）
    └── settings.properties
```

**README.txtの例**:
```
JJDICOMViewer-Lite 使用方法
============================

1. このCD/DVDをドライブに挿入してください

2. JJDICOMViewer-Lite.jarをダブルクリックして起動してください
   （Javaがインストールされている必要があります）

3. ファイルメニューから「DICOMフォルダを開く」を選択し、
   DICOMフォルダを選択してください

操作説明:
- パンニング: 左クリック + ドラッグ
- ズーム: CTRL + マウスホイール
- Window Level/Width: SHIFT + 左クリック + ドラッグ
- スライス移動: マウスホイール
- 左右反転: Hキー
- 上下反転: Vキー
- 右回転: Rキー
- 左回転: Lキー

システム要件:
- Java 21以上が必要です
- Windows 10/11、macOS、Linuxで動作します
```

## 5. 操作性の統一チェックリスト

### 5.1 マウス操作

- [ ] パンニング: 左クリック + ドラッグ（SHIFTキーが押されていない場合）
- [ ] ズーム: CTRL + マウスホイール
- [ ] Window Level/Width: SHIFT + 左クリック + ドラッグ
- [ ] スライス移動: 通常のマウスホイール（CTRLが押されていない場合）

### 5.2 キーボードショートカット

- [ ] Hキー: 左右反転
- [ ] Vキー: 上下反転
- [ ] Rキー: 右回転（90度）
- [ ] Lキー: 左回転（90度）

### 5.3 UI要素

- [ ] 画像表示エリアのレイアウト
- [ ] ツールバーの配置
- [ ] メニューの構成

## 6. 実装順序

1. **JJDICOMViewer-Liteの実装確認**
   - 既存の操作性を確認
   - 実装パターンを理解

2. **JJDICOMViewerの基本実装**
   - JJDICOMViewer-Liteと同じ操作性を実装
   - マウス操作とキーボードショートカット

3. **操作性の統一確認**
   - 両方のビューワーで同じ操作が可能か確認
   - ユーザーテスト

4. **CD/DVD同梱用の最適化**
   - 軽量化
   - ポータブル実行の実装

## 7. 参考資料

- JJDICOMViewer-Liteソースコード: `C:\Users\jam11\JJDicomViewer-Lite\`
- 画像表示パネル: `src/main/java/com/jjdicomviewer/ui/ImageViewerPanel.java`
- 詳細は[画像表示・レンダリング](03-Image-Display-Rendering.md)を参照

