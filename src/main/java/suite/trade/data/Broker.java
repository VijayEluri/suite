package suite.trade.data;

import java.util.List;
import java.util.function.Predicate;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Trade;
import suite.util.FunUtil.Source;
import suite.util.Memoize;

public interface Broker {

	public List<Trade> queryHistory(Predicate<Trade> pred);

	public double transactionFee(double transactionAmount);

	// https://www.personal.hsbc.com.hk/1/2/hk/investments/stocks/detail
	public class Hsbc implements Broker {
		public List<Trade> queryHistory(Predicate<Trade> pred) {
			return memoizeHistoryRecords.source().filter(pred).toList();
		}

		private static Source<Streamlet<Trade>> memoizeHistoryRecords = Memoize.source(Hsbc::historyRecords);

		private static Streamlet<Trade> historyRecords() {
			return Read.url("https://raw.githubusercontent.com/stupidsing/home-data/master/stock.txt") //
					.collect(As::table) //
					.map(Trade::new) //
					.collect(As::streamlet);
		}

		public double transactionFee(double transactionAmount) {

			// .15d during promotion period
			double hsbcInternetBanking = Math.min(transactionAmount * .01d * .25d, 100d);

			double stampDuty = transactionAmount * .01d * .1d;
			double sfcTxLevy = transactionAmount * .01d * .0027d;
			double sfcInvestorLevy = transactionAmount * .01d * .002d; // suspended
			double hkex = transactionAmount * .01d * .005d;

			// TODO deposit transaction charge (for purchase transaction only)
			// HKD5/RMB5 per board lot (minimum charge: HKD30/RMB30, maximum
			// charge: HKD200/RMB200), waived if the same stocks are purchased
			// and then sold on the same trading day or the subsequent trading
			// day (T or T+1)

			return hsbcInternetBanking + stampDuty + sfcTxLevy + 0d * sfcInvestorLevy + hkex;
		}
	}

}
