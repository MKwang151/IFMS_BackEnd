package com.mkwang.backend.modules.accounting.repository;

import com.mkwang.backend.modules.accounting.entity.Payslip;
import com.mkwang.backend.modules.accounting.entity.PayslipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    @Query(
            value = """
                    select p
                    from Payslip p
                    join p.period period
                    where p.user.id = :userId
                      and (:year is null or period.year = :year)
                      and (:status is null or p.status = :status)
                    order by period.year desc, period.month desc, p.createdAt desc
                    """,
            countQuery = """
                    select count(p.id)
                    from Payslip p
                    join p.period period
                    where p.user.id = :userId
                      and (:year is null or period.year = :year)
                      and (:status is null or p.status = :status)
                    """
    )
    Page<Payslip> findMyPayslips(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("status") PayslipStatus status,
            Pageable pageable
    );

    @Query("""
            select p
            from Payslip p
            join fetch p.period period
            join fetch p.user user
            left join fetch user.profile profile
            left join fetch user.department department
            where p.id = :id and user.id = :userId
            """)
    Optional<Payslip> findMyPayslipDetailById(@Param("userId") Long userId, @Param("id") Long id);
}

