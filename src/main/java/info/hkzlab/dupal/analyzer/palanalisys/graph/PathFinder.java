package info.hkzlab.dupal.analyzer.palanalisys.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class PathFinder {
   private PathFinder() {};

   @SuppressWarnings("unchecked")
   public static ArrayList<GraphLink> findPathToNearestUnfilledState(GraphState start) {
       Map<Integer, ArrayList<GraphLink>> pathMap = new HashMap<>();
       Queue<GraphState> statesQueue = new LinkedList<>();
       ArrayList<GraphLink> linkStack = null;
       GraphState currentState = start;

       pathMap.put(start.hashCode(), new ArrayList<>()); // Bootstrap the pathMap by adding an empty path to the start state

       while(currentState != null) {
           linkStack = pathMap.get(currentState.hashCode()); // Get the map to the current state
           if(!currentState.isStateFull()) return linkStack; // Ok, we found a state where we need to map other links
       
           GraphLink[] stateLinks = currentState.getLinks(); // Get links present in the current state

            for(GraphLink l : stateLinks) { // For evey link...
                if(!pathMap.containsKey(l.getDestinationState().hashCode())) { // If it's not leading somewhere we've already visited or we've already put in our path map
                    ArrayList<GraphLink> statePath = (ArrayList<GraphLink>)linkStack.clone(); // Copy the map to the current state
                    statePath.add(l); // And append this link to it

                    pathMap.put(l.getDestinationState().hashCode(), statePath); // Then put this new path into the map
                    statesQueue.add(l.getDestinationState()); // And add the state to the queue, to examine later
                }
            }

            currentState = statesQueue.poll(); // Pick the next state from the queue
       }

       return null; // Found nothing...
   }
}
