package edu.jhu.gm.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import edu.jhu.gm.Var;
import edu.jhu.gm.VarConfig;

public class ErmaWriter {

    public void writePredictions(File outFile, List<VarConfig> configs, Map<Var,Double> marginals) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
        writePredictions(writer, configs, marginals);
    }

    private void writePredictions(Writer writer, List<VarConfig> configs, Map<Var,Double> marginals) throws IOException {
        int i = 0;
        for (VarConfig config : configs) {
            writer.write("//example " + i + "\n");
            writer.write("example:\n");
            for (Var var : config.getVars()) {
                writer.write(var.getName());
                writer.write("=");
                writer.write(config.getStateName(var));
                writer.write(" ");
                writer.write(String.valueOf(marginals.get(var)));
            }
            i++;
        }
    }
    
}
