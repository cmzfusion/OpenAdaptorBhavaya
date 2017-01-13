package org.bhavaya.ui.options;

import org.bhavaya.ui.GenericDialog;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.view.Workspace;
import org.bhavaya.util.ValidationException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.7 $
 */
public class OptionsDialog extends GenericDialog {

    private static final String EMPTY_PANEL = "emptyPanel";

    private HashSet categoryNames = new HashSet();

    private JPanel optionsPanel;
    private JList categoriesList;
    private JPanel emptyPanel;
    private CardLayout optionsPanelCardLayout;
    private OptionsPanelController[] optionsPanelControllers;

    public OptionsDialog() throws HeadlessException {
        super();
        setTitle("Options");
        initialize();
    }

    public OptionsDialog(Frame owner) throws HeadlessException {
        this(owner, "Options", false);
    }

    public OptionsDialog(Frame owner, String title, boolean modal) throws HeadlessException {
        super(owner, title, modal);
        initialize();
    }

    private void initialize() {
        optionsPanelControllers = OptionsController.getInstance().getOptionsPanelControllers();

        categoriesList = new JList(optionsPanelControllers);
        categoriesList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    OptionsPanelController controller = (OptionsPanelController) categoriesList.getSelectedValue();
                    showOptionsPanel(controller);
                }
            }
        });
        categoriesList.setCellRenderer(new CategoriesListCellRenderer());

        optionsPanelCardLayout = new CardLayout();
        optionsPanel = new JPanel(optionsPanelCardLayout);
        emptyPanel = new JPanel();
        optionsPanel.add(emptyPanel, EMPTY_PANEL);
        if (optionsPanelControllers.length >=0){
            showOptionsPanel(optionsPanelControllers[0]);
        }
        else{
            optionsPanelCardLayout.show(optionsPanel, EMPTY_PANEL);
        }

        Box buttonPanel = Box.createHorizontalBox();
        buttonPanel.add(Box.createHorizontalGlue());
        JButton okButton = new JButton(new AbstractAction("OK") {
            public void actionPerformed(ActionEvent e) {
                applyOptions();
            }
        });
        okButton.setMnemonic('O');
        buttonPanel.add(okButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        JButton cancelButton = new JButton(new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }

        });
        cancelButton.setMnemonic('C');
        buttonPanel.add(cancelButton);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(new JScrollPane(categoriesList), BorderLayout.WEST);
        content.add(optionsPanel, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(content);
        setSize(820, 640);
        setResizable(true);
    }

    private void applyOptions() {
        for (int i = 0; i < optionsPanelControllers.length; i++) {
            OptionsPanelController optionsPanelController = optionsPanelControllers[i];
            try {
                optionsPanelController.validate();
            } catch (ValidationException ex) {
                showOptionsPanel(optionsPanelController);
                UIUtilities.displayValidationExceptionDialog(ex, "Error!", this);
                return;
            }
        }
        for (int i = 0; i < optionsPanelControllers.length; i++) {
            OptionsPanelController optionsPanelController = optionsPanelControllers[i];
            optionsPanelController.applyChanges();
        }
        Workspace.getInstance().forceUpdate();
        setVisible(false);
    }

    public void reloadOptions() {
        for (int i = 0; i < optionsPanelControllers.length; i++) {
            OptionsPanelController optionsPanelController = optionsPanelControllers[i];
            optionsPanelController.reload();
        }
    }

    private void cancel() {
        for (int i = 0; i < optionsPanelControllers.length; i++) {
            OptionsPanelController optionsPanelController = optionsPanelControllers[i];
            optionsPanelController.cancel();
        }
        setVisible(false);
    }

    private void showOptionsPanel(OptionsPanelController controller) {
        if (controller == null) {
            optionsPanelCardLayout.show(optionsPanel, EMPTY_PANEL);
        } else {
            String categoryName = controller.getCategoryName();
            if (!categoryNames.contains(categoryName)) {
                JComponent component = createOptionsPanel(controller);
                categoryNames.add(categoryName);
                optionsPanel.add(component, categoryName);
            }
            optionsPanelCardLayout.show(optionsPanel, categoryName);
        }
    }

    private JComponent createOptionsPanel(OptionsPanelController controller) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JLabel categoryLabel = new CategoryLabel(controller.getCategoryName(), controller.getIcon(), JLabel.LEFT);
        categoryLabel.setForeground(Color.WHITE);
        categoryLabel.setFont(categoryLabel.getFont().deriveFont(18f));
        categoryLabel.setOpaque(false);
        categoryLabel.setIconTextGap(10);
        categoryLabel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        panel.add(categoryLabel, BorderLayout.NORTH);
        panel.add(controller.getComponent(), BorderLayout.CENTER);
        return panel;
    }

    private static class CategoryLabel extends JLabel {
        private Color color1 = new Color(10, 36, 106);
        private Color color2 = new Color(182, 183, 237);

        public CategoryLabel(String text, Icon icon, int horizontalAlignment) {
            super(text, icon, horizontalAlignment);
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            Dimension size = getSize();

            g2.setPaint(new GradientPaint(0, 0, color1, (float)size.getWidth(),
                    (float)size.getHeight(), color2, false));

            Rectangle r = new Rectangle(0, 0, (int)size.getWidth(), (int)size.getHeight());

            g2.fill(r);
            super.paintComponent(g);
        }
    }

    private static final class CategoriesListCellRenderer extends DefaultListCellRenderer {

        public CategoriesListCellRenderer() {
            setHorizontalAlignment(CENTER);
            setVerticalTextPosition(BOTTOM);
            setHorizontalTextPosition(CENTER);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value != null) {
                OptionsPanelController controller = (OptionsPanelController) value;
                setIcon(controller.getIcon());
            }
            setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

            return this;
        }
    }
}
