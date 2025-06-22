package org.kosa.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_withdrawn_member")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawnMember {

    @Id
    @Column(name = "WITHDRAWN_ID", length = 30)
    private String withdrawnId;

    @Column(name = "USER_ID", length = 30, nullable = false)
    private String userId;

    @Column(name = "WITHDRAWN_NAME", length = 30, nullable = false)
    private String withdrawnName;

    @Column(name = "WITHDRAWN_EMAIL", length = 50, nullable = false)
    private String withdrawnEmail;

    @Column(name = "WITHDRAWN_PHONE", length = 20)
    private String withdrawnPhone;

    @Column(name = "GRADE_ID", length = 20, nullable = false)
    private String gradeId;

    @Column(name = "WITHDRAWN_REASON", nullable = false)
    private String withdrawnReason;

    @Column(name = "WITHDRAWN_ORIGINAL_CREATED_DATE", nullable = false)
    private LocalDate withdrawnOriginalCreatedDate;

    @Column(name = "WITHDRAWN_WITHDRAWN_DATE", nullable = false)
    private LocalDate withdrawnWithdrawnDate;

    @Column(name = "WITHDRAWN_SECESSION_DATE", nullable = false)
    private LocalDate withdrawnSecessionDate;
}