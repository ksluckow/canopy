/*
 * MIT License
 *
 * Copyright (c) 2017 Carnegie Mellon University.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package edu.cmu.sv.isstac.canopy.mcts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.cmu.sv.isstac.canopy.AnalysisStrategy;
import edu.cmu.sv.isstac.canopy.analysis.MCTSEventObserver;
import edu.cmu.sv.isstac.canopy.policies.SimulationPolicy;
import edu.cmu.sv.isstac.canopy.search.BackPropagator;
import edu.cmu.sv.isstac.canopy.search.TerminationType;
import edu.cmu.sv.isstac.canopy.structure.Node;
import edu.cmu.sv.isstac.canopy.structure.NodeCreationException;
import edu.cmu.sv.isstac.canopy.structure.NodeFactory;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.VM;

/**
 * @author Kasper Luckow
 */
public class MCTSStrategy implements AnalysisStrategy {

  private enum MCTS_STATE {
    SELECTION {
      @Override
      public String toString() {
        return "Selection";
      }
    },
    SIMULATION {
      @Override
      public String toString() {
        return "Simulation";
      }
    };
  }

  private static final Logger logger = JPFLogger.getLogger(MCTSStrategy.class.getName());

  private MCTS_STATE mctsState;
  private MCTSNode last = null;
  private MCTSNode playOutNode = null;
  private MCTSNode root = null;
  private final NodeFactory<MCTSNode> nodeFactory;

  private final SelectionPolicy selectionPolicy;
  private final SimulationPolicy simulationPolicy;

  private boolean expandedFlag = false;
  private int expandedChoice = -1;

  //This is a bit redundant. The event observers are also used by the SamplingAnalysisListener
  private Collection<MCTSEventObserver> observers = new LinkedList<>();

  public MCTSStrategy(SelectionPolicy selectionPolicy,
                      SimulationPolicy simulationPolicy) {
    this.selectionPolicy = selectionPolicy;
    this.simulationPolicy = simulationPolicy;

    this.mctsState = MCTS_STATE.SELECTION;

    //For now we just stick with the default factory
    this.nodeFactory = new MCTSNodeFactory();
  }

  public void addObserver(MCTSEventObserver observer) {
    this.observers.add(observer);
  }

  @Override
  public void makeStateChoice(VM vm, ChoiceGenerator<?> cg, ArrayList<Integer> eligibleChoices) {
    if (this.nodeFactory.isSupportedChoiceGenerator(cg)) {

      // If we expanded a child in the previous CG advancement,
      // we now want to create the node for that child.
      // We can only do that now, because otherwise the CG
      // is not available
      if (expandedFlag) {
        assert mctsState == MCTS_STATE.SIMULATION;
        try {
          last = playOutNode = this.nodeFactory.create(last, cg, expandedChoice);
          assert playOutNode.isSearchTreeNode() == false;
          playOutNode.setIsSearchTreeNode(true);
        } catch (NodeCreationException e) {
          String msg = "Could not create node";
          logger.severe(msg);
          throw new MCTSAnalysisException(msg);
        }
        expandedFlag = false;
      }

      // If empty, we entered an invalid state
      if (eligibleChoices.isEmpty()) {
        String msg = "Entered invalid state: No eligible choices";
        logger.severe(msg);
        throw new MCTSAnalysisException(msg);
      }

      int choice = -1;

      // Check if we are currently in the Selection phase of MCTS
      if (mctsState == MCTS_STATE.SELECTION) {

        // create root
        if (root == null) {
          try {
            root = last = this.nodeFactory.create(null, cg, -1);
            root.setIsSearchTreeNode(true);
          } catch (NodeCreationException e) {
            String msg = "Could not create root node";
            logger.severe(msg);
            throw new MCTSAnalysisException(msg);
          }
        }

        // Check if node is a "frontier", i.e. it has eligible, unexpanded children
        // In this case, we perform the expansion step of MCTS
        if (isFrontierNode(last, eligibleChoices)) {
          ArrayList<Integer> unexpandedEligibleChoices = getUnexpandedEligibleChoices(last, eligibleChoices);

          // Select the unexpanded children according to our selection policy, e.g. randomly
          choice = expandedChoice = selectionPolicy.expandChild(last, unexpandedEligibleChoices);
          expandedFlag = true;

          // After expansion, we proceed to simulation step of MCTS
          mctsState = MCTS_STATE.SIMULATION;
        } else {

          // If it was not a frontier node, we perform the selection step of MCTS
          // A node is selected based on the selection policy, e.g., classic UCB
          last = selectionPolicy.selectBestChild(last, eligibleChoices);
          choice = last.getChoice();
        }
      } else if (mctsState == MCTS_STATE.SIMULATION) {
        // Select choice according to simulation policy, e.g., randomly
        choice = simulationPolicy.selectChoice(vm, cg, eligibleChoices);
        try {
          last = this.nodeFactory.create(last, cg, choice);
          last.setIsSearchTreeNode(false);
        } catch (NodeCreationException e) {
          String msg = "Could not create node";
          logger.severe(msg);
          throw new MCTSAnalysisException(msg);
        }

      } else {
        String msg = "Entered invalid MCTS state: " + mctsState;
        logger.severe(msg);
        throw new MCTSAnalysisException(msg);
      }

      assert choice != -1;

      cg.select(choice);
    } else {
      if (logger.isLoggable(Level.FINE)) {
        String msg = "Unexpected CG: " + cg.getClass().getName();
        logger.fine(msg);
      }
    }
  }

