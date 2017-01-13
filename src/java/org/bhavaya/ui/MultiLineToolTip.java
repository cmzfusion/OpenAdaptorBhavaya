package org.bhavaya.ui;

import javax.swing.*;

/**
 * Description
 *
 * <p>
 * Original code comes from http://www.codeguru.com/java/articles/122.shtml thanks to Zafir Anjum
 * </p>
 * @author Zafir Anjum
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class MultiLineToolTip extends JToolTip {

    public MultiLineToolTip() {
        updateUI();
    }

    public void updateUI() {
        setUI(MultiLineToolTipUI.createUI(this));
    }

    public void setColumns(int columns) {
        this.columns = columns;
        this.fixedwidth = 0;
    }

    public int getColumns() {
        return columns;
    }

    public void setFixedWidth(int width) {
        this.fixedwidth = width;
        this.columns = 0;
    }

    public int getFixedWidth() {
        return fixedwidth;
    }

    protected int columns    = 0;
    protected int fixedwidth = 0;

}
