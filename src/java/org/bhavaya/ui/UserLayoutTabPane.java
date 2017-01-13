package org.bhavaya.ui;


import org.bhavaya.ui.view.View;
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.Log;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.Encoder;
import java.beans.Statement;
import java.util.*;
import java.util.List;

/**
 * A tabbed container that allows the user to rearrange their tabs. Have not invested much (any) time into thinking about
 * how the api should look. I'm still a little uncertain. I'll use it for a bit and see what evolves.
 * <p/>
 * Notes from Brendon:  I think getting this to look and behave as similarly as possible to JTabbedPane is essential.
 * Current differences are mainly to with the preferred size (owing to the internal use of RowLayout) and also the non-
 * dynamic handling of rows.  In fact preferred size is pretty messed up at the moment.  Think everyone will be better
 * off forcing this be a certain size.
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.21.6.6 $
 */
public class UserLayoutTabPane extends JPanel {
    private static final Log log = Log.getCategory(UserLayoutTabPane.class);

    static {
        BeanUtilities.addPersistenceDelegate(UserLayoutTabPane.class, new UserLayoutTabPanePersistenceDelegate());
        BeanUtilities.addPersistenceDelegate(RowLayoutHeaderPanelController.class, new BhavayaPersistenceDelegate() {
            protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
                RowLayoutHeaderPanelController controller = (RowLayoutHeaderPanelController) oldInstance;
                Map tabLayout = controller.getPersistableTabLayout();
                out.writeStatement(new Statement(controller, "setPersistedLayout", new Object[]{tabLayout}));
            }
        });
        BeanUtilities.addPersistenceDelegate(RowLayoutHeaderPanelController.BoundedSharedWidthConstraint.class, new BhavayaPersistenceDelegate(new String[]{"row"}));
        BeanUtilities.addPersistenceDelegate(TabMetaData.class, new BhavayaPersistenceDelegate(new String[]{"data"}));
    }

    private HeaderPanel headerPanel;
    private CardPanel cardPanel;
    private HashMap buttonToComponent = new HashMap();
    private HashMap componentToButton = new HashMap();
    private HashMap componentToMetaData = new HashMap();

    private DragListener tabDragListener;
    private RowLayoutHeaderPanelController headerPanelController;
    private ArrayList changeListeners = new ArrayList(3);
    private TabMouseListenerDelegator tabMouseListenerDelegator = new TabMouseListenerDelegator();
    protected CircularBuffer<View> historicalViews = new CircularBuffer<View>(30);

    public UserLayoutTabPane() {
        this(new RowLayoutHeaderPanelController());
    }

    public UserLayoutTabPane(RowLayoutHeaderPanelController headerPanelController) {
        super(new BorderLayout());
        headerPanel = new HeaderPanel();
        headerPanel.setOpaque(false);

        tabDragListener = new DragListener();

        cardPanel = new CardPanel();
        cardPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIUtilities.createDarkerColor(getBackground(), 0.5f)), BorderFactory.createEmptyBorder(1, 1, 1, 1)));

        add(headerPanel, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);

        setHeaderPanelController(headerPanelController);
    }

    /**
     * the headerPanelController is persistable, and is responsible for laying out (and controlling) the header panel.
     * if two instances of UserLayoutTabPane have common instances of TabMetaData (e.g. by calling setTabMetaData) then
     * calling a.setHeaderPanelController( b.getHeaderPanelController() )
     *
     * @param headerPanelController
     */
    public void setHeaderPanelController(RowLayoutHeaderPanelController headerPanelController) {
        this.headerPanelController = headerPanelController;
        headerPanel.setLayout(headerPanelController.getInterpolatingLayoutManager());
        headerPanelController.setUserLayoutTabPane(this);
    }

    public RowLayoutHeaderPanelController getHeaderPanelController() {
        return headerPanelController;
    }

    public void addTab(String name, final Component component) {
        TabMetaData tabMetaData = new TabMetaData(new HashMap(8));
        tabMetaData.setTitle(name);
        if (!UIUtilities.usingAppleLAF()) tabMetaData.setColor(getBackground());
        addTab(component, tabMetaData);
    }

    public void addTab(final Component component, TabMetaData tabMetaData) {
        checkSafeToModify();

        final TabButton tabButton = new TabButton(tabMetaData, component);
        tabButton.addMouseListener(tabMouseListenerDelegator);

        headerPanelController.addTabButton(tabButton);
        cardPanel.addComponent(component);
        buttonToComponent.put(tabButton, component);
        componentToButton.put(component, tabButton);
        componentToMetaData.put(component, tabMetaData);
    }

    public void removeTab(View viewToRemove) {
        checkSafeToModify();

        Component componentToRemove = viewToRemove.getComponent();
        TabButton tabButton = getTabButton(componentToRemove);

        headerPanelController.removeTabButton(tabButton);
        cardPanel.removeComponent(componentToRemove); // This may mess up the selected index
        buttonToComponent.remove(tabButton);
        componentToButton.remove(componentToRemove);
        componentToMetaData.remove(componentToRemove);

        View currentView = historicalViews.getCurrent();
        View newView = currentView;
        if(currentView.getComponent().equals(componentToRemove)){
            newView = selectPrevHistoricalComponent();
        }
        historicalViews.remove(viewToRemove);

        if(newView == null){
            setSelectedComponent(null, null, false);
        }

        headerPanel.repaint();
        cardPanel.repaint();
    }

    public int getTabCount() {
        return cardPanel.getComponentCount();
    }

    public Component getTab(int n) {
        return cardPanel.getComponent(n);
    }

    public TabMetaData getTabMetaData(Component tab) {
        TabButton button = getTabButton(tab);
        if (button == null) return null;
        return button.getTabMetaData();
    }

    public void setTabMetaData(Component tab, TabMetaData tabMetaData) {
        checkSafeToModify();

        TabButton button = getTabButton(tab);
        if (button != null) {
            button.setTabMetaData(tabMetaData);
        }
    }

    protected TabButton getTabButton(Component tab) {
        return (TabButton) componentToButton.get(tab);
    }

    protected Component getComponentForTab(TabButton tabButton) {
        return (Component) buttonToComponent.get(tabButton);
    }

    public Component getComponentForTabMouseEvent(MouseEvent e) {
        Component component = e.getComponent();
        if (component instanceof TabButton) {
            return getComponentForTab((TabButton) component);
        }
        return null;
    }

    public Component getSelectedComponent() {
        return cardPanel.getSelectedComponent();
    }



    public void setSelectedComponent(Component component, Component oldComponent, boolean updateHistory) {
        checkSafeToModify();

        cardPanel.setSelectedComponent(component);
        if (oldComponent != null) {
            getTabButton(oldComponent).updateAppearance();
        }
        if (component != null) {
            TabButton button = getTabButton(component);
            if (button != null) button.updateAppearance();
        }
        fireTabSelected(component);
    }

    public void addChangeListener(ChangeListener changeListener) {
        changeListeners.add(changeListener);
    }

    public void removeChangeListener(ChangeListener changeListener) {
        changeListeners.remove(changeListener);
    }

    protected void fireTabSelected(Component component) {
        ChangeEvent event = new ChangeEvent(this);
        for (int i = 0; i < changeListeners.size(); i++) {
            ((ChangeListener) changeListeners.get(i)).stateChanged(event);
        }
    }

    public void addTabMouseListener(MouseListener tabMouseListener) {
        tabMouseListenerDelegator.addMouseListener(tabMouseListener);
    }

    public void removeTabMouseListener(MouseListener tabMouseListener) {
        tabMouseListenerDelegator.removeMouseListener(tabMouseListener);
    }

    public void setSelectedComponent(Component component) {
        setSelectedComponent(component, getSelectedComponent(), true);
    }

    private void checkSafeToModify() {
        if (!(getParent() == null || EventQueue.isDispatchThread())) {
            log.error(new RuntimeException("We are probably on a wrong thread, but don't take this too seriously."));
        }
    }

    protected class HeaderPanel extends JPanel {
        public HeaderPanel() {
        }

        public void paint(Graphics g) {
            super.paint(g);
            tabDragListener.paintDrag(g);
        }

        public Component add(Component comp) {
            if (headerPanelController != null) {
                int index = headerPanelController.getInsertionIndex(comp);
                return super.add(comp, index);
            } else {
                return super.add(comp);
            }
        }
    }

    private class DragListener extends MouseAdapter implements MouseMotionListener {
        private JButton currentDrag = null;
        private Rectangle dragRect;
        private Point lastMousePoint;
        private AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

        public void mouseMoved(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
            if (currentDrag == null) {
                if (e.getSource() instanceof JButton) {
                    currentDrag = (TabButton) e.getSource();
                    dragRect = new Rectangle(e.getPoint(), currentDrag.getSize());
                    lastMousePoint = SwingUtilities.convertPoint(currentDrag, e.getPoint(), headerPanel);
                    headerPanelController.setAnimationEnabled(true);
                }
            } else {
                lastMousePoint = SwingUtilities.convertPoint(currentDrag, e.getPoint(), headerPanel);
                Point target = new Point(lastMousePoint.x - dragRect.x, lastMousePoint.y - dragRect.y);
                headerPanelController.moveComponentTo((TabButton) currentDrag, target);
                headerPanel.repaint();
                Point cardPanelRepaintPoint = SwingUtilities.convertPoint(headerPanel, target, cardPanel);
                cardPanelRepaintPoint.translate(-1 * currentDrag.getWidth() / 2, 0);
                Dimension repaintSize = new Dimension(currentDrag.getWidth() * 2, currentDrag.getHeight() * 2);
                cardPanel.repaint(new Rectangle(cardPanelRepaintPoint, repaintSize));
            }
        }

        public void mouseReleased(MouseEvent e) {
            currentDrag = null;
            lastMousePoint = null;
            headerPanel.repaint();
            cardPanel.repaint();
            headerPanelController.setAnimationEnabled(false);
        }

        public void paintDrag(Graphics g) {
            if (lastMousePoint != null) {
                if (g instanceof Graphics2D) {
                    Graphics2D graphics2D = ((Graphics2D) g);
                    Composite oldComposite = graphics2D.getComposite();
                    graphics2D.setComposite(alphaComposite);
                    int x = lastMousePoint.x - dragRect.x;
                    int y = lastMousePoint.y - dragRect.y;
                    int width = currentDrag.getWidth();
                    int height = currentDrag.getWidth();
                    Shape oldClip = graphics2D.getClip();
                    graphics2D.translate(x, y);
                    graphics2D.setClip(0, 0, width, height);
                    currentDrag.paint(g);
                    graphics2D.setClip(oldClip);
                    graphics2D.setComposite(oldComposite);
                    graphics2D.translate(-x, -y);
                }
            }
        }
    }

    public static class RowLayoutHeaderPanelController {
        private UserLayoutTabPane userLayoutTabPane;
        private LayoutManager targetLayoutManager;
        private InterpolatingLayoutManager interpolatingLayoutManager;
        private Map persistedLayout = null;

        public RowLayoutHeaderPanelController() {
            this.targetLayoutManager = new RowLayout(0, -4, true);
        }

        protected HeaderPanel getHeaderPanel() {
            return userLayoutTabPane.headerPanel;
        }

        protected LayoutManager getTargetLayoutManager() {
            return this.targetLayoutManager;
        }

        public UserLayoutTabPane getUserLayoutTabPane() {
            return userLayoutTabPane;
        }

        public void moveComponentTo(TabButton tabButton, Point target) {
            int targetY = (int) target.getY();
            RowLayout layout = (RowLayout) getTargetLayoutManager();

            HeaderPanel headerPanel = getHeaderPanel();
            RowLayout.Row targetRow = getClosestRow(targetY);
            RowLayout.Row oldRow = layout.getRow(tabButton);
            int yOffset = targetRow.getYOffset();
            int height = targetRow.getHeight();

            int distanceFromRowBottom = (yOffset + height) - targetY;
            if (distanceFromRowBottom < height / 2) {
                // create a new row for this component
                // ah, but, don't bother if it is already in the target row and is on its own
                if (!(targetRow.getComponentCount() == 1 && targetRow.getComponent(0) == tabButton)) {
                    headerPanel.remove(tabButton);  // remove it, because we are going to give it a better home

                    int rowIndex = layout.getRowIndex(targetRow);
                    RowLayout.Row newRow = new RowLayout.Row(targetRow.getDefaultHGap(), targetRow.getHorizontalAlign(), targetRow.getVerticalAlign(), targetRow.isJustify());
                    layout.addRow(rowIndex + 1, newRow);
                    addTabToRow(tabButton, newRow, -1);
                    getUserLayoutTabPane().doLayout();
                }
            } else {
                headerPanel.remove(tabButton);  // remove it, because we are going to find it a better home

                //find best x position in row
                int bestIndex = -1;
                double bestDist = Double.POSITIVE_INFINITY;
                for (int i = 0; i < targetRow.getComponentCount() + 1; i++) {
                    addTabToRow(tabButton, targetRow, i);
                    Point componentPoint = getTargetLocationForComponent(tabButton);

                    double dist = componentPoint.distanceSq(target);
                    if (dist < bestDist) {
                        bestIndex = i;
                        bestDist = dist;
                    }
                    headerPanel.remove(tabButton);
                }
                addTabToRow(tabButton, targetRow, bestIndex);
            }
            if (oldRow.getComponentCount() == 0) {
                layout.removeRow(oldRow);
                getUserLayoutTabPane().doLayout();
            }
            headerPanel.doLayout();
        }

        public LayoutManager getInterpolatingLayoutManager() {
            if (interpolatingLayoutManager == null) {
                interpolatingLayoutManager = new InterpolatingLayoutManager(targetLayoutManager, 1000);
            }
            return interpolatingLayoutManager;
        }

        public Point getTargetLocationForComponent(Component component) {
            return interpolatingLayoutManager.getTargetLocationForComponent(component);
        }

        public void setAnimationEnabled(boolean animationEnabled) {
            interpolatingLayoutManager.setAnimationEnabled(animationEnabled);
        }

        protected void setUserLayoutTabPane(UserLayoutTabPane tabPane) {
            Map layoutToApply = persistedLayout;
            if (layoutToApply == null && getUserLayoutTabPane() != null) {
                layoutToApply = getPersistableTabLayout();
            }

            this.userLayoutTabPane = tabPane;

            //with a new layoutTabPane, we are going to have different instances of tabButton. clear out the layout
            //manager
            RowLayout rowLayout = ((RowLayout) getTargetLayoutManager());
            int rowCount = rowLayout.getRowCount();
            for (int i = rowCount - 1; i >= 0; i--) {
                rowLayout.removeRow(i);
            }
            applyLayout(layoutToApply);
            persistedLayout = null;
        }

        private void addTabToRow(TabButton tabButton, RowLayout.Row row, int i) {
            getHeaderPanel().add(row.addComponent(tabButton, new BoundedSharedWidthConstraint(row), i));
        }

        public void addTabButton(TabButton button) {
            RowLayout layout = (RowLayout) getTargetLayoutManager();
            int index = layout.getRowCount() - 1;
            RowLayout.Row row;
            if (index < 0) {
                row = createRow();
                layout.addRow(row);
            } else {
                row = layout.getRow(index);
                int targetWidth = getHeaderPanel().getWidth();
                int rowWidth = row.getWidth(targetWidth);
                if (rowWidth + button.getPreferredSize().width > targetWidth) {
                    row = createRow();
                    layout.addRow(row);
                }
            }

            addTabToRow(button, row, -1);
        }

        public void removeTabButton(TabButton button) {
            RowLayout layout = (RowLayout) getTargetLayoutManager();
            RowLayout.Row row = layout.getRow(button);

            userLayoutTabPane.headerPanel.remove(button);

            row.removeComponent(button);
            if (row.getComponentCount() == 0) {
                layout.removeRow(row);
            }
        }

        public RowLayout.Row createRow() {
            return new RowLayout.Row(-2, RowLayout.LEFT, RowLayout.TOP, false);
        }

        private RowLayout.Row getClosestRow(int y) {
            RowLayout layout = (RowLayout) getTargetLayoutManager();
            Insets insets = getHeaderPanel().getInsets();

            RowLayout.Row closest = null;
            int bestDist = Integer.MAX_VALUE;

            int currentRowTop = insets.top;
            for (int i = 0; i < layout.getRowCount(); i++) {
                RowLayout.Row row = layout.getRow(i);
                int dist = Math.abs(y - currentRowTop);

                if (dist > bestDist) {
                    return closest;    //we have already found a closest, things are now getting further away.
                } else {
                    closest = row;
                    bestDist = dist;
                }
                currentRowTop += row.getHeight();
                currentRowTop += layout.getVGap();
            }

            return closest;
        }

        public int getInsertionIndex(Component component) {
            RowLayout layout = (RowLayout) getTargetLayoutManager();
            int componentIndex = layout.getComponentIndex(component);
            if (componentIndex < 0) throw new RuntimeException("Component not found in rowLayout " + component);
            // now we have to reverse it - components is iterated in reverse
            int componentCount = getHeaderPanel().getComponentCount();
            componentIndex = componentCount - componentIndex;
            return componentIndex;
        }

        public Map getPersistableTabLayout() {
            RowLayout layout = (RowLayout) getTargetLayoutManager();
            HashMap map = new HashMap();

            int count = layout.getRowCount();
            for (int rowI = 0; rowI < count; rowI++) {
                RowLayout.Row row = layout.getRow(rowI);
                for (int tabI = 0; tabI < row.getComponentCount(); tabI++) {
                    TabButton tabButton = (TabButton) row.getComponent(tabI);
                    TabMetaData tabMetaData = tabButton.getTabMetaData();
                    if (tabMetaData.persistable()) {
                        map.put(tabMetaData, new Point(tabI, rowI));
                    }
                }
            }
            return map;
        }

        /**
         * used by the persistence delegate
         *
         * @param tabCoordinates map of tabMetaData to coordinates (java.awt.Point)
         */
        public void setPersistedLayout(Map tabCoordinates) {
            this.persistedLayout = tabCoordinates;
        }

        public void applyLayout(Map tabCoordinates) {
            List componentsNotInMap = new ArrayList();
            int count = getHeaderPanel().getComponentCount();
            for (int i = 0; i < count; i++) {
                TabButton button = (TabButton) getHeaderPanel().getComponent(i);
                if (tabCoordinates == null || !tabCoordinates.containsKey(button.getTabMetaData())) {
                    componentsNotInMap.add(button);
                }
            }
            getHeaderPanel().removeAll();

            if (tabCoordinates != null) {
                RowLayout layout = (RowLayout) getTargetLayoutManager();
                List sortedLeftToRight = new ArrayList(tabCoordinates.entrySet());
                Collections.sort(sortedLeftToRight, new Comparator() {
                    public int compare(Object o1, Object o2) {
                        Point p1 = (Point) ((Map.Entry) o1).getValue();
                        Point p2 = (Point) ((Map.Entry) o2).getValue();
                        return p1.x - p2.x;
                    }
                });

                for (Iterator iterator = sortedLeftToRight.iterator(); iterator.hasNext();) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    TabMetaData tabMetaData = (TabMetaData) entry.getKey();
                    Point coordinates = (Point) entry.getValue();

                    TabButton tabButton = tabMetaData.getTabButton();
                    if (tabButton == null) continue;

                    ensureRowCount(coordinates.y + 1);
                    RowLayout.Row row = layout.getRow(coordinates.y);
                    addTabToRow(tabButton, row, -1);
                }
            }

            for (Iterator iterator = componentsNotInMap.iterator(); iterator.hasNext();) {
                TabButton tabButton = (TabButton) iterator.next();
                addTabButton(tabButton);
            }

            getHeaderPanel().doLayout();
        }

        private void ensureRowCount(int rowCount) {
            RowLayout layout = ((RowLayout) getTargetLayoutManager());
            for (int i = layout.getRowCount(); i < rowCount; i++) {
                layout.addRow(createRow());
            }
        }

        public int indexOf(TabButton tabButton) {
            RowLayout rowLayout = (RowLayout) getTargetLayoutManager();
            return rowLayout.getComponentIndex(tabButton);
        }

        public Component getComponentAtIndex(int index) {
            return ((RowLayout) getTargetLayoutManager()).getComponentAtIndex(index);
        }

        public static class BoundedSharedWidthConstraint implements RowLayout.WidthConstraint {
            private RowLayout.Row row;

            public BoundedSharedWidthConstraint(RowLayout.Row row) {
                this.row = row;
            }

            public RowLayout.Row getRow() {
                return row;
            }

            public int getWidth(Component component, int targetRowWidth, int currentRowWidth) {
                int componentCount = row.getComponentCount();
                int width = targetRowWidth / componentCount;
                int preferred = component.getPreferredSize().width;
                return Math.min(preferred, width);
            }
        }
        // New navigation
        public int indexOfTabButton(TabButton button){
            RowLayout layout = (RowLayout) getTargetLayoutManager();
            int compIndex = layout.getComponentIndex(button);
            return compIndex;

    }

        public TabButton getNextTabButton(TabButton button){
            RowLayout layout = (RowLayout) getTargetLayoutManager();
            int compIndex = layout.getComponentIndex(button) + 1;
            if(compIndex >= layout.getComponentCount()){
                compIndex = 0;
            }
            TabButton tabBtn = (TabButton)layout.getComponentAtIndex(compIndex);
            return tabBtn;
        }

        public TabButton getPrevTabButton(TabButton button){
            RowLayout layout = (RowLayout) getTargetLayoutManager();
            int compIndex = layout.getComponentIndex(button) - 1;
            if(compIndex < 0){
                compIndex = layout.getComponentCount() -1;
            }
            TabButton tabBtn = (TabButton)layout.getComponentAtIndex(compIndex);
            return tabBtn;
        }

    }


    public static class TabMetaData {
        private static String TITLE = "title";
        private static String COLOR = "color";
        private static String TOOLTIP = "tooltip";
        private static String ICON = "icon";
        private Map data = new HashMap(8);
        private TabButton tabButton;

        /**
         * "but why the map", you ask. "what sort of an annoying api is this?"
         * The only time you should be calling this constructor is if you have extended TabMetaData
         * if you have done that, then in order to preserve the whole persistence thing, you should to persist the map
         * that TabMetaData wraps (getData). The best way to stop you forgetting about this, is to only provide this constructor.
         * sorry to be a pain.
         *
         * @param data
         */
        public TabMetaData(Map data) {
            this.data = data;
        }

        public String getTitle() {
            return (String) getData().get(TITLE);
        }

        public void setTitle(String title) {
            getData().put(TITLE, title);
            applyChangesToTab();
        }

        public Color getColor() {
            Color color = (Color) getData().get(COLOR);
            if (color == null && tabButton != null) {
                return Color.white;
            }
            return color;
        }

        public void setColor(Color color) {
            getData().put(COLOR, color);
            applyChangesToTab();
        }

        public Icon getIcon() {
            return (Icon) getData().get(ICON);
        }

        public void setIcon(Icon icon) {
            getData().put(ICON, icon);
            applyChangesToTab();
        }

        private void setTabButton(TabButton tabButton) {
            this.tabButton = tabButton;
        }

        public String getToolTip() {
            return (String) getData().get(TOOLTIP);
        }

        public void setToolTip(String toolTip) {
            getData().put(TOOLTIP, toolTip);
        }

        public void applyChangesToTab() {
            if (tabButton != null) {
                tabButton.updateAppearance();
            }
        }

        /**
         * only really public for the persistence delegate
         *
         * @return
         */
        public Map getData() {
            return data;
        }

        protected TabButton getTabButton() {
            return this.tabButton;
        }

        public boolean persistable() {
            return true;
        }
    }

    private class TabButtonAction extends AbstractAction {
        private final Component component;

        public TabButtonAction(String name, Component component) {
            super(name);
            this.component = component;
        }

        public void actionPerformed(ActionEvent e) {
            setSelectedComponent(component);
        }
    }

    protected class TabButton extends JButton {
        private TabMetaData tabMetaData;
        private BufferedImage dragImage;

        protected Color paintClrWhite = new Color(0.95f, 0.95f, 0.95f, 0.7f);
        protected Color paintClrGray =   new Color(0.95f, 0.95f, 0.95f, 0f);
        private GradientPaint unselectedPaint ;

        public TabButton(TabMetaData tabMetaData, Component component) {
            super(new TabButtonAction(tabMetaData.getTitle(), component));
            setOpaque(false);
            setContentAreaFilled(true);

            setFocusable(false);
            setRolloverEnabled(true);
            if (!UIUtilities.usingAppleLAF()) { // Apple knows best!
                setMargin(new Insets(2, 10, 2, 10));
            }

            addMouseMotionListener(tabDragListener);
            addMouseListener(tabDragListener);
            setTabMetaData(tabMetaData);
        }

        public TabMetaData getTabMetaData() {
            return tabMetaData;
        }

        public void setTabMetaData(TabMetaData tabMetaData) {
            this.tabMetaData = tabMetaData;
            tabMetaData.setTabButton(this);
            updateAppearance();
        }

        public void reshape(int x, int y, int w, int h) {
            super.reshape(x, y, w, h);
            dragImage = null;
        }

        protected void paintComponent(Graphics g) {
            if (tabDragListener.currentDrag == this) {
                setOpaque(false);
                super.paintComponent(g);

                if (dragImage == null) {
                    dragImage = ((Graphics2D) g).getDeviceConfiguration().createCompatibleImage(getWidth(), getHeight(), Transparency.TRANSLUCENT);
                    Graphics2D graphics = (Graphics2D) dragImage.getGraphics();
                    super.paintComponent(graphics);
                    graphics.setColor(new Color(0, 0, 0));
                    graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, 0.05f));
                    graphics.fillRect(0, 0, getWidth(), getHeight());
                }
                g.drawImage(dragImage, 0, 0, null);
            } else {
                dragImage = null;
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D)g ;
                Component tabComponent = getComponentForTab(this);

                if(unselectedPaint == null){
                    unselectedPaint = new GradientPaint(0,0, paintClrWhite,
                            0, this.getHeight()/2, paintClrGray,
                            true);
                }

                if (getSelectedComponent() != tabComponent) {
                    Composite comp = g2.getComposite();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                    if(!tabMetaData.getColor().equals(Color.white)){
                        g2.setPaint(tabMetaData.getColor());
                    }
                    else{
                        g2.setPaint(Color.gray);
                    }
                    g2.fillRect(1,1, this.getWidth()-2, this.getHeight()-2);
                    g2.setComposite(comp);
                    g2.setPaint(unselectedPaint);
                    g2.fillRect(1,1, this.getWidth()-2, this.getHeight()/2);
                }
            }
        }


        public String getToolTipText() {
            return super.getToolTipText();
        }

        public void updateAppearance() {
            setText(tabMetaData.getTitle());
            setIcon(tabMetaData.getIcon());
            setToolTipText(tabMetaData.getToolTip());

            Component tabComponent = getComponentForTab(this);
            if (getSelectedComponent() == tabComponent) {
                setFont(getFont().deriveFont(Font.BOLD));
                setForeground(UIUtilities.arbitratryHSBOperation(tabMetaData.getColor(), 0f, 0f, -0.65f));
                setBackground(Color.white);
            } else {
                setFont(getFont().deriveFont(Font.PLAIN));
                setForeground(UIUtilities.arbitratryHSBOperation(tabMetaData.getColor(), 0f, -0.65f, 1f));
                setBackground(UIUtilities.arbitratryHSBOperation(tabMetaData.getColor(), 0f, -0.17f, -0.30f));
            }

            setForeground(Color.black);



        }
    }

    private static class TabMouseListenerDelegator implements MouseListener {
        private ArrayList tabMouseListeners = new ArrayList(3);

        public void addMouseListener(MouseListener listener) {
            tabMouseListeners.add(listener);
        }

        public void removeMouseListener(MouseListener listener) {
            tabMouseListeners.remove(listener);
        }

        public void mouseClicked(MouseEvent e) {
            for (Iterator iterator = tabMouseListeners.iterator(); iterator.hasNext();) {
                MouseListener mouseListener = (MouseListener) iterator.next();
                mouseListener.mouseClicked(e);
            }
        }

        public void mouseEntered(MouseEvent e) {
            for (Iterator iterator = tabMouseListeners.iterator(); iterator.hasNext();) {
                MouseListener mouseListener = (MouseListener) iterator.next();
                mouseListener.mouseEntered(e);
            }
        }

        public void mouseExited(MouseEvent e) {
            for (Iterator iterator = tabMouseListeners.iterator(); iterator.hasNext();) {
                MouseListener mouseListener = (MouseListener) iterator.next();
                mouseListener.mouseExited(e);
            }
        }

        public void mousePressed(MouseEvent e) {
            for (Iterator iterator = tabMouseListeners.iterator(); iterator.hasNext();) {
                MouseListener mouseListener = (MouseListener) iterator.next();
                mouseListener.mousePressed(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            for (Iterator iterator = tabMouseListeners.iterator(); iterator.hasNext();) {
                MouseListener mouseListener = (MouseListener) iterator.next();
                mouseListener.mouseReleased(e);
            }
        }
    }

    public static class UserLayoutTabPanePersistenceDelegate extends BhavayaPersistenceDelegate {
        protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
            UserLayoutTabPane oldTabPane = ((UserLayoutTabPane) oldInstance);
            int count = oldTabPane.getTabCount();
            for (int i = 0; i < count; i++) {
                Component component = oldTabPane.getTab(i);
                TabMetaData metaData = oldTabPane.getTabMetaData(component);
                out.writeStatement(new Statement(oldTabPane, "addTab", new Object[]{component, metaData}));
            }
            out.writeStatement(new Statement(oldTabPane, "setHeaderPanelController", new Object[]{oldTabPane.getHeaderPanelController()}));
        }
    }

    public void selectNextButtonComponent(){
        Component oldComponent = cardPanel.getSelectedComponent();
        TabButton oldBtn = (TabButton)this.componentToButton.get(oldComponent);
        TabButton btn = headerPanelController.getNextTabButton(oldBtn);
        Component newComponent = (Component)buttonToComponent.get(btn);
        setSelectedComponent(newComponent, oldComponent, true);
    }
    public void selectPrevButtonComponent(){
        Component oldComponent = cardPanel.getSelectedComponent();
        TabButton oldBtn = (TabButton)this.componentToButton.get(oldComponent);
        TabButton btn = headerPanelController.getPrevTabButton(oldBtn);
        Component newComponent = (Component)buttonToComponent.get(btn);
        setSelectedComponent(newComponent, oldComponent, true);
    }

    public void selectNextHistoricalComponent(){
        if(!historicalViews.canIncCurrentPtr()){
            return;
        }
        View view = historicalViews.getNext();
        Component oldComponent = cardPanel.getSelectedComponent();

        Component viewComp = view.getComponent();
        TabButton tabBtn = (TabButton)componentToButton.get(viewComp);
        if(tabBtn != null){
            setSelectedComponent(viewComp, oldComponent, false);
        }
    }

    public View selectPrevHistoricalComponent(){
        if(!historicalViews.canDecCurrentPtr()){
            return null;
        }
        View view = historicalViews.getPrev();

        Component oldComponent = cardPanel.getSelectedComponent();
        Component viewComp = view.getComponent();
        setSelectedComponent(viewComp, oldComponent, false);
        return view;
    }
}
