package xmu.edu.yiyuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xmu.edu.yiyuan.entity.MedicalRecord;
import xmu.edu.yiyuan.repository.MedicalRecordRepository;

import java.util.List;
import java.util.Optional;

@Service
public class MedicalRecordService {

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    public Optional<MedicalRecord> findById(Long id) {
        return medicalRecordRepository.findById(id);
    }

    public Optional<MedicalRecord> findByAppointmentId(Long appointmentId) {
        return medicalRecordRepository.findByAppointmentId(appointmentId);
    }

    public List<MedicalRecord> findAllByAppointmentId(Long appointmentId) {
        return medicalRecordRepository.findAllByAppointmentId(appointmentId);
    }

    public List<MedicalRecord> findByPatientId(Long patientId) {
        return medicalRecordRepository.findByPatientId(patientId);
    }

    public List<MedicalRecord> findByDoctorId(Long doctorId) {
        return medicalRecordRepository.findByDoctorId(doctorId);
    }

    public List<MedicalRecord> findByHospitalizationId(Long hospitalizationId) {
        return medicalRecordRepository.findByHospitalizationId(hospitalizationId);
    }

    public List<MedicalRecord> findAll() {
        return medicalRecordRepository.findAll();
    }

    public MedicalRecord save(MedicalRecord medicalRecord) {
        return medicalRecordRepository.save(medicalRecord);
    }

    public void update(MedicalRecord medicalRecord) {
        medicalRecordRepository.update(medicalRecord);
    }

    public void deleteById(Long id) {
        medicalRecordRepository.deleteById(id);
    }
}
