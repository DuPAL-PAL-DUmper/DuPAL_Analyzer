package net.hkzlab.dupal.boardio;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.hkzlab.devices.PALSpecs;
import net.hkzlab.dupal.dupalproto.DuPALProto;
import net.hkzlab.palanalisys.MacroState;
import net.hkzlab.palanalisys.StateLink;
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
        MacroState ms = new MacroState(buildTag(mstate_idx), mstate_idx, pspecs.getNumROUTPins(), pspecs.getNumINPins());
        MacroState nms = null;

        mStates[mstate_idx] = ms; // Save it in our Array
        logger.info("Added " + ms + " at index " + mstate_idx);

        while(true) {
            nms = analyzeMacroState(ms);
            
            if(nms != null) {
                logger.info("We moved to state ["+nms+"]");
                ms = nms;
                nms = null;
            } else {
                logger.info("No more unknown links to follow on ["+ms+"]");
                return; // TODO: figure how to move away from this state 
            }
        }


        // TODO: Now, we have a starting point
    }

    private MacroState analyzeMacroState(MacroState ms) {
        if(!ms.ss_ready) {
            logger.info("Generating all possible substates for macro state ["+ms+"]");
            genAllMSSubStates(ms);
        } else {
            logger.info("Substates already generated for macro state ["+ms+"]");
        }

        int idx_mask = buildInputMask();
        int links_counter = 0;

        logger.info("Now check if we have a link to follow...");

        // Check if we have a link to generate
        int maxidx = pspecs.getIO_WRITEMask() | pspecs.getINMask();
        for(int idx = 0; idx <= maxidx; idx+=2) {
            if((idx & idx_mask) != 0) continue; // Skip this run

            if(ms.links[links_counter] == null) {
                logger.info("Generating link at index " + links_counter);

                pulseClock(idx); // Enter the new state
                int pins = readPINs();
                int mstate_idx = (pins & pspecs.getROUT_READMask()) >> pspecs.getROUT_READMaskShift();
                MacroState nms = mStates[mstate_idx];
                SubState ss = null;
                StateLink sl = null;

                if(nms == null) {
                    nms = new MacroState(buildTag(mstate_idx), mstate_idx, pspecs.getNumROUTPins(), pspecs.getNumINPins());
                    mStates[mstate_idx] = nms;
                }
                ss = generateSubState(nms, idx, idx_mask);
                sl = new StateLink(ms.tag, idx, writeAddrToBooleans(idx, idx_mask), ss);
                ms.links[links_counter] = sl;

                logger.info("Connected MS '"+ms+"' with SS '"+ss+"' ["+nms+"] with link '"+sl+"'");

                return nms;
            }

            links_counter++; // Keep the counter up to date
        }

        return null; // We did not move from the macrostate
    }

    private int buildInputMask() {
        return (pspecs.getROUT_WRITEMask() | pspecs.getOEPinMask() | pspecs.getCLKPinMask() | (IOasOUT_Mask << 10));
    }

    private boolean[] writeAddrToBooleans(int addr, int mask) {
        ArrayList<Boolean> instate = new ArrayList<>();
        for(int pin_idx = 0; pin_idx < 18; pin_idx++) {
           if(((mask >> pin_idx) & 0x01) > 0) continue; // Output pin, not interested

           if(((addr >> pin_idx) & 0x01) > 0) instate.add(true);
           else instate.add(false);
        }

        boolean[] barr = new boolean[instate.size()];

        for(int idx = 0; idx < barr.length; idx++) barr[idx] = instate.get(idx);
        
        return barr;
    }

    private SubState generateSubState(MacroState ms, int idx, int idx_mask) {
        SubState ss = null;
        int pins_1, pins_2, hiz_pins;

        ArrayList<Byte> pinstate = new ArrayList<>();
        boolean[] instate = null;

        writePINs(idx);
        pins_1 = readPINs();
           
        writePINs(idx | (IOasOUT_Mask << 10));
        pins_2 = readPINs();

        hiz_pins = (pins_1 ^ pins_2) & IOasOUT_Mask;

        for(int pin_idx = 0; pin_idx < 8; pin_idx++) {
            if(((IOasOUT_Mask >> pin_idx) & 0x01) == 0) continue; // Not an output pin we're interested in

            if(((hiz_pins >> pin_idx) & 0x01) > 0) pinstate.add((byte)-1);
            else if (((pins_1 >> pin_idx) & 0x01) > 0) pinstate.add((byte)1);
            else pinstate.add((byte)0);
        }

        instate = writeAddrToBooleans(idx, idx_mask);

        logger.debug("pinstate len: " + pinstate.size() + " instate len: " + instate.length);

        Byte[] out_state = pinstate.toArray(new Byte[pinstate.size()]);
        int ss_idx = SubState.calculateSubStateIndex(instate);
        int ss_key = SubState.calculateSubStateKey(out_state);
            
        logger.debug("substate index: " + ss_idx + " key: " + ss_key);

        ss = ms.ssMap.get(Integer.valueOf(ss_key));
        if(ss == null) {
            ss = new SubState(ms.tag, ms, out_state);
            ms.ssMap.put(Integer.valueOf(ss_key), ss);
        } 
        
        ms.substates[ss_idx] = ss;

        return ss;
    }

    private void genAllMSSubStates(MacroState ms) {
        int idx_mask = buildInputMask();
        logger.debug("Input mask " + Integer.toBinaryString(idx_mask) + "b");

        int maxidx = pspecs.getIO_WRITEMask() | pspecs.getINMask();
        for(int idx = 0; idx <= maxidx; idx+=2) {
            if((idx & idx_mask) != 0) continue; // Skip this run

            logger.debug("Testing combination 0x" + Integer.toHexString(idx));
            generateSubState(ms, idx, idx_mask);
        }

        ms.ss_ready = true;

        logger.info("Macrostate ["+ms+"] now has "+ms.ssMap.size()+" substates in array of size " + ms.substates.length);

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

    static private String buildTag(int idx) {
        return "TAG_"+Integer.toHexString(idx);
    }
}