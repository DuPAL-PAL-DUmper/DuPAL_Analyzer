package net.hkzlab.dupal.boardio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.hkzlab.devices.PALSpecs;
import net.hkzlab.dupal.dupalproto.DuPALProto;
import net.hkzlab.palanalisys.MacroState;

public class DuPALAnalyzer {
    private final Logger logger = LoggerFactory.getLogger(DuPALAnalyzer.class);

    private final MacroState[] mStates;

    private final DuPALManager dpm;
    private final PALSpecs pspecs;
    private int IOasOUT_Mask = -1;

    public DuPALAnalyzer(final DuPALManager dpm, final PALSpecs pspecs, final int IOasOUT_Mask) {
        this.dpm = dpm;
        this.pspecs = pspecs;
        this.IOasOUT_Mask = IOasOUT_Mask;

        this.mStates = new MacroState[2^pspecs.getNumROUTPins()];
    } 
    
    public DuPALAnalyzer(final DuPALManager dpm, final PALSpecs pspecs) {
        this(dpm, pspecs, -1);
    }

    public void startAnalisys() {
        logger.info("Device:" + pspecs + " known IOs? " + (IOasOUT_Mask < 0 ? "Y" : "N"));

        if(IOasOUT_Mask < 0) { // We need to detect the status of the IOs...
            IOasOUT_Mask = guessIOs(); // Try to guess whether IOs are Inputs or Outputs
        }

        internal_analisys();
    }

    private int guessIOs() {
        logger.info("starting...");

        int inmask = pspecs.getINMask() | pspecs.getIO_WRITEMask();

        logger.info("inmask: " + Integer.toHexString(inmask));

        int read, out_pins = 0;
        for(int idx = 0; idx <= inmask; idx++) {
            if((idx & ~inmask) != 0) continue; // We need to skip this round

            if(out_pins == pspecs.getIO_READMask()) break; // Apparently we found that all the IOs are outputs...

            logger.debug("run " + Integer.toHexString(idx >> 1) + " current guessed outs: 0x" + Integer.toHexString(out_pins) + " / " + Integer.toBinaryString(out_pins)+"b");

            for(int i_idx = 0; i_idx <= inmask; i_idx++) {
                if((i_idx & ~inmask) != 0) continue; // We need to skip this round
                if(out_pins == pspecs.getIO_READMask()) break; // Stop checking, we already found that all IOs are outputs...

                logger.debug("internal loop: " + (i_idx >> 1));
                
                writePINs((i_idx | pspecs.getIO_WRITEMask()) & ~(pspecs.getOEPinMask() | pspecs.getCLKPinMask()));
                read = readPINs();
                out_pins |= (read ^ pspecs.getIO_READMask()) & pspecs.getIO_READMask();
                
                writePINs(i_idx & ~(pspecs.getOEPinMask() | pspecs.getCLKPinMask() | pspecs.getIO_WRITEMask()));
                read = readPINs();
                out_pins |= ((read ^ ~pspecs.getIO_READMask())) & pspecs.getIO_READMask();
            }

            pulseClock(idx & ~pspecs.getOEPinMask());
        }

        logger.info("end... I guessed: 0x" + Integer.toHexString(out_pins) + " / " + Integer.toBinaryString(out_pins)+"b");

        return out_pins;
    }

    private void pulseClock(int addr) {
        logger.debug("Pulsing clock with addr: " + Integer.toHexString(addr));
        writePINs((addr | pspecs.getCLKPinMask()) & ~pspecs.getOEPinMask()); // Clock high,
        writePINs(addr & ~(pspecs.getOEPinMask() | pspecs.getCLKPinMask())); // Clock low
    }

    private void internal_analisys() {
        logger.info("Device: " + pspecs + " Outs: " + Integer.toBinaryString(IOasOUT_Mask)+"b");
        int pins, mstate_idx;

        writePINs(0x00); // Set the address to 0, enable the /OE pin and leave clock to low
        pins = readPINs();

        int routstate = pins & pspecs.getROUT_READMask();
        logger.info("Registered output states at start: " + Integer.toBinaryString(routstate) + "b");
        logger.info("Output states at start: " + Integer.toBinaryString(pins & IOasOUT_Mask) + "b");

        mstate_idx = routstate >> pspecs.getROUT_READMaskShift();
        MacroState ms = new MacroState(buildMSTag(mstate_idx), mstate_idx, pspecs.getNumROUTPins(), pspecs.getNumINPins());
    }

    private int readPINs() {
        dpm.writeCommand(DuPALProto.buildREADCommand());
        return DuPALProto.handleREADResponse(dpm.readResponse());
    }

    private int writePINs(int addr) {
        dpm.writeCommand(DuPALProto.buildWRITECommand(addr));
        return DuPALProto.handleWRITEResponse(dpm.readResponse());
    }

    static private String buildMSTag(int idx) {
        return "MS_"+Integer.toHexString(idx);
    }
}