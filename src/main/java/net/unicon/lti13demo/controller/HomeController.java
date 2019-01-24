package net.unicon.lti13demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class HomeController {

    @RequestMapping(method = RequestMethod.GET)
    public String index() {
        return "home"; // name of the template
    }

}
