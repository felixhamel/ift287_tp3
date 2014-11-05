package ligueBaseball.exceptions;

public class FailedToRetrieveTeamIdException extends Exception 
{
	private static final long serialVersionUID = 1817247278647557672L;

	public FailedToRetrieveTeamIdException(String teamName)
	{
		super(String.format("Impossible de récupérer l'ID de l'équipe '%s'.", teamName));
	}
}
