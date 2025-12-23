package com.jj.dicomviewer.ui;

import javax.swing.JPanel;

/**
 * DCMView - DICOM画像表示ビュー
 * 
 * HOROS-20240407のDCMViewをJava Swingに移植
 */
public class DCMView extends JPanel {
    
    /**
     * ToolMode - ツールモード
     * HOROS-20240407準拠: DCMViewのToolMode enum
     */
    public enum ToolMode {
        tPlain,      // 通常モード
        tMesure,     // 測定
        tOval,       // 楕円
        tOPolygon,   // 開いた多角形
        tCPolygon,   // 閉じた多角形
        tAngle,      // 角度
        tText,       // テキスト
        tArrow,      // 矢印
        tPencil,     // 鉛筆
        tROI,        // ROI
        t2DPoint,    // 2Dポイント
        t2DPolygon,   // 2D多角形
        t3DCut,      // 3Dカット
        t3Dpoint,     // 3Dポイント
        t3DROI,      // 3D ROI
        tRepulsor,   // リパルサー
        tBonesRemoval, // 骨除去
        t3DRotate,   // 3D回転
        tCross,      // クロス
        tUnselect,   // 選択解除
        tWLWW,       // ウィンドウレベル/ウィンドウ幅
        tNext,       // 次
        tPrevious,   // 前
        tFlip,       // フリップ
        tRefresh,    // リフレッシュ
        tRotate,     // 回転
        tZoom,       // ズーム
        tTranslate,  // 移動
        t3DpointTool, // 3Dポイントツール
        t3DCutTool,   // 3Dカットツール
        t3DROITool,   // 3D ROIツール
        t3DRotateTool, // 3D回転ツール
        tBonesRemovalTool, // 骨除去ツール
        tRepulsorTool,     // リパルサーツール
        t3DPointTool,      // 3Dポイントツール
        t3DPolygonTool,    // 3D多角形ツール
        t3DROITool2,      // 3D ROIツール2
        t3DCutTool2,      // 3Dカットツール2
        t3DRotateTool2,   // 3D回転ツール2
        tBonesRemovalTool2, // 骨除去ツール2
        tRepulsorTool2,     // リパルサーツール2
        t3DPointTool2,     // 3Dポイントツール2
        t3DPolygonTool2,   // 3D多角形ツール2
        t3DROITool3,       // 3D ROIツール3
        t3DCutTool3,       // 3Dカットツール3
        t3DRotateTool3,    // 3D回転ツール3
        tBonesRemovalTool3, // 骨除去ツール3
        tRepulsorTool3,     // リパルサーツール3
        t3DPointTool3,     // 3Dポイントツール3
        t3DPolygonTool3,   // 3D多角形ツール3
        t3DROITool4,       // 3D ROIツール4
        t3DCutTool4,       // 3Dカットツール4
        t3DRotateTool4,    // 3D回転ツール4
        tBonesRemovalTool4, // 骨除去ツール4
        tRepulsorTool4,     // リパルサーツール4
        t3DPointTool4,     // 3Dポイントツール4
        t3DPolygonTool4    // 3D多角形ツール4
    }
    
    /**
     * コンストラクタ
     */
    public DCMView() {
        super();
        // TODO: 実装
    }
}
