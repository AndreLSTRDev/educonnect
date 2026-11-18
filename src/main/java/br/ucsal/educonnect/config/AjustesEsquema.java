package br.ucsal.educonnect.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ajustes de esquema para bancos criados por versões anteriores.
 * A coluna denuncias.categoria nasceu como ENUM no H2, o que impedia as categorias novas
 * (Assédio e Discriminação, pedidas pelo cliente). Aqui ela passa a ser VARCHAR.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AjustesEsquema implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AjustesEsquema.class);

    private final JdbcTemplate jdbc;

    public AjustesEsquema(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String tipo = jdbc.queryForObject(
                    "select data_type from information_schema.columns "
                    + "where upper(table_name) = 'DENUNCIAS' and upper(column_name) = 'CATEGORIA'", String.class);
            if (tipo != null && tipo.toUpperCase().contains("ENUM")) {
                jdbc.execute("alter table denuncias alter column categoria varchar(30) not null");
                log.info("Coluna denuncias.categoria convertida de ENUM para VARCHAR.");
            }
        } catch (Exception e) {
            // Banco novo (sem a tabela ainda) ou outro SGBD: nada a ajustar.
            log.debug("Ajuste de esquema não aplicado: {}", e.getMessage());
        }
    }
}
