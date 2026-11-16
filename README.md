# EduConnect – Sistema de Gestão Acadêmica (Spring Boot)

Implementação da ERS "EduConnect" (UCSAL, 2026) com Spring Boot 3, Spring Security, Spring Data JPA, Thymeleaf e Bootstrap.

## Como executar

Requisitos: **Java 17+** e **Maven 3.9+** (com internet para baixar as dependências).

```bash
mvn test                 # roda os testes
mvn spring-boot:run      # sobe em http://localhost:8080
```

Usuários de demonstração (criados só se o banco estiver vazio; senha `Edu@12345`):

| Perfil | Login |
|---|---|
| Administrador | `admin` |
| Professor | `prof` |
| Aluno | `aluno1`, `aluno2` |
| Gestor de Denúncias | `gestor` |

Para desligar os dados de exemplo: `educonnect.seed.enabled=false`. **Troque as senhas fora do ambiente de demonstração.**

### HTTPS (RNF-S.4)

```bash
keytool -genkeypair -alias educonnect -keyalg RSA -keysize 2048 -storetype PKCS12 \
  -keystore keystore.p12 -validity 365 -storepass changeit -dname "CN=localhost"
mvn spring-boot:run -Dspring-boot.run.profiles=https     # https://localhost:8443
```
Em produção, use um certificado válido e a variável `KEYSTORE_PASSWORD`.

## Mapa dos requisitos

| Requisito | Onde está |
|---|---|
| RF01 Login e acesso por perfil | `SecurityConfig`, `UsuarioDetailsService`, `AuthController` (`/painel` usa `exibirPainel()`) |
| RF02 Cadastro de usuários (criar/alterar/desativar, identificador único) | `UsuarioService`, `AdminController` |
| RF03 Cursos e disciplinas (nome, código, carga horária) | `CadastroService` |
| RF04 Turmas (professor + alunos, sem aluno duplicado) | `CadastroService`, `Turma` (Set + chave composta) |
| RF05 Avaliações e notas (valida a escala) | `AvaliacaoService`, `Nota.validar()` |
| RF06 Frequência e percentual | `FrequenciaService` |
| RF07 Desempenho (médias, frequência, situação por período) | `DesempenhoService`, `/aluno/desempenho` |
| RF08 Histórico acadêmico | `DesempenhoService.historico`, `/aluno/historico` |
| RF09 Registro de denúncias com protocolo | `DenunciaService.registrar`, `Denuncia.gerarProtocolo()` |
| RF10 Gestão de denúncias com histórico de alterações | `DenunciaService.atualizarStatus`, `HistoricoDenuncia` |
| RNF-D | Índices por chaves/unique; indicador de processamento em `static/js/app.js` |
| RNF-S.2/S.3 | Perfis por rota + checagem de dono (professor/aluno); senhas com BCrypt |
| RNF-S.5 (LGPD) | Só dados necessários; cada aluno vê apenas os próprios dados |
| RNF-S.6 | JPA/Spring Data com consultas parametrizadas |
| RNF-U | Bootstrap responsivo, rótulos, `*` em obrigatórios, mensagens claras, `lang`, skip-link |
| RNF-C.2–C.4 | `BackupService` (diário, 02:00, pasta `backups/`) |
| RNF-C.5 | Log em `logs/educonnect.log` |

Conceitos do diagrama de classes: **herança** (`Usuario` → `Aluno`, `Professor`, `Administrador`, `GestorDenuncias`), **polimorfismo** (`exibirPainel()`), **sobrecarga** (`AvaliacaoService.lancarNota` com/sem observações; `DesempenhoService.calcularMedia` por turma ou por período).

## Decisões que a ERS não define (ajuste em `application.properties`)

- Escala 0 a 10 (`educonnect.escala-maxima`).
- Média = soma das notas ÷ soma dos valores máximos das avaliações lançadas × 10.
- Aprovação: média ≥ 6,0 (`media-minima`) e frequência ≥ 75% (`frequencia-minima`). A situação final só vale com a turma **encerrada** (botão do Administrador); antes disso aparece "Em andamento".
- Frequência = presenças ÷ aulas registradas.
- Status de denúncia: Aberta, Em análise, Resolvida, Arquivada.
- Banco H2 em arquivo (`./data`), para rodar sem instalar nada. Para produção, troque o `spring.datasource.*` por PostgreSQL/MySQL e adicione o driver no `pom.xml`.

## Restaurar backup (RNF-C.4/C.6)

1. Pare a aplicação.
2. Apague `data/educonnect.mv.db`.
3. Execute o script com o H2: `java -cp h2-*.jar org.h2.tools.RunScript -url jdbc:h2:file:./data/educonnect -user sa -script backups/educonnect-AAAAMMDD-HHMMSS.sql`
4. Suba a aplicação e teste o login. Repita periodicamente para validar os backups.

## Fora desta versão (conforme a ERS)

Pagamentos, financeiro e aulas virtuais. Os requisitos de 500 usuários simultâneos, 99% de disponibilidade e 3 s de resposta dependem de infraestrutura e precisam de teste de carga (ex.: JMeter) no ambiente final; o código não os comprova sozinho.
