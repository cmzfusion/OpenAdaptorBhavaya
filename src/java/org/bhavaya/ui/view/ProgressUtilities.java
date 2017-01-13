package org.bhavaya.ui.view;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 09-Apr-2009
 * Time: 10:00:04
 */
public class ProgressUtilities {

    public static final ProgressIndicator DUMMY_PROGRESS_INDICATOR = new ProgressIndicator() {

        public void startProgressAnimation(String message) {}

        public void stopProgressAnimation() {}

        public void setProgress(boolean displayProgressBar, int currentStep, int totalSteps, String message) {}
    };

    /**
     * @return  The component itself, if it implements ProgressIndicator.
     * Alternatively returns the first available ancestor in the component hierarchy which implements
     * ProgressIndicator, or the DUMMY_PROGRESS_INDICATOR if there is no such component available
     */
    public static ProgressIndicator findProgressIndicator(Component c) {
        ProgressIndicator result;
        if ( c instanceof ProgressIndicator) {
            result = (ProgressIndicator)c;
        } else if ( c.getParent() != null ) {
            result = findProgressIndicator(c.getParent());
        } else {
            result = DUMMY_PROGRESS_INDICATOR;
        }
        return result;
    }
}
