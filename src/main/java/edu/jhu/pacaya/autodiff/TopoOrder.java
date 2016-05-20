package edu.jhu.pacaya.autodiff;

import java.util.HashSet;
import java.util.List;

import edu.jhu.pacaya.autodiff.Toposort.Deps;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.pacaya.util.semiring.Algebra;


/**
 * Topographically ordered list of modules, combined into a single module. This module will call
 * foward() in topo-order and backward() in reverse topo-order on all the modules.
 * 
 * @author mgormley
 */
public class TopoOrder<Y extends MVec> implements Module<Y> {

    private List<? extends Module<?>> inputs;
    private Module<Y> outMod;
    private List<? extends Module<?>> topoOrder;
    private String name;
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public TopoOrder(Module<Y> root) {
        this.inputs = QLists.getList();
        this.outMod = root;
        Deps deps = getModuleDeps();
        this.topoOrder = Toposort.toposort(root, deps);
    }
    
    public TopoOrder(List<? extends Module<?>> inputsList, Module<Y> root) {
        this(inputsList, root, null);
    }
        
    public TopoOrder(List<? extends Module<?>> inputsList, Module<Y> root, String name) {
        this(inputsList, root, name, true);
    }

    /**
     * Detailed constructor.
     * 
     * @param inputsList Leaf modules that are excluded along with their descendents
     * @param root Root module
     * @param name Name of this module
     * @param checkFullCut Whether to check that inputs form a full cut through the DAG below the root
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public TopoOrder(List<? extends Module<?>> inputsList, Module<Y> root, String name, boolean checkFullCut) {
        HashSet inputs = new HashSet(inputsList);
        if (inputs.size() != inputsList.size()) {
            throw new IllegalStateException("Multiple copies of module in inputs list: " + inputsList);
        }
        this.inputs = inputsList;
        this.outMod = root;
        this.name = name;
        
        Deps deps = getModuleDeps();
        this.topoOrder = Toposort.toposort(inputs, root, deps, checkFullCut);
    }
    
    protected TopoOrder() { }
    
    protected void shallowCopy(TopoOrder<Y> other) {
        this.inputs = other.inputs;
        this.outMod = other.outMod;
        this.name = other.name;
        this.topoOrder = other.topoOrder;
    }
    
    @SuppressWarnings({ "rawtypes" })
    private static Deps getModuleDeps() {
        Deps deps = new Deps() {
            @Override
            public List getDeps(Object x) {
                return ((Module) x).getInputs();
            }
        };
        return deps;
    }

    @Override
    public Y forward() {
        for (Module<? extends Object> m : topoOrder) {
            m.forward();
        }
        return outMod.getOutput();
    }

    @Override
    public void backward() {
        List<? extends Module<?>> revTopo = QLists.reverse(topoOrder);
        for (Module<?> m : revTopo) {
            m.backward();
        }
    }

    @Override
    public List<? extends Module<?>> getInputs() {
        return inputs;
    }

    @Override
    public Y getOutput() {
        return outMod.getOutput();
    }

    @Override
    public Y getOutputAdj() {
        return outMod.getOutputAdj();
    }
    
    @Override
    public void zeroOutputAdj() {
        for (Module<? extends Object> m : topoOrder) {
            m.zeroOutputAdj();
        }
    }
    
    public List<? extends Module<?>> getTopoOrder() {
        return topoOrder;
    }

    @Override
    public Algebra getAlgebra() {
        return outMod.getAlgebra();
    }

    @Override
    public String toString() {
        return "TopoOrder [name=" + name + ", inputs=" + inputs + ", outMod=" + outMod + ", topoOrder=" + topoOrder
                + "]";
    }
    
}