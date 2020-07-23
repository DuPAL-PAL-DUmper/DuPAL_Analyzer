package net.hkzlab.dupal;

import net.hkzlab.dupal.dupalproto.DuPALProto;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println(DuPALProto.buildWRITECommand(0xAABBCCDD));
    }
}
