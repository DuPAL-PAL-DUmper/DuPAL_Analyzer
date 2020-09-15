package info.hkzlab.dupal.analyzer.palanalisys.padding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.hkzlab.dupal.analyzer.devices.PALSpecs;
import info.hkzlab.dupal.analyzer.exceptions.DuPALAnalyzerException;
import info.hkzlab.dupal.analyzer.palanalisys.graph.OutLink;
import info.hkzlab.dupal.analyzer.palanalisys.graph.OutState;
import info.hkzlab.dupal.analyzer.palanalisys.graph.OutStatePins;
import info.hkzlab.dupal.analyzer.palanalisys.graph.RegLink;
import info.hkzlab.dupal.analyzer.utilities.BitUtils;

public class OutStatePadder {
    private static final Logger logger = LoggerFactory.getLogger(OutStatePadder.class);

    private OutStatePadder() {};

    static public OutState[] padUnknownOutStates(OutState[] states, PALSpecs pSpecs, int ioAsOutMask)
            throws DuPALAnalyzerException {
        int ioAsOut_W = BitUtils.scatterBitField(BitUtils.consolidateBitField(ioAsOutMask, pSpecs.getMask_IO_R()), pSpecs.getMask_IO_W());
        ArrayList<OutState> statesList = new ArrayList<>();
        ArrayList<OutState> outList = new ArrayList<>();
        statesList.addAll(Arrays.asList(states));
        Collections.sort(statesList, new OutStateIOOutComparator(ioAsOutMask));

        OutState zeroOutState = new OutState(new OutStatePins(0, 0), 0, false);

        int curIOIdx = 0;
        for(OutState os : statesList) {
            int osIdx = BitUtils.consolidateBitField(os.pins.out, ioAsOutMask);

            for(int idx = curIOIdx; idx < osIdx; idx++) {
                OutState padOS = new OutState(new OutStatePins(BitUtils.scatterBitField(idx, ioAsOutMask), 0), os.getOutLinks().length, os.getRegLinks().length > 0);

                for(int link_idx = 0; link_idx < padOS.getOutLinks().length; link_idx++) padOS.addOutLink(new OutLink(padOS, zeroOutState, BitUtils.scatterBitField(link_idx, pSpecs.getMask_IN() | (pSpecs.getMask_IO_W() & ~ioAsOut_W))));
                for(int link_idx = 0; link_idx < padOS.getRegLinks().length; link_idx++) padOS.addRegLink(new RegLink(padOS, padOS, zeroOutState, BitUtils.scatterBitField(link_idx, pSpecs.getMask_IN() | (pSpecs.getMask_IO_W() & ~ioAsOut_W))));

                outList.add(padOS);
                logger.info("padUnknownOutStates() -> pad with state " + padOS);
            }
            outList.add(os);

            curIOIdx = osIdx+1;
        }

        return outList.toArray(new OutState[outList.size()]);
    }
}
