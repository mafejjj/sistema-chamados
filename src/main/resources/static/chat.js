const mensagens = document.getElementById('mensagens');
const formulario = document.getElementById('formulario');
const campo = document.getElementById('texto');

function adicionar(texto, autor) {
    const balao = document.createElement('div');
    balao.className = 'balao ' + autor;
    balao.textContent = texto; // textContent evita injeção de HTML
    mensagens.appendChild(balao);
    mensagens.scrollTop = mensagens.scrollHeight;
}

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const texto = campo.value.trim();
    if (!texto) {
        return;
    }
    adicionar(texto, 'usuario');
    campo.value = '';
    try {
        const resposta = await fetch('/api/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ mensagem: texto })
        });
        const dados = await resposta.json();
        adicionar(dados.resposta || dados.erro || 'Algo deu errado.', 'bot');
    } catch (erro) {
        adicionar('Não consegui falar com o servidor.', 'bot');
    }
});

adicionar('Oi! Me conte suas faltas ou pergunte quantas você tem. Digite "ajuda" para ver exemplos.', 'bot');
campo.focus();
