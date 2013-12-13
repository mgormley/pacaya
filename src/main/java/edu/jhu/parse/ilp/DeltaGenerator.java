package edu.jhu.parse.ilp;

import edu.jhu.parse.ilp.IdentityDeltaGenerator.DeltaList;

public interface DeltaGenerator {

    DeltaList getDeltas(double weight);
    
}
