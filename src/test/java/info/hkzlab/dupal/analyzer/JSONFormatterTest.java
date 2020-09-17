package info.hkzlab.dupal.analyzer;

import static org.junit.Assert.*;


import org.junit.Test;

import info.hkzlab.dupal.analyzer.devices.PAL16L8Specs;
import info.hkzlab.dupal.analyzer.exceptions.DuPALAnalyzerException;
import info.hkzlab.dupal.analyzer.palanalisys.formatter.JSONFormatter;
import info.hkzlab.dupal.analyzer.palanalisys.graph.*;

public class JSONFormatterTest {
    @Test
    public void JSONFormatterShouldCreateCorrectJSON() throws DuPALAnalyzerException {
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

        OutState[] oStates = new OutState[] {os_a, os_b, os_c, os_d, os_e};

        String expectedJson = "{\"oLinks\":[{\"inputs\":16,\"destination\":{\"outputs\":0,\"hiz\":0},\"source\":{\"outputs\":0,\"hiz\":0}},{\"inputs\":32,\"destination\":{\"outputs\":1,\"hiz\":0},\"source\":{\"outputs\":0,\"hiz\":0}},{\"inputs\":48,\"destination\":{\"outputs\":2,\"hiz\":0},\"source\":{\"outputs\":0,\"hiz\":0}},{\"inputs\":16,\"destination\":{\"outputs\":0,\"hiz\":0},\"source\":{\"outputs\":1,\"hiz\":0}},{\"inputs\":32,\"destination\":{\"outputs\":4,\"hiz\":0},\"source\":{\"outputs\":1,\"hiz\":0}},{\"inputs\":48,\"destination\":{\"outputs\":3,\"hiz\":0},\"source\":{\"outputs\":1,\"hiz\":0}},{\"inputs\":16,\"destination\":{\"outputs\":0,\"hiz\":0},\"source\":{\"outputs\":2,\"hiz\":0}},{\"inputs\":32,\"destination\":{\"outputs\":1,\"hiz\":0},\"source\":{\"outputs\":2,\"hiz\":0}},{\"inputs\":48,\"destination\":{\"outputs\":3,\"hiz\":0},\"source\":{\"outputs\":2,\"hiz\":0}},{\"inputs\":16,\"destination\":{\"outputs\":2,\"hiz\":0},\"source\":{\"outputs\":3,\"hiz\":0}},{\"inputs\":32,\"destination\":{\"outputs\":1,\"hiz\":0},\"source\":{\"outputs\":3,\"hiz\":0}},{\"inputs\":48,\"destination\":{\"outputs\":4,\"hiz\":0},\"source\":{\"outputs\":3,\"hiz\":0}},{\"inputs\":16,\"destination\":{\"outputs\":0,\"hiz\":0},\"source\":{\"outputs\":4,\"hiz\":0}},{\"inputs\":32,\"destination\":{\"outputs\":3,\"hiz\":0},\"source\":{\"outputs\":4,\"hiz\":0}},{\"inputs\":32,\"destination\":{\"outputs\":4,\"hiz\":0},\"source\":{\"outputs\":4,\"hiz\":0}}],\"header\":{\"analyzerProgram\":\"DuPAL Analyzer\",\"PAL\":{\"IOsAsOUT\":63,\"type\":\"PAL16L8\"},\"revision\":1},\"rLinks\":[]}";
        String json = JSONFormatter.formatJSON(new PAL16L8Specs(), 0x3F, oStates, false);

        assertEquals("JSONFormatter should create correct JSON", expectedJson, json);
    }
}
