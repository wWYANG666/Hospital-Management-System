package xmu.edu.yiyuan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import xmu.edu.yiyuan.config.PortalSettings;

@Controller
public class SpaController {
    private final PortalSettings portal;

    public SpaController(PortalSettings portal) { this.portal = portal; }

    @GetMapping({"/app", "/app/{*path}"})
    public String app() {
        return "forward:" + portal.assetPrefix() + "index.html";
    }
}
