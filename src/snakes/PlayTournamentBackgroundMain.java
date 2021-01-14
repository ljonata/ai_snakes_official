package snakes;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import snakes.stats.Duel;
import snakes.stats.Results;

/**
 * Implements tournament of the snake game with several rounds. To run the
 * tournament in background and get them saved you should run
 * PlayTournamentBackgroundMain.java with names of bots in arguments ( for ex.
 * a_zhuchkov.MyBot b_j_grooten.MyBot bertram_timo.MyBot )
 */
public class PlayTournamentBackgroundMain {
	// private static final String LOG_DIRECTORY_PATH = "logs";
	public static final String GAME_RECORDINGS_DIRECTORY_PATH = ".\\recordings\\";
	private static final int NUM_ITERATIONS = 3;
	// private static FileWriter results_fw;
	// private static int[][] total_results_table;

	/**
	 * UI Entry point
	 * 
	 * @param args Two classes implementing the Bot interface
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		initializeTournament(args);

		// Load the bots passed by argument
		ArrayList<Class<? extends Bot>> bots = new ArrayList<>();
		BotLoader loader = new BotLoader();
		for (String arg : args) {
			bots.add(loader.getBotClass(arg));
		}

		// Running the tournament
		Results allResults = new Results(bots);
		start_tournament_n_times(NUM_ITERATIONS, bots, allResults);

		// Saving the overall results in JSON files
		allResults.save();

		// This can be removed
		for (int iBot = 0; iBot < bots.size(); iBot++) {
			double total = allResults.getTotalScore(iBot);
			System.out.println(
					bots.get(iBot).getConstructor().newInstance().getClass().getSimpleName() + " earned: " + total);
		}
	}

	/**
	 * Check if the necessary directories for logs and other files exists. Also,
	 * validates the number of arguments.
	 * 
	 * @throws Exception
	 */
	private static void initializeTournament(String[] args) {
		File recordingsDir = new File(GAME_RECORDINGS_DIRECTORY_PATH);
		if (!recordingsDir.exists() && !recordingsDir.mkdirs()) {
			System.out.println("Cannot create recordings directory");
			System.exit(1);
		}

		if (args.length < 2) {
			System.err.println("You must provide two classes implementing the Bot interface.");
			System.exit(1);
		}
	}

	/**
	 * Launch several rounds of snake game between bots
	 * 
	 * @param iterations Number of rounds or iterations
	 * @param bots       Competitive bots
	 * @throws IOException          FileWriter handler
	 * @throws InterruptedException Threads handler
	 */
	public static Results start_tournament_n_times(int iterations, ArrayList<Class<? extends Bot>> bots,
			Results results) throws IOException, InterruptedException, InvocationTargetException, NoSuchMethodException,
			InstantiationException, IllegalAccessException {

		// total_results_table = new int[bots.size() + 1][bots.size() + 1];

		for (int iteration = 0; iteration < iterations; iteration++) {
			System.out.println("\nTournament iteration number " + iteration + "\n");
			start_round_robin_tournament(bots, iteration, results);
		}

		/*
		 * for (int i = 0; i < bots.size(); i++) for (int j = i + 1; j < bots.size();
		 * j++) { if (bots.get(i) == null || bots.get(j) == null) continue;
		 * 
		 * System.out.println("\n" +
		 * bots.get(i).getConstructor().newInstance().getClass().getSimpleName() +
		 * " vs. " +
		 * bots.get(j).getConstructor().newInstance().getClass().getSimpleName() + ": "
		 * + total_results_table[i][j] + " - " + total_results_table[j][i]);
		 * results_fw.write(bots.get(i).getConstructor().newInstance().getClass().
		 * getSimpleName() + " vs. " +
		 * bots.get(j).getConstructor().newInstance().getClass().getSimpleName() + ": "
		 * + total_results_table[i][j] + " - " + total_results_table[j][i] + "\n"); }
		 */

		// total calculation
		// System.out.println();
		// results_fw.write("\n-------------------------------------------\n\n");

		return results;
	}

	/**
	 * Single iteration of a round robin tournament among the bots
	 * 
	 * @param bots Competitive bots
	 * @throws InterruptedException Threads handler
	 * @throws IOException          FileWriter handler
	 */
	public static void start_round_robin_tournament(ArrayList<Class<? extends Bot>> bots, int iterationNumber,
			Results results) throws InterruptedException, IOException, NoSuchMethodException, IllegalAccessException,
			InvocationTargetException, InstantiationException {

		// Initialize game settings
		Coordinate mazeSize = new Coordinate(14, 14);
		Coordinate head0 = new Coordinate(6, 5);
		Direction tailDirection0 = Direction.DOWN;
		Coordinate head1 = new Coordinate(7, 8);
		Direction tailDirection1 = Direction.UP;
		int snakeSize = 3;

		for (int indexBot0 = 0; indexBot0 < bots.size() - 1; indexBot0++)
			for (int indexBot1 = indexBot0 + 1; indexBot1 < bots.size(); indexBot1++) {
				Bot bot0 = bots.get(indexBot0).getConstructor().newInstance();
				Bot bot1 = bots.get(indexBot1).getConstructor().newInstance();
				if (results.duels[indexBot0][indexBot1] == null)
					results.duels[indexBot0][indexBot1] = new Duel(bots.get(indexBot0), bots.get(indexBot1));

				// play game in the background
				SnakeGame game = new SnakeGame(mazeSize, head0, tailDirection0, head1, tailDirection1, snakeSize, bot0,
						bot1);
				BackgroundGameRunner backgroundGameRunner = new BackgroundGameRunner(game);
				Thread t = new Thread(backgroundGameRunner);
				t.start();
				t.join();

				// Saving the match bot0 x bot1
				backgroundGameRunner.finishedGame.save(GAME_RECORDINGS_DIRECTORY_PATH);
				System.out.println(game.name0 + " vs. " + game.name1 + " : " + game.getResult());

				// add the result of the game to total points
				results.duels[indexBot0][indexBot1].matches.add(backgroundGameRunner.finishedGame);
				/*
				 * points.set(playerNumber.get(i), points.get(playerNumber.get(i)) +
				 * Integer.parseInt(game.gameResult.substring(0, 1)));
				 * points.set(playerNumber.get(bots.size() - i - 1),
				 * points.get(playerNumber.get(bots.size() - i - 1)) +
				 * Integer.parseInt(game.gameResult.substring(game.gameResult.length() - 1)));
				 * 
				 * // add to the total results table
				 * total_results_table[playerNumber.get(i)][playerNumber.get(bots.size() - i -
				 * 1)] += Integer .parseInt(game.gameResult.substring(0, 1));
				 * total_results_table[playerNumber.get(bots.size() - i -
				 * 1)][playerNumber.get(i)] += Integer
				 * .parseInt(game.gameResult.substring(game.gameResult.length() - 1));
				 */
			}

	}

}
