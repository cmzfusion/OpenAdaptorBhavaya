package org.bhavaya.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

public class JFontChooser extends JComponent {

    private JComboBox familyComboBox;
    private JCheckBox italicCheckBox;
    private JCheckBox showAllFontsCheckBox;
    private JCheckBox boldCheckBox;
    private JComboBox sizeComboBox;
    private JTextArea sampleTextArea;
    private java.awt.Font fontValue;
    private String[] simpleFonts = {"Arial", "Arial Narrow", "Courier New", "Dresdner Bank AG", "Times New Roman"};


    public JFontChooser(Font initialFont) {
        initGuiComponents();
        initListeners();
        setFontValue(initialFont);
    }

    public static Font showDialog(Component parent, String title,
                                  Font initialfont) {

        Window windowParent = UIUtilities.getWindowParent(parent);

        final JDialog dialog;
        if (windowParent instanceof Frame) {
            dialog = new JDialog((Frame) windowParent, title, true);
        } else {
            dialog = new JDialog((Dialog) windowParent, title, true);
        }

        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));

        final JFontChooser fontChooser = new JFontChooser(initialfont);
        dialog.getContentPane().add(fontChooser);

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        JPanel buttonsBox = new JPanel();
        buttonsBox.setLayout(new BoxLayout(buttonsBox, BoxLayout.X_AXIS));
        buttonsBox.add(Box.createHorizontalGlue());
        buttonsBox.add(okButton);
        buttonsBox.add(Box.createHorizontalStrut(30));
        buttonsBox.add(cancelButton);
        buttonsBox.add(Box.createHorizontalGlue());
        dialog.getContentPane().add(buttonsBox);
        dialog.setSize(300, 300);
        UIUtilities.centreInContainer(windowParent, dialog, 0, 0);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        });
        dialog.getRootPane().setDefaultButton(okButton);

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                fontChooser.setFontValue(null);
                dialog.dispose();
            }
        });

        dialog.setVisible(true);

        return fontChooser.getFont();
    }

    protected void initGuiComponents() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        familyComboBox = new JComboBox(simpleFonts);
        familyComboBox.setSelectedItem("Arial");
        familyComboBox.setPreferredSize(new Dimension(150, 20));

        sizeComboBox = new JComboBox(new String[]{"6", "8", "10", "12", "14", "16",
                "18", "20", "22", "24", "26"});
        sizeComboBox.setSelectedItem("12");

        showAllFontsCheckBox = new JCheckBox("Show All available fonts", false);

        italicCheckBox = new JCheckBox("Italic", false);
        boldCheckBox = new JCheckBox("Bold", false);

        JPanel fontBox = new JPanel(new GridLayout(2, 1));
        JPanel topFontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topFontPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        topFontPanel.add(familyComboBox);
        topFontPanel.add(sizeComboBox);
        JPanel bottomFontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomFontPanel.add(showAllFontsCheckBox);
        fontBox.add(topFontPanel);
        fontBox.add(bottomFontPanel);
        fontBox.setBorder(BorderFactory.createTitledBorder(" Font "));
        add(fontBox);

        JPanel effectsBox = new JPanel();
        effectsBox.setLayout(new FlowLayout(FlowLayout.LEFT));
        effectsBox.add(italicCheckBox);
        effectsBox.add(boldCheckBox);
        effectsBox.setBorder(BorderFactory.createTitledBorder(" Effects "));
        add(effectsBox);

        sampleTextArea = new JTextArea("Example 1234568790");
        sampleTextArea.setBackground(Color.WHITE);
        sampleTextArea.setEditable(false);
        JPanel samplePanel = new JPanel();
        samplePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        samplePanel.add(sampleTextArea);
        samplePanel.setBorder(BorderFactory.createTitledBorder(" Sample "));
        add(samplePanel);
    }

    protected void initListeners() {
        familyComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateFont();
            }
        });

        sizeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateFont();
            }
        });

        boldCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateFont();
            }
        });

        italicCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateFont();
            }
        });

        showAllFontsCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean selected = showAllFontsCheckBox.isSelected();
                String selectedFont = (String) familyComboBox.getSelectedItem();
                String[] fontsToAdd = null;
                if (selected) {
                    fontsToAdd = GraphicsEnvironment.getLocalGraphicsEnvironment().
                            getAvailableFontFamilyNames();
                } else {
                    fontsToAdd = simpleFonts;
                }
                familyComboBox.removeAllItems();
                familyComboBox.addItem(selectedFont);
                for (String font : fontsToAdd) {
                    familyComboBox.addItem(font);
                }
            }
        });

    }

    protected void updateFont() {
        String selectedFont = (String) familyComboBox.getSelectedItem();

        if (selectedFont != null && selectedFont.length() > 0) {
            Map fontAttrs = new HashMap();
            fontAttrs.put(TextAttribute.FAMILY, selectedFont);
            fontAttrs.put(TextAttribute.SIZE, new Float((String) sizeComboBox.getSelectedItem()));

            if (boldCheckBox.isSelected())
                fontAttrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
            else fontAttrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);

            if (italicCheckBox.isSelected())
                fontAttrs.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
            else fontAttrs.put(TextAttribute.POSTURE, TextAttribute.POSTURE_REGULAR);

            Font newFont = new Font(fontAttrs);
            Font oldFont = fontValue;
            fontValue = newFont;
            sampleTextArea.setFont(newFont);
            String text = sampleTextArea.getText();
            sampleTextArea.setText("");
            sampleTextArea.setText(text);
            sampleTextArea.repaint(100);
            firePropertyChange("fontValue", oldFont, newFont);
        }
    }

    private void setFontValue(Font newFontValue) {
        if (newFontValue != null) {
            boldCheckBox.setSelected(newFontValue.isBold());
            italicCheckBox.setSelected(newFontValue.isItalic());
            familyComboBox.setSelectedItem(newFontValue.getName());
            sizeComboBox.setSelectedItem(Integer.toString(newFontValue.getSize()));
        }
        this.fontValue = newFontValue;
    }

    public Font getFont() {
        return fontValue;
    }
}
