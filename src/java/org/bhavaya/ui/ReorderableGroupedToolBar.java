package org.bhavaya.ui;

import org.bhavaya.ui.ghost.*;
import org.bhavaya.util.ImageIconCache;
import org.bhavaya.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Jonathan Moore
 * Date: 13-Feb-2008
 * Time: 13:51:38
 *
 * Extension of GroupedToolBar which allows drag and drop reordering of groups
 */
public class ReorderableGroupedToolBar extends GroupedToolBar {

    private static final ImageIcon EXPAND_ICON = ImageIconCache.getImageIcon("expand.gif");
    private static final ImageIcon COLLAPSE_ICON = ImageIconCache.getImageIcon("collapse.gif");

    private JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
        @Override
        public Dimension getPreferredSize() {
            int parentWidth = getParent().getWidth();
            Insets parentInsets = getParent().getInsets();
            int maxWidth = parentWidth - parentInsets.left - parentInsets.right - buttonPanel.getWidth();
            return new Dimension(parentWidth, getMainPanelFlowLayoutHeight(maxWidth));
        }
    };
    private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private boolean expanded = false;
    private ExpandButton expandButton = new ExpandButton();

    private GhostGlassPane glassPane = new GhostGlassPane();

    public ReorderableGroupedToolBar(RootPaneContainer rootPaneContainer) {
        super();
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                //Make sure the WrappedFlowLayout gets resized correctly
                revalidate();
                expandButton.setVisible(hasMultipleRows());
            }
        });

        setLayout(new BorderLayout(2, 0));
        setOpaque(false);

        //Expand button
        expandButton.setActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setExpanded(!expanded);
            }
        });

        Dimension d = new Dimension(20, 20);
        buttonPanel.setMinimumSize(d);
        buttonPanel.setMaximumSize(d);
        buttonPanel.setPreferredSize(d);
        buttonPanel.add(expandButton);
        add(buttonPanel, BorderLayout.EAST);

        add(mainPanel, BorderLayout.CENTER);

        Component existingGlassPane = rootPaneContainer.getGlassPane();
        if(existingGlassPane instanceof GhostGlassPane) {
            //We've already set the ghost pane for this container, so don't set it again
            //as the Workspace associates toggle actions for the menu and tool bars
            //with the glass pane
            glassPane = (GhostGlassPane)existingGlassPane;
        } else {
            //Need to ensure any actions that were set on the previous glass pane are migrated
            JComponent panel = (JComponent)existingGlassPane;
            glassPane.setInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW, panel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW));
            glassPane.setActionMap(panel.getActionMap());
            rootPaneContainer.setGlassPane(glassPane);
        }
    }

    private boolean hasMultipleRows() {
        int lastx = -1;
        for(int i=0; i<mainPanel.getComponentCount(); i++) {
            Rectangle r = mainPanel.getComponent(i).getBounds();
            if(r.x <= lastx) {
                return true;
            }
            lastx = r.x;
        }
        return false;
    }

    public void addToolBarGroup(ToolBarGroup toolBarGroup) {
        if(!toolBarGroup.isEmpty()) {
            mainPanel.add(new ToolBarGroupPanel(toolBarGroup));
        }
    }

    public List<String> getOrder() {
        List<String> order = new ArrayList<String>(mainPanel.getComponentCount());
        for(Component component : mainPanel.getComponents()) {
            if(component instanceof ToolBarGroupPanel) {
                order.add(((ToolBarGroupPanel)component).getToolBarGroup().getId());
            }
        }
        return order;
    }

    class ToolBarGroupPanel extends JPanel {
        private Component grip;
        private ToolBarGroup toolBarGroup;

        ToolBarGroupPanel(ToolBarGroup toolBarGroup) {
            super(new BorderLayout(2, 2));
            this.grip = createGrip();
            this.toolBarGroup = toolBarGroup;
            add(grip, BorderLayout.WEST);
            JToolBar centre = new JToolBar();
            centre.setFloatable(false);
            centre.setBorderPainted(false);
            centre.setOpaque(false);
            add(centre);
            for (ToolBarGroup.Element element : toolBarGroup.getElements()) {
                element.applyAdd(centre);
            }
            GhostDropListener listener = new GhostDropListener() {
                public void ghostDropped(Point p) {
                    drop(getToolBarGroup(), p);
	            }
            };
            GhostComponentAdapter componentAdapter = new GhostComponentAdapter(glassPane, listener);
            addMouseListener(componentAdapter);
            addMouseMotionListener(new GhostMotionAdapter(glassPane, mainPanel));
        }

        public ToolBarGroup getToolBarGroup() {
            return toolBarGroup;
        }
    }

    private void drop(ToolBarGroup draggedGroup, Point pt) {
        if(draggedGroup != null) {
            SwingUtilities.convertPointFromScreen(pt, mainPanel);
            Component c = mainPanel.getComponentAt(pt);
            if(c instanceof ToolBarGroupPanel || c == mainPanel) {
                try {
                    moveGroupToIndex(draggedGroup, getComponentIndexAtPoint(pt));
                } finally {
                    draggedGroup = null;
                }
            }
        }
    }

    protected int getComponentIndexAtPoint(Point p) {
        int lastx = 0;
        for(int i=0; i<mainPanel.getComponentCount(); i++) {
            Rectangle r = mainPanel.getComponent(i).getBounds();
            if(p.x <= r.x + r.width && p.y <= r.y + r.height) {
                //To the left of this component
                return i;
            }
            if (r.x <= lastx && p.y <= r.y) {
                //We've wrapped and the drop is at the end of the previous row
                return i;
            }
            lastx = r.x;
        }
        //To the far right of bottom row
        return mainPanel.getComponentCount();
    }

    public void moveGroupToIndex(ToolBarGroup group, int newIndex) {
        int currentIndex = getIndexOfGroup(group);

        if(currentIndex > -1 && newIndex > -1) {
            if(currentIndex < newIndex) {
                newIndex--;
            }
            Component toMove = mainPanel.getComponent(currentIndex);
            mainPanel.remove(toMove);
            mainPanel.add(toMove, newIndex);
            mainPanel.revalidate();
            firePropertyChange("order", null, getOrder());
        }
    }

    private int getIndexOfGroup(ToolBarGroup group) {
        for(int i=0; i<mainPanel.getComponentCount(); i++) {
            Component c = mainPanel.getComponent(i);
            if(c instanceof ToolBarGroupPanel) {
                ToolBarGroupPanel panel = (ToolBarGroupPanel)c;
                if(Utilities.equals(group, panel.getToolBarGroup())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private Component createGrip() {
        return new ToolbarBump();
    }


    public int getMainPanelFlowLayoutHeight(int maxWidth) {
        Insets insets = mainPanel.getInsets();
        int width = insets.left + insets.right;
        int height = insets.top + insets.bottom;
        int rowHeight = 0;

        int componentCount = mainPanel.getComponentCount();

        int startOfCurrentRow = 0;
        for (int i = 0; i < componentCount; i++) {
            Component component = mainPanel.getComponent(i);
            if (component.isVisible()) {
                Dimension d = component.getPreferredSize();
                if(width + d.width < maxWidth) {
                    width += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                } else {
                    //First make sure all components in the previous row are the same height
                    resizeRowHeight(startOfCurrentRow, i, rowHeight);
                    if(!expanded) {
                        return height + rowHeight;
                    }
                    startOfCurrentRow = i;
                    height += rowHeight;
                    rowHeight = d.height;
                    width = insets.left + insets.right + d.width;
                }
            }
        }
        resizeRowHeight(startOfCurrentRow, componentCount, rowHeight);
        return height + rowHeight;
    }

    private void resizeRowHeight(int start, int end, int height) {
        if(height > 0) {
            //Make sure all components in the previous row are the same height
            end = Math.min(end, mainPanel.getComponentCount());
            for(int j=start; j<end; j++) {
                Component component2 = mainPanel.getComponent(j);
                if (component2.isVisible()) {
                    Dimension d2 = component2.getPreferredSize();
                    component2.setPreferredSize(new Dimension(d2.width, height));
                }
            }
        }
    }

    /** Bumps for floatable toolbar */
    private final class ToolbarBump extends JPanel {
        /** Top gap. */
        static final int TOPGAP = 3;
        /** Bottom gap. */
        static final int BOTGAP = 3;
        /** Width of bump element. */
        static final int WIDTH = 10;

        /** Create new ToolbarBump. */
        public ToolbarBump () {
        }

        /** Paint bumps to specific Graphics. */
        public void paint (Graphics g) {
            Dimension size = this.getSize ();
            int height = size.height - BOTGAP;
            g.setColor (this.getBackground ());
            Color lighter = getBackground().brighter();
            Color darker = getBackground().darker().darker().darker();

            for (int x = 0; x+1 < size.width; x+=4) {
                for (int y = TOPGAP; y+1 < height; y+=4) {
                    g.setColor (lighter);
                    g.drawLine (x, y, x, y);
                    if (x+5 < size.width && y+5 < height) {
                        g.drawLine (x+2, y+2, x+2, y+2);
                    }
                    g.setColor (darker);
                    g.drawLine (x+1, y+1, x+1, y+1);
                    if (x+5 < size.width && y+5 < height) {
                        g.drawLine (x+3, y+3, x+3, y+3);
                    }
                }
            }
        }

        /** @return minimum size */
        public Dimension getMinimumSize () {
            return limitDimensionWidth(super.getMinimumSize());
        }

        /** @return preferred size */
        public Dimension getPreferredSize () {
            return limitDimensionWidth(super.getPreferredSize());
        }

        public Dimension getMaximumSize () {
            return limitDimensionWidth(super.getMaximumSize());
        }

        private Dimension limitDimensionWidth(Dimension dim) {
            return new Dimension(WIDTH, dim.height);
        }
    } // end of inner class ToolbarBump

    private class ExpandButton extends JLabel implements MouseListener {

        private ActionListener actionListener;

        private ExpandButton() {
            super();
            addMouseListener(this);
            update();
        }

        public void update() {
            if(expanded) {
                setIcon(COLLAPSE_ICON);
                setToolTipText("Collapse toolbar");
            } else {
                setIcon(EXPAND_ICON);
                setToolTipText("Expand toolbar");
            }
        }

        public void setActionListener(ActionListener actionListener) {
            this.actionListener = actionListener;
        }

        public void mouseClicked(MouseEvent e) {
            if(actionListener != null) {
                actionListener.actionPerformed(new ActionEvent(this, 0, ""));
            }
        }

        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}

        public void mouseEntered(MouseEvent e) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        public void mouseExited(MouseEvent e) {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        boolean oldValue = this.expanded;
        this.expanded = expanded;
        expandButton.update();
        firePropertyChange("expanded", oldValue, expanded);
        revalidate();
    }
}
