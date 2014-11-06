package ligueBaseball.exceptions;

public class NegativeScore extends Exception
{

    private static final long serialVersionUID = 8623566461742380323L;

    public NegativeScore() {
        super(String.format("Score can't be negative"));
    }
}
