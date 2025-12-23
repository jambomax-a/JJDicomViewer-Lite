# UIコンポーネント - BrowserController

## 1. 概要

このドキュメントは、HOROSのBrowserController（ブラウザウィンドウ）を解析し、Java Swingでの実装方法をまとめたものです。

**注意**: ViewerControllerの詳細については、`06-UI-Components-ViewerController-Complete.md`を参照してください。

## 2. BrowserController (ブラウザウィンドウ)

### 2.1 機能概要

- データベースブラウザ表示
- スタディ/シリーズ/画像の階層表示
- 検索機能
- プレビュー表示
- サムネイル表示

### 2.2 Java実装

```java
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.List;

public class BrowserController extends JFrame {
    private DicomDatabase database;
    private JTree studyTree;
    private DefaultTreeModel treeModel;
    private PreviewPanel previewPanel;
    private ThumbnailPanel thumbnailPanel;
    private JTextField searchField;
    
    public BrowserController(DicomDatabase database) {
        this.database = database;
        initializeUI();
        loadStudies();
    }
    
    private void initializeUI() {
        setTitle("DICOM Browser");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        
        // メインレイアウト
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // 左パネル: ツリーと検索
        JPanel leftPanel = new JPanel(new BorderLayout());
        
        // 検索バー
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        searchField.addActionListener(e -> performSearch());
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());
        searchPanel.add(new JLabel("Search: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        
        // ツリー
        studyTree = new JTree();
        studyTree.setCellRenderer(new StudyTreeCellRenderer());
        studyTree.addTreeSelectionListener(e -> onTreeSelectionChanged());
        JScrollPane treeScroll = new JScrollPane(studyTree);
        
        leftPanel.add(searchPanel, BorderLayout.NORTH);
        leftPanel.add(treeScroll, BorderLayout.CENTER);
        
        // 右パネル: プレビューとサムネイル
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        previewPanel = new PreviewPanel();
        thumbnailPanel = new ThumbnailPanel();
        thumbnailPanel.addSelectionListener(this::onThumbnailSelected);
        
        rightSplit.setTopComponent(previewPanel);
        rightSplit.setBottomComponent(thumbnailPanel);
        rightSplit.setResizeWeight(0.6);
        
        mainSplit.setLeftComponent(leftPanel);
        mainSplit.setRightComponent(rightSplit);
        mainSplit.setResizeWeight(0.3);
        
        add(mainSplit);
        
        // メニューバー
        createMenuBar();
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File メニュー
        JMenu fileMenu = new JMenu("File");
        JMenuItem importItem = new JMenuItem("Import Files...");
        importItem.addActionListener(e -> importFiles());
        fileMenu.add(importItem);
        
        JMenuItem exportItem = new JMenuItem("Export...");
        exportItem.addActionListener(e -> exportFiles());
        fileMenu.add(exportItem);
        
        menuBar.add(fileMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void loadStudies() {
        try {
            List<DicomStudy> studies = database.searchStudies(null, null, null);
            buildTree(studies);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading studies: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void buildTree(List<DicomStudy> studies) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Studies");
        
        for (DicomStudy study : studies) {
            DefaultMutableTreeNode studyNode = 
                new DefaultMutableTreeNode(study);
            
            try {
                List<DicomSeries> series = database.getSeriesByStudy(study.getId());
                for (DicomSeries s : series) {
                    DefaultMutableTreeNode seriesNode = 
                        new DefaultMutableTreeNode(s);
                    studyNode.add(seriesNode);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            root.add(studyNode);
        }
        
        treeModel = new DefaultTreeModel(root);
        studyTree.setModel(treeModel);
    }
    
    private void performSearch() {
        String query = searchField.getText();
        if (query == null || query.trim().isEmpty()) {
            loadStudies();
            return;
        }
        
        try {
            List<DicomStudy> studies = database.searchStudies(query, null, null);
            buildTree(studies);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Search error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void onTreeSelectionChanged() {
        TreePath path = studyTree.getSelectionPath();
        if (path == null) return;
        
        DefaultMutableTreeNode node = 
            (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        
        if (userObject instanceof DicomSeries) {
            DicomSeries series = (DicomSeries) userObject;
            loadSeries(series);
        } else if (userObject instanceof DicomStudy) {
            DicomStudy study = (DicomStudy) userObject;
            // スタディ選択時の処理
        }
    }
    
    private void loadSeries(DicomSeries series) {
        try {
            List<DicomImage> images = database.getImagesBySeries(series.getId());
            thumbnailPanel.setImages(images);
            
            if (!images.isEmpty()) {
                previewPanel.setImage(images.get(0));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void onThumbnailSelected(DicomImage image) {
        previewPanel.setImage(image);
    }
    
    private void importFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "DICOM Files", "dcm", "dicom"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            List<Path> filePaths = Arrays.stream(files)
                .map(File::toPath)
                .collect(Collectors.toList());
            
            // インポート処理（別スレッドで実行）
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    database.importFiles(filePaths);
                    return null;
                }
                
                @Override
                protected void done() {
                    try {
                        get();
                        loadStudies();
                        JOptionPane.showMessageDialog(BrowserController.this,
                            "Files imported successfully",
                            "Import", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(BrowserController.this,
                            "Import failed: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void exportFiles() {
        // エクスポート処理
    }
}

// ツリーセルレンダラー
class StudyTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel, boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, 
            leaf, row, hasFocus);
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();
        
        if (userObject instanceof DicomStudy) {
            DicomStudy study = (DicomStudy) userObject;
            setText(study.getPatientName() + " - " + study.getStudyDescription());
            setIcon(UIManager.getIcon("FileView.directoryIcon"));
        } else if (userObject instanceof DicomSeries) {
            DicomSeries series = (DicomSeries) userObject;
            setText(series.getSeriesDescription() + 
                " (" + series.getNumberOfImages() + " images)");
            setIcon(UIManager.getIcon("FileView.fileIcon"));
        }
        
        return this;
    }
}
```

