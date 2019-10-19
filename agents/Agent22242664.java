package agents;

import loveletter.*;

/**
 * An agent for the card game Love Letter
 * This class implements the Agent interface
 */
public class Agent22242664 implements Agent {

    private State current;

    /**
     * Reports the Agent's name
     */
	public String toString() {
        return "Bruce";
    }

    public void newRound(State start) {
        current = start;
    }

    public void see(Action act, State results) {
        current = results;
    }

    /**
	 * Perform an action after drawing a card from the deck
	 * @param c the card drawn from the deck
	 * @return the action the agent chooses to perform
	 * @throws IllegalActionException when the Action produced is not legal.
     */
    public Action playCard(Card c) {
        MCTS22242664 mcts = new MCTS22242664(current, c);
        return mcts.getAction();
    }

}