#!/bin/bash
# weasis-native.zipからOpenCVネイティブライブラリを抽出するスクリプト（macOS/Linux用）

ZIP_FILE="${1:-weasis-native.zip}"

if [ ! -f "$ZIP_FILE" ]; then
    echo "ERROR: ZIP file not found: $ZIP_FILE"
    echo "Usage: $0 [weasis-native.zip]"
    exit 1
fi

echo "Extracting OpenCV native libraries from: $ZIP_FILE"
echo ""

# 一時ディレクトリを作成
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# ZIPファイルを解凍
echo "Step 1: Extracting ZIP file..."
unzip -q "$ZIP_FILE" -d "$TEMP_DIR"

# プラットフォーム別のディレクトリを探す
declare -A PLATFORMS=(
    ["macosx-aarch64"]="macosx-arm64"
    ["macosx-x86-64"]="macosx-x86-64"
    ["windows-x86-64"]="windows-x86-64"
    ["linux-x86-64"]="linux-x86-64"
    ["linux-aarch64"]="linux-arm64"
)

# JARファイルからも抽出を試みる（bin-dist/weasis/bundle/ ディレクトリ構造の場合）
JAR_SEARCH_PATHS=(
    "weasis-native/bin-dist/weasis/bundle"
    "bin-dist/weasis/bundle"
    "weasis/bundle"
    "bundle"
)

FOUND_ANY=false

for platform_dir in "${!PLATFORMS[@]}"; do
    target_dir="${PLATFORMS[$platform_dir]}"
    source_dir="$TEMP_DIR/weasis-native/$platform_dir"
    
    # 別の構造を試す
    if [ ! -d "$source_dir" ]; then
        source_dir="$TEMP_DIR/$platform_dir"
    fi
    
    if [ -d "$source_dir" ]; then
        echo "Step 2: Processing $platform_dir..."
        
        # ネイティブライブラリを探す
        LIB_FILES=$(find "$source_dir" -type f \( -name "opencv_java.dll" -o -name "libopencv_java.dylib" -o -name "libopencv_java.jnilib" -o -name "libopencv_java.so" \) 2>/dev/null)
        
        if [ -n "$LIB_FILES" ]; then
            TARGET_DIR="src/main/resources/native/$target_dir"
            
            # ターゲットディレクトリを作成
            mkdir -p "$TARGET_DIR"
            
            echo "$LIB_FILES" | while read -r lib_file; do
                if [ -f "$lib_file" ]; then
                    LIB_NAME=$(basename "$lib_file")
                    cp "$lib_file" "$TARGET_DIR/$LIB_NAME"
                    echo "  Copied: $LIB_NAME -> $TARGET_DIR/$LIB_NAME"
                    FOUND_ANY=true
                fi
            done
        else
            echo "  No native library found in $platform_dir"
        fi
    else
        echo "  Directory not found: $platform_dir"
    fi
done

# JARファイルからも抽出を試みる
echo ""
echo "Step 3: Searching for OpenCV JAR files..."
for jar_search_path in "${JAR_SEARCH_PATHS[@]}"; do
    jar_dir="$TEMP_DIR/$jar_search_path"
    if [ -d "$jar_dir" ]; then
        while IFS= read -r jar_file; do
            if [ -f "$jar_file" ]; then
                echo "  Found JAR: $(basename "$jar_file")"
                
                # JARファイル名からプラットフォームを判定
                jar_name=$(basename "$jar_file")
                platform=""
                target=""
                
                if echo "$jar_name" | grep -q "macosx-aarch64"; then
                    platform="macosx-arm64"
                    target="macosx-arm64"
                elif echo "$jar_name" | grep -qE "macosx-x86-64|macosx-x86_64"; then
                    platform="macosx-x86-64"
                    target="macosx-x86-64"
                elif echo "$jar_name" | grep -qE "windows-x86-64|windows-x86_64"; then
                    platform="windows-x86-64"
                    target="windows-x86-64"
                elif echo "$jar_name" | grep -q "linux-aarch64"; then
                    platform="linux-arm64"
                    target="linux-arm64"
                elif echo "$jar_name" | grep -qE "linux-x86-64|linux-x86_64"; then
                    platform="linux-x86-64"
                    target="linux-x86-64"
                fi
                
                if [ -n "$platform" ] && [ -n "$target" ]; then
                    jar_temp_dir=$(mktemp -d)
                    trap "rm -rf $jar_temp_dir" EXIT
                    
                    unzip -q "$jar_file" -d "$jar_temp_dir" 2>/dev/null || jar -xf "$jar_file" -C "$jar_temp_dir" 2>/dev/null
                    
                    lib_files=$(find "$jar_temp_dir" -type f \( -name "opencv_java.dll" -o -name "libopencv_java.dylib" -o -name "libopencv_java.jnilib" -o -name "libopencv_java.so" \) 2>/dev/null)
                    
                    if [ -n "$lib_files" ]; then
                        target_dir="src/main/resources/native/$target"
                        mkdir -p "$target_dir"
                        
                        echo "$lib_files" | while read -r lib_file; do
                            if [ -f "$lib_file" ]; then
                                lib_name=$(basename "$lib_file")
                                cp "$lib_file" "$target_dir/$lib_name"
                                echo "    Extracted: $lib_name -> $target_dir/$lib_name"
                                FOUND_ANY=true
                            fi
                        done
                    fi
                    
                    rm -rf "$jar_temp_dir"
                fi
            fi
        done < <(find "$jar_dir" -name "weasis-opencv-core-*-*.jar" 2>/dev/null)
        break
    fi
done

if [ "$FOUND_ANY" = false ]; then
    echo ""
    echo "ERROR: No native libraries found in ZIP file"
    echo "Contents of ZIP:"
    find "$TEMP_DIR" -type f | head -20 | while read -r file; do
        echo "  $file"
    done
    exit 1
fi

echo ""
echo "SUCCESS: Extraction complete!"
echo ""
echo "Copied libraries:"
find "src/main/resources/native" -type f \( -name "*.dll" -o -name "*.dylib" -o -name "*.jnilib" -o -name "*.so" \) 2>/dev/null | while read -r file; do
    echo "  $file"
done

