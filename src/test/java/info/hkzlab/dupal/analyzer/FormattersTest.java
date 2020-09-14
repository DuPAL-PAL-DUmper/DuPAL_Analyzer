package info.hkzlab.dupal.analyzer;

import static org.junit.Assert.*;

import org.junit.Test;

import info.hkzlab.dupal.analyzer.devices.PAL16L8Specs;
import info.hkzlab.dupal.analyzer.exceptions.DuPALAnalyzerException;
import info.hkzlab.dupal.analyzer.palanalisys.formatter.EspressoFormatter;
import info.hkzlab.dupal.analyzer.palanalisys.graph.OutLink;
import info.hkzlab.dupal.analyzer.palanalisys.graph.OutState;
import info.hkzlab.dupal.analyzer.palanalisys.graph.OutStatePins;

public class FormattersTest {
    @Test
    public void espressoFormatterShouldBuildCorrect16L8Header() {
        PAL16L8Specs pSpecs = new PAL16L8Specs();
        int ioAsOutMask = 0x03;
        String header = EspressoFormatter.formatEspressoTableHeader(pSpecs, ioAsOutMask);
        String expectedHeader = "# PAL16L8\n" + ".i 16\n" + ".o 8\n"
                + ".ilb i1 i2 i3 i4 i5 i6 i7 i8 i9 i11 io16 io15 io14 io13 fio18 fio17 \n"
                + ".ob o19 o12 io18 io17 o19oe o12oe io18oe io17oe \n" + ".phase 00001111\n\n";

        assertEquals("EspressoFormatter should build a correct 16L8 header", expectedHeader, header);
    }

    @Test
    public void espressoFormatterShouldBuildCorrect16L8TableWithoutRegLinks() throws DuPALAnalyzerException {
        PAL16L8Specs pSpecs = new PAL16L8Specs();
        int ioAsOutMask = 0x38;

        OutState[] states = new OutState[3];

        states[0] = new OutState(new OutStatePins(0x38, 0xC0), 3);
        states[1] = new OutState(new OutStatePins(0xAF, 0x20), 3);
        states[2] = new OutState(new OutStatePins(0x00, 0x00), 3);

        states[0].addOutLink(new OutLink(states[0], states[0], 0x00));
        states[0].addOutLink(new OutLink(states[0], states[1], 0x3800));
        states[0].addOutLink(new OutLink(states[0], states[2], 0x04));
        
        states[1].addOutLink(new OutLink(states[1], states[0], 0x00));
        states[1].addOutLink(new OutLink(states[1], states[1], 0x07));
        states[1].addOutLink(new OutLink(states[1], states[2], 0x04));
        
        states[2].addOutLink(new OutLink(states[2], states[2], 0x09));
        states[2].addOutLink(new OutLink(states[2], states[1], 0x2800));
        states[2].addOutLink(new OutLink(states[2], states[1], 0x04));

        String[] rows = EspressoFormatter.formatEspressoTable(pSpecs, ioAsOutMask, states);

        System.out.print(EspressoFormatter.formatEspressoTableHeader(pSpecs, ioAsOutMask));
        for(String row : rows) System.out.print(row);

        String[] expected = new String[] {
            "0000000000000111 --11111000\n",
            "0000000000011111 0110-00001\n",
            "0010000000000111 0000000000\n",
            
            "000000000000010- --11111000\n",
            "111000000000010- 0110-00001\n",
            "001000000000010- 0000000000\n",
            
            "1001000000000000 0000000000\n",
            "0000000000010000 0110-00001\n",
            "0010000000000000 0110-00001\n",
        };

        assertArrayEquals("EspressoFormatter should build the correct truth table for specified states", expected, rows);
    }
}
