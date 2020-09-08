package info.hkzlab.dupal.analyzer.palanalisys.explorers;

import info.hkzlab.dupal.analyzer.board.boardio.DuPALCmdInterface;
import info.hkzlab.dupal.analyzer.exceptions.DuPALBoardException;

public class OSExplorer {
    private OSExplorer() {
    };

public static void exploreOutStates(final DuPALCmdInterface dpci) throws DuPALBoardException {
        dpci.write(0); // Set the status to 0
    }
}
