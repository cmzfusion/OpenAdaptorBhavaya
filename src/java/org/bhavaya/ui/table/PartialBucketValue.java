package org.bhavaya.ui.table;

import org.bhavaya.util.Utilities;

import java.util.Comparator;

/**
 * Represents incomplete/partial value of a bucket. Will typically be rendered with a question mark.
 * It is used when bucket contains NaNs and/or positive/negative infinities.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class PartialBucketValue implements Comparable {
    private Object partialValue;
    private Object fullValue;
    private int bucketValueCount; // number of values in the bucket (including sub-buckets contained in this bucket)

    public PartialBucketValue(Object partialValue, Object fullValue, int bucketValueCount) {
        this.partialValue = partialValue;
        this.fullValue = fullValue;
        this.bucketValueCount = bucketValueCount;
    }

    public Object getPartialValue() {
        return partialValue;
    }

    public Object getFullValue() {
        return fullValue;
    }

    public int getBucketValueCount() {
        return bucketValueCount;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final PartialBucketValue that = (PartialBucketValue) o;

        if (bucketValueCount != that.bucketValueCount) return false;
        if (fullValue != null ? !fullValue.equals(that.fullValue) : that.fullValue != null) return false;
        if (partialValue != null ? !partialValue.equals(that.partialValue) : that.partialValue != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (partialValue != null ? partialValue.hashCode() : 0);
        result = 29 * result + (fullValue != null ? fullValue.hashCode() : 0);
        result = 29 * result + bucketValueCount;
        return result;
    }


    public String toString() {
        return "PartialBucketValue{" +
                "partialValue=" + partialValue +
                ", fullValue=" + fullValue +
                ", bucketValueCount=" + bucketValueCount +
                "}";
    }

    public int compareTo(Object o) {
        if (o instanceof PartialBucketValue) {
            return ((Double) getPartialValue()).compareTo((Double) ((PartialBucketValue) o).getPartialValue());
        } else if (o instanceof Double) {
            return ((Double) getPartialValue()).compareTo((Double) o);
        } else {
            return -1;
        }
    }

    public static class PartialBucketValueAwareComparator implements Comparator {
        private Comparator comparator;

        public PartialBucketValueAwareComparator() {
            this(Utilities.COMPARATOR);
        }

        public PartialBucketValueAwareComparator(Comparator comparator) {
            this.comparator = comparator;
        }

        public int compare(Object o1, Object o2) {
            if (o1 instanceof PartialBucketValue) o1 = ((PartialBucketValue) o1).getPartialValue();
            if (o2 instanceof PartialBucketValue) o2 = ((PartialBucketValue) o2).getPartialValue();
            return comparator.compare(o1, o2);
        }
    }

}

