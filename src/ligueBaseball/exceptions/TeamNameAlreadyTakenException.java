package ligueBaseball.exceptions;

public class TeamNameAlreadyTakenException extends Exception
{

    private static final long serialVersionUID = 2905615981888147299L;

    public TeamNameAlreadyTakenException(String teamName) {
        super(String.format("Le nom d'équipe '%s' a déjà été prit.", teamName));
    }
}
