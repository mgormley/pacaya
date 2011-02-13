package edu.jhu.hltcoe.inference;

import edu.jhu.hltcoe.model.Model;

public interface MStep<C> {

    Model getModel(C counts);

}
