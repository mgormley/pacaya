package edu.jhu.hltcoe.parse;

import edu.jhu.hltcoe.parse.IdentityDeltaGenerator.DeltaList;

public interface DeltaGenerator {

    DeltaList getDeltas(double weight);
    
}
