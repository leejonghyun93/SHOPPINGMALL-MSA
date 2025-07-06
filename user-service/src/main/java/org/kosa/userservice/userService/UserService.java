package org.kosa.userservice.userService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.dto.UserDto;
import org.kosa.userservice.entity.*;
import org.kosa.userservice.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WithdrawnUserRepository withdrawnUserRepository;
    private final UserGradeRepository userGradeRepository;

    @Transactional
    public Member saveMember(Member member) {
        member.setPassword(passwordEncoder.encode(member.getPassword()));

        if (member.getMemberGrade() == null) {
            MemberGrade defaultGrade = getLowestGrade();
            member.setMemberGrade(defaultGrade);
        }

        if (member.getStatus() == null) {
            member.setStatus("Y");
        }
        if (member.getLoginFailCnt() == null) {
            member.setLoginFailCnt(0);
        }
        if (member.getGender() == null) {
            member.setGender("U");
        }
        if (member.getMarketingAgree() == null) {
            member.setMarketingAgree("N");
        }
        if (member.getSuccessionYn() == null) {
            member.setSuccessionYn("N");
        }
        if (member.getBlacklisted() == null) {
            member.setBlacklisted("N");
        }
        if (member.getSecessionYn() == null) {
            member.setSecessionYn("N");
        }

        Member savedMember = userRepository.save(member);
        return savedMember;
    }

    private MemberGrade getLowestGrade() {
        return userGradeRepository.findTopByOrderByGradeMinAmountAsc()
                .orElseThrow(() -> new RuntimeException("기본 등급을 찾을 수 없습니다."));
    }

    @Transactional
    public void updateMemberGradeByPurchaseAmount(String userId, int totalPurchaseAmount) {
        Member member = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        if (isSpecialGrade(member.getMemberGrade())) {
            return;
        }

        Optional<MemberGrade> newGrade = userGradeRepository.findBestGradeByPurchaseAmount(totalPurchaseAmount);

        if (newGrade.isPresent() &&
                !newGrade.get().getGradeId().equals(member.getMemberGrade().getGradeId())) {

            member.setMemberGrade(newGrade.get());
            userRepository.save(member);
        }
    }

    private boolean isSpecialGrade(MemberGrade grade) {
        String gradeName = grade.getGradeName().toUpperCase();
        return gradeName.contains("관리자") || gradeName.contains("ADMIN") ||
                gradeName.contains("호스트") || gradeName.contains("HOST");
    }

    private Optional<MemberGrade> findBestGradeByPurchaseAmount(int purchaseAmount) {
        return userGradeRepository.findBestGradeByPurchaseAmount(purchaseAmount);
    }

    public Optional<UserDto> getMemberDetail(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Optional.empty();
        }

        return userRepository.findByUserId(userId.trim())
                .map(this::toUserDto);
    }

    private UserDto toUserDto(Member member) {
        return UserDto.builder()
                .userId(member.getUserId())
                .name(member.getName())
                .password(member.getPassword())
                .email(member.getEmail())
                .phone(member.getPhone())
                .zipcode(member.getZipcode())
                .address(member.getAddress())
                .birthDate(member.getBirthDate())
                .gender(member.getGender())
                .successionYn(member.getSuccessionYn())
                .blacklisted(member.getBlacklisted())
                .createdDate(member.getCreatedDate())
                .sessionDate(member.getSessionDate())
                .loginFailCnt(member.getLoginFailCnt())
                .status(member.getStatus())
                .lastLogin(member.getLastLogin())
                .marketingAgree(member.getMarketingAgree())
                .socialId(member.getSocialId())
                .marketingAgent(member.getMarketingAgent())
                .gradeId(member.getMemberGrade() != null ? member.getMemberGrade().getGradeId() : null)
                .updatedDate(member.getUpdatedDate())
                .myAddress(member.getMyAddress())
                .secessionYn(member.getSecessionYn())
                .secessionDate(member.getSecessionDate())
                .profileImg(member.getProfileImg())
                .socialType(member.getSocialType())
                .accountLocked(member.isAccountLocked())
                .isBlacklisted(member.isBlacklisted())
                .isSeceded(member.isSeceded())
                .build();
    }

    public boolean isMemberExists(String userId) {
        return userRepository.existsByUserId(userId);
    }

    @Transactional
    public void deleteMember(String userId) {
        Member member = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("회원이 존재하지 않습니다."));

        WithdrawnMember withdrawnMember = WithdrawnMember.builder()
                .withdrawnId(generateWithdrawnId())
                .userId(member.getUserId())
                .withdrawnName(member.getName())
                .withdrawnEmail(member.getEmail())
                .withdrawnPhone(member.getPhone())
                .gradeId(member.getMemberGrade() != null ? member.getMemberGrade().getGradeId() : null)
                .withdrawnReason("사용자 요청")
                .withdrawnOriginalCreatedDate(member.getCreatedDate().toLocalDate())
                .withdrawnWithdrawnDate(LocalDate.now())
                .withdrawnSecessionDate(LocalDate.now())
                .build();
        withdrawnUserRepository.save(withdrawnMember);

        userRepository.delete(member);
    }

    private String generateWithdrawnId() {
        return "WD" + System.currentTimeMillis();
    }

    public Map<String, String> getNicknameMapByUserIds(List<String> userIds) {
        Map<String, String> nicknameMap = new HashMap<>();
        for (String id : userIds) {
            userRepository.findById(id).ifPresent(member -> {
                nicknameMap.put(id, member.getName());
            });
        }
        return nicknameMap;
    }

    @Transactional
    public void updateMember(UserDto userDto) {
        Member member = userRepository.findById(userDto.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        member.setName(userDto.getName());
        member.setEmail(userDto.getEmail());
        member.setPhone(userDto.getPhone());
        member.setZipcode(userDto.getZipcode());
        member.setAddress(userDto.getAddress());
        member.setBirthDate(userDto.getBirthDate());
        member.setGender(userDto.getGender());
        member.setMarketingAgree(userDto.getMarketingAgree());
        member.setMyAddress(userDto.getMyAddress());

        String password = userDto.getPassword();
        if (password != null && !password.trim().isEmpty()) {
            member.setPassword(passwordEncoder.encode(password));
        }

        userRepository.save(member);
    }

    public Optional<Member> findMemberEntityByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public Optional<UserDto> getMemberByNameAndEmail(String name, String email) {
        try {
            Optional<Member> memberOpt = userRepository.findByNameAndEmailAndStatusAndSecessionYn(
                    name, email, "Y", "N"
            );

            if (memberOpt.isPresent()) {
                Member member = memberOpt.get();
                UserDto userDto = toUserDto(member);
                return Optional.of(userDto);
            } else {
                return Optional.empty();
            }

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Transactional
    public void updatePasswordRaw(String userId, String rawPassword) {
        Optional<Member> memberOptional = userRepository.findByUserId(userId);
        if (memberOptional.isPresent()) {
            Member member = memberOptional.get();
            member.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(member);
        } else {
            throw new RuntimeException("해당 회원을 찾을 수 없습니다.");
        }
    }

    public Optional<MemberGrade> getMemberGrade(String userId) {
        return userRepository.findByUserId(userId)
                .map(Member::getMemberGrade);
    }

    public long countMembersByGrade(String gradeId) {
        return userRepository.countByMemberGradeGradeId(gradeId);
    }

    @Transactional
    public void changeMemberGrade(String userId, String newGradeId) {
        Member member = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        MemberGrade newGrade = userGradeRepository.findById(newGradeId)
                .orElseThrow(() -> new RuntimeException("등급을 찾을 수 없습니다: " + newGradeId));

        member.setMemberGrade(newGrade);
        userRepository.save(member);
    }

    public List<MemberGrade> getAllGrades() {
        return userGradeRepository.findAllByOrderByGradeMinAmountAsc();
    }

    public String getUserEmailByUserId(String userId) {
        try {
            Optional<Member> memberOpt = userRepository.findByUserId(userId);
            if (memberOpt.isPresent()) {
                Member member = memberOpt.get();
                String email = member.getEmail();
                return email;
            }

            return null;

        } catch (Exception e) {
            throw new RuntimeException("사용자 이메일 조회 실패", e);
        }
    }

    /**
     * 소셜 로그인 사용자 생성 또는 업데이트
     */
    @Transactional
    public UserDto createOrUpdateSocialUser(Map<String, Object> socialUserData) {
        try {
            String socialId = (String) socialUserData.get("socialId");
            String provider = (String) socialUserData.get("provider");
            String email = (String) socialUserData.get("email");
            String name = (String) socialUserData.get("name");
            String nickname = (String) socialUserData.get("nickname");
            String profileImage = (String) socialUserData.get("profileImage");
            String gender = (String) socialUserData.get("gender");
            String mobile = (String) socialUserData.get("mobile");

            log.info("소셜 로그인 사용자 처리 시작 - provider: {}, socialId: {}, name: '{}', nickname: '{}'",
                    provider, socialId, name, nickname);

            // 1. 기존 소셜 사용자 확인 (socialId로 검색)
            Optional<Member> existingSocialUser = userRepository.findBySocialIdAndSocialType(socialId, provider);

            if (existingSocialUser.isPresent()) {
                // 기존 소셜 사용자 업데이트
                Member member = existingSocialUser.get();
                updateSocialUserInfo(member, socialUserData);
                Member updatedMember = userRepository.save(member);
                log.info("기존 소셜 사용자 업데이트 완료 - userId: {}, name: '{}'",
                        updatedMember.getUserId(), updatedMember.getName());
                return toUserDto(updatedMember);
            }

            // 2. 이메일로 기존 일반 회원 확인 (이메일이 있는 경우)
            if (email != null && !email.trim().isEmpty()) {
                Optional<Member> existingEmailUser = userRepository.findByEmail(email);
                if (existingEmailUser.isPresent()) {
                    // 기존 회원에 소셜 정보 연동
                    Member member = existingEmailUser.get();
                    linkSocialAccount(member, socialUserData);
                    Member linkedMember = userRepository.save(member);
                    log.info("기존 이메일 회원에 소셜 연동 완료 - userId: {}, name: '{}'",
                            linkedMember.getUserId(), linkedMember.getName());
                    return toUserDto(linkedMember);
                }
            }

            // 3. 새로운 소셜 사용자 생성
            Member newSocialUser = createNewSocialUser(socialUserData);
            Member savedMember = userRepository.save(newSocialUser);
            log.info("새로운 소셜 사용자 생성 완료 - userId: {}, name: '{}'",
                    savedMember.getUserId(), savedMember.getName());
            return toUserDto(savedMember);

        } catch (Exception e) {
            log.error("소셜 사용자 처리 실패", e);
            throw new RuntimeException("소셜 사용자 처리 실패", e);
        }
    }

    /**
     * 기존 소셜 사용자 정보 업데이트 (이름 처리 개선)
     */
    private void updateSocialUserInfo(Member member, Map<String, Object> socialUserData) {
        String name = (String) socialUserData.get("name");
        String nickname = (String) socialUserData.get("nickname");
        String email = (String) socialUserData.get("email");
        String profileImage = (String) socialUserData.get("profileImage");
        String mobile = (String) socialUserData.get("mobile");

        log.info("기존 소셜 사용자 업데이트 - 현재 이름: '{}', 새 이름: '{}', 새 닉네임: '{}'",
                member.getName(), name, nickname);

        // 이름 업데이트 로직 개선
        String currentName = member.getName();
        String newName = determineFinalName(name, nickname, member.getSocialType());

        // 현재 이름이 기본값이거나 새 이름이 더 좋으면 업데이트
        if (shouldUpdateName(currentName, newName)) {
            member.setName(newName);
            log.info("이름 업데이트: '{}' → '{}'", currentName, newName);
        } else {
            log.info("기존 이름 유지: '{}'", currentName);
        }

        if (email != null && !email.trim().isEmpty()) {
            member.setEmail(email);
        }
        if (profileImage != null && !profileImage.trim().isEmpty()) {
            member.setProfileImg(profileImage);
        }
        if (mobile != null && !mobile.trim().isEmpty()) {
            member.setPhone(mobile);
        }

        member.setSessionDate(LocalDateTime.now());
        member.setLastLogin(LocalDateTime.now());
    }

    /**
     * 기존 회원에 소셜 계정 연동
     */
    private void linkSocialAccount(Member member, Map<String, Object> socialUserData) {
        String socialId = (String) socialUserData.get("socialId");
        String provider = (String) socialUserData.get("provider");
        String profileImage = (String) socialUserData.get("profileImage");

        member.setSocialId(socialId);
        member.setSocialType(provider);

        if (profileImage != null && !profileImage.trim().isEmpty()) {
            member.setProfileImg(profileImage);
        }

        member.setSessionDate(LocalDateTime.now());
        member.setLastLogin(LocalDateTime.now());
    }

    /**
     * 새로운 소셜 사용자 생성 (이름 처리 개선)
     */
    private Member createNewSocialUser(Map<String, Object> socialUserData) {
        String socialId = (String) socialUserData.get("socialId");
        String provider = (String) socialUserData.get("provider");
        String email = (String) socialUserData.get("email");
        String name = (String) socialUserData.get("name");
        String nickname = (String) socialUserData.get("nickname");
        String profileImage = (String) socialUserData.get("profileImage");
        String gender = (String) socialUserData.get("gender");
        String mobile = (String) socialUserData.get("mobile");

        // 이름 우선순위 개선
        String finalName = determineFinalName(name, nickname, provider);

        log.info("소셜 사용자 생성 - provider: {}, 원본 이름: '{}', 닉네임: '{}', 최종 이름: '{}'",
                provider, name, nickname, finalName);

        String userId = generateSocialUserId(provider, socialId);

        Member member = Member.builder()
                .userId(userId)
                .name(finalName) // 개선된 이름 설정
                .email(email)
                .phone(mobile)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .socialId(socialId)
                .socialType(provider)
                .profileImg(profileImage)
                .gender(gender != null ? gender : "U")
                .status("Y")
                .loginFailCnt(0)
                .marketingAgree("N")
                .successionYn("N")
                .blacklisted("N")
                .secessionYn("N")
                .createdDate(LocalDateTime.now())
                .sessionDate(LocalDateTime.now())
                .lastLogin(LocalDateTime.now())
                .build();

        MemberGrade defaultGrade = getLowestGrade();
        member.setMemberGrade(defaultGrade);

        return member;
    }

    /**
     * 소셜 로그인 이름 결정 로직
     */
    private String determineFinalName(String name, String nickname, String provider) {
        // 1. 실제 이름이 있고 유효하면 우선 사용
        if (isValidName(name)) {
            log.info("실제 이름 사용: '{}'", name);
            return name.trim();
        }

        // 2. 닉네임이 있고 유효하면 사용
        if (isValidName(nickname)) {
            log.info("닉네임 사용: '{}'", nickname);
            return nickname.trim();
        }

        // 3. 모두 없으면 제공업체별 기본값
        String defaultName = getProviderDefaultName(provider);
        log.info("기본 이름 사용: '{}'", defaultName);
        return defaultName;
    }

    /**
     * 유효한 이름인지 확인
     */
    private boolean isValidName(String name) {
        return name != null &&
                !name.trim().isEmpty() &&
                !name.equals("사용자") &&
                !name.equals("소셜사용자") &&
                name.trim().length() > 0;
    }

    /**
     * 제공업체별 기본 이름
     */
    private String getProviderDefaultName(String provider) {
        switch (provider.toLowerCase()) {
            case "kakao":
                return "카카오사용자";
            case "naver":
                return "네이버사용자";
            default:
                return provider + "사용자";
        }
    }

    /**
     * 이름 업데이트 여부 결정
     */
    private boolean shouldUpdateName(String currentName, String newName) {
        // 현재 이름이 기본값이면 업데이트
        if (isDefaultName(currentName)) {
            return true;
        }

        // 새 이름이 실제 이름이고 현재 이름이 닉네임이면 업데이트
        if (isRealName(newName) && !isRealName(currentName)) {
            return true;
        }

        return false;
    }

    /**
     * 기본 이름인지 확인
     */
    private boolean isDefaultName(String name) {
        if (name == null) return true;
        return name.equals("소셜사용자") ||
                name.equals("카카오사용자") ||
                name.equals("네이버사용자") ||
                name.endsWith("사용자");
    }

    /**
     * 실제 이름인지 확인 (한글 이름 또는 영문 이름)
     */
    private boolean isRealName(String name) {
        if (name == null || name.trim().isEmpty()) return false;

        // 한글 이름 패턴 (2-4글자)
        if (name.matches("^[가-힣]{2,4}$")) {
            return true;
        }

        // 영문 이름 패턴 (공백 포함 가능, 2-50글자)
        if (name.matches("^[a-zA-Z\\s]{2,50}$") && !name.toLowerCase().contains("user")) {
            return true;
        }

        return false;
    }

    /**
     * 소셜 로그인용 userId 생성
     */
    private String generateSocialUserId(String provider, String socialId) {
        // 접두사 설정 (2자리)
        String prefix;
        if ("naver".equals(provider.toLowerCase())) {
            prefix = "nv";
        } else if ("kakao".equals(provider.toLowerCase())) {
            prefix = "kt";
        } else {
            prefix = provider.substring(0, Math.min(2, provider.length())).toLowerCase();
        }

        // socialId를 안전한 길이로 자르기 (최대 30자)
        String safeSocialId = socialId.length() > 30 ? socialId.substring(0, 30) : socialId;

        // 기본 userId 생성: prefix_safeSocialId (최대 33자)
        String baseUserId = prefix + "_" + safeSocialId;

        // 데이터베이스 컬럼 길이 제한 (보통 50자) 내에서 생성
        String userId = baseUserId;

        // 중복 확인 및 처리
        int counter = 1;
        while (userRepository.existsByUserId(userId)) {
            String suffix = String.valueOf(counter);
            // 전체 길이가 50자를 넘지 않도록 조정
            int maxBaseLength = 49 - suffix.length(); // 50 - suffix길이 - 1(언더스코어)
            if (baseUserId.length() > maxBaseLength) {
                userId = baseUserId.substring(0, maxBaseLength) + "_" + suffix;
            } else {
                userId = baseUserId + "_" + suffix;
            }
            counter++;

            // 무한루프 방지
            if (counter > 9999) {
                // 타임스탬프 기반 고유 ID로 변경
                long timestamp = System.currentTimeMillis();
                userId = prefix + "_" + String.valueOf(timestamp).substring(8); // 뒤 5자리만 사용
                break;
            }
        }

        return userId;
    }
}