plugins {
    java
    application
}

group = "com.jj"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// HOROS-20240407準拠: weasis-core-img:4.12.2が利用可能になったため、opencvディレクトリを有効化
// sourceSetsの除外設定は不要（opencv.disabledをopencvに戻したため）

repositories {
    // ローカルMavenリポジトリを最初に指定（優先度を上げる）
    mavenLocal()
    mavenCentral()
    // dcm4che用のリポジトリ
    maven {
        url = uri("https://www.dcm4che.org/maven2/")
    }
    // Weasis用のリポジトリ（GitHub - masterブランチ）
    // weasis-core-imgはこのリポジトリから取得可能
    // https://raw.githubusercontent.com/nroduit/mvn-repo/master/org/weasis/core/weasis-core-img/
    maven {
        url = uri("https://raw.githubusercontent.com/nroduit/mvn-repo/master/")
        metadataSources {
            artifact()
        }
    }
    // Weasis用のリポジトリ（releasesブランチ - フォールバック）
    maven {
        url = uri("https://raw.githubusercontent.com/nroduit/mvn-repo/master/releases")
        metadataSources {
            artifact()
        }
    }
    // JitPack（追加のリポジトリ）
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    // DICOM processing
    // ローカルMavenリポジトリにインストールされたdcm4cheを使用
    // (dcm4che-5.34.1のソースコードからMavenでビルドしてインストール済み)
    implementation("org.dcm4che:dcm4che-core:5.34.1")
    implementation("org.dcm4che:dcm4che-image:5.34.1")
    implementation("org.dcm4che:dcm4che-imageio:5.34.1")
    // HOROS-20240407準拠: RLE Lossless圧縮画像を読み込むために必要
    implementation("org.dcm4che:dcm4che-imageio-rle:5.34.1")
    // JPEG圧縮DICOM画像を読み込むために必要（weasis-core-imgに依存）
    // HOROS-20240407準拠: JPEG圧縮画像（jpeg-cv）を読み込むために必要
    // weasis-core-imgはWeasisのMavenリポジトリ（GitHub）から取得
    // Weasis-4.6.5ではweasis.core.img.version=4.12.2を使用
    // GitHubリポジトリから直接ダウンロード可能: https://raw.githubusercontent.com/nroduit/mvn-repo/master/org/weasis/core/weasis-core-img/4.12.2/weasis-core-img-4.12.2.jar
    implementation("org.weasis.core:weasis-core-img:4.12.2")
    // HOROS-20240407準拠: OpenCVネイティブライブラリ（opencv_java.dll）を含むJAR
    // 注意: JavaFXとは無関係。OpenCVは画像処理ライブラリ（JNI）であり、UIフレームワークではない
    // Weasis-4.6.5ではweasis-opencv-core-windows-x86-64を使用
    // このJARにはopencv_java.dllが含まれており、実行時に自動的にロードされる
    // 注意: このJARはWeasisのMavenリポジトリから取得できないため、コメントアウト
    // 代わりに、実行時にOpenCVネイティブライブラリを手動でロードする方法を実装する必要がある
    // implementation("org.weasis.opencv:weasis-opencv-core-windows-x86-64:4.12.2-dcm")
    // dcm4che-imageio-opencvはweasis-core-imgに依存
    // 注意: ローカルMavenリポジトリにインストールする必要があります
    // または、ソースコードから直接ビルドする必要があります
    // 現時点では、dcm4che-imageio-opencvのソースコードを直接プロジェクトに含める方法を検討
    // implementation("org.dcm4che:dcm4che-imageio-opencv:5.34.1")
    // HOROS-20240407準拠: JPEG2000 Lossless/Lossy圧縮画像を読み込むために必要
    // OpenJPEGを使用（Kakaduの代替）
    // implementation("org.dcm4che:dcm4che-imageio-j2k:5.34.1") // 存在しない場合はOpenJPEGを直接使用
    implementation("org.dcm4che:dcm4che-net:5.34.1")
    
    // SwingX (TreeTable support for HOROS-like outline view)
    implementation("org.swinglabs.swingx:swingx-all:1.6.5-1")
    
    // Database
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // JSON/YAML processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.1")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

application {
    mainClass.set("com.jj.dicomviewer.JJDicomViewerApp")
    applicationDefaultJvmArgs = listOf(
        // Java 9+モジュールシステム: NativeImageReaderがFileImageInputStream.rafにアクセスするために必要
        "--add-opens=java.desktop/javax.imageio.stream=ALL-UNNAMED",
        // Java 9+モジュールシステム: NativeImageReaderがRandomAccessFile.pathにアクセスするために必要
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        // ネイティブアクセスを有効化（Java 9+）
        "--enable-native-access=ALL-UNNAMED"
    )
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}
