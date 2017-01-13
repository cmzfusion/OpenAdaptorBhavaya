package org.bhavaya.ui.table;

import org.bhavaya.util.Log;

import javax.swing.table.TableCellRenderer;
import java.util.Map;

/**
 * renderers are created by their ID string, however, in order to provide configuration UIs for renderers, we need
 * a structure with more information than a string. Therefore a map interface is used. This provides a key/value parameter list.
 *
 * I should look at removing the string interface - but consideration needs to be made as to whether this would create config file bloat
 * as multiple instances of maps that are .equals get persisted.
 *
 * TODO: think about error handling
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.4 $
 */
public abstract class TableCellRendererFactory {
    private static Log log = Log.getCategory(TableCellRendererFactory.class);

    public final TableCellRenderer getInstance(String rendererId) {
        Map parameters = null;
        try {
            parameters = convertIdStringToParameterMap(rendererId);
        } catch (Exception e) {
            log.error("could not convert "+rendererId+" to parameter map", e);
        }
        if (parameters == null) {
            rendererId = getDefaultRendererId();
            parameters = convertIdStringToParameterMap(rendererId);
        }
        TableCellRenderer instance = getInstance(parameters);
        assert (instance != null) : "getInstance for renderId "+rendererId+" cannot return null";
        return instance;
    }

    public abstract String[] getParameterNames();
    public abstract String[] getAllowedValuesForParameter(String parameterName);

    public abstract String getDefaultRendererId();

    /**
     * if the parameter map was created by convertIdStringToParameterMap( getDefaultRendererId() ) this should
     * NOT return null.
     * @param parameterMap
     */
    public abstract TableCellRenderer getInstance(Map parameterMap);
    public abstract Map convertIdStringToParameterMap(String parameterString);
    public abstract String convertParameterMapToIdString(Map parameterMap);

}
