#!/bin/bash
# Weasis PKGファイルからOpenCVネイティブライブラリを抽出するスクリプト（macOS用）

if [ $# -eq 0 ]; then
    echo "Usage: $0 <Weasis-*.pkg>"
    echo "Example: $0 Weasis-4.6.5-aarch64.pkg"
    exit 1
fi

PKG_FILE="$1"

if [ ! -f "$PKG_FILE" ]; then
    echo "ERROR: PKG file not found: $PKG_FILE"
    exit 1
fi

echo "Extracting PKG file: $PKG_FILE"
echo ""

# 一時ディレクトリを作成
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

cd "$TEMP_DIR"

# PKGファイルを展開
echo "Step 1: Extracting PKG file..."
xar -xf "$PKG_FILE" 2>/dev/null || {
    echo "ERROR: Failed to extract PKG file. Make sure xar command is available."
    echo "Install xar with: brew install xar"
    exit 1
}

# Payloadを探す
PAYLOAD_FILE=$(find . -name "Payload" | head -1)

if [ -z "$PAYLOAD_FILE" ]; then
    echo "ERROR: Payload file not found in PKG"
    exit 1
fi

echo "Step 2: Extracting Payload..."
cd "$(dirname "$PAYLOAD_FILE")"
cat Payload | gunzip -dc | cpio -i 2>/dev/null || {
    echo "ERROR: Failed to extract Payload"
    exit 1
}

echo "Step 3: Searching for OpenCV JAR files..."
OPENCV_JAR=$(find . -name "weasis-opencv-core-*-*.jar" | head -1)

if [ -z "$OPENCV_JAR" ]; then
    echo "ERROR: OpenCV JAR file not found in PKG"
    echo "Contents:"
    find . -type f -name "*.jar" | head -10
    exit 1
fi

echo "Found OpenCV JAR: $OPENCV_JAR"
echo ""

# JARファイルを一時ディレクトリにコピー
JAR_TEMP=$(mktemp -d)
cp "$OPENCV_JAR" "$JAR_TEMP/"

cd "$JAR_TEMP"
JAR_FILE=$(basename "$OPENCV_JAR")

echo "Step 4: Extracting JAR file..."
jar -xf "$JAR_FILE" 2>/dev/null || unzip -q "$JAR_FILE" 2>/dev/null || {
    echo "ERROR: Failed to extract JAR file"
    exit 1
}

echo "Step 5: Searching for native library..."
NATIVE_LIB=$(find . -name "libopencv_java.dylib" -o -name "libopencv_java.jnilib" | head -1)

if [ -z "$NATIVE_LIB" ]; then
    echo "ERROR: Native library not found in JAR"
    echo "Contents:"
    find . -type f | head -20
    exit 1
fi

echo "Found native library: $NATIVE_LIB"
echo ""

# ターゲットディレクトリを決定
OS_ARCH=$(uname -m)
if [[ "$OS_ARCH" == "arm64" ]]; then
    TARGET_DIR="../../src/main/resources/native/macosx-arm64"
else
    TARGET_DIR="../../src/main/resources/native/macosx-x86-64"
fi

# ターゲットディレクトリを作成
mkdir -p "$TARGET_DIR"

# ネイティブライブラリをコピー
LIB_NAME=$(basename "$NATIVE_LIB")
cp "$NATIVE_LIB" "$TARGET_DIR/$LIB_NAME"

echo "SUCCESS: Copied $LIB_NAME to $TARGET_DIR"
echo ""
echo "Extraction complete!"
echo "Native library location: $TARGET_DIR/$LIB_NAME"

