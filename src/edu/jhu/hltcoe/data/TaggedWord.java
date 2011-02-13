package edu.jhu.hltcoe.data;


public class TaggedWord {

    private String word;
    private String tag;
    private int position;
    
    public TaggedWord(String word, String tag, int position) {
        this.word = word;
        this.tag = tag;
        this.position = position;
    }

    public String getWord() {
        return word;
    }

    public String getTag() {
        return tag;
    }

    public int getPosition() {
        return position;
    }
    

}
