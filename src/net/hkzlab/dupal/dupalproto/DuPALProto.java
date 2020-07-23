package net.hkzlab.dupal.dupalproto;

public class DuPALProto {
    private static char CMD_START = '>';
    private static char CMD_END = '<';

    private static char CMD_WRITE = 'W';
    private static char CMD_READ = 'R';
    private static char CMD_EXIT = 'X';
    private static char CMD_RESET = 'K';

    private static String CMD_RESP_ERROR = "CMD_ERROR";

    public static String buildREADCommand() {
        return null;
    }

    public static String buildWRITECommand(long address) {
        return null;
    }
}