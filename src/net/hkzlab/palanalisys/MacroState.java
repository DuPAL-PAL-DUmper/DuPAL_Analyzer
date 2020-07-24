package net.hkzlab.palanalisys;

import java.util.ArrayList;
import java.util.Arrays;

public class MacroState {
    public static final String MS_PRE_TAG = "MS_";

    public final String tag;
    public final boolean[] rpin_status;
    public final SubState[] substates;
    public final StateLink[] links;

    public MacroState(final String tag, final boolean[] rpin_status, final int outPins, final int inPins) {
        this.tag = tag;
        this.rpin_status = rpin_status;

        links = new StateLink[2 ^ inPins]; // Create space for the future links out of this
        substates = new SubState[3 ^ outPins]; // Create space for substates (each output pin is 3-state)
    }

    @Override
    public String toString() {
        final StringBuffer strBuf = new StringBuffer();
        strBuf.append(MS_PRE_TAG + tag + " - ");

        for (final boolean rpin : rpin_status)
            strBuf.append(rpin ? '1' : '0');

        return strBuf.toString();
    }

    @Override
    public int hashCode() {
        int hash = 0;

        for (int idx = 0; idx < rpin_status.length; idx++) {
            hash ^= ((rpin_status[idx] ? 1 : 0) << (idx % 32));
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

        final MacroState ops = (MacroState) o;
        if(!ops.tag.equals(this.tag)) return false;
        if(!Arrays.equals(ops.rpin_status, this.rpin_status)) return false;

        return true;
    }

    public static int calculateMacroStateIndex(boolean[] rpinStatus) {
        int index = 0;

        for(int idx = 0; idx < rpinStatus.length; idx++) {
            index |= ((rpinStatus[idx] ? 1:0) << idx);
        }

        return index;
    }    
}