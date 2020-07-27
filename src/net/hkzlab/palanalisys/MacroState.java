package net.hkzlab.palanalisys;

import java.util.Arrays;
import java.util.HashMap;

public class MacroState {
    public static final String MS_PRE_TAG = "MS_";

    public final String tag;
    public final int rpins;
    public final int rpin_status;

    public final SubState[] substates;
    public final StateLink[] links;
    public final HashMap<Integer, SubState> ssMap;

    public MacroState(final String tag, final int rpin_status, final int rpins, final int inPins) {
        this.tag = tag;
        this.rpin_status = rpin_status;
        this.rpins = rpins;

        links = new StateLink[2 ^ inPins]; // Create space for the future links out of this
        substates = new SubState[2 ^ inPins]; // Create space for substates (each output pin is 3-state, but as they're triggered via input changes, we can have at most 2^inPins)
        ssMap = new HashMap<>(); // Prepare the hashmap we'll use to avoid substate duplicates
    }

    @Override
    public String toString() {
        return MS_PRE_TAG + tag + " - " + Integer.toBinaryString(rpin_status);
    }

    @Override
    public int hashCode() {
        int hash = 0;

        for (int idx = 0; idx < (2^rpins); idx++) {
            hash ^= (((rpin_status >> idx) & 0x01) << (idx % 32));
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
        if(ops.rpin_status != this.rpin_status) return false;
        if(!ops.tag.equals(this.tag)) return false;

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