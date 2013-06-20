package edu.jhu.data.conll;

import edu.jhu.data.DepTree;
import edu.jhu.data.Label;
import edu.jhu.data.Sentence;
import edu.jhu.util.Alphabet;

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

    @Override
    public Sentence getSentence(Alphabet<Label> alphabet) {
        return new ValidParentsSentence(sent, alphabet);
    }

}
