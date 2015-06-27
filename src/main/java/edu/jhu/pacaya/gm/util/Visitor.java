package edu.jhu.pacaya.gm.util;

public interface Visitor<T> {
    void visit(T t);
}