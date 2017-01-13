package org.bhavaya.ui.table;

import javax.swing.table.TableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.event.TableModelEvent;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;


/**
 * There's currently a really icky defect in the way JTable processes TableModel Events after a custom row height is set.
 * Essentially, once the JTable's internal rowModel is initialized to handle this, subsequent update events always cause
 * the entire JTable to get repainted. This makes table painting less efficient by a factor of 100 or so, makes traders
 * gnash their teeth and start to foam at the mouth, and causes sleepless nights for the development team. 
 * A bug report is submitted. We hold our breath.
 *
 * This class extends JTable and overrides tableChanged, in order to work around the broken repainting for updates when
 * there are custom height rows.
 * See http://www.objectdefinitions.com/odblog/2009/jtable-setrowheight-causes-slow-repainting/  for more details
 *
 * This fix does not support row sorting in jdk1.6 -
 * to support that would require firing sortedTableChanged to sortManager
 */
public class FixedForCustomRowHeightsJTable extends JTable {



    protected int oldHoverRow = -1;
    protected int hoverRow = -1;

    public final static Color TABLE_TRACK_COLOUR    = new Color(102f/255, 102f/255, 1, 0.1f);

    protected static Color trackPaintClr = new Color(102f/255, 102f/255, 1, 0.1f);
    protected static Color trackPaintClrTop = new Color(0.95f, 0.95f, 0.95f, 0.2f);
    protected static Color trackPaintClrBottom =   new Color(0.1f, 0.1f, 0.1f, 0.2f);

    protected static boolean rowTrackingOn = true;
    protected static boolean rowTrack3DOn = true;
    protected static boolean rowSelected3DOn = true;
    protected static boolean tableMouseToolbarMetaOn = true;


    class TableBufferedImage extends BufferedImage{

        private boolean trackingEnabled = true;
        private boolean r3DEnabled = true;
        private Color paintClr;

        public TableBufferedImage(int width, int height, int imageType,
                                  boolean trackingEnabled,
                                  boolean r3DEnabled,
                                  Color paintClr) {
            super(width, height, imageType);
            this.trackingEnabled = trackingEnabled;
            this.r3DEnabled = r3DEnabled;
            this.paintClr =  paintClr;

            init();

        }

        private void init(){
            Graphics2D g2d = createGraphics();

            if(this.r3DEnabled){
                GradientPaint paintTop = new GradientPaint(0,0, trackPaintClrTop,
                        0, getRowHeight()/2-1, trackPaintClr, false);

                GradientPaint paintBottom = new GradientPaint(0,getRowHeight()/2+1, trackPaintClr,
                        0, getRowHeight(), trackPaintClrBottom, false);


                g2d.setPaint(paintTop);
                g2d.fillRect(0, 0, 2, getRowHeight()/2);
                g2d.setPaint(paintBottom);
                g2d.fillRect(0, getRowHeight()/2, 2, getRowHeight()/2);
            }
            else{
                g2d.setColor(this.paintClr);
                g2d.fillRect(0, 0, 2, getRowHeight());
            }
        }

        boolean isTrackingEnabled() {
            return trackingEnabled;
        }

        boolean isR3DEnabled() {
            return r3DEnabled;
        }

        Color getPaintClr() {
            return paintClr;
        }
    };
    protected TableBufferedImage cache;



    void validateCache(){
        if (cache == null
                || cache.getHeight() != getRowHeight()
                || cache.isTrackingEnabled() != rowTrackingOn
                || cache.isR3DEnabled() != rowTrack3DOn
                || !cache.getPaintClr().equals(trackPaintClr)) {

            cache = new TableBufferedImage(2, getRowHeight(),
                                           BufferedImage.TYPE_4BYTE_ABGR,
                    rowTrackingOn,
                    rowTrack3DOn,
                    trackPaintClr);
        }
     }

    public interface HoverListener{
        void hoverMoved(int row);
    }

    java.util.List<HoverListener> hoverListeners = new ArrayList<HoverListener>();

    public void addHoverListener(HoverListener l){
        hoverListeners.add(l)  ;
    }

    void fireHoverListeners(){
        for(HoverListener h  : hoverListeners){
            h.hoverMoved(hoverRow);
        }
    }

    public static Color getTrackPaintClr() {
        return trackPaintClr;
    }

    public static void setTrackPaintClr(Color trackPaintClr) {
        FixedForCustomRowHeightsJTable.trackPaintClr = trackPaintClr;
    }

    public static boolean isRowTrackingOn() {
        return rowTrackingOn;
    }

    public static void setRowTrackingOn(boolean rowTrackingOn) {
        FixedForCustomRowHeightsJTable.rowTrackingOn = rowTrackingOn;
    }

    public static boolean isRowTrack3DOn() {
        return rowTrack3DOn;
    }

    public static void setRowTrack3DOn(boolean rowTrack3DOn) {
        FixedForCustomRowHeightsJTable.rowTrack3DOn = rowTrack3DOn;
    }

