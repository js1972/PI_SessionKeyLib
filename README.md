PI_SessionKeyLib
================

Class library which provides the functionality to call a web service to get a session id, to be used for subsequent authentication of a web service call in PI.

### Overview
This class library enables a PI java program to get a session key from an application system (for logon); subsequently use it in a web service call; then use this library to logoff (if requried).

Factory methods are provided to create an object that will perform the work of getting the session key, or logging off and handling the placement of that session key in the web service message.

### Example implementations provided
 - SessionMessageIdentityImpl - Get a session id and then just copy the input payload to the output and log the determined session key as a dynamic configuration attribute.
 - SessionMessagePayloadImpl - Get a session id and then set it into a specified payload field.
 - SessionMessageAddToPayloadImpl - Get a session id and then add it as a new payload field.
 - SessionMessageSoapHeaderImpl - Get a session id and then add it as a soap header field.
 - LogoffHandlerImpl - Logoff with the previously determined session id (only used if requried).


### Notes
 - The SOAP Header implementation requires the PI SOAP channel to be run in NOSOAP mode.
 -  All scenario's write the session key to a Dynamic Configuration attribute. This can be used if you wish to make use of the session key in a channel (axis) to set a http cookie for example.
 - For a detailed walk-through of this solution on Evernote: https://www.evernote.com/shard/s4/sh/407418fc-0dd8-4b89-9f55-308d9820093c/f821979213af65d7dc7426fc2b708edd

### Factory method parameters

Example call:
```
String loginXml = "<SessionKeyRequest xmlns=\"urn:pi:session:key\"><data>session key request from add-to-payload handler</data></SessionKeyRequest>";
			
			au.com.inpex.mapping.lib.SessionMessageFactoryImpl smf = au.com.inpex.mapping.lib.SessionMessageFactoryImpl.getInstance();
			au.com.inpex.mapping.lib.SessionMessage sessionMessageHandler = smf.createSessionMessageHandler(
				mappingType,
				inputHandler,
				outputHandler,
				businessComponentName,
				channelName,
				dcNamespace,
				dcKey,
				getTrace(),
				loginXml,
				sessionIdField,
				newSessionIdField,
				sessionIdResponseField);
			
			sessionMessageHandler.process();
```

mappingType - ADD\_FIELD, SET\_FIELD, SOAP\_HEADER. All other values create the identity instance.
inputHandler - This is the TransformationInput object as provided to the PI mapping program.
outputHandler - This is the TransformationOutput object as provided to the PI mapping program.
businessComponentName - this is the business component that holds the communication channels in PI used for logon and logoff service calls.
channelName - This is the communication channel name used for the logon service call to get the session id.
dcNamespace - This is the namespace to use for the created dynamic configuration attribute.
dcKey - This is the name of the dynamic configuration attribute to create for the session id.
getTrace() - This is just the trace object as provided to the PI java mapping.
loginXxml - This is a string containing the XML payload to use for the logon call.
sessionIdField - This is the name of the fielt where the session id will be placed for the SET\_FIELD type or alternatively it is the name of an adjacent (sibling) field where you want the NEW field added in the ADD\_FIELD type.
newSessionIdField - This is the name of the new fields to create for the ADD\_FIELD type and it is to contain and node and value string when using the SOAP_HEADER type like such: "node/element".
sessionIdResponseField - This is the field to search for in the logon response where the session id will be returned.

### Contributions
Please ensure that any code changes or additional template pattern implementations are covered by unit tests. This project uses JUnit and Mockito.
