package org.bhavaya.ui;

import org.bhavaya.ui.table.BeanFormTableModel;
import org.bhavaya.ui.table.DateTimeRenderer;
import org.bhavaya.ui.table.DecimalRenderer;
import org.bhavaya.ui.view.Workspace;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.TimeZone;
import java.util.ArrayList;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.4.38.1 $
 */
public class ViewBeanAction extends AuditedAbstractAction {
    private static final Log log = Log.getCategory(ViewBeanAction.class);

    private static final DateTimeRenderer SQL_DATE_RENDERER = new DateTimeRenderer("dd-MMM-yyyy", TimeZone.getTimeZone("GMT"));
    private static final DateTimeRenderer DATETIME_RENDERER = new DateTimeRenderer("dd-MMM-yyyy - HH:mm:ss", TimeZone.getDefault());
    private static final DateTimeRenderer TIME_RENDERER = new DateTimeRenderer("HH:mm:ss", TimeZone.getDefault());

    private Observable bean;
    private String windowTitle;
    private String[] beanPathArray;
    private String[] displayNameArray;

    public ViewBeanAction(Observable bean) {
        super("View " + ClassUtilities.getDisplayName(bean.getClass()));
        this.bean = bean;
    }

    public ViewBeanAction(String name, Observable bean) {
        super(name);
        this.bean = bean;
    }

    public ViewBeanAction(String name, String windowTitle, Observable bean) {
        super(name);
        this.bean = bean;
        this.windowTitle = windowTitle;
    }

    public Observable getBean() {
        return bean;
    }

    public void setBeanPathArray(String[] beanPathArray) {
        this.beanPathArray = beanPathArray;
    }

    public void setDisplayNameArray(String[] displayNameArray) {
        this.displayNameArray = displayNameArray;
    }

    /**
     * Override to return a custom renderer for a property. Return null if a standard renderer for the
     * type should be used.
     */
    protected TableCellRenderer getPropertyRenderer(String propertyBeanPath) {
        return null; 
    }

    public void auditedActionPerformed(final ActionEvent e) {
        if (bean == null) return;
        UIUtilities.runTaskWithProgressDialog((String) getValue(Action.NAME), "Loading data...", new Runnable() {
            public void run() {

                if (beanPathArray == null) {
                    PropertyModel propertyModel = PropertyModel.getInstance(bean.getClass());
                    Attribute[] attributes = propertyModel.getAttributes();
                    ArrayList beanPathList = new ArrayList(attributes.length);
                    for (int i = 0; i < attributes.length; i++) {
                        Attribute attribute = attributes[i];
                        if (!(attribute.getName().equals("class") || attribute.getName().equals("propertyChangeListeners"))) {
                            beanPathList.add(attribute.getName());
                        }
                    }
                    beanPathArray = (String[]) beanPathList.toArray(new String[beanPathList.size()]);
                }

                if (displayNameArray == null) {
                    displayNameArray = new String[beanPathArray.length];
                    for (int i = 0; i < beanPathArray.length; i++) {
                        String[] splitBeanPath = Generic.beanPathStringToArray(beanPathArray[i]);
                        displayNameArray[i] = Utilities.getDisplayName(splitBeanPath[splitBeanPath.length - 1]);
                    }
                }

                final BeanFormTableModel tableModel = new BeanFormTableModel(bean, beanPathArray, displayNameArray);
                UIUtilities.touchAllCells(tableModel);

                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        String title = windowTitle == null ? (String) getValue(Action.NAME) : windowTitle;
                        final JFrame frame = new GenericFrame(title) {
                            public void dispose() {
                                try {
                                    tableModel.dispose();
                                    super.dispose();
                                } catch (Exception e1) {
                                    log.error(e1);
                                }
                            }
                        };

                        final JTable table = new JTable(tableModel) {
                            public TableCellRenderer getCellRenderer(int row, int column) {
                                if (column == 1) {
                                    TableCellRenderer renderer = getPropertyRenderer(beanPathArray[row]);
                                    if (renderer != null) return renderer;
                                }
                                Object value = getValueAt(row, column);
                                if (value instanceof java.sql.Date) {
                                    return SQL_DATE_RENDERER;
                                } else if (value instanceof java.sql.Time) {
                                    return TIME_RENDERER;
                                } else if (value instanceof java.util.Date) {
                                    return DATETIME_RENDERER;
                                } else if (value instanceof Number) {
                                    return new DecimalRenderer();
                                }
                                return super.getCellRenderer(row, column);
                            }
                        };
                        table.setCellSelectionEnabled(true);
                        //specially for jeremy
                        table.addMouseListener(new MouseAdapter() {
                            public void mouseClicked(MouseEvent e) {
                                if (e.getButton() == MouseEvent.BUTTON2) {
                                    TransferHandler.getCopyAction().actionPerformed(new ActionEvent(table, 0, ""));
                                }
                            }
                        });
                        table.getColumnModel().getColumn(0).setPreferredWidth((int) (table.getPreferredScrollableViewportSize().width * 0.3));
                        table.getColumnModel().getColumn(0).setMaxWidth((int) (table.getPreferredScrollableViewportSize().width * 0.3) + 100);
                        JScrollPane scrollPane = new JScrollPane(table);

                        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
                        buttonPanel.add(new JButton(new AuditedAbstractAction("Close") {
                            public void auditedActionPerformed(ActionEvent e) {
                                frame.dispose();
                            }
                        }));

                        JPanel mainPanel = new JPanel(new BorderLayout());
                        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
                        mainPanel.add(scrollPane, BorderLayout.CENTER);
                        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

                        frame.setIconImage(Workspace.getInstance().getApplicationFrame().getIconImage());
                        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                        frame.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, UIUtilities.customForwardTraversalKeystrokes);
                        frame.getContentPane().add(mainPanel);
                        frame.setResizable(true);
                        frame.pack();
                        UIUtilities.centreInContainer(UIUtilities.getWindowParent((Component) e.getSource()), frame, 40, 0);
                        frame.setVisible(true);
                        frame.toFront();
                    }
                });
            }
        }, Thread.NORM_PRIORITY, false);
    }
}
