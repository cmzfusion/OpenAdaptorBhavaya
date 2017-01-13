package org.bhavaya.ui.table;

import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;

import javax.swing.table.TableCellRenderer;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author Daniel van Enckevort
 * @version $Revision: 1.1 $
 */
public class SimpleTableCellRendererFactory extends TableCellRendererFactory {
    private static final Log log = Log.getCategory( SimpleTableCellRendererFactory.class );

    private TableCellRenderer renderer;

    public SimpleTableCellRendererFactory(TableCellRenderer renderer) {
        this.renderer = renderer;
    }

    public SimpleTableCellRendererFactory(String[] params) throws Exception {
        String tableCellRendererClassName = params[0];
        Class rendererClass = ClassUtilities.getClass( tableCellRendererClassName );
        this.renderer = (TableCellRenderer) rendererClass.newInstance();
    }

    public String[] getParameterNames() {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String[] getAllowedValuesForParameter(String parameterName) {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDefaultRendererId() {
        return "";  //To change body of implemented methods use File | Settings | File Templates.
    }

    public TableCellRenderer getInstance(Map parameterMap) {
        return renderer;
    }

    public Map convertIdStringToParameterMap(String parameterString) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String convertParameterMapToIdString(Map parameterMap) {
        return "";  //To change body of implemented methods use File | Settings | File Templates.
    }
}
