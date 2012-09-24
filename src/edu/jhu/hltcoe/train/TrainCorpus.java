package edu.jhu.hltcoe.train;

import util.Alphabet;
import edu.jhu.hltcoe.data.Label;

public interface TrainCorpus {
    
    Alphabet<Label> getLabelAlphabet();
    int size();
    boolean isLabeled(int s);
    
}
