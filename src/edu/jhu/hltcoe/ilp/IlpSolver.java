package edu.jhu.hltcoe.ilp;

import java.io.File;
import java.util.Map;

public interface IlpSolver {

    void solve(File lpFile);

    Map<String, Double> getResult();

}
