package org.bhavaya.ui.view;

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.ui.*;
import org.bhavaya.ui.view.composite.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ebbuttn
 * Date: 12-Feb-2008
 * Time: 10:35:53
 * To change this template use File | Settings | File Templates.
 * <p/>
 * A view composed of several child view components,
 * which has a layout concept to determine how the child view components are arranged
 * By default a MenuPanel is supplied which allows the user to change the layout and 
 * set whether each child view is displayable
 */
public abstract class AbstractCompositeView extends ProgressAnimationView {

    private Component viewComponent;
    private JComponent mainViewContainer = new JPanel();
    private ArrayList<View> childViews = new ArrayList<View>();
    private CompositeViewLayoutStrategy layoutStrategy = new TabbedLayout();
    private BeanCollection beanCollection;
    private List<CompositeViewContainerListener> compositeViewContainerListeners = new ArrayList<CompositeViewContainerListener>();
    private LayoutSelectionPanel layoutSelectionPanel = new LayoutSelectionPanel();

    public AbstractCompositeView(String name, String tabTitle, String frameTitle) {
        super(name, tabTitle, frameTitle);
        addCompositeViewContainerListener(layoutSelectionPanel);
        layoutStrategy.configureContainer(mainViewContainer);
    }

    public void setLayoutStrategy(CompositeViewLayoutStrategy newLayoutStrategy) {
        disposeAndLayout(newLayoutStrategy);
        layoutSelectionPanel.selectCurrentLayout();
    }

    public CompositeViewLayoutStrategy getLayoutStrategy() {
        return layoutStrategy;
    }

    private void disposeAndLayout(CompositeViewLayoutStrategy layoutStrategy) {
        mainViewContainer.removeAll();
        this.layoutStrategy.dispose(mainViewContainer);
        this.layoutStrategy = layoutStrategy;
        layoutStrategy.configureContainer(mainViewContainer);

        addChildViewsIfInited(
                new AbstractView[0],  //have not yet added any of the children to the new layout
                childViews.toArray(new AbstractView[childViews.size()])
        );

        mainViewContainer.validate();
        mainViewContainer.repaint();
    }

    public void addChildViews(View... children) {
        addChildViewsIfInited(childViews.toArray(new AbstractView[childViews.size()]), children);
        childViews.addAll(Arrays.asList(children));
        for ( View child : children ) {
            fireViewAdded(child);
        }
    }

    public void removeChildViews(View... children) {
        removeChildViewsIfInited(childViews.toArray(new AbstractView[childViews.size()]), children);
        childViews.removeAll(Arrays.asList(children));
        for ( View child : children ) {
            fireViewRemoved(child);
        }
    }

    protected void removeAllChildViews() {
        removeChildViews((AbstractView[])childViews.toArray(new AbstractView[childViews.size()]));
    }

    private void addChildViewsIfInited(View[] existingViews, View... children) {
        if (isInited()) {
            layoutStrategy.addComponents(
                    mainViewContainer,
                    existingViews,
                    children
            );
            mainViewContainer.validate();
        }
    }

    private void removeChildViewsIfInited(View[] existingViews, View... children) {
        if (isInited()) {
            layoutStrategy.removeComponents(
                    mainViewContainer,
                    existingViews,
                    children
            );
            mainViewContainer.validate();
        }
    }

    public void addCompositeViewContainerListener(CompositeViewContainerListener l) {
        compositeViewContainerListeners.add(l);
    }

    public void removeCompositeViewContainerListener(CompositeViewContainerListener l) {
        compositeViewContainerListeners.remove(l);
    }

    private void fireViewAdded(View view) {
        CompositeViewContainerListener.ViewContainerEvent event = new CompositeViewContainerListener.ViewContainerEvent(this, view);
        for (CompositeViewContainerListener l : compositeViewContainerListeners) {
            l.viewAdded(event);
        }
    }

    private void fireViewRemoved(View view) {
        CompositeViewContainerListener.ViewContainerEvent event = new CompositeViewContainerListener.ViewContainerEvent(this, view);
        for (CompositeViewContainerListener l : compositeViewContainerListeners) {
            l.viewRemoved(event);
        }
    }

    public final ImageIcon getImageIcon() {
        return getImageIconForCompositeView();
    }

    /**
     * The composite view subclass must define which icon should be used
     */
    protected abstract ImageIcon getImageIconForCompositeView();


    protected void disposeImpl() {
        for (View child : childViews) {
            child.dispose();
        }
        super.disposeImpl();
    }

    public MenuPanel[] createMenuPanels() {
        return new MenuPanel[] {
            new MenuPanel("Layout", layoutSelectionPanel, true)
        };
    }

    /**
     * Create and layout a main component for the composite view
     * This will contain the main components of each of the
     */
    public Component doGetComponent() {
        init();
        if (viewComponent == null ) {
            viewComponent = createViewComponent(mainViewContainer);
        }
        return viewComponent;
    }

    protected void initImpl() {
        super.initImpl();
        layoutStrategy.addComponents(
                mainViewContainer,
                new AbstractView[0],
                childViews.toArray(new View[childViews.size()])
        );
    }

