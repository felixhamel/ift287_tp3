package ligueBaseball.entities;

import java.sql.Connection;

import ligueBaseball.exceptions.FailedToDeleteEntityException;
import ligueBaseball.exceptions.FailedToSaveEntityException;

public class Player extends DatabaseEntity
{
    String lastName;
    String firstName;
    int number = -1;
    int teamId = -1;

    public static Player getPlayerWithId(Connection databaseConnection, int id)
    {
        return null;
    }

    @Override
    protected void create(Connection databaseConnection) throws FailedToSaveEntityException
    {
        // TODO Auto-generated method stub

    }

    @Override
    protected void update(Connection databaseConnection) throws FailedToSaveEntityException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Connection databaseConnection) throws FailedToDeleteEntityException
    {
        // TODO Auto-generated method stub

    }

    /**
     * Get the team this player plays for.
     *
     * @param databaseConnection
     * @return
     */
    public Team getTeam(Connection databaseConnection)
    {
        return Team.getTeamWithId(databaseConnection, teamId);
    }

    /**
     * Set the team this player will play for.
     *
     * @param databaseConnection
     * @param team
     * @throws FailedToSaveEntityException
     */
    public void setTeam(Connection databaseConnection, Team team) throws FailedToSaveEntityException
    {
        team.addPlayer(databaseConnection, this);
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    /**
     * Get the shirt number.
     *
     * @return number - Shirt number.
     */
    public int getNumber()
    {
        return number;
    }

    /**
     * Set the shirt number.
     *
     * @param number - Shirt number.
     */
    public void setNumber(int number)
    {
        this.number = number;
    }
}
