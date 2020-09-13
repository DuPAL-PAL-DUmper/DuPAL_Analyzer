package info.hkzlab.dupal.analyzer.palanalisys.graph;

public class RegLink implements GraphLink {
   public final int inputs;
   public final OutState src, middle, dest;
   
   public RegLink(final OutState src, final OutState middle, final OutState dest, final int inputs) {
       this.src = src;
       this.middle = middle;
       this.dest = dest;
       this.inputs = inputs;
   }

    @Override
    public int hashCode() {
        int hash = 7;

        hash = hash*31 + inputs;
        hash = hash*31 + src.hashCode();
        hash = hash*31 + middle.hashCode();
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

        return  (this.src.equals(((RegLink)o).src)) &&
                (this.dest.equals(((RegLink)o).dest)) &&
                (this.middle.equals(((RegLink)o).middle)) &&
                (this.inputs == ((RegLink)o).inputs);
    }

    @Override
    public String toString() {
        return "("+src+")->("+middle+")->RL["+String.format("%06X", inputs)+"]->("+dest+")";
    }


    @Override
    public int getLinkInputs() {
        return inputs;
    }

    public OutState getMiddleState() {
        return middle;
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
        return true;
    }
    
}
