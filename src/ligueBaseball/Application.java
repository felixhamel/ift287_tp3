package ligueBaseball;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;

import ligueBaseball.Logger.LOG_TYPE;
import ligueBaseball.command.Command;
import ligueBaseball.entities.Match;
import ligueBaseball.entities.Official;
import ligueBaseball.entities.Team;
import ligueBaseball.exceptions.CannotFindTeamWithNameException;
import ligueBaseball.exceptions.CreatePlayerParametersMissingException;
import ligueBaseball.exceptions.FailedToConnectToDatabaseException;
import ligueBaseball.exceptions.FailedToDeleteEntityException;
import ligueBaseball.exceptions.FailedToRetrieveFieldIdException;
import ligueBaseball.exceptions.FailedToRetrieveNextKeyFromSequenceException;
import ligueBaseball.exceptions.FailedToRetrieveTeamIdException;
import ligueBaseball.exceptions.FieldNameAlreadyTakenException;
import ligueBaseball.exceptions.MissingCommandParameterException;
import ligueBaseball.exceptions.NegativeScore;
import ligueBaseball.exceptions.PlayerAlreadyExistsException;
import ligueBaseball.exceptions.TeamCantPlayAgainstItselfException;
import ligueBaseball.exceptions.TeamIsNotEmptyException;
import ligueBaseball.exceptions.TeamNameAlreadyTakenException;
import ligueBaseball.exceptions.UnknownCommandException;

public class Application
{
    private ApplicationParameters parameters;
    private Connection connectionWithDatabase;
    private static HashMap<String, String> actions = new HashMap<>();

    static {
        // Create all the available actions.
        actions.put("creerEquipe", "<EquipeNom> [<NomTerrain> AdresseTerrain]");
        actions.put("afficherEquipes", null);
        actions.put("supprimerEquipe", "<EquipeNom>");
        actions.put("creerJoueur", "<JoueurNom> <JoueurPrenom> [<EquipeNom> <Numero> [<DateDebut>]]");
        actions.put("afficherJoueursEquipe", "[<EquipeNom >]");
        actions.put("supprimerJoueur", "<JoueurNom> <JoueurPrenom>");
        actions.put("creerMatch", "<MatchDate> <MatchHeure> <EquipeNomLocal> <EquipeNomVisiteur>");
        actions.put("creerArbitre", "<ArbitreNom> <ArbitrePrenom>");
        actions.put("afficherArbitres", null);
        actions.put("arbitrerMatch", "<MatchDate> <MatchHeure> <EquipeNomLocal> <EquipeNomVisiteur> <ArbitreNom> <ArbitrePrenom>");
        actions.put("entrerResultatMatch", "<MatchDate> <MatchHeure> <EquipeNomLocal> <EquipeNomVisiteur> <PointsLocal> <PointsVisiteur>");
        actions.put("afficherResultatsDate", "[<APartirDate>]");
        actions.put("afficherResultats", "[<EquipeNom>]");

        actions.put("aide", null);
        actions.put("quitter", null);
    }

    /**
     * Constructor
     *
     * @param parameters - ApplicationParameters
     */
    public Application(ApplicationParameters parameters) {
        this.parameters = parameters;
    }

    /**
     * Open a connection with the database.
     *
     * @throws FailedToConnectToDatabaseException
     */
    private void openConnectionWithDatabase() throws FailedToConnectToDatabaseException
    {
        try {
            String connectionString = "jdbc:postgresql:" + parameters.getDatabaseName();
            Properties connectionParameters = new Properties();
            connectionParameters.setProperty("user", parameters.getUsername());
            connectionParameters.setProperty("password", parameters.getPassword());
            connectionWithDatabase = DriverManager.getConnection(connectionString, connectionParameters);
            connectionWithDatabase.setAutoCommit(false);
        } catch (SQLException e) {
            throw new FailedToConnectToDatabaseException(parameters.getDatabaseName(), e);
        }
    }

    /**
     * Close the opened connection with the database. Won't close it again if already closed.
     */
    private void closeConnectionWithDatabase()
    {
        if (connectionWithDatabase != null) {
            try {
                if (!connectionWithDatabase.isClosed()) {
                    connectionWithDatabase.close();
                }
            } catch (SQLException e) {
                connectionWithDatabase = null;
            }
        }
    }

