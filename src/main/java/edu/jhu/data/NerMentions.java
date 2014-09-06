package edu.jhu.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Labeling of spans in a sentence. This can be used to represent, for example, the labeling of
 * named-entities.
 * 
 * @author mgormley
 */
public class NerMentions implements Iterable<NerMention> {

    // The sentence length.
    private int n;
    // The labels for each span.
    List<NerMention> spans;

    public NerMentions(int n) {
        spans = new ArrayList<>();
    }

    public NerMentions(int n, List<NerMention> spans) {
        this.spans = spans;
    }

    /** Deep copy constructor. */
    public NerMentions(NerMentions other) {
        this.n = other.n;
        this.spans = new ArrayList<>(other.spans.size());
        for (int i = 0; i < spans.size(); i++) {
            this.spans.add(new NerMention(other.spans.get(i)));
        }
    }

    /**
     * Gets the label corresponding to the span with given start / end indices.
     * 
     * @param start Starting token of the span (inclusive).
     * @param end Ending token of the span (exclusive).
     * @return The label corresponding to the span, or null if there is no label.
     */
    public String getLabel(int start, int end) {
        checkSpan(start, end);
        throw new RuntimeException();
        // TODO:
        // int idx = Collections.binarySearch(spans, new Span(start, end));
        // if (idx >= 0) {
        // return null;
        // }
        // return spans.get(idx).label;
    }

    private void checkSpan(int start, int end) {
        if (start < 0 || end < 0 || start >= end || start >= n || end > n) {
            throw new IllegalArgumentException(String.format("Invalid span: start=%d end=%d", start, end));
        }
    }

    public void intern() {
        for (NerMention s : spans) {
            s.intern();
        }
    }

    @Override
    public Iterator<NerMention> iterator() {
        return spans.iterator();
    }

    @Override
    public String toString() {
        return "LabeledSpans [n=" + n + ", spans=" + spans + "]";
    }

    public String toString(List<String> words) {
        StringBuilder sb = new StringBuilder();
        sb.append("LabeledSpans [");
        for (int i = 0; i < words.size(); i++) {
            if (i != 0) {
                sb.append(" ");
            }
            for (int j = 0; j < spans.size(); j++) {
                NerMention s = spans.get(j);
                if (s.getSpan().start() == i) {
                    sb.append(String.format("<e j=%d t=%s st=%s pt=%s>", j, s.getEntityType(), s.getEntitySubType(),
                            s.getPhraseType()));
                }
            }
            sb.append(words.get(i));
            for (int j = 0; j < spans.size(); j++) {
                NerMention s = spans.get(j);
                if (s.getSpan().end() == i + 1) {
                    sb.append(String.format("</e j=%d>", j));
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public NerMention get(int i) {
        return spans.get(i);
    }
    
    public int size() {
        return spans.size();
    }

}
