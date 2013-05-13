package edu.jhu.hltcoe.data.conll;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.util.Alphabet;

/**
 * Dependency tree that carries the original CoNLL-X sentence as metadata.
 * 
 * @author mgormley
 *
 */
public class CoNLLXDepTree extends DepTree {

    private CoNLLXSentence sent;
    
    public CoNLLXDepTree(CoNLLXSentence sent, Alphabet<Label> alphabet) {
        // TODO: filter out punctuation.
        super(new Sentence(sent, alphabet), sent.getParents(), false);
        this.sent = sent;
    }

    public CoNLLXSentence getCoNLLXSentence() {
        return sent;
    }
    
}
