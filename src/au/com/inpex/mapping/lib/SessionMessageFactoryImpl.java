package au.com.inpex.mapping.lib;

import com.sap.aii.mapping.api.AbstractTrace;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;


/**
 * Implementation of a Simple Factory for SessionMessage objects.
 * 
 * Note: The default implementation is the identity transform which only
 * logs the session key and does nothing to the payload.
 */
public class SessionMessageFactoryImpl implements SessionMessageFactory {
	private static SessionMessageFactoryImpl instance;
	
	public static SessionMessageFactoryImpl getInstance() {
		if (instance == null) {
			instance = new SessionMessageFactoryImpl();
		}
		
		return new SessionMessageFactoryImpl();
	}

	@Override
	public SessionMessage createSessionMessageHandler(
		String type,
		TransformationInput in,
		TransformationOutput out,
		String businessComponentName,
		String channelName,
		String dcNamespace,
		String dcKey,
		AbstractTrace trace,
		String payloadXml,
		String sessionIdField,
		String newSessionIdField,
		String sessionIdResponseField) {
		
		AsmaParameter dc = new AsmaParameterImpl(in, dcNamespace, dcKey);
		
		if (type.equals("SET_FIELD")) {
			return new SessionMessagePayloadImpl(
				in,
				out,
				businessComponentName,
				channelName,
				dc,
				trace,
				payloadXml,
				sessionIdField,
				sessionIdResponseField);
			
		} else if (type.equals("ADD_FIELD")) {
			return new SessionMessageAddToPayloadImpl(
				in,
				out,
				businessComponentName,
				channelName,
				dc,
				trace,
				payloadXml,
				sessionIdField,
				newSessionIdField,
				sessionIdResponseField);
			
		} else if (type.equals("SOAP_HEADER")) {
			return new SessionMessageSoapHeaderImpl(
				in,
				out,
				businessComponentName,
				channelName,
				dc,
				trace,
				payloadXml,
				newSessionIdField,
				sessionIdResponseField);
			
		} else {
			return new SessionMessageIdentityImpl(
				in,
				out,
				businessComponentName,
				channelName,
				dc,
				trace,
				payloadXml,
				newSessionIdField);
		}
	}

	public SessionMessage createLogoffHandler(
		TransformationInput in,
		TransformationOutput out,
		String businessComponentName,
		String channelName,
		String dcNamespace,
		String dcKey,
		AbstractTrace trace,
		String payloadXml,
		String sessionIdField) {
		
		AsmaParameter dc = new AsmaParameterImpl(in, dcNamespace, dcKey);
		
		return new LogoffHandlerImpl(
			in,
			out,
			businessComponentName,
			channelName,
			dc,
			trace,
			payloadXml,
			sessionIdField);
	}
}