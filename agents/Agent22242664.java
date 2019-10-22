package agents;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;

import loveletter.*;

/**
 * Subclass Action used to store an action and its relative priority.
 * This class is used to enable the use of the Comparable variable
 * within the Priority Queue
 */
class Act {
    public Action action;
    public double priority;

    /**
     * Constructor for the Act class
     * 
     * @param action The action to play
     * @param priority The associated priority for that action given the current state
     */
    public Act(Action action, double priority) {
        this.action = action;
        this.priority = priority;
    }
}

/**
 * An agent for the card game Love Letter
 * This class implements the Agent interface
 */
public class Agent22242664 implements Agent {
    private State current; // Current state
    private int me; // Agent player index
    private PriorityQueue<Act> actions; // Priority queue storing a list of possible actions and their priority
    private int num; // Number of non-eliminated players

    /**
     * Base priority values range from 1 to 20 with 20 being the best
     * possible move to play. A priority of 1 represents an action that
     * negatively benefits the player. The baseline priority of 2
     * indicates a play that has no effect (e.g. playing the priest on a
     * player whos cards are already known)
     */
    private final double GUARD = 8; // Target score and card probability (1 if known) of occuring is added
    private final double PRIEST = 7; // Increase the priority by 2 if we have an agressive card (GUARD/BARON/KING)
    private final double BARON = 7.5; // Target score is added, prefer to play GUARD for safety
    private final double HANDMAIDEN = 9; // Increases based on the number of players alive (higher chance of being targeted)
    private final double PRINCE = 7.4; // Standard base value
    private final double KING = 3; // Increases to 4 if another player has the HANDMAIDEN or 3.5 if they have a PRINCESS
    private final double COUNTESS = 20.0; // Always play if we have to, defaults at 1.5 since we would rather keep a high value card

    /**
     * Reports the Agent's name
     */
	public String toString() {
        return "Bruce";
    }

    /**
     * Method called at the start of a round
     * @param start the starting state of the round
     */
    public void newRound(State start) {
        current = start;
        me = current.getPlayerIndex();
        num = current.numPlayers();
    }

    /**
     * Method called when any agent performs an action. 
     * @param act the action an agent performs
     * @param results the state of play the agent is able to observe.
     */
    public void see(Action act, State results) {
        current = results;
    }

    /**
     * Returns the best card to guess when playing the guard card.
     * This is based on the list of unseen card probabilities and
     * excludes the GUARD card since a guard cannon't guess GUARD.
     * 
     * @param cardProbability The list of cards and their probabilities of occuring
     * @return The best card to guess based on the probabilities
     */
    public Card bestGuardGuess(HashMap<Card, Double> cardProbability) {
        Card bestGuess = null;
        double highest = -1;
        for (Card card : cardProbability.keySet()) {
            if (card == Card.GUARD) continue; // Cant guess guard for guard
            if (cardProbability.get(card) >= highest) {
                highest = cardProbability.get(card);
                // If the probability is the same, we take the higher value card
                bestGuess = (bestGuess == null ? card : 
                    card.value() > bestGuess.value() ? card : bestGuess);
            }
        }
        return bestGuess;
    }

    /**
     * Returns the best card to guess based on the list of 
     * unseen card probabilities.
     * 
     * @param cardProbability The list of cards and their probabilities of occuring
     * @return The best card to guess based on the probabilities
     */
    public Card bestGuess(HashMap<Card, Double> cardProbability) {
        Card bestGuess = null;
        double highest = -1;
        for (Card card : cardProbability.keySet()) {
            if (cardProbability.get(card) >= highest) {
                highest = cardProbability.get(card);
                // If the probability is the same, we take the higher value card
                bestGuess = (bestGuess == null ? card : card.value() > bestGuess.value() ? card : bestGuess);
            }
        }
        return bestGuess;
    }    

