package ligueBaseball.command;

import java.util.ArrayList;

public class Command 
{
	/**
	 * Extract the command and parameters if any from a given String.
	 * @param commandWithParametersIfAny - String with command and parameters if any.
	 */
	public static Command extractCommandFromString(String commandWithParametersIfAny)
	{
		String[] splitedString = commandWithParametersIfAny.split(" ");
		
		// Extract parameters if any
		ArrayList<String> parameters = new ArrayList<>();
		if(splitedString.length> 1) {
			for(int i = 1; i < splitedString.length; ++i) {
				parameters.add(splitedString[i]);
			}
		}
		return new Command(splitedString[0], parameters);
	}
	
	private String commandName;
	private ArrayList<String> parameters;
	
	/**
	 * Default constructor.
	 * @param commandName - Name of the command.
	 * @param parameters - Parameters given with the command.
	 */
	private Command(String commandName, ArrayList<String> parameters)
	{
		this.commandName = commandName;
		this.parameters = parameters;
	}
	
	/**
	 * Get the name of the command.
	 * @return String - Name of the command.
	 */
	public String getCommandName()
	{
		return commandName;
	}
	
	/**
	 * Get the parameters given with the command if any.
	 * @return ArrayList or String - Parameters if any.
	 */
	public ArrayList<String> getParameters()
	{
		return parameters;
	}
}
