# AI画像解析機能 - Java移植ドキュメント

## 1. 概要

このドキュメントは、DICOMビューワーにAI（人工知能）モデルを使用した画像解析機能を追加するための設計と実装方針をまとめたものです。

HOROS自体には直接的なAI機能は実装されていませんが、ITK（Insight Segmentation and Registration Toolkit）を使用した画像処理機能（`ITKSegmentation3D`）が存在します。本ドキュメントでは、Java実装においてAIモデルを統合する方法を説明します。

## 2. AI画像解析の用途

### 2.1 主要な用途

1. **セグメンテーション**
   - 臓器の自動抽出
   - 病変領域の検出
   - 骨格の抽出

2. **検出（Detection）**
   - 異常所見の検出
   - 病変の位置特定
   - 解剖学的ランドマークの検出

3. **分類（Classification）**
   - 画像の分類（正常/異常）
   - 病変の種類分類
   - 重症度の評価

4. **測定支援**
   - 自動測定（長径、短径、面積、体積）
   - 3D再構成の支援
   - 経時的変化の追跡

5. **画像品質向上**
   - ノイズ除去
   - 解像度向上（Super Resolution）
   - コントラスト強調

## 3. AIモデルの統合方法

### 3.1 モデル形式の選択

**推奨形式**:
- **ONNX (Open Neural Network Exchange)**: クロスプラットフォーム対応
- **TensorFlow Lite**: 軽量でモバイル対応
- **TensorFlow SavedModel**: フル機能のTensorFlowモデル
- **PyTorch TorchScript**: PyTorchモデル

**Java実装での推奨**:
- **ONNX Runtime for Java**: ONNXモデルの実行
- **TensorFlow Java API**: TensorFlowモデルの実行
- **Deep Java Library (DJL)**: 複数のフレームワークに対応

### 3.2 アーキテクチャ設計

```
AI Analysis Module
├── ModelManager - モデルの管理とロード
├── InferenceEngine - 推論エンジン
│   ├── ONNXInferenceEngine
│   ├── TensorFlowInferenceEngine
│   └── DJLInferenceEngine
├── Preprocessor - 前処理（正規化、リサイズ等）
├── Postprocessor - 後処理（結果の可視化、ROI生成等）
└── ResultManager - 結果の管理と表示
```

### 3.3 モデル管理の実装

**Java実装**:
```java
import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;

public class ModelManager {
    private Map<String, InferenceEngine> loadedModels;
    private Path modelDirectory;
    
    public ModelManager(Path modelDirectory) {
        this.modelDirectory = modelDirectory;
        this.loadedModels = new HashMap<>();
    }
    
    /**
     * モデルをロード
     * @param modelName モデル名
     * @param modelType モデルタイプ（ONNX, TensorFlow等）
     * @return 推論エンジン
     */
    public InferenceEngine loadModel(String modelName, ModelType modelType) {
        if (loadedModels.containsKey(modelName)) {
            return loadedModels.get(modelName);
        }
        
        Path modelPath = modelDirectory.resolve(modelName);
        
        InferenceEngine engine;
        switch (modelType) {
            case ONNX:
                engine = new ONNXInferenceEngine(modelPath);
                break;
            case TENSORFLOW:
                engine = new TensorFlowInferenceEngine(modelPath);
                break;
            case DJL:
                engine = new DJLInferenceEngine(modelPath);
                break;
            default:
                throw new IllegalArgumentException("Unsupported model type: " + modelType);
        }
        
        engine.load();
        loadedModels.put(modelName, engine);
        
        return engine;
    }
    
    /**
     * モデルをアンロード
     */
    public void unloadModel(String modelName) {
        InferenceEngine engine = loadedModels.remove(modelName);
        if (engine != null) {
            engine.close();
        }
    }
    
    /**
     * 利用可能なモデルのリストを取得
     */
    public List<ModelInfo> listAvailableModels() {
        List<ModelInfo> models = new ArrayList<>();
        
        // モデルディレクトリをスキャン
        // ...
        
        return models;
    }
}

public enum ModelType {
    ONNX,
    TENSORFLOW,
    DJL
}

public class ModelInfo {
    private String name;
    private ModelType type;
    private String description;
    private String[] inputNames;
    private String[] outputNames;
    private int[][] inputShapes;
    private int[][] outputShapes;
    
    // Getters and setters
}
```

### 3.4 ONNX推論エンジンの実装

