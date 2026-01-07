package ru.mokrischev.vendingsupply.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mokrischev.vendingsupply.model.entity.Employee;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByFranchiseeEmail(String email);

    List<Employee> findAllByMachinesContaining(ru.mokrischev.vendingsupply.model.entity.VendingMachine machine);
}
