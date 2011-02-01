package edu.jhu.hltcoe.eval;

import java.io.PrintWriter;

import edu.jhu.hltcoe.inference.Trainer;

public interface Evaluator {

    void evaluate(Trainer model);

    void print(PrintWriter pw);

}
