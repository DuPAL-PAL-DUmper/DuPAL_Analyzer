package info.hkzlab.dupal.analyzer.devices;

public class PAL16L8Specs implements PALSpecs {

    public static final String PAL_TYPE = "16L8";

    private static final String[] LABELS_RO = { };
    private static final String[] LABELS_IN =  {  "i1",  "i2",  "i3",  "i4",  "i5",  "i6",  "i7",  "i8",  "i9", "i11",   null,   null,   null,   null,   null,   null,   null,   null };
    private static final String[] LABELS_IO =  {  null,  null,  null,  null,  null,  null,  null,  null,  null,  null, "io18", "io17", "io16", "io15", "io14", "io13",   null,   null };
    private static final String[] LABELS_O  = {  null,  null,  null,  null,  null,  null,  null,  null,  null,  null,   null,   null,   null,   null,   null,   null,  "o19",  "o12" };

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
        return 0x00;
    }

    @Override
    public int getMask_OE() {
        return 0x00;
    }

    @Override
    public int getMask_IN() {
        return 0x3FF;
    }

    @Override
    public int getMask_IO_R() {
        return 0x3F;
    }

    @Override
    public int getMask_IO_W() {
        return 0xFC00;
    }

    @Override
    public int getMask_RO_R() {
        return 0x00;
    }

    @Override
    public int getMask_RO_W() {
        return 0x00;
    }

    @Override
    public int getMask_O_R() {
        return 0xC0;
    }

    @Override
    public int getMask_O_W() {
        return 0x30000;
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
        return 10;
    }

    @Override
    public int getPinCount_IO() {
        return 6;
    }

    @Override
    public int getPinCount_O() {
        return 2;
    }

    @Override
    public int getPinCount_RO() {
        return 0;
    }

    @Override
    public int minimumBoardRev() {
        return 1;
    }
}