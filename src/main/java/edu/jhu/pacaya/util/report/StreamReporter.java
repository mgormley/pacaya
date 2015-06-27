package edu.jhu.pacaya.util.report;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class StreamReporter extends Reporter {
    
    private String colSep = "\t";
    private String rowSep = "\n";
    private BufferedWriter w;
    
    protected StreamReporter(BufferedWriter w) {
        super();
        this.w = w;
    }
    
    protected StreamReporter(OutputStream out) {
        this(new BufferedWriter(new OutputStreamWriter(out)));
    }
    
    @Override
    public void report(String key, Object val) {
        try {
            w.write(key); 
            w.write(colSep);        
            w.write(val.toString());
            
            // These two fields can be ignored, but the python reader expects them. 
            w.write(colSep);
            w.write("True"); // exclude name
            w.write(colSep);
            w.write("True"); // exclude arg
            
            w.write(rowSep);
            w.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            w.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
