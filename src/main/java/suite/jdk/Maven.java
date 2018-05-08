package suite.jdk;

import suite.http.HttpUtil;
import suite.util.Rethrow;
import suite.util.To;
import suite.util.XmlUtil;

public class Maven {

	public String getUrl(String groupId, String artifactId, String version) {
		return getUrl("http://repo.maven.apache.org/maven2/", groupId, artifactId, version);
	}

	public String getLatestUrl(String m2repo, String groupId, String artifactId) {
		var url = m2repo + groupId.replace('.', '/') + "/" + artifactId + "/maven-metadata.xml";

		var version = Rethrow.ex(() -> new XmlUtil() //
				.read(HttpUtil.get(url).out.collect(To::inputStream)) //
				.children("metadata") //
				.uniqueResult() //
				.children("versioning") //
				.uniqueResult() //
				.children("latest") //
				.uniqueResult() //
				.text());

		return getUrl(m2repo, groupId, artifactId, version);
	}

	private String getUrl(String m2repo, String groupId, String artifactId, String version) {
		return m2repo + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".pom";
	}

}
