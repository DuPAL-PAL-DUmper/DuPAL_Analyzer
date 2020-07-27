package net.hkzlab.palanalisys;

import java.util.Arrays;

public class SubState {
    public static final String SS_PRE_TAG = "SS_";

    public final String tag;
    public final MacroState macroState;
    public final Byte[] pin_status;

    public SubState(final String tag, final MacroState macroState, final Byte[] pin_status) {
        this.tag = tag;
        this.macroState = macroState;
        this.pin_status = pin_status;
    } 

    @Override
    public String toString() {
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(SS_PRE_TAG+tag+"-");

        for(byte pin : pin_status) {
            if(pin < 0) strBuf.append('x');
            else if (pin == 0) strBuf.append(0);
            else strBuf.append(1);
        }

        return strBuf.toString();
    }

    @Override
    public int hashCode() {
        return calculateSubStateKey(pin_status) ^ tag.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null) return false;
        if(this.getClass() != o.getClass()) return false;

        SubState ops = (SubState)o;
        if(!ops.tag.equals(this.tag)) return false;
        if(!Arrays.equals(ops.pin_status, this.pin_status)) return false;

        return true;
    }

    public static int calculateSubStateIndex(final boolean[] inputs) {
        int index = 0;

        for(int idx = 0; idx < inputs.length; idx++) {
            index += ((inputs[idx] ? 1 : 0) << idx);
        }

        return index;
    }

    /**
     * 
     */
    public static int calculateSubStateKey(final Byte[] out_comb) {
        int hash = 0;

        for(int idx = 0; idx < out_comb.length; idx++) {
            int byte_idx = idx % 4;
            hash ^= (out_comb[idx] & 0xFF) << (8 * byte_idx);
        }

        return hash;       
    }
}