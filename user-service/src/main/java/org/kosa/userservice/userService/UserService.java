package org.kosa.userservice.userService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.dto.PageDto;
import org.kosa.userservice.dto.PageRequestDto;
import org.kosa.userservice.dto.UserDto;
import org.kosa.userservice.dto.UserResponseDto;
import org.kosa.userservice.entity.User;
import org.kosa.userservice.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    // 사용자 저장
    public User saveUser(User user) {
        user.setPasswd(passwordEncoder.encode(user.getPasswd())); // 비밀번호 암호화
        return userRepository.save(user);
    }
    // 사용자 ID로 조회

    public Optional<UserResponseDto> getUserDetail(String userid) {
        if (userid == null || userid.trim().isEmpty()) {
            return Optional.empty();
        }

        return userRepository.findByUserid(userid.trim())
                .map(this::toDto);
    }

    // 모든 사용자 목록 조회

    public PageDto<UserResponseDto> list(PageRequestDto requestDto) {
        // 정렬 컬럼 없으면 기본 name, 오름차순 정렬
        Sort sort = Sort.by(Sort.Direction.ASC,
                requestDto.getSortBy() != null ? requestDto.getSortBy() : "name");

        Pageable pageable = PageRequest.of(requestDto.getPage() - 1, requestDto.getSize(), sort);

        Page<User> userPage = userRepository.searchUsers(requestDto.getSearchValue(), pageable);

        // Entity -> DTO 변환
        var userDtoList = userPage.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new PageDto<>(
                userDtoList,
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                requestDto.getPage(),
                requestDto.getSize()
        );
    }

    private UserResponseDto toDto(User user) {
        return UserResponseDto.builder()
                .userid(user.getUserid())
                .name(user.getName())
                .passwd(user.getPasswd())
                .email(user.getEmail())
                .age(user.getAge())
                .role(user.getRole())
                .regDate(user.getRegDate())
                .address(user.getAddress())
                .detailAddress(user.getDetailAddress())
                .fullAddress(user.getFullAddress())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .loginTime(user.getLoginTime())
                .accountLocked(user.getAccountLocked())
                .build();
    }
    @Transactional
    public void increaseLoginFailCount(String userid) {
        User user = userRepository.findById(userid)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            throw new RuntimeException("계정이 잠겼습니다.");
        }

        int failCount = user.getLoginFailCount() + 1;
        user.setLoginFailCount(failCount);

        if (failCount >= 5) {
            user.setAccountLocked(true);
            userRepository.save(user);
            // 잠겼다는 예외를 여기서 던지면 클라이언트가 받을 수 있습니다.
            throw new RuntimeException("계정이 5회 이상 로그인 실패로 잠겼습니다.");
        }
        userRepository.save(user);
    }

    @Transactional
    public void resetLoginFailCount(String userid) {
        User user = userRepository.findById(userid)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        user.setLoginFailCount(0);
        user.setAccountLocked(false);
        userRepository.save(user);
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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserid(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자 없음: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUserid())
                .password(user.getPasswd()) // 반드시 암호화된 비밀번호!
                .roles(user.getRole())
                .accountLocked(Boolean.TRUE.equals(user.getAccountLocked()))
                .build();
    }
}
