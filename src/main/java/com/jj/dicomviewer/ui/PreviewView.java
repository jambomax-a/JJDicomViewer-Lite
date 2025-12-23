package com.jj.dicomviewer.ui;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;

import com.jj.dicomviewer.model.DicomPix;

/**
 * PreviewView - プレビュー画像表示ビュー
 * 
 * HOROS-20240407/Horos/Sources/PreviewView.h
 * HOROS-20240407/Horos/Sources/PreviewView.m
 * の写経
 * 
 * DCMViewを継承し、BrowserController用のプレビュー表示を提供
 */
public class PreviewView extends DCMView {
    
    // HOROS: BrowserControllerへの参照
    private BrowserController browserController;
    
    /**
     * HOROS: - (id) initWithFrame:(NSRect)frame
     */
    public PreviewView() {
        super();
        
        // HOROS: マウスホイールリスナー
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                scrollWheel(e);
            }
        });
    }
    
    /**
     * HOROS: BrowserControllerを設定
     */
    public void setBrowserController(BrowserController controller) {
        this.browserController = controller;
    }
    
    /**
     * HOROS-20240407準拠: - (void) setTheMatrix:(NSMatrix *)matrix
     * BrowserController.m 行14250: [imageView setTheMatrix:oMatrix];
     * マトリックスを設定（DCMViewには存在しないため、ここで実装）
     */
    private Object theMatrix;
    
    public void setTheMatrix(Object matrix) {
        this.theMatrix = matrix;
    }
    
    public Object getTheMatrix() {
        return theMatrix;
    }
    
    /**
     * HOROS: - (BOOL)is2DViewer
     * PreviewView.m 行72
     */
    public boolean is2DViewer() {
        return false;
    }
    
    /**
     * HOROS: - (void) scrollWheel:(NSEvent *)theEvent
     * BrowserControllerに委譲
     * HOROS-20240407準拠: PreviewView.m のscrollWheel実装
     */
    private void scrollWheel(MouseWheelEvent e) {
        if (browserController != null) {
            // HOROS-20240407準拠: BrowserControllerのscrollWheelメソッドに委譲
            // wheelRotationは通常、上にスクロールで負の値、下にスクロールで正の値
            // HOROSのdeltaYに相当
            browserController.previewScrollWheel(e.getWheelRotation());
        }
    }
    
    /**
     * HOROS: pixelsを設定（List版）
     */
    public void setPixels(List<DicomPix> pixels) {
        // TODO: 実装 - pixelsを設定して表示を更新
    }
    
    /**
     * HOROS: pixelsを設定（Listとインデックス版）
     */
    public void setPixels(List<DicomPix> pixels, int index) {
        // TODO: 実装 - pixelsとインデックスを設定して表示を更新
        setPixels(pixels);
    }
    
    /**
     * インデックスを設定
     * HOROS-20240407準拠: - (void)setIndex:(int)index (DCMView.m)
     */
    private int currentIndex = 0;
    
    public void setIndex(int index) {
        this.currentIndex = index;
        // TODO: インデックスに応じた画像を表示
        repaint();
    }
    
    public int getIndex() {
        return currentIndex;
    }
}
