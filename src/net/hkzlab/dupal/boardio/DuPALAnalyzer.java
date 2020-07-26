package net.hkzlab.dupal.boardio;

import net.hkzlab.devices.PALSpecs;

public class DuPALAnalyzer {
    private final DuPALManager dpm;
    private final PALSpecs pspecs;
    private int IOasOUT_Mask = -1;

    public DuPALAnalyzer(final DuPALManager dpm, final PALSpecs pspecs, final int IOasOUT_Mask) {
        this.dpm = dpm;
        this.pspecs = pspecs;
        this.IOasOUT_Mask = IOasOUT_Mask;
    } 
    
    public DuPALAnalyzer(final DuPALManager dpm, final PALSpecs pspecs) {
        this(dpm, pspecs, -1);
    }

    public void startAnalisys() {
        if(IOasOUT_Mask < 0) { // We need to detect the status of the IOs...
            // TODO: Try to guess whether IOs are Inputs or Outputs
        }

        // TODO: Actually perform the analisys

    }
}