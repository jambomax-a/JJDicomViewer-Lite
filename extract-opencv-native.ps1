# OpenCVネイティブライブラリ抽出スクリプト（Windows用）
# HOROS-20240407のWeasis JARからOpenCVネイティブライブラリを抽出します

$horosPath = "horos-20240407\Binaries\weasis-extracted\weasis\bundle"
$targetPath = "src\main\resources\native"

# Windows x86-64版
$jarFile = Join-Path $horosPath "weasis-opencv-core-windows-x86-64-4.2.0-dcm.jar.xz"
$extractedJar = $jarFile -replace "\.xz$", ""
$nativeLib = "opencv_java.dll"
$targetDir = Join-Path $targetPath "windows-x86-64"

Write-Host "Extracting OpenCV native library for Windows x86-64..."

# .xzファイルを解凍（7-Zipまたはxzコマンドが必要）
if (Get-Command 7z -ErrorAction SilentlyContinue) {
    Write-Host "Using 7-Zip to extract .xz file..."
    7z x $jarFile -o(Split-Path $jarFile) | Out-Null
} elseif (Get-Command xz -ErrorAction SilentlyContinue) {
    Write-Host "Using xz to extract .xz file..."
    xz -d $jarFile
} else {
    Write-Host "ERROR: Neither 7z nor xz command found. Please install 7-Zip or xz-utils."
    Write-Host "Alternatively, manually extract the .xz file and run:"
    Write-Host "  jar -xf `"$extractedJar`""
    exit 1
}

if (-not (Test-Path $extractedJar)) {
    Write-Host "ERROR: Failed to extract JAR file: $extractedJar"
    exit 1
}

# 一時ディレクトリを作成
$tempDir = New-TemporaryFile | ForEach-Object { Remove-Item $_; New-Item -ItemType Directory -Path $_ }

# JARファイルを解凍（JARはZIP形式）
Write-Host "Extracting JAR file..."
Expand-Archive -Path $extractedJar -DestinationPath $tempDir -Force

# ネイティブライブラリを探す
$foundLib = $null
$searchPaths = @(
    Join-Path $tempDir "META-INF\native\windows-x86-64\$nativeLib",
    Join-Path $tempDir "META-INF\native\win32-x86-64\$nativeLib",
    Join-Path $tempDir $nativeLib,
    (Get-ChildItem -Path $tempDir -Recurse -Filter $nativeLib -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
)

foreach ($path in $searchPaths) {
    if ($path -and (Test-Path $path)) {
        $foundLib = $path
        break
    }
}

# macOSの場合、.jnilibも探す
if (-not $foundLib -and $nativeLib -like "*.dylib") {
    $jnilibName = $nativeLib -replace "\.dylib$", ".jnilib"
    $jnilibSearchPaths = @(
        Join-Path $tempDir "META-INF\native\macosx-x86-64\$jnilibName",
        Join-Path $tempDir "META-INF\native\macosx-arm64\$jnilibName",
        Join-Path $tempDir $jnilibName,
        (Get-ChildItem -Path $tempDir -Recurse -Filter $jnilibName -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
    )
    
    foreach ($path in $jnilibSearchPaths) {
        if ($path -and (Test-Path $path)) {
            $foundLib = $path
            $nativeLib = $jnilibName
            Write-Host "Found .jnilib instead of .dylib: $jnilibName"
            break
        }
    }
}

if (-not $foundLib) {
    Write-Host "ERROR: Native library not found in JAR file: $extractedJar"
    Write-Host "Searched for: $nativeLib"
    if ($nativeLib -like "*.dylib") {
        Write-Host "Also searched for: $($nativeLib -replace '\.dylib$', '.jnilib')"
    }
    Write-Host "Contents of JAR (native libraries):"
    Get-ChildItem -Path $tempDir -Recurse -Include "*.dylib", "*.jnilib", "*.so", "*.dll" -ErrorAction SilentlyContinue | Select-Object -First 10 | ForEach-Object { Write-Host "  - $($_.FullName)" }
    Remove-Item -Recurse -Force $tempDir
    exit 1
}

# ターゲットディレクトリを作成
if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
}

# ネイティブライブラリをコピー
Copy-Item $foundLib (Join-Path $targetDir $nativeLib) -Force
Write-Host "SUCCESS: Copied $nativeLib to $targetDir"

# 一時ディレクトリを削除
Remove-Item -Recurse -Force $tempDir

Write-Host ""
Write-Host "Extraction complete!"
Write-Host "Native library location: $(Join-Path $targetDir $nativeLib)"

