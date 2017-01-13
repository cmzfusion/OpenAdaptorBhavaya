package org.bhavaya.ui.diagnostics;

import org.bhavaya.ui.MenuGroup;
import org.bhavaya.ui.AuditedAbstractAction;
import org.bhavaya.util.Log;

import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.io.File;

/**
 * Adds profiling options to diagnostic context menu.
 * <p/>
 * Usage:
 * <pre>
       ApplicationDiagnostics.getInstance().addDiagnosticContext(new YourKitDiagnosticContext());
 * </pre>
 * To enable YourKit profiler you need to
 * <ul>
 * <li>add yjp-controller-api-redist.jar to your lib directory</li>
 * <li>add yjpagent.dll to your path</li>
 * <li>add this VM parameter to your startup script: -Xrunyjpagent or -agentlib:yjpagent for Java 5.0 and higher (note that you can use path like format here: -XrunShivaApps/Shiva/lib/yjpagent)</li>
 * </ul>
 * Read more in your YourKit docs.
 * <p/>
 * Note for Trove users: YourKit's yjp.jar library contains trove library which in my case conflicted
 * with stand-alone trove library. You must make sure trove.jar is in classpath before yjp.jar.
 * <p>
 * Bad news is that YourKit cannot be used with web started applications as the -Xrunyjpagent option
 * will be rejected/ignored by JWS :-(
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.8 $
 */
public class YourKitDiagnosticContext extends DiagnosticContext {
    /*
     * Implementation note: This class uses reflection to invoke YourKit calls as I don't want
     * our compilation process to be dependent on their libraries.
     */

    private static final Log log = Log.getCategory(YourKitDiagnosticContext.class);
    private static final Log userLog = Log.getUserCategory();
    private Object controler;
    private String initErrorCause; // Explanation why YourKit is not available

    public YourKitDiagnosticContext() {
        super("YourKit", null);
        try {
            Class clazz = Class.forName("com.yourkit.api.Controller");
            controler = clazz.newInstance();
        } catch (Throwable t) {
            initErrorCause = t.toString();
            log.info("YourKit is not available due to: " + initErrorCause);
        }
    }

    public Component createComponent() {
        return null;
    }

    public String createHTMLDescription() {
        return null;
    }

    public MenuGroup[] createMenuGroups() {
        if (controler != null) {
            Action memorySnapshotAction = new MemorySnapshotAction();
            Action cpuUsageAction = new CpuUsageAction();
            MenuGroup menuGroup = new MenuGroup("Profiling", KeyEvent.VK_P);
            menuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(memorySnapshotAction)));
            menuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(cpuUsageAction)));
            return new MenuGroup[]{menuGroup};
        } else {
            MenuGroup menuGroup = new MenuGroup("Profiling", KeyEvent.VK_P);
            menuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new InitErrorInfoAction())));
            return new MenuGroup[]{menuGroup};
        }
    }

    public Attachment[] createAttachments() {
        return new Attachment[0];
    }

    public Object createMBean() {
        if (controler != null) {
            return new DynamicMBeanAdaptor(controler) {
                public Object invoke(String name, Object[] params, String[] signature) throws MBeanException, ReflectionException {
                    Object result = super.invoke(name, params, signature);
                    if (name.startsWith("capture") && result instanceof String) {
                        // send the resulting file by email
                        File f = new File((String) result);
                        if (f.exists() && (f.length() < 10000000)) {
                            HeadlessApplicationDiagnostics.getHeadlessInstance().sendFile((String) result);
                        } else {
                            result = "Unsent: " + result;
                        }
                    }
                    return result;
                }
            };
        } else {
            return new YourKitInfo();
        }
    }

    private class MemorySnapshotAction extends AuditedAbstractAction {
        public MemorySnapshotAction() {
            super("Capture memory snapshot");
        }

        public void auditedActionPerformed(ActionEvent e) {
            if (controler == null) return;
            setEnabled(false);
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        Class clazz = controler.getClass();
                        Method method = clazz.getMethod("captureMemorySnapshot", boolean.class);
                        String fileName = (String) method.invoke(controler, Boolean.FALSE);
                        if (fileName != null) {
                            HeadlessApplicationDiagnostics.getHeadlessInstance().sendFile(fileName);
                        }
                        userLog.info("Memory snapshot captured");
                    } catch (Throwable t) {
                        log.error("Couldn't capture memory snapshot", t);
                        userLog.error("Couldn't capture memory snapshot");
                    } finally {
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                setEnabled(true);
                            }
                        });
                    }
                }
            });
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.start();
        }
    }

    private class CpuUsageAction extends AuditedAbstractAction {
        private boolean sampling;

        public CpuUsageAction() {
            super("Start CPU sampling");
        }

        public void auditedActionPerformed(ActionEvent e) {
            if (controler == null) return;
            setEnabled(false);
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        Class clazz = controler.getClass();
                        if (!sampling) {
                            Method method = clazz.getMethod("startCPUSampling");
                            method.invoke(controler);
                            EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    putValue(Action.NAME, "Capture CPU usage data");
                                }
                            });
                            sampling = true;
                        } else {
                            Method method = clazz.getMethod("captureCPUSnapshot", boolean.class);
                            String fileName = (String) method.invoke(controler, Boolean.FALSE);
                            EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    putValue(Action.NAME, "Start CPU sampling");
                                }
                            });
                            sampling = false;
                            if (fileName != null) {
                                HeadlessApplicationDiagnostics.getHeadlessInstance().sendFile(fileName);
                            }
                            userLog.info("CPU usage data captured.");
                        }
                    } catch (Throwable t) {
                        log.error("Couldn't capture CPU usage data (sampling:" + sampling + ")", t);
                        userLog.error("Couldn't capture CPU usage data.");
                    } finally {
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                setEnabled(true);
                            }
                        });
                    }
                }
            });
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.start();
        }
    }

    private class InitErrorInfoAction extends AuditedAbstractAction {
        public InitErrorInfoAction() {
            super("YourKit Info");
        }

        public void auditedActionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(null, initErrorCause, "YourKit not available", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public interface YourKitInfoMBean {
        public String getInitErrorCause();
    }

    public class YourKitInfo implements YourKitInfoMBean {
        public String getInitErrorCause() {
            return initErrorCause;
        }
    }
}
