package info.hkzlab.dupal.analyzer.palanalisys.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class PathFinder {
   private PathFinder() {};

   @SuppressWarnings("unchecked")
   public static ArrayList<GraphLink> findPathToNearestUnfilledState(GraphState start) {
       Map<Integer, ArrayList<GraphLink>> pathMap = new HashMap<>();
       Queue<GraphState> statesQueue = new LinkedList<>();
       ArrayList<GraphLink> linkStack = null;
       GraphState currentState = start;

       pathMap.put(start.hashCode(), new ArrayList<>());

       while(currentState != null) {
           linkStack = pathMap.get(currentState.hashCode());
           if(!currentState.isStateFull()) return linkStack; // Ok, we found a state where we need to map other links
       
           GraphLink[] stateLinks = currentState.getLinks();

            for(GraphLink l : stateLinks) {
                if(!pathMap.containsKey(l.getDestinationState().hashCode())) {
                    ArrayList<GraphLink> statePath = (ArrayList<GraphLink>)linkStack.clone();
                    statePath.add(l);

                    pathMap.put(l.getDestinationState().hashCode(), statePath);
                    statesQueue.add(l.getDestinationState());
                }
            }

            currentState = statesQueue.poll();
       }

       return null;
   }
}
