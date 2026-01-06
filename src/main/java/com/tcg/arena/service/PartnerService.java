package com.tcg.arena.service;

import com.tcg.arena.config.CacheConfig;
import com.tcg.arena.model.Partner;
import com.tcg.arena.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PartnerService {

    @Autowired
    private PartnerRepository partnerRepository;

    @Cacheable(value = CacheConfig.PARTNERS_CACHE, key = "'active'")
    public List<Partner> getAllActivePartners() {
        return partnerRepository.findByIsActiveTrue();
    }

    public List<Partner> getAllPartners() {
        return partnerRepository.findAll();
    }

    @Cacheable(value = CacheConfig.PARTNER_BY_ID_CACHE, key = "#id")
    public Optional<Partner> getPartnerById(Long id) {
        return partnerRepository.findById(id);
    }

    @CacheEvict(value = {CacheConfig.PARTNERS_CACHE, CacheConfig.PARTNER_BY_ID_CACHE}, allEntries = true)
    public Partner savePartner(Partner partner) {
        return partnerRepository.save(partner);
    }

    @CacheEvict(value = {CacheConfig.PARTNERS_CACHE, CacheConfig.PARTNER_BY_ID_CACHE}, allEntries = true)
    public void deletePartner(Long id) {
        partnerRepository.deleteById(id);
    }
}
