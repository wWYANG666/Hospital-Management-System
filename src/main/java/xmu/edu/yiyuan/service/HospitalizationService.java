package xmu.edu.yiyuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import xmu.edu.yiyuan.entity.Bed;
import xmu.edu.yiyuan.entity.Hospitalization;
import xmu.edu.yiyuan.repository.HospitalizationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class HospitalizationService {

    @Autowired
    private HospitalizationRepository hospitalizationRepository;

    @Autowired
    private BedService bedService;

    @Autowired private JdbcTemplate jdbc;

    public Optional<Hospitalization> findById(Long id) {
        return hospitalizationRepository.findById(id);
    }

    public List<Hospitalization> findByPatientId(Long patientId) {
        return hospitalizationRepository.findByPatientId(patientId);
    }

    public List<Hospitalization> findByDoctorId(Long doctorId) {
        return hospitalizationRepository.findByDoctorId(doctorId);
    }

    public List<Hospitalization> findByBedId(Long bedId) {
        return hospitalizationRepository.findByBedId(bedId);
    }

    public List<Hospitalization> findAll() {
        return hospitalizationRepository.findAll();
    }

    @Transactional
    public Hospitalization save(Hospitalization hospitalization) {
        if (hospitalization.getId() == null && hospitalization.getAppointmentId() != null) {
            BusinessValidation.text(hospitalization.getAdmissionReason(), 4000, "住院原因");
            BusinessValidation.require(hospitalization.getExpectedDays() != null && hospitalization.getExpectedDays() >= 1 && hospitalization.getExpectedDays() <= 365, "预计住院天数必须为1至365");
            BusinessValidation.require(hospitalization.getRequestDepartmentId() != null && jdbc.queryForObject("SELECT COUNT(*) FROM department WHERE id=?", Integer.class, hospitalization.getRequestDepartmentId()) == 1, "申请科室不存在");
            jdbc.queryForList("SELECT id FROM appointment WHERE id=? FOR UPDATE", Long.class, hospitalization.getAppointmentId());
            Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM hospitalization WHERE appointment_id=? "
                    + "AND status='ADMITTED' AND (request_status IS NULL OR request_status<>'REJECTED')",
                    Integer.class, hospitalization.getAppointmentId());
            if (existing != null && existing > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "该挂号已有有效住院记录");
        }
        return hospitalizationRepository.save(hospitalization);
    }

    public void update(Hospitalization hospitalization) {
        hospitalizationRepository.update(hospitalization);
    }

    public void deleteById(Long id) {
        hospitalizationRepository.deleteById(id);
    }

    public Hospitalization lock(Long id) {
        if (jdbc.queryForList("SELECT id FROM hospitalization WHERE id=? FOR UPDATE", Long.class, id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "住院记录不存在");
        }
        return hospitalizationRepository.findById(id).orElseThrow();
    }

    public void requireActiveCare(Long id) {
        Hospitalization h = lock(id);
        if (h.getStatus() != Hospitalization.HospitalizationStatus.ADMITTED || h.getBedId() == null
                || h.getRequestStatus() != Hospitalization.RequestStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "请先完成床位分配，且只能操作当前在院患者");
        }
    }

    @Transactional
    public void assignBed(Long id, Long bedId) {
        Hospitalization h = lock(id);
        if (h.getStatus() != Hospitalization.HospitalizationStatus.ADMITTED
                || h.getRequestStatus() == Hospitalization.RequestStatus.REJECTED || Boolean.TRUE.equals(h.getPaid())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该住院记录不能分配床位");
        }
        if (h.getBedId() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "该住院记录已分配床位，不能重复分配");
        if (jdbc.queryForList("SELECT id FROM bed WHERE id=? FOR UPDATE", Long.class, bedId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "床位不存在");
        }
        Bed bed = bedService.findById(bedId).orElseThrow();
        Integer occupants = jdbc.queryForObject("SELECT COUNT(*) FROM hospitalization WHERE bed_id=? AND status='ADMITTED' "
                + "AND (request_status IS NULL OR request_status<>'REJECTED')", Integer.class, bedId);
        if (bed.getStatus() != Bed.BedStatus.AVAILABLE || occupants == null || occupants > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "床位已被使用，请选择其他床位");
        }
        h.setBedId(bedId);
        h.setRequestStatus(Hospitalization.RequestStatus.APPROVED);
        h.setAdmissionDate(LocalDate.now());
        hospitalizationRepository.update(h);
        bed.setStatus(Bed.BedStatus.OCCUPIED);
        bedService.update(bed);
    }

    public void addOrderCost(Long id, Long orderId, BigDecimal cost) {
        requireActiveCare(id);
        if (cost == null || cost.signum() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "医嘱费用无效");
        jdbc.update("UPDATE medical_order SET cost=? WHERE id=? AND hospitalization_id=?", cost, orderId, id);
        jdbc.update("UPDATE hospitalization SET order_cost=COALESCE(order_cost,total_cost,0)+?, "
                + "total_cost=COALESCE(total_cost,0)+? WHERE id=? AND settled_at IS NULL", cost, cost, id);
    }

    public java.util.Map<String, Object> settlementDetails(Long id) {
        Hospitalization h = findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "住院记录不存在"));
        java.util.Map<String, Object> snapshot = jdbc.queryForMap("SELECT order_cost,bed_cost,settled_at FROM hospitalization WHERE id=?", id);
        BigDecimal orderCost = snapshot.get("order_cost") instanceof BigDecimal value ? value :
                (h.getTotalCost() == null ? BigDecimal.ZERO : h.getTotalCost());
        BigDecimal bedCost = snapshot.get("bed_cost") instanceof BigDecimal value ? value : bedCost(h);
        BigDecimal total = snapshot.get("settled_at") == null ? bedCost.add(orderCost) : h.getTotalCost();
        if (snapshot.get("settled_at") != null && snapshot.get("bed_cost") == null) {
            // Old discharged rows have an immutable total but no trustworthy breakdown.
            bedCost = BigDecimal.ZERO;
            orderCost = total;
        }
        BigDecimal medicines = jdbc.queryForObject("SELECT COALESCE(SUM(cost),0) FROM medical_order WHERE hospitalization_id=? AND order_type='MEDICATION' AND status<>'CANCELLED'", BigDecimal.class, id);
        BigDecimal examinations = jdbc.queryForObject("SELECT COALESCE(SUM(cost),0) FROM medical_order WHERE hospitalization_id=? AND order_type='EXAMINATION' AND status<>'CANCELLED'", BigDecimal.class, id);
        return java.util.Map.of("totalCost", total, "bedCost", bedCost, "medicineCost", medicines,
                "examinationCost", examinations, "otherCost", orderCost.subtract(medicines).subtract(examinations),
                "settled", snapshot.get("settled_at") != null);
    }

    private BigDecimal bedCost(Hospitalization h) {
        Bed bed = h.getBedId() == null ? null : bedService.findById(h.getBedId()).orElse(null);
        if (bed == null || bed.getPricePerDay() == null || h.getAdmissionDate() == null) return BigDecimal.ZERO;
        long days = Math.max(0, ChronoUnit.DAYS.between(h.getAdmissionDate(), h.getDischargeDate() == null ? LocalDate.now() : h.getDischargeDate()));
        if (bed.getPricePerDay().signum() < 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "床位收费无效，请先修正床位价格");
        return bed.getPricePerDay().multiply(BigDecimal.valueOf(days));
    }

    /**
     * 已出院记录自动结算：按床位日价 ×（出院日 − 入院日）写入 totalCost，并释放床位。
     * 与管理员手动结算规则一致，供医生办理出院后自动触发。
     */
    @Transactional
    public void settleAndReleaseBedAfterDischarge(Long hospitalizationId) {
        Hospitalization hospitalization = lock(hospitalizationId);
        if (hospitalization.getStatus() != Hospitalization.HospitalizationStatus.DISCHARGED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只能结算已出院的住院记录");
        }
        if (jdbc.queryForMap("SELECT settled_at FROM hospitalization WHERE id=?", hospitalizationId).get("settled_at") != null) return;
        BigDecimal bedCost = bedCost(hospitalization);
        BigDecimal orderCost = jdbc.queryForObject("SELECT COALESCE(order_cost,total_cost,0) FROM hospitalization WHERE id=?", BigDecimal.class, hospitalizationId);
        jdbc.update("UPDATE hospitalization SET total_cost=?,order_cost=?,bed_cost=?,settled_at=NOW() WHERE id=?",
                bedCost.add(orderCost), orderCost, bedCost, hospitalizationId);
        if (hospitalization.getBedId() != null) {
            jdbc.queryForList("SELECT id FROM bed WHERE id=? FOR UPDATE", Long.class, hospitalization.getBedId());
            Integer others = jdbc.queryForObject("SELECT COUNT(*) FROM hospitalization WHERE bed_id=? AND id<>? "
                    + "AND status='ADMITTED' AND (request_status IS NULL OR request_status<>'REJECTED')",
                    Integer.class, hospitalization.getBedId(), hospitalizationId);
            if (others != null && others == 0) jdbc.update("UPDATE bed SET status='AVAILABLE' WHERE id=? AND status='OCCUPIED'", hospitalization.getBedId());
        }
    }
}