### 2.3 プレビューパネル

```java
public class PreviewPanel extends JPanel {
    private DicomView dicomView;
    private JLabel infoLabel;
    
    public PreviewPanel() {
        setLayout(new BorderLayout());
        
        dicomView = new DicomView();
        add(dicomView, BorderLayout.CENTER);
        
        infoLabel = new JLabel("No image selected");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(infoLabel, BorderLayout.SOUTH);
    }
    
    public void setImage(DicomImage image) {
        if (image == null) {
            dicomView.setPixels(null);
            infoLabel.setText("No image selected");
            return;
        }
        
        try {
            Path imagePath = Paths.get(image.getPathString());
            DicomPix pix = new DicomPix();
            pix.loadFromDicomFile(imagePath);
            
            dicomView.setPixels(Collections.singletonList(pix));
            
            infoLabel.setText(String.format(
                "Patient: %s | Study: %s | Instance: %d",
                image.getSeries().getStudy().getPatientName(),
                image.getSeries().getStudy().getStudyDescription(),
                image.getInstanceNumber()));
        } catch (IOException e) {
            infoLabel.setText("Error loading image: " + e.getMessage());
        }
    }
}
```

### 2.4 サムネイルパネル

```java
public class ThumbnailPanel extends JPanel {
    private List<DicomImage> images;
    private List<ThumbnailButton> buttons;
    private Consumer<DicomImage> selectionListener;
    
    public ThumbnailPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBorder(BorderFactory.createTitledBorder("Thumbnails"));
    }
    
    public void setImages(List<DicomImage> images) {
        this.images = images;
        removeAll();
        buttons = new ArrayList<>();
        
        for (DicomImage image : images) {
            ThumbnailButton button = new ThumbnailButton(image);
            button.addActionListener(e -> {
                if (selectionListener != null) {
                    selectionListener.accept(image);
                }
            });
            buttons.add(button);
            add(button);
        }
        
        revalidate();
        repaint();
    }
    
    public void addSelectionListener(Consumer<DicomImage> listener) {
        this.selectionListener = listener;
    }
}

class ThumbnailButton extends JButton {
    private DicomImage image;
    private BufferedImage thumbnail;
    
    public ThumbnailButton(DicomImage image) {
        this.image = image;
        setPreferredSize(new Dimension(100, 100));
        loadThumbnail();
    }
    
    private void loadThumbnail() {
        // 非同期でサムネイルを読み込み
        SwingWorker<BufferedImage, Void> worker = 
            new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                Path imagePath = Paths.get(image.getPathString());
                DicomPix pix = new DicomPix();
                pix.loadFromDicomFile(imagePath);
                
                BufferedImage img = pix.getRenderedImage();
                return createThumbnail(img, 100, 100);
            }
            
            @Override
            protected void done() {
                try {
                    thumbnail = get();
                    if (thumbnail != null) {
                        setIcon(new ImageIcon(thumbnail));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private BufferedImage createThumbnail(BufferedImage source, 
                                         int width, int height) {
        BufferedImage thumbnail = new BufferedImage(width, height, 
            BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(source, 0, 0, width, height, null);
        g2d.dispose();
        return thumbnail;
    }
}
```

## 3. ViewerController (ビューワーウィンドウ)

### 3.1 機能概要

- 複数画像の表示
- スライダーによる画像ナビゲーション
- ツールバー（Window Level/Width、ズーム等）
- ROI描画
- エクスポート機能

