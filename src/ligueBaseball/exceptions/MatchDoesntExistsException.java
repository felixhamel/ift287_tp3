package ligueBaseball.exceptions;

public class MatchDoesntExistsException extends Exception
{
    private static final long serialVersionUID = 8755745078931295236L;

    public MatchDoesntExistsException() {
        super("Le match n'a pas été trouvé.");
    }
}
