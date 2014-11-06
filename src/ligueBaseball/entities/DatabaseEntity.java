package ligueBaseball.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ligueBaseball.exceptions.FailedToDeleteEntityException;
import ligueBaseball.exceptions.FailedToRetrieveNextKeyFromSequenceException;
import ligueBaseball.exceptions.FailedToSaveEntityException;

public abstract class DatabaseEntity
{
    protected int id = -1;

    /**
     * Return the ID related to this entity.
     *
     * @return ID - Unique ID.
     */
    public final int getId()
    {
        return id;
    }

    /**
     * Save the current entity
     *
     * @param databaseConnection - Connection with database
     * @throws FailedToSaveEntityException Failed to save entity.
     */
    public void save(Connection databaseConnection) throws FailedToSaveEntityException
    {
        if (id < 0) {
            create(databaseConnection);
        } else {
            update(databaseConnection);
        }
    }

    /**
     * Create the new entity.
     *
     * @param databaseConnection - Connection with database
     * @throws FailedToSaveEntityException Failed to save entity.
     */
    protected abstract void create(Connection databaseConnection) throws FailedToSaveEntityException;

    /**
     * Update the current entity.
     *
     * @param databaseConnection - Connection with database
     * @throws FailedToSaveEntityException Failed to save entity.
     */
    protected abstract void update(Connection databaseConnection) throws FailedToSaveEntityException;

    /**
     * Delete the current entity.
     *
     * @param databaseConnection - Connection with database
     * @throws FailedToDeleteEntityException Failed to delete entity.
     */
    public abstract void delete(Connection databaseConnection) throws FailedToDeleteEntityException, Exception;

    /**
     * Close the given statement if not null.
     *
     * @param statement - Statement.
     */
    protected static void closeStatement(Statement statement)
    {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                // Nothing
            }
        }
    }

    /**
     * Retrieve the next ID for the given table name.
     *
     * @param databaseConnection - Connection with database
     * @param tableName - Name of the table we want the next primary id.
     * @param keyColumnName - Name of the column where is the primary key in the table given in the first parameter.
     * @return int - ID to use.
     * @throws FailedToRetrieveNextKeyFromSequenceException Thrown if there is a problem while retriving the next ID to use.
     */
    protected synchronized int getNextIdForTable(Connection databaseConnection, String tableName, String keyColumnName) throws FailedToRetrieveNextKeyFromSequenceException
    {
        PreparedStatement statement = null;

        try {
            statement = databaseConnection.prepareStatement("SELECT nextcle FROM sequence WHERE nomtable = ?;");
            statement.setString(1, tableName);
            ResultSet result = statement.executeQuery();

            if (!result.next()) {
                // Do not exists in the sequence table
                closeStatement(statement);
                statement = databaseConnection.prepareStatement("INSERT INTO sequence (nomtable, nextcle) SELECT ? AS nomtable, (MAX(" + keyColumnName + ") + 1) AS nextcle FROM " + tableName + ";");
                statement.setString(1, tableName);

                statement.execute();
                databaseConnection.commit();
                closeStatement(statement);

                // Recurcivity because we now have an entry in this table.
                return getNextIdForTable(databaseConnection, tableName, keyColumnName);
            }

            int nextId = result.getInt("nextcle");
            closeStatement(statement);

            // Increment current value
            statement = databaseConnection.prepareStatement("UPDATE sequence SET nextcle = nextcle + 1 WHERE nomtable = ?;");
            statement.setString(1, tableName);
            statement.executeUpdate();
            databaseConnection.commit();

            return nextId;

        } catch (SQLException e) {
            throw new FailedToRetrieveNextKeyFromSequenceException(tableName);

        } finally {
            closeStatement(statement);
        }
    }
}
