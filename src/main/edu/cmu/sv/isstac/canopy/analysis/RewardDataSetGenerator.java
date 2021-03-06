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

package edu.cmu.sv.isstac.canopy.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import edu.cmu.sv.isstac.canopy.analysis.SamplingResult.ResultContainer;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.JPFLogger;

/**
 * @author Kasper Luckow
 *
 */
public class RewardDataSetGenerator extends AbstractAnalysisProcessor {
  public static final Logger logger = JPFLogger.getLogger(RewardDataSetGenerator.class.getName());

  private final List<Long> rewards = new LinkedList<>();
  private final FileWriter writer;

  public RewardDataSetGenerator(String outputPath) {
    try {
      this.writer = new FileWriter(outputPath);
    } catch (IOException e) {
      throw new AnalysisException(e);
    }
  }

  @Override
  public void sampleDone(Search searchState, long samples, long propagatedReward,
                         long pathVolume, ResultContainer currentBestResult,
                         boolean hasBeenExplored) {
    rewards.add(propagatedReward);
  }
  
  @Override  
  public void analysisDone(SamplingResult result) {
    BufferedWriter bw = new BufferedWriter(writer);
    try {
      bw.write("sample,reward\n");

      int sampleNum = 1;
      for(Long reward : rewards) {
        bw.write(sampleNum + "," + reward.longValue() + "\n");
        sampleNum++;
      }

      bw.flush();
      bw.close();
    } catch (IOException e) {
      throw new AnalysisException(e);
    }
  }

  @Override
  public void analysisStarted(Search search) { }

}