**Java実装** (ONNX Runtime for Java):
```java
import ai.onnxruntime.*;
import java.nio.FloatBuffer;
import java.util.Map;

public class ONNXInferenceEngine implements InferenceEngine {
    private OrtEnvironment env;
    private OrtSession session;
    private Path modelPath;
    
    public ONNXInferenceEngine(Path modelPath) {
        this.modelPath = modelPath;
        this.env = OrtEnvironment.getEnvironment();
    }
    
    @Override
    public void load() throws Exception {
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        
        // GPU使用可能な場合はGPUを使用
        try {
            opts.addCUDA();
        } catch (Exception e) {
            // GPUが使用できない場合はCPUを使用
            System.out.println("GPU not available, using CPU");
        }
        
        this.session = env.createSession(modelPath.toString(), opts);
    }
    
    @Override
    public Map<String, float[]> infer(Map<String, float[][]> inputs) throws Exception {
        // 入力テンソルの準備
        Map<String, OnnxTensor> inputTensors = new HashMap<>();
        
        for (Map.Entry<String, float[][]> entry : inputs.entrySet()) {
            String inputName = entry.getKey();
            float[][] inputData = entry.getValue();
            
            // 形状情報の取得
            NodeInfo inputInfo = session.getInputInfo().get(inputName);
            TensorInfo tensorInfo = (TensorInfo) inputInfo.getInfo();
            long[] shape = tensorInfo.getShape();
            
            // テンソルの作成
            OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(flatten(inputData)), shape);
            inputTensors.put(inputName, tensor);
        }
        
        // 推論実行
        try (OrtSession.Result result = session.run(inputTensors)) {
            Map<String, float[]> outputs = new HashMap<>();
            
            for (Map.Entry<String, OnnxValue> entry : result) {
                String outputName = entry.getKey();
                OnnxTensor tensor = (OnnxTensor) entry.getValue();
                float[] outputData = tensor.getFloatBuffer().array();
                outputs.put(outputName, outputData);
            }
            
            return outputs;
        } finally {
            // 入力テンソルの解放
            for (OnnxTensor tensor : inputTensors.values()) {
                tensor.close();
            }
        }
    }
    
    private float[] flatten(float[][] data) {
        int totalSize = 0;
        for (float[] row : data) {
            totalSize += row.length;
        }
        
        float[] flattened = new float[totalSize];
        int index = 0;
        for (float[] row : data) {
            System.arraycopy(row, 0, flattened, index, row.length);
            index += row.length;
        }
        
        return flattened;
    }
    
    @Override
    public void close() throws Exception {
        if (session != null) {
            session.close();
        }
    }
}
```

### 3.5 前処理の実装

**Java実装**:
```java
import java.awt.image.BufferedImage;

public class ImagePreprocessor {
    /**
     * DICOM画像をAIモデルの入力形式に変換
     * @param dicomPix DICOM画像データ
     * @param targetWidth 目標幅
     * @param targetHeight 目標高さ
     * @param normalize 正規化するか
     * @return 前処理済み画像データ
     */
    public float[][] preprocess(DCMPix dicomPix, int targetWidth, int targetHeight, boolean normalize) {
        // 1. リサイズ
        BufferedImage resized = resizeImage(dicomPix, targetWidth, targetHeight);
        
        // 2. グレースケール変換（必要に応じて）
        BufferedImage grayscale = convertToGrayscale(resized);
        
        // 3. ピクセル値の抽出
        float[][] pixels = extractPixels(grayscale);
        
        // 4. 正規化（0-1の範囲に）
        if (normalize) {
            pixels = normalizePixels(pixels, dicomPix.getWindowLevel(), dicomPix.getWindowWidth());
        }
        
        return pixels;
    }
    
    /**
     * ボリュームデータの前処理（3Dモデル用）
     */
    public float[][][] preprocessVolume(VolumeData volumeData, int[] targetShape) {
        // 1. リサンプリング（必要に応じて）
        VolumeData resampled = resampleVolume(volumeData, targetShape);
        
        // 2. 正規化
        float[][][] normalized = normalizeVolume(resampled);
        
        return normalized;
    }
    
    private BufferedImage resizeImage(DCMPix dicomPix, int width, int height) {
        BufferedImage original = dicomPix.toBufferedImage();
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();
        
        return resized;
    }
    
    private float[][] normalizePixels(float[][] pixels, double windowLevel, double windowWidth) {
        float min = (float) (windowLevel - windowWidth / 2.0);
        float max = (float) (windowLevel + windowWidth / 2.0);
        float range = max - min;
        
        float[][] normalized = new float[pixels.length][pixels[0].length];
        
        for (int y = 0; y < pixels.length; y++) {
            for (int x = 0; x < pixels[0].length; x++) {
                float value = pixels[y][x];
                // ウィンドウ範囲内にクリップ
                value = Math.max(min, Math.min(max, value));
                // 0-1に正規化
                normalized[y][x] = (value - min) / range;
            }
        }
        
        return normalized;
    }
}
```

