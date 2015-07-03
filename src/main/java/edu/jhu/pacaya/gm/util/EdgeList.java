package edu.jhu.pacaya.gm.util;

import edu.jhu.prim.list.IntArrayList;

public class EdgeList {

    private IntArrayList n1List;
    private IntArrayList n2List;
    
    public EdgeList(int numEdges) {
        n1List = new IntArrayList(numEdges);
        n2List = new IntArrayList(numEdges);
    }
    
    public void addEdge(int n1, int n2) {
        n1List.add(n1);
        n2List.add(n2);
    }
    
    public int size() {
        return n1List.size();
    }
    
    public int getN1(int e) {
        return n1List.get(e);
    }
    
    public int getN2(int e) {
        return n2List.get(e);
    }
    
}
