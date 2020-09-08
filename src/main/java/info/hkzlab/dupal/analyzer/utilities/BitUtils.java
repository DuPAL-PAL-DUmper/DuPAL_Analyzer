package info.hkzlab.dupal.analyzer.utilities;

public class BitUtils {
    private BitUtils() {};
   
    static public int countBits(int mask) {
        int tot = 0;

        for(int idx = 0; idx < 32; idx++) {
            if((mask & (0x01 << idx)) > 0) tot++;
        }

        return tot;
    }
    
    static public int consolidateBitField(int field, int mask) {
        int data = 0;
        int shift = 0;

        for(int idx = 0; idx < 32; idx++) {
            if(((mask >> idx) & 0x01) != 0) {
                data |= (field >> (idx-shift)) & (1 << shift);
                shift++;
            }
        }

        return data;
    }
    
    static public int scatterBitField(int field, int mask) {
        int bit_idx = 0;
        int data = 0;

        for(int idx = 0; idx < 32; idx++) {
            if(((mask >> idx) & 0x01) != 0) {
                data |= ((field >> bit_idx) & 0x01) << idx;
                bit_idx++;
            }
        }

        return data;
    }
}
