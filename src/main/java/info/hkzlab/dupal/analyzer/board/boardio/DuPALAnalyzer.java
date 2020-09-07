package info.hkzlab.dupal.analyzer.board.boardio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.hkzlab.dupal.analyzer.board.dupalproto.DuPALProto;
import info.hkzlab.dupal.analyzer.devices.*;
import info.hkzlab.dupal.analyzer.exceptions.*;

public class DuPALAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(DuPALAnalyzer.class);

    private static final String SERIALIZED_DUMP = "dupalstat.dmp";
    private static final String OUT_TABLE = "dupal_thrtable.tbl";
    private static final String DUPAL_STRUCT = "dupal_struct.txt";

    private final DuPALManager dpm;
    private final PALSpecs pspecs;
    
    private final String serdump_path;
    private final String tblPath;
    private final String structPath;
    
    private int IOasOUT_Mask = -1;
    private int additionalOUTs = 0;


    public DuPALAnalyzer(final DuPALManager dpm, final PALSpecs pspecs, final int IOasOUT_Mask, final String outPath) {
        this.dpm = dpm;
        this.pspecs = pspecs;
        this.IOasOUT_Mask = IOasOUT_Mask;

        serdump_path = outPath + File.separator+ SERIALIZED_DUMP;
        tblPath = outPath + File.separator + OUT_TABLE;
        structPath = outPath + File.separator + DUPAL_STRUCT;
    } 
    
    public DuPALAnalyzer(final DuPALManager dpm, final PALSpecs pspecs) {
        this(dpm, pspecs, -1, null);
    }

    public void startAnalisys() throws InvalidIOPinStateException, ICStateException, DuPALBoardException {
    }
}