/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.ui.table;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Created with IntelliJ IDEA.
 * User: ga2armn
 * Date: 5/2/13
 * Time: 8:28 AM
 */
public class ScrollToolBar extends FadingWindow {

    final int H = 30;
    final int W = 120;
    final int BOTTOM = 6;
    final int BTN_W = 15;
    final int CAP = 2;

    public static final int SCROLL_DELAY = 20;
    public static final int SCROLL_DX = 1;
    private int scrollDx = SCROLL_DX;
    protected int scrollRow = -1;

    protected static Stroke stroke = new BasicStroke(2);
    protected static Stroke normalStroke = new BasicStroke(1);
    protected static Stroke ticStroke = new BasicStroke(1);
    protected Point mousePoint = new Point();

    protected Color trackPaintClr = new Color(122f/255, 122f/255, 1);
    protected Color trackPaintClrTop = new Color(0.9f, 0.9f, 0.95f);
    protected Color trackPaintClrBottom =   new Color(0.3f, 0.3f, 0.5f);

    final protected JTable scrollableTable;

    protected JPanel panel;
    private Point lastMousePressPoint = new Point();
    private Rectangle firstVisRect = new Rectangle();


    class ScrollActionListener implements java.awt.event.ActionListener {
        private boolean incrementing = true;
        ScrollActionListener(){
        }
        public void actionPerformed(ActionEvent e) {
            Rectangle rect = scrollableTable.getVisibleRect();
            if(incrementing){
                rect.x -= scrollDx;
            }
            else{
                rect.x += scrollDx;
            }
            scrollableTable.scrollRectToVisible(rect);
            repaint();
            if(cantScroll()){
                scrollTimer.stop();
                checkButtonState();
            }
        }
        boolean isIncrementing() {
            return incrementing;
        }
        void setIncrementing(boolean incrementing) {
            this.incrementing = incrementing;
        }
    }


    ScrollActionListener scrollAction = new ScrollActionListener();
    Timer scrollTimer = new Timer(SCROLL_DELAY, scrollAction);

    enum State{DISABLED, HIGHLIGHTED, NORMAL};

    Action leftBtnAction = new AbstractAction(){
        public void actionPerformed(ActionEvent e) {
            resetMousePoint();
            Rectangle r = new Rectangle(firstVisRect);
            r.x = 0;
            scrollableTable.scrollRectToVisible(r);
            setEnabled(false);
        }
    };
    Action rightBtnAction = new AbstractAction(){
        public void actionPerformed(ActionEvent e) {
            resetMousePoint();
            Rectangle r = new Rectangle(firstVisRect);
            r.x = scrollableTable.getWidth() - firstVisRect.width;
            scrollableTable.scrollRectToVisible(r);
            setEnabled(false);
        }
    };
    Action midBtnAction = new AbstractAction(){
        public void actionPerformed(ActionEvent e) {
            resetMousePoint();
            scrollableTable.scrollRectToVisible(firstVisRect);
        }
    };


    class RectBtn{
        private State state = State.NORMAL;
        private  Rectangle rect;
        private Action action;
        RectBtn(Rectangle rect, Action action){
            this.rect = new Rectangle(rect);
            this.action = action;
        }
        Rectangle getRect() {
            return rect;
        }

        State getState() {
            if(!action.isEnabled()){
                return State.DISABLED;
            }
            return state;
        }

        void setState(boolean in, boolean canScroll) {
            if(!canScroll){
                this.state = State.DISABLED;
            }
            else{
                if(in){
                    state = State.HIGHLIGHTED;
                }
                else{
                    state = State.NORMAL;
                }
            }
            action.setEnabled(state != State.DISABLED);
        }
       void doPress(MouseEvent e){
           if(rect.contains(e.getPoint()) && state != State.DISABLED){
              action.actionPerformed(null);
           }
           if(!action.isEnabled()){
               state = State.DISABLED;
           }
       }
    }
    protected class ToolPanel extends JPanel{

