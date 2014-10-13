package edu.jhu.gm.model.globalfac;

import java.util.List;

import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.util.collections.Lists;

/**
 * Link variable. When true it indicates that there is an edge between its
 * parent and child.
 * 
 * @author mgormley
 */
public class LinkVar extends Var {

    private static final long serialVersionUID = 1L;

    // The variable states.
    public static final int TRUE = 1;
    public static final int FALSE = 0;
    
    private static final List<String> BOOLEANS = Lists.getList("FALSE", "TRUE");
    private int parent;
    private int child;     
    
    public LinkVar(VarType type, String name, int parent, int child) {
        super(type, BOOLEANS.size(), name, BOOLEANS);
        this.parent = parent;
        this.child = child;
    }

    public int getParent() {
        return parent;
    }

    public int getChild() {
        return child;
    }

    public static String getDefaultName(int i, int j) {
        return String.format("Link_%d_%d", i, j);
    }
    
}