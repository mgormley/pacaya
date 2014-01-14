package edu.jhu.data;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.data.conll.CoNLLXSentence;
import edu.jhu.data.conll.CoNLLXToken;
import edu.jhu.data.deptree.DepTreebankReader;
import edu.jhu.util.Alphabet;


public class Sentence extends LabelSequence<Label> {

    private static final long serialVersionUID = 1L;
    
    protected Sentence(Alphabet<Label> alphabet) {
        super(alphabet); 
    }
    
    public Sentence(Alphabet<Label> alphabet, DepTree tree) {
        this(alphabet);
        for (DepTreeNode node : tree.getNodes()) {
            if (!node.isWall()) {
                add(node.getLabel());
            }
        }
    }
    
    public Sentence(CoNLLXSentence sent, Alphabet<Label> alphabet) {
        this(alphabet);
        for (CoNLLXToken token : sent) {
            add(new TaggedWord(token.getForm(), token.getPosTag()));
        }
    }

    public Sentence(CoNLL09Sentence sent, Alphabet<Label> alphabet) {
        this(alphabet);
        for (CoNLL09Token token : sent) {
            if (DepTreebankReader.usePredictedPosTags) {
                add(new TaggedWord(token.getForm(), token.getPpos()));
            } else {
                add(new TaggedWord(token.getForm(), token.getPos()));
            }
        }
    }

    public Sentence(Alphabet<Label> alphabet, Iterable<Label> labels) {
        super(alphabet, labels);
    }
    
    public Sentence(Alphabet<Label> alphabet, int[] labelIds) {
        super(alphabet, labelIds);
    }

}
