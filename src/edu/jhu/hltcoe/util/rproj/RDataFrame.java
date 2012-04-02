package edu.jhu.hltcoe.util.rproj;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class RDataFrame {

    LinkedHashSet<String> colNames;
    List<RRow> rows;

    public RDataFrame() {
        colNames = new LinkedHashSet<String>();
        rows = new ArrayList<RRow>();
    }

    public RDataFrame(String... args) {
        for (String colName : args) {
            colNames.add(colName);
        }
    }

    public void add(RRow row) {
        updateColNames(row);
        rows.add(row);
    }

    private void updateColNames(RRow row) {
        for (String colName : row.getColNames()) {
            colNames.add(colName);
        }
    }

    public void write(Writer writer) {
        String sep = " ";
        try {
            for (String colName : colNames) {
                writer.write(colName);
                writer.write(sep);
            }
            writer.write("\n");
            for (RRow row : rows) {
                for (String colName : colNames) {
                    writer.write(row.get(colName).toString());
                    writer.write(sep);
                }
                writer.write("\n");
            }
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(baos);
        write(writer);
        byte[] bytes = baos.toByteArray();
        return new String(bytes);
    }

}
