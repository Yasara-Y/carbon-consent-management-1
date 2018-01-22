/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.consent.mgt.core;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.consent.mgt.core.connector.PIIController;
import org.wso2.carbon.consent.mgt.core.dao.PIICategoryDAO;
import org.wso2.carbon.consent.mgt.core.dao.PurposeCategoryDAO;
import org.wso2.carbon.consent.mgt.core.dao.PurposeDAO;
import org.wso2.carbon.consent.mgt.core.dao.ReceiptDAO;
import org.wso2.carbon.consent.mgt.core.exception.ConsentManagementClientException;
import org.wso2.carbon.consent.mgt.core.exception.ConsentManagementException;
import org.wso2.carbon.consent.mgt.core.internal.ConsentManagerConfiguration;
import org.wso2.carbon.consent.mgt.core.model.AddReceiptResponse;
import org.wso2.carbon.consent.mgt.core.model.PIICategory;
import org.wso2.carbon.consent.mgt.core.model.PiiController;
import org.wso2.carbon.consent.mgt.core.model.Purpose;
import org.wso2.carbon.consent.mgt.core.model.PurposeCategory;
import org.wso2.carbon.consent.mgt.core.model.Receipt;
import org.wso2.carbon.consent.mgt.core.model.ReceiptInput;
import org.wso2.carbon.consent.mgt.core.model.ReceiptPurposeInput;
import org.wso2.carbon.consent.mgt.core.model.ReceiptServiceInput;
import org.wso2.carbon.consent.mgt.core.util.ConsentConfigParser;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.API_VERSION;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_AT_LEAST_ONE_CATEGORY_ID_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_AT_LEAST_ONE_PII_CATEGORY_ID_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_AT_LEAST_ONE_PURPOSE_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_AT_LEAST_ONE_SERVICE_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_CONSENT_TYPE_MANDATORY;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_INVALID_ARGUMENTS_FOR_LIM_OFFSET;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_IS_PRIMARY_PURPOSE_IS_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PII_CATEGORY_ALREADY_EXIST;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PII_CATEGORY_ID_INVALID;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PII_CATEGORY_ID_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PII_CATEGORY_NAME_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PII_COLLECTION_METHOD_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PII_PRINCIPAL_ID_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PURPOSE_ALREADY_EXIST;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PURPOSE_CATEGORY_ALREADY_EXIST;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PURPOSE_CATEGORY_ID_INVALID;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PURPOSE_CATEGORY_ID_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PURPOSE_CATEGORY_NAME_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PURPOSE_ID_INVALID;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PURPOSE_ID_MANDATORY;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PURPOSE_ID_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PURPOSE_NAME_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_SERVICE_NAME_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_TERMINATION_IS_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_THIRD_PARTY_DISCLOSURE_IS_REQUIRED;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.PURPOSE_SEARCH_LIMIT_PATH;
import static org.wso2.carbon.consent.mgt.core.util.ConsentUtils.handleClientException;
import static org.wso2.carbon.consent.mgt.core.util.LambdaExceptionUtils.rethrowConsumer;

/**
 * Consent manager service implementation.
 */
public class ConsentManager {

    private static final Log log = LogFactory.getLog(ConsentManager.class);
    private static final int DEFAULT_SEARCH_LIMIT = 100;
    private PurposeDAO purposeDAO;
    private PurposeCategoryDAO purposeCategoryDAO;
    private PIICategoryDAO piiCategoryDAO;
    private ReceiptDAO receiptDAO;
    private ConsentConfigParser configParser;
    private PIIController piiController;

    public ConsentManager(ConsentManagerConfiguration configuration) {

        purposeDAO = getPurposeDAO(configuration.getPurposeDAOs());
        purposeCategoryDAO = getPurposeCategoryDAO(configuration.getPurposeCategoryDAOs());
        piiCategoryDAO = getPiiCategoryDAO(configuration.getPiiCategoryDAOs());
        receiptDAO = getReceiptsDAO(configuration.getReceiptDAOs());
        piiController = getPIIController(configuration.getPiiControllers());
        configParser = configuration.getConfigParser();
    }

