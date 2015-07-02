package edu.jhu.pacaya.gm.model;

import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.Semiring;

/**
 * Factor which clamps a variable to a specific value.
 * @author mgormley
 */
public class ClampFactor extends ExplicitFactor implements Factor {

    private static final long serialVersionUID = 1L;
    
    private int state;

    public ClampFactor(Var v, int state) {
        super(new VarSet(v));
        this.state = state;

        Semiring s = LogSemiring.getInstance();
        this.fill(s.zero());
        this.setValue(state, s.one());
    }

    public void updateFromModel(FgModel model) {
        // No-op.
    }
        
}
