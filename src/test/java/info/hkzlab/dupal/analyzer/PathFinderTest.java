package info.hkzlab.dupal.analyzer;

import static org.junit.Assert.*;


import org.junit.Test;

import info.hkzlab.dupal.analyzer.exceptions.DuPALAnalyzerException;
import info.hkzlab.dupal.analyzer.palanalisys.graph.*;

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
        os_b.addOutLink(new OutLink(os_b, os_e, 0x20));
        os_b.addOutLink(new OutLink(os_b, os_d, 0x30));
        
        os_c.addOutLink(new OutLink(os_c, os_a, 0x10));
        os_c.addOutLink(new OutLink(os_c, os_b, 0x20));
        os_c.addOutLink(new OutLink(os_c, os_d, 0x30));
        
        os_d.addOutLink(new OutLink(os_d, os_c, 0x10));
        os_d.addOutLink(new OutLink(os_d, os_b, 0x20));
        os_d.addOutLink(new OutLink(os_d, os_e, 0x30));

        // 'e' is incomplete
        os_e.addOutLink(new OutLink(os_e, os_a, 0x10));
        os_e.addOutLink(new OutLink(os_e, os_d, 0x20));

        GraphLink[] path = PathFinder.findPathToNearestUnfilledState(os_a);
        GraphLink[] expectedPath = new GraphLink[] { new OutLink(os_a, os_b, 0x20), new OutLink(os_b, os_e, 0x20) }; // a->b->e

        assertArrayEquals("PathFinder should find the shortest path between a node and an incomplete one", expectedPath, path);
    }

    @Test
    public void PathFinderShouldProvideShortestPathToDestinationWithRegLinks() throws DuPALAnalyzerException {
        OutState os_a = new OutState(new OutStatePins(0x00, 0x00), 3);
        OutState os_b = new OutState(new OutStatePins(0x01, 0x00), 3);
        OutState os_c = new OutState(new OutStatePins(0x02, 0x00), 3);
        OutState os_d = new OutState(new OutStatePins(0x03, 0x00), 3);
        OutState os_e = new OutState(new OutStatePins(0x04, 0x00), 3, true);
        OutState os_f = new OutState(new OutStatePins(0x05, 0x00), 3, true);

        os_a.addOutLink(new OutLink(os_a, os_a, 0x10));
        os_a.addOutLink(new OutLink(os_a, os_b, 0x20));
        os_a.addOutLink(new OutLink(os_a, os_c, 0x30));

        os_b.addOutLink(new OutLink(os_b, os_a, 0x10));
        os_b.addOutLink(new OutLink(os_b, os_e, 0x20));
        os_b.addOutLink(new OutLink(os_b, os_d, 0x30));
        
        os_c.addOutLink(new OutLink(os_c, os_a, 0x10));
        os_c.addOutLink(new OutLink(os_c, os_b, 0x20));
        os_c.addOutLink(new OutLink(os_c, os_d, 0x30));
        
        os_d.addOutLink(new OutLink(os_d, os_c, 0x10));
        os_d.addOutLink(new OutLink(os_d, os_b, 0x20));
        os_d.addOutLink(new OutLink(os_d, os_e, 0x30));

        os_e.addOutLink(new OutLink(os_e, os_a, 0x10));
        os_e.addOutLink(new OutLink(os_e, os_d, 0x20));
        os_e.addOutLink(new OutLink(os_e, os_e, 0x20));
        os_e.addRegLink(new RegLink(os_e, os_e, os_f, 0x10));
        os_e.addRegLink(new RegLink(os_e, os_e, os_e, 0x20));
        os_e.addRegLink(new RegLink(os_e, os_e, os_e, 0x30));
        
        os_f.addOutLink(new OutLink(os_f, os_f, 0x10));
        os_f.addOutLink(new OutLink(os_f, os_f, 0x20));
        os_f.addRegLink(new RegLink(os_f, os_f, os_a, 0x20));

        GraphLink[] path = PathFinder.findPathToNearestUnfilledState(os_a);
        GraphLink[] expectedPath = new GraphLink[] { new OutLink(os_a, os_b, 0x20), new OutLink(os_b, os_e, 0x20), new RegLink(os_e, os_e, os_f, 0x10) }; // a->b->e=>f

        assertArrayEquals("PathFinder should find the shortest path between a node and an incomplete one even if regLinks are involved", expectedPath, path);
    }


    @Test
    public void PathFinderShouldProvideNoPathIfAllNodesAreComplete() throws DuPALAnalyzerException {
        OutState os_a = new OutState(new OutStatePins(0x00, 0x00), 3);
        OutState os_b = new OutState(new OutStatePins(0x01, 0x00), 3);
        OutState os_c = new OutState(new OutStatePins(0x02, 0x00), 3);
        OutState os_d = new OutState(new OutStatePins(0x03, 0x00), 3);
        OutState os_e = new OutState(new OutStatePins(0x04, 0x00), 3);

        os_a.addOutLink(new OutLink(os_a, os_a, 0x10));
        os_a.addOutLink(new OutLink(os_a, os_b, 0x20));
        os_a.addOutLink(new OutLink(os_a, os_c, 0x30));

        os_b.addOutLink(new OutLink(os_b, os_a, 0x10));
        os_b.addOutLink(new OutLink(os_b, os_e, 0x20));
        os_b.addOutLink(new OutLink(os_b, os_d, 0x30));
        
        os_c.addOutLink(new OutLink(os_c, os_a, 0x10));
        os_c.addOutLink(new OutLink(os_c, os_b, 0x20));
        os_c.addOutLink(new OutLink(os_c, os_d, 0x30));
        
        os_d.addOutLink(new OutLink(os_d, os_c, 0x10));
        os_d.addOutLink(new OutLink(os_d, os_b, 0x20));
        os_d.addOutLink(new OutLink(os_d, os_e, 0x30));

        os_e.addOutLink(new OutLink(os_e, os_a, 0x10));
        os_e.addOutLink(new OutLink(os_e, os_d, 0x20));
        os_e.addOutLink(new OutLink(os_e, os_e, 0x20));

        GraphLink[] path = PathFinder.findPathToNearestUnfilledState(os_a);
        
        assertArrayEquals("PathFinder should return null if no incomplete node exists", null, path);
    }

    @Test
    public void PathFinderShouldProvideShortestPathBetweenStates() throws DuPALAnalyzerException {
        OutState os_a = new OutState(new OutStatePins(0x00, 0x00), 3);
        OutState os_b = new OutState(new OutStatePins(0x01, 0x00), 3);
        OutState os_c = new OutState(new OutStatePins(0x02, 0x00), 3);
        OutState os_d = new OutState(new OutStatePins(0x03, 0x00), 3);
        OutState os_e = new OutState(new OutStatePins(0x04, 0x00), 3);

        os_a.addOutLink(new OutLink(os_a, os_a, 0x10));
        os_a.addOutLink(new OutLink(os_a, os_b, 0x20));
        os_a.addOutLink(new OutLink(os_a, os_c, 0x30));

        os_b.addOutLink(new OutLink(os_b, os_a, 0x10));
        os_b.addOutLink(new OutLink(os_b, os_e, 0x20));
        os_b.addOutLink(new OutLink(os_b, os_d, 0x30));
        
        os_c.addOutLink(new OutLink(os_c, os_a, 0x10));
        os_c.addOutLink(new OutLink(os_c, os_b, 0x20));
        os_c.addOutLink(new OutLink(os_c, os_d, 0x30));
        
        os_d.addOutLink(new OutLink(os_d, os_c, 0x10));
        os_d.addOutLink(new OutLink(os_d, os_b, 0x20));
        os_d.addOutLink(new OutLink(os_d, os_e, 0x30));

        os_e.addOutLink(new OutLink(os_e, os_a, 0x10));
        os_e.addOutLink(new OutLink(os_e, os_d, 0x20));
        os_e.addOutLink(new OutLink(os_e, os_e, 0x20));

        GraphLink[] path = PathFinder.findPathToState(os_b, os_c);
        
        assertEquals("PathFinder should find shortest path between two states", 2, path.length);
    }
}