  private ArrayList<Integer> getUnexpandedEligibleChoices(Node n, ArrayList<Integer> eligibleChoices) {
    ArrayList<Integer> unexpandedEligibleChoices = new ArrayList<>();
    Collection<Node> expandedChildren = new HashSet<>();
    for (Node child : n.getChildren()) {
      if (((MCTSNode) child).isSearchTreeNode()) {
        expandedChildren.add(child);
      }
    }

    Set<Integer> childChoices = new HashSet<>();

    //Could expose a method in a node to obtain the following
    for (Node child : expandedChildren) {
      childChoices.add(child.getChoice());
    }

    // We only select the unexpanded children
    // that are eligible for selection, e.g.,
    // not pruned.
    for (int eligibleChoice : eligibleChoices) {
      if (!childChoices.contains(eligibleChoice))
        unexpandedEligibleChoices.add(eligibleChoice);
    }

    // We have hit an illegal state if there
    // are no choices that can be expanded
    if (unexpandedEligibleChoices.isEmpty()) {
      String msg = "No eligible, unexpanded children possible";
      logger.severe(msg);
      throw new MCTSAnalysisException(new IllegalStateException(msg));
    }

    return unexpandedEligibleChoices;
  }

  @Override
  public void pathTerminated(TerminationType termType, long reward,
                             long pathVolume, long amplifiedReward,
                             Search searchState, boolean hasBeenExploredBefore) {
    // Create a final node.
    // It marks a leaf in the Monte Carlo Tree AND
    // in the symbolic execution tree, i.e. it will
    // only be created in the event that MCT reaches
    // and actual leaf in the symbolic execution tree
    if (expandedFlag) {
      assert mctsState == MCTS_STATE.SIMULATION;
      try {
        last = playOutNode = this.nodeFactory.create(last, null, expandedChoice);
        assert playOutNode.isSearchTreeNode() == false;
        playOutNode.setIsSearchTreeNode(true);
      } catch (NodeCreationException e) {
        String msg = "Could not create node  at path termination";
        logger.severe(msg);
        throw new MCTSAnalysisException(msg);
      }
      expandedFlag = false;
    }


    // If this path has been seen before (e.g. if pruning was not used), then we don't perform
    // back progation of rewards!
    if (hasBeenExploredBefore) {
      logger.warning("Path has been explored before (Pruning is turned off? If not, this is an " +
          "error). MCTS *STILL* propagates reward and visit count");
    }
    // Perform backup phase, back propagating rewards and updated visited num according to vol.
    BackPropagator.cumulativeRewardPropagation(last, amplifiedReward, pathVolume, termType);

    // Notify MCTS observers with sample done event
    for (MCTSEventObserver obs : this.observers) {
      obs.sampleDone(playOutNode);
    }

    // Reset exploration to drive a new round of sampling
    this.mctsState = MCTS_STATE.SELECTION;
    this.last = this.root;
    this.playOutNode = null;
  }

  private boolean isFrontierNode(Node node, Collection<Integer> eligibleChoices) {
    for (int eligibleChoice : eligibleChoices) {
      if (!node.hasChildForChoice(eligibleChoice) ||
          ((MCTSNode) node.getChild(eligibleChoice)).isSearchTreeNode() == false)
        return true;
    }
    return false;
  }

  @Override
  public void newSampleStarted(Search samplingSearch) {
    // We don't need to track anything here
  }
}
