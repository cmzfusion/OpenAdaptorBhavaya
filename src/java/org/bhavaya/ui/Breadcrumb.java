package org.bhavaya.ui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;

/**
 * Created with IntelliJ IDEA.
 * User: ga2armn
 * Date: 4/26/13
 * Time: 10:18 AM
 */
public class Breadcrumb extends JLabel {

    public static final Color BACKGROUND_CLR = new Color(200,200,255);
    public static final Color HIGHLIGHT_CLR = new Color(220,220,255);

    DefaultMutableTreeNode tNode;
    JTree jTree;
    boolean highlighted = false;

    {
        this.setBorder(BorderFactory.createEmptyBorder(1,4,1,4));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        RoundRectangle2D r = new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 15, 15);
        if(highlighted){
            g2.setColor(HIGHLIGHT_CLR);
        }
        else{
            g2.setColor(BACKGROUND_CLR);
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2.fill(r);
        g2.setColor(Color.gray);
        g2.draw(r);
        super.paintComponent(g);
    }

    public Breadcrumb(JTree tree, DefaultMutableTreeNode tn){
        this.jTree = tree;
        this.tNode = tn;

        if (tn instanceof BeanPropertyTreeNode){
            BeanPropertyTreeNode bp = (BeanPropertyTreeNode)tn;
            this.setText(bp.getDisplayName());
        }
        else{
           this.setText(tn.toString());
        }

        this.addMouseListener(new MouseListener(){
            public void mouseClicked(MouseEvent e) {
                TreePath tp = new TreePath(tNode.getPath());
                jTree.setSelectionPath(tp);
                jTree.scrollPathToVisible(tp);
            }
            public void mousePressed(MouseEvent e) {
            }
            public void mouseReleased(MouseEvent e) {
            }
            public void mouseEntered(MouseEvent e) {
                highlighted = true;
                repaint();
            }
            public void mouseExited(MouseEvent e) {
                highlighted = false;
                repaint();
            }
        });
   }

}
