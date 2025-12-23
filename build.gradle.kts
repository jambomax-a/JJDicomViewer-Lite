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

repositories {
    // ローカルMavenリポジトリを最初に指定（優先度を上げる）
    mavenLocal()
    mavenCentral()
    // dcm4che用のリポジトリ
    maven {
        url = uri("https://www.dcm4che.org/maven2/")
    }
    // Weasis用のリポジトリ（GitHub）
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
