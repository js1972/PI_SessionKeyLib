package au.com.inpex.mapping.lib;

import com.sap.aii.mapping.lookup.LookupException;
import com.sap.aii.mapping.lookup.Payload;

public interface CommunicationChannel {
	public Payload call(Payload request) throws LookupException;
}
