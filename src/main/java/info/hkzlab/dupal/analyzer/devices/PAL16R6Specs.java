package info.hkzlab.dupal.analyzer.devices;

public class PAL16R6Specs implements PALSpecs {

    public static final String PAL_TYPE = "16R6";

    private static final String[] LABELS_RO =  {  null,  null,  null,  null,  null,  null,  null,  null,  null,   null, "ro18", "ro17", "ro16", "ro15", "ro14", "ro13",   null,   null };
    private static final String[] LABELS_IN =  {  null,  "i2",  "i3",  "i4",  "i5",  "i6",  "i7",  "i8",  "i9",   null,   null,   null,   null,   null,   null,   null,   null,   null };
    private static final String[] LABELS_IO =  {  null,  null,  null,  null,  null,  null,  null,  null,  null,   null,   null,   null,   null,   null,   null,   null, "io19", "io12" };
    private static final String[] LABELS_O  =  { };

    @Override
    public String toString() {
        return "PAL"+PAL_TYPE;
    }

    @Override
    public boolean isActiveLow() {
        return true;
    }

    @Override
    public int getMask_CLK() {
        return 0x01;
    }

    @Override
    public int getMask_OE() {
        return 0x0200;
    }

    @Override
    public int getMask_IN() {
        return 0x1FE;
    }

    @Override
    public int getMask_IO_R() {
        return 0xC0;
    }

    @Override
    public int getMask_IO_W() {
        return 0x030000;
    }

    @Override
    public int getMask_RO_R() {
        return 0x3F;
    }

    @Override
    public int getMask_RO_W() {
        return 0xFC00;
    }

    @Override
    public int getMask_O_R() {
        return 0x00;
    }

    @Override
    public int getMask_O_W() {
        return 0x00;
    }

    @Override
    public String[] getLabels_RO() {
        return LABELS_RO;
    }

    @Override
    public String[] getLabels_O() {
        return LABELS_O;
    }

    @Override
    public String[] getLabels_IO() {
        return LABELS_IO;
    }

    @Override
    public String[] getLabels_IN() {
        return LABELS_IN;
    }

    @Override
    public int getPinCount_IN() {
        return 8;
    }

    @Override
    public int getPinCount_IO() {
        return 2;
    }

    @Override
    public int getPinCount_O() {
        return 0;
    }

    @Override
    public int getPinCount_RO() {
        return 6;
    }

    @Override
    public int minimumBoardRev() {
        return 1;
    }

    @Override
    public int slotNumber() {
        return 0;
    }
}