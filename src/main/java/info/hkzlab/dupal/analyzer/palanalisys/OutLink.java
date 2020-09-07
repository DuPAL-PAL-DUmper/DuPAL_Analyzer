package info.hkzlab.dupal.analyzer.palanalisys;

public class OutLink {

   public final int inputs;
   public final OutLink src, dest;
   
   public OutLink(OutLink src, OutLink dest, int inputs) {
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
}
