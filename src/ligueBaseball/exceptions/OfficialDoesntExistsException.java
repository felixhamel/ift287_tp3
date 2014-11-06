package ligueBaseball.exceptions;

public class OfficialDoesntExistsException extends Exception
{
    private static final long serialVersionUID = 5348480785815397137L;

    public OfficialDoesntExistsException() {
        super("L'arbitre n'existe pas.");
    }
}
