package info.hkzlab.dupal.analyzer.palanalisys.explorers;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.hkzlab.dupal.analyzer.board.boardio.DuPALCmdInterface;
import info.hkzlab.dupal.analyzer.exceptions.DuPALBoardException;
import info.hkzlab.dupal.analyzer.palanalisys.simple.SimpleState;
import info.hkzlab.dupal.analyzer.utilities.BitUtils;

public class SimpleExplorer {
    private static final Logger logger = LoggerFactory.getLogger(SimpleExplorer.class);

    private SimpleExplorer() {
    };

    public static SimpleState[] exploreStates(final DuPALCmdInterface dpci) throws DuPALBoardException {
        ArrayList<SimpleState> ssList = new ArrayList<>();
        int maxIdx = 1 << dpci.palSpecs.getPinCount_IN();

        int read_a, read_b, w_idx;
        for(int idx = 0; idx < maxIdx; idx++) {
            w_idx = BitUtils.scatterBitField(idx, dpci.palSpecs.getMask_IN());
            dpci.write(w_idx);
            read_a = dpci.read() & dpci.palSpecs.getMask_O_R();
            dpci.write(w_idx | dpci.palSpecs.getMask_O_W());
            read_b = dpci.read() & dpci.palSpecs.getMask_O_R();

            SimpleState ss = new SimpleState(w_idx, read_a, (read_a ^ read_b));
            ssList.add(ss);

            logger.debug("exploreStates() -> Generated " + ss);
        }

        return ssList.toArray(new SimpleState[ssList.size()]);
    }
}
