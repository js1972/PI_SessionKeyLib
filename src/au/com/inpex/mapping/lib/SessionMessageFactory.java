package au.com.inpex.mapping.lib;

import com.sap.aii.mapping.api.AbstractTrace;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;

public interface SessionMessageFactory {
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
		String sessionIdResponseField,
		boolean namespaceAware
	);
	
	public SessionMessage createLogoffHandler(
		TransformationInput in,
		TransformationOutput out,
		String businessComponentName,
		String channelName,
		String dcNamespace,
		String dcKey,
		AbstractTrace trace,
		String payloadXml,
		String sessionIdField,
		boolean namespaceAware
	);
}
