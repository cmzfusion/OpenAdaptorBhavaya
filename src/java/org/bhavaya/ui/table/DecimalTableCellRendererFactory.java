package org.bhavaya.ui.table;

import org.bhavaya.util.Log;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author Daniel van Enckevort
 * @version $Revision: 1.6 $
 */
public class DecimalTableCellRendererFactory extends TableCellRendererFactory {
    private static final Log log = Log.getCategory(DecimalTableCellRendererFactory.class);

    private static final String PRECISION = "Precision";
    private static final String ALIGNMENT = "Alignment";
    private static final String MULTIPLIER = "Value in";

    private String[] parameterNames;

    protected static final String FREE = "Free";

    private static final String LEFT = "Left";
    private static final String CENTRE = "Centre";
    private static final String RIGHT = "Right";

    private static final String UNITS = "Units";
    private static final String THOUSANDS = "Thousands";
    private static final String MILLIONS = "Millions";
    private static final String AUTO = "Auto";

    private int rangeStart;
    private int rangeEnd;
    private boolean freeRange;
    private String defaultId;

    private String[] precisionOptions;
    private String[] alignmentOptions;
    private String[] multiplierOptions;

    public DecimalTableCellRendererFactory(String[] params) {
        if (params == null || params.length < 4) throw new IllegalArgumentException("Params must be at least 4 elements long");

        int paramIdx = 0;
        int rangeStart = Integer.parseInt(params[paramIdx++]);
        int rangeEnd = Integer.parseInt(params[paramIdx++]);
        boolean freeRange = Boolean.valueOf(params[paramIdx++]).booleanValue();
        boolean multipliers = false;
        if (params.length > 4) {
            multipliers = Boolean.valueOf(params[paramIdx++]).booleanValue();
        }
        init(rangeStart, rangeEnd, freeRange, multipliers,  params[paramIdx]);
    }

    public DecimalTableCellRendererFactory(int rangeStart, int rangeEnd, boolean freeRange, boolean multipliers, String defaultId) {
        init(rangeStart, rangeEnd, freeRange, multipliers, defaultId);
    }

    private void init(int rangeStart, int rangeEnd, boolean freeRange, boolean multipliers, String defaultId) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.freeRange = freeRange;
        this.defaultId = defaultId;

        precisionOptions = getAvailablePrecisionOptions();

        alignmentOptions = new String[]{LEFT, CENTRE, RIGHT};
        multiplierOptions = new String[]{UNITS, THOUSANDS, MILLIONS, AUTO};

        ArrayList parameterNamesList = new ArrayList();
        if (multipliers) {
            parameterNamesList.add(MULTIPLIER);
        }
        if (precisionOptions.length >1) {
            parameterNamesList.add(PRECISION);
        }
        parameterNamesList.add(ALIGNMENT);
        parameterNames = (String[]) parameterNamesList.toArray(new String[parameterNamesList.size()]);
    }

    protected String[] getAvailablePrecisionOptions() {
        ArrayList precisions = new ArrayList();
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
        } else if (MULTIPLIER.equals(parameterName)) {
            return multiplierOptions;
        }
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDefaultRendererId() {
        return defaultId;
    }

    public TableCellRenderer getInstance(Map parameterMap) {
        String precision = (String) parameterMap.get(PRECISION);
        DefaultTableCellRenderer renderer = getRendererForPrecision(precision);

        String alignment = (String) parameterMap.get(ALIGNMENT);
        renderer.setHorizontalAlignment( getAlignment(alignment) );

        if (renderer instanceof DecimalRenderer) {
            DecimalRenderer decimalRenderer = (DecimalRenderer) renderer;
            String multiplier = (String) parameterMap.get(MULTIPLIER);
            decimalRenderer.setMultiplier(getMultiplier(multiplier));
        }

        return renderer;
    }

    protected DefaultTableCellRenderer getRendererForPrecision(String precision) {
        DefaultTableCellRenderer renderer = null;
        if (FREE.equals(precision)) {
            renderer = new DecimalRenderer();
        } else {
            try {
                int digits = Integer.parseInt(precision);
                renderer = new DecimalRenderer(digits);
            } catch (NumberFormatException e) {
                log.error("could not parse renderer precision string", e);
            }
        }
        if (renderer == null) renderer = new DecimalRenderer();
        return renderer;
    }

    public Map convertIdStringToParameterMap(String parameterString) {
        if (parameterString == null) parameterString = getDefaultRendererId();
        String[] params = parameterString.split("\\|");
        if (params.length >= 2) {
            HashMap map = new HashMap(4);
            map.put(PRECISION, params[0]);
            map.put(ALIGNMENT, params[1]);
            map.put(MULTIPLIER, params.length > 2 ? params[2] : UNITS);
            return map;
        }
        log.error("Could not convert parameter string: "+parameterString+" to map", new RuntimeException());
        return null;
    }

    public String convertParameterMapToIdString(Map parameterMap) {
        return parameterMap.get(PRECISION)+"|"+parameterMap.get(ALIGNMENT)+"|"+parameterMap.get(MULTIPLIER);
    }

    private int getAlignment(String alignment) {
        if (LEFT.equals(alignment)) return JLabel.LEFT;
        if (CENTRE.equals(alignment)) return JLabel.CENTER;
        if (RIGHT.equals(alignment)) return JLabel.RIGHT;
        return JLabel.RIGHT;
    }

    private double getMultiplier(String multiplierDescription) {
        if (AUTO.equals(multiplierDescription)) {
            return DecimalRenderer.AUTO_MULTIPLIER;
        } else if (THOUSANDS.equals(multiplierDescription)) {
            return 0.001;
        } else if (MILLIONS.equals(multiplierDescription)) {
            return 0.000001;
        } else {
            return Double.NaN;
        }
    }
}
