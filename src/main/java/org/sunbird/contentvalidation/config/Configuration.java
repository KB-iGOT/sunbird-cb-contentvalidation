package org.sunbird.contentvalidation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
public class Configuration {
	@Value("${notify.service.host}")
	private String notifyServiceHost;

	@Value("${notify.service.path}")
	private String notifyServicePath;

	@Value("${profanity.service.host}")
	private String profanityServiceHost;

	@Value("${profanity.service.path}")
	private String profanityServicePath;

	@Value("${lexcore.service.host}")
	private String lexCoreServiceHost;

	@Value("${lexcore.hierarchy.searchpath}")
	private String heirarchySearchPath;

	@Value("${content.service.host}")
	private String contentServiceHost;
	
	@Value("${kafka.topics.incoming.rest.validation}")
	private String startContentValidationTopic;

	@Value("${content-service-host}")
	private String contentHost;

	@Value("${content-hierarchy-endpoint}")
	private String hierarchyEndPoint;

	@Value("${profanity.image.service.host}")
	private String profanityImageServiceHost;

	@Value("${profanity.image.service.path}")
	private String profanityImageServicePath;

	@Value("${cloud.container.name}")
	private String cloudContainerName;

	@Value("${cloud.storage.type.name}")
	private String cloudStorageTypeName;

	@Value("${cloud.storage.key}")
	private String cloudStorageKey;

	@Value("${cloud.storage.secret}")
	private String cloudStorageSecret;

	@Value("${cloud.storage.endpoint}")
	private String cloudStorageEndpoint;
}
