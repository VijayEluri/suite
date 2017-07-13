package suite.trade.backalloc.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.stat.BollingerBands;
import suite.math.stat.Quant;
import suite.math.stat.Statistic;
import suite.math.stat.Statistic.MeanVariance;
import suite.primitive.DblPrimitives.ObjObj_Dbl;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.MovingAverage;
import suite.trade.MovingAverage.MovingRange;
import suite.trade.backalloc.BackAllocator;
import suite.trade.backalloc.BackAllocator.OnDateTime;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.trade.singlealloc.BuySellStrategy;
import suite.trade.singlealloc.BuySellStrategy.GetBuySell;
import suite.trade.singlealloc.Strategos;
import suite.util.String_;

public class BackAllocator_ {

	private static BollingerBands bb = new BollingerBands();
	private static MovingAverage ma = new MovingAverage();
	private static Statistic stat = new Statistic();

	public static BackAllocator bollingerBands() {
		return bollingerBands_(32, 0, 2f);
	}

	public static BackAllocator cash() {
		return (akds, indices) -> index -> Collections.emptyList();
	}

	public static BackAllocator donchian(int window) {
		float threshold = .05f;

		return (akds, indices) -> {
			Streamlet2<String, float[]> holdsBySymbol = akds.dsByKey //
					.mapValue(ds -> {
						MovingRange[] movingRange = ma.movingRange(ds.prices, window);
						int length = movingRange.length;
						float[] holds = new float[length];
						double hold = 0d;
						for (int i = 0; i < length; i++) {
							MovingRange range = movingRange[i];
							double price = ds.prices[i];
							double min = range.min;
							double max = range.max;
							double vol = (max - min) / (price * threshold);
							if (1d < vol)
								if (price <= min)
									hold = 1d;
								else if (price < range.median)
									hold = Math.max(0f, hold);
								else if (price < max)
									hold = Math.min(0f, hold);
								else
									hold = -1d;
							holds[i] = (float) hold;
						}
						return holds;
					}) //
					.collect(As::streamlet2);

			return index -> holdsBySymbol //
					.map2((symbol, holds) -> (double) holds[index - 1]) //
					.toList();
		};
	}

	public static BackAllocator ema() {
		int halfLife = 2;
		double scale = 1d / Math.log(.8d);

		return (akds, indices) -> {
			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;

			Map<String, float[]> ema = dsBySymbol //
					.mapValue(ds -> ma.exponentialMovingAvg(ds.prices, halfLife)) //
					.toMap();

			return index -> dsBySymbol //
					.map2((symbol, ds) -> {
						int last = index - 1;
						double lastEma = ema.get(symbol)[last];
						double latest = ds.prices[last];
						return Quant.logReturn(lastEma, latest) * scale;
					}) //
					.toList();
		};
	}

	// reverse draw-down; trendy strategy
	public static BackAllocator revDrawdown() {
		return (akds, indices) -> //
		index -> Read //
				.from2(akds.dsByKey) //
				.map2((symbol, ds) -> {
					int i = index - 1;
					int i0 = Math.max(0, i - 128);
					int dir = 0;

					float[] prices = ds.prices;
					float lastPrice = prices[i];
					float priceo = lastPrice;
					int io = i;

					for (; i0 <= i; i--) {
						float price = prices[i];
						int dir1 = sign(price, lastPrice);

						if (dir != 0 && dir != dir1) {
							double r = (index - io) / (double) (index - i);
							return .36d < r ? Quant.return_(priceo, lastPrice) * r * 4d : 0d;
						} else
							dir = dir1;

						if (sign(price, priceo) == dir) {
							priceo = price;
							io = i;
						}
					}

					return 0d;
				}) //
				.toList();
	}

	public static BackAllocator lastReturn(int nWorsts, int nBests) {
		return (akds, indices) -> index -> {
			List<String> list = akds.dsByKey //
					.map2((symbol, ds) -> ds.lastReturn(index)) //
					.sortBy((symbol, return_) -> return_) //
					.keys() //
					.toList();

			int size = list.size();

			return Streamlet //
					.concat(Read.from(list.subList(0, nWorsts)), Read.from(list.subList(size - nBests, size))) //
					.map2(symbol -> 1d / (nWorsts + nBests)) //
					.toList();
		};
	}

	public static BackAllocator lastReturnsProRata() {
		return (akds, indices) -> index -> {
			Streamlet2<String, Double> returns = akds.dsByKey //
					.map2((symbol, ds) -> ds.lastReturn(index)) //
					.filterValue(return_ -> 0d < return_) //
					.collect(As::streamlet2);

			double sum = returns.collectAsDouble(ObjObj_Dbl.sum((symbol, price) -> price));
			return returns.mapValue(return_ -> return_ / sum).toList();
		};
	}

