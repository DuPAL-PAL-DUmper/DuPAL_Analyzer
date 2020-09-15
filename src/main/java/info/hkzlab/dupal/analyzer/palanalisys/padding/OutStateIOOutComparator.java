package info.hkzlab.dupal.analyzer.palanalisys.padding;

import java.util.Comparator;

import info.hkzlab.dupal.analyzer.palanalisys.graph.OutState;
import info.hkzlab.dupal.analyzer.utilities.BitUtils;

public class OutStateIOOutComparator implements Comparator<OutState> {
    private final int ioAsOutMask;

    public OutStateIOOutComparator(final int ioAsOutMask) {
       this.ioAsOutMask = ioAsOutMask;
    }

    @Override
    public int compare(OutState s1, OutState s2) {
        int io1 = BitUtils.consolidateBitField(s1.pins.out, ioAsOutMask);
        int io2 = BitUtils.consolidateBitField(s2.pins.out, ioAsOutMask);

        if(io1 < io2) return -1;
        else if (io1 > io2) return 1;
        else return 0;
    }
    
}
