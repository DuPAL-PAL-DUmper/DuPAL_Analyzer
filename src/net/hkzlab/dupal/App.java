package net.hkzlab.dupal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.hkzlab.devices.PAL16R4Specs;
import net.hkzlab.devices.PAL16R6Specs;
import net.hkzlab.devices.PALSpecs;
import net.hkzlab.dupal.boardio.DuPALAnalyzer;
import net.hkzlab.dupal.boardio.DuPALManager;

public class App {
    private final static Logger logger = LoggerFactory.getLogger(DuPALManager.class);

    public static void main(String[] args) throws Exception {
        DuPALManager dpm = new DuPALManager("/dev/ttyUSB0");
         PALSpecs pspecs = new PAL16R6Specs();
        //PALSpecs pspecs = new PAL16R4Specs();
        // DuPALAnalyzer dpan = new DuPALAnalyzer(dpm, pspecs);
        DuPALAnalyzer dpan = new DuPALAnalyzer(dpm, pspecs, -1, "/tmp/dupal.dmp");
        //DuPALAnalyzer dpan = new DuPALAnalyzer(dpm, pspecs, 0x80, "/tmp/dupal.dmp");

        if(!dpm.enterRemoteMode()) {
            System.out.println("Unable to enter remote mode!");
            System.exit(-1);
        }

        dpan.startAnalisys();

        /*
        try {
            System.out.println(dpm.enterRemoteMode());

            for(int idx = 0; idx <= 0xFFFF; idx++) {
                dpm.writeCommand(DuPALProto.buildWRITECommand(idx));
                int addr = DuPALProto.handleWRITEResponse(dpm.readResponse());

                if(addr >= 0) {
                    System.out.println("idx -> " + idx + " written -> " + addr);
                } else {
                    System.out.println("Write error!");
                    break;
                }

                dpm.writeCommand(DuPALProto.buildREADCommand());
                System.out.println("Read ... " + DuPALProto.handleREADResponse(dpm.readResponse()));
            }
        } finally {
            dpm.cleanup();
        }
        */
        //SubState ss = new SubState("TEST", new byte[] {-1, 0, -1, 0, -1, -1});
        //System.out.println(ss.toString());
        //System.out.println(ss.hashCode());

        //MacroState ms = new MacroState("TEST", new boolean[] {true, true, false, true}, 3);
        //System.out.println(ms.toString());
        //System.out.println(ms.hashCode());

    }
}
