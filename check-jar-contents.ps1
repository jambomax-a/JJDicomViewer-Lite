# Weasis JARファイルの中身を確認するスクリプト（Windows用）

$horosPath = "horos-20240407\Binaries\weasis-extracted\weasis\bundle"

Write-Host "Checking Weasis OpenCV JAR files..."
Write-Host ""

# macOS用のJARファイルを探す
$macJars = Get-ChildItem -Path $horosPath -Filter "*opencv*macosx*.jar.xz" -ErrorAction SilentlyContinue

if ($macJars.Count -eq 0) {
    Write-Host "No macOS OpenCV JAR files found in: $horosPath"
    exit 1
}

foreach ($jarXz in $macJars) {
    Write-Host "=========================================="
    Write-Host "File: $($jarXz.Name)"
    Write-Host "=========================================="
    
    # .xzファイルを一時的に解凍
    $tempJar = $jarXz.FullName -replace "\.xz$", ""
    
    if (Get-Command 7z -ErrorAction SilentlyContinue) {
        Write-Host "Extracting with 7-Zip..."
        7z x $jarXz.FullName -o(Split-Path $jarXz.FullName) | Out-Null
    } elseif (Get-Command xz -ErrorAction SilentlyContinue) {
        Write-Host "Extracting with xz..."
        xz -d $jarXz.FullName
    } else {
        Write-Host "ERROR: Neither 7z nor xz command found."
        Write-Host "Please install 7-Zip or extract manually."
        continue
    }
    
    if (Test-Path $tempJar) {
        Write-Host "JAR contents (OpenCV related files):"
        Write-Host ""
        
        # JARファイルの中身を確認
        $jarContents = jar -tf $tempJar 2>$null | Select-String -Pattern "opencv|dylib|jnilib|\.so|\.dll" -CaseSensitive:$false
        
        if ($jarContents) {
            $jarContents | ForEach-Object { Write-Host "  $_" }
        } else {
            Write-Host "  No OpenCV native library files found in JAR"
            Write-Host ""
            Write-Host "All files in JAR:"
            jar -tf $tempJar 2>$null | Select-Object -First 20 | ForEach-Object { Write-Host "  $_" }
        }
        
        Write-Host ""
    } else {
        Write-Host "ERROR: Failed to extract JAR file"
    }
}

Write-Host ""
Write-Host "To extract the native library, use:"
Write-Host "  .\extract-opencv-native.ps1"
Write-Host ""
Write-Host "Or manually:"
Write-Host "  1. Extract .xz file (7z x filename.jar.xz)"
Write-Host "  2. Extract JAR file (jar -xf filename.jar)"
Write-Host "  3. Find libopencv_java.dylib or libopencv_java.jnilib"
Write-Host "  4. Copy to src\main\resources\native\macosx-x86-64\"