    /**
     * This API is used to add a new Purpose.
     *
     * @param purpose Purpose element with name and description.
     * @return 201 Created. Return purpose element with purpose Id.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public Purpose addPurpose(Purpose purpose) throws ConsentManagementException {

        validateInputParameters(purpose);
        return purposeDAO.addPurpose(purpose);
    }

    /**
     * This API is used to get the purpose by purpose Id.
     *
     * @param purposeId ID of the purpose.
     * @return 200 OK with purpose element.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public Purpose getPurpose(int purposeId) throws ConsentManagementException {

        return purposeDAO.getPurposeById(purposeId);
    }

    /**
     * This API is used to get the purpose by purpose name.
     *
     * @param name Name of the purpose.
     * @return 200 Ok with purpose element.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public Purpose getPurposeByName(String name) throws ConsentManagementException {

        return purposeDAO.getPurposeByName(name);
    }

    /**
     * This API is used to get all or filtered existing purposes.
     *
     * @param limit  Number of search results.
     * @param offset Start index of the search.
     * @return 200 OK with Filtered list of Purpose elements
     * @throws ConsentManagementException Consent Management Exception.
     */
    public List<Purpose> listPurposes(int limit, int offset) throws ConsentManagementException {

        validatePaginationParameters(limit, offset);

        if (limit == 0) {
            limit = getDefaultLimitFromConfig();
            if (log.isDebugEnabled()) {
                log.debug("Limit is not defied the request, default to :" + limit);
            }
        }
        return purposeDAO.listPurposes(limit, offset);
    }

