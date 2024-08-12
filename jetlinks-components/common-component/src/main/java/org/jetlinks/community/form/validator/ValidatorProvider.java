package org.jetlinks.community.form.validator;

import org.jetlinks.community.spi.Provider;

import java.util.Map;
import java.util.Optional;

/**
 * @author gyl
 * @since 2.2
 */
public interface ValidatorProvider {

    Provider<ValidatorProvider> providers = Provider.create(ValidatorProvider.class);


    static Optional<Validator> creatValidator(ValidatorSpec validatorSpec) {
        return providers
            .get(validatorSpec.getProvider())
            .map(provider -> provider.creatValidator(validatorSpec.getConfiguration()));
    }

    String getProvider();

    Validator creatValidator(Map<String,Object> configuration);
}
