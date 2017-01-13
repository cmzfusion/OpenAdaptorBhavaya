package org.bhavaya.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalIconFactory;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import java.awt.*;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class LowVisionMetalTheme extends DefaultMetalTheme {
    private static final String name = "Low Vision";

    private ColorUIResource lighterOrange = new ColorUIResource(new Color(255, 222, 0));
    private ColorUIResource lightOrange = new ColorUIResource(new Color(255, 242, 143));
    private ColorUIResource orange = new ColorUIResource(Color.orange);
    private ColorUIResource darkOrange = new ColorUIResource(new Color(255, 126, 0));
    private ColorUIResource darkerOrange = new ColorUIResource(new Color(156, 61, 0));
    private ColorUIResource veryDarkOrange = new ColorUIResource(new Color(84, 42, 0));

    private final Border orangeBorder = new BorderUIResource(new LineBorder(darkOrange, 2));
    private final Border darkOrangeBorder = new BorderUIResource(new LineBorder(darkerOrange, 2));
    private final Object textBorder = new BorderUIResource(new CompoundBorder(orangeBorder, new BasicBorders.MarginBorder()));

    private final int internalFrameIconSize = 30;

    private final Integer scrollBarWidth = new Integer(25);

    private final FontUIResource controlFont = new FontUIResource("Dialog", Font.BOLD, 18);
    private final FontUIResource systemFont = new FontUIResource("Dialog", Font.PLAIN, 18);
    private final FontUIResource windowTitleFont = new FontUIResource("Dialog", Font.BOLD, 18);
    private final FontUIResource userFont = new FontUIResource("SansSerif", Font.PLAIN, 18);
    private final FontUIResource smallFont = new FontUIResource("Dialog", Font.PLAIN, 14);


    public String getName() {
        return name;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Desktop stuff

    public ColorUIResource getDesktopColor() {
        return getBlack();
    }

    public ColorUIResource getWindowTitleBackground() {
        return darkOrange;
    }

    public ColorUIResource getWindowTitleForeground() {
        return getBlack();
    }

    public ColorUIResource getWindowTitleInactiveBackground() {
        return veryDarkOrange;
    }

    public ColorUIResource getWindowTitleInactiveForeground() {
        return orange;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Text colours

    public ColorUIResource getSystemTextColor() {
        return darkOrange;
    }

    public ColorUIResource getControlTextColor() {
        return lighterOrange;
    }

    public ColorUIResource getInactiveControlTextColor() {
        return super.getInactiveControlTextColor();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public ColorUIResource getInactiveSystemTextColor() {
        return super.getInactiveSystemTextColor();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public ColorUIResource getUserTextColor() {
        return getWhite();
    }

    public ColorUIResource getTextHighlightColor() {
        return orange;
    }

    public ColorUIResource getHighlightedTextColor() {
        return getBlack();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Menu specifics

    public ColorUIResource getMenuBackground() {
        return getBlack();
    }

    public ColorUIResource getMenuForeground() {
        return lightOrange;
    }

    public ColorUIResource getMenuSelectedBackground() {
        return orange;
    }

    public ColorUIResource getMenuSelectedForeground() {
        return getBlack();
    }

    public ColorUIResource getMenuDisabledForeground() {
        return darkerOrange;
    }

    public ColorUIResource getSeparatorBackground() {
        return darkOrange;
    }

    public ColorUIResource getSeparatorForeground() {
        return lightOrange;
    }

    public ColorUIResource getAcceleratorForeground() {
        return getWhite();
    }

    public ColorUIResource getAcceleratorSelectedForeground() {
        return getBlack();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Primary Control specifics

    public ColorUIResource getPrimaryControl() {
        return orange;
    }

    public ColorUIResource getPrimaryControlShadow() {
        return darkOrange;
    }

    public ColorUIResource getPrimaryControlDarkShadow() {
        return darkerOrange;
    }

    public ColorUIResource getPrimaryControlInfo() {
        return getBlack();
    }

    public ColorUIResource getPrimaryControlHighlight() {
        return orange;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Secondary Control specifics

    public ColorUIResource getControl() {
        return getBlack();
    }

    public ColorUIResource getControlShadow() {
        return darkerOrange;
    }

    public ColorUIResource getControlDarkShadow() {
        return darkOrange;
    }

    public ColorUIResource getControlInfo() {
        return darkOrange;
    }

    public ColorUIResource getControlHighlight() {
        return orange;
    }

    public ColorUIResource getControlDisabled() {
        return veryDarkOrange;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Others

    public ColorUIResource getFocusColor() {
        return darkOrange;
    }

    public ColorUIResource getWindowBackground() {
        return getBlack();
    }

    protected ColorUIResource getPrimary1() {
        return darkerOrange;
    }

    protected ColorUIResource getPrimary2() {
        return orange;
    }

    protected ColorUIResource getPrimary3() {
        return darkOrange;
    }

    protected ColorUIResource getSecondary1() {
        return veryDarkOrange;
    }

    protected ColorUIResource getSecondary2() {
        return darkOrange;
    }

    protected ColorUIResource getSecondary3() {
        return getBlack();
    }

    public void addCustomEntriesToTable(UIDefaults aTable) {
        super.addCustomEntriesToTable(aTable);

        aTable.put("ToolTip.border", orangeBorder);
        aTable.put("TitledBorder.border", darkOrangeBorder);
        aTable.put("ScrollPane.border", orangeBorder);
        aTable.put("InternalFrame.border", orangeBorder);
        aTable.put("InternalFrame.paletteBorder", orangeBorder);
        aTable.put("InternalFrame.optionDialogBorder", orangeBorder);

        aTable.put("TextField.border", textBorder);
        aTable.put("PasswordField.border", textBorder);
        aTable.put("TextArea.border", textBorder);
        aTable.put("TextPane.border", textBorder);
        aTable.put("EditorPane.border", textBorder);

        aTable.put("InternalFrame.closeIcon",
                MetalIconFactory.getInternalFrameCloseIcon(internalFrameIconSize));
        aTable.put("InternalFrame.maximizeIcon",
                MetalIconFactory.getInternalFrameMaximizeIcon(internalFrameIconSize));
        aTable.put("InternalFrame.iconifyIcon",
                MetalIconFactory.getInternalFrameMinimizeIcon(internalFrameIconSize));
        aTable.put("InternalFrame.minimizeIcon",
                MetalIconFactory.getInternalFrameAltMaximizeIcon(internalFrameIconSize));

        aTable.put("ScrollBar.width", scrollBarWidth);
    }

    //fonts are larger than defaults
    public FontUIResource getControlTextFont() {
        return controlFont;
    }

    public FontUIResource getSystemTextFont() {
        return systemFont;
    }

    public FontUIResource getUserTextFont() {
        return userFont;
    }

    public FontUIResource getMenuTextFont() {
        return controlFont;
    }

    public FontUIResource getWindowTitleFont() {
        return windowTitleFont;
    }

    public FontUIResource getSubTextFont() {
        return smallFont;
    }

    /**
     * This override is provided such that Theme objects can
     * be directly passed to JComboBox, instead of Strings. (This would
     * not be necessary if getName had been named toString instead).
     */
    public final String toString() {
        return getName();
    }


    public static void main(String[] args) {
        LowVisionMetalTheme lowVisionTheme = new LowVisionMetalTheme();
        update(lowVisionTheme);
//        SwingSet2.main(new String[0]);
    }

    static void update(MetalTheme aTheme) {
        MetalLookAndFeel.setCurrentTheme(aTheme);
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException ex) {
            System.out.println("Cannot set new Theme for Java Look and Feel.");
        }
    }
}



