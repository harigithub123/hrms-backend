package com.hrms.leave;

import com.hrms.leave.dto.LeaveTypeDto;
import com.hrms.leave.dto.LeaveTypeRequest;
import com.hrms.leave.entity.LeaveType;
import com.hrms.leave.repository.LeaveTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveTypeService(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeDto> findAll() {
        return leaveTypeRepository.findAll().stream().map(LeaveTypeDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeDto> findAllActive() {
        return leaveTypeRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(LeaveTypeDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LeaveTypeDto get(Long id) {
        return LeaveTypeDto.from(leaveTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found: " + id)));
    }

    @Transactional
    public LeaveTypeDto create(LeaveTypeRequest req) {
        if (leaveTypeRepository.existsByCodeIgnoreCase(req.code())) {
            throw new IllegalArgumentException("Leave type code already exists: " + req.code());
        }
        LeaveType t = new LeaveType();
        map(req, t);
        return LeaveTypeDto.from(leaveTypeRepository.save(t));
    }

    @Transactional
    public LeaveTypeDto update(Long id, LeaveTypeRequest req) {
        LeaveType t = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found: " + id));
        leaveTypeRepository.findByCodeIgnoreCase(req.code()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new IllegalArgumentException("Leave type code already exists: " + req.code());
            }
        });
        map(req, t);
        return LeaveTypeDto.from(leaveTypeRepository.save(t));
    }

    @Transactional
    public void delete(Long id) {
        if (!leaveTypeRepository.existsById(id)) {
            throw new IllegalArgumentException("Leave type not found: " + id);
        }
        leaveTypeRepository.deleteById(id);
    }

    private void map(LeaveTypeRequest req, LeaveType t) {
        t.setName(req.name().trim());
        t.setCode(req.code().trim().toUpperCase());
        t.setDaysPerYear(req.daysPerYear());
        t.setCarryForward(req.carryForward());
        t.setPaid(req.paid());
        t.setActive(req.active());
    }
}
