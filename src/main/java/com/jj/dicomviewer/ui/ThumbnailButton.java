package com.jj.dicomviewer.ui;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * ThumbnailButton - サムネイルボタン
 * 
 * HOROS-20240407のThumbnailCellをJava Swingに移植
 * HOROS-20240407準拠: ThumbnailCell.h, ThumbnailCell.m
 * 
 * サムネイル画像とテキストを表示するボタン
 */
public class ThumbnailButton extends JButton {
    
    // HOROS-20240407準拠: ThumbnailCell.m 41-43行目
    private static final int FULLSIZEHEIGHT = 120;
    private static final int HALFSIZEHEIGHT = 60;
    private static final int SIZEWIDTH = 100;
    
    // HOROS-20240407準拠: ThumbnailCell.h 43行目
    // BOOL rightClick;
    private boolean rightClick = false;
    
    // HOROS-20240407準拠: ThumbnailCell.h 44行目
    // BOOL invertedSet, invertedColors;
    private boolean invertedSet = false;
    private boolean invertedColors = false;
    
    // HOROS-20240407準拠: ThumbnailCell.h 46行目
    // @property(readonly) BOOL rightClick;
    public boolean isRightClick() {
        return rightClick;
    }
    
    /**
     * サムネイルセルの幅を取得
     * HOROS-20240407準拠: ThumbnailCell.m 49行目 - thumbnailCellWidth
     */
    public static float getThumbnailCellWidth() {
        Preferences prefs = Preferences.userNodeForPackage(ThumbnailButton.class);
        int dbFontSize = prefs.getInt("dbFontSize", 0);
        
        float width = SIZEWIDTH;
        switch (dbFontSize) {
            case -1:
                width = SIZEWIDTH * 0.8f;
                break;
            case 0:
                width = SIZEWIDTH;
                break;
            case 1:
                width = SIZEWIDTH * 1.3f;
                break;
        }
        return width;
    }
    
    /**
     * コンストラクタ
     */
    public ThumbnailButton() {
        super();
        
        // HOROS-20240407準拠: ViewerController.m 5072-5081行目
        // [cell setImagePosition: NSImageBelow];
        // [cell setTransparent:NO];
        // [cell setEnabled:YES];
        // [cell setButtonType:NSMomentaryPushInButton];
        // [cell setBezelStyle:NSShadowlessSquareBezelStyle];
        // [cell setHighlightsBy:NSContentsCellMask];
        // [cell setImageScaling:NSImageScaleProportionallyDown];
        // [cell setBordered:YES];
        setHorizontalTextPosition(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setOpaque(false);
        setEnabled(true);
        setBorderPainted(true);
        setContentAreaFilled(false);
        setFocusPainted(true);
        
        // HOROS-20240407準拠: ViewerController.m 5083行目
        // [cell setTitle:@""];
        setText("");
        
        // HOROS-20240407準拠: ViewerController.m 5085行目
        // [cell setImage: nil];
        setIcon(null);
    }
    
    /**
     * セルのサイズを取得
     * HOROS-20240407準拠: ThumbnailCell.m 97行目 - cellSize
     */
    public Dimension getCellSize() {
        // HOROS-20240407準拠: ThumbnailCell.m 99-106行目
        // O2ViewerThumbnailsMatrixRepresentedObject* oro = [self representedObject];
        // float h = 0;
        // if ([oro.object isKindOfClass:[NSManagedObject class]] || oro.children.count || oro == nil)
        //     h = FULLSIZEHEIGHT;
        // else
        //     h = HALFSIZEHEIGHT;
        // 簡易実装: 常にFULLSIZEHEIGHTを使用
        float h = FULLSIZEHEIGHT;
        
        Preferences prefs = Preferences.userNodeForPackage(ThumbnailButton.class);
        int dbFontSize = prefs.getInt("dbFontSize", 0);
        
        float width = getThumbnailCellWidth();
        float height = h;
        
        switch (dbFontSize) {
            case -1:
                height = h * 0.8f;
                break;
            case 0:
                height = h;
                break;
            case 1:
                height = h * 1.3f;
                break;
        }
        
        return new Dimension((int)width, (int)height);
    }
    
    /**
     * 推奨サイズを取得
     */
    @Override
    public Dimension getPreferredSize() {
        return getCellSize();
    }
    
    /**
     * 最小サイズを取得
     */
    @Override
    public Dimension getMinimumSize() {
        return getCellSize();
    }
    
    /**
     * 最大サイズを取得
     */
    @Override
    public Dimension getMaximumSize() {
        return getCellSize();
    }
}
