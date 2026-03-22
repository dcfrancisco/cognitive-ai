package ph.francisco.interfaceadapters;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DemoPageController {

    @GetMapping("/demo")
    public String demoPage() {
        return "demo";
    }
}
