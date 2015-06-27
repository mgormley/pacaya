package edu.jhu.pacaya.nlp.data;

import edu.jhu.pacaya.util.Alphabet;


public class Sentence extends LabelSequence<String> {

    private static final long serialVersionUID = 1L;
        
    protected Sentence(Alphabet<String> alphabet) {
        super(alphabet); 
    }

    public Sentence(Alphabet<String> alphabet, Iterable<String> labels) {
        super(alphabet, labels);
    }
    
    public Sentence(Alphabet<String> alphabet, int[] labelIds) {
        super(alphabet, labelIds);
    }

}
