/*
    Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
    except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
    the specific language governing permissions and limitations under the License.
 */

package com.amazon.ask.builder;

import com.amazon.ask.attributes.persistence.PersistenceAdapter;
import com.amazon.ask.dispatcher.exception.ExceptionHandler;
import com.amazon.ask.dispatcher.exception.ExceptionMapper;
import com.amazon.ask.dispatcher.request.interceptor.RequestInterceptor;
import com.amazon.ask.dispatcher.request.interceptor.ResponseInterceptor;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.dispatcher.request.mapper.RequestMapper;
import com.amazon.ask.dispatcher.request.handler.impl.DefaultHandlerAdapter;
import com.amazon.ask.dispatcher.request.mapper.impl.DefaultRequestMapper;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.services.ApiClient;
import com.amazon.ask.module.SdkModule;
import com.amazon.ask.module.SdkModuleContext;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SkillBuilderTest {

    private CustomSkillBuilder builder;
    private RequestHandler mockRequestHandler;
    private ExceptionHandler mockExceptionHandler;

    @Before
    public void setup() {
        builder = new CustomSkillBuilder();
        mockRequestHandler = mock(RequestHandler.class);
        mockExceptionHandler = mock(ExceptionHandler.class);
    }

    @Test
    public void request_mapper_configured_with_handler() {
        when(mockRequestHandler.canHandle(any())).thenReturn(true);
        builder.addRequestHandler(mockRequestHandler);
        SkillConfiguration configuration = builder.getConfigBuilder().build();
        RequestMapper mapper = configuration.getRequestMappers().get(0);
        assertTrue(mapper instanceof DefaultRequestMapper);
        assertEquals(mockRequestHandler, mapper.getRequestHandlerChain(getInputForIntent("FooIntent")).get().getRequestHandler());
    }

    @Test
    public void request_interceptor_used() {
        RequestInterceptor requestInterceptor = mock(RequestInterceptor.class);
        builder.addRequestHandler(mockRequestHandler);
        builder.addRequestInterceptor(requestInterceptor);
        SkillConfiguration configuration = builder.getConfigBuilder().build();
        assertEquals(configuration.getRequestInterceptors().get(0), requestInterceptor);
    }

    @Test
    public void response_interceptor_used() {
        ResponseInterceptor responseInterceptor = mock(ResponseInterceptor.class);
        builder.addRequestHandler(mockRequestHandler);
        builder.addResponseInterceptor(responseInterceptor);
        SkillConfiguration configuration = builder.getConfigBuilder().build();
        assertEquals(configuration.getResponseInterceptors().get(0), responseInterceptor);
    }

    @Test
    public void error_handler_chain_configured() {
        when(mockExceptionHandler.canHandle(any(), any())).thenReturn(true);
        builder.addRequestHandler(mockRequestHandler);
        builder.addExceptionHandler(mockExceptionHandler);
        SkillConfiguration configuration = builder.getConfigBuilder().build();
        ExceptionMapper exceptionMapper = configuration.getExceptionMapper();
        assertEquals(mockExceptionHandler, exceptionMapper.getHandler(HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder().build()).build(), new Exception()).get());
    }

    @Test
    public void default_handler_adapter_used() {
        builder.addRequestHandler(mockRequestHandler);
        SkillConfiguration configuration = builder.getConfigBuilder().build();
        assertEquals(1, configuration.getHandlerAdapters().size());
        assertTrue(configuration.getHandlerAdapters().get(0) instanceof DefaultHandlerAdapter);
    }

    @Test
    public void no_custom_persistence_adapter_null() {
        builder.addRequestHandler(mockRequestHandler);
        builder.withPersistenceAdapter(null);
        SkillConfiguration config = builder.getConfigBuilder().build();
        assertNull(config.getPersistenceAdapter());
    }

    @Test
    public void custom_persistence_adapter_used() {
        PersistenceAdapter persistenceAdapter = mock(PersistenceAdapter.class);
        builder.addRequestHandler(mockRequestHandler);
        builder.withPersistenceAdapter(persistenceAdapter);
        SkillConfiguration config = builder.getConfigBuilder().build();
        assertEquals(config.getPersistenceAdapter(), persistenceAdapter);
    }

    @Test
    public void no_custom_api_client_null() {
        builder.addRequestHandler(mockRequestHandler);
        builder.withApiClient(null);
        SkillConfiguration config = builder.getConfigBuilder().build();
        assertNull(config.getApiClient());
    }

    @Test
    public void custom_api_client_used() {
        ApiClient apiClient = mock(ApiClient.class);
        builder.addRequestHandler(mockRequestHandler);
        builder.withApiClient(apiClient);
        SkillConfiguration config = builder.getConfigBuilder().build();
        assertEquals(config.getApiClient(), apiClient);
    }

    @Test
    public void sdk_module_executed() {
        SdkModule mockModule = mock(SdkModule.class);
        builder.addRequestHandler(mockRequestHandler);
        builder.registerSdkModule(mockModule);
        builder.build();
        verify(mockModule).setupModule(any(SdkModuleContext.class));
    }

    private HandlerInput getInputForIntent(String intentName) {
        return HandlerInput.builder()
                .withRequestEnvelope(getRequestEnvelopeForIntent(intentName))
                .build();
    }

    private RequestEnvelope getRequestEnvelopeForIntent(String intentName) {
        return RequestEnvelope.builder()
                .withRequest(IntentRequest.builder()
                        .withIntent(Intent.builder()
                                .withName(intentName)
                                .build())
                        .build())
                .build();
    }

}
