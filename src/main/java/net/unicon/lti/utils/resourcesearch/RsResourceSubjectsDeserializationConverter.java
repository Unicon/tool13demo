package net.unicon.lti.utils.resourcesearch;

import com.fasterxml.jackson.databind.util.StdConverter;
import net.unicon.lti.model.resourcesearch.RsSubjectEntity;
import net.unicon.lti.repository.RsSubjectRepository;
import net.unicon.lti.service.resourcesearch.RsRepositoryService;

import java.util.HashSet;
import java.util.Set;

public class RsResourceSubjectsDeserializationConverter extends StdConverter<Set<String>, Set<RsSubjectEntity>> {

    @Override
    public Set<RsSubjectEntity> convert(Set<String> subjects) {
        RsSubjectRepository rsSubjectRepository = (RsSubjectRepository) RsRepositoryService.getRepos().get(RsSubjectEntity.class);
        Set<RsSubjectEntity> rsSubjects = new HashSet<>();
        for (String subject : subjects) {
            RsSubjectEntity rsSubject = rsSubjectRepository.findByName(subject);
            if (rsSubject != null) {
                rsSubjects.add(rsSubject);
            }
        }
        return rsSubjects;
    }
}
