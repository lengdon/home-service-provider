package com.moran.home.service.provider.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

import org.apache.activemq.command.ActiveMQObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

import com.moran.home.service.constants.HsConstants;
import com.moran.home.service.entity.master.CityTownVillage;
import com.moran.home.service.entity.provider.Address;
import com.moran.home.service.entity.provider.ServiceProvider;
import com.moran.home.service.repositories.ProviderAddressRepository;
import com.moran.home.service.repositories.ProviderCityRepository;
import com.moran.home.service.repositories.ServiceProviderRepository;

@Component
public class RegistrationListener {
	private static final Logger logger = LoggerFactory.getLogger(RegistrationListener.class);

	@Autowired
	private JmsMessagingTemplate jmsTemplate;

	@Autowired
	private ServiceProviderRepository serviceProviderRepo;

	@Autowired
	private ProviderAddressRepository addressRepo;

	@Autowired
	private ProviderCityRepository cityRepo;

	@JmsListener(destination = HsConstants.PROVIDER_REQUEST_ACCEPT_QUEUE)
	public void registrationRequestListener(ActiveMQObjectMessage message) {
		logger.debug("Received Message from Queue");
		try {
			ServiceProvider serviceProvider = (ServiceProvider) message.getObject();

			Address address = serviceProvider.getAddress();

			CityTownVillage cityTown = cityRepo.findById(address.getCityTownVillage().getCode()).get();
			address.setCityTownVillage(cityTown);

			addressRepo.save(address);
			serviceProviderRepo.save(serviceProvider);
			message.acknowledge();
		} catch (JMSException e) {
			logger.error(e.getMessage(), e);
			Map<String, Object> errorObject = new HashMap<>();
			try {
				errorObject.put(HsConstants.PAYLOAD, message.getObject());
				errorObject.put(HsConstants.EXCEPTION, Arrays.asList(e.getStackTrace()));
			} catch (JMSException e1) {
				logger.error(e1.getMessage(), e1);
				errorObject.put(HsConstants.EXCEPTION, Arrays.asList(e.getStackTrace()));
			}
			jmsTemplate.convertAndSend(HsConstants.PROVIDER_REGISTRATION_ERROR_QUEUE, errorObject);
		}
	}
}
