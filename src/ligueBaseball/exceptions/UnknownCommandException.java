package ligueBaseball.exceptions;

public class UnknownCommandException extends Exception
{
    private static final long serialVersionUID = 6391299047705630360L;

    public UnknownCommandException(String command) {
        super(String.format("%s n'est pas une command connue.", command));
    }
}
