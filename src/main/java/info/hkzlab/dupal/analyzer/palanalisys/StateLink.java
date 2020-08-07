package info.hkzlab.dupal.analyzer.palanalisys;

import java.io.Serializable;

public class StateLink implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String SL_PRE_TAG = "SL_";

    public final String tag;
    public final int raw_addr;
    public final MacroState destMS;

    public StateLink(final String tag, final int raw_addr, final MacroState destMS) {
        this.tag = tag;
        this.raw_addr = raw_addr;
        this.destMS = destMS;
    }

    @Override
    public String toString() {
        return (SL_PRE_TAG + tag + "-" + String.format("%08X", raw_addr));
    }

    @Override
    public int hashCode() {
        return raw_addr ^ tag.hashCode();
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
        if(ops.raw_addr != this.raw_addr) return false;
        if(!ops.tag.equals(this.tag)) return false;

        return true;
    }
}