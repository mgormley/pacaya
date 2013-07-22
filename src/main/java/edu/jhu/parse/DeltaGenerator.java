package edu.jhu.parse;

import edu.jhu.parse.IdentityDeltaGenerator.DeltaList;

public interface DeltaGenerator {

    DeltaList getDeltas(double weight);
    
}
