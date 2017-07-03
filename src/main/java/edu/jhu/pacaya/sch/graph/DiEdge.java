package edu.jhu.pacaya.sch.graph;

/**
 * A pair of ints that is a directed edge;
 * Is meant to essentially be Pair<Integer, Integer> but slightly less verbose
 * and possibly slightly cheaper because using primitive ints
 */
public class DiEdge implements Comparable<DiEdge> {
    private int s;
    private int t;

    public DiEdge(int s, int t) {
        this.s = s;
        this.t = t;
    }
    
    public int get1() {
        return s;
    }

    public int get2() {
        return t;
    }

    @Override
    public boolean equals(Object o) { 
        if (o instanceof DiEdge) {
            DiEdge e = (DiEdge) o;
            return s == e.s && t == e.t;
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        int result = 17;
        result = 37*result + s;
        result = 37*result + t;
        return result;
    }

    @Override
    public String toString() {
        return String.format("<%s, %s>", s,t);
    }

    /**
     * static method for slight convenience of notation in tests especially
     */
    public static DiEdge edge(int s, int t) {
        return new DiEdge(s, t);
    }

    @Override
    public int compareTo(DiEdge p) {
        return s < p.s ? -1 : (
                   s > p.s ? 1 : (
                       t < p.t ? -1 : (
                           t > p.t ? 1 : 0)));
    }

}
