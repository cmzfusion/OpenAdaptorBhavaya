package org.bhavaya.ui.options;

import org.bhavaya.ui.DecimalSpinner;
import org.bhavaya.ui.RowLayout;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.VerticalFlowLayout;
import org.bhavaya.ui.adaptor.*;
import org.bhavaya.util.Attribute;
import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Generic;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Description
 *
 * @author Vladimir Hrmo
 */
public class GenericOptionsPanel extends JPanel {
    private Collection<OptionController> optionControllers = new ArrayList<OptionController>();
    private GenericOptionsPanelController controller;

    public GenericOptionsPanel(GenericOptionsPanelController controller) {
        this.controller = controller;
        setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true));
        JComponent mainPanel;
        String[] tabs = controller.getOptionTabNames();
        if (tabs.length > 1) {
            mainPanel = new JTabbedPane();
            for (String tab : tabs) {
                mainPanel.add(tab, createTab(tab));
            }
        } else {
            // dont create a tab as there is only one
            mainPanel = new JPanel();
            mainPanel.add(createTab(tabs[0]));
        }

        for (OptionController optionController : optionControllers) {
            optionController.init();
        }
        add(mainPanel);
    }

    private JPanel createTab(String tabName) {
        RowLayout layout = new RowLayout(500, 5, true);
        JPanel tabbedPanel = new JPanel(layout);
        String[] optionGroupNames = controller.getOptionGroupNames(tabName);
        for (String optionGroupName : optionGroupNames) {
            Option[] options = controller.getOptions(tabName, optionGroupName);
            JPanel panel = createPanel(options);
            panel.setBorder(new CompoundBorder(new TitledBorder(optionGroupName), new EmptyBorder(1, 5, 5, 5)));

            RowLayout.Row row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.LEFT, false);
            tabbedPanel.add(row.addComponent(panel));
            layout.addRow(row);
        }
        return tabbedPanel;
    }

    private JPanel createPanel(Option[] options) {
        RowLayout layout = new RowLayout(500, 5, true);
        JPanel panel = new JPanel(layout);
        for (Option option : options) {
            OptionController optionController = createOptionController(option);
            optionControllers.add(optionController);

            RowLayout.Row row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.LEFT, false);
            panel.add(row.addComponent(optionController.getComponent()));
            layout.addRow(row);
        }
        return panel;
    }

    private OptionController createOptionController(Option option) {
        OptionController optionController = controller.getOptionController(option);
        if (optionController == null) {
            if (option.getBeanCollection() != null) {
                optionController = new BeanCollectionOptionController(option);
            } else {
                Attribute attribute = Generic.getAttribute(option.getTarget(), Generic.beanPathStringToArray(option.getPropertyBeanPath()));
                Class optionType = ClassUtilities.typeToClass(attribute.getType());

                // TODO Copied from Vlad, but not sure whether we should be applying transform first?
                if (Number.class.isAssignableFrom(optionType)) {
                    optionController = new NumericOptionController(option);
                } else if (Boolean.class.isAssignableFrom(optionType)) {
                    optionController = new BooleanOptionController(option);
                }
            }
        }
        return optionController;
    }

    public static abstract class OptionController {
        private Option option;
        private Pipe guiToBeanPipe;
        private Pipe beanToGuiPipe;

        public OptionController(Option option) {
            this.option = option;
        }

        public Option getOption() {
            return option;
        }

        public void init() {
            beanToGuiPipe = new AWTPropertyPipe(option.getTarget(), option.getPropertyBeanPath(), getTarget(), getGuiPropertyPath(), option.getGetTransform());
            beanToGuiPipe.execute();

            PropertySource source = new PropertySource(getTarget(), getGuiPropertyPath());
            guiToBeanPipe = new PropertyPipe(source, new PropertySink(option.getTarget(), option.getPropertyBeanPath()), option.getSetTransform(), false);
            guiToBeanPipe.prime(source.getData());
        }

        protected abstract Object getTarget();

        protected abstract String getGuiPropertyPath();

        public void guiToBean() {
            guiToBeanPipe.execute();
        }

        void beanToGui() {
            beanToGuiPipe.execute();
        }

        public abstract Component getComponent();
    }


    private static class BeanCollectionOptionController extends OptionController {
        private JComboBox comboBox;
        private Component labelledComponent;

        BeanCollectionOptionController(Option option) {
            super(option);
            this.comboBox = new JComboBox(new DefaultComboBoxModel(option.getBeanCollection()));
            this.comboBox.setToolTipText(option.getOptionDescription());
            this.labelledComponent = UIUtilities.createLabelledComponent(option.getOptionLabel(), this.comboBox);
        }

        public Object getTarget() {
            return comboBox;
        }

        public Component getComponent() {
            return labelledComponent;
        }

        public String getGuiPropertyPath() {
            return "selectedItem";
        }
    }

    private static class BooleanOptionController extends OptionController {
        private JCheckBox checkbox;

        public BooleanOptionController(Option option) {
            super(option);
            this.checkbox = new JCheckBox(option.getOptionLabel());
            this.checkbox.setToolTipText(option.getOptionDescription());
        }

        public Component getTarget() {
            return checkbox;
        }

        public Component getComponent() {
            return checkbox;
        }

        public String getGuiPropertyPath() {
            return "selected";
        }
    }

    private static class NumericOptionController extends OptionController {
        private DecimalSpinner decimalSpinner;
        private JComponent labelledComponent;

        public NumericOptionController(Option option) {
            super(option);
            this.decimalSpinner = new DecimalSpinner();
            this.decimalSpinner.setToolTipText(option.getOptionDescription());
            this.labelledComponent = UIUtilities.createLabelledComponent(option.getOptionLabel(), decimalSpinner);
            UIUtilities.formatSpinner(decimalSpinner, "###,##0.######");
        }

        public Component getTarget() {
            return decimalSpinner;
        }

        public Component getComponent() {
            return labelledComponent;
        }

        public String getGuiPropertyPath() {
            return "value";
        }
    }

    public void reload() {
        for (OptionController optionController : optionControllers) {
            optionController.beanToGui();
        }
    }

    public void applyChanges() {
        for (OptionController optionController : optionControllers) {
            optionController.guiToBean();
        }
    }
}
