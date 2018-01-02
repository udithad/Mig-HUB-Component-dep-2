/*******************************************************************************
 * Copyright (c) 2015-2017, WSO2.Telco Inc. (http://www.wso2telco.com)
 *
 * All Rights Reserved. WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.wso2telco.dep.apihandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.AbstractHandler;

import com.wso2telco.dep.apihandler.dto.AddNewSpDTO;
import com.wso2telco.dep.apihandler.dto.TokenDTO;
import com.wso2telco.dep.apihandler.util.APIManagerDBUtil;
import com.wso2telco.dep.apihandler.util.ReadPropertyFile;
import com.wso2telco.dep.apihandler.util.TokenPoolUtil;

public class ApiInvocationHandler extends AbstractHandler {
	private static final Log log = LogFactory.getLog(ApiInvocationHandler.class);
	public static final String NEW_LINE = System.getProperty("line.separator");
	private static final String TOKEN_POOL_ENABLED = "enable_token_pool";
	private static final String AUTH_ENDPOINT = "authorize";
	private static final String TOKEN_ENDPOINT = "token";
	private static final String USERINFO_ENDPOINT = "userinfo";
	private static final String AUTH_HEADER = "Authorization";
	private static final String TEMP_AUTH_HEADER = "tempAuthVal";
	private static final String TOKEN_TYPE = "Bearer ";
	private static Map<String, String> spToken = new HashMap();

	public ApiInvocationHandler() {
	}

	public boolean handleRequest(org.apache.synapse.MessageContext messageContext) {
		handleAPIWise(messageContext);
		return true;
	}

	private void handleAPIWise(org.apache.synapse.MessageContext messageContext) {
		String fullPath = (String) ((Axis2MessageContext) messageContext).getProperty("REST_FULL_REQUEST_PATH");
		Map headerMap = (Map) ((Axis2MessageContext) messageContext).getAxis2MessageContext()
				.getProperty("TRANSPORT_HEADERS");

		if (fullPath.contains("authorize")) {
			handleAuthRequest(fullPath, headerMap);
		} else if (fullPath.contains("token")) {
			handleTokenRequest(messageContext, headerMap);
		} else if (fullPath.contains("userinfo"))
			handleUserInfoRequest(messageContext, headerMap);
	}

	private void handleUserInfoRequest(org.apache.synapse.MessageContext messageContext, Map headerMap) {
		swapHeader(messageContext, headerMap);
	}

	private void handleTokenRequest(org.apache.synapse.MessageContext messageContext, Map headerMap) {
		String basicAuth = swapHeader(messageContext, headerMap);
		String clientId = getTokenClientKey(basicAuth);
		processTokenResponse(headerMap, clientId);
	}

	private void handleAuthRequest(String fullPath, Map headerMap) {
		String clientId = getAuthClientKey(fullPath);
		processTokenResponse(headerMap, clientId);
	}

	private String getTokenClientKey(String basicAuth) {
		byte[] valueDecoded = Base64.decodeBase64(basicAuth.split(" ")[1].getBytes());
		String decodeString = new String(valueDecoded);
		return decodeString.split(":")[0];
	}

	private String getAuthClientKey(String fullPath) {
		return fullPath.split("client_id=")[1].split("&")[0];
	}

	private String swapHeader(org.apache.synapse.MessageContext messageContext, Map headerMap) {
		String basicAuth = (String) headerMap.get("Authorization");
		messageContext.setProperty("tempAuthVal", basicAuth);
		return basicAuth;
	}

	private void processTokenResponse(Map headerMap, String clientId) {
		String token = null;
		if (spToken.containsKey(clientId)) {
			token = (String) spToken.get(clientId);
		} else {
			token = APIManagerDBUtil.getTokenDetailsFromAPIManagerDB(clientId).getAccessToken();
			spToken.put(clientId, token);
		}
		headerMap.put("Authorization", "Bearer " + token);
	}

	private void handleByTokenPool(String clientId, TokenDTO tokenDTO) {
		Map<String, String> propertyMap = ReadPropertyFile.getPropertyFile();
		if (((String) propertyMap.get("enable_token_pool")).equals("true")) {
			if (log.isDebugEnabled()) {
				log.debug("TOken Pool Service Enabled for Insertion");
			}
			AddNewSpDTO newSpDto = new AddNewSpDTO();
			ArrayList<TokenDTO> tokenList = new ArrayList();

			newSpDto.setOwnerId(clientId);
			tokenDTO.setTokenAuth(getbase64EncodedTokenAouth(tokenDTO.getTokenAuth()));
			tokenList.add(tokenDTO);
			newSpDto.setSpTokenList(tokenList);
			callTokenPool(newSpDto);
		}
	}

	public boolean handleResponse(org.apache.synapse.MessageContext messageContext) {
		return true;
	}

	private void callTokenPool(final AddNewSpDTO newSpDto) {
		ExecutorService executorService = Executors.newFixedThreadPool(1);

		executorService.execute(new Runnable() {
			public void run() {
				ApiInvocationHandler.log.debug("Calling Token Pool Endpoint");

				TokenPoolUtil.callTokenPoolToAddSpToken(newSpDto);
			}
		});
		executorService.shutdown();
	}

	private String getbase64EncodedTokenAouth(String plainTextAouth) {
		byte[] bytesEncoded = Base64.encodeBase64(plainTextAouth.getBytes());
		String base64EncodedAouthString = new String(bytesEncoded);
		log.debug("ecncoded value is " + base64EncodedAouthString);
		return base64EncodedAouthString;
	}
}
