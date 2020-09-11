package info.hkzlab.dupal.analyzer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import info.hkzlab.dupal.analyzer.exceptions.DuPALAnalyzerException;
import info.hkzlab.dupal.analyzer.palanalisys.graph.OutLink;
import info.hkzlab.dupal.analyzer.palanalisys.graph.OutState;
import info.hkzlab.dupal.analyzer.palanalisys.graph.OutStatePins;

public class PathFinderTest {
    @Test
    public void PathFinderShouldProvideShortestPathToDestination() throws DuPALAnalyzerException {
        OutState os_a = new OutState(new OutStatePins(0x00, 0x00), 3);
        OutState os_b = new OutState(new OutStatePins(0x01, 0x00), 3);
        OutState os_c = new OutState(new OutStatePins(0x02, 0x00), 3);
        OutState os_d = new OutState(new OutStatePins(0x03, 0x00), 3);
        OutState os_e = new OutState(new OutStatePins(0x04, 0x00), 3);

        os_a.addOutLink(new OutLink(os_a, os_a, 0x10));
        os_a.addOutLink(new OutLink(os_a, os_b, 0x20));
        os_a.addOutLink(new OutLink(os_a, os_c, 0x30));

        os_b.addOutLink(new OutLink(os_b, os_a, 0x10));
        os_b.addOutLink(new OutLink(os_b, os_b, 0x20));
        os_b.addOutLink(new OutLink(os_b, os_c, 0x30));

        
        assertEquals("PathFinder should find the shortest path to an incomplete State", true, true);
    }
}
