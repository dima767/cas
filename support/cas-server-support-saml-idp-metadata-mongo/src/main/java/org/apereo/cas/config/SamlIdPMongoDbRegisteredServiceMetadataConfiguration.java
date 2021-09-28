package org.apereo.cas.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.mongo.MongoDbConnectionFactory;
import org.apereo.cas.support.saml.OpenSamlConfigBean;
import org.apereo.cas.support.saml.metadata.resolver.MongoDbSamlRegisteredServiceMetadataResolver;
import org.apereo.cas.support.saml.services.idp.metadata.cache.resolver.SamlRegisteredServiceMetadataResolver;
import org.apereo.cas.support.saml.services.idp.metadata.plan.SamlRegisteredServiceMetadataResolutionPlanConfigurer;

import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import javax.net.ssl.SSLContext;

/**
 * This is {@link SamlIdPMongoDbIdPMetadataConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Configuration(value = "samlIdPMongoDbRegisteredServiceMetadataConfiguration", proxyBeanMethods = false)
public class SamlIdPMongoDbRegisteredServiceMetadataConfiguration {

    @Bean
    @RefreshScope
    @Autowired
    public SamlRegisteredServiceMetadataResolver mongoDbSamlRegisteredServiceMetadataResolver(final CasConfigurationProperties casProperties,
                                                                                              @Qualifier("mongoDbSamlMetadataResolverTemplate")
                                                                                              final MongoTemplate mongoDbSamlMetadataResolverTemplate,
                                                                                              @Qualifier(OpenSamlConfigBean.DEFAULT_BEAN_NAME)
                                                                                              final OpenSamlConfigBean openSamlConfigBean) {
        val idp = casProperties.getAuthn().getSamlIdp();
        return new MongoDbSamlRegisteredServiceMetadataResolver(idp, openSamlConfigBean, mongoDbSamlMetadataResolverTemplate);
    }

    @ConditionalOnMissingBean(name = "mongoDbSamlMetadataResolverTemplate")
    @Bean
    @RefreshScope
    @Autowired
    public MongoTemplate mongoDbSamlMetadataResolverTemplate(final CasConfigurationProperties casProperties,
                                                             @Qualifier("sslContext")
                                                             final SSLContext sslContext) {
        val mongo = casProperties.getAuthn().getSamlIdp().getMetadata().getMongo();
        val factory = new MongoDbConnectionFactory(sslContext);
        val mongoTemplate = factory.buildMongoTemplate(mongo);
        MongoDbConnectionFactory.createCollection(mongoTemplate, mongo.getCollection(), mongo.isDropCollection());
        return mongoTemplate;
    }

    @Bean
    @RefreshScope
    @ConditionalOnMissingBean(name = "mongoDbSamlRegisteredServiceMetadataResolutionPlanConfigurer")
    public SamlRegisteredServiceMetadataResolutionPlanConfigurer mongoDbSamlRegisteredServiceMetadataResolutionPlanConfigurer(
        @Qualifier("mongoDbSamlRegisteredServiceMetadataResolver")
        final SamlRegisteredServiceMetadataResolver mongoDbSamlRegisteredServiceMetadataResolver) {
        return plan -> plan.registerMetadataResolver(mongoDbSamlRegisteredServiceMetadataResolver);
    }
}
