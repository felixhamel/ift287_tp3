package ligueBaseball.exceptions;

public class MatchAlreadyHaveTheMaximumNumberOfOfficialsException extends Exception
{
    private static final long serialVersionUID = -8594418341439367495L;

    public MatchAlreadyHaveTheMaximumNumberOfOfficialsException() {
        super("Le match a déjà le nombre maximal d'arbitres (4).");
    }
}
