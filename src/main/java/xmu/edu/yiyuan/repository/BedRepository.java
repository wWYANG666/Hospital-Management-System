package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.Bed;
import java.util.List;
import java.util.Optional;

public interface BedRepository {
    Optional<Bed> findById(Long id);
    List<Bed> findAll();
    List<Bed> findByStatus(Bed.BedStatus status);
    List<Bed> findAvailableBeds();
    List<Bed> findByDepartmentId(Long departmentId);
    Bed save(Bed bed);
    void update(Bed bed);
    void deleteById(Long id);
}
