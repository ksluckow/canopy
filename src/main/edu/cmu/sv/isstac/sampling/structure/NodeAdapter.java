package edu.cmu.sv.isstac.sampling.structure;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.sv.isstac.sampling.reward.Reward;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.vm.ChoiceGenerator;

/**
 * @author Kasper Luckow
 *
 */
public abstract class NodeAdapter implements Node {

  private final Node parent;
  private final int choice;
  private final int totalChoicesNum;
  private long visitedNum = 0;
  
  private final Map<Integer, Node> children = new HashMap<>();
  
  private final Reward reward = new Reward();

  private final PathCondition pc;
  
  public NodeAdapter(Node parent, ChoiceGenerator<?> cg, int choice) {
    this.parent = parent;
    this.choice = choice;
    this.totalChoicesNum = (cg != null) ? cg.getTotalNumberOfChoices() : 0;

    //NOTE: This can be **very** expensive to compute for deep paths!
    //I have no idea why we need to get the *previous* pc cg to obtain the pathcondition here
    //if we just use the current cg (i.e. the one provided to the ctor), then the pathcondition
    // is empty at this point
    if(cg != null) {
      PCChoiceGenerator prevPcCg = cg.getPreviousChoiceGeneratorOfType(PCChoiceGenerator.class);
      this.pc = (prevPcCg != null) ? prevPcCg.getCurrentPC() : new PathCondition();
    } else {
      this.pc = new PathCondition();
    }
  }
  
  @Override
  public Node getParent() {
    return parent;
  }
  
  @Override
  public Collection<Node> getChildren() {
    return children.values();
  }
  
  @Override
  public boolean hasChildForChoice(int choice) {
    return this.children.containsKey(choice);
  }
  
  @Override
  public void addChild(Node child) {
    this.children.put(child.getChoice(), child);
  }
  
  @Override
  public Node getChild(int choice) {
    return this.children.get(choice);
  }

  @Override
  public int getChoice() {
    return choice;
  }

  @Override
  public int getTotalChoicesNum() {
    return totalChoicesNum;
  }
  
  @Override
  public Reward getReward() {
    return this.reward;
  }
  
  @Override
  public long getVisitedNum() {
    return this.visitedNum;
  }
  
  @Override
  public void incVisitedNum(long visitedNum) {
    this.visitedNum += visitedNum;
  }

  @Override
  public PathCondition getPathCondition() {
    return this.pc;
  }
}