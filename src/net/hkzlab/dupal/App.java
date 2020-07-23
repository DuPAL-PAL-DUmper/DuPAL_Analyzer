package net.hkzlab.dupal;

import net.hkzlab.dupal.boardio.DuPALManager;

public class App {
    public static void main(String[] args) throws Exception {
        DuPALManager dpm = new DuPALManager("/dev/ttyUSB0");

        try {
            System.out.println(dpm.enterRemoteMode());

        } finally {
            dpm.cleanup();
        }
        
    }
}
