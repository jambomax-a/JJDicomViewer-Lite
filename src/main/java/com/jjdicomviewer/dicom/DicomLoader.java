package com.jjdicomviewer.dicom;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * DICOMファイル読み込みクラス（Swing版）
 * RAWピクセルデータを直接読み込んで、ウィンドウ/レベルを適用する前の状態で返す
 */
public class DicomLoader {
    private static final Logger logger = LoggerFactory.getLogger(DicomLoader.class);

    /**
     * DICOMファイルからBufferedImageを読み込む（RAWデータ）
     * @param dicomFile DICOMファイル
     * @return BufferedImage（RAWピクセルデータ、ウィンドウ/レベル未適用）
     */
    public BufferedImage loadDicomImage(File dicomFile) throws IOException {
        return loadDicomImageRaw(dicomFile);
    }
    
    /**
     * DICOMファイルからBufferedImageを読み込む（ウィンドウ/レベルを指定）
     * 注意: windowCenter/windowWidthは現在未使用（後でprocessImageで適用）
     * @param dicomFile DICOMファイル
     * @param windowCenter ウィンドウセンター（将来の拡張用、現在は未使用）
     * @param windowWidth ウィンドウ幅（将来の拡張用、現在は未使用）
     * @return BufferedImage（RAWピクセルデータ、ウィンドウ/レベル未適用）
     */
    public BufferedImage loadDicomImage(File dicomFile, Double windowCenter, Double windowWidth) throws IOException {
        // RAWデータを読み込む（ウィンドウ/レベルはprocessImageで適用）
        return loadDicomImageRaw(dicomFile);
    }
    
