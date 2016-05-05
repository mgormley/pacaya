package edu.jhu.pacaya.sch;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Schedule implements Iterable<Integer> {

    private List<Integer> actions;
    
    public Schedule(Integer... initActions) {
        actions = Arrays.asList(initActions);
    }

    @Override
    public Iterator<Integer> iterator() {
        return actions.iterator();
    }

    public int size() {
        return actions.size();
    }

    
}
