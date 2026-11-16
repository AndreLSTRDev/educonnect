package br.ucsal.educonnect.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** RNF-C.2: backup periódico do banco (script SQL do H2). Restauração: veja o README (RNF-C.4). */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final JdbcTemplate jdbc;
    private final String diretorio;

    public BackupService(JdbcTemplate jdbc, @Value("${educonnect.backup.dir:backups}") String diretorio) {
        this.jdbc = jdbc;
        this.diretorio = diretorio;
    }

    @Scheduled(cron = "${educonnect.backup.cron:0 0 2 * * *}")
    public void executarBackup() {
        try {
            Path dir = Paths.get(diretorio);
            Files.createDirectories(dir);
            String arquivo = dir.resolve("educonnect-" + LocalDateTime.now().format(FMT) + ".sql")
                    .toAbsolutePath().toString().replace("\\", "/").replace("'", "''");
            jdbc.execute("SCRIPT TO '" + arquivo + "'");
            log.info("Backup concluído: {}", arquivo);
        } catch (Exception e) {
            log.error("Falha ao executar o backup do banco de dados", e);
        }
    }
}
