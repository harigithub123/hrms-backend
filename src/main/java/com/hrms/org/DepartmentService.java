package com.hrms.org;

import com.hrms.org.entity.Department;
import com.hrms.org.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    private final DepartmentRepository repository;

    public DepartmentService(DepartmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> findAll() {
        return repository.findAll().stream().map(DepartmentDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentDto getById(Long id) {
        return repository.findById(id).map(DepartmentDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
    }

    @Transactional
    public DepartmentDto create(DepartmentRequest req) {
        Department e = new Department();
        e.setName(req.name());
        e.setCode(req.code());
        e.setDescription(req.description());
        return DepartmentDto.from(repository.save(e));
    }

    @Transactional
    public DepartmentDto update(Long id, DepartmentRequest req) {
        Department e = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
        e.setName(req.name());
        e.setCode(req.code());
        e.setDescription(req.description());
        return DepartmentDto.from(repository.save(e));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new IllegalArgumentException("Department not found: " + id);
        repository.deleteById(id);
    }
}
