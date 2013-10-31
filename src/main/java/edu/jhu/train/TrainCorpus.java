package edu.jhu.train;

import edu.jhu.data.Label;
import edu.jhu.util.Alphabet;

public interface TrainCorpus {
    
    Alphabet<Label> getLabelAlphabet();
    int size();
    boolean isLabeled(int s);
    
}
