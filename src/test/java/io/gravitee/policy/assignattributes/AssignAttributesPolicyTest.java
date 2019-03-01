/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.assignattributes;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.assignattributes.configuration.AssignAttributesPolicyConfiguration;
import io.gravitee.policy.assignattributes.configuration.Attribute;
import io.gravitee.policy.assignattributes.configuration.PolicyScope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AssignAttributesPolicyTest {

    @InjectMocks
    private AssignAttributesPolicy assignVariablePolicy;

    @Mock
    private AssignAttributesPolicyConfiguration assignVariablePolicyConfiguration;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    protected PolicyChain policyChain;

    @Before
    public void init() {
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue(any(String.class), any())).thenAnswer(returnsFirstArg());
    }

    @Test
    public void testOnRequest_noAssignation() {
        assignVariablePolicy.onRequest(request, response, executionContext, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnResponse_noAssignation() {
        assignVariablePolicy.onResponse(request, response, executionContext, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_invalidScope() {
        when(assignVariablePolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        assignVariablePolicy.onRequest(request, response, executionContext, policyChain);

        verify(executionContext, never()).setAttribute(any(), any());
        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnResponse_invalidScope() {
        when(assignVariablePolicyConfiguration.getScope()).thenReturn(PolicyScope.REQUEST);
        assignVariablePolicy.onResponse(request, response, executionContext, policyChain);

        verify(executionContext, never()).setAttribute(any(), any());
        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_addAttribute() {
        // Prepare
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-Gravitee-Test", "Value");
        when(request.headers()).thenReturn(httpHeaders);

        when(assignVariablePolicyConfiguration.getAttributes())
                .thenReturn(Collections.singletonList(new Attribute("Context-Attribute-Key", "{#request.headers['X-Gravitee-Test']}")));

        // Run
        assignVariablePolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(executionContext).setAttribute(eq("Context-Attribute-Key"), any());
        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnResponse_addAttribute() {
        // Prepare
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-Gravitee-Test", "Value");
        when(response.headers()).thenReturn(httpHeaders);

        when(assignVariablePolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(assignVariablePolicyConfiguration.getAttributes())
                .thenReturn(Collections.singletonList(new Attribute("Context-Attribute-Key", "{#response.headers['X-Gravitee-Test']}")));

        // Run
        assignVariablePolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(executionContext).setAttribute(eq("Context-Attribute-Key"), any());
        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_addAttributes() {
        // Prepare
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-Gravitee-Test", "Value");
        httpHeaders.add("X-Gravitee-Test2", "Value2");
        when(request.headers()).thenReturn(httpHeaders);


        List<Attribute> attributes = new LinkedList<>();
        attributes.add(new Attribute("Context-Attribute-Key", "{#request.headers['X-Gravitee-Test']}"));
        attributes.add(new Attribute("Context-Attribute-Key2", "{#request.headers['X-Gravitee-Test2']}"));

        when(assignVariablePolicyConfiguration.getAttributes()).thenReturn(attributes);

        // Run
        assignVariablePolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(executionContext, times(2)).setAttribute(any(), any());
        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnResponse_addAttributes() {
        // Prepare
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-Gravitee-Test", "Value");
        httpHeaders.add("X-Gravitee-Test2", "Value2");
        when(response.headers()).thenReturn(httpHeaders);


        List<Attribute> attributes = new LinkedList<>();
        attributes.add(new Attribute("Context-Attribute-Key", "{#response.headers['X-Gravitee-Test']}"));
        attributes.add(new Attribute("Context-Attribute-Key2", "{#response.headers['X-Gravitee-Test2']}"));

        when(assignVariablePolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(assignVariablePolicyConfiguration.getAttributes()).thenReturn(attributes);

        // Run
        assignVariablePolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(executionContext, times(2)).setAttribute(any(), any());
        verify(policyChain).doNext(request, response);
    }

}
