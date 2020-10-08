package info.hkzlab.dupal.analyzer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.*;

import info.hkzlab.dupal.analyzer.board.boardio.*;
import info.hkzlab.dupal.analyzer.devices.*;

public class App {
    public static volatile String[] palTypes = { 
                                                // Simple devices
                                                PAL10L8Specs.PAL_TYPE,

                                                // Asynchronous outputs
                                                PAL16L8Specs.PAL_TYPE,
                                                PAL20L8Specs.PAL_TYPE,

                                                // Registered devices
                                                PAL16R4Specs.PAL_TYPE,
                                                PAL16R6Specs.PAL_TYPE,
                                                PAL16R8Specs.PAL_TYPE,
                                                PAL20R4Specs.PAL_TYPE,
                                                PAL20R6Specs.PAL_TYPE,
                                                PAL20R8Specs.PAL_TYPE 
                                            };

    private final static Logger logger = LoggerFactory.getLogger(App.class);

    private final static String version = App.class.getPackage().getImplementationVersion();

    private static String serialDevice = null;
    private static PALSpecs pspecs = null;
    private static int outMask = -1;
    private static String outFile = null;

    public static void main(String[] args) throws Exception {
        logger.info("DuPAL Analyzer " + version);

        if (args.length < 3) {
            StringBuffer supportedPALs = new StringBuffer();

            for(String palT : palTypes) {
                supportedPALs.append("\t"+palT+"\n");
            }

            logger.error("Wrong number of arguments passed.\n"
                    + "dupal_analyzer <serial_port> <pal_type> <output_file> [hex_output_mask]\n"
                    + "Where <pal_type> can be:\n" + supportedPALs.toString() + "\n");

            return;
        }

        parseArgs(args);

        DuPALManager dpm = new DuPALManager(serialDevice);
        DuPALCmdInterface dpci = new DuPALCmdInterface(dpm, pspecs);
        DuPALAnalyzer dpan = new DuPALAnalyzer(dpci, outMask, outFile);

        if (!dpm.enterRemoteMode()) {
            logger.error("Unable to put DuPAL board in REMOTE MODE!");
            System.exit(-1);
        } 

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                dpci.reset();
            }
        });

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

        outFile = args[2];

        if(args.length >= 4) {
            outMask = Integer.parseInt(args[3], 16);
        }

        checkOutPath(outFile);
    }

    private static void checkOutPath(String path) {
        File file = new File(path);

        boolean exists = file.exists();
        boolean isDirectory = file.isDirectory();

        if(isDirectory) {
            logger.error("Output path " + path + " points to a directory, please specify an output file!");
            System.exit(-1);
        }

        if(exists) {
            logger.error("Output path " + path + " points to an existing file. We're not going to overwrite it!");
            System.exit(-1);
        }
    }
}
