package info.hkzlab.dupal.analyzer.devices;

public interface PALSpecs {
    public int getPinCount_IN();
    public int getPinCount_IO();
    public int getPinCount_O();
    public int getPinCount_RO();

    public int getMask_CLK();
    public int getMask_OE();
    public int getMask_IN();
    public int getMask_IO_R();
    public int getMask_IO_W();
    public int getMask_RO_R();
    public int getMask_RO_W();
    public int getMask_O_R();
    public int getMask_O_W();

    public String[] getLabels_RO();
    public String[] getLabels_O();
    public String[] getLabels_IO();
    public String[] getLabels_IN();

    public boolean isActiveLow();

    public int minimumBoardRev();

    public int slotNumber();
}