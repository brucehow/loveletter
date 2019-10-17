package agents;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import loveletter.*;

class Node {
    Node parent;
    State22242664 state;
    Set<Node> children;
    Action action;
    float prob;

    public Node(Node parent, State22242664 state, Action action, float prob) {
        this.parent = parent;
        this.state = state;
        this.prob = prob;
        this.children = new HashSet<Node>();
    }
}

public class MCTS22242664 {
    public MCTS22242664(State current, Card drawn) {
        State22242664 state = new State22242664(current, drawn);
        Deque<Node> queue = new LinkedList<Node>(); 
        Node tree = new Node(null, state, null, 1);
        queue.addLast(tree);

        while (!queue.isEmpty()) {
            Node currentNode = queue.removeFirst();
            currentNode.children = getChildren(currentNode.state, currentNode.state.drawn, currentNode);
            for (Node child : currentNode.children) {
                queue.addLast(child);
            }
        }
    }

    public Set<Node> getChildren(State22242664 state, Card drawn, Node parent) {
        Set<Node> children = new HashSet<Node>();
        Card[] playable = {drawn, state.hand};

        if (state.deckSize == 1) return children;

        // New variables for states
        HashMap<Card, Integer> newUnseenCount;
        boolean[] newElim;
        Card other;
        float prob;
        boolean[] newHandmaid = new boolean[state.num];
        for (int j = 0; j < state.num; j++) {
            newHandmaid[j] = (j == state.playerIndex) ? false : state.handmaid[j];
        }
        
        for (Card cardPlayed : playable) {
            switch (cardPlayed) {
                case GUARD:
                    for (Card card : state.unseenCount.keySet()) {
                        if (card == Card.GUARD) { // Can't guess guard
                            continue;
                        }
                        prob = state.unseenCount.get(card) / state.numUnseen;
                        boolean correctGuess = true;

                        for (int k = 0; k < 2; k++) { // For each guess correct/incorrect
                            for (int i = 0; i < state.num; i++) {
                                if (state.handmaid[i] || i == state.playerIndex || state.eliminated[i]) continue;
                                // need to do Edge case where all players are hand maidened
                                
                                newUnseenCount = new HashMap<Card, Integer>(state.unseenCount);
                                newUnseenCount.put(card, newUnseenCount.get(card) - (correctGuess ? 1 : 0));

                                newElim = new boolean[state.num];
                                for (int j = 0; j < state.num; j++) {
                                    newElim[j] = i == j ? (correctGuess ? true : state.eliminated[j]) : state.eliminated[j];
                                }

                                for (Card newCard1 : newUnseenCount.keySet()) {
                                    for (Card newCard2 : newUnseenCount.keySet()) {
                                        if (newCard2 == newCard1 && newUnseenCount.get(newCard1) <= 1) {
                                            continue;
                                        }
                                        HashMap<Card, Integer> tempUnseenCount = new HashMap<Card, Integer>(newUnseenCount);
                                        tempUnseenCount.put(newCard1, tempUnseenCount.get(newCard1) - 1);
                                        tempUnseenCount.put(newCard2, tempUnseenCount.get(newCard2) - 1);
                                        removeUnseenCards(tempUnseenCount);
                                        State22242664 newState = new State22242664(state.num, getNextPlayer(state.playerIndex, newElim), newCard1, newCard2, 
                                            state.numUnseen - (correctGuess ? 2 : 1), tempUnseenCount, newHandmaid, newElim, state.deckSize - 1);
                                        try {
                                            Action newAction = Action.playGuard(state.playerIndex, i, card);
                                            Node newNode = new Node(parent, newState, newAction, (correctGuess ? prob : 1 - prob));
                                            children.add(newNode);
                                        } catch (IllegalActionException e) {
                                            e.printStackTrace();
                                            System.exit(1);
                                        }
                                    }
                                }
                            }
                            correctGuess = false;
                        }
                    }
                    break;
                case PRIEST:
                    // Temp null card pretty mcuh plays it randomly, need logic using knonw[] array
                    newUnseenCount = new HashMap<Card, Integer>(state.unseenCount);
                    other = drawn == Card.PRIEST ? state.hand : drawn;
                    newUnseenCount.put(other, newUnseenCount.get(other) - 1);

                    for (int i = 0; i < state.num; i++) {
                        if (state.handmaid[i] || i == state.playerIndex || state.eliminated[i]) continue;
                        for (Card newCard1 : newUnseenCount.keySet()) {
                            for (Card newCard2 : newUnseenCount.keySet()) {
                                if (newCard2 == newCard1 && newUnseenCount.get(newCard1) <= 1) {
                                    continue;
                                }
                                HashMap<Card, Integer> tempUnseenCount = new HashMap<Card, Integer>(newUnseenCount);
                                tempUnseenCount.put(newCard1, tempUnseenCount.get(newCard1) - 1);
                                tempUnseenCount.put(newCard2, tempUnseenCount.get(newCard2) - 1);
                                removeUnseenCards(tempUnseenCount);
                                State22242664 newState = new State22242664(state.num, getNextPlayer(state.playerIndex, state.eliminated), newCard1, newCard2, 
                                    state.numUnseen - 1, tempUnseenCount, newHandmaid, state.eliminated, state.deckSize - 1);
                                try {
                                    Action newAction = Action.playPriest(state.playerIndex, i);
                                    Node newNode = new Node(parent, newState, newAction, 1);
                                    children.add(newNode);
                                } catch (IllegalActionException e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                            }
                        }
                    }
                    break;
                case BARON:
                    // need to implement with known[]
                    newUnseenCount = new HashMap<Card, Integer>(state.unseenCount);
                    other = drawn == Card.BARON ? state.hand : drawn;
                    newUnseenCount.put(other, newUnseenCount.get(other) - 1);

                    newHandmaid = new boolean[state.num];
                    for (int j = 0; j < state.num; j++) {
                        newHandmaid[j] = j == state.playerIndex ? false : state.handmaid[j];
                    }

                    for (int i = 0; i < state.num; i++) {
                        if (state.handmaid[i] || i == state.playerIndex || state.eliminated[i]) continue;
                        for (Card newCard1 : newUnseenCount.keySet()) {
                            for (Card newCard2 : newUnseenCount.keySet()) {
                                if (newCard2 == newCard1 && newUnseenCount.get(newCard1) <= 1) {
                                    continue;
                                }
                                HashMap<Card, Integer> tempUnseenCount = new HashMap<Card, Integer>(newUnseenCount);
                                tempUnseenCount.put(newCard1, tempUnseenCount.get(newCard1) - 1);
                                tempUnseenCount.put(newCard2, tempUnseenCount.get(newCard2) - 1);
                                removeUnseenCards(tempUnseenCount);
                                State22242664 newState = new State22242664(state.num, getNextPlayer(state.playerIndex, state.eliminated), newCard1, newCard2, 
                                    state.numUnseen - 1, tempUnseenCount, newHandmaid, state.eliminated, state.deckSize - 1);
                                try {
                                    Action newAction = Action.playBaron(state.playerIndex, i);
                                    Node newNode = new Node(parent, newState, newAction, 1);
                                    children.add(newNode);
                                } catch (IllegalActionException e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                            }
                        }
                    }
                    break;
                case HANDMAID:
                    
                    break;
                case PRINCE:  
                    break;
                case KING:
                    break;
                case COUNTESS:
                    break;
                default: // Princess Card
                    break;
            }
        }        
        return children;
    }

    public Action getAction() {
        return null;
    }

    public int getNextPlayer(int currentPlayer, boolean[] eliminated) {
        int nextPlayer = (currentPlayer + 1) % eliminated.length;
        while (eliminated[nextPlayer]) {
            nextPlayer = (nextPlayer + 1) % eliminated.length;
        }
        return nextPlayer;
    }

    public void removeUnseenCards(HashMap<Card, Integer> unseenCount) {
        for (Card card : unseenCount.keySet()) {
            if (unseenCount.get(card) <= 0) {
                unseenCount.remove(card);
            }
        }
    }
}