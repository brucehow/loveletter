package agents;

import java.util.ArrayList;
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

    int player;
    Node tree;

    public MCTS22242664(State current, Card drawn) {
        player = current.getPlayerIndex();
        State22242664 state = new State22242664(current, drawn);
        Deque<Node> queue = new LinkedList<Node>(); 
        tree = new Node(null, state, null, 1);
        queue.addLast(tree);

        while (!queue.isEmpty()) {
            Node currentNode = queue.removeFirst();
            currentNode.children = getChildren(currentNode.state, currentNode.state.drawn, currentNode);
            for (Node child : currentNode.children) {
                queue.addLast(child);
            }
            // If the node is a leaf
            if (currentNode.children.isEmpty()) {
                System.out.println("Calculating winner val for node");
                currentNode.state.unseenCount.entrySet().forEach(entry->{
                    System.out.println(entry.getKey() + " " + entry.getValue());  
                });
                Arrays.toString(currentNode.state.eliminated);
                currentNode.win = calculateWinner(currentNode.state);
                System.out.println("Done");
            }
        }

        System.out.println("STARTED BACK PROP"); // DEBUG

        // Back propagation
        Deque<Node> stack = new LinkedList<Node>();
        Set<Node> visited = new HashSet<Node>();
        stack.add(tree);
        while (!stack.isEmpty()) {
            Node currentNode = stack.removeLast();

            boolean visitedAllChildren = true;
            for (Node child : currentNode.children) {
                if (!visited.contains(child)) {
                    stack.add(child);
                    visitedAllChildren = false;
                }
            }
            if (visitedAllChildren) {
                visited.add(currentNode);
                // Calculate parents win based on child
                currentNode.parent.win += currentNode.win * currentNode.prob;
            }   
        }
    }

    public float calculateWinner(State22242664 state) {
        if (state.eliminated[player]) return 0; // Eliminated

        // Check if other players are eliminated
        boolean othersElim = true;
        for (int i = 0; i < state.num; i++) {
            if (i == player) continue;
            if (!state.eliminated[i]) {
                othersElim = false;
            }
        }
        if (othersElim) return 1;
       

        Card[] map = new Card[state.numUnseen];
        int index = 0;
        for (Card card : state.unseenCount.keySet()) {
            for (int i = 0 ; i < state.unseenCount.get(card); i++) {
                map[index++] = card;
            }
        }

        ArrayList<Card[]> permutations = new ArrayList<>();
        nextPermutation(permutations, map, map.length);
        float prob = 0;

        for (Card[] hand : permutations) {
            int winner = -1;
            int topCard = -1; 
            int discardValue = -1;

            for (int p = 0; p < state.num; p++) {
                if (!state.eliminated[p]) {
                    int dv = 0;
                    for (int j = 0; j < state.discardCount[p]; j++) dv += state.discards[p][j].value();
                    if (hand[p].value() > topCard || (hand[p].value() == topCard && dv > discardValue)){
                        winner = p;
                        topCard = hand[p].value();
                        discardValue = dv;
                    }
                }
            }
            if (winner == player) prob++;
        }
        return prob / permutations.size();
    }

    public void nextPermutation(ArrayList<Card[]> rv, Card[] array, int size){
        if (size == 1)
            rv.add(Arrays.copyOf(array, array.length));
        for (int i = 0; i < size; ++i) {
            nextPermutation(rv, array, size - 1);
            if (size % 2 == 1) {
                Card temp = array[0];
                array[0] = array[size-1];
                array[size - 1] = temp;
            } else {
                Card temp = array[i];
                array[i] = array[size - 1];
                array[size - 1] = temp;
            }
        }
    }

    public Action getAction() {
        float highestWin = -1;
        Action bestAction = null;
        for (Node child : tree.children) {
            if (child.win > highestWin) {
                highestWin = child.win;
                bestAction = child.action;
            }
        }
        return bestAction;
    }

    public int getNextPlayer(int currentPlayer, boolean[] eliminated) {
        int nextPlayer = (currentPlayer + 1) % eliminated.length;
        while (eliminated[nextPlayer]) {
            nextPlayer = (nextPlayer + 1) % eliminated.length;
        }
        return nextPlayer;
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
                        if (state.unseenCount.get(card) <= 0) continue;
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
                                    if (state.unseenCount.get(newCard1) <= 0) continue;
                                    for (Card newCard2 : newUnseenCount.keySet()) {
                                        if (state.unseenCount.get(newCard2) <= 0) continue;
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
                            if (state.unseenCount.get(newCard1) <= 0) continue;
                            for (Card newCard2 : newUnseenCount.keySet()) {
                                if (state.unseenCount.get(newCard2) <= 0) continue;
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
                        if (state.unseenCount.get(card) <= 0) continue;
                        if (card.value() < 3) winCount++;
                        else if (card.value() > 3) loseCount++;
                        else drawCount++;
                    }

                    for (Card card : state.unseenCount.keySet()) {
                        if (state.unseenCount.get(card) <= 0) continue;
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
                                if (state.unseenCount.get(newCard1) <= 0) continue;
                                for (Card newCard2 : newUnseenCount.keySet()) {
                                    if (state.unseenCount.get(newCard2) <= 0) continue;
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
                        if (state.unseenCount.get(newCard1) <= 0) continue;
                        for (Card newCard2 : newUnseenCount.keySet()) {
                            if (state.unseenCount.get(newCard2) <= 0) continue;
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
                        if (state.unseenCount.get(card) <= 0) continue;
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
                                if (state.unseenCount.get(newCard1) <= 0) continue;
                                for (Card newCard2 : newUnseenCount.keySet()) {
                                    if (state.unseenCount.get(newCard2) <= 0) continue;
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
                                    State22242664 newState = new State22242664(state.num, getNextPlayer(state.playerIndex, state.eliminated), newCard1, newCard2, 
                                        state.numUnseen - 2, tempUnseenCount, newHandmaid, state.eliminated, state.deckSize - 1, newDiscards, newDiscardCount);
                                    try {
                                        Action newAction = Action.playPrince(state.playerIndex, i);
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
                    boolean swap = true;
                    for (int i = 0; i < 2; i++) { // Swap with next player or not
                        for (Card hand : newUnseenCount.keySet()) {
                            if (state.unseenCount.get(hand) <= 0) continue;
                            for (Card newCard : newUnseenCount.keySet()) {
                                if (state.unseenCount.get(newCard) <= 0) continue;

                                Card[][] newDiscards = Arrays.copyOf(state.discards, state.discards.length);
                                int[] newDiscardCount = Arrays.copyOf(state.discardCount, state.discardCount.length);
                                newDiscards[state.playerIndex][newDiscardCount[state.playerIndex]++] = cardPlayed;

                                HashMap<Card, Integer> tempUnseenCount = new HashMap<Card, Integer>(newUnseenCount);

                                // If swapping with next player
                                if (swap) {
                                    hand = other; // need known[] implementation
                                }
                                // new card becomes unseen
                                tempUnseenCount.put(newCard, tempUnseenCount.get(newCard) - 1);
                                
                                State22242664 newState = new State22242664(state.num, getNextPlayer(state.playerIndex, state.eliminated), hand, newCard, 
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
                        }
                        swap = false;
                    }
                    break;
                case COUNTESS:
                    for (Card newCard1 : newUnseenCount.keySet()) {
                        if (state.unseenCount.get(newCard1) <= 0) continue;
                        for (Card newCard2 : newUnseenCount.keySet()) {
                            if (state.unseenCount.get(newCard2) <= 0) continue;
                            if (newCard2 == newCard1 && newUnseenCount.get(newCard1) <= 1) {
                                continue;
                            }
                            Card[][] newDiscards = Arrays.copyOf(state.discards, state.discards.length);
                            int[] newDiscardCount = Arrays.copyOf(state.discardCount, state.discardCount.length);
                            newDiscards[state.playerIndex][newDiscardCount[state.playerIndex]++] = cardPlayed;

                            HashMap<Card, Integer> tempUnseenCount = new HashMap<Card, Integer>(newUnseenCount);
                            tempUnseenCount.put(newCard1, tempUnseenCount.get(newCard1) - 1);
                            tempUnseenCount.put(newCard2, tempUnseenCount.get(newCard2) - 1);
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
}