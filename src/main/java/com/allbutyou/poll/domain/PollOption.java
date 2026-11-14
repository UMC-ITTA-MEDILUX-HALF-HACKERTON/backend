package com.allbutyou.poll.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 투표 선택지(메뉴·키워드·장소 등). 장소 정보는 선택이다. */
@Entity
@Table(name = "poll_options", indexes = @Index(name = "idx_poll_option_poll", columnList = "poll_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PollOption {

    public static final int LABEL_MAX_LENGTH = 50;
    public static final int PLACE_NAME_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(nullable = false, length = LABEL_MAX_LENGTH)
    private String label;

    @Column(length = PLACE_NAME_MAX_LENGTH)
    private String placeName;

    /** lat·lng는 둘 다 있거나 둘 다 없다. */
    private Double lat;

    private Double lng;

    private PollOption(Poll poll, String label, String placeName, Double lat, Double lng) {
        if ((lat == null) != (lng == null)) {
            throw new IllegalArgumentException("lat과 lng는 함께 입력해야 합니다.");
        }
        this.poll = Objects.requireNonNull(poll);
        this.label = Objects.requireNonNull(label);
        this.placeName = placeName;
        this.lat = lat;
        this.lng = lng;
    }

    public static PollOption create(Poll poll, String label, String placeName, Double lat, Double lng) {
        return new PollOption(poll, label, placeName, lat, lng);
    }

    public boolean belongsTo(Poll target) {
        return poll.getId() != null && poll.getId().equals(target.getId());
    }
}
