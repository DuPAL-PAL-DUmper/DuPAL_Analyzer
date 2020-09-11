package info.hkzlab.dupal.analyzer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import info.hkzlab.dupal.analyzer.devices.PAL16L8Specs;
import info.hkzlab.dupal.analyzer.palanalisys.formatter.EspressoFormatter;

public class FormattersTest 
{
    @Test
    public void espressoFormatterShouldBuildCorrect16L8Header() {
        PAL16L8Specs pSpecs = new PAL16L8Specs();
        int ioAsOutMask = 0x03;
        String header = EspressoFormatter.formatEspressoTableHeader(pSpecs, ioAsOutMask);
        String expectedHeader = "# PAL16L8\n" +
                                ".i 16\n" +
                                ".o 8\n" +
                                ".ilb i1 i2 i3 i4 i5 i6 i7 i8 i9 i11 io16 io15 io14 io13 fio18 fio17 \n" +
                                ".ob o19 o12 io18 io17 o19oe o12oe io18oe io17oe \n" +
                                ".phase 00001111\n\n";
        
        assertEquals("EspressoFormatter should build a correct 16L8 header", expectedHeader, header);
    }
}
