package edu.jhu.nlp.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Labeling of spans in a sentence. This can be used to represent, for example, the labeling of
 * named-entities. The mentions are kept in sorted order.
 * 
 * @author mgormley
 */
public class NerMentions implements Iterable<NerMention> {

    // The sentence length.
    private int n;
    // The sorted named entity mentions.
    List<NerMention> ments;

    public NerMentions(int n, List<NerMention> spans) {
        this.ments = spans;
        Collections.sort(spans);
    }

    /** Deep copy constructor. */
    public NerMentions(NerMentions other) {
        this.n = other.n;
        this.ments = new ArrayList<>(other.ments.size());
        for (int i = 0; i < ments.size(); i++) {
            this.ments.add(new NerMention(other.ments.get(i)));
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
        for (NerMention s : ments) {
            s.intern();
        }
    }

    @Override
    public Iterator<NerMention> iterator() {
        return ments.iterator();
    }

    @Override
    public String toString() {
        return "LabeledSpans [n=" + n + ", spans=" + ments + "]";
    }

    public String toString(List<String> words) {
        StringBuilder sb = new StringBuilder();
        sb.append("LabeledSpans [");
        for (int i = 0; i < words.size(); i++) {
            if (i != 0) {
                sb.append(" ");
            }
            for (int j = 0; j < ments.size(); j++) {
                NerMention s = ments.get(j);
                if (s.getSpan().start() == i) {
                    sb.append(String.format("<e j=%d t=%s st=%s pt=%s>", j, s.getEntityType(), s.getEntitySubType(),
                            s.getPhraseType()));
                }
            }
            sb.append(words.get(i));
            for (int j = 0; j < ments.size(); j++) {
                NerMention s = ments.get(j);
                if (s.getSpan().end() == i + 1) {
                    sb.append(String.format("</e j=%d>", j));
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public NerMention get(int i) {
        return ments.get(i);
    }
    
    public int size() {
        return ments.size();
    }

}
