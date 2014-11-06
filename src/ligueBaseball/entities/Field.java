package ligueBaseball.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ligueBaseball.Logger;
import ligueBaseball.Logger.LOG_TYPE;
import ligueBaseball.exceptions.FailedToDeleteEntityException;
import ligueBaseball.exceptions.FailedToRetrieveNextKeyFromSequenceException;
import ligueBaseball.exceptions.FailedToSaveEntityException;

public class Field extends DatabaseEntity
{
    String name;
    String address;

    /**
     * Get the field with the given ID.
     *
     * @param databaseConnection
     * @param id - ID of the field.
     * @return Field - If found, otherwise return null.
     */
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
            return createFieldFromResultSet(fieldResult);

        } catch (SQLException e) {
            Logger.error(LOG_TYPE.EXCEPTION, e.getMessage());
            return null;

        } finally {
            closeStatement(statement);
        }
    }

    /**
     * Get the field with the given name.
     *
     * @param databaseConnection
     * @param name - Name of the field.
     * @return Field - If found, otherwise return null.
     */
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
            return createFieldFromResultSet(fieldResult);

        } catch (SQLException e) {
            Logger.error(LOG_TYPE.EXCEPTION, e.getMessage());
            return null;

        } finally {
            closeStatement(statement);
        }
    }

    private static Field createFieldFromResultSet(ResultSet resultSet) throws SQLException
    {
        Field field = new Field();
        field.id = resultSet.getInt("terrainid");
        field.name = resultSet.getString("terrainnom");
        field.address = resultSet.getString("terrainadresse");

        return field;
    }

    @Override
    protected void create(Connection databaseConnection) throws FailedToSaveEntityException
    {
        PreparedStatement statement = null;
        try {
            id = getNextIdForTable(databaseConnection, "terrain", "terrainid");
            statement = databaseConnection.prepareStatement("INSERT INTO terrain (terrainid, terrainnom, terrainadresse) VALUES(?, ?, ?);");
            statement.setInt(1, id);
            statement.setString(2, name);
            statement.setString(3, address);
            statement.execute();
            databaseConnection.commit();

        } catch (SQLException | FailedToRetrieveNextKeyFromSequenceException e) {
            try {
                databaseConnection.rollback();
            } catch (SQLException e1) {
                Logger.error(LOG_TYPE.EXCEPTION, e1.getMessage());
            }
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
            statement = databaseConnection.prepareStatement("UPDATE terrain SET terrainnom = ?, terrainadresse = ? WHERE terrainid = ?;");
            statement.setString(1, name);
            statement.setString(2, address);
            statement.setInt(3, id);
            statement.executeUpdate();
            databaseConnection.commit();

        } catch (SQLException e) {
            try {
                databaseConnection.rollback();
            } catch (SQLException e1) {
                Logger.error(LOG_TYPE.EXCEPTION, e1.getMessage());
            }
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
                statement = databaseConnection.prepareStatement("DELETE FROM terrain WHERE terrainid = ?;");
                statement.setInt(1, id);
                statement.executeQuery();
                databaseConnection.commit();

            } catch (SQLException e) {
                try {
                    databaseConnection.rollback();
                } catch (SQLException e1) {
                    Logger.error(LOG_TYPE.EXCEPTION, e1.getMessage());
                }
                throw new FailedToDeleteEntityException(e);
            } finally {
                closeStatement(statement);
            }
        }
        // Else the current field has never been created in the database before.
    }

    /**
     * Get the name of the field.
     *
     * @return String - Name of the field.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the name of the field.
     *
     * @param name - Name of the field.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Get the address of the field.
     *
     * @return String - Address of the field.
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * Set the address of the field.
     *
     * @param address - Address of the field.
     */
    public void setAddress(String address)
    {
        this.address = address;
    }
}
