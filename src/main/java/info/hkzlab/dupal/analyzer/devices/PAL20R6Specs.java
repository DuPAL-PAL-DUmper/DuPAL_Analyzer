package info.hkzlab.dupal.analyzer.devices;

public class PAL20R6Specs implements PALSpecs {

    public static final String PAL_TYPE = "20R6";

    private static final String[] LABELS_RO =  {  null,  null,  null,  null,  null,  null,  null,  null,  null, null,    null, "ro16", "ro17", "ro18", "ro19", "ro20", "ro21",   null,   null,   null,   null,   null };
    private static final String[] LABELS_IN =  {  "i1",  "i2",  "i3",  "i4",  "i5",  "i6",  "i7",  "i8",  "i9", "i10",   null,   null,   null,   null,   null,   null,   null,   null,  "i11",  "i13",  "i14",  "i23" };
    private static final String[] LABELS_IO =  {  null,  null,  null,  null,  null,  null,  null,  null,  null,  null, "io15",   null,   null,   null,   null,   null,   null, "io22",   null,   null,   null,   null };
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
        return 0x80000;
    }

    @Override
    public int getMask_IN() {
        return 0x3403FE;
    }

    @Override
    public int getMask_IO_R() {
        return 0x81;
    }

    @Override
    public int getMask_IO_W() {
        return 0x020400;
    }

    @Override
    public int getMask_RO_R() {
        return 0x7E;
    }

    @Override
    public int getMask_RO_W() {
        return 0x01F800;
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
        return 12;
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
        return 2;
    }

    @Override
    public int slotNumber() {
        return 1;
    }
}