package br.com.sistemas.chamados;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.repository.CategoriaRepository;

@DataJpaTest
class CategoriaRepositoryTests {

    @Autowired
    private CategoriaRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    @Test
    void deveEncontrarCategoriaPorIdENomeDuplicado() {
        Categoria salva = repository.save(new Categoria("Software", "Aplicativos"));

        assertThat(repository.findById(salva.getId())).isPresent();
        assertThat(repository.existsByNome("Software")).isTrue();
        assertThat(repository.existsByNomeAndIdNot("Software", salva.getId())).isFalse();
    }

    @Test
    void deveDetectarNomeDuplicadoEmOutraCategoria() {
        Categoria salva = repository.save(new Categoria("Rede", "Conectividade"));
        Categoria outra = repository.save(new Categoria("Hardware", "Equipamentos"));

        assertThat(repository.existsByNomeAndIdNot("Rede", outra.getId())).isTrue();
        assertThat(repository.existsByNomeAndIdNot("Rede", salva.getId())).isFalse();
    }
}
