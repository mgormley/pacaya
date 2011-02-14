package edu.jhu.hltcoe.ilp;

import java.util.Map;

public interface IlpSolver {

    void solve();

    Map<String, String> getResult();

}