    /**
     * This api is used to delete existing purpose by purpose Id.
     *
     * @param purposeId ID of the purpose.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public void deletePurpose(int purposeId) throws ConsentManagementException {

        if (purposeId == 0 || purposeId < 0) {
            if (log.isDebugEnabled()) {
                log.debug("Purpose Id is not found in the request or invalid purpose Id");
            }
            throw handleClientException(ERROR_CODE_PURPOSE_ID_REQUIRED, null);
        }

        if (getPurpose(purposeId) == null) {
            throw handleClientException(ERROR_CODE_PURPOSE_ID_INVALID, String.valueOf(purposeId));
        }
        int id = purposeDAO.deletePurpose(purposeId);
        if (log.isDebugEnabled()) {
            log.debug("Purpose deleted successfully. ID: " + id);
        }
    }

    /**
     * This API is used to check whether a purpose exists with given name.
     *
     * @param name Name of the purpose.
     * @return true, if an element is found.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public boolean isPurposeExists(String name) throws ConsentManagementException {

        return getPurposeByName(name) != null;
    }

    /**
     * This API is used to add a new purpose category.
     *
     * @param purposeCategory purpose category element with name and description.
     * @return 201 created. Return PurposeCategory element with the category ID.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public PurposeCategory addPurposeCategory(PurposeCategory purposeCategory) throws ConsentManagementException {

        validateInputParameters(purposeCategory);
        return purposeCategoryDAO.addPurposeCategory(purposeCategory);
    }

    /**
     * This API is used to get purpose category by ID.
     *
     * @param purposeCategoryId Purpose category ID.
     * @return 200 Ok with purpose category element.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public PurposeCategory getPurposeCategory(int purposeCategoryId) throws ConsentManagementException {

        return purposeCategoryDAO.getPurposeCategoryById(purposeCategoryId);
    }

    /**
     * This API is used to get purpose category by name.
     *
     * @param name Name of the purpose category.
     * @return 200 Ok with purpose category element.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public PurposeCategory getPurposeCategoryByName(String name) throws ConsentManagementException {

        return purposeCategoryDAO.getPurposeCategoryByName(name);
    }

    /**
     * This API is used to list all or filtered list of purpose categories.
     *
     * @param limit  Number of search results.
     * @param offset Start index of the search.
     * @return Filtered list of purpose categories.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public List<PurposeCategory> listPurposeCategories(int limit, int offset) throws ConsentManagementException {

        validatePaginationParameters(limit, offset);

        if (limit == 0) {
            limit = getDefaultLimitFromConfig();
            if (log.isDebugEnabled()) {
                log.debug("Limit is not defied the request, default to :" + limit);
            }
        }
        return purposeCategoryDAO.listPurposeCategories(limit, offset);
    }

    /**
     * This API is used to delete purpose category by ID.
     *
     * @param purposeCategoryId ID of the purpose category to be deleted.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public void deletePurposeCategory(int purposeCategoryId) throws ConsentManagementException {

        if (purposeCategoryId == 0 || purposeCategoryId < 0) {
            if (log.isDebugEnabled()) {
                log.debug("Purpose Category Id is not found in the request or invalid Id");
            }
            throw handleClientException(ERROR_CODE_PURPOSE_CATEGORY_ID_REQUIRED, null);
        }

        if (getPurposeCategory(purposeCategoryId) == null) {
            throw handleClientException(ERROR_CODE_PURPOSE_CATEGORY_ID_INVALID, String.valueOf(purposeCategoryId));
        }
        int id = purposeCategoryDAO.deletePurposeCategory(purposeCategoryId);
        if (log.isDebugEnabled()) {
            log.debug("Purpose category deleted successfully. ID: " + id);
        }
    }

    /**
     * This API is used to check whether a purpose category exists for a given name.
     *
     * @param name Name of the purpose.
     * @return true if a category found.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public boolean isPurposeCategoryExists(String name) throws ConsentManagementException {

        return getPurposeCategoryByName(name) != null;
    }

    /**
     * This API is used to add a new PII category.
     *
     * @param piiCategory PIICategory element with name and description.
     * @return 201 Created. Returns PII Category element with ID.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public PIICategory addPIICategory(PIICategory piiCategory) throws ConsentManagementException {

        validateInputParameters(piiCategory);
        return piiCategoryDAO.addPIICategory(piiCategory);
    }

    /**
     * This API is used ot get PII category by name.
     *
     * @param name Name of the PII category.
     * @return 200 OK. Returns PII category with ID.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public PIICategory getPIICategoryByName(String name) throws ConsentManagementException {

        return piiCategoryDAO.getPIICategoryByName(name);
    }

    /**
     * This API is sued to get PII category by ID.
     *
     * @param piiCategoryId ID of the PII category.
     * @return 200 OK. Returns PII category
     * @throws ConsentManagementException
     */
    public PIICategory getPIICategory(int piiCategoryId) throws ConsentManagementException {

        return piiCategoryDAO.getPIICategoryById(piiCategoryId);
    }

    /**
     * This API is used to list all or filtered set of PII categories.
     *
     * @param limit  Number of search results.
     * @param offset Start index of the search.
     * @return 200 Ok. Returns filtered list of PII category elements.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public List<PIICategory> listPIICategories(int limit, int offset) throws ConsentManagementException {

        validatePaginationParameters(limit, offset);

        if (limit == 0) {
            limit = getDefaultLimitFromConfig();
            if (log.isDebugEnabled()) {
                log.debug("Limit is not defied the request, default to :" + limit);
            }
        }
        return piiCategoryDAO.listPIICategories(limit, offset);
    }

    /**
     * This API is used to delete PII category by ID.
     *
     * @param piiCategoryId ID of the PII category.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public void deletePIICategory(int piiCategoryId) throws ConsentManagementException {

        if (piiCategoryId == 0 || piiCategoryId < 0) {
            if (log.isDebugEnabled()) {
                log.debug("PII Category Id is not found in the request or invalid PII category Id");
            }
            throw handleClientException(ERROR_CODE_PII_CATEGORY_ID_REQUIRED, null);
        }

        if (getPIICategory(piiCategoryId) == null) {
            throw handleClientException(ERROR_CODE_PII_CATEGORY_ID_INVALID, String.valueOf(piiCategoryId));
        }
        int id = piiCategoryDAO.deletePIICategory(piiCategoryId);
        if (log.isDebugEnabled()) {
            log.debug("PII Category deleted successfully. ID: " + id);
        }
    }

    /**
     * This API is sued to check whether a PII category exists for a given name.
     *
     * @param name Name of the PII category.
     * @return true if a category exists.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public boolean isPIICategoryExists(String name) throws ConsentManagementException {

        return getPIICategoryByName(name) != null;
    }

    /**
     * This API is used to verify and store consent input.
     *
     * @param receiptInput consent input.
     * @throws ConsentManagementException Consent Management Exception.
     */
    public AddReceiptResponse addConsent(ReceiptInput receiptInput) throws ConsentManagementException {
        //TODO checkIsReceiptExists
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        receiptInput.setTenantDomain(tenantDomain);
        validateInputParameters(receiptInput);
        receiptInput.setConsentReceiptId(generateConsentReceiptId(receiptInput));
        setAPIVersion(receiptInput);
        receiptDAO.addReceipt(receiptInput);
        return new AddReceiptResponse(receiptInput.getConsentReceiptId(), receiptInput.getCollectionMethod(),
                receiptInput.getLanguage(), receiptInput.getPiiPrincipalId(), receiptInput.getTenantDomain());
    }

