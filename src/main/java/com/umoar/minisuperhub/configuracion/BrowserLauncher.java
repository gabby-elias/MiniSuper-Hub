package com.umoar.minisuperhub.configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;

@Component
@ConditionalOnProperty(prefix = "app.browser", name = "auto-open", havingValue = "true", matchIfMissing = true)
public class BrowserLauncher implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserLauncher.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!(event.getApplicationContext() instanceof WebServerApplicationContext context)) {
            return;
        }

        int port = context.getWebServer().getPort();
        String contextPath = context.getEnvironment().getProperty("server.servlet.context-path", "");
        String scheme = context.getEnvironment().getProperty("server.ssl.enabled", Boolean.class, false)
                ? "https" : "http";
        String basePath = contextPath.replaceAll("/+$", "");
        URI url = URI.create(scheme + "://localhost:" + port + basePath + "/");

        try {
            if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(url);
                    LOGGER.info("Abriendo MiniSuper Hub en {}", url);
                    return;
                }
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.debug("La API Desktop no pudo abrir el navegador; se probará el lanzador del sistema.", exception);
        }

        try {
            new ProcessBuilder(comandoNavegador(url))
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            LOGGER.info("Abriendo MiniSuper Hub con el lanzador del sistema en {}", url);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("No se pudo abrir automáticamente el navegador en {}", url, exception);
        }
    }

    private List<String> comandoNavegador(URI url) {
        String sistemaOperativo = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (sistemaOperativo.contains("win")) {
            return List.of("cmd.exe", "/c", "start", "", url.toString());
        }
        if (sistemaOperativo.contains("mac")) {
            return List.of("open", url.toString());
        }
        return List.of("xdg-open", url.toString());
    }
}
