package edu.jhu.data;


public class TaggedWord implements Label {

    private static final long serialVersionUID = -3732352766423007305L;
    private String word;
    private String tag;
    
    public TaggedWord(String word, String tag) {
        if (tag == null) {
            throw new IllegalArgumentException("tag is null");
        }
        if (word == null) {
            throw new IllegalArgumentException("word is null");
        }        
        this.word = word.intern();
        this.tag = tag.intern();
    }

    public String getWord() {
        return word;
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
        return tag;
    }
    
    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public int hashCode() {
        return tag.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TaggedWord) {
            TaggedWord other = (TaggedWord) obj;
            return tag.equals(other.tag);
        } else if (obj instanceof Tag) {
            Tag other = (Tag) obj;
            return tag.equals(other.getTag());
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
    
}
