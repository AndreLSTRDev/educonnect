// RNF-D.4: indicador de processamento ao enviar formulários.
document.addEventListener('submit', function (e) {
    var form = e.target;
    if (!(form instanceof HTMLFormElement) || !form.checkValidity()) { return; }
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
