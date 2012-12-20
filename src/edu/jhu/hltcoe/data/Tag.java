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

}
