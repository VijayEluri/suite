package suite.trade.data;

import suite.Constants;
import suite.node.util.Singleton;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.streamlet.As;
import suite.streamlet.Streamlet;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.util.String_;

public class Quandl {

	public DataSource dataSourceCsv(String symbol, TimeRange period) {
		String[] m = Constants.secrets("quandl .0");
		long usMarketClose = 16l;
		long ph0 = period.to.epochSec() - (usMarketClose + 4) * 24 * 3600;
		long ph1 = ph0 - (ph0 % 86400l);

		String urlString = "https://www.quandl.com/api/v1/datasets/CHRIS/CME_CL1.csv" //
				+ "?ph=" + ph1 //
				+ (m != null ? "&api_key=" + m[0] : "");

		// Date, Open, High, Low, Last, Change, Settle, Volume, Previous Day
		// Open Interest
		Streamlet<String[]> arrays = Singleton.me //
				.storeCache //
				.http(urlString) //
				.collect(As::csv) //
				.skip(1) //
				.sort((a0, a1) -> String_.compare(a0[0], a1[0])) //
				.collect(As::streamlet);

		long[] ts = arrays.collect(Obj_Lng.lift(array -> Time.of(array[0] + " 18:00:00").epochSec(-4))).toArray();
		float[] opens = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[1]))).toArray();
		float[] settles = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[6]))).toArray();
		float[] lows = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[3]))).toArray();
		float[] highs = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[2]))).toArray();

		return DataSource.ofOhlc(ts, opens, settles, lows, highs).range(period);
	}

}