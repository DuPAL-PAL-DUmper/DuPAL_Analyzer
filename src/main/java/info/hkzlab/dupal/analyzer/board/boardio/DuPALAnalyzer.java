package info.hkzlab.dupal.analyzer.board.boardio;

import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.hkzlab.dupal.analyzer.exceptions.*;
import info.hkzlab.dupal.analyzer.palanalisys.explorers.OSExplorer;
import info.hkzlab.dupal.analyzer.palanalisys.explorers.SimpleExplorer;
import info.hkzlab.dupal.analyzer.palanalisys.formatter.JSONFormatter;
import info.hkzlab.dupal.analyzer.palanalisys.graph.OutState;
import info.hkzlab.dupal.analyzer.palanalisys.simple.SimpleState;
import info.hkzlab.dupal.analyzer.utilities.BitUtils;

public class DuPALAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(DuPALAnalyzer.class);

    private final String outFile;

    private final DuPALCmdInterface dpci;
    private int ioAsOutMask;
    
    public DuPALAnalyzer(final DuPALCmdInterface dpci, int ioAsOutMask, final String outFile) {
        this.dpci = dpci;
        this.ioAsOutMask = ioAsOutMask;
        this.outFile = outFile;
    } 

    public int detectIOTypeMask(final DuPALCmdInterface dpci) throws DuPALBoardException {
        if(dpci.palSpecs.getPinCount_IO() == 0) {
            logger.info("detectIOTypeMask -> This PAL has no IOs");
            return 0;
        }

        int ioAsOutMask = 0;
        int maxINVal = 1 << (dpci.palSpecs.getPinCount_IN() + dpci.palSpecs.getPinCount_IO());

        logger.info("detectIOTypeMask -> Starting IO type detection... This could take a while.");
        logger.debug("detectIOTypeMask -> Highest address for input pins: " + String.format("%06X", maxINVal-1));

        for(int idx = 0; idx < maxINVal; idx++) {

            int o_write_mask = BitUtils.scatterBitField(BitUtils.consolidateBitField(ioAsOutMask, (dpci.palSpecs.getMask_IO_R())), dpci.palSpecs.getMask_IO_W());
            int writeAddr = BitUtils.scatterBitField(idx, dpci.palSpecs.getMask_IN()) | BitUtils.scatterBitField((idx >> dpci.palSpecs.getPinCount_IN()), dpci.palSpecs.getMask_IO_W());
            
            // Skip this run if we end up trying to write on an output pin
            if((writeAddr & o_write_mask) != 0) continue;

            // Try all input combinations in this state
            for(int sub_idx = 0; sub_idx < maxINVal; sub_idx++) {
                if(ioAsOutMask == dpci.palSpecs.getMask_IO_R()) return ioAsOutMask; // All the IOs we already found to be outputs, no need to continue

                int sub_o_write_mask = BitUtils.scatterBitField(BitUtils.consolidateBitField(ioAsOutMask, (dpci.palSpecs.getMask_IO_R())), dpci.palSpecs.getMask_IO_W());
                int sub_writeAddr = BitUtils.scatterBitField(sub_idx, dpci.palSpecs.getMask_IN()) | BitUtils.scatterBitField(sub_idx >> dpci.palSpecs.getPinCount_IN(), dpci.palSpecs.getMask_IO_W());
                
                if((sub_writeAddr & sub_o_write_mask) != 0) continue; // Skip this run, as we're trying to set something we found to be an output

                dpci.write(sub_writeAddr);
                int pinstat = dpci.read();

                ioAsOutMask |= (pinstat ^ BitUtils.scatterBitField((sub_idx >> dpci.palSpecs.getPinCount_IN()), dpci.palSpecs.getMask_IO_R())) & dpci.palSpecs.getMask_IO_R();
                
                logger.debug(String.format("detectIOTypeMask -> idx: C(%06X) -> S(%06X) | M[%02X]", sub_idx, sub_writeAddr, ioAsOutMask));
            }

            logger.info(String.format("detectIOTypeMask -> Currently detected mask is %02X", ioAsOutMask));

            if(dpci.palSpecs.getPinCount_RO() == 0) break; // No need to try multiple registered states

            logger.debug("detectIOTypeMask -> Pulsing clock with address " + String.format("%06X", writeAddr));
            dpci.writeAndPulseClock(writeAddr);
        }

        return ioAsOutMask;
    }
    
    public DuPALAnalyzer(final DuPALCmdInterface dpci) {
        this(dpci, -1, null);
    }

    public void startAnalisys() throws Exception {
        int board_revision = dpci.getBoardVersion();
        DuPALCmdInterface.DuPAL_LED led;
        JSONObject formatterOutput = null;
       
        switch(dpci.palSpecs.slotNumber()) {
            default:
            case 0:
                led = DuPALCmdInterface.DuPAL_LED.P20_LED;
                break;
            case 1:
                 led = DuPALCmdInterface.DuPAL_LED.P24_LED;               
                break;
        }

        if(board_revision >= 2) dpci.setLED(led, true);

        try {
            if((dpci.palSpecs.getPinCount_IO() == 0) && (dpci.palSpecs.getPinCount_RO() == 0)) { // Purely combinatorial and no feedbacks, we can perform simple bruteforcing
                SimpleState[] ssArray = SimpleExplorer.exploreStates(dpci);
                
                logger.info("Got " + ssArray.length + " output states!");

                formatterOutput = JSONFormatter.formatJSON(dpci.palSpecs, ssArray);
            } else { // Either registered, or with feedbacks
                if(ioAsOutMask < 0) {
                    ioAsOutMask = detectIOTypeMask(dpci);
                    logger.info("Detected the following IO as Outputs mask: " + String.format("%02X", ioAsOutMask));
                    logger.info("Now, turn OFF and ON again the DuPAL to reset the PAL and run this tool again by specifying the mask and output file.");

                    return;
                } else {
                    OutState[] osArray = OSExplorer.exploreOutStates(dpci, ioAsOutMask);

                    logger.info("Got " + osArray.length + " output states!");
                    formatterOutput = JSONFormatter.formatJSON(dpci.palSpecs, ioAsOutMask, osArray);
                }
            }

            if(outFile != null) saveOutputToFile(outFile, formatterOutput);
        } catch(Exception e) {
            throw e;
        } finally {
            if(board_revision >= 2) dpci.setLED(led, false);
        }
    }

    private void saveOutputToFile(String destination, JSONObject output) throws IOException {
        logger.info("saveOutputToFile() -> Saving to " + destination);

        try {
            FileWriter filew = new FileWriter(destination);

            output.write(filew);

            filew.flush();
            filew.close();

        } catch(IOException e) {
            logger.error("Error printing out the registered outputs table (not including outputs).");
            throw e;
        }
    }
}