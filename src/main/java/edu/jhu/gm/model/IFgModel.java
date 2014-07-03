package edu.jhu.gm.model;

import edu.jhu.gm.feat.FeatureVector;

public interface IFgModel {

    void add(int feat, double addend);

    void addAfterScaling(FeatureVector fv, double multiplier);    

}