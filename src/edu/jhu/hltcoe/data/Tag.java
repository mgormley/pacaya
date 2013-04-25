package edu.jhu.hltcoe.data;


public class Tag extends AbstractLabel implements Label {

    private String tag;
    
    public Tag(String tag) {
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
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        Tag other = (Tag) obj;
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
    
    @Override
    public String toString() {
        return getLabel();
    }
    
}
