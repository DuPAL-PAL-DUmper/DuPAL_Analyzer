package net.hkzlab.dupal.dupalproto;

public class DuPALProto {
    private static String CMD_START = ">";
    private static String CMD_END = "<";

    private static String CMD_WRITE = "W";
    private static String CMD_READ = "R";
    private static String CMD_EXIT = "X";
    private static String CMD_RESET = "K";

    private static String CMD_RESP_ERROR = "CMD_ERROR";

    public static String buildREADCommand() {
        return CMD_START+CMD_READ+CMD_END;
    }

    public static String buildWRITECommand(int address) {
        return CMD_START+CMD_WRITE+" "+String.format("%08X", address & 0x3FFFF)+CMD_END;
    }
}