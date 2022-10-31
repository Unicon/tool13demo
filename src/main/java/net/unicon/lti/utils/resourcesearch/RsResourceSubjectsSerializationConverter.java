package net.unicon.lti.utils.resourcesearch;

import com.fasterxml.jackson.databind.util.StdConverter;
import net.unicon.lti.model.resourcesearch.RsSubjectEntity;

import java.util.HashSet;
import java.util.Set;

public class RsResourceSubjectsSerializationConverter extends StdConverter<Set<RsSubjectEntity>, Set<String>> {
    @Override
    public Set<String> convert(Set<RsSubjectEntity> rsSubjects) {
        Set<String> subjects = new HashSet<>();
        for (RsSubjectEntity rsSubject : rsSubjects) {
            subjects.add(rsSubject.getName());
        }
        return subjects;
    }
}
