package ligueBaseball.exceptions;

public class PlayerAlreadyExistsException extends Exception {

	private static final long serialVersionUID = 8623566461742380323L;

	public PlayerAlreadyExistsException(String playerName, String playerLastName) {
		super(String.format("Player '%s' '%d' already exists.", playerName, playerLastName));
	}
}
