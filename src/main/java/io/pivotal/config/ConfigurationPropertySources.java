package io.pivotal.config;

import org.springframework.boot.env.EnumerableCompositePropertySource;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Holds the configuration {@link PropertySource}s as they are loaded can relocate
 * them once configuration classes have been processed.
 */
public class ConfigurationPropertySources
        extends EnumerablePropertySource<Collection<PropertySource<?>>> {

    private final Collection<PropertySource<?>> sources;

    private final String[] names;

    ConfigurationPropertySources(Collection<PropertySource<?>> sources) {
        super(ConfigFileEnvironmentProcessor.APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME, sources);
        this.sources = sources;
        List<String> names = new ArrayList<>();
        for (PropertySource<?> source : sources) {
            if (source instanceof EnumerablePropertySource) {
                names.addAll(Arrays.asList(
                        ((EnumerablePropertySource<?>) source).getPropertyNames()));
            }
        }
        this.names = names.toArray(new String[names.size()]);
    }

    @Override
    public Object getProperty(String name) {
        for (PropertySource<?> propertySource : this.sources) {
            Object value = propertySource.getProperty(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public void finishAndRelocate(MutablePropertySources propertySources) {
        String name = ConfigFileEnvironmentProcessor.APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME;
        ConfigurationPropertySources removed = (ConfigurationPropertySources) propertySources
                .get(name);
        if (removed != null) {
            for (PropertySource<?> propertySource : removed.sources) {
                if (propertySource instanceof EnumerableCompositePropertySource) {
                    EnumerableCompositePropertySource composite = (EnumerableCompositePropertySource) propertySource;
                    for (PropertySource<?> nested : composite.getSource()) {
                        propertySources.addAfter(name, nested);
                        name = nested.getName();
                    }
                } else {
                    propertySources.addAfter(name, propertySource);
                }
            }
            propertySources.remove(ConfigFileEnvironmentProcessor.APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME);
        }
    }

    @Override
    public String[] getPropertyNames() {
        return this.names;
    }
}
