package net.hkzlab.devices;

public class PAL16R4Specs implements PALSpecs {

    @Override
    public int getNumINPins() {
        return 8;
    }

    @Override
    public int getNumIOPins() {
        return 4;
    }

    @Override
    public int getNumROUTPins() {
        return 4;
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
        return 0xE1;
    }

    @Override
    public int getIO_WRITEMask() {
        return getIO_READMask() << 10;
    }

    @Override
    public int getROUT_READMask() {
        return 0x1E;
    }

    @Override
    public int getROUT_WRITEMask() {
        return getROUT_READMask() << 10;
    }

    @Override
    public String toString() {
        return "PAL16R4";
    }

    @Override
    public int getROUT_READMaskShift() {
        return 1;
    }
}