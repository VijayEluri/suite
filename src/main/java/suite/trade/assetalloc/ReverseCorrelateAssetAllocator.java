package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.pair.Pair;
import suite.math.linalg.CholeskyDecomposition;
import suite.math.stat.Statistic;
import suite.math.stat.TimeSeries;
import suite.streamlet.Read;
import suite.trade.DatePeriod;
import suite.trade.Trade_;
import suite.trade.data.DataSource;
import suite.util.To;

public class ReverseCorrelateAssetAllocator implements AssetAllocator {

	private int tor;
	private double kellyReduction;
	private double reverseCorrelationThreshold;

	private CholeskyDecomposition cholesky = new CholeskyDecomposition();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	public static AssetAllocator of() {
		return of(48, .1d, .03d);
	}

	public static AssetAllocator of(int tor, double kellyReduction, double reverseCorrelationThreshold) {
		return AssetAllocator_.unleverage(new ReverseCorrelateAssetAllocator(tor, kellyReduction, reverseCorrelationThreshold));
	}

	private ReverseCorrelateAssetAllocator(int tor, double kellyReduction, double reverseCorrelationThreshold) {
		this.tor = tor;
		this.kellyReduction = kellyReduction;
		this.reverseCorrelationThreshold = reverseCorrelationThreshold;
	}

	@Override
	public List<Pair<String, Double>> allocate(Map<String, DataSource> dataSourceBySymbol, LocalDate backTestDate) {
		DatePeriod samplePeriod = DatePeriod.backTestDaysBefore(backTestDate, 512, 32);
		double riskFreeInterestRate = Trade_.riskFreeInterestRate(tor);

		Set<String> reverseCorrelatingSymbols = Read.from2(dataSourceBySymbol) //
				.mapValue(dataSource -> {
					float[] prices = dataSource.range(samplePeriod).prices;
					float[] logReturns = ts.logReturns(prices);
					double sum = 0d;
					for (int i = tor; i < logReturns.length - tor; i++) {
						int i_ = i;
						sum += stat.correlation(j -> logReturns[i_ - j], j -> logReturns[i_ + j], tor);
					}
					return sum / (logReturns.length - 2 * tor);
				}) //
				.filterValue(corr -> reverseCorrelationThreshold < corr) //
				.keys() //
				.toSet();

		Map<String, float[]> reversePricesBySymbol = Read.from2(dataSourceBySymbol) //
				.filterKey(reverseCorrelatingSymbols::contains) //
				.mapValue(dataSource -> {
					float[] prices = dataSource.prices;
					int length1 = prices.length - 1;
					return To.arrayOfFloats(tor, i -> prices[length1 - i]);
				}) //
				.toMap();

		Map<String, float[]> returnsBySymbol = Read.from2(reversePricesBySymbol) //
				.mapValue(ts::returns) //
				.toMap();

		Map<String, Float> excessReturnBySymbol = Read.from2(reversePricesBySymbol) //
				.mapValue(prices -> {
					double price0 = prices[0];
					double returnTor = (prices[tor - 1] - price0) / price0;
					return (float) (returnTor - riskFreeInterestRate);
				}) //
				.toMap();

		String[] symbols = returnsBySymbol.keySet().toArray(new String[0]);
		int nSymbols = symbols.length;

		float[][] cov = To.arrayOfFloats(nSymbols, nSymbols, (i0, i1) -> {
			float[] returns0 = returnsBySymbol.get(symbols[i0]);
			float[] returns1 = returnsBySymbol.get(symbols[i1]);
			return (float) (stat.covariance(returns0, returns1) * tor);
		});

		float[] returns = To.arrayOfFloats(symbols, excessReturnBySymbol::get);
		float[] allocations = cholesky.inverseMul(cov).apply(returns);

		return Read.range(nSymbols) //
				.map2(i -> symbols[i], i -> allocations[i] * kellyReduction) //
				.toList();
	}

}
