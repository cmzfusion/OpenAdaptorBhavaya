package org.bhavaya.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;
import java.awt.*;

/**
 * Description
 * <p/>
 * Original code comes from http://www.codeguru.com/java/articles/122.shtml thanks to Zafir Anjum
 * </p>
 *
 * @author Zafir Anjum
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class MultiLineToolTipUI extends BasicToolTipUI {

    private static final MultiLineToolTipUI sharedInstance = new MultiLineToolTipUI();
    private static final Border border = BorderFactory.createEmptyBorder(1, 3, 1, 3);
    private static final JTextArea textArea = new JTextArea();

    protected CellRendererPane rendererPane;

    public static ComponentUI createUI(JComponent c) {
        return sharedInstance;
    }

    private MultiLineToolTipUI() {
        super();
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        rendererPane = new CellRendererPane();
        c.add(rendererPane);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        c.remove(rendererPane);
        rendererPane = null;
    }

    public void paint(Graphics g, JComponent c) {
        Dimension size = c.getSize();
        textArea.setBackground(c.getBackground());
        rendererPane.paintComponent(g, textArea, c, 1, 1, size.width - 1, size.height - 1, true);
    }

    public Dimension getPreferredSize(JComponent c) {
        String tipText = ((JToolTip) c).getTipText();
        if (tipText == null) return new Dimension(0, 0);
        textArea.setText(tipText);
        textArea.setFont(c.getFont());
        textArea.setBorder(border);
        textArea.setWrapStyleWord(true);
        int width = ((MultiLineToolTip) c).getFixedWidth();
        int columns = ((MultiLineToolTip) c).getColumns();

        if (columns > 0) {
            textArea.setColumns(columns);
            textArea.setSize(0, 0);
            textArea.setLineWrap(true);
            textArea.setSize(textArea.getPreferredSize());
        } else if (width > 0) {
            textArea.setLineWrap(true);
            Dimension d = textArea.getPreferredSize();
            d.width = width;
            d.height++;
            textArea.setSize(d);
        } else {
            textArea.setLineWrap(false);
        }

        Dimension dim = textArea.getPreferredSize();

        dim.height += 1;
        dim.width += 1;
        return dim;
    }

    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }
}
