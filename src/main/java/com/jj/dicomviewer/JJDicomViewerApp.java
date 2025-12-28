package com.jj.dicomviewer;

import com.jj.dicomviewer.ui.BrowserController;

import javax.imageio.ImageIO;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.IIORegistry;
import javax.swing.SwingUtilities;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

// dcm4che3のJPEG圧縮DICOM画像サポート
// 注意: dcm4che-imageio-opencvモジュールが必要（weasis-core-imgに依存）

/**
 * JJDicomViewerApp - HOROS-20240407準拠のメインアプリケーションクラス
 * 
 * HOROS-20240407のAppControllerの起動処理を参考に実装
 */
public class JJDicomViewerApp {
    
    /**
     * OpenCVネイティブライブラリをロード（プラットフォーム別）
     * HOROS-20240407準拠: OpenCVのpre-builtバイナリを使用（C++ソースのコンパイルは不要）
     * プラットフォーム別のライブラリ名:
     * - Windows: opencv_java.dll
     * - macOS: libopencv_java.dylib
     * - Linux: libopencv_java.so
     * 
     * 注意: ネイティブライブラリは以下のいずれかの方法で取得可能:
     * 1. OpenCV公式リリースからpre-builtバイナリをダウンロード（macOS/Linuxはソースからビルドが必要な場合がある）
     * 2. Weasisのバイナリリリースから抽出
     * 3. resourcesフォルダに配置して、実行時にJARから抽出してロード
     */
    private static void loadOpenCVNativeLibrary() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String osArch = System.getProperty("os.arch", "").toLowerCase();
        String libraryName;
        String libraryFileName;
        String resourcePath;
        String opencvPath;
        
        // プラットフォーム別のライブラリ名を決定
        if (osName.contains("win")) {
            // Windows
            libraryName = "opencv_java";
            libraryFileName = "opencv_java.dll";
            resourcePath = "/native/windows-x86-64/opencv_java.dll";
            opencvPath = "opencv/build/java/x64/opencv_javaXXX.dll";
        } else if (osName.contains("mac")) {
            // macOS
            libraryName = "opencv_java";
            if (osArch.contains("aarch64") || osArch.contains("arm64")) {
                libraryFileName = "libopencv_java.dylib";
                resourcePath = "/native/macosx-arm64/libopencv_java.dylib";
                opencvPath = "opencv/build/java/arm64/libopencv_javaXXX.dylib";
            } else {
                libraryFileName = "libopencv_java.dylib";
                resourcePath = "/native/macosx-x86-64/libopencv_java.dylib";
                opencvPath = "opencv/build/java/x86_64/libopencv_javaXXX.dylib";
            }
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            // Linux/Unix
            libraryName = "opencv_java";
            if (osArch.contains("aarch64") || osArch.contains("arm64")) {
                libraryFileName = "libopencv_java.so";
                resourcePath = "/native/linux-arm64/libopencv_java.so";
                opencvPath = "opencv/build/java/arm64/libopencv_javaXXX.so";
            } else {
                libraryFileName = "libopencv_java.so";
                resourcePath = "/native/linux-x86-64/libopencv_java.so";
                opencvPath = "opencv/build/java/x64/libopencv_javaXXX.so";
            }
        } else {
            System.err.println("WARNING: Unsupported operating system: " + osName);
            System.err.println("WARNING: OpenCV native library may not be available.");
            return;
        }
        
