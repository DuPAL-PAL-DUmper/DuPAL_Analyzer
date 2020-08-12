package info.hkzlab.dupal.analyzer.devices;

public interface PALSpecs {
    public static final int READ_WRITE_SHIFT = 10;

    public int getNumINPins();
    public int getNumIOPins();
    public int getNumOUTPins();
    public int getNumROUTPins();

    public int getCLKPinMask();
    public int getOEPinMask();
    public int getINMask();
    public int getIO_READMask();
    public int getIO_WRITEMask();
    public int getROUT_READMask();
    public int getROUT_WRITEMask();
    public int getOUT_WRITEMask();
    public int getOUT_READMask();

    public int getROUT_READMaskShift();

    public String[] getROUT_PinNames();
    public String[] getIN_PinNames();
    public String[] getIO_PinNames();
    public String[] getOUT_PinNames();

    public boolean isActiveLow();
}