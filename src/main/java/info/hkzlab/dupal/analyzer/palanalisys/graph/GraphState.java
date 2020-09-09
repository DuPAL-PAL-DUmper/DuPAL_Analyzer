package info.hkzlab.dupal.analyzer.palanalisys.graph;

public interface GraphState {
    public int[] getInternalState();

    public boolean isStateFull();
    public GraphLink getLinkAtIdx(int idx);
    public int getMaxLinks();
}
