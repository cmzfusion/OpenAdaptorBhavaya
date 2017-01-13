package org.bhavaya.ui.view;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 21-Aug-2008
 * Time: 10:43:14
 */
public abstract class ProgressAnimationView extends AbstractView implements ProgressIndicator {

    private ProgressLayeredPane progressPane;

    public ProgressAnimationView(String name, String tabTitle, String frameTitle) {
        super(name, tabTitle, frameTitle);
    }

    public final Component getComponent() {
        if ( progressPane == null ) {
            progressPane = new ProgressLayeredPane();
            progressPane.setViewComponent(doGetComponent());
        }
        return progressPane;
    }

    protected abstract Component doGetComponent();

    public void startProgressAnimation(String message) {
        if(progressPane != null) {
            progressPane.startProgressAnimation(message);
        }
    }

    public void stopProgressAnimation() {
        if(progressPane != null) {
            progressPane.stopProgressAnimation();
        }
    }

    public void setProgress(boolean displayProgressBar, int currentStep, int totalSteps, String message) {
        if(progressPane != null) {
            progressPane.setProgress(displayProgressBar, currentStep, totalSteps, message);
        }
    }
}
