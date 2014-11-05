package ligueBaseball.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ligueBaseball.exceptions.FailedToDeleteEntityException;
import ligueBaseball.exceptions.FailedToRetrieveNextKeyFromSequenceException;
import ligueBaseball.exceptions.FailedToSaveEntityException;

public class Field extends DatabaseEntity
{
    String name;
    String address;

    public static Field getFieldWithId(Connection databaseConnection, int id)
    {
        PreparedStatement statement = null;

        try {
            statement = databaseConnection.prepareStatement("SELECT * FROM terrain WHERE terrainid = ?;");
            statement.setInt(1, id);

            ResultSet fieldResult = statement.executeQuery();
            if (!fieldResult.next()) {
                return null;
            }

            Field field = new Field();
            field.id = fieldResult.getInt("terrainid");
            field.name = fieldResult.getString("terrainnom");
            field.address = fieldResult.getString("terrainadresse");

            return field;

        } catch (SQLException e) {
            return null;

        } finally {
            closeStatement(statement);
        }
    }

    public static Field getFieldWithName(Connection databaseConnection, String name)
    {
        PreparedStatement statement = null;

        try {
            statement = databaseConnection.prepareStatement("SELECT * FROM terrain WHERE terrainnom = ?;");
            statement.setString(1, name);

            ResultSet fieldResult = statement.executeQuery();
            if (!fieldResult.next()) {
                return null;
            }

            Field field = new Field();
            field.id = fieldResult.getInt("terrainid");
            field.name = fieldResult.getString("terrainnom");
            field.address = fieldResult.getString("terrainadresse");

            return field;

        } catch (SQLException e) {
            return null;

        } finally {
            closeStatement(statement);
        }
    }

    @Override
    protected void create(Connection databaseConnection) throws FailedToSaveEntityException
    {
        PreparedStatement statement = null;
        try {
            id = getNextIdForTable(databaseConnection, "terrain", "terrainid");
            statement = databaseConnection.prepareStatement("INSERT INTO terrain (terrainid, terrainnom, terrainadresse) VALUES(?, ?' ?);");
            statement.setInt(1, id);
            statement.setString(2, name);
            statement.setString(3, address);
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
            statement = databaseConnection.prepareStatement("UPDATE terrain SET terrainnom = ? AND terrainadresse = ? WHERE terrainid = ?;");
            statement.setString(1, name);
            statement.setString(2, address);
            statement.setInt(3, id);
            statement.executeUpdate();
            databaseConnection.commit();

        } catch (SQLException e) {
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
                statement = databaseConnection.prepareStatement("DELETE terrain WHERE terrainid = ?;");
                statement.setInt(1, id);
                statement.executeQuery();
                databaseConnection.commit();

            } catch (SQLException e) {
                throw new FailedToDeleteEntityException(e);
            } finally {
                closeStatement(statement);
            }
        }
        // Else the current field has never been created in the database before.
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }
}