    /**
     * Launch the application.
     *
     * @throws FailedToConnectToDatabaseException
     * @throws UnknownCommandException
     */
    public void launch() throws FailedToConnectToDatabaseException, UnknownCommandException
    {
        openConnectionWithDatabase();
        executeCommandsFromFile();

        while (true) {
            try {
                Command command = askCommandToUser();
                if (!actions.containsKey(command.getCommandName())) {
                    throw new UnknownCommandException(command.getCommandName());
                } else {
                    executeCommand(command);
                }
            } catch (Exception e) {
                Logger.error(LOG_TYPE.SYSTEM, e.getMessage() + "(" + e.getClass().getName() + ")");
                // e.printStackTrace();
            }
        }
    }

    /**
     * Ask the player enter a command with parameters if needed.
     *
     * @return Command - The requested command by the user.
     */
    private Command askCommandToUser()
    {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.print("$ ");
            return Command.extractCommandFromString(scanner.nextLine().trim());
        } finally {
            // BUG dans Eclipse, si on le ferme ça va faire plein de null pointer exception.
            // Ce bug n'est pas présent si on roule le programme en console.
            // scanner.close();
        }
    }

    /**
     * Execute all the commands inside the file given as the entry file.
     *
     * @throws FileNotFoundException
     */
    private void executeCommandsFromFile()
    {
        if (!parameters.getEntryFile().isEmpty()) {
            Logger.info(LOG_TYPE.OTHER, "Exécution des commandes fournies dans le fichier '%s'...", parameters.getEntryFile());

            Scanner scanner = null;
            try {
                scanner = new Scanner(new File(parameters.getEntryFile()));
                String line;
                while ((line = scanner.nextLine().trim()) != null) {
                    if (!line.startsWith("--") && line.length() > 0) {
                        try {
                            Logger.info(LOG_TYPE.COMMAND, line);
                            executeCommand(Command.extractCommandFromString(line));
                        } catch (Exception e) {
                            Logger.error(LOG_TYPE.EXCEPTION, e.getMessage());
                        }
                    } else {
                        Logger.info(LOG_TYPE.COMMENT, line.substring(2));
                    }
                }
            } catch (FileNotFoundException e) {
                Logger.error(LOG_TYPE.USER, "Impossible de trouver le fichier %s.", parameters.getEntryFile());
            } finally {
                Logger.info(LOG_TYPE.OTHER, "Exécution des commandes terminé.");
                if (scanner != null) {
                    scanner.close();
                }
            }
        }
    }

    /**
     * Execute the command with the informations given by the user.
     *
     * @param command - Command requested by the user.
     */
    private void executeCommand(Command command) throws Exception
    {
        switch (command.getCommandName()) {
            case "creerEquipe":
                createTeam(command.getParameters());
                break;
            case "afficherEquipes":
                displayTeams();
                break;
            case "supprimerEquipe":
                supprimerEquipe(command.getParameters());
                break;
            case "creerJoueur":
                creerJoueur(command.getParameters());
                break;
            case "afficherJoueursEquipe":
                afficherJoueursEquipe(command.getParameters());
                break;
            case "supprimerJoueur":
                deletePlayer(command.getParameters());
                break;
            case "creerMatch":
                creerMatch(command.getParameters());
                break;
            case "creerArbitre":
                creerArbitre(command.getParameters());
                break;
            case "afficherArbitres":
                afficherArbitres();
                break;
            case "arbitrerMatch":
                arbitrerMatch(command.getParameters());
                break;
            case "entrerResultatMatch":
                entrerResultatMatch(command.getParameters());
                break;
            case "afficherResultatsDate":
                afficherResultatsDate(command.getParameters());
                break;
            case "afficherResultats":
                afficherResultats(command.getParameters());
                break;
            case "aide":
                showAvailableActions();
                break;
            case "quitter":
                exitProgram();
            default:
                System.out.println("Commande non implémentée.");
                break;
        }
    }

    /**
     * Create a new team.
     *
     * @param parameters - <EquipeNom> [<NomTerrain> AdresseTerrain]
     */
    private void createTeam(ArrayList<String> parameters) throws SQLException, TeamNameAlreadyTakenException, FieldNameAlreadyTakenException
    {
        Statement statement = null;
        String query = "SELECT equipenom FROM equipe WHERE equipenom = '" + parameters.get(1) + "';";
        try {
            statement = connectionWithDatabase.createStatement();
            ResultSet rs = statement.executeQuery(query);
            if (rs.next()) {
                throw new TeamNameAlreadyTakenException(parameters.get(1));
            } else {
                query = "SELECT * FROM terrain WHERE terrainnom = '" + parameters.get(2) + "';";
                rs = statement.executeQuery(query);
                if (rs.next()) {
                    throw new FieldNameAlreadyTakenException(parameters.get(2));
                } else {
                    String update = null;
                    if (parameters.get(3) != null) {
                        update = "INSERT INTO terrain (terrainnom, terrainadresse) VALUES ('" + parameters.get(2) + "', '" + parameters.get(3) + "');";
                    } else {
                        update = "INSERT INTO terrain (terrainnom) VALUES ('" + parameters.get(2) + "');";
                    }
                    statement.executeUpdate(update);
                    query = "SELECT terrainid FROM terrain WHERE terrainnom = '" + parameters.get(2) + "';";
                    rs = statement.executeQuery(query);
                    update = "INSERT INTO equipe (equipenom, terrainid) VALUES ('" + parameters.get(1) + "', '" + rs.getString("terrainid") + ");";
                    statement.executeUpdate(update);

                    connectionWithDatabase.commit();
                }
            }
        } finally {
            closeStatement(statement);
        }
    }

    /**
     * Display all the teams.
     */
    private void displayTeams()
    {
        List<Team> teams = Team.getAllTeams(connectionWithDatabase);
        for (Team team : teams) {
            System.out.println(String.format("%s, équipe # %s", team.getName(), team.getId()));
        }
    }

    /**
     * Supprimer une equipe
     *
     * @param parameters - <EquipeNom>
     * @throws FailedToDeleteEntityException
     * @throws NumberFormatException
     */
    private void supprimerEquipe(ArrayList<String> parameters) throws TeamIsNotEmptyException, FailedToDeleteEntityException
    {
        Team.getTeamWithName(connectionWithDatabase, parameters.get(0)).delete(connectionWithDatabase);
    }

    /**
     * Creer un joueur
     *
     * @param parameters - <JoueurNom> <JoueurPrenom> [<EquipeNom> <Numero> [<DateDbut>]]
     */
    private void creerJoueur(ArrayList<String> parameters) throws SQLException, CreatePlayerParametersMissingException, PlayerAlreadyExistsException
    {
        if (parameters.get(3) != null || parameters.get(4) != null) {
            if (parameters.get(3) == null || parameters.get(4) == null) {
                throw new CreatePlayerParametersMissingException();
            }
        }
        Statement statement = null;
        String query = "SELECT * FROM joueur WHERE joueurnom = " + parameters.get(1) + " OR joueurprenom = " + parameters.get(2) + ";";
        try {
            statement = connectionWithDatabase.createStatement();
            ResultSet rs = statement.executeQuery(query);
            if (rs.next()) {
                throw new PlayerAlreadyExistsException(parameters.get(2), parameters.get(1));
            } else {
                String update = "INSERT INTO joueur (joueurnom, joueurprenom) VALUES ('" + parameters.get(1) + "', '" + parameters.get(2) + "');";
                statement.executeUpdate(update);
                if (parameters.get(3) != null) {
                    query = "SELECT joueurid FROM joueur WHERE joueurprenom = " + parameters.get(2) + " AND joueurnom = " + parameters.get(1) + ";";
                    rs = statement.executeQuery(query);
                    String joueurId = rs.getString("joueurid");
                    // update = "INSERT INTO faitpartie ("
                    // TODO
                    connectionWithDatabase.commit();
                }
            }
        } finally {
            closeStatement(statement);
        }
    }

    /**
     * Afficher la liste des joueurs
     *
     * @param parameters - [<EquipeNom>]
     * @throws MissingCommandParameterException
     */
    private void afficherJoueursEquipe(ArrayList<String> parameters)
    {
        if (parameters.isEmpty()) {
            try {
                // Go get all the teams
                PreparedStatement statement = connectionWithDatabase.prepareStatement("SELECT * FROM equipe");
                ResultSet teams = statement.executeQuery();
                while (teams.next()) {
                    System.out.println("Équipe: " + teams.getString("equipenom"));
                    showAllPlayersForTeam(teams.getString("equipenom"));
                }
                statement.close();
            } catch (SQLException e) {
                Logger.error(LOG_TYPE.SYSTEM, "Problème lors de la lecture des joueurs de/des équipes.");
            }
        } else {
            showAllPlayersForTeam(parameters.get(0));
        }
    }

    /**
     * Show all the players that plays for the team given in parameters.
     *
     * @param teamName - Name of the Team.
     */
    private void showAllPlayersForTeam(String teamName)
    {
        try {
            PreparedStatement statement = connectionWithDatabase.prepareStatement("SELECT joueur.joueurnom, joueur.joueurprenom, joueur.joueurid " + "FROM faitpartie " + "INNER JOIN joueur ON faitpartie.joueurid = joueur.joueurid " + "INNER JOIN equipe ON faitpartie.equipeid = equipe.equipeid " + "WHERE equipe.equipenom = ? "
                    + "ORDER BY equipe.equipeid ASC, faitpartie.numero ASC;");
            statement.setString(1, teamName);
            ResultSet result = statement.executeQuery();

            // Check if we received any results
            if (!result.isBeforeFirst()) {
                Logger.error(LOG_TYPE.USER, "L'équipe %s n'existe pas ou n'a pas de joueur.", teamName);
            }
            while (result.next()) {
                System.out.println(" -> " + result.getString("joueurprenom") + " " + result.getString("joueurnom") + " #" + result.getInt("joueurid"));
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete a player and all informations related to it.
     *
     * @param parameters - <JoueurNom> <JoueurPrenom>
     * @throws MissingCommandParameterException Missing parameter.
     */
    private void deletePlayer(ArrayList<String> parameters) throws MissingCommandParameterException
    {
        if (parameters.isEmpty()) {
            throw new MissingCommandParameterException("supprimerJoueur", "JoueurNom");
        } else if (parameters.size() == 1) {
            throw new MissingCommandParameterException("supprimerJoueur", "JoueurPrenom");
        }

        try {
            PreparedStatement statement = connectionWithDatabase.prepareStatement("SELECT joueurid FROM joueur WHERE joueurnom = ? AND joueurprenom = ?;");
            statement.setString(1, parameters.get(0));
            statement.setString(2, parameters.get(1));
            ResultSet player = statement.executeQuery();

            if (!player.isBeforeFirst()) {
                Logger.error(LOG_TYPE.USER, "Le joueur '%s %s' n'existe pas.", parameters.get(1), parameters.get(0));
                statement.close();
            } else {
                player.next();
                int playerId = player.getInt("joueurid");
                statement.close();

                // Confirmation
                System.out.println("Êtes-vous certain de vouloir supprimer ce joueur ? (O/N) : ");
                char confirmation = new BufferedReader(new InputStreamReader(System.in)).readLine().trim().charAt(0);
                if (confirmation == 'o' || confirmation == 'O') {
                    deletePlayerFromDatabase(playerId);
                }
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete a player from the database. It will remove all traces of it in plays and others.
     *
     * @param playerId - ID of the player to delete.
     */
    private void deletePlayerFromDatabase(int playerId)
    {
        try {
            connectionWithDatabase.setAutoCommit(false);

            PreparedStatement deleteParticipations = connectionWithDatabase.prepareStatement("DELETE FROM participe WHERE joueurid = ?;");
            try {
                deleteParticipations.setInt(1, playerId);
                deleteParticipations.executeUpdate();
            } catch (SQLException e) {
                connectionWithDatabase.rollback();
                Logger.error(LOG_TYPE.SYSTEM, "Problème lors du retrait du joueur dans la table 'participe'.");
                return;
            } finally {
                deleteParticipations.close();
            }

            PreparedStatement deleteInTeam = connectionWithDatabase.prepareStatement("DELETE FROM faitpartie WHERE joueurid = ?;");
            try {
                deleteInTeam.setInt(1, playerId);
                deleteInTeam.executeUpdate();
            } catch (SQLException e) {
                connectionWithDatabase.rollback();
                Logger.error(LOG_TYPE.SYSTEM, "Problème lors du retrait du joueur dans la table 'faitpartie'.");
                return;
            } finally {
                deleteInTeam.close();
            }

            PreparedStatement deleteJoueur = connectionWithDatabase.prepareStatement("DELETE FROM joueur WHERE joueurid = ?;");
            try {
                deleteJoueur.setInt(1, playerId);
                deleteJoueur.executeUpdate();
            } catch (SQLException e) {
                connectionWithDatabase.rollback();
                Logger.error(LOG_TYPE.SYSTEM, "Problème lors du retrait du joueur dans la table 'joueur'.");
                return;
            } finally {
                deleteJoueur.close();
            }

            // Commit modifications
            connectionWithDatabase.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ajouter un match, en calculant le MatchId automatiquement
     *
     * @param parameters - <MatchDate> <MatchHeure> <EquipeNomLocal> <EquipeNomVisiteur>
     * @throws MissingCommandParameterException
     * @throws TeamCantPlayAgainstItselfException
     * @throws FailedToRetrieveFieldIdException
     * @throws CannotFindTeamWithNameException
     * @throws FailedToRetrieveTeamIdException
     * @throws FailedToRetrieveNextKeyFromSequenceException
     */
    private void creerMatch(ArrayList<String> parameters) throws MissingCommandParameterException, TeamCantPlayAgainstItselfException, CannotFindTeamWithNameException, FailedToRetrieveFieldIdException, FailedToRetrieveTeamIdException, FailedToRetrieveNextKeyFromSequenceException
    {
        // Validate parameters
        switch (parameters.size()) {
            case 0:
                throw new MissingCommandParameterException("creerMatch", "MatchDate");
            case 1:
                throw new MissingCommandParameterException("creerMatch", "MatchHeure");
            case 2:
                throw new MissingCommandParameterException("creerMatch", "EquipeNomLocal");
            case 3:
                throw new MissingCommandParameterException("creerMatch", "EquipeNomVisiteur");
            default:
                // Ok !
        }

        // Verifications
        if (parameters.get(2).equalsIgnoreCase(parameters.get(3))) {
            throw new TeamCantPlayAgainstItselfException(parameters.get(2));
        }

        // Ex.: creerMatch Red_Sox Yankees 2000-01-01 08:00:00

        int nextId = getNextIdForTable("match", "matchid");

        // Insert
        PreparedStatement statement = null;
        try {
            statement = connectionWithDatabase.prepareStatement("INSERT INTO match (matchid, equipelocal, terrainid , equipevisiteur, matchdate, matchheure) " + "SELECT " + "? as matchid, " + "(SELECT equipeid FROM equipe WHERE equipenom = ?) AS equipevisiteur, " + "equipe.equipeid AS equipelocal, " + "equipe.terrainid AS terrainid, "
                    + "? AS matchdate, " + "? AS matcheure " + "FROM equipe " + "WHERE equipenom = ?;");
            statement.setInt(1, nextId);
            statement.setString(2, parameters.get(1));
            statement.setDate(3, Date.valueOf(parameters.get(2)));
            statement.setTime(4, Time.valueOf(parameters.get(3)));
            statement.setString(5, parameters.get(0));

            statement.execute();
            connectionWithDatabase.commit();

        } catch (SQLException e) {
            Logger.error(LOG_TYPE.SYSTEM, "Problème lors de la création du match.");
            e.printStackTrace();
        } finally {
            closeStatement(statement);
        }
    }

    /**
     * Retrieve the next ID for the given table name.
     *
     * @param tableName - Name of the table we want the next primary id.
     * @param keyColumnName - Name of the column where is the primary key in the table given in the first parameter.
     * @return int - ID to use.
     * @throws FailedToRetrieveNextKeyFromSequenceException Thrown if there is a problem while retriving the next ID to use.
     */
    private synchronized int getNextIdForTable(String tableName, String keyColumnName) throws FailedToRetrieveNextKeyFromSequenceException
    {
        PreparedStatement statement = null;

        try {
            statement = connectionWithDatabase.prepareStatement("SELECT nextcle FROM sequence WHERE nomtable = ?;");
            statement.setString(1, tableName);
            ResultSet result = statement.executeQuery();

            if (!result.next()) {
                // Do not exists in the sequence table
                closeStatement(statement);
                statement = connectionWithDatabase.prepareStatement("INSERT INTO sequence (nomtable, nextcle) SELECT ? AS nomtable, (MAX(" + keyColumnName + ") + 1) AS nextcle FROM " + tableName + ";");
                statement.setString(1, tableName);

                statement.execute();
                connectionWithDatabase.commit();
                closeStatement(statement);

                // Recurcivity because we now have an entry in this table.
                return getNextIdForTable(tableName, keyColumnName);
            }

            int nextId = result.getInt("nextcle");
            closeStatement(statement);

            // Increment current value
            statement = connectionWithDatabase.prepareStatement("UPDATE sequence SET nextcle = nextcle + 1 WHERE nomtable = ?;");
            statement.setString(1, tableName);
            statement.executeUpdate();
            connectionWithDatabase.commit();

            return nextId;

        } catch (SQLException e) {
            throw new FailedToRetrieveNextKeyFromSequenceException(tableName);

        } finally {
            closeStatement(statement);
        }
    }

    /**
     * Crée un nouvel arbitre, en calculant le ArbitreId automatiquement
     *
     * @param parameters - <ArbitreNom> <ArbitrePrenom>
     */
    private void creerArbitre(ArrayList<String> parameters)
    {
        PreparedStatement statement = null;

        try {
            statement = connectionWithDatabase.prepareStatement("SELECT * FROM arbitre WHERE arbitrenom = ? AND arbitreprenom = ?;");
            statement.setString(1, parameters.get(0));
            statement.setString(2, parameters.get(1));

            ResultSet arbitres = statement.executeQuery();
            if (!arbitres.isBeforeFirst()) {
                Logger.error(LOG_TYPE.USER, "L'arbitre existe déjà.");
                return;
            } else {
                closeStatement(statement);
            }

            int nextId = getNextIdForTable("arbitre", "arbitreid");

            statement = connectionWithDatabase.prepareStatement("INSERT INTO arbitre (arbitreid, arbitrenom, arbitreprenom) VALUES(?, ?, ?);");
            statement.setInt(1, nextId);
            statement.setString(2, parameters.get(0));
            statement.setString(3, parameters.get(1));
            statement.executeUpdate();

            closeStatement(statement);
            System.out.println("Ajout fait avec succès.");

        } catch (Exception e) {
            Logger.error(LOG_TYPE.SYSTEM, "Problème lors de l'ajout dans la table 'arbitre'.");

        } finally {
            closeStatement(statement);
        }
    }

    /**
     * Afficher la liste des arbitres en ordre alphabétique
     */
    private void afficherArbitres()
    {
        PreparedStatement statement = null;

        try {
            statement = connectionWithDatabase.prepareStatement("SELECT arbitrenom, arbitreprenom FROM arbitre ORDER BY arbitrenom;");
            ResultSet arbitres = statement.executeQuery();

            System.out.println("Les arbitres sont: ");
            while (arbitres.next()) {
                System.out.println(String.format("%s %s", arbitres.getString("arbitrenom"), arbitres.getString("arbitreprenom")));
            }

        } catch (SQLException e) {
            Logger.error(LOG_TYPE.SYSTEM, "Problème lors de la requête dans la table 'arbitre'.");

        } finally {
            closeStatement(statement);
        }
    }

    /**
     * Affecter des arbitres à un match
     *
     * @param parameters - <MatchDate> <MatchHeure> <EquipeNomLocal> <EquipeNomVisiteur> <ArbitreNom> <ArbitrePrenom>
     */
    private void arbitrerMatch(ArrayList<String> parameters)
    {
        boolean trouver = false;
        int nbrArbitres = 0;
        PreparedStatement statement = null;
        try {
            statement = connectionWithDatabase.prepareStatement("select matchid from match " + "left outer join equipe as local on local.equipeid = match.equipelocal " + "left outer join equipe as visiteur on visiteur.equipeid = match.equipevisiteur " + "where match.matchdate = '" + parameters.get(0) + "' and match.matchheure = '"
                    + parameters.get(1) + "' and " + "local.equipenom = '" + parameters.get(2) + "' and visiteur.equipenom = '" + parameters.get(3) + "' ;");
            ResultSet matchs = statement.executeQuery();
            while (matchs.next()) {
                trouver = true;
            }
            if (trouver == true) {
                trouver = false;
                closeStatement(statement);
                statement = connectionWithDatabase.prepareStatement("SELECT * FROM arbitre where arbitrenom =" + "'" + parameters.get(4) + "' and arbitreprenom = '" + parameters.get(5) + "';");
                ResultSet arbitre = statement.executeQuery();
                while (arbitre.next()) {
                    trouver = true;
                }
                if (trouver == true) {
                    closeStatement(statement);
                    statement = connectionWithDatabase.prepareStatement("SELECT count(*) as nbr from arbitrer " + "left outer join match on match.matchid = arbitrer.matchid " + "left outer join equipe as local on local.equipeid = match.equipelocal " + "left outer join equipe as visiteur on visiteur.equipeid = match.equipevisiteur "
                            + "where match.matchdate = '" + parameters.get(0) + "' and match.matchheure = '" + parameters.get(1) + "' and " + "local.equipenom = '" + parameters.get(2) + "' and visiteur.equipenom = '" + parameters.get(3) + "' ;");
                    ResultSet nbrarbitres = statement.executeQuery();
                    while (nbrarbitres.next()) {
                        nbrArbitres = nbrarbitres.getInt("nbr");
                    }
                    if (nbrArbitres < 4) {
                        closeStatement(statement);
                        statement = connectionWithDatabase.prepareStatement("INSERT INTO arbitrer(arbitreid, matchid) " + "VALUES ((SELECT arbitreid FROM arbitre where arbitrenom =" + "'" + parameters.get(4) + "' and arbitreprenom = '" + parameters.get(5) + "'), " + "(select matchid from match left outer join equipe as local on local.equipeid = "
                                + "match.equipelocal left outer join equipe as visiteur on visiteur.equipeid = " + "match.equipevisiteur where match.matchdate = '" + parameters.get(0) + "' and match.matchheure = '" + parameters.get(1) + "' and local.equipenom = '" + parameters.get(2) + "' and visiteur.equipenom = '" + parameters.get(3) + "' ));");

                        statement.executeUpdate();
                        connectionWithDatabase.commit();
                        Logger.info(LOG_TYPE.SYSTEM, "Ajout fait avec succès.");

                    } else {
                        Logger.error(LOG_TYPE.USER, "Il y a déjà 4 arbitres pour ce match.");
                    }
                } else {
                    Logger.error(LOG_TYPE.USER, "L'arbitre n'existe pas.");
                }
            } else {
                Logger.error(LOG_TYPE.USER, "Le match n'existe pas.");
            }
        } catch (SQLException e) {
            Logger.error(LOG_TYPE.SYSTEM, "Problème lors de l'ajout dans la table 'arbitrer'.");
        } finally {
            closeStatement(statement);
        }
    }

    /**
     * Entrer le résultat d'un match.
     *
     * @param parameters - <MatchDate> <MatchHeure> <EquipeNomLocal> <EquipeNomVisiteur> <PointsLocal> <PointsVisiteur>
     */
    private void entrerResultatMatch(ArrayList<String> parameters) throws MissingCommandParameterException, NegativeScore
    {
    	
    	if(Integer.parseInt(parameters.get(4)) < 0 || Integer.parseInt(parameters.get(5)) < 0){
    		throw new NegativeScore();
		}
    	
    	//Update
    	//EX : entrerResultatMatch 2007-06-16 19:30:00 Yankees Mets 45 22
    	//EX : entrerResultatMatch 2000-01-01 08:00:00 Yankees Red_Sox 70 30
    	
    	PreparedStatement statement = null;
    	try{
        	statement = connectionWithDatabase
        			.prepareStatement("UPDATE match SET pointslocal = ?, pointsvisiteur = ? WHERE "
        					+ "equipelocal = (SELECT equipeid FROM equipe WHERE equipenom = ?) AND "
        					+ "equipevisiteur = (SELECT equipeid FROM equipe WHERE equipenom = ?) AND "
        					+ "matchdate = ? AND "
        					+ "matchheure = ?");
        	statement.setInt(1, Integer.parseInt(parameters.get(4)));
        	statement.setInt(2, Integer.parseInt(parameters.get(5)));
        	statement.setString(3,parameters.get(2));
        	statement.setString(4,parameters.get(3));
        	statement.setDate(5, Date.valueOf(parameters.get(0)));
        	statement.setTime(6, Time.valueOf(parameters.get(1)));
        	statement.execute();
        	connectionWithDatabase.commit();
        	
        	
        	

        } catch (SQLException e) {
            Logger.error(LOG_TYPE.SYSTEM, "Probl�me lors de la cr�ation du match.");
            e.printStackTrace();
        } finally {
            closeStatement(statement);
        }
    }

    /**
     * Afficher les résultats de tous les matchs
     *
     * @param parameters - [<APartirDate>]
     */
    private void afficherResultatsDate(ArrayList<String> parameters)
    {
    	//afficherResultatsDate 2000-01-01
    	
        List<Match> matchs;
        List<Official> official;
        
        if(parameters.isEmpty() == false){matchs = Match.getMatchWithDate(connectionWithDatabase, parameters.get(0));}
        else {matchs = Match.getAllMatch(connectionWithDatabase);}
        
        for (Match match : matchs){
        	System.out.println(String.format("%-10s %-10s %-5s %-5s %-12s %-10s","Equipelocal", "Equipevisiteur", "Scorelocal", "ScoreVisiteur", " Matchdate", "MatchHeure"));
        	System.out.println(String.format("%-11s %-15s %-10s %-13s %-11s %-10s"
        			,match.getLocalTeam(connectionWithDatabase).getName()
        			,match.getVisitorTeam(connectionWithDatabase).getName()
        			,match.getLocalTeamScore()
        			,match.getVisitorTeamScore()
        			,match.getDate()
        			,match.getTime()));
        	
        	official = match.getOfficials(connectionWithDatabase);
        	System.out.println(String.format("\n"));
        	System.out.println(String.format("Liste des arbitres"));
        	if (official.size() !=0){
        	for(Official offi : official){
        			System.out.println(String.format("%-10s %-10s"
        					,offi.getFirstName()
        					,offi.getLastName()));
        		}
        	}
        	else {System.out.println(String.format("Aucun arbitre durant le match."));}
        	System.out.println(String.format("\n"));
        }
    }

    /**
     * Afficher les résultats des matchs où une équipe a participé
     *
     * @param parameters - [<EquipeNom>]
     */
    private void afficherResultats(ArrayList<String> parameters)
    {
        //afficherResultats Yankees
    	
    	List<Match> matchs = Match.getMatchForTeam(connectionWithDatabase, parameters.get(0));
    	List<Official> official;

        for (Match match : matchs){
        	System.out.println(String.format("%-10s %-10s %-5s %-5s %-12s %-10s","Equipelocal", "Equipevisiteur", "Scorelocal", "ScoreVisiteur", " Matchdate", "MatchHeure"));
        	System.out.println(String.format("%-11s %-15s %-10s %-13s %-11s %-10s"
        			,match.getLocalTeam(connectionWithDatabase).getName()
        			,match.getVisitorTeam(connectionWithDatabase).getName()
        			,match.getLocalTeamScore()
        			,match.getVisitorTeamScore()
        			,match.getDate()
        			,match.getTime()));
        	official = match.getOfficials(connectionWithDatabase);
        	System.out.println(String.format("\n"));
        	System.out.println(String.format("Liste des arbitres"));
        	if (official.size() !=0){
        	for(Official offi : official){
        			System.out.println(String.format("%-10s %-10s"
        					,offi.getFirstName()
        					,offi.getLastName()));
        		}
        	}
        	else {System.out.println(String.format("Aucun arbitre durant le match."));}
        	System.out.println(String.format("\n"));
        }
    }

    /**
     * Show all the available actions to the user.
     */
    private void showAvailableActions()
    {
        System.out.println("Liste de toutes les commandes disponibles : ");
        for (Entry<String, String> entry : actions.entrySet()) {
            System.out.println(" - " + entry.getKey() + " : " + entry.getValue());
        }
    }

    /**
     * Close the statement if not null
     *
     * @param stmt - SQL statement
     */
    private void closeStatement(Statement stmt)
    {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Exception e) {
                System.out.println("Exception (" + e.getClass().getName() + "): " + e.getMessage());
                // e.printStackTrace();
            }
        }
    }

    /**
     * Close connection with database and exit.
     */
    private void exitProgram()
    {
        closeConnectionWithDatabase();
        System.exit(0);
    }
}
