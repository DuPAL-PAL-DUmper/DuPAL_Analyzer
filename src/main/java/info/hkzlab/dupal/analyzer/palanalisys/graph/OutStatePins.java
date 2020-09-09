package info.hkzlab.dupal.analyzer.palanalisys.graph;

public class OutStatePins {
    public final int out;
    public final int hiz;

    public OutStatePins(int out, int hiz) {
        this.out = out;
        this.hiz = hiz;
    }

    
    @Override
    public int hashCode() {
        int hash = 7;

        hash = hash*31+out;
        hash = hash*31+hiz;

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

        return (this.hiz == ((OutStatePins)o).hiz) &&
               (this.out == ((OutStatePins)o).out);
    }
}
