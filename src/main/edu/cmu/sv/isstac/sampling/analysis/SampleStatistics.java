package edu.cmu.sv.isstac.sampling.analysis;

import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.concurrent.TimeUnit;

import edu.cmu.sv.isstac.sampling.analysis.AnalysisEventObserver;
import edu.cmu.sv.isstac.sampling.analysis.SamplingResult;
import gov.nasa.jpf.search.Search;

/**
 * @author Kasper Luckow
 */
public class SampleStatistics implements AnalysisEventObserver {

  private static final TimeUnit TIMEUNIT = TimeUnit.SECONDS;

  private Stopwatch stopwatch;

  //Statistics we keep track of
  private long bestRewardSampleNum = 0;
  private long bestRewardTime = 0;
  private long bestReward = -1;
  private long totalSampleNum = 0;
  private double avgThroughput = 0.0;
  private long totalAnalysisTime = 0;

  //statistics NOT stored in memory (as opposed to DescriptiveStatistics)
  private SummaryStatistics sumStats = new SummaryStatistics();
  private int numberOfBestRewards = 0;

  private SamplingResult finalResult;

  @Override
  public void sampleDone(Search searchState, long samples, long propagatedReward, long pathVolume, SamplingResult.ResultContainer currentBestResult) {
    sumStats.addValue(propagatedReward);
    if(propagatedReward > bestReward) {
      bestReward = propagatedReward;
      bestRewardSampleNum = samples;
      bestRewardTime = stopwatch.elapsed(TIMEUNIT);
      numberOfBestRewards = 1;
    } else if(propagatedReward == bestReward) {
      numberOfBestRewards++;
    }
  }

  @Override
  public void analysisDone(SamplingResult result) {
    long totalAnalysisTimeMS = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    totalAnalysisTime = TIMEUNIT.convert(totalAnalysisTimeMS, TimeUnit.MILLISECONDS);
    stopwatch.stop();
    totalSampleNum = result.getNumberOfSamples();
    if(totalAnalysisTimeMS > 0) {
      long msToS = TIMEUNIT.toMillis(1);
      avgThroughput = (totalSampleNum / (double)totalAnalysisTimeMS) * msToS;
    }

    this.finalResult = result;
  }

  @Override
  public void analysisStarted(Search search) {
    this.stopwatch = Stopwatch.createStarted();
  }

  public double getRewardVariance() {
    return this.sumStats.getVariance();
  }

  public double getRewardStandardDeviation() {
    return this.sumStats.getStandardDeviation();
  }

  public double getRewardMean() {
    return this.sumStats.getMean();
  }

  public double getMinReward() {
    return this.sumStats.getMin();
  }

  public int getNumberOfBestRewards() {
    return numberOfBestRewards;
  }

  public long getBestRewardSampleNum() {
    return bestRewardSampleNum;
  }

  public long getBestRewardTime() {
    return bestRewardTime;
  }

  public long getBestReward() {
    return bestReward;
  }

  public long getTotalSampleNum() {
    return totalSampleNum;
  }

  public double getAvgThroughput() {
    return avgThroughput;
  }

  public long getTotalAnalysisTime() {
    return totalAnalysisTime;
  }

  public TimeUnit getTimeUnit() {
    return TIMEUNIT;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("min. reward", getMinReward())
        .add("max. reward", getBestReward())
        .add("max. reward sample #", getBestRewardSampleNum())
        .add("max. reward time", getBestRewardTime())
        .add("# same max. rewards", getNumberOfBestRewards())
        .add("total sample number", getTotalSampleNum())
        .add("total analysis time", getTotalAnalysisTime())
        .add("avg. throughput", getAvgThroughput())
        .add("reward mean", getRewardMean())
        .add("reward variance", getRewardVariance())
        .add("reward stddev", getRewardStandardDeviation())
        .add("final result", this.finalResult)
        .toString();
  }
}