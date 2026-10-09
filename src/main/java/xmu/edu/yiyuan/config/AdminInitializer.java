package xmu.edu.yiyuan.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import xmu.edu.yiyuan.entity.User;
import xmu.edu.yiyuan.service.UserService;

/**
 * 管理员账号初始化器
 * 应用启动时检查并创建唯一的管理员账号
 */
@Component
@ConditionalOnProperty(name = "hospital.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${hospital.admin.username:admin}")
    private String adminUsername;

    @Value("${hospital.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        if (adminPassword == null || adminPassword.isBlank()) {
            System.out.println("未配置 hospital.admin.password，跳过默认管理员初始化。");
            return;
        }

        // 检查是否已存在管理员账号
        if (userService.findByUsername(adminUsername).isEmpty()) {
            // 检查是否已有其他管理员
            if (userService.findByRole(User.Role.ADMIN).isEmpty()) {
                // 创建唯一的管理员账号
                User admin = new User();
                admin.setUsername(adminUsername);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRealName("系统管理员");
                admin.setRole(User.Role.ADMIN);
                admin.setStatus(1);

                userService.save(admin);
                System.out.println("========================================");
                System.out.println("管理员账号已创建！");
                System.out.println("用户名: " + adminUsername);
                System.out.println("请使用环境变量 HOSPITAL_ADMIN_PASSWORD 管理初始密码。");
                System.out.println("========================================");
            } else {
                System.out.println("管理员账号已存在，跳过初始化。");
            }
        } else {
            System.out.println("管理员账号已存在。");
        }
    }
}
