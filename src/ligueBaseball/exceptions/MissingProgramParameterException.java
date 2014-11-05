package ligueBaseball.exceptions;

public class MissingProgramParameterException extends Exception
{
    private static final long serialVersionUID = 3192564306690250063L;

    public MissingProgramParameterException(String parameterName) {
        super(String.format("Missing program parameter '%s'.", parameterName));
    }
}
