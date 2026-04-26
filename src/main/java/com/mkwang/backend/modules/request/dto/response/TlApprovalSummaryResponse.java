package com.mkwang.backend.modules.request.dto.response;

import com.mkwang.backend.modules.request.entity.RequestType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TlApprovalSummaryResponse {
    private Long id;
    private String requestCode;
    private RequestType type;
    private BigDecimal amount;
    private RequesterSnippet requester;
    private ProjectSnippet project;
    private PhaseSnippet phase;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class RequesterSnippet {
        private Long id;
        private String fullName;
        private String avatar;
        private String employeeCode;
    }

    @Getter
    @Builder
    public static class ProjectSnippet {
        private Long id;
        private String projectCode;
    }

    @Getter
    @Builder
    public static class PhaseSnippet {
        private Long id;
        private String phaseCode;
    }
}

