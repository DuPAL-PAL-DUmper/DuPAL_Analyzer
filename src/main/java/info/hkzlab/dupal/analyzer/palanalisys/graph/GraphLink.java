package info.hkzlab.dupal.analyzer.palanalisys.graph;

public interface GraphLink {
    public int getLinkInputs();
    public GraphState getSourceState();
    public GraphState getDestinationState();

    public boolean isFarLink(); // If true, we will require a "clock" pulse to perform a long jump
}
