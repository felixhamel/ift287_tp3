package ligueBaseball.exceptions;

public class FailedToRetrievePlayersOfTeamException extends Exception
{
    private static final long serialVersionUID = -8028746831845363836L;

    public FailedToRetrievePlayersOfTeamException(String name, Throwable cause) {
        super(String.format("Impossible d'aller récupérer les joueurs de l'équipe %s.", name), cause);
    }
}
