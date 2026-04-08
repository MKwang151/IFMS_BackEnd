package com.mkwang.backend.modules.project.repository;

import com.mkwang.backend.modules.project.entity.Project;
import com.mkwang.backend.modules.project.entity.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByDepartment_IdOrderByCreatedAtDesc(Long departmentId);

    List<Project> findByDepartment_IdAndStatusOrderByCreatedAtDesc(Long departmentId, ProjectStatus status);

    List<Project> findAllByOrderByCreatedAtDesc();

    List<Project> findByStatusOrderByCreatedAtDesc(ProjectStatus status);

    @Query(
            value = """
                    select distinct p
                    from Project p
                    join p.members pm
                    where pm.user.id = :userId
                      and (:status is null or p.status = :status)
                    order by p.createdAt desc
                    """
    )
    List<Project> findMemberProjects(
            @Param("userId") Long userId,
            @Param("status") ProjectStatus status
    );
}