	public static BackAllocator movingAvg() {
		int nPastDays = 64;
		int nHoldDays = 8;
		float threshold = .15f;
		Strategos strategos = new Strategos();
		BuySellStrategy mamr = strategos.movingAvgMeanReverting(nPastDays, nHoldDays, threshold);

		return (akds, indices) -> {
			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;

			Map<String, GetBuySell> getBuySellBySymbol = dsBySymbol //
					.mapValue(ds -> mamr.analyze(ds.prices)) //
					.toMap();

			return index -> dsBySymbol //
					.map2((symbol, ds) -> {
						GetBuySell gbs = getBuySellBySymbol.get(symbol);
						int hold = 0;
						for (int i = 0; i < index; i++)
							hold += gbs.get(i);
						return (double) hold;
					}) //
					.toList();
		};
	}

	public static BackAllocator movingAvgMedian() {
		int windowSize0 = 4;
		int windowSize1 = 12;

		return (akds, indices) -> {
			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;

			Map<String, float[]> movingAvg0BySymbol = dsBySymbol //
					.mapValue(ds -> ma.movingAvg(ds.prices, windowSize0)) //
					.toMap();

			Map<String, float[]> movingAvg1BySymbol = dsBySymbol //
					.mapValue(ds -> ma.movingAvg(ds.prices, windowSize1)) //
					.toMap();

			Map<String, MovingRange[]> movingRange0BySymbol = dsBySymbol //
					.mapValue(ds -> ma.movingRange(ds.prices, windowSize0)) //
					.toMap();

			Map<String, MovingRange[]> movingRange1BySymbol = dsBySymbol //
					.mapValue(ds -> ma.movingRange(ds.prices, windowSize1)) //
					.toMap();

			return index -> dsBySymbol //
					.map2((symbol, ds) -> {
						int last = index - 1;
						float movingAvg0 = movingAvg0BySymbol.get(symbol)[last];
						float movingAvg1 = movingAvg1BySymbol.get(symbol)[last];
						float movingMedian0 = movingRange0BySymbol.get(symbol)[last].median;
						float movingMedian1 = movingRange1BySymbol.get(symbol)[last].median;
						int sign0 = sign(movingAvg0, movingMedian0);
						int sign1 = sign(movingAvg1, movingMedian1);
						return sign0 == sign1 ? (double) sign0 : 0d;
					}) //
					.toList();
		};
	}

	public static BackAllocator movingMedianMeanRevn() {
		int windowSize0 = 1;
		int windowSize1 = 32;

		return (akds, indices) -> {
			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;

			Map<String, MovingRange[]> movingMedian0BySymbol = dsBySymbol //
					.mapValue(ds -> ma.movingRange(ds.prices, windowSize0)) //
					.toMap();

			Map<String, MovingRange[]> movingMedian1BySymbol = dsBySymbol //
					.mapValue(ds -> ma.movingRange(ds.prices, windowSize1)) //
					.toMap();

			return index -> dsBySymbol //
					.map2((symbol, ds) -> {
						MovingRange[] movingRange0 = movingMedian0BySymbol.get(symbol);
						MovingRange[] movingRange1 = movingMedian1BySymbol.get(symbol);
						int last = index - 1;
						return Quant.return_(movingRange0[last].median, movingRange1[last].median);
					}) //
					.toList();
		};
	}

	public static BackAllocator ofSingle(String symbol) {
		return (akds, indices) -> index -> Arrays.asList(Pair.of(symbol, 1d));
	}

	public static BackAllocator pairs(Configuration cfg, String symbol0, String symbol1) {
		return BackAllocator_ //
				.rsi_(32, .3d, .7d) //
				.relativeToIndex(cfg, symbol0) //
				.filterByAsset(symbol -> String_.equals(symbol, symbol1));
	}

	public static BackAllocator questoQuella(String symbol0, String symbol1) {
		int tor = 64;
		double threshold = 0d;

		BackAllocator ba0 = (akds, indices) -> {
			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;
			Map<String, DataSource> dsBySymbol_ = dsBySymbol.toMap();
			DataSource ds0 = dsBySymbol_.get(symbol0);
			DataSource ds1 = dsBySymbol_.get(symbol1);

			return index -> {
				int ix = index - 1;
				int i0 = ix - tor;
				double p0 = ds0.get(i0).t1, px = ds0.get(ix).t1;
				double q0 = ds1.get(i0).t1, qx = ds1.get(ix).t1;
				double pdiff = Quant.return_(p0, px);
				double qdiff = Quant.return_(q0, qx);

				if (threshold < Math.abs(pdiff - qdiff))
					return Arrays.asList( //
							Pair.of(pdiff < qdiff ? symbol0 : symbol1, 1d), //
							Pair.of(pdiff < qdiff ? symbol1 : symbol0, -1d));
				else
					return Collections.emptyList();
			};
		};

		return ba0.filterByAsset(symbol -> String_.equals(symbol, symbol0) || String_.equals(symbol, symbol1));
	}

