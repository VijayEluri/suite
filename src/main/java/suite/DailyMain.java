package suite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.pair.Pair;
import suite.math.MathUtil;
import suite.os.LogUtil;
import suite.os.SerializedStoreCache;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.smtp.SmtpSslGmail;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Account;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.analysis.Summarize;
import suite.trade.analysis.Summarize.SummarizeByStrategy;
import suite.trade.backalloc.BackAllocConfiguration;
import suite.trade.backalloc.BackAllocConfigurations;
import suite.trade.backalloc.BackAllocConfigurations.Bacs;
import suite.trade.backalloc.BackAllocTester;
import suite.trade.backalloc.BackAllocator;
import suite.trade.backalloc.strategy.BackAllocatorOld;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.trade.singlealloc.BuySellStrategy;
import suite.trade.singlealloc.SingleAllocBackTest;
import suite.trade.singlealloc.Strategos;
import suite.util.FunUtil.Sink;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;
import suite.util.Serialize;
import suite.util.Set_;
import suite.util.String_;
import suite.util.To;

// mvn compile exec:java -Dexec.mainClass=suite.DailyMain
public class DailyMain extends ExecutableProgram {

	private Set<String> blackList = To.set();

	private Configuration cfg = new ConfigurationImpl();
	private Serialize serialize = Serialize.me;
	private StringBuilder sb = new StringBuilder();
	private Sink<String> log = To.sink(sb);
	private Time today = Time.now();

	private Bacs bacs = new BackAllocConfigurations(cfg).bacs();

	private class Result {
		private String strategy;
		private List<Trade> trades;

		private Result(String strategy, List<Trade> trades) {
			this.strategy = strategy;
			this.trades = trades;
		}
	}