### 3.6 後処理の実装

**Java実装**:
```java
import java.util.List;
import java.util.ArrayList;

public class ResultPostprocessor {
    /**
     * セグメンテーション結果をROIに変換
     * @param segmentationResult セグメンテーション結果（マスク画像）
     * @param originalPix 元のDICOM画像
     * @return ROIのリスト
     */
    public List<ROI> convertSegmentationToROIs(float[][] segmentationResult, DCMPix originalPix) {
        List<ROI> rois = new ArrayList<>();
        
        // 連結成分解析で各領域を抽出
        List<Region> regions = extractRegions(segmentationResult);
        
        for (Region region : regions) {
            // 領域をROIに変換
            ROI roi = regionToROI(region, originalPix);
            rois.add(roi);
        }
        
        return rois;
    }
    
    /**
     * 検出結果をROIに変換
     * @param detectionResult 検出結果（バウンディングボックス等）
     * @param originalPix 元のDICOM画像
     * @return ROIのリスト
     */
    public List<ROI> convertDetectionToROIs(DetectionResult detectionResult, DCMPix originalPix) {
        List<ROI> rois = new ArrayList<>();
        
        for (BoundingBox bbox : detectionResult.getBoundingBoxes()) {
            // バウンディングボックスを矩形ROIに変換
            RectangleROI roi = new RectangleROI(
                bbox.getX(), bbox.getY(),
                bbox.getWidth(), bbox.getHeight()
            );
            roi.setName("AI Detection: " + bbox.getLabel());
            roi.setColor(Color.RED);
            rois.add(roi);
        }
        
        return rois;
    }
    
    /**
     * 分類結果を表示用テキストに変換
     */
    public String formatClassificationResult(ClassificationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Classification Result:\n");
        
        for (Map.Entry<String, Float> entry : result.getProbabilities().entrySet()) {
            sb.append(String.format("  %s: %.2f%%\n", entry.getKey(), entry.getValue() * 100));
        }
        
        return sb.toString();
    }
}

public class DetectionResult {
    private List<BoundingBox> boundingBoxes;
    private List<String> labels;
    private List<Float> confidences;
    
    // Getters and setters
}

public class ClassificationResult {
    private Map<String, Float> probabilities;
    private String predictedClass;
    
    // Getters and setters
}
```

### 3.7 AI解析コントローラーの実装

**Java実装**:
```java
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AIAnalysisController {
    private ModelManager modelManager;
    private ImagePreprocessor preprocessor;
    private ResultPostprocessor postprocessor;
    private ViewerController viewerController;
    
    private JDialog analysisDialog;
    private JComboBox<ModelInfo> modelComboBox;
    private JProgressBar progressBar;
    private JTextArea resultTextArea;
    
    public AIAnalysisController(ViewerController viewerController) {
        this.viewerController = viewerController;
        this.modelManager = new ModelManager(Paths.get("models"));
        this.preprocessor = new ImagePreprocessor();
        this.postprocessor = new ResultPostprocessor();
        
        createAnalysisDialog();
    }
    
    private void createAnalysisDialog() {
        analysisDialog = new JDialog((Frame) null, "AI Image Analysis", true);
        analysisDialog.setLayout(new BorderLayout());
        
        // モデル選択
        JPanel modelPanel = new JPanel(new FlowLayout());
        modelPanel.add(new JLabel("Model:"));
        modelComboBox = new JComboBox<>();
        modelComboBox.setModel(new DefaultComboBoxModel<>(
            modelManager.listAvailableModels().toArray(new ModelInfo[0])));
        modelPanel.add(modelComboBox);
        analysisDialog.add(modelPanel, BorderLayout.NORTH);
        
        // 実行ボタン
        JButton analyzeButton = new JButton("Analyze");
        analyzeButton.addActionListener(this::performAnalysis);
        modelPanel.add(analyzeButton);
        
        // プログレスバー
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        analysisDialog.add(progressBar, BorderLayout.CENTER);
        
        // 結果表示
        resultTextArea = new JTextArea(10, 40);
        resultTextArea.setEditable(false);
        analysisDialog.add(new JScrollPane(resultTextArea), BorderLayout.SOUTH);
        
        analysisDialog.pack();
    }
    
    private void performAnalysis(ActionEvent e) {
        ModelInfo selectedModel = (ModelInfo) modelComboBox.getSelectedItem();
        if (selectedModel == null) {
            JOptionPane.showMessageDialog(analysisDialog, "Please select a model");
            return;
        }
        
        DCMPix currentPix = viewerController.getCurrentPix();
        if (currentPix == null) {
            JOptionPane.showMessageDialog(analysisDialog, "No image loaded");
            return;
        }
        
        // バックグラウンドで解析を実行
        CompletableFuture.supplyAsync(() -> {
            try {
                progressBar.setString("Loading model...");
                InferenceEngine engine = modelManager.loadModel(
                    selectedModel.getName(), selectedModel.getType());
                
                progressBar.setString("Preprocessing...");
                float[][] preprocessed = preprocessor.preprocess(
                    currentPix, 512, 512, true);
                
                progressBar.setString("Running inference...");
                Map<String, float[]> results = engine.infer(
                    Map.of("input", preprocessed));
                
                progressBar.setString("Postprocessing...");
                return postprocessor.processResults(results, selectedModel, currentPix);
                
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(analysisDialog, 
                        "Analysis failed: " + ex.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                });
                return null;
            }
        }).thenAccept(result -> {
            SwingUtilities.invokeLater(() -> {
                if (result != null) {
                    displayResults(result);
                }
                progressBar.setString("Complete");
            });
        });
    }
    
    private void displayResults(AnalysisResult result) {
        resultTextArea.setText(result.getSummary());
        
        // ROIをビューアに追加
        if (result.getRois() != null && !result.getRois().isEmpty()) {
            viewerController.addROIs(result.getRois());
        }
        
        // 結果画像を表示（セグメンテーション結果等）
        if (result.getResultImage() != null) {
            viewerController.displayResultImage(result.getResultImage());
        }
    }
    
    public void showDialog() {
        analysisDialog.setLocationRelativeTo(viewerController.getFrame());
        analysisDialog.setVisible(true);
    }
}
```

