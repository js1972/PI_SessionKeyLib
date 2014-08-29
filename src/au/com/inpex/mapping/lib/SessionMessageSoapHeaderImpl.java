package au.com.inpex.mapping.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import au.com.inpex.mapping.lib.exceptions.BuildMessagePayloadException;
import au.com.inpex.mapping.lib.exceptions.SessionKeyResponseException;

import com.sap.aii.mapping.api.AbstractTrace;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;
import com.sap.aii.mapping.lookup.Payload;


/**
 * Copy the input to the output and wrap in a soap header with the session key.
 * The soap header fields are defined by variables node and fieldName. These are
 * extracted from PI mapping parameters and are used as follows:
 * 
 * <node><fieldName>---sessionkey---</fieldName></node>
 * 
 * This scenario requires the receiver adapter to be in nosoap mode to allow us
 * manually construct the soap header. You cannot alter the soap header
 * otherwise.
 * 
 */
public class SessionMessageSoapHeaderImpl extends SessionMessage {
	private String prefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
		+ "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:pi:session:key\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
		+ "<soapenv:Header>" + "<urn:SessionHeader>" + "<urn:sessionId>";
	private String suffix = "</urn:sessionId></urn:SessionHeader></soapenv:Header><soapenv:Body>";
	private String envelope = "</soapenv:Body></soapenv:Envelope>";
	
	private String nodeName = "";
	protected String newSessionIdFieldName = "";
	protected String sessionKeyResponseFieldName = "";
	
	
	SessionMessageSoapHeaderImpl(
		TransformationInput in,
		TransformationOutput out,
		String businessComponentName,
		String channelName,
		AsmaParameter dc,
		AbstractTrace trace,
		String payloadXml,
		String newSessionIdField,
		String sessionIdResponseField,
		boolean namespaceAware) {
		
		super(in, out, businessComponentName, channelName, dc, trace, payloadXml, namespaceAware);
		
		try {
			this.nodeName = newSessionIdField.split("/")[0];
			this.newSessionIdFieldName = newSessionIdField.split("/")[1];
			
		} catch(ArrayIndexOutOfBoundsException e) {
			throw new RuntimeException("For SOAP_HEADER processing the newSessionIdField must be passed in the form: 'node/field'! You provided: " + newSessionIdField + ". " + e.getMessage());
		} 
		
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
			Document document = builder.parse(is);
			
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
	 * Copy input to output then wrap it in a soap envelope with
	 * the session Id in the soap header.
	 * Note: requires comm.channel to be in 'nosoap' mode.
	 * Note: need to use a transformer so that we can get rid of the xml tag.
	 */
	@Override
	protected void buildMessage(String sessionId) {		
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			docFactory.setNamespaceAware(this.namespaceAware);
			
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			
			try {
				DocumentBuilder builder = docFactory.newDocumentBuilder();
				Document document = builder.parse(this.messageInputstream);
				
				DOMSource source = new DOMSource(document);
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				
				StreamResult streamResult = new StreamResult(os);
				transformer.transform(source, streamResult);
				
			} catch (Exception e) {
				throw new BuildMessagePayloadException("Session Id: '" + sessionId + "' - " + e.getMessage());
			}
			
			//Wrap payload in soap envelope/header
			prefix.replaceAll("SessionHeader", this.nodeName);
			prefix.replaceAll("sessionId", this.newSessionIdFieldName);
			
			this.messageOutputStream.write(prefix.getBytes());
			this.messageOutputStream.write(sessionId.getBytes());
			this.messageOutputStream.write(suffix.getBytes());
			this.messageOutputStream.write(os.toByteArray());
			this.messageOutputStream.write(envelope.getBytes());
			
		} catch (IOException e) {
			throw new BuildMessagePayloadException("Session Id: '" + sessionId + "' - " + e.getMessage());
		}
	}
}
