package au.com.inpex.mapping.lib;

import com.sap.aii.mapping.api.DynamicConfiguration;
import com.sap.aii.mapping.api.DynamicConfigurationKey;
import com.sap.aii.mapping.api.TransformationInput;

import au.com.inpex.mapping.lib.exceptions.AsmaParameterException;

public class AsmaParameterImpl implements AsmaParameter {
	DynamicConfiguration adapterConfig;
	DynamicConfigurationKey keySessionId;

	
	public AsmaParameterImpl(TransformationInput inputHandler, String ns, String name) {
		try {
			if (ns.equals("") || name.equals("")) {
				throw new AsmaParameterException("Must provide a namespace and name for Dynamic Configurations!");
			}

			this.adapterConfig = inputHandler.getDynamicConfiguration();
			this.keySessionId = DynamicConfigurationKey.create(ns, name);
		}
		catch (Exception e) {
			throw new AsmaParameterException(e.getMessage());
		}
	}
	
	@Override
	public String get() {
		return this.adapterConfig.get(this.keySessionId);
	}

	@Override
	public void set(String value) {
		this.adapterConfig.put(this.keySessionId, value);
	}

}
