package info.hkzlab.dupal.analyzer.palanalisys.graph;

import info.hkzlab.dupal.analyzer.exceptions.DuPALAnalyzerException;

public class OutState implements GraphState {
    public final OutStatePins pins;
    private final OutLink[] links;

    private int lastOutLinkIdx;

    public OutState(OutStatePins pins, int maxLinks) {
        this.pins = pins;
        links = new OutLink[maxLinks];

        lastOutLinkIdx = 0;
    }

    public int addOutLink(OutLink link) throws DuPALAnalyzerException {
        int idx = lastOutLinkIdx;

        if(idx >= links.length) throw new DuPALAnalyzerException("Tried to insert a link above maximum possible for this State " + this.toString());

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
        return "OS[O:"+String.format("%08X", pins.out)+"|Z:"+String.format("%08X", pins.hiz)+"]";
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

    @Override
    public OutStatePins getInternalState() {
        return pins;
    }

    @Override
    public boolean isStateFull() {
        return links.length == lastOutLinkIdx;
    }

    @Override
    public GraphLink[] getLinks() {
        return links;
    }
}
