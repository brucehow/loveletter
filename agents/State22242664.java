package agents;
import java.util.HashMap;
import java.util.Iterator;

import loveletter.*;

/**
 * State in the perspective of the next player
 */
public class State22242664 {
    public int num; // The number of players in the game
    public Card hand; // the cards players currently hold, or null if the player has been eliminated 
    public Card drawn; // Drawn card
    public int numUnseen;
    public boolean[] handmaid;
    public boolean[] eliminated; // the current score of each player
    public HashMap<Card, Integer> unseenCount;
    public int playerIndex;
    public int deckSize;
    public Card[][] discards; //the discarded cards or each player
    public int[] discardCount; //how many cards each player has discarded

    /**
     * Constructor for known actual state
     */
    public State22242664(State current, Card drawn) {
        num = current.numPlayers();
        hand = current.getCard(current.getPlayerIndex());
        this.drawn = drawn;
        handmaid = new boolean[num];
        eliminated = new boolean[num];
        numUnseen = current.unseenCards().length;
        for (int i = 0; i < num; i++) {
            handmaid[i] = current.handmaid(i);
            eliminated[i] = current.eliminated(i);
        }
        unseenCount = new HashMap<>();
        for (int i = 0; i < numUnseen; i++) {
            Card currentCard = current.unseenCards()[i];
            unseenCount.put(currentCard, unseenCount.containsKey(currentCard) ? unseenCount.get(currentCard) + 1 : 1);
        }
        playerIndex = current.getPlayerIndex();
        deckSize = current.deckSize();

        discards = new Card[num][12]; // 12 max discards pp
        discardCount = new int[num];
        for (int i = 0; i < num; i++) {
            discardCount[i] = 0;
            Iterator<Card> it = current.getDiscards(i);
            while (it.hasNext()) {
                discards[i][discardCount[i]++] = it.next();
            }
        }

    }

    public State22242664(int num, int playerIndex, Card hand, Card drawn, int numUnseen, HashMap<Card, Integer> unseenCount,
        boolean[] handmaid, boolean[] eliminated, int deckSize, Card[][] discards, int[] discardCount) {
        this.num = num;
        this.playerIndex = playerIndex;
        this.hand = hand;
        this.drawn = drawn;
        this.handmaid = handmaid;
        this.eliminated = eliminated;
        this.numUnseen = numUnseen;
        this.unseenCount = unseenCount;
        this.deckSize = deckSize;
        this.discards = discards;
        this.discardCount = discardCount;
    }
}