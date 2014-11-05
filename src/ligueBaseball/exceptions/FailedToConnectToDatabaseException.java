package ligueBaseball.exceptions;

public class FailedToConnectToDatabaseException extends Exception
{
    private static final long serialVersionUID = 5354390958805004929L;

    public FailedToConnectToDatabaseException(String databaseName, Throwable cause) {
        super(String.format("Failed to connect to database '%s'.", databaseName), cause);
    }
}
