package net.hkzlab.dupal;

import net.hkzlab.dupal.boardio.DuPALManager;
import net.hkzlab.dupal.dupalproto.DuPALProto;
import net.hkzlab.palanalisys.MacroState;
import net.hkzlab.palanalisys.SubState;

public class App {
    public static void main(String[] args) throws Exception {
        DuPALManager dpm = new DuPALManager("/dev/ttyUSB0");

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
