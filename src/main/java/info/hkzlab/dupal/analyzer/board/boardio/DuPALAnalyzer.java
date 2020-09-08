package info.hkzlab.dupal.analyzer.board.boardio;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.hkzlab.dupal.analyzer.devices.*;
import info.hkzlab.dupal.analyzer.exceptions.*;
import info.hkzlab.dupal.analyzer.utilities.BitUtils;

public class DuPALAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(DuPALAnalyzer.class);

    private static final String SERIALIZED_DUMP = "dupalstat.dmp";
    private static final String OUT_TABLE = "dupal_thrtable.tbl";
    private static final String DUPAL_STRUCT = "dupal_struct.txt";

    
    private final String serdump_path;
    private final String tblPath;
    private final String structPath;

    private final DuPALCmdInterface dpci;
    private int ioAsOutMask;
    
    public DuPALAnalyzer(final DuPALManager dpm, final PALSpecs palSpecs, int ioAsOutMask, final String outPath) {
        this.dpci = new DuPALCmdInterface(dpm, palSpecs);
        this.ioAsOutMask = ioAsOutMask;

        serdump_path = outPath + File.separator+ SERIALIZED_DUMP;
        tblPath = outPath + File.separator + OUT_TABLE;
        structPath = outPath + File.separator + DUPAL_STRUCT;
    } 

    public int detectIOTypeMask(final DuPALCmdInterface dpci) throws DuPALBoardException {
        int ioAsOutMask = 0;
        int maxINVal = 1 << (dpci.palSpecs.getPinCount_IN() + dpci.palSpecs.getPinCount_IO());
        int extraOutputs = 0;

        logger.info("Starting IO type detection... This could take a while.");
        logger.info("Highest address for input pins: " + String.format("%06X", maxINVal-1));

        int writeAddr, o_write_mask;
        for(int idx = 0; idx < maxINVal; idx++) {
            for(int sub_idx = 0; sub_idx < maxINVal; sub_idx++) {
                if(ioAsOutMask == dpci.palSpecs.getMask_IO_R()) break; // All the IOs we already found to be outputs, no need to continue

                o_write_mask = BitUtils.scatterBitField(BitUtils.consolidateBitField(ioAsOutMask, (dpci.palSpecs.getMask_IO_R())), dpci.palSpecs.getMask_IO_W());
                writeAddr = BitUtils.scatterBitField(sub_idx, dpci.palSpecs.getMask_IN()) | BitUtils.scatterBitField(sub_idx >> dpci.palSpecs.getPinCount_IN(), dpci.palSpecs.getMask_IO_W());
                if((writeAddr & o_write_mask) != 0) continue;
                
                dpci.write(writeAddr);
                int pinstat = dpci.read();

                ioAsOutMask |= (pinstat ^ BitUtils.scatterBitField((sub_idx >> dpci.palSpecs.getPinCount_IN()), dpci.palSpecs.getMask_IO_R())) & dpci.palSpecs.getMask_IO_R();
                
                logger.info(String.format("idx: C(%06X) -> S(%06X) | M[%06X]", sub_idx, writeAddr, ioAsOutMask));
            }

            if(dpci.palSpecs.getPinCount_RO() == 0) break; // No need to try multiple registered states

            o_write_mask = BitUtils.scatterBitField(BitUtils.consolidateBitField(ioAsOutMask, (dpci.palSpecs.getMask_IO_R())), dpci.palSpecs.getMask_IO_W());
            writeAddr = BitUtils.scatterBitField(idx, dpci.palSpecs.getMask_IN()) | BitUtils.scatterBitField((idx >> dpci.palSpecs.getPinCount_IN()), dpci.palSpecs.getMask_IO_W());
            if((writeAddr & o_write_mask) != 0) continue;

            dpci.writeAndPulseClock(writeAddr);
        }

        return ioAsOutMask;
    }
    
    public DuPALAnalyzer(final DuPALManager dpm, final PALSpecs palSpecs) {
        this(dpm, palSpecs, -1, null);
    }

    public void startAnalisys() throws InvalidIOPinStateException, ICStateException, DuPALBoardException {
        if(ioAsOutMask < 0) {
            ioAsOutMask = detectIOTypeMask(dpci);

            logger.info("Detected the following IO Type mask: " + String.format("%06X", ioAsOutMask));
        }
    }
}