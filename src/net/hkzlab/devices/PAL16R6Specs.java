package net.hkzlab.devices;

public class PAL16R6Specs implements PALSpecs {

    @Override
    public int getNumINPins() {
        return 8;
    }

    @Override
    public int getNumIOPins() {
        return 2;
    }

    @Override
    public int getNumROUTPins() {
        return 6;
    }

    @Override
    public int getCLKPinMask() {
        return 0x00000001;
    }

    @Override
    public int getOEPinMask() {
        return 0x00000200;
    }

    @Override
    public int getINMask() {
        return 0x000001FE;
    }

    @Override
    public int getIO_READMask() {
        return 0xC0;
    }

    @Override
    public int getIO_WRITEMask() {
        return 0x00030000;
    }

    @Override
    public int getROUT_READMask() {
        return 0x3F;
    }

    @Override
    public int getROUT_WRITEMask() {
        return getROUT_READMask() << 10;
    }

    @Override
    public String toString() {
        return "PAL16R6";
    }

    @Override
    public int getROUT_READMaskShift() {
        return 0;
    }
}