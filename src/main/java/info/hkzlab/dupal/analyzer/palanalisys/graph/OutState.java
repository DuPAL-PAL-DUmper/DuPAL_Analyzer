package info.hkzlab.dupal.analyzer.palanalisys.graph;

import java.util.Arrays;

public class OutState {
    public final OutStatePins pins;
    private final OutLink[] links;

    private int lastOutLinkIdx;

    public OutState(OutStatePins pins, int maxLinks) {
        this.pins = pins;
        links = new OutLink[maxLinks];

        lastOutLinkIdx = 0;
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

        hash = pins.hashCode()*31 + links.length;

        return hash;
    }

    @Override
    public String toString() {
        return "OS["+String.format("%08X", pins.out)+"|"+String.format("%08X", pins.hiz)+"]";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (this.getClass() != o.getClass())
            return false;

        return this.pins.equals(((OutState)o).pins);
    }
}
