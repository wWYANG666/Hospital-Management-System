package xmu.edu.yiyuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmu.edu.yiyuan.entity.User;
import xmu.edu.yiyuan.entity.Doctor;
import xmu.edu.yiyuan.entity.Patient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
public class RegisterService {

    @Autowired
    private UserService userService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private PatientService patientService;

    /**
     * 注册患者
     */
    @Transactional
    public void registerPatient(User user, Patient patient) {
        // 检查用户名是否已存在
        if (userService.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }

        // 保存用户信息
        User savedUser = userService.save(user);

        // 生成患者编号（格式：P + 日期 + 随机数）
        if (patient.getPatientCode() == null || patient.getPatientCode().isEmpty()) {
            String patientCode = generatePatientCode();
            patient.setPatientCode(patientCode);
        }

        // 设置用户ID
        patient.setUserId(savedUser.getId());

        // 保存患者信息
        patientService.save(patient);
    }

    /**
     * 注册医生
     */
    @Transactional
    public void registerDoctor(User user, Doctor doctor) {
        // 检查用户名是否已存在
        if (userService.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }

        // 科室必填
        if (doctor.getDepartment() == null || doctor.getDepartment().trim().isEmpty()) {
            throw new RuntimeException("科室不能为空");
        }
        doctor.setDepartment(doctor.getDepartment().trim());

        // 保存用户信息
        User savedUser = userService.save(user);

        // 生成医生工号（格式：D + 日期 + 随机数）
        if (doctor.getDoctorCode() == null || doctor.getDoctorCode().isEmpty()) {
            String doctorCode = generateDoctorCode();
            doctor.setDoctorCode(doctorCode);
        }

        // 设置用户ID
        doctor.setUserId(savedUser.getId());

        // 保存医生信息
        doctorService.save(doctor);
    }

    /**
     * 生成患者编号
     */
    private String generatePatientCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%04d", new Random().nextInt(10000));
        return "P" + dateStr + randomStr;
    }

    /**
     * 生成医生工号
     */
    private String generateDoctorCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%04d", new Random().nextInt(10000));
        return "D" + dateStr + randomStr;
    }
}
