package ligueBaseball.exceptions;

public class FailedToRetrieveNextKeyFromSequenceException extends Exception 
{
	private static final long serialVersionUID = 9201727617032353947L;

	public FailedToRetrieveNextKeyFromSequenceException(String tableName)
	{
		super(String.format("Problème lors de la récupération de la prochaine clé primaire pour la table '%s'.", tableName));
	}
}
