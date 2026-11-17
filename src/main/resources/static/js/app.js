// Confirmação antes de ações destrutivas: <form data-confirmar="Mensagem?">.
document.addEventListener('submit', function (e) {
    var form = e.target;
    if (form instanceof HTMLFormElement && form.dataset.confirmar && !window.confirm(form.dataset.confirmar)) {
        e.preventDefault();
        e.stopImmediatePropagation();
    }
}, true);

// RNF-D.4: indicador de processamento ao enviar formulários.
document.addEventListener('submit', function (e) {
    var form = e.target;
    if (!(form instanceof HTMLFormElement) || e.defaultPrevented || !form.checkValidity()) { return; }
    var botao = form.querySelector('button[type=submit]');
    if (botao) {
        botao.dataset.rotulo = botao.innerHTML;
        botao.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Processando...';
        setTimeout(function () { botao.disabled = true; }, 0);
    }
});

// Restaura os botões se o usuário voltar pelo histórico do navegador.
window.addEventListener('pageshow', function () {
    document.querySelectorAll('button[data-rotulo]').forEach(function (b) {
        b.innerHTML = b.dataset.rotulo;
        b.disabled = false;
    });
});

// Pedido do cliente (validação do RF01): modo escuro e troca de cor de destaque, salvos no navegador.
(function () {
    var raiz = document.documentElement;
    var midiaEscura = window.matchMedia('(prefers-color-scheme: dark)');

    function ler(chave, padrao) {
        try { return localStorage.getItem(chave) || padrao; } catch (e) { return padrao; }
    }
    function gravar(chave, valor) {
        try { localStorage.setItem(chave, valor); } catch (e) { /* navegador sem armazenamento */ }
    }
    function aplicar() {
        var escolha = ler('ec-tema', 'claro');
        var tema = escolha === 'auto' ? (midiaEscura.matches ? 'escuro' : 'claro') : escolha;
        var cor = ler('ec-cor', 'indigo');
        raiz.setAttribute('data-tema', tema);
        raiz.setAttribute('data-bs-theme', tema === 'escuro' ? 'dark' : 'light');
        if (cor === 'indigo') { raiz.removeAttribute('data-cor'); } else { raiz.setAttribute('data-cor', cor); }
        document.querySelectorAll('[data-ec-tema]').forEach(function (b) {
            var ativo = b.dataset.ecTema === escolha;
            b.classList.toggle('active', ativo);
            b.setAttribute('aria-pressed', ativo);
        });
        document.querySelectorAll('[data-ec-cor]').forEach(function (b) {
            var ativo = b.dataset.ecCor === cor;
            b.classList.toggle('active', ativo);
            b.setAttribute('aria-pressed', ativo);
        });
    }

    document.addEventListener('click', function (e) {
        var alvo = e.target.closest('[data-ec-tema], [data-ec-cor]');
        if (!alvo) { return; }
        if (alvo.dataset.ecTema) { gravar('ec-tema', alvo.dataset.ecTema); }
        if (alvo.dataset.ecCor) { gravar('ec-cor', alvo.dataset.ecCor); }
        aplicar();
    });
    if (midiaEscura.addEventListener) { midiaEscura.addEventListener('change', aplicar); }
    aplicar();
})();
