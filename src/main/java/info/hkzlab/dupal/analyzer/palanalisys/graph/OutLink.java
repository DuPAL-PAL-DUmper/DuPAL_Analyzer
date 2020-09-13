package info.hkzlab.dupal.analyzer.palanalisys.graph;

public class OutLink implements GraphLink {

   public final int inputs;
   public final OutState src, dest;
   
   public OutLink(final OutState src, final OutState dest, final int inputs) {
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
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (this.getClass() != o.getClass())
            return false;

        return  (this.src.equals(((OutLink)o).src)) &&
                (this.dest.equals(((OutLink)o).dest)) &&
                (this.inputs == ((OutLink)o).inputs);
    }

    @Override
    public String toString() {
        return "("+src+")->OL["+String.format("%06X", inputs)+"]->("+dest+")";
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

    @Override
    public boolean isFarLink() {
        return false;
    }
}
