package com.jj.dicomviewer.model;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.jj.dicomviewer.ui.DCMView;

/**
 * ROI - Region of Interest
 * 
 * HOROS-20240407のROIをJava Swingに移植
 */
public class ROI {
    
    // HOROS: NSRecursiveLock *roiLock
    private ReentrantLock roiLock;
    
    // HOROS: NSMutableArray *points
    private List<Point2D.Float> points;
    
    // HOROS: NSRect rect
    private Rectangle rect;
    
    // HOROS: ToolMode type
    private DCMView.ToolMode type;
    
    // HOROS: long mode
    private int mode;
    
    // HOROS: float thickness
    private float thickness = 1.0f;
    
    // HOROS: BOOL fill
    private boolean fill = false;
    
    // HOROS: float opacity
    private float opacity = 1.0f;
    
    // HOROS: RGBColor color
    private Color color = Color.YELLOW;
    
    // HOROS: BOOL closed
    private boolean closed = false;
    
    // HOROS: NSString *name
    private String name = "";
    
    // HOROS: NSString *comments
    private String comments = "";
    
    // HOROS: double pixelSpacingX, pixelSpacingY
    private double pixelSpacingX = 1.0;
    private double pixelSpacingY = 1.0;
    
    // HOROS: DCMView *curView
    private DCMView curView;
    
    // HOROS: DCMPix *pix
    private DicomPix pix;
    
    // HOROS: BOOL locked, selectable, hidden
    private boolean locked = false;
    private boolean selectable = true;
    private boolean hidden = false;
    
    // HOROS: float rmean, rmax, rmin, rdev, rtotal
    private float rmean, rmax, rmin, rdev, rtotal;
    
    /**
     * Constructor
     */
    public ROI() {
        roiLock = new ReentrantLock();
        points = new ArrayList<>();
        rect = new Rectangle();
    }
    
    /**
     * HOROS: - (id)initWithType:(ToolMode)itype :(float)ipixelSpacing :(NSPoint)iimageOrigin
     */
    public ROI(DCMView.ToolMode type, float pixelSpacingX, float pixelSpacingY, Point2D.Float imageOrigin) {
        this();
        this.type = type;
        this.pixelSpacingX = pixelSpacingX;
        this.pixelSpacingY = pixelSpacingY;
    }
    
    /**
     * HOROS: - (void)addPoint:(NSPoint)pt
     */
    public void addPoint(Point2D.Float pt) {
        roiLock.lock();
        try {
            points.add(pt);
            updateRect();
        } finally {
            roiLock.unlock();
        }
    }
    
    /**
     * HOROS: - (void)setPoints:(NSMutableArray*)pts
     */
    public void setPoints(List<Point2D.Float> pts) {
        roiLock.lock();
        try {
            points.clear();
            points.addAll(pts);
            updateRect();
        } finally {
            roiLock.unlock();
        }
    }
    
    /**
     * Get points
     */
    public List<Point2D.Float> getPoints() {
        return new ArrayList<>(points);
    }
    
    /**
     * Update bounding rectangle
     */
    private void updateRect() {
        if (points.isEmpty()) {
            rect = new Rectangle();
            return;
        }
        
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        
        for (Point2D.Float pt : points) {
            if (pt.x < minX) minX = pt.x;
            if (pt.y < minY) minY = pt.y;
            if (pt.x > maxX) maxX = pt.x;
            if (pt.y > maxY) maxY = pt.y;
        }
        
        rect = new Rectangle((int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY));
    }
    
