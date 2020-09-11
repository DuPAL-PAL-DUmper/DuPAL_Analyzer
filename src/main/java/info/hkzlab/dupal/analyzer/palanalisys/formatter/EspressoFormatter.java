package info.hkzlab.dupal.analyzer.palanalisys.formatter;

import info.hkzlab.dupal.analyzer.devices.PALSpecs;

public class EspressoFormatter {
    private EspressoFormatter() {};

    public static String formatEspressoTableHeader(PALSpecs pSpecs, int ioAsOutMask) {
        StringBuffer strBuf = new StringBuffer();

        strBuf.append("# " + pSpecs.toString() + "\n");
        strBuf.append(".i " + (pSpecs.getPinCount_IN() + pSpecs.getPinCount_IO() + pSpecs.getPinCount_RO()) + "\n"); // Inputs, IO as inputs, IO as outputs (as feedbacks), registered outputs (as feedbacks)

        return strBuf.toString();
    }
    
}
