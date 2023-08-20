package org.apereo.cas.nativex;

import org.apereo.cas.adaptors.x509.authentication.principal.AbstractX509PrincipalResolver;
import org.apereo.cas.util.nativex.CasRuntimeHintsRegistrar;
import lombok.val;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import java.util.Collection;

/**
 * This is {@link X509CertificateRuntimeHints}.
 *
 * @author Misagh Moayyed
 * @since 7.0.0
 */
public class X509CertificateRuntimeHints implements CasRuntimeHintsRegistrar {
    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        val classes = findSubclassesInPackage(AbstractX509PrincipalResolver.class, AbstractX509PrincipalResolver.class.getPackageName());
        registerReflectionHints(hints, classes);
    }

    private static void registerReflectionHints(final RuntimeHints hints, final Collection entries) {
        entries.forEach(el -> hints.reflection().registerType((Class) el,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
            MemberCategory.INVOKE_DECLARED_METHODS,
            MemberCategory.INVOKE_PUBLIC_METHODS,
            MemberCategory.DECLARED_FIELDS,
            MemberCategory.PUBLIC_FIELDS));
    }
}
