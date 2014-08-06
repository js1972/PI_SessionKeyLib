package au.com.inpex.mapping.lib;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import com.sap.aii.mapping.api.AbstractTrace;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;
import com.sap.aii.mapping.lookup.LookupException;
import com.sap.aii.mapping.lookup.LookupService;
import com.sap.aii.mapping.lookup.Payload;


public abstract class SessionMessage {
	protected AbstractTrace logger;
	protected AsmaParameter dynConfig = null;
	protected InputStream messageInputstream;
	protected OutputStream messageOutputStream;
	protected CommunicationChannel commChannel;
	protected String loginXml = "";

	
	SessionMessage(
		TransformationInput in,
		TransformationOutput out,
		String businessComponentName,
		String channelName,
		AsmaParameter dc,
		AbstractTrace trace,
		String payloadXml) {
		
		this.logger = trace;
		this.dynConfig = dc;
		this.messageInputstream = in.getInputPayload().getInputStream();
		this.messageOutputStream = out.getOutputPayload().getOutputStream();
		this.loginXml = payloadXml;
		
		this.commChannel = new CommunicationChannelImpl(
			businessComponentName,
			channelName
		);
	}
	
	/**
	 * Template-pattern method
	 * 
	 */
	public final void process() throws LookupException {
		Payload payload = setRequestPayload();
		Payload response = callSessionKeyWebService(payload);
		String sessionId = getSessionKeyFromResponse(response);
		buildMessage(sessionId);
		
		if (sessionId != null && !sessionId.equals("")) {
			this.dynConfig.set(sessionId);
		}
	}
	
	/**
	 * Override this method in implementation class to set the payload for the 
	 * Get Session Request web service call.
	 * @return Payload
	 */
	protected Payload setRequestPayload() {
		InputStream is = new ByteArrayInputStream(this.loginXml.getBytes());
		return LookupService.getXmlPayload(is);
	}
	
	private Payload callSessionKeyWebService(Payload payload) throws LookupException {
		return this.commChannel.call(payload);
	}
	
	protected void logInfo(String msg) {
		try {
			logger.addInfo(msg);
		}
		catch (Exception e) { }
	}
	
	/**
	 * Utility method to extract an XML document as a string
	 * @param d XML document (org.w3c.dom.Document)
	 * @return Indented string representation of the XML document
	 */
	String getXmlDocAsString(Document d) {
		TransformerFactory transfac = TransformerFactory.newInstance();
		Transformer trans;
		try {
			trans = transfac.newTransformer();
		} catch (TransformerConfigurationException e) {
			return "Unable to create java TransformerFactory. Cannot display XML!";
		}
		trans.setOutputProperty(OutputKeys.METHOD, "xml");
		trans.setOutputProperty(OutputKeys.INDENT, "yes");
		trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));

		StringWriter sw = new StringWriter();
		StreamResult result = new StreamResult(sw);
		DOMSource source = new DOMSource(d.getDocumentElement());

		try {
			trans.transform(source, result);
		} catch (TransformerException e) {
			return "Unable to transform document with java XML transformer. Cannot display XML!";
		}
		return sw.toString();
	}
	
	/**
	 * Override this method to extract the session key value from the web
	 * service response.
	 * @param Payload response
	 * @return String session id value
	 */
	protected abstract String getSessionKeyFromResponse(Payload response);
	
	/**
	 * Override this method to build the output message which is sent to
	 * the adapter.
	 */
	protected abstract void buildMessage(String sessionId);
}
