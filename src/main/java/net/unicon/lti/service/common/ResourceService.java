package net.unicon.lti.service.common;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public interface ResourceService<T> {

    default Set<T> getResources(Class<T> objectClass) throws IOException {
        // the seed files leave keyId null for the database to assign; Jackson 3 rejects null for a
        // primitive unless told otherwise
        ObjectMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();
        ClassLoader cl = this.getClass().getClassLoader();
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(cl);
        Resource[] resources;
        try {
            resources = resourceResolver.getResources(getDirectoryPath() + "/*.json");
        } catch (FileNotFoundException e) {
            // no seed folder means nothing to load
            return new HashSet<>();
        }
        Set<T> objects = new HashSet<>();
        for (Resource resource : resources) {
            T object = mapper.readValue(resource.getInputStream(), objectClass);
            objects.add(object);
        }
        return objects;
    }

    String getDirectoryPath();

    void setDefaults();

}
