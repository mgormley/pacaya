package edu.jhu.induce.train;

import edu.jhu.data.Label;
import edu.jhu.util.Alphabet;

public interface SemiSupervisedCorpus {
    
    Alphabet<Label> getLabelAlphabet();
    int size();
    boolean isLabeled(int s);
    
}
