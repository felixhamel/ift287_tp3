package ligueBaseball.entities;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import ligueBaseball.Logger;
import ligueBaseball.Logger.LOG_TYPE;
import ligueBaseball.exceptions.FailedToDeleteEntityException;
import ligueBaseball.exceptions.FailedToRetrieveNextKeyFromSequenceException;
import ligueBaseball.exceptions.FailedToSaveEntityException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class Match extends DatabaseEntity
{
    private int localTeamId;
    private int visitorTeamId;
    private int fieldId;
    private Date date;
    private Time time;
    private int localTeamScore = 0;
    private int visitorTeamScore = 0;

    /**
     * Get the match that match with the given ID.
     *
     * @param databaseConnection
     * @param id - ID of the match to find.
     * @return Match - If found, otherwise return null.
     */
    public static Match getMatchWithId(Connection databaseConnection, int id)
    {
        PreparedStatement statement = null;

        try {
            statement = databaseConnection.prepareStatement("SELECT * FROM match WHERE matchid = ?;");
            statement.setInt(1, id);

            ResultSet matchResult = statement.executeQuery();
            if (!matchResult.next()) {
                return null;
            }
            return getEntityFromResultSet(matchResult);

        } catch (SQLException e) {
            return null;

        } finally {
            closeStatement(statement);
        }
    }

    private static Match getEntityFromResultSet(ResultSet resultSet) throws SQLException
    {
        Match match = new Match();
        match.id = resultSet.getInt("matchid");
        match.localTeamId = resultSet.getInt("equipelocal");
        match.visitorTeamId = resultSet.getInt("equipevisiteur");
        match.fieldId = resultSet.getInt("terrainid");
        match.date = resultSet.getDate("matchdate");
        match.time = resultSet.getTime("matchheure");
        match.localTeamScore = resultSet.getInt("pointslocal");
        match.visitorTeamScore = resultSet.getInt("pointsvisiteur");

        return match;
    }

    @Override
    protected void create(Connection databaseConnection) throws FailedToSaveEntityException
    {
        PreparedStatement statement = null;
        try {
            id = getNextIdForTable(databaseConnection, "match", "matchid");
            statement = databaseConnection.prepareStatement("INSERT INTO match (matchid, equipelocal, equipevisiteur, terrainid, matchdate, matchheure, pointslocal, pointsvisiteur) VALUES(?, ?, ?, ?, ?, ?, ?, ?);");
            statement.setInt(1, id);
            statement.setInt(2, localTeamId);
            statement.setInt(3, visitorTeamId);
            statement.setInt(4, fieldId);
            statement.setDate(5, date);
            statement.setTime(6, time);
            statement.setInt(7, localTeamScore);
            statement.setInt(8, visitorTeamScore);
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
            id = getNextIdForTable(databaseConnection, "match", "matchid");
            statement = databaseConnection.prepareStatement("UPDATE match SET equipelocal = ? AND equipevisiteur = ? AND terrainid = ? AND matchdate = ? AND matchheure = ? AND pointslocal = ? AND pointsvisiteur = ? WHERE matchid = ?;");
            statement.setInt(1, localTeamId);
            statement.setInt(2, visitorTeamId);
            statement.setInt(3, fieldId);
            statement.setDate(4, date);
            statement.setTime(5, time);
            statement.setInt(6, localTeamScore);
            statement.setInt(7, visitorTeamScore);
            statement.setInt(8, id);
            statement.executeUpdate();
            databaseConnection.commit();

        } catch (SQLException | FailedToRetrieveNextKeyFromSequenceException e) {
            throw new FailedToSaveEntityException(e);
        } finally {
            closeStatement(statement);
        }
    }

    @Override
    public void delete(Connection databaseConnection) throws FailedToDeleteEntityException, Exception
    {
        throw new NotImplementedException();
    }

    /**
     * Get the officials for this match, if any.
     *
     * @param databaseConnection
     * @return List of the officials that where there for the match.
     */
    public List<Official> getOfficials(Connection databaseConnection)
    {
        List<Official> officials = new ArrayList<>();
        PreparedStatement statement = null;
        try {
            statement = databaseConnection.prepareStatement("SELECT arbitreid FROM arbitrer WHERE matchid = ?;");
            statement.setInt(1, id);
            ResultSet officialResultSet = statement.executeQuery();
            while (officialResultSet.next()) {
                officials.add(Official.getOfficialWithId(databaseConnection, officialResultSet.getInt("arbitreid")));
            }
        } catch (SQLException e) {
            Logger.error(LOG_TYPE.EXCEPTION, e.getMessage());
        } finally {
            closeStatement(statement);
        }
        return officials;
    }

    /**
     * Add an official that was present at this match. Match MUST have been saved in the database before doing this.
     *
     * @param databaseConnection
     * @param official - Official that was present at this match.
     * @throws FailedToSaveEntityException
     */
    public void addOfficial(Connection databaseConnection, Official official) throws FailedToSaveEntityException
    {
        PreparedStatement statement = null;
        try {
            statement = databaseConnection.prepareStatement("INSERT INTO arbitrer (arbitreid, matchid) VALUES(?, ?);");
            statement.setInt(1, id);
            statement.setInt(2, official.getId());
            statement.execute();
            databaseConnection.commit();

        } catch (SQLException e) {
            throw new FailedToSaveEntityException(e);
        } finally {
            closeStatement(statement);
        }
    }

    /**
     * Get the local team for this match.
     *
     * @param databaseConnection
     * @return Team - If found, otherwise return null.
     */
    public Team getLocalTeam(Connection databaseConnection)
    {
        return Team.getTeamWithId(databaseConnection, localTeamId);
    }

    /**
     * Set the local team for this match.
     *
     * @param localTeam - Local team.
     */
    public void setLocalTeam(Team localTeam)
    {
        this.localTeamId = localTeam.getId();
    }

    /**
     * Get the visitor team for this match.
     *
     * @param databaseConnection
     * @return Team - If found, otherwise return null.
     */
    public Team getVisitorTeam(Connection databaseConnection)
    {
        return Team.getTeamWithId(databaseConnection, visitorTeamId);
    }

    /**
     * Set the visitor team for this match.
     *
     * @param visitorTeam - Visitor team.
     */
    public void setVisitorTeam(Team visitorTeam)
    {
        this.visitorTeamId = visitorTeam.getId();
    }

    /**
     * Get the fiels the match was/will be played on.
     *
     * @param databaseConnection
     * @return Field - If found, otherwise return null.
     */
    public Field getField(Connection databaseConnection)
    {
        return Field.getFieldWithId(databaseConnection, fieldId);
    }

    /**
     * Set the field for this match.
     *
     * @param field - Match was played on this field.
     */
    public void setField(Field field)
    {
        this.fieldId = field.getId();
    }

    /**
     * Get the date of the match.
     *
     * @return Date - Date of the match.
     */
    public Date getDate()
    {
        return date;
    }

    /**
     * Set the date of the match.
     *
     * @param date - Date of the match.
     */
    public void setDate(Date date)
    {
        this.date = date;
    }

    /**
     * Get the time of the match.
     *
     * @return Time - Time of the match.
     */
    public Time getTime()
    {
        return time;
    }

    /**
     * Set the time of the match.
     *
     * @param time - Time of the match.
     */
    public void setTime(Time time)
    {
        this.time = time;
    }

    /**
     * Get the local team score.
     *
     * @return int - Local team score.
     */
    public final int getLocalTeamScore()
    {
        return localTeamScore;
    }

    /**
     * Set the local team score.
     *
     * @param localTeamScore - Local team score.
     */
    public void setLocalTeamScore(int localTeamScore)
    {
        this.localTeamScore = localTeamScore;
    }

    /**
     * Get the visitor team score.
     *
     * @return int - Visitor team score.
     */
    public final int getVisitorTeamScore()
    {
        return visitorTeamScore;
    }

    /**
     * Set the visitor team score.
     *
     * @param visitorTeamScore - Visitor team score.
     */
    public void setVisitorTeamScore(int visitorTeamScore)
    {
        this.visitorTeamScore = visitorTeamScore;
    }

}
