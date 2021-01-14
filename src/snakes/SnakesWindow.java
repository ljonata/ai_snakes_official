package snakes;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * This class is responsible for the game's GUI window
 */
public class SnakesWindow implements Runnable {
	private final JFrame frame;
	private final SnakeCanvas canvas;
	private final SnakeGame game;
	private final GameKeeper finishedGame;

	/**
	 * Creates and set ups the window
	 * 
	 * @param game main game flow with all its states within
	 */
	public SnakesWindow(SnakeGame game, GameKeeper finishedGame) {

		this.finishedGame = finishedGame;
		this.game = game;

		frame = new JFrame("Snake Game");
		canvas = new SnakeCanvas(game);
		JPanel panel = (JPanel) frame.getContentPane();
		panel.setPreferredSize(canvas.renderSize);
		panel.setLayout(new GridLayout());

		canvas.setIgnoreRepaint(false);

		panel.add(canvas);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setResizable(false);
		frame.setVisible(true);

		canvas.requestFocus();

		centreWindow(frame);
	}

	/**
	 * Centers the window
	 * 
	 * @param frame game's window
	 */
	public static void centreWindow(Window frame) {
		Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (int) ((dimension.getWidth() - frame.getWidth()) / 2);
		int y = (int) ((dimension.getHeight() - frame.getHeight()) / 2);
		frame.setLocation(x, y);
	}

	/**
	 * Runs the UI
	 */
	public void run() {
		canvas.repaint();
		for (int i = 0; i < finishedGame.firstBotMovements.size(); i++) {
			long t = System.currentTimeMillis();

			game.appleCoordinate = finishedGame.appleCoordinates.get(i);
			game.showOneStep(finishedGame.firstBotMovements.get(i), finishedGame.secondBotMovements.get(i));

			canvas.repaint();
			long elapsed = System.currentTimeMillis() - t;

			try {
				Thread.sleep(Math.max(200 - elapsed, 0));
			} catch (InterruptedException e) {
				// TODO Necessary?
				// if (game.getResult() != null)
				// game.gameResult = "interrupted";
				break;
			}
		}
	}

	/**
	 * Closes the frame
	 */
	public void closeWindow() {
		frame.setVisible(false);
		frame.dispose();
	}
}
