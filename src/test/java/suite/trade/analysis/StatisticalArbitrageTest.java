package suite.trade.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.algo.KmeansCluster;
import suite.math.stat.BollingerBands;
import suite.math.stat.Quant;
import suite.math.stat.Statistic;
import suite.math.stat.Statistic.LinearRegression;
import suite.math.stat.TimeSeries;
import suite.math.transform.DiscreteCosineTransform;
import suite.os.LogUtil;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.Floats_;
import suite.primitive.Int_Flt;
import suite.primitive.Ints_;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.pair.IntFltPair;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.MovingAverage;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.Sina;
import suite.util.FunUtil.Fun;
import suite.util.String_;
import suite.util.To;

public class StatisticalArbitrageTest {

	private TimeRange period = TimeRange.threeYears();

	BollingerBands bb = new BollingerBands();
	private Configuration cfg = new ConfigurationImpl();
	private DiscreteCosineTransform dct = new DiscreteCosineTransform();
	private MovingAverage ma = new MovingAverage();
	private Random random = new Random();
	private Sina sina = new Sina();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	@Test
	public void testAutoRegressivePowersOfTwo() {
		int power = 6;

		DataSource ds = cfg.dataSource(Asset.hsiSymbol).cleanse();
		float[] prices = ds.prices;
		float[][] mas = To.array(float[].class, power, p -> ma.movingAvg(prices, 1 << p));
		float[] returns = ts.returns(prices);
		List<float[]> xsList = new ArrayList<>();
		FloatsBuilder ys = new FloatsBuilder();

		for (int i = 1 << power; i < prices.length; i++) {
			int i_ = i;
			xsList.add(Floats_.toArray(power, p -> mas[p][i_ - (1 << p)]));
			ys.append(returns[i]);
		}

		LinearRegression lr = stat.linearRegression(xsList.toArray(new float[0][]), ys.toFloats().toArray());
		System.out.println(lr);
	}

	// Auto-regressive test
	@Test
	public void testCointegration() {

		// 0004.HK, 0020.HK
		// 0011.HK, 0005.HK
		int tor = 8;
		String symbol0 = "0004.HK";
		String symbol1 = "0945.HK";

		AlignKeyDataSource<String> akds = cfg.dataSources(period, Read.each(symbol0, symbol1));
		Map<String, float[]> pricesBySymbol = akds.dsByKey.mapValue(DataSource::returns).toMap();

		int length = akds.ts.length;
		float[] prices0 = pricesBySymbol.get(symbol0);
		float[] prices1 = pricesBySymbol.get(symbol1);

		float[][] xs = Ints_ //
				.range(tor, length) //
				.map(i -> Floats_.toArray(tor, j -> prices0[i + j - tor])) //
				.toArray(float[].class);

		float[] ys = Ints_ //
				.range(tor, length) //
				.collect(Int_Flt.lift(i -> prices1[i])) //
				.toArray();

		LinearRegression lr = stat.linearRegression(xs, ys);
		System.out.println(lr);
	}

	@Test
	public void testHurstExponent() {
		System.out.println(showStats(ds -> ts.hurst(ds.prices, 64)));
	}

	@Test
	public void testKMeansCluster() {
		AlignKeyDataSource<String> akds = dataSources();
		Map<String, float[]> returnsBySymbol = akds.dsByKey.mapValue(DataSource::returns).toMap();
		System.out.println(kmc(akds.ts.length, returnsBySymbol));
	}

	@Test
	public void testKMeansClusterDct() {
		DctDataSource dctDataSource = dctDataSources();
		System.out.println(kmc(dctDataSource.length, dctDataSource.dctByKey.toMap()));
	}

	private String kmc(int length, Map<String, float[]> ptBySymbol) {
		return new KmeansCluster(length).result(ptBySymbol, 9, 300);
	}

