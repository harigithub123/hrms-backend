package com.hrms.org;

import com.hrms.org.entity.Designation;
import com.hrms.org.repository.DesignationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DesignationService {

    private final DesignationRepository repository;

    public DesignationService(DesignationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<DesignationDto> findAll() {
        return repository.findAll().stream().map(DesignationDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DesignationDto getById(Long id) {
        return repository.findById(id).map(DesignationDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Designation not found: " + id));
    }

    @Transactional
    public DesignationDto create(DesignationRequest req) {
        Designation e = new Designation();
        e.setName(req.name());
        e.setCode(req.code());
        return DesignationDto.from(repository.save(e));
    }

    @Transactional
    public DesignationDto update(Long id, DesignationRequest req) {
        Designation e = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Designation not found: " + id));
        e.setName(req.name());
        e.setCode(req.code());
        return DesignationDto.from(repository.save(e));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new IllegalArgumentException("Designation not found: " + id);
        repository.deleteById(id);
    }
}
