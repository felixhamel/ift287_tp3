package ligueBaseball.exceptions;

public class TeamDoesntExistException extends Exception
{
    private static final long serialVersionUID = 7707694789199755725L;

    public TeamDoesntExistException(String teamName) {
        super(String.format("L'équipe %s n'existe pas.", teamName));
    }
}