    /**
     * HOROS: - (void)drawROI:(CGContextRef)ctx
     */
    public void draw(Graphics2D g2d) {
        if (hidden || points.isEmpty()) {
            return;
        }
        
        g2d.setColor(color);
        
        switch (type) {
            case tMesure:
                // Draw line measurement
                if (points.size() >= 2) {
                    Point2D.Float p1 = points.get(0);
                    Point2D.Float p2 = points.get(1);
                    g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
                    
                    // Draw measurement text
                    double length = calculateLength();
                    String text = String.format("%.2f mm", length);
                    g2d.drawString(text, (int) ((p1.x + p2.x) / 2), (int) ((p1.y + p2.y) / 2) - 5);
                }
                break;
                
            case tOval:
                // Draw oval
                g2d.drawOval(rect.x, rect.y, rect.width, rect.height);
                if (fill) {
                    g2d.fillOval(rect.x, rect.y, rect.width, rect.height);
                }
                break;
                
            case tCPolygon:
            case tOPolygon:
                // Draw polygon
                if (points.size() >= 2) {
                    int[] xPoints = new int[points.size()];
                    int[] yPoints = new int[points.size()];
                    for (int i = 0; i < points.size(); i++) {
                        xPoints[i] = (int) points.get(i).x;
                        yPoints[i] = (int) points.get(i).y;
                    }
                    if (closed || type == DCMView.ToolMode.tCPolygon) {
                        g2d.drawPolygon(xPoints, yPoints, points.size());
                        if (fill) {
                            g2d.fillPolygon(xPoints, yPoints, points.size());
                        }
                    } else {
                        g2d.drawPolyline(xPoints, yPoints, points.size());
                    }
                }
                break;
                
            case tAngle:
                // Draw angle
                if (points.size() >= 3) {
                    Point2D.Float p1 = points.get(0);
                    Point2D.Float p2 = points.get(1);
                    Point2D.Float p3 = points.get(2);
                    g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
                    g2d.drawLine((int) p2.x, (int) p2.y, (int) p3.x, (int) p3.y);
                    
                    // Draw angle text
                    double angle = calculateAngle();
                    String text = String.format("%.1f°", angle);
                    g2d.drawString(text, (int) p2.x + 5, (int) p2.y - 5);
                }
                break;
                
            case tText:
                // Draw text
                if (!points.isEmpty()) {
                    Point2D.Float p = points.get(0);
                    g2d.drawString(name, (int) p.x, (int) p.y);
                }
                break;
                
            case tArrow:
                // Draw arrow
                if (points.size() >= 2) {
                    Point2D.Float p1 = points.get(0);
                    Point2D.Float p2 = points.get(1);
                    g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
                    // Draw arrowhead
                    drawArrowHead(g2d, p1, p2);
                }
                break;
                
            default:
                // Draw points
                for (Point2D.Float pt : points) {
                    g2d.fillOval((int) pt.x - 2, (int) pt.y - 2, 4, 4);
                }
                break;
        }
    }
    
    /**
     * Draw arrow head
     */
    private void drawArrowHead(Graphics2D g2d, Point2D.Float from, Point2D.Float to) {
        double angle = Math.atan2(to.y - from.y, to.x - from.x);
        int arrowSize = 10;
        
        int x1 = (int) (to.x - arrowSize * Math.cos(angle - Math.PI / 6));
        int y1 = (int) (to.y - arrowSize * Math.sin(angle - Math.PI / 6));
        int x2 = (int) (to.x - arrowSize * Math.cos(angle + Math.PI / 6));
        int y2 = (int) (to.y - arrowSize * Math.sin(angle + Math.PI / 6));
        
        g2d.drawLine((int) to.x, (int) to.y, x1, y1);
        g2d.drawLine((int) to.x, (int) to.y, x2, y2);
    }
    
    /**
     * Calculate length (for measurement ROI)
     */
    public double calculateLength() {
        if (points.size() < 2) {
            return 0;
        }
        
        Point2D.Float p1 = points.get(0);
        Point2D.Float p2 = points.get(1);
        
        double dx = (p2.x - p1.x) * pixelSpacingX;
        double dy = (p2.y - p1.y) * pixelSpacingY;
        
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calculate angle (for angle ROI)
     */
    public double calculateAngle() {
        if (points.size() < 3) {
            return 0;
        }
        
        Point2D.Float p1 = points.get(0);
        Point2D.Float p2 = points.get(1);
        Point2D.Float p3 = points.get(2);
        
        double angle1 = Math.atan2(p1.y - p2.y, p1.x - p2.x);
        double angle2 = Math.atan2(p3.y - p2.y, p3.x - p2.x);
        
        double angle = Math.abs(Math.toDegrees(angle2 - angle1));
        if (angle > 180) {
            angle = 360 - angle;
        }
        
        return angle;
    }
    
    // Getters and Setters
    
    public DCMView.ToolMode getType() {
        return type;
    }
    
    public void setType(DCMView.ToolMode type) {
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Color getColor() {
        return color;
    }
    
    public void setColor(Color color) {
        this.color = color;
    }
    
    public float getOpacity() {
        return opacity;
    }
    
    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }
    
    public boolean isFill() {
        return fill;
    }
    
    public void setFill(boolean fill) {
        this.fill = fill;
    }
    
    public boolean isLocked() {
        return locked;
    }
    
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
    
    public boolean isHidden() {
        return hidden;
    }
    
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
    
    public boolean isSelectable() {
        return selectable;
    }
    
    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }
    
    public Rectangle getRect() {
        return rect;
    }
    
    public void setPixelSpacing(double x, double y) {
        this.pixelSpacingX = x;
        this.pixelSpacingY = y;
    }
    
    public void setCurView(DCMView view) {
        this.curView = view;
    }
    
    public void setPix(DicomPix pix) {
        this.pix = pix;
    }
}

