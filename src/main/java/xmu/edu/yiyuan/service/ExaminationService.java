package xmu.edu.yiyuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xmu.edu.yiyuan.entity.Examination;
import xmu.edu.yiyuan.repository.ExaminationRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ExaminationService {

    @Autowired
    private ExaminationRepository examinationRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

    public Optional<Examination> findById(Long id) {
        return examinationRepository.findById(id);
    }

    public List<Examination> findAll() {
        return examinationRepository.findAll();
    }

    /**
     * 查询指定科室可用的检查项目；如果 departmentId 为空，则返回全部
     * 同时包含全院通用（departmentId 为 null）的项目
     */
    public List<Examination> findByDepartmentOrCommon(Long departmentId) {
        if (departmentId == null) {
            // 医生端只应看到启用(status=1)的项目
            return examinationRepository.findAvailableExaminations();
        }
        return examinationRepository.findByDepartmentIdOrDepartmentIdIsNull(departmentId);
    }

    public List<Examination> findAvailableExaminations() {
        return examinationRepository.findAvailableExaminations();
    }

    public Examination save(Examination examination) {
        validate(examination);
        return examinationRepository.save(examination);
    }

    public void update(Examination examination) {
        validate(examination);
        if (findById(examination.getId()).isEmpty()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "检查项目不存在");
        examinationRepository.update(examination);
    }

    public void deleteById(Long id) {
        examinationRepository.deleteById(id);
    }

    public void resetAutoIncrementIfEmpty() {
        examinationRepository.resetAutoIncrementIfEmpty();
    }

    private void validate(Examination examination) {
        BusinessValidation.text(examination.getName(), 100, "检查名称");
        BusinessValidation.text(examination.getType(), 50, "检查类型");
        BusinessValidation.money(examination.getPrice(), "检查费用");
        BusinessValidation.require(examination.getStatus() != null && (examination.getStatus() == 0 || examination.getStatus() == 1), "检查状态无效");
        if (examination.getDepartmentId() != null) BusinessValidation.require(jdbc.queryForObject("SELECT COUNT(*) FROM department WHERE id=?", Integer.class, examination.getDepartmentId()) == 1, "科室不存在");
    }
}
