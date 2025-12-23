# 画像解析機能 - Java移植ドキュメント

## 1. 概要

このドキュメントは、HOROSの画像解析機能（ROI、測定、統計計算等）を解析し、Javaでの実装方法をまとめたものです。

## 2. ROI (Region of Interest)

### 2.1 ROIの種類（全17種類）

HOROSでは以下のROIタイプをサポートしています（`DCMView.h`の`ToolMode` enumより）：

#### 基本ROIタイプ（16種類）

1. **tMesure (5)**: 線（距離測定）
2. **tROI (6)**: 矩形（Rectangle）
3. **tOval (9)**: 楕円（Oval）
4. **tOPolygon (10)**: 開いた多角形（Open Polygon）
5. **tCPolygon (11)**: 閉じた多角形（Closed Polygon）
6. **tAngle (12)**: 角度測定（Angle）
7. **tText (13)**: テキストアノテーション（Text）
8. **tArrow (14)**: 矢印（Arrow）
9. **tPencil (15)**: フリーハンド（Pencil）
10. **t2DPoint (19)**: 2Dポイント（2D Point）
11. **tPlain (20)**: ブラシROI（Brush ROI）
12. **tLayerROI (24)**: レイヤーオーバーレイ（Layer Overlay）
13. **tAxis (26)**: 軸（Axis）
14. **tDynAngle (27)**: 動的角度（Dynamic Angle）
15. **tCurvedROI (28)**: 曲線ROI（Curved ROI）
16. **tTAGT (29)**: 2本の平行線と1本の垂直線（TAGT）

#### 3D/MPR関連ROIタイプ

17. **t3Dpoint (16)**: 3Dポイント（3D Point）
    - MPR（Multi-Planar Reconstruction）解析で使用
    - 3D空間でのポイント位置を定義
    - OrthogonalMPRViewer、CPRViewer、MPRControllerで使用

**注意**: 
- `t3DCut (17)`, `tCamera3D (18)`, `tBonesRemoval (21)`, `tWLBlended (22)`, `tRepulsor (23)`, `tROISelector (25)`はROI描画ツールではなく、他の機能用です。
- MPR解析では、通常の2D ROIタイプ（tROI、tOval、tCPolygon等）も使用可能です。

### 2.2 ROIタイプの詳細

**HOROS実装** (`ROI.h`):
```objective-c
/**
 * Region of Interest on a 2D Image:
 * Types:
 *  tMesure  = line
 *  tROI = Rectangle
 *  tOval = Oval
 *  tOPolygon = Open Polygon
 *  tCPolygon = Closed Polygon
 *  tAngle = Angle
 *  tText = Text
 *  tArrow = Arrow
 *  tPencil = Pencil
 *  t3Dpoint= 3D Point
 *  t2DPoint = 2D Point
 *  tPlain = Brush ROI
 *  tLayerROI = Layer Overlay
 *  tAxis = Axis
 *  tDynAngle = Dynamic Angle
 *  tTAGT = 2 paralles lines and 1 perpendicular line
 */
```

**各ROIタイプの説明**:

1. **tMesure (5)**: 線（距離測定）
   - 2点間の距離を測定
   - ピクセル単位または実世界単位（mm）で表示

2. **tROI (6)**: 矩形（Rectangle）
   - 矩形領域のROI
   - 統計計算が可能

3. **tOval (9)**: 楕円（Oval）
   - 楕円形のROI
   - 統計計算が可能

4. **tOPolygon (10)**: 開いた多角形（Open Polygon）
   - 閉じていない多角形
   - 複数の点で構成

5. **tCPolygon (11)**: 閉じた多角形（Closed Polygon）
   - 閉じた多角形
   - 統計計算が可能

6. **tAngle (12)**: 角度測定（Angle）
   - 3点で角度を測定
   - 度数で表示

7. **tText (13)**: テキストアノテーション（Text）
   - テキスト注釈
   - 統計計算不可

8. **tArrow (14)**: 矢印（Arrow）
   - 矢印の描画
   - 統計計算不可

9. **tPencil (15)**: フリーハンド（Pencil）
   - フリーハンド描画
   - 統計計算が可能

10. **t2DPoint (19)**: 2Dポイント（2D Point）
    - 単一の点
    - 座標表示

