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

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.BufferedReadWriteStream;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.SimpleReadWriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;
import io.gravitee.policy.assignattributes.configuration.AssignAttributesPolicyConfiguration;
import io.gravitee.policy.assignattributes.configuration.PolicyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AssignAttributesPolicy {

    private static final Logger logger = LoggerFactory.getLogger(AssignAttributesPolicy.class);

    private final static String REQUEST_VARIABLE = "request";
    private final static String RESPONSE_VARIABLE = "request";

    private final AssignAttributesPolicyConfiguration assignVariablePolicyConfiguration;

    public AssignAttributesPolicy(final AssignAttributesPolicyConfiguration assignVariablePolicyConfiguration) {
        this.assignVariablePolicyConfiguration = assignVariablePolicyConfiguration;
    }

    @OnRequestContent
    public ReadWriteStream onRequestContent(Request request, ExecutionContext executionContext) {
        if (assignVariablePolicyConfiguration.getScope() != null && assignVariablePolicyConfiguration.getScope() == PolicyScope.REQUEST_CONTENT) {
            return new BufferedReadWriteStream() {

                Buffer buffer = Buffer.buffer();

                @Override
                public SimpleReadWriteStream<Buffer> write(Buffer content) {
                    buffer.appendBuffer(content);
                    return this;
                }

                @Override
                public void end() {
                    String content = buffer.toString();
                    executionContext.getTemplateEngine().getTemplateContext()
                            .setVariable(REQUEST_VARIABLE, new EvaluableRequest(request, content));

                    // assign
                    assign(executionContext);

                    if (buffer.length() > 0) {
                        super.write(buffer);
                    }

                    super.end();
                }
            };
        }

        return null;
    }

    @OnResponseContent
    public ReadWriteStream onResponseContent(Response response, ExecutionContext executionContext) {
        if (assignVariablePolicyConfiguration.getScope() != null && assignVariablePolicyConfiguration.getScope() == PolicyScope.RESPONSE_CONTENT) {
            return new BufferedReadWriteStream() {

                Buffer buffer = Buffer.buffer();

                @Override
                public SimpleReadWriteStream<Buffer> write(Buffer content) {
                    buffer.appendBuffer(content);
                    return this;
                }

                @Override
                public void end() {
                    String content = buffer.toString();
                    executionContext.getTemplateEngine().getTemplateContext()
                            .setVariable(RESPONSE_VARIABLE, new EvaluableResponse(response, content));

                    // assign
                    assign(executionContext);

                    if (buffer.length() > 0) {
                        super.write(buffer);
                    }

                    super.end();
                }
            };
        }

        return null;
    }

    @OnRequest
    public void onRequest(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        if (assignVariablePolicyConfiguration.getScope() == null || assignVariablePolicyConfiguration.getScope() == PolicyScope.REQUEST) {
            // assign
            assign(executionContext);
        }

        // continue chaining
        policyChain.doNext(request, response);
    }

    @OnResponse
    public void onResponse(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        if (assignVariablePolicyConfiguration.getScope() != null && assignVariablePolicyConfiguration.getScope() == PolicyScope.RESPONSE) {
            // assign
            assign(executionContext);
        }

        // continue chaining
        policyChain.doNext(request, response);
    }

    private void assign(ExecutionContext executionContext) {
        if (assignVariablePolicyConfiguration.getAttributes() != null) {
            assignVariablePolicyConfiguration.getAttributes().forEach(
                    attribute -> {
                        if (attribute.getName() != null && !attribute.getName().trim().isEmpty()) {
                            try {
                                Object extValue = (attribute.getValue() != null) ? executionContext.getTemplateEngine().getValue(attribute.getValue(), Object.class) : null;
                                if (extValue != null) {
                                    executionContext.setAttribute(attribute.getName(), extValue);
                                }
                            } catch (Exception ex) {
                                logger.error("An error occurs while decoding context attribute", ex);
                            }
                        }
                    });
        }
    }
}
