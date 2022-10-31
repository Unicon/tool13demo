package net.unicon.lti.utils.resourcesearch;

import com.fasterxml.jackson.databind.util.StdConverter;
import net.unicon.lti.model.resourcesearch.RsSubjectEntity;
import net.unicon.lti.repository.RsSubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RsResourceSubjectsDeserializationConverter extends StdConverter<Set<String>, Set<RsSubjectEntity>> {

    @Autowired
    RsSubjectRepository rsSubjectRepository;

    @Override
    public Set<RsSubjectEntity> convert(Set<String> subjects) {
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
