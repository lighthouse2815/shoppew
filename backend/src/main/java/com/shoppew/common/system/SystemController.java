package com.shoppew.common.system;

import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.config.AppProperties;
import java.time.Clock;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/system")
public class SystemController {

    private final AppProperties properties;
    private final Clock clock;

    public SystemController(AppProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<Map<String, String>> systemInformation() {
        return ApiResponse.success(
                Map.of(
                        "apiVersion", properties.apiVersion(),
                        "locale", properties.locale(),
                        "currency", properties.currency(),
                        "timeZone", properties.timeZone()),
                clock);
    }
}
