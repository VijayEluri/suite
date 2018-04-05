package suite.trade;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.math.numeric.Statistic;
import suite.primitive.Ints_;
import suite.primitive.Longs_;
import suite.primitive.adt.pair.FltObjPair;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;

/**
 * Finds the period of various stocks using FFT.
 *
 * @author ywsing
 */
public class PairTest {

	private Configuration cfg = new ConfigurationImpl();
	private Statistic statistic = new Statistic();

	@Test
	public void test() {
		TimeRange period = TimeRange.of(Time.of(2016, 1, 1), Time.of(2017, 1, 1));
		// test(period, "0005.HK", "2888.HK");
		test(period, "0341.HK", "0052.HK");
	}

	private void test(TimeRange period, String symbol0, String symbol1) {
		DataSource ds0 = cfg.dataSource(symbol0, period);
		DataSource ds1 = cfg.dataSource(symbol1, period);
		var ts0 = Longs_.of(ds0.ts);
		var ts1 = Longs_.of(ds1.ts);
		var tradeTimes = Longs_.concat(ts0, ts1).distinct().sort().toArray();
		var prices0 = ds0.alignBeforePrices(tradeTimes).prices;
		var prices1 = ds1.alignBeforePrices(tradeTimes).prices;
		var length = prices0.length;

		var lr = statistic.linearRegression(Ints_ //
				.range(length) //
				.map(i -> FltObjPair.of(prices1[i], new float[] { prices0[i], 1f, })));

		System.out.println(symbol0 + " -> " + symbol1 + lr);
		assertTrue(.4d < lr.r2);
	}

}
