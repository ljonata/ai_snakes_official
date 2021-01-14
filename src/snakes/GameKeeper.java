package snakes;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class GameKeeper implements java.io.Serializable {
	ArrayList<Direction> firstBotMovements;
	ArrayList<Direction> secondBotMovements;
	ArrayList<Coordinate> appleCoordinates;
	// String gameResult;
	public int scoreBot0, scoreBot1;
	String resultMessage;

	int startSize;
	Coordinate mazeSize;
	Coordinate startHead0;
	Coordinate startHead1;
	Direction startTailDirection0;
	Direction startTailDirection1;
	public String botFullname0;
	public String botFullname1;

	ArrayList<Integer> snake1_scores;
	ArrayList<Integer> snake2_scores;

	Snake snake1;
	Snake snake2;

	public String filename;

	public GameKeeper(Coordinate mazeSize, Coordinate startHead0, Direction startTailDirection0, Coordinate startHead1,
			Direction startTailDirection1, int startSize, String botFullName0, String botFullname1) {

		this.snake1_scores = new ArrayList<>();
		this.snake2_scores = new ArrayList<>();

		this.mazeSize = mazeSize;
		this.startHead0 = startHead0;
		this.startTailDirection0 = startTailDirection0;
		this.startHead1 = startHead1;
		this.startTailDirection1 = startTailDirection1;
		this.startSize = startSize;
		this.botFullname0 = botFullName0;
		this.botFullname1 = botFullname1;

		this.firstBotMovements = new ArrayList<>();
		this.secondBotMovements = new ArrayList<>();
		this.appleCoordinates = new ArrayList<>();
		// this.gameResult = "";
		this.resultMessage = "";

		this.snake1 = new Snake(this.startHead0, this.startTailDirection0, this.startSize, this.mazeSize);
		this.snake2 = new Snake(this.startHead1, this.startTailDirection1, this.startSize, this.mazeSize);
	}

	@SuppressWarnings("unchecked")
	public void save(String path) throws IOException {
		JSONObject game = new JSONObject();
		JSONObject metadata = new JSONObject();
		metadata.put("mazeSize", this.mazeSize.toString());
		metadata.put("startHead1", this.startHead0.toString());
		metadata.put("startHead2", this.startHead1.toString());
		metadata.put("startTail1", this.startTailDirection0.v.toString());
		metadata.put("startTail2", this.startTailDirection1.v.toString());
		metadata.put("startSize", Integer.toString(this.startSize));
		game.put("metadata", metadata);
		game.put("bot1", botFullname0);
		game.put("bot2", botFullname1);
		game.put("finished", getNow());
		// Generating and saving the ID of the match
		int id = getUniqueGameId();
		game.put("ID", id);
		this.filename = path + "game_id_" + id + ".json";

		for (int i = 0; i < firstBotMovements.size(); i++) {
			JSONObject timestamp = new JSONObject();
			JSONArray snake1_out = new JSONArray();
			boolean grow = false;
			if (i == 0) {
				if (snake1_scores.get(i) != 0) {
					grow = true;
				}
				snake1.moveTo(firstBotMovements.get(i), grow);
			} else {
				if ((snake1_scores.get(i) - snake1_scores.get(i - 1)) != 0) {
					grow = true;
				}
				snake1.moveTo(firstBotMovements.get(i), grow);
			}
			for (Coordinate c : snake1.elements) {
				snake1_out.add(c.toString());
			}
			JSONArray snake2_out = new JSONArray();
			grow = false;
			if (i == 0) {
				if (snake2_scores.get(i) != 0) {
					grow = true;
				}
				snake2.moveTo(secondBotMovements.get(i), grow);
			} else {
				if ((snake2_scores.get(i) - snake2_scores.get(i - 1)) != 0) {
					grow = true;
				}
				snake2.moveTo(secondBotMovements.get(i), grow);
			}
			for (Coordinate c : snake2.elements) {
				snake2_out.add(c.toString());
			}
			timestamp.put("snake1move", firstBotMovements.get(i).v.toString());
			timestamp.put("snake2move", secondBotMovements.get(i).v.toString());
			timestamp.put("snake1", snake1_out);
			timestamp.put("snake2", snake2_out);
			timestamp.put("apple", this.appleCoordinates.get(i).toString());
			timestamp.put("score1", this.snake1_scores.get(i));
			timestamp.put("score2", this.snake2_scores.get(i));
			game.put(i, timestamp);
		}
		try (FileWriter file = new FileWriter(filename)) {

			file.write(game.toJSONString());
			file.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * @SuppressWarnings("unchecked") public void saveToFile(String filename) throws
	 * IOException { JSONObject game = new JSONObject(); JSONArray snake1_init = new
	 * JSONArray(); JSONArray snake2_init = new JSONArray(); for(Coordinate c :
	 * snake1.elements) { snake1_init.add(c.toString()); } for(Coordinate c :
	 * snake2.elements) { snake2_init.add(c.toString()); } game.put("Snake1",
	 * snake1_init); game.put("Snake2", snake2_init); for(int i = 0; i <
	 * firstBotMovements.size(); i++) { JSONObject timestamp = new JSONObject();
	 * timestamp.put("snake1", firstBotMovements.get(i).v.toString());
	 * timestamp.put("snake2", secondBotMovements.get(i).v.toString());
	 * timestamp.put("apple", this.appleCoordinates.get(i).toString());
	 * timestamp.put("score1", this.snake1_scores.get(i)); timestamp.put("score2",
	 * this.snake2_scores.get(i)); game.put(i, timestamp); } try (FileWriter file =
	 * new FileWriter(filename)) {
	 * 
	 * file.write(game.toJSONString()); file.flush();
	 * 
	 * } catch (IOException e) { e.printStackTrace(); } }
	 */

	public String getResult() {
		return scoreBot0 + "-" + scoreBot1;
	}

	private static String getNow() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		return dtf.format(now);
	}

	/**
	 * Generate an unique game ID based on the name of the players and the end that
	 * the game finished.
	 * 
	 * @return
	 */
	private int getUniqueGameId() {

		int prime = 31;
		int result = 1;
		result = prime * result + ((botFullname0 == null) ? 0 : botFullname0.hashCode());
		result = prime * result + ((botFullname1 == null) ? 0 : botFullname1.hashCode());

		// DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		// System.out.println(dtf.format(now));
		result = prime * result + now.hashCode();

		return result;
	}
}
