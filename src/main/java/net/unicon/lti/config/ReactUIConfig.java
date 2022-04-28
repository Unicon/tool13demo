package net.unicon.lti.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class ReactUIConfig {

    @Bean
    public ClassLoaderTemplateResolver reactTemplateResolver() {
        // It's important to define an additional set of templates for the React UI which is going to be deployed in the static folder.
        ClassLoaderTemplateResolver reactTemplateResolver = new ClassLoaderTemplateResolver();
        reactTemplateResolver.setPrefix("static/");
        reactTemplateResolver.setSuffix(".html");
        reactTemplateResolver.setTemplateMode(TemplateMode.HTML);
        reactTemplateResolver.setCharacterEncoding("UTF-8");
        reactTemplateResolver.setOrder(2);
        reactTemplateResolver.setCheckExistence(true);
        return reactTemplateResolver;
    }

}
