package snakes.stats;

import java.util.ArrayList;

import snakes.Bot;
import snakes.GameKeeper;

public class Duel {
	public ArrayList<GameKeeper> matches;
	Class<? extends Bot> bot0, bot1;

	public Duel(Class<? extends Bot> bot0, Class<? extends Bot> bot1) {
		this.bot0 = bot0;
		this.bot1 = bot1;
		matches = new ArrayList<GameKeeper>();
	}

	public double totalScoreBot0() {
		double sum = 0;
		for (GameKeeper game : matches)
			sum += game.scoreBot0;
		return sum;
	}

	public double totalScoreBot1() {
		double sum = 0;
		for (GameKeeper game : matches)
			sum += game.scoreBot1;
		return sum;
	}
}
