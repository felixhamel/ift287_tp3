package ligueBaseball.exceptions;

public class FailedToRetrieveFieldException extends Exception
{
    private static final long serialVersionUID = 1558880207169647304L;

    public FailedToRetrieveFieldException() {
        super("Problème lors de la requête pour aller chercher un terrain.");
    }

    public FailedToRetrieveFieldException(Throwable cause) {
        super("Problème lors de la requête pour aller chercher un terrain.", cause);
    }
}
