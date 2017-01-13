package org.bhavaya.ui.table;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author Daniel van Enckevort
 * @version $Revision: 1.2 $
 */
public class StringRendererFactory extends TableCellRendererFactory {
    private static final String LEFT = "Left";
    private static final String CENTRE = "Centre";
    private static final String RIGHT = "Right";

    private String ALIGNMENT = "Alignment";
    private String[] parameterNames = new String[]{ALIGNMENT};
    private String[] alignmentOptions = new String[]{LEFT, CENTRE, RIGHT};

    public StringRendererFactory() {
    }

    public String[] getParameterNames() {
        return parameterNames;
    }

    public String[] getAllowedValuesForParameter(String parameterName) {
        if (ALIGNMENT.equals(parameterName)) return alignmentOptions;
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDefaultRendererId() {
        return LEFT;
    }

    public TableCellRenderer getInstance(Map parameterMap) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        String alignment = (String) parameterMap.get(ALIGNMENT);
        renderer.setHorizontalAlignment( getAlignment(alignment) );
        return renderer;
    }

    public Map convertIdStringToParameterMap(String parameterString) {
        if (parameterString == null) parameterString = getDefaultRendererId();
        HashMap map = new HashMap(2);
        map.put(ALIGNMENT, parameterString);
        return map;
    }

    public String convertParameterMapToIdString(Map parameterMap) {
        return (String) parameterMap.get(ALIGNMENT);
    }

    private int getAlignment(String alignment) {
        if (LEFT.equals(alignment)) return JLabel.LEFT;
        if (CENTRE.equals(alignment)) return JLabel.CENTER;
        if (RIGHT.equals(alignment)) return JLabel.RIGHT;
        return JLabel.RIGHT;
    }

}
