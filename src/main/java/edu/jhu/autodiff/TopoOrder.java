package edu.jhu.autodiff;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;


/**
 * Topographically ordered list of modules, combined into a single module. This module will call
 * foward() in topo-order and backward() in reverse topo-order on all the modules.
 * 
 * @author mgormley
 */
public class TopoOrder extends AbstractModule<Tensor> implements Module<Tensor> {

    private List<Module<? extends ModuleTensor<?>>> topoOrder = new ArrayList<Module<? extends ModuleTensor<?>>>();
    protected Tensor y;
    protected Tensor yAdj;

    public TopoOrder() { super(null); }
    
    public void add(Module<? extends ModuleTensor<?>> m) {
        topoOrder.add(m);
        s = m.getAlgebra();
    }
    
    @Override
    public Tensor forward() {
        for (Module<? extends Object> m : topoOrder) {
            m.forward();
        }
        return getLast().getOutput();
    }

    @Override
    public void backward() {
        List<Module<? extends ModuleTensor<?>>> revTopo = Lists.reverse(topoOrder);
        for (Module<? extends ModuleTensor<?>> m : revTopo) {
            m.backward();
        }
    }

    @Override
    public List<Module<? extends ModuleTensor<?>>> getInputs() {
        List inputs = new ArrayList();
        for (Module m : topoOrder) {
            inputs.add(m);
        }
        return inputs;
    }

    @Override
    public Tensor getOutput() {
        return getLast().getOutput();
    }

    @Override
    public Tensor getOutputAdj() {
        return getLast().getOutputAdj();
    }
    
    @Override
    public void zeroOutputAdj() {
        for (Module<? extends Object> m : topoOrder) {
            m.zeroOutputAdj();
        }
    }
    
    public Module<Tensor> getLast() {
        return (Module<Tensor>)topoOrder.get(topoOrder.size()-1);
    }

    public List<Module<? extends ModuleTensor<?>>> getTopoOrder() {
        return topoOrder;
    }
    
}