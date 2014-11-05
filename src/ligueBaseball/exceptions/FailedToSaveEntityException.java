package ligueBaseball.exceptions;

public class FailedToSaveEntityException extends Exception
{
    private static final long serialVersionUID = 890161362571158318L;

    public FailedToSaveEntityException(String message) {
        super("Impossible de sauvegarder l'entitée parce que " + message);
    }

    public FailedToSaveEntityException(Throwable cause) {
        super("Impossible de sauvegarder l'entité.", cause);
    }
}