11. **tPlain (20)**: ブラシROI（Brush ROI）
    - ブラシで塗りつぶした領域
    - 統計計算が可能

12. **tLayerROI (24)**: レイヤーオーバーレイ（Layer Overlay）
    - 外部画像をレイヤーとして表示
    - 統計計算不可

13. **tAxis (26)**: 軸（Axis）
    - 座標軸の表示
    - 統計計算不可

14. **tDynAngle (27)**: 動的角度（Dynamic Angle）
    - 動的に角度を測定
    - マウス移動に応じて角度が変化

15. **tCurvedROI (28)**: 曲線ROI（Curved ROI）
    - 曲線（スプライン）で描画されたROI
    - 統計計算が可能

16. **tTAGT (29)**: 2本の平行線と1本の垂直線
    - 特殊な測定用ROI
    - 統計計算不可

17. **t3Dpoint (16)**: 3Dポイント（3D Point）
    - 3D空間でのポイント
    - MPR解析で使用

### 2.3 Java実装

```java
import java.awt.*;
import java.awt.geom.*;
import java.util.List;

public enum ROIType {
    MEASURE(5, "Measure", "線（距離測定）"),
    RECTANGLE(6, "Rectangle", "矩形"),
    OVAL(9, "Oval", "楕円"),
    OPEN_POLYGON(10, "Open Polygon", "開いた多角形"),
    CLOSED_POLYGON(11, "Closed Polygon", "閉じた多角形"),
    ANGLE(12, "Angle", "角度測定"),
    TEXT(13, "Text", "テキストアノテーション"),
    ARROW(14, "Arrow", "矢印"),
    PENCIL(15, "Pencil", "フリーハンド"),
    POINT_3D(16, "3D Point", "3Dポイント"),
    POINT_2D(19, "2D Point", "2Dポイント"),
    BRUSH(20, "Brush", "ブラシROI"),
    LAYER(24, "Layer", "レイヤーオーバーレイ"),
    AXIS(26, "Axis", "軸"),
    DYNAMIC_ANGLE(27, "Dynamic Angle", "動的角度"),
    CURVED(28, "Curved ROI", "曲線ROI"),
    TAGT(29, "TAGT", "2本の平行線と1本の垂直線");
    
    private final int code;
    private final String name;
    private final String description;
    
    ROIType(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
    
    public int getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    
    public boolean supportsStatistics() {
        return this == RECTANGLE || this == OVAL || 
               this == CLOSED_POLYGON || this == PENCIL || 
               this == BRUSH || this == CURVED;
    }
}

public abstract class ROI {
    protected ROIType type;
    protected String name;
    protected Color color = Color.YELLOW;
    protected boolean selected = false;
    protected List<Point2D> points;
    protected float thickness = 2.0f;
    protected boolean fill = false;
    protected float opacity = 1.0f;
    
    public ROI(ROIType type) {
        this.type = type;
        this.points = new ArrayList<>();
    }
    
    public abstract void draw(Graphics2D g2d, AffineTransform transform);
    public abstract boolean contains(Point2D point);
    public abstract Rectangle2D getBounds();
    
    public ROIStatistics computeStatistics(DicomPix pix) {
        if (!type.supportsStatistics()) {
            return null;
        }
        
        // ROI内のピクセル値を取得
        List<Float> pixelValues = getPixelValues(pix);
        
        if (pixelValues.isEmpty()) {
            return null;
        }
        
        ROIStatistics stats = new ROIStatistics();
        stats.setMean(calculateMean(pixelValues));
        stats.setStdDev(calculateStdDev(pixelValues, stats.getMean()));
        stats.setMin(Collections.min(pixelValues));
        stats.setMax(Collections.max(pixelValues));
        stats.setCount(pixelValues.size());
        
        return stats;
    }
    
    protected abstract List<Float> getPixelValues(DicomPix pix);
    
    private float calculateMean(List<Float> values) {
        return (float) values.stream()
            .mapToDouble(Float::doubleValue)
            .average()
            .orElse(0.0);
    }
    
    private float calculateStdDev(List<Float> values, float mean) {
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        return (float) Math.sqrt(variance);
    }
    
    public ROIType getType() { return type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}

// 矩形ROI
public class RectangleROI extends ROI {
    private Rectangle2D rect;
    
    public RectangleROI(double x, double y, double width, double height) {
        this.rect = new Rectangle2D.Double(x, y, width, height);
    }
    
    @Override
    public void draw(Graphics2D g2d, AffineTransform transform) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2.0f));
        
        Shape transformed = transform.createTransformedShape(rect);
        g2d.draw(transformed);
        
        if (selected) {
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 0, new float[]{5}, 0));
            g2d.draw(transformed);
        }
    }
    
    @Override
    public boolean contains(Point2D point) {
        return rect.contains(point);
    }
    
    @Override
    public Rectangle2D getBounds() {
        return rect.getBounds2D();
    }
    
    @Override
    protected List<Float> getPixelValues(DicomPix pix) {
        List<Float> values = new ArrayList<>();
        float[] pixelData = pix.getPixelData();
        int width = pix.getWidth();
        int height = pix.getHeight();
        
        int minX = (int) Math.max(0, rect.getMinX());
        int maxX = (int) Math.min(width - 1, rect.getMaxX());
        int minY = (int) Math.max(0, rect.getMinY());
        int maxY = (int) Math.min(height - 1, rect.getMaxY());
        
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (rect.contains(x, y)) {
                    values.add(pixelData[y * width + x]);
                }
            }
        }
        
        return values;
    }
}

// 多角形ROI
public class PolygonROI extends ROI {
    private Polygon2D polygon;
    
    public PolygonROI(List<Point2D> points) {
        this.points = new ArrayList<>(points);
        this.polygon = new Polygon2D();
        for (Point2D p : points) {
            polygon.addPoint((int) p.getX(), (int) p.getY());
        }
    }
    
    @Override
    public void draw(Graphics2D g2d, AffineTransform transform) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2.0f));
        
        GeneralPath path = new GeneralPath();
        if (!points.isEmpty()) {
            Point2D first = transform.transform(points.get(0), null);
            path.moveTo(first.getX(), first.getY());
            
            for (int i = 1; i < points.size(); i++) {
                Point2D p = transform.transform(points.get(i), null);
                path.lineTo(p.getX(), p.getY());
            }
            
            if (polygon.isClosed()) {
                path.closePath();
            }
        }
        
        g2d.draw(path);
        
        // ポイントの描画
        for (Point2D p : points) {
            Point2D transformed = transform.transform(p, null);
            g2d.fill(new Ellipse2D.Double(
                transformed.getX() - 3, transformed.getY() - 3, 6, 6));
        }
    }
    
    @Override
    public boolean contains(Point2D point) {
        return polygon.contains(point.getX(), point.getY());
    }
    
    @Override
    public Rectangle2D getBounds() {
        return polygon.getBounds2D();
    }
    
    @Override
    protected List<Float> getPixelValues(DicomPix pix) {
        List<Float> values = new ArrayList<>();
        float[] pixelData = pix.getPixelData();
        int width = pix.getWidth();
        int height = pix.getHeight();
        
        Rectangle2D bounds = polygon.getBounds2D();
        int minX = (int) Math.max(0, bounds.getMinX());
        int maxX = (int) Math.min(width - 1, bounds.getMaxX());
        int minY = (int) Math.max(0, bounds.getMinY());
        int maxY = (int) Math.min(height - 1, bounds.getMaxY());
        
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (polygon.contains(x, y)) {
                    values.add(pixelData[y * width + x]);
                }
            }
        }
        
        return values;
    }
}

// ROI統計情報
public class ROIStatistics {
    private float mean;
    private float stdDev;
    private float min;
    private float max;
    private int count;
    
    // getters and setters
    public float getMean() { return mean; }
    public void setMean(float mean) { this.mean = mean; }
    
    public float getStdDev() { return stdDev; }
    public void setStdDev(float stdDev) { this.stdDev = stdDev; }
    
    public float getMin() { return min; }
    public void setMin(float min) { this.min = min; }
    
    public float getMax() { return max; }
    public void setMax(float max) { this.max = max; }
    
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    
    @Override
    public String toString() {
        return String.format(
            "Mean: %.2f, StdDev: %.2f, Min: %.2f, Max: %.2f, Count: %d",
            mean, stdDev, min, max, count);
    }
}
```

