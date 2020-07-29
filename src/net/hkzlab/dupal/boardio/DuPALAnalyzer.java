package net.hkzlab.dupal.boardio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

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
    private int additionalOUTs = 0;

    public DuPALAnalyzer(final DuPALManager dpm, final PALSpecs pspecs, final int IOasOUT_Mask) {
        this.dpm = dpm;
        this.pspecs = pspecs;
        this.IOasOUT_Mask = IOasOUT_Mask;

        this.mStates = new MacroState[1 << pspecs.getNumROUTPins()];
        logger.info("Provisioning for " +this.mStates.length+" possible MacroStates");
    } 
    
    public DuPALAnalyzer(final DuPALManager dpm, final PALSpecs pspecs) {
        this(dpm, pspecs, -1);
    }

    public void startAnalisys() {
        logger.info("Device:" + pspecs + " known IOs? " + (IOasOUT_Mask >= 0 ? "Y" : "N"));

        if(IOasOUT_Mask < 0) { // We need to detect the status of the IOs...
            IOasOUT_Mask = guessIOs(); // Try to guess whether IOs are Inputs or Outputs
        }

        additionalOUTs = calculateAdditionalOutsFromMask(IOasOUT_Mask);

        internal_analisys();
    }

    private int guessIOs() {
        logger.info("starting...");

        int inmask = pspecs.getINMask() | pspecs.getIO_WRITEMask();

        logger.info("inmask: " + Integer.toHexString(inmask));

        int read, out_pins = 0;
        for(int idx = 0; idx <= inmask; idx+=2) { // Pin 1 is the clock and we'll skip it anyway
            if((idx & ~inmask) != 0) continue; // We need to skip this round

            if(out_pins == pspecs.getIO_READMask()) break; // Apparently we found that all the IOs are outputs...

            logger.info("run " + Integer.toHexString(idx >> 1) + " | inmask: 0x"+String.format("%06X", inmask)+" guessed outs: 0x" + String.format("%02X", out_pins) + " / " + Integer.toBinaryString(out_pins)+"b");

            int new_inmask, write_addr;
            for(int i_idx = 0; i_idx <= inmask; i_idx+=2) {
                if((i_idx & ~inmask) != 0) continue; // We need to skip this round
                if(out_pins == pspecs.getIO_READMask()) break; // Stop checking, we already found that all IOs are outputs...
                
                write_addr = i_idx & ~(pspecs.getOEPinMask() | pspecs.getCLKPinMask());
                writePINs(write_addr);
                read = readPINs();
                out_pins |= (read ^ (write_addr >> 10)) & pspecs.getIO_READMask();
               
                // Check if we need to update the input mask
                new_inmask = inmask & ~(out_pins << 10);
                if(new_inmask != inmask) {
                    inmask = new_inmask;
                    logger.info("Updated input mask, now -> " + String.format("%06X", inmask) + " outs: " + String.format("%02X", out_pins));
                }
                    
                logger.debug("internal loop: " + Integer.toBinaryString(i_idx) + " outs:" + String.format("%02X", out_pins));
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

        mstate_idx = routstate >> pspecs.getROUT_READMaskShift();
        MacroState ms = null;
        MacroState nms = null;
        
        if(mStates[mstate_idx] == null) {
            ms = new MacroState(buildTag(mstate_idx), mstate_idx, pspecs.getNumROUTPins(), (pspecs.getNumINPins()  + pspecs.getNumIOPins() - additionalOUTs));
            mStates[mstate_idx] = ms;
            logger.info("Added MacroState [" + ms + "] at index " + mstate_idx);
        } else {
            ms = mStates[mstate_idx];
            logger.info("Recovered MacroState ["+ms+"] from index " + mstate_idx);
        }

        while(true) {
            if(ms == null) {
                logger.info("There are no more unknown StateLinks we can reach.");
                return;
            }

            nms = analyzeMacroState(ms);
            
            if(nms != null) {
                logger.info("We moved to MacroState ["+nms+"]");
                ms = nms;
                nms = null;
            } else {
                logger.info("No more StateLinks to generate in ["+ms+"]");

                StateLink[] slPath = findPathToNewStateLinks(ms);
                
                if(slPath == null) {
                    logger.info("Found no paths starting from ["+ms+"]");
                    ms = null;
                } else {
                    ms = slPath[slPath.length - 1].destSState.macroState; // Mark the new macro state
                    
                    logger.info("Found a path to another MacroState: ["+ms+"]");
                    // Traverse the path
                    for(StateLink sl : slPath) pulseClock(sl.raw_addr);
                }
            }
        }
    }

    private StateLink[] findPathToNewStateLinks(MacroState start_ms) {
        // Search for a state that still has unexplored links
        for(int ms_idx = 0; ms_idx < mStates.length; ms_idx++) {
            if((mStates[ms_idx] != null) && (mStates[ms_idx] != start_ms)) {
                for(int sl_idx = 0; sl_idx < mStates[ms_idx].links.length; sl_idx++) {
                    if(mStates[ms_idx].links[sl_idx] == null) { // Found an unexplored link, we need to search a path to it
                        logger.info("Found unexplored link in ["+mStates[ms_idx]+"]");
                        StateLink[] sll = internal_searchPath(start_ms, mStates[ms_idx]);
                        if(sll != null) return sll; // Ok, we found a path
                    }
                }
            }
        }

        return null; // Finding nothing
    }

    private StateLink[] internal_searchPath(MacroState start, MacroState dest) {
        logger.info("Searching from a path from ["+start+"] to ["+dest+"]");

        Stack<StateLink> slStack = new Stack<>();
        Set<MacroState> msSet = new HashSet<>();
        Set<StateLink> slSet = new HashSet<>();

        MacroState curMS = start;
        msSet.add(start);

        while(curMS != null) {
            if(curMS.equals(dest)) {
                StateLink[] arr = slStack.toArray(new StateLink[slStack.size()]);

                StringBuffer arrbuf = new StringBuffer();
                arrbuf.append("Found path from ["+start+"] to ["+dest+"] via:\n");
                for(StateLink sl : arr) arrbuf.append("\t"+sl.toString()+"\n");
                logger.info(arrbuf.toString());

                return arr;
            }

            boolean foundLink = false;
            for(int idx = 0; idx < curMS.links.length; idx++) {
                if((curMS.links[idx] != null) && !slSet.contains(curMS.links[idx])) { // We have not yet tried this link
                    slSet.add(curMS.links[idx]);
                    if(!msSet.contains(curMS.links[idx].destSState.macroState)) { // And we have not yet tried this macrostate!
                        logger.info("Moving from ["+curMS+"] to ["+curMS.links[idx].destSState.macroState+"] - via ["+curMS.links[idx]+"]");

                        slStack.push(curMS.links[idx]);
                        msSet.add(curMS.links[idx].destSState.macroState);
                        curMS = curMS.links[idx].destSState.macroState;
                        foundLink = true;
                        
                        break; // Break out of this loop
                    }
                }
            }

            // Aleady searched through all this state
            if(!foundLink) {
                if(slStack.size() > 0) {
                    msSet.remove(slStack.pop().destSState.macroState); // Remove the last link we followed and remove the macrostate from nodes we visited
                    if(slStack.size() > 0) {
                        curMS = slStack.peek().destSState.macroState; // Back to the previous node
                    } else curMS = start; // Back at the beginning it seems...
                    logger.info("Moved back to ["+curMS+"]");

                } else return null; 
                
                if(slStack.size() > 0) {

                } else return null; // Found no possible path
            }

        }

        return null;
    }

    private MacroState analyzeMacroState(MacroState ms) {
        if(!ms.ss_ready) {
            logger.info("Generating all ("+ms.substates.length+") possible SubStates for MacroState ["+ms+"]");
            genAllMSSubStates(ms);
        } else {
            logger.info("SubStates already generated for MacroStates ["+ms+"]");
        }

        int idx_mask = buildInputMask();
        int links_counter = 0;

        logger.info("Now check if we have a new StateLink to try...");

        // Check if we have a link to generate
        int maxidx = pspecs.getIO_WRITEMask() | pspecs.getINMask();
        for(int idx = 0; idx <= maxidx; idx+=2) {
            if((idx & idx_mask) != 0) continue; // Skip this run

            if(ms.links[links_counter] == null) {
                logger.info("Generating StateLink at index " + links_counter);

                pulseClock(idx); // Enter the new state
                int pins = readPINs();
                int mstate_idx = (pins & pspecs.getROUT_READMask()) >> pspecs.getROUT_READMaskShift();
                MacroState nms = mStates[mstate_idx];
                SubState ss = null;
                StateLink sl = null;

                if(nms == null) {
                    nms = new MacroState(buildTag(mstate_idx), mstate_idx, pspecs.getNumROUTPins(), (pspecs.getNumINPins() + pspecs.getNumIOPins() - additionalOUTs));
                    mStates[mstate_idx] = nms;
                }
                ss = generateSubState(nms, idx, idx_mask);
                sl = new StateLink(ms.tag, idx, writeAddrToBooleans(idx, idx_mask), ss);
                ms.links[links_counter] = sl;

                logger.info("Connected MS '"+ms+"' with SS '"+ss+"' ["+nms+"] with SL '"+sl+"'");

                return nms;
            }

            links_counter++; // Keep the counter up to date
        }

        return null; // We did not move from the macrostate
    }

    private int calculateAdditionalOutsFromMask(int mask) {
        int count = 0;

        for(int idx = 0; idx < 32; idx++) {
            count += (((mask >> idx) & 0x01) != 0) ? 1 : 0;
        }

        return count;
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
        
        // Check that inputs really are inputs
        if((pins_1 & (pspecs.getIO_READMask() & ~IOasOUT_Mask)) != ((idx >> 10) & (pspecs.getIO_READMask() & ~IOasOUT_Mask))) {
            logger.warn("Detected an input that is acting as output when in MS ["+ms+"] -> " + String.format("%02X", pins_1) + " expected outs: " + String.format("%02X", IOasOUT_Mask));
        }
        
        writePINs(idx | (IOasOUT_Mask << 10));
        pins_2 = readPINs();

        // Check that inputs really are inputs
        if((pins_2 & (pspecs.getIO_READMask() & ~IOasOUT_Mask)) != ((idx >> 10) & (pspecs.getIO_READMask() & ~IOasOUT_Mask))) {
            logger.warn("Detected an input that is acting as output when in MS ["+ms+"] -> " + String.format("%02X", pins_2) + " expected outs: " + String.format("%02X", IOasOUT_Mask));
        }

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
            
        logger.debug("SubState index: " + ss_idx + " key: " + ss_key);

        ss = ms.ssMap.get(Integer.valueOf(ss_key));
        if(ss == null) {
            ss = new SubState(ms.tag, ms, out_state);
            ms.ssMap.put(Integer.valueOf(ss_key), ss);
        } else {
            logger.debug("SubState index: " + ss_idx + " key: " +ss_key+ " was already present.");
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

        logger.debug("MacroState ["+ms+"] now has "+ms.ssMap.size()+" SubStates in array of size " + ms.substates.length);

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
        return String.format("%02X", idx);
    }
}