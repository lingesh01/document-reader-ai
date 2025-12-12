package com.documentreaderai.repository;

import com.documentreaderai.model.entity.BatchJob;
import com.documentreaderai.model.entity.BatchJob.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BatchJobRepository extends JpaRepository<BatchJob, UUID> {
    
    List<BatchJob> findByStatusIn(List<BatchStatus> statuses);
    
    List<BatchJob> findByStatusOrderByCreatedAtDesc(BatchStatus status);
    
    List<BatchJob> findAllByOrderByCreatedAtDesc();
}