package suite.nntp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.adt.Pair;
import suite.os.SocketUtil;
import suite.util.CommandUtil;
import suite.util.Util;

public class NntpServer {

	private enum NntpCommand {
		ARTICLE, BODY, GROUP, HEAD, LIST, LISTGROUP, NEWNEWS, POST
	}

	private Nntp nntp;

	public static void main(String args[]) throws IOException {
		new NntpServer().run();
	}

	private void run() throws IOException {
		new SocketUtil().listenIo(119, (sis, sos) -> new Server().serve(sis, sos));
	}

	private class Server {
		private void serve(InputStream sis, OutputStream sos) throws IOException {
			try (OutputStreamWriter osw = new OutputStreamWriter(sos); PrintWriter pw = new PrintWriter(osw)) {
				String currentGroupId = null;
				Map<String, String> article;
				String line;

				while (!(line = Util.readLine(sis)).isEmpty()) {
					Pair<NntpCommand, String> pair = new CommandUtil<>(NntpCommand.values()).recognize(line.toUpperCase());
					String options = pair.t1;

					switch (pair.t0) {
					case ARTICLE:
						article = nntp.getArticle(currentGroupId, options);
						if (article != null) {
							pw.println("220 Okay");
							printHead(pw, article);
							pw.println();
							pw.println(article.get(Nntp.contentKey));
							pw.println(".");
						} else
							pw.println("423 Error");
						break;
					case BODY:
						article = nntp.getArticle(currentGroupId, options);
						if (article != null) {
							pw.println("222 Okay");
							pw.println(article.get(Nntp.contentKey));
							pw.println(".");
						} else
							pw.println("423 Error");
						break;
					case GROUP:
						currentGroupId = options;
						break;
					case HEAD:
						article = nntp.getArticle(currentGroupId, options);
						if (article != null) {
							pw.println("221 Okay");
							printHead(pw, article);
							pw.println(".");
						} else
							pw.println("423 Bad article number");
						break;
					case LIST:
						if (Util.stringEquals(options, "ACTIVE")) {
							pw.println("215 Okay");
							for (String groupId : nntp.listGroupIds()) {
								Pair<String, String> articleIdRange = nntp.getArticleIdRange(groupId);
								pw.println(groupId + " " + articleIdRange.t0 + " " + articleIdRange.t1 + " y");
							}
							pw.println(".");
						} else if (Util.stringEquals(options, "NEWSGROUPS")) {
							pw.println("215 Okay");
							for (String group : nntp.listGroupIds())
								pw.println(group + " " + group);
							pw.println(".");
						} else
							throw new RuntimeException("Unrecognized LIST command " + line);
						break;
					case LISTGROUP:
						pw.println("211 Okay");
						for (String articleId : nntp.listArticleIds(currentGroupId, 0))
							pw.println(articleId);
						pw.println(".");
						break;
					case NEWNEWS:
						break;
					case POST:
						pw.println("340 Okay");
						int size = 0;
						List<String> lines = new ArrayList<>();
						while (!Util.stringEquals(line = Util.readLine(sis), ".") && size < 1048576) {
							lines.add(line);
							size += line.length();
						}
						article = new HashMap<>();
						int pos = 0;
						while (!(line = lines.get(pos++)).isEmpty()) {
							Pair<String, String> lr = Util.split2(line, ":");
							article.put(lr.t0, lr.t1);
						}
						StringBuilder sb = new StringBuilder();
						while (pos < lines.size())
							sb.append(lines.get(pos++));
						article.put(Nntp.contentKey, sb.toString());
						nntp.createArticle(currentGroupId, article);
						pw.println("240 Okay");
						break;
					default:
						throw new RuntimeException("Unrecognized command " + line);
					}
				}
			}
		}

		private void printHead(PrintWriter pw, Map<String, String> article) {
			for (Entry<String, String> entry : article.entrySet()) {
				String key = entry.getKey();
				if (!Util.stringEquals(key, Nntp.contentKey))
					pw.println(key + ": " + entry.getValue());
			}
		}
	}

}
