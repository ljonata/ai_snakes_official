package snakes.stats;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import snakes.Bot;
import snakes.GameKeeper;

public class Results {

	public Duel[][] duels;
	public ArrayList<Class<? extends Bot>> bots;
	private static final String FILENAME_SCORES_TABLE = ".//recordings//scores_table.json";
	private static final String FILENAME_LEADERBOARD_TABLE = ".//recordings//leaderboard.json";

	public Results(ArrayList<Class<? extends Bot>> bots) {
		this.bots = bots;
		duels = new Duel[bots.size()][bots.size()];
	}

	public void save() {
		saveScoresTable();
		saveRanking();
	}

	@SuppressWarnings("unchecked")
	public void saveScoresTable() {
		JSONArray root = new JSONArray();
		try {
			for (int i = 0; i < bots.size(); i++) {
				JSONObject row = new JSONObject();
				row.put("name", bots.get(i).getConstructor().newInstance().getClass().getSimpleName());

				JSONArray results = new JSONArray();
				for (int j = 0; j < bots.size(); j++) {
					JSONObject col = new JSONObject();
					col.put("opponent", bots.get(j).getConstructor().newInstance().getClass().getSimpleName());
					if (i == j) {
						col.put("total_score", "n/a");
					} else if (i < j) {
						// Superior diagonal
						int sumScore0 = 0;
						int sumScore1 = 0;
						JSONArray matches = new JSONArray();
						for (GameKeeper game : duels[i][j].matches) {
							// calculating the total scores
							sumScore0 += game.scoreBot0;
							sumScore1 += game.scoreBot1;
							// getting the id of the matches
							JSONObject match = new JSONObject();
							match.put("filename", game.filename);
							match.put("result", game.getResult());
							matches.add(match);
						}
						col.put("total_score", sumScore0 + "-" + sumScore1);
						col.put("matches", matches);
					} else {
						// i (row) > j (column)
						// get from the superior diagonal (reversed)
						int sumScore0 = 0;
						int sumScore1 = 0;
						JSONArray matches = new JSONArray();
						for (GameKeeper game : duels[j][i].matches) {
							// calculating the total scores
							sumScore0 += game.scoreBot0;
							sumScore1 += game.scoreBot1;
							// getting the id of the matches
							JSONObject match = new JSONObject();
							match.put("filename", game.filename);
							match.put("result", game.scoreBot1 + "-" + game.scoreBot0);
							matches.add(match);
						}
						col.put("total_score", sumScore1 + "-" + sumScore0);
						col.put("matches", matches);
					}
					results.add(col);
				}
				row.put("results", results);
				root.add(row);
			}
			FileWriter file = new FileWriter(FILENAME_SCORES_TABLE);
			file.write(root.toJSONString());
			file.flush();
			file.close();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void saveRanking() {
		ArrayList<Pair> botScores = new ArrayList<Results.Pair>();
		for (int i = 0; i < bots.size(); i++) {
			int sumScores = 0;
			for (int j = 0; j < bots.size(); j++) {
				// adding points when the bot was player 0
				if (duels[i][j] != null)
					for (GameKeeper game : duels[i][j].matches)
						sumScores += game.scoreBot0;
				// adding points when the bot was player 1
				if (duels[j][i] != null)
					for (GameKeeper game : duels[j][i].matches)
						sumScores += game.scoreBot1;
			}
			try {
				String botName = bots.get(i).getConstructor().newInstance().getClass().getSimpleName();
				botScores.add(new Pair(botName, sumScores));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Collections.sort(botScores, new SortbyScore());
		Collections.reverse(botScores);

		// Generating the JSON file
		JSONArray root = new JSONArray();
		for (int i = 0; i < botScores.size(); i++) {
			JSONObject row = new JSONObject();
			row.put("place", i + 1);
			row.put("name", botScores.get(i).botName);
			row.put("score", botScores.get(i).totalScore);
			root.add(row);
		}
		try {
			FileWriter file;
			file = new FileWriter(FILENAME_LEADERBOARD_TABLE);
			file.write(root.toJSONString());
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Return the total number of points that a certain bot scored in the
	 * tournament.
	 * 
	 * @param iBot
	 * @return
	 */
	public double getTotalScore(int iBot) {
		double sum = 0;

		// Number of points when bot is the player 0 in the triangle-table
		for (int c = 0; c < bots.size(); c++)
			if (duels[iBot][c] != null)
				sum += duels[iBot][c].totalScoreBot0();

		// Number of points when bot is the player 1 in the triangle-table
		for (int r = 0; r < bots.size(); r++)
			if (duels[r][iBot] != null)
				sum += duels[r][iBot].totalScoreBot1();

		return sum;
	}

	private class Pair {
		public String botName;
		public int totalScore;

		public Pair(String botName, int totalScore) {
			super();
			this.botName = botName;
			this.totalScore = totalScore;
		}
	}

	private class SortbyScore implements Comparator<Pair> {
		public int compare(Pair a, Pair b) {
			return a.totalScore - b.totalScore;
		}
	}

}
