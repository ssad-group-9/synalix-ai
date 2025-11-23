package ai.synalix.synalixai;

import ai.synalix.synalixai.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CreateAdminCommandLineRunner implements CommandLineRunner {

    private final UserService userService;

    @Value("${app.admin.username}")
    private String username;

    @Value("${app.admin.password}")
    private String password;

    @Value("${app.admin.nickname}")
    private String nickname;

    @Value("${app.admin.email}")
    private String email;

    @Autowired
    public CreateAdminCommandLineRunner(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        userService.createOrUpdateInitialAdmin(username, password, nickname, email);
    }
}
