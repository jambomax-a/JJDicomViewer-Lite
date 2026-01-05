package com.jj.dicomviewer.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
    
    // HOROS-20240407準拠: ThumbnailCell.h representedObject
    // O2ViewerThumbnailsMatrixRepresentedObject *representedObject;
    // Java Swingでは、SeriesまたはStudyを直接保持
    private Object representedObject = null;

    // HOROS-20240407準拠: ThumbnailCell.m 73-91行目 - drawBezelWithFrame:inView:
    // ハイライト表示用の背景色
    private Color highlightColor = null;

    // HOROS-20240407準拠: ThumbnailCell.h 46行目
    // @property(readonly) BOOL rightClick;
    public boolean isRightClick() {
        return rightClick;
    }
    
    /**
     * representedObjectを設定
     * HOROS-20240407準拠: ThumbnailCell.h representedObject
     */
    public void setRepresentedObject(Object obj) {
        this.representedObject = obj;
    }
    
    /**
     * representedObjectを取得
     * HOROS-20240407準拠: ThumbnailCell.h representedObject
     */
    public Object getRepresentedObject() {
        return this.representedObject;
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
        setEnabled(true);
        setBorderPainted(true);
        setFocusPainted(true);

        // HOROS-20240407準拠: クリック可能にする
        setFocusable(true);

        // HOROS-20240407準拠: NSButtonCellのデフォルト背景色（明るいグレー）
        // backgroundColor=nilの場合、NSButtonCellは明るいグレーの背景を表示
        // カスタム描画を使用するため、setContentAreaFilled(false)にする
        setOpaque(false);
        setContentAreaFilled(false);
        setBackground(new Color(220, 220, 220)); // 明るいグレー（previewMatrixより明るい）

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

    /**
     * ハイライト色を設定
     * HOROS-20240407準拠: ThumbnailCell.m 73-91行目 - drawBezelWithFrame:inView:
     *
     * @param color ハイライト色（nullでハイライトを解除）
     */
    public void setHighlightColor(Color color) {
        this.highlightColor = color;

        // HOROS-20240407準拠: ThumbnailCell.m 86行目
        // [[backc colorWithAlphaComponent:0.75] setFill];
        if (color != null) {
            // HOROS-20240407準拠: 背景色を半透明にする
            Color transparentColor = new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (int)(255 * 0.75)
            );
            setOpaque(false);
            setContentAreaFilled(false);
            setBackground(transparentColor);
            System.out.println("ThumbnailButton.setHighlightColor: setting color to " + transparentColor);
        } else {
            // HOROS-20240407準拠: backgroundColor=nilの場合、デフォルトの明るい背景色に戻す
            setOpaque(false);
            setContentAreaFilled(false);
            setBackground(new Color(220, 220, 220)); // 明るいグレー
            System.out.println("ThumbnailButton.setHighlightColor: clearing color, setting to light gray (220,220,220)");
        }

        repaint();
    }

    /**
     * ハイライト色を取得
     */
    public Color getHighlightColor() {
        return highlightColor;
    }

    /**
     * ハイライトされているか確認
     */
    public boolean isHighlighted() {
        return highlightColor != null;
    }

    /**
     * 背景色を確実に描画するためのpaintComponentオーバーライド
     * HOROS-20240407準拠: ThumbnailCell.m 73-91行目 - drawBezelWithFrame:inView:
     */
    @Override
    protected void paintComponent(Graphics g) {
        // HOROS-20240407準拠: 背景色を確実に描画
        // NSButtonCellのdrawBezelWithFrame相当の処理
        Graphics2D g2d = (Graphics2D) g.create();

        // アンチエイリアスを有効化
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 背景を常に描画（setContentAreaFilled(false)にしているため）
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.dispose();

        // 通常の描画処理を実行
        super.paintComponent(g);
    }
}