    /**
	 * Perform an action after drawing a card from the deck
     * 
	 * @param c the card drawn from the deck
	 * @return the action the agent chooses to perform
	 * @throws IllegalActionException when the Action produced is not legal.
     */
    public Action playCard(Card c) {
        // Store the list of possible actions
        Action act;
        Comparator<Act> actionComparator  = new Comparator<Act>() {
            @Override
            public int compare(Act action1, Act action2) {
                return Double.compare(action2.priority, action1.priority);
            }
        };
        actions = new PriorityQueue<>(actionComparator);
        
        // Fetches the known cards from all players
        Card[] known = new Card[num];
        for (int i = 0; i < num; i++) {
            known[i] = current.getCard(i);
        }

        // Generates a counter of the unseen cards based on the discard pile
        HashMap<Card, Integer> unseenCount = new HashMap<>();
        double unseenTotal = 16;
        unseenCount.put(Card.GUARD, 5);
        unseenCount.put(Card.PRIEST, 2);
        unseenCount.put(Card.BARON, 2);
        unseenCount.put(Card.HANDMAID, 2);
        unseenCount.put(Card.PRINCE, 2);
        unseenCount.put(Card.KING, 1);
        unseenCount.put(Card.COUNTESS, 1);
        unseenCount.put(Card.PRINCESS, 1);
        for (int i = 0; i < num; i++) {
            Iterator<Card> it = current.getDiscards(i);
            Card card = null;
            while (it.hasNext()) {
                card = it.next();
                unseenCount.put(card, unseenCount.get(card) - 1);
                unseenTotal--;
            }
        }
        
        // Determine the probabilty of each card occuring
        HashMap<Card, Double> cardProbability = new HashMap<>();
        for (Card card : unseenCount.keySet()) {
            double probability = (double) unseenCount.get(card) / unseenTotal;
            cardProbability.put(card, probability);
        }

        // Determine the best cards to guess
        Card bestGuess = bestGuess(cardProbability);
        Card bestGuardGuess = bestGuardGuess(cardProbability); // Exclude GUARD
        
        Card[] playable = {c, known[me]};
        for (Card card : playable) {
            Card other = (card == playable[0] ? playable[1] : playable[0]); // Grab other card in hand
            switch (card) {
                case GUARD:
                    for (int i = 0; i < num; i++) {
                        if (i == me || current.eliminated(i)) continue;
                        if (known[i] != null && known[i] != Card.GUARD) { // If we know that a player has a certain card 
                            try {
                                act = Action.playGuard(me, i, known[i]);
                                // Higher priority with higher scored player
                                double priority = GUARD + current.score(i) + 1; // 1 for 100% probability
                                priority = (current.handmaid(i) ? 2 : priority);
                                actions.add(new Act(act, priority));
                            } catch (IllegalActionException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                double priority = GUARD + current.score(i) + cardProbability.get(bestGuardGuess);
                                act = Action.playGuard(me, i, bestGuardGuess);
                                priority = (current.handmaid(i) ? 2 : priority);
                                actions.add(new Act(act, priority));
                            } catch (IllegalActionException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                case PRIEST:
                    for (int i = 0; i < num; i++) {
                        if (i == me || current.eliminated(i)) continue;
                        if (current.handmaid(i)) {
                            try {
                                act = Action.playPriest(me, i);
                                actions.add(new Act(act, 2));
                            } catch (IllegalActionException e) {
                                e.printStackTrace();
                            } 
                        }
                        if (known[i] != null) { // We already know their card
                            try {
                                act = Action.playPriest(me, i);
                                actions.add(new Act(act, 2)); // Low priority, useless action
                            } catch (IllegalActionException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                act = Action.playPriest(me, i);
                                // If we have an agressive card, let's find out what they have
                                double priority = PRIEST + current.score(i) 
                                    + (other == Card.BARON || other == Card.GUARD || other == Card.KING ? 2 : 0);
                                priority = (current.handmaid(i) ? 2 : priority);
                                actions.add(new Act(act, priority));
                            } catch (IllegalActionException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                case BARON:
                    for (int i = 0; i < num; i++) {
                        if (i == me || current.eliminated(i)) continue;
                        if (current.handmaid(i)) {
                            try {
                                act = Action.playBaron(me, i);
                                actions.add(new Act(act, 2));
                            } catch (IllegalActionException e) {
                                e.printStackTrace();
                            } 
                        }
                        if (known[i] != null) {
                            if (known[i].value() < 3) {
                                // Guarantee to lose if played
                                try {
                                    act = Action.playBaron(me, i);
                                    actions.add(new Act(act, 1));
                                } catch (IllegalActionException e) {
                                    e.printStackTrace();
                                }
                            } else if (known[i].value() > 3) {
                                try {
                                    act = Action.playBaron(me, i);
                                    // ≤ Priority than Guard as Guard is a safer card
                                    double priority = BARON + current.score(i); // Prioritise targetting the the winning player
                                    actions.add(new Act(act, priority));
                                } catch (IllegalActionException e) {
                                    e.printStackTrace();
                                }
                            } else { // Draw case
                                try {
                                    // Only play this if nothing else can be played
                                    act = Action.playBaron(me, i);
                                    actions.add(new Act(act, 2));
                                } catch (IllegalActionException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            // Calc probability of winning
                            Iterator<Card> discards = current.getDiscards(i);
                            boolean likelyToLose = false;
                            while (discards.hasNext()) {
                                Card lastPlayed = discards.next();
                                // If they previously played Countess
                                if (lastPlayed == Card.COUNTESS && !discards.hasNext()) { 
                                    // Then we know they likely have a high value card > 3
                                    likelyToLose = true;
                                }
                            }
                            try {
                                act = Action.playBaron(me, i);
                                double priority = 0.0;
                                if (bestGuess.value() < 3) {
                                    priority = BARON + (likelyToLose ? - BARON + 0.5 : -1 + current.score(i) + cardProbability.get(bestGuess));
                                } else {
                                    priority = BARON + (likelyToLose ? - BARON + 0.5 : -3);
                                }
                                actions.add(new Act(act, priority));
                            } catch (IllegalActionException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                case HANDMAID:
                    try {
                        act = Action.playHandmaid(me);
                        int playing = 0;
                        for (int i = 0; i < num; i++) {
                            if (!current.eliminated(i)) playing++;
                        }
                        double priority = HANDMAIDEN + playing/4;
                        actions.add(new Act(act, priority));
                    } catch (IllegalActionException e) {
                        e.printStackTrace();
                    }
                    break;
                case PRINCE:
                    for (int i = 0; i < num; i++) {
                        if (i == me || current.eliminated(i)) continue;
                        if (current.handmaid(i)) {
                            try {
                                act = Action.playPrince(me, i);
                                actions.add(new Act(act, 2));
                            } catch (IllegalActionException e) {
                                e.printStackTrace();
                            } 
                        }
                        if (known[i] != null) {
                            // Force player to discard high value cards and Guard
                            if (known[i] == Card.GUARD || known[i].value() >= 4) {
                               try {
                                    act = Action.playPrince(me, i);
                                    double priority = PRINCE + current.score(i) 
                                        + (known[i] == Card.PRINCESS || known[i] == Card.GUARD ? 0.5 : 0);
                                    actions.add(new Act(act, priority));
                                } catch (IllegalActionException e) {
                                    e.printStackTrace();
                                } 
                            }
                        } else {
                            // Probabilty of them having high card
                            double probability = 0.0;
                            for (Card card2 : cardProbability.keySet()) {
                                if (card2.value() >= 4) continue;
                                probability += cardProbability.get(card2);
                            }
                            Iterator<Card> discards = current.getDiscards(i);
                            boolean likelyHasHigh = false;
                            while (discards.hasNext()) {
                                Card lastPlayed = discards.next();
                                // If they previously played Countess
                                if (lastPlayed == Card.COUNTESS && !discards.hasNext()) { 
                                    // Then we know they likely have a high value card > 3
                                    likelyHasHigh = true;
                                }
                            }
                            try {
                                act = Action.playPrince(me, i);
                                double priority = PRINCE + (likelyHasHigh ? 0 : probability - 1);
                                actions.add(new Act(act, priority));
                            } catch (IllegalActionException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    // Play prince on self if everyone is handmaidened
                    try {
                        act = Action.playPrince(me, me);
                        actions.add(new Act(act, 2)); // If we need to play something
                    } catch (IllegalActionException e) {
                        e.printStackTrace();
                    }
                    break;
                case KING:
                    for (int i = 0; i < num; i++) {
                        if (i == me || current.eliminated(i)) continue;
                        if (current.handmaid(i)) {
                            try {
                                act = Action.playKing(me, i);
                                actions.add(new Act(act, 1));
                            } catch (IllegalActionException e) {
                                e.printStackTrace();
                            }
                            continue;
                        }
                        if (known[i] != null) {
                            // Force player to trade good cards
                            if (known[i] == Card.HANDMAID || known[i] == Card.PRINCESS || known[i] == Card.GUARD) {
                               try {
                                    act = Action.playKing(me, i);
                                    double priority = KING + current.score(i) + 1;
                                    actions.add(new Act(act, priority));
                                } catch (IllegalActionException e) {
                                    e.printStackTrace();
                                } 
                            } else {
                                try {
                                    act = Action.playKing(me, i);
                                    double priority = KING;
                                    actions.add(new Act(act, priority));
                                } catch (IllegalActionException e) {
                                    e.printStackTrace();
                                } 
                            }
                        } else {
                            try {
                                double priority = KING;
                                act = Action.playKing(me, i);
                                actions.add(new Act(act, priority));
                            } catch (IllegalActionException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                case COUNTESS:
                    if (other.value() > 4) {
                        try {
                            act = Action.playCountess(me);
                            actions.add(new Act(act, COUNTESS));
                        } catch (IllegalActionException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            act = Action.playCountess(me);
                            // Lower than 2 since we would rather keep the high card
                            actions.add(new Act(act, 1.5));
                        } catch (IllegalActionException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                default:
                    // Don't play the princess ever
                    break;
            }
        }

        // Play the most optimal card, the card with the highest priority
        return actions.poll().action;
    }
}
