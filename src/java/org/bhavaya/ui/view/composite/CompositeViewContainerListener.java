package org.bhavaya.ui.view.composite;

import org.bhavaya.ui.view.AbstractView;
import org.bhavaya.ui.view.View;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 06-May-2008
 * Time: 17:01:40
 */
public interface CompositeViewContainerListener {

    public void viewAdded(ViewContainerEvent viewEvent);

    public void viewRemoved(ViewContainerEvent viewEvent);


    public static class ViewContainerEvent {

        private Object source;
        private View viewAffected;

        public ViewContainerEvent(Object source, View viewAffected) {
            this.source = source;
            this.viewAffected = viewAffected;
        }

        public Object getSource() {
            return source;
        }

        public View getViewAffected() {
            return viewAffected;
        }
    }

}
