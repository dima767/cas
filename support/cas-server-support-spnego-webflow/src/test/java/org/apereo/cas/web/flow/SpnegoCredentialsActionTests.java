package org.apereo.cas.web.flow;

import org.apereo.cas.authentication.adaptive.AdaptiveAuthenticationPolicy;
import org.apereo.cas.support.spnego.MockJcifsAuthentication;
import org.apereo.cas.support.spnego.util.SpnegoConstants;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.EncodingUtils;
import org.apereo.cas.web.flow.resolver.CasDelegatingWebflowEventResolver;
import org.apereo.cas.web.flow.resolver.CasWebflowEventResolver;

import jcifs.spnego.Authentication;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.webflow.action.EventFactorySupport;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.test.MockRequestContext;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link SpnegoCredentialsActionTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@Import(SpnegoCredentialsActionTests.SpnegoAuthenticationTestConfiguration.class)
@Tag("Spnego")
class SpnegoCredentialsActionTests extends AbstractSpnegoTests {
    @Test
    void verifyOperation() throws Throwable {
        val context = new MockRequestContext();
        val request = new MockHttpServletRequest();
        request.addHeader(SpnegoConstants.HEADER_AUTHORIZATION, SpnegoConstants.NEGOTIATE + ' ' + EncodingUtils.encodeBase64("credential"));
        val response = new MockHttpServletResponse();
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, response));
        spnegoAction.execute(context);
        assertNotNull(response.getHeader(SpnegoConstants.HEADER_AUTHENTICATE));
    }

    @Test
    void verifyNoAuthzHeader() throws Throwable {
        val context = new MockRequestContext();
        val request = new MockHttpServletRequest();
        val response = new MockHttpServletResponse();
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, response));
        assertEquals(CasWebflowConstants.TRANSITION_ID_ERROR, spnegoAction.execute(context).getId());
    }

    @Test
    void verifyErrorWithBadCredential() throws Throwable {
        val context = new MockRequestContext();
        val request = new MockHttpServletRequest();
        request.addHeader(SpnegoConstants.HEADER_AUTHORIZATION, SpnegoConstants.NEGOTIATE + ' ' + EncodingUtils.encodeBase64("credential"));
        val response = new MockHttpServletResponse();
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, response));
        val stResolver = mock(CasWebflowEventResolver.class);
        val err = new EventFactorySupport().error(this);
        when(stResolver.resolveSingle(any())).thenReturn(err);
        val adaptive = mock(AdaptiveAuthenticationPolicy.class);
        when(adaptive.isAuthenticationRequestAllowed(any(), anyString(), any())).thenReturn(false);
        val action = new SpnegoCredentialsAction(mock(CasDelegatingWebflowEventResolver.class),
            stResolver, adaptive, false);
        assertEquals(CasWebflowConstants.TRANSITION_ID_AUTHENTICATION_FAILURE, action.execute(context).getId());
    }

    @Test
    void verifyBadAuthorizationHeader() throws Throwable {
        val context = new MockRequestContext();
        val request = new MockHttpServletRequest();
        val response = new MockHttpServletResponse();
        request.addHeader(SpnegoConstants.HEADER_AUTHORIZATION, "XYZ");
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, response));
        assertEquals(CasWebflowConstants.TRANSITION_ID_ERROR, spnegoAction.execute(context).getId());
    }
    
    @TestConfiguration(value = "SpnegoAuthenticationTestConfiguration", proxyBeanMethods = false)
    static class SpnegoAuthenticationTestConfiguration {
        @Bean
        public BlockingQueue<List<Authentication>> spnegoAuthenticationsPool() {
            val queue = new ArrayBlockingQueue<List<Authentication>>(1);
            queue.add(CollectionUtils.wrapList(new MockJcifsAuthentication()));
            return queue;
        }
    }
}
