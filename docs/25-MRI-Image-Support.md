# MRI画像サポート

## 現状

MRI画像はまだ確認できていませんが、実装はCT、US、CR画像と同様にpixel spacingを考慮した表示を行います。

## 実装済み機能

### Pixel Spacingを考慮した表示
- Pixel Spacingタグ（(0028,0030)）から値を取得
- 表示スケールを自動計算
- ウィンドウにフィット表示

### Window Level/Widthの適用
- DICOMファイルからWindow Center/Widthを取得
- 8-bit範囲内の場合は適用
- 12-bit/16-bit範囲の場合は元画像を表示

## MRI画像の特徴

- **Pixel Spacing**: 通常、CT画像と同様に0.5mm/pixel程度
- **Window Level/Width**: MRI特有の設定値が存在する可能性
- **モダリティ**: "MR"

## 確認が必要な項目

1. Pixel Spacingが正しく取得できるか
2. 表示スケールが適切か
3. Window Level/Widthが正しく適用されるか
4. 位置ズレや縮尺異常がないか

## 実装

現在の実装では、MRI画像もCT、US、CR画像と同様に処理されます：

- Pixel Spacingを取得して表示スケールを計算
- Window Level/Widthを適用
- ウィンドウにフィット表示

MRI画像が見つかり次第、確認をお願いします。問題があれば、具体的な症状を教えてください。

