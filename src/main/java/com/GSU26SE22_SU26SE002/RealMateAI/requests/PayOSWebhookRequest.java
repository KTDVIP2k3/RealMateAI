package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class PayOSWebhookRequest {
    private String code;
    private String desc;
    private Map<String, Object> data;
}