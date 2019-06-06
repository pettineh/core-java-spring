package eu.arrowhead.common.database.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.database.entity.ServiceDefinition;
import eu.arrowhead.common.database.entity.ServiceRegistry;
import eu.arrowhead.common.database.entity.System;
import eu.arrowhead.common.database.repository.ServiceDefinitionRepository;
import eu.arrowhead.common.database.repository.ServiceInterfaceRepository;
import eu.arrowhead.common.database.repository.ServiceRegistryInterfaceConnectionRepository;
import eu.arrowhead.common.database.repository.ServiceRegistryRepository;
import eu.arrowhead.common.database.repository.SystemRepository;
import eu.arrowhead.common.dto.DTOConverter;
import eu.arrowhead.common.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.common.dto.ServiceDefinitionsListResponseDTO;
import eu.arrowhead.common.dto.SystemResponseDTO;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.BadPayloadException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.InvalidParameterException;

@Service
public class ServiceRegistryDBService {
	
	//=================================================================================================
	// members
	
	@Autowired
	private ServiceRegistryRepository serviceRegistryRepository;
	
	@Autowired
	private ServiceDefinitionRepository serviceDefinitionRepository;
	
	@Autowired
	private ServiceInterfaceRepository serviceInterfaceRepository;
	
	@Autowired
	private ServiceRegistryInterfaceConnectionRepository serviceRegistryInterfaceConnectionRepository;
	
	@Autowired
	private SystemRepository systemRepository;
	
	private final Logger logger = LogManager.getLogger(ServiceRegistryDBService.class);
	
	private static final String COULD_NOT_CREATE_SYSTEM_ERROR_MESSAGE = "Could not crate System, with given parameters";
	private static final String COULD_NOT_UPDATE_SYSTEM_ERROR_MESSAGE = "Could not update System, with given parameters";
	private static final String COULD_NOT_DELETE_SYSTEM_ERROR_MESSAGE = "Could not delete System, with given parameters";
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	
	public System getSystemById(final long systemId) {
		
		logger.debug(" getSystemById started ...");
		
		final Optional<System> systemOption = systemRepository.findById(systemId);
		
		if (!systemOption.isPresent()){
			throw new NoSuchElementException();		
		}
		
		return systemOption.get();			
	}

	//-------------------------------------------------------------------------------------------------
	
	public Page<System> getSystemEntries(final int page, final int size, final String direction, final String sortField) {
		
		logger.debug(" getSystemEntries started ...");
		
		final int validatedPage;
		final int validatedSize;
		final Direction validatedDirection;
		final String validatedSortField;
		
		if (page < 0) {
			validatedPage = 0;
		}else {
			validatedPage = page;
		}
		
		if (size < 0) {
			validatedSize = Integer.MAX_VALUE;
		}else {
			validatedSize = size;
		}
		
		if (direction == null) {
			validatedDirection = Direction.ASC;
		}else {
			try {
				validatedDirection = Direction.fromString(direction);
			}catch (final IllegalArgumentException e) {
				throw new IllegalArgumentException("Direction field with reference '" + direction + "' is not available");
			}
			
		}
		
		if(sortField==null || "".equalsIgnoreCase(sortField)) {
			validatedSortField = CommonConstants.COMMON_FIELD_NAME_ID;
		}else {
			if (! System.SORTABLE_FIELDS_BY.contains(sortField)) {
				throw new IllegalArgumentException("Sortable field with reference '" + sortField + "' is not available");
			}else {
				validatedSortField = sortField;
			}
		}
		
		return systemRepository.findAll(PageRequest.of(validatedPage, validatedSize, validatedDirection, validatedSortField));
	}
	
	//-------------------------------------------------------------------------------------------------
	
	@Transactional (rollbackFor = Exception.class)
	public System createSystem(final String validatedSystemName, final String validatedAddress, final int validatedPort,
			final String validatedAuthenticationInfo) {
		
		logger.debug(" createSystem started ...");
		
		final System system = new System(validatedSystemName, validatedAddress, validatedPort, validatedAuthenticationInfo);
		
		try {
			return systemRepository.saveAndFlush(system);
		} catch ( final Exception e) {
		  throw new BadPayloadException(COULD_NOT_CREATE_SYSTEM_ERROR_MESSAGE, e);
		}
		
	}
	//-------------------------------------------------------------------------------------------------
	
