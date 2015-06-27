package edu.jhu.pacaya.util;

import java.util.BitSet;

import edu.jhu.prim.bimap.IntObjectBimap;



public class FeatureNames extends IntObjectBimap<Object> {

	private static final long serialVersionUID = 1L;

	private BitSet isBias = new BitSet();
	
	public FeatureNames() {
		super();
	}
	
	public FeatureNames(FeatureNames other) {
		super(other);
	}

	/** Mark this feature as a bias feature if the alphabet is growing. */
	public void setIsBias(int index) {
		if (isGrowing()) {
			isBias.set(index, true);
		}		
	}

	/** Returns whether this the feature is a bias feature. */
	public boolean isBiasFeature(int index) {
		return isBias.get(index);
	}

}
