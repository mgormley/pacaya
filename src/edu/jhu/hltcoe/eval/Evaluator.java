package edu.jhu.hltcoe.eval;

import java.io.PrintWriter;

import edu.jhu.hltcoe.model.Model;

public interface Evaluator {

    void evaluate(Model model);

    void print(PrintWriter pw);

}
