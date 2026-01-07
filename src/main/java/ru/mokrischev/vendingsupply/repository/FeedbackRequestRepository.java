package ru.mokrischev.vendingsupply.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mokrischev.vendingsupply.model.entity.FeedbackRequest;

@Repository
public interface FeedbackRequestRepository extends JpaRepository<FeedbackRequest, Long> {
}
