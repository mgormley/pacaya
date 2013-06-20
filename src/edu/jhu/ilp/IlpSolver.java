package edu.jhu.ilp;

import java.io.File;
import java.util.Map;

public interface IlpSolver {

    boolean solve(File ilpFile);

    Map<String, Double> getResult();

    double getObjective();

    String getType();
}
