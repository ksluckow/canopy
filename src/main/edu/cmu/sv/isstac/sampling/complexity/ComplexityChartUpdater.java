/*
 * Copyright 2017 Carnegie Mellon University Silicon Valley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.cmu.sv.isstac.sampling.complexity;

import java.awt.*;
import java.util.logging.Logger;

import edu.cmu.sv.isstac.sampling.analysis.AbstractAnalysisProcessor;
import edu.cmu.sv.isstac.sampling.analysis.LiveTrackerChart;
import edu.cmu.sv.isstac.sampling.analysis.SamplingResult;
import edu.cmu.sv.isstac.sampling.analysis.SamplingResult.ResultContainer;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.JPFLogger;

/**
 * @author Kasper Luckow
 *
 */
public class ComplexityChartUpdater extends AbstractAnalysisProcessor {

  public static final Logger logger = JPFLogger.getLogger(ComplexityChartUpdater.class.getName());

  private final int inputSize;

  private ComplexityChart chart;

  public ComplexityChartUpdater(ComplexityChart chart, int inputSize) {
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
