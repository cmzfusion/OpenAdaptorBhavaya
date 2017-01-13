package org.bhavaya.ui.table.formula;

import org.bhavaya.ui.table.DecimalRenderer;
import org.bhavaya.ui.table.DecimalTableCellRendererFactory;
import org.bhavaya.ui.table.TableCellRendererFactory;
import org.bhavaya.util.Log;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renderer factory for formula result cells
 * User: Jon Moore
 * Date: 27/01/11
 * Time: 17:05
 */
public class FormulaResultTableCellRendererFactory extends TableCellRendererFactory {
    private static final Log log = Log.getCategory(DecimalTableCellRendererFactory.class);

    private static final String PRECISION = "Precision";
    private static final String ALIGNMENT = "Alignment";

    private String[] parameterNames;

    protected static final String FREE = "Free";

    private static final String LEFT = "Left";
    private static final String CENTRE = "Centre";
    private static final String RIGHT = "Right";

    private int rangeStart;
    private int rangeEnd;
    private boolean freeRange;
    private String defaultId;

    private String[] precisionOptions;
    private String[] alignmentOptions;

    public FormulaResultTableCellRendererFactory(String[] params) {
        if (params == null || params.length < 4) throw new IllegalArgumentException("Params must be at least 4 elements long");

        int paramIdx = 0;
        int rangeStart = Integer.parseInt(params[paramIdx++]);
        int rangeEnd = Integer.parseInt(params[paramIdx++]);
        boolean freeRange = Boolean.valueOf(params[paramIdx++]).booleanValue();
        init(rangeStart, rangeEnd, freeRange, params[paramIdx]);
    }

    public FormulaResultTableCellRendererFactory(int rangeStart, int rangeEnd, boolean freeRange, String defaultId) {
        init(rangeStart, rangeEnd, freeRange, defaultId);
    }

    private void init(int rangeStart, int rangeEnd, boolean freeRange, String defaultId) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.freeRange = freeRange;
        this.defaultId = defaultId;

        precisionOptions = getAvailablePrecisionOptions();

        alignmentOptions = new String[]{LEFT, CENTRE, RIGHT};

        List<String> parameterNamesList = new ArrayList<String>();
        if (precisionOptions.length >1) {
            parameterNamesList.add(PRECISION);
        }
        parameterNamesList.add(ALIGNMENT);
        parameterNames = (String[]) parameterNamesList.toArray(new String[parameterNamesList.size()]);
    }

    protected String[] getAvailablePrecisionOptions() {
        List<String> precisions = new ArrayList<String>();
        for (int i=rangeStart; i<=rangeEnd; i++) {
            precisions.add(Integer.toString(i));
        }
        if (freeRange) precisions.add(FREE);
        return (String[]) precisions.toArray(new String[0]);
    }

    public String[] getParameterNames() {
        return parameterNames;
    }

    public String[] getAllowedValuesForParameter(String parameterName) {
        if (PRECISION.equals(parameterName)) {
            return precisionOptions;
        } else if (ALIGNMENT.equals(parameterName)) {
            return alignmentOptions;
        }
        return new String[0];
    }

    public String getDefaultRendererId() {
        return defaultId;
    }

    public TableCellRenderer getInstance(Map parameterMap) {
        String precision = (String) parameterMap.get(PRECISION);
        FormulaResultRenderer renderer = getRendererForPrecision(precision);

        String alignment = (String) parameterMap.get(ALIGNMENT);
        renderer.setHorizontalAlignment( getAlignment(alignment) );

        return renderer;
    }

    protected FormulaResultRenderer getRendererForPrecision(String precision) {
        DecimalRenderer renderer = null;
        if (!FREE.equals(precision)) {
            try {
                int digits = Integer.parseInt(precision);
                renderer = new DecimalRenderer(digits);
            } catch (NumberFormatException e) {
                log.error("could not parse renderer precision string", e);
            }
        }
        if(renderer == null) {
            renderer = new DecimalRenderer();
        }
        return new FormulaResultRenderer(renderer);
    }

    public Map convertIdStringToParameterMap(String parameterString) {
        if (parameterString == null) parameterString = getDefaultRendererId();
        String[] params = parameterString.split("\\|");
        if (params.length >= 2) {
            Map<String, String> map = new HashMap<String, String>(2);
            map.put(PRECISION, params[0]);
            map.put(ALIGNMENT, params[1]);
            return map;
        }
        log.error("Could not convert parameter string: "+parameterString+" to map", new RuntimeException());
        return null;
    }

    public String convertParameterMapToIdString(Map parameterMap) {
        return parameterMap.get(PRECISION)+"|"+parameterMap.get(ALIGNMENT);
    }

    private int getAlignment(String alignment) {
        if (LEFT.equals(alignment)) return JLabel.LEFT;
        if (CENTRE.equals(alignment)) return JLabel.CENTER;
        if (RIGHT.equals(alignment)) return JLabel.RIGHT;
        return JLabel.RIGHT;
    }
}
