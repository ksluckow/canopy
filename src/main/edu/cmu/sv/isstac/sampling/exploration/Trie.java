/*
 * MIT License
 *
 * Copyright (c) 2017 The ISSTAC Authors
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

package edu.cmu.sv.isstac.sampling.exploration;

import java.util.Iterator;

import edu.cmu.sv.isstac.sampling.util.JPFUtil;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.Path;
import gov.nasa.jpf.vm.Transition;

/**
 *
 * @author Kasper Luckow
 *
 */
public class Trie {

  private TrieNode lastAdded = null;
  private TrieNode root;
  private int flags;

  public static class TrieNode {
    // We currently only use the parent for efficiently propagating
    // pruning information. It is an additional reference that could take up significant mem
    private final TrieNode parent;

    private boolean flag;
    private final int choice;
    private TrieNode[] next;

    public TrieNode(int choice, TrieNode parent) {
      this.choice = choice;
      this.parent = parent;
    }

    public void initNext(int siblingSize) {
      this.next = new TrieNode[siblingSize];
    }

    public TrieNode[] getNext() {
      return next;
    }

    public void setFlag(boolean pruned) {
      this.flag = pruned;
    }

    public TrieNode getParent() {
      return this.parent;
    }

    public boolean isFlagSet() {
      return flag;
    }

    @Override
    public String toString() {
      return "<choice " + choice + "; flag: " + this.flag + ">";
    }
  }

  public TrieNode getRoot() {
    return this.root;
  }

  public TrieNode getNode(Path path) {
    return getNode(root, path, 0);
  }

  private TrieNode getNode(TrieNode x, Path path, int d) {
    if (x == null || x.next == null) return null;
    if (d == path.size()) return x;

    int choice = getChoice(path, d);
    return getNode(x.next[choice], path, d + 1);
  }

  public boolean isFlagSet(Path path) {
    TrieNode node = getNode(root, path, 0);
    if(node != null)
      return node.isFlagSet();
    else
      return false;
  }

  public boolean contains(Path path) {
    return getNode(path) != null;
  }

  private int getNumberOfChoices(Path path, int idx) {
    ChoiceGenerator<?> cg = path.get(idx).getChoiceGenerator();
    return cg.getTotalNumberOfChoices();
  }

  private int getChoice(Path path, int idx) {
    ChoiceGenerator<?> cg = path.get(idx).getChoiceGenerator();
    int choice = JPFUtil.getCurrentChoiceOfCG(cg);
    return choice;
  }

  public void setFlag(Path path, boolean flag) {

    root = put(root, null, path, 0, flag);
  }

  private TrieNode put(TrieNode current, TrieNode parent, Path path, int d, boolean flag) {
    if(current == null) {

      //-1 represents choice for root
      int choice = -1;
      if(d != 0) {
        choice = getChoice(path, d - 1);
      }
//      int numberOfChoices = 0;
//      if(d < path.size()) {
       //int numberOfChoices = getNumberOfChoices(path, d);
//      }
      current = new TrieNode(choice, parent);
      current.setFlag(false);
    }
    //We are done adding the path
    if (d == path.size()) {
      if(flag)
        flags++;
      current.setFlag(flag);
      lastAdded = current;
      return current;
    }
//    else {
//      //by default we dont't set the flag for intermediate nodes
//      current.setFlag(false);
//    }
    if(current.next == null) {
      int numberOfChoices = getNumberOfChoices(path, d);
//      }
      current.initNext(numberOfChoices);
    }
    int choice = getChoice(path, d);
    current.next[choice] = put(current.next[choice], current, path, d + 1, flag);
    return current;
  }

  public TrieNode getLastAddedLeafNode() {
    return this.lastAdded;
  }

  public int numberOfFlags() {
    return flags;
  }

  public void clear() {
    //Should be enough. GC to the rescue
    root = lastAdded = null;
    flags = 0;
  }

  public boolean isEmpty() {
    return numberOfFlags() == 0;
  }
}

