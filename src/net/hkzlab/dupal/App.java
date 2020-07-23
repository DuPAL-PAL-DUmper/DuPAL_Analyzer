package net.hkzlab.dupal;

import net.hkzlab.dupal.boardio.DuPALManager;
import net.hkzlab.dupal.dupalproto.DuPALProto;

public class App {
    public static void main(String[] args) throws Exception {
        DuPALManager dpm = new DuPALManager("/dev/ttyUSB0");

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
        
    }
}
