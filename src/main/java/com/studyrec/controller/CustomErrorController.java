package com.studyrec.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError() {
        // Provide a fallback route instead of Whitelabel error page
        // We just redirect to index.html to let SPA handle or show a nice UI
        return "forward:/index.html";
    }
}