    public Receipt getReceipt(String receiptId) throws ConsentManagementException {

        Receipt receipt = receiptDAO.getReceipt(receiptId);
        setPIIControllerInfo(receipt);
        return receipt;
    }

    /**
     * This API is used to select the PIIController from List of registered PIIController. By default set the highest
     * priority one.
     *
     * @param piiControllers list of PIIControllers.
     * @return selected PIIController.
     */
    protected PIIController getPIIController(List<PIIController> piiControllers) {

        PIIController piiController = null;
        if (CollectionUtils.isNotEmpty(piiControllers)) {
            piiController = piiControllers.get(piiControllers.size() - 1);
        }
        return piiController;
    }

    /**
     * This API is used to select the ReceiptDAO from List of registered ReceiptDAO. By default set the highest
     * priority one.
     *
     * @param receiptDAOs list of ReceiptDAOs.
     * @return selected ReceiptDAO.
     */
    protected ReceiptDAO getReceiptsDAO(List<ReceiptDAO> receiptDAOs) {

        ReceiptDAO receiptDAO = null;
        if (CollectionUtils.isNotEmpty(receiptDAOs)) {
            receiptDAO = receiptDAOs.get(receiptDAOs.size() - 1);
        }
        return receiptDAO;
    }

    /**
     * This API is used to select the PIICategoryDAO from List of registered PIICategoryDAOs. By default set the highest
     * priority one.
     *
     * @param piiCategoryDAOs list of PIICategoryDAOs.
     * @return selected PIICategoryDAO.
     */
    protected PIICategoryDAO getPiiCategoryDAO(List<PIICategoryDAO> piiCategoryDAOs) {

        PIICategoryDAO piiCategoryDAO = null;
        if (CollectionUtils.isNotEmpty(piiCategoryDAOs)) {
            piiCategoryDAO = piiCategoryDAOs.get(piiCategoryDAOs.size() - 1);
        }
        return piiCategoryDAO;
    }

    /**
     * This API is used to select the PurposeCategoryDAO from List of registered PurposeCategoryDAOs. By default set the
     * highest priority one.
     *
     * @param purposeCategoryDAOs list of PurposeCategoryDAOs.
     * @return selected PurposeCategoryDAO.
     */
    protected PurposeCategoryDAO getPurposeCategoryDAO(List<PurposeCategoryDAO> purposeCategoryDAOs) {

        PurposeCategoryDAO purposeCategoryDAO = null;
        if (CollectionUtils.isNotEmpty(purposeCategoryDAOs)) {
            purposeCategoryDAO = purposeCategoryDAOs.get(purposeCategoryDAOs.size() - 1);
        }
        return purposeCategoryDAO;
    }

