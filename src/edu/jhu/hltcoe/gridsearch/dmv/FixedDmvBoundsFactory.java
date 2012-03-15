package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.List;

import edu.jhu.hltcoe.data.SentenceCollection;

public class FixedDmvBoundsFactory implements DmvBoundsFactory {

    public FixedDmvBoundsFactory(SentenceCollection sentences) {
        // Run Viterbi EM a few times and divide the parameter space
        // based on the resulting parameter values
    }
    
    @Override
    public List<DmvBounds> getDmvBounds(DmvProblemNode dmvProblemNode) {
        // TODO Auto-generated method stub
        return null;
    }

}
