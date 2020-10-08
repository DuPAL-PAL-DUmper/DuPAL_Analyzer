package info.hkzlab.dupal.analyzer.palanalisys.explorers;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.hkzlab.dupal.analyzer.board.boardio.DuPALCmdInterface;
import info.hkzlab.dupal.analyzer.devices.PALSpecs;
import info.hkzlab.dupal.analyzer.exceptions.*;
import info.hkzlab.dupal.analyzer.palanalisys.graph.*;
import info.hkzlab.dupal.analyzer.utilities.BitUtils;

public class OSExplorer {
    private static final Logger logger = LoggerFactory.getLogger(OSExplorer.class);

    private OSExplorer() {};

    public static OutState[] exploreOutStates(final DuPALCmdInterface dpci, final int ioAsOutMask) throws DuPALBoardException, DuPALAnalyzerException {
        Map<Integer, OutState> statesMap = new HashMap<>();
        PALSpecs pSpecs = dpci.palSpecs;
        int maxLinks = 1 << (pSpecs.getPinCount_IN() + (pSpecs.getPinCount_IO()-BitUtils.countBits(ioAsOutMask)));
        OutState curState;

        curState = getOutStateForIdx(dpci, 0, false, ioAsOutMask, maxLinks, statesMap);
        logger.info("exploreOutStates() -> Initial state: " + curState);

        while(curState != null) {
            // If we ended up in a state where all the links have already been explored...
            if(curState.isStateFull()) {
                logger.debug("exploreOutStates() -> " + curState + " is full.");
                GraphLink[] linkPath = PathFinder.findPathToNearestUnfilledState(curState);
                if(linkPath != null && linkPath.length > 0) {
                    for(GraphLink l : linkPath) {
                        logger.debug("exploreOutStates() -> Walking link " + l);
                        // Now let's walk the path, with proper actions depending on the type of link
                        if(l.isFarLink()) dpci.writeAndPulseClock(l.getLinkInputs()); 
                        else dpci.write(l.getLinkInputs()); 
                    }
                    curState = (OutState) (linkPath[linkPath.length-1].getDestinationState());
                    int w_link = (linkPath[linkPath.length-1]).getLinkInputs();
                    logger.debug("exploreOutStates() -> walked path to state " + curState);

                    // Do some doublechecking
                    // Extract expected outputs and actual outputs
                    int pins = (dpci.read() & (ioAsOutMask | pSpecs.getMask_O_R() | pSpecs.getMask_RO_R()));
                    int expected_pins = curState.pins.out & (pSpecs.getMask_O_R() | ioAsOutMask | pSpecs.getMask_RO_R());

                    int cur_hiz = 0;
                    int ioAsOut_W = BitUtils.scatterBitField(BitUtils.consolidateBitField(ioAsOutMask, pSpecs.getMask_IO_R()), pSpecs.getMask_IO_W()); // Generate IO as output mask for writing
                    dpci.write(w_link | pSpecs.getMask_O_W() | ioAsOut_W);
                    int read_forced = dpci.read();
                    cur_hiz = (pins ^ read_forced) & (pSpecs.getMask_O_R() | ioAsOutMask);

                    if((pins != expected_pins) || (curState.pins.hiz != cur_hiz)) {
                        logger.error("exploreOutStates() -> Mismatch in expected pins ("+String.format("E:%02X|%02X - A:%02X|%02X", expected_pins, curState.pins.hiz, pins, cur_hiz)+") after walking path to state " + curState);
                        throw new DuPALAnalyzerException("exploreOutStates() -> Mismatch in expected pins after walking to state " + curState);
                    }
                    continue; // Loop again

                } else break; // We're done: can't move to anywhere else
            }

            int nextIdx;
            OutState nOutState;

            if(!curState.isStateFullOutLinks()) { // We can build a normal OutLink
                nextIdx = curState.getNextLinkIdx();
                nOutState = getOutStateForIdx(dpci, nextIdx, false, ioAsOutMask, maxLinks, statesMap);
                int w_idx = calcolateWriteINFromIdx(nextIdx, pSpecs, ioAsOutMask);
                OutLink ol = new OutLink(curState, nOutState, w_idx);
                curState.addOutLink(ol);
            
                logger.debug("exploreOutStates() -> Creating OutLink ["+nextIdx+"/"+(maxLinks-1)+"] - " + ol);
            } else { // We'll get a RegLink
                nextIdx = curState.getNextRegLinkIdx();
                nOutState = getOutStateForIdx(dpci, nextIdx, true, ioAsOutMask, maxLinks, statesMap);
                int w_idx = calcolateWriteINFromIdx(nextIdx, pSpecs, ioAsOutMask);
                OutState middleState = null;

                // If we have no outlinks (eg for 16R8), just use the current state as middle
                if(curState.getOutLinks().length == 0) middleState = curState;
                else middleState = curState.getOutLinks()[nextIdx].dest;

                RegLink rl = new RegLink(curState, middleState, nOutState, w_idx);
                curState.addRegLink(rl);
                logger.debug("exploreOutStates() -> Creating RegLink ["+nextIdx+"/"+(maxLinks-1)+"] - " + rl);
            }


            curState = nOutState;
        }

        return statesMap.values().toArray(new OutState[statesMap.size()]);
    }

