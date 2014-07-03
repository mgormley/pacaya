package edu.jhu.data;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.data.conll.CoNLLXSentence;
import edu.jhu.data.conll.CoNLLXToken;
import edu.jhu.util.Alphabet;


public class Sentence extends LabelSequence<String> {

    private static final long serialVersionUID = 1L;
    
    protected Sentence(Alphabet<String> alphabet) {
        super(alphabet); 
    }
    
    public Sentence(Alphabet<String> alphabet, DepTree tree) {
        this(alphabet);
        for (DepTreeNode node : tree.getNodes()) {
            if (!node.isWall()) {
                add(node.getLabel());
            }
        }
    }
    
    public Sentence(CoNLLXSentence sent, Alphabet<String> alphabet) {
        this(alphabet);
        for (CoNLLXToken token : sent) {
            // TODO: Here we just add the tags.
            add(token.getPosTag());
        }
    }

    public Sentence(CoNLL09Sentence sent, Alphabet<String> alphabet, boolean usePredictedPosTags) {
        this(alphabet);
        for (CoNLL09Token token : sent) {
            // TODO: Here we just add the tags.
            if (usePredictedPosTags) {                
                add(token.getPpos());
            } else {
                add(token.getPos());
            }
        }
    }

    public Sentence(Alphabet<String> alphabet, Iterable<String> labels) {
        super(alphabet, labels);
    }
    
    public Sentence(Alphabet<String> alphabet, int[] labelIds) {
        super(alphabet, labelIds);
    }

}
