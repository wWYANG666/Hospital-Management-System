package xmu.edu.yiyuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xmu.edu.yiyuan.entity.Doctor;
import xmu.edu.yiyuan.repository.DoctorRepository;

import java.util.List;
import java.util.Optional;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    public Optional<Doctor> findById(Long id) {
        return doctorRepository.findById(id);
    }

    public Optional<Doctor> findByUserId(Long userId) {
        return doctorRepository.findByUserId(userId);
    }

    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }

    public Doctor save(Doctor doctor) {
        return doctorRepository.save(doctor);
    }

    public void update(Doctor doctor) {
        doctorRepository.update(doctor);
    }

    public void deleteById(Long id) {
        doctorRepository.deleteById(id);
    }
}
