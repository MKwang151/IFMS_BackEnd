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
}

