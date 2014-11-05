package ligueBaseball.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ligueBaseball.exceptions.FailedToDeleteEntityException;
import ligueBaseball.exceptions.FailedToRetrieveNextKeyFromSequenceException;
import ligueBaseball.exceptions.FailedToSaveEntityException;

public class Player extends DatabaseEntity
{
    String lastName;
    String firstName;
    int number = -1;
    int teamId = -1;

    /**
     * Get the player with the given ID.
     *
     * @param databaseConnection
     * @param id - ID of the player.
     * @return Player - If found, otherwise return null.
     */
    public static Player getPlayerWithId(Connection databaseConnection, int id)
    {
        PreparedStatement statement = null;

        try {
            statement = databaseConnection.prepareStatement("SELECT joueur.joueurid, joueur.joueurprenom, joueur.joueurnom, faitpartie.numero, faitpartie.equipeid FROM joueur, faitpartie WHERE joueur.joueurid = ?;");
            statement.setInt(1, id);

            ResultSet fieldResult = statement.executeQuery();
            if (!fieldResult.next()) {
                return null;
            }
            return createFieldFromResultSet(fieldResult);

        } catch (SQLException e) {
            return null;

        } finally {
            closeStatement(statement);
        }
    }

    private static Player createFieldFromResultSet(ResultSet resultSet) throws SQLException
    {
        Player player = new Player();

        player.id = resultSet.getInt("joueurid");
        player.firstName = resultSet.getString("joueurprenom");
        player.lastName = resultSet.getString("joueurnom");
        player.number = resultSet.getInt("numero");
        player.teamId = resultSet.getInt("equipeid");

        return player;
    }

    @Override
    protected void create(Connection databaseConnection) throws FailedToSaveEntityException
    {
        PreparedStatement statement = null;
        try {
            id = getNextIdForTable(databaseConnection, "joueur", "joueurid");
            statement = databaseConnection.prepareStatement("INSERT INTO joueur (joueurid, joueurnom, joueurprenom) VALUES(?, ?, ?);");
            statement.setInt(1, id);
            statement.setString(2, lastName);
            statement.setString(3, firstName);
            statement.execute();
            databaseConnection.commit();

        } catch (SQLException | FailedToRetrieveNextKeyFromSequenceException e) {
            throw new FailedToSaveEntityException(e);
        } finally {
            closeStatement(statement);
        }
    }

    @Override
    protected void update(Connection databaseConnection) throws FailedToSaveEntityException
    {
        PreparedStatement statement = null;
        try {
            id = getNextIdForTable(databaseConnection, "joueur", "joueurid");
            statement = databaseConnection.prepareStatement("UPDATE joueur SET joueurnom = ? AND joueurprenom = ? WHERE joueurid = ?;");
            statement.setString(1, lastName);
            statement.setString(2, firstName);
            statement.setInt(3, id);
            statement.executeUpdate();
            databaseConnection.commit();

        } catch (SQLException | FailedToRetrieveNextKeyFromSequenceException e) {
            throw new FailedToSaveEntityException(e);
        } finally {
            closeStatement(statement);
        }
    }

    @Override
    public void delete(Connection databaseConnection) throws FailedToDeleteEntityException
    {
        if (id >= 0) {
            PreparedStatement statement = null;
            try {
                statement = databaseConnection.prepareStatement("DELETE joueur WHERE joueurid = ?;");
                statement.setInt(1, id);
                statement.executeUpdate();
                databaseConnection.commit();

            } catch (SQLException e) {
                throw new FailedToDeleteEntityException(e);
            } finally {
                closeStatement(statement);
            }
        }
        lastName = null;
        firstName = null;
        number = -1;
        teamId = -1;
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

    /**
     * Get the last name of the player.
     *
     * @return String - Last name.
     */
    public String getLastName()
    {
        return lastName;
    }

    /**
     * Set the last name of the player.
     *
     * @param lastName - Last name.
     */
    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    /**
     * Get the first name of the player.
     *
     * @return String - First name.
     */
    public String getFirstName()
    {
        return firstName;
    }

    /**
     * Set the first name of the player.
     *
     * @param firstName - First name.
     */
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
