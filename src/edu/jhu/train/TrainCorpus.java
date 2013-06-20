package edu.jhu.train;

import edu.jhu.util.Alphabet;
import edu.jhu.data.Label;

public interface TrainCorpus {
    
    Alphabet<Label> getLabelAlphabet();
    int size();
    boolean isLabeled(int s);
    
}