	@Test
	public void testMarketDirection() {
		int lookback = 40;

		DataSource ds = cfg.dataSource(Asset.hsiSymbol).cleanse();
		float[] prices = ds.prices;
		float[] ma20 = ma.movingAvg(prices, 20);
		float[] ma50 = ma.movingAvg(prices, 50);
		double lookback80 = lookback * .8d;
		String flags0 = "-----";

		for (int i = 0; i < prices.length; i++) {
			int past = Math.max(0, i - lookback);
			IntStreamlet past_i = Ints_.range(past, i);
			IntStreamlet past1_i = past_i.drop(1);

			int ma20abovema50 = past_i.filter(j -> ma50[j] < ma20[j]).size();
			int ma50abovema20 = past_i.filter(j -> ma20[j] < ma50[j]).size();
			double r = ma50abovema20 / (double) ma20abovema50;

			boolean isStrglyBullish = true //
					&& lookback <= ma20abovema50 //
					&& past1_i.isAll(j -> ma20[j - 1] <= ma20[j]) //
					&& past1_i.isAll(j -> ma50[j - 1] <= ma50[j]) //
					&& (1.02d * ma50[i] <= ma20[i] || ma20[past] - ma50[past] < ma20[i] - ma50[i]) //
					&& past_i.isAll(j -> ma20[j] <= prices[j]);

			boolean isWeaklyBullish = true //
					&& lookback80 <= ma20abovema50 //
					&& past1_i.isAll(j -> ma50[j - 1] <= ma50[j]) //
					&& past_i.isAll(j -> ma50[j] <= prices[j]);

			boolean isStrglyBearish = true //
					&& lookback <= ma50abovema20 //
					&& past1_i.isAll(j -> ma20[j] <= ma20[j - 1]) //
					&& past1_i.isAll(j -> ma50[j] <= ma50[j - 1]) //
					&& (1.02d * ma20[i] <= ma50[i] || ma50[past] - ma20[past] < ma50[i] - ma20[i]) //
					&& past_i.isAll(j -> prices[j] <= ma20[j]);

			boolean isWeaklyBearish = true //
					&& lookback80 <= ma50abovema20 //
					&& past1_i.isAll(j -> ma50[j] <= ma50[j - 1]) //
					&& past_i.isAll(j -> prices[j] <= ma50[j]);

			boolean isRangeBound__ = true // non-trending
					&& 2d / 3d <= r && r <= 3d / 2d //
					&& stat.meanVariance(past_i.collect(Int_Flt.lift(j -> ma50[j])).toArray()).volatility() < .02d //
					&& .02d < stat.meanVariance(past_i.collect(Int_Flt.lift(j -> ma20[j])).toArray()).volatility() //
					&& (ma20[i] + ma50[i]) * .02d <= Math.abs(ma20[i] - ma50[i]);

			int strgBear = 1 << 4;
			int weakBear = 1 << 3;
			int rngBound = 1 << 2;
			int weakBull = 1 << 1;
			int strgBull = 1 << 0;

			int flag = 0 //
					+ (isStrglyBearish ? strgBear : 0) //
					+ (isWeaklyBearish ? weakBear : 0) //
					+ (isRangeBound__ ? rngBound : 0) //
					+ (isWeaklyBullish ? weakBull : 0) //
					+ (isStrglyBullish ? strgBull : 0);

			String flags = String_ //
					.right("00000" + Integer.toBinaryString(flag), -5) //
					.replace('0', '-') //
					.replace('1', 'M');

			if (!String_.equals(flags0, flags))
				System.out.println(Time.ofEpochSec(ds.ts[i]).ymd() + " " + flags);

			flags0 = flags;
		}
	}

	@Test
	public void testMonteCarloBestBet() {
		int nTrials = 10000;
		int nBets = 40;

		DataSource ds = cfg.dataSource(Asset.hsiSymbol).range(period).cleanse();
		float[] returns = ds.returns();

		for (float bet = 0f - 2f; bet < 1f + 2f; bet += .02f) {
			float notBet = 1f - bet;
			double sum = 0d;

			for (int i = 0; i < nTrials; i++) {
				double account = 1d;
				for (int j = 0; j < nBets; j++) {
					double return_ = returns[random.nextInt(returns.length)];
					account = notBet * account + bet * account * (1d + return_);
				}
				sum += account;
			}

			System.out.println("bet = " + To.string(bet) + ", avg outcome = " + To.string(sum / nTrials));
		}
	}

	@Test
	public void testPeRatio() {
		String out = cfg //
				.queryCompaniesByMarketCap(Time.now()) //
				.map(asset -> asset.symbol) //
				.collect(symbols -> sina.queryFactors(As.streamlet(symbols), true)) //
				.map2(factor -> factor.symbol, factor -> factor.pe) //
				.sortByValue(Float::compare) //
				.map((symbol, peRatio) -> Pair.of(symbol, peRatio).toString()) //
				.collect(As.joinedBy("\n"));
		System.out.println(out);
	}

