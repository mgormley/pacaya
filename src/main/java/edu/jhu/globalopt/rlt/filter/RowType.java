/**
 * 
 */
package edu.jhu.globalopt.rlt.filter;

public enum RowType {
    /**
     * For rows created at initialization time.
     */
    INITIAL,
    /**
     * For rows subsequently added as cuts.
     */
    CUT
}