package ru.mokrischev.vendingsupply.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mokrischev.vendingsupply.model.entity.ServiceReport;

import java.util.List;

@Repository
public interface ServiceReportRepository extends JpaRepository<ServiceReport, Long> {
    List<ServiceReport> findByEmployeeFranchiseeEmailOrderByServiceDateDesc(String email);
    List<ServiceReport> findByEmployeeFranchiseeEmailAndMachineIdOrderByServiceDateDesc(String email, Long machineId);
    
    // For mobile API
    List<ServiceReport> findByEmployeeIdOrderByServiceDateDesc(Long employeeId);
    List<ServiceReport> findByEmployeeIdAndMachineIdOrderByServiceDateDesc(Long employeeId, Long machineId);
    ServiceReport findTopByMachineIdOrderByServiceDateDesc(Long machineId);
}
