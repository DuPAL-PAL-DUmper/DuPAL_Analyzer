package info.hkzlab.dupal.analyzer.board.boardio;

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
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.hkzlab.dupal.analyzer.board.dupalproto.DuPALProto;
import info.hkzlab.dupal.analyzer.devices.*;
import info.hkzlab.dupal.analyzer.exceptions.*;
import info.hkzlab.dupal.analyzer.palanalisys.*;

public class DuPALAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(DuPALAnalyzer.class);

    private static final String SERIALIZED_DUMP = "dupalstat.dmp";
    private static final String OUT_TABLE = "dupal_thrtable.tbl";
    private static final String DUPAL_STRUCT = "dupal_struct.txt";

    private static final long SAVE_INTERVAL = 5 * 60 * 1000; // 5 minutes

    private MacroState[] mStates;

    private final DuPALManager dpm;
    private final PALSpecs pspecs;
    private final String outPath;
    private final HashMap<Integer, StateLink[]> pathMap;
    
    private final String serdump_path;
    private final String tblPath;
    private final String structPath;
    
    private int IOasOUT_Mask = -1;
    private int additionalOUTs = 0;

    private int lastUnexploredMS_idx = -1;

    public DuPALAnalyzer(final DuPALManager dpm, final PALSpecs pspecs, final int IOasOUT_Mask, final String outPath) {
        this.dpm = dpm;
        this.pspecs = pspecs;
        this.IOasOUT_Mask = IOasOUT_Mask;
        this.outPath = outPath;

        serdump_path = outPath + File.separator+ SERIALIZED_DUMP;
        tblPath = outPath + File.separator + OUT_TABLE;
        structPath = outPath + File.separator + DUPAL_STRUCT;

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

    public void startAnalisys() throws InvalidIOPinStateException, ICStateException, DuPALBoardException {
        logger.info("Device:" + pspecs + " known IOs? " + (IOasOUT_Mask >= 0 ? "Y" : "N"));

        if(IOasOUT_Mask < 0) { // We need to detect the status of the IOs...
            IOasOUT_Mask = guessIOs(); // Try to guess whether IOs are Inputs or Outputs
        }

        // Given the mask that we have recovered before, find how many additional outputs we have in this PAL
        additionalOUTs = countHIBits(IOasOUT_Mask);

        if(outPath != null) restoreStatus(serdump_path);
        internal_analisys();
        if(serdump_path != null) saveStatus(serdump_path);

        printUnvisitedMacroStates(mStates);
        printAnalisysOutput();
    }

    private void printAnalisysOutput() {
        FileOutputStream fout = null;
        
        try {
            fout = new FileOutputStream(structPath);
            printStateStructure(fout, pspecs, mStates);
            fout.close();
        } catch(IOException e) {
            logger.error("Error printing out the analisys struct.");
            e.printStackTrace();           
        }
        try {
            fout = new FileOutputStream(tblPath);
            printLogicTable(fout, pspecs, additionalOUTs, IOasOUT_Mask, mStates);
            fout.close();
        } catch(IOException e) {
            logger.error("Error printing out the registered outputs table (not including outputs).");
            e.printStackTrace();
        }
    }

    private int guessIOs() throws DuPALBoardException {
        logger.info("starting...");

        int inmask = pspecs.getINMask() | pspecs.getIO_WRITEMask(); // Get a mask for writing the INputs and the IOs (which could be inputs)

        logger.info("inmask: " + Integer.toHexString(inmask));

        /* We'll try toggling all inputs combinations in the current state,
         * Then we'll toggle the clock to try to move to another state, this for all the input combinations, again
         * 
         * We won't cover all the possible states, but we're just trying to guess which pins are output here.
         */

        int read, out_pins = 0;
        for(int idx = 0; idx <= inmask; idx+=2) { // Pin 1 (bit 0 of the idx) is the clock and we'd skip it anyway, so we'll increment by 2
            if((idx & ~inmask) != 0) continue; // We're trying to set a pin that is neither an input nor an IO

            if(out_pins == pspecs.getIO_READMask()) break; // Apparently we found that all the IOs are outputs...

            logger.info("run " + Integer.toHexString(idx >> 1) + " | inmask: 0x"+String.format("%06X", inmask)+" guessed outs: 0x" + String.format("%02X", out_pins) + " / " + Integer.toBinaryString(out_pins)+"b");

            int new_inmask, write_addr;
            for(int i_idx = 0; i_idx <= inmask; i_idx+=2) { // Now, try all the input combinations for this state
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

            // pulse the clock to try and move to a random new state
            pulseClock(idx & ~pspecs.getOEPinMask());
        }

        logger.info("end... I guessed: 0x" + Integer.toHexString(out_pins) + " / " + Integer.toBinaryString(out_pins)+"b");

        return out_pins;
    }

    private void pulseClock(int addr) throws DuPALBoardException {
        int addr_clk = (addr | pspecs.getCLKPinMask()) & ~pspecs.getOEPinMask();
        int addr_noclk = addr & ~(pspecs.getOEPinMask() | pspecs.getCLKPinMask());
        logger.debug("Pulsing clock with addr: " + Integer.toHexString(addr_clk) + " | " + Integer.toHexString(addr_noclk));
        
        try {
            writePINs(addr_noclk); // Set the address, but keep CLK pin low
            writePINs(addr_clk); // Set CLK pin high..
            writePINs(addr_noclk); // CLK pin low again
        } catch(DuPALBoardException e) {
            logger.error("Pulsing clock to get to address " + Integer.toHexString(addr_noclk) + " failed.");
            throw e;
        }
    }

    private void internal_analisys() throws InvalidIOPinStateException, ICStateException, DuPALBoardException {
        logger.info("Device: " + pspecs + " Outs: " + Integer.toBinaryString(IOasOUT_Mask | pspecs.getO_READMask())+"b");
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
                logger.debug("We moved to MacroState ["+nms+"]");
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

                    // Check that we got to the right state
                    cur_rpin_status = ((readPINs() & pspecs.getROUT_READMask()) >> pspecs.getROUT_READMaskShift());
                    if(cur_rpin_status != ms.rpin_status) {
                        logger.error("Mismatch between the current reg. out state ("+String.format("%02X", cur_rpin_status)+") and expected state ("+String.format("%02X", ms.rpin_status)+"), old reg. out state was " + String.format("%02X", old_rpin_status));
                        throw new ICStateException("Unexpected registered out state after traversing a state link path.");
                    }
                }
            }
        }
    }

    private int slPathGetHash(MacroState sms, MacroState dms) {
        return ((sms.rpin_status * 31) + dms.rpin_status);
    }

    private StateLink[] findPathToNewStateLinks(MacroState start_ms) {
        int precalc_idx = 0;

        if((lastUnexploredMS_idx >= 0) && (mStates[lastUnexploredMS_idx].link_count < start_ms.links.length)) {
            int pathHash = slPathGetHash(start_ms, mStates[lastUnexploredMS_idx]);
            if(pathMap.containsKey(pathHash)) { 
                logger.info("Trying to reach MacroState " + String.format("%02X", lastUnexploredMS_idx) + ": it has " + mStates[lastUnexploredMS_idx].link_count + " links.");
                precalc_idx = lastUnexploredMS_idx;
            }
        }

        // Search for a state that still has unexplored links
        for(int ms_idx = precalc_idx; ms_idx < mStates.length; ms_idx++) {
            if((mStates[ms_idx] != null) && (mStates[ms_idx] != start_ms)) {
                if(mStates[ms_idx].link_count < mStates[ms_idx].links.length) {
                        logger.info("Found unexplored link in ["+mStates[ms_idx]+"]");

                        lastUnexploredMS_idx = ms_idx; // Save it for faster search later

                        int path_hash = slPathGetHash(start_ms, mStates[ms_idx]);
                        StateLink[] sll = pathMap.get(Integer.valueOf(path_hash));

                        if(sll != null) {
                            if(sll[sll.length-1].destMS != mStates[ms_idx]) {
                                logger.warn("Got an hash collision trying to reach ["+mStates[ms_idx]+"] from ["+start_ms+"]");
                                lastUnexploredMS_idx = -1;
                                sll = null;
                            }
                        }

                        if(sll == null) {
                            sll = internal_searchBestPath(start_ms, mStates[ms_idx]);
                            if (sll != null) pathMap.put(Integer.valueOf(path_hash), sll);
                        }

                        if(sll != null) return sll; // Ok, we found a path
                }
            }
        }

        return null; // Finding nothing
    }

    private Map<MacroState, Integer> internal_calculateCostMap(MacroState[] states, MacroState destState) {
        HashMap<MacroState, Integer> costMap = new HashMap<>();
        Set<MacroState> msToSearch = new HashSet<>();

        Set<MacroState> searchAdd = new HashSet<>();
        Set<MacroState> searchDel = new HashSet<>();
        searchAdd.add(destState);

        costMap.put(destState, 0);

        while(searchAdd.size() > 0) {
            msToSearch.clear();
            msToSearch.addAll(searchAdd);

            searchAdd.clear();
            searchDel.clear();

            // Loop through all the states
            for(int msIdx = 0; msIdx < states.length; msIdx++) {
                if((states[msIdx] == null) || msToSearch.contains(states[msIdx])) continue;

                // Loop through all the kinks for this state
                for(int linkIdx = 0; linkIdx < states[msIdx].links.length; linkIdx++) {
                    if(states[msIdx].links[linkIdx] == null) continue; // This link was not created, skip
                    
                    if(msToSearch.contains(states[msIdx].links[linkIdx].destMS)) { // The destination is between the MacroStates we're searching
                        if(!costMap.containsKey(states[msIdx])) searchAdd.add(states[msIdx]); // If cost was not calculated for this one
                        else { // Cost already calculated. Check if we need to update it
                            int newCost = costMap.get(states[msIdx].links[linkIdx].destMS) + 1;
                            int oldCost = costMap.get(states[msIdx]);
                            if(oldCost > newCost) {
                                costMap.put(states[msIdx], newCost);
                                logger.trace("Cost updated from " + oldCost + " to " + newCost + " to reach " + states[msIdx]);
                            }
                        }

                        if(states[msIdx].links[linkIdx].destMS == destState) costMap.put(states[msIdx], 1);
                        else {
                            int cost = costMap.get(states[msIdx].links[linkIdx].destMS) + 1;
                            if(!costMap.containsKey(states[msIdx]) || (costMap.get(states[msIdx]) > cost)) costMap.put(states[msIdx], cost);
                        }
                    }
                }
            }
        }
        

        return costMap;
    }

    private StateLink[] internal_searchBestPath(MacroState start, MacroState dest) {
        logger.info("Searching for best path from ["+start+"] to ["+dest+"]");

        Stack<StateLink> slStack = new Stack<>();
        MacroState curMS = start, nextMS;
        StateLink nextSL;

        Map<MacroState, Integer> costMap = internal_calculateCostMap(mStates, dest);
        
        boolean foundLink = false;
        int cost = -1;
        while(curMS != null) {
            nextMS = null;
            nextSL = null;

            if(curMS == dest) {
                logger.trace("Arrived at " + dest);
                foundLink = true;
                break;
            } 

            for(int sl_idx = 0; sl_idx < curMS.links.length; sl_idx++) {
                if(curMS.links[sl_idx] == null) continue;

                MacroState dms = curMS.links[sl_idx].destMS;

                if(costMap.containsKey(dms) && ((cost < 0) || costMap.get(dms) < cost)) {
                    logger.trace("current cost " + cost + " dms:"+dms+" cost:"+costMap.get(dms));
                    cost = costMap.get(dms);
                    nextMS = curMS.links[sl_idx].destMS;
                    nextSL = curMS.links[sl_idx];
                }
            }

            curMS = nextMS;
            if(nextSL != null) slStack.push(nextSL);
        }

        if(foundLink && slStack.size() > 0) {
            logger.info("Path from ["+start+"] to ["+dest+"] found with cost " + slStack.size());
            return slStack.toArray(new StateLink[slStack.size()]);
        } 
        else {
            logger.info("No path found between ["+start+"] to ["+dest+"]");
            return null;
        } 
    }
