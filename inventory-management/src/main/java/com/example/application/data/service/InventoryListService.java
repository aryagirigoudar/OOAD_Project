package com.example.application.data.service;

import com.example.application.data.entity.InventoryList;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class InventoryListService {

    private final InventoryListRepository repository;

    public InventoryListService(InventoryListRepository repository) {
        this.repository = repository;
    }

    public Optional<InventoryList> get(Long id) {
        return repository.findById(id);
    }

    public InventoryList update(InventoryList entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<InventoryList> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<InventoryList> list(Pageable pageable, Specification<InventoryList> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
