package com.mkwang.backend.modules.expense.repository;

import com.mkwang.backend.modules.expense.entity.PhaseCategoryBudget;
import com.mkwang.backend.modules.expense.entity.PhaseCategoryBudgetId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhaseCategoryBudgetRepository extends JpaRepository<PhaseCategoryBudget, PhaseCategoryBudgetId> {

    List<PhaseCategoryBudget> findByIdPhaseId(Long phaseId);

    List<PhaseCategoryBudget> findByIdCategoryId(Long categoryId);
}

