# weasis-native.zipからOpenCVネイティブライブラリを抽出するスクリプト（Windows用）

param(
    [Parameter(Mandatory=$true)]
    [string]$ZipFile = "weasis-native.zip"
)

if (-not (Test-Path $ZipFile)) {
    Write-Host "ERROR: ZIP file not found: $ZipFile"
    exit 1
}

Write-Host "Extracting OpenCV native libraries from: $ZipFile"
Write-Host ""

# 一時ディレクトリを作成
$tempDir = New-TemporaryFile | ForEach-Object { Remove-Item $_; New-Item -ItemType Directory -Path $_ }

# ZIPファイルを解凍
Write-Host "Step 1: Extracting ZIP file..."
Expand-Archive -Path $ZipFile -DestinationPath $tempDir -Force

# プラットフォーム別のディレクトリを探す
$platformDirs = @(
    @{Name="macosx-aarch64"; Target="macosx-arm64"},
    @{Name="macosx-x86-64"; Target="macosx-x86-64"},
    @{Name="windows-x86-64"; Target="windows-x86-64"},
    @{Name="linux-x86-64"; Target="linux-x86-64"},
    @{Name="linux-aarch64"; Target="linux-arm64"}
)

# JARファイルからも抽出を試みる（bin-dist/weasis/bundle/ ディレクトリ構造の場合）
$jarSearchPaths = @(
    "weasis-native\bin-dist\weasis\bundle",
    "bin-dist\weasis\bundle",
    "weasis\bundle",
    "bundle"
)

$foundAny = $false

foreach ($platform in $platformDirs) {
    $sourceDir = Join-Path $tempDir "weasis-native\$($platform.Name)"
    
    if (-not (Test-Path $sourceDir)) {
        # 別の構造を試す
        $sourceDir = Join-Path $tempDir $platform.Name
    }
    
    if (Test-Path $sourceDir) {
        Write-Host "Step 2: Processing $($platform.Name)..."
        
        # ネイティブライブラリを探す
        $libFiles = Get-ChildItem -Path $sourceDir -Include "opencv_java.dll", "libopencv_java.dylib", "libopencv_java.jnilib", "libopencv_java.so" -Recurse -ErrorAction SilentlyContinue
        
        if ($libFiles.Count -gt 0) {
            $targetDir = "src\main\resources\native\$($platform.Target)"
            
            # ターゲットディレクトリを作成
            if (-not (Test-Path $targetDir)) {
                New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
            }
            
            foreach ($libFile in $libFiles) {
                $targetFile = Join-Path $targetDir $libFile.Name
                Copy-Item $libFile.FullName $targetFile -Force
                Write-Host "  Copied: $($libFile.Name) -> $targetFile"
                $foundAny = $true
            }
        } else {
            Write-Host "  No native library found in $($platform.Name)"
        }
    } else {
        Write-Host "  Directory not found: $($platform.Name)"
    }
}

# JARファイルからも抽出を試みる
Write-Host ""
Write-Host "Step 3: Searching for OpenCV JAR files..."
foreach ($jarSearchPath in $jarSearchPaths) {
    $jarDir = Join-Path $tempDir $jarSearchPath
    if (Test-Path $jarDir) {
        $jarFiles = Get-ChildItem -Path $jarDir -Filter "weasis-opencv-core-*-*.jar" -ErrorAction SilentlyContinue
        foreach ($jarFile in $jarFiles) {
            Write-Host "  Found JAR: $($jarFile.Name)"
            
            # JARファイル名からプラットフォームを判定
            $jarName = $jarFile.Name
            $platform = $null
            $target = $null
            
            if ($jarName -match "macosx-aarch64") {
                $platform = "macosx-arm64"
                $target = "macosx-arm64"
            } elseif ($jarName -match "macosx-x86-64" -or $jarName -match "macosx-x86_64") {
                $platform = "macosx-x86-64"
                $target = "macosx-x86-64"
            } elseif ($jarName -match "windows-x86-64" -or $jarName -match "windows-x86_64") {
                $platform = "windows-x86-64"
                $target = "windows-x86-64"
            } elseif ($jarName -match "linux-aarch64") {
                $platform = "linux-arm64"
                $target = "linux-arm64"
            } elseif ($jarName -match "linux-x86-64" -or $jarName -match "linux-x86_64") {
                $platform = "linux-x86-64"
                $target = "linux-x86-64"
            }
            
            if ($platform -and $target) {
                $jarTempDir = New-TemporaryFile | ForEach-Object { Remove-Item $_; New-Item -ItemType Directory -Path $_ }
                try {
                    Expand-Archive -Path $jarFile.FullName -DestinationPath $jarTempDir -Force
                    $libFiles = Get-ChildItem -Path $jarTempDir -Include "opencv_java.dll", "libopencv_java.dylib", "libopencv_java.jnilib", "libopencv_java.so" -Recurse -ErrorAction SilentlyContinue
                    
                    if ($libFiles.Count -gt 0) {
                        $targetDir = "src\main\resources\native\$target"
                        if (-not (Test-Path $targetDir)) {
                            New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
                        }
                        
                        foreach ($libFile in $libFiles) {
                            $targetFile = Join-Path $targetDir $libFile.Name
                            Copy-Item $libFile.FullName $targetFile -Force
                            Write-Host "    Extracted: $($libFile.Name) -> $targetFile"
                            $foundAny = $true
                        }
                    }
                } finally {
                    Remove-Item -Recurse -Force $jarTempDir
                }
            }
        }
        break
    }
}

if (-not $foundAny) {
    Write-Host ""
    Write-Host "ERROR: No native libraries found in ZIP file"
    Write-Host "Contents of ZIP:"
    Get-ChildItem -Path $tempDir -Recurse | Select-Object -First 20 | ForEach-Object { Write-Host "  $($_.FullName)" }
    Remove-Item -Recurse -Force $tempDir
    exit 1
}

# 一時ディレクトリを削除
Remove-Item -Recurse -Force $tempDir

Write-Host ""
Write-Host "SUCCESS: Extraction complete!"
Write-Host ""
Write-Host "Copied libraries:"
Get-ChildItem -Path "src\main\resources\native" -Recurse -Include "*.dll", "*.dylib", "*.jnilib", "*.so" -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "  $($_.FullName)"
}