    /**
     * This API is used to select the PurposeDAO from List of registered PurposeDAOs. By default set the
     * highest priority one.
     *
     * @param purposeDAOs list of PurposeDAOs.
     * @return selected PurposeDAO.
     */
    protected PurposeDAO getPurposeDAO(List<PurposeDAO> purposeDAOs) {

        PurposeDAO purposeDAO = null;
        if (CollectionUtils.isNotEmpty(purposeDAOs)) {
            purposeDAO = purposeDAOs.get(purposeDAOs.size() - 1);
        }
        return purposeDAO;
    }

    /**
     * This API is used to set the API version is being used.
     *
     * @param receiptInput ReceiptInput.
     */
    protected void setAPIVersion(ReceiptInput receiptInput) {

        receiptInput.setVersion(API_VERSION);
    }

    /**
     * This API is used to generate a unique consent receipt Id.
     *
     * @param receiptInput ReceiptInput
     * @return A unique ID.
     */
    protected String generateConsentReceiptId(ReceiptInput receiptInput) {

        return UUID.randomUUID().toString();
    }

    private void setPIIControllerInfo(Receipt receipt) {

        PiiController controllerInfo = piiController.getControllerInfo(receipt.getTenantDomain());
        List<PiiController> piiControllers = Arrays.asList(controllerInfo);
        receipt.setPiiControllers(piiControllers);
    }

    private void validateInputParameters(ReceiptInput receiptInput) throws ConsentManagementClientException {

        validateRequiredParametersInConsent(receiptInput);
        receiptInput.getServices().forEach(rethrowConsumer(receiptServiceInput -> {
            validateRequiredParametersInService(receiptServiceInput);
            receiptServiceInput.getPurposes().forEach(rethrowConsumer(receiptPurposeInput -> {
                validateRequiredParametersInPurpose(receiptServiceInput, receiptPurposeInput);
            }));
        }));
    }

    private void validateRequiredParametersInConsent(ReceiptInput receiptInput) throws ConsentManagementClientException {

        if (isBlank(receiptInput.getPiiPrincipalId())) {
            throw handleClientException(ERROR_CODE_PII_PRINCIPAL_ID_REQUIRED, null);
        }

        if (isBlank(receiptInput.getCollectionMethod())) {
            throw handleClientException(ERROR_CODE_PII_COLLECTION_METHOD_REQUIRED, null);
        }

        if (isEmpty(receiptInput.getServices())) {
            throw handleClientException(ERROR_CODE_AT_LEAST_ONE_SERVICE_REQUIRED, null);
        }
    }

    private void validateRequiredParametersInService(ReceiptServiceInput receiptServiceInput)
            throws ConsentManagementClientException {

        if (isBlank(receiptServiceInput.getService())) {
            throw handleClientException(ERROR_CODE_SERVICE_NAME_REQUIRED, null);
        }

        if (isEmpty(receiptServiceInput.getPurposes())) {
            throw handleClientException(ERROR_CODE_AT_LEAST_ONE_PURPOSE_REQUIRED, null);
        }
    }

