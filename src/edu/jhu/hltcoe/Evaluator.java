package edu.jhu.hltcoe;

import java.io.PrintWriter;

public interface Evaluator {

    void evaluate(Model model);

    void print(PrintWriter pw);

}
