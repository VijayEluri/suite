package suite.trade.data;

import static java.util.Map.entry;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import suite.node.util.Singleton;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.Longs.LongsBuilder;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.trade.Time;
import suite.util.ParseUtil;
import suite.util.String_;

public class HongKongGovernment {

	public List<Time> queryPublicHolidays() {
		var yyyymmdd = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ENGLISH);

		return Singleton.me.storeCache //
				.http("http://www.1823.gov.hk/common/ical/gc/en.ics") //
				.collect(As::lines) //
				.map(line -> line.split(":")) //
				.filter(array -> 2 <= array.length) //
				.split(array -> String_.equals(array[0], "BEGIN") && String_.equals(array[1], "VEVENT")) //
				.map(arrays -> Read //
						.from(arrays) //
						.map2(array -> array[0], array -> array[1]) //
						.toMap()) //
				.map2(map -> map.get("DTSTART;VALUE=DATE"), map -> map.get("SUMMARY")) //
				.filterKey(s -> s != null) //
				.keys() //
				.map(s -> Time.of(LocalDate.parse(s, yyyymmdd).atStartOfDay())) //
				.toList();
	}

	public Map<String, DataSource> queryWeather() {
		var t0 = Time.of(2000, 1, 1).epochSec();
		var tx = Time.today().epochSec();

		var ts = new LongsBuilder();
		var fs0 = new FloatsBuilder();
		var fs1 = new FloatsBuilder();

		for (var t = t0; t < tx; t += 86400l) {
			var time = Time.ofEpochSec(t);

			var html = Singleton.me.storeCache //
					.http("http://www.hko.gov.hk/cgi-bin/hko/yes.pl" //
							+ "?year=" + time.year() //
							+ "&month=" + time.month() //
							+ "&day=" + time.dayOfMonth() //
							+ "&language=english&B1=Confirm#") //
					.collect(As::string);

			var data = ParseUtil.fit(html, "<pre>", "</pre>").t1;

			ts.append(t);
			fs0.append(getFloatValue(data, "Maximum Air Temperature", "C"));
			fs1.append(getFloatValue(data, "Rainfall", "mm"));
		}

		var ts_ = ts.toLongs().toArray();

		return Map.ofEntries( //
				entry("hko.TEMP", DataSource.of(ts_, fs0.toFloats().toArray())), //
				entry("hko.RAIN", DataSource.of(ts_, fs1.toFloats().toArray())));
	}

	private float getFloatValue(String data, String s0, String s1) {
		return Float.parseFloat(ParseUtil.fit(data, s0, s1).t1.replace(" ", ""));
	}

}
