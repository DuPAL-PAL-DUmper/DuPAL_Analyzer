package net.hkzlab.devices;

public interface PALSpecs {
    public int getNumINPins();
    public int getNumIOPins();
    public int getNumROUTPins();

    public int getCLKPinMask();
    public int getOEPinMask();
    public int getINMask();
    public int getIO_READMask();
    public int getIO_WRITEMask();
    public int getROUT_READMask();
    public int getROUT_WRITEMask();

    public int getROUT_READMaskShift();
}