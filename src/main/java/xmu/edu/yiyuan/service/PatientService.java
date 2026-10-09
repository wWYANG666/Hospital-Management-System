package xmu.edu.yiyuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xmu.edu.yiyuan.entity.Patient;
import xmu.edu.yiyuan.repository.PatientRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    public Optional<Patient> findById(Long id) {
        return patientRepository.findById(id);
    }

    public Optional<Patient> findByUserId(Long userId) {
        return patientRepository.findByUserId(userId);
    }

    public List<Patient> findAll() {
        return patientRepository.findAll();
    }

    public Patient save(Patient patient) {
        return patientRepository.save(patient);
    }

    public void update(Patient patient) {
        patientRepository.update(patient);
    }

    public void deleteById(Long id) {
        patientRepository.deleteById(id);
    }
}
