package com.mkwang.backend.modules.project.service;

import com.mkwang.backend.common.dto.PageResponse;
import com.mkwang.backend.modules.project.dto.request.CreateManagerProjectRequest;
import com.mkwang.backend.modules.project.dto.request.UpdateManagerProjectRequest;
import com.mkwang.backend.modules.project.dto.response.AvailableMemberResponse;
import com.mkwang.backend.modules.project.dto.response.DepartmentMemberDetailResponse;
import com.mkwang.backend.modules.project.dto.response.DepartmentMemberSummaryResponse;
import com.mkwang.backend.modules.project.dto.response.ProjectDetailResponse;
import com.mkwang.backend.modules.project.dto.response.ProjectSummaryResponse;
import com.mkwang.backend.modules.project.entity.ProjectStatus;

import java.util.List;

public interface ManagerProjectService {

    PageResponse<DepartmentMemberSummaryResponse> getDepartmentMembers(
            Long managerId, String search, int page, int limit);

    DepartmentMemberDetailResponse getDepartmentMemberDetail(Long managerId, Long memberId);

    PageResponse<ProjectSummaryResponse> getDepartmentProjects(
            Long managerId, ProjectStatus status, String search, int page, int limit);

    ProjectDetailResponse getDepartmentProjectDetail(Long managerId, Long projectId);

    ProjectDetailResponse createProject(Long managerId, CreateManagerProjectRequest request);

    ProjectDetailResponse updateProject(Long managerId, Long projectId, UpdateManagerProjectRequest request);

    List<AvailableMemberResponse> getDepartmentTeamLeaders(Long managerId);
}

