package info.hkzlab.dupal.analyzer;

import org.slf4j.*;

import info.hkzlab.dupal.analyzer.board.boardio.*;
import info.hkzlab.dupal.analyzer.devices.*;

public class App {
    private final static Logger logger = LoggerFactory.getLogger(DuPALManager.class);

    private final static String version = App.class.getPackage().getImplementationVersion();

    private static String serialDevice = null;
    private static PALSpecs pspecs = null;
    private static int outMask = -1;
    private static String outDir = null;

    public static void main(String[] args) throws Exception {
        System.out.println("DuPAL Analyzer " + version);

        if(args.length < 3) {
            logger.error("Wrong number of arguments passed.\n"+
                        "dupal_analyzer <serial_port> <pal_type> <output_dir> [hex_output_mask]\n" +
                        "Where <pal_type> can be: 16R8, 16R6, 16R4, 16L8\n");

            return;
        }

        parseArgs(args);

        DuPALManager dpm = new DuPALManager(serialDevice);
        DuPALAnalyzer dpan = new DuPALAnalyzer(dpm, pspecs, outMask, outDir);

        if(!dpm.enterRemoteMode()) {
            System.out.println("Unable to put DuPAL board in REMOTE MODE!");
            System.exit(-1);
        }

        dpan.startAnalisys();
    }

    private static void parseArgs(String[] args) {
        serialDevice = args[0];
        
        switch(args[1].toUpperCase()) {
            case "16R8":
                pspecs = new PAL16R8Specs();
                break;
            case "16R6":
                pspecs = new PAL16R6Specs();
                break;
            case "16R4":
                pspecs = new PAL16R4Specs();
                break;
            case "16L8":
                pspecs = new PAL16L8Specs();
                break;
            default:
                logger.error("Invalid PAL type selected.");
                System.exit(-1);
        }

        outDir = args[2];

        if(args.length >= 4) {
            outMask = Integer.parseInt(args[3], 16);
        }
    }
}
