package ligueBaseball;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;

import ligueBaseball.Logger.LOG_TYPE;
import ligueBaseball.command.Command;
import ligueBaseball.entities.Field;
import ligueBaseball.entities.Match;
import ligueBaseball.entities.Official;
import ligueBaseball.entities.Player;
import ligueBaseball.entities.Team;
import ligueBaseball.exceptions.CannotFindTeamWithNameException;
import ligueBaseball.exceptions.CreatePlayerParametersMissingException;
import ligueBaseball.exceptions.FailedToConnectToDatabaseException;
import ligueBaseball.exceptions.FailedToDeleteEntityException;
import ligueBaseball.exceptions.FailedToRetrieveFieldIdException;
import ligueBaseball.exceptions.FailedToRetrieveNextKeyFromSequenceException;
import ligueBaseball.exceptions.FailedToRetrievePlayersOfTeamException;
import ligueBaseball.exceptions.FailedToRetrieveTeamIdException;
import ligueBaseball.exceptions.FailedToSaveEntityException;
import ligueBaseball.exceptions.MissingCommandParameterException;
import ligueBaseball.exceptions.PlayerAlreadyExistsException;
import ligueBaseball.exceptions.TeamCantPlayAgainstItselfException;
import ligueBaseball.exceptions.TeamDoesntExistException;
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
                deleteTeam(command.getParameters());
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
                createMatch(command.getParameters());
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
     * @throws FailedToSaveEntityException
     * @throws MissingCommandParameterException
     * @throws TeamNameAlreadyTakenException
     */
    private void createTeam(ArrayList<String> parameters) throws FailedToSaveEntityException, MissingCommandParameterException, TeamNameAlreadyTakenException
    {
        if (parameters.size() < 1) {
            throw new MissingCommandParameterException("creerEquipe", "EquipeNom");
        }

        // Prepare field
        Field field = null;
        if (parameters.size() > 1) {
            // Check if field already exists
            field = Field.getFieldWithName(connectionWithDatabase, parameters.get(1));
            if (field == null) {
                field = new Field();
                field.setName(parameters.get(1));
                if (parameters.size() > 2) {
                    field.setAddress(parameters.get(2));
                }
                field.save(connectionWithDatabase);
            }
        }

        // Check if team already exists
        Team team = Team.getTeamWithName(connectionWithDatabase, parameters.get(0));
        if (team != null) {
            throw new TeamNameAlreadyTakenException(parameters.get(0));
        }
        team = new Team();
        team.setName(parameters.get(0));
        team.setField(connectionWithDatabase, field);
        team.save(connectionWithDatabase);
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
     * Delete a team.
     *
     * @param parameters - <EquipeNom>
     * @throws FailedToDeleteEntityException
     * @throws MissingCommandParameterException
     * @throws NumberFormatException
     */
    private void deleteTeam(ArrayList<String> parameters) throws TeamIsNotEmptyException, FailedToDeleteEntityException, MissingCommandParameterException
    {
        if (parameters.size() < 1) {
            throw new MissingCommandParameterException("supprimerEquipe", "EquipeNom");
        }

        Team team = Team.getTeamWithName(connectionWithDatabase, parameters.get(0));
        if (team == null) {
            Logger.error(LOG_TYPE.USER, "L'équipe %s n'existe pas.", parameters.get(0));
        } else {
            team.delete(connectionWithDatabase);
        }
    }

    /**
     * Create a player
     * @param parameters - <JoueurNom> <JoueurPrenom> [<EquipeNom> <Numero> [<DateDbut>]]
     * @throws MissingCommandParameterException
     * @throws TeamDoesntExistException
     * @throws FailedToSaveEntityException
     * @throws ParseException 
     */
    private void creerJoueur(ArrayList<String> parameters) throws MissingCommandParameterException, TeamDoesntExistException, FailedToSaveEntityException, NullPointerException, IllegalArgumentException, ParseException
    {
    	if (parameters.get(3) != null || parameters.get(4) != null) {
    		
    		//If param 3 or 4 exists, then they both can't be null
    		if (parameters.get(3) == null) {
    			throw new MissingCommandParameterException("EquipeNom", "creerJoueur");
    		}
    		else if (parameters.get(4) == null) {
    			throw new MissingCommandParameterException("Numero", "creerJoueur");
    		}
    		
    		//Check if team exists
            Team team = Team.getTeamWithName(connectionWithDatabase, parameters.get(3));
            if (team == null) {
                throw new TeamDoesntExistException(parameters.get(3));
            }
    	}
    	
        Player player = new Player();
        player.setFirstName(parameters.get(2));
        player.setLastName(parameters.get(1));
        if (parameters.get(3) != null) { //Checking only 1 param validates both
    		player.setTeam(connectionWithDatabase, Team.getTeamWithName(connectionWithDatabase, parameters.get(3)));
    		player.setNumber(Integer.parseInt(parameters.get(4)));    		
        }
        if (parameters.get(5) != null) {
        	player.setDate(new SimpleDateFormat("yyyy-mm-dd", Locale.FRENCH).parse(parameters.get(5)));
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
            List<Team> teams = Team.getAllTeams(connectionWithDatabase);
            for (Team team : teams) {
                showAllPlayersForTeam(team.getName());
            }
        } else {
            showAllPlayersForTeam(parameters.get(0));
        }
    }

    /**
     * Show all the players that plays for the team given in parameters.
     *
     * @param teamName - Name of the Team.
     * @throws FailedToRetrievePlayersOfTeamException
     */
    private void showAllPlayersForTeam(String teamName)
    {
        try {
            Team team = Team.getTeamWithName(connectionWithDatabase, teamName);
            if (team == null) {
                Logger.error(LOG_TYPE.USER, "L'équipe %s n'existe pas.", teamName);
            } else {
                System.out.println(String.format("Équipe: %s", team.getName()));
                List<Player> players = team.getPlayers(connectionWithDatabase);
                if (players.isEmpty()) {
                    System.out.println(" -> Aucun joueur ne fait partie de cette équipe.");
                } else {
                    for (Player player : players) {
                        System.out.println(String.format(" -> %s %s #%s", player.getFirstName(), player.getLastName(), player.getNumber()));
                    }
                }
            }
        } catch (FailedToRetrievePlayersOfTeamException e) {
            Logger.error(LOG_TYPE.EXCEPTION, e.getMessage());
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
        Player player = Player.getPlayerWithName(connectionWithDatabase, parameters.get(1), parameters.get(0));
        if (player == null) {
            Logger.error(LOG_TYPE.USER, "Le joueur '%s %s' n'existe pas.", parameters.get(1), parameters.get(0));
        } else {
            try {
                // Confirmation
                System.out.println("Êtes-vous certain de vouloir supprimer ce joueur ? (O/N) : ");
                char confirmation = new BufferedReader(new InputStreamReader(System.in)).readLine().trim().charAt(0);
                if (confirmation == 'o' || confirmation == 'O') {
                    deletePlayerFromDatabase(player);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Delete a player from the database. It will remove all traces of it in plays and others.
     *
     * @param playerId - ID of the player to delete.
     * @throws FailedToDeleteEntityException
     */
    private void deletePlayerFromDatabase(Player player) throws FailedToDeleteEntityException
    {
        try {
            connectionWithDatabase.setAutoCommit(false);

            PreparedStatement deleteParticipations = connectionWithDatabase.prepareStatement("DELETE FROM participe WHERE joueurid = ?;");
            try {
                deleteParticipations.setInt(1, player.getId());
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
                deleteInTeam.setInt(1, player.getId());
                deleteInTeam.executeUpdate();
            } catch (SQLException e) {
                connectionWithDatabase.rollback();
                Logger.error(LOG_TYPE.SYSTEM, "Problème lors du retrait du joueur dans la table 'faitpartie'.");
                return;
            } finally {
                deleteInTeam.close();
            }

            // Commit modifications
            connectionWithDatabase.commit();

            player.delete(connectionWithDatabase);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a new match.
     *
     * @param parameters - <MatchDate> <MatchHeure> <EquipeNomLocal> <EquipeNomVisiteur>
     * @throws MissingCommandParameterException
     * @throws TeamCantPlayAgainstItselfException
     * @throws FailedToRetrieveFieldIdException
     * @throws CannotFindTeamWithNameException
     * @throws FailedToRetrieveTeamIdException
     * @throws FailedToRetrieveNextKeyFromSequenceException
     */
    private void createMatch(ArrayList<String> parameters) throws MissingCommandParameterException, TeamCantPlayAgainstItselfException, CannotFindTeamWithNameException, FailedToRetrieveFieldIdException, FailedToRetrieveTeamIdException, FailedToRetrieveNextKeyFromSequenceException
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

        // Ex.: creerMatch 2000-01-01 08:00:00 Red_Sox Yankees

        Match match = new Match();

        Team local = Team.getTeamWithName(connectionWithDatabase, parameters.get(2));
        if (local == null) {
            throw new CannotFindTeamWithNameException(parameters.get(2));
        }
        match.setLocalTeam(local);
        match.setField(local.getField(connectionWithDatabase));

        Team visitor = Team.getTeamWithName(connectionWithDatabase, parameters.get(3));
        if (visitor == null) {
            throw new CannotFindTeamWithNameException(parameters.get(3));
        }
        match.setVisitorTeam(visitor);

        match.setDate(Date.valueOf(parameters.get(0)));
        match.setTime(Time.valueOf(parameters.get(1)));
        try {
            match.save(connectionWithDatabase);
        } catch (FailedToSaveEntityException e) {
            Logger.error(LOG_TYPE.EXCEPTION, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Crée un nouvel arbitre, en calculant le ArbitreId automatiquement
     *
     * @param parameters - <ArbitreNom> <ArbitrePrenom>
     * @throws MissingCommandParameterException
     * @throws FailedToSaveEntityException
     */
    private void creerArbitre(ArrayList<String> parameters) throws MissingCommandParameterException, FailedToSaveEntityException
    {
        if (parameters.isEmpty()) {
            throw new MissingCommandParameterException("creerArbitre", "ArbitreNom");
        } else if (parameters.size() == 1) {
            throw new MissingCommandParameterException("creerArbitre", "ArbitrePrenom");
        }

        // Check if this official already exists
        Official official = Official.getOfficialWithName(connectionWithDatabase, parameters.get(1), parameters.get(0));
        if (official != null) {
            Logger.error(LOG_TYPE.USER, "L'arbitre existe déjà.");
        } else {
            official = new Official();
            official.setFirstName(parameters.get(1));
            official.setLastName(parameters.get(0));
            official.save(connectionWithDatabase);
            Logger.info(LOG_TYPE.SYSTEM, "Ajout fait avec succès.");
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
    private void entrerResultatMatch(ArrayList<String> parameters)
    {
        // TODO
    }

    /**
     * Afficher les résultats de tous les matchs
     *
     * @param parameters - [<APartirDate>]
     */
    private void afficherResultatsDate(ArrayList<String> parameters)
    {
        // TODO
    }

    /**
     * Afficher les résultats des matchs où une équipe a participé
     *
     * @param parameters - [<EquipeNom>]
     */
    private void afficherResultats(ArrayList<String> parameters)
    {
        // TODO
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
