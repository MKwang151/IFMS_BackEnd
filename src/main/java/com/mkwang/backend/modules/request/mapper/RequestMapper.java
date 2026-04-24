package com.mkwang.backend.modules.request.mapper;

import com.mkwang.backend.modules.request.dto.response.AttachmentResponse;
import com.mkwang.backend.modules.request.dto.response.RequestDetailResponse;
import com.mkwang.backend.modules.request.dto.response.RequestHistoryResponse;
import com.mkwang.backend.modules.request.dto.response.RequestSummaryResponse;
import com.mkwang.backend.modules.request.entity.Request;
import com.mkwang.backend.modules.request.entity.RequestAttachment;
import com.mkwang.backend.modules.request.entity.RequestHistory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RequestMapper {

    public RequestSummaryResponse toSummaryResponse(Request request) {
        return RequestSummaryResponse.builder()
                .id(request.getId())
                .requestCode(request.getRequestCode())
                .type(request.getType())
                .status(request.getStatus())
                .amount(request.getAmount())
                .approvedAmount(request.getApprovedAmount())
                .description(request.getDescription())
                .rejectReason(request.getRejectReason())
                .projectId(request.getProject() != null ? request.getProject().getId() : null)
                .projectName(request.getProject() != null ? request.getProject().getName() : null)
                .phaseId(request.getPhase() != null ? request.getPhase().getId() : null)
                .phaseName(request.getPhase() != null ? request.getPhase().getName() : null)
                .categoryId(request.getCategory() != null ? request.getCategory().getId() : null)
                .categoryName(request.getCategory() != null ? request.getCategory().getName() : null)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    public RequestDetailResponse toDetailResponse(Request request, List<RequestHistoryResponse> timeline) {
        return RequestDetailResponse.builder()
                .id(request.getId())
                .requestCode(request.getRequestCode())
                .type(request.getType())
                .status(request.getStatus())
                .amount(request.getAmount())
                .approvedAmount(request.getApprovedAmount())
                .description(request.getDescription())
                .rejectReason(request.getRejectReason())
                .paidAt(request.getPaidAt())
                .projectId(request.getProject() != null ? request.getProject().getId() : null)
                .projectCode(request.getProject() != null ? request.getProject().getProjectCode() : null)
                .projectName(request.getProject() != null ? request.getProject().getName() : null)
                .phaseId(request.getPhase() != null ? request.getPhase().getId() : null)
                .phaseCode(request.getPhase() != null ? request.getPhase().getPhaseCode() : null)
                .phaseName(request.getPhase() != null ? request.getPhase().getName() : null)
                .categoryId(request.getCategory() != null ? request.getCategory().getId() : null)
                .categoryName(request.getCategory() != null ? request.getCategory().getName() : null)
                .advanceBalanceId(request.getAdvanceBalance() != null ? request.getAdvanceBalance().getId() : null)
                .requesterId(request.getRequester() != null ? request.getRequester().getId() : null)
                .requesterName(request.getRequester() != null ? request.getRequester().getFullName() : null)
                .attachments(request.getAttachments().stream().map(this::toAttachmentResponse).toList())
                .timeline(timeline)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    public RequestHistoryResponse toHistoryResponse(RequestHistory history) {
        return RequestHistoryResponse.builder()
                .id(history.getId())
                .action(history.getAction())
                .statusAfterAction(history.getStatusAfterAction())
                .actorId(history.getActor() != null ? history.getActor().getId() : null)
                .actorName(history.getActor() != null ? history.getActor().getFullName() : null)
                .comment(history.getComment())
                .createdAt(history.getCreatedAt())
                .build();
    }

    public AttachmentResponse toAttachmentResponse(RequestAttachment attachment) {
        return AttachmentResponse.builder()
                .fileId(attachment.getFile().getId())
                .fileName(attachment.getFile().getFileName())
                .cloudinaryPublicId(attachment.getFile().getCloudinaryPublicId())
                .url(attachment.getFile().getUrl())
                .fileType(attachment.getFile().getFileType())
                .size(attachment.getFile().getSize())
                .build();
    }

}

