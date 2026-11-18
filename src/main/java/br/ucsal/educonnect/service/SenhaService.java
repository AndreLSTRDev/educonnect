package br.ucsal.educonnect.service;

import br.ucsal.educonnect.exception.RegraNegocioException;
import br.ucsal.educonnect.model.TokenRedefinicaoSenha;
import br.ucsal.educonnect.model.Usuario;
import br.ucsal.educonnect.repository.TokenRedefinicaoSenhaRepository;
import br.ucsal.educonnect.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pedido do cliente (validação do RF01): "Esqueci minha senha" com link de uso único válido por 30 minutos. */
@Service
public class SenhaService {

    static final int MINUTOS_VALIDADE = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final TokenRedefinicaoSenhaRepository tokens;
    private final UsuarioService usuarioService;
    private final NotificacaoService notificacao;
    private final PasswordEncoder encoder;

    public SenhaService(UsuarioRepository usuarios, TokenRedefinicaoSenhaRepository tokens,
                        UsuarioService usuarioService, NotificacaoService notificacao, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.usuarioService = usuarioService;
        this.notificacao = notificacao;
        this.encoder = encoder;
    }

    /**
     * Gera o link e envia ao e-mail cadastrado. Por segurança a tela mostra sempre a mesma mensagem,
     * exista ou não o usuário. Retorna o token gerado (vazio se o usuário não existe), usado nos testes.
     */
    @Transactional
    public Optional<String> solicitar(String identificadorOuEmail, String urlBase) {
        String chave = identificadorOuEmail == null ? "" : identificadorOuEmail.trim();
        Optional<Usuario> usuario = usuarios.findByIdentificadorInstitucionalIgnoreCase(chave)
                .or(() -> usuarios.findFirstByEmailIgnoreCase(chave))
                .filter(Usuario::isAtivo);
        if (usuario.isEmpty()) {
            return Optional.empty();
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        TokenRedefinicaoSenha t = new TokenRedefinicaoSenha();
        t.setTokenHash(hash(token));
        t.setUsuario(usuario.get());
        t.setExpiraEm(LocalDateTime.now().plusMinutes(MINUTOS_VALIDADE));
        tokens.save(t);

        notificacao.enviar(usuario.get().getEmail(), "EduConnect: redefinição de senha",
                "Olá, " + usuario.get().getNomeCompleto() + ".\n\n"
                + "Para criar uma nova senha, acesse o link abaixo (válido por " + MINUTOS_VALIDADE + " minutos):\n"
                + urlBase + "/redefinir-senha?token=" + token + "\n\n"
                + "Se você não pediu a troca, ignore este e-mail.");
        return Optional.of(token);
    }

    @Transactional(readOnly = true)
    public boolean tokenValido(String token) {
        return token != null && tokens.findByTokenHash(hash(token)).filter(TokenRedefinicaoSenha::valido).isPresent();
    }

    @Transactional
    public void redefinir(String token, String novaSenha, String confirmacao) {
        TokenRedefinicaoSenha t = tokens.findByTokenHash(hash(token == null ? "" : token))
                .filter(TokenRedefinicaoSenha::valido)
                .orElseThrow(() -> new RegraNegocioException(
                        "O link de redefinição é inválido ou expirou. Peça um novo link."));
        if (novaSenha == null || !novaSenha.equals(confirmacao)) {
            throw new RegraNegocioException("A confirmação não confere com a nova senha.");
        }
        usuarioService.validarSenha(novaSenha);
        t.getUsuario().setSenhaHash(encoder.encode(novaSenha));
        t.setUsado(true);
    }

    private static String hash(String token) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
