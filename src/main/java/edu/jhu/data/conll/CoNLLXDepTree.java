package edu.jhu.data.conll;

import edu.jhu.data.DepTree;
import edu.jhu.data.Sentence;
import edu.jhu.util.Alphabet;

/**
 * Dependency tree that carries the original CoNLL-X sentence as metadata.
 * 
 * @author mgormley
 *
 */
public class CoNLLXDepTree extends DepTree {

    private CoNLLXSentence sent;
    
    public CoNLLXDepTree(CoNLLXSentence sent, Alphabet<String> alphabet) {
        // TODO: filter out punctuation.
        super(new Sentence(sent, alphabet), sent.getParents(), false);
        this.sent = sent;
    }

    public CoNLLXSentence getCoNLLXSentence() {
        return sent;
    }
    
}
