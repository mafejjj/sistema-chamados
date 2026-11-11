/*
 * Funções compartilhadas por todas as telas.
 * O front-end não acessa o banco: ele só consome as rotas REST da API.
 */

const STATUS = {
    ABERTO: 'Aberto',
    EM_ATENDIMENTO: 'Em atendimento',
    AGUARDANDO_CLIENTE: 'Aguardando cliente',
    FECHADO: 'Fechado'
};

const PRIORIDADES = { BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta', CRITICA: 'Crítica' };

const PERFIS = { ADMIN: 'Administrador', TECNICO: 'Técnico' };

// Mesmas transições validadas no ChamadoService (a API continua sendo quem decide).
const TRANSICOES = {
    ABERTO: ['EM_ATENDIMENTO'],
    EM_ATENDIMENTO: ['AGUARDANDO_CLIENTE', 'FECHADO'],
    AGUARDANDO_CLIENTE: ['EM_ATENDIMENTO', 'FECHADO'],
    FECHADO: []
};

const MENU = [
    ['index.html', 'Dashboard'],
    ['chamados.html', 'Chamados'],
    ['clientes.html', 'Clientes'],
    ['categorias.html', 'Categorias'],
    ['tecnicos.html', 'Técnicos']
];

function montarLayout(paginaAtiva) {
    const header = document.createElement('header');
    const links = MENU.map(([href, texto]) =>
        `<a href="${href}" class="${href === paginaAtiva ? 'ativo' : ''}">${texto}</a>`).join('');
    header.innerHTML = `<div class="wrap">
        <a class="marca" href="index.html">Sistema de Chamados</a>
        <nav>${links}</nav>
    </div>`;
    document.body.prepend(header);
}

/** Chama a API e transforma ErroResposta (400/404/409) em Error com mensagem legível. */
async function api(metodo, url, corpo) {
    const opcoes = { method: metodo, headers: { Accept: 'application/json' } };
    if (corpo !== undefined) {
        opcoes.headers['Content-Type'] = 'application/json';
        opcoes.body = JSON.stringify(corpo);
    }

    let resposta;
    try {
        resposta = await fetch(url, opcoes);
    } catch {
        throw new Error('Não foi possível conectar à API. Verifique se a aplicação está rodando.');
    }

    if (resposta.status === 204) return null;
    const dados = await resposta.json().catch(() => null);
    if (!resposta.ok) {
        const partes = dados && dados.erro
            ? [dados.erro, ...(dados.detalhes || [])]
            : [`Erro ${resposta.status}`];
        throw new Error(partes.join('\n'));
    }
    return dados;
}

function mensagem(texto, tipo = 'ok') {
    const el = document.getElementById('mensagem');
    el.textContent = texto;
    el.className = `mensagem ${tipo}`;
    el.hidden = false;
    clearTimeout(el.temporizador);
    if (tipo === 'ok') {
        el.temporizador = setTimeout(() => { el.hidden = true; }, 4000);
    }
    el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function erro(e) {
    mensagem(e.message, 'erro');
}

function esc(valor) {
    return String(valor ?? '').replace(/[&<>"']/g, c =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
}

function formatarData(iso) {
    if (!iso) return '—';
    const data = new Date(iso.slice(0, 19));
    if (isNaN(data)) return iso;
    return data.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

function badgeStatus(status) {
    return `<span class="badge status-${status}">${STATUS[status] ?? esc(status)}</span>`;
}

function badgePrioridade(prioridade) {
    return `<span class="badge prio-${prioridade}">${PRIORIDADES[prioridade] ?? esc(prioridade)}</span>`;
}

function linkChamado(chamado) {
    return `<a href="chamado.html?id=${chamado.id}">${esc(chamado.titulo)}</a>`;
}

function parametro(nome) {
    return new URLSearchParams(location.search).get(nome);
}

/** Lê os campos do formulário; campos vazios viram null para a Bean Validation avaliar. */
function dadosFormulario(form) {
    const dados = {};
    for (const [campo, valor] of new FormData(form)) {
        const texto = String(valor).trim();
        dados[campo] = texto === '' ? null : texto;
    }
    return dados;
}

function paraNumero(valor) {
    return valor == null || valor === '' ? null : Number(valor);
}

function opcoesMapa(mapa, selecionado = '', textoVazio = null) {
    const vazio = textoVazio === null ? '' : `<option value="">${esc(textoVazio)}</option>`;
    return vazio + Object.entries(mapa).map(([valor, rotulo]) =>
        `<option value="${valor}" ${valor === selecionado ? 'selected' : ''}>${esc(rotulo)}</option>`
    ).join('');
}

function opcoesLista(lista, selecionado = '', textoVazio = null) {
    const vazio = textoVazio === null ? '' : `<option value="">${esc(textoVazio)}</option>`;
    return vazio + lista.map(item =>
        `<option value="${item.id}" ${String(item.id) === String(selecionado) ? 'selected' : ''}>${esc(item.nome)}</option>`
    ).join('');
}

function indexarPorId(lista) {
    return Object.fromEntries(lista.map(item => [item.id, item]));
}

function nomeDe(mapa, id) {
    if (id == null) return '—';
    return esc(mapa[id]?.nome ?? `#${id}`);
}

/** Desenha "Anterior / Página x de y / Próxima" a partir do Page devolvido pela API. */
function renderizarPaginacao(elemento, pagina, aoMudar) {
    const info = pagina.page ?? pagina;
    const atual = info.number;
    const total = info.totalPages;
    const quantidade = `<span>${info.totalElements} registro(s)</span>`;
    if (!total || total <= 1) {
        elemento.innerHTML = quantidade;
        return;
    }
    elemento.innerHTML = `${quantidade}
        <button type="button" class="pequeno secundario" data-pagina="${atual - 1}" ${atual === 0 ? 'disabled' : ''}>Anterior</button>
        <span>Página ${atual + 1} de ${total}</span>
        <button type="button" class="pequeno secundario" data-pagina="${atual + 1}" ${atual >= total - 1 ? 'disabled' : ''}>Próxima</button>`;
    elemento.querySelectorAll('[data-pagina]').forEach(botao =>
        botao.addEventListener('click', () => aoMudar(Number(botao.dataset.pagina))));
}
