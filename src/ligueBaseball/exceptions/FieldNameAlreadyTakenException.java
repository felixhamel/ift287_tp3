package ligueBaseball.exceptions;

public class FieldNameAlreadyTakenException extends Exception {

	private static final long serialVersionUID = -6375248318667356528L;

	public FieldNameAlreadyTakenException(String fieldName) {
		super(String.format("Field name '%s'is already taken.", fieldName));
	}
}
