package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    List<User> findByRole(User.Role role);
    User save(User user);
    void update(User user);
    void deleteById(Long id);
}
