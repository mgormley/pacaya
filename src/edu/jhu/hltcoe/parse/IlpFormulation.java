/**
 * 
 */
package edu.jhu.hltcoe.parse;

public enum IlpFormulation {
    DP_PROJ("deptree-dp-proj", true),
    DP_PROJ_LPRELAX("deptree-dp-proj-lprelax", true, true),
    EXPLICIT_PROJ("deptree-explicit-proj", true),
    FLOW_NONPROJ("deptree-flow-nonproj", false),
    FLOW_NONPROJ_LPRELAX("deptree-flow-nonproj-lprelax", false, true),
    FLOW_PROJ("deptree-flow-proj", true),
    FLOW_PROJ_LPRELAX("deptree-flow-proj", true, true),
    MFLOW_NONPROJ("deptree-multiflow-nonproj", false),
    MFLOW_PROJ("deptree-multiflow-proj", true);
    
    private String id;
    private boolean isProjective;
    private boolean isLpRelaxation;
    
    private IlpFormulation(String id, boolean isProjective) {
        this(id, isProjective, false);
    }    
    
    private IlpFormulation(String id, boolean isProjective, boolean isLpRelaxation) {
        this.id = id;
        this.isProjective = isProjective;
        this.isLpRelaxation = isLpRelaxation;
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

    public boolean isLpRelaxation() {
        return isLpRelaxation;
    }
    
}