package ligueBaseball.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ligueBaseball.exceptions.FailedToDeleteEntityException;
import ligueBaseball.exceptions.FailedToRetrieveNextKeyFromSequenceException;
import ligueBaseball.exceptions.FailedToSaveEntityException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class Official extends DatabaseEntity
{
    private String firstName;
    private String lastName;

    /**
     * Get the official with the given ID.
     *
     * @param databaseConnection
     * @param id - ID of the official.
     * @return Officiel - If found, otherwise return null.
     */
    public static Official getOfficialWithId(Connection databaseConnection, int id)
    {
        PreparedStatement statement = null;

        try {
            statement = databaseConnection.prepareStatement("SELECT * FROM arbitre WHERE arbitreid = ?;");
            statement.setInt(1, id);

            ResultSet officialResult = statement.executeQuery();
            if (!officialResult.next()) {
                return null;
            }
            return getEntityFromResultSet(officialResult);

        } catch (SQLException e) {
            e.printStackTrace();
            return null;

        } finally {
            closeStatement(statement);
        }
    }

    /**
     * Get the official with the given name.
     *
     * @param databaseConnection
     * @param firstName - First name of the official.
     * @param lastName - Last name of the official.
     * @return Official - If found, otherwise return null.
     */
    public static Official getOfficialWithName(Connection databaseConnection, String firstName, String lastName)
    {
        PreparedStatement statement = null;

        try {
            statement = databaseConnection.prepareStatement("SELECT * FROM arbitre WHERE arbitreprenom = ? AND arbitrenom = ?;");
            statement.setString(1, firstName);
            statement.setString(2, lastName);

            ResultSet officialResult = statement.executeQuery();
            if (!officialResult.next()) {
                return null;
            }
            return getEntityFromResultSet(officialResult);

        } catch (SQLException e) {
            e.printStackTrace();
            return null;

        } finally {
            closeStatement(statement);
        }
    }

    protected static Official getEntityFromResultSet(ResultSet resultSet) throws SQLException
    {
        Official entity = new Official();
        entity.id = resultSet.getInt("arbitreid");
        entity.firstName = resultSet.getString("arbitreprenom");
        entity.lastName = resultSet.getString("arbitrenom");

        return entity;
    }

    @Override
    protected void create(Connection databaseConnection) throws FailedToSaveEntityException
    {
        PreparedStatement statement = null;
        try {
            id = getNextIdForTable(databaseConnection, "arbitre", "arbitreid");
            statement = databaseConnection.prepareStatement("INSERT INTO arbitre (arbitreid, arbitreprenom, arbitrenom) VALUES(?, ?, ?);");
            statement.setInt(1, id);
            statement.setString(2, firstName);
            statement.setString(3, lastName);
            statement.execute();
            databaseConnection.commit();

        } catch (SQLException | FailedToRetrieveNextKeyFromSequenceException e) {
            e.printStackTrace();
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
            statement = databaseConnection.prepareStatement("UPDATE arbitre SET arbitreprenom = ? AND arbitrenom = ? WHERE arbitreid = ?;");
            statement.setString(1, firstName);
            statement.setString(2, lastName);
            statement.setInt(3, id);
            statement.executeUpdate();
            databaseConnection.commit();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new FailedToSaveEntityException(e);
        } finally {
            closeStatement(statement);
        }
    }

    @Override
    public void delete(Connection databaseConnection) throws FailedToDeleteEntityException, Exception
    {
        throw new NotImplementedException(); // Not needed for the moment
    }

    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }
}
