package edu.jhu.gm.data.erma;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.util.Utilities;

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
                writer.write(toErmaLetter(var.getType()));
                writer.write("=");
                writer.write(config.getStateName(var));
                writer.write(" ");
                if (var.getType() == VarType.OBSERVED) {
                    writer.write("1.0");
                } else {
                    writer.write(String.valueOf(Utilities.exp(marginals.get(var))));
                }
                writer.write("\n");
            }
            i++;
        }
        writer.close();
    }

    private String toErmaLetter(VarType type) {
        switch(type) {
        case OBSERVED:
            return "in";
        case LATENT:
            return "h";
        case PREDICTED:
            return "o";
        default:
            throw new RuntimeException("Unhandled type: " + type);
        }
    }
    
}
