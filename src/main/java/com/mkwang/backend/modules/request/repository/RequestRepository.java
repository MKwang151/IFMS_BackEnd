package com.mkwang.backend.modules.request.repository;

import com.mkwang.backend.modules.request.entity.Request;
import com.mkwang.backend.modules.request.entity.RequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long>, JpaSpecificationExecutor<Request> {

    @Query("""
            SELECT r.status, COUNT(r) FROM Request r
            WHERE r.requester.id = :userId
            GROUP BY r.status
            """)
    List<Object[]> countByStatusForUser(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT r FROM Request r
            LEFT JOIN FETCH r.project
            LEFT JOIN FETCH r.phase
            LEFT JOIN FETCH r.category
            LEFT JOIN FETCH r.requester
            LEFT JOIN FETCH r.attachments att
            LEFT JOIN FETCH att.file
            WHERE r.id = :id AND r.requester.id = :userId
            """)
    Optional<Request> findDetailByIdAndRequesterId(
            @Param("id") Long id,
            @Param("userId") Long userId);

    @Query("""
            SELECT h FROM RequestHistory h
            LEFT JOIN FETCH h.actor
            WHERE h.request.id = :requestId
            ORDER BY h.createdAt ASC
            """)
    List<RequestHistory> findHistoriesByRequestId(@Param("requestId") Long requestId);

    Optional<Request> findByIdAndRequesterId(Long id, Long requesterId);

    @Query("""
            SELECT DISTINCT r FROM Request r
            LEFT JOIN FETCH r.requester u
            LEFT JOIN FETCH u.profile up
            LEFT JOIN FETCH up.avatarFile
            LEFT JOIN FETCH r.project
            LEFT JOIN FETCH r.phase
            LEFT JOIN FETCH r.category
            LEFT JOIN FETCH r.attachments att
            LEFT JOIN FETCH att.file
            WHERE r.id = :id AND r.project.id IN :projectIds
            """)
    Optional<Request> findDetailByIdForTl(
            @Param("id") Long id,
            @Param("projectIds") List<Long> projectIds);

    @Query("""
            SELECT DISTINCT r FROM Request r
            LEFT JOIN FETCH r.requester u
            LEFT JOIN FETCH u.profile up
            LEFT JOIN FETCH up.avatarFile
            LEFT JOIN FETCH r.project p
            LEFT JOIN FETCH p.department
            LEFT JOIN FETCH r.attachments att
            LEFT JOIN FETCH att.file
            WHERE r.id = :id
              AND r.type = 'PROJECT_TOPUP'
              AND p.department.id = :departmentId
            """)
    Optional<Request> findDetailByIdForManager(
            @Param("id") Long id,
            @Param("departmentId") Long departmentId);

    @Query("""
            SELECT DISTINCT r FROM Request r
            LEFT JOIN FETCH r.requester u
            LEFT JOIN FETCH u.profile up
            LEFT JOIN FETCH up.avatarFile
            LEFT JOIN FETCH u.department
            LEFT JOIN FETCH r.project
            LEFT JOIN FETCH r.phase
            LEFT JOIN FETCH r.category
            LEFT JOIN FETCH r.advanceBalance
            LEFT JOIN FETCH r.attachments att
            LEFT JOIN FETCH att.file
            WHERE r.id = :id
              AND r.status = 'APPROVED_BY_TEAM_LEADER'
              AND r.type IN ('ADVANCE', 'EXPENSE', 'REIMBURSE')
            """)
    Optional<Request> findDetailByIdForAccountant(@Param("id") Long id);

    @Query("""
            SELECT r FROM Request r
            LEFT JOIN FETCH r.requester u
            LEFT JOIN FETCH u.profile up
            LEFT JOIN FETCH up.avatarFile
            LEFT JOIN FETCH u.department
            WHERE r.id = :id
              AND r.type = 'DEPARTMENT_TOPUP'
            """)
    Optional<Request> findDetailByIdForCfo(@Param("id") Long id);

    @Query("""
            SELECT r.requester.id, COUNT(r)
            FROM Request r
            WHERE r.requester.id IN :userIds
              AND r.status IN ('PENDING', 'APPROVED_BY_TEAM_LEADER')
            GROUP BY r.requester.id
            """)
    List<Object[]> countPendingByRequesterIds(@Param("userIds") List<Long> userIds);

    @Query("""
            SELECT COUNT(r)
            FROM Request r
            WHERE r.requester.id = :userId
              AND r.status IN ('PENDING', 'APPROVED_BY_TEAM_LEADER')
            """)
    int countPendingForRequester(@Param("userId") Long userId);

    /**
     * Count pending requests for a specific member scoped to a set of projects.
     * Used by Team Leader team-members list to populate pendingRequestsCount.
     * "Pending" = PENDING or APPROVED_BY_TEAM_LEADER (not yet disbursed).
     */
    @Query("""
            SELECT COUNT(r) FROM Request r
            WHERE r.requester.id = :userId
              AND r.project.id IN :projectIds
              AND r.status IN ('PENDING', 'APPROVED_BY_TEAM_LEADER')
            """)
    int countPendingForMemberInProjects(
            @Param("userId") Long userId,
            @Param("projectIds") List<Long> projectIds
    );

    /**
     * Top-10 most recent requests for a member scoped to a set of projects.
     * Used by Team Leader team-members detail to populate recentRequests.
     */
    @Query("""
            SELECT r FROM Request r
            LEFT JOIN FETCH r.project p
            LEFT JOIN FETCH r.category
            WHERE r.requester.id = :userId
              AND p.id IN :projectIds
            ORDER BY r.createdAt DESC
            """)
    List<Request> findTop10RecentByRequesterInProjects(
            @Param("userId") Long userId,
            @Param("projectIds") List<Long> projectIds,
            org.springframework.data.domain.Pageable pageable
    );
}


