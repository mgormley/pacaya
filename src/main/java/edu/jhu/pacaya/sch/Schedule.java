package edu.jhu.pacaya.sch;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Schedule implements Iterable<Integer> {

    // the anticipated action to perfom
    private List<Integer> actions;
    
    // the anticipated number of actions to perform before halting
    private int haltTime;
    
    public Schedule(Integer... initActions) {
        actions = Arrays.asList(initActions);
        haltTime = actions.size();
    }

    @Override
    public Iterator<Integer> iterator() {
        return actions.iterator();
    }

    public void setHaltTime(int newHalt) {
        haltTime = newHalt;
    }
    
    public int getHaltTime() {
        return haltTime;
    }
    
    public int size() {
        return actions.size();
    }

    
}
