package edu.jhu.eval;

import edu.jhu.data.DepTreebank;
import edu.jhu.model.Model;

public interface Evaluator {

    void evaluate(Model model);

    void print();

    DepTreebank getParses();

}
