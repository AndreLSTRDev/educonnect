// Pedido do cliente (validação do RF07): gráfico de evolução das notas do aluno.
(function () {
    var series = window.EC_EVOLUCAO || [];
    var canvas = document.getElementById('graficoEvolucao');
    if (!canvas || !series.length || typeof Chart === 'undefined') { return; }

    var css = getComputedStyle(document.documentElement);
    var cor = function (nome, padrao) { return (css.getPropertyValue(nome) || '').trim() || padrao; };
    var paleta = [cor('--ec-primaria', '#4f46e5'), cor('--ec-destaque', '#0d9488'), '#d97706', '#be185d', '#2563eb', '#65a30d'];
    var texto = cor('--ec-texto-suave', '#64748b');
    var grade = cor('--ec-borda', '#e5e7f0');

    // Eixo X: datas das avaliações de todas as disciplinas, em ordem.
    var datas = [];
    series.forEach(function (s) { s.pontos.forEach(function (p) { if (datas.indexOf(p.data) < 0) { datas.push(p.data); } }); });
    datas.sort();
    var rotulo = function (iso) { var p = iso.split('-'); return p[2] + '/' + p[1]; };

    var datasets = series.map(function (s, i) {
        var porData = {};
        s.pontos.forEach(function (p) { porData[p.data] = p; });
        return {
            label: s.disciplina,
            data: datas.map(function (d) { return porData[d] ? porData[d].nota : null; }),
            avaliacoes: datas.map(function (d) { return porData[d] ? porData[d].avaliacao : null; }),
            borderColor: paleta[i % paleta.length],
            backgroundColor: paleta[i % paleta.length],
            borderWidth: 2, pointRadius: 4, pointHoverRadius: 6, spanGaps: true, tension: 0.25
        };
    });
    datasets.push({
        label: 'Média mínima (6,0)', data: datas.map(function () { return 6; }),
        borderColor: texto, borderDash: [6, 4], borderWidth: 1, pointRadius: 0, pointHoverRadius: 0
    });

    new Chart(canvas, {
        type: 'line',
        data: { labels: datas.map(rotulo), datasets: datasets },
        options: {
            responsive: true, maintainAspectRatio: false,
            interaction: { mode: 'nearest', intersect: false },
            scales: {
                y: { min: 0, max: 10, ticks: { stepSize: 2, color: texto }, grid: { color: grade } },
                x: { ticks: { color: texto }, grid: { display: false } }
            },
            plugins: {
                legend: { labels: { color: texto, usePointStyle: true, boxWidth: 8 } },
                tooltip: {
                    callbacks: {
                        title: function (itens) {
                            var it = itens[0];
                            var av = it.dataset.avaliacoes ? it.dataset.avaliacoes[it.dataIndex] : null;
                            return av ? av + ' (' + it.label + ')' : it.label;
                        },
                        label: function (it) {
                            return it.dataset.label + ': ' + String(it.parsed.y.toFixed(1)).replace('.', ',');
                        }
                    }
                }
            }
        }
    });
})();