    public BeanCollection getBeanCollection() {
        return beanCollection;
    }

    public void setBeanCollection(BeanCollection beanCollection) {
        this.beanCollection = beanCollection;
    }

    public java.util.List<View> getChildViews() {
        return (java.util.List<View>)childViews.clone();
    }

    protected void displayErrorPanel(Throwable t) {
        removeAllChildViews();
        addChildViews(new ErrorPanelView("Error", "Error", "Error"));
        setLayoutStrategy(new HorizontalLayout());
    }

    public void setTabTitle(String tabTitle) {
        for (View view : getChildViews()) {
            if ( view instanceof AbstractView ) {
                ((AbstractView)view).setTabTitle(tabTitle);
            }
        }
        super.setTabTitle(tabTitle);
    }

    public void setFrameTitle(String frameTitle) {
        for (View view : getChildViews()) {
            if ( view instanceof AbstractView ) {
                ((AbstractView)view).setFrameTitle(frameTitle);
            }
        }
        super.setFrameTitle(frameTitle);
    }


    /**
     * An error panel which can be used if the composite view fails to load correctly
     */
    private static class ErrorPanelView extends AbstractView {

        public ErrorPanelView(String name, String tabTitle, String frameTitle) {
            super(name, tabTitle, frameTitle);
        }

        public ViewContext getViewContext() {
            return new DefaultViewContext(this);
        }


        public Component getComponent() {
            return UIUtilities.createErrorComponent("<html>There was a problem loading the data.<br>Please go to the Help/Diagnotics menu and send a report to support</html>");
        }

        public BeanCollection getBeanCollection() {
            return null;
        }

        public void setBeanCollection(BeanCollection beanCollection) {
        }
    }

    /**
     * A component which can be used to control the layout of the composite view
     */
    private class LayoutSelectionPanel extends JPanel implements CompositeViewContainerListener {

        private List<ViewCheckBox> viewCheckBoxes = new ArrayList<ViewCheckBox>();
        private Box childPanel;
        private Set<JRadioButton> layoutRadioButtons = new HashSet<JRadioButton>();

        public LayoutSelectionPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            ButtonGroup buttonGroup = new ButtonGroup();

            Box layoutPanel = new Box(BoxLayout.Y_AXIS);
            layoutPanel.setBorder(new TitledBorder("Alignment"));
            add(layoutPanel);

            add(Box.createVerticalStrut(10));

            childPanel = new Box(BoxLayout.Y_AXIS);
            childPanel.setBorder(new TitledBorder("Views:"));
            add(childPanel);

            //need to sort out how split layouts will deal with more than 2 child views
            addLayoutRadioButtons(
                    layoutPanel,
                    buttonGroup,
                    new HorizontalLayout(),
                    new VerticalLayout(),
                    new TabbedLayout()
            );
            selectCurrentLayout();
        }

        public void selectCurrentLayout() {
            for( JRadioButton button : layoutRadioButtons ) {
                if (button.getText().equals(layoutStrategy.getDescription())) {
                    if ( ! button.isSelected() ) {
                        button.setSelected(true);
                    }
                    break;
                }
            }
        }

        private void addLayoutRadioButtons(JComponent layoutPanel, ButtonGroup buttonGroup, CompositeViewLayoutStrategy... layouts) {
            for ( CompositeViewLayoutStrategy layout : layouts ) {
                JRadioButton button = new JRadioButton(layout.getDescription());
                addLayoutListener(button, layout);
                layoutPanel.add(button);
                buttonGroup.add(button);
                layoutRadioButtons.add(button);
            }
        }

        private void addLayoutListener(JRadioButton leftRightButton, final CompositeViewLayoutStrategy compositeViewLayoutStrategy) {
            leftRightButton.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            setLayoutStrategy(compositeViewLayoutStrategy);
                        }
                    }
            );
        }

        public void viewAdded(ViewContainerEvent viewEvent) {
            ViewCheckBox viewCheckBox = new ViewCheckBox(viewEvent.getViewAffected());
            viewCheckBoxes.add(viewCheckBox);
            childPanel.add(viewCheckBox);
        }

        public void viewRemoved(ViewContainerEvent viewEvent) {
            Iterator<ViewCheckBox> checkBoxes = viewCheckBoxes.iterator();
            while(checkBoxes.hasNext()) {
                ViewCheckBox viewCheckBox = checkBoxes.next();
                if (viewCheckBox.getChildView() == viewEvent.getViewAffected()) {
                    checkBoxes.remove();
                    remove(viewCheckBox);
                }
            }
        }

        private class ViewCheckBox extends JCheckBox{

            private View childView;

            public ViewCheckBox(final View childView) {
                super(childView.getName());
                this.childView = childView;
                setSelected(childView.isDisplayable());

                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        childView.setDisplayable(isSelected());
                        disposeAndLayout(layoutStrategy);
                    }
                });
            }

            public View getChildView() {
                return childView;
            }

        }
    }

}
