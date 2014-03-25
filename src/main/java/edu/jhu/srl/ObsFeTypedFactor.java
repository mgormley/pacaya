package edu.jhu.srl;

import edu.jhu.gm.feat.ObsFeExpFamFactor;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.VarSet;

/**
 * A factor which includes its type (i.e. template).
 * 
 * @author mgormley
 */
public class ObsFeTypedFactor extends ObsFeExpFamFactor {

    private static final long serialVersionUID = 1L;

    Enum<?> type;
    
    public ObsFeTypedFactor(VarSet vars, Enum<?> type, ObsFeatureConjoiner cj, ObsFeatureExtractor obsFe) {
        super(vars, type, cj, obsFe);
        this.type = type;
    }
    
    /**
     * Constructs a {@link ObsFeTypedFactor}.
     * 
     * This constructor allows us to differentiate between the "type" of
     * factor (e.g. SENSE_UNARY) and its "templateKey" (e.g.
     * SENSE_UNARY_satisfacer.a1). Using Sense factors as an example, this
     * way we can use the type to determine which type of features should be
     * extracted, and the templateKey to determine which independent
     * classifier should be used.
     * 
     * @param vars The variables.
     * @param type The type.
     * @param templateKey The template key.
     */
    public ObsFeTypedFactor(VarSet vars, Enum<?> type, Object templateKey, ObsFeatureConjoiner cj, ObsFeatureExtractor obsFe) {
        super(vars, templateKey, cj, obsFe);
        this.type = type;
    }
    
    public Enum<?> getFactorType() {
        return type;
    }
    
}