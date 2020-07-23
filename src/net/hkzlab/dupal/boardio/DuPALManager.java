package net.hkzlab.dupal.boardio;

import static jssc.SerialPort.*;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

public class DuPALManager {
    private SerialPort serport = null;


    private final static String REMOTE_MODE_STRING = "REMOTE_CONTROL_ENABLED";

    public DuPALManager(final String serPort) {
        serport = new SerialPort(serPort);

        try {
            serport.openPort();
        } catch (final SerialPortException e) {
            e.printStackTrace();
            serport = null;
        }

        if (serport != null) {
            try {
                serport.setParams(BAUDRATE_19200, DATABITS_8, STOPBITS_1, PARITY_NONE);
            } catch (final SerialPortException e) {
                e.printStackTrace();
                try {
                    serport.closePort();
                    serport = null;
                } catch (final SerialPortException e1) { ; }
            }
        }
    }

    private void resetBoard() {
        if (serport != null && serport.isOpened()) {
            try {
                serport.setDTR(true);
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                }
                ;
                serport.setDTR(false);
            } catch (final SerialPortException e) {
                e.printStackTrace();
            }
        }
    }

    private void purgeRX() {
        if(serport != null && serport.isOpened()) {
            while(true) {
                try { serport.readString(10, 100); }
                catch (SerialPortTimeoutException | SerialPortException e) { break; }
            }
        }
    }

    public boolean enterRemoteMode() {
        if (serport != null && serport.isOpened()) {
            resetBoard();
            try {
                Thread.sleep(6000);
            } catch (final InterruptedException e) {}
            
            // Wait for it to boot
            try {
                serport.purgePort(PURGE_RXABORT | PURGE_TXCLEAR);
                purgeRX();

                serport.writeByte((byte) 0x78); // 'x' in ascii
                try { Thread.sleep(100); } catch(InterruptedException e) {};
                final String response = serport.readString();

                if ((response != null)) {
                    if (response.trim().equals(REMOTE_MODE_STRING))
                        return true;
                    else
                        return false;
                } else
                    return false;

            } catch (SerialPortException e) {
                e.printStackTrace();
                return false;
            }
        }

        return false;
    }

    public void cleanup() {
        try {
            if ((serport != null) && serport.isOpened())
                serport.closePort();
        } catch (final SerialPortException e) {
            e.printStackTrace();
        }
    }
}