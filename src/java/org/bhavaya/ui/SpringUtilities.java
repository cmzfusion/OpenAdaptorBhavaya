package org.bhavaya.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Professor Milne deigned to make all his classes package protected so these have been ripped out.  Plus a few others
 * added.
 */
public class SpringUtilities {
    public static abstract class AbstractSpring extends Spring {
        protected int size = UNSET;

        public int getValue() {
            return size != UNSET ? size : getPreferredValue();
        }

        public void setValue(int size) {
            if (size == UNSET) {
                clear();
                return;
            }
            this.size = size;
        }

        protected void clear() {
            size = UNSET;
        }
    }

    public static class SpringProxy extends Spring {
        private String edgeName;
        private Component c;
        private SpringLayout l;

        public SpringProxy(String edgeName, Component c, SpringLayout l) {
            this.edgeName = edgeName;
            this.c = c;
            this.l = l;
        }

        private Spring getConstraint() {
            return l.getConstraints(c).getConstraint(edgeName);
        }

        public int getMinimumValue() {
            return getConstraint().getMinimumValue();
        }

        public int getPreferredValue() {
            return getConstraint().getPreferredValue();
        }

        public int getMaximumValue() {
            return getConstraint().getMaximumValue();
        }

        public int getValue() {
            return getConstraint().getValue();
        }

        public void setValue(int size) {
            getConstraint().setValue(size);
        }

        public String toString() {
            return "SpringProxy for " + edgeName + " edge of " + c.getName() + ".";
        }
    }

    public static class HeightSpring extends AbstractSpring {
        private Component c;

        public HeightSpring(Component c) {
            this.c = c;
        }

        public int getMinimumValue() {
            return c.getMinimumSize().height;
        }

        public int getPreferredValue() {
            return c.getPreferredSize().height;
        }

        public int getMaximumValue() {
            return Math.min(Short.MAX_VALUE, c.getMaximumSize().height);
        }
    }

     public static class WidthSpring extends AbstractSpring {
        private Component c;

        public WidthSpring(Component c) {
            this.c = c;
        }

        public int getMinimumValue() {
            return c.getMinimumSize().width;
        }

        public int getPreferredValue() {
            return c.getPreferredSize().width;
        }

        public int getMaximumValue() {
            // We will be doing arithmetic with the results of this call,
            // so if a returned value is Integer.MAX_VALUE we will get
            // arithmetic overflow. Truncate such values.
            return Math.min(Short.MAX_VALUE, c.getMaximumSize().width);
        }
    }
}
