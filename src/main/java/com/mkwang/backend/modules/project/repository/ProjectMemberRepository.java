package com.mkwang.backend.modules.project.repository;

import com.mkwang.backend.modules.project.entity.ProjectMember;
import com.mkwang.backend.modules.project.entity.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    boolean existsByProject_IdAndUser_Id(Long projectId, Long userId);
}


