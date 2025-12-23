package com.jj.dicomviewer.ui;

import com.jj.dicomviewer.model.DicomStudy;
import com.jj.dicomviewer.model.DicomSeries;
import com.jj.dicomviewer.model.DicomImage;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.List;

/**
 * DatabaseOutlineTreeModel - データベースアウトラインビューのTreeModel
 * 
 * HOROS-20240407準拠: NSOutlineViewのデータソース/デリゲートパターンを実装
 * - outlineView:child:ofItem: → getChild(parent, index)
 * - outlineView:isItemExpandable: → isLeaf(node)
 * - outlineView:numberOfChildrenOfItem: → getChildCount(parent)
 * - outlineView:objectValueForTableColumn:byItem: → セルレンダラーで処理
 */
public class DatabaseOutlineTreeModel implements TreeModel {
    
    private BrowserController browserController;
    private Object root = "Database"; // ルートは常に"Database"
    
    /**
     * コンストラクタ
     */
    public DatabaseOutlineTreeModel(BrowserController browserController) {
        this.browserController = browserController;
    }
    
    /**
     * ルートを取得
     * HOROS-20240407準拠: item==nilの場合はoutlineViewArrayから取得
     */
    @Override
    public Object getRoot() {
        return root;
    }
    
    /**
     * 子のインデックスで子を取得
     * HOROS-20240407: - (id)outlineView:(NSOutlineView *)outlineView child:(NSInteger)index ofItem:(id)item
     */
    @Override
    public Object getChild(Object parent, int index) {
        if (parent == root) {
            // ルートの子はoutlineViewArrayの要素
            List<Object> outlineViewArray = browserController.getOutlineViewArray();
            synchronized (outlineViewArray) {
                if (index >= 0 && index < outlineViewArray.size()) {
                    return outlineViewArray.get(index);
                }
            }
            return null;
        } else if (parent instanceof DicomStudy) {
            // Studyの子はimageSeries（シリーズの配列）
            // HOROS-20240407準拠: [[self childrenArray: item] objectAtIndex: index]
            DicomStudy study = (DicomStudy) parent;
            List<DicomSeries> imageSeries = study.imageSeries();
            if (index >= 0 && index < imageSeries.size()) {
                return imageSeries.get(index);
            }
            return null;
        } else if (parent instanceof DicomSeries) {
            // Seriesの子は画像（sortedImages）
            // HOROS-20240407準拠: [[item sortedImages] objectAtIndex: index]
            DicomSeries series = (DicomSeries) parent;
            List<DicomImage> sortedImages = series.sortedImages();
            if (index >= 0 && index < sortedImages.size()) {
                return sortedImages.get(index);
            }
            return null;
        }
        return null;
    }
    
    /**
     * 子の数を取得
     * HOROS-20240407: - (NSInteger)outlineView:(NSOutlineView *)outlineView numberOfChildrenOfItem:(id)item
     */
    @Override
    public int getChildCount(Object parent) {
        if (parent == root) {
            // ルートの子の数はoutlineViewArrayのサイズ
            List<Object> outlineViewArray = browserController.getOutlineViewArray();
            synchronized (outlineViewArray) {
                return outlineViewArray.size();
            }
        } else if (parent instanceof DicomStudy) {
            // Studyの子の数はimageSeriesの数
            // HOROS-20240407準拠: [[item valueForKey:@"imageSeries"] count]
            DicomStudy study = (DicomStudy) parent;
            List<DicomSeries> imageSeries = study.imageSeries();
            return imageSeries.size();
        } else if (parent instanceof DicomSeries) {
            // HOROS-20240407準拠: Seriesは展開不可なので子の数は0
            // ただし、numberOfChildrenOfItemでは[[item valueForKey:@"noFiles"] intValue]を返すが
            // isItemExpandableでSeriesはNOを返すため、実際には子は表示されない
            return 0;
        }
        return 0;
    }
    
    /**
     * 子のインデックスを取得
     */
    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent == root) {
            List<Object> outlineViewArray = browserController.getOutlineViewArray();
            synchronized (outlineViewArray) {
                return outlineViewArray.indexOf(child);
            }
        } else if (parent instanceof DicomStudy && child instanceof DicomSeries) {
            // HOROS-20240407準拠: imageSeriesから検索
            DicomStudy study = (DicomStudy) parent;
            DicomSeries series = (DicomSeries) child;
            List<DicomSeries> imageSeries = study.imageSeries();
            return imageSeries.indexOf(series);
        } else if (parent instanceof DicomSeries && child instanceof DicomImage) {
            // HOROS-20240407準拠: sortedImagesから検索
            DicomSeries series = (DicomSeries) parent;
            DicomImage image = (DicomImage) child;
            List<DicomImage> sortedImages = series.sortedImages();
            return sortedImages.indexOf(image);
        }
        return -1;
    }
    
    /**
     * リーフかどうか
     * HOROS-20240407: - (BOOL)outlineView:(NSOutlineView *)outlineView isItemExpandable:(id)item
     * Studyは展開可能、Seriesは展開不可（画像は表示しない）
     */
    @Override
    public boolean isLeaf(Object node) {
        if (node == root) {
            return false; // ルートは展開可能
        } else if (node instanceof DicomStudy) {
            // HOROS-20240407準拠: Studyは展開可能
            return false;
        } else if (node instanceof DicomSeries) {
            // HOROS-20240407準拠: Seriesは展開不可（画像は表示しない）
            // [[item valueForKey:@"type"] isEqualToString:@"Series"] → returnVal = NO; だが、実際には画像を表示しない
            return true;
        } else if (node instanceof DicomImage) {
            // Imageはリーフ
            return true;
        }
        return true;
    }
    
    /**
     * 値が変更された
     */
    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // HOROS-20240407準拠: 編集は別のメソッドで処理
    }
    
    private javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
    
    /**
     * ツリーモデルリスナーを追加
     */
    @Override
    public void addTreeModelListener(javax.swing.event.TreeModelListener l) {
        listenerList.add(javax.swing.event.TreeModelListener.class, l);
    }
    
    /**
     * ツリーモデルリスナーを削除
     */
    @Override
    public void removeTreeModelListener(javax.swing.event.TreeModelListener l) {
        listenerList.remove(javax.swing.event.TreeModelListener.class, l);
    }
    
    /**
     * リスナーを取得（デバッグ用）
     */
    public javax.swing.event.TreeModelListener[] getListeners() {
        return listenerList.getListeners(javax.swing.event.TreeModelListener.class);
    }
    
    /**
     * ツリー構造変更イベントを発火
     * HOROS-20240407準拠: TreeModelのリスナーに変更を通知
     */
    public void fireTreeStructureChanged(TreePath path) {
        Object[] listeners = listenerList.getListenerList();
        javax.swing.event.TreeModelEvent e = new javax.swing.event.TreeModelEvent(this, path);
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == javax.swing.event.TreeModelListener.class) {
                ((javax.swing.event.TreeModelListener) listeners[i+1]).treeStructureChanged(e);
            }
        }
    }
}

