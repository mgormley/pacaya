package edu.jhu.hltcoe.eval;

import edu.jhu.hltcoe.model.Model;

public interface Evaluator {

    void evaluate(Model model);

    void print();

}
