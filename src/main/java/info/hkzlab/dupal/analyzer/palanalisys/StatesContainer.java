package info.hkzlab.dupal.analyzer.palanalisys;

import java.io.Serializable;

public class StatesContainer implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public final MacroState[] mStates;
    public final String palName;
    
    public StatesContainer(final int mStatesCount, final String palName) {
        this.mStates = new MacroState[mStatesCount];
        this.palName = palName;
    }
}