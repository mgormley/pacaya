/**
 * 
 */
package edu.jhu.hltcoe.parse;

public enum IlpFormulation {
    DP_PROJ("deptree-dp-proj", true),
    EXPLICIT_PROJ("deptree-explicit-proj", true),
    FLOW_NONPROJ("deptree-flow-nonproj", false),
    FLOW_PROJ("deptree-flow-proj", true),
    MFLOW_NONPROJ("deptree-multiflow-nonproj", false),
    MFLOW_PROJ("deptree-multiflow-proj", true);
    
    private String id;
    private boolean isProjective;
    
    private IlpFormulation(String id, boolean isProjective) {
        this.id = id;
        this.isProjective = isProjective;
    }
    
    @Override
    public String toString() {
        return id;
    }
    
    public static IlpFormulation getById(String id) {
        for (IlpFormulation f : values()) {
            if (f.id.equals(id)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unrecognized IlpFormulation id: " + id);
    }
    
    public boolean isProjective() {
        return isProjective;
    }
}