## 4. 実装の考慮事項

### 4.1 パフォーマンス

- **GPUアクセラレーション**: CUDA/OpenCL対応の推論エンジンを使用
- **バッチ処理**: 複数画像を一度に処理
- **非同期処理**: UIをブロックしないバックグラウンド処理
- **キャッシング**: 同じ画像の再解析結果をキャッシュ

### 4.2 メモリ管理

- **ストリーミング処理**: 大きな画像を分割して処理
- **モデルの遅延ロード**: 必要な時だけモデルをロード
- **結果の圧縮**: 大きな結果データの圧縮保存

### 4.3 モデルの管理

- **モデルバージョン管理**: モデルのバージョン管理システム
- **モデルの検証**: モデルの整合性チェック
- **自動更新**: モデルの自動更新機能（オプション）

### 4.4 セキュリティとプライバシー

- **データの暗号化**: 送信データの暗号化（クラウド推論の場合）
- **ローカル処理**: 可能な限りローカルで処理
- **データの削除**: 処理後のデータの即座削除

## 5. 推奨ライブラリ

### 5.1 ONNX Runtime

```gradle
dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime:1.16.3")
    // GPU版（オプション）
    // implementation("com.microsoft.onnxruntime:onnxruntime_gpu:1.16.3")
}
```

### 5.2 TensorFlow Java

```gradle
dependencies {
    implementation("org.tensorflow:tensorflow-core-platform:0.5.0")
}
```

### 5.3 Deep Java Library (DJL)

```gradle
dependencies {
    implementation("ai.djl:api:0.24.0")
    implementation("ai.djl:model-zoo:0.24.0")
    implementation("ai.djl.onnxruntime:onnxruntime-engine:0.24.0")
    implementation("ai.djl.pytorch:pytorch-engine:0.24.0")
    implementation("ai.djl.pytorch:pytorch-native-cpu:2.0.1")
}
```

## 6. 実装順序

1. **ModelManagerクラス**: モデルの管理機能
2. **InferenceEngineインターフェース**: 推論エンジンの抽象化
3. **ONNXInferenceEngineクラス**: ONNXモデルの実行
4. **ImagePreprocessorクラス**: 前処理機能
5. **ResultPostprocessorクラス**: 後処理機能
6. **AIAnalysisControllerクラス**: UIコントローラー
7. **モデル統合**: 実際のAIモデルの統合

## 7. 参考資料

- ONNX Runtime: https://onnxruntime.ai/
- TensorFlow Java: https://www.tensorflow.org/jvm
- Deep Java Library: https://djl.ai/
- Medical Image AI: https://www.monai.io/ (Python, 参考)

## 8. 将来の拡張

- **プラグインシステム**: サードパーティモデルの統合
- **クラウド推論**: クラウドベースの推論サービスとの連携
- **モデルトレーニング**: カスタムモデルのトレーニング機能（オプション）
- **結果の可視化**: 3D可視化、アニメーション等