### 3.2 Java実装

```java
public class ViewerController extends JFrame {
    private DicomView imageView;
    private JSlider imageSlider;
    private List<DicomPix> imageList;
    private int currentIndex = 0;
    
    private JLabel imageInfoLabel;
    private JTextField wlField, wwField;
    private JButton zoomInButton, zoomOutButton, fitButton;
    
    public ViewerController(List<DicomImage> images) {
        initializeUI();
        loadImages(images);
    }
    
    private void initializeUI() {
        setTitle("DICOM Viewer");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 800);
        
        // メインレイアウト
        setLayout(new BorderLayout());
        
        // ツールバー
        JToolBar toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);
        
        // 画像ビュー
        imageView = new DicomView();
        add(imageView, BorderLayout.CENTER);
        
        // スライダー
        imageSlider = new JSlider(JSlider.HORIZONTAL, 0, 0, 0);
        imageSlider.addChangeListener(e -> {
            if (!imageSlider.getValueIsAdjusting()) {
                int index = imageSlider.getValue();
                setCurrentImage(index);
            }
        });
        add(imageSlider, BorderLayout.SOUTH);
        
        // 情報ラベル
        imageInfoLabel = new JLabel();
        imageInfoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(imageInfoLabel, BorderLayout.NORTH);
    }
    
    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        
        // Window Level/Width
        toolBar.add(new JLabel("WL:"));
        wlField = new JTextField("0", 5);
        wlField.addActionListener(e -> updateWindowLevel());
        toolBar.add(wlField);
        
        toolBar.add(new JLabel("WW:"));
        wwField = new JTextField("0", 5);
        wwField.addActionListener(e -> updateWindowWidth());
        toolBar.add(wwField);
        
        toolBar.addSeparator();
        
        // ズーム
        zoomInButton = new JButton("Zoom In");
        zoomInButton.addActionListener(e -> imageView.zoom(1.2f));
        toolBar.add(zoomInButton);
        
        zoomOutButton = new JButton("Zoom Out");
        zoomOutButton.addActionListener(e -> imageView.zoom(0.8f));
        toolBar.add(zoomOutButton);
        
        fitButton = new JButton("Fit");
        fitButton.addActionListener(e -> imageView.scaleToFit());
        toolBar.add(fitButton);
        
        return toolBar;
    }
    
    private void loadImages(List<DicomImage> images) {
        imageList = new ArrayList<>();
        
        // 非同期で画像を読み込み
        SwingWorker<Void, DicomPix> worker = new SwingWorker<Void, DicomPix>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (DicomImage image : images) {
                    Path imagePath = Paths.get(image.getPathString());
                    DicomPix pix = new DicomPix();
                    pix.loadFromDicomFile(imagePath);
                    publish(pix);
                }
                return null;
            }
            
            @Override
            protected void process(List<DicomPix> chunks) {
                imageList.addAll(chunks);
                if (imageList.size() == 1) {
                    setCurrentImage(0);
                }
                imageSlider.setMaximum(imageList.size() - 1);
            }
        };
        worker.execute();
    }
    
    private void setCurrentImage(int index) {
        if (imageList == null || index < 0 || index >= imageList.size()) {
            return;
        }
        
        currentIndex = index;
        DicomPix pix = imageList.get(index);
        imageView.setPixels(Collections.singletonList(pix));
        imageSlider.setValue(index);
        
        updateImageInfo();
    }
    
    private void updateImageInfo() {
        if (currentIndex >= 0 && currentIndex < imageList.size()) {
            DicomPix pix = imageList.get(currentIndex);
            imageInfoLabel.setText(String.format(
                "Image %d of %d | Size: %dx%d",
                currentIndex + 1, imageList.size(),
                pix.getWidth(), pix.getHeight()));
        }
    }
    
    private void updateWindowLevel() {
        try {
            float wl = Float.parseFloat(wlField.getText());
            imageView.setWindowLevel(wl, imageView.getWindowWidth());
        } catch (NumberFormatException e) {
            // エラー処理
        }
    }
    
    private void updateWindowWidth() {
        try {
            float ww = Float.parseFloat(wwField.getText());
            imageView.setWindowLevel(imageView.getWindowLevel(), ww);
        } catch (NumberFormatException e) {
            // エラー処理
        }
    }
}
```

## 4. まとめ

UIコンポーネントのJava実装では：

1. **BrowserController**: データベースブラウザと検索機能
2. **ViewerController**: 画像ビューワーとナビゲーション
3. **Swingコンポーネント**を使用したモダンなUI
4. **非同期処理**によるパフォーマンス最適化

次のステップ: 画像解析機能の実装

