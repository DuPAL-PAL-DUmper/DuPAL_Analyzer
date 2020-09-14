package info.hkzlab.dupal.analyzer.palanalisys.formatter;

import java.util.ArrayList;

import info.hkzlab.dupal.analyzer.devices.PALSpecs;
import info.hkzlab.dupal.analyzer.palanalisys.graph.OutState;
import info.hkzlab.dupal.analyzer.palanalisys.simple.SimpleState;
import info.hkzlab.dupal.analyzer.utilities.BitUtils;

public class EspressoFormatter {
    private EspressoFormatter() {};

    public static String formatEspressoTableHeader(PALSpecs pSpecs, int ioAsOutMask) {
        StringBuffer strBuf = new StringBuffer();
        int ioAsOut_W = BitUtils.scatterBitField(BitUtils.consolidateBitField(ioAsOutMask, pSpecs.getMask_IO_R()), pSpecs.getMask_IO_W());
        int outCount = (pSpecs.getPinCount_O() + BitUtils.countBits(ioAsOutMask) + pSpecs.getPinCount_RO());

        strBuf.append("# " + pSpecs.toString() + "\n");
        strBuf.append(".i " + (pSpecs.getPinCount_IN() + pSpecs.getPinCount_IO() + pSpecs.getPinCount_RO()) + "\n"); // Inputs, IO as inputs, IO as outputs (as feedbacks), registered outputs (as feedbacks)
        strBuf.append(".o " + outCount*2 + "\n"); // Outputs, IO as outputs, Registered Outputs, then an out for all of those as OE
        
        strBuf.append(".ilb ");
        for(int idx = 0; idx < 32; idx++) if(((pSpecs.getMask_IN() >> idx) & 0x01) > 0) strBuf.append(pSpecs.getLabels_IN()[idx] + " ");
        for(int idx = 0; idx < 32; idx++) if((((pSpecs.getMask_IO_W() & ~ioAsOut_W) >> idx) & 0x01) > 0) strBuf.append(pSpecs.getLabels_IO()[idx] + " ");
        for(int idx = 0; idx < 32; idx++) if((((pSpecs.getMask_IO_W() & ioAsOut_W) >> idx) & 0x01) > 0) strBuf.append("f" + pSpecs.getLabels_IO()[idx] + " ");
        for(int idx = 0; idx < 32; idx++) if(((pSpecs.getMask_RO_W() >> idx) & 0x01) > 0) strBuf.append("ps"+pSpecs.getLabels_RO()[idx] + " ");
        strBuf.append("\n");
        
        strBuf.append(".ob ");
        for(int idx = 0; idx < 32; idx++) if(((pSpecs.getMask_O_W() >> idx) & 0x01) > 0) strBuf.append(pSpecs.getLabels_O()[idx] + " ");
        for(int idx = 0; idx < 32; idx++) if((((pSpecs.getMask_IO_W() & ioAsOut_W) >> idx) & 0x01) > 0) strBuf.append(pSpecs.getLabels_IO()[idx] + " ");
        for(int idx = 0; idx < 32; idx++) if(((pSpecs.getMask_RO_W() >> idx) & 0x01) > 0) strBuf.append(pSpecs.getLabels_RO()[idx] + " ");
        for(int idx = 0; idx < 32; idx++) if(((pSpecs.getMask_O_W() >> idx) & 0x01) > 0) strBuf.append(pSpecs.getLabels_O()[idx] + "oe ");
        for(int idx = 0; idx < 32; idx++) if((((pSpecs.getMask_IO_W() & ioAsOut_W) >> idx) & 0x01) > 0) strBuf.append(pSpecs.getLabels_IO()[idx] + "oe ");
        for(int idx = 0; idx < 32; idx++) if(((pSpecs.getMask_RO_W() >> idx) & 0x01) > 0) strBuf.append(pSpecs.getLabels_RO()[idx] + "oe ");
        strBuf.append("\n");
       
        strBuf.append(".phase ");
        for(int idx = 0; idx < outCount; idx++) strBuf.append(pSpecs.isActiveLow() ? '0' : '1');
        for(int idx = 0; idx < outCount; idx++) strBuf.append('1');
        strBuf.append("\n\n");
        
        return strBuf.toString();
    }
   
    public static String[] formatEspressoTable(PALSpecs pSpecs, OutState[] states) {
        ArrayList<String> tableRows = new ArrayList<>();

        for(OutState ss : states) {

        }

        return tableRows.toArray(new String[tableRows.size()]);
    }

    public static String[] formatEspressoTable(PALSpecs pSpecs, SimpleState[] states) {
        ArrayList<String> tableRows = new ArrayList<>();

        StringBuffer strBuf = new StringBuffer();
        for(SimpleState ss : states) {
            strBuf.delete(0, strBuf.length());
            
            int inputs = BitUtils.consolidateBitField(ss.input, pSpecs.getMask_IN());
            int output = BitUtils.consolidateBitField(ss.output, pSpecs.getMask_O_R());
            int hiz = BitUtils.consolidateBitField(ss.hiz, pSpecs.getMask_O_R());
            for(int idx = 0; idx < pSpecs.getPinCount_IN(); idx++) strBuf.append((char)(((inputs >> idx) & 0x01) + 0x30));
            strBuf.append(' ');
            for(int idx = 0; idx < pSpecs.getPinCount_O(); idx++) {
                boolean hiz_pin = ((hiz >> idx) & 0x01) != 0;
                strBuf.append(hiz_pin ? '-' : (char)(((output >> idx) & 0x01) + 0x30));
            }
            for(int idx = 0; idx < pSpecs.getPinCount_O(); idx++) strBuf.append((char)(((hiz >> idx) & 0x01) + 0x30));
            strBuf.append('\n');

            tableRows.add(strBuf.toString());
        }

        return tableRows.toArray(new String[tableRows.size()]);
    }

    public static String formatEspressoFooter() {
        return ".e\n\n";
    }
}