    /**
     * DICOMファイルからRAWピクセルデータを読み込む
     * @param dicomFile DICOMファイル
     * @return BufferedImage（RAWピクセルデータ）
     */
    private BufferedImage loadDicomImageRaw(File dicomFile) throws IOException {
        if (dicomFile == null || !dicomFile.exists()) {
            logger.error("DICOMファイルが存在しません: {}", dicomFile);
            throw new IOException("DICOMファイルが存在しません: " + dicomFile);
        }
        
        long fileSize = dicomFile.length();
        if (fileSize == 0) {
            logger.error("DICOMファイルのサイズが0です: {}", dicomFile.getAbsolutePath());
            throw new IOException("DICOMファイルのサイズが0です: " + dicomFile.getAbsolutePath());
        }

        try (DicomInputStream dis = new DicomInputStream(dicomFile)) {
            // まず、すべてのデータを読み込む（ピクセルデータを含む）
            // readDataset(-1, -1)でピクセルデータも含めてすべてのデータを読み込む
            @SuppressWarnings("deprecation")
            Attributes attrs;
            try {
                attrs = dis.readDataset(-1, -1);
            } catch (Exception e) {
                logger.error("readDataset()でエラー: ファイル={}, ファイルサイズ={}, エラー={}",
                    dicomFile.getName(), fileSize, e.getMessage(), e);
                throw new IOException("DICOMデータセットの読み込みに失敗: " + e.getMessage(), e);
            }
            
            if (!attrs.contains(Tag.PixelData)) {
                dis.close();
                try (DicomInputStream dis2 = new DicomInputStream(dicomFile)) {
                    @SuppressWarnings("deprecation")
                    Attributes attrs2 = dis2.readDataset(-1, Tag.PixelData);
                    if (attrs2.contains(Tag.PixelData)) {
                        attrs = attrs2;
                    }
                }
            }
            
            // 画像属性を取得
            int rows = attrs.getInt(Tag.Rows, 0);
            int columns = attrs.getInt(Tag.Columns, 0);
            int bitsAllocated = attrs.getInt(Tag.BitsAllocated, 16);
            int bitsStored = attrs.getInt(Tag.BitsStored, bitsAllocated);
            int samplesPerPixel = attrs.getInt(Tag.SamplesPerPixel, 1);
            int planarConfiguration = attrs.getInt(Tag.PlanarConfiguration, 0);
            boolean signed = attrs.getInt(Tag.PixelRepresentation, 0) != 0;
            String photometricInterpretation = attrs.getString(Tag.PhotometricInterpretation, "MONOCHROME2");
            
            // Rescale Slope/Intercept
            double rescaleSlope = attrs.getDouble(Tag.RescaleSlope, 1.0);
            double rescaleIntercept = attrs.getDouble(Tag.RescaleIntercept, 0.0);
            
            if (rows <= 0 || columns <= 0) {
                throw new IOException("無効な画像サイズ: " + rows + "x" + columns);
            }
            
            String transferSyntax = null;
            try (DicomInputStream dis3 = new DicomInputStream(dicomFile)) {
                transferSyntax = dis3.getTransferSyntax().toString();
            }
            
            boolean isCompressed = transferSyntax != null && 
                    !transferSyntax.contains("1.2.840.10008.1.2") && 
                    !transferSyntax.equals("1.2.840.10008.1.2");
            
            if (isCompressed) {
                try {
                    BufferedImage decodedImage = javax.imageio.ImageIO.read(dicomFile);
                    if (decodedImage != null) {
                        return decodedImage;
                    }
                } catch (Exception e) {
                    logger.warn("ImageIOによる画像デコードに失敗: {}", e.getMessage());
                }
            }
            
            // ピクセルデータを取得
            byte[] pixelData = null;
            
            // まず通常の方法で取得を試みる
            if (attrs.contains(Tag.PixelData)) {
                try {
                    VR vr = attrs.getVR(Tag.PixelData);
                    if (vr == VR.OB || vr == VR.OW) {
                        try {
                            pixelData = attrs.getBytes(Tag.PixelData);
                        } catch (Exception e) {
                            logger.warn("ピクセルデータの取得に失敗 (VR: {}): {}", vr, e.getMessage());
                        }
                    } else {
                        try {
                            pixelData = attrs.getBytes(Tag.PixelData);
                        } catch (Exception e) {
                            logger.warn("ピクセルデータの取得に失敗 (VR: {}): {}", vr, e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.error("PixelDataのVR取得に失敗: {}", e.getMessage(), e);
                }
            } else {
                logger.warn("AttributesにPixelDataタグが含まれていません");
            }
            
            if (pixelData == null || pixelData.length == 0) {
                try (DicomInputStream dis2 = new DicomInputStream(dicomFile)) {
                    @SuppressWarnings("deprecation")
                    Attributes attrs2 = dis2.readDataset(-1, Tag.PixelData);
                    if (attrs2.contains(Tag.PixelData)) {
                        pixelData = attrs2.getBytes(Tag.PixelData);
                    }
                } catch (Exception e) {
                    logger.warn("再読み込みでもピクセルデータの取得に失敗: {}", e.getMessage());
                }
            }
            
            if (pixelData == null || pixelData.length == 0) {
                try (DicomInputStream dis3 = new DicomInputStream(dicomFile)) {
                    @SuppressWarnings("deprecation")
                    Attributes attrs3 = dis3.readDataset(-1, -1);
                    if (attrs3.contains(Tag.PixelData)) {
                        pixelData = attrs3.getBytes(Tag.PixelData);
                    }
                } catch (Exception e) {
                    logger.warn("直接読み込みでもピクセルデータの取得に失敗: {}", e.getMessage());
                }
            }
            
            if (pixelData == null || pixelData.length == 0) {
                // ファイルサイズは既に取得済み（fileSize変数を使用）
                logger.error("ピクセルデータが見つかりません。ファイルサイズ: {} バイト, Transfer Syntax: {}", fileSize, transferSyntax);
                throw new IOException("ピクセルデータが見つかりません（ファイルサイズ: " + fileSize + " バイト, Transfer Syntax: " + transferSyntax + "）");
            }
            
            // ByteBufferを作成（エンディアンを考慮）
            ByteBuffer buffer = ByteBuffer.wrap(pixelData);
            // DICOMのデフォルトはリトルエンディアン
            // 一部のTransferSyntax（Explicit VR Big Endian）はビッグエンディアンだが、
            // ほとんどのDICOMファイルはリトルエンディアン
            if (transferSyntax != null && transferSyntax.contains("BigEndian")) {
                buffer.order(ByteOrder.BIG_ENDIAN);
            } else {
                // デフォルトはリトルエンディアン
                buffer.order(ByteOrder.LITTLE_ENDIAN);
            }
            
            // BufferedImageを作成
            logger.info("画像作成: サイズ={}x{}, bitsAllocated={}, samplesPerPixel={}, photometricInterpretation={}", 
                columns, rows, bitsAllocated, samplesPerPixel, photometricInterpretation);
            
            BufferedImage image;
            if (samplesPerPixel == 3 || samplesPerPixel == 4) {
                // カラー画像（RGB/RGBA）
                if (bitsAllocated == 8) {
                    image = new BufferedImage(columns, rows, BufferedImage.TYPE_INT_RGB);
                } else {
                    // 16bitカラーは未対応（一旦グレースケールとして扱う）
                    logger.warn("16bitカラー画像は未対応。グレースケールとして処理します。");
                    image = new BufferedImage(columns, rows, BufferedImage.TYPE_BYTE_GRAY);
                }
            } else {
                // グレースケール画像
                if (bitsAllocated == 8) {
                    image = new BufferedImage(columns, rows, BufferedImage.TYPE_BYTE_GRAY);
                } else if (bitsAllocated == 16) {
                    // 16bit画像用にUSHORT_GRAYを使用
                    image = new BufferedImage(columns, rows, BufferedImage.TYPE_USHORT_GRAY);
                } else {
                    // その他のビット深度は16bitとして扱う
                    logger.warn("未対応のビット深度: {}。16bitとして処理します。", bitsAllocated);
                    image = new BufferedImage(columns, rows, BufferedImage.TYPE_USHORT_GRAY);
                }
            }
            
            logger.info("BufferedImage作成完了: サイズ={}x{}, タイプ={}", 
                image.getWidth(), image.getHeight(), image.getType());
            
            // ピクセルデータを読み込む
            // 注意: Rescale Slope/Interceptは適用せず、生のピクセル値を保持
            // ウィンドウ/レベル適用時にRescale Slope/Interceptを考慮する
            if (bitsAllocated == 8) {
                // 8bit画像
                if (samplesPerPixel == 3 || samplesPerPixel == 4) {
                    // RGB/RGBA画像
                    if (planarConfiguration == 1) {
                        // Planar形式（RRR... GGG... BBB...）をインターリーブ形式に変換
                        int pixelCount = rows * columns;
                        byte[] redPlane = new byte[pixelCount];
                        byte[] greenPlane = new byte[pixelCount];
                        byte[] bluePlane = new byte[pixelCount];
                        buffer.get(redPlane);
                        buffer.get(greenPlane);
                        buffer.get(bluePlane);
                        if (samplesPerPixel == 4) {
                            // Alphaチャネルが存在する場合は読み飛ばす
                            byte[] alphaPlane = new byte[pixelCount];
                            buffer.get(alphaPlane);
                        }
                        
                        for (int y = 0; y < rows; y++) {
                            for (int x = 0; x < columns; x++) {
                                int idx = y * columns + x;
                                int r = redPlane[idx] & 0xFF;
                                int g = greenPlane[idx] & 0xFF;
                                int b = bluePlane[idx] & 0xFF;
                                int rgb = (r << 16) | (g << 8) | b;
                                image.setRGB(x, y, rgb);
                            }
                        }
                    } else {
                        // Interleaved形式
                        for (int y = 0; y < rows; y++) {
                            for (int x = 0; x < columns; x++) {
                                int r = buffer.get() & 0xFF;
                                int g = buffer.get() & 0xFF;
                                int b = buffer.get() & 0xFF;
                                if (samplesPerPixel == 4) {
                                    buffer.get(); // Alphaチャネルをスキップ
                                }
                                int rgb = (r << 16) | (g << 8) | b;
                                image.setRGB(x, y, rgb);
                            }
                        }
                    }
                } else {
                    // グレースケール画像
                    for (int y = 0; y < rows; y++) {
                        for (int x = 0; x < columns; x++) {
                            int pixelValue = buffer.get() & 0xFF;
                            // 生のピクセル値をそのまま保持（Rescale Slope/Interceptは後で適用）
                            // MONOCHROME1の場合は反転が必要だが、ここでは生値を保持
                            int rgb = (pixelValue << 16) | (pixelValue << 8) | pixelValue;
                            image.setRGB(x, y, rgb);
                        }
                    }
                }
            } else if (bitsAllocated == 16) {
                // 16bit画像 - WritableRasterを使って直接データを書き込む
                java.awt.image.WritableRaster raster = image.getRaster();
                short[] pixelArray = new short[rows * columns];
                
                for (int i = 0; i < pixelArray.length; i++) {
                    int pixelValue;
                    if (signed) {
                        // signed 16bit: -32768 から 32767
                        pixelValue = buffer.getShort();
                    } else {
                        // unsigned 16bit: 0 から 65535
                        pixelValue = buffer.getShort() & 0xFFFF;
                    }
                    
                    // BitsStoredでマスク（上位ビットを無視）
                    if (bitsStored < 16) {
                        if (signed) {
                            // signedの場合、符号拡張を考慮
                            int mask = (1 << bitsStored) - 1;
                            int signBit = 1 << (bitsStored - 1);
                            pixelValue = pixelValue & mask;
                            // 符号ビットが立っている場合は符号拡張
                            if ((pixelValue & signBit) != 0) {
                                pixelValue = pixelValue | (~mask);
                            }
                        } else {
                            pixelValue = pixelValue & ((1 << bitsStored) - 1);
                        }
                    }
                    
                    // 生のピクセル値を保持（Rescale Slope/Interceptは後で適用）
                    // signedピクセルの場合、負の値も保持する必要があるため、
                    // 0-65535の範囲にオフセットして保存
                    if (signed) {
                        // signed値を0-65535の範囲にマッピング（オフセット32768を加算）
                        pixelArray[i] = (short) (pixelValue + 32768);
                    } else {
                        pixelArray[i] = (short) pixelValue;
                    }
                }
                
                // データをラスターに書き込む
                raster.setDataElements(0, 0, columns, rows, pixelArray);
            } else {
                throw new IOException("未対応のビット深度: " + bitsAllocated);
            }
            
            return image;
        } catch (Exception e) {
            logger.error("DICOM画像の読み込みに失敗しました", e);
            throw new IOException("DICOMファイルの読み込みに失敗しました: " + e.getMessage(), e);
        }
    }
}

