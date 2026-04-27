package com.mkwang.backend.modules.user.repository;

import com.mkwang.backend.modules.user.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<User> hasDepartmentId(Long departmentId) {
        return (root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId);
    }

    public static Specification<User> excludeUserId(Long excludedUserId) {
        return (root, query, cb) -> cb.notEqual(root.get("id"), excludedUserId);
    }

    public static Specification<User> matchesSearch(String search) {
        return (root, query, cb) -> {
            String pattern = "%" + search.toLowerCase() + "%";
            Join<Object, Object> profile = root.join("profile", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(profile.get("employeeCode")), pattern)
            );
        };
    }

    public static Specification<User> fetchProfile() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("profile", JoinType.LEFT).fetch("avatarFile", JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    public static Specification<User> filterDepartmentMembers(Long departmentId, Long excludedUserId, String search) {
        Specification<User> spec = Specification.where(fetchProfile())
                .and(hasDepartmentId(departmentId));

        if (excludedUserId != null) {
            spec = spec.and(excludeUserId(excludedUserId));
        }

        if (search != null && !search.isBlank()) {
            spec = spec.and(matchesSearch(search.trim()));
        }

        return spec;
    }
}

