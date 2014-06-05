package edu.jhu.gm.model;

import edu.jhu.util.semiring.LogSemiring;
import edu.jhu.util.semiring.RealAlgebra;
import edu.jhu.util.semiring.Semiring;

public class ClampFactor extends ExplicitFactor implements Factor {

    private static final long serialVersionUID = 1L;
    
    private int state;

    public ClampFactor(Var v, int state) {
        super(new VarSet(v));
        this.state = state;

        Semiring s = new LogSemiring();
        this.fill(s.zero());
        this.setValue(state, s.one());
    }

    public void updateFromModel(FgModel model) {
        // No-op.
    }

    @Override
    public ClampFactor getClamped(VarConfig clmpVarConfig) {
        throw new IllegalStateException("Clamp factors shouldn't be clamped.");
    }
        
}
