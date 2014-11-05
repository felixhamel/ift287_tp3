package ligueBaseball.tools;

public class Logger
{
    public enum LOG_TYPE {
        USER("Utilisateur"), SYSTEM("Système"), OTHER("Autre"), EXCEPTION("Exception"), COMMENT("Commentaire"), COMMAND("Commande");

        private final String value;

        private LOG_TYPE(final String value) {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return value;
        }
    }

    private static void log(String level, LOG_TYPE type, String message)
    {
        System.out.println(String.format("%s[%s]: %s", level, type, message));
    }

    private static void log(String level, LOG_TYPE type, String message, String... args)
    {
        log(level, type, String.format(message, args));
    }

    public static void error(LOG_TYPE type, String message)
    {
        log("Erreur", type, message);
    }

    public static void error(LOG_TYPE type, String message, String... args)
    {
        log("Erreur", type, message, args);
    }

    public static void info(LOG_TYPE type, String message)
    {
        log("Info", type, message);
    }

    public static void info(LOG_TYPE type, String message, String... args)
    {
        log("Info", type, message, args);
    }
}
