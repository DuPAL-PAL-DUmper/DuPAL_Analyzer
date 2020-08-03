package net.hkzlab.dupal.boardio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
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
    private static final Logger logger = LoggerFactory.getLogger(DuPALAnalyzer.class);

    private static final String SERIALIZED_DUMP = "dupalstat.dmp";
    private static final String OUT_TABLE = "dupal_outputs.tbl";
    private static final String REGOUT_TABLE = "dupal_regoutputs.tbl";

    private static final long SAVE_INTERVAL = 5 * 60 * 1000; // 5 minutes

    private MacroState[] mStates;

    private final DuPALManager dpm;
    private final PALSpecs pspecs;
    private final String outPath;
    private final HashMap<Integer, StateLink[]> pathMap;
    
    private final String serdump_path;
    private final String tblPath_out;
    private final String tblPath_regout;
    
    private int IOasOUT_Mask = -1;
    private int additionalOUTs = 0;

    public DuPALAnalyzer(final DuPALManager dpm, final PALSpecs pspecs, final int IOasOUT_Mask, final String outPath) {
        this.dpm = dpm;
        this.pspecs = pspecs;
        this.IOasOUT_Mask = IOasOUT_Mask;
        this.outPath = outPath;

        serdump_path = outPath + File.separator+ SERIALIZED_DUMP;
        tblPath_out = outPath + File.separator + OUT_TABLE;
        tblPath_regout = outPath + File.separator + REGOUT_TABLE;

        this.pathMap = new HashMap<>();
        this.mStates = new MacroState[1 << pspecs.getNumROUTPins()];
        logger.info("Provisioning for " +this.mStates.length+" possible MacroStates");
    } 
    
    public DuPALAnalyzer(final DuPALManager dpm, final PALSpecs pspecs) {
        this(dpm, pspecs, -1, null);
    }

    public void saveStatus(final String path) {
        try {
            FileOutputStream fileOut = new FileOutputStream(path);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
        
            out.writeObject(mStates);
            out.close();
            fileOut.close();

            logger.info("Saved state to " + path);
        } catch (IOException e) {
            logger.warn("Unable to save status to " + path);
            e.printStackTrace();
        }
    }

    public void restoreStatus(final String path) {
        try {
            FileInputStream fileIn = new FileInputStream(path);
            ObjectInputStream in = new ObjectInputStream(fileIn);
        
            mStates = (MacroState[])in.readObject();
            in.close();
            fileIn.close();

            logger.info("Restored state from " + path);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            logger.warn("Could not find " + path);
        } catch (IOException e) {
            logger.warn("Unable to save status to " + path);
            e.printStackTrace();
        }
    }

    public void startAnalisys() {
        logger.info("Device:" + pspecs + " known IOs? " + (IOasOUT_Mask >= 0 ? "Y" : "N"));

        if(IOasOUT_Mask < 0) { // We need to detect the status of the IOs...
            IOasOUT_Mask = guessIOs(); // Try to guess whether IOs are Inputs or Outputs
        }

        additionalOUTs = calculateAdditionalOutsFromMask(IOasOUT_Mask);

        if(outPath != null) restoreStatus(serdump_path);
        internal_analisys();
        if(serdump_path != null) saveStatus(serdump_path);

        //try { printStateStructure(System.out, pspecs, mStates); } catch(IOException e){};
        printUnvisitedMacroStates(mStates);
        printTables();
    }

    private void printTables() {
        FileOutputStream fout = null;
        
        if(additionalOUTs >= 0) {
            try {
                fout = new FileOutputStream(tblPath_out);
                printLogicTableOUTPUTS(fout, pspecs, additionalOUTs, IOasOUT_Mask, mStates);
                fout.close();
            } catch(IOException e) {
                logger.error("Error printing out the outputs table.");
                e.printStackTrace();
            }
        } else {
            logger.warn("We have 0 outputs, will not print the OUTPUTs table.");
        }
        
        try {
            fout = new FileOutputStream(tblPath_regout);
            printLogicTableREGOUTPUTS(fout, pspecs, additionalOUTs, IOasOUT_Mask, mStates);
            fout.close();
        } catch(IOException e) {
            logger.error("Error printing out the registered outputs table.");
            e.printStackTrace();
        }
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
                out_pins |= (read ^ (write_addr >>  PALSpecs.READ_WRITE_SHIFT)) & pspecs.getIO_READMask();
               
                // Check if we need to update the input mask
                new_inmask = inmask & ~(out_pins << PALSpecs.READ_WRITE_SHIFT);
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
        int addr_clk = (addr | pspecs.getCLKPinMask()) & ~pspecs.getOEPinMask();
        int addr_noclk = addr & ~(pspecs.getOEPinMask() | pspecs.getCLKPinMask());
        logger.debug("Pulsing clock with addr: " + Integer.toHexString(addr_clk) + " | " + Integer.toHexString(addr_noclk));
        writePINs(addr_noclk);
        writePINs(addr_clk);
        //try { Thread.sleep(10); } catch(InterruptedException e) {};
        writePINs(addr_noclk); // Clock low
        //try { Thread.sleep(5); } catch(InterruptedException e) {};
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

        long last_save = 0;
        while(true) {
            long now = System.currentTimeMillis();
            if(((now - last_save) >= SAVE_INTERVAL) && (serdump_path != null)) {
                saveStatus(serdump_path);
                last_save = now;
            }

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
                    int old_rpin_status, cur_rpin_status;

                    old_rpin_status = ((readPINs() & pspecs.getROUT_READMask()) >> pspecs.getROUT_READMaskShift());
                    ms = slPath[slPath.length - 1].destMS; // Mark the new macro state
                    
                    logger.info("Found a path to another MacroState: ["+ms+"]");
                    // Traverse the path
                    for(StateLink sl : slPath) {
                        logger.trace("Traversing SL -> " + sl);
                        pulseClock(sl.raw_addr);
                    }

                    // Check that we got to the right place
                    cur_rpin_status = ((readPINs() & pspecs.getROUT_READMask()) >> pspecs.getROUT_READMaskShift());
                    if(cur_rpin_status != ms.rpin_status) {
                        logger.error("Mismatch between the registered output status ("+String.format("%02X", cur_rpin_status)+") and expected status ("+String.format("%02X", ms.rpin_status)+"), old rout was " + String.format("%02X", old_rpin_status));
                        System.exit(-1);
                    }
                }
            }
        }
    }

    private StateLink[] findPathToNewStateLinks(MacroState start_ms) {
        // Search for a state that still has unexplored links
        for(int ms_idx = 0; ms_idx < mStates.length; ms_idx++) {
            if((mStates[ms_idx] != null) && (mStates[ms_idx] != start_ms)) {
                if(mStates[ms_idx].link_count < mStates[ms_idx].links.length) {
                        logger.info("Found unexplored link in ["+mStates[ms_idx]+"]");
                        int path_hash = ((start_ms.rpin_status * 31) + mStates[ms_idx].rpin_status);
                        StateLink[] sll = pathMap.get(Integer.valueOf(path_hash));

                        if(sll != null) {
                            if(sll[sll.length-1].destMS != mStates[ms_idx]) {
                                logger.warn("Got an hash collision trying to reach ["+mStates[ms_idx]+"] from ["+start_ms+"]");
                                sll = null;
                            }
                        }

                        if(sll == null) {
                            sll = internal_searchPath(start_ms, mStates[ms_idx]);
                            if (sll != null) pathMap.put(Integer.valueOf(path_hash), sll);
                        }

                        if(sll != null) return sll; // Ok, we found a path
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
                    if(!msSet.contains(curMS.links[idx].destMS)) { // And we have not yet tried this macrostate!
                        logger.debug("Moving from ["+curMS+"] to ["+curMS.links[idx].destMS+"] - via ["+curMS.links[idx]+"]");

                        slStack.push(curMS.links[idx]);
                        msSet.add(curMS.links[idx].destMS);
                        curMS = curMS.links[idx].destMS;
                        foundLink = true;
                        
                        break; // Break out of this loop
                    }
                }
            }

            // Aleady searched through all this state
            if(!foundLink) {
                if(slStack.size() > 0) {
                    msSet.remove(slStack.pop().destMS); // Remove the last link we followed and remove the macrostate from nodes we visited
                    if(slStack.size() > 0) {
                        curMS = slStack.peek().destMS; // Back to the previous node
                    } else curMS = start; // Back at the beginning it seems...
                    logger.trace("Moved back to ["+curMS+"]");

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
                StateLink sl = null;

                if(nms == null) {
                    nms = new MacroState(buildTag(mstate_idx), mstate_idx, pspecs.getNumROUTPins(), (pspecs.getNumINPins() + pspecs.getNumIOPins() - additionalOUTs));
                    mStates[mstate_idx] = nms;
                }
                sl = new StateLink(ms.tag, idx, nms);
                ms.links[links_counter] = sl;
                ms.link_count++;

                logger.info("Connected MS '"+ms+"' with MS '"+nms+"' by SL '"+sl+"'");

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
        return (pspecs.getROUT_WRITEMask() | pspecs.getOEPinMask() | pspecs.getCLKPinMask() | (IOasOUT_Mask << PALSpecs.READ_WRITE_SHIFT));
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
        if((pins_1 & (pspecs.getIO_READMask() & ~IOasOUT_Mask)) != ((idx >> PALSpecs.READ_WRITE_SHIFT) & (pspecs.getIO_READMask() & ~IOasOUT_Mask))) {
            logger.warn("Detected an input that is acting as output when in MS ["+ms+"] -> " + String.format("%02X", pins_1) + " expected outs: " + String.format("%02X", IOasOUT_Mask));
        }
        
        writePINs(idx | (IOasOUT_Mask << PALSpecs.READ_WRITE_SHIFT));
        pins_2 = readPINs();

        // Check that inputs really are inputs
        if((pins_2 & (pspecs.getIO_READMask() & ~IOasOUT_Mask)) != ((idx >> PALSpecs.READ_WRITE_SHIFT) & (pspecs.getIO_READMask() & ~IOasOUT_Mask))) {
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
        int res;
        dpm.writeCommand(DuPALProto.buildWRITECommand(addr));
        res = DuPALProto.handleWRITEResponse(dpm.readResponse());

        if(res < 0) {
            logger.error("writePINs("+String.format("%08X", addr)+") -> FAILED!");
            System.exit(-1);
        }

        return res;
    }

    static private String buildTag(int idx) {
        return String.format("%02X", idx);
    }

    static private void printUnvisitedMacroStates(MacroState[] mStates) {
        StringBuffer strBuf = new StringBuffer();

        strBuf.append("The following MacroStates were not visited:\n");
        for(int idx = 0; idx < mStates.length; idx++) {
            if(mStates[idx] == null) {
                strBuf.append("\t " + String.format("\t%02X\n", idx));
            }
        }

        strBuf.append('\n');

        logger.info(strBuf.toString());
    }

    static private void printStateStructure(OutputStream out, PALSpecs specs, MacroState[] mStates) throws IOException {
        out.write(("Printing graph structure for " + specs.toString()+"\n").getBytes(StandardCharsets.US_ASCII));
        for(int ms_idx = 0; ms_idx < mStates.length; ms_idx++) {
            if(mStates[ms_idx] == null) continue;

            out.write(("MacroState ["+mStates[ms_idx]+"]\n").getBytes(StandardCharsets.US_ASCII));
            out.write(("\tPrinting SubStates\n").getBytes(StandardCharsets.US_ASCII));
            for(int ss_idx = 0; ss_idx < mStates[ms_idx].substates.length; ss_idx++) {
                out.write(("\t\tSubState ("+ss_idx+") ["+mStates[ms_idx].substates[ss_idx]+"]\n").getBytes(StandardCharsets.US_ASCII));
            }
            out.write(("\n").getBytes(StandardCharsets.US_ASCII));

            out.write(("\tPrinting StateLinks\n").getBytes(StandardCharsets.US_ASCII));
            for(int sl_idx = 0; sl_idx < mStates[ms_idx].links.length; sl_idx++) {
                out.write(("\t\tStateLink ("+sl_idx+") ["+mStates[ms_idx].links[sl_idx]+"] -> ["+mStates[ms_idx].links[sl_idx].destMS+"]\n").getBytes(StandardCharsets.US_ASCII));
            }
            out.write(("\n").getBytes(StandardCharsets.US_ASCII));
        }
    }

    static private void printLogicTableREGOUTPUTS(OutputStream out, PALSpecs specs, int additionalOUTs, int ioOUTMask, MacroState[] mStates) throws IOException {
        logger.info("Printing logic table for registered outputs.");

        out.write(("# OUTPUT logic table\n").getBytes(StandardCharsets.US_ASCII));
        int totInputs = specs.getNumINPins() + (specs.getNumIOPins() - additionalOUTs);
        StringBuffer strBuf = new StringBuffer();

        out.write((".i " + (totInputs + specs.getNumROUTPins()) + "\n").getBytes(StandardCharsets.US_ASCII));
        out.write((".o " + specs.getNumROUTPins() + "\n").getBytes(StandardCharsets.US_ASCII));
        
        // Input labels
        strBuf.delete(0, strBuf.length()); 
        strBuf.append(".ilb ");
        for(int idx = 0; idx < specs.getNumROUTPins(); idx++) {
            strBuf.append("o_"+specs.getROUT_PinNames()[idx]+" ");
        }
        for(int idx = 0; idx < specs.getNumINPins(); idx++) {
            strBuf.append(specs.getIN_PinNames()[idx]+" ");
        }
        if(totInputs > specs.getNumINPins()) {
            int ioINMask = specs.getIO_READMask() & ~ioOUTMask;
            for(int idx = 0; idx < 8; idx++) {
                if(((ioINMask >> idx) & 0x01) > 0) strBuf.append(specs.getIO_PinNames()[idx] + " ");
            }
        }
        strBuf.append('\n');
        out.write(strBuf.toString().getBytes(StandardCharsets.US_ASCII));

        // Output labels
        strBuf.delete(0, strBuf.length());
        strBuf.append(".ob ");
        for(int idx = 0; idx < specs.getNumROUTPins(); idx++) {
            strBuf.append(specs.getROUT_PinNames()[idx]+" ");
        }
        strBuf.append("\n\n");
        out.write(strBuf.toString().getBytes(StandardCharsets.US_ASCII));

        for(int ms_idx = 0; ms_idx < mStates.length; ms_idx++) {
            MacroState ms = mStates[ms_idx];
            if(ms == null) { // This state was not explored, so we're marking its outputs ad "don't care"
                    for(int fake_sl_idx = 0; fake_sl_idx < (1 << totInputs); fake_sl_idx++) {
                        strBuf.delete(0, strBuf.length());
                    
                        // Inputs
                        for(int bit_idx = 0; bit_idx < specs.getNumROUTPins(); bit_idx++) {
                            strBuf.append(((ms_idx >> ((specs.getNumROUTPins() - 1) - bit_idx)) & 0x01) > 0 ? '1' : '0');
                        }
                        for(int bit_idx = 0; bit_idx < totInputs; bit_idx++) {
                            strBuf.append(((fake_sl_idx >> bit_idx) & 0x01) > 0 ? '1' : '0');
                        } 

                        strBuf.append(' ');
                        
                        // Outputs
                        for(int bit_idx = 0; bit_idx < specs.getNumROUTPins(); bit_idx++) {
                            strBuf.append('-');
                        }

                        strBuf.append('\n');
                        out.write(strBuf.toString().getBytes(StandardCharsets.US_ASCII));
                    }
            } else {
                for(int sl_idx = 0; sl_idx < ms.links.length; sl_idx++) {
                    strBuf.delete(0, strBuf.length());

                    // Add the registered outputs as inputs
                    for(int bit_idx = 0; bit_idx < specs.getNumROUTPins(); bit_idx++) {
                        strBuf.append(((mStates[ms_idx].rpin_status >> ((specs.getNumROUTPins() - 1) - bit_idx)) & 0x01) > 0 ? '1' : '0');
                    }

                    // Add the inputs as inputs
                    for(int bit_idx = 0; bit_idx < totInputs; bit_idx++) {
                        strBuf.append(((sl_idx >> bit_idx) & 0x01) > 0 ? '1' : '0');
                    }

                    strBuf.append(' ');

                    // Add the registered outputs of the new state as outputs
                    for(int bit_idx = 0; bit_idx < specs.getNumROUTPins(); bit_idx++) {
                        strBuf.append(((mStates[ms_idx].links[sl_idx].destMS.rpin_status >> ((specs.getNumROUTPins() - 1) - bit_idx)) & 0x01) > 0 ? '1' : '0');
                    }

                    strBuf.append('\n');
                    out.write(strBuf.toString().getBytes(StandardCharsets.US_ASCII));
                }
            }
        }

        out.write(("\n.e\n").getBytes(StandardCharsets.US_ASCII));
    }

    /*
     * This method will write out a table with all the OUTPUTS states, to be minimized
     * Inputs for this table will be
     * - Inputs pins (both normal inputs and IOs as inputs)
     * - Registered outputs current status (they act as a feed for the output)
     * 
     * Outputs for this table will be
     * - Outputs (non-registered) as binary outputs. In case they're hi-z, we'll set the state as ignore ('-')
     * - OE status for each output
     */
    static private void printLogicTableOUTPUTS(OutputStream out, PALSpecs specs, int additionalOUTs, int ioOUTMask, MacroState[] mStates) throws IOException {
        logger.info("Printing logic table for normal outputs.");
        
        out.write(("# OUTPUT logic table\n").getBytes(StandardCharsets.US_ASCII));
        int totInputs = specs.getNumINPins() + (specs.getNumIOPins() - additionalOUTs);
        StringBuffer strBuf = new StringBuffer();

        out.write((".i " + (totInputs + specs.getNumROUTPins()) + "\n").getBytes(StandardCharsets.US_ASCII));
        out.write((".o " + (additionalOUTs*2) + "\n").getBytes(StandardCharsets.US_ASCII)); // * 2 because we get both an output and its OE state
       
        // Input labels
        strBuf.delete(0, strBuf.length()); 
        strBuf.append(".ilb ");
        for(int idx = 0; idx < specs.getNumROUTPins(); idx++) {
            strBuf.append(specs.getROUT_PinNames()[idx]+" ");
        }
        for(int idx = 0; idx < specs.getNumINPins(); idx++) {
            strBuf.append(specs.getIN_PinNames()[idx]+" ");
        }
        if(totInputs > specs.getNumINPins()) {
            int ioINMask = specs.getIO_READMask() & ~ioOUTMask;
            for(int idx = 0; idx < 8; idx++) {
                if(((ioINMask >> idx) & 0x01) > 0) strBuf.append(specs.getIO_PinNames()[idx] + " ");
            }
        }
        strBuf.append('\n');
        out.write(strBuf.toString().getBytes(StandardCharsets.US_ASCII));

        // Output labels
        strBuf.delete(0, strBuf.length());
        strBuf.append(".ob ");
        for(int idx = 0; idx < 8; idx++) {
            if(((ioOUTMask >> idx) & 0x01) > 0) strBuf.append(specs.getIO_PinNames()[idx] + " ");
        }
        for(int idx = 0; idx < 8; idx++) {
            if(((ioOUTMask >> idx) & 0x01) > 0) strBuf.append(specs.getIO_PinNames()[idx] + ".oe ");
        }
        strBuf.append("\n\n");

        out.write(strBuf.toString().getBytes(StandardCharsets.US_ASCII));

        for(int ms_idx = 0; ms_idx < mStates.length; ms_idx++) {
            MacroState ms = mStates[ms_idx];
            if(ms == null) { // This state was not visited, so we're generating the outputs as "don't care"
                for(int fake_ss_idx = 0; fake_ss_idx < (1<<totInputs); fake_ss_idx++) {
                    strBuf.delete(0, strBuf.length());
                
                    // Set what the inputs would be
                    for(int bit_idx = 0; bit_idx < specs.getNumROUTPins(); bit_idx++) {
                        strBuf.append(((ms_idx >> ((specs.getNumROUTPins() - 1) - bit_idx)) & 0x01) > 0 ? '1' : '0');
                    }
                
                    for(int bit_idx = 0; bit_idx < totInputs; bit_idx++) {
                        strBuf.append(((fake_ss_idx >> bit_idx) & 0x01) > 0 ? '1' : '0');
                    }
                
                    strBuf.append(' ');
               
                    // Fake digital outputs
                    for(int bit_idx = 0; bit_idx < additionalOUTs; bit_idx++) {
                        strBuf.append('-');
                    }

                    // Fake hi-z outputs
                    for(int bit_idx = 0; bit_idx < additionalOUTs; bit_idx++) {
                        strBuf.append('-');
                    }
                    
                    strBuf.append('\n');
                    out.write(strBuf.toString().getBytes(StandardCharsets.US_ASCII));
                }
            } else {
                for(int ss_idx = 0; ss_idx < ms.substates.length; ss_idx++) {
                    strBuf.delete(0, strBuf.length());
                    SubState ss = ms.substates[ss_idx];

                    // Add the registered outputs as inputs
                    for(int bit_idx = 0; bit_idx < specs.getNumROUTPins(); bit_idx++) {
                        strBuf.append(((mStates[ms_idx].rpin_status >> ((specs.getNumROUTPins() - 1) - bit_idx)) & 0x01) > 0 ? '1' : '0');
                    }

                    // Add the inputs as inputs
                    for(int bit_idx = 0; bit_idx < totInputs; bit_idx++) {
                        strBuf.append(((ss_idx >> bit_idx) & 0x01) > 0 ? '1' : '0');
                    }

                    strBuf.append(' ');

                    // Add the digital outputs as outputs
                    for(int bit_idx = 0; bit_idx < ss.pin_status.length; bit_idx++) {
                        if(ss.pin_status[bit_idx] == 0) strBuf.append('0');
                        else if (ss.pin_status[bit_idx] > 0) strBuf.append('1');
                        else strBuf.append('-');
                    }
                    
                    // Add the hi-z state as output
                    for(int bit_idx = 0; bit_idx < ss.pin_status.length; bit_idx++) {
                        if(ss.pin_status[bit_idx] >= 0) strBuf.append('1');
                        else strBuf.append('0');
                    }

                    strBuf.append('\n');
                    out.write(strBuf.toString().getBytes(StandardCharsets.US_ASCII));
                }
            }
        }

        out.write(("\n.e\n").getBytes(StandardCharsets.US_ASCII));
    }
}