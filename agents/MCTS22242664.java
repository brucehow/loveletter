package agents;

import java.util.Arrays;
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
    float win;

    public Node(Node parent, State22242664 state, Action action, float prob) {
        this.parent = parent;
        this.state = state;
        this.prob = prob;
        this.children = new HashSet<Node>();
        this.win = 0;
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
            
            // If the node is a leaf
            
        }

        // Back propagation
        Deque<Node> stack = new LinkedList<Node>();
        Set<Node> visited = new HashSet<Node>();
        stack.add(tree);
        while (!stack.isEmpty()) {
            Node view = stack.removeLast();

            boolean visitedAllChildren = true;
            for (Node child : view.children) {
                if (!visited.contains(child)) {
                    stack.add(child);
                    visitedAllChildren = false;
                }
            }
            if (visitedAllChildren) {
                visited.add(view);
                view.parent.win = ; // Todo calc new win prob
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
        
        for (Card cardPlayed : playable) {
            other = (drawn == cardPlayed) ? state.hand : drawn;
            if (cardPlayed.value() > 4 && other == Card.COUNTESS) { // Countess restriction
                continue;
            }

            for (int i = 0; i < state.num; i++) {
                newHandmaid[i] = (i == state.playerIndex) ? false : state.handmaid[i];
            }

            // Adjust the new unseenCount for next player
            newUnseenCount = new HashMap<Card, Integer>(state.unseenCount);

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

                                // Discard handling
                                Card[][] newDiscards = Arrays.copyOf(state.discards, state.discards.length);
                                int[] newDiscardCount = Arrays.copyOf(state.discardCount, state.discardCount.length);
                                newDiscards[state.playerIndex][newDiscardCount[state.playerIndex]++] = cardPlayed; // Current player discards played card
                                if (correctGuess) {
                                    newDiscards[i][newDiscardCount[i]++] = card; // Other player discards card
                                }

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
                                
                                        // Add the other card in hand to unseen for next player
                                        if (tempUnseenCount.containsKey(other)) {
                                            tempUnseenCount.put(other, tempUnseenCount.get(other) + 1);
                                        } else {
                                            tempUnseenCount.put(other, 1);
                                        }
                                        tempUnseenCount.put(newCard1, tempUnseenCount.get(newCard1) - 1);
                                        tempUnseenCount.put(newCard2, tempUnseenCount.get(newCard2) - 1);
                                        removeUnseenCards(tempUnseenCount);
                                        State22242664 newState = new State22242664(state.num, getNextPlayer(state.playerIndex, newElim), newCard1, newCard2, 
                                            state.numUnseen - (correctGuess ? 2 : 1), tempUnseenCount, newHandmaid, newElim, state.deckSize - 1, newDiscards, newDiscardCount);
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
                    for (int i = 0; i < state.num; i++) {
                        if (state.handmaid[i] || i == state.playerIndex || state.eliminated[i]) continue;
                        for (Card newCard1 : newUnseenCount.keySet()) {
                            for (Card newCard2 : newUnseenCount.keySet()) {
                                if (newCard2 == newCard1 && newUnseenCount.get(newCard1) <= 1) {
                                    continue;
                                }

                                Card[][] newDiscards = Arrays.copyOf(state.discards, state.discards.length);
                                int[] newDiscardCount = Arrays.copyOf(state.discardCount, state.discardCount.length);
                                newDiscards[state.playerIndex][newDiscardCount[state.playerIndex]++] = cardPlayed;

                                HashMap<Card, Integer> tempUnseenCount = new HashMap<Card, Integer>(newUnseenCount);
                                if (tempUnseenCount.containsKey(other)) {
                                    tempUnseenCount.put(other, tempUnseenCount.get(other) + 1);
                                } else {
                                    tempUnseenCount.put(other, 1);
                                }
                                tempUnseenCount.put(newCard1, tempUnseenCount.get(newCard1) - 1);
                                tempUnseenCount.put(newCard2, tempUnseenCount.get(newCard2) - 1);
                                removeUnseenCards(tempUnseenCount);
                                State22242664 newState = new State22242664(state.num, getNextPlayer(state.playerIndex, state.eliminated), newCard1, newCard2, 
                                    state.numUnseen - 1, tempUnseenCount, newHandmaid, state.eliminated, state.deckSize - 1, newDiscards, newDiscardCount);
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

                    // Probabily calculation
                    int winCount = 0;
                    int loseCount = 0;
                    int drawCount= 0;
                    for (Card card : state.unseenCount.keySet()) {
                        if (card.value() < 3) winCount++;
                        else if (card.value() > 3) loseCount++;
                        else drawCount++;
                    }

                    for (Card card : state.unseenCount.keySet()) {
                        for (int i = 0; i < state.num; i++) {
                            if (state.handmaid[i] || i == state.playerIndex || state.eliminated[i]) continue;
                            // need to do Edge case where all players are hand maidened
                            
                            newElim = new boolean[state.num];
                            for (int j = 0; j < state.num; j++) {
                                newElim[j] = state.eliminated[j];
                            }
                            int newNumUnseen = state.numUnseen;

                            Card[][] newDiscards = Arrays.copyOf(state.discards, state.discards.length);
                            int[] newDiscardCount = Arrays.copyOf(state.discardCount, state.discardCount.length);
                            newDiscards[state.playerIndex][newDiscardCount[state.playerIndex]++] = cardPlayed;

                            if (card.value() < 3) {
                                prob = winCount / state.numUnseen;
                                newUnseenCount.put(card, newUnseenCount.get(card) - 1);
                                newNumUnseen -= 2;
                                newElim[i] = true;
                                newDiscards[i][newDiscardCount[i]++] = card;
                            } else if (card.value() > 3) {
                                prob = loseCount / state.numUnseen;
                                newElim[state.playerIndex] = true;
                                // Discard other card
                                newUnseenCount.put(other, newUnseenCount.get(other) - 1);
                                newNumUnseen -= 2;
                                newDiscards[state.playerIndex][newDiscardCount[state.playerIndex]++] = other;
                            } else {
                                prob = drawCount / state.numUnseen;
                            }
                            // maybe account when all other players eliminated

                            for (Card newCard1 : newUnseenCount.keySet()) {
                                for (Card newCard2 : newUnseenCount.keySet()) {
                                    if (newCard2 == newCard1 && newUnseenCount.get(newCard1) <= 1) {
                                        continue;
                                    }
                                    HashMap<Card, Integer> tempUnseenCount = new HashMap<Card, Integer>(newUnseenCount);
                                    if (tempUnseenCount.containsKey(other)) {
                                        tempUnseenCount.put(other, tempUnseenCount.get(other) + 1);
                                    } else {
                                        tempUnseenCount.put(other, 1);
                                    }
                                    tempUnseenCount.put(newCard1, tempUnseenCount.get(newCard1) - 1);
                                    tempUnseenCount.put(newCard2, tempUnseenCount.get(newCard2) - 1);
                                    removeUnseenCards(tempUnseenCount);

                                    State22242664 newState = new State22242664(state.num, getNextPlayer(state.playerIndex, newElim), newCard1, newCard2, 
                                        newNumUnseen, tempUnseenCount, newHandmaid, newElim, state.deckSize - 1, newDiscards, newDiscardCount);
                                    try {
                                        Action newAction = Action.playBaron(state.playerIndex, i);
                                        Node newNode = new Node(parent, newState, newAction, prob);
                                        children.add(newNode);
                                    } catch (IllegalActionException e) {
                                        e.printStackTrace();
                                        System.exit(1);
                                    }
                                }
                            }
                        }
                    }
                    break;
                case HANDMAID:
                    newHandmaid[state.playerIndex] = true;
                    for (Card newCard1 : newUnseenCount.keySet()) {
                        for (Card newCard2 : newUnseenCount.keySet()) {
                            if (newCard2 == newCard1 && newUnseenCount.get(newCard1) <= 1) {
                                continue;
                            }

                            Card[][] newDiscards = Arrays.copyOf(state.discards, state.discards.length);
                            int[] newDiscardCount = Arrays.copyOf(state.discardCount, state.discardCount.length);
                            newDiscards[state.playerIndex][newDiscardCount[state.playerIndex]++] = cardPlayed;

                            HashMap<Card, Integer> tempUnseenCount = new HashMap<Card, Integer>(newUnseenCount);
                            if (tempUnseenCount.containsKey(other)) {
                                tempUnseenCount.put(other, tempUnseenCount.get(other) + 1);
                            } else {
                                tempUnseenCount.put(other, 1);
                            }
                            tempUnseenCount.put(newCard1, tempUnseenCount.get(newCard1) - 1);
                            tempUnseenCount.put(newCard2, tempUnseenCount.get(newCard2) - 1);
                            removeUnseenCards(tempUnseenCount);
                            State22242664 newState = new State22242664(state.num, getNextPlayer(state.playerIndex, state.eliminated), newCard1, newCard2, 
                                state.numUnseen - 1, tempUnseenCount, newHandmaid, state.eliminated, state.deckSize - 1, newDiscards, newDiscardCount);
                            try {
                                Action newAction = Action.playHandmaid(state.playerIndex);
                                Node newNode = new Node(parent, newState, newAction, 1);
                                children.add(newNode);
                            } catch (IllegalActionException e) {
                                e.printStackTrace();
                                System.exit(1);
                            }
                        }
                    }
                    break;
                case PRINCE:  
                    for (Card card : state.unseenCount.keySet()) {
                        prob = state.unseenCount.get(card) / state.numUnseen; // todo

                        for (int i = 0; i < state.num; i++) {
                            if (state.handmaid[i] || state.eliminated[i]) continue;
                            // need to do Edge case where all players are hand maidened

                            // Prince can be played on self, but only consider other card in hand to discard
                            if (i == state.playerIndex && card != other) continue;
                            
                            newUnseenCount.put(card, newUnseenCount.get(card) - 1);
                            Card[][] newDiscards = Arrays.copyOf(state.discards, state.discards.length);
                            int[] newDiscardCount = Arrays.copyOf(state.discardCount, state.discardCount.length);
                            newDiscards[state.playerIndex][newDiscardCount[state.playerIndex]++] = cardPlayed;
                            newDiscards[i][newDiscardCount[i]++] = card;

                            for (Card newCard1 : newUnseenCount.keySet()) {
                                for (Card newCard2 : newUnseenCount.keySet()) {
                                    if (newCard2 == newCard1 && newUnseenCount.get(newCard1) <= 1) {
                                        continue;
                                    }
                                    HashMap<Card, Integer> tempUnseenCount = new HashMap<Card, Integer>(newUnseenCount);
                                    if (tempUnseenCount.containsKey(other)) {
                                        tempUnseenCount.put(other, tempUnseenCount.get(other) + 1);
                                    } else {
                                        tempUnseenCount.put(other, 1);
                                    }
                                    tempUnseenCount.put(newCard1, tempUnseenCount.get(newCard1) - 1);
                                    tempUnseenCount.put(newCard2, tempUnseenCount.get(newCard2) - 1);
                                    removeUnseenCards(tempUnseenCount);
                                    State22242664 newState = new State22242664(state.num, getNextPlayer(state.playerIndex, state.eliminated), newCard1, newCard2, 
                                        state.numUnseen - 2, tempUnseenCount, newHandmaid, state.eliminated, state.deckSize - 1, newDiscards, newDiscardCount);
                                    try {
                                        Action newAction = Action.playKing(state.playerIndex, i);
                                        Node newNode = new Node(parent, newState, newAction, 1); // 1 prob?
                                        children.add(newNode);
                                    } catch (IllegalActionException e) {
                                        e.printStackTrace();
                                        System.exit(1);
                                    }
                                }
                            }
                        }
                    }
                    break;
                case KING:
                    // Need known[] implementation!!
                    for (Card newCard : newUnseenCount.keySet()) {

                        Card[][] newDiscards = Arrays.copyOf(state.discards, state.discards.length);
                        int[] newDiscardCount = Arrays.copyOf(state.discardCount, state.discardCount.length);
                        newDiscards[state.playerIndex][newDiscardCount[state.playerIndex]++] = cardPlayed;

                        HashMap<Card, Integer> tempUnseenCount = new HashMap<Card, Integer>(newUnseenCount);

                        // Old card becomes unseen
                        if (tempUnseenCount.containsKey(newCard)) {
                            tempUnseenCount.put(newCard, tempUnseenCount.get(newCard) + 1);
                        } else {
                            tempUnseenCount.put(newCard, 1);
                        }
                        State22242664 newState = new State22242664(state.num, getNextPlayer(state.playerIndex, state.eliminated), other, newCard, 
                            state.numUnseen - 1, tempUnseenCount, newHandmaid, state.eliminated, state.deckSize - 1, newDiscards, newDiscardCount);
                        try {
                            Action newAction = Action.playKing(state.playerIndex, getNextPlayer(state.playerIndex, state.eliminated));
                            Node newNode = new Node(parent, newState, newAction, 1);
                            children.add(newNode);
                        } catch (IllegalActionException e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                    break;
                case COUNTESS:
                    for (Card newCard1 : newUnseenCount.keySet()) {
                        for (Card newCard2 : newUnseenCount.keySet()) {
                            if (newCard2 == newCard1 && newUnseenCount.get(newCard1) <= 1) {
                                continue;
                            }
                            Card[][] newDiscards = Arrays.copyOf(state.discards, state.discards.length);
                            int[] newDiscardCount = Arrays.copyOf(state.discardCount, state.discardCount.length);
                            newDiscards[state.playerIndex][newDiscardCount[state.playerIndex]++] = cardPlayed;

                            HashMap<Card, Integer> tempUnseenCount = new HashMap<Card, Integer>(newUnseenCount);
                            tempUnseenCount.put(newCard1, tempUnseenCount.get(newCard1) - 1);
                            tempUnseenCount.put(newCard2, tempUnseenCount.get(newCard2) - 1);
                            removeUnseenCards(tempUnseenCount);
                            State22242664 newState = new State22242664(state.num, getNextPlayer(state.playerIndex, state.eliminated), newCard1, newCard2, 
                                state.numUnseen - 1, tempUnseenCount, newHandmaid, state.eliminated, state.deckSize - 1, newDiscards, newDiscardCount);
                            try {
                                Action newAction = Action.playCountess(state.playerIndex);
                                Node newNode = new Node(parent, newState, newAction, 1);
                                children.add(newNode);
                            } catch (IllegalActionException e) {
                                e.printStackTrace();
                                System.exit(1);
                            }
                        }
                    }
                    break;
                default: // Never play princess Card
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