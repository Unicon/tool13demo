package net.unicon.lti13demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;

@Controller
@RequestMapping("/linkables")
public class LinkablesController {

    @GetMapping("/**")
    public String getLinkable(Model model) {
        model.addAttribute("today", new Date());
        return "linkable";
    }

}
