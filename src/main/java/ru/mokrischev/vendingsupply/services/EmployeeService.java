package ru.mokrischev.vendingsupply.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.mokrischev.vendingsupply.model.entity.Employee;
import ru.mokrischev.vendingsupply.model.entity.User;
import ru.mokrischev.vendingsupply.model.entity.VendingMachine;
import ru.mokrischev.vendingsupply.repository.EmployeeRepository;
import ru.mokrischev.vendingsupply.repository.UserRepository;
import ru.mokrischev.vendingsupply.repository.VendingMachineRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final VendingMachineRepository vendingMachineRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Employee> findByFranchisee(String email) {
        return employeeRepository.findByFranchiseeEmail(email); // Assuming this method exists or needs creation
    }

    public Employee findByIdAndFranchisee(Long id, String email) {
        // Need to check specific repo method or custom check
        // EmployeeRepository usually doesn't have complex checks out of box unless
        // defined
        // For simplicity:
        Employee employee = employeeRepository.findById(id).orElse(null);
        if (employee != null && employee.getFranchisee().getEmail().equals(email)) {
            return employee;
        }
        return null;
    }

    @Transactional
    public void save(Employee employee, List<Long> machineIds, String franchiseeEmail) {
        User franchisee = userRepository.findByEmail(franchiseeEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + franchiseeEmail));

        employee.setFranchisee(franchisee);

        if (employee.getPassword() != null && !employee.getPassword().isEmpty()) {
            employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        } else if (employee.getId() != null) {
            // Keep old password if it's an update and field is empty
            Employee existing = employeeRepository.findById(employee.getId()).orElse(null);
            if (existing != null) {
                employee.setPassword(existing.getPassword());
            }
        }

        if (machineIds != null) {
            List<VendingMachine> machines = vendingMachineRepository.findAllById(machineIds);
            // Security check: ensure all machines belong to this franchisee
            machines.removeIf(m -> !m.getFranchisee().getEmail().equals(franchiseeEmail));
            employee.setMachines(machines);
        }

        employeeRepository.save(employee);
    }

    @Transactional
    public void delete(Long id, String franchiseeEmail) {
        Employee employee = findByIdAndFranchisee(id, franchiseeEmail);
        if (employee != null) {
            employeeRepository.delete(employee);
        }
    }
}