/*
    private StateLink[] internal_searchPath(MacroState start, MacroState dest) {
        logger.info("Searching for a path from ["+start+"] to ["+dest+"]");

        Stack<StateLink> slStack = new Stack<>();
        Set<MacroState> msSet = new HashSet<>();
        Set<StateLink> slSet = new HashSet<>();

        MacroState curMS = start;
        msSet.add(start);

        // We'll search for a path of links that join "start" and "dest"
        while(curMS != null) {
            // The current state we're in is equal to "dest", it means we've found a path!
            // Convert the stack of links we followed into an array and return it
            if(curMS.equals(dest)) {
                StateLink[] arr = slStack.toArray(new StateLink[slStack.size()]);

                StringBuffer arrbuf = new StringBuffer();
                arrbuf.append("Found path from ["+start+"] to ["+dest+"] via:\n");
                for(StateLink sl : arr) arrbuf.append("\t"+sl.toString()+"\n");
                logger.info(arrbuf.toString());

                return arr;
            }

            // We're still not at our destination, so check every link we have not yet visited in the current macrostate,
            // and if it ends up in a state we've not yet examined, push the link into the stack, mark that state as visited 
            // and set it as the next current one to examine to search for a path
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
                    } else { // The MacroState has been visited already, which means that all its links were explored and deemed unsuitable
                        logger.debug("MacroState ["+curMS.links[idx].destMS+"] has been visited previously, so we can skip it.");
                    }
                }
            }

            // Aleady searched through all this state, we found no link to follow
            // So, if the stack of links we followed contains an element (which means we moved at least one place through the graph)
            // then pop this link from the stack and go back to the previous node, so we can continue searching for a path
            if(!foundLink) {
                if(slStack.size() > 0) {
                    slStack.pop(); // Remove the last link, the last macrostate will remain in the list of nodes we visited, so we won't search it twice
                    if(slStack.size() > 0) {
                        curMS = slStack.peek().destMS; // Back to the previous node
                    } else curMS = start; // We ended up back at the beginning
                    logger.trace("Moved back to ["+curMS+"]");
                } else { // We were already at the beginning, no possible path found
                    logger.info("Found no possible path out of [" + start + "] to [" + dest + "].");
                    return null;  // Found no possible path from start to dest
                }
            }

        }

        return null;
    }
*/
    private MacroState analyzeMacroState(MacroState ms) throws InvalidIOPinStateException, DuPALBoardException {
        if(!ms.ss_ready) {
            logger.info("Generating all ("+ms.substates.length+") possible SubStates for MacroState ["+ms+"]");
            genAllMSSubStates(ms);
        } else {
            logger.debug("SubStates already generated for MacroStates ["+ms+"]");
        }

        int idx_mask = buildInputMask();

        logger.debug("Now check if we have a new StateLink to try...");
        if(ms.link_count == ms.links.length) return null;

        // Check if we have a link to generate
        int links_counter;
        int maxidx = pspecs.getIO_WRITEMask() | pspecs.getINMask();
        while (ms.last_link_idx <= maxidx) { // Search for the next valid address
            if((ms.last_link_idx & idx_mask) != 0) { ms.last_link_idx +=2; continue; } // Skip this run
            links_counter = ms.link_count;

            logger.debug("Generating StateLink at index " + links_counter);

            pulseClock(ms.last_link_idx); // Enter the new state
            int pins = readPINs();
            int mstate_idx = (pins & pspecs.getROUT_READMask()) >> pspecs.getROUT_READMaskShift();
            MacroState nms = mStates[mstate_idx];
            StateLink sl = null;

            if(nms == null) {
                nms = new MacroState(buildTag(mstate_idx), mstate_idx, pspecs.getNumROUTPins(), (pspecs.getNumINPins() + pspecs.getNumIOPins() - additionalOUTs));
                mStates[mstate_idx] = nms;
            }
            sl = new StateLink(ms.tag, ms.last_link_idx, nms);
            ms.links[links_counter] = sl;
            ms.link_count++;

            logger.info("Connected MS '"+ms+"' with MS '"+nms+"' by SL '"+sl+"' ("+links_counter+"/"+ms.links.length+")");

            ms.last_link_idx += 2;

            return nms;
        }

        return null; // We did not move from the macrostate
    }

    /* 
     * Simply count how many bits set to high we have in the mask
     */
    private int countHIBits(int mask) {
        int count = 0;

        for(int idx = 0; idx < 32; idx++) count += ((mask >> idx) & 0x01);

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

    private SubState generateSubState(MacroState ms, int idx, int idx_mask) throws InvalidIOPinStateException,
            DuPALBoardException {
        SubState ss = null;
        int pins_1 = 0, pins_2 = 0, hiz_pins = 0;

        ArrayList<Byte> pinstate = new ArrayList<>();
        boolean[] instate = null;

        if(IOasOUT_Mask != 0) { // Some PALs have no simple outputs or no IO ports, so the following test is useless
            writePINs(idx); // Write the address to the inputs, attempt to force the current outputs to 0
            pins_1 = readPINs(); // And read back
            
            // Check that inputs really are inputs,
            // We'd expect that what we wrote to the IO pins that we consider inputs is not forced to a different level,
            // E.g. if we write an address that contains a high level into an input and we detect a low level, it means that what
            // we considered as an input, is actually behaving as an output. We probably did not detect them correctly.
            if((pins_1 & (pspecs.getIO_READMask() & ~IOasOUT_Mask)) != ((idx >> PALSpecs.READ_WRITE_SHIFT) & (pspecs.getIO_READMask() & ~IOasOUT_Mask))) {
                int extraOut = (pins_1 & (pspecs.getIO_READMask() & ~IOasOUT_Mask)) ^ ((idx >> PALSpecs.READ_WRITE_SHIFT) & (pspecs.getIO_READMask() & ~IOasOUT_Mask));
                logger.error("Detected an input that is acting as output when in MS ["+ms+"] -> expected outs: " + String.format("%02X", IOasOUT_Mask) + " actual outs: " + String.format("%02X", IOasOUT_Mask | extraOut));
                throw new InvalidIOPinStateException("Invalid IO State detected. Expected outputs: " + String.format("%02X", IOasOUT_Mask) + " detected: " + String.format("%02X", IOasOUT_Mask | extraOut), IOasOUT_Mask, IOasOUT_Mask | extraOut);
            }
            
            // Write the address to the inputs, this time try to force the outputs to 1
            writePINs(idx | (IOasOUT_Mask << PALSpecs.READ_WRITE_SHIFT));
            pins_2 = readPINs();

            // Check if forcing the outputs has create a difference in the result:
            // If a difference is detected, it means the pin is in high impedence state.
            hiz_pins = (pins_1 ^ pins_2) & IOasOUT_Mask;
        }

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

    private void genAllMSSubStates(MacroState ms) throws InvalidIOPinStateException, DuPALBoardException {
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

    private int writePINs(int addr) throws DuPALBoardException {
        int res;
        dpm.writeCommand(DuPALProto.buildWRITECommand(addr));
        res = DuPALProto.handleWRITEResponse(dpm.readResponse());

        if(res < 0) {
            logger.error("writePINs("+String.format("%08X", addr)+") -> FAILED!");
            throw new DuPALBoardException("writePINs("+String.format("%08X", addr)+") command failed!");
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
        logger.info("Printing state structure.");

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

    static private void printLogicTable(OutputStream out, PALSpecs specs, int additionalOUTs, int ioOUTMask, MacroState[] mStates) throws IOException {
        logger.info("Printing logic table. ");

        out.write(("# "+specs+" logic table\n").getBytes(StandardCharsets.US_ASCII));
        int totInputs = specs.getNumINPins() + (specs.getNumIOPins()  - additionalOUTs);
        StringBuffer strBuf = new StringBuffer();
        
        out.write((".i " + (totInputs + specs.getNumROUTPins()) + "\n").getBytes(StandardCharsets.US_ASCII));
        out.write((".o " + (specs.getNumROUTPins() + (additionalOUTs*2)) + "\n").getBytes(StandardCharsets.US_ASCII));

        // Input labels
        strBuf.delete(0, strBuf.length()); 
        strBuf.append(".ilb ");
        for(int idx = 0; idx < specs.getNumROUTPins(); idx++) strBuf.append("o_"+specs.getROUT_PinNames()[idx]+" "); // Old registerd states
        for(int idx = 0; idx < specs.getNumINPins(); idx++) strBuf.append(specs.getIN_PinNames()[idx]+" "); // Inputs
        if(totInputs > specs.getNumINPins()) { // IOs as inputs
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
        // registered outputs
        for(int idx = 0; idx < specs.getNumROUTPins(); idx++) strBuf.append(specs.getROUT_PinNames()[idx]+" ");
        for(int idx = 0; idx < 8; idx++) if(((ioOUTMask >> idx) & 0x01) > 0) strBuf.append(specs.getIO_PinNames()[idx] + " ");
        for(int idx = 0; idx < 8; idx++) if(((ioOUTMask >> idx) & 0x01) > 0) strBuf.append(specs.getIO_PinNames()[idx] + ".oe ");

        strBuf.append("\n");

        // Phase, if the chip is active low, we'll be interested in the equations that gives us the OFF-set of the truth table
        strBuf.append(".phase ");
        for(int idx = 0; idx < specs.getNumROUTPins(); idx++) strBuf.append(specs.isActiveLow() ? '0' : '1'); // REG outputs
        for(int idx = 0; idx < additionalOUTs; idx++) strBuf.append(specs.isActiveLow() ? '0' : '1'); // Outputs
        for(int idx = 0; idx < additionalOUTs; idx++) strBuf.append('1'); // OEs
        strBuf.append("\n\n");
        out.write(strBuf.toString().getBytes(StandardCharsets.US_ASCII));   
        
        // Table
        for(int ms_idx = 0; ms_idx < mStates.length; ms_idx++) {
            MacroState ms = mStates[ms_idx];
                if(ms == null) { // This state was not explored, so we're marking its outputs ad "don't care"
                    for(int fake_sl_idx = 0; fake_sl_idx < (1 << totInputs); fake_sl_idx++) {
                        strBuf.delete(0, strBuf.length());
                    
                        // Old registered outputs
                        for(int bit_idx = 0; bit_idx < specs.getNumROUTPins(); bit_idx++) {
                            strBuf.append(((ms_idx >> ((specs.getNumROUTPins() - 1) - bit_idx)) & 0x01) > 0 ? '1' : '0');
                        }
                        // Inputs
                        for(int bit_idx = 0; bit_idx < totInputs; bit_idx++) strBuf.append(((fake_sl_idx >> bit_idx) & 0x01) > 0 ? '1' : '0');

                        strBuf.append(' ');
                        
                        // Fake Outputs
                        for(int bit_idx = 0; bit_idx < specs.getNumROUTPins(); bit_idx++) strBuf.append('-');
                        
                        // Fake digital outputs + hi-z outputs
                        for(int bit_idx = 0; bit_idx < additionalOUTs; bit_idx++) strBuf.append("--");

                        strBuf.append('\n');
                        out.write(strBuf.toString().getBytes(StandardCharsets.US_ASCII));
                    }
            } else { // This state was explored
                for(int ss_idx = 0; ss_idx < ms.links.length; ss_idx++) {
                    SubState ss = ms.substates[ss_idx];
                    strBuf.delete(0, strBuf.length());

                    // Add the registered outputs as inputs
                    for(int bit_idx = 0; bit_idx < specs.getNumROUTPins(); bit_idx++) {
                        strBuf.append(((mStates[ms_idx].rpin_status >> ((specs.getNumROUTPins() - 1) - bit_idx)) & 0x01) > 0 ? '1' : '0');
                    }

                    // Add the inputs as inputs
                    for(int bit_idx = 0; bit_idx < totInputs; bit_idx++) strBuf.append(((ss_idx >> bit_idx) & 0x01) > 0 ? '1' : '0');

                    strBuf.append(' ');

                    // Add the registered outputs of the new state as outputs
                    for(int bit_idx = 0; bit_idx < specs.getNumROUTPins(); bit_idx++) {
                        strBuf.append(((mStates[ms_idx].links[ss_idx].destMS.rpin_status >> ((specs.getNumROUTPins() - 1) - bit_idx)) & 0x01) > 0 ? '1' : '0');
                    }

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
    }
}