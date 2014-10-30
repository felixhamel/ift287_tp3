package ligueBaseball.exceptions;

public class CreatePlayerParametersMissingException extends Exception {
	
	private static final long serialVersionUID = -8629218409925615997L;

	public CreatePlayerParametersMissingException() {
		super(String.format("To assign a player to a team, the team name and his number are required."));
	}
}
