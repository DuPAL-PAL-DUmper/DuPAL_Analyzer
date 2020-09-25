package info.hkzlab.dupal.analyzer.palanalisys.graph;

import info.hkzlab.dupal.analyzer.exceptions.DuPALAnalyzerException;

public class OutState implements GraphState {
    public final OutStatePins pins;
    private final OutLink[] outLinks;
    private final RegLink[] regLinks;

    private int lastOutLinkIdx;
    private int lastRegLinkIdx;

    public OutState(final OutStatePins pins, final int maxLinks) {
        this(pins, maxLinks, 0);
    }

    public OutState(final OutStatePins pins, final int maxOLinks, final int maxRLinks) {
        this.pins = pins;
        outLinks = new OutLink[maxOLinks];
        regLinks = new RegLink[maxRLinks];

        lastOutLinkIdx = 0;
        lastRegLinkIdx = 0;
    }

    public int addOutLink(OutLink link) throws DuPALAnalyzerException {
        int idx = lastOutLinkIdx;

        if(idx >= outLinks.length) throw new DuPALAnalyzerException("Tried to insert a link above maximum possible for this State " + this.toString());

        lastOutLinkIdx++;

        setOutLinkAtIdx(link, idx);

        return idx;
    }

    
    public int addRegLink(RegLink link) throws DuPALAnalyzerException {
        int idx = lastRegLinkIdx;

        if(idx >= regLinks.length) throw new DuPALAnalyzerException("Tried to insert a registered link above maximum possible for this State " + this.toString());

        lastRegLinkIdx++;

        setRegLinkAtIdx(link, idx);

        return idx;
    }

    public int getNextLinkIdx() {
        return lastOutLinkIdx;
    }

    public int getNextRegLinkIdx() {
        return lastRegLinkIdx;
    }

    private boolean setOutLinkAtIdx(OutLink link, int idx) {
        if(outLinks[idx] != null) return false;
        outLinks[idx] = link;

        return true;
    }

    private boolean setRegLinkAtIdx(RegLink link, int idx) {
        if(regLinks[idx] != null) return false;
        regLinks[idx] = link;

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;

        hash = hash*31 + pins.hashCode();
        hash = hash*31 + outLinks.length;
        hash = hash*31 + regLinks.length;

        return hash;
    }

    @Override
    public String toString() {
        return "OS[O:"+String.format("%02X", pins.out)+"|Z:"+String.format("%02X", pins.hiz)+"]";
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
        return  isStateFullOutLinks() &&
                isStateFullRegLinks();
    }

    public boolean isStateFullOutLinks() {
        return outLinks.length == lastOutLinkIdx;
    }
    
    public boolean isStateFullRegLinks() {
        return regLinks.length == lastRegLinkIdx;
    }

    @Override
    public GraphLink[] getLinks() {
        GraphLink[] linkArray = new GraphLink[outLinks.length + regLinks.length];
        System.arraycopy(outLinks, 0, linkArray, 0, outLinks.length);
        System.arraycopy(regLinks, 0, linkArray, outLinks.length, regLinks.length);

        return linkArray;
    }

    public OutLink[] getOutLinks() {
        return outLinks.clone();
    }

    public RegLink[] getRegLinks() {
        return regLinks.clone();
    }
}
