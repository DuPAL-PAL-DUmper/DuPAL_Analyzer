package info.hkzlab.dupal.analyzer.palanalisys.explorers;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.hkzlab.dupal.analyzer.board.boardio.DuPALCmdInterface;
import info.hkzlab.dupal.analyzer.devices.PALSpecs;
import info.hkzlab.dupal.analyzer.exceptions.DuPALAnalyzerException;
import info.hkzlab.dupal.analyzer.exceptions.DuPALBoardException;
import info.hkzlab.dupal.analyzer.palanalisys.graph.GraphLink;
import info.hkzlab.dupal.analyzer.palanalisys.graph.OutLink;
import info.hkzlab.dupal.analyzer.palanalisys.graph.OutState;
import info.hkzlab.dupal.analyzer.palanalisys.graph.OutStatePins;
import info.hkzlab.dupal.analyzer.palanalisys.graph.PathFinder;
import info.hkzlab.dupal.analyzer.utilities.BitUtils;

public class OSExplorer {
    private static final Logger logger = LoggerFactory.getLogger(OSExplorer.class);

    private OSExplorer() {};

    public static OutState[] exploreOutStates(final DuPALCmdInterface dpci, final int ioAsOutMask) throws DuPALBoardException, DuPALAnalyzerException {
        Map<Integer, OutState> statesMap = new HashMap<>();
        PALSpecs pSpecs = dpci.palSpecs;
        int maxLinks = 1 << (pSpecs.getPinCount_IN() + (pSpecs.getPinCount_IO()-BitUtils.countBits(ioAsOutMask)));
        OutState curState;

        curState = getOutStateForIdx(dpci, 0, ioAsOutMask, maxLinks, statesMap);
        logger.info("exploreOutStates() -> Initial state: " + curState);

        while(curState != null) {
            // If we ended up in a state where all the links have already been explored...
            if(curState.isStateFull()) {
                logger.info("exploreOutStates() -> " + curState + " is full.");
                ArrayList<GraphLink> linkPath = PathFinder.findPathToNearestUnfilledState(curState);
                if(linkPath != null && !linkPath.isEmpty()) {
                    for(GraphLink l : linkPath) dpci.write(l.getLinkInputs()); // Walk the path to the new state
                    curState = (OutState) (linkPath.get(linkPath.size() - 1)).getDestinationState();
                    logger.info("exploreOutStates() -> walked path to state " + curState);

                    // Do some doublecheckin
                    int pins = (dpci.read() & ~curState.pins.hiz) & (pSpecs.getMask_O_R() | ioAsOutMask);
                    int expected_pins = ((curState.pins.out & ~curState.pins.hiz) & (pSpecs.getMask_O_R() | ioAsOutMask));
                    if(pins != expected_pins) {
                        logger.error("exploreOutStates() -> Mismatch in expected pins ("+String.format("E:%02X|A:%02X", pins, expected_pins)+") after walink path to state " + curState);
                        throw new DuPALAnalyzerException("exploreOutStates() -> Mismatch in expected pins after walking to state " + curState);
                    }
                    continue; // Loop again

                } else break; // We're done: can't move to anywhere else
            }

            int nextIdx = curState.getNextLinkIdx();
            OutState nOutState = getOutStateForIdx(dpci, nextIdx, ioAsOutMask, maxLinks, statesMap);

            int w_idx = calcolateWriteINFromIdx(nextIdx, pSpecs, ioAsOutMask);
            OutLink ol = new OutLink(curState, nOutState, w_idx);
            curState.addOutLink(ol);

            logger.info("Creating link ["+nextIdx+"/"+maxLinks+"] - " + ol);

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

    private static OutState getOutStateForIdx(final DuPALCmdInterface dpci, final int idx, final int ioAsOutMask, final int maxLinks, final Map<Integer, OutState> statesMap)
            throws DuPALBoardException {
        PALSpecs pSpecs = dpci.palSpecs;
        int ioAsOut_W = BitUtils.scatterBitField(BitUtils.consolidateBitField(ioAsOutMask, pSpecs.getMask_IO_R()), pSpecs.getMask_IO_W());
        int pinState_A, pinState_B;

        int w_idx = calcolateWriteINFromIdx(idx, pSpecs, ioAsOutMask);

        dpci.write(w_idx);
        pinState_A = dpci.read();
        dpci.write(w_idx | pSpecs.getMask_O_W() | ioAsOut_W); // Try to force the outputs
        pinState_B = dpci.read();

        // TODO: Check that the IOs that we consider as inputs are actually inputs, and are not remaining set to other values (which would mean they're actually outputs)

        OutStatePins osp = extractOutPinStates(pSpecs, ioAsOutMask, pinState_A, pinState_B);

        OutState os = new OutState(osp, maxLinks);

        // Check if we already visited this state, in which case, recover that state, otherwise save the state in the map
        if(statesMap.containsKey(os.hashCode())) os = statesMap.get(os.hashCode());
        else statesMap.put(os.hashCode(), os);

        return os;
    }

    private static OutStatePins extractOutPinStates(PALSpecs pSpecs, int ioAsOutMask, int read_a, int read_b) {
        int out, hiz;

        hiz = (read_a ^ read_b) & (ioAsOutMask | pSpecs.getMask_O_R());
        out = (read_a & (ioAsOutMask | pSpecs.getMask_O_R())) & ~hiz;

        return new OutStatePins(out, hiz);
    }
}
