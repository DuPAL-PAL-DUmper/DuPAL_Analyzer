package info.hkzlab.dupal.analyzer.exceptions;

public class InvalidIOPinStateException extends Exception {
    private static final long serialVersionUID = 1L;
    public final int curOuts, expOuts;

    public InvalidIOPinStateException(String errorMessage, int curOuts, int expOuts) {
        super(errorMessage);
        
        this.curOuts = curOuts;
        this.expOuts = expOuts;
    }
    
}