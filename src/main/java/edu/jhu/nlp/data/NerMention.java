package edu.jhu.nlp.data;

import org.apache.commons.lang3.ObjectUtils;

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

    public NerMention(Span span, String entityType, String entitySubType) {
        this.span = span;
        this.entityType = entityType;
        this.entitySubType = entitySubType;
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
        this.span = new Span(other.span.start(), other.span.end());
        this.entityType = other.entityType;
        this.entitySubType = other.entitySubType;
        this.phraseType = other.phraseType;
        this.head = other.head;
        this.id = other.id;
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
    
    public void setSpan(Span span) {
        this.span = span;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntitySubType(String entitySubType) {
        this.entitySubType = entitySubType;
    }

    public void setPhraseType(String phraseType) {
        this.phraseType = phraseType;
    }

    public void setHead(int head) {
        this.head = head;
    }

    public void setId(String id) {
        this.id = id;
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
        int diff;
        diff = ObjectUtils.compare(this.head, other.head);
        if (diff != 0) { return diff; }
        diff = ObjectUtils.compare(this.span, other.span);
        if (diff != 0) { return diff; }
        diff = ObjectUtils.compare(this.entityType, other.entityType);
        if (diff != 0) { return diff; }
        diff = ObjectUtils.compare(this.entitySubType, other.entitySubType);
        if (diff != 0) { return diff; }
        diff = ObjectUtils.compare(this.phraseType, other.phraseType);
        if (diff != 0) { return diff; }
        diff = ObjectUtils.compare(this.id, other.id);
        return diff;        
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entitySubType == null) ? 0 : entitySubType.hashCode());
        result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
        result = prime * result + head;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((phraseType == null) ? 0 : phraseType.hashCode());
        result = prime * result + ((span == null) ? 0 : span.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NerMention other = (NerMention) obj;
        if (entitySubType == null) {
            if (other.entitySubType != null)
                return false;
        } else if (!entitySubType.equals(other.entitySubType))
            return false;
        if (entityType == null) {
            if (other.entityType != null)
                return false;
        } else if (!entityType.equals(other.entityType))
            return false;
        if (head != other.head)
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (phraseType == null) {
            if (other.phraseType != null)
                return false;
        } else if (!phraseType.equals(other.phraseType))
            return false;
        if (span == null) {
            if (other.span != null)
                return false;
        } else if (!span.equals(other.span))
            return false;
        return true;
    }    

}