        try {
            // まず、システムのjava.library.pathからOpenCVネイティブライブラリを探す
            String libraryPath = System.getProperty("java.library.path");
            String[] paths = libraryPath.split(File.pathSeparator);
            for (String path : paths) {
                File libFile = new File(path, libraryFileName);
                if (libFile.exists()) {
                    System.loadLibrary(libraryName);
                    System.out.println("INFO: OpenCV native library loaded from: " + libFile.getAbsolutePath());
                    return;
                }
            }
            
            // java.library.pathに見つからない場合、resourcesフォルダからJARに含まれたライブラリを抽出してロード
            // macOSでは.dylibと.jnilibの両方を試す
            String[] resourcePathsToTry = {resourcePath};
            if (osName.contains("mac")) {
                // macOSの場合、.dylibと.jnilibの両方を試す
                String jnilibResourcePath = resourcePath.replace(".dylib", ".jnilib");
                resourcePathsToTry = new String[]{resourcePath, jnilibResourcePath};
            }
            
            for (String tryResourcePath : resourcePathsToTry) {
                try {
                    InputStream libStream = JJDicomViewerApp.class.getResourceAsStream(tryResourcePath);
                    if (libStream != null) {
                        // 一時ファイルに抽出
                        Path tempDir = Files.createTempDirectory("opencv-native-");
                        String actualFileName = tryResourcePath.substring(tryResourcePath.lastIndexOf('/') + 1);
                        Path tempLib = tempDir.resolve(actualFileName);
                        Files.copy(libStream, tempLib, StandardCopyOption.REPLACE_EXISTING);
                        libStream.close();
                        
                        // 一時ファイルからロード
                        System.load(tempLib.toAbsolutePath().toString());
                        System.out.println("INFO: OpenCV native library loaded from JAR resources: " + tryResourcePath);
                        // 注意: 一時ファイルはアプリケーション終了時に削除される
                        tempLib.toFile().deleteOnExit();
                        tempDir.toFile().deleteOnExit();
                        return;
                    }
                } catch (Exception e) {
                    // 次のパスを試す
                }
            }
            
            if (osName.contains("mac")) {
                System.out.println("INFO: OpenCV native library not found in JAR resources (tried .dylib and .jnilib)");
                if (osArch.contains("aarch64") || osArch.contains("arm64")) {
                    System.out.println("INFO: Note: ARM64 native library may not be available in HOROS-20240407 Weasis JAR.");
                    System.out.println("INFO:       You can use x86_64 version with Rosetta 2, or build from source.");
                }
            } else {
                System.out.println("INFO: OpenCV native library not found in JAR resources: " + resourcePath);
            }
            
            // System.loadLibraryを試みる（システムの標準的なライブラリ検索パスからも検索）
            try {
                System.loadLibrary(libraryName);
                System.out.println("INFO: OpenCV native library loaded via System.loadLibrary(\"" + libraryName + "\")");
                return;
            } catch (UnsatisfiedLinkError e) {
                // System.loadLibraryも失敗した場合、手動配置が必要
            }
            
            // すべての方法で見つからない場合
            System.out.println("INFO: OpenCV native library (" + libraryFileName + ") not found.");
            if (osName.contains("mac")) {
                System.out.println("INFO: Note: macOS also supports .jnilib format (libopencv_java.jnilib)");
            }
            System.out.println("INFO: To enable JPEG compressed DICOM image support, please:");
            System.out.println("INFO: 1. Place " + libraryFileName + " in src/main/resources" + resourcePath);
            if (osName.contains("mac")) {
                System.out.println("INFO:    Or place libopencv_java.jnilib in src/main/resources" + resourcePath.replace(".dylib", ".jnilib"));
            }
            System.out.println("INFO:    (The library will be automatically extracted from JAR at runtime)");
            System.out.println("INFO: 2. Or download OpenCV and extract " + libraryFileName + " from " + opencvPath);
            System.out.println("INFO: 3. Or place " + libraryFileName + " in a directory in java.library.path");
            System.out.println("INFO:    or set -Djava.library.path=<path-to-" + libraryFileName + ">");
            if (osName.contains("nix") || osName.contains("nux")) {
                System.out.println("INFO:    or set LD_LIBRARY_PATH environment variable");
            } else if (osName.contains("mac")) {
                System.out.println("INFO:    or set DYLD_LIBRARY_PATH environment variable");
            }
            System.out.println("INFO: Note: OpenCV pre-built binaries for macOS/Linux may require building from source.");
            System.out.println("INFO:       Alternatively, extract from Weasis binary release or use package manager.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("WARNING: Failed to load OpenCV native library (" + libraryFileName + "): " + e.getMessage());
            System.err.println("WARNING: JPEG compressed DICOM images may not be readable without OpenCV native library.");
        } catch (Exception e) {
            System.err.println("WARNING: Error loading OpenCV native library: " + e.getMessage());
        }
    }
    
