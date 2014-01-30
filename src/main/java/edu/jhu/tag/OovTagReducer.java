package edu.jhu.tag;

import edu.jhu.util.Alphabet;

public class OovTagReducer extends AbstractTagReducer {

    private Alphabet<String> alphabet;
    private String unk;
    
    public OovTagReducer(Alphabet<String> alphabet, String unk) {
        super();
        this.alphabet = alphabet;
        this.unk = unk;
    }

    @Override
    public String reduceTag(String tag) {
        if (alphabet.lookupIndex(tag) == -1) {
            return unk;
        }
        return tag;
    }

}
