package xmu.edu.yiyuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xmu.edu.yiyuan.entity.Report;
import xmu.edu.yiyuan.repository.ReportRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private ExaminationService examinationService;

    /**
     * 待出报告且患者仍需缴费（含 paid==null 但检查项目单价&gt;0 的推断，兼容未执行库迁移的情况）
     */
    public boolean needsPatientPayment(Report report) {
        if (report == null || report.getStatus() != Report.ReportStatus.PENDING) {
            return false;
        }
        if (Boolean.TRUE.equals(report.getPaid())) {
            return false;
        }
        if (Boolean.FALSE.equals(report.getPaid())) {
            return true;
        }
        if (report.getExaminationId() == null) {
            return false;
        }
        return examinationService.findById(report.getExaminationId())
                .map(ex -> ex.getPrice() != null && ex.getPrice().compareTo(BigDecimal.ZERO) > 0)
                .orElse(false);
    }

    /** 医生是否可审阅填写（已缴费或免缴费） */
    public boolean canDoctorReviewReport(Report report) {
        return !needsPatientPayment(report);
    }

    public Optional<Report> findById(Long id) {
        return reportRepository.findById(id);
    }

    public List<Report> findByPatientId(Long patientId) {
        return reportRepository.findByPatientId(patientId);
    }

    public List<Report> findByDoctorId(Long doctorId) {
        return reportRepository.findByDoctorId(doctorId);
    }

    public List<Report> findByAppointmentId(Long appointmentId) {
        return reportRepository.findByAppointmentId(appointmentId);
    }

    public List<Report> findAll() {
        return reportRepository.findAll();
    }

    public Report save(Report report) {
        return reportRepository.save(report);
    }

    public void update(Report report) {
        reportRepository.update(report);
    }

    public void deleteById(Long id) {
        // 删除报告单时，同步清理对应的电子病历（medical_record）数据，避免残留
        Report report = reportRepository.findById(id).orElse(null);
        if (report != null && report.getAppointmentId() != null) {
            medicalRecordService.findByAppointmentId(report.getAppointmentId())
                    .ifPresent(mr -> medicalRecordService.deleteById(mr.getId()));
        }
        reportRepository.deleteById(id);
    }

    /**
     * 仅删除 {@code report} 表记录，不触碰 {@code medical_record}。
     * <p>用于：重新诊断时患者撤掉某项检查、删除「待出且未缴费」报告等；与 {@link #deleteById(Long)}（会级联删病历）区分。
     */
    public void deleteReportOnly(Long id) {
        if (id == null) {
            return;
        }
        reportRepository.deleteById(id);
    }
}
