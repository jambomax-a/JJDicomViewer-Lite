# 依存関係の問題について

## 現在の問題

dcm4che 5.34.1が`weasis-core-img-bom:4.11.0`に依存していますが、この依存関係がMavenリポジトリで見つかりません。

## 解決方法

### 方法1: dcm4cheのバージョンを変更する

dcm4cheの利用可能なバージョンを確認し、weasis-core-img-bomに依存しないバージョンを使用します。

```kotlin
dependencies {
    // 利用可能なバージョンを確認して使用
    implementation("org.dcm4che:dcm4che-core:5.33.0") // 例
    implementation("org.dcm4che:dcm4che-image:5.33.0")
    implementation("org.dcm4che:dcm4che-imageio:5.33.0")
    implementation("org.dcm4che:dcm4che-net:5.33.0")
}
```

### 方法2: weasis-core-img-bomを手動で追加する

weasis-core-img-bomの正しいリポジトリを見つけて追加します。

### 方法3: 依存関係を除外する

dcm4cheのPOMファイルからweasis-core-img-bomの依存関係を除外します（ただし、これが機能するかは不明です）。

## 次のステップ

1. dcm4cheの利用可能なバージョンを確認
2. weasis-core-img-bomの正しいリポジトリを探す
3. 必要に応じてdcm4cheのバージョンを変更

## 参考

- dcm4che公式サイト: https://www.dcm4che.org/
- Maven Central: https://mvnrepository.com/artifact/org.dcm4che

