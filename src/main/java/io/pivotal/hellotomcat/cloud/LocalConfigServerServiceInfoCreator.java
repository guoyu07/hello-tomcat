package io.pivotal.hellotomcat.cloud;

import io.pivotal.spring.cloud.service.common.ConfigServerServiceInfo;
import org.springframework.cloud.localconfig.LocalConfigServiceInfoCreator;

public class LocalConfigServerServiceInfoCreator extends LocalConfigServiceInfoCreator<ConfigServerServiceInfo> {

    public LocalConfigServerServiceInfoCreator() {
        super("http");
    }

    @Override
    public ConfigServerServiceInfo createServiceInfo(String id, String uri) {
        return new ConfigServerServiceInfo(id, uri, null, null, null);
    }
}
