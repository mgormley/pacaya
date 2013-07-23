package edu.jhu.data;

import edu.jhu.util.Alphabet;

public class OovTagReducer extends AbstractTagReducer {

    private Alphabet<Label> alphabet;
    private String unk;
    
    public OovTagReducer(Alphabet<Label> alphabet, String unk) {
        super();
        this.alphabet = alphabet;
        this.unk = unk;
    }

    @Override
    public String reduceTag(String tag) {
        if (alphabet.lookupIndex(new TaggedWord(tag, tag)) == -1) {
            return unk;
        }
        return tag;
    }

}