    private static int calcolateWriteINFromIdx(int idx, PALSpecs pSpecs, int ioAsOutMask) {
        int ioAsOut_W = BitUtils.scatterBitField(BitUtils.consolidateBitField(ioAsOutMask, pSpecs.getMask_IO_R()), pSpecs.getMask_IO_W());
        int w_idx = BitUtils.scatterBitField(idx, pSpecs.getMask_IN());
        w_idx |= BitUtils.scatterBitField((idx >> pSpecs.getPinCount_IN()), pSpecs.getMask_IO_W() & ~ioAsOut_W);

        return w_idx;
    }

    private static OutState getOutStateForIdx(final DuPALCmdInterface dpci, final int idx, final boolean pulseClock, final int ioAsOutMask, final int maxLinks, final Map<Integer, OutState> statesMap)
            throws DuPALBoardException, DuPALAnalyzerException {
        PALSpecs pSpecs = dpci.palSpecs;
        int ioAsOut_W = BitUtils.scatterBitField(BitUtils.consolidateBitField(ioAsOutMask, pSpecs.getMask_IO_R()), pSpecs.getMask_IO_W()); // Generate IO as output mask for writing
        int pinState_A, pinState_B;

        int w_idx = calcolateWriteINFromIdx(idx, pSpecs, ioAsOutMask);

        if(!pulseClock) { // Normal Link
            dpci.write(w_idx);
        } else { // Pulse the clock
            dpci.writeAndPulseClock(w_idx);
        }
        
        pinState_A = dpci.read();
        dpci.write(w_idx | pSpecs.getMask_O_W() | ioAsOut_W); // Try to force the outputs
        pinState_B = dpci.read();

        // Check that the IOs that we consider as inputs are actually inputs, and are not remaining set to other values (which would mean they're actually outputs)
        int io_in_r = BitUtils.consolidateBitField(pinState_A, pSpecs.getMask_IO_R() & ~ioAsOutMask);
        int io_in_w = BitUtils.consolidateBitField(w_idx, pSpecs.getMask_IO_W() & ~ioAsOut_W);
        if(io_in_r != io_in_w) {
            int newMask = BitUtils.scatterBitField(io_in_r, pSpecs.getMask_IO_R() ^ BitUtils.scatterBitField(io_in_w, pSpecs.getMask_IO_R()));
            newMask |= ioAsOutMask;

            logger.error("An IO pin marked as input is behaving as an output. New IO mask: " + String.format("%02X", newMask));
            throw new DuPALAnalyzerException("IO pin marked as input is behaving as output.");
        }

        OutStatePins osp = extractOutPinStates(pSpecs, ioAsOutMask, pinState_A, pinState_B);
        OutState os = new OutState(osp, ((pSpecs.getPinCount_O() + pSpecs.getPinCount_IO()) > 0 ? maxLinks : 0), (pSpecs.getPinCount_RO() > 0 ? maxLinks : 0));

        // Check if we already visited this state, in which case, recover that state, otherwise save the state in the map
        if(statesMap.containsKey(os.hashCode())) os = statesMap.get(os.hashCode());
        else statesMap.put(os.hashCode(), os);

        return os;
    }

    private static OutStatePins extractOutPinStates(PALSpecs pSpecs, int ioAsOutMask, int read_a, int read_b) {
        int out, hiz;

        hiz = (read_a ^ read_b) & (ioAsOutMask | pSpecs.getMask_O_R()); // Registered Outputs cannot be high impedence (controlled by /OE)
        out = (read_a & (ioAsOutMask | pSpecs.getMask_O_R() | pSpecs.getMask_RO_R())) & ~hiz;

        return new OutStatePins(out, hiz);
    }
}
