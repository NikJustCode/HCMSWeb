package ru.mokrischev.vendingsupply.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mokrischev.vendingsupply.model.entity.ServiceReportPhoto;

@Repository
public interface ServiceReportPhotoRepository extends JpaRepository<ServiceReportPhoto, Long> {
}
