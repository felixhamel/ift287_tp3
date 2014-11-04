package ligueBaseball.exceptions;

public class MissingCommandParameterException extends Exception 
{
	private static final long serialVersionUID = -4448744431331688994L;

	public MissingCommandParameterException(String command, String parameter)
	{
		super(String.format("Il manque le paramètre '%s' pour la commande '%s'.", parameter, command));
	}
}
