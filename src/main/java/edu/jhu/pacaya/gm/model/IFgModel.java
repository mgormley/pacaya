package edu.jhu.pacaya.gm.model;

import edu.jhu.pacaya.gm.feat.FeatureVector;

public interface IFgModel {

    void add(int feat, double addend);

    void addAfterScaling(FeatureVector fv, double multiplier);    

}