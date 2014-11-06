package ligueBaseball.exceptions;

public class PlayerAlreadyExistsException extends Exception
{
    private static final long serialVersionUID = 9127290524773620438L;

    public PlayerAlreadyExistsException() {
        super("Le joueur existe déjà dans la base de donnée.");
    }
}
