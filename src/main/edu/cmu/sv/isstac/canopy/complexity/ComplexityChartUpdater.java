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

package edu.cmu.sv.isstac.canopy.complexity;

import java.util.logging.Logger;

import edu.cmu.sv.isstac.canopy.analysis.AbstractAnalysisProcessor;
import edu.cmu.sv.isstac.canopy.analysis.GenericLiveChart;
import edu.cmu.sv.isstac.canopy.analysis.SamplingResult;
import edu.cmu.sv.isstac.canopy.analysis.SamplingResult.ResultContainer;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.JPFLogger;

/**
 * @author Kasper Luckow
 *
 */
public class ComplexityChartUpdater extends AbstractAnalysisProcessor {

  public static final Logger logger = JPFLogger.getLogger(edu.cmu.sv.isstac.canopy.complexity
      .ComplexityChartUpdater.class.getName());

  private final int inputSize;

  private GenericLiveChart chart;

  public ComplexityChartUpdater(GenericLiveChart chart, int inputSize) {
    this.inputSize = inputSize;
    this.chart = chart;
  }

  @Override
  public void sampleDone(Search searchState, long samples, long propagatedReward,
                         long pathVolume, ResultContainer currentBestResult,
                         boolean hasBeenExplored) { }

  @Override
  public void analysisDone(SamplingResult result) {
    chart.update(inputSize, result.getMaxSuccResult().getReward());
  }

  @Override
  public void analysisStarted(Search search) { }

}
