package suite.trade.backalloc.run;

import suite.math.stat.Statistic;
import suite.math.stat.Statistic.MeanVariance;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.streamlet.FltStreamlet;
import suite.streamlet.As;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.TimeRange;
import suite.trade.backalloc.BackAllocTester;
import suite.trade.backalloc.BackAllocTester.Simulate;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.util.FunUtil;
import suite.util.FunUtil.Sink;
import suite.util.Object_;
import suite.util.To;

public class BackTester {

	private float initial = 1000000f;
	private Sink<String> log = FunUtil.nullSink();
	private Configuration cfg = new ConfigurationImpl();
	private Statistic stat = new Statistic();

	public Simulate backTest(BackAllocator backAllocator, TimeRange period) {
		Streamlet<Asset> assets0 = cfg.queryCompaniesByMarketCap(period.from); // hkex.getCompanies()
		return backTest(backAllocator, period, assets0);
	}

	public Simulate backTest(BackAllocator backAllocator, TimeRange period, Streamlet<Asset> assets) {
		return BackAllocTester.of(cfg, period, assets, backAllocator, log).simulate(initial);
	}

	public <T> String conclude(Streamlet2<T, Simulate> simulationsByKey) {
		Streamlet<String> results0 = simulationsByKey //
				.map((key, simulate) -> "\nTEST = " + key + ", " + simulate.conclusion());

		Streamlet<String> results1 = simulationsByKey //
				.filterValue(sim -> sim.exception == null) //
				.groupBy(sims -> {
					double txFee = sims.collectAsDouble(Obj_Dbl.sum(sim -> cfg.transactionFee(sim.account.transactionAmount())));

					float[] returns = sims //
							.collect(Obj_Flt.lift(sim -> (float) sim.annualReturn)) //
							.toArray();

					MeanVariance mv = stat.meanVariance(returns);
					double logCagr = FltStreamlet.of(returns).mapFlt(return_ -> (float) Math.log1p(return_)).average();

					return ">> cagr = " + To.string(Math.expm1(logCagr)) //
							+ ", sharpe = " + To.string(mv.mean / mv.standardDeviation()) //
							+ ", txFee = " + To.string(txFee / sims.size());
				}) //
				.map((key, summary) -> "\nTEST = " + key + " " + summary);

		return Streamlet.concat(results0, results1).sort(Object_::compare).collect(As::joined);
	}

}