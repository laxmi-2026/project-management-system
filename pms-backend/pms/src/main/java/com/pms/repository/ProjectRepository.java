package com.pms.repository;

import com.pms.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * JOIN FETCH loads the createdBy User in the SAME query, avoiding the
     * lazy-loading ByteBuddyInterceptor serialization crash we hit and
     * fixed in Task 2's LMS project. Without this, Hibernate would try
     * to lazily load createdBy AFTER the database session is already
     * closed, which throws LazyInitializationException.
     */
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.createdBy ORDER BY p.createdAt DESC")
    List<Project> findAllWithCreatedBy();

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.createdBy WHERE p.id = :id")
    Optional<Project> findByIdWithCreatedBy(@Param("id") Long id);

    /**
     * Used for Members: only show projects where at least one task is
     * assigned to them. This keeps Member dashboards focused on relevant
     * work instead of every project in the company.
     */
    @Query("""
           SELECT DISTINCT p FROM Project p
           LEFT JOIN FETCH p.createdBy
           WHERE p.id IN (
               SELECT t.project.id FROM Task t WHERE t.assignedTo.id = :userId
           )
           ORDER BY p.createdAt DESC
           """)
    List<Project> findProjectsForMember(@Param("userId") Long userId);

    long countByStatus(Project.Status status);
}