	// find the period of various stocks using FFT
	@Test
	public void testPeriod() {
		int minPeriod = 4;
		DctDataSource dctDataSources = dctDataSources();

		for (Pair<String, float[]> e : dctDataSources.dctByKey) {
			float[] dct = e.t1;
			IntFltPair max = IntFltPair.of(Integer.MIN_VALUE, Float.MIN_VALUE);

			for (int i = minPeriod; i < dct.length; i++) {
				float f = Math.abs(dct[i]);
				if (max.t1 < f)
					max = IntFltPair.of(i, f);
			}

			LogUtil.info(e.t0 + " has period " + max.t0);
		}
	}

	// Naive Bayes return prediction
	@Test
	public void testReturnDistribution() {
		float[] prices = cfg.dataSource(Asset.hsiSymbol).range(period).prices;
		int maxTor = 16;

		IntObjMap<float[]> differencesByTor = Ints_ //
				.range(1, maxTor) //
				.mapIntObj(tor -> {
					float[] differences = ts.differences(tor, prices);
					Arrays.sort(differences);
					return differences;
				}) //
				.toMap();

		for (int tor = 1; tor < maxTor; tor++)
			System.out.println("tor = " + tor + ", " + stat.moments(differencesByTor.get(tor)));

		Int_Flt predictFun = t -> {
			double[][] cpsArray = Ints_ //
					.range(1, maxTor) //
					.map(tor -> {
						float[] differences = differencesByTor.get(tor);
						int length = differences.length;

						// cumulative probabilities
						double[] cps = new double[11];

						for (int cpsi = 0, predDiff = -500; predDiff <= 500; cpsi++, predDiff += 100) {
							float f = prices[t - 1] + predDiff - prices[t - tor];
							int i = 0;
							while (i < length && differences[i] < f)
								i++;
							cps[cpsi] = i / (double) length;
						}

						return cps;
					}) //
					.toArray(double[].class);

			Map<Double, Double> probabilities = new HashMap<>();

			for (int cpsi = 0, predDiff = -500; predDiff < 500; cpsi++, predDiff += 100) {
				int cpsi_ = cpsi;

				double sum = Ints_ //
						.range(1, maxTor) //
						.map(i -> i) //
						.collectAsDouble(Obj_Dbl.sum(tor -> {
							double probability = cpsArray[tor - 1][cpsi_ + 1] - cpsArray[tor - 1][cpsi_];
							return 1d / probability;
						}));

				probabilities.put(predDiff + 100d / 2d, sum);
			}

			return Read.from2(probabilities) //
					.sortByValue((p0, p1) -> Double.compare(p1, p0)) //
					.first().t0.floatValue();
		};

		for (int t = maxTor + 1; t < prices.length; t++) {
			float predicted = prices[t - 1] + predictFun.apply(t);

			System.out.println("t = " + t //
					+ ", actual = " + prices[t] //
					+ ", predicted = " + predicted);
		}
	}

	@Test
	public void testVarianceRatio() {
		System.out.println(showStats(ds -> ts.varianceRatio(ds.prices, 64)));
	}

	// any relationship between returns and volatility?
	@Test
	public void testVolatility() {
		System.out.println(showStats(ds -> {
			float[] bandwidths0 = bb.bb(ds.prices, 32, 0, 2f).bandwidths;
			float[] returns0 = ds.returns();
			float[] bandwidths1 = ts.drop(1, bandwidths0);
			float[] returns1 = ts.drop(1, returns0);
			return stat.project(bandwidths1, returns1);
		}));
	}

	private DctDataSource dctDataSources() {
		AlignKeyDataSource<String> akds = dataSources();
		int length0 = akds.ts.length;
		int log2 = Quant.log2trunc(length0);
		int fr = length0 - log2;
		return new DctDataSource(log2, akds.dsByKey.mapValue(ds -> dct.dct(Arrays.copyOfRange(ds.prices, fr, length0))));
	}

	private class DctDataSource {
		private int length;
		private Streamlet2<String, float[]> dctByKey;

		private DctDataSource(int t0, Streamlet2<String, float[]> t1) {
			this.length = t0;
			this.dctByKey = t1;
		}
	}

	private String showStats(Fun<DataSource, Double> fun) {
		return dataSources().dsByKey //
				.mapValue(fun) //
				.sortByValue(Double::compare) //
				.map((symbol, value) -> Pair.of(symbol, value).toString()) //
				.collect(As.joinedBy("\n"));
	}

	private AlignKeyDataSource<String> dataSources() {
		Streamlet<String> symbols = cfg //
				.queryCompaniesByMarketCap(Time.now()) //
				.map(asset -> asset.symbol);

		return cfg.dataSources(period, symbols);
	}

}