## 3. 測定機能

### 3.1 距離測定

```java
public class DistanceROI extends ROI {
    private Point2D startPoint;
    private Point2D endPoint;
    
    public DistanceROI(Point2D start, Point2D end) {
        this.startPoint = start;
        this.endPoint = end;
        this.points = Arrays.asList(start, end);
    }
    
    @Override
    public void draw(Graphics2D g2d, AffineTransform transform) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2.0f));
        
        Point2D start = transform.transform(startPoint, null);
        Point2D end = transform.transform(endPoint, null);
        
        g2d.draw(new Line2D.Double(start, end));
        
        // 距離表示
        double distance = calculateDistance();
        Point2D midPoint = new Point2D.Double(
            (start.getX() + end.getX()) / 2,
            (start.getY() + end.getY()) / 2);
        
        String text = String.format("%.2f mm", distance);
        FontMetrics fm = g2d.getFontMetrics();
        Rectangle2D textBounds = fm.getStringBounds(text, g2d);
        
        g2d.setColor(Color.WHITE);
        g2d.fill(new Rectangle2D.Double(
            midPoint.getX() - textBounds.getWidth() / 2 - 2,
            midPoint.getY() - textBounds.getHeight() / 2 - 2,
            textBounds.getWidth() + 4,
            textBounds.getHeight() + 4));
        
        g2d.setColor(Color.BLACK);
        g2d.drawString(text,
            (float) (midPoint.getX() - textBounds.getWidth() / 2),
            (float) (midPoint.getY() + textBounds.getHeight() / 2));
    }
    
    private double calculateDistance() {
        // ピクセル距離を計算
        double dx = endPoint.getX() - startPoint.getX();
        double dy = endPoint.getY() - startPoint.getY();
        double pixelDistance = Math.sqrt(dx * dx + dy * dy);
        
        // 実世界距離に変換（ピクセル間隔を使用）
        // 注意: DicomPixからピクセル間隔を取得する必要がある
        return pixelDistance; // 仮実装
    }
    
    @Override
    public boolean contains(Point2D point) {
        // 線分との距離を計算
        double distance = pointToLineDistance(point, startPoint, endPoint);
        return distance < 5.0; // 5ピクセル以内
    }
    
    private double pointToLineDistance(Point2D point, 
                                       Point2D lineStart, 
                                       Point2D lineEnd) {
        double A = point.getX() - lineStart.getX();
        double B = point.getY() - lineStart.getY();
        double C = lineEnd.getX() - lineStart.getX();
        double D = lineEnd.getY() - lineStart.getY();
        
        double dot = A * C + B * D;
        double lenSq = C * C + D * D;
        double param = (lenSq != 0) ? dot / lenSq : -1;
        
        double xx, yy;
        if (param < 0) {
            xx = lineStart.getX();
            yy = lineStart.getY();
        } else if (param > 1) {
            xx = lineEnd.getX();
            yy = lineEnd.getY();
        } else {
            xx = lineStart.getX() + param * C;
            yy = lineStart.getY() + param * D;
        }
        
        double dx = point.getX() - xx;
        double dy = point.getY() - yy;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    @Override
    public Rectangle2D getBounds() {
        double minX = Math.min(startPoint.getX(), endPoint.getX());
        double maxX = Math.max(startPoint.getX(), endPoint.getX());
        double minY = Math.min(startPoint.getY(), endPoint.getY());
        double maxY = Math.max(startPoint.getY(), endPoint.getY());
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }
    
    @Override
    protected List<Float> getPixelValues(DicomPix pix) {
        // 距離測定ではピクセル値は使用しない
        return Collections.emptyList();
    }
}
```

