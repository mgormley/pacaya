package edu.jhu.hltcoe.ilp.decomp;

import java.io.File;

public interface BlockFileWriter {

    void writeBlockFile(File blockFile, File mpsFile, File tblFile);

}
