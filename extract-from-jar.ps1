# JARファイルからOpenCVネイティブライブラリを抽出するスクリプト（Windows用）

param(
    [Parameter(Mandatory=$true)]
    [string]$JarFile
)

if (-not (Test-Path $JarFile)) {
    Write-Host "ERROR: JAR file not found: $JarFile"
    exit 1
}

Write-Host "Extracting OpenCV native library from: $JarFile"
Write-Host ""

# JARファイル名からプラットフォームを判定
$jarName = Split-Path $JarFile -Leaf
$platform = $null
$target = $null

if ($jarName -match "macosx-aarch64") {
    $platform = "macosx-arm64"
    $target = "macosx-arm64"
    $libPattern = "libopencv_java.dylib", "libopencv_java.jnilib"
} elseif ($jarName -match "macosx-x86-64" -or $jarName -match "macosx-x86_64") {
    $platform = "macosx-x86-64"
    $target = "macosx-x86-64"
    $libPattern = "libopencv_java.dylib", "libopencv_java.jnilib"
} elseif ($jarName -match "windows-x86-64" -or $jarName -match "windows-x86_64") {
    $platform = "windows-x86-64"
    $target = "windows-x86-64"
    $libPattern = "opencv_java.dll"
} elseif ($jarName -match "linux-aarch64") {
    $platform = "linux-arm64"
    $target = "linux-arm64"
    $libPattern = "libopencv_java.so"
} elseif ($jarName -match "linux-x86-64" -or $jarName -match "linux-x86_64") {
    $platform = "linux-x86-64"
    $target = "linux-x86-64"
    $libPattern = "libopencv_java.so"
} else {
    Write-Host "WARNING: Could not determine platform from JAR filename. Searching for all native libraries..."
    $libPattern = "opencv_java.dll", "libopencv_java.dylib", "libopencv_java.jnilib", "libopencv_java.so"
}

# 一時ディレクトリを作成
$tempDir = New-TemporaryFile | ForEach-Object { Remove-Item $_; New-Item -ItemType Directory -Path $_ }

# JARファイルを解凍
Write-Host "Step 1: Extracting JAR file..."
Expand-Archive -Path $JarFile -DestinationPath $tempDir -Force

# ネイティブライブラリを探す
Write-Host "Step 2: Searching for native library..."
$libFiles = Get-ChildItem -Path $tempDir -Include $libPattern -Recurse -ErrorAction SilentlyContinue

if ($libFiles.Count -eq 0) {
    Write-Host "ERROR: Native library not found in JAR file"
    Write-Host "JAR contents:"
    Get-ChildItem -Path $tempDir -Recurse | Select-Object -First 20 | ForEach-Object { Write-Host "  $($_.FullName)" }
    Remove-Item -Recurse -Force $tempDir
    exit 1
}

# ターゲットディレクトリを作成
if ($target) {
    $targetDir = "src\main\resources\native\$target"
} else {
    $targetDir = "src\main\resources\native\unknown"
}

if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
}

# ネイティブライブラリをコピー
foreach ($libFile in $libFiles) {
    $targetFile = Join-Path $targetDir $libFile.Name
    Copy-Item $libFile.FullName $targetFile -Force
    Write-Host "SUCCESS: Copied $($libFile.Name) to $targetFile"
}

# 一時ディレクトリを削除
Remove-Item -Recurse -Force $tempDir

Write-Host ""
Write-Host "Extraction complete!"

