package info.hkzlab.dupal.analyzer.devices;

public class PAL16R6Specs implements PALSpecs {
    private static final String[] ROUT_PIN_NAMES = { "ro13", "ro14", "ro15", "ro16", "ro17", "ro18" };
    private static final String[] IN_PIN_NAMES = { "i2", "i3", "i4", "i5", "i6", "i7", "i8", "i9" };
    private static final String[] IO_PIN_NAMES = { null, null, null, null, null, null, "io19", "io12" };
    private static final String[] OUT_PIN_NAMES = { null, null, null, null, null, null, null, null };

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
    public int getNumOUTPins() {
        return 0;
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
        return getIO_READMask() << READ_WRITE_SHIFT;
    }

    @Override
    public int getOUT_READMask() {
        return 0x00;
    }

    @Override
    public int getOUT_WRITEMask() {
        return getOUT_READMask() << READ_WRITE_SHIFT;
    }

    @Override
    public int getROUT_READMask() {
        return 0x3F;
    }

    @Override
    public int getROUT_WRITEMask() {
        return getROUT_READMask() << READ_WRITE_SHIFT;
    }

    @Override
    public String toString() {
        return "PAL16R6";
    }

    @Override
    public int getROUT_READMaskShift() {
        return 0;
    }

    @Override
    public String[] getROUT_PinNames() {
        return ROUT_PIN_NAMES;
    }

    @Override
    public String[] getIN_PinNames() {
        return IN_PIN_NAMES;
    }

    @Override
    public String[] getIO_PinNames() {
        return IO_PIN_NAMES;
    }

    @Override
    public String[] getOUT_PinNames() {
        return OUT_PIN_NAMES;
    }

    @Override
    public boolean isActiveLow() {
        return true;
    }
}