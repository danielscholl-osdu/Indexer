package org.opengroup.osdu.indexer.swagger;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {
    @RequestMapping(value = "/swagger")
    public String swagger() {
        System.out.println("swagger-ui.html");
        return "redirect:swagger-ui.html";
    }
}