	public static BackAllocator rsi() {
		return rsi_(32, .3d, .7d);
	}

	public static BackAllocator sum(BackAllocator... bas) {
		return (akds, indices) -> {
			Streamlet<OnDateTime> odts = Read.from(bas) //
					.map(ba -> ba.allocate(akds, indices)) //
					.collect(As::streamlet);

			return index -> odts //
					.flatMap(odt -> odt.onDateTime(index)) //
					.groupBy(Pair::first_, st -> st.collectAsDouble(Obj_Dbl.sum(Pair::second))) //
					.toList();
		};
	}

	public static BackAllocator tripleMovingAvgs() {
		return (akds, indices) -> {
			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;

			Map<String, float[]> movingAvg0BySymbol = dsBySymbol //
					.mapValue(ds -> ma.exponentialMovingGeometricAvg(ds.prices, 18)) //
					.toMap();

			Map<String, float[]> movingAvg1BySymbol = dsBySymbol //
					.mapValue(ds -> ma.exponentialMovingGeometricAvg(ds.prices, 6)) //
					.toMap();

			Map<String, float[]> movingAvg2BySymbol = dsBySymbol //
					.mapValue(ds -> ma.exponentialMovingGeometricAvg(ds.prices, 2)) //
					.toMap();

			return index -> dsBySymbol //
					.map2((symbol, ds) -> {
						int last = index - 1;
						float movingAvg0 = movingAvg0BySymbol.get(symbol)[last];
						float movingAvg1 = movingAvg1BySymbol.get(symbol)[last];
						float movingAvg2 = movingAvg2BySymbol.get(symbol)[last];
						int sign0 = sign(movingAvg0, movingAvg1);
						int sign1 = sign(movingAvg1, movingAvg2);
						return sign0 == sign1 ? (double) -sign0 : 0d;
					}) //
					.toList();
		};
	}

	public static BackAllocator variableBollingerBands() {
		return (akds, indices) -> {
			return index -> akds.dsByKey //
					.map2((symbol, ds) -> {
						int last = index - 1;
						double hold = 0d;
						float[] prices = ds.prices;

						for (int window = 1; hold == 0d && window < 256; window++) {
							float price = prices[last];
							MeanVariance mv = stat.meanVariance(Arrays.copyOfRange(prices, last - window, last));
							double mean = mv.mean;
							double diff = 3d * mv.standardDeviation();

							if (price < mean - diff)
								hold = 1d;
							else if (mean + diff < price)
								hold = -1d;
						}

						return hold;
					}) //
					.toList();
		};
	}

	private static BackAllocator bollingerBands_(int backPos0, int backPos1, float k) {
		return (akds, indices) -> {
			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;

			Map<String, float[]> percentbBySymbol = dsBySymbol //
					.mapValue(ds -> bb.bb(ds.prices, backPos0, backPos1, k).percentb) //
					.toMap();

			return index -> dsBySymbol //
					.map2((symbol, ds) -> {
						float[] percentbs = percentbBySymbol.get(symbol);
						double hold = 0d;
						for (int i = 0; i < index; i++) {
							float percentb = percentbs[i];
							if (percentb <= 0f)
								hold = 1d;
							else if (.5f < percentb) // un-short
								hold = Math.max(0d, hold);
							else if (percentb < 1f) // un-long
								hold = Math.min(0d, hold);
							else
								hold = -1d;
						}
						return hold;
					}) //
					.toList();
		};
	}

	private static BackAllocator rsi_(int window, double threshold0, double threshold1) {
		return (akds, indices) -> index -> akds.dsByKey //
				.mapValue(ds -> {
					float[] prices = ds.prices;
					int gt = 0, ge = 0;
					for (int i = index - window; i < index; i++) {
						int compare = Float.compare(prices[i - 1], prices[i]);
						gt += compare < 0 ? 1 : 0;
						ge += compare <= 0 ? 1 : 0;
					}
					double rsigt = (double) gt / window;
					double rsige = (double) ge / window;
					if (rsige < threshold0) // over-sold
						return .5d - rsige;
					else if (threshold1 < rsigt) // over-bought
						return .5d - rsigt;
					else
						return 0d;
				}) //
				.toList();
	}

	private static int sign(float f0, float f1) {
		if (f0 < f1)
			return 1;
		else if (f1 < f0)
			return -1;
		else
			return 0;
	}

}
