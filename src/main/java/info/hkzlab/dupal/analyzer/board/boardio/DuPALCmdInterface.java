package info.hkzlab.dupal.analyzer.board.boardio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.hkzlab.dupal.analyzer.board.dupalproto.DuPALProto;
import info.hkzlab.dupal.analyzer.devices.PALSpecs;
import info.hkzlab.dupal.analyzer.exceptions.DuPALBoardException;
import info.hkzlab.dupal.analyzer.utilities.BitUtils;

public class DuPALCmdInterface {
    private static final Logger logger = LoggerFactory.getLogger(DuPALCmdInterface.class);

    private final DuPALManager dpm;
    public final PALSpecs palSpecs;

    public DuPALCmdInterface(DuPALManager dpm, PALSpecs palSpecs) {
        this.dpm = dpm;
        this.palSpecs = palSpecs;
    }

    public int read() {
        dpm.writeCommand(DuPALProto.buildREADCommand());
        return DuPALProto.handleREADResponse(dpm.readResponse());
    }

    public int write(int data) throws DuPALBoardException {
        int res;
        dpm.writeCommand(DuPALProto.buildWRITECommand(data));
        res = DuPALProto.handleWRITEResponse(dpm.readResponse());

        if(res < 0) {
            logger.error("write("+String.format("%08X", data)+") -> FAILED!");
            throw new DuPALBoardException("write("+String.format("%08X", data)+") command failed!");
        }

        return res;
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
