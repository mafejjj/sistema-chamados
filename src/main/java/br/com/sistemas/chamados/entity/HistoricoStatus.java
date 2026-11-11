package br.com.sistemas.chamados.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Registra cada mudança de status de um chamado. */
@Entity
@Table(name = "historico_status")
public class HistoricoStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StatusChamado statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusChamado statusNovo;

    @Column(nullable = false)
    private LocalDateTime data;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chamado_id", nullable = false)
    private Chamado chamado;

    public HistoricoStatus() { }

    public HistoricoStatus(Chamado chamado, StatusChamado statusAnterior,
        StatusChamado statusNovo) {
        this.chamado = chamado;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.data = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public StatusChamado getStatusAnterior() { return statusAnterior; }
    public StatusChamado getStatusNovo() { return statusNovo; }
    public LocalDateTime getData() { return data; }
    public Chamado getChamado() { return chamado; }
}
