package suite.trade.backalloc;

import static suite.util.Friends.max;
import static suite.util.Friends.min;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import suite.adt.Mutable;
import suite.adt.pair.Pair;
import suite.math.numeric.Statistic;
import suite.primitive.DblDbl_Dbl;
import suite.primitive.DblPrimitives.ObjObj_Dbl;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.adt.pair.DblFltPair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.Trade_;
import suite.trade.Usex;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSource.Datum;
import suite.trade.walkforwardalloc.WalkForwardAllocator;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.Object_;
import suite.util.Set_;
import suite.util.String_;
import ts.Quant;

/**
 * Strategy that advise you how to divide your money into different investments,
 * i.e. set up a portfolio.
 *
 * @author ywsing
 */
public interface BackAllocator {

	public OnDateTime allocate(AlignKeyDataSource<String> akds, int[] indices);

	public interface OnDateTime {

		/**
		 * @return a portfolio consisting of list of symbols and potential values, or
		 *         null if the strategy do not want to trade on that date. The assets
		 *         will be allocated according to potential values pro-rata.
		 */
		public List<Pair<String, Double>> onDateTime(int index);
	}

	public default BackAllocator byRiskOfReturn() {
		Statistic stat = new Statistic();
		var nDays = 32;

		return (akds, indices) -> {
			Map<String, float[]> returnsByKey = akds.dsByKey.mapValue(DataSource::returns).toMap();
			OnDateTime ba0 = allocate(akds, indices);

			return index -> Read //
					.from2(ba0.onDateTime(index)) //
					.map2((symbol, potential) -> {
						float[] returns = Arrays.copyOfRange(returnsByKey.get(symbol), index - nDays, index);
						return potential / stat.variance(returns);
					}) //
					.toList();
		};
	}

	public default BackAllocator byTime(IntPredicate monthPred) {
		return (akds, indices) -> {
			OnDateTime onDateTime = allocate(akds, indices);

			return index -> monthPred.test(Time.ofEpochSec(akds.ts[index - 1]).month()) //
					? onDateTime.onDateTime(index) //
					: List.of();
		};
	}

	public default BackAllocConfiguration cfg(Fun<Time, Streamlet<Asset>> assetsFun) {
		return new BackAllocConfiguration(assetsFun, this);
	}

	public default BackAllocConfiguration cfgUnl(Fun<Time, Streamlet<Asset>> assetsFun) {
		return pick(40).unleverage().cfg(assetsFun);
	}

	public default BackAllocator dump() {
		return (akds, indices) -> {
			OnDateTime onDateTime = allocate(akds, indices);

			return index -> {
				List<Pair<String, Double>> ratioBySymbol = onDateTime.onDateTime(index);
				System.out.println("ratioBySymbol = " + ratioBySymbol);
				return ratioBySymbol;
			};
		};
	}

	public default BackAllocator even() {
		BackAllocator ba1 = longOnly();

		return (akds, indices) -> {
			OnDateTime onDateTime = ba1.allocate(akds, indices);

			return index -> {
				Streamlet2<String, Double> potentialBySymbol = Read //
						.from2(onDateTime.onDateTime(index)) //
						.collect(As::streamlet2);

				var size = potentialBySymbol.size();

				if (0 < size) {
					double each = 1d / size;

					return potentialBySymbol //
							.filterKey(symbol -> !String_.equals(symbol, Asset.cashSymbol)) //
							.mapValue(potential -> 1d / each) //
							.toList();
				} else
					return List.of();
			};
		};
	}

	public default BackAllocator filterByAsset(Predicate<String> pred) {
		return (akds0, indices) -> {
			AlignKeyDataSource<String> akds1 = new AlignKeyDataSource<>(akds0.ts, akds0.dsByKey.filterKey(pred));

			return allocate(akds1, indices)::onDateTime;
		};
	}

	public default BackAllocator filterByIndex(Configuration cfg) {
		return filterByIndexReturn(cfg, Usex.sp500);
	}

	public default BackAllocator filterByIndexReturn(Configuration cfg, String indexSymbol) {
		DataSource indexDataSource = cfg.dataSource(indexSymbol);

		return (akds, indices) -> {
			OnDateTime onDateTime = allocate(akds, indices);

			return index -> {
				Time date = Time.ofEpochSec(akds.ts[index - 1]).date();
				long t0 = date.addDays(-7).epochSec();
				long tx = date.epochSec();
				DataSource ids = indexDataSource.range(t0, tx);

				double indexPrice0 = ids.get(-1).t1;
				double indexPricex = ids.get(-2).t1;
				double indexReturn = Quant.return_(indexPrice0, indexPricex);

				return -.03f < indexReturn //
						? onDateTime.onDateTime(index) //
						: List.of();
			};
		};
	}

