package org.bhavaya.ui.view;

import javax.swing.*;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ga2adaz
 * Date: 23/07/12
 * Time: 11:04
 * To change this template use File | Settings | File Templates.
 */
public class ViewUtils {

    public static boolean isViewOpen(long viewId) {
        Map<Long,View> viewsMap = getViews();
        return viewsMap.containsKey(viewId);
    }

    /**
     * Returns the id of the with the given tab title. If it cannot find the view it will return null
     * @param tabTitle
     * @return
     */
    public static Long getViewId(String tabTitle) {
        Map<Long,View> viewsMap = getViews();
        for(View view : viewsMap.values()) {
            if(view.getTabTitle().equals(tabTitle)) {
                return view.getViewId();
            }
        }
        return null;
    }

    public static View getView(long viewId) {
        Map<Long,View> viewsMap = getViews();
        return viewsMap.get(viewId);
    }

    public static Map<Long,View> getViews() {
        return getViewsMap();
    }

    private static Map<Long,View> getViewsMap() {
        Collection<View> views = Workspace.getInstance().getViews();

        Map<Long,View> selectableViews = new HashMap<Long,View> ();
        Set<View> viewSet = new HashSet<View>();
        for(View view : views) {
            viewSet.add(view);
            selectableViews.put(view.getViewId(), view);
        }
        return selectableViews;
    }

    /**
     * Looks high and low for the named view and then frisks that view gently but firmly for the action
     * by type. It is returned as a WeakReference so as we do not want exposed actions to prevent view objects from being
     * gc'd. If it can't find the action even after subjecting the view to torture it will give up and return WeakReference
     * with a null referent
     *
     * @param originatingViewId
     * @return
     */
    public static WeakReference<Action> getActionFromView(String actionClassName, Long originatingViewId) {
        WeakReference<Action> action = new WeakReference<Action>(null);
        View view = ViewUtils.getView(originatingViewId);
        if (view != null) {
            List<WeakReference<Action>> actions = view.getExportedActions();
            for (WeakReference<Action> actionWeakReference : actions) {
                String exportedClassname = actionWeakReference.get().getClass().getName();
                String referencedClassname = actionClassName;
                if (exportedClassname.equals(referencedClassname)) {
                    action = actionWeakReference;
                }
            }
        }
        return action;
    }


}
