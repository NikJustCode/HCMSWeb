package ru.mokrischev.vendingsupply.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mokrischev.vendingsupply.model.entity.User;
import ru.mokrischev.vendingsupply.model.entity.VendingMachine;
import ru.mokrischev.vendingsupply.repository.UserRepository;
import ru.mokrischev.vendingsupply.repository.VendingMachineRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendingMachineService {

    private final VendingMachineRepository vendingMachineRepository;
    private final UserRepository userRepository;
    private final ru.mokrischev.vendingsupply.repository.EmployeeRepository employeeRepository;

    public List<VendingMachine> findAllByFranchisee(String email) {
        return vendingMachineRepository.findByFranchiseeEmailAndActiveTrue(email);
    }

    public void save(VendingMachine machine, String franchiseeEmail) {
        User franchisee = userRepository.findByEmail(franchiseeEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + franchiseeEmail));

        if (machine.getId() != null) {
            VendingMachine existing = vendingMachineRepository.findById(machine.getId()).orElse(null);
            if (existing != null && !existing.getFranchisee().getEmail().equals(franchiseeEmail)) {
                throw new RuntimeException("Access denied: You do not own this machine.");
            }
        }

        machine.setFranchisee(franchisee);
        vendingMachineRepository.save(machine);
    }

    public void delete(Long id, String franchiseeEmail) {
        VendingMachine machine = vendingMachineRepository.findById(id).orElse(null);
        if (machine != null && machine.getFranchisee().getEmail().equals(franchiseeEmail)) {
            // Soft delete logic
            // 1. Unlink from Employees
            List<ru.mokrischev.vendingsupply.model.entity.Employee> employees = employeeRepository
                    .findAllByMachinesContaining(machine);
            for (ru.mokrischev.vendingsupply.model.entity.Employee emp : employees) {
                emp.getMachines().remove(machine);
                employeeRepository.save(emp);
            }

            // 2. Set active false
            machine.setActive(false);
            vendingMachineRepository.save(machine);
        }
    }

    public VendingMachine findByIdAndFranchisee(Long id, String email) {
        VendingMachine machine = vendingMachineRepository.findById(id).orElse(null);
        if (machine != null && machine.getFranchisee().getEmail().equals(email) && machine.isActive()) {
            return machine;
        }
        return null;
    }
}
