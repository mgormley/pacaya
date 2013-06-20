package edu.jhu.data;


public class Tag extends AbstractLabel implements Label {

    private String tag;
    
    public Tag(String tag) {
        if (tag == null) {
            throw new IllegalArgumentException("tag is null");
        }
        this.tag = tag.intern();
    }

    public String getTag() {
        return tag;
    }

    /**
     * For reducing from 45 to 17 tags.
     */
    @Deprecated
    void setTag(String tag) {
        this.tag = tag;
    }
    
    public String getLabel() {
        // Must also update hashCode and equals if changing this
        return tag;
    }

    @Override
    public int hashCode() {
        return tag.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TaggedWord) {
            TaggedWord other = (TaggedWord) obj;
            return tag.equals(other.getTag());
        } else if (obj instanceof Tag) {
            Tag other = (Tag) obj;
            return tag.equals(other.tag);
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(Label label) {
        if (label instanceof Tag || label instanceof TaggedWord) {
            return getLabel().compareTo(label.getLabel());
        } else {
            return -1;
        }
    }
    
    @Override
    public String toString() {
        return getLabel();
    }
    
}
