package edu.jhu.hltcoe.ilp;

import java.io.IOException;
import java.util.Map;

/**
 * Copies the start vals for variables in mipStart into the mst file
 * produced by zimpl.
 */
public class CopyingMstFileUpdater implements MstFileUpdater {

    private Map<String, Double> mipStart;

    public CopyingMstFileUpdater(Map<String, Double> mipStart) {
        this.mipStart = mipStart;
    }

    public void updateMstFile(ZimplRunner zimplRunner) {
        try {
            // Read the TBL map so we can interpret mipStart
            Map<String, String> solverToZimpl = ZimplRunner.readTblMapToZimpl(zimplRunner.getTblFile());
            
            // Read the ZIMPL mst file
            Map<String, Double> zimplMst = MstFile.read(zimplRunner.getMstFile());
 
            for (Map.Entry<String, Double> entry : zimplMst.entrySet()) {
                Double newVal = mipStart.get(solverToZimpl.get(entry.getKey()));
                if (newVal != null) {
                    entry.setValue(newVal);
                }
            }
            
            MstFile.write(zimplMst, zimplRunner.getMstFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}