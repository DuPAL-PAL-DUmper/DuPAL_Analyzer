package info.hkzlab.dupal.analyzer.palanalisys.graph;

public interface GraphState {
    public OutStatePins getInternalState();
    public boolean isStateFull();
    public GraphLink[] getLinks();
}
