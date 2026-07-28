package com.qianfan.tag.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 让 Vue history 路由在直接访问或刷新时回退到前端入口。 */
@Controller
public class SpaController {
    @GetMapping({"/persons", "/tags", "/reviews", "/sync", "/indicators", "/rules", "/imports", "/profiles"})
    public String index() {
        return "forward:/index.html";
    }
}
