package info.hkzlab.dupal.analyzer.palanalisys.simple;

public class SimpleState {
    public final int input;
    public final int output;
    public final int hiz;
    
    public SimpleState(final int input, final int output, final int hiz) {
        this.input = input;
        this.output = output;
        this.hiz = hiz;
    }

    @Override
    public String toString() {
        return "SS[I:"+String.format("%06X", input)+"|O:"+String.format("%02X", output)+"|Z:"+String.format("%02X", hiz)+"]";
    }
}
