package net.hkzlab.devices;

public interface PALSpecsInterface {
    public int getNumINPins();
    public int getNumIOPins();
    public int getNumOUTPins();
    public int getNumROUTPins();

    public int getCLKPinMask();
    public int getOEPinMask();
    public int getINMask();
    public int getOUTMask();
    public int getIO_INMask();
    public int getIO_OUTMask();
    public int getROUTMask();
}