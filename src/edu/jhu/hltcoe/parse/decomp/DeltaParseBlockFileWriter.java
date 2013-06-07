package edu.jhu.hltcoe.parse.decomp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.ilp.ZimplRunner;
import edu.jhu.hltcoe.ilp.decomp.BlockFileWriter;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.IlpDepParser;
import edu.jhu.hltcoe.util.Files;
import edu.jhu.hltcoe.util.Utilities;

public class DeltaParseBlockFileWriter implements BlockFileWriter {

    private static final Logger log = Logger.getLogger(DeltaParseBlockFileWriter.class);

    private final Pattern spaceRegex = Pattern.compile("\\s+");
    
    public DeltaParseBlockFileWriter(IlpFormulation ilpFormulation) {
        if (!(ilpFormulation == IlpFormulation.FLOW_NONPROJ || ilpFormulation == IlpFormulation.FLOW_PROJ)) {
            throw new IllegalStateException("Unsupported ILP formulation: " + ilpFormulation);
        }
    }

    @Override
    public void writeBlockFile(File blockFile, File mpsFile, File tblFile) {
        try {
            Map<String, String> varTblMap = ZimplRunner.readTblMapToZimpl(tblFile, "v");
            Map<String, String> rowTblMap = ZimplRunner.readTblMapToZimpl(tblFile, "c");
            
            FileWriter writer = new FileWriter(blockFile);
            BufferedReader reader = new BufferedReader(new FileReader(mpsFile));

            String line;

            // Read ROWS in order into an ArrayList
            ArrayList<String> rows = new ArrayList<String>();
            Files.readUntil(reader, "ROWS");
            // By default the first "N" row defined in the ROWS section becomes a problem's objective.
            // This row is NOT counted when computing the row indices, so we read and skip it.
            line = reader.readLine();
            assert(line.contains("N  OBJECTIV"));
            while ((line = reader.readLine()) != null) {
                if (line.equals("COLUMNS")) {
                    break;
                }
                String[] splits = spaceRegex.split(line);
                String row = ZimplRunner.safeMap(rowTblMap, splits[2]);
                log.trace(String.format("Row mapping: r(%d) -> %s -> %s", rows.size(), splits[2], row));
                rows.add(row);
            }

            // Read COLUMNS (variable, rowname, value) into a map from row-name
            // to a set of integers (representing sentences). Also create a set of 
            // coupling constraints.
            Map<String,Set<Integer>> rowsToSents = new HashMap<String,Set<Integer>>();
            Set<String> couplingRows = new HashSet<String>();
            
            String prevColVar = null;
            int colNum = 0;
            while ((line = reader.readLine()) != null) {
                if (line.contains("'MARKER'")) {
                    // Skip header rows " MARK0000  'MARKER'                 'INTORG'"
                    continue;
                }

                if (line.equals("RHS")) {
                    break;
                }
                String[] lineSplits = spaceRegex.split(line);
                String colVar = lineSplits[1];
                String zimplVar = varTblMap.get(colVar);
                String row = ZimplRunner.safeMap(rowTblMap, lineSplits[2]);
                                
                if (!colVar.equals(prevColVar)) {
                    log.trace(String.format("Col mapping: x(%d) -> %s -> %s", colNum, colVar, zimplVar));
                    colNum++;
                }
                prevColVar = colVar;
                
                log.trace(String.format("x(%d) %s\t%s r(%d)", colNum, zimplVar, row, rows.indexOf(row)));
                
                if (zimplVar == null) {
                    log.error("Could not find zimpl var for: " + colVar);
                    log.debug("Line Splits: " + Arrays.toString(lineSplits));
                }
                
                String[] varSplits = IlpDepParser.zimplVarRegex.split(zimplVar);
                if (varSplits.length <= 1) {
                    log.debug("Var Splits: " + Arrays.toString(varSplits));
                    log.debug("Line: " + line);
                }

                String varType = varSplits[0];
                if (!varType.equals("cwDelta") && varSplits.length > 1) {
                    int sentId = Integer.parseInt(varSplits[1]);
                    Utilities.addToSet(rowsToSents, row, sentId);
                } else {
                    couplingRows.add(row);
                }
            }

            // Loop through each row in the array list, look up the row in the
            // map and if it contains a single sentence print out the block number
            // for that row as the sentence number.
            // TODO: also explicitly disallow rows of certain types (e.g. sum to one, conjunction constraints)
            for (int i=0; i<rows.size(); i++) {
                String row = rows.get(i);
                if (couplingRows.contains(row)) {
                    // Do not include the coupling constraints in the blocks
                    continue;
                }
                Set<Integer> sentIds = rowsToSents.get(row);
                if (sentIds == null) {
                    throw new RuntimeException("empty row: " + row);
                }
                if (sentIds.size() > 1) {
                    // This should only fire on the objective function
                    if (!row.equals("OBJECTIV")) {
                        log.warn("Found a row with multiple sentences: " + row);
                    }
                    continue;
                }
                assert(sentIds.size() == 1);
                int sentId = sentIds.iterator().next();
                log.trace("row(" + i + "): " + row + " sentId: " + sentId);
                writer.write(String.format("%d %d\n", sentId, i)); 
            }            
            
            reader.close();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
