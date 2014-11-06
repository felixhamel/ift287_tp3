package ligueBaseball;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Time;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import ligueBaseball.exceptions.FailedToConnectToDatabaseException;
import ligueBaseball.exceptions.FailedToDeleteEntityException;
import ligueBaseball.exceptions.FailedToRetrievePlayersOfTeamException;
import ligueBaseball.exceptions.FailedToSaveEntityException;
import ligueBaseball.exceptions.MatchAlreadyHaveTheMaximumNumberOfOfficialsException;
import ligueBaseball.exceptions.MatchDoesntExistsException;
import ligueBaseball.exceptions.MissingCommandParameterException;
import ligueBaseball.exceptions.NegativeScore;
import ligueBaseball.exceptions.OfficialDoesntExistsException;
import ligueBaseball.exceptions.PlayerAlreadyExistsException;
import ligueBaseball.exceptions.TeamCantPlayAgainstItselfException;
import ligueBaseball.exceptions.TeamDoesntExistException;
import ligueBaseball.exceptions.TeamIsNotEmptyException;
import ligueBaseball.exceptions.TeamNameAlreadyTakenException;
import ligueBaseball.exceptions.UnknownCommandException;

class Application
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
    Application(ApplicationParameters parameters) {
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
                Logger.error(LOG_TYPE.EXCEPTION, e.getMessage() + "(" + e.getClass().getName() + ")");
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
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (!line.startsWith("--") && line.length() > 5) {
                        try {
                            Logger.info(LOG_TYPE.COMMAND, line);
                            executeCommand(Command.extractCommandFromString(line));
                        } catch (Exception e) {
                            Logger.error(LOG_TYPE.EXCEPTION, e.getMessage() + "(" + e.getClass().getName() + ")");
                        } finally {
                            System.out.println("");
                        }
                    } else {
                        // Logger.info(LOG_TYPE.COMMENT, line.substring(2));
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
                createPlayer(command.getParameters());
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
                displayOfficials();
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
            System.out.println(String.format(" -> %s, équipe # %s", team.getName(), team.getId()));
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
            try {
                team.delete(connectionWithDatabase);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Create a player
     *
     * @param parameters - <JoueurNom> <JoueurPrenom> [<EquipeNom> <Numero> [<DateDbut>]]
     * @throws MissingCommandParameterException
     * @throws TeamDoesntExistException
     * @throws FailedToSaveEntityException
     * @throws ParseException
     * @throws PlayerAlreadyExistsException
     */
    private void createPlayer(ArrayList<String> parameters) throws MissingCommandParameterException, TeamDoesntExistException, FailedToSaveEntityException, NullPointerException, IllegalArgumentException, ParseException, PlayerAlreadyExistsException
    {
        Team team = null;

        if (parameters.get(1) != null || parameters.get(2) != null) {

            // If param 3 or 4 exists, then they both can't be null
            if (parameters.get(1) == null) {
                throw new MissingCommandParameterException("EquipeNom", "creerJoueur");
            } else if (parameters.get(2) == null) {
                throw new MissingCommandParameterException("Numero", "creerJoueur");
            }

            // Check if team exists
            team = Team.getTeamWithName(connectionWithDatabase, parameters.get(2));
            if (team == null) {
                throw new TeamDoesntExistException(parameters.get(2));
            }
        }

        // Make sure that the player don't already exists
        if (parameters.size() >= 3) {
            List<Player> players = Player.getPlayerWithName(connectionWithDatabase, parameters.get(1), parameters.get(0));
            for (Player player : players) {
                if (player.getNumber() == Integer.parseInt(parameters.get(2))) {
                    throw new PlayerAlreadyExistsException();
                }
            }
        }

        Player player = new Player();
        player.setFirstName(parameters.get(1));
        player.setLastName(parameters.get(0));

        if (parameters.size() == 4) { // Checking only 1 param validates both
            player.setNumber(Integer.parseInt(parameters.get(3)));
        }

        if (parameters.size() == 5) {
            try {
                player.setDate(Date.valueOf(parameters.get(4)));
            } catch (IllegalArgumentException e) {
                throw new InvalidParameterException("La date est invalide.");
            }
        }

        player.save(connectionWithDatabase);

        if (parameters.size() >= 3) {
            team.addPlayer(connectionWithDatabase, player);
        }
    }

    /**
     * Afficher la liste des joueurs
     *
     * @param parameters - [<EquipeNom>]
     * @throws TeamDoesntExistException
     * @throws MissingCommandParameterException
     */
    private void afficherJoueursEquipe(ArrayList<String> parameters) throws TeamDoesntExistException
    {
        if (parameters.isEmpty()) {
            List<Team> teams = Team.getAllTeams(connectionWithDatabase);
            if (teams != null) {
                for (Team team : teams) {
                    showAllPlayersForTeam(team);
                }
            } else {
                throw new TeamDoesntExistException(parameters.get(0));
            }
        } else {
            Team team = Team.getTeamWithName(connectionWithDatabase, parameters.get(0));
            if (team != null) {
                showAllPlayersForTeam(team);
            } else {
                throw new TeamDoesntExistException(parameters.get(0));
            }
        }
    }

    /**
     * Show all the players that plays for the team given in parameters.
     *
     * @param teamName - Name of the Team.
     * @throws FailedToRetrievePlayersOfTeamException
     */
    private void showAllPlayersForTeam(Team team)
    {
        try {
            System.out.println(String.format("Équipe: %s", team.getName()));
            List<Player> players = team.getPlayers(connectionWithDatabase);
            if (players.isEmpty()) {
                System.out.println(" -> Aucun joueur ne fait partie de cette équipe.");
            } else {
                for (Player player : players) {
                    System.out.println(String.format(" -> %s %s #%s", player.getFirstName(), player.getLastName(), player.getNumber()));
                }
            }
        } catch (FailedToRetrievePlayersOfTeamException e) {
            e.printStackTrace();
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

        List<Player> players = Player.getPlayerWithName(connectionWithDatabase, parameters.get(1), parameters.get(0));

        if (players.isEmpty()) {
            Logger.error(LOG_TYPE.USER, "Le joueur '%s %s' n'existe pas.", parameters.get(1), parameters.get(0));
        } else {
            try {
                if (players.size() == 1) {
                    // Confirmation
                    System.out.print("Êtes-vous certain de vouloir supprimer ce joueur ? (O/N) : ");
                    char confirmation = new BufferedReader(new InputStreamReader(System.in)).readLine().trim().charAt(0);
                    if (confirmation == 'o' || confirmation == 'O') {
                        Team team = players.get(0).getTeam(connectionWithDatabase);
                        if (team != null) {
                            team.removePlayer(connectionWithDatabase, players.get(0));
                        }
                    }
                } else {
                    System.out.println("Entrez le # du joueur a supprimer parmi les suivants : ");

                    int i = 0;
                    for (Player player : players) {
                        System.out.println(String.format(" (%s) %s %s id=%s numero=%s", i, player.getFirstName(), player.getLastName(), player.getId(), player.getNumber()));
                        i++;
                    }

                    System.out.print("Votre choix : ");

                    int playerNumber = Integer.parseInt(new BufferedReader(new InputStreamReader(System.in)).readLine().trim());
                    Team team = players.get(playerNumber).getTeam(connectionWithDatabase);
                    if (team != null) {
                        team.removePlayer(connectionWithDatabase, players.get(playerNumber));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Create a new match.
     *
     * @param parameters - <MatchDate> <MatchHeure> <EquipeNomLocal> <EquipeNomVisiteur>
     * @throws MissingCommandParameterException
     * @throws TeamCantPlayAgainstItselfException
     * @throws CannotFindTeamWithNameException
     */
    private void createMatch(ArrayList<String> parameters) throws MissingCommandParameterException, TeamCantPlayAgainstItselfException, CannotFindTeamWithNameException
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

        // PATCH: in case the user forgot the seconds, we add them.
        String timeAsString = parameters.get(1);
        if (timeAsString.lastIndexOf(':') <= 2) {
            timeAsString += ":00";
        }

        match.setDate(Date.valueOf(parameters.get(0)));
        match.setTime(Time.valueOf(timeAsString));
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
     * Display all the officials.
     */
    private void displayOfficials()
    {
        List<Official> officials = Official.getAllOfficials(connectionWithDatabase);

        System.out.println("Les arbitres sont: ");
        for (Official official : officials) {
            System.out.println(String.format(" -> %s %s", official.getFirstName(), official.getLastName()));
        }
    }

    /**
     * Affecter des arbitres à un match
     *
     * @param parameters - <MatchDate> <MatchHeure> <EquipeNomLocal> <EquipeNomVisiteur> <ArbitreNom> <ArbitrePrenom>
     * @throws MatchDoesntExistsException
     * @throws MissingCommandParameterException
     * @throws OfficialDoesntExistsException
     * @throws MatchAlreadyHaveTheMaximumNumberOfOfficialsException
     * @throws FailedToSaveEntityException
     * @throws TeamDoesntExistException
     */
    private void arbitrerMatch(ArrayList<String> parameters) throws MatchDoesntExistsException, MissingCommandParameterException, OfficialDoesntExistsException, MatchAlreadyHaveTheMaximumNumberOfOfficialsException, FailedToSaveEntityException, TeamDoesntExistException
    {
        if (parameters.size() != 6) {
            throw new MissingCommandParameterException("arbitrerMatch", "");
        }

        // Find if match exists
        Match match = Match.getMatchWithDateTimeEquipe(connectionWithDatabase, parameters.get(0), parameters.get(1), parameters.get(2), parameters.get(3));
        if (match == null) {
            throw new MatchDoesntExistsException();
        }

        // Find if official exists
        Official official = Official.getOfficialWithName(connectionWithDatabase, parameters.get(5), parameters.get(4));
        if (official == null) {
            throw new OfficialDoesntExistsException();
        }

        // Make sure that the match don't have more than 4 officials
        if (match.getOfficials(connectionWithDatabase).size() >= 4) {
            throw new MatchAlreadyHaveTheMaximumNumberOfOfficialsException();
        }

        match.addOfficial(connectionWithDatabase, official);
    }

    /**
     * Entrer le résultat d'un match.
     *
     * @param parameters - <MatchDate> <MatchHeure> <EquipeNomLocal> <EquipeNomVisiteur> <PointsLocal> <PointsVisiteur>
     * @throws TeamDoesntExistException
     */
    private void entrerResultatMatch(ArrayList<String> parameters) throws MissingCommandParameterException, NegativeScore, TeamDoesntExistException
    {
        // Update
        // EX : entrerResultatMatch 2007-06-16 19:30:00 Yankees Mets 45 22
        // EX : entrerResultatMatch 2000-01-01 08:00:00 Yankees Red_Sox 70 30

        if (Integer.parseInt(parameters.get(4)) < 0 || Integer.parseInt(parameters.get(5)) < 0) {
            throw new NegativeScore();
        }

        Match match = Match.getMatchWithDateTimeEquipe(connectionWithDatabase, parameters.get(0), parameters.get(1), parameters.get(2), parameters.get(3));

        match.setLocalTeamScore(Integer.parseInt(parameters.get(4)));
        match.setVisitorTeamScore(Integer.parseInt(parameters.get(5)));
        try {
            match.save(connectionWithDatabase);
            Logger.info(LOG_TYPE.SYSTEM, "Update fait avec succes.");
        } catch (FailedToSaveEntityException e) {
            Logger.error(LOG_TYPE.EXCEPTION, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Afficher les résultats de tous les matchs
     *
     * @param parameters - [<APartirDate>]
     */
    private void afficherResultatsDate(ArrayList<String> parameters)
    {
        // afficherResultatsDate 2000-01-01

        List<Match> matchs;
        List<Official> official;

        if (parameters.isEmpty() == false) {
            matchs = Match.getMatchWithDate(connectionWithDatabase, parameters.get(0));
        } else {
            matchs = Match.getAllMatch(connectionWithDatabase);
        }

        for (Match match : matchs) {
            System.out.println(String.format("%-10s %-10s %-5s %-5s %-12s %-10s", "Equipelocal", "Equipevisiteur", "Scorelocal", "ScoreVisiteur", " Matchdate", "MatchHeure"));
            System.out.println(String.format("%-11s %-15s %-10s %-13s %-11s %-10s", match.getLocalTeam(connectionWithDatabase).getName(), match.getVisitorTeam(connectionWithDatabase).getName(), match.getLocalTeamScore(), match.getVisitorTeamScore(), match.getDate(), match.getTime()));

            official = match.getOfficials(connectionWithDatabase);
            System.out.println(String.format("\n"));
            System.out.println(String.format("Liste des arbitres"));
            if (official.size() != 0) {
                for (Official offi : official) {
                    System.out.println(String.format(" -> %-10s %-10s", offi.getFirstName(), offi.getLastName()));
                }
            } else {
                System.out.println(String.format(" -> Aucun arbitre durant le match."));
            }
            System.out.println(String.format("\n"));
        }
    }

    /**
     * Afficher les résultats des matchs où une équipe a participé
     *
     * @param parameters - [<EquipeNom>]
     * @throws TeamDoesntExistException
     */
    private void afficherResultats(ArrayList<String> parameters) throws TeamDoesntExistException
    {
        // afficherResultats Yankees

        List<Match> matchs = Match.getMatchForTeam(connectionWithDatabase, parameters.get(0));
        List<Official> official;

        for (Match match : matchs) {
            System.out.println(String.format("%-10s %-10s %-5s %-5s %-12s %-10s", "Equipelocal", "Equipevisiteur", "Scorelocal", "ScoreVisiteur", " Matchdate", "MatchHeure"));
            System.out.println(String.format("%-11s %-15s %-10s %-13s %-11s %-10s", match.getLocalTeam(connectionWithDatabase).getName(), match.getVisitorTeam(connectionWithDatabase).getName(), match.getLocalTeamScore(), match.getVisitorTeamScore(), match.getDate(), match.getTime()));

            official = match.getOfficials(connectionWithDatabase);

            System.out.println(String.format("\n"));
            System.out.println(String.format("Liste des arbitres"));
            if (official.size() != 0) {
                for (Official offi : official) {
                    System.out.println(String.format(" -> %-10s %-10s", offi.getFirstName(), offi.getLastName()));
                }
            } else {
                System.out.println(String.format(" -> Aucun arbitre durant le match."));
            }
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
     * Close connection with database and exit.
     */
    private void exitProgram()
    {
        closeConnectionWithDatabase();
        System.exit(0);
    }
}