### 3.2 角度測定

```java
public class AngleROI extends ROI {
    private Point2D vertex;
    private Point2D point1;
    private Point2D point2;
    
    public AngleROI(Point2D vertex, Point2D p1, Point2D p2) {
        this.vertex = vertex;
        this.point1 = p1;
        this.point2 = p2;
        this.points = Arrays.asList(vertex, p1, p2);
    }
    
    @Override
    public void draw(Graphics2D g2d, AffineTransform transform) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2.0f));
        
        Point2D v = transform.transform(vertex, null);
        Point2D p1 = transform.transform(point1, null);
        Point2D p2 = transform.transform(point2, null);
        
        // 線分を描画
        g2d.draw(new Line2D.Double(v, p1));
        g2d.draw(new Line2D.Double(v, p2));
        
        // 角度を計算して表示
        double angle = calculateAngle();
        String text = String.format("%.1f°", Math.toDegrees(angle));
        
        // 角度表示位置（2つの線分の中間）
        Point2D mid1 = new Point2D.Double(
            (v.getX() + p1.getX()) / 2,
            (v.getY() + p1.getY()) / 2);
        Point2D mid2 = new Point2D.Double(
            (v.getX() + p2.getX()) / 2,
            (v.getY() + p2.getY()) / 2);
        Point2D textPos = new Point2D.Double(
            (mid1.getX() + mid2.getX()) / 2,
            (mid1.getY() + mid2.getY()) / 2);
        
        FontMetrics fm = g2d.getFontMetrics();
        Rectangle2D textBounds = fm.getStringBounds(text, g2d);
        
        g2d.setColor(Color.WHITE);
        g2d.fill(new Rectangle2D.Double(
            textPos.getX() - textBounds.getWidth() / 2 - 2,
            textPos.getY() - textBounds.getHeight() / 2 - 2,
            textBounds.getWidth() + 4,
            textBounds.getHeight() + 4));
        
        g2d.setColor(Color.BLACK);
        g2d.drawString(text,
            (float) (textPos.getX() - textBounds.getWidth() / 2),
            (float) (textPos.getY() + textBounds.getHeight() / 2));
    }
    
    private double calculateAngle() {
        double dx1 = point1.getX() - vertex.getX();
        double dy1 = point1.getY() - vertex.getY();
        double dx2 = point2.getX() - vertex.getX();
        double dy2 = point2.getY() - vertex.getY();
        
        double angle1 = Math.atan2(dy1, dx1);
        double angle2 = Math.atan2(dy2, dx2);
        
        double angle = Math.abs(angle1 - angle2);
        if (angle > Math.PI) {
            angle = 2 * Math.PI - angle;
        }
        
        return angle;
    }
    
    @Override
    public boolean contains(Point2D point) {
        // 頂点または線分に近いかチェック
        return vertex.distance(point) < 5.0 ||
               new DistanceROI(vertex, point1).contains(point) ||
               new DistanceROI(vertex, point2).contains(point);
    }
    
    @Override
    public Rectangle2D getBounds() {
        double minX = Math.min(Math.min(vertex.getX(), point1.getX()), point2.getX());
        double maxX = Math.max(Math.max(vertex.getX(), point1.getX()), point2.getX());
        double minY = Math.min(Math.min(vertex.getY(), point1.getY()), point2.getY());
        double maxY = Math.max(Math.max(vertex.getY(), point1.getY()), point2.getY());
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }
    
    @Override
    protected List<Float> getPixelValues(DicomPix pix) {
        return Collections.emptyList();
    }
}
```

