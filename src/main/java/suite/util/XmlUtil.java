package suite.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import suite.Constants;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;

public class XmlUtil {

	private DocumentBuilder documentBuilder;
	private DOMImplementationLS di;
	private LSSerializer lss;

	public interface XmlNode {
		public int nodeType();

		public String namespaceUri();

		public String localName();

		public String text();

		public Streamlet<XmlNode> children();

		public Streamlet<XmlNode> children(String tagName);
	}

	public XmlUtil() {
		try {
			documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			di = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS");
			lss = di.createLSSerializer();
			lss.getDomConfig().setParameter("format-pretty-print", true);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public String format(String xml) throws SAXException {
		try (InputStream is = new ByteArrayInputStream(xml.getBytes(Constants.charset)); Writer writer = new StringWriter()) {
			LSOutput lso = di.createLSOutput();
			lso.setEncoding(Constants.charset.name());
			lso.setCharacterStream(writer);

			lss.write(documentBuilder.parse(is), lso);
			return writer.toString();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public XmlNode read(InputStream is) throws SAXException {
		return node(Rethrow.ex(() -> {
			Document document = documentBuilder.parse(is);
			document.normalize();
			return document;
		}));
	}

	private XmlNode node(Node n) {
		return new XmlNode() {
			public int nodeType() {
				return n.getNodeType();
			}

			public String namespaceUri() {
				return n.getNamespaceURI();
			}

			public String localName() {
				return n.getLocalName();
			}

			public String text() {
				return n.getTextContent();
			}

			public Streamlet<XmlNode> children() {
				return xmlNodes(n.getChildNodes());
			}

			public Streamlet<XmlNode> children(String tagName) {
				return xmlNodes(((Element) n).getElementsByTagName(tagName));
			}

			private Streamlet<XmlNode> xmlNodes(NodeList nodeList) {
				return Read.from(() -> new Source<XmlNode>() {
					private int i = 0;

					public XmlNode source() {
						return i < nodeList.getLength() ? node(nodeList.item(i)) : null;
					}
				});
			}
		};
	}

}
