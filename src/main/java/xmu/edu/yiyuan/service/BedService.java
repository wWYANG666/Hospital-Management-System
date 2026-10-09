package xmu.edu.yiyuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import xmu.edu.yiyuan.entity.Bed;
import xmu.edu.yiyuan.repository.BedRepository;

import java.util.List;
import java.util.Optional;

@Service
public class BedService {

    @Autowired
    private BedRepository bedRepository;
    @Autowired private JdbcTemplate jdbc;

    public Optional<Bed> findById(Long id) {
        return bedRepository.findById(id);
    }

    public List<Bed> findAll() {
        return bedRepository.findAll();
    }

    public List<Bed> findByStatus(Bed.BedStatus status) {
        return bedRepository.findByStatus(status);
    }

    public List<Bed> findAvailableBeds() {
        return bedRepository.findAvailableBeds();
    }

    public List<Bed> findByDepartmentId(Long departmentId) {
        return bedRepository.findByDepartmentId(departmentId);
    }

    public Bed save(Bed bed) {
        validate(bed);
        if (bed.getId() == null && bed.getStatus() == Bed.BedStatus.OCCUPIED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新床位不能直接设为占用，请通过住院分床操作");
        }
        return bedRepository.save(bed);
    }

    @Transactional
    public void update(Bed bed) {
        validate(bed);
        if (jdbc.queryForList("SELECT id FROM bed WHERE id=? FOR UPDATE", Long.class, bed.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "床位不存在");
        }
        Integer occupants = jdbc.queryForObject("SELECT COUNT(*) FROM hospitalization WHERE bed_id=? AND status='ADMITTED' AND (request_status IS NULL OR request_status<>'REJECTED')", Integer.class, bed.getId());
        if (occupants != null && occupants > 0 && bed.getStatus() != Bed.BedStatus.OCCUPIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "床位有在院患者，不能修改为非占用状态");
        }
        bedRepository.update(bed);
    }

    @Transactional
    public void deleteById(Long id) {
        if (jdbc.queryForList("SELECT id FROM bed WHERE id=? FOR UPDATE", Long.class, id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "床位不存在");
        }
        Bed bed = findById(id).orElseThrow();
        Integer occupants = jdbc.queryForObject("SELECT COUNT(*) FROM hospitalization WHERE bed_id=? AND status='ADMITTED' AND (request_status IS NULL OR request_status<>'REJECTED')", Integer.class, id);
        if (bed.getStatus() == Bed.BedStatus.OCCUPIED || occupants != null && occupants > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "床位正在使用，不能删除");
        }
        bedRepository.deleteById(id);
    }

    private void validate(Bed bed) {
        BusinessValidation.text(bed.getBedNumber(), 50, "床位编号");
        BusinessValidation.money(bed.getPricePerDay(), "床位日费");
        BusinessValidation.optional(bed.getRoomNumber(), 50, "房间编号");
        BusinessValidation.optional(bed.getWard(), 50, "病区");
        if (bed.getDepartmentId() != null) BusinessValidation.require(
                jdbc.queryForObject("SELECT COUNT(*) FROM department WHERE id=?", Integer.class, bed.getDepartmentId()) == 1, "科室不存在");
    }
}
