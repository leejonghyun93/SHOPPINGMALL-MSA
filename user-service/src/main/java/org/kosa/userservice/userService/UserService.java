package org.kosa.userservice.userService;

import lombok.RequiredArgsConstructor;
import org.kosa.userservice.entity.User;
import org.kosa.userservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    // 사용자 저장
    public User saveUser(User user) {
        user.setPasswd(passwordEncoder.encode(user.getPasswd())); // 비밀번호 암호화
        return userRepository.save(user);
    }
    // 사용자 ID로 조회
    public Optional<User> getUserById(String userid) {
        return userRepository.findByUserid(userid);
    }

    // 모든 사용자 목록 조회
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 사용자 존재 여부 확인
    public boolean isUserExists(String userid) {
        return userRepository.existsByUserid(userid);
    }

    // 사용자 삭제
    public void deleteUser(String userid) {
        userRepository.deleteById(userid);
    }

    public Map<String, String> getNicknameMapByUserIds(List<String> userIds) {
        Map<String, String> nicknameMap = new HashMap<>();

        for (String id : userIds) {
            userRepository.findById(id).ifPresent(user -> {
                nicknameMap.put(id, user.getNickname());
            });
        }

        return nicknameMap;
    }

}
