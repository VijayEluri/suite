package suite.trade.analysis;

import java.util.List;
import java.util.Map;

import suite.math.linalg.Matrix;
import suite.math.stat.Quant;
import suite.math.stat.Statistic;
import suite.math.stat.Statistic.LinearRegression;
import suite.primitive.Int_Flt;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSourceView;
import suite.trade.data.HkexUtil;

public class FactorLr {

	private long[] timestamps;
	private Streamlet<String> indexSymbols;
	private List<float[]> indexPrices;

	private Configuration cfg;
	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();
	private Time now = Time.now();

	public static FactorLr of(Configuration cfg, Streamlet<String> indexSymbols) {
		return new FactorLr(cfg, indexSymbols);
	}

	private FactorLr(Configuration cfg, Streamlet<String> indexSymbols_) {
		this.cfg = cfg;

		indexSymbols = indexSymbols_.collect(As::streamlet);
		AlignKeyDataSource<String> akds = cfg.dataSources(TimeRange.of(Time.MIN, now), indexSymbols_);
		Map<String, DataSource> dsBySymbol = akds.dsByKey.toMap();

		timestamps = akds.ts;
		indexPrices = indexSymbols.map(symbol -> dsBySymbol.get(symbol).prices).toList();
	}

	public Map<Asset, String> query(Streamlet<Asset> assets) {
		TimeRange period = TimeRange.daysBefore(HkexUtil.getOpenTimeBefore(now), 250 * 3);

		return assets //
				.map2(asset -> ols(cfg.dataSource(asset.symbol), period).toString()) //
				.toMap();
	}

	public BackAllocator backAllocator() {
		return (akds, indices) -> {
			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;
			Map<String, DataSource> dsBySymbol_ = dsBySymbol.toMap();

			DataSourceView<String, LinearRegression> dsv = DataSourceView.of(0, 64, akds,
					(symbol, ds, period) -> ols(dsBySymbol_.get(symbol), period));

			return index -> {
				float[] xs = IntStreamlet //
						.range(indexSymbols.size()) //
						.collect(Int_Flt.lift(i -> {
							float[] indexPrices_ = indexPrices.get(i);
							return (float) Quant.return_(indexPrices_[index - 2], indexPrices_[index - 1]);
						})) //
						.toArray();

				return dsBySymbol //
						.map2((symbol, ds) -> (double) dsv.get(symbol, index).predict(xs)) //
						.toList();
			};
		};
	}

	private LinearRegression ols(DataSource rds0, TimeRange period) {
		DataSource ys = rds0.range(period);

		float[][] returns_ = Read //
				.from(indexPrices) //
				.map(prices -> DataSource.of(timestamps, prices).range(period).alignBeforePrices(ys.ts).returns()) //
				.toArray(float[].class);

		float[][] xs = mtx.transpose(returns_);
		return stat.linearRegression(xs, ys.returns(), indexSymbols.toArray(String.class));
	}

}