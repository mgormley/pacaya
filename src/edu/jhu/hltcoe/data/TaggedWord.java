package edu.jhu.hltcoe.data;


public class TaggedWord implements Label {

    private String word;
    private String tag;
    
    public TaggedWord(String word, String tag) {
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
    public void setTag(String tag) {
        this.tag = tag;
    }
    
    public String getLabel() {
        // Must also update hashCode and equals if changing this
        return tag;
    }
    
    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
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
        TaggedWord other = (TaggedWord) obj;
        if (tag == null) {
            if (other.tag != null)
                return false;
        } else if (!tag.equals(other.tag))
            return false;
        return true;
    }

    @Override
    public int compareTo(Label arg0) {
        return getLabel().compareTo(arg0.getLabel());
    }
    

}
