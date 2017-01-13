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

package org.bhavaya.ui;

import org.bhavaya.db.DBUtilities;
import org.bhavaya.ui.table.AnalyticsTable;
import org.bhavaya.ui.table.BeanFormTableModel;
import org.bhavaya.ui.view.Workspace;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.EventHandler;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.46.2.1.2.6 $
 */
public class UIUtilities {
    private static final Log log = Log.getCategory(UIUtilities.class);

    public static final Set<KeyStroke> normalForwardTraversalKeystrokes = new HashSet<KeyStroke>();
    public static final Set<KeyStroke> backwardTraversalKeystrokes = new HashSet<KeyStroke>();
    public static final Set<KeyStroke> customForwardTraversalKeystrokes = new HashSet<KeyStroke>();
    private static final Pattern DOUBLE_QUOTE_PATTERN = Pattern.compile("\\\"");

    private static final int TASKBAR_HEIGHT = 30;
    public static JDialog currentProgressDialog;
    private static Rectangle dirtyScreenSizeWithOutAdjustment;

    private static Dimension DEFAULT_SIZE = new Dimension(1000, 800);

    static {
        UIUtilities.normalForwardTraversalKeystrokes.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        UIUtilities.backwardTraversalKeystrokes.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
        UIUtilities.customForwardTraversalKeystrokes.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        UIUtilities.customForwardTraversalKeystrokes.add(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
    }

    public static LookAndFeel createDefaultLookAndFeel() {
        try {
            LookAndFeel lookAndFeel;
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                Class<?> lookAndFeelClass = ClassUtilities.getClass(UIManager.getSystemLookAndFeelClassName());
                lookAndFeel = (LookAndFeel) lookAndFeelClass.newInstance();
            } else {
                Class<?> lookAndFeelClass = ClassUtilities.getClass(UIManager.getSystemLookAndFeelClassName());
                lookAndFeel = (LookAndFeel) lookAndFeelClass.newInstance();
            }

            return lookAndFeel;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void centreInFocusedWindow(Container containee) {
        Window focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        if (focusedWindow != null) {
            Point containingFrameTopLeftHandCorner = focusedWindow.getLocation();
            Dimension containingFrameSize = focusedWindow.getSize();
            Dimension thisFrameSize = containee.getSize();

            int x = (int) (containingFrameTopLeftHandCorner.getX() + (containingFrameSize.width / 2) - (thisFrameSize.width / 2));
            int y = (int) (containingFrameTopLeftHandCorner.getY() + (containingFrameSize.height / 2) - (thisFrameSize.height / 2));
    
            containee.setLocation(x, y);
        }
    }

    public static void centreInContainer(Component container, Container containee, int xOffSet, int yOffSet) {
        if (container == null) return;
        Point containingFrameTopLeftHandCorner = container.getLocation();
        Dimension containingFrameSize = container.getSize();
        Dimension thisFrameSize = containee.getSize();

        int x = (int) (containingFrameTopLeftHandCorner.getX() + (containingFrameSize.width / 2) - (thisFrameSize.width / 2) + xOffSet);
        int y = (int) (containingFrameTopLeftHandCorner.getY() + (containingFrameSize.height / 2) - (thisFrameSize.height / 2) + yOffSet);

        containee.setLocation(x, y);
        ensureOnScreen(containee);

        // Prevent problem of always on top window starting a modal child completely hidden and centred beneath it
        if (getFrameParent(container) != null && getFrameParent(container).isAlwaysOnTop()) {
            final JFrame frame = getFrameParent(containee);
            if (frame == null) return;
            if (frame.isShowing()) {
                runInDispatchThread(new Runnable() {
                    public void run() {
                    	frame.setAlwaysOnTop(true);
                    }
                });
            } else {
                frame.addWindowListener(new WindowAdapter() {
                    public void windowOpened(WindowEvent e) {
                        runInDispatchThread(new Runnable() {
                            public void run() {
                            	frame.setAlwaysOnTop(true);
                            }
                        });
                    }

                    public void windowClosing(WindowEvent e) {
                        frame.removeWindowListener(this);
                    }
                });
            }
        }
    }

    public static void ensureOnScreen(Container container) {
        if (container == null) return;
        Point containerTopLeftHandCorner = container.getLocation();
        Dimension containerSize = container.getSize();

        Rectangle screenSize = getDefaultScreenSizeWithoutAdjustment();

        int x = containerTopLeftHandCorner.x;
        if (x < 0) x = 0;
        if ((x + containerSize.width) > screenSize.width) x = screenSize.width - containerSize.width;

        int y = containerTopLeftHandCorner.y;
        if (y < 0) y = 0;
        if ((y + containerSize.height + TASKBAR_HEIGHT) > screenSize.height) y = screenSize.height - containerSize.height - TASKBAR_HEIGHT;

        container.setLocation(x, y);
    }

    public static Dimension getDefaultScreenSize() {
        Rectangle dimension = getDefaultScreenSizeWithoutAdjustment();

        // many drivers will return a single graphics device even where there is more than one device (monitor)
        // because the graphics card is making them appear as one, therefore use a hack
        // This assumes the aspect ratio of a monitor is somewhere between 1 and 1.5.
        // The calculation starts to fail as the number of monitors increase.
        // It also assumes that monitors are laids out from left-right, and not from top-bottom
        // TODO: one day this may not be neccessary
        int noOfScreens = (int) Math.floor((double) dimension.width / (double) dimension.height); // this is a good estimate in most cases

        if(noOfScreens <1){
            noOfScreens = 1;
        }
        if(dimension.width <100 || dimension.height < 100 ){
            dimension.width = DEFAULT_SIZE.width;
            dimension.height = DEFAULT_SIZE.height;
        }
        return new Dimension(dimension.width / noOfScreens, dimension.height);
    }

    public static Rectangle getDefaultScreenSizeWithoutAdjustment() {
        return getDefaultScreenSizeWithoutAdjustment(false);
    }

    public static Rectangle getDefaultScreenSizeWithoutAdjustment(boolean useDirty) {
        if (useDirty && dirtyScreenSizeWithOutAdjustment != null) return dirtyScreenSizeWithOutAdjustment;

        Rectangle totalBounds = new Rectangle(0, 0);

        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();  // to work with multiple screens
        GraphicsDevice[] screenDevices = graphicsEnvironment.getScreenDevices();
        for (GraphicsDevice screenDevice : screenDevices) {
            GraphicsConfiguration graphicsConfiguration = screenDevice.getDefaultConfiguration();
            Rectangle singleScreenBounds = graphicsConfiguration.getBounds();
            totalBounds.width += singleScreenBounds.width;
            totalBounds.height = Math.max(totalBounds.height, singleScreenBounds.height);
        }
        dirtyScreenSizeWithOutAdjustment = totalBounds;
        return totalBounds;
    }

    /**
     * Returns whether the specified rectangle is visible on screen somewhere.  A rectangle
     * is deemed to be visible if any part of it intersects with any of the available screens.
     * @param rectangle the rectangle to test.
     * @return <code>true</code> if the rectangle is visible, <code>false</code> otherwise.
     */
    public static boolean isRectangleOnVisibleMonitor(Rectangle rectangle) {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();  // to work with multiple screens
        GraphicsDevice[] screenDevices = graphicsEnvironment.getScreenDevices();
        for (GraphicsDevice screenDevice : screenDevices) {
            GraphicsConfiguration graphicsConfiguration = screenDevice.getDefaultConfiguration();
            Rectangle singleScreenBounds = graphicsConfiguration.getBounds();
            if (singleScreenBounds.intersects(rectangle)) return true;
        }
        return false;
    }

    public static void centreInScreen(Container containee, int xOffSet, int yOffSet) {
        Dimension screenSize = getDefaultScreenSize();
        Dimension thisSize = containee.getSize();

        int x = (int) (((screenSize.getWidth() - thisSize.width) / 2) + xOffSet);
        if (x < 0) x = 0;

        int y = (int) (((screenSize.getHeight() - thisSize.height) / 2) + yOffSet);
        if (y < 0) y = 0;

        containee.setLocation(x, y);
    }

    /**
     * Finds a column for the given column name in a table model
     */
    public static int findColumn(TableModel m, String columnName) {
        if (columnName != null) {
            if (m instanceof AbstractTableModel) {
                return ((AbstractTableModel) m).findColumn(columnName);
            } else {
                for (int i = 0; i < m.getColumnCount(); i++) {
                    if (columnName.equals(m.getColumnName(i))) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    public static void disposeParentWindow(Component component) {
        getWindowParent(component).dispose();
    }

    public static JDialog getDialogParent(Component component) {
        Window windowAncestor = getWindowParent(component);
        if (windowAncestor instanceof JDialog) return (JDialog) windowAncestor;
        return null;
    }

    public static JFrame getFrameParent(Component component) {
        Window windowAncestor = getWindowParent(component);
        if (windowAncestor instanceof JFrame) return (JFrame) windowAncestor;
        return null;
    }

    public static Window getWindowParent(Component component) {
        if (component == null) return null;

        if (component instanceof Window) {
            return (Window) component;
        }

        for (Container parent = component.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof Window) {
                return (Window) parent;
            } else if (parent instanceof JPopupMenu) {
                parent = (Container) ((JPopupMenu) parent).getInvoker();
            }
        }

        return null;
    }

    public static DefaultComboBoxModel createComboBoxModelFromSQL(String dataSourceName, String sql) {
        DefaultComboBoxModel cm = new DefaultComboBoxModel();
        Collection<?> result = DBUtilities.execute(dataSourceName, sql);
        for (Object aResult : result) {
            cm.addElement(aResult);
        }

        return cm;
    }

    public static ListCellRenderer createListCellRenderer(final String objectRenderPattern) {
        final SimpleObjectFormat simpleObjectFormat = new SimpleObjectFormat(objectRenderPattern);
        return new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setText(simpleObjectFormat.formatObject(value));
                return label;
            }
        };
    }

    public static ListCellRenderer createListCellRenderer(final StringRenderer objectRenderer) {
        return new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setText(objectRenderer.render(value));
                return label;
            }
        };
    }

    public static TreeCellRenderer createTreeCellRenderer(final StringRenderer objectRenderer) {
        return new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                if (value instanceof MutableTreeNode) {
                    label.setText(objectRenderer.render(((DefaultMutableTreeNode) value).getUserObject()));
                } else {
                    label.setText(objectRenderer.render(value));
                }
                return label;
            }
        };
    }

    public static Color createBrighterColor(Color color, float ratio) {
        float[] comp = color.getColorComponents(null);
        return new Color(comp[0] + ((1 - comp[0]) * ratio),
                comp[1] + ((1 - comp[1]) * ratio),
                comp[2] + ((1 - comp[2]) * ratio));
    }

    public static Color createDarkerColor(Color color, float ratio) {
        float[] comp = color.getColorComponents(null);
        return new Color(comp[0] - (comp[0] * ratio),
                comp[1] - (comp[1] * ratio),
                comp[2] - (comp[2] * ratio));
    }

    public static <T> T triggerMethodOnEvent(Class<T> listenerClass, final String listenerMethod, final Object target, final String targetMethod) {
        return EventHandler.create(listenerClass, target, targetMethod, null, listenerMethod);
    }

    // TODO: Make look like triggerMethodOnEvent
    public static <T> T triggerOnEvent(Class<T> listenerClass, final String listenerMethod, final Runnable runnable) {
        InvocationHandler handler = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.equals(Object.class.getMethod("equals", Object.class))) {
                    return proxy == args[0];
                } else if (method.getDeclaringClass().equals(Object.class)) {
                    return method.invoke(this, args);
                }

                if (listenerMethod == null || method.getName().equals(listenerMethod)) {
                    runnable.run();
                }
                return null;
            }
        };

        return (T) Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class[]{listenerClass}, handler);
    }

    public static String asCommaDelimitedString(final AnalyticsTable analyticsTable, boolean includeHeader) {
        // Masquerades the AnalyticsTable as a proper JTable.
        TableProxy tableAdapter = new TableProxy() {
            public int getColumnCount() {
                return analyticsTable.getColumnCount();
            }

            public int getRowCount() {
                return analyticsTable.getRowCount();
            }

            public String getColumnName(int column) {
                return analyticsTable.getColumnName(column);
            }

            public TableCellRenderer getCellRenderer(int row, int column) {
                return analyticsTable.getCellRenderer(row, column);
            }

            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                JTable fixedTable = analyticsTable.getFixedTable();
                JTable scrollableTable = analyticsTable.getScrollableTable();
                if (column < fixedTable.getColumnCount()) {
                    return fixedTable.prepareRenderer(renderer, row, column);
                } else {
                    return scrollableTable.prepareRenderer(renderer, row, column - fixedTable.getColumnCount());
                }
            }

            public TableColumn getColumn(Object identifier) {
                return analyticsTable.getColumn(identifier);
            }

            public Object getValueAt(int row, int col) {
                return analyticsTable.getValueAt(row, col);
            }
        };
        return asCommaDelimitedString(tableAdapter, includeHeader);
    }

    public static String asCommaDelimitedString(final JTable table, boolean includeHeader) {
        TableProxy tableAdapter = new TableProxy() {
            public int getColumnCount() {
                return table.getColumnCount();
            }

            public int getRowCount() {
                return table.getRowCount();
            }

            public String getColumnName(int col) {
                return table.getColumnName(col);
            }

            public Object getValueAt(int row, int col) {
                return table.getValueAt(row, col);
            }

            public TableCellRenderer getCellRenderer(int row, int col) {
                return table.getCellRenderer(row, col);
            }

            public Component prepareRenderer(TableCellRenderer tableCellRenderer, int row, int col) {
                return table.prepareRenderer(tableCellRenderer, row, col);
            }
        };
        return asCommaDelimitedString(tableAdapter, includeHeader);
    }

    public static String asFixedWidthColumnString(final AnalyticsTable analyticsTable, boolean includeHeader, Map alternativeHeaderNames) {
        // Masquerades the AnalyticsTable as a proper JTable.
        TableProxy tableAdapter = new TableProxy() {
            public int getColumnCount() {
                return analyticsTable.getColumnCount();
            }

            public int getRowCount() {
                return analyticsTable.getRowCount();
            }

            public String getColumnName(int column) {
                return analyticsTable.getColumnName(column);
            }

            public TableCellRenderer getCellRenderer(int row, int column) {
                return analyticsTable.getCellRenderer(row, column);
            }

            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                JTable fixedTable = analyticsTable.getFixedTable();
                JTable scrollableTable = analyticsTable.getScrollableTable();
                if (column < fixedTable.getColumnCount()) {
                    return fixedTable.prepareRenderer(renderer, row, column);
                } else {
                    return scrollableTable.prepareRenderer(renderer, row, column);
                }
            }

            public TableColumn getColumn(Object identifier) {
                return analyticsTable.getColumn(identifier);
            }

            public Object getValueAt(int row, int col) {
                return analyticsTable.getValueAt(row, col);
            }
        };
        return asFixedWidthColumnString(tableAdapter, includeHeader, alternativeHeaderNames);
    }

    private static String asFixedWidthColumnString(TableProxy table, boolean includeHeader, Map alternativeHeaderNames) {
        int columnCount = table.getColumnCount();
        int rowCount = table.getRowCount();
        final String NEW_LINE = System.getProperty("line.separator");

        int[] columnWidths = new int[columnCount];
        StringBuffer csv = new StringBuffer(rowCount * columnCount * 10);
        //Iterate over the rows and get the maximum size of the text for each position
        if (includeHeader) {
            for (int i = 0; i < columnCount; i++) {
                String columnName = table.getColumnName(i);
                if (alternativeHeaderNames != null) {
                    String alternativeColumnName = (String)alternativeHeaderNames.get(columnName);
                    columnName = alternativeColumnName == null ? columnName : alternativeColumnName;
                }
                columnWidths[i] = Math.max(columnWidths[i], columnName.length());
            }
        }


        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < columnCount; col++) {
                Object cellValue = table.getValueAt(row, col);
                if (cellValue != null) {
                    TableCellRenderer tableCellRenderer = table.getCellRenderer(row, col);
                    Component cellComponent = table.prepareRenderer(tableCellRenderer, row, col);
                    String cellText;
                    if (cellComponent instanceof JLabel) {
                        JLabel label = (JLabel) cellComponent;
                        cellText = label.getText();
                    } else {
                        cellText = cellValue.toString();
                    }


                    if (cellText != null) {
                        columnWidths[col] = Math.max(columnWidths[col], cellText.length());
                    }
                }
            }
        }

        if (includeHeader) {
            for (int x = 0; x < columnCount; x++) {
                String columnName = table.getColumnName(x);
                if (alternativeHeaderNames != null) {
                    String alternativeName = (String)alternativeHeaderNames.get(columnName);
                    columnName = alternativeName == null ? columnName : alternativeName;
                }
                columnName = Utilities.pad(columnName, columnWidths[x] + 1, ' ');
                csv.append(columnName);
            }
            csv.append(NEW_LINE);
        }

        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < columnCount; col++) {
                Object cellValue = table.getValueAt(row, col);
                if (cellValue != null) {
                    TableCellRenderer tableCellRenderer = table.getCellRenderer(row, col);
                    Component cellComponent = table.prepareRenderer(tableCellRenderer, row, col);
                    String cellText;
                    if (cellComponent instanceof JLabel) {
                        JLabel label = (JLabel) cellComponent;
                        cellText = label.getText();
                    } else {
                        cellText = cellValue.toString();
                    }

                    csv.append(Utilities.pad(cellText, columnWidths[col] + 1, ' '));
                } else {
                    csv.append(Utilities.pad("", columnWidths[col] + 1, ' '));
                }
            }
            csv.append(NEW_LINE);
        }

        return csv.toString();
    }

    public static JComponent leftAlignInBox(Component... components) {
        Box b = new Box(BoxLayout.X_AXIS);
        for ( Component component : components) {
            b.add(component);
        }
        b.add(Box.createHorizontalGlue());
        return b;
    }

    // Assumes table is contained in a JScrollPane. Scrolls the
   // cell (rowIndex, vColIndex) so that it is visible at the center of viewport.
   public static void scrollToCenter(JTable table, int rowIndex, int vColIndex) {
       if (!(table.getParent() instanceof JViewport)) {
           return;
       }
       JViewport viewport = (JViewport) table.getParent();

       // This rectangle is relative to the table where the
       // northwest corner of cell (0,0) is always (0,0).
       Rectangle rect = table.getCellRect(rowIndex, vColIndex, true);

       // The location of the view relative to the table
       Rectangle viewRect = viewport.getViewRect();

       // Translate the cell location so that it is relative
       // to the view, assuming the northwest corner of the
       // view is (0,0).
       rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

       // Calculate location of rect if it were at the center of view
       int centerX = (viewRect.width - rect.width) / 2;
       int centerY = (viewRect.height - rect.height) / 2;

       // Fake the location of the cell so that scrollRectToVisible
       // will move the cell to the center
       if (rect.x < centerX) {
           centerX = -centerX;
       }
       if (rect.y < centerY) {
           centerY = -centerY;
       }
       rect.translate(centerX, centerY);

       // Scroll the area into view.
       viewport.scrollRectToVisible(rect);
   }

    private static interface TableProxy {
        int getColumnCount();

        int getRowCount();

        String getColumnName(int col);

        Object getValueAt(int row, int col);

        TableCellRenderer getCellRenderer(int row, int col);

        Component prepareRenderer(TableCellRenderer tableCellRenderer, int row, int col);
    }

    private static String asCommaDelimitedString(TableProxy table, boolean includeHeader) {
        int columnCount = table.getColumnCount();
        int rowCount = table.getRowCount();

        StringBuffer csv = new StringBuffer(rowCount * columnCount * 10);

        if (includeHeader) {
            for (int x = 0; x < columnCount; x++) {
                if (x > 0) csv.append(",");
                csv.append('\"');
                String columnName = escapeTextForCommaDelimitedFile(table.getColumnName(x));
                if (columnName != null) csv.append(columnName);
                csv.append('\"');
            }
            csv.append('\n');
        }

        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < columnCount; col++) {
                if (col > 0) csv.append(",");
                Object cellValue = table.getValueAt(row, col);
                if (cellValue != null) {
                    TableCellRenderer tableCellRenderer = table.getCellRenderer(row, col);
                    Component cellComponent = table.prepareRenderer(tableCellRenderer, row, col);
                    String cellText;
                    if (cellComponent instanceof JLabel) {
                        JLabel label = (JLabel) cellComponent;
                        cellText = label.getText();
                    } else {
                        cellText = cellValue.toString();
                    }

                    cellText = escapeTextForCommaDelimitedFile(cellText);

                    csv.append('\"');
                    if (cellText != null) csv.append(cellText);
                    csv.append('\"');
                }
            }
            csv.append('\n');
        }

        return csv.toString();
    }

    private static String escapeTextForCommaDelimitedFile(String string) {
        if (string == null) return string;

        // replace new line chars
        string = string.replace('\n', '-');

        // escape double quote with another double quote
        if (string.indexOf('\"') != -1) {
            string = DOUBLE_QUOTE_PATTERN.matcher(string).replaceAll("\"\"");
        }

        return string;
    }

    public static void runTaskWithProgressDialog(String dialogTitle, String dialogMessage, Runnable task) {
        runTaskWithProgressDialog(dialogTitle, dialogMessage, task, Thread.NORM_PRIORITY, true);
    }

    public static void runTaskWithProgressDialog(String dialogTitle, String dialogMessage, Runnable task, int taskPriority, boolean modal) {
        runTaskWithProgressDialog(Workspace.getInstance().getApplicationFrame().getContentPane(), dialogTitle, dialogMessage, task, taskPriority, modal);
    }

    public static void runTaskWithProgressDialog(Component owner, String dialogTitle, String dialogMessage, Runnable task, int taskPriority, boolean modal) {
        runTasksWithProgressDialog(owner, dialogTitle, new Task[]{new RunnableTask(dialogMessage, task)}, taskPriority, modal);
    }

    public static void runTaskWithProgressDialog(String dialogTitle, Task task) {
        runTaskWithProgressDialog(Workspace.getInstance().getApplicationFrame().getContentPane(), dialogTitle, task);
    }

    public static void runTaskWithProgressDialog(Component owner, String dialogTitle, Task task) {
        runTasksWithProgressDialog(owner, dialogTitle, new Task[]{task}, Thread.NORM_PRIORITY, true);
    }

    public static void runTasksWithProgressDialog(String dialogTitle, Task[] tasks, int taskPriority, final boolean modal) {
        runTasksWithProgressDialog(Workspace.getInstance().getApplicationFrame().getContentPane(), dialogTitle, tasks, taskPriority, modal);
    }

    public static void runTasksWithProgressDialog(final Component owner, final String dialogTitle, final Task[] tasks, final int taskPriority, final boolean modal) {
        Runnable runnable = new Runnable() {
            public void run() {
                runTasksWithProgressDialogOnEDT(owner, dialogTitle, tasks, taskPriority, modal);
            }
        };
        if(SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private static void runTasksWithProgressDialogOnEDT(final Component owner, final String dialogTitle, final Task[] tasks, int taskPriority, final boolean modal) {
        final JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.setPreferredSize(new Dimension(250, 50));
        final JLabel label = new JLabel("");
        contentPanel.add(label, BorderLayout.NORTH);
        contentPanel.add(progressBar);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        final TaskDialog dialog;
        dialog = new TaskDialog(owner, dialogTitle, modal, tasks, taskPriority);
        dialog.setVisible(true);

    }

    public static JComponent createLabelledComponent(String label, Component component) {
        return createLabelledComponent(new JLabel(label), component);
    }

    public static JComponent createLabelledComponent(JLabel label, Component component) {
        Box box = new Box(BoxLayout.X_AXIS) {
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        box.add(label);
        box.add(Box.createHorizontalStrut(3));
        box.add(component);
        box.setOpaque(false);
        return box;
    }

    public static JComponent createTopLabelledComponent(String labelname, JComponent component) {
        return createTopLabelledComponent(new JLabel(labelname), component);
    }

    public static JComponent createTopLabelledComponent(JLabel label, JComponent component) {
        return new LabelledComponent(label, component, 1, LabelledComponent.TOP);
    }

    /**
     * t == 0 return start, t==1 return finish
     * note from Dan umm, this no longer seems to be the case. but if you ensure t moves from 1 -> 0, thigs look ok
     * <p/>
     * Note from BM: Do we really want t==1 to return finish or do we want something else?
     * What we're doing here changing the ratio of a blend operation.  Traditionally a
     * graphical 'Highlight' is actually a multiplicative operation rather than a blend.
     * For instance, if you take a yellow marker pen over a blue sheet, you end up with
     * green as the full highlight color rather than yellow (which is painting).  Unfortunately,
     * in RGB world, a multiplication only appears correct on greys to whites owing to its additive nature.
     * If you deal with colour indices as floats, you'll need something like:
     * multiplyColor = (white * (1 - t)) + (green * t);
     * = [1 - t, 1 - t, 1 - t] + [0, 1 * t, 0]
     * = [1 - t
     * finalColor = startColor * multiplyColor
     * so for t = 0.5 and startColor = [0.9, 0.9, 0.9] (lightGray),
     * finalColor = [0.9, 0.9, 0.9] * [0.5, 1.0, 0.5]
     * = [0.45, 0.9, 0.45] which I think is right.
     */
    public static Color blend(Color start, Color finish, float t) {
        float[] sc = start.getRGBColorComponents(null);
        float[] fc = finish.getRGBColorComponents(null);
        try {
            t = 1.0f - t;
//            Color color = new Color((1 - t + sc[0] * t) * fc[0], (1 - t + sc[1] * t) * fc[1], (1 - t + sc[2] * t) * fc[2]);
            Color color = new Color(sc[0] + (fc[0] - sc[0]) * t, sc[1] + (fc[1] - sc[1]) * t, sc[2] + (fc[2] - sc[2]) * t);
//            if (log.isDebug()) log.debug("Start: "+start+" t= "+t+" color = "+color+" finish "+finish);
            return color;
        } catch (Exception e) {
            log.error(e);
        }
        return (t == 1.0 ? finish : start);
    }

    public static Color multiply(Color a, Color b) {
        if (a == null) return b;
        if (b == null) return a;

        float[] ca = a.getColorComponents(null);
        float[] cb = b.getColorComponents(null);
        Color color = new Color(ca[0] * cb[0], ca[1] * cb[1], ca[2] * cb[2]);
        return color;
    }

    public static void formatSpinner(JSpinner spinner, String format) {
        JComponent editor = spinner.getEditor();
        Dimension preferredSize = null;

        if (editor instanceof JSpinner.NumberEditor) {
            JSpinner.NumberEditor numberEditor = (JSpinner.NumberEditor) editor;
            numberEditor.getFormat().applyPattern(format);
            preferredSize = calculateDimensionForFormat(format);
        } else if (editor instanceof JSpinner.DateEditor) {
            JSpinner.DateEditor dateEditor = (JSpinner.DateEditor) editor;
            dateEditor.getFormat().applyPattern(format);
            preferredSize = calculateDimensionForDateFormat(format);
        }

        if (preferredSize != null) {
            preferredSize.height = preferredSize.height - spinner.getInsets().top - spinner.getInsets().bottom;
            preferredSize.width = preferredSize.width - spinner.getInsets().left - spinner.getInsets().right;
            editor.setPreferredSize(preferredSize);
        }
    }

    public static Dimension calculateDimensionForDateFormat(String dateFormatString) {
        // the standard dateSpinner size is based on its contents, therefore if you initalise one with a null date
        // it appears as a smaller spinner.  Override the preferredSize so it is based on the date format
        DateFormat dateFormat = new SimpleDateFormat(dateFormatString);
        Calendar dummyCalendar = Calendar.getInstance();
        dummyCalendar.set(2222, 02, 22, 22, 22, 22); // try and achieve the maximum size, therefore no '1' digits
        JTextField dateFormatField = new JTextField(dateFormat.format(dummyCalendar.getTime()));
        Dimension preferredSize = dateFormatField.getPreferredSize();
        // Increase the width to accommodate the some of space the JSpinner button uses on certain LookAndFeel Themes.
        preferredSize.width = preferredSize.width + 4;
        return preferredSize;
    }

    public static Dimension calculateDimensionForFormat(String formatString) {
        return new JTextField(formatString).getPreferredSize();
    }

    public static int getMaxWidth(Component[] components) {
        int maxWidth = 0;
        for (Component component : components) {
            if (component.getPreferredSize().width > maxWidth) {
                maxWidth = component.getPreferredSize().width;
            }
        }
        return maxWidth;
    }

    public static Component getWidestComponent(Component[] components) {
        int maxWidth = 0;
        Component widestComponent = null;
        for (Component component : components) {
            if (component.getPreferredSize().width > maxWidth) {
                maxWidth = component.getPreferredSize().width;
                widestComponent = component;
            }
        }
        return widestComponent;
    }

    public static void runInDispatchThread(Runnable addToGui) {
        if (EventQueue.isDispatchThread()) {
            addToGui.run();
        } else {
            EventQueue.invokeLater(addToGui);
        }
    }

    /**
     * Iterates over all cells in a table model (touches them).  Intended to be used in a non-gui thread to reduce
     * gui blocking.
     */
    public static void touchAllCells(BeanFormTableModel tableModel) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            for (int j = 0; j < tableModel.getColumnCount(); j++) {
                tableModel.getValueAt(i, j);
            }
        }
    }

    public static boolean usingAppleLAF() {
        return UIManager.getLookAndFeel().getClass().getName().startsWith("apple.laf");
    }

    public static Color createTranslucentColor(Color color, float ratio) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * ratio));
    }

    public static Color arbitratryHSBOperation(Color color, float rH, float rS, float rB) {
        assert rH > -1f && rH <= 1f && rS > -1f && rS <= 1f && rB > -1f && rB <= 1f;

        float hsb[] = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float multipliers[] = new float[]{rH, rS, rB};

        for (int i = 0; i < hsb.length; i++) {
            float m = multipliers[i];
            if (m < 0) {
                hsb[i] = hsb[i] - (Math.abs(m) * hsb[i]);
            } else {
                hsb[i] = hsb[i] + (Math.abs(m) * (1f - hsb[i]));
            }
        }
        return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
    }

    private static class RunnableTask extends Task {
        private final Runnable task;

        public RunnableTask(String dialogMessage, Runnable task) {
            super(dialogMessage);
            this.task = task;
        }

        public void run() throws Throwable {
            task.run();
        }
    }

    public static void displayValidationExceptionDialog(List<String> errorMessages, final String title, final Component owner) {
        final StringBuffer errorMessageBuffer = new StringBuffer("<html>");
        String message;

        //limit the max number of errors shown, so the dialog isn't 9 million pixels high
        int maxToShow = Math.min(20, errorMessages.size());
        for (int i = 0; i < maxToShow; i++) {
            if (i > 0) errorMessageBuffer.append("<br>");
            message = errorMessages.get(i);
            errorMessageBuffer.append(message);
            log.warn(message);
        }

        int notShown = errorMessages.size() - maxToShow;
        if ( notShown > 0 ) {
            errorMessageBuffer.append("<br>");
            message = notShown + " more";
            errorMessageBuffer.append(message);
            log.warn(message);
        }

        errorMessageBuffer.append("</html>");

        runInDispatchThread(
            new Runnable() {
                public void run() {
                    AlwaysOnTopJOptionPane.showMessageDialog(
                            UIUtilities.getWindowParent(owner),
                            errorMessageBuffer.toString(),
                            title,
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        );
    }

    public static void displayValidationExceptionDialog(ValidationException e, String title, Component owner) {
        displayValidationExceptionDialog(Arrays.asList(e.getErrorMessages()), title, owner);
    }

    public static Box createErrorComponent(final String errorText) {
        ImageIcon errorIcon = ImageIconCache.getImageIcon("error.png");
        JLabel errorLabel = new JLabel(errorText, errorIcon, SwingConstants.CENTER);
        errorLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        errorLabel.setMaximumSize(errorLabel.getPreferredSize());

        Box centerX = Box.createHorizontalBox();
        centerX.add(Box.createGlue());
        centerX.add(errorLabel);
        centerX.add(Box.createGlue());

        Box errorComponent = Box.createVerticalBox();
        errorComponent.add(Box.createGlue());
        errorComponent.add(centerX);
        errorComponent.add(Box.createGlue());
        return errorComponent;
    }

    public static TreePath createTreePathFromTreeNode(TreeNode treeNode) {
        ArrayList<TreeNode> list = new ArrayList<TreeNode>();
        do {
            list.add(treeNode);
            treeNode = treeNode.getParent();
        } while (treeNode != null);
        Collections.reverse(list);

        return new TreePath(list.toArray(new Object[list.size()]));
    }

    public static void centreInFocusedWindow(Window window, int xOffset, int yOffset) {
        Window focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        centreInContainer(focusedWindow, window, xOffset, yOffset);
    }

    public static void displayProgressFrame(final Container owner, String title, int timeoutMillis, String message, final Runnable yesRunnable, final Runnable noRunnable, final Runnable timeoutRunnable, final boolean cancelable, final boolean alwaysOnTop) {
        final JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setIconImage(Workspace.getInstance().getApplicationFrame().getIconImage());
        frame.setResizable(false);

        JPanel textPanel = new JPanel(new BorderLayout());
        final JProgressBar progressBar = new JProgressBar(0, timeoutMillis);
        progressBar.setStringPainted(true);

        textPanel.add(new JLabel(message), BorderLayout.CENTER);
        textPanel.add(progressBar, BorderLayout.SOUTH);
        textPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        final MutableBoolean buttonPressed = new MutableBoolean(false);

        JButton yesButton = new JButton(new AbstractAction("Yes") {
            public void actionPerformed(ActionEvent e) {
                synchronized (buttonPressed) {
                    if (!buttonPressed.value) {
                        buttonPressed.value = true;
                        yesRunnable.run();
                        frame.dispose();
                    }
                }
            }
        });

        JButton noButton = new JButton(new AbstractAction("No") {
            public void actionPerformed(ActionEvent e) {
                synchronized (buttonPressed) {
                    if (!buttonPressed.value) {
                        buttonPressed.value = true;
                        noRunnable.run();
                        frame.dispose();
                    }
                }
            }
        });

        JButton cancelButton = null;
        if (cancelable) {
            cancelButton = new JButton(new AbstractAction("Cancel") {
                public void actionPerformed(ActionEvent e) {
                    synchronized (buttonPressed) {
                        if (!buttonPressed.value) {
                            buttonPressed.value = true;
                            frame.dispose();
                        }
                    }
                }
            });
        }

        final Timer progressTimer = new Timer(1000, new ActionListener() {
            private int seconds;

            public void actionPerformed(ActionEvent e) {
                seconds++;
                progressBar.setValue(seconds * 1000);
                progressBar.setString("" + seconds);
            }
        });

        final Timer timer = new Timer(timeoutMillis, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                synchronized (buttonPressed) {
                    if (!buttonPressed.value) {
                        buttonPressed.value = true;
                        timeoutRunnable.run();
                        frame.dispose();
                    }
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        if (cancelButton != null) buttonPanel.add(cancelButton);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(textPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        frame.setContentPane(panel);

        UIUtilities.runInDispatchThread(new Runnable() {
            public void run() {
                frame.pack();
                UIUtilities.centreInContainer(owner, frame, 0, 0);
                frame.show();
                if (alwaysOnTop) 
                {
                	frame.setAlwaysOnTop(true);
                }
                progressTimer.start();
                timer.start();
            }
        });
    }

    public static BufferedImage[] getScreenCaptures() throws AWTException {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        toolkit.sync();

        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] graphicsDevices = graphicsEnvironment.getScreenDevices();
        BufferedImage[] bufferedImages = new BufferedImage[graphicsDevices.length];
        for (int i = 0; i < graphicsDevices.length; i++) {
            GraphicsDevice graphicsDevice = graphicsDevices[i];
            GraphicsConfiguration graphicsConfiguration = graphicsDevice.getDefaultConfiguration();
            Rectangle virtualBounds = graphicsConfiguration.getBounds();
            Robot robot = new Robot(graphicsDevice);
            robot.setAutoDelay(0);
            robot.setAutoWaitForIdle(false);
            if (virtualBounds.getWidth() > 0 && virtualBounds.getHeight() > 0) {
                bufferedImages[i] = robot.createScreenCapture(new Rectangle(0, 0, virtualBounds.width, virtualBounds.height));
            }
        }

        return bufferedImages;
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        Runnable runnable = new Runnable() {
            public void run() {
                final Task waitTask = new RunnableTask("Waiting 5 seconds", new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                        }
                    }
                });

                final JFrame frame = new JFrame("Test ProgressBar");
                JButton launchButton = new JButton(new AbstractAction("Launch") {
                    public void actionPerformed(ActionEvent e) {
                        UIUtilities.runTasksWithProgressDialog(frame.getContentPane(), "Test", new Task[]{waitTask}, Thread.NORM_PRIORITY, true);
                    }
                });

                frame.getContentPane().add(launchButton);
                frame.pack();
                frame.show();
            }
        };
        EventQueue.invokeAndWait(runnable);
    }
}

