package net.hkzlab.dupal.boardio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.hkzlab.devices.PALSpecs;
import net.hkzlab.dupal.dupalproto.DuPALProto;

public class DuPALAnalyzer {
    private final Logger logger = LoggerFactory.getLogger(DuPALAnalyzer.class);

    private final DuPALManager dpm;
    private final PALSpecs pspecs;
    private int IOasOUT_Mask = -1;

    public DuPALAnalyzer(final DuPALManager dpm, final PALSpecs pspecs, final int IOasOUT_Mask) {
        this.dpm = dpm;
        this.pspecs = pspecs;
        this.IOasOUT_Mask = IOasOUT_Mask;
    } 
    
    public DuPALAnalyzer(final DuPALManager dpm, final PALSpecs pspecs) {
        this(dpm, pspecs, -1);
    }

    public void startAnalisys() {
        if(IOasOUT_Mask < 0) { // We need to detect the status of the IOs...
            guessIOs(); // TODO: Try to guess whether IOs are Inputs or Outputs
        }

        // TODO: Actually perform the analisys

    }

    private int guessIOs() {
        logger.info("guessIOs() - starting...");

        int inmask = pspecs.getINMask() | pspecs.getIO_WRITEMask();

        logger.info("guessIOs() - inmask: " + Integer.toHexString(inmask));

        int read, out_pins = 0;
        for(int idx = 0; idx <= inmask; idx++) {
            if((idx & ~inmask) != 0) continue; // We need to skip this round

            if(out_pins == pspecs.getIO_READMask()) break; // Apparently we found that all the IOs are outputs...

            logger.info("guessIOs() -> run " + Integer.toHexString(idx >> 1) + " current guessed outs: 0x" + Integer.toHexString(out_pins) + " / " + Integer.toBinaryString(out_pins));

            for(int i_idx = 0; i_idx <= inmask; i_idx++) {
                if((i_idx & ~inmask) != 0) continue; // We need to skip this round
                if(out_pins == pspecs.getIO_READMask()) break;
                
                dpm.writeCommand(DuPALProto.buildWRITECommand((i_idx | pspecs.getIO_WRITEMask()) & ~(pspecs.getOEPinMask() | pspecs.getCLKPinMask() )));
                dpm.writeCommand(DuPALProto.buildREADCommand());
                read = DuPALProto.handleREADResponse(dpm.readResponse());
                out_pins |= (read ^ pspecs.getIO_READMask()) & pspecs.getIO_READMask();
                
                dpm.writeCommand(DuPALProto.buildWRITECommand(i_idx & ~(pspecs.getOEPinMask() | pspecs.getCLKPinMask() | pspecs.getIO_WRITEMask())));
                dpm.writeCommand(DuPALProto.buildREADCommand());
                read = DuPALProto.handleREADResponse(dpm.readResponse());
                out_pins |= ((read ^ ~pspecs.getIO_READMask())) & pspecs.getIO_READMask();
            }

            pulseClock(idx & ~pspecs.getOEPinMask());
        }

        logger.info("guessIOs() - end... I guessed: 0x" + Integer.toHexString(out_pins) + " / " + Integer.toBinaryString(out_pins));

        return out_pins;
    }

    private void pulseClock(int addr) {
        dpm.writeCommand(DuPALProto.buildWRITECommand((addr | pspecs.getCLKPinMask()) & ~pspecs.getOEPinMask())); // Clock high,
        dpm.writeCommand(DuPALProto.buildWRITECommand(addr & ~(pspecs.getOEPinMask() | pspecs.getCLKPinMask()))); // Clock low
    }
}