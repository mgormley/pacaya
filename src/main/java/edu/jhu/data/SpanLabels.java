package edu.jhu.data;

/**
 * Labeling of spans in a sentence. This can be used to represent, for example, the labeling of
 * named-entities.
 * 
 * @author mgormley
 */
public class SpanLabels {

    // The sentence length.
    private int n;
    // The labels for each span. labels[i][j] gives the label for the span from i to j, or null is
    // there is no label.
    // TODO: This could be represented as a String[] with careful indexing.
    private String[][] labels;
    
    public SpanLabels(int sentLength) {
        this.n = sentLength;
        labels = new String[n][n];
    }
        
    /**
     * Gets the label corresponding to the span with given start / end indices.
     * @param start Starting token of the span (inclusive).
     * @param end Ending token of the span (exclusive).
     * @return The label corresponding to the span, or null if there is no label.
     */
    public String getLabel(int start, int end) {
        checkSpan(start, end);
        return labels[start][end];
    }
    
    /**
     * Sets the label corresponding to the span with given start / end indices.
     * @param start Starting token of the span (inclusive).
     * @param end Ending token of the span (exclusive).
     * @param label The label.
     * @return The previous label for the span.
     */
    public String setLabel(int start, int end, String label) {
        checkSpan(start, end);
        String prev = labels[start][end];
        labels[start][end] = label;
        return prev;
    }
    
    /**
     * Removes the label corresponding to the span with given start / end indices.
     * @param start Starting token of the span (inclusive).
     * @param end Ending token of the span (exclusive).
     * @return The previous label for the span.
     */
    public String removeLabel(int start, int end) {
        return setLabel(start, end, null);
    }
    
    /**
     * Gets the label corresponding to the span.
     * @param span The span to get.
     * @return The label for the span.
     */
    public String getLabel(Span span) {
        return getLabel(span.start(), span.end());
    }

    /**
     * Sets the label corresponding to the span.
     * @param span The span to set.
     * @return The previous label for the span.
     */
    public String setLabel(Span span, String label) {
        return setLabel(span.start(), span.end(), label);
    }

    private void checkSpan(int start, int end) {
        if (start < 0 || end < 0 || start >= end || start >= n || end > n) {
            throw new IllegalArgumentException(String.format("Invalid span: start=%d end=%d", start, end));
        }
    }
    
}
