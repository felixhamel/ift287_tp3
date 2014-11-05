package ligueBaseball.exceptions;

public class TeamCantPlayAgainstItselfException extends Exception
{
    private static final long serialVersionUID = -687048539614922834L;

    public TeamCantPlayAgainstItselfException(String team) {
        super(String.format("L'équipe '%s' ne peut pas jouer contre elle-même.", team));
    }
}
