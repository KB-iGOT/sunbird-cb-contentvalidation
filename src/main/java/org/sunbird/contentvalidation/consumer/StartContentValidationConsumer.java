package org.sunbird.contentvalidation.consumer;

import java.io.IOException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Service;
import org.sunbird.contentvalidation.model.ContentPdfValidation;
import org.sunbird.contentvalidation.repo.ContentValidationRepoServiceImpl;
import org.sunbird.contentvalidation.service.ContentValidationService;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class StartContentValidationConsumer {

	@Autowired
	ContentValidationService validationService;

	@Autowired
	ContentValidationRepoServiceImpl repoService;

	@KafkaListener(id = "id0", groupId = "startContentValidationTopic-consumer", topicPartitions = {
			@TopicPartition(topic = "${kafka.topics.incoming.rest.validation}", partitions = { "0", "1", "2", "3" }) })
	public void processMessage(ConsumerRecord<String, String> data) {

		try {
			ObjectMapper mapper = new ObjectMapper();
			ContentPdfValidation contentPdfValidation = mapper.readValue(String.valueOf(data.value()),
					ContentPdfValidation.class);
			log.info("Received Topic Request : {}", contentPdfValidation);
			repoService.insertStartData(contentPdfValidation.getContentId(), contentPdfValidation.getFileName());
			validationService.validatePdfContentV2(contentPdfValidation);
		} catch (IOException e) {
			log.error("Failed to process the ContentValidation Request. Error: ", e);
		}
	}
}