## 4. ROI管理

```java
public class ROIManager {
    private List<ROI> rois = new ArrayList<>();
    private ROI selectedROI;
    
    public void addROI(ROI roi) {
        rois.add(roI);
    }
    
    public void removeROI(ROI roi) {
        rois.remove(roi);
        if (selectedROI == roi) {
            selectedROI = null;
        }
    }
    
    public ROI findROIAt(Point2D point) {
        // 逆順で検索（最後に追加されたROIを優先）
        for (int i = rois.size() - 1; i >= 0; i--) {
            ROI roi = rois.get(i);
            if (roi.contains(point)) {
                return roi;
            }
        }
        return null;
    }
    
    public void selectROI(ROI roi) {
        if (selectedROI != null) {
            selectedROI.setSelected(false);
        }
        selectedROI = roi;
        if (roi != null) {
            roi.setSelected(true);
        }
    }
    
    public void drawAll(Graphics2D g2d, AffineTransform transform) {
        for (ROI roi : rois) {
            roi.draw(g2d, transform);
        }
    }
    
    public List<ROI> getROIs() {
        return Collections.unmodifiableList(rois);
    }
}
```

## 5. まとめ

画像解析機能のJava実装では：

1. **ROI**: 様々な形状のROI実装
2. **統計計算**: 平均、標準偏差、最小、最大値
3. **測定機能**: 距離、角度測定
4. **ROI管理**: ROIの追加、削除、選択

次のステップ: dcm4cheライブラリ使用ガイド

