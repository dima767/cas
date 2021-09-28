package org.apereo.cas.config;

import org.apereo.cas.audit.AuditableExecution;
import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.apereo.cas.validation.DelegatedAuthenticationServiceTicketValidationAuthorizer;
import org.apereo.cas.validation.RegisteredServiceDelegatedAuthenticationPolicyAuditableEnforcer;
import org.apereo.cas.validation.ServiceTicketValidationAuthorizer;
import org.apereo.cas.validation.ServiceTicketValidationAuthorizerConfigurer;
import org.apereo.cas.web.flow.DelegatedAuthenticationSingleSignOnParticipationStrategy;
import org.apereo.cas.web.flow.SingleSignOnParticipationStrategy;
import org.apereo.cas.web.flow.SingleSignOnParticipationStrategyConfigurer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.scribejava.core.model.OAuth1RequestToken;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This is {@link Pac4jDelegatedAuthenticationConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Configuration(value = "pac4jDelegatedAuthenticationConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class Pac4jDelegatedAuthenticationConfiguration {

    @Bean
    @RefreshScope
    @ConditionalOnMissingBean(name = "registeredServiceDelegatedAuthenticationPolicyAuditableEnforcer")
    public AuditableExecution registeredServiceDelegatedAuthenticationPolicyAuditableEnforcer() {
        return new RegisteredServiceDelegatedAuthenticationPolicyAuditableEnforcer();
    }

    @Bean
    @ConditionalOnMissingBean(name = "pac4jJacksonModule")
    public Module pac4jJacksonModule() {
        val module = new SimpleModule();
        module.setMixInAnnotation(OAuth1RequestToken.class, AbstractOAuth1RequestTokenMixin.class);
        return module;
    }

    @Bean
    @RefreshScope
    @Autowired
    public ServiceTicketValidationAuthorizer pac4jServiceTicketValidationAuthorizer(
        @Qualifier("registeredServiceDelegatedAuthenticationPolicyAuditableEnforcer")
        final AuditableExecution registeredServiceDelegatedAuthenticationPolicyAuditableEnforcer,
        @Qualifier(ServicesManager.BEAN_NAME)
        final ServicesManager servicesManager) {
        return new DelegatedAuthenticationServiceTicketValidationAuthorizer(servicesManager,
            registeredServiceDelegatedAuthenticationPolicyAuditableEnforcer);
    }

    @Bean
    @RefreshScope
    @Autowired
    public ServiceTicketValidationAuthorizerConfigurer pac4jServiceTicketValidationAuthorizerConfigurer(
        @Qualifier("pac4jServiceTicketValidationAuthorizer")
        final ServiceTicketValidationAuthorizer pac4jServiceTicketValidationAuthorizer) {
        return plan -> plan.registerAuthorizer(pac4jServiceTicketValidationAuthorizer);
    }

    @Bean
    @RefreshScope
    @Autowired
    @ConditionalOnMissingBean(name = "pac4jDelegatedAuthenticationSingleSignOnParticipationStrategy")
    public SingleSignOnParticipationStrategy pac4jDelegatedAuthenticationSingleSignOnParticipationStrategy(
        @Qualifier("authenticationServiceSelectionPlan")
        final AuthenticationServiceSelectionPlan authenticationServiceSelectionPlan,
        @Qualifier("defaultTicketRegistrySupport")
        final TicketRegistrySupport ticketRegistrySupport,
        @Qualifier(ServicesManager.BEAN_NAME)
        final ServicesManager servicesManager) {
        return new DelegatedAuthenticationSingleSignOnParticipationStrategy(servicesManager,
            authenticationServiceSelectionPlan, ticketRegistrySupport);
    }

    @Bean
    @ConditionalOnMissingBean(name = "pac4jDelegatedAuthenticationSingleSignOnParticipationStrategyConfigurer")
    @RefreshScope
    @Autowired
    public SingleSignOnParticipationStrategyConfigurer pac4jDelegatedAuthenticationSingleSignOnParticipationStrategyConfigurer(
        @Qualifier("pac4jDelegatedAuthenticationSingleSignOnParticipationStrategy")
        final SingleSignOnParticipationStrategy pac4jDelegatedAuthenticationSingleSignOnParticipationStrategy) {
        return chain -> chain.addStrategy(pac4jDelegatedAuthenticationSingleSignOnParticipationStrategy);
    }

    /**
     * The type Oauth1 request token mixin.
     */
    private abstract static class AbstractOAuth1RequestTokenMixin extends OAuth1RequestToken {
        private static final long serialVersionUID = -7839084408338396531L;

        @JsonCreator
        AbstractOAuth1RequestTokenMixin(
            @JsonProperty("token")
            final String token,
            @JsonProperty("tokenSecret")
            final String tokenSecret,
            @JsonProperty("oauthCallbackConfirmed")
            final boolean oauthCallbackConfirmed,
            @JsonProperty("rawResponse")
            final String rawResponse) {
            super(token, tokenSecret, oauthCallbackConfirmed, rawResponse);
        }

        @JsonIgnore
        @Override
        public abstract boolean isEmpty();
    }
}
