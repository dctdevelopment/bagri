package com.bagri.common.stats;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import com.bagri.common.util.DateUtils;

public class InvocationStatistics extends StatisticsCollector implements StatisticsProvider {

	// stats indexes provided in alphabetical order
	public static final int idx_Avg_Time = 0;
	public static final int idx_Duration = 1;
	public static final int idx_Failed = 2;
	public static final int idx_First = 3;
	public static final int idx_Invoked = 4;
	public static final int idx_Last = 5;
	public static final int idx_Max_Time = 6;
	public static final int idx_Method = 7;
	public static final int idx_Min_Time = 8;
	public static final int idx_Succeed = 9;
	public static final int idx_Sum_Time = 10;
	public static final int idx_Throughput = 11;
	
	public InvocationStatistics(String name) {
		super(name);
	}
/*	
	@Override
	public TabularData getStatisticSeries() {
        TabularData result = null;
        for (Map.Entry<String, MethodInvocationStats> entry: mStats.entrySet()) {
            try {
                Map<String, Object> stats = entry.getValue().toMap();
                stats.put(sn_Method, entry.getKey());
                CompositeData data = JMXUtils.mapToComposite(sn_Name, sn_Header, stats);
                result = JMXUtils.compositeToTabular(sn_Name, sn_Header, sn_Method, result, data);
            } catch (Exception ex) {
                logger.error("getStatisticSeries; error", ex);
            }
        }
        //logger.trace("getStatisticSeries.exit; returning: {}", result);
        return result;
    }
*/
	protected MethodInvocationStats createStatistics(String name) {
		return new MethodInvocationStats();
	}
	
	private class MethodInvocationStats implements Statistics {

		// stats names
		private static final String sn_Avg_Time = "Avg time";
		private static final String sn_Duration = "Duration";
		private static final String sn_Failed = "Failed";
		private static final String sn_First = "First";
		private static final String sn_Invoked = "Invoked";
		private static final String sn_Last = "Last";
		private static final String sn_Max_Time = "Max time";
		private static final String sn_Method = "Method"; 
		private static final String sn_Min_Time = "Min time";
		private static final String sn_Succeed = "Succeed";
		private static final String sn_Sum_Time = "Sum time";
		private static final String sn_Throughput = "Throughput";
		
		private static final String sn_Name = "invocation";
		private static final String sn_Header = "Method Invocation Statistics";
		
		private int cntInvoked;
		private int cntFailed;
		private int cntSucceed;
		
		private long tmMin = Long.MAX_VALUE;
		private long tmMax;
		private long tmSum;
		private long tmFirst = 0;
		private long tmLast;

		@Override
		public String getDescription() {
			return sn_Header;
		}

		@Override
		public String getHeader() {
			return sn_Method;
		}
		
		@Override
		public String getName() {
			return sn_Name;
		}
		
		public void update(StatisticsEvent event) {
			cntInvoked++;
			tmSum += event.getDuration();
			if (event.getDuration() < tmMin) {
				tmMin = event.getDuration();
			}
			if (event.getDuration() > tmMax) {
				tmMax = event.getDuration();
			}
			if (event.isSuccess()) {
				cntSucceed++;
			} else {
				cntFailed++;
			}
			tmLast = event.getTimestamp();
			if (tmFirst == 0) {
				tmFirst = tmLast;
			}
		}
		
		@Override
		public Map<String, Object> toMap() {
			Map<String, Object> result = new HashMap<String, Object>(12);
			result.put(sn_First, new Date(tmFirst));
			result.put(sn_Last, new Date(tmLast));
			result.put(sn_Invoked, cntInvoked);
			result.put(sn_Failed, cntFailed);
			result.put(sn_Succeed, cntSucceed);
			result.put(sn_Max_Time, tmMax);
			result.put(sn_Sum_Time, tmSum);
			if (cntInvoked > 0) {
				result.put(sn_Min_Time, tmMin);
				//double dSum = tmSum;
				double tmAvg = tmSum/cntInvoked;
				result.put(sn_Avg_Time, tmAvg);
				double dCnt = cntInvoked*1000;
				result.put(sn_Throughput, dCnt/(tmLast - tmFirst + tmAvg));
				result.put(sn_Duration, DateUtils.getDuration(tmLast - tmFirst));
			} else {
				result.put(sn_Min_Time, 0L);
				result.put(sn_Avg_Time, 0.0d);
				result.put(sn_Throughput, 0.0d);
				result.put(sn_Duration, 0L);
			}
			return result;
		}
		
		@Override
		public String toString() {
			StringBuffer buff = new StringBuffer(sn_Header).append(" [");
			buff.append(sn_First).append(colon).append(new Date(tmFirst)).append(semicolon);
			buff.append(sn_Last).append(colon).append(new Date(tmLast)).append(semicolon);
			buff.append(sn_Invoked).append(colon).append(cntInvoked).append(semicolon);
			buff.append(sn_Failed).append(colon).append(cntFailed).append(semicolon);
			buff.append(sn_Succeed).append(colon).append(cntSucceed).append(semicolon);
			buff.append(sn_Max_Time).append(colon).append(tmMax).append(semicolon);
			if (cntInvoked > 0) {
				buff.append(sn_Min_Time).append(colon).append(tmMin).append(semicolon);
				//double dSum = tmSum;
				double tmAvg = tmSum/cntInvoked;
				buff.append(sn_Avg_Time).append(colon).append(tmAvg).append(semicolon);
				double dCnt = cntInvoked*1000;
				buff.append(sn_Throughput).append(colon).append(dCnt/(tmLast - tmFirst + tmAvg)).append(semicolon);
				buff.append(sn_Duration).append(colon).append(DateUtils.getDuration(tmLast - tmFirst)).append(semicolon);
			} else {
				buff.append(sn_Min_Time).append(empty);
				buff.append(sn_Avg_Time).append(empty);
				buff.append(sn_Throughput).append(empty);
				buff.append(sn_Duration).append(empty);
			}
			buff.append(sn_Sum_Time).append(colon).append(tmSum);
			buff.append("]");
			return buff.toString();
		}

	}

}
