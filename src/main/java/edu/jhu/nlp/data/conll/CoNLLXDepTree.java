package edu.jhu.nlp.data.conll;

import edu.jhu.nlp.data.DepTree;
import edu.jhu.nlp.data.Sentence;
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
        super(new Sentence(sent, alphabet), sent.getParentsFromHead(), false);
        this.sent = sent;
    }

    public CoNLLXSentence getCoNLLXSentence() {
        return sent;
    }
    
}
