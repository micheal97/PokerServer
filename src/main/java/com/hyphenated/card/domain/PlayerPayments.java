package com.hyphenated.card.domain;

import com.hyphenated.card.enums.Payment;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
public class PlayerPayments {
    @NonNull
    private List<Payment> payments;
}