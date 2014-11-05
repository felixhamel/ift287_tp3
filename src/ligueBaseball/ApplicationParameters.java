package ligueBaseball;

public class ApplicationParameters
{
    private String username;
    private String password;
    private String databaseName;
    private String entryFile = ""; // Not NULL

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public void setDatabaseName(String databaseName)
    {
        this.databaseName = databaseName;
    }

    public String getEntryFile()
    {
        return entryFile;
    }

    public void setEntryFile(String entryFile)
    {
        this.entryFile = entryFile;
    }
}
