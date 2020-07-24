package net.hkzlab.devices;

public interface PALSpecsInterface {
    public int getNumINPins();
    public int getNumIOPins();
    public int getNumOUTPins();
    public int getNumROUTPins();

    public int getCLKPin();
    public int getOEPin();
}