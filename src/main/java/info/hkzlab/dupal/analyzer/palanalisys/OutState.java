package info.hkzlab.dupal.analyzer.palanalisys;

import java.util.Arrays;

public class OutState {
    public final static int IDX_O = 0;
    public final static int IDX_IO = 1;
    public final static int IDX_HIZ = 2;

    private final int[] status;
    private final OutLink[] links;

    private int lastOutLinkIdx;

    public OutState(int o_state, int io_state, int hiz_state, int maxLinks) {
        status = new int[]{o_state, io_state, hiz_state};
        links = new OutLink[maxLinks];

        lastOutLinkIdx = 0;
    }

    public int[] getStatus() {
        return status.clone();
    }

    public OutLink getOutLinkAtIdx(int idx) {
        return links[idx];
    }

    public int addOutLink(OutLink link) {
        int idx = lastOutLinkIdx;
        lastOutLinkIdx++;

        setOutLinkAtIdx(link, idx);

        return idx;
    }

    public int getMaxLinks() {
        return links.length;
    }

    private boolean setOutLinkAtIdx(OutLink link, int idx) {
        if(links[idx] != null) return false;
        links[idx] = link;

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;

        for(int s : status) hash = hash*31 + s;

        return hash;
    }

    @Override
    public String toString() {
        return "OS["+String.format("%08X", status[0])+"|"+String.format("%08X", status[1])+"|"+String.format("%08X", status[2])+"]";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (this.getClass() != o.getClass())
            return false;

        return Arrays.equals(this.status, ((OutState) o).getStatus());
    }
}
