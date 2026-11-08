package br.com.fiap.vacismart.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import java.util.UUID;

@MappedSuperclass
@Getter
public abstract class Entidade {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
}
