package info.hkzlab.dupal.analyzer.palanalisys.graph;

public class OutLink implements GraphLink {

   public final int inputs;
   public final OutState src, dest;
   
   public OutLink(OutState src, OutState dest, int inputs) {
       this.src = src;
       this.dest = dest;
       this.inputs = inputs;
   }

    @Override
    public int hashCode() {
        int hash = 7;

        hash = hash*31 + inputs;
        hash = hash*31 + src.hashCode();
        hash = hash*31 + dest.hashCode();

        return hash;
    }

    @Override
    public String toString() {
        return "<"+src+">-OL["+String.format("%08X", inputs)+"]-<"+dest+">";
    }

    @Override
    public int getLinkInputs() {
        return inputs;
    }

    @Override
    public GraphState getSourceState() {
        return src;
    }

    @Override
    public GraphState getDestinationState() {
        return dest;
    }
}
