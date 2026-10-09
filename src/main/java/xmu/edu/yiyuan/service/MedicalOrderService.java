package xmu.edu.yiyuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xmu.edu.yiyuan.entity.MedicalOrder;
import xmu.edu.yiyuan.repository.MedicalOrderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MedicalOrderService {

    @Autowired
    private MedicalOrderRepository medicalOrderRepository;

    public Optional<MedicalOrder> findById(Long id) {
        return medicalOrderRepository.findById(id);
    }

    public List<MedicalOrder> findByHospitalizationId(Long hospitalizationId) {
        return medicalOrderRepository.findByHospitalizationId(hospitalizationId);
    }

    public List<MedicalOrder> findByPatientId(Long patientId) {
        return medicalOrderRepository.findByPatientId(patientId);
    }

    public List<MedicalOrder> findByDoctorId(Long doctorId) {
        return medicalOrderRepository.findByDoctorId(doctorId);
    }

    public List<MedicalOrder> findAll() {
        return medicalOrderRepository.findAll();
    }

    public MedicalOrder save(MedicalOrder medicalOrder) {
        return medicalOrderRepository.save(medicalOrder);
    }

    public void update(MedicalOrder medicalOrder) {
        medicalOrderRepository.update(medicalOrder);
    }

    public void deleteById(Long id) {
        medicalOrderRepository.deleteById(id);
    }

    /**
     * 患者出院时：将仍处「执行中」的用药、检查医嘱标记为已完成；
     * 同时将「申请出院」类其他医嘱一并关闭（与医生端办理出院原逻辑一致）。
     */
    public void completeActiveOrdersOnDischarge(Long hospitalizationId) {
        List<MedicalOrder> orders = medicalOrderRepository.findByHospitalizationId(hospitalizationId);
        LocalDateTime now = LocalDateTime.now();
        for (MedicalOrder o : orders) {
            if (o.getStatus() != MedicalOrder.OrderStatus.ACTIVE) {
                continue;
            }
            boolean dischargeRequestOther = o.getOrderType() == MedicalOrder.OrderType.OTHER
                    && o.getOrderContent() != null
                    && o.getOrderContent().contains("申请出院");
            boolean medOrExam = o.getOrderType() == MedicalOrder.OrderType.MEDICATION
                    || o.getOrderType() == MedicalOrder.OrderType.EXAMINATION;
            if (dischargeRequestOther || medOrExam) {
                o.setStatus(MedicalOrder.OrderStatus.COMPLETED);
                o.setUpdatedAt(now);
                medicalOrderRepository.update(o);
            }
        }
    }
}
