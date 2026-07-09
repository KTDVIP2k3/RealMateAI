package com.GSU26SE22_SU26SE002.RealMateAI.responses;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedResponseDTO<T> {
    private List<T> content;
    private int pageNo;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean isLast;
    private List<Integer> pageNumbers;
    private boolean hasPreviousChunk;
    private boolean hasNextChunk;
}