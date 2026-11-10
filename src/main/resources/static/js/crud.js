/*
 * Tela genérica de "lista + cadastro/edição", usada por Clientes, Categorias e Técnicos.
 * Os nomes dos campos do formulário são os mesmos dos DTOs de Request da API.
 */
function iniciarCrud({ endpoint, colunas, tituloNovo, tituloEditar }) {
    const form = document.getElementById('form');
    const tituloForm = document.getElementById('titulo-form');
    const botaoCancelar = document.getElementById('cancelar');
    const linhas = document.getElementById('linhas');
    let registros = [];
    let editandoId = null;

    function novo() {
        editandoId = null;
        form.reset();
        tituloForm.textContent = tituloNovo;
        botaoCancelar.hidden = true;
    }

    function editar(id) {
        const registro = registros.find(r => r.id === id);
        if (!registro) return;
        editandoId = id;
        for (const campo of form.elements) {
            if (campo.name) campo.value = registro[campo.name] ?? '';
        }
        tituloForm.textContent = tituloEditar;
        botaoCancelar.hidden = false;
        form.scrollIntoView({ behavior: 'smooth', block: 'center' });
        form.elements[0].focus();
    }

    async function carregar() {
        try {
            registros = await api('GET', endpoint);
        } catch (e) {
            erro(e);
            return;
        }
        if (!registros.length) {
            linhas.innerHTML = `<tr><td colspan="${colunas.length + 1}" class="vazio">Nenhum registro cadastrado.</td></tr>`;
            return;
        }
        linhas.innerHTML = registros.map(registro => `<tr>
            ${colunas.map(c => `<td>${c.render ? c.render(registro) : esc(registro[c.campo] ?? '—')}</td>`).join('')}
            <td class="col-acoes">
                <button type="button" class="pequeno secundario" data-editar="${registro.id}">Editar</button>
                <button type="button" class="pequeno perigo" data-excluir="${registro.id}">Excluir</button>
            </td>
        </tr>`).join('');
    }

    linhas.addEventListener('click', async evento => {
        const botao = evento.target.closest('button');
        if (!botao) return;
        if (botao.dataset.editar) {
            editar(Number(botao.dataset.editar));
            return;
        }
        if (botao.dataset.excluir) {
            const id = Number(botao.dataset.excluir);
            if (!confirm('Confirma a exclusão deste registro?')) return;
            try {
                await api('DELETE', `${endpoint}/${id}`);
                mensagem('Registro excluído.');
                if (editandoId === id) novo();
                await carregar();
            } catch (e) {
                erro(e);
            }
        }
    });

    form.addEventListener('submit', async evento => {
        evento.preventDefault();
        const dados = dadosFormulario(form);
        try {
            if (editandoId) {
                await api('PUT', `${endpoint}/${editandoId}`, dados);
                mensagem('Alterações salvas.');
            } else {
                await api('POST', endpoint, dados);
                mensagem('Cadastro realizado.');
            }
            novo();
            await carregar();
        } catch (e) {
            erro(e);
        }
    });

    botaoCancelar.addEventListener('click', novo);

    novo();
    carregar().then(() => {
        const id = parametro('editar');
        if (id) editar(Number(id));
    });
}