    private void validateRequiredParametersInPurpose(ReceiptServiceInput receiptServiceInput,
                                                     ReceiptPurposeInput receiptPurposeInput)
            throws ConsentManagementException {

        String serviceName = receiptServiceInput.getService();
        if (receiptPurposeInput.getPurposeId() == null) {
            throw handleClientException(ERROR_CODE_PURPOSE_ID_MANDATORY, serviceName);
        } else {
            // To verify whether the purpose exist in the system. This method will throw an exception if not exist.
            getPurpose(receiptPurposeInput.getPurposeId());
        }

        if (isBlank(receiptPurposeInput.getConsentType())) {
            throw handleClientException(ERROR_CODE_CONSENT_TYPE_MANDATORY, serviceName);
        }

        if (isEmpty(receiptPurposeInput.getPurposeCategoryId())) {
            throw handleClientException(ERROR_CODE_AT_LEAST_ONE_CATEGORY_ID_REQUIRED, serviceName);
        } else {
            // To verify whether the purposeCategory exist in the system.
            // This method will throw an exception if not exist.
            receiptPurposeInput.getPurposeCategoryId().forEach(rethrowConsumer(this::getPurposeCategory));
        }

        if (isEmpty(receiptPurposeInput.getPiiCategoryId())) {
            throw handleClientException(ERROR_CODE_AT_LEAST_ONE_PII_CATEGORY_ID_REQUIRED, serviceName);
        } else {
            // To verify whether the piiCategory exist in the system.
            // This method will throw an exception if not exist.
            receiptPurposeInput.getPiiCategoryId().forEach(rethrowConsumer(this::getPIICategory));
        }

        if (receiptPurposeInput.isPrimaryPurpose() == null) {
            throw handleClientException(ERROR_CODE_IS_PRIMARY_PURPOSE_IS_REQUIRED, serviceName);
        }

        if (isBlank(receiptPurposeInput.getTermination())) {
            throw handleClientException(ERROR_CODE_TERMINATION_IS_REQUIRED, serviceName);
        }

        if (receiptPurposeInput.isThirdPartyDisclosure() == null) {
            throw handleClientException(ERROR_CODE_THIRD_PARTY_DISCLOSURE_IS_REQUIRED, serviceName);
        }
    }

    private int getDefaultLimitFromConfig() {

        int limit = DEFAULT_SEARCH_LIMIT;

        if (configParser.getConfiguration().get(PURPOSE_SEARCH_LIMIT_PATH) != null) {
            limit = Integer.parseInt(configParser.getConfiguration()
                    .get(PURPOSE_SEARCH_LIMIT_PATH).toString());
        }
        return limit;
    }

    private void validatePaginationParameters(int limit, int offset) throws ConsentManagementClientException {

        if (limit < 0 || offset < 0) {
            throw handleClientException(ERROR_CODE_INVALID_ARGUMENTS_FOR_LIM_OFFSET, null);
        }
    }

    private void validateInputParameters(PurposeCategory purposeCategory) throws ConsentManagementException {

        if (isBlank(purposeCategory.getName())) {
            if (log.isDebugEnabled()) {
                log.debug("Purpose Category name cannot be empty");
            }
            throw handleClientException(ERROR_CODE_PURPOSE_CATEGORY_NAME_REQUIRED, null);
        }

        if (isPurposeCategoryExists(purposeCategory.getName())) {
            if (log.isDebugEnabled()) {
                log.debug("A purpose category already exists with name: " + purposeCategory.getName());
            }
            throw handleClientException(ERROR_CODE_PURPOSE_CATEGORY_ALREADY_EXIST, purposeCategory.getName());
        }
    }

    private void validateInputParameters(Purpose purpose) throws ConsentManagementException {

        if (isBlank(purpose.getName())) {
            if (log.isDebugEnabled()) {
                log.debug("Purpose name cannot be empty");
            }
            throw handleClientException(ERROR_CODE_PURPOSE_NAME_REQUIRED, null);
        }

        if (isPurposeExists(purpose.getName())) {
            if (log.isDebugEnabled()) {
                log.debug("A purpose already exists with name: " + purpose.getName());
            }
            throw handleClientException(ERROR_CODE_PURPOSE_ALREADY_EXIST, purpose.getName());
        }
    }

    private void validateInputParameters(PIICategory piiCategory) throws ConsentManagementException {

        if (isBlank(piiCategory.getName())) {
            if (log.isDebugEnabled()) {
                log.debug("PII Category name cannot be empty");
            }
            throw handleClientException(ERROR_CODE_PII_CATEGORY_NAME_REQUIRED, null);
        }

        if (isPIICategoryExists(piiCategory.getName())) {
            if (log.isDebugEnabled()) {
                log.debug("A PII Category already exists with name: " + piiCategory.getName());
            }
            throw handleClientException(ERROR_CODE_PII_CATEGORY_ALREADY_EXIST, piiCategory.getName());
        }

        if (piiCategory.getSensitive() == null) {
            piiCategory.setSensitive(false);
        }
    }
}