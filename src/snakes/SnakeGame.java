package snakes;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

/**
 * Implements main game flow Run game for two bots
 */
public class SnakeGame {

	private static final String LOG_FILE = "log.txt";
	private static final long TIME_FOR_BOT_DECISION = 1;// timeout threshold for taking a decision in seconds

	public final Snake snake0, snake1;
	public final Coordinate mazeSize;
	private final Bot bot0, bot1;
	private final Random rnd = new Random();
	public Coordinate appleCoordinate;
	// public String gameResult;
	public int scoreBot0, scoreBot1;
	public int appleEaten0 = 0;
	public int appleEaten1 = 0;
	private final int snakeSize;
	public String name0, name1;
	private final GameKeeper gameKeeper;

	private final SnakesRunner bot0_runner, bot1_runner;

	/**
	 * Constructs SnakeGame class
	 *
	 * @param mazeSize size of the game board
	 * @param head0    initial coordinate of first snake's head
	 * @param tailDir0 initial direction of first snake's tail
	 * @param head1    initial coordinate of second snake's head
	 * @param tailDir1 initial direction of first snake's tail
	 * @param size     initial length of snakes
	 * @param bot0     first smart snake bot
	 * @param bot1     second smart snake bot
	 */
	public SnakeGame(Coordinate mazeSize, Coordinate head0, Direction tailDir0, Coordinate head1, Direction tailDir1,
			int size, Bot bot0, Bot bot1) {

		snakeSize = size;
		this.mazeSize = mazeSize;
		this.snake0 = new Snake(head0, tailDir0, size, mazeSize);
		this.snake1 = new Snake(head1, tailDir1, size, mazeSize);
		this.bot0 = bot0;
		this.bot1 = bot1;
		this.name0 = bot0.getClass().getSimpleName();
		this.name1 = bot1.getClass().getSimpleName();
		// this.gameResult = "";

		this.gameKeeper = new GameKeeper(mazeSize, head0, tailDir0, head1, tailDir1, size, bot0.getClass().getName(),
				bot1.getClass().getName());

		appleCoordinate = randomNonOccupiedCell();

		this.bot0_runner = new SnakesRunner(bot0, snake0, snake1, mazeSize, appleCoordinate);
		this.bot1_runner = new SnakesRunner(bot1, snake1, snake0, mazeSize, appleCoordinate);
	}