	@Transactional (rollbackFor = Exception.class)
	public SystemResponseDTO createSystemResponse(final String validatedSystemName, final String validatedAddress, final int validatedPort,
			final String validatedAuthenticationInfo) {
		logger.debug(" createSystemResponse started ...");
		
		return DTOConverter.convertSystemToSystemResponseDTO(createSystem(validatedSystemName, validatedAddress, validatedPort, validatedAuthenticationInfo));
	}
	
	
	//-------------------------------------------------------------------------------------------------
	public ServiceDefinition getServiceDefinitionById(final long id) {
		logger.debug("getServiceDefinitionById started...");
		try {
			Optional<ServiceDefinition> find = serviceDefinitionRepository.findById(id);
			if (find.isPresent()) {
				return find.get();
			} else {
				throw new InvalidParameterException("Service definition with id of '" + id + "' not exists");
			}
		} catch (InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}		
	}
	
	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionResponseDTO getServiceDefinitionByIdResponse(final long id) {
		logger.debug("getServiceDefinitionByIdResponse started..");
		final ServiceDefinition serviceDefinitionEntry = getServiceDefinitionById(id);
		return DTOConverter.convertServiceDefinitionToServiceDefinitionResponseDTO(serviceDefinitionEntry);
	}
	
	//-------------------------------------------------------------------------------------------------
	public Page<ServiceDefinition> getServiceDefinitionEntries(final int page, final int size, final Direction direction, final String sortField) {
		logger.debug("getServiceDefinitionEntries started..");
		final int validatedPage = page < 0 ? 0 : page;
		final int validatedSize = size < 0 ? Integer.MAX_VALUE : size; 		
		final Direction validatedDirection = direction == null ? Direction.ASC : direction;
		final String validatedSortField = sortField == null ? CommonConstants.COMMON_FIELD_NAME_ID : sortField.trim();
		if (! ServiceDefinition.SORTABLE_FIELDS_BY.contains(validatedSortField)) {
			throw new InvalidParameterException("Sortable field with reference '" + validatedSortField + "' is not available");
		}
		try {
			return serviceDefinitionRepository.findAll(PageRequest.of(validatedPage, validatedSize, validatedDirection, validatedSortField));
		} catch (Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
		
		
	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionsListResponseDTO getServiceDefinitionEntriesResponse(final int page, final int size, final Direction direction, final String sortField) {
		logger.debug("getServiceDefinitionEntriesResponse started..");
		final List<ServiceDefinition> serviceDefinitionEntries = getServiceDefinitionEntries(page, size, direction, sortField).getContent();
		return DTOConverter.convertServiceDefinitionsListToServiceDefinitionListResponseDTO(serviceDefinitionEntries);
	}
	
	//-------------------------------------------------------------------------------------------------
	
	@Transactional (rollbackFor = ArrowheadException.class)
	public ServiceDefinition createServiceDefinition (final String serviceDefinition) {
		logger.debug("createServiceDefinition started..");
		checkConstraintsOfServiceDefinitionTable(serviceDefinition);
		final ServiceDefinition serviceDefinitionEntry = new ServiceDefinition(serviceDefinition);
		try {
			return serviceDefinitionRepository.saveAndFlush(serviceDefinitionEntry);
		} catch (Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public ServiceDefinitionResponseDTO createServiceDefinitionResponse (final String serviceDefinition) {
		logger.debug("createServiceDefinitionResponse started..");
		final ServiceDefinition serviceDefinitionEntry = createServiceDefinition(serviceDefinition);
		return DTOConverter.convertServiceDefinitionToServiceDefinitionResponseDTO(serviceDefinitionEntry);
	}
		
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public ServiceDefinition updateServiceDefinitionById(final long id, final String serviceDefinition) {
		logger.debug("updateServiceDefinitionById started..");
		try {
			Optional<ServiceDefinition> find = serviceDefinitionRepository.findById(id);
			if (find.isPresent()) {
				ServiceDefinition serviceDefinitionEntry = find.get();
				serviceDefinitionEntry.setServiceDefinition(serviceDefinition);
				return serviceDefinitionRepository.saveAndFlush(serviceDefinitionEntry);
			} else {
				throw new InvalidParameterException("Service definition with id of '" + id + "' not exists");
			}
		} catch (InvalidParameterException ex) {
			throw ex;
		} catch (Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public ServiceDefinitionResponseDTO updateServiceDefinitionByIdResponse(final long id, final String serviceDefinition) {
		logger.debug("updateServiceDefinitionByIdResponse started..");
		final ServiceDefinition serviceDefinitionEntry = updateServiceDefinitionById(id, serviceDefinition);
		return DTOConverter.convertServiceDefinitionToServiceDefinitionResponseDTO(serviceDefinitionEntry);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public void removeServiceDefinitionById(final long id) {
		logger.debug("removeServiceDefinitionById started..");
		try {
			if (!serviceRegistryRepository.existsById(id)) {
				throw new InvalidParameterException("Service Definition with id '" + id + "' not exists");
			}
			serviceDefinitionRepository.deleteById(id);
			serviceDefinitionRepository.flush();
		} catch (InvalidParameterException ex) {
			throw ex;
		} catch (Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	
	public Page<ServiceRegistry> getAllServiceReqistryEntries(final int page, final int size, final Direction direction, final String sortField) {
		logger.debug("getAllServiceReqistryEntries started..");
		final int validatedPage = page < 0 ? 0 : page;
		final int validatedSize = size < 0 ? Integer.MAX_VALUE : size; 		
		final Direction validatedDirection = direction == null ? Direction.ASC : direction;
		final String validatedSortField = sortField == null ? CommonConstants.COMMON_FIELD_NAME_ID : sortField.trim();
		if (! ServiceRegistry.SORTABLE_FIELDS_BY.contains(validatedSortField)) {
			throw new InvalidParameterException("Sortable field with reference '" + validatedSortField + "' is not available");
		}
		try {
			return serviceRegistryRepository.findAll(PageRequest.of(validatedPage, validatedSize, validatedDirection, validatedSortField));
		} catch (Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	
	@Transactional (rollbackFor = ArrowheadException.class)
	public void removeServiceRegistryEntryById(final long id) {
		logger.debug("removeServiceRegistryEntryById started..");
		try {
			if (!serviceRegistryRepository.existsById(id)) {
				throw new InvalidParameterException("Service Definition with id '" + id + "' not exists");
			}
			serviceRegistryRepository.deleteById(id);
			serviceRegistryRepository.flush();
		} catch (InvalidParameterException ex) {
			throw ex;
		} catch (Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	
	@Transactional (rollbackFor = ArrowheadException.class)
	public void removeBulkOfServiceRegistryEntries(final Iterable<ServiceRegistry> entities) {
		logger.debug("removeBulkOfServiceRegistryEntries started..");
		try {
			serviceRegistryRepository.deleteInBatch(entities);
			serviceRegistryRepository.flush();
		} catch  (Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}

	//-------------------------------------------------------------------------------------------------
	
	@Transactional (rollbackFor = Exception.class)
	public SystemResponseDTO updateSystemResponse(final long validatedSystemId, final String validatedSystemName, final String validatedAddress,
			final int validatedPort, final String validatedAuthenticationInfo) {
		
		try {			
			
			return DTOConverter.convertSystemToSystemResponseDTO(updateSystem(validatedSystemId,
					validatedSystemName,
					validatedAddress,
					validatedPort,
					validatedAuthenticationInfo));
		} catch ( final Exception e) {
		  throw new BadPayloadException(COULD_NOT_UPDATE_SYSTEM_ERROR_MESSAGE, e);
		}
	}

	//-------------------------------------------------------------------------------------------------
	
	@Transactional (rollbackFor = Exception.class)
	public System updateSystem(final long validatedSystemId, final String validatedSystemName, final String validatedAddress,
			final int validatedPort, final String validatedAuthenticationInfo) {
		
		logger.debug(" updateSystem started ...");
		
		final Optional<System> systemOptional = systemRepository.findById(validatedSystemId);
		if (!systemOptional.isPresent()) {
			throw new DataNotFoundException("No system with id : "+ validatedSystemId);
		}
		
		final System system = systemOptional.get();
		
		if (checkIfUniqValidationNeeded(system, validatedSystemName, validatedAddress, validatedPort)) {
			checkConstraintsOfSystemTable(validatedSystemName, validatedAddress, validatedPort);
		}
		
		system.setSystemName(validatedSystemName);
		system.setAddress(validatedAddress);
		system.setPort(validatedPort);
		system.setAuthenticationInfo(validatedAuthenticationInfo);
		
		return systemRepository.saveAndFlush(system);
		
	}
	
	//-------------------------------------------------------------------------------------------------
	
	@Transactional (rollbackFor = Exception.class)
	public void removeSystemById(final long id) {
		
		logger.debug(" removeSystemById started ...");
		
		if (!systemRepository.existsById(id)) {
			throw new DataNotFoundException(COULD_NOT_DELETE_SYSTEM_ERROR_MESSAGE);
		}		
		systemRepository.deleteById(id);
		systemRepository.flush();
		
	}
	
	//-------------------------------------------------------------------------------------------------
	
	@Transactional (rollbackFor = Exception.class)
	public SystemResponseDTO updateNonNullableSystemResponse(final long validatedSystemId, final String validatedSystemName,
			final String validatedAddress, final Integer validatedPort, final String validatedAuthenticationInfo) {
		
		logger.debug(" updateNonNullableSystemResponse started ...");
		
		try {			
			
			return DTOConverter.convertSystemToSystemResponseDTO(updateNonNullableSystem(validatedSystemId,
					validatedSystemName,
					validatedAddress,
					validatedPort,
					validatedAuthenticationInfo));
		} catch ( final Exception e) {
			throw new BadPayloadException(COULD_NOT_UPDATE_SYSTEM_ERROR_MESSAGE, e);
		}
	}
	
	
	//-------------------------------------------------------------------------------------------------
	
	
	@Transactional (rollbackFor = Exception.class)
	public System updateNonNullableSystem(final long validatedSystemId, final String validatedSystemName, final String validatedAddress,
			final Integer validatedPort, final String validatedAuthenticationInfo) {
		
		logger.debug(" updateNonNullableSystem started ...");
		
		final Optional<System> systemOptional = systemRepository.findById(validatedSystemId);
		if (!systemOptional.isPresent()) {
			throw new DataNotFoundException("No system with id : "+ validatedSystemId);
		}
		
		final System system = systemOptional.get();
		
		if (checkIfUniqValidationNeeded(system, validatedSystemName, validatedAddress, validatedPort)) {
			checkConstraintsOfSystemTable(validatedSystemName, validatedAddress, validatedPort);
		}
		if ( !Utilities.isEmpty(validatedSystemName)) {
			system.setSystemName(validatedSystemName);
		}
		if ( !Utilities.isEmpty(validatedAddress)) {
			system.setAddress(validatedAddress);
		}
		if ( validatedPort != null) {
			system.setPort(validatedPort);
		}
		if ( !Utilities.isEmpty(validatedAuthenticationInfo)) {
			system.setAuthenticationInfo(validatedAuthenticationInfo);
		}
		
		return systemRepository.saveAndFlush(system);
		
	}

	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	
	private boolean checkIfUniqValidationNeeded(final System system, final String validatedSystemName, final String validatedAddress,
			final Integer validatedPort) {
		
		logger.debug(" removeSystemById started ...");
		
		boolean isUniqnessCheckNeeded = false;
		
		final String actualSystemName = system.getSystemName();
		final String actualAddress = system.getAddress();
		final int  actualPort = system.getPort();
		
		if ( actualSystemName != null && validatedSystemName != null && !actualSystemName.equalsIgnoreCase(validatedSystemName)) {
				isUniqnessCheckNeeded = true;
		}
		
		if ( actualAddress != null &&  validatedAddress != null && !actualAddress.equalsIgnoreCase(validatedAddress)) {
			isUniqnessCheckNeeded = true;
		}
		
		if ( actualPort != validatedPort) {
			isUniqnessCheckNeeded = true;
		}
		
		return isUniqnessCheckNeeded;	
		
	}
	
	//-------------------------------------------------------------------------------------------------
	
	private void checkConstraintsOfServiceDefinitionTable(final String serviceDefinition) {
		try {
			Optional<ServiceDefinition> find = serviceDefinitionRepository.findByServiceDefinition(serviceDefinition);
			if (find.isPresent()) {
				throw new InvalidParameterException(serviceDefinition + "definition already exists");
			}
		} catch (InvalidParameterException ex) {
			throw ex;
		} catch (Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}		
	}
	
	//-------------------------------------------------------------------------------------------------
	
	private void checkConstraintsOfSystemTable(final String validatedSystemName, final String validatedAddress,
			final int validatedPort) {
		
		logger.debug(" checkConstraintsOfSystemTable started ...");
		
		final System find = systemRepository.findBySystemNameAndAddressAndPort(validatedSystemName, validatedAddress, validatedPort);
		if (find != null) {
			throw new BadPayloadException("Service by name:"+validatedSystemName+
					", address:" + validatedAddress +
					", port: "+validatedPort + 
					" already exists");
		}
		
		
	}
	



}
