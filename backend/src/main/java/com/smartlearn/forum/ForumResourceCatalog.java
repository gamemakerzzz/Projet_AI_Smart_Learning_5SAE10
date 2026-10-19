package com.smartlearn.forum;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.*;
import static com.smartlearn.forum.ForumInsightModels.*;

/** A reviewed resource list. The model selects IDs; it cannot supply destinations. */
@Component
public class ForumResourceCatalog {
    private final List<LearningResource> resources;
    public ForumResourceCatalog(ObjectMapper mapper) throws java.io.IOException {
        try(var input=new ClassPathResource("forum-resources.json").getInputStream()) {
            resources=List.of(mapper.readValue(input,LearningResource[].class));
        }
        Set<String> ids=new HashSet<>();
        for(var resource:resources) {
            URI uri=URI.create(resource.url());
            if(!ids.add(resource.id()) || !"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null)
                throw new IllegalStateException("Invalid forum resource catalogue entry.");
        }
    }
    public List<LearningResource> resources() { return resources; }
}
