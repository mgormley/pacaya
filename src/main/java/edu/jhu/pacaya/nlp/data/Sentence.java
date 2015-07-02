package edu.jhu.pacaya.nlp.data;

import edu.jhu.prim.bimap.IntObjectBimap;


public class Sentence extends LabelSequence<String> {

    private static final long serialVersionUID = 1L;
        
    protected Sentence(IntObjectBimap<String> alphabet) {
        super(alphabet); 
    }

    public Sentence(IntObjectBimap<String> alphabet, Iterable<String> labels) {
        super(alphabet, labels);
    }
    
    public Sentence(IntObjectBimap<String> alphabet, int[] labelIds) {
        super(alphabet, labelIds);
    }

}
