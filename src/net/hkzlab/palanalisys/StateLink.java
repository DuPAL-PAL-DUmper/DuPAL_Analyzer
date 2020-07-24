package net.hkzlab.palanalisys;

public class StateLink {
    public final boolean[] inputs;
    public final SubState destSState;

    public StateLink(final boolean[] inputs, final SubState destSState) {
        this.inputs = inputs;
        this.destSState = destSState;
    }
}