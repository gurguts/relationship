package org.example.clientservice.models.client;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "clients")
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "company")
    private String company;

    @Column(name = "person")
    private String person;

    @JsonManagedReference
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhoneNumber> phoneNumbers = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "location")
    private String location;

    @Column(name = "price_purchase")
    private String pricePurchase;

    @Column(name = "price_sale")
    private String priceSale;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "volume_month")
    private String volumeMonth;

    @Column(name = "status_id")
    private Long status;

    @Column(name = "source_id")
    private Long source;

    @Column(name = "route_id")
    private Long route;

    @Column(name = "region_id")
    private Long region;

    @Column(name = "business_id")
    private Long business;

    @Column(name = "comment")
    private String comment;

    @Column(name = "urgently")
    private Boolean urgently = false;

    @Column(name = "edrpou")
    private String edrpou;

    @Column(name = "enterprise_name")
    private String enterpriseName;

    @Column(name = "vat")
    private Boolean vat;
}
