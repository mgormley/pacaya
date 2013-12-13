package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.Iterator;

@Deprecated
public class Template<X,Y> implements Iterable<Y> {
    private Y first;
    private Y second;
    private ArrayList<Y> pair;
    
    public Template(Y first, Y second) {
        this.first = first;
        this.second = second;
        this.pair = new ArrayList<Y>();
        this.pair.add(first);
        this.pair.add(second);
    }

    public Template() {
        this.first = null;
        this.second = null;
        this.pair = new ArrayList<Y>();
        this.pair.add(null);
        this.pair.add(null);
    }

    public int hashCode() {
        int hashFirst = first != null ? first.hashCode() : 0;
        int hashSecond = second != null ? second.hashCode() : 0;

        return (hashFirst + hashSecond) * hashSecond + hashFirst;
    }

    public boolean equals(Object other) {
        if (other instanceof Template) {
            Template<X,Y> otherPair = (Template<X,Y>) other;
            return 
            ((  this.first == otherPair.first ||
                ( this.first != null && otherPair.first != null &&
                  this.first.equals(otherPair.first))) &&
             (  this.second == otherPair.second ||
                ( this.second != null && otherPair.second != null &&
                  this.second.equals(otherPair.second))) );
        }

        return false;
    }

    public String toString()
    { 
           return "(" + first + ", " + second + ")"; 
    }

    public Y getFirst() {
        return first;
    }

    public void setFirst(Y first) {
        this.first = first;
    }

    public Y getSecond() {
        return second;
    }

    public void setSecond(Y second) {
        this.second = second;
    }

    @Override
    public Iterator<Y> iterator() {
        return pair.iterator();
    }

    public int getSize() {
        if (this.first != null) {
            if (this.second != null) {
                return 2;
            }
            return 1;
        }
        return 0;
    }
}
