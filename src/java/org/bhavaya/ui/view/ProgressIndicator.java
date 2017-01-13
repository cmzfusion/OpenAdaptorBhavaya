package org.bhavaya.ui.view;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 09-Apr-2009
 * Time: 09:55:25
 */
public interface ProgressIndicator {
    
    void startProgressAnimation(String message);

    void stopProgressAnimation();

    void setProgress(boolean displayProgressBar, int currentStep, int totalSteps, String message);
}
