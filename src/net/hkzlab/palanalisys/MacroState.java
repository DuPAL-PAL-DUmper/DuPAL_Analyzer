package net.hkzlab.palanalisys;

import java.util.ArrayList;

public class MacroState {
    public static final String MS_PRE_TAG = "MS_";

    public final String tag;
    public final boolean[] rpin_status;
    public final SubState[] substates;

    public MacroState(final String tag, final boolean[] rpin_status, int outPins) {
        this.tag = tag;
        this.rpin_status = rpin_status;

        int possible_bin_sstates = 2 ^ outPins;
        ArrayList<SubState> sStates = new ArrayList<>();
        byte[] pstatus;
        for(int idx = 0; idx < possible_bin_sstates; idx++) {
            pstatus = new byte[outPins];

            // Generate this binary combination
            for(int idx_bit = 0; idx_bit < outPins; idx_bit++) {
                pstatus[idx_bit] = (byte)((idx >> idx_bit) & 0x01);
            }

            sStates.add(new SubState(tag, pstatus));

            // Generate the remaining combinations with hi-z
            for(int hiz_idx = 0; hiz_idx < possible_bin_sstates; hiz_idx++) {
                for(int idx_bit = 0; idx_bit < outPins; idx_bit++) {
                    byte[] oc_pstatus = new byte[outPins];
                    oc_pstatus[idx_bit] = ((hiz_idx >> idx_bit) & 0x01) > 0 ? -1 : pstatus[idx_bit];
                }  

                sStates.add(new SubState(tag, pstatus));
            }
        }

        substates = sStates.toArray(new SubState[sStates.size()]);
    }

    @Override
    public String toString() {
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(MS_PRE_TAG+tag+" - ");

        for(boolean rpin : rpin_status) strBuf.append(rpin ? '1':'0');

        return strBuf.toString();
    }
}