package ligueBaseball.exceptions;

public class FailedToDeleteEntityException extends Exception
{
    private static final long serialVersionUID = 2048533964955399849L;

    public FailedToDeleteEntityException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedToDeleteEntityException(Throwable cause) {
        super("Impossible de détruire l'entitée.", cause);
    }
}
