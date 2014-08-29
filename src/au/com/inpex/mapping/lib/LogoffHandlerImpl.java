package au.com.inpex.mapping.lib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

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

//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//
//import org.w3c.dom.Document;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;
//
//import au.com.inpex.mapping.exceptions.SessionKeyResponseException;

import au.com.inpex.mapping.lib.exceptions.BuildMessagePayloadException;

import com.sap.aii.mapping.api.AbstractTrace;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;
import com.sap.aii.mapping.lookup.LookupService;
import com.sap.aii.mapping.lookup.Payload;

/**
 * Implement a SessionMessage handler (based on the Template pattern).
 * Uses an identity transform (copy input to output) for the message
 * payload.
 *
 */
public class LogoffHandlerImpl extends SessionMessage {
	private AsmaParameter dynConfig = null;
	protected String loginXml = "";
	protected String sessionIdFieldName = "";
	
	
	LogoffHandlerImpl(
		TransformationInput in,
		TransformationOutput out,
		String businessComponentName,
		String channelName,
		AsmaParameter dc,
		AbstractTrace trace,
		String payloadXml,
		String sessionIdField,
		boolean namespaceAware) {
		
		super(in, out, businessComponentName, channelName, dc, trace, payloadXml, namespaceAware);
		this.dynConfig = dc;
		this.loginXml = payloadXml;
		this.sessionIdFieldName = sessionIdField;
	}

	@Override
	protected Payload setRequestPayload() {
		String sessionId = dynConfig.get();
		ByteArrayOutputStream requestPayloadOutputStream = new ByteArrayOutputStream();
		
		
		//Put the session-id into the xml payload
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		docFactory.setNamespaceAware(this.namespaceAware);
		
		try {
			DocumentBuilder builder = docFactory.newDocumentBuilder();
			Document document = builder.parse(new ByteArrayInputStream(loginXml.getBytes()));
			NodeList nodes = document.getElementsByTagNameNS("*", this.sessionIdFieldName);
			if (nodes.getLength() == 0) {
				nodes = document.getElementsByTagName(this.sessionIdFieldName);
			}
			
			if (nodes.getLength() == 0) {
				logInfo(this.sessionIdFieldName + " element not found!");
			} else {
				Node node = nodes.item(0);
				node.setTextContent(sessionId);
			}
			
			DOMSource source = new DOMSource(document);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			
			StreamResult streamResult = new StreamResult(requestPayloadOutputStream);
			transformer.transform(source, streamResult);
			
			
		} catch (Exception e) {
			throw new BuildMessagePayloadException(e.getMessage());
		}
		
		
		InputStream is = new ByteArrayInputStream(requestPayloadOutputStream.toByteArray());
		Payload payload = LookupService.getXmlPayload(is);
		
		return payload;
	}
	
	@Override
	protected String getSessionKeyFromResponse(Payload response) {
		return null;
	}

	/**
	 * Perform an identity transform - copy input to output without change.
	 */
	@Override
	protected void buildMessage(String sessionId) {
		char[] buffer = new char[100];
		StringBuilder str = new StringBuilder();
		
		Reader reader = new InputStreamReader(messageInputstream);
		
		try {
			for (;;) {
				int read_size = reader.read(buffer, 0, buffer.length);
				if (read_size < 0) {
					break;
				}
				str.append(buffer, 0, read_size);
			}
		}
		catch (IOException e) {
		}
		finally {
			try {
				reader.close();
			}
			catch (IOException e) {
			}
		}
		
		PrintStream ps = new PrintStream(messageOutputStream);
		ps.print(str.toString());
	}
}
