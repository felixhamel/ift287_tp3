package ligueBaseball;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;

import ligueBaseball.command.Command;
import ligueBaseball.exceptions.CreatePlayerParametersMissingException;
import ligueBaseball.exceptions.FailedToConnectToDatabaseException;
import ligueBaseball.exceptions.FieldNameAlreadyTakenException;
import ligueBaseball.exceptions.MissingCommandParameterException;
import ligueBaseball.exceptions.PlayerAlreadyExistsException;
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

	public Application(ApplicationParameters parameters) 
	{
		this.parameters = parameters;
	}
	
	/**
	 * Open a connection with the database.
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
		} catch (SQLException e) {
			throw new FailedToConnectToDatabaseException(parameters.getDatabaseName(), e);
		}
	}
	
	/**
	 * Close the opened connection with the database. Won't close it again if already closed.
	 */
	private void closeConnectionWithDatabase()
	{
		if(connectionWithDatabase != null) {
			try {
				if(!connectionWithDatabase.isClosed()) {
					connectionWithDatabase.close();
				}
			} catch (SQLException e) {
				connectionWithDatabase = null;
			}
		}
	}

	/**
	 * Launch the application.
	 * @throws FailedToConnectToDatabaseException
	 * @throws UnknownCommandException 
	 */
	public void launch() throws FailedToConnectToDatabaseException, UnknownCommandException
	{
		openConnectionWithDatabase();
		while(true) {
			try {
				Command command = askCommandToUser();
				if(!actions.containsKey(command.getCommandName())) {
					throw new UnknownCommandException(command.getCommandName());
				} else {
					executeCommand(command);
				}
			} catch(Exception e) {
				System.out.println("Exception ("+e.getClass().getName()+"): " + e.getMessage() + " ");
				//e.printStackTrace();
			}
		}
	}
	
	/**
	 * Ask the player enter a command with parameters if needed.
	 * @return Command - The requested command by the user.
	 */
	private Command askCommandToUser()
	{
		Scanner scanner = new Scanner(System.in);
		try {
			System.out.print("$ ");
			return Command.extractCommandFromString(scanner.nextLine());
		} finally { 
			// BUG dans Eclipse, si on le ferme ça va faire plein de null pointer exception.
			// Ce bug n'est pas présent si on roule le programme en console.
			//scanner.close();
		}
	}
	
	/**
	 * Execute the command with the informations given by the user.
	 * @param command - Command requested by the user.
	 */
	private void executeCommand(Command command) throws Exception
	{
		switch(command.getCommandName()) {
			case "creerEquipe":
				createANewTeam(command.getParameters());
				break;
			case "afficherEquipes":
				afficherEquipes();
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
				supprimerJoueur(command.getParameters());
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
	 * @param parameters - <EquipeNom> [<NomTerrain> AdresseTerrain]
	 */
	private void createANewTeam(ArrayList<String> parameters) throws SQLException, TeamNameAlreadyTakenException, FieldNameAlreadyTakenException {
		Statement statement = null;
		String query = "SELECT equipenom FROM equipe WHERE equipenom = " + parameters.get(1) + ";";
		try {
			statement = connectionWithDatabase.createStatement();
			ResultSet rs = statement.executeQuery(query);
			if (rs.next()) {
				throw new TeamNameAlreadyTakenException(parameters.get(1));
			}
			else {
				query = "SELECT * FROM terrain WHERE terrainnom = " + parameters.get(2) + ";";
				rs = statement.executeQuery(query);
				if (rs.next()) {
					throw new FieldNameAlreadyTakenException(parameters.get(2));
				}
				else {
					String update = null;
					if (parameters.get(3) != null) {
						update = "INSERT INTO terrain (terrainnom, terrainadresse) VALUES ('" + parameters.get(2) + "', '" + parameters.get(3) + "');";
					}
					else {
						update = "INSERT INTO terrain (terrainnom) VALUES ('" + parameters.get(2) + "');";
					}
					statement.executeUpdate(update);
					query = "SELECT terrainid FROM terrain WHERE terrainnom = " + parameters.get(2) + ";";
					rs = statement.executeQuery(query);
					update = "INSERT INTO equipe (equipenom, terrainid) VALUES ('" + parameters.get(1) + "', '" + rs.getString("terrainid") + ");";
					statement.executeUpdate(update);
				}
			}
		}
		finally {
			closeStmt(statement);
		}
	}
	
	/**
	 * Afficher la liste des equipes
	 */
	private void afficherEquipes() throws SQLException {
		Statement stmt = null;
		String query = "SELECT equipeid, equipenom FROM equipe ORDER BY equipenom;";
		try {
			stmt = connectionWithDatabase.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				System.out.println(rs.getString("equipenom") + ", equipe no " + rs.getString("equipeid"));
			}
		}
		finally {
			closeStmt(stmt);
		}
	}
	
	/**
	 * Supprimer une equipe
	 * @param parameters - <EquipeNom>
	 */
	private void supprimerEquipe(ArrayList<String> parameters) throws SQLException, TeamIsNotEmptyException {
		Statement stmt = null;
		String query = "SELECT equipeid FROM equipe WHERE equipenom = " + parameters.get(1) + ";";
		try {
			stmt = connectionWithDatabase.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			query = "SELECT * FROM faitpartie WHERE equipedid = " + rs.getString("equipeid") + ";";
			rs = stmt.executeQuery(query);
			if (rs.next()) {
				throw new TeamIsNotEmptyException(parameters.get(1));
			}
			else {
				String delete = "DELETE FROM equipe WHERE equipenom = " + parameters.get(1) + ";";
				stmt.executeUpdate(delete);
			}
		}
		finally {
			closeStmt(stmt);
		}
	}
	
	/**
	 * Creer un joueur	
	 * @param parameters - <JoueurNom> <JoueurPrenom> [<EquipeNom> <Numero> [<DateDbut>]]
	 */
	private void creerJoueur(ArrayList<String> parameters) throws SQLException, CreatePlayerParametersMissingException, PlayerAlreadyExistsException {
		if (parameters.get(3) != null || parameters.get(4) != null) {
			if (parameters.get(3) == null || parameters.get(4) == null) {
				throw new CreatePlayerParametersMissingException();
			}
		}
		Statement stmt = null;
		String query = "SELECT * FROM joueur WHERE joueurnom = " + parameters.get(1) + " OR joueurprenom = " + parameters.get(2) + ";";
		try {
			stmt = connectionWithDatabase.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next()) {
				throw new PlayerAlreadyExistsException(parameters.get(2), parameters.get(1));
			}
			else {
				String update = "INSERT INTO joueur (joueurnom, joueurprenom) VALUES ('" + parameters.get(1) + "', '" + parameters.get(2) + "');";
				stmt.executeUpdate(update);
				if (parameters.get(3) != null) {
					query = "SELECT joueurid FROM joueur WHERE joueurprenom = " + parameters.get(2) + " AND joueurnom = " + parameters.get(1) + ";";
					rs = stmt.executeQuery(query);
					String joueurId = rs.getString("joueurid");
					//update = "INSERT INTO faitpartie ("
					//TODO
				}
			}
		}
		finally {
			closeStmt(stmt);
		}
	}
	
	/**
	 * Afficher la liste des joueurs
	 * @param parameters - [<EquipeNom>]
	 * @throws MissingCommandParameterException 
	 */
	private void afficherJoueursEquipe(ArrayList<String> parameters)
	{
		if(parameters.isEmpty()) {
			try {
				// Go get all the teams
				PreparedStatement statement = connectionWithDatabase.prepareStatement("SELECT * FROM equipe");
				ResultSet teams = statement.executeQuery();
				while(teams.next()) {
					System.out.println("Équipe: " + teams.getString("equipenom"));
					showAllPlayersForTeam(teams.getString("equipenom"));
				}
				statement.close();
			} catch(SQLException e) {
				e.printStackTrace();
			}
		} else {	
			showAllPlayersForTeam(parameters.get(0));
		}
	}
	
	/**
	 * Show all the players that plays for the team given in parameters.
	 * @param teamName - Name of the Team.
	 */
	private void showAllPlayersForTeam(String teamName)
	{
		try {
			PreparedStatement statement = connectionWithDatabase.prepareStatement(
					"SELECT joueur.joueurnom, joueur.joueurprenom, joueur.joueurid "
					+ "FROM faitpartie "
					+ "INNER JOIN joueur ON faitpartie.joueurid = joueur.joueurid "
					+ "INNER JOIN equipe ON faitpartie.equipeid = equipe.equipeid "
					+ "WHERE equipe.equipenom = ? "
					+ "ORDER BY equipe.equipeid ASC, faitpartie.numero ASC;");
			statement.setString(1, teamName);
			ResultSet result = statement.executeQuery();
			
			// Check if we received any results
			if(!result.isBeforeFirst()) {
				System.out.println("Erreur: L'équipe '" + teamName + " n'existe pas ou n'a pas de joueur.");
			}
			while(result.next()) {
				System.out.println(" -> " + result.getString("joueurprenom") + " " + result.getString("joueurnom") + " #" + result.getInt("joueurid"));
			}
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Supprime un joueur et ses informations
	 * @param parameters - <JoueurNom> <JoueurPrenom>
	 * @throws MissingCommandParameterException 
	 */
	private void supprimerJoueur(ArrayList<String> parameters) throws MissingCommandParameterException 
	{
		if(parameters.isEmpty()) {
			throw new MissingCommandParameterException("supprimerJoueur", "JoueurNom");
		} else if(parameters.size() == 1) {
			throw new MissingCommandParameterException("supprimerJoueur", "JoueurPrenom");
		}
		
		try {
			PreparedStatement statement = connectionWithDatabase.prepareStatement("SELECT joueurid FROM joueur WHERE joueurnom = ? AND joueurprenom = ?;");
			statement.setString(1, parameters.get(0));
			statement.setString(2, parameters.get(1));
			ResultSet player = statement.executeQuery();
			
			if(!player.isBeforeFirst()) {
				System.out.println("Erreur: Le joueur n'existe pas.");
				statement.close();
			} else {
				player.next();
				int playerId = player.getInt("joueurid");
				statement.close();
				
				System.out.println("Êtes-vous certain de vouloir supprimer ce joueur ? (O/N) : ");
				int confirmation = new BufferedReader(new InputStreamReader(System.in)).readLine().trim().charAt(0);
				if(confirmation == 'o' || confirmation == 'O') {
					deletePlayerFromDatabase(playerId);
				}
			}
		} catch(SQLException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private void deletePlayerFromDatabase(int playerId) {
		try {
			connectionWithDatabase.setAutoCommit(false);
		
			PreparedStatement deleteParticipations = connectionWithDatabase.prepareStatement("DELETE FROM participe WHERE joueurid = ?;");
			try {
				deleteParticipations.setInt(1, playerId);
				deleteParticipations.executeUpdate();
			} catch(SQLException e) {
				connectionWithDatabase.rollback();
				System.out.println("Erreur: Problème lors du retrait du joueur dans la table 'participe'.");
				return;
			} finally {
				deleteParticipations.close();
			}
			
			PreparedStatement deleteInTeam = connectionWithDatabase.prepareStatement("DELETE FROM faitpartie WHERE joueurid = ?;");
			try {
				deleteInTeam.setInt(1, playerId);
				deleteInTeam.executeUpdate();
			} catch(SQLException e) {
				connectionWithDatabase.rollback();
				System.out.println("Erreur: Problème lors du retrait du joueur dans la table 'faitpartie'.");
				return;
			} finally {
				deleteInTeam.close();
			}
			
			PreparedStatement deleteJoueur = connectionWithDatabase.prepareStatement("DELETE FROM joueur WHERE joueurid = ?;");
			try {
				deleteJoueur.setInt(1, playerId);
				deleteJoueur.executeUpdate();
			} catch(SQLException e) {
				connectionWithDatabase.rollback();
				System.out.println("Erreur: Problème lors du retrait du joueur dans la table 'joueur'.");
				return;
			} finally {
				deleteJoueur.close();
			}
			
			connectionWithDatabase.commit();
			
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Ajouter un match, en calculant le MatchId automatiquement
	 * @param parameters - <MatchDate> <MatchHeure> <EquipeNomLocal> <EquipeNomVisiteur>
	 */
	private void creerMatch(ArrayList<String> parameters) {
		//TODO
	}
	
	/**
	 * Crée un nouvel arbitre, en calculant le ArbitreId automatiquement
	 * @param parameters - <ArbitreNom> <ArbitrePrenom>
	 */
	private void creerArbitre(ArrayList<String> parameters) {
		//TODO
	}
	
	/**
	 * Afficher la liste des arbitres en ordre alphabétique
	 */
	private void afficherArbitres() {
		//TODO
	}
	
	/**
	 * Affecter des arbitres à un match
	 * @param parameters - <MatchDate> <MatchHeure> <EquipeNomLocal> <EquipeNomVisiteur> <ArbitreNom> <ArbitrePrenom>
	 */
	private void arbitrerMatch(ArrayList<String> parameters) {
		//TODO
	}
	
	/**
	 * Entrer le résultat d'un match.
	 * @param parameters - <MatchDate> <MatchHeure> <EquipeNomLocal> <EquipeNomVisiteur> <PointsLocal> <PointsVisiteur>
	 */
	private void entrerResultatMatch(ArrayList<String> parameters) {
		//TODO
	}
	
	/**
	 * Afficher les résultats de tous les matchs
	 * @param parameters - [<APartirDate>]
	 */
	private void afficherResultatsDate(ArrayList<String> parameters) {
		//TODO
	}
	
	/**
	 * Afficher les résultats des matchs où une équipe a participé
	 * @param parameters - [<EquipeNom>]
	 */
	private void afficherResultats(ArrayList<String> parameters) {
		//TODO
	}
	
	/**
	 * Show all the available actions to the user.
	 */
	private void showAvailableActions()
	{
		System.out.println("Liste de toutes les commandes disponibles : ");
		for(Entry<String, String> entry : actions.entrySet()) {
			System.out.println(" - " + entry.getKey() + " : " + entry.getValue());
		}
	}
	
	/**
	 * Close the statement if not null
	 * @param stmt - SQL statement
	 */
	private void closeStmt(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			}
			catch (Exception e) {
				System.out.println("Exception ("+e.getClass().getName()+"): " + e.getMessage());
				//e.printStackTrace();
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
