package com.mkwang.backend.modules.user.repository;

import com.mkwang.backend.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            SELECT u
            FROM User u
            LEFT JOIN FETCH u.profile up
            LEFT JOIN FETCH up.avatarFile
            WHERE u.id = :userId
            """)
    Optional<User> findByIdWithProfile(@Param("userId") Long userId);

    @Query("""
            SELECT u
            FROM User u
            LEFT JOIN FETCH u.profile up
            LEFT JOIN FETCH up.avatarFile
            WHERE u.department.id = :departmentId
              AND u.status = 'ACTIVE'
              AND u.role.name = 'TEAM_LEADER'
            ORDER BY u.fullName ASC
            """)
    List<User> findActiveTeamLeadersByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("""
            SELECT u
            FROM User u
            LEFT JOIN FETCH u.profile up
            LEFT JOIN FETCH up.avatarFile
            WHERE u.id = :userId
              AND u.department.id = :departmentId
            """)
    Optional<User> findByIdAndDepartmentIdWithProfile(@Param("userId") Long userId, @Param("departmentId") Long departmentId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.department.id = :departmentId")
    long countByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.profile up
            LEFT JOIN FETCH up.avatarFile
            WHERE u.department.id = :departmentId
            ORDER BY u.fullName ASC
            """)
    List<User> findByDepartmentIdWithProfile(@Param("departmentId") Long departmentId);
}
