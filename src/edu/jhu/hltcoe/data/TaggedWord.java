package edu.jhu.hltcoe.data;


public class TaggedWord extends AbstractLabel implements Label {

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

}
