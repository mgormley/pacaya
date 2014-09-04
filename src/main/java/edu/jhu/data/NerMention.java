package edu.jhu.data;

/**
 * Mention of a named entity or a similarly labeled span in a sentence.
 * 
 * @author mgormley
 */
public class NerMention implements Comparable<NerMention> {

    // Required fields.
    private Span span;
    private String entityType;
    private String entitySubType;
    // Optional fields.
    private String phraseType;
    private int head;
    private String id;

    public NerMention(Span span, String entityType) {
        this.span = span;
        this.entityType = entityType;
    }

    public NerMention(Span span, String entityType, String entitySubType, String phraseType, int head, String id) {
        this.span = span;
        this.entityType = entityType;
        this.entitySubType = entitySubType;
        this.phraseType = phraseType;
        this.head = head;
        this.id = id;
    }

    /** Deep copy constructor. */
    public NerMention(NerMention other) {
        this.span = other.span;
        this.entityType = other.entityType;
        this.entitySubType = other.entitySubType;
        this.phraseType = other.phraseType;
        this.head = other.head;
        this.id = other.id;
    }

    public int hashCode() {
        throw new RuntimeException("not implemented");
    }

    public boolean equals() {
        throw new RuntimeException("not implemented");
    }

    public Span getSpan() {
        return span;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntitySubType() {
        return entitySubType;
    }

    public String getPhraseType() {
        return phraseType;
    }

    public int getHead() {
        return head;
    }

    public String getId() {
        return id;
    }

    public void intern() {
        entityType = entityType.intern();
        entitySubType = entitySubType.intern();
        phraseType = phraseType.intern();
        id = id.intern();
    }

    @Override
    public String toString() {
        return "FancySpan [span=" + span + ", entityType=" + entityType + ", entitySubType=" + entitySubType
                + ", phraseType=" + phraseType + ", head=" + head + ", id=" + id + "]";
    }

    @Override
    public int compareTo(NerMention other) {
        if (this.span == null && other.span == null) {
            return 0;
        } else if (this.span == null) {
            return -1;
        } else if (other.span == null) {
            return 1;
        }
        return this.span.compareTo(other.span);
    }

}
