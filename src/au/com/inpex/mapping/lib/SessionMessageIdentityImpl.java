package au.com.inpex.mapping.lib;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import au.com.inpex.mapping.lib.exceptions.SessionKeyResponseException;

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
public class SessionMessageIdentityImpl extends SessionMessage {
	protected String sessionKeyResponseFieldName = "";
	
	
	SessionMessageIdentityImpl(
		TransformationInput in,
		TransformationOutput out,
		String businessComponentName,
		String channelName,
		AsmaParameter dc,
		AbstractTrace trace,
		String payloadXml,
		String sessionIdResponseField,
		boolean namespaceAware) {
		
		super(in, out, businessComponentName, channelName, dc, trace, payloadXml, namespaceAware);
		
		if (sessionIdResponseField == null || sessionIdResponseField.equals("")) {
			throw new SessionKeyResponseException("No session id response field name specified!");
		}
		
		this.sessionKeyResponseFieldName = sessionIdResponseField;
	}

	@Override
	protected Payload setRequestPayload() {
		InputStream is = new ByteArrayInputStream(this.loginXml.getBytes());
		Payload payload = LookupService.getXmlPayload(is);
		
		return payload;
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
	 * Perform an identity transform - copy input to output without change.
	 */
	@Override
	protected void buildMessage(String sessionId) {
		char[] buffer = new char[100];
		StringBuilder str = new StringBuilder();
		
		Reader reader = new InputStreamReader(this.messageInputstream);
		
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
		
		PrintStream ps = new PrintStream(this.messageOutputStream);
		ps.print(str.toString());
		
		logInfo("Session Id: " + sessionId);
	}

}
