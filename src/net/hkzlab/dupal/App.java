package net.hkzlab.dupal;

import net.hkzlab.devices.PAL16R6Specs;
import net.hkzlab.devices.PALSpecs;
import net.hkzlab.dupal.boardio.DuPALAnalyzer;
import net.hkzlab.dupal.boardio.DuPALManager;

public class App {
    public static void main(String[] args) throws Exception {
        DuPALManager dpm = new DuPALManager("/dev/ttyUSB0");
        PALSpecs pspecs = new PAL16R6Specs();
        DuPALAnalyzer dpan = new DuPALAnalyzer(dpm, pspecs);

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