	public default BackAllocator frequency(int freq) {
		return (akds, indices) -> {
			OnDateTime onDateTime = allocate(akds, indices);

			return new OnDateTime() {
				private Time time0;
				private List<Pair<String, Double>> result0;

				public List<Pair<String, Double>> onDateTime(int index) {
					Time time_ = Time.ofEpochSec(akds.ts[index - 1]);
					Time time1 = time_.addDays(-(time_.epochDay() % freq));

					if (!Objects.equals(time0, time1)) {
						time0 = time1;
						result0 = onDateTime.onDateTime(index);
					}

					return result0;
				}
			};
		};
	}

	public default BackAllocator holdDelay(int period) {
		return hold(period, Math::min);
	}

	public default BackAllocator holdExtend(int period) {
		return hold(period, Math::max);
	}

	public default BackAllocator hold(int period, DblDbl_Dbl fun) {
		return (akds, indices) -> {
			Deque<Map<String, Double>> queue = new ArrayDeque<>();
			OnDateTime onDateTime = allocate(akds, indices);

			return index -> {
				queue.addLast(Read //
						.from2(onDateTime.onDateTime(index)) //
						.toMap());

				while (period < queue.size())
					queue.removeFirst();

				Map<String, Double> map = new HashMap<>();

				for (Map<String, Double> m : queue)
					for (Entry<String, Double> e : m.entrySet())
						map.compute(e.getKey(), (k, v) -> fun.apply(v != null ? v : 0d, e.getValue()));

				return Read.from2(map).toList();
			};
		};
	}

	public default BackAllocator january() {
		return byTime(month -> month == 1);
	}

	public default BackAllocator longOnly() {
		return (akds, indices) -> {
			OnDateTime onDateTime = allocate(akds, indices);

			return index -> Read //
					.from2(onDateTime.onDateTime(index)) //
					.map2((symbol, potential) -> {
						return Double.isFinite(potential) ? potential : Fail.t("potential is " + potential);
					}) //
					.filterValue(potential -> 0d < potential) //
					.toList();
		};
	}

	public default BackAllocator pick(int top) {
		return (akds, indices) -> {
			OnDateTime onDateTime = allocate(akds, indices);

			return index -> Read //
					.from2(onDateTime.onDateTime(index)) //
					.sortByValue((r0, r1) -> Object_.compare(r1, r0)) //
					.take(top) //
					.toList();
		};
	}

	public default BackAllocator reallocate() {
		return (akds, indices) -> {
			OnDateTime onDateTime = allocate(akds, indices);

			return index -> {
				List<Pair<String, Double>> potentialBySymbol = onDateTime.onDateTime(index);
				return BackAllocatorUtil.scale(potentialBySymbol, 1d / BackAllocatorUtil.totalPotential(potentialBySymbol));
			};
		};
	}

	public default BackAllocator relative(DataSource indexDataSource) {
		return (akds0, times_) -> {
			Streamlet2<String, DataSource> dsBySymbol1 = akds0.dsByKey //
					.mapValue(ds0 -> {
						float[] indexPrices = indexDataSource.alignBeforePrices(ds0.ts).prices;
						var length = ds0.ts.length;
						Datum[] data1 = new Datum[length];

						for (int i = 0; i < length; i++) {
							double r = 1d / indexPrices[i];
							long t = ds0.ts[i];
							data1[i] = new Datum( //
									t, //
									t + DataSource.tickDuration, //
									(float) (ds0.opens[i] * r), //
									(float) (ds0.closes[i] * r), //
									(float) (ds0.lows[i] * r), //
									(float) (ds0.highs[i] * r), //
									ds0.volumes[i]);
						}

						return DataSource.of(Read.from(data1));
					}) //
					.collect(As::streamlet2);

			return allocate(new AlignKeyDataSource<>(akds0.ts, dsBySymbol1), times_)::onDateTime;
		};
	}

	public default BackAllocator relativeToHsi(Configuration cfg) {
		return relativeToIndex(cfg, "^HSI");
	}

	public default BackAllocator relativeToIndex(Configuration cfg, String indexSymbol) {
		return relative(cfg.dataSource(indexSymbol));
	}

	public default BackAllocator sellInMay() {
		return byTime(month -> month < 5 || 11 <= month);
	}

