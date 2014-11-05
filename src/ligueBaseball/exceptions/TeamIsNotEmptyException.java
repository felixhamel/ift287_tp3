package ligueBaseball.exceptions;

public class TeamIsNotEmptyException extends Exception
{

    private static final long serialVersionUID = 1875701240978298710L;

    public TeamIsNotEmptyException(String teamName) {
        super(String.format("Team '%s'is not empty.", teamName));
    }
}
