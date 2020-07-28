package net.hkzlab.palanalisys;

import java.util.Arrays;

public class StateLink {
    public static final String SL_PRE_TAG = "SL_";

    public final String tag;
    public final int raw_addr;
    public final boolean[] inputs;
    public final SubState destSState;

    public StateLink(final String tag, final int raw_addr, final boolean[] inputs, final SubState destSState) {
        this.tag = tag;
        this.raw_addr = raw_addr;
        this.inputs = inputs;
        this.destSState = destSState;
    }

    @Override
    public String toString() {
        final StringBuffer strBuf = new StringBuffer();
        strBuf.append(SL_PRE_TAG + tag + "-");

        for (final boolean in : inputs)
            strBuf.append(in ? '1' : '0');

        return strBuf.toString();
    }

    @Override
    public int hashCode() {
        int hash = 0;

        for (int idx = 0; idx < inputs.length; idx++) {
            hash ^= ((inputs[idx] ? 1 : 0) << (idx % 32));
        }

        return hash ^ tag.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (this.getClass() != o.getClass())
            return false;

        final StateLink ops = (StateLink) o;
        if(!ops.tag.equals(this.tag)) return false;
        if(!Arrays.equals(ops.inputs, this.inputs)) return false;

        return true;
    }
}