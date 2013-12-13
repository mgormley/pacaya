package edu.jhu.globalopt.rlt.filter;

/**
 * Adds all RLT rows.
 * 
 * @author mgormley
 * 
 */
public class AllRltRowAdder extends RandPropRltRowAdder implements RltRowAdder {

    public AllRltRowAdder() {
        super(1.0, 1.0);
    }

}