    /**
     * ImageIOプラグインを初期化
     * HOROS-20240407準拠: すべての圧縮フォーマットをサポート
     * - JPEG Baseline (1.2.840.10008.1.2.4.50)
     * - JPEG Extended (1.2.840.10008.1.2.4.51)
     * - JPEG Lossless (1.2.840.10008.1.2.4.70)
     * - JPEG Lossless 14 (1.2.840.10008.1.2.4.57)
     * - JPEG2000 Lossless (1.2.840.10008.1.2.4.90)
     * - JPEG2000 Lossy (1.2.840.10008.1.2.4.91)
     * - JPEG-LS Lossless (1.2.840.10008.1.2.4.80)
     * - JPEG-LS Lossy (1.2.840.10008.1.2.4.81)
     * - RLE Lossless (1.2.840.10008.1.2.5)
     * - JPEG (jpeg-cv) - dcm4che-imageio-opencv
     */
    private static void initializeImageIO() {
        try {
            // ImageIOプラグインをスキャン
            ImageIO.scanForPlugins();
            
            IIORegistry registry = IIORegistry.getDefaultInstance();
            
            // HOROS-20240407準拠: DicomImageReaderSpiを登録（JPEG Baseline, JPEG Lossless, JPEG-LSをサポート）
            try {
                org.dcm4che3.imageio.plugins.dcm.DicomImageReaderSpi dicomSpi = new org.dcm4che3.imageio.plugins.dcm.DicomImageReaderSpi();
                registry.registerServiceProvider(dicomSpi, ImageReaderSpi.class);
                System.out.println("INFO: DicomImageReaderSpi registered successfully (JPEG Baseline, JPEG Lossless, JPEG-LS support)");
            } catch (Exception e) {
                System.err.println("WARNING: Failed to register DicomImageReaderSpi: " + e.getMessage());
            }
            
            // HOROS-20240407準拠: NativeJPEGImageReaderSpiを登録（JPEG圧縮画像 jpeg-cvをサポート）
            // weasis-core-img:4.12.2がGitHubリポジトリから利用可能
            // 注意: OpenCVネイティブライブラリが利用できない場合、NativeImageReaderは失敗する
            // その場合、DicomImageReaderはJava実装のJPEGデコンプレッサーを使用する
            // しかし、NativeJPEGImageReaderSpiを登録すると、DicomImageReaderがNativeImageReaderを優先的に使用しようとする
            // そのため、OpenCVネイティブライブラリが利用できない場合は、NativeJPEGImageReaderSpiを登録しない
            // これにより、DicomImageReaderはJava実装のJPEGデコンプレッサーを使用する
            boolean opencvAvailable = false;
            try {
                // OpenCVネイティブライブラリが利用可能か確認
                // NativeImageReaderが使用できるかテスト
                Class.forName("org.dcm4che3.opencv.NativeImageReader");
                // ネイティブライブラリのロードを試みる（実際には使用時にロードされる）
                // ここでは、クラスが存在するかどうかのみ確認
                opencvAvailable = true;
            } catch (ClassNotFoundException e) {
                System.out.println("INFO: NativeImageReader not available. Will use Java-based JPEG decompressor.");
                opencvAvailable = false;
            }
            
            if (opencvAvailable) {
                try {
                    org.dcm4che3.opencv.NativeJPEGImageReaderSpi jpegCvSpi = new org.dcm4che3.opencv.NativeJPEGImageReaderSpi();
                    registry.registerServiceProvider(jpegCvSpi, ImageReaderSpi.class);
                    System.out.println("INFO: NativeJPEGImageReaderSpi registered successfully (JPEG compressed DICOM images - jpeg-cv)");
                    System.out.println("INFO: Note: OpenCV native library must be available at runtime for NativeImageReader to work.");
                } catch (NoClassDefFoundError e) {
                    System.err.println("WARNING: weasis-core-img not found. JPEG compressed DICOM images (jpeg-cv) may not be readable.");
                    System.err.println("WARNING: Will fall back to Java-based JPEG decompressor.");
                } catch (Exception e) {
                    System.err.println("WARNING: Failed to register NativeJPEGImageReaderSpi: " + e.getMessage());
                    System.err.println("WARNING: Will fall back to Java-based JPEG decompressor.");
                }
            } else {
                System.out.println("INFO: NativeJPEGImageReaderSpi not registered. Will use Java-based JPEG decompressor for JPEG compressed DICOM images.");
            }
            
            // HOROS-20240407準拠: RLE Lossless ImageReaderSpiを登録
            try {
                // dcm4che-imageio-rleのImageReaderSpiを登録
                // 注意: dcm4che-imageio-rleモジュールが必要
                Class<?> rleSpiClass = Class.forName("org.dcm4che3.imageio.plugins.rle.RLEImageReaderSpi");
                ImageReaderSpi rleSpi = (ImageReaderSpi) rleSpiClass.getDeclaredConstructor().newInstance();
                registry.registerServiceProvider(rleSpi, ImageReaderSpi.class);
                System.out.println("INFO: RLEImageReaderSpi registered successfully (RLE Lossless support)");
            } catch (ClassNotFoundException e) {
                System.err.println("WARNING: dcm4che-imageio-rle not found. RLE Lossless DICOM images may not be readable.");
            } catch (Exception e) {
                System.err.println("WARNING: Failed to register RLEImageReaderSpi: " + e.getMessage());
            }
            
            // HOROS-20240407準拠: JPEG2000 ImageReaderSpiを登録（OpenJPEGまたはKakadu）
            // 注意: JPEG2000のサポートには追加のライブラリが必要
            // dcm4che-imageio-j2kは存在しない可能性があるため、OpenJPEGを直接使用する必要がある
            // 現時点では、dcm4che-imageioがJPEG2000をサポートしているか確認
            try {
                // JPEG2000のImageReaderSpiを探す
                Iterator<ImageReaderSpi> readerSpis = registry.getServiceProviders(ImageReaderSpi.class, true);
                boolean jpeg2000ReaderFound = false;
                while (readerSpis.hasNext()) {
                    ImageReaderSpi spi = readerSpis.next();
                    String[] formatNames = spi.getFormatNames();
                    if (formatNames != null) {
                        for (String formatName : formatNames) {
                            if ("jpeg2000".equalsIgnoreCase(formatName) || "jp2".equalsIgnoreCase(formatName) || "j2k".equalsIgnoreCase(formatName)) {
                                jpeg2000ReaderFound = true;
                                System.out.println("INFO: JPEG2000 ImageReader found: " + spi.getClass().getName());
                                break;
                            }
                        }
                    }
                    if (jpeg2000ReaderFound) {
                        break;
                    }
                }
                if (!jpeg2000ReaderFound) {
                    System.err.println("WARNING: JPEG2000 ImageReader not found. JPEG2000 Lossless/Lossy DICOM images may not be readable.");
                    System.err.println("WARNING: To enable JPEG2000 support, add OpenJPEG or Kakadu library.");
                }
            } catch (Exception e) {
                System.err.println("WARNING: Failed to check JPEG2000 ImageReader: " + e.getMessage());
            }
            
                   // 登録されたImageReaderを確認
                   System.out.println("INFO: ImageIO plugins initialized. Supported formats:");
                   Iterator<ImageReaderSpi> readerSpis = registry.getServiceProviders(ImageReaderSpi.class, true);
                   while (readerSpis.hasNext()) {
                       ImageReaderSpi spi = readerSpis.next();
                       String[] formatNames = spi.getFormatNames();
                       if (formatNames != null && formatNames.length > 0) {
                           System.out.println("  - " + String.join(", ", formatNames) + " (" + spi.getClass().getName() + ")");
                       }
                   }
                   
                   // OpenCVネイティブライブラリのロード状態を確認
                   try {
                       Class<?> nativeImageReaderClass = Class.forName("org.dcm4che3.opencv.NativeImageReader");
                       System.out.println("INFO: NativeImageReader class found: " + nativeImageReaderClass.getName());
                       
                       // ネイティブライブラリがロードされているか確認
                       try {
                           java.lang.reflect.Method loadMethod = nativeImageReaderClass.getMethod("isAvailable");
                           Object isAvailable = loadMethod.invoke(null);
                           System.out.println("INFO: NativeImageReader.isAvailable() = " + isAvailable);
                       } catch (Exception e) {
                           System.out.println("INFO: Could not check NativeImageReader availability: " + e.getMessage());
                       }
                   } catch (ClassNotFoundException e) {
                       System.out.println("WARNING: NativeImageReader class not found. JPEG compressed DICOM images may not be readable.");
                   }
        } catch (Exception e) {
            System.err.println("WARNING: Failed to initialize ImageIO plugins: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * メインメソッド
     * HOROS-20240407準拠でBrowserControllerを起動
     */
    public static void main(String[] args) {
        // OpenCVネイティブライブラリをロード（JPEG圧縮DICOM画像を読み込むために必要）
        // 注意: opencv_java.dllはC++ソースをコンパイルする必要はなく、pre-builtバイナリを使用可能
        loadOpenCVNativeLibrary();
        
        // ImageIOプラグインを初期化（JPEG圧縮DICOM画像を読み込むために必要）
        initializeImageIO();
        
        // 未処理例外ハンドラーを設定（スタックトレースを抑制）
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            // NullPointerExceptionの場合はスタックトレースを表示（デバッグ用）
            if (e instanceof NullPointerException) {
                System.err.println("NullPointerException in thread: " + t.getName());
                e.printStackTrace();
                return;
            }
            // ConcurrentModificationException、ArrayIndexOutOfBoundsExceptionは抑制
            if (e instanceof java.util.ConcurrentModificationException ||
                e instanceof ArrayIndexOutOfBoundsException) {
                // ログを抑制（重要なログが見やすくなるように）
                return;
            }
            // その他の例外はログ出力
            System.err.println("Uncaught exception in thread: " + t.getName());
            e.printStackTrace();
        });
        
        // Swingイベントディスパッチスレッドで実行
        SwingUtilities.invokeLater(() -> {
            try {
                // HOROS-20240407準拠：BrowserControllerを起動
                // コンストラクタ内でinitializeUIComponents()が呼び出され、
                // その中でawakeFromNib()が呼び出されるため、ここでは呼び出す必要がない
                BrowserController browser = new BrowserController();
                
                // ウィンドウを表示
                // componentShownイベントが発生するまで待機するため、
                // setVisible(true)を呼び出した後、componentShownイベントが発生するまで待機
                if (!browser.isVisible()) {
                    browser.setVisible(true);
                    // componentShownイベントが発生するまで待機（初期化が完了するまで）
                    // ただし、これは非同期処理のため、実際にはcomponentShownイベント内で
                    // uiInitializedフラグが設定されるまで待機する必要がある
                }
                
            } catch (Exception e) {
                // デバッグログを出力（エラー原因を特定するため）
                System.err.println("Failed to start JJDicomViewer: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}