	/**
	 * Converts game to string representation
	 *
	 * @return game state as a string
	 */
	public String toString() {

		char[][] cc = new char[mazeSize.x][mazeSize.y];
		for (int x = 0; x < mazeSize.x; x++)
			for (int y = 0; y < mazeSize.y; y++)
				cc[x][y] = '.';

		// Coordinate of head of first snake on board
		Coordinate h0 = snake0.getHead();
		cc[h0.x][h0.y] = 'h';

		// Coordinate of head of second snake on board
		Coordinate h1 = snake1.getHead();
		cc[h1.x][h1.y] = 'H';

		Iterator<Coordinate> it = snake0.body.stream().skip(1).iterator();
		while (it.hasNext()) {
			Coordinate bp = it.next();
			cc[bp.x][bp.y] = 'b';
		}

		it = snake1.body.stream().skip(1).iterator();
		while (it.hasNext()) {
			Coordinate bp = it.next();
			cc[bp.x][bp.y] = 'B';
		}

		cc[appleCoordinate.x][appleCoordinate.y] = 'X';

		StringBuilder sb = new StringBuilder();
		for (int y = mazeSize.y - 1; y >= 0; y--) {
			for (int x = 0; x < mazeSize.x; x++)
				sb.append(cc[x][y]);
			if (y != 0)
				sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Outputs text to stdout and file
	 *
	 * @param text text that should be displayed
	 */
	private void output(String text) {

		FileWriter fw;
		try {
			fw = new FileWriter(LOG_FILE, true);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		try {
			fw.write(text + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Run one game step, return whether to continue the game
	 *
	 * @return whether to continue the game
	 */
	public boolean runOneStep() throws InterruptedException {

		output(toString());

		// the first bot takes a decision for its move
		bot0_runner.apple = appleCoordinate;
		Thread bot0_thread = new Thread(bot0_runner);

		bot0_thread.start();
		bot0_thread.join(TIME_FOR_BOT_DECISION * 1000);
		boolean s0timeout = false;
		if (bot0_thread.isAlive()) {
			bot0_thread.interrupt();
			s0timeout = true;
			gameKeeper.resultMessage = bot0.getClass().getSimpleName() + " took too long to make a decision";
		}

		Direction d0 = bot0_runner.chosen_direction;

		// the second bot takes a decision for its move
		bot1_runner.apple = appleCoordinate;
		Thread bot1_thread = new Thread(bot1_runner);

		bot1_thread.start();
		bot1_thread.join(TIME_FOR_BOT_DECISION * 1000);

		boolean s1timeout = false;
		if (bot1_thread.isAlive()) {
			bot1_thread.interrupt();
			s1timeout = true;
			gameKeeper.resultMessage = bot1.getClass().getSimpleName() + " took too long to make a decision";
		}

		Direction d1 = bot1_runner.chosen_direction;

		/*
		 * Stopping game condition - one of the snakes decides what it's next move too
		 * long
		 */
		boolean timeout = s0timeout || s1timeout;
		if (timeout) {
			scoreBot0 = s0timeout ? 0 : 1;
			scoreBot1 = s1timeout ? 0 : 1;
			// gameResult = (s0timeout ? 0 : 1) + " - " + ();
			return false;
		}

		output("snake0->" + d0 + ", snake1->" + d1);
		output("Apples eaten: " + appleEaten0 + " - " + appleEaten1);

		boolean grow0 = snake0.getHead().moveTo(d0).equals(appleCoordinate);
		boolean grow1 = snake1.getHead().moveTo(d1).equals(appleCoordinate);

		boolean wasGrow = grow0 || grow1;

		boolean s0dead = !snake0.moveTo(d0, grow0);
		boolean s1dead = !snake1.moveTo(d1, grow1);

		if (wasGrow || appleCoordinate == null) {
			appleEaten0 = snake0.body.size() - snakeSize;
			appleEaten1 = snake1.body.size() - snakeSize;
			appleCoordinate = randomNonOccupiedCell();
		}
		s0dead |= snake0.headCollidesWith(snake1);
		s1dead |= snake1.headCollidesWith(snake0);

		/*
		 * stopping game condition - one of snakes collides with something
		 */
		boolean cont = !(s0dead || s1dead);

		if (!cont) {
			// gameResult = "";
			// String result = "0 - 0";

			if (s0dead ^ s1dead) {
				scoreBot0 = s0dead ? 0 : 1;
				scoreBot1 = s1dead ? 0 : 1;
				// result = (s0dead ? 0 : 1) + " - " + (s1dead ? 0 : 1);
			} else if (s0dead && s1dead) {
				scoreBot0 = appleEaten0 > appleEaten1 ? 1 : 0;
				scoreBot1 = appleEaten1 > appleEaten0 ? 1 : 0;
				// result = (appleEaten0 > appleEaten1 ? 1 : 0) + " - " + (appleEaten1 >
				// appleEaten0 ? 1 : 0);
			} else {
				// draw
				scoreBot0 = 0;
				scoreBot1 = 0;
			}
		}
		return cont;
	}

	/**
	 * Runs game without pauses and store it in gameKeeper
	 */
	public GameKeeper runWithoutPauses(int stepsAllowed) {
		System.out.println("Running game " + bot0.getClass().getSimpleName() + " vs. " + bot1.getClass().getSimpleName()
				+ " in background");

		boolean gameFinished = false;
		for (int i = 0; i < stepsAllowed; i++) {
			try {
				System.out.print("\rSteps played: " + (i + 1) + "/" + stepsAllowed);
				gameKeeper.appleCoordinates.add(appleCoordinate);
				gameFinished = !runOneStep();
				if (gameFinished) {
					gameKeeper.firstBotMovements.add(bot0_runner.chosen_direction);
					gameKeeper.secondBotMovements.add(bot1_runner.chosen_direction);
					gameKeeper.snake1_scores.add(appleEaten0);
					gameKeeper.snake2_scores.add(appleEaten1);
					break;
				}
				gameKeeper.firstBotMovements.add(bot0_runner.chosen_direction);
				gameKeeper.secondBotMovements.add(bot1_runner.chosen_direction);
				gameKeeper.snake1_scores.add(appleEaten0);
				gameKeeper.snake2_scores.add(appleEaten1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println();

		// check for iterations limit:
		if (!gameFinished) {
			int snake0_size = snake0.body.size();
			int snake1_size = snake1.body.size();

			scoreBot0 = snake0_size > snake1_size ? 1 : 0;
			scoreBot1 = snake1_size > snake0_size ? 1 : 0;
			// gameResult = (snake0_size > snake1_size ? 1 : 0) + " - " + (snake1_size >
			// snake0_size ? 1 : 0);
			gameKeeper.resultMessage = "Maximum steps reached (" + stepsAllowed + ")";
		}

		if (gameKeeper.resultMessage != null && gameKeeper.resultMessage.length() > 0) {
			System.out.println(gameKeeper.resultMessage);
			output(gameKeeper.resultMessage + "\n");
		}

		gameKeeper.scoreBot0 = scoreBot0;
		gameKeeper.scoreBot1 = scoreBot1;
		// gameKeeper.gameResult = gameResult;

		return gameKeeper;
	}

	/**
	 * Runs one game step
	 */
	public void showOneStep(Direction firstBotDecision, Direction secondBotDecision) {

		boolean grow0 = snake0.getHead().moveTo(firstBotDecision).equals(appleCoordinate);
		boolean grow1 = snake1.getHead().moveTo(secondBotDecision).equals(appleCoordinate);

		snake0.moveTo(firstBotDecision, grow0);
		snake1.moveTo(secondBotDecision, grow1);

		appleEaten1 = snake1.body.size() - snakeSize;
		appleEaten0 = snake0.body.size() - snakeSize;
	}

	/**
	 * Selects random non-occupied cell of maze
	 *
	 * @return random non-occupied coordinate of the game board
	 */
	private Coordinate randomNonOccupiedCell() {
		while (true) {
			Coordinate c = new Coordinate(rnd.nextInt(mazeSize.x), rnd.nextInt(mazeSize.y));
			if (snake0.elements.contains(c))
				continue;
			if (snake1.elements.contains(c))
				continue;

			return c;
		}
	}

	public String getResult() {
		return scoreBot0 + "-" + scoreBot1;
	}
}
