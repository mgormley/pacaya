package edu.jhu.hltcoe.inference;

import edu.jhu.hltcoe.data.DepTreebank;

public interface MStep<M,C> {

    M getModel(C counts);

}
