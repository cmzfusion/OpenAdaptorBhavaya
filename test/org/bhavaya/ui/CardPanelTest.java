package org.bhavaya.ui;

import junit.framework.TestCase;
import org.bhavaya.ui.CardPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;


public class CardPanelTest extends TestCase {
    private interface ComponentClosure {
        public Component component();
    }

    private CardPanel cardPanel;
    private ComponentClosure[] componentClosures;

    {
        ArrayList closureList = new ArrayList();
        closureList.add(new ComponentClosure() {
            public Component component() {
                JTextArea textArea = new JTextArea("Text area");
                JScrollPane scrollPane = new JScrollPane(textArea);
                return scrollPane;
            }
        });
        closureList.add(new ComponentClosure() {
            public Component component() {
                JTable table = new JTable();
                JScrollPane scrollPane = new JScrollPane(table);
                return scrollPane;
            }
        });
        closureList.add(new ComponentClosure() {
            public Component component() {
                JList list = new JList(new Object[]{"one", "two", "three"});
                JScrollPane scrollPane = new JScrollPane(list);
                return scrollPane;
            }
        });
        closureList.add(new ComponentClosure() {
            public Component component() {
                return new JButton("Button");
            }
        });
        componentClosures = (ComponentClosure[]) closureList.toArray(new ComponentClosure[closureList.size()]);
    }

    public CardPanelTest(String s) {
        super(s);
    }

    protected void setUp() throws Exception {
        cardPanel = new CardPanel();
    }

    public void testAdd() {
        Component[] components = createRandomComponents(6);
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            cardPanel.addComponent(component);
            assertEquals("Selected index should not move.", 0, cardPanel.getSelectedIndex());
            assertEquals("Component count wrong", i + 1, cardPanel.getComponentCount());
        }
        assertOnlySelectedVisible(components);
    }

    public void testInsert() {
        Component[] components = createRandomComponents(6);
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            cardPanel.insertComponent(component, 0);
            assertEquals("Selected index should not move.", i, cardPanel.getSelectedIndex());
            assertEquals("Component count wrong", i + 1, cardPanel.getComponentCount());
        }
        assertOnlySelectedVisible(components);
    }

    public void testRemoveAt() {
        testAdd();
        Component[] components = cardPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            int deletionIndex = (int) (Math.random() * (components.length - i));
            cardPanel.removeComponentAt(deletionIndex);
            assertEquals("Card panel reporting incorrect size", components.length - 1 - i, cardPanel.getComponentCount());
            if (i != (components.length - 1)) {
                assertOnlySelectedVisible(cardPanel.getComponents());
                assertTrue("Selected index must not be null", cardPanel.getSelectedComponent() != null);
            }
        }
    }

    public void testSelectionTracking() {
        testAdd();
        cardPanel.setSelectedIndex(3);
        cardPanel.insertComponent(new JButton("Button"), 3);
        assertEquals("Selected index should increment when component inserted before it", 4, cardPanel.getSelectedIndex());
        assertOnlySelectedVisible(cardPanel.getComponents());

        cardPanel.removeComponentAt(3);
        assertEquals("Selected index should decrement when component removed before it", 3, cardPanel.getSelectedIndex());
        assertOnlySelectedVisible(cardPanel.getComponents());

        cardPanel.removeComponentAt(3);
        assertEquals("Selected index should move to next component when selected component removed", 3, cardPanel.getSelectedIndex());
        assertOnlySelectedVisible(cardPanel.getComponents());

        cardPanel.setSelectedIndex(cardPanel.getComponentCount() - 1);
        cardPanel.removeComponent(cardPanel.getSelectedComponent());
        assertEquals("Selected index should move to previous component when selected is deleted and last", cardPanel.getComponentCount() - 1,
                cardPanel.getSelectedIndex());
        assertOnlySelectedVisible(cardPanel.getComponents());
    }

    private void assertOnlySelectedVisible(Component[] components) {
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            if (component == cardPanel.getSelectedComponent()) {
                assertTrue("Selected component not visible", component.isVisible());
            } else {
                assertTrue("Unselected component should not be visible", !component.isVisible());
            }
        }
    }

    private Component[] createRandomComponents(int componentCount) {
        ArrayList componentList = new ArrayList();
        for (int i = 0; i < componentCount; i++) {
            componentList.add(createRandomComponent());
        }
        return (Component[]) componentList.toArray(new Component[componentList.size()]);
    }

    private Component createRandomComponent() {
        int closureCount = componentClosures.length;
        return componentClosures[((int) (Math.random() * closureCount))].component();
    }
}
