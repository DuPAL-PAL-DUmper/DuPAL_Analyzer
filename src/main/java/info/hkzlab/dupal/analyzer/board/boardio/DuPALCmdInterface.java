package info.hkzlab.dupal.analyzer.board.boardio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.hkzlab.dupal.analyzer.board.dupalproto.DuPALProto;
import info.hkzlab.dupal.analyzer.devices.PALSpecs;
import info.hkzlab.dupal.analyzer.exceptions.DuPALBoardException;
import info.hkzlab.dupal.analyzer.utilities.BitUtils;

public class DuPALCmdInterface {
    private static final Logger logger = LoggerFactory.getLogger(DuPALCmdInterface.class);

    public static enum DuPAL_LED {
        P20_LED, P24_LED
    }

    private final DuPALManager dpm;
    public final PALSpecs palSpecs;

    public DuPALCmdInterface(DuPALManager dpm, PALSpecs palSpecs) {
        this.dpm = dpm;
        this.palSpecs = palSpecs;
    }

    public void reset() {
        dpm.writeCommand(DuPALProto.buildRESETCommand());
    }

    public int read() {
        dpm.writeCommand(DuPALProto.buildREADCommand());
        return DuPALProto.handleREADResponse(dpm.readResponse());
    }

    public int getBoardVersion() {
        dpm.writeCommand(DuPALProto.buildMODELCommand());
        return DuPALProto.handleMODELResponse(dpm.readResponse());
    }
    
    public boolean setLED(DuPAL_LED led, boolean enabled) {
        int led_status = enabled ? 1 : 0;

        switch(led) {
            case P20_LED:
                led_status |= 0x02;
                break;
            case P24_LED:
                led_status |= 0x04;
                break;
        }

        dpm.writeCommand(DuPALProto.buildLEDCommand(led_status));
        return (DuPALProto.handleLEDResponse(dpm.readResponse()) == led_status);
    }

    public int write(int data) throws DuPALBoardException {
        int res;
        dpm.writeCommand(DuPALProto.buildWRITECommand(data));
        res = DuPALProto.handleWRITEResponse(dpm.readResponse());

        if(res < 0) {
            logger.error("write(%08X) -> FAILED!", data);
            throw new DuPALBoardException("write("+String.format("%08X", data)+") command failed!");
        }

        return res;
    }

    public void writeAndPulseClock(int data) throws DuPALBoardException {
        int data_clk = data | palSpecs.getMask_CLK();
        int data_noclk = data & ~palSpecs.getMask_CLK();

        try {
            write(data_noclk);
            write(data_clk);
            write(data_noclk);
        } catch(DuPALBoardException e) {
            logger.error("Pulsing clock to insert data %06X failed.", data);
            throw e;
        }

    }

    public int build_WData(int in, int io, boolean clk, boolean oe) {
        int data = 0;

        data |= BitUtils.scatterBitField(in, palSpecs.getMask_IN());
        data |= BitUtils.scatterBitField(io, palSpecs.getMask_IO_W());

        if(clk) data |= BitUtils.scatterBitField(1, palSpecs.getMask_CLK());
        if(oe) data |= BitUtils.scatterBitField(1, palSpecs.getMask_OE());

        return data;
    }
}
