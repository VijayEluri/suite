package suite.trade.backalloc;

import suite.adt.pair.Pair;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.analysis.Factor;
import suite.trade.backalloc.strategy.BackAllocatorGeneral;
import suite.trade.backalloc.strategy.BackAllocatorMech;
import suite.trade.backalloc.strategy.BackAllocatorOld;
import suite.trade.backalloc.strategy.BackAllocator_;
import suite.trade.backalloc.strategy.Pmamr2BackAllocator;
import suite.trade.backalloc.strategy.PmamrBackAllocator;
import suite.trade.backalloc.strategy.ReverseCorrelateBackAllocator;
import suite.trade.data.Configuration;
import suite.util.FunUtil.Fun;

public class BackAllocConfigurations {

	private Configuration cfg;
	private Fun<Time, Streamlet<Asset>> fun;

	public class Bacs {
		private Fun<Time, Streamlet<Asset>> fun_hsi = time -> Read.each(Asset.hsi);

		private BackAllocatorGeneral baGen = BackAllocatorGeneral.me;
		private BackAllocatorMech baMech = BackAllocatorMech.me;
		private BackAllocatorOld baOld = BackAllocatorOld.me;

		private BackAllocator ba_bbHold = baGen.bb_.filterByIndex(cfg).holdExtend(8);
		private BackAllocator ba_donHold = baGen.donHold;
		private BackAllocator ba_facoil = Factor.ofCrudeOil(cfg).backAllocator().longOnly().pick(3).even();
		private BackAllocator ba_pmamr = new PmamrBackAllocator().backAllocator();
		private BackAllocator ba_pmamr2 = new Pmamr2BackAllocator().backAllocator();
		private BackAllocator ba_pmmmr = baOld.movingMedianMeanRevn().holdExtend(9);
		private BackAllocator ba_revco = ReverseCorrelateBackAllocator.of();

		public final BackAllocConfiguration bac_sell = baGen.cash.cfgUnl(fun);

		public final Pair<String, BackAllocConfiguration> pair_bb = pair("bb", ba_bbHold);
		public final Pair<String, BackAllocConfiguration> pair_ema = pair("ema", baGen.ema);
		public final Pair<String, BackAllocConfiguration> pair_donchian = pair("donchian", ba_donHold);
		public final Pair<String, BackAllocConfiguration> pair_pmamr = pair("pmamr", ba_pmamr);
		public final Pair<String, BackAllocConfiguration> pair_pmamr2 = pair("pmamr2", ba_pmamr2);
		public final Pair<String, BackAllocConfiguration> pair_pmmmr = pair("pmmmr", ba_pmmmr);
		public final Pair<String, BackAllocConfiguration> pair_revco = pair("revco", ba_revco);
		public final Pair<String, BackAllocConfiguration> pair_tma = pair("tma", baGen.tma);

		private Streamlet2<String, BackAllocator> bas_ = baGen.baByName;
		private Streamlet2<String, BackAllocator> bas_mech = baMech.baByName.map2((n, ba) -> "me." + n, (n, ba) -> ba);

		private Streamlet2<String, BackAllocConfiguration> bacs_ = Streamlet2 //
				.concat(bas_, bas_mech) //
				.mapValue(ba -> ba.cfgUnl(fun));

		private Streamlet2<String, BackAllocConfiguration> bacByName0 = Read //
				.<String, BackAllocConfiguration> empty2() //
				.cons("hsi", BackAllocConfiguration.ofSingle(Asset.hsi)) //
				.cons("hsi.ppr", baGen.pprHsi.cfgUnl(fun_hsi)) //
				.cons(pair_bb) //
				.cons("bbslope", baOld.bbSlope().cfgUnl(fun)) //
				.cons("facoil", ba_facoil.cfgUnl(fun)) //
				.cons("january", BackAllocator_.ofSingle(Asset.hsiSymbol).january().cfgUnl(fun_hsi)) //
				.cons("mix", BackAllocator_.sum(ba_bbHold, ba_donHold).cfgUnl(fun)) //
				.cons(pair_pmamr) //
				.cons(pair_pmamr2) //
				.cons(pair_pmmmr) //
				.cons(pair_revco) //
				.cons("revdd", baOld.revDrawdown().holdExtend(40).cfgUnl(fun)) //
				.cons("sellInMay", BackAllocator_.ofSingle(Asset.hsiSymbol).sellInMay().cfgUnl(fun_hsi));

		public final Streamlet2<String, BackAllocConfiguration> bacByName = Streamlet2 //
				.concat(bacs_, bacByName0);

		private Pair<String, BackAllocConfiguration> pair(String tag, BackAllocator ba) {
			return Pair.of(tag, ba.cfgUnl(fun));
		}
	}

	public BackAllocConfigurations(Configuration cfg) {
		this(cfg, cfg::queryCompaniesByMarketCap);
	}

	public BackAllocConfigurations(Configuration cfg, Fun<Time, Streamlet<Asset>> fun) {
		this.cfg = cfg;
		this.fun = fun;
	}

	public Bacs bacs() {
		return new Bacs();
	}

}
