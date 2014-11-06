package ligueBaseball;

import ligueBaseball.exceptions.FailedToConnectToDatabaseException;
import ligueBaseball.exceptions.UnknownCommandException;

public class Main
{
    /**
     * Main function of the program.
     *
     * @param args - Program arguments
     * @throws UnknownCommandException Unknown command
     * @throws FailedToConnectToDatabaseException Failed to connect to the database
     */
    public static void main(String[] args) throws FailedToConnectToDatabaseException, UnknownCommandException
    {
        new Application(extractProgramParameters(args)).launch();
    }

    /**
     * Extract the program parameters and put them in the programParameters class variable.
     *
     * @param args - All the arguments received when the program was launched.
     * @return ApplicationParameters - Parameters expected by the application.
     */
    private static ApplicationParameters extractProgramParameters(String[] args)
    {
        // Check if we at least have what we need to launch the program.
        if (args.length < 3) {
            System.out.println("Missing or invalid program parameters. It should be like this :");
            System.out.println("program [userId] [password] [dataBase] [entryFile]");
            System.out.println("Without the [] !");
            System.exit(1);
        }

        ApplicationParameters parameters = new ApplicationParameters();
        parameters.setUsername(args[0]);
        parameters.setPassword(args[1]);
        parameters.setDatabaseName(args[2]);

        if (args.length > 3) {
            parameters.setEntryFile(args[3]);
        }

        return parameters;
    }
}
