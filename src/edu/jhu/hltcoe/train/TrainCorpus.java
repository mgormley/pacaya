package edu.jhu.hltcoe.train;

import edu.jhu.hltcoe.util.Alphabet;
import edu.jhu.hltcoe.data.Label;

public interface TrainCorpus {
    
    Alphabet<Label> getLabelAlphabet();
    int size();
    boolean isLabeled(int s);
    
}