	public static void main(String[] args) {
		RunUtil.run(DailyMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		Trade_.blackList = Set_.union(Trade_.blackList, blackList);

		var sellPool = "sellpool";
		var ymd = Time.now().ymd();
		var td = ymd + "#";

		// perform systematic trading
		List<Result> results = List.of( //
				alloc(bacs.pair_bb, 66666f), //
				alloc("bug", bacs.bac_sell, 0f), //
				alloc(bacs.pair_donchian, 100000f), //
				alloc(bacs.pair_ema, 0f), //
				mamr(50000f), //
				alloc(bacs.pair_pmamr, 150000f), //
				alloc(bacs.pair_pmamr2, 366666f), //
				alloc(bacs.pair_pmmmr, 80000f), //
				alloc(bacs.pair_revco, 0f), //
				alloc(bacs.pair_tma, 0f), //
				alloc(sellPool, bacs.bac_sell, 0f));

		// unused strategies
		if (Boolean.FALSE) {
			alloc(bacs.pair_donchian, 100000f);
			pairs(0f, "0341.HK", "0052.HK");
			sellForEarn(sellPool);
		}

		SummarizeByStrategy<Object> sbs = Summarize.of(cfg).summarize();

		Streamlet2<String, Trade> strategyTrades = Read //
				.from(results) //
				.concatMap2(result -> Read.from(result.trades).map2(trade -> result.strategy, trade -> trade)) //
				.filterValue(trade -> trade.buySell != 0) //
				.collect(As::streamlet2);

		Streamlet2<String, Trade> requestTrades = strategyTrades.filterKey(strategy -> !String_.equals(strategy, sellPool));
		var amounts = strategyTrades.values().collect(Obj_Dbl.lift(Trade::amount));
		var buys_ = amounts.filter(amount -> 0d < amount).sum();
		var sells = amounts.filter(amount -> amount < 0d).sum();

		sb.append(sbs.log //
				+ "\n" + sbs.pnlByKey //
				+ "\n" + strategyTrades //
						.sortBy((strategy, trade) -> trade.amount()) //
						.map((strategy, trade) -> "\n" //
								+ (0 <= trade.buySell ? "BUY^" : "SELL") //
								+ " SIGNAL(" + strategy + ")" + trade //
								+ " = " + To.string(trade.amount())) //
						.collect(As::joined) //
				+ "\n" //
				+ "\nBUY REQUESTS" //
				+ requestTrades //
						.filterValue(trade -> 0 < trade.buySell) //
						.map((strategy, t) -> "" //
								+ "\n" + Trade.of(td, -t.buySell, t.symbol, t.price, sellPool).record() //
								+ "\n" + Trade.of(td, +t.buySell, t.symbol, t.price, strategy).record()) //
						.collect(As::joined) //
				+ "\n" //
				+ "\nSELL REQUESTS" //
				+ requestTrades //
						.filterValue(trade -> trade.buySell < 0) //
						.map((strategy, t) -> "" //
								+ "\n" + Trade.of(td, +t.buySell, t.symbol, t.price, strategy).record() //
								+ "\n" + Trade.of(td, -t.buySell, t.symbol, t.price, sellPool).record()) //
						.collect(As::joined) //
				+ "\n" //
				+ "\nTOTAL BUYS_ = " + To.string(buys_) //
				+ "\nTOTAL SELLS = " + To.string(sells) //
				+ "\n" //
				+ "\nSUGGESTIONS" //
				+ "\n- check your balance" //
				+ "\n- sort the orders and get away the small ones" //
				+ "\n- get away the stocks after ex-date" //
				+ "\n- sell mamr and " + sellPool //
				+ "\n- for mamr, check actual execution using SingleAllocBackTestTest.testBackTestHkexDetails()" //
				+ "\n");

		var result = sb.toString();
		LogUtil.info(result);

		var smtp = new SmtpSslGmail();
		smtp.send(null, getClass().getName(), result);
		return true;
	}

	// moving average mean reversion
	private Result mamr(float factor) {
		var tag = "mamr";
		var nHoldDays = 8;
		var assets = cfg.queryCompanies();
		BuySellStrategy strategy = new Strategos().movingAvgMeanReverting(64, nHoldDays, .15f);

		// pre-fetch quotes
		cfg.quote(assets.map(asset -> asset.symbol).toSet());

		// identify stocks that are mean-reverting
		Map<String, Boolean> backTestBySymbol = SerializedStoreCache //
				.of(serialize.mapOfString(serialize.boolean_)) //
				.get(getClass().getSimpleName() + ".backTestBySymbol", () -> assets //
						.map2(stock -> stock.symbol, stock -> {
							try {
								var period = TimeRange.threeYears();
								DataSource ds = cfg.dataSource(stock.symbol, period).range(period).validate();
								SingleAllocBackTest backTest = SingleAllocBackTest.test(ds, strategy);
								return MathUtil.isPositive(backTest.account.cash());
							} catch (Exception ex) {
								LogUtil.warn(ex + " for " + stock);
								return false;
							}
						}) //
						.toMap());

		var period = TimeRange.daysBefore(128);
		List<Trade> trades = new ArrayList<>();

		// capture signals
		for (var asset : assets) {
			var symbol = asset.symbol;

			if (backTestBySymbol.get(symbol))
				try {
					var ds = cfg.dataSource(symbol, period).validate();
					var prices = ds.prices;
					var last = prices.length - 1;
					var latestPrice = prices[last];

					var signal = strategy.analyze(prices).get(last);
					var nShares = signal * asset.lotSize * Math.round(factor / nHoldDays / (asset.lotSize * latestPrice));
					var trade = Trade.of(nShares, symbol, latestPrice);

					if (signal != 0)
						trades.add(trade);
				} catch (Exception ex) {
					LogUtil.warn(ex.getMessage() + " in " + asset);
				}
		}

		return new Result(tag, trades);
	}

	private Result pairs(float fund, String symbol0, String symbol1) {
		return alloc("pairs/" + symbol0 + "/" + symbol1, pairs(symbol0, symbol1), fund);
	}

	public BackAllocConfiguration pairs(String symbol0, String symbol1) {
		var assets = Read.each(symbol0, symbol1).map(cfg::queryCompany).collect(As::streamlet);
		BackAllocator backAllocator = BackAllocatorOld.me.pairs(cfg, symbol0, symbol1).unleverage();
		return new BackAllocConfiguration(time -> assets, backAllocator);
	}

	// some orders caused by stupid bugs. need to sell those at suitable times.
	private Result sellForEarn(String tag) {
		Streamlet<Trade> history = cfg.queryHistory().filter(r -> String_.equals(r.strategy, tag));
		var account = Account.ofPortfolio(history);

		Map<String, Float> faceValueBySymbol = history //
				.groupBy(record -> record.symbol, rs -> (float) Read.from(rs).toDouble(Obj_Dbl.sum(Trade::amount))) //
				.toMap();

		List<Trade> trades = account //
				.portfolio() //
				.map((symbol, sell) -> {
					var targetPrice = (1d + 3 * Trade_.riskFreeInterestRate) * faceValueBySymbol.get(symbol) / sell;
					return Trade.of(-sell, symbol, (float) targetPrice);
				}) //
				.toList();

		return new Result(tag, trades);
	}

	private Result alloc(Pair<String, BackAllocConfiguration> pair, float fund) {
		var bac = pair.t1;
		return alloc(pair.t0, fund, bac.backAllocator, bac.assetsFun.apply(today));
	}

	private Result alloc(String tag, BackAllocConfiguration pair, float fund) {
		return alloc(tag, fund, pair.backAllocator, pair.assetsFun.apply(today));
	}

	private Result alloc(String tag, float fund, BackAllocator backAllocator, Streamlet<Asset> assets) {
		var period = TimeRange.daysBefore(64);
		var sim = BackAllocTester.of(cfg, period, assets, backAllocator, log).simulate(fund);
		var account0 = Account.ofPortfolio(cfg.queryHistory().filter(r -> String_.equals(r.strategy, tag)));
		var account1 = sim.account;
		Map<String, Integer> assets0 = account0.assets();
		Map<String, Integer> assets1 = account1.assets();

		Set<String> symbols = Set_.union(assets0.keySet(), assets1.keySet());
		Map<String, Float> priceBySymbol = cfg.quote(symbols);
		List<Trade> trades = Trade_.diff(Trade.NA, assets0, assets1, priceBySymbol::get).toList();

		sb.append("\nstrategy = " + tag + ", " + sim.conclusion());

		return new Result(tag, trades);
	}

}