    public static boolean isRowSelected3DOn() {
        return rowSelected3DOn;
    }

    public static void setRowSelected3DOn(boolean rowSelected3DOn) {
        FixedForCustomRowHeightsJTable.rowSelected3DOn = rowSelected3DOn;
    }

    public static void setTableMouseToolbarMetaOn(boolean tableMouseToolbarMetaOn) {
        FixedForCustomRowHeightsJTable.tableMouseToolbarMetaOn = tableMouseToolbarMetaOn;
    }

    public static boolean isTableMouseToolbarMetaOn() {
        return tableMouseToolbarMetaOn;
    }

    /******************************************
     *
     * @param g
     *****************************************/
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Rectangle clip = g.getClipBounds();
        Graphics2D g2 = (Graphics2D)g;

        if(rowTrackingOn && hoverRow >=0){
            validateCache();
            Rectangle r = getRepaintRow(hoverRow);
            if(r.intersects(clip)){
                g2.drawImage(cache, 0, r.getLocation().y, getWidth(), getRowHeight(), null);
            }
        }

        if(rowSelected3DOn && this.getSelectedRows().length >=0){
            validateCache();
            for(int row : getSelectedRows()){
                Rectangle r = this.getCellRect(row, -1, true);
                if(r.y + r.height > clip.y && r.y < clip.y + clip.height){
                    g2.drawImage(cache, 0, r.getLocation().y, getWidth(), getRowHeight(), null);
                }
            }
        }

    }

    public void setHoverRow(int hoverRow) {
        oldHoverRow = this.hoverRow;
        this.hoverRow = hoverRow;
        repaint(getRepaintRow(oldHoverRow));
        repaint(getRepaintRow(hoverRow));
    }

    protected Rectangle getRepaintRow(int row){
        int h = getRowHeight();
        return new Rectangle(0, h * row, getWidth(), h);
    }

    {
        this.addMouseMotionListener(new MouseMotionListener(){

            public void mouseDragged(MouseEvent e) {
            }
            public void mouseMoved(MouseEvent e) {
                setHoverRow(rowAtPoint(e.getPoint()));
                fireHoverListeners();
            }
        });
        this.addMouseListener(new MouseListener(){
            public void mouseClicked(MouseEvent e) {
            }
            public void mousePressed(MouseEvent e) {
            }
            public void mouseReleased(MouseEvent e) {
            }
            public void mouseEntered(MouseEvent e) {
            }
            public void mouseExited(MouseEvent e) {
                hoverRow = -1;
                repaint(getRepaintRow(oldHoverRow));
                fireHoverListeners();
            }
        });
    }

    public FixedForCustomRowHeightsJTable() {
    }


    public FixedForCustomRowHeightsJTable(TableModel tableModel) {
        super(tableModel);
    }

    public FixedForCustomRowHeightsJTable(TableModel tableModel, TableColumnModel tableColumnModel) {
        super(tableModel, tableColumnModel);
    }

    public void tableChanged(TableModelEvent e) {
        //if just an update, and not a data or structure changed event or an insert or delete, use the fixed row update handling
        //otherwise call super.tableChanged to let the standard JTable update handling manage it
        if ( e != null &&
            e.getType() == TableModelEvent.UPDATE &&
            e.getFirstRow() != TableModelEvent.HEADER_ROW &&
            e.getLastRow() != Integer.MAX_VALUE) {

            handleRowUpdate(e);
        } else {
            super.tableChanged(e);
        }
    }

    /**
     * This borrows most of the logic from the superclass handling of update events, but changes the calculation of the height
     * for the dirty region to provide proper handling for repainting custom height rows
     */
    private void handleRowUpdate(TableModelEvent e) {
        int modelColumn = e.getColumn();
        int start = e.getFirstRow();
        int end = e.getLastRow();

        Rectangle dirtyRegion;
        if (modelColumn == TableModelEvent.ALL_COLUMNS) {
            // 1 or more rows changed
            dirtyRegion = new Rectangle(0, start * getRowHeight(),
                                        getColumnModel().getTotalColumnWidth(), 0);
        }
        else {
            // A cell or column of cells has changed.
            // Unlike the rest of the methods in the JTable, the TableModelEvent
            // uses the coordinate system of the model instead of the view.
            // This is the only place in the JTable where this "reverse mapping"
            // is used.
            int column = convertColumnIndexToView(modelColumn);
            dirtyRegion = getCellRect(start, column, false);
        }

        // Now adjust the height of the dirty region
        dirtyRegion.height = 0;
        for ( int row=start; row <= end; row ++ ) {
            dirtyRegion.height += getRowHeight(row);  //THIS IS CHANGED TO CALCULATE THE DIRTY REGION HEIGHT CORRECTLY
        }
        repaint(dirtyRegion.x, dirtyRegion.y, dirtyRegion.width, dirtyRegion.height);
    }

}

