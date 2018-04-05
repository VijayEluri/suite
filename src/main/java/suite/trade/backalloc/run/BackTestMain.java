package suite.trade.backalloc.run;

import java.nio.file.Paths;

import suite.adt.pair.Pair;
import suite.node.io.Operator.Assoc;
import suite.parser.Wildcard;
import suite.primitive.Chars;
import suite.primitive.Ints_;
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
import suite.trade.backalloc.BackAllocTester.Simulate;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.util.FunUtil.Fun;
import suite.util.Object_;
import suite.util.ParseUtil;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.trade.bcakalloc.run.BackTestMain
public class BackTestMain extends ExecutableProgram {

	private BackTester runner = new BackTester();
	private Configuration cfg = new ConfigurationImpl();

	public static void main(String[] args) {
		RunUtil.run(BackTestMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		// BEGIN
		// END

		var arg0 = 0 < args.length ? args[0] : "";
		var arg1 = 1 < args.length ? args[1] : "";
		var arg2 = 2 < args.length ? args[2] : "";

		Streamlet<String> strategyMatches = !arg0.isEmpty() ? Read.from(arg0.split(",")) : null;

		var years = !arg1.isEmpty() ? Read //
				.from(arg1.split(",")) //
				.concatMap(s -> {
					var pair = ParseUtil.search(s, "-", Assoc.RIGHT);
					return pair != null //
							? Ints_.range(Integer.valueOf(pair.t0), Integer.valueOf(pair.t1)).map(i -> i) //
							: Read.each(Integer.valueOf(s));
				}) //
				: Ints_.range(2007, Trade_.thisYear).map(i -> i);

		Fun<Time, Streamlet<Asset>> fun = !arg2.isEmpty() //
				? time -> Read.from(arg2.split(",")).map(cfg::queryCompany).collect(As::streamlet) //
				: cfg::queryCompaniesByMarketCap;

		BackAllocConfigurations bac_ = new BackAllocConfigurations(cfg, fun);
		Streamlet2<String, BackAllocConfiguration> bacByTag = bac_.bacs().bacByName;

		Streamlet2<String, Simulate> simulationByKey = bacByTag //
				.filterKey(n -> strategyMatches == null || strategyMatches.isAny(sm -> Wildcard.match(sm, n) != null)) //
				.map(Pair::of) //
				.join2(years.sort(Object_::compare).map(TimeRange::ofYear)) //
				.map2((pair, period) -> pair.t0, (pair, period) -> {
					var bac = pair.t1;
					var assets = bac.assetsFun.apply(period.from);
					return runner.backTest(bac.backAllocator, period, assets);
				}) //
				.collect(As::streamlet2);

		var content0 = Read //
				.bytes(Paths.get("src/main/java/" + getClass().getName().replace('.', '/') + ".java")) //
				.collect(As::utf8decode) //
				.map(Chars::toString) //
				.collect(As::joined);

		String content1 = ParseUtil.fit(content0, "// BEGIN", "// END")[1];

		System.out.println(content1);
		System.out.println(runner.conclude(simulationByKey));

		return true;
	}

}
