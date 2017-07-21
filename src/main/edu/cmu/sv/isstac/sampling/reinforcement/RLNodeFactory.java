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

package edu.cmu.sv.isstac.sampling.reinforcement;

import java.util.logging.Logger;

import edu.cmu.sv.isstac.sampling.structure.DefaultNodeFactory;
import edu.cmu.sv.isstac.sampling.structure.Node;
import edu.cmu.sv.isstac.sampling.structure.NodeCreationException;
import edu.cmu.sv.isstac.sampling.structure.NodeFactory;
import edu.cmu.sv.isstac.sampling.structure.NondeterministicNode;
import edu.cmu.sv.isstac.sampling.structure.PCNode;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.vm.ChoiceGenerator;

import static edu.cmu.sv.isstac.sampling.structure.CGClassification.isNondeterministicChoice;
import static edu.cmu.sv.isstac.sampling.structure.CGClassification.isPCNode;

/**
 * @author Kasper Luckow
 */
public class RLNodeFactory implements NodeFactory<RLNode> {
  private static final Logger LOGGER = JPFLogger.getLogger(RLNodeFactory.class.getName());

  @Override
  public RLNode create(RLNode parent, ChoiceGenerator<?> currentCG, int choice) throws NodeCreationException {
    RLNode newNode;
    //Currentcg is null for final nodes
    if(isSupportedChoiceGenerator(currentCG) || currentCG == null) {
      newNode = new RLNode(parent, currentCG, choice);
    } else {
      String msg = "Cannot create node for choicegenerators of type " + currentCG.getClass().getName();
      LOGGER.severe(msg);
      throw new IllegalStateException(msg);
    }
    if (parent != null)
      parent.addChild(newNode);
    return newNode;
  }

  @Override
  public boolean isSupportedChoiceGenerator(ChoiceGenerator<?> cg) {
    return isPCNode(cg) || isNondeterministicChoice(cg);
  }
}
