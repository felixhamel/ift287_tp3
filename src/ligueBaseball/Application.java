package ligueBaseball;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;

import ligueBaseball.command.Command;
import ligueBaseball.exceptions.FailedToConnectToDatabaseException;
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
		/*try {
			connectionWithDatabase = 
					DriverManager.getConnection("jdbc :postgresql :" + parameters.getDatabaseName(), 
												parameters.getUsername(), parameters.getPassword());
		} catch (SQLException e) {
			throw new FailedToConnectToDatabaseException(parameters.getDatabaseName(), e);
		}*/
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
				System.out.println("Exception ("+e.getClass().getName()+"): " + e.getMessage());
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
		System.out.print("$ ");
		Scanner reader = new Scanner(System.in);
		try {
			return Command.extractCommandFromString(reader.next());
		} finally {
			// BUG dans Eclipse, va faire plein de retour de ligne si décommenté.
			//reader.close();
		}
	}
	
	/**
	 * Execute the command with the informations given by the user.
	 * @param command - Command requested by the user.
	 */
	private void executeCommand(Command command)
	{
		switch(command.getCommandName()) {
			case "creerEquipe":
				creerEquipe(command.getParameters());
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
	 * Crée une nouvelle équipe
	 * @param parameters - <EquipeNom> [<NomTerrain> AdresseTerrain]
	 */
	private void creerEquipe(ArrayList<String> parameters) {
		//TODO
	}
	
	/**
	 * Afficher la liste des équipes
	 */
	private void afficherEquipes() {
		//TODO
	}
	
	/**
	 * Supprimer une équipe
	 * @param parameters - <EquipeNom>
	 */
	private void supprimerEquipe(ArrayList<String> parameters) {
		//TODO
	}
	
	/**
	 * Créer un joueur	
	 * @param parameters - <JoueurNom> <JoueurPrenom> [<EquipeNom> <Numero> [<DateDbut>]]
	 */
	private void creerJoueur(ArrayList<String> parameters) {
		//TODO
	}
	
	/**
	 * Afficher la liste des joueurs
	 * @param parameters - [<EquipeNom>]
	 */
	private void afficherJoueursEquipe(ArrayList<String> parameters) {
		//TODO
	}
	
	/**
	 * Supprime un joueur et ses informations
	 * @param parameters - <JoueurNom> <JoueurPrenom>
	 */
	private void supprimerJoueur(ArrayList<String> parameters) {
		//TODO
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
	 * Close connection with database and exit.
	 */
	private void exitProgram()
	{
		closeConnectionWithDatabase();
		System.exit(0);
	}
}
