package snakes;

public class BackgroundGameRunner implements Runnable {

    private final static int ITERATIONS_PER_GAME = 3 * 60;

    private final SnakeGame game;
    GameKeeper finishedGame;

    public BackgroundGameRunner(SnakeGame game) {
        this.game = game;
    }

    @Override
    public void run() {
        finishedGame = game.runWithoutPauses(ITERATIONS_PER_GAME);
    }
}
