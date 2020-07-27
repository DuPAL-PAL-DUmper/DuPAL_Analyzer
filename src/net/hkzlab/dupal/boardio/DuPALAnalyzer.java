package net.hkzlab.dupal.boardio;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.hkzlab.devices.PALSpecs;
import net.hkzlab.dupal.dupalproto.DuPALProto;
import net.hkzlab.palanalisys.MacroState;
import net.hkzlab.palanalisys.SubState;

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

        this.mStates = new MacroState[1 << pspecs.getNumROUTPins()];
        logger.info("Provisioning for " +this.mStates.length+" possible macro states");
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
        logger.info("Registered outputs at start: " + String.format("%02X", routstate));
        //logger.info("Output states at start: " + String.format("%02X", (pins & IOasOUT_Mask)));

        mstate_idx = routstate >> pspecs.getROUT_READMaskShift();
        MacroState ms = new MacroState(buildMSTag(mstate_idx), mstate_idx, pspecs.getNumROUTPins(), pspecs.getNumINPins());
        
        mStates[mstate_idx] = ms; // Save it in our Array
        logger.info("Added " + ms + " at index " + mstate_idx);

        analyzeMacroState(ms);
        // TODO: Now, we have a starting point
    }

    private boolean analyzeMacroState(MacroState ms) {
        if((ms.substates.length > 0) && (ms.substates[0] == null)) {
            logger.info("Generating all possible substates for this macro state...");
            genAllMSSubStates(ms);
        }

        int idx_mask = buildInputMask();
        int links_counter = 0;

        // Check if we have a link to generate
        for(int idx = 0; idx <= 0x387FE; idx+=2) {
            if((idx & idx_mask) != 0) continue; // Skip this run

            if(ms.links[links_counter] == null) {
                // TODO: Ok, build a link and move to the next macrostate

                return true;
            }

            links_counter++; // Keep the counter up to date
        }

        return false; // We did not move from the macrostate
    }

    private int buildInputMask() {
        return (pspecs.getROUT_WRITEMask() | pspecs.getOEPinMask() | pspecs.getCLKPinMask() | (IOasOUT_Mask << 10));
    }

    private void genAllMSSubStates(MacroState ms) {
        int idx_mask = buildInputMask();
        int pins_1, pins_2, hiz_pins;

        logger.debug("Input mask " + Integer.toBinaryString(idx_mask) + "b");

        ArrayList<Byte> pinstate = new ArrayList<>();
        ArrayList<Boolean> instate = new ArrayList<>();

        for(int idx = 0; idx <= 0x387FE; idx+=2) {
            if((idx & idx_mask) != 0) continue; // Skip this run

            logger.debug("Testing combination 0x" + Integer.toHexString(idx));

            pinstate.clear();
            instate.clear();

            writePINs(idx);
            pins_1 = readPINs();
            
            writePINs(idx | IOasOUT_Mask);
            pins_2 = readPINs();

            hiz_pins = (pins_1 ^ pins_2) & IOasOUT_Mask;

            for(int pin_idx = 0; pin_idx < 8; pin_idx++) {
                if(((IOasOUT_Mask >> pin_idx) & 0x01) == 0) continue; // Not an output pin we're interested in

                if(((hiz_pins >> pin_idx) & 0x01) > 0) pinstate.add((byte)-1);
                else if (((pins_1 >> pin_idx) & 0x01) > 0) pinstate.add((byte)1);
                else pinstate.add((byte)0);
            }

            for(int pin_idx = 0; pin_idx < 18; pin_idx++) {
                if(((idx_mask >> pin_idx) & 0x01) > 0) continue; // Output pin, not interested

                if(((idx >> pin_idx) & 0x01) > 0) instate.add(true);
                else instate.add(false);
            }
            
            logger.debug("pinstate len: " + pinstate.size() + " instate len: " + instate.size());

            Byte[] out_state = pinstate.toArray(new Byte[pinstate.size()]);
            int ss_idx = SubState.calculateSubStateIndex(instate.toArray(new Boolean[instate.size()]));
            int ss_key = SubState.calculateSubStateKey(out_state);
            SubState ss = null;
            
            logger.debug("substate index: " + ss_idx + " key: " + ss_key);

            ss = ms.ssMap.get(Integer.valueOf(ss_key));
            if(ss == null) {
                ss = new SubState(ms.tag, ms, out_state);
                ms.ssMap.put(Integer.valueOf(ss_key), ss);
            }
            ms.substates[ss_idx] = ss;
        }

        writePINs(0);
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
        return "TAG_"+Integer.toHexString(idx);
    }
}