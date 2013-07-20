package edu.jhu.gm;

public interface Factor {

    Factor getClamped(VarConfig clmpVarConfig);

    /** Gets the variables associated with this factor. */
    VarSet getVars();
    
//    /**
//     * Gets the marginal distribution over a subset of the variables in this
//     * factor, optionally normalized. This method assumes the values are reals
//     * (i.e. not in the log domain).
//     * 
//     * @param vars The subset of variables for the marginal distribution. This will sum over all variables not in this set.
//     * @param normalize Whether to normalize the resulting distribution.
//     * @return The marginal distribution represented as log-probabilities.
//     */
//    Factor getMarginal(VarSet vars, boolean normalize);
//
//    /**
//     * Gets the marginal distribution over a subset of the variables in this
//     * factor, optionally normalized. This method assumes the values are 
//     * in the log domain.
//     * 
//     * @param vars The subset of variables for the marginal distribution. This will sum over all variables not in this set.
//     * @param normalize Whether to normalize the resulting distribution.
//     * @return The marginal distribution represented as log-probabilities.
//     */
//    Factor getLogMarginal(VarSet vars, boolean normalize);
//
//    /**
//     * Gets the value of the c'th configuration of the variables.
//     */
//    double getValue(int c);
//
//    /** Sets the value of the c'th configuration of the variables. */
//    void setValue(int c, double value);
//
//    /** Set all the values to the given value. */
//    void set(double value);
//
//    /** Add the addend to each value. */
//    void add(double addend);
//
//    /** Scale each value by lambda. */
//    void scale(double lambda);
//
//    /** Normalizes the values. */
//    void normalize();
//
//    /** Normalizes the values. */
//    void logNormalize();
//
//    /** Takes the log of each value. */
//    void convertRealToLog();
//
//    /** Takes the exp of each value. */
//    void convertLogToReal();
//
//    /** Gets the sum of the values for this factor. */
//    double getSum();
//
//    /** Gets the log of the sum of the values for this factor. */
//    double getLogSum();
//
//    /**
//     * Adds a factor to this one.
//     * 
//     * From libDAI: 
//     *  The sum of two factors is defined as follows: if
//     *  \f$f : \prod_{l\in L} X_l \to [0,\infty)\f$ and \f$g : \prod_{m\in M} X_m \to [0,\infty)\f$, then
//     *  \f[f+g : \prod_{l\in L\cup M} X_l \to [0,\infty) : x \mapsto f(x_L) + g(x_M).\f]
//     */
//    void add(DenseFactor f);
//
//    /**
//     * Multiplies a factor to this one.
//     * 
//     * From libDAI:
//     *  The product of two factors is defined as follows: if
//     *  \f$f : \prod_{l\in L} X_l \to [0,\infty)\f$ and \f$g : \prod_{m\in M} X_m \to [0,\infty)\f$, then
//     *  \f[fg : \prod_{l\in L\cup M} X_l \to [0,\infty) : x \mapsto f(x_L) g(x_M).\f]
//     */
//    void prod(DenseFactor f);
//
//    /**
//     * Log-adds a factor to this one.
//     * 
//     * This is analogous to factor addition, except that the logAdd operator
//     * is used instead.
//     */
//    void logAdd(DenseFactor f);
//
//    /**
//     * Sets each entry in this factor to that of the given factor.
//     * @param factor
//     */
//    void set(DenseFactor f);
//
//    String toString();
//
//    /** For testing only. */
//    double[] getValues();
//
//    /** Special equals with a tolerance. */
//    boolean equals(DenseFactor other, double delta);
//
//    /** Gets the ID of the configuration with the maximum value. */
//    int getArgmaxConfigId();

}