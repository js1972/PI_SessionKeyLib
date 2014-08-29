package au.com.inpex.mapping.lib;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import au.com.inpex.mapping.lib.exceptions.BuildMessagePayloadException;
import au.com.inpex.mapping.lib.exceptions.SessionKeyResponseException;

import com.sap.aii.mapping.api.AbstractTrace;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;
import com.sap.aii.mapping.lookup.Payload;

public class SessionMessageAddToPayloadImpl extends SessionMessage {
	protected String newSessionIdFieldName = "";
	protected String sessionIdFieldName = "";
	protected String sessionKeyResponseFieldName = "";
	
	SessionMessageAddToPayloadImpl(
		TransformationInput in,
		TransformationOutput out,
		String businessComponentName,
		String channelName,
		AsmaParameter dc,
		AbstractTrace trace,
		String payloadXml,
		String sessionIdField,
		String newSessionIdField,
		String sessionIdResponseField,
		boolean namespaceAware) {
		
		super(in, out, businessComponentName, channelName, dc, trace, payloadXml, namespaceAware);
		
		if (sessionIdField == null || sessionIdField.equals("")) {
			throw new BuildMessagePayloadException("No session id field name specified!");
		}
		if (newSessionIdField == null || newSessionIdField.equals("")) {
			throw new BuildMessagePayloadException("No *new* session id field name specified!");
		}
		if (sessionIdResponseField == null || sessionIdResponseField.equals("")) {
			throw new SessionKeyResponseException("No session id response field name specified!");
		}
		
		this.sessionIdFieldName = sessionIdField;
		this.newSessionIdFieldName = newSessionIdField;
		this.sessionKeyResponseFieldName = sessionIdResponseField;
	}
	
	@Override
	protected String getSessionKeyFromResponse(Payload response) {
		String sessionId = null;
		
		InputStream is = response.getContent();
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		docFactory.setNamespaceAware(this.namespaceAware);
		
		try {
			DocumentBuilder builder = docFactory.newDocumentBuilder();
			Document document;
			document = builder.parse(is);

			NodeList nodes = document.getElementsByTagNameNS("*", this.sessionKeyResponseFieldName);
			if (nodes.getLength() == 0) {
				nodes = document.getElementsByTagName(this.sessionKeyResponseFieldName);
				if (nodes.getLength() == 0) {
					throw new SessionKeyResponseException(
						"Unable to find field: '" + 
						this.sessionKeyResponseFieldName + 
						"' in response! Web Service Response:\n\n" +
						getXmlDocAsString(document)
					);
				}
			}
			
			Node node = nodes.item(0);
			
			if (node != null) {
				node = node.getFirstChild();
				if (node != null) {
					sessionId = node.getNodeValue();
				}
			}
			
		} catch (Exception e) {
			throw new SessionKeyResponseException(e.getMessage());
		}
		
		return sessionId;
	}

	/**
	 * Copy input to output and add a new node for the session Id.
	 */
	@Override
	protected void buildMessage(String sessionId) {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		docFactory.setNamespaceAware(this.namespaceAware);
		
		try {
			DocumentBuilder builder = docFactory.newDocumentBuilder();
			Document document = builder.parse(this.messageInputstream);
			NodeList nodes = document.getElementsByTagNameNS("*", this.sessionIdFieldName);
			if (nodes.getLength() == 0) {
				nodes = document.getElementsByTagName(this.sessionIdFieldName);
			}
			
			Element sessionIdElement = document.createElement(this.newSessionIdFieldName);
			sessionIdElement.appendChild(document.createTextNode(sessionId));
			
			if (nodes.getLength() == 0) {
				logInfo(this.sessionIdFieldName + " element not found!");
			} else {
				Node node = nodes.item(0);
				Node parent = node.getParentNode();
				parent.appendChild(sessionIdElement);
			}
			
			DOMSource source = new DOMSource(document);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			
			StreamResult streamResult = new StreamResult(this.messageOutputStream);
			transformer.transform(source, streamResult);
			
		} catch (Exception e) {
			throw new BuildMessagePayloadException(e.getMessage()); 
		}
	}
}
