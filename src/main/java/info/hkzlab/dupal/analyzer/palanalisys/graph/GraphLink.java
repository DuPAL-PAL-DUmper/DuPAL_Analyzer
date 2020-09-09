package info.hkzlab.dupal.analyzer.palanalisys.graph;

public interface GraphLink {
    public int getLinkInputs();
    public GraphState getSourceState();
    public GraphState getDestinationState();
}
