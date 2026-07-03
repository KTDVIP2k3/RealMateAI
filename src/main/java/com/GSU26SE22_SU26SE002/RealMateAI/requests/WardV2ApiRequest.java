package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WardV2ApiRequest {
    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("name_en")
    private String nameEn;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("code_name")
    private String codeName;
}