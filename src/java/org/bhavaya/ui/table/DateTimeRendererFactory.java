package org.bhavaya.ui.table;

import org.bhavaya.collection.Association;
import org.bhavaya.collection.DefaultAssociation;

import javax.swing.table.TableCellRenderer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author Daniel van Enckevort
 * @version $Revision: 1.3 $
 */
public class DateTimeRendererFactory extends TableCellRendererFactory {
    private Association dateFormats;
    private String defaultId;

    private String DATE_FORMAT = "Date Format";

    public DateTimeRendererFactory(String[] dateFormatStrings) {
        this.dateFormats = new DefaultAssociation(dateFormatStrings.length/2);
        this.defaultId = dateFormatStrings[0];
        for (int i = 0; i < dateFormatStrings.length; i+=2) {
            String name = dateFormatStrings[i];
            String format = dateFormatStrings[i+1];
            dateFormats.put(name, format);
        }
    }

    public String[] getParameterNames() {
        return new String[]{DATE_FORMAT};
    }

    public String[] getAllowedValuesForParameter(String parameterName) {
        if (DATE_FORMAT.equals(parameterName)) {
            return (String[]) dateFormats.keySet().toArray(new String[0]);
        }
        return new String[0];
    }

    public String getDefaultRendererId() {
        return defaultId;
    }

    public TableCellRenderer getInstance(Map parameterMap) {
        String formatName = (String) parameterMap.get(DATE_FORMAT);
        String format = (String) dateFormats.get(formatName);
        return new DateTimeRenderer(format);
    }

    public Map convertIdStringToParameterMap(String parameterString) {
        HashMap map = new HashMap(2);
        if (! dateFormats.containsKey(parameterString)) {
            parameterString = getDefaultRendererId();
        }
        map.put(DATE_FORMAT, parameterString);
        return map;
    }


    public String convertParameterMapToIdString(Map parameterMap) {
        if (parameterMap == null) return null;
        return (String) parameterMap.get(DATE_FORMAT);
    }
}
