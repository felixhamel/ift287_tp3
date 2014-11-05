package ligueBaseball.exceptions;

public class CannotFindTeamWithNameException extends Exception 
{
	private static final long serialVersionUID = 7845272165153980956L;

	public CannotFindTeamWithNameException(String teamName) 
	{
		super(String.format("L'équipe '%s' n'existe pas.", teamName));
	}
}