	public default BackAllocator stopLoss(double percent) {
		return stop(percent, 1E6d);
	}

	public default BackAllocator stop(double stopLoss, double stopGain) {
		return (akds, indices) -> {
			OnDateTime onDateTime = allocate(akds, indices);
			Map<String, DataSource> dsBySymbol = akds.dsByKey.toMap();
			Mutable<Map<String, Double>> mutable = Mutable.of(new HashMap<>());
			Map<String, List<DblFltPair>> entriesBySymbol = new HashMap<>();

			return index -> {
				var last = index - 1;
				List<Pair<String, Double>> potentialBySymbol = onDateTime.onDateTime(index);
				Map<String, Double> potentialBySymbol0 = mutable.get();
				Map<String, Double> potentialBySymbol1 = Read.from2(potentialBySymbol).toMap();

				// find out the transactions
				Map<String, Double> diffBySymbol = Read //
						.from(Set_.union(potentialBySymbol0.keySet(), potentialBySymbol1.keySet())) //
						.map2(symbol -> {
							double potential0 = potentialBySymbol0.getOrDefault(symbol, 0d);
							double potential1 = potentialBySymbol1.getOrDefault(symbol, 0d);
							return potential1 - potential0;
						}) //
						.toMap();

				// check on each stock symbol
				for (Entry<String, Double> e : diffBySymbol.entrySet()) {
					var symbol = e.getKey();
					double diff = e.getValue();
					var bs = Quant.sign(diff);
					float price = dsBySymbol.get(symbol).prices[last];

					List<DblFltPair> entries0 = entriesBySymbol.getOrDefault(symbol, new ArrayList<>());
					List<DblFltPair> entries1 = new ArrayList<>();

					Collections.sort(entries0, (pair0, pair1) -> -bs * Float.compare(pair0.t1, pair1.t1));

					for (DblFltPair entry0 : entries0) {
						double potential0 = entry0.t0;
						float entryPrice = entry0.t1;
						double cancellation;

						// a recent sell would cancel out the highest price buy
						// a recent buy would cancel out the lowest price sell
						if (bs == -1)
							cancellation = min(0, max(diff, -potential0));
						else if (bs == 1)
							cancellation = max(0, min(diff, -potential0));
						else
							cancellation = 0d;

						double potential1 = potential0 + cancellation;
						diff -= cancellation;

						double min = entryPrice * (potential1 < 0 ? stopGain : stopLoss);
						double max = entryPrice * (potential1 < 0 ? stopLoss : stopGain);

						// drop entries that got past their stopping prices
						if (min < price && price < max)
							entries1.add(DblFltPair.of(potential1, entryPrice));
					}

					if (diff != 0d)
						entries1.add(DblFltPair.of(diff, price));

					entriesBySymbol.put(symbol, entries1);
				}

				mutable.update(potentialBySymbol1);

				// re-assemble the entries into current profile
				return Read //
						.multimap(entriesBySymbol) //
						.groupBy(entries -> entries.toDouble(Obj_Dbl.sum(pair -> pair.t0))) //
						.toList();
			};
		};
	}

	public default BackAllocator unleverage() {
		BackAllocator ba0 = this;
		BackAllocator ba1 = Trade_.isShortSell ? ba0 : ba0.longOnly();
		BackAllocator ba2;

		if (Trade_.leverageAmount < 999999f)
			ba2 = (akds, indices) -> {
				OnDateTime onDateTime = ba1.allocate(akds, indices);

				return index -> {
					List<Pair<String, Double>> potentialBySymbol = onDateTime.onDateTime(index);
					double totalPotential = BackAllocatorUtil.totalPotential(potentialBySymbol);
					if (1d < totalPotential)
						return BackAllocatorUtil.scale(potentialBySymbol, 1d / totalPotential);
					else
						return potentialBySymbol;
				};
			};
		else
			ba2 = ba1;

		return ba2;
	}

	public default WalkForwardAllocator walkForwardAllocator() {
		return (akds, index) -> allocate(akds, null).onDateTime(index);
	}

}

class BackAllocatorUtil {

	static List<Pair<String, Double>> scale(List<Pair<String, Double>> potentialBySymbol, double scale) {
		return Read.from2(potentialBySymbol) //
				.filterValue(potential -> potential != 0d) //
				.mapValue(potential -> potential * scale) //
				.toList();
	}

	static double totalPotential(List<Pair<String, Double>> potentialBySymbol) {
		return Read.from2(potentialBySymbol).toDouble(ObjObj_Dbl.sum((symbol, potential) -> potential));
	}

}
