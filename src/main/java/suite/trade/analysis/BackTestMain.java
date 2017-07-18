package suite.trade.analysis;

import java.nio.file.Paths;
import java.util.Set;

import suite.adt.pair.Pair;
import suite.node.io.Operator.Assoc;
import suite.os.LogUtil;
import suite.primitive.Chars;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.trade.backalloc.BackAllocConfiguration;
import suite.trade.backalloc.BackAllocConfigurations;
import suite.trade.backalloc.BackAllocConfigurations.Bacs;
import suite.trade.backalloc.BackAllocTester.Simulate;
import suite.trade.backalloc.strategy.BackAllocator_;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.util.FunUtil.Fun;
import suite.util.Object_;
import suite.util.ParseUtil;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.trade.analysis.BackTestMain
public class BackTestMain extends ExecutableProgram {

	private BackTester runner = new BackTester();
	private Configuration cfg = new ConfigurationImpl();

	private Bacs bacs = new BackAllocConfigurations(cfg, LogUtil::info).bacs;

	public static void main(String[] args) {
		Util.run(BackTestMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		Fun<Time, Streamlet<Asset>> fun = cfg::queryCompaniesByMarketCap;
		Streamlet2<String, BackAllocConfiguration> bacByTag0 = bacs.bacByName;
		Streamlet2<String, BackAllocConfiguration> bacByTag1;

		if (Boolean.TRUE)
			bacByTag1 = bacByTag0;
		else
			bacByTag1 = Streamlet2.concat( //
					Read.each2("donchian9", BackAllocator_.donchian(9).cfgUnl(fun)), //
					questoaQuella("0020.HK", "0004.HK"), //
					questoaQuella("0052.HK", "0341.HK"), //
					questoaQuella("0670.HK", "1055.HK"), //
					questoaQuella("0753.HK", "1055.HK"), //
					bacByTag0);

		// BEGIN
		// END

		Set<String> strategyNames = 0 < args.length ? Read.from(args[0].split(",")).toSet() : null;

		Streamlet<Integer> years = (1 < args.length ? Read //
				.from(args[1].split(",")).concatMap(s -> {
					Pair<String, String> pair = ParseUtil.search(s, "-", Assoc.RIGHT);
					return pair != null //
							? IntStreamlet.range(Integer.valueOf(pair.t0), Integer.valueOf(pair.t1)).map(i -> i) //
							: Read.each(Integer.valueOf(s));
				}) //
				: IntStreamlet.range(2007, Trade_.thisYear).map(i -> i));

		Streamlet2<String, Simulate> simulationByKey = bacByTag1 //
				.filterKey(strategyName -> strategyNames == null || strategyNames.contains(strategyName)) //
				.map(Pair::of) //
				.join2(years.sort(Object_::compare).map(TimeRange::ofYear)) //
				.map2((pair, period) -> pair.t0, (pair, period) -> {
					BackAllocConfiguration bac = pair.t1;
					Streamlet<Asset> assets = bac.assetsFun.apply(period.from);
					return runner.backTest(bac.backAllocator, period, assets);
				}) //
				.collect(As::streamlet2);

		String content0 = Read.bytes(Paths.get("src/main/java/suite/trade/analysis/BackTestMain.java")) //
				.collect(As::utf8decode) //
				.map(Chars::toString) //
				.collect(As.joined());

		int p0 = 0;
		int p1 = 0 <= p0 ? content0.indexOf("// BEGIN", p0) + 8 : -1;
		int p2 = 0 <= p1 ? content0.indexOf("// END", p1) : -1;
		String content1 = content0.substring(p1, p2);

		System.out.println(content1);
		System.out.println(runner.conclude(simulationByKey));

		return true;
	}

	private Streamlet2<String, BackAllocConfiguration> questoaQuella(String symbol0, String symbol1) {
		return Read //
				.<String, BackAllocConfiguration> empty2() //
				.cons(symbol0, BackAllocConfiguration.ofSingle(cfg.queryCompany(symbol0))) //
				.cons(symbol1, BackAllocConfiguration.ofSingle(cfg.queryCompany(symbol1))) //
				.cons("pair/" + symbol0 + "/" + symbol1, bacs.questoaQuella(symbol0, symbol1));
	}

}
