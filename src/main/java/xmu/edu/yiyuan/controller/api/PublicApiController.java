package xmu.edu.yiyuan.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xmu.edu.yiyuan.dto.ApiResponse;
import xmu.edu.yiyuan.service.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/public")
public class PublicApiController {
    @Autowired private DepartmentService departments;
    @Autowired private DoctorService doctors;
    @Autowired private UserService users;
    @Autowired private ScheduleService schedules;

    @GetMapping("/directory")
    public ApiResponse<Object> directory() {
        List<Map<String, Object>> directory = new ArrayList<>();
        for (var doctor : doctors.findAll()) {
            var user = users.findById(doctor.getUserId()).orElse(null);
            if (user == null || !Integer.valueOf(1).equals(user.getStatus())) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", doctor.getId());
            item.put("name", user.getRealName());
            item.put("department", doctor.getDepartment());
            item.put("title", doctor.getTitle());
            item.put("specialty", doctor.getSpecialty());
            item.put("introduction", doctor.getIntroduction());
            item.put("schedules", schedules.findByDoctorId(doctor.getId()).stream()
                    .filter(s -> Integer.valueOf(1).equals(s.getStatus()) && s.getWorkDate() != null
                            && !s.getWorkDate().isBefore(LocalDate.now())
                            && !s.getWorkDate().isAfter(LocalDate.now().plusDays(14)))
                    .map(ApiSupport::sanitize).toList());
            directory.add(item);
        }
        return ApiResponse.success(Map.of("departments", ApiSupport.sanitize(departments.findAll()), "doctors", directory));
    }
}
