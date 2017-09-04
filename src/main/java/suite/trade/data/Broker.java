package suite.trade.data;

import java.nio.file.Files;
import java.nio.file.Path;

import suite.primitive.Bytes;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Trade;
import suite.util.FunUtil.Source;
import suite.util.HomeDir;
import suite.util.Memoize;

public interface Broker {

	public Streamlet<Trade> queryHistory();

	public double transactionFee(double transactionAmount);

	// https://www.personal.hsbc.com.hk/1/2/hk/investments/stocks/detail
	public class Hsbc implements Broker {
		public Streamlet<Trade> queryHistory() {
			return memoizeHistoryRecords.source();
		}

		private static Source<Streamlet<Trade>> memoizeHistoryRecords = Memoize.source(Hsbc::queryHistory_);

		private static Streamlet<Trade> queryHistory_() {
			String url = "https://raw.githubusercontent.com/stupidsing/home-data/master/stock.txt";
			Path path = HomeDir.resolve("workspace").resolve("home-data").resolve("stock.txt");
			Streamlet<Bytes> bytes = Files.exists(path) ? Read.bytes(path) : Read.url(url);
			return bytes.collect(As::table).map(Trade::of).collect(As::streamlet);
		}

		public double dividendFee(double dividendAmount) {
			return Math.min(30d, dividendAmount * .01d * .5d);
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
