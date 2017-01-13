package org.bhavaya.ui.table;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 04-Jun-2009
 * Time: 11:01:38
 *
 * A performance debugging utility to allow us to turn cell highlighters on or off at runtime
 * This can be useful to detect any adverse effects a particular highligher may be having on performance
 */
public class HighlighterChainEditor extends JDialog {

    private Box checkboxContainer = Box.createVerticalBox();
    private java.util.List<HighligherControlCheckbox> checkboxes = new ArrayList<HighligherControlCheckbox>();
    private JTable tableComponent;

    public HighlighterChainEditor(JTable tableComponent, HighlightedTable.CellHighlighter rootHighlighter) {
        this.tableComponent = tableComponent;
        setModal(false);
        setAlwaysOnTop(true);

        createCheckboxes(rootHighlighter);
        JComponent toggleAllButton = createToggleAllButton();

        JScrollPane scrollPane = new JScrollPane(checkboxContainer);
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(toggleAllButton, BorderLayout.SOUTH);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(300, 600);
        setLocationRelativeTo(tableComponent);
    }

    private JComponent createToggleAllButton() {
        JButton toggleAllRenderersButton = new JButton("Toggle All Renderers");
        toggleAllRenderersButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( ! checkboxes.isEmpty()) {
                    boolean newState = ! checkboxes.get(0).isSelected();
                    for ( HighligherControlCheckbox c : checkboxes) {
                        c.setSelected(newState);
                    }
                }
            }
        });
        return toggleAllRenderersButton;
    }

    private void createCheckboxes(HighlightedTable.CellHighlighter highlighter) {

        HighligherControlCheckbox checkbox;
        if ( highlighter instanceof AnalyticsTable.MultiplexingCellHighlighter) {
            checkbox = new HighligherControlCheckbox(((AnalyticsTable.MultiplexingCellHighlighter) highlighter).getMultiplexedHighlighter());
        } else {
            checkbox = new HighligherControlCheckbox(highlighter);
        }

        checkboxes.add(checkbox);
        checkboxContainer.add(checkbox);
        checkboxContainer.add(Box.createVerticalStrut(3));

        if ( highlighter.getUnderlyingHighlighter() != null ) {
            createCheckboxes(highlighter.getUnderlyingHighlighter());
        }
    }

    private class HighligherControlCheckbox extends JCheckBox {

        private HighlightedTable.CellHighlighter highlighter;

        public HighligherControlCheckbox(final HighlightedTable.CellHighlighter highlighter) {
            super(highlighter.getClass().getName(), highlighter.isEnabled());
            this.highlighter = highlighter;

            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    highlighter.setEnabled(isSelected());
                    tableComponent.repaint();
                }
            });
        }

        public void setSelected(boolean enabled) {
            highlighter.setEnabled(enabled);
            super.setSelected(enabled);
            tableComponent.repaint();
        }
    }
}
