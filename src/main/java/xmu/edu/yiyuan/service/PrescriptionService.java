package xmu.edu.yiyuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xmu.edu.yiyuan.entity.Prescription;
import xmu.edu.yiyuan.entity.PrescriptionItem;
import xmu.edu.yiyuan.repository.PrescriptionRepository;
import xmu.edu.yiyuan.repository.PrescriptionItemRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PrescriptionService {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    public Optional<Prescription> findById(Long id) {
        return prescriptionRepository.findById(id);
    }

    public List<Prescription> findAll() {
        return prescriptionRepository.findAll();
    }

    public List<Prescription> findByPatientId(Long patientId) {
        return prescriptionRepository.findByPatientId(patientId);
    }

    public List<Prescription> findByPatientIdAndStatus(Long patientId, Prescription.PrescriptionStatus status) {
        return prescriptionRepository.findByPatientIdAndStatus(patientId, status);
    }

    public List<Prescription> findByAppointmentId(Long appointmentId) {
        return prescriptionRepository.findByAppointmentId(appointmentId);
    }

    public List<Prescription> findByStatus(Prescription.PrescriptionStatus status) {
        return prescriptionRepository.findByStatus(status);
    }

    public List<Prescription> findByPrescriptionNumber(String prescriptionNumber) {
        return prescriptionRepository.findByPrescriptionNumber(prescriptionNumber);
    }

    public Prescription save(Prescription prescription) {
        return prescriptionRepository.save(prescription);
    }

    public void update(Prescription prescription) {
        prescriptionRepository.update(prescription);
    }

    public void deleteById(Long id) {
        prescriptionRepository.deleteById(id);
    }

    public List<PrescriptionItem> getPrescriptionItems(Long prescriptionId) {
        return prescriptionItemRepository.findByPrescriptionId(prescriptionId);
    }

    public PrescriptionItem savePrescriptionItem(PrescriptionItem item) {
        return prescriptionItemRepository.save(item);
    }
}
