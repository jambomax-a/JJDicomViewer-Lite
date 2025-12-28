#!/bin/bash
# OpenCVネイティブライブラリ抽出スクリプト（macOS/Linux用）
# HOROS-20240407のWeasis JARからOpenCVネイティブライブラリを抽出します

HOROS_PATH="horos-20240407/Binaries/weasis-extracted/weasis/bundle"
TARGET_PATH="src/main/resources/native"

# プラットフォームを検出
OS_NAME=$(uname -s | tr '[:upper:]' '[:lower:]')
ARCH=$(uname -m)

echo "Detected OS: $OS_NAME, Architecture: $ARCH"

# プラットフォーム別の設定
if [[ "$OS_NAME" == "darwin" ]]; then
    # macOS
    if [[ "$ARCH" == "arm64" || "$ARCH" == "aarch64" ]]; then
        JAR_FILE="${HOROS_PATH}/weasis-opencv-core-macosx-aarch64-4.2.0-dcm.jar.xz"
        NATIVE_LIB="libopencv_java.dylib"
        TARGET_DIR="${TARGET_PATH}/macosx-arm64"
    else
        JAR_FILE="${HOROS_PATH}/weasis-opencv-core-macosx-x86-64-4.2.0-dcm.jar.xz"
        NATIVE_LIB="libopencv_java.dylib"
        TARGET_DIR="${TARGET_PATH}/macosx-x86-64"
    fi
elif [[ "$OS_NAME" == "linux" ]]; then
    # Linux
    if [[ "$ARCH" == "x86_64" ]]; then
        JAR_FILE="${HOROS_PATH}/weasis-opencv-core-linux-x86-64-4.2.0-dcm.jar.xz"
        NATIVE_LIB="libopencv_java.so"
        TARGET_DIR="${TARGET_PATH}/linux-x86-64"
    elif [[ "$ARCH" == "i386" || "$ARCH" == "i686" ]]; then
        JAR_FILE="${HOROS_PATH}/weasis-opencv-core-linux-x86-4.2.0-dcm.jar.xz"
        NATIVE_LIB="libopencv_java.so"
        TARGET_DIR="${TARGET_PATH}/linux-x86-64"  # 32bit版はx86-64ディレクトリに配置
    else
        echo "ERROR: Unsupported Linux architecture: $ARCH"
        exit 1
    fi
else
    echo "ERROR: Unsupported operating system: $OS_NAME"
    exit 1
fi

EXTRACTED_JAR="${JAR_FILE%.xz}"

echo "Extracting OpenCV native library..."
echo "Source JAR: $JAR_FILE"
echo "Target directory: $TARGET_DIR"

# .xzファイルを解凍
if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: JAR file not found: $JAR_FILE"
    exit 1
fi

if command -v xz &> /dev/null; then
    echo "Using xz to extract .xz file..."
    xz -d "$JAR_FILE"
elif command -v unxz &> /dev/null; then
    echo "Using unxz to extract .xz file..."
    unxz "$JAR_FILE"
else
    echo "ERROR: xz or unxz command not found. Please install xz-utils."
    exit 1
fi

if [ ! -f "$EXTRACTED_JAR" ]; then
    echo "ERROR: Failed to extract JAR file: $EXTRACTED_JAR"
    exit 1
fi

# 一時ディレクトリを作成
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# JARファイルを解凍（JARはZIP形式）
echo "Extracting JAR file..."
unzip -q "$EXTRACTED_JAR" -d "$TEMP_DIR"

# ネイティブライブラリを探す（.dylibと.jnilibの両方を試す）
FOUND_LIB=$(find "$TEMP_DIR" -name "$NATIVE_LIB" | head -1)

# macOSの場合、.jnilibも探す
if [[ -z "$FOUND_LIB" && "$OS_NAME" == "darwin" ]]; then
    JNILIB_NAME="${NATIVE_LIB%.dylib}.jnilib"
    FOUND_LIB=$(find "$TEMP_DIR" -name "$JNILIB_NAME" | head -1)
    if [ -n "$FOUND_LIB" ]; then
        NATIVE_LIB="$JNILIB_NAME"
        echo "Found .jnilib instead of .dylib: $JNILIB_NAME"
    fi
fi

if [ -z "$FOUND_LIB" ]; then
    echo "ERROR: Native library not found in JAR file: $EXTRACTED_JAR"
    echo "Searched for: $NATIVE_LIB"
    if [[ "$OS_NAME" == "darwin" ]]; then
        echo "Also searched for: ${NATIVE_LIB%.dylib}.jnilib"
    fi
    echo "Contents of JAR:"
    find "$TEMP_DIR" -type f -name "*.dylib" -o -name "*.jnilib" -o -name "*.so" -o -name "*.dll" | head -10
    exit 1
fi

# ターゲットディレクトリを作成
mkdir -p "$TARGET_DIR"

# ネイティブライブラリをコピー
cp "$FOUND_LIB" "$TARGET_DIR/$NATIVE_LIB"
echo "SUCCESS: Copied $NATIVE_LIB to $TARGET_DIR"

echo ""
echo "Extraction complete!"
echo "Native library location: $TARGET_DIR/$NATIVE_LIB"

