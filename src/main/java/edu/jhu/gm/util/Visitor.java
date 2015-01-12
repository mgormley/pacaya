package edu.jhu.gm.util;

public interface Visitor<T> {
    void visit(T t);
}