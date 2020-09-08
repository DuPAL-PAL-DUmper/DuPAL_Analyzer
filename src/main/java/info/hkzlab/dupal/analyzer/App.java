package info.hkzlab.dupal.analyzer;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.*;

import info.hkzlab.dupal.analyzer.board.boardio.*;
import info.hkzlab.dupal.analyzer.devices.*;

public class App {
    public static volatile String[] palTypes = { PAL16L8Specs.PAL_TYPE };

    private final static Logger logger = LoggerFactory.getLogger(DuPALManager.class);

    private final static String version = App.class.getPackage().getImplementationVersion();

    private static String serialDevice = null;
    private static PALSpecs pspecs = null;
    private static int outMask = -1;
    private static String outDir = null;

    public static void main(String[] args) throws Exception {
        System.out.println("DuPAL Analyzer " + version);

        if (args.length < 3) {
            StringBuffer supportedPALs = new StringBuffer();

            for(String palT : palTypes) {
                supportedPALs.append("\t"+palT+"\n");
            }

            logger.error("Wrong number of arguments passed.\n"
                    + "dupal_analyzer <serial_port> <pal_type> <output_dir> [hex_output_mask]\n"
                    + "Where <pal_type> can be:\n" + supportedPALs.toString() + "\n");

            return;
        }

        parseArgs(args);

        DuPALManager dpm = new DuPALManager(serialDevice);
        DuPALAnalyzer dpan = new DuPALAnalyzer(dpm, pspecs, outMask, outDir);

        if (!dpm.enterRemoteMode()) {
            System.out.println("Unable to put DuPAL board in REMOTE MODE!");
            System.exit(-1);
        } 

        dpan.startAnalisys();
    }

    private static void parseArgs(String[] args) {
        serialDevice = args[0];

        try {
            Class<?> specsClass = Class.forName("info.hkzlab.dupal.analyzer.devices.PAL" + args[1].toUpperCase() + "Specs");
            pspecs = (PALSpecs) specsClass.getConstructor().newInstance(new Object[]{});
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            logger.error("Invalid PAL type selected.");
            System.exit(-1);
        }

        outDir = args[2];

        if(args.length >= 4) {
            outMask = Integer.parseInt(args[3], 16);
        }
    }
}
