package snakes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Objects;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ShowTournamentMain {

	public static void main(String[] args) {
		File dir = new File(PlayTournamentBackgroundMain.GAME_RECORDINGS_DIRECTORY_PATH);
		if (!dir.exists()) {
			System.err.println("Cannot find directory with recordings - "
					+ PlayTournamentBackgroundMain.GAME_RECORDINGS_DIRECTORY_PATH);
		}

		for (int i = 0; i < Objects.requireNonNull(dir.listFiles()).length; i++) {
			File currentDir = new File(
					PlayTournamentBackgroundMain.GAME_RECORDINGS_DIRECTORY_PATH + String.format("//iteration_%d", i));
			if (currentDir.exists()) {
				System.out.println("\nPlaying games from directory " + currentDir.getPath() + ":\n");
				File[] games = currentDir.listFiles((dir1, name) -> name.endsWith(".json"));

				if (games == null) {
					System.err.println("I/O error while searching for .json files");
					continue;
				}

				for (File game : games) {
					try {
						String filename = currentDir.getPath() + "//" + game.getName();
						System.out.println("Playing game from file: " + filename);

						JSONParser jsonParser = new JSONParser();

						try (FileReader reader = new FileReader(filename)) {
							Object obj = jsonParser.parse(reader);
							JSONObject jsonObject = (JSONObject) obj;
							JSONObject metadata = (JSONObject) jsonObject.get("metadata");
							String[] str = ((String) metadata.get("mazeSize")).split(" ");
							Coordinate mazeSize = new Coordinate(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
							str = ((String) metadata.get("startHead1")).split(" ");
							Coordinate startHead0 = new Coordinate(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
							str = ((String) metadata.get("startHead2")).split(" ");
							Coordinate startHead1 = new Coordinate(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
							str = ((String) metadata.get("startTail1")).split(" ");
							Coordinate startTailDirection0 = new Coordinate(Integer.parseInt(str[0]),
									Integer.parseInt(str[1]));
							str = ((String) metadata.get("startTail2")).split(" ");
							Coordinate startTailDirection1 = new Coordinate(Integer.parseInt(str[0]),
									Integer.parseInt(str[1]));
							Integer startSize = Integer.parseInt((String) metadata.get("startSize"));
							Direction one = startTailDirection0.convert();
							Direction two = startTailDirection1.convert();
							GameKeeper currentGame = new GameKeeper(mazeSize, startHead0, one, startHead1, two,
									startSize, (String) jsonObject.get("bot1"), (String) jsonObject.get("bot2"));

							ArrayList<Direction> firstBotMovements = new ArrayList<Direction>();
							ArrayList<Direction> secondBotMovements = new ArrayList<Direction>();
							ArrayList<Coordinate> appleCoordinates = new ArrayList<Coordinate>();

							for (int j = 0; j < jsonObject.size() - 3; j++) {
								JSONObject timestamp = (JSONObject) jsonObject.get(Integer.toString(j));
								str = ((String) timestamp.get("apple")).split(" ");
								appleCoordinates
										.add(new Coordinate(Integer.parseInt(str[0]), Integer.parseInt(str[1])));
								str = ((String) timestamp.get("snake1move")).split(" ");
								firstBotMovements.add(
										new Coordinate(Integer.parseInt(str[0]), Integer.parseInt(str[1])).convert());
								str = ((String) timestamp.get("snake2move")).split(" ");
								secondBotMovements.add(
										new Coordinate(Integer.parseInt(str[0]), Integer.parseInt(str[1])).convert());
							}
							currentGame.firstBotMovements = firstBotMovements;
							currentGame.secondBotMovements = secondBotMovements;
							currentGame.appleCoordinates = appleCoordinates;
							// currentGame.gameResult = "";
							currentGame.resultMessage = "";
							showGame(currentGame);

						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/*
	 * Restore and show the game to user with UI
	 */
	private static void showGame(GameKeeper gameKeeper) throws InterruptedException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException, InstantiationException {
		BotLoader loader = new BotLoader();
		Bot bot0 = loader.getBotClass(gameKeeper.botFullname0).getConstructor().newInstance();
		Bot bot1 = loader.getBotClass(gameKeeper.botFullname1).getConstructor().newInstance();

		SnakeGame gameCopy = new SnakeGame(gameKeeper.mazeSize, gameKeeper.startHead0, gameKeeper.startTailDirection0,
				gameKeeper.startHead1, gameKeeper.startTailDirection1, gameKeeper.startSize, bot0, bot1);
		SnakesWindow window = new SnakesWindow(gameCopy, gameKeeper);
		Thread showThread = new Thread(window);
		showThread.start();
		showThread.join();

		if (gameKeeper.resultMessage != null && gameKeeper.resultMessage.length() > 0) {
			System.out.println(gameKeeper.resultMessage);
		}
		System.out.println(gameCopy.name0 + " vs " + gameCopy.name1 + " : " + gameKeeper.getResult());

		Thread.sleep(1000); // to allow users see the result
		window.closeWindow();
	}
}
