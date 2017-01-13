package org.bhavaya.ui.view.composite;

import org.bhavaya.ui.view.View;

import javax.swing.*;
import java.awt.*;

/**
 * Lay out child views in a tabbed pane
 */
public class TabbedLayout implements CompositeViewLayoutStrategy {

    private JTabbedPane tabbedPane = new JTabbedPane();

    public TabbedLayout() {
        this(JTabbedPane.TOP);
    }

    public TabbedLayout(int tabPlacement) {
        tabbedPane.setTabPlacement(tabPlacement);
    }

    protected JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public void configureContainer(Container c) {
        c.setLayout(new BorderLayout());
        c.add(tabbedPane, BorderLayout.CENTER);
    }

    public void addComponents(Container c, View[] existingViews, View... viewsToAdd) {
        for (View view : viewsToAdd) {
            if ( view.isDisplayable()) {
                TabbedViewHolder viewHolder = new TabbedViewHolder(view);
                tabbedPane.add(view.getTabTitle(), viewHolder);
            }
        }
    }

    public void removeComponents(Container c, View[] existingViews, View... viewsToRemove) {
        for (View view : viewsToRemove) {
            removeView(view);
        }
    }

    private void removeView(View view) {
        if ( view.isDisplayable()) {
            tabbedPane.remove(getIndexForView(view));
        }
    }

    public void renameView(View view) {
        if ( view.isDisplayable()) {
            tabbedPane.setTitleAt(getIndexForView(view), view.getTabTitle());
        }
    }

    private int getIndexForView(View view) {
        for (int loop = 0; loop < tabbedPane.getTabCount(); loop++) {
            if (((TabbedViewHolder) tabbedPane.getComponentAt(loop)).isHoldingView(view)) {
                return loop;
            }
        }
        return -1;
    }

    public View getSelectedView() {
        if (tabbedPane.getSelectedComponent() != null) {
            return ((TabbedViewHolder) tabbedPane.getSelectedComponent()).getView();
        }
        return null;
    }


    public void dispose(Container c) {
        tabbedPane.removeAll();
    }

    public String getDescription() {
        return "Tabbed";
    }

    /**
     * Hold the menu, toolbar and view component in a tab
     */
    public class TabbedViewHolder extends JPanel {
        private View view;

        public TabbedViewHolder(View view) {
            this.view = view;
            setLayout(new BorderLayout());
            add(view.getComponent(), BorderLayout.CENTER);
        }

        public boolean isHoldingView(View myview) {
            return myview == view;
        }

        public View getView() {
            return view;
        }
    }

}
