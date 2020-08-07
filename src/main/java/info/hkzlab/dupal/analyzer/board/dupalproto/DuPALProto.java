package info.hkzlab.dupal.analyzer.board.dupalproto;

import java.util.ArrayList;

public class DuPALProto {
    private final static String CMD_START = ">";
    private final static String CMD_END = "<";
   
    private final static String RESP_START = "[";
    private final static String RESP_END = "]";

    private final static char CMD_WRITE = 'W';
    private final static char CMD_READ = 'R';
    private final static char CMD_EXIT = 'X';
    private final static char CMD_RESET = 'K';

    private final static String CMD_RESP_ERROR = "CMD_ERROR";

    public static String buildREADCommand() {
        return CMD_START+CMD_READ+CMD_END;
    }

    /**
     * The command will toggle the following pins on the DuPAL, from LSB to MSB
     * 1, 2, 3, 4, 5, 6, 7, 8, 9, 11 (these are connected directly to the PAL)
     * 18, 17, 16, 15, 14, 13, 19, 12 (these are connected to the pin through a 10k resistor, acting as pulls)
     * @param address the combination of output to compose on the DuPAL
     * @return The generated command
     */
    public static String buildWRITECommand(int address) {
        return ""+CMD_START+CMD_WRITE+" "+String.format("%08X", address & 0x3FFFF)+CMD_END;
    }

    public static String buildEXITCommand() {
        return ""+CMD_START+CMD_EXIT+CMD_END;
    }

    public static String buildRESETCommand() {
        return ""+CMD_START+CMD_RESET+CMD_END;
    }

    /**
     * This method returns the status of the PINs of the device inserted in the DuPAL, in this order, from LSB to MSB:
     * 18, 17, 16, 15, 14, 13, 19, 12
     * @param response String containing the response received by the DuPAL
     * @return Returns an integer containing the state of the PAL pins
     */
    public static int handleREADResponse(String response) {
        String[] readRes = parseResponse(response);

        if((readRes == null) || readRes.length != 2 || readRes[0].charAt(0) != CMD_READ) return -1;
        
        try {
            return Integer.parseInt(readRes[1], 16);
        } catch(NumberFormatException e) {
            return -1;
        }
    }

    public static int handleWRITEResponse(String response) {
         String[] readRes = parseResponse(response);

        if((readRes == null) || readRes.length != 2 || readRes[0].charAt(0) != CMD_WRITE) return -1;
        
        try {
            return Integer.parseInt(readRes[1], 16);
        } catch(NumberFormatException e) {
            return -1;
        }       
    }

    public static String[] parseResponse(String response) {
        if(response == null) return null;

        ArrayList<String> respString = new ArrayList<>();        
        response = response.trim();

        if(response.equals(CMD_RESP_ERROR)) {
            respString.add(CMD_RESP_ERROR);
        } else if(response.startsWith(RESP_START) && response.endsWith(RESP_END)) {
            response = response.substring(1, response.length()-1).trim();
            char command = response.charAt(0);
            switch(command) {
                case CMD_READ: 
                case CMD_WRITE: {
                        String[] cmd_comp = response.split(" ");
                        if(cmd_comp.length != 2) return null;
                        return cmd_comp;
                    }
                default:
                    return null;
            }

        } else return null;

        return respString.toArray(new String[respString.size()]);
    }
}