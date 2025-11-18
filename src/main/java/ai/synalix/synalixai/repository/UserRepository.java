package ai.synalix.synalixai.repository;

import ai.synalix.synalixai.entity.User;
import ai.synalix.synalixai.enums.UserRole;
import ai.synalix.synalixai.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户数据访问层接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据角色查找用户
     */
    List<User> findByRole(UserRole role);

    /**
     * 根据状态查找用户
     */
    List<User> findByStatus(UserStatus status);

    /**
     * 根据角色和状态查找用户
     */
    List<User> findByRoleAndStatus(UserRole role, UserStatus status);

    /**
     * 查找所有管理员用户
     */
    @Query("SELECT u FROM User u WHERE u.role = :role ORDER BY u.createdAt")
    List<User> findAllAdmins(@Param("role") UserRole role);

    /**
     * 统计指定角色的用户数量
     */
    long countByRole(UserRole role);

    /**
     * 统计启用状态的用户数量
     */
    long countByStatus(UserStatus status);

    /**
     * 批量更新用户状态
     */
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id IN :ids")
    int updateStatusByIds(@Param("status") UserStatus status, @Param("ids") List<UUID> ids);
}