package org.bhavaya.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class InterpolatingLayoutManager implements LayoutManager2 {
    private static final int FRAME_RATE = 25;
    private static final boolean USE_EQUATIONS_OF_MOTION = true;

    private static class Bounds {
        private Point location;
        private Dimension size;

        public Bounds(Point location, Dimension size) {
            this.location = location;
            this.size = size;
        }

        public Point getLocation() {
            return location;
        }

        public Dimension getSize() {
            return size;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Bounds)) return false;

            final Bounds bounds = (Bounds) o;

            if (location != null ? !location.equals(bounds.location) : bounds.location != null) return false;
            if (size != null ? !size.equals(bounds.size) : bounds.size != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (location != null ? location.hashCode() : 0);
            result = 29 * result + (size != null ? size.hashCode() : 0);
            return result;
        }
    }

    private long travelTime;
    private LayoutManager targetLayoutManager;
    private boolean animationEnabled = false;

    private transient Map sourceBoundsMap = new HashMap();
    private transient HashMap targetBoundsMap = new HashMap();

    private transient Container animationParent;
    private transient long animationsStartTime;
    private transient Timer timer;
    private transient boolean animating;


    public InterpolatingLayoutManager(LayoutManager targetLayoutManager, long travelTime) {
        this.targetLayoutManager = targetLayoutManager;
        this.travelTime = travelTime;
    }

    public void targetLayoutManagerUpdated() {

    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    public void setAnimationEnabled(boolean animationEnabled) {
        this.animationEnabled = animationEnabled;
    }

    public void addLayoutComponent(String name, Component comp) {
        targetLayoutManager.addLayoutComponent(name, comp);
    }

    public void layoutContainer(Container parent) {
        if (animationEnabled) {
            Map currentComponentBoundsMap = new HashMap(sourceBoundsMap.size());
            Map currentTargetBoundsMap = (Map) targetBoundsMap.clone();

            updateBoundsMap(currentComponentBoundsMap, parent);
            targetLayoutManager.layoutContainer(parent);
            updateBoundsMap(targetBoundsMap, parent);

            // Check to see if the targets are the same.  If yes, then ignore
            if (!currentTargetBoundsMap.equals(targetBoundsMap)) {
                sourceBoundsMap = currentComponentBoundsMap;
                animationsStartTime = System.currentTimeMillis();

                if (!animating) {
                    if (parent.isVisible()) startAnimating(parent);
                } else {
                    // Ignore, the timer will take care of this.
                }
            }

            // Undo the previous layout which only did to ascertain targetBoundsMap
            applyBoundsMap(currentComponentBoundsMap);
        } else {
            targetLayoutManager.layoutContainer(parent);
        }
    }

    public Point getTargetLocationForComponent(Component component) {
        Map currentLayoutBoundsMap = new HashMap(targetBoundsMap.size());
        updateBoundsMap(currentLayoutBoundsMap, component.getParent());
        targetLayoutManager.layoutContainer(component.getParent());
        Point result = component.getLocation();
        applyBoundsMap(currentLayoutBoundsMap);
        return result;
    }

    private void startAnimating(Container parent) {
        animating = true;
        animationParent = parent;

        getTimer().start();
    }

    private void animationComplete() {
        getTimer().stop();

        targetLayoutManager.layoutContainer(animationParent);
        animationParent = null;
        animating = false;
    }

    private Timer getTimer() {
        if (timer == null) {
            timer = new Timer(1000 / FRAME_RATE, new AnimationAction());
            timer.setCoalesce(true);
        }
        return timer;
    }

    private static void applyBoundsMap(Map boundsMap) {
        Set componentKeySet = boundsMap.keySet();
        for (Iterator iterator = componentKeySet.iterator(); iterator.hasNext();) {
            Component component = (Component) iterator.next();
            Bounds bounds = (Bounds) boundsMap.get(component);
            component.setLocation(bounds.getLocation());
            component.setSize(bounds.getSize());
        }
    }

    private void interpolateLayout(double completionRatio) {
        Set componetKeySet = targetBoundsMap.keySet();
        for (Iterator iterator = componetKeySet.iterator(); iterator.hasNext();) {
            Component component = (Component) iterator.next();
            Bounds sourceBounds = (Bounds) sourceBoundsMap.get(component);
            Bounds targetBounds = (Bounds) targetBoundsMap.get(component);

            Bounds interpolatedBounds = interpolateBounds(sourceBounds, targetBounds, completionRatio);
            component.setLocation(interpolatedBounds.getLocation());
            component.setSize(interpolatedBounds.getSize());
        }
    }

    private Bounds interpolateBounds(Bounds sourceBounds, Bounds targetBounds, double completionRatio) {
        if (sourceBounds == null) sourceBounds = targetBounds;

        double sourceX = sourceBounds.getLocation().x;
        double sourceY = sourceBounds.getLocation().y;
        double sourceWidth = sourceBounds.getSize().width;
        double sourceHeight = sourceBounds.getSize().height;

        double targetX = targetBounds.getLocation().x;
        double targetY = targetBounds.getLocation().y;
        double targetWidth = targetBounds.getSize().width;
        double targetHeight = targetBounds.getSize().height;

        int interpolatedX;
        int interpolatedY;
        int interpolatedWidth;
        int interpolatedHeight;
        if (USE_EQUATIONS_OF_MOTION) {
            interpolatedX = (int) interpolateUsingEquationsOfMotion(sourceX, targetX, completionRatio);
            interpolatedY = (int) interpolateUsingEquationsOfMotion(sourceY, targetY, completionRatio);
            interpolatedWidth = (int) interpolateUsingEquationsOfMotion(sourceWidth, targetWidth, completionRatio);
            interpolatedHeight = (int) interpolateUsingEquationsOfMotion(sourceHeight, targetHeight, completionRatio);
        } else {
            interpolatedX = (int) interpolateUsingLinearEquations(sourceX, targetX, completionRatio);
            interpolatedY = (int) interpolateUsingLinearEquations(sourceY, targetY, completionRatio);
            interpolatedWidth = (int) interpolateUsingLinearEquations(sourceWidth, targetWidth, completionRatio);
            interpolatedHeight = (int) interpolateUsingLinearEquations(sourceHeight, targetHeight, completionRatio);
        }

        return new Bounds(new Point(interpolatedX, interpolatedY), new Dimension(interpolatedWidth, interpolatedHeight));
    }

    /**
     * Using the equations of motions makes the movement look more natural.
     */
    private static double interpolateUsingEquationsOfMotion(double x0, double x1, double completionRatio) {
        /*
           The basic algorithn is based on the standard equations of motion.  For the first half of the animated
           duration, the component will be accelerating, while for the second half it will be decellerating.  Time
           has been normalised so that the animation starts at t=0 and ends at t-1.  Therefore.

           For the acceleration phase:
               x(t) = x(0) + (at^2)/2

           For the decelleration phase:
               x(t) = x(0) + (x(1) - x(0) - (a(1-t)^2)/2)

           Where:
                  a = 2s/t^2
                    = 2(x(1) - x(0))/0.5^2    (t == 0.5 because accel is split between accel and decell)
        */
        double acceleration = 2d * ((x1 - x0) / 2) / Math.pow(1d / 2d, 2d);
        if (completionRatio < 0.5) {
            return x0 + 0.5 * acceleration * Math.pow(completionRatio, 2);
        } else {
            return x0 + ((x1 - x0) - 0.5 * acceleration * Math.pow(1 - completionRatio, 2));
        }
    }

    private static double interpolateUsingLinearEquations(double x0, double x1, double completionRatio) {
        return x0 + (x1 - x0) * completionRatio;
    }

    private static void updateBoundsMap(Map map, Container parent) {
        map.clear();
        Component[] components = parent.getComponents();
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            Bounds bounds = new Bounds(component.getLocation(), component.getSize());
            map.put(component, bounds);
        }
    }

    public Dimension minimumLayoutSize(Container parent) {
        return targetLayoutManager.minimumLayoutSize(parent);
    }

    public Dimension preferredLayoutSize(Container parent) {
        return targetLayoutManager.preferredLayoutSize(parent);
    }

    public void removeLayoutComponent(Component comp) {
        targetLayoutManager.removeLayoutComponent(comp);
    }

    public void addLayoutComponent(Component comp, Object constraints) {
        if (targetLayoutManager instanceof LayoutManager2) {
            ((LayoutManager2) targetLayoutManager).addLayoutComponent(comp, constraints);
        }
    }

    public float getLayoutAlignmentX(Container target) {
        return 0;
    }

    public float getLayoutAlignmentY(Container target) {
        return 0;     // Do we need?
    }

    public void invalidateLayout(Container target) {
        // Do we need?
    }

    public Dimension maximumLayoutSize(Container target) {
        if (targetLayoutManager instanceof LayoutManager2) {
            return ((LayoutManager2) targetLayoutManager).maximumLayoutSize(target);
        } else {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
    }

    private class AnimationAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            long duration = System.currentTimeMillis() - animationsStartTime;
            if (duration > travelTime) {
                animationComplete();
            } else {
                double completionRatio = (double) duration / (double) travelTime;
                interpolateLayout(completionRatio);
            }
        }
    }
}
