package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageRequest {
    private int page = 0;
    private int size = 10;
}