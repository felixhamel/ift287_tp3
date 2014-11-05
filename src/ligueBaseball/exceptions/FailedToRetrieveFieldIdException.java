package ligueBaseball.exceptions;

public class FailedToRetrieveFieldIdException extends Exception 
{
	private static final long serialVersionUID = 4866384103915926302L;

	public FailedToRetrieveFieldIdException(String teamName)
	{
		super(String.format("Impossible de récupérer l'ID du terrain de l'équipe '%s'.", teamName));
	}
}
