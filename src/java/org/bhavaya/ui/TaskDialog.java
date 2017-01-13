package org.bhavaya.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.bhavaya.util.Log;
import org.bhavaya.util.Task;
import org.bhavaya.util.Utilities;

/**
 * Created with IntelliJ IDEA.
 * User: ga2armn
 * Date: 07/10/13
 * Time: 08:46
 */
public class TaskDialog extends JDialog {

    private static final Log log = Log.getCategory(TaskDialog.class);

    private JProgressBar[] progressBars;
    private Component ownerComponent;
    private Task[] tasks;
    private JLabel label;
    private int taskPriority;
    private String dialogTitle = "";

    public TaskDialog(Component component,
                      String title,
                      boolean modal,
                      Task[] tasks,
                      int taskPriority){
        super(UIUtilities.getWindowParent(component), title);
        setModal(modal);
        this.dialogTitle = title;
        ownerComponent = component;
        this.tasks = tasks;
        this.taskPriority = taskPriority;
        init();
    }

    protected void init(){
        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new CompactGridLayout(tasks.length, 2, 10, 5));

        progressBars = new JProgressBar[tasks.length];
        for(int i= 0; i< tasks.length; i++){
            JPanel p = new JPanel();
            progressBars[i] = new JProgressBar();
            progressBars[i].setValue(0);
            progressBars[i].setPreferredSize(new Dimension(100, 15));
            progressPanel.add(new JLabel(tasks[i].getName()));
            progressPanel.add(progressBars[i]);
        }
        progressPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        label = new JLabel("Task : ");
        this.getContentPane().add(label, BorderLayout.NORTH);
        this.getContentPane().add(progressPanel, BorderLayout.CENTER);

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        pack();

    }

    public void setVisible(boolean visible){
        if(visible){
            UIUtilities.centreInContainer(ownerComponent, this, 0, 0);
            processTasks();
            super.setVisible(true);
        }
        else{
            super.setVisible(false);
        }
    }

    SwingWorker sw;

    protected void processTasks() {
        final Thread thread = Utilities.newThread(new Runnable() {
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    }
                });

                try {
                    for (int i=0; i< tasks.length; i++) {
                        final int pInd = i;
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                progressBars[pInd].setIndeterminate(true);
                                label.setText("Executing Task : " + tasks[pInd].getName());
                            }
                        });
                        try{
                            System.out.println("Executing " + tasks[pInd].getName());

                            tasks[pInd].run();
                        }  catch (Throwable t) {
                       	    log.error("Error, " + dialogTitle + ", while running task: " + label.getText(), t);
                       	    displayErrorwithTimeout();
                       }
                       finally{
                            SwingUtilities.invokeAndWait(new Runnable() {
                                public void run() {
                                    progressBars[pInd].setIndeterminate(false);
                                    progressBars[pInd].setValue(100);
                                    label.setText("Finished Task : " + tasks[pInd].getName());}
                            });
                        }
                    }
                }
                catch (Throwable t) {
                  	 log.error("Error, " + dialogTitle + ", while running task: " + label.getText(), t);
                }
                finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            dispose();
                        }
                    });
                }
            }
        }, "ProgressDialogTaskRunner", false);

        if (taskPriority > 0) {
            thread.setPriority(taskPriority);
        }
        thread.start();
    }
    
    
    protected void displayErrorwithTimeout()
    {
    	String errorMessage =  "Error" + (dialogTitle != null && dialogTitle.length() > 0 ? (", " + dialogTitle + ",") : "")
                + " while running task: " + label.getText();
        final JOptionPane errorPane = new JOptionPane(errorMessage, JOptionPane.ERROR_MESSAGE );
        final JDialog dialog = errorPane.createDialog(TaskDialog.this, "Error");
          
        TimerTask tt = new TimerTask(){
            public void run()
            {
                if (dialog != null)
                   dialog.dispose();
            }
        };
        int timeout_ms = 60000; // Gone in 60 seconds...
        Timer timer = new Timer();
        timer.schedule(tt, timeout_ms);
        dialog.setVisible(true);                	
    }
    
    
    public static void main(String a[]){
        JFrame jf = new JFrame();
        jf.setPreferredSize(new Dimension(200,200));
        jf.pack();
        jf.setVisible(true);

        Task[] tasks = new Task[3];
        for(int i=0; i<tasks.length; i++){
            tasks[i] = new Task("Test " + i){
                @Override
                public void run() throws AbortTaskException, Throwable {
                     Thread.sleep(5000);
                     throw new Exception("no one expects the inquisition!");
                }
            };

        }
        TaskDialog td = new TaskDialog(jf, "Test", true, tasks, 1);
        td.setVisible(true);
    }
}
