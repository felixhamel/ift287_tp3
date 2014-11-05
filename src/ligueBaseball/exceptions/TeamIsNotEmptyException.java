package ligueBaseball.exceptions;

public class TeamIsNotEmptyException extends Exception
{

    private static final long serialVersionUID = 1875701240978298710L;

    public TeamIsNotEmptyException(String teamName) {
        super(String.format("L'équipe '%s' n'est pas vide. Retirer tout les joueurs avant de pouvoir détruire cette équipe.", teamName));
    }
}
