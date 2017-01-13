package org.bhavaya.util;

/**
 * @author <a href="mailto:Sabine.Haas@dkib.com">Sabine Haas, Dresdner Kleinwort</a>
 */
public class ThresholdSetConstraint implements SetConstraint {
    static {
        BeanUtilities.addPersistenceDelegate(ThresholdSetConstraint.class, new BhavayaPersistenceDelegate(new String[]{"threshold", "requiredDecimalPlaces"}));
    }

    private double threshold = Double.NaN;
    private int requiredDecimalPlaces = 8;

    public ThresholdSetConstraint(double threshold, int requiredDecimalPlaces) {
        this.requiredDecimalPlaces = requiredDecimalPlaces;
        this.threshold = threshold;
    }

    public ThresholdSetConstraint(double threshold) {
        this.threshold = threshold;
    }

    public String getErrorMessage(Object oldValue, Object newValue) {
        double oldVal = Utilities.round(((Number) oldValue).doubleValue(), requiredDecimalPlaces);
        double newVal = Utilities.round(((Number) newValue).doubleValue(), requiredDecimalPlaces);
        return "abs(" + oldVal + "-" + newVal + ") > " + threshold;
    }

    public boolean constraintViolated(Object oldValue, Object newValue) {
        return !(oldValue instanceof Number) || !(newValue instanceof Number)
                || roundedAbsDifference((Number) oldValue, (Number) newValue) > threshold;
    }

    private double roundedAbsDifference(Number oldValue, Number newValue) {
        double oldDoubleValue = 0.0;
        if (oldValue != null && !(Double.isNaN(oldValue.doubleValue()))) {
            oldDoubleValue = oldValue.doubleValue();
        }
        double diff = oldDoubleValue - newValue.doubleValue();
        double abs = Math.abs(diff);
        return Utilities.round(abs, requiredDecimalPlaces);
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setRequiredDecimalPlaces(int requiredDecimalPlaces) {
        this.requiredDecimalPlaces = requiredDecimalPlaces;
    }

    public int getRequiredDecimalPlaces() {
        return requiredDecimalPlaces;
    }
}
