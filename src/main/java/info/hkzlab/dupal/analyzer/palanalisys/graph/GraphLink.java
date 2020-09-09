package info.hkzlab.dupal.analyzer.palanalisys.graph;

public interface GraphLink {
    public int getLinkCombination();
    public GraphState getSourceState();
    public GraphState getDestinationState();
}