        private int hBy2 = H/2;
        private int midLineH = hBy2-2;
        private int wBy2 = W/2;

        private GradientPaint paintTop = new GradientPaint(0,0, trackPaintClrTop,
                                                        0, hBy2-1, trackPaintClr, false);

        private GradientPaint paintBottom= new GradientPaint(0,hBy2+1, trackPaintClr,
                                                 0, H, trackPaintClrBottom, false);
        private GradientPaint paintBtns = new  GradientPaint(0, H - BOTTOM, trackPaintClrTop,
                                                   0, H, trackPaintClrBottom, false);

        private RectBtn leftBtn;
        private RectBtn rightBtn;
        private RectBtn midBtn;

        protected ToolPanel(){
            init();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, opacity));

            RoundRectangle2D rect = new RoundRectangle2D.Float(0,0,W, H - BOTTOM, CAP, 2*CAP);

            g2.setPaint(paintTop);
            g2.setClip(0, 0, (int) rect.getWidth(), (int) rect.getHeight() / 2);
            g2.fill(rect);
            g2.setPaint(paintBottom);
            g2.setClip(0, (int) rect.getHeight() / 2, (int) rect.getWidth(), (int) rect.getHeight());
            g2.fill(rect);

            g2.setPaint(paintBtns);
            g2.fillRect(CAP, H-BOTTOM, getWidth()-2*CAP, BOTTOM);
            g2.drawRect(CAP, H-BOTTOM, getWidth()-2*CAP, BOTTOM);

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);


            g2.setStroke(stroke);
            g2.setColor(Color.white);
            g2.drawLine(CAP, midLineH, getWidth()-CAP, midLineH);

            g2.setStroke(ticStroke);
            int dx = (getWidth() - 2*CAP)/4;
            for(int i=0; i<= 5; i++){
                int x = CAP + dx * i;
                g2.drawLine(x, midLineH-3, x, midLineH+3);
            }

            g2.setStroke(stroke);
            g2.setColor(Color.green);
            if(cantScroll()){
                g2.setColor(Color.red);
            }
            else{
                g2.setColor(Color.green);
            }
            int xPos = mousePoint.x;
            if(xPos < CAP){
                xPos = CAP;
            }
            else if (xPos > W-CAP){
               xPos = W-CAP;
            }

            g2.drawLine(wBy2, midLineH, xPos, midLineH);
            g2.setStroke(normalStroke);

            g2.setColor(getBtnColour(leftBtn));
            g2.fill(leftBtn.getRect());

            g2.setColor(getBtnColour(midBtn));
            g2.fill(midBtn.getRect());

            g2.setColor(getBtnColour(rightBtn));
            g2.fill(rightBtn.getRect());
        }

        Color getBtnColour(RectBtn btn){
            switch (btn.getState()){
                case NORMAL : return Color.BLUE;
                case DISABLED : return Color.GRAY;
                case HIGHLIGHTED : return Color.RED;
                default: return Color.BLUE;
            }
        }




        /**
         *
         */
        void init(){

            setPreferredSize(new Dimension(W, H));
            setOpaque(false);

            this.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    leftBtn.setState(leftBtn.getRect().contains(e.getPoint()), canScrollLeft());
                    rightBtn.setState(rightBtn.getRect().contains(e.getPoint()), canScrollRight());
                    midBtn.setState(midBtn.getRect().contains(e.getPoint()), canScrollLeft() || canScrollRight());
                    if (e.getPoint().y >= H - BOTTOM) {
                        scrollTimer.stop();
                        resetMousePoint();
                        repaint();
                        return;
                    }
                    mousePoint.setLocation(e.getPoint());

                    int row = ScrollToolBar.this.scrollableTable.rowAtPoint(e.getPoint());
                    if (row < 0) {
                        scrollTimer.stop();
                        return;
                    }
                    scrollRow = row;
                    int xPos = e.getPoint().getLocation().x;

                    if (xPos > getWidth() / 2) {// && rect.x + rect.width < scrollableTable.getWidth()){
                        scrollDx = SCROLL_DX * computeMultiplier(getWidth() / 2 - xPos);
                        scrollAction.setIncrementing(true);
                        if (!scrollTimer.isRunning()) {
                            scrollTimer.restart();
                        }
                    } else if (xPos < getWidth() / 2) {// &&  rect.x > 0){
                        scrollDx = SCROLL_DX * computeMultiplier(xPos - getWidth() / 2);
                        scrollAction.setIncrementing(false);
                        if (!scrollTimer.isRunning()) {
                            scrollTimer.restart();
                        }
                    } else {
                        scrollTimer.stop();
                    }
                    repaint();
                }
            });

            this.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    leftBtn.doPress(e);
                    midBtn.doPress(e);
                    rightBtn.doPress(e);
                    repaint();
                }

                public void mouseExited(MouseEvent e) {
                    scrollTimer.stop();
                    resetMousePoint();
                    repaint();
                }
                public void mouseEntered(MouseEvent e) {
                    scrollTimer.stop();
                    resetMousePoint();
                    repaint();
                }
            });

            leftBtn = new RectBtn(new Rectangle(0, H-BOTTOM, BTN_W, BOTTOM), leftBtnAction);
            rightBtn = new RectBtn(new Rectangle(W-BTN_W, H-BOTTOM, BTN_W, BOTTOM), rightBtnAction);
            midBtn = new RectBtn(new Rectangle(wBy2-BTN_W/2, H-BOTTOM, BTN_W, BOTTOM), midBtnAction);

        }
    }


    public ScrollToolBar(JTable jTable){

        super((JFrame) SwingUtilities.getRoot(jTable));
        this.scrollableTable = jTable;
        initTableListeners();

        panel = new ToolPanel();
        super.initPanel(panel);

    }


    /******************************************************
     * Setup table
     *****************************************************/

    void initTableListeners(){

        scrollableTable.addMouseListener(new MouseAdapter(){
            public void mousePressed(MouseEvent e) {
                stop();
                if(scrollableTable.isEditing()
                        || scrollableTable.getRowCount() == 0
                        || !isMetaKeyDown(e)
                        || !HighlightedTable.isTableMouseToolbarMetaOn()
                        || SwingUtilities.isRightMouseButton(e)){
                    return;
                }
                lastMousePressPoint.setLocation(e.getPoint());
                firstVisRect.setFrame(scrollableTable.getVisibleRect());

                int row = scrollableTable.rowAtPoint(e.getPoint());
                Rectangle rowRect = scrollableTable.getCellRect(row, 0, false);
                Point p = scrollableTable.getLocationOnScreen();
                p.x += e.getPoint().x - ScrollToolBar.this.getWidth() / 2;
                p.y += rowRect.y - ScrollToolBar.this.getHeight();
                ScrollToolBar.this.setLocation(p);
                ScrollToolBar.this.setVisible(true);
            }
        });
    }

    int computeMultiplier(int margin){
        return margin;
    }

    protected boolean isMetaKeyDown(MouseEvent e){
        return e.isAltDown();
    }

    protected void stop(){
       scrollTimer.stop();
       super.stop();
    }

    void resetMousePoint(){
        mousePoint.x = W/2;
    }
    public void setVisible(boolean vis) {

        if(vis){
            opacity = MAX_OPACITY;
            resetMousePoint();
        }
        super.setVisible(vis);
    }

    boolean cantScroll(){
        return  scrollableTable.getVisibleRect().x == 0 ||
                scrollableTable.getVisibleRect().x >= scrollableTable.getWidth() - scrollableTable.getVisibleRect().width;
    }
    boolean canScrollLeft(){
        return  scrollableTable.getVisibleRect().x > 0;
    }
    boolean canScrollRight(){
        return  scrollableTable.getVisibleRect().x < scrollableTable.getWidth() - scrollableTable.getVisibleRect().width;
    }
    void checkButtonState(){
        leftBtnAction.setEnabled(canScrollLeft());
        rightBtnAction.setEnabled(canScrollRight());
    }
}
