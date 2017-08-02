package suite.trade.data;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import suite.trade.Time;
import suite.util.FunUtil.Source;
import suite.util.Memoize;
import suite.util.String_;

public class HkexUtil {

	private static HongKongGovernment hkg = new HongKongGovernment();
	private static Set<DayOfWeek> weekends = new HashSet<>(Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
	private static Predicate<Time> marketOpen_ = HkexUtil::isMarketOpen_;
	private static Predicate<Time> marketClose = marketOpen_.negate();

	public static boolean isMarketOpen(Time time) {
		return isMarketOpen_(time);
	}

	public static Time getOpenTimeBefore(Time time) {
		time = until(time, -1, marketOpen_);
		time = until(time, -1, marketClose);
		return time.addSeconds(1);
	}

	public static Time getCloseTimeBefore(Time time) {
		time = until(time, -1, marketClose);
		time = until(time, -1, marketOpen_);
		return time.addSeconds(1);
	}

	public static Time getTradeTimeBefore(Time time) {
		return until(time, -1, marketOpen_);
	}

	public static Time getTradeTimeAfter(Time time) {
		return until(time, 1, marketOpen_);
	}

	public static String toStockCode(String symbol) {
		return "" + Integer.parseInt(symbol.replace(".HK", ""));
	}

	public static String toSymbol(String stockCode) {
		return String_.right("0000" + stockCode.trim(), -4) + ".HK";
	}

	private static Time until(Time start, int dir, Predicate<Time> pred) {
		Time time = start;
		if (!pred.test(time)) {
			time = time.thisSecond().addSeconds(dir < 0 ? 0 : 1);
			Time time1 = null;
			for (int d : new int[] { 14400, 3600, 300, 30, 5, 1, })
				while (!pred.test(time1 = time.addSeconds(dir * d)))
					time = time1;
			time = time1;
		}
		return time;
	}

	private static boolean isMarketOpen_(Time time) {
		List<Time> phs = publicHolidays.source();
		int hhmm = time.hhmm();
		return !phs.contains(time.date()) //
				&& !weekends.contains(time.dow()) //
				&& 930 <= hhmm && hhmm < 1630;
	}

	private static Source<List<Time>> publicHolidays = Memoize.source(hkg::queryPublicHolidays);

}
