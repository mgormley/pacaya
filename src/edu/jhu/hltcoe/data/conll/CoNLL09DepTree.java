package edu.jhu.hltcoe.data.conll;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.util.Alphabet;

/**
 * Dependency tree that carries the original CoNLL-2009 sentence as metadata.
 * 
 * @author mgormley
 *
 */
public class CoNLL09DepTree extends DepTree {

    private CoNLL09Sentence sent;
    
    public CoNLL09DepTree(CoNLL09Sentence sent, Alphabet<Label> alphabet) {
        // TODO: filter out punctuation.
        super(new Sentence(sent, alphabet), sent.getParents(), false);
        this.sent = sent;
    }

    public CoNLL09Sentence getCoNLL09Sentence() {
        return sent;
    }
    
}
