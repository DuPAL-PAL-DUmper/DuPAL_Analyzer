package info.hkzlab.dupal.analyzer.palanalisys;

import java.io.Serializable;
import java.util.Arrays;

public class SubState implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String SS_PRE_TAG = "SS_";

    public final String tag;
    public final MacroState macroState;
    public final Byte[] IOpin_status;
    public final Byte[] Opin_status;

    public SubState(final String tag, final MacroState macroState, final Byte[] IOpin_status, final Byte[] Opin_status) {
        this.tag = tag;
        this.macroState = macroState;
        this.IOpin_status = IOpin_status;
        this.Opin_status = Opin_status;
    } 

    @Override
    public String toString() {
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(SS_PRE_TAG+tag+"-");

        if(IOpin_status.length > 0) {
            for(byte pin : IOpin_status) {
                if(pin < 0) strBuf.append('x');
                else if (pin == 0) strBuf.append(0);
                else strBuf.append(1);
            }
        } else strBuf.append("noIO");

        strBuf.append("_");
        
        if(Opin_status.length > 0) {
            for(byte pin : Opin_status) {
                if(pin < 0) strBuf.append('x');
                else if (pin == 0) strBuf.append(0);
                else strBuf.append(1);
            }
        } else strBuf.append("noO");

        return strBuf.toString();
    }

    @Override
    public int hashCode() {
        return calculateSubStateKey(IOpin_status) ^ calculateSubStateKey(Opin_status) ^ tag.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null) return false;
        if(this.getClass() != o.getClass()) return false;

        SubState ops = (SubState)o;
        if(!ops.tag.equals(this.tag)) return false;
        if(!Arrays.equals(ops.IOpin_status, this.IOpin_status)) return false;
        if(!Arrays.equals(ops.Opin_status, this.Opin_status)) return false;

